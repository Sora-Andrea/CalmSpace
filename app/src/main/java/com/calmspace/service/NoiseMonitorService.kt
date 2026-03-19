package com.calmspace.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.calmspace.service.AudioTimingConfig.GENERATED_NOISE_FADE_IN_DURATION_MS
import com.calmspace.service.AudioTimingConfig.GENERATED_NOISE_FADE_IN_STEP_MS
import com.calmspace.service.AudioTimingConfig.GENERATED_NOISE_HEADPHONE_FADE_IN_DURATION_MS
import com.calmspace.service.AudioTimingConfig.GENERATED_NOISE_HEADPHONE_FADE_IN_STEPS
import com.calmspace.masking.MaskingDecisionEngine
import com.calmspace.masking.MaskingDecision
import com.calmspace.masking.MaskingBucket
import com.calmspace.masking.YamnetLabelBucketResolver
import com.calmspace.masking.smoothMaskingVolume
import com.calmspace.service.AudioTimingConfig.MASKING_INFERENCE_MS
import com.calmspace.service.AudioTimingConfig.MASKING_DECISION_PROFILE
import com.calmspace.service.AudioTimingConfig.MASKING_TOP_PREDICTIONS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.sqrt
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import androidx.core.app.ActivityCompat
import java.util.concurrent.ArrayBlockingQueue
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────
// Noise Monitor Service
//
// Foreground service that handles both:
//   1. Ambient noise monitoring via microphone (AudioRecord)
//   2. Adaptive white noise masking playback (AudioTrack)
//
// The screen binds to this service and calls startWhiteNoise() /
// stopWhiteNoise() to control playback. Volume is applied internally
// based on the calculated maskingVolume — the screen does not need to
// manage volume at all.
//
// HEADPHONES IMPLEMENTATION NOTES:
// ─────────────────────────────────────────────────────────────────────
// TODO: Detect headphone connection/disconnection
//   - Use AudioManager.registerAudioDeviceCallback() to detect when
//     headphones are plugged/unplugged or Bluetooth connects
//   - Adjust volume scaling when headphones detected (see below)
//   - Pause/resume playback based on headphone state if desired
//
// TODO: Different volume limits for headphones vs speakers
//   - Speakers: Allow full 0.0-1.0 range (user's ears have distance protection)
//   - Headphones: Cap maximum at 0.7 (70%) to prevent hearing damage
//   - Store headphone state and apply different MAX_VOLUME constant
//
// TODO: Volume curve adjustment for headphones
//   - Headphones deliver sound directly to ear canal (more sensitive)
//   - Consider using a gentler curve (e.g., cube root instead of sqrt)
//   - Lower starting volume floor for headphones (0.2 vs 0.5)
//
// TODO: Safety fade-in on headphone connection
//   - If user plugs in headphones mid-session, fade volume from 0 to
//     current level over 1-2 seconds to avoid startle
//   - Detect device change via AudioManager callback
//
// TODO: Different audio routing for headphones
//   - AudioTrack should use AudioAttributes.USAGE_MEDIA for headphones
//   - Consider AudioAttributes.USAGE_ASSISTANCE_SONIFICATION for speakers
//   - This ensures proper audio focus behavior
// ─────────────────────────────────────────────────────────────────────

class NoiseMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "noise_monitor"
        private const val NOTIFICATION_ID = 1
        const val QUIET_TIMEOUT_MS = 1000L
        private const val SILENT_CALIBRATION_MS = 2000L
        private const val WN_CALIBRATION_MS    = 1000L
        private const val ATTACK_SMOOTHING = 0.55f
        private const val DECAY_SMOOTHING  = 0.88f
        private const val SAMPLE_RATE = 44100
        private const val VISUALIZER_UPDATE_MS = 33L
        private const val STATUS_UPDATE_MS = 100L
        private const val MASKING_SILENCE_RELEASE_DELAY_MS = 2500L
        private const val MASKING_MODEL_FILENAME = "yamnet.tflite"
    }

    // ─────────────────────────────────────────────────────────────────
    // State exposed to UI
    // ─────────────────────────────────────────────────────────────────

    private val _currentDb     = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _baselineDb    = MutableStateFlow(0f)
    val baselineDb: StateFlow<Float> = _baselineDb.asStateFlow()

    private val _soundEvents   = MutableStateFlow<List<SoundEvent>>(emptyList())
    val soundEvents: StateFlow<List<SoundEvent>> = _soundEvents.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isRecording   = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying     = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _startingVolume = MutableStateFlow(0.5f)
    val startingVolume: StateFlow<Float> = _startingVolume.asStateFlow()

    private val _automatedTargetVolume = MutableStateFlow(_startingVolume.value)
    val automatedTargetVolume: StateFlow<Float> = _automatedTargetVolume.asStateFlow()

    private val _automatedDecisionReason = MutableStateFlow("Idle")
    val automatedDecisionReason: StateFlow<String> = _automatedDecisionReason.asStateFlow()

    private val _currentMaskingBucket = MutableStateFlow("")
    val currentMaskingBucket: StateFlow<String> = _currentMaskingBucket.asStateFlow()

    private val _currentTopPrediction = MutableStateFlow("")
    val currentTopPrediction: StateFlow<String> = _currentTopPrediction.asStateFlow()

    private val _selectedSound = MutableStateFlow(SoundType.WHITE_NOISE)
    val selectedSound: StateFlow<SoundType> = _selectedSound.asStateFlow()

    val isHeadphonesConnected: StateFlow<Boolean> get() = headphoneManager.isConnected

    // ─────────────────────────────────────────────────────────────────
    // Internal state
    // ─────────────────────────────────────────────────────────────────

    private var wakeLock: PowerManager.WakeLock? = null
    private var recorder: AudioRecord? = null

    @Volatile private var currentMaskingVolume = 0f
    @Volatile private var isMaskingAutomationEnabled = false
    private var maskingClassifier: AudioClassifier? = null
    private var maskingTensorAudio: TensorAudio? = null
    private val yamnetLabelBucketResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        YamnetLabelBucketResolver.fromAssets(this.assets)
    }

    private val maskingDecisionEngine = MaskingDecisionEngine(
        policy = MASKING_DECISION_PROFILE,
        labelToBucket = { label -> yamnetLabelBucketResolver.resolve(label) }
    )

    private var running = false

    // Noise playback
    private var audioTrack: AudioTrack? = null
    private var noiseThread: Thread? = null
    private val isNoiseThreadRunning = AtomicBoolean(false)
    private var fadeInThread: Thread? = null
    @Volatile private var isNoiseFadeInActive = false
    @Volatile private var fadeInGain = 0f
    @Volatile private var isGeneratedNoisePlaybackEnabled = false
    @Volatile private var isHeadphoneFadeInActive = false

    private val maskingInferenceQueue = ArrayBlockingQueue<MaskingInferenceRequest>(4)
    private var maskingInferenceThread: Thread? = null
    private val isMaskingInferenceRunning = AtomicBoolean(false)

    // Pink noise generator state (Paul Kellett's refined method)
    private var b0 = 0.0; private var b1 = 0.0; private var b2 = 0.0
    private var b3 = 0.0; private var b4 = 0.0; private var b5 = 0.0; private var b6 = 0.0

    // Brown noise generator state
    private var brownLast = 0.0

    // Grey noise — inverse A-weighting coefficients at key bands (simplified IIR)
    private var greyZ1 = 0.0; private var greyZ2 = 0.0

    inner class LocalBinder : Binder() {
        fun getService(): NoiseMonitorService = this@NoiseMonitorService
    }

    private val binder = LocalBinder()

    private lateinit var headphoneManager: HeadphoneManager

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initAudioTrack()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        headphoneManager = HeadphoneManager(
            audioManager = audioManager,
            onConnected = {
                // Android auto-routes USAGE_MEDIA to headphones automatically.
                // Always use the phone's built-in mic for ambient detection — the headset mic
                // is near the user's mouth/ear and misses room noise entirely.
                // Fade in so plugging in mid-session doesn't cause a sudden loud burst.
                if (isNoiseThreadRunning.get()) startFadeIn(currentMaskingVolume)
            },
            onDisconnected = {
                // Nothing to do — mic was always on the phone mic.
            }
        )
        headphoneManager.start()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startRecording()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRecording()
        releaseAudioTrack()
        stopMaskingInferenceWorker()
        stopMaskingClassifier()

        // TODO: Unregister headphone callback
        // val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // audioManager.unregisterAudioDeviceCallback(headphoneCallback)

        headphoneManager.stop()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────
    // White noise playback — called from MonitorScreen
    // ─────────────────────────────────────────────────────────────────

    fun startWhiteNoise() {
        if (isNoiseThreadRunning.get()) return
        isNoiseThreadRunning.set(true)
        isNoiseFadeInActive = true
        _isPlaying.value = true
        currentMaskingVolume = _startingVolume.value
        fadeInGain = 0f
        audioTrack?.setVolume(0f)

        val bufSize = maxOf(
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            ), 4096
        )
        val noiseBuf = ShortArray(bufSize / 2)

        noiseThread = Thread {
            while (isNoiseThreadRunning.get()) {
                val type = _selectedSound.value
                for (i in noiseBuf.indices) {
                    noiseBuf[i] = nextSample(type)
                }
                // Keep volume synchronized with the fade/automation layer
                // while the generator thread is actively writing audio.
                if (isNoiseFadeInActive || currentMaskingVolume > 0f) {
                    applyNoiseTrackVolume()
                }
                audioTrack?.write(noiseBuf, 0, noiseBuf.size)
            }
        }.apply { start() }

        startWhiteNoiseFadeIn(_startingVolume.value)
    }

    fun stopWhiteNoise() {
        if (!isNoiseThreadRunning.get()) return
        isNoiseThreadRunning.set(false)
        fadeInThread?.interrupt()
        fadeInThread = null
        isNoiseFadeInActive = false
        fadeInGain = 0f

        // Do not join here — this may be called from the main thread (button click).
        // The thread will exit on its own once the flag is false.
        noiseThread = null
        audioTrack?.setVolume(0f)
        _isPlaying.value = false
    }

    private fun startWhiteNoiseFadeIn(targetVolume: Float) {
        val clampedTarget = targetVolume.coerceIn(0f, 1f)
        fadeInThread?.interrupt()
        isNoiseFadeInActive = true
        fadeInThread = Thread {
            val startedAt = SystemClock.elapsedRealtime()
            while (isNoiseThreadRunning.get()) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                val fraction = (elapsed.toFloat() / GENERATED_NOISE_FADE_IN_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                fadeInGain = clampedTarget * (fraction * fraction)
                applyNoiseTrackVolume()

                if (fraction >= 1f) break
                try {
                    Thread.sleep(GENERATED_NOISE_FADE_IN_STEP_MS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }

            fadeInGain = if (isNoiseThreadRunning.get()) clampedTarget else 0f
            isNoiseFadeInActive = false
        }.apply { start() }
    }

    private fun applyNoiseTrackVolume() {
        val targetGain = if (isNoiseFadeInActive) {
            fadeInGain
        } else {
            currentMaskingVolume
        }
        val gain = targetGain.coerceIn(0f, 1f)
        audioTrack?.setVolume(gain)
    }

    fun setSoundType(type: SoundType) {
        _selectedSound.value = type
        // Reset generator state so the new sound starts clean
        b0 = 0.0; b1 = 0.0; b2 = 0.0; b3 = 0.0; b4 = 0.0; b5 = 0.0; b6 = 0.0
        brownLast = 0.0; greyZ1 = 0.0; greyZ2 = 0.0; lastWhiteForBlue = 0.0
    }

    fun setStartingVolume(volume: Float) {
        // TODO: Apply headphone-specific volume cap here
        // val maxVolume = if (_isHeadphonesConnected.value) 0.7f else 1.0f
        // _startingVolume.value = volume.coerceIn(0f, maxVolume)

        val clampedVolume = volume.coerceIn(0f, 1f)
        _startingVolume.value = clampedVolume

        if (isMaskingAutomationEnabled) {
            maskingDecisionEngine.reset(clampedVolume)
            maskingInferenceQueue.clear()
            _automatedTargetVolume.value = clampedVolume
            // For automation mode, treat slider as baseline and let the engine re-target from there.
            if (!isNoiseThreadRunning.get()) {
                currentMaskingVolume = clampedVolume
            }
        } else {
            currentMaskingVolume = clampedVolume
            if (_isPlaying.value) applyNoiseTrackVolume()
            _automatedTargetVolume.value = clampedVolume
        }
    }

    fun setGeneratedNoisePlaybackEnabled(enabled: Boolean) {
        isGeneratedNoisePlaybackEnabled = enabled
        if (!enabled) {
            stopWhiteNoise()
        } else if (running && !isNoiseThreadRunning.get()) {
            startWhiteNoise()
        }
    }

    // -------------------------------------------------------------------------
    // ML masking control surface.
    // This is the monitoring-time switch for YAMNet-driven adaptive playback.
    // -------------------------------------------------------------------------
    fun setMaskingAutomationEnabled(enabled: Boolean) {
        isMaskingAutomationEnabled = enabled
        if (enabled) {
            initMaskingClassifier()
            startMaskingInferenceWorker()
            maskingDecisionEngine.reset(_startingVolume.value)
            _currentMaskingBucket.value = "Listening..."
            _currentTopPrediction.value = ""
            _automatedTargetVolume.value = _startingVolume.value
            _automatedDecisionReason.value = "YAMNet automation ready"
        } else {
            stopMaskingInferenceWorker()
            _currentMaskingBucket.value = ""
            _currentTopPrediction.value = ""
            _automatedDecisionReason.value = "YAMNet automation disabled"
            _automatedTargetVolume.value = _startingVolume.value
        }
        // Always sync the AudioTrack so the slider gives live feedback during preview
        currentMaskingVolume = _startingVolume.value
        if (isNoiseThreadRunning.get()) applyNoiseTrackVolume()
    }

    // ─────────────────────────────────────────────────────────────────
    // Recording + monitoring
    // ─────────────────────────────────────────────────────────────────

    fun startRecording() {
        if (running) return
        running = true
        _isRecording.value = true
        _soundEvents.value = emptyList()
        val now = System.currentTimeMillis()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CalmSpace::NoiseMonitor")
        wakeLock?.acquire(10 * 60 * 60 * 1000L)

        Thread {
            val sampleRate  = 16000
            val minBufBytes = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSizeBytes  = minBufBytes
            val bufSizeShorts = bufSizeBytes / 2

            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@Thread
                }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.UNPROCESSED,
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSizeBytes
                )
                if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                    recorder?.release()
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSizeBytes
                    )
                }
                if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                    _statusMessage.value = "Error: mic init failed"
                    _isRecording.value = false
                    return@Thread
                }

                if (isMaskingAutomationEnabled) {
                    initMaskingClassifier()
                    startMaskingInferenceWorker()
                }

                recorder?.startRecording()
                val buf = ShortArray(bufSizeShorts)

                // ── Phase 1: Silent calibration ──
                var baselineSum = 0.0
                var baselineSamples = 0
                val phase1Start = System.currentTimeMillis()

                while (running && System.currentTimeMillis() - phase1Start < SILENT_CALIBRATION_MS) {
                    val read = recorder?.read(buf, 0, bufSizeShorts) ?: break
                    if (read > 0) {
                        val db = rmsToDb(buf, read)
                        baselineSum += db
                        baselineSamples++
                        _currentDb.value = db
                        _statusMessage.value = "Calibrating — measuring room: ${db.toInt()} dB (stay quiet)"
                    }
                }

                if (!running) return@Thread

                val baselineDb = if (baselineSamples > 0)
                    (baselineSum / baselineSamples).toFloat().coerceIn(20f, 70f)
                else 40f
                _baselineDb.value = baselineDb

                // ── Phase 2: Calibrate with noise playing ──
                // Measure the mic level WITH noise running so triggerLevel reflects
                // noise + quiet room together. Phase 3 only increases volume when
                // ambient sounds push ABOVE this combined floor — the speaker output
                // is effectively "calibrated out."
                currentMaskingVolume = _startingVolume.value
                applyNoiseTrackVolume()
                if (isGeneratedNoisePlaybackEnabled && !isNoiseThreadRunning.get()) startWhiteNoise()
                _automatedTargetVolume.value = _startingVolume.value

                var wnSum = 0.0; var wnSamples = 0
                val phase2Start = System.currentTimeMillis()
                while (running && System.currentTimeMillis() - phase2Start < WN_CALIBRATION_MS) {
                    val read = recorder?.read(buf, 0, bufSizeShorts) ?: break
                    if (read > 0) {
                        val db = rmsToDb(buf, read)
                        wnSum += db; wnSamples++
                        _currentDb.value = db
                        _statusMessage.value = "Calibrating..."
                    }
                }
                if (!running) return@Thread

                var triggerLevel = if (wnSamples > 0)
                    (wnSum / wnSamples).toFloat().coerceIn(baselineDb, 80f)
                else baselineDb + 3f

                _statusMessage.value = "Monitoring"

                // ── Phase 3: Monitoring ──
                val sessionStart  = phase1Start
                val detectedEvents = mutableListOf<SoundEvent>()
                var inEvent        = false
                var eventStartTime = 0L
                var eventPeakDb    = 0f
                var lastAboveThreshold = 0L
                var lastStatusUpdate   = 0L
                var lastVisualizerUpdate = 0L
                val eventThreshold = triggerLevel + 5f
                var smoothedDb = triggerLevel
                var lastInferenceMs = now
                var lastAutomationSmoothingMs = now
                var lastLoudWindowMs = now

                while (running) {
                    val read = recorder?.read(buf, 0, bufSizeShorts) ?: break
                    if (read > 0) {
                        val latestDb = rmsToDb(buf, read)
                        smoothedDb = smoothedDb * 0.7f + latestDb * 0.3f

                        // Excess above the calibrated noise+room floor triggers masking.
                        // Speaker output is "calibrated out" — only real ambient sounds
                        // push latestDb above triggerLevel.
                        val excess = latestDb - triggerLevel
                        val floor  = _startingVolume.value

                        val nowMs = System.currentTimeMillis()

                        val amplitudeBasedTargetVolume = if (excess > 0) {
                            val t = (excess / 30f).coerceIn(0f, 1f)
                            (floor + sqrt(t) * (1f - floor)).coerceIn(floor, 1f)
                        } else {
                            floor
                        }

                        if (isMaskingAutomationEnabled) {
                            // --- YAMNet-driven control path ---
                            val shouldRunMaskingInference = nowMs - lastInferenceMs >= MASKING_INFERENCE_MS
                            if (shouldRunMaskingInference) {
                                if (latestDb > eventThreshold && read > 0) {
                                    lastLoudWindowMs = nowMs
                                    enqueueMaskingInference(buf, read, floor, nowMs)
                                } else {
                                    applyMaskingDecisionFromPredictions(
                                        maskingDecisionEngine.evaluate(emptyList(), nowMs, floor),
                                        topPredictions = emptyList()
                                    )
                                }
                                lastInferenceMs = nowMs
                            }

                            if (latestDb <= eventThreshold &&
                                nowMs - lastLoudWindowMs >= MASKING_SILENCE_RELEASE_DELAY_MS &&
                                _automatedTargetVolume.value > floor
                            ) {
                                // Long-tail decay behavior: if no relevant ambient event is
                                // detected for a short while, return to baseline and decay slowly.
                                _automatedTargetVolume.value = floor
                                _automatedDecisionReason.value = "Silence detected, returning to baseline"
                            }

                            currentMaskingVolume = smoothMaskingVolume(
                                current = currentMaskingVolume,
                                target = _automatedTargetVolume.value,
                                deltaMs = nowMs - lastAutomationSmoothingMs,
                                attackRatePerMs = MASKING_DECISION_PROFILE.attackRatePerMs,
                                releaseRatePerMs = MASKING_DECISION_PROFILE.releaseRatePerMs
                            )
                            lastAutomationSmoothingMs = nowMs
                        } else {
                            // Amplitude-based control path (non-YAMNet mode).
                            // Uses envelope/energy target and compatibility smoothing.
                            currentMaskingVolume = if (amplitudeBasedTargetVolume > currentMaskingVolume) {
                                (currentMaskingVolume * ATTACK_SMOOTHING + amplitudeBasedTargetVolume * (1f - ATTACK_SMOOTHING))
                                    .coerceIn(floor, 1f)
                            } else {
                                (currentMaskingVolume * DECAY_SMOOTHING + amplitudeBasedTargetVolume * (1f - DECAY_SMOOTHING))
                                    .coerceAtLeast(floor)
                            }
                        }

                        // TODO: Apply headphone volume cap
                        // val maxVol = if (_isHeadphonesConnected.value) 0.7f else 1.0f
                        // val cappedTarget = targetVolume.coerceAtMost(maxVol)

                        // Apply volume directly — use the atomic flag, not the StateFlow,
                        // to avoid any UI-state sync lag
                        if (isNoiseThreadRunning.get()) {
                            applyNoiseTrackVolume()
                        }


                        // Sound event detection
                        val now = System.currentTimeMillis()
                        if (latestDb >= eventThreshold) {
                            if (!inEvent) {
                                inEvent = true
                                eventStartTime = now
                                eventPeakDb = latestDb
                            } else if (latestDb > eventPeakDb) {
                                eventPeakDb = latestDb
                            }
                            lastAboveThreshold = now
                        } else if (inEvent && (now - lastAboveThreshold) >= QUIET_TIMEOUT_MS) {
                            detectedEvents.add(SoundEvent(
                                offsetMs  = eventStartTime - sessionStart,
                                peakDb    = eventPeakDb,
                                durationMs = lastAboveThreshold - eventStartTime
                            ))
                            inEvent = false
                        }

                        if (now - lastVisualizerUpdate >= VISUALIZER_UPDATE_MS) {
                            _currentDb.value = latestDb
                            lastVisualizerUpdate = now
                        }

                        if (now - lastStatusUpdate >= STATUS_UPDATE_MS) {
                            _statusMessage.value =
                                "ambient: ${latestDb.toInt()} dB | mask: ${(currentMaskingVolume * 100).toInt()}%"
                            lastStatusUpdate = now
                        }
                    } else {
                        // Avoid a hot spin if AudioRecord returns non-positive values.
                        Thread.sleep(2)
                    }
                }

                if (inEvent) {
                    detectedEvents.add(SoundEvent(
                        offsetMs   = eventStartTime - sessionStart,
                        peakDb     = eventPeakDb,
                        durationMs = lastAboveThreshold - eventStartTime
                    ))
                }
                _soundEvents.value = detectedEvents

            } finally {
                try {
                    recorder?.stop()
                } catch (_: IllegalStateException) {
                    // Ignore stop calls if recorder did not reach ACTIVE state.
                }
                recorder?.release()
                recorder = null
                wakeLock?.release()
                wakeLock = null
                _isRecording.value = false
                _statusMessage.value = if (_soundEvents.value.isEmpty())
                    "Stopped — no sounds detected"
                else
                    "Stopped — ${_soundEvents.value.size} sound(s) detected"
            }
        }.start()
    }

    fun stopRecording() {
        running = false
        setGeneratedNoisePlaybackEnabled(false)
        stopWhiteNoise()
        _automatedDecisionReason.value = "Session ended"
        _automatedTargetVolume.value = _startingVolume.value
        stopMaskingInferenceWorker()
        stopMaskingClassifier()
        // Do not call recorder?.stop() here — the recording thread's finally block
        // handles its own cleanup. Calling stop() from two threads races and throws
        // IllegalStateException.
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─────────────────────────────────────────────────────────────────
    // Headphone fade-in
    // ─────────────────────────────────────────────────────────────────

    // Gradually ramps audio from 0 to targetVolume over ~1.5 seconds when
    // headphones are plugged in mid-session, to avoid a sudden loud burst.
    private fun startFadeIn(targetVolume: Float) {
        isHeadphoneFadeInActive = true
        Thread {
            val steps = GENERATED_NOISE_HEADPHONE_FADE_IN_STEPS
            val stepMs = GENERATED_NOISE_HEADPHONE_FADE_IN_DURATION_MS / steps.toLong()
            audioTrack?.setVolume(0f)
            for (i in 1..steps) {
                Thread.sleep(stepMs)
                if (!isNoiseThreadRunning.get()) {
                    isHeadphoneFadeInActive = false
                    return@Thread
                }
                val vol = targetVolume * (i.toFloat() / steps)
                audioTrack?.setVolume(vol)
                currentMaskingVolume = vol
            }
            isHeadphoneFadeInActive = false
        }.start()
    }

    // ─────────────────────────────────────────────────────────────────
    // Audio utilities
    // ─────────────────────────────────────────────────────────────────

    // ---------------------------------------------------------------------
    // YAMNet-driven masking control
    // ---------------------------------------------------------------------
    private fun initMaskingClassifier() {
        if (!isMaskingAutomationEnabled) return
        if (maskingClassifier != null && maskingTensorAudio != null) return

        runCatching {
            val classifier = AudioClassifier.createFromFile(this, MASKING_MODEL_FILENAME)
            val tensorAudio = classifier.createInputTensorAudio()
            maskingClassifier = classifier
            maskingTensorAudio = tensorAudio
            _automatedDecisionReason.value = "YAMNet model loaded"
        }.onFailure {
            isMaskingAutomationEnabled = false
            _automatedDecisionReason.value = "YAMNet unavailable"
            _currentMaskingBucket.value = ""
            _currentTopPrediction.value = ""
        }
    }

    private fun stopMaskingClassifier() {
        maskingClassifier?.close()
        maskingClassifier = null
        maskingTensorAudio = null
    }

    private fun startMaskingInferenceWorker() {
        if (!isMaskingAutomationEnabled || isMaskingInferenceRunning.get()) return
        maskingInferenceQueue.clear()
        isMaskingInferenceRunning.set(true)
        maskingInferenceThread = Thread {
            while (isMaskingInferenceRunning.get()) {
                try {
                    val request = maskingInferenceQueue.take()
                    if (!isMaskingInferenceRunning.get()) {
                        break
                    }
                    updateMaskingDecisionFromAudioWindow(
                        buffer = request.buffer,
                        read = request.read,
                        baseline = request.baseline,
                        decisionTimeMs = request.decisionTimeMs
                    )
                } catch (_: InterruptedException) {
                    // Stop path interrupts the worker thread.
                }
            }
        }.apply {
            start()
        }
    }

    private fun stopMaskingInferenceWorker() {
        if (!isMaskingInferenceRunning.getAndSet(false)) return
        maskingInferenceThread?.interrupt()
        maskingInferenceThread = null
        maskingInferenceQueue.clear()
    }

    private fun enqueueMaskingInference(
        buffer: ShortArray,
        read: Int,
        baseline: Float,
        decisionTimeMs: Long
    ) {
        if (!isMaskingAutomationEnabled || !isMaskingInferenceRunning.get()) return
        val copied = buffer.copyOfRange(0, read)
        val request = MaskingInferenceRequest(
            buffer = copied,
            read = copied.size,
            baseline = baseline,
            decisionTimeMs = decisionTimeMs
        )

        if (!maskingInferenceQueue.offer(request)) {
            // Drop the oldest pending window to keep latency bounded.
            maskingInferenceQueue.poll()
            maskingInferenceQueue.offer(request)
        }
    }

    private data class MaskingInferenceRequest(
        val buffer: ShortArray,
        val read: Int,
        val baseline: Float,
        val decisionTimeMs: Long
    )

    private fun updateMaskingDecisionFromAudioWindow(
        buffer: ShortArray,
        read: Int,
        baseline: Float,
        decisionTimeMs: Long = System.currentTimeMillis()
    ) {
        val classifier = maskingClassifier ?: return
        val tensorAudio = maskingTensorAudio ?: return

        runCatching {
            tensorAudio.load(buffer, 0, read)
            val results = classifier.classify(tensorAudio)
            val topPredictions = buildTopPredictions(results)

            val decision = maskingDecisionEngine.evaluate(topPredictions, decisionTimeMs, baseline)
            applyMaskingDecisionFromPredictions(
                decision,
                topPredictions = topPredictions
            )
        }.onFailure {
            _automatedDecisionReason.value = "YAMNet classification failed"
        }
    }

    private fun buildTopPredictions(results: List<Classifications>): MutableList<Pair<String, Float>> {
        val topPredictions = ArrayList<Pair<String, Float>>(MASKING_TOP_PREDICTIONS)

        for (result in results) {
            for (category in result.categories) {
                val score = category.score.toFloat()
                val label = category.label

                var inserted = false
                for (index in topPredictions.indices) {
                    if (score > topPredictions[index].second) {
                        topPredictions.add(index, label to score)
                        inserted = true
                        break
                    }
                }

                if (!inserted && topPredictions.size < MASKING_TOP_PREDICTIONS) {
                    topPredictions.add(label to score)
                    inserted = true
                }

                if (topPredictions.size > MASKING_TOP_PREDICTIONS) {
                    topPredictions.removeAt(topPredictions.size - 1)
                }
            }
        }

        return topPredictions
    }

    private fun applyMaskingDecisionFromPredictions(
        decision: MaskingDecision,
        topPredictions: List<Pair<String, Float>> = emptyList()
    ) {
        _automatedDecisionReason.value = "${decision.displayWinner}: ${decision.reason}"
        _currentMaskingBucket.value = decision.displayWinner
        _currentTopPrediction.value = if (decision.winner == MaskingBucket.UNKNOWN) {
            ""
        } else {
            topPredictions.firstOrNull()?.first.orEmpty()
        }

        if (decision.shouldAffectPlayback) {
            _automatedTargetVolume.value = decision.targetVolume
        }
    }

    private fun initAudioTrack() {
        val bufSize = maxOf(
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            ), 4096
        )
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.setVolume(0f)
        audioTrack?.play()
    }

    private fun releaseAudioTrack() {
        isNoiseThreadRunning.set(false)
        noiseThread = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _isPlaying.value = false
    }

    // ─────────────────────────────────────────────────────────────────
    // Noise generators
    // ─────────────────────────────────────────────────────────────────

    private fun nextSample(type: SoundType): Short = when (type) {
        SoundType.WHITE_NOISE -> nextWhite()
        SoundType.PINK_NOISE  -> nextPink()
        SoundType.BROWN_NOISE -> nextBrown()
        SoundType.BLUE_NOISE  -> nextBlue()
        SoundType.GREY_NOISE  -> nextGrey()
    }

    private fun nextWhite(): Short =
        Random.nextInt(-16384, 16384).toShort()

    // Paul Kellett's refined pink noise method (1/f spectrum)
    private var lastWhiteForBlue = 0.0
    private fun nextPink(): Short {
        val w = Random.nextDouble(-1.0, 1.0)
        b0 = 0.99886 * b0 + w * 0.0555179
        b1 = 0.99332 * b1 + w * 0.0750759
        b2 = 0.96900 * b2 + w * 0.1538520
        b3 = 0.86650 * b3 + w * 0.3104856
        b4 = 0.55000 * b4 + w * 0.5329522
        b5 = -0.7616 * b5 - w * 0.0168980
        val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11
        b6 = w * 0.115926
        return (pink * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
    }

    // Brownian / red noise: leaky integrator of white noise (1/f² spectrum)
    // Decay factor keeps the signal from drifting to the clamp boundaries,
    // which would cause DC plateaus and audible pops/breakup.
    private fun nextBrown(): Short {
        val w = Random.nextDouble(-1.0, 1.0)
        brownLast = (brownLast * 0.998 + w * 0.02).coerceIn(-1.0, 1.0)
        return (brownLast * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
    }

    // Blue noise: differentiated white noise — emphasises high frequencies
    private fun nextBlue(): Short {
        val w = Random.nextDouble(-1.0, 1.0)
        val blue = (w - lastWhiteForBlue) * 0.5
        lastWhiteForBlue = w
        return (blue * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
    }

    // Grey noise: white noise shaped by a simplified inverse A-weighting IIR
    // to sound perceptually flat to human ears
    private fun nextGrey(): Short {
        val w = Random.nextDouble(-1.0, 1.0)
        // Two-pole high-shelf boost around 2–4 kHz to compensate for ear sensitivity curve
        val out = w + 0.97 * greyZ1 - 0.47 * greyZ2
        greyZ2 = greyZ1
        greyZ1 = out
        return (out * 0.4 * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun rmsToDb(buf: ShortArray, count: Int): Float {
        var sumOfSquares = 0.0
        for (i in 0 until count) {
            val s = buf[i].toDouble()
            sumOfSquares += s * s
        }
        val rms = sqrt(sumOfSquares / count)
        return if (rms > 1) (20 * log10(rms)).toFloat().coerceIn(0f, 100f) else 0f
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Noise Monitor", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "CalmSpace is monitoring ambient noise"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("CalmSpace")
            .setContentText("Monitoring and masking ambient noise...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    // TODO: Headphone detection callback
    /*
    private val headphoneCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            addedDevices.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    _isHeadphonesConnected.value = true
                    // TODO: Implement safety fade-in
                    // - Store current volume
                    // - Set volume to 0
                    // - Gradually increase to stored volume over 1-2 seconds
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            removedDevices.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    _isHeadphonesConnected.value = false
                    // Optionally pause playback when headphones removed
                }
            }
        }
    }
    */
}

// ─────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────

data class SoundEvent(
    val offsetMs: Long,
    val peakDb: Float,
    val durationMs: Long
)

fun formatOffset(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "${min}m ${sec}s in" else "${sec}s in"
}

fun formatDuration(ms: Long): String {
    return if (ms < 1000) "${ms}ms" else "${"%.1f".format(ms / 1000f)}s"
}

package com.calmspace

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.LoginScreen
import com.calmspace.ui.authentication.SignupScreen
import com.calmspace.ui.authentication.WelcomeScreen
import com.calmspace.ui.components.BottomNavigationBar
import com.calmspace.ui.onboarding.QuestionnaireScreen
import com.calmspace.ui.ml.YamnetPocScreen
import com.calmspace.ui.screens.HomeScreen
import com.calmspace.ui.screens.MonitorScreen
import com.calmspace.ui.screens.ProfileScreen
import com.calmspace.ui.screens.SettingsScreen
import com.calmspace.ui.player.PlaybackTrackOption
import com.calmspace.ui.player.PlaybackTrack
import com.calmspace.ui.player.PrecomputedPlaybackTracks
import com.calmspace.ui.player.mediaPlayerScreen
import com.calmspace.ui.theme.CalmSpaceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

// ─────────────────────────────────────────────
// Route Constants
// TODO: Move to a dedicated Routes.kt file
// ─────────────────────────────────────────────
object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val QUESTIONNAIRE = "questionnaire"
    const val HOME = "home"
    const val MONITOR = "monitor"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val MEDIA_PLAYER = "media_player"
    const val YAMNET_POC = "yamnet_poc"
}

// ─────────────────────────────────────────────
// Screens where the bottom nav bar should show
// ─────────────────────────────────────────────
val bottomNavRoutes = setOf(
    Routes.HOME,
    Routes.MONITOR,
    Routes.PROFILE,
    Routes.SETTINGS
)

class MainActivity : ComponentActivity() {
    companion object {
        private const val VISUALIZER_BAR_COUNT = 32
        private const val MIC_SAMPLE_RATE_HZ = 44100
        private const val MIC_BUFFER_SIZE = 2048
        private const val VISUALIZER_DB_FLOOR = -80f
        private const val VISUALIZER_DB_CEILING = 0f
    }

    private var exoPlayer: ExoPlayer? = null
    private val isLoopPlayingState = mutableStateOf(false)
    private val availablePlaybackTracks = PrecomputedPlaybackTracks.tracks
    private val playbackTrackOptions = availablePlaybackTracks.map { PlaybackTrackOption(it.id, it.title) }
    private val selectedTrackIdState = mutableStateOf(availablePlaybackTracks.firstOrNull()?.id.orEmpty())
    private val playbackLevelsState = mutableStateOf(List(VISUALIZER_BAR_COUNT) { 0f })
    private val playbackDbfsState = mutableStateOf(VISUALIZER_DB_FLOOR)
    private val micLevelsState = mutableStateOf(List(VISUALIZER_BAR_COUNT) { 0f })
    private val micDbfsState = mutableStateOf(VISUALIZER_DB_FLOOR)
    private val isMicRunningState = mutableStateOf(false)
    private val hasMicPermissionState = mutableStateOf(false)

    private var playbackSyncJob: Job? = null
    private var loadedTrackId: String? = null
    private var micAudioRecord: AudioRecord? = null
    private var micCaptureJob: Job? = null
    private var startMicVisualizerAfterPermissionGrant = false

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermissionState.value = granted
        if (granted && startMicVisualizerAfterPermissionGrant) {
            try {
                startMicrophoneVisualizer()
            } catch (_: SecurityException) {
                hasMicPermissionState.value = false
                isMicRunningState.value = false
            }
        }
        startMicVisualizerAfterPermissionGrant = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasMicPermissionState.value = hasRecordAudioPermission()
        refreshPlaybackLevelsFromSelectedTrack(0)

        setContent {
            CalmSpaceTheme {

                val navController = rememberNavController()
                val currentRoute = navController
                    .currentBackStackEntryAsState().value?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute in bottomNavRoutes) {
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = Routes.MEDIA_PLAYER,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // ───────── Welcome Screen ─────────
                        composable(Routes.WELCOME) {
                            WelcomeScreen(
                                onLoginClick = {
                                    navController.navigate(Routes.LOGIN)
                                },
                                onSignupClick = {
                                    navController.navigate(Routes.SIGNUP)
                                }
                            )
                        }

                        // ───────── Login Screen ─────────
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onBack = {
                                    navController.popBackStack()
                                },
                                onLogin = { email, password ->
                                    // TODO:
                                    // - Authenticate user
                                    // - Handle success / error
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ───────── Signup Screen ─────────
                        composable(Routes.SIGNUP) {
                            SignupScreen(
                                onBackToLogin = {
                                    navController.popBackStack()
                                },
                                onSignup = { username, email, password ->
                                    // TODO:
                                    // - Create account
                                    // - Handle success / error
                                    navController.navigate(Routes.QUESTIONNAIRE)
                                }
                            )
                        }

                        // ───────── Questionnaire Screen ─────────
                        composable(Routes.QUESTIONNAIRE) {
                            QuestionnaireScreen(
                                onFinish = {
                                    // TODO:
                                    // - Save onboarding completion
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ───────── Home Screen ─────────
                        composable(Routes.HOME) {
                            HomeScreen(
                                onStartSession = {
                                    navController.navigate(Routes.MONITOR)
                                },
                                onSeeAllSessions = {
                                    // TODO: Navigate to session history screen
                                }
                            )
                        }

                        // ───────── Monitor Screen ─────────
                        composable(Routes.MONITOR) {
                            MonitorScreen(
                                onStopRecording = {
                                    // TODO: Stop foreground service, finalize session
                                }
                            )
                        }

                        // ───────── Profile Screen ─────────
                        composable(Routes.PROFILE) {
                            ProfileScreen()
                        }

                        // ───────── Settings Screen ─────────
                        composable(Routes.SETTINGS) {
                            SettingsScreen()
                        }

                        composable(Routes.MEDIA_PLAYER) {
                            mediaPlayerScreen(
                                isPlaying = isLoopPlayingState.value,
                                trackOptions = playbackTrackOptions,
                                selectedTrackId = selectedTrackIdState.value,
                                playerLevels = playbackLevelsState.value,
                                playbackDbfs = playbackDbfsState.value,
                                isMicRunning = isMicRunningState.value,
                                micLevels = micLevelsState.value,
                                micDbfs = micDbfsState.value,
                                hasMicPermission = hasMicPermissionState.value,
                                onTrackSelected = { trackId -> selectPlaybackTrack(trackId) },
                                onOpenYamnetPoc = { navController.navigate(Routes.YAMNET_POC) },
                                onTogglePlayback = { toggleLoopPlayback() },
                                onToggleMicrophone = { toggleMicrophoneVisualizer() },
                                onRequestMicrophonePermission = { requestMicrophonePermissionForMicVisualizer() }
                            )
                        }

                        composable(Routes.YAMNET_POC) {
                            YamnetPocScreen(
                                hasMicPermission = hasMicPermissionState.value,
                                onRequestMicrophonePermission = { requestMicrophonePermissionForYamnet() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- ExoPlayer amplitude source (precomputed waveform) ---
    private fun createLoopingPlayerIfNeeded() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    private fun selectedPlaybackTrack() = PrecomputedPlaybackTracks.findById(selectedTrackIdState.value)

    private fun selectPlaybackTrack(trackId: String) {
        if (trackId == selectedTrackIdState.value) return
        val wasPlaying = isLoopPlayingState.value
        selectedTrackIdState.value = trackId
        loadedTrackId = null
        refreshPlaybackLevelsFromSelectedTrack(0)

        if (exoPlayer != null) {
            loadSelectedTrackIntoPlayer()
            if (wasPlaying) {
                exoPlayer?.play()
                startPlaybackVisualizerSync()
            }
        }
    }

    private fun loadSelectedTrackIntoPlayer() {
        val track = selectedPlaybackTrack() ?: return
        createLoopingPlayerIfNeeded()
        if (loadedTrackId == track.id) return

        exoPlayer?.apply {
            val mediaItem = MediaItem.fromUri("android.resource://$packageName/${track.rawResId}")
            setMediaItem(mediaItem)
            prepare()
            seekToDefaultPosition()
        }
        loadedTrackId = track.id
    }

    private fun startLoopPlayback() {
        loadSelectedTrackIntoPlayer()
        exoPlayer?.play()
        isLoopPlayingState.value = true
        startPlaybackVisualizerSync()
    }

    private fun stopLoopPlayback() {
        exoPlayer?.pause()
        exoPlayer?.seekToDefaultPosition()
        isLoopPlayingState.value = false
        stopPlaybackVisualizerSync()
        playbackLevelsState.value = emptyLevels()
        playbackDbfsState.value = VISUALIZER_DB_FLOOR
    }

    private fun toggleLoopPlayback() {
        if (isLoopPlayingState.value) {
            stopLoopPlayback()
        } else {
            startLoopPlayback()
        }
    }

    private fun startPlaybackVisualizerSync() {
        stopPlaybackVisualizerSync()
        playbackSyncJob = lifecycleScope.launch {
            while (isActive) {
                val player = exoPlayer
                val track = selectedPlaybackTrack()
                if (player != null && track != null && player.isPlaying) {
                    val dbfs = sampleTrackDbfsAtPosition(
                        track = track,
                        positionMs = player.currentPosition,
                        durationMs = player.duration
                    )
                    playbackDbfsState.value = dbfs
                    playbackLevelsState.value = pushLevel(
                        playbackLevelsState.value,
                        dbfsToVisualizerLevel(dbfs)
                    )
                }
                awaitFrame()
            }
        }
    }

    private fun stopPlaybackVisualizerSync() {
        playbackSyncJob?.cancel()
        playbackSyncJob = null
    }

    private fun refreshPlaybackLevelsFromSelectedTrack(startIndex: Int) {
        playbackLevelsState.value = emptyLevels()
        playbackDbfsState.value = VISUALIZER_DB_FLOOR
    }

    // --- Microphone amplitude source ---
    private fun requestMicrophonePermissionForMicVisualizer() {
        startMicVisualizerAfterPermissionGrant = true
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestMicrophonePermissionForYamnet() {
        startMicVisualizerAfterPermissionGrant = false
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun toggleMicrophoneVisualizer() {
        if (isMicRunningState.value) {
            stopMicrophoneVisualizer()
        } else {
            startMicrophoneVisualizer()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicrophoneVisualizer() {
        if (isMicRunningState.value) return
        if (!hasRecordAudioPermission()) {
            hasMicPermissionState.value = false
            requestMicrophonePermissionForMicVisualizer()
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return

        val bufferSize = maxOf(minBufferSize, MIC_BUFFER_SIZE)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                MIC_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: SecurityException) {
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        micAudioRecord = recorder
        try {
            recorder.startRecording()
        } catch (_: SecurityException) {
            recorder.release()
            micAudioRecord = null
            isMicRunningState.value = false
            return
        }
        isMicRunningState.value = true

        micCaptureJob = lifecycleScope.launch(Dispatchers.Default) {
            val sampleBuffer = ShortArray(MIC_BUFFER_SIZE / 2)

            while (isActive) {
                val samplesRead = recorder.read(sampleBuffer, 0, sampleBuffer.size)
                if (samplesRead > 0) {
                    val linearLevel = calculateMicrophoneLevel(sampleBuffer, samplesRead)
                    val micDbfs = linearToDbfs(linearLevel)
                    runOnUiThread {
                        micDbfsState.value = micDbfs
                        micLevelsState.value = pushLevel(
                            micLevelsState.value,
                            dbfsToVisualizerLevel(micDbfs)
                        )
                    }
                }
            }
        }
    }

    private fun stopMicrophoneVisualizer() {
        micCaptureJob?.cancel()
        micCaptureJob = null

        micAudioRecord?.let { recorder ->
            try {
                recorder.stop()
            } catch (_: IllegalStateException) {
            }
            recorder.release()
        }
        micAudioRecord = null
        isMicRunningState.value = false
        micLevelsState.value = emptyLevels()
        micDbfsState.value = VISUALIZER_DB_FLOOR
    }

    // --- Shared visualizer helpers ---
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sampleTrackDbfsAtPosition(
        track: PlaybackTrack,
        positionMs: Long,
        durationMs: Long
    ): Float {
        val levels = track.envelopeDbfsLevels
        if (levels.isEmpty()) return VISUALIZER_DB_FLOOR
        val safeDurationMs = durationMs.coerceAtLeast((track.envelopeHopMs * levels.size).toLong())
        val wrappedPositionMs = ((positionMs % safeDurationMs) + safeDurationMs) % safeDurationMs
        val positionInBins = wrappedPositionMs.toDouble() / track.envelopeHopMs.toDouble()
        val baseIndex = (positionInBins.toInt() % levels.size).coerceAtLeast(0)
        val nextIndex = (baseIndex + 1) % levels.size
        val fraction = (positionInBins - baseIndex).toFloat().coerceIn(0f, 1f)
        return lerp(levels[baseIndex], levels[nextIndex], fraction)
    }

    private fun calculateMicrophoneLevel(buffer: ShortArray, samplesRead: Int): Float {
        var sumSquares = 0.0
        for (i in 0 until samplesRead) {
            val value = buffer[i].toDouble()
            sumSquares += value * value
        }
        val rms = sqrt(sumSquares / samplesRead)
        return (rms / 32767.0).toFloat().coerceIn(0f, 1f)
    }

    private fun pushLevel(history: List<Float>, level: Float): List<Float> {
        val trimmed = if (history.size >= VISUALIZER_BAR_COUNT) history.drop(1) else history
        return trimmed + level
    }

    private fun linearToDbfs(linear: Float): Float {
        if (linear <= 1e-12f) return VISUALIZER_DB_FLOOR
        val db = (20.0 * log10(linear.toDouble())).toFloat()
        return db.coerceIn(VISUALIZER_DB_FLOOR, VISUALIZER_DB_CEILING)
    }

    private fun dbfsToVisualizerLevel(dbfs: Float): Float {
        val clamped = dbfs.coerceIn(VISUALIZER_DB_FLOOR, VISUALIZER_DB_CEILING)
        return ((clamped - VISUALIZER_DB_FLOOR) /
            (VISUALIZER_DB_CEILING - VISUALIZER_DB_FLOOR)).coerceIn(0f, 1f)
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    private fun emptyLevels(): List<Float> = List(VISUALIZER_BAR_COUNT) { 0f }

    override fun onDestroy() {
        stopMicrophoneVisualizer()
        stopPlaybackVisualizerSync()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}

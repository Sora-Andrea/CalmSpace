package com.calmspace.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.calmspace.service.NoiseMonitorService
import com.calmspace.service.SoundEvent
import com.calmspace.service.SoundType
import com.calmspace.ui.player.AmplitudeVisualizer
import com.calmspace.ui.player.AmplitudeVisualizerMode
import com.calmspace.ui.player.PlaybackTrackOption
import com.calmspace.ui.screens.monitor.AudioPlayerCard
import com.calmspace.ui.screens.monitor.MonitorRingsDisplay
import com.calmspace.ui.screens.monitor.RecordingStatusPill
import com.calmspace.ui.screens.monitor.SoundPickerSheet
import kotlinx.coroutines.launch

private const val VISUALIZER_DB_FLOOR = -80f
private const val VISUALIZER_DB_CEILING = 0f
private const val SERVICE_RMS_DB_REFERENCE = 90f

// ─────────────────────────────────────────────────────────────────────
// Monitor Screen
//
// Active sleep session UI. Binds to NoiseMonitorService, which handles
// both ambient noise monitoring and adaptive noise playback. This screen
// calls service methods and observes resulting state.
//
// HEADPHONES INTEGRATION NOTES:
// ─────────────────────────────────────────────────────────────────────
// TODO: Observe service's isHeadphonesConnected state
//   - Pass to RecordingStatusPill to show headphone icon in header
//   - Pass to AudioPlayerCard to show volume safety warning
//
// TODO: Handle headphone disconnect dialog
//   - Show AlertDialog: "Headphones disconnected. Pause or continue?"
//   - User chooses to pause session or continue on speakers
// ─────────────────────────────────────────────────────────────────────

@Composable
fun MonitorScreen(
    micLevels: List<Float>,
    playbackLevels: List<Float>,
    trackOptions: List<PlaybackTrackOption>,
    selectedTrackId: String,
    isServiceTrack: (String) -> Boolean,
    onTrackSelected: (String) -> Unit,
    onMonitoringSessionStateChanged: (Boolean) -> Unit,
    isTrackPlaybackPlaying: Boolean,
    onToggleTrackPlayback: () -> Unit,
    onTrackVolumeChange: (Float) -> Unit,
    onMaskingAutomationVolume: (Float) -> Unit,
    onStopRecording: () -> Unit,
    onImportAudio: () -> Unit
) {
    val context = LocalContext.current

    // ─────────────────────────────────────────────────────────────────
    // Service binding
    // ─────────────────────────────────────────────────────────────────

    var service by remember { mutableStateOf<NoiseMonitorService?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = (binder as NoiseMonitorService.LocalBinder).getService()
            }
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
    }

    DisposableEffect(Unit) {
        isServiceBound = context.bindService(
            Intent(context, NoiseMonitorService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        onDispose {
            if (isServiceBound) {
                context.unbindService(connection)
            }
            service = null
            isServiceBound = false
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Collect state from service
    // ─────────────────────────────────────────────────────────────────

    var currentDb      by remember { mutableStateOf(0f) }
    var soundEvents    by remember { mutableStateOf(emptyList<SoundEvent>()) }
    var statusMessage  by remember { mutableStateOf("Ready") }
    var isRecording    by remember { mutableStateOf(false) }
    var currentBucket  by remember { mutableStateOf("") }
    var topPrediction  by remember { mutableStateOf("") }
    var isWhiteNoisePlaying by remember { mutableStateOf(false) }
    var startingVolume by remember { mutableStateOf(0.5f) }
    var selectedSound  by remember { mutableStateOf(SoundType.WHITE_NOISE) }
    var isHeadphonesConnected by remember { mutableStateOf(false) }
    var isVisualizerActive by remember { mutableStateOf(false) }
    val monitorLevels = remember(micLevels.size.coerceAtLeast(1)) {
        mutableStateListOf<Float>().apply { repeat(micLevels.size.coerceAtLeast(1)) { add(0f) } }
    }
    var maskingVolume by remember { mutableStateOf(0f) }
    val maskingLevels = remember(micLevels.size.coerceAtLeast(1)) {
        mutableStateListOf<Float>().apply { repeat(micLevels.size.coerceAtLeast(1)) { add(0f) } }
    }
    var showHeadphoneDisconnectDialog by remember { mutableStateOf(false) }
    val selectedTrackIdState by rememberUpdatedState(selectedTrackId)

    LaunchedEffect(service) {
        val svc = service ?: return@LaunchedEffect
        launch {
            var lastVizUpdateMs = 0L
            svc.currentDb.collect { db ->
                currentDb = db
                if (!isVisualizerActive) return@collect

                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastVizUpdateMs < 33L) return@collect

                lastVizUpdateMs = nowMs
                pushLevel(monitorLevels, dbToVisualizerLevel(db))
                pushLevel(maskingLevels, maskingVolume)
            }
        }
        launch { svc.soundEvents.collect    { soundEvents    = it } }
        launch { svc.statusMessage.collect  { statusMessage  = it } }
        launch { svc.isRecording.collect    { isRecording    = it } }
        launch { svc.currentMaskingBucket.collect { currentBucket = it } }
        launch { svc.currentTopPrediction.collect { topPrediction = it } }
        launch { svc.isPlaying.collect      { isWhiteNoisePlaying      = it } }
        launch { svc.startingVolume.collect { startingVolume = it } }
        launch { svc.selectedSound.collect  { selectedSound  = it } }
        launch {
            svc.automatedTargetVolume.collect { target ->
                maskingVolume = target  // always capture for visualization
                // Option A decision feed: only apply to ExoPlayer path.
                // Keeps service-generated white-noise track untouched by this control loop.
                if (!isServiceTrack(selectedTrackIdState) && isRecording) {
                    onMaskingAutomationVolume(target)
                }
            }
        }

        // TODO: Collect headphone state
        // launch { svc.isHeadphonesConnected.collect { isHeadphonesConnected = it } }
        launch {
            var prevConnected = false
            svc.isHeadphonesConnected.collect { connected ->
                if (prevConnected && !connected && isRecording) {
                    showHeadphoneDisconnectDialog = true
                }
                isHeadphonesConnected = connected
                prevConnected = connected
            }
        }
    }

    LaunchedEffect(service, isRecording) {
        val svc = service ?: return@LaunchedEffect
        svc.setMaskingAutomationEnabled(isRecording)
        svc.setGeneratedNoisePlaybackEnabled(isRecording && isServiceTrack(selectedTrackId))
    }

    LaunchedEffect(isRecording) {
        onMonitoringSessionStateChanged(isRecording)
        isVisualizerActive = isRecording
        if (!isRecording) {
            resetLevels(monitorLevels)
            resetLevels(maskingLevels)
        }
    }

    // Defensive reconciliation: keep actual playback aligned with dropdown selection
    // when returning from other screens or recovering from state drift.
    LaunchedEffect(
        selectedTrackId,
        isRecording,
        service
    ) {
        service?.setGeneratedNoisePlaybackEnabled(isRecording && isServiceTrack(selectedTrackId))
        if (!isRecording) return@LaunchedEffect

        val svc = service ?: return@LaunchedEffect
        if (isServiceTrack(selectedTrackId)) {
            if (!isWhiteNoisePlaying) svc.startWhiteNoise()
        } else if (!isTrackPlaybackPlaying) {
            onToggleTrackPlayback()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Permission handling
    // ─────────────────────────────────────────────────────────────────

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    // Start the selected ambient path for a monitoring session.
    // This keeps the logic in one place: service track starts service playback,
    // Exo track starts Exo playback.
    val startAmbientTrackForSelection = {
        service?.setMaskingAutomationEnabled(true)
        service?.setGeneratedNoisePlaybackEnabled(isServiceTrack(selectedTrackId))
        service?.setStartingVolume(startingVolume)
        if (isServiceTrack(selectedTrackId)) {
            if (!isWhiteNoisePlaying) {
                service?.startWhiteNoise()
            }
        } else if (!isTrackPlaybackPlaying) {
            onToggleTrackPlayback()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            isVisualizerActive = true
            context.startForegroundService(Intent(context, NoiseMonitorService::class.java))
            startAmbientTrackForSelection()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Sound picker state
    // ─────────────────────────────────────────────────────────────────

    var showSoundPicker by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────────────────
    // Headphone disconnect dialog
    // ─────────────────────────────────────────────────────────────────

    if (showHeadphoneDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showHeadphoneDisconnectDialog = false },
            title = { Text("Headphones disconnected") },
            text = { Text("Your headphones were removed. Continue masking on speaker or stop the session?") },
            confirmButton = {
                TextButton(onClick = { showHeadphoneDisconnectDialog = false }) {
                    Text("Continue on speaker")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHeadphoneDisconnectDialog = false
                    isVisualizerActive = false
                    service?.stopRecording()
                    onStopRecording()
                }) {
                    Text("Stop session")
                }
            }
        )
    }

    if (showSoundPicker) {
        SoundPickerSheet(
            selectedSound = selectedSound,
            onSoundSelected = { service?.setSoundType(it) },
            onDismiss = { showSoundPicker = false }
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Derived UI state
    // ─────────────────────────────────────────────────────────────────

    val bucketLabel = if (isRecording) {
        currentBucket.ifBlank { "Listening..." }
    } else {
        "No session active"
    }
    val topPredictionText = if (isRecording) topPrediction else ""
    val displayLine = if (topPredictionText.isBlank()) {
        bucketLabel
    } else {
        "$bucketLabel · $topPredictionText"
    }

    val inactiveVisualizerLevels = remember(micLevels.size) {
        List(micLevels.size.coerceAtLeast(1)) { 0f }
    }
    val visualizerLevels = if (isVisualizerActive) monitorLevels else inactiveVisualizerLevels

    val playbackSourceLevels = if (isServiceTrack(selectedTrackId)) {
        maskingLevels
    } else {
        playbackLevels
    }

    val playbackOverlayLevels = if (isVisualizerActive) {
        when {
            playbackSourceLevels.isEmpty() -> null
            playbackSourceLevels.size == visualizerLevels.size -> playbackSourceLevels
            else -> List(visualizerLevels.size) { index ->
                playbackSourceLevels[index % playbackSourceLevels.size]
            }
        }
    } else {
        null
    }

    // TODO: Track actual session start time and calculate elapsed
    val sleepTime = if (isRecording) "0:00" else "8:42"

    // ─────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {

        // ───────── Scrollable content ─────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ───────── Header ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CalmSpace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                RecordingStatusPill(
                    isRecording = isRecording,
                    isHeadphonesConnected = isHeadphonesConnected
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        // ───────── Decorative Rings ─────────
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            MonitorRingsDisplay(
                isRecording = isRecording,
                amplitude = visualizerLevels.lastOrNull() ?: 0f
            )
            AmplitudeVisualizer(
                levels = visualizerLevels,
                modifier = Modifier.fillMaxSize(),
                mode = AmplitudeVisualizerMode.CIRCULAR,
                barColor = MaterialTheme.colorScheme.primary,
                secondaryLevels = playbackOverlayLevels,
                secondaryBarColor = brighterColor(
                    MaterialTheme.colorScheme.primary,
                    0.26f
                ),
                secondaryAngleOffsetDeg = if (visualizerLevels.isNotEmpty()) {
                    360f / visualizerLevels.size.toFloat() / 2f
                } else {
                    0f
                }
            )
        }

            // ───────── Sleep Time Display ─────────
            Text(
                text = sleepTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sleep Time",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )
            Text(
                text = displayLine,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray
            )
            if (isRecording) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ───────── Audio Player Card ─────────
            val generatedNoiseIds = setOf("white_noise", "pink_noise", "brown_noise", "blue_noise", "grey_noise")
            val isGeneratedNoise = selectedTrackId in generatedNoiseIds

            AudioPlayerCard(
                trackOptions = trackOptions,
                selectedTrackId = selectedTrackId,
                isPlaying = if (isGeneratedNoise) isWhiteNoisePlaying else isTrackPlaybackPlaying,
                volume = startingVolume,
                onTrackSelected = { trackId ->
                    // Switching away from a generated noise to a file track — stop the noise thread
                    if (isGeneratedNoise && trackId !in generatedNoiseIds) {
                        service?.stopWhiteNoise()
                    }
                    // Switching to a generated noise — update the sound type
                    if (trackId in generatedNoiseIds) {
                        service?.setSoundType(when (trackId) {
                            "pink_noise"  -> SoundType.PINK_NOISE
                            "brown_noise" -> SoundType.BROWN_NOISE
                            "blue_noise"  -> SoundType.BLUE_NOISE
                            "grey_noise"  -> SoundType.GREY_NOISE
                            else          -> SoundType.WHITE_NOISE
                        })
                    }
                    onTrackSelected(trackId)
                },
                onTogglePlayback = {
                    if (isGeneratedNoise) {
                        if (isWhiteNoisePlaying) service?.stopWhiteNoise()
                        else service?.startWhiteNoise()
                    } else {
                        onToggleTrackPlayback()
                    }
                },
                onVolumeChange = { volume ->
                    service?.setStartingVolume(volume)
                    if (!isGeneratedNoise) {
                        onTrackVolumeChange(volume)
                    }
                },
                onChangeSoundClick = { showSoundPicker = true },
                isSessionActive = isRecording
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Import Audio button
            Button(
                onClick = onImportAudio,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Import Audio")
            }
            Spacer(modifier = Modifier.height(16.dp)) // optional spacing
        }

        // ───────── Start/Stop Session Button — always visible ─────────
        Button(
            onClick = {
                // NOTE for auditing:
                // session start/stop should only gate service recording + playback here.
                if (isRecording) {
                    isVisualizerActive = false
                    resetLevels(monitorLevels)
                    resetLevels(maskingLevels)
                    if (isServiceTrack(selectedTrackId)) {
                        service?.stopWhiteNoise()
                        service?.setGeneratedNoisePlaybackEnabled(false)
                    } else if (isTrackPlaybackPlaying) {
                        onToggleTrackPlayback()
                    }
                    try {
                        service?.stopRecording()
                        service?.setMaskingAutomationEnabled(false)
                    } catch (_: Exception) {
                    }
                    onStopRecording()
                } else {
                    if (hasAudioPermission) {
                        isVisualizerActive = true
                        context.startForegroundService(Intent(context, NoiseMonitorService::class.java))
                        startAmbientTrackForSelection()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isRecording)
                    MaterialTheme.colorScheme.onError
                else
                    MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isRecording) "Stop Session" else "Start Session",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun brighterColor(color: Color, strength: Float = 0.2f): Color {
    val safeStrength = strength.coerceIn(0f, 1f)
    val lighten = { component: Float ->
        (component + (1f - component) * safeStrength).coerceIn(0f, 1f)
    }
    return Color(
        red = lighten(color.red),
        green = lighten(color.green),
        blue = lighten(color.blue),
        alpha = color.alpha
    )
}

private fun dbToVisualizerLevel(dbfs: Float): Float {
    val normalizedDb = when {
        dbfs > 0f -> {
            // Service emits RMS dB (approx. 0..100), not dBFS.
            // rms is raw RMS amplitude from PCM samples (0..32767 for 16-bit signed audio)
            // Shift by the 32767 peak reference so it aligns with microphone path.
            dbfs - SERVICE_RMS_DB_REFERENCE
        }
        else -> dbfs
    }
    val clampedDb = normalizedDb.coerceIn(VISUALIZER_DB_FLOOR, VISUALIZER_DB_CEILING)
    return ((clampedDb - VISUALIZER_DB_FLOOR) / (VISUALIZER_DB_CEILING - VISUALIZER_DB_FLOOR)).coerceIn(0f, 1f)
}

private fun pushLevel(levels: MutableList<Float>, level: Float) {
    if (levels.isNotEmpty()) {
        levels.removeAt(0)
    }
    levels.add(level.coerceIn(0f, 1f))
}

private fun resetLevels(levels: MutableList<Float>) {
    for (index in levels.indices) {
        levels[index] = 0f
    }
}

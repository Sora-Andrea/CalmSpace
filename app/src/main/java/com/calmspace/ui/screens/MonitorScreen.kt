package com.calmspace.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.calmspace.service.NoiseMonitorService
import com.calmspace.service.SoundEvent
import com.calmspace.service.SoundType
import com.calmspace.ui.screens.monitor.AudioPlayerCard
import com.calmspace.ui.screens.monitor.MonitorRingsDisplay
import com.calmspace.ui.screens.monitor.RecordingStatusPill
import com.calmspace.ui.screens.monitor.SoundPickerSheet
import kotlinx.coroutines.launch

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
    onStopRecording: () -> Unit
) {
    val context = LocalContext.current

    // ─────────────────────────────────────────────────────────────────
    // Service binding
    // ─────────────────────────────────────────────────────────────────

    var service by remember { mutableStateOf<NoiseMonitorService?>(null) }
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
        context.bindService(
            Intent(context, NoiseMonitorService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        onDispose {
            context.unbindService(connection)
            service = null
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Collect state from service
    // ─────────────────────────────────────────────────────────────────

    var currentDb      by remember { mutableStateOf(0f) }
    var soundEvents    by remember { mutableStateOf(emptyList<SoundEvent>()) }
    var statusMessage  by remember { mutableStateOf("Ready") }
    var isRecording    by remember { mutableStateOf(false) }
    var isPlaying      by remember { mutableStateOf(false) }
    var startingVolume by remember { mutableStateOf(0.5f) }
    var selectedSound  by remember { mutableStateOf(SoundType.WHITE_NOISE) }

    LaunchedEffect(service) {
        val svc = service ?: return@LaunchedEffect
        launch { svc.currentDb.collect      { currentDb      = it } }
        launch { svc.soundEvents.collect    { soundEvents    = it } }
        launch { svc.statusMessage.collect  { statusMessage  = it } }
        launch { svc.isRecording.collect    { isRecording    = it } }
        launch { svc.isPlaying.collect      { isPlaying      = it } }
        launch { svc.startingVolume.collect { startingVolume = it } }
        launch { svc.selectedSound.collect  { selectedSound  = it } }

        // TODO: Collect headphone state
        // launch { svc.isHeadphonesConnected.collect { isHeadphonesConnected = it } }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            context.startForegroundService(Intent(context, NoiseMonitorService::class.java))
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Sound picker state
    // ─────────────────────────────────────────────────────────────────

    var showSoundPicker by remember { mutableStateOf(false) }

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

    val ambientNoise = if (isRecording) "${currentDb.toInt()} dB" else "-- dB"

    val lastDetected = when {
        soundEvents.isNotEmpty() -> "Detected recently"
        isRecording              -> "Monitoring..."
        else                     -> "No session active"
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
                RecordingStatusPill(isRecording = isRecording)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ───────── Decorative Rings ─────────
            MonitorRingsDisplay()

            Spacer(modifier = Modifier.height(24.dp))

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
                text = "$ambientNoise · $lastDetected",
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
            AudioPlayerCard(
                soundName = selectedSound.displayName,
                isPlaying = isPlaying,
                volume = startingVolume,
                onTogglePlayback = {
                    if (isPlaying) service?.stopWhiteNoise()
                    else service?.startWhiteNoise()
                },
                onVolumeChange = { service?.setStartingVolume(it) },
                onChangeSoundClick = { showSoundPicker = true },
                isSessionActive = isRecording
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ───────── Start/Stop Session Button — always visible ─────────
        Button(
            onClick = {
                if (isRecording) {
                    service?.stopRecording()
                    onStopRecording()
                } else {
                    if (hasAudioPermission) {
                        context.startForegroundService(Intent(context, NoiseMonitorService::class.java))
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

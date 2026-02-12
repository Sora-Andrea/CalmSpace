package com.calmspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Monitor Screen
// Active sleep session view
// Shows sleep time, mic threshold, audio controls
// ─────────────────────────────────────────────

@Composable
fun MonitorScreen(
    onStopRecording: () -> Unit
) {

    // ─────────────────────────────────────────────
    // Placeholder State
    // TODO: Replace with ViewModel + foreground service state
    // ─────────────────────────────────────────────

    // TODO: Replace with real elapsed session time from foreground service
    val sleepTime = "8:42"

    // TODO: Replace with real ambient noise dB reading from AudioRecord
    val ambientNoise = "35-40 dB"

    // TODO: Replace with real last detected noise event timestamp from Room
    val lastDetected = "Detected 2m ago"

    // TODO: Replace with real threshold from user preferences / Room
    var micThreshold by remember { mutableStateOf(0.7f) }

    // TODO: Replace with actual selected sound from user profile
    val currentSound = "White Noise"

    // TODO: Replace with real playback position from audio service
    var playbackPosition by remember { mutableStateOf(0.4f) }
    val playbackStart = "2:15"
    val playbackEnd = "3:22"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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

            // TODO: Wire to real microphone monitoring state from foreground service
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Active\nRecording",
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ───────── Decorative Rings + Icon ─────────
        // Static decorative visualization
        // TODO: Animate rings in response to real-time ambient noise levels
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
            // Inner ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
            // Center icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

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
            color = Color.Gray
        )

        // TODO: Replace with live dB reading + last noise event time from service
        Text(
            text = "$ambientNoise · $lastDetected",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Microphone Threshold Card ─────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Microphone Threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // TODO: Wire to sensitivity setting in ViewModel
                    // TODO: Persist updated value to Room user preferences
                    Slider(
                        value = micThreshold,
                        onValueChange = { micThreshold = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Audio Player Card ─────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // ───── Sound Title ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ambient Sounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        // TODO: Replace with selected sound from user profile
                        Text(
                            text = currentSound,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // TODO: Navigate to sound library / selection screen
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Add, contentDescription = "Change sound")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ───── Playback Scrubber ─────
                // TODO: Wire to real audio playback position from audio service
                Slider(
                    value = playbackPosition,
                    onValueChange = { playbackPosition = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playbackStart,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = playbackEnd,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ───── Playback Controls ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TODO: Wire to audio service skip previous
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // TODO: Wire to audio service play/pause
                    FilledIconButton(
                        onClick = { },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // TODO: Wire to audio service skip next
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ───────── Stop Recording Button ─────────
        // TODO: Stop foreground service, finalize and save session to Room
        Button(
            onClick = onStopRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Stop Recording",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
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
// Shows sleep time, ambient noise info, and audio controls
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

    // TODO: Replace with actual selected sound from user profile
    val currentSound = "White Noise"

    // TODO: Replace with real isPlaying state from audio service
    var isPlaying by remember { mutableStateOf(true) }

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
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
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

                Spacer(modifier = Modifier.height(16.dp))

                // ───── Play/Pause Button ─────
                // No scrubber or skip controls for ambient loops
                // TODO: Add scrubber + skip controls when audiobook feature is implemented
                // TODO: Wire isPlaying and onClick to audio service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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
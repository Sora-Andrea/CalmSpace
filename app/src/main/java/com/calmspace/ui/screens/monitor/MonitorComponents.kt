package com.calmspace.ui.screens.monitor

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────
// Monitor Screen Components
//
// Screen-specific composables for MonitorScreen. These are not app-wide
// reusable components — they live here alongside the screen they belong to.
// ─────────────────────────────────────────────────────────────────────

// ───────── Recording Status Pill ─────────
// Header badge showing active/inactive session state.
// TODO: Add headphone icon parameter when isHeadphonesConnected is implemented.

@Composable
fun RecordingStatusPill(isRecording: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isRecording) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.errorContainer
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (isRecording) Color.Green else Color.Red,
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isRecording) "Active\nRecording" else "Not\nActive",
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 14.sp
        )
    }
}

// ───────── Monitor Rings Display ─────────
// Three concentric decorative rings with sleep/audio icons in the center.
// TODO: Animate rings in response to real-time ambient noise levels.

@Composable
fun MonitorRingsDisplay() {
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
}

// ───────── Audio Player Card ─────────
// Card showing the active ambient sound with a play/pause toggle.
// TODO: Wire onChangeSoundClick to a sound library / selection screen.
// TODO: Show "Volume capped for safety" text when headphones are connected.

@Composable
fun AudioPlayerCard(
    soundName: String,
    isPlaying: Boolean,
    volume: Float,
    onTogglePlayback: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onChangeSoundClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Sound title + change button ──
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
                    Text(
                        text = soundName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // TODO: Show headphone warning when connected
                    // if (isHeadphonesConnected) {
                    //     Text(
                    //         text = "Volume capped for safety",
                    //         style = MaterialTheme.typography.labelSmall,
                    //         color = MaterialTheme.colorScheme.error
                    //     )
                    // }
                }

                IconButton(onClick = onChangeSoundClick) {
                    Icon(Icons.Default.Add, contentDescription = "Change sound")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Volume slider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Volume down",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume up",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Play / Pause ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledIconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

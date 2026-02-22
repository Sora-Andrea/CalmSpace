package com.calmspace.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class PlaybackTrackOption(
    val id: String,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mediaPlayerScreen(
    isPlaying: Boolean,
    trackOptions: List<PlaybackTrackOption>,
    selectedTrackId: String,
    playerLevels: List<Float>,
    playbackDbfs: Float,
    isMicRunning: Boolean,
    micLevels: List<Float>,
    micDbfs: Float,
    hasMicPermission: Boolean,
    onTrackSelected: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onRequestMicrophonePermission: () -> Unit
) {
    var trackMenuExpanded by remember { mutableStateOf(false) }
    val selectedTrackTitle = trackOptions.firstOrNull { it.id == selectedTrackId }?.title ?: "Select Track"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- ExoPlayer Visualizer ---
        Text(text = "Playback Visualizer")

        ExposedDropdownMenuBox(
            expanded = trackMenuExpanded,
            onExpandedChange = { trackMenuExpanded = !trackMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedTrackTitle,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                label = { Text("Loop Track") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackMenuExpanded)
                }
            )

            DropdownMenu(
                expanded = trackMenuExpanded,
                onDismissRequest = { trackMenuExpanded = false }
            ) {
                trackOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title) },
                        onClick = {
                            trackMenuExpanded = false
                            onTrackSelected(option.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AmplitudeVisualizer(
            levels = playerLevels,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = if (isPlaying) "Audio loop is playing" else "Audio loop is stopped")
        Text(text = "Level: ${formatDbfs(playbackDbfs)} dBFS")

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onTogglePlayback) {
            Text(text = if (isPlaying) "Stop Playback" else "Start Playback")
        }

        Spacer(modifier = Modifier.height(36.dp))

        // --- Microphone Visualizer ---
        Text(text = "Microphone Visualizer")

        AmplitudeVisualizer(
            levels = micLevels,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                !hasMicPermission -> "Microphone permission required"
                isMicRunning -> "Microphone visualizer is running"
                else -> "Microphone visualizer is stopped"
            }
        )
        Text(text = "Level: ${formatDbfs(micDbfs)} dBFS")

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (hasMicPermission) {
                    onToggleMicrophone()
                } else {
                    onRequestMicrophonePermission()
                }
            }
        ) {
            Text(
                text = when {
                    !hasMicPermission -> "Grant Microphone Permission"
                    isMicRunning -> "Stop Microphone Visualizer"
                    else -> "Start Microphone Visualizer"
                }
            )
        }
    }
}

private fun formatDbfs(dbfs: Float): String {
    val rounded = ((dbfs.coerceIn(-80f, 0f)) * 10f).roundToInt() / 10f
    return if (rounded == -0.0f) "0.0" else rounded.toString()
}

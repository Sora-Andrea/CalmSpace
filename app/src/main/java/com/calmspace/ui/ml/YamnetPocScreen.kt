package com.calmspace.ui.ml

import android.media.AudioRecord
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

@Composable
fun YamnetPocScreen(
    hasMicPermission: Boolean,
    onRequestMicrophonePermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var topClasses by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }

    LaunchedEffect(isRunning, hasMicPermission) {
        if (!isRunning || !hasMicPermission) return@LaunchedEffect

        withContext(Dispatchers.Main) {
            status = "Initializing YAMNet..."
            topClasses = emptyList()
        }

        var classifier: AudioClassifier? = null
        var audioRecord: AudioRecord? = null
        try {
            classifier = withContext(Dispatchers.Default) {
                AudioClassifier.createFromFile(context, "yamnet.tflite")
            }
            val tensorAudio = classifier.createInputTensorAudio()
            val record = classifier.createAudioRecord()
            audioRecord = record
            record.startRecording()

            withContext(Dispatchers.Main) {
                status = "Listening..."
            }

            while (isActive) {
                tensorAudio.load(audioRecord)
                val results = classifier.classify(tensorAudio)
                val predictions = results
                    .flatMap { it.categories }
                    .sortedByDescending { it.score }
                    .take(5)
                    .map { category -> category.label to category.score }

                withContext(Dispatchers.Main) {
                    topClasses = predictions
                }
            }
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                isRunning = false
                status = "YAMNet init/classification failed. Add app/src/main/assets/yamnet.tflite"
                topClasses = emptyList()
            }
        } finally {
            try {
                audioRecord?.stop()
            } catch (_: IllegalStateException) {
            }
            audioRecord?.release()
            classifier?.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text("YAMNet Sound Classifier")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Model file: app/src/main/assets/yamnet.tflite")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Status: $status")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!hasMicPermission) {
                    onRequestMicrophonePermission()
                } else {
                    isRunning = !isRunning
                    if (!isRunning) {
                        status = "Stopped"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    !hasMicPermission -> "Grant Microphone Permission"
                    isRunning -> "Stop Classification"
                    else -> "Start Classification"
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Top predictions")
        Spacer(modifier = Modifier.height(8.dp))

        if (topClasses.isEmpty()) {
            Text("No predictions yet.")
        } else {
            topClasses.forEachIndexed { index, prediction ->
                Text("${index + 1}. ${prediction.first} (${(prediction.second * 100f).toInt()}%)")
            }
        }
    }
}

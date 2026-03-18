package com.calmspace.ui.ml

import android.media.AudioRecord
import android.os.SystemClock
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
import com.calmspace.masking.MaskingBucket
import com.calmspace.masking.MaskingDecisionEngine
import com.calmspace.service.AudioTimingConfig.MASKING_DECISION_PROFILE
import com.calmspace.masking.YamnetLabelBucketResolver
import com.calmspace.masking.bucketScoresToDisplayList
import com.calmspace.masking.maskingBucketDisplayName
import com.calmspace.masking.smoothMaskingVolume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

private val MASKING_BASELINE = MASKING_DECISION_PROFILE.baselineVolume

@Composable
fun YamnetPocScreen(
    hasMicPermission: Boolean,
    onRequestMicrophonePermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val labelBucketResolver = remember { YamnetLabelBucketResolver.fromAssets(context.assets) }
    val decisionEngine = remember {
        MaskingDecisionEngine(
            policy = MASKING_DECISION_PROFILE,
            labelToBucket = { label -> labelBucketResolver.resolve(label) }
        )
    }

    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var statusDetail by remember { mutableStateOf("Idle") }
    var topClasses by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var rawBucketState by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var smoothBucketState by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var activeBucket by remember { mutableStateOf(MaskingBucket.UNKNOWN) }
    var bucketWinnerScore by remember { mutableStateOf(0f) }
    var bucketSecondScore by remember { mutableStateOf(0f) }
    var targetVolume by remember { mutableStateOf(MASKING_BASELINE) }
    var smoothedVolume by remember { mutableStateOf(MASKING_BASELINE) }

    LaunchedEffect(isRunning, hasMicPermission) {
        if (!isRunning || !hasMicPermission) return@LaunchedEffect

        withContext(Dispatchers.Main) {
            status = "Initializing YAMNet..."
            statusDetail = "Initializing..."
            topClasses = emptyList()
            rawBucketState = emptyList()
            smoothBucketState = emptyList()
            activeBucket = MaskingBucket.UNKNOWN
            bucketWinnerScore = 0f
            bucketSecondScore = 0f
            targetVolume = MASKING_BASELINE
            smoothedVolume = MASKING_BASELINE
            decisionEngine.reset(MASKING_BASELINE)
        }

        var classifier: AudioClassifier? = null
        var audioRecord: AudioRecord? = null
        var lastVolumeUpdateMs = SystemClock.elapsedRealtime()

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
                val nowMs = SystemClock.elapsedRealtime()
                val (decision, predictions) = withContext(Dispatchers.Default) {
                    tensorAudio.load(audioRecord)
                    val results = classifier.classify(tensorAudio)
                    val topPredictions = results
                        .flatMap { it.categories }
                        .sortedByDescending { it.score }
                        .take(12)
                        .map { category ->
                            Pair(category.label, category.score.toFloat())
                        }

                    decisionEngine.evaluate(topPredictions, nowMs, MASKING_BASELINE) to topPredictions
                }

                val nextVolume = smoothMaskingVolume(
                    current = smoothedVolume,
                    target = decision.targetVolume,
                    deltaMs = nowMs - lastVolumeUpdateMs
                )
                lastVolumeUpdateMs = nowMs
                smoothedVolume = nextVolume
                if (decision.shouldAffectPlayback) {
                    targetVolume = decision.targetVolume
                }

                withContext(Dispatchers.Main) {
                    topClasses = predictions
                    rawBucketState = bucketScoresToDisplayList(decision.rawBucketScores)
                    smoothBucketState = bucketScoresToDisplayList(decision.smoothedBucketScores)
                    activeBucket = decision.winner
                    bucketWinnerScore = if (decision.winner == MaskingBucket.UNKNOWN) {
                        0f
                    } else {
                        decision.winnerScore
                    }
                    bucketSecondScore = if (decision.runnerUpBucket == MaskingBucket.UNKNOWN) {
                        0f
                    } else {
                        decision.runnerUpScore
                    }
                    statusDetail = if (decision.shouldAffectPlayback) {
                        decision.reason
                    } else {
                        "Hold volume; low confidence."
                    }
                    status = "Decision: ${decision.displayWinner} (${(decision.winnerScore * 100f).toInt()}%), target ${(targetVolume * 100f).toInt()}%"
                }
            }
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                isRunning = false
                status = "YAMNet init/classification failed. Add app/src/main/assets/yamnet.tflite"
                topClasses = emptyList()
                rawBucketState = emptyList()
                smoothBucketState = emptyList()
                activeBucket = MaskingBucket.UNKNOWN
                bucketWinnerScore = 0f
                bucketSecondScore = 0f
                targetVolume = MASKING_BASELINE
                smoothedVolume = MASKING_BASELINE
                statusDetail = "YAMNet failed. Add app/src/main/assets/yamnet.tflite"
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

        Spacer(modifier = Modifier.height(16.dp))

        Text("Raw bucket scores")
        Spacer(modifier = Modifier.height(8.dp))
        rawBucketState.forEach { bucket ->
            Text("${bucket.first}: ${(bucket.second * 100f).toInt()}%")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Smoothed bucket scores")
        Spacer(modifier = Modifier.height(8.dp))
        smoothBucketState.forEach { bucket ->
            Text("${bucket.first}: ${(bucket.second * 100f).toInt()}%")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Masking decision: ${maskingBucketDisplayName(activeBucket)}")
        Text("Winner score: ${(bucketWinnerScore * 100f).toInt()}%")
        Text("Runner-up score: ${(bucketSecondScore * 100f).toInt()}%")
        Text("Target volume: ${(targetVolume * 100f).toInt()}%")
        Text("Slew volume: ${(smoothedVolume * 100f).toInt()}%")
        Text("Decision reason: $statusDetail")
        if (activeBucket != MaskingBucket.UNKNOWN) {
            Text(
                if (activeBucket == MaskingBucket.ALERT) {
                    "Action: Freeze/Reduce masking for safety."
                } else {
                    "Action: Move toward target with fast attack + slow release."
                }
            )
        } else {
            Text("Action: Hold current volume.")
        }
    }
}

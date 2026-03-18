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
import com.calmspace.masking.YamnetLabelBucketResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

private const val MASKING_BASELINE = 0.40f
private const val MASKING_MIN_VOLUME = 0.05f
private const val MASKING_MAX_VOLUME = 1.00f
private const val MAX_AUTOMATIC_INCREASE = 0.25f
private const val MAX_AUTOMATIC_DECREASE = 0.10f
private const val MIN_WINNER_SCORE = 0.05f
private const val MIN_WINNER_MARGIN = 0.010f

private const val SMOOTHING_WINDOW_MS = 2000L
private const val ATTACK_PER_MS = 0.0001f // 3% every 300ms
private const val RELEASE_PER_MS = 0.0000334f // 1% every 300ms

private val bucketTargetOffset = mapOf(
    MaskingBucket.VOICE to 0.20f,
    MaskingBucket.HOUSEHOLD to 0.12f,
    MaskingBucket.TRAFFIC to 0.15f,
    MaskingBucket.NATURE to 0.00f,
    MaskingBucket.ALERT to -0.05f,
    MaskingBucket.UNKNOWN to 0.00f
)

private val bucketDisplayNames = mapOf(
    MaskingBucket.VOICE to "Speech / Human Activity",
    MaskingBucket.HOUSEHOLD to "Household Noise",
    MaskingBucket.TRAFFIC to "Outdoor / Traffic",
    MaskingBucket.NATURE to "Natural Ambient",
    MaskingBucket.ALERT to "Safety / Alert",
    MaskingBucket.UNKNOWN to "Unknown / Low Confidence"
)

private data class BucketScores(
    val raw: FloatArray,
    val smoothed: FloatArray,
    val winner: MaskingBucket,
    val winnerScore: Float,
    val secondScore: Float,
    val secondBucket: MaskingBucket,
    val shouldAffectVolume: Boolean,
    val target: Float,
    val decisionReason: String
)

private fun labelBucket(label: String, resolver: YamnetLabelBucketResolver): MaskingBucket {
    return resolver.resolve(label)
}

private fun computeBucketAverages(
    history: List<Pair<Long, FloatArray>>
): FloatArray {
    val averages = FloatArray(MaskingBucket.values().size)
    if (history.isEmpty()) return averages

    for (frame in history) {
        for (i in averages.indices) {
            averages[i] += frame.second[i]
        }
    }

    for (i in averages.indices) {
        averages[i] /= history.size.toFloat()
    }

    return averages
}

private fun evaluateBuckets(
    bucketScores: FloatArray,
    nowMs: Long,
    history: MutableList<Pair<Long, FloatArray>>,
    currentTarget: Float
): BucketScores {
    val cutoffMs = nowMs - SMOOTHING_WINDOW_MS
    history.removeIf { it.first < cutoffMs }
    history.add(nowMs to bucketScores)

    val smoothed = computeBucketAverages(history)

    val mappedRanking = smoothed.indices
        .filter { it != MaskingBucket.UNKNOWN.ordinal }
        .sortedByDescending { smoothed[it] }

    if (mappedRanking.isEmpty()) {
        return BucketScores(
            raw = bucketScores,
            smoothed = smoothed,
            winner = MaskingBucket.UNKNOWN,
            winnerScore = 0f,
            secondScore = 0f,
            secondBucket = MaskingBucket.UNKNOWN,
            shouldAffectVolume = false,
            target = currentTarget,
            decisionReason = "No mapped bucket received mass in this window; holding."
        )
    }

    val best = mappedRanking[0]
    val second = mappedRanking.getOrElse(1) { mappedRanking[0] }
    val bestScore = smoothed[best]
    val secondScore = smoothed[second]
    val bestBucket = MaskingBucket.values()[best]
    val secondBucket = MaskingBucket.values()[second]
    val alertScore = smoothed[MaskingBucket.ALERT.ordinal]

    val winner: MaskingBucket
    val shouldAffectVolume: Boolean
    val decisionReason: String

    // Alert bucket has highest priority if detected confidently.
    val alertValid = alertScore >= MIN_WINNER_SCORE
    if (alertValid && alertScore >= bestScore - MIN_WINNER_MARGIN) {
        winner = MaskingBucket.ALERT
        shouldAffectVolume = true
        decisionReason = "Safety sound detected, reducing masking."
    } else if (bestScore >= MIN_WINNER_SCORE &&
        (mappedRanking.size == 1 || (bestScore - secondScore) >= MIN_WINNER_MARGIN)
    ) {
        winner = bestBucket
        shouldAffectVolume = true
        decisionReason = when (winner) {
            MaskingBucket.ALERT -> "Safety sound detected, reducing masking."
            MaskingBucket.VOICE -> "Speech detected, increasing masking quickly."
            MaskingBucket.TRAFFIC -> "Traffic detected, increasing masking."
            MaskingBucket.HOUSEHOLD -> "Household noise detected, moderate increase."
            MaskingBucket.NATURE -> "Nature-like sounds detected, keeping baseline."
            else -> "Holding at computed target."
        }
    } else {
        winner = MaskingBucket.UNKNOWN
        shouldAffectVolume = false
        decisionReason = "Confidence/margin not sufficient; holding volume."
    }

    val clampedOffset = bucketTargetOffset[winner] ?: 0f
    val targetFromBaseline = MASKING_BASELINE + clampedOffset
    val minLimit = (MASKING_BASELINE - MAX_AUTOMATIC_DECREASE).coerceAtLeast(MASKING_MIN_VOLUME)
    val maxLimit = (MASKING_BASELINE + MAX_AUTOMATIC_INCREASE).coerceAtMost(MASKING_MAX_VOLUME)
    val computedTarget = targetFromBaseline.coerceIn(minLimit, maxLimit)
    val target = if (shouldAffectVolume) computedTarget else currentTarget

    return BucketScores(
        raw = bucketScores,
        smoothed = smoothed,
        winner = winner,
        winnerScore = bestScore,
        secondScore = secondScore,
        secondBucket = secondBucket,
        shouldAffectVolume = shouldAffectVolume,
        target = target,
        decisionReason = decisionReason
    )
}

private fun bucketDisplay(scores: FloatArray): List<Pair<String, Float>> =
    MaskingBucket.values().mapIndexed { index, bucket ->
        val name = bucketDisplayNames[bucket] ?: bucket.name
        Pair(name, scores[index])
    }

private fun smoothVolume(current: Float, target: Float, deltaMs: Long): Float {
    if (deltaMs <= 0L) return current
    val rate = if (target > current) ATTACK_PER_MS else RELEASE_PER_MS
    val allowedDelta = rate * deltaMs.toFloat()
    return when {
        target > current -> (current + allowedDelta).coerceAtMost(target)
        target < current -> (current - allowedDelta).coerceAtLeast(target)
        else -> current
    }
}

@Composable
fun YamnetPocScreen(
    hasMicPermission: Boolean,
    onRequestMicrophonePermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val labelBucketResolver = remember { YamnetLabelBucketResolver.fromAssets(context.assets) }
    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var decisionReason by remember { mutableStateOf("Idle") }
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
            decisionReason = "Initializing..."
            topClasses = emptyList()
            rawBucketState = emptyList()
            smoothBucketState = emptyList()
            activeBucket = MaskingBucket.UNKNOWN
            bucketWinnerScore = 0f
            bucketSecondScore = 0f
            targetVolume = MASKING_BASELINE
            smoothedVolume = MASKING_BASELINE
        }

        var classifier: AudioClassifier? = null
        var audioRecord: AudioRecord? = null
        var lastVolumeUpdateMs = SystemClock.elapsedRealtime()
        val bucketHistory = mutableListOf<Pair<Long, FloatArray>>()

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
                val (predictions, bucketEval) = withContext(Dispatchers.Default) {
                    tensorAudio.load(audioRecord)
                    val results = classifier.classify(tensorAudio)
                    val topPredictions: List<Pair<String, Float>> = results
                        .flatMap { it.categories }
                        .sortedByDescending { it.score }
                        .take(5)
                        .map { category ->
                            Pair(category.label, category.score.toFloat())
                        }

                    val raw = FloatArray(MaskingBucket.values().size)
                    for ((label, score) in topPredictions) {
                        val bucket = labelBucket(label, labelBucketResolver)
                        raw[bucket.ordinal] += score
                    }

                    val mappedTotal = raw.sum()
                    raw[MaskingBucket.UNKNOWN.ordinal] += max(0f, 1f - mappedTotal)

                    val nowMs = SystemClock.elapsedRealtime()
                    val eval = evaluateBuckets(raw, nowMs, bucketHistory, targetVolume)
                    topPredictions to eval
                }

                val nowMs = SystemClock.elapsedRealtime()
                val nextVolume = smoothVolume(
                    current = smoothedVolume,
                    target = bucketEval.target,
                    deltaMs = nowMs - lastVolumeUpdateMs
                )
                lastVolumeUpdateMs = nowMs

                withContext(Dispatchers.Main) {
                    topClasses = predictions
                    rawBucketState = bucketDisplay(bucketEval.raw)
                    smoothBucketState = bucketDisplay(bucketEval.smoothed)
                    activeBucket = bucketEval.winner
                    bucketWinnerScore = if (bucketEval.winner == MaskingBucket.UNKNOWN) {
                        0f
                    } else {
                        bucketEval.winnerScore
                    }
                    bucketSecondScore = if (bucketEval.secondBucket == MaskingBucket.UNKNOWN) {
                        0f
                    } else {
                        bucketEval.secondScore
                    }
                    if (bucketEval.shouldAffectVolume) {
                        targetVolume = bucketEval.target
                    }
                    smoothedVolume = nextVolume
                    decisionReason = if (bucketEval.shouldAffectVolume) {
                        bucketEval.decisionReason
                    } else {
                        "Hold volume; low confidence."
                    }
                    status = "Decision: ${bucketDisplayNames[bucketEval.winner]} (${(bucketEval.winnerScore * 100f).toInt()}%), target ${(targetVolume * 100f).toInt()}%"
                }
                smoothedVolume = nextVolume
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
                decisionReason = "YAMNet failed. Add app/src/main/assets/yamnet.tflite"
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

        Text("Smoothed bucket scores (2s window)")
        Spacer(modifier = Modifier.height(8.dp))
        smoothBucketState.forEach { bucket ->
            Text("${bucket.first}: ${(bucket.second * 100f).toInt()}%")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Masking decision: ${bucketDisplayNames[activeBucket]}")
        Text("Winner score: ${(bucketWinnerScore * 100f).toInt()}%")
        Text("Runner-up score: ${(bucketSecondScore * 100f).toInt()}%")
        Text("Target volume: ${(targetVolume * 100f).toInt()}%")
        Text("Slew volume: ${(smoothedVolume * 100f).toInt()}%")
        Text("Decision reason: $decisionReason")
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

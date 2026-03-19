package com.calmspace.masking

import android.content.res.AssetManager
import java.util.Locale

private const val YAMNET_CLASS_MAP_ASSET = "yamnet_class_map.csv"
private const val YAMNET_CLASS_BUCKET_MAP_ASSET = "yamnet_label_bucket_map.csv"
private const val YAMNET_CLASS_MAP_HEADER_PREFIX = "index,"

class YamnetLabelBucketResolver private constructor(
    private val exactLabelMap: Map<String, MaskingBucket>,
    private val orderedBucketRules: List<Pair<MaskingBucket, List<String>>> = YamnetLabelBucketRules.orderedRulesFromSource()
) {
    fun resolve(label: String): MaskingBucket {
        val normalized = normalizeLabel(label)
        return exactLabelMap[normalized] ?: YamnetLabelBucketRules.classify(normalized, orderedBucketRules)
    }

    companion object {
        fun fromAssets(
            assetManager: AssetManager,
            classMapAssetFileName: String = YAMNET_CLASS_MAP_ASSET
        ): YamnetLabelBucketResolver {
            val rules = YamnetLabelBucketRules.orderedRulesFromSource()
            val map = mutableMapOf<String, MaskingBucket>()

            // Prefer explicit, per-label bucket assignments if available.
            map.putAll(loadExactBucketMap(assetManager, YAMNET_CLASS_BUCKET_MAP_ASSET))

            // Always ensure every class-map label has a deterministic bucket entry.
            // Missing entries are back-filled by the keyword/classic rules.
            val fallbackMap = loadClassMapBuckets(assetManager, classMapAssetFileName, rules)
            for ((normalizedLabel, bucket) in fallbackMap) {
                map.putIfAbsent(normalizedLabel, bucket)
            }

            return YamnetLabelBucketResolver(
                exactLabelMap = map,
                orderedBucketRules = rules
            )
        }
    }
}

private fun loadExactBucketMap(
    assetManager: AssetManager,
    assetFileName: String
): Map<String, MaskingBucket> {
    return try {
        assetManager.open(assetFileName).bufferedReader().use { reader ->
            val result = linkedMapOf<String, MaskingBucket>()
            for ((index, line) in reader.readLines().withIndex()) {
                val columns = parseCsvLine(line)
                if (columns.size < 2) continue

                if (index == 0 && columns.size >= 2 && columns[0].trim().equals("label", true)) {
                    continue
                }

                val normalizedLabel = normalizeLabel(columns[0])
                if (normalizedLabel.isBlank()) continue

                val bucket = parseBucket(columns[1]).let(::consolidateAlertBucket)
                result[normalizedLabel] = bucket
            }
            result
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun loadClassMapBuckets(
    assetManager: AssetManager,
    classMapAssetFileName: String,
    orderedBucketRules: List<Pair<MaskingBucket, List<String>>>
): Map<String, MaskingBucket> {
    return assetManager.open(classMapAssetFileName).bufferedReader().use { reader ->
        val result = linkedMapOf<String, MaskingBucket>()
        val lines = reader.readLines()
        for ((index, line) in lines.withIndex()) {
            if (index == 0 && line.startsWith(YAMNET_CLASS_MAP_HEADER_PREFIX)) {
                continue
            }
            val columns = parseCsvLine(line)
            if (columns.size < 3) continue

            val rawLabel = columns[2].trim()
            if (rawLabel.isBlank()) continue

            val normalized = normalizeLabel(rawLabel)
            val bucket = YamnetLabelBucketRules.classify(normalized, orderedBucketRules)
            result[normalized] = consolidateAlertBucket(bucket)
        }
        result
    }
}

private fun parseBucket(value: String): MaskingBucket {
    return runCatching {
        MaskingBucket.valueOf(value.trim().uppercase(Locale.ROOT))
    }.getOrNull() ?: MaskingBucket.UNKNOWN
}

private fun consolidateAlertBucket(bucket: MaskingBucket): MaskingBucket {
    return when (bucket) {
        MaskingBucket.ALERT -> MaskingBucket.TRAFFIC
        else -> bucket
    }
}

private fun parseCsvLine(line: String): List<String> {
    val columns = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val char = line[i]
        when {
            char == '"' -> {
                inQuotes = !inQuotes
            }
            char == ',' && !inQuotes -> {
                columns.add(current.toString())
                current.setLength(0)
            }
            else -> {
                current.append(char)
            }
        }
        i++
    }
    columns.add(current.toString())
    return columns
}

private fun normalizeLabel(label: String): String {
    return label.lowercase(Locale.ROOT)
        .replace("_", " ")
        .replace("-", " ")
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

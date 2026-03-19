package com.calmspace.masking

import android.content.res.AssetManager
import java.util.Locale

private const val YAMNET_CLASS_MAP_ASSET = "yamnet_class_map.csv"
private const val YAMNET_CLASS_BUCKET_MAP_ASSET = "yamnet_label_bucket_map.csv"
private const val YAMNET_CLASS_MAP_HEADER_PREFIX = "index,"

class YamnetLabelBucketResolver private constructor(
    private val exactLabelMap: Map<String, MaskingBucket>
) {
    fun resolve(label: String): MaskingBucket {
        val normalized = normalizeLabel(label)
        return exactLabelMap[normalized] ?: MaskingBucket.UNKNOWN
    }

    companion object {
        fun fromAssets(
            assetManager: AssetManager,
            classMapAssetFileName: String = YAMNET_CLASS_MAP_ASSET
        ): YamnetLabelBucketResolver {
            val map = mutableMapOf<String, MaskingBucket>()

            // Explicit per-label assignments from csv.
            map.putAll(loadExactBucketMap(assetManager, YAMNET_CLASS_BUCKET_MAP_ASSET))
            // Ensure full YAMNet class coverage with a safe default.
            loadClassMapForCompleteness(assetManager, classMapAssetFileName, map)

            return YamnetLabelBucketResolver(
                exactLabelMap = map
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

private fun loadClassMapForCompleteness(
    assetManager: AssetManager,
    classMapAssetFileName: String,
    target: MutableMap<String, MaskingBucket>
) {
    runCatching {
        assetManager.open(classMapAssetFileName).bufferedReader().use { reader ->
            for ((index, line) in reader.readLines().withIndex()) {
                if (index == 0 && line.startsWith(YAMNET_CLASS_MAP_HEADER_PREFIX)) continue

                val columns = parseCsvLine(line)
                if (columns.size < 3) continue

                val normalized = normalizeLabel(columns[2].trim())
                if (normalized.isBlank()) continue

                target.putIfAbsent(normalized, MaskingBucket.UNKNOWN)
            }
        }
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

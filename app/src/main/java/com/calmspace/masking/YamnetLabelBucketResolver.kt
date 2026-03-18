package com.calmspace.masking

import android.content.res.AssetManager
import java.util.Locale

private const val YAMNET_CLASS_MAP_ASSET = "yamnet_class_map.csv"
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
            assetFileName: String = YAMNET_CLASS_MAP_ASSET
        ): YamnetLabelBucketResolver {
            val rules = YamnetLabelBucketRules.orderedRulesFromSource()
            val map = mutableMapOf<String, MaskingBucket>()

            assetManager.open(assetFileName).bufferedReader().use { reader ->
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
                    val bucket = YamnetLabelBucketRules.classify(normalized, rules)
                    map[normalized] = bucket
                }
            }

            return YamnetLabelBucketResolver(
                exactLabelMap = map,
                orderedBucketRules = rules
            )
        }
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
            else -> current.append(char)
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

package com.calmspace.masking

import java.util.Locale

private val RULES_SOURCE = """
VOICE,speech
VOICE,conversation
VOICE,narration
VOICE,whisper
VOICE,whispering
VOICE,speaking
VOICE,spoken
VOICE,shout
VOICE,screaming
VOICE,scream
VOICE,shouting
VOICE,yelling
VOICE,chatter
VOICE,voice
VOICE,babble
VOICE,singing
VOICE,chuckle
VOICE,yodel
VOICE,chant
VOICE,groan
VOICE,rap
VOICE,narrative
VOICE,laughter
VOICE,laugh
VOICE,cry
VOICE,crying
VOICE,sobbing
VOICE,wail
VOICE,moan
VOICE,sigh
VOICE,whistling
VOICE,gasp
VOICE,sneeze
VOICE,cough
VOICE,snoring
VOICE,snore
VOICE,pant
VOICE,snort
VOICE,humming
VOICE,rapping
TRAFFIC,car
TRAFFIC,automobile
TRAFFIC,motor
TRAFFIC,truck
TRAFFIC,bus
TRAFFIC,motorcycle
TRAFFIC,motorbike
TRAFFIC,scooter
TRAFFIC,bike
TRAFFIC,vehicle
TRAFFIC,engine
TRAFFIC,traffic
TRAFFIC,train
TRAFFIC,rail
TRAFFIC,aircraft
TRAFFIC,helicopter
TRAFFIC,airplane
TRAFFIC,plane
TRAFFIC,road
TRAFFIC,horn
TRAFFIC,toot
TRAFFIC,car horn
TRAFFIC,truck horn
TRAFFIC,air horn
TRAFFIC,reversing
TRAFFIC,brake
TRAFFIC,steering
TRAFFIC,takeoff
TRAFFIC,tire
TRAFFIC,street
TRAFFIC,driving
TRAFFIC,highway
TRAFFIC,alarm
TRAFFIC,siren
TRAFFIC,fire alarm
TRAFFIC,smoke detector
TRAFFIC,car alarm
TRAFFIC,ambulance
TRAFFIC,fire engine
TRAFFIC,police
TRAFFIC,knock
TRAFFIC,doorbell
TRAFFIC,crash
TRAFFIC,gunshot
TRAFFIC,explosion
TRAFFIC,alert
TRAFFIC,bell
HOUSEHOLD,vacuum
HOUSEHOLD,blender
HOUSEHOLD,dryer
HOUSEHOLD,dishwasher
HOUSEHOLD,washing machine
HOUSEHOLD,microwave
HOUSEHOLD,air conditioner
HOUSEHOLD,aircon
HOUSEHOLD,fan
HOUSEHOLD,ceiling fan
HOUSEHOLD,printer
HOUSEHOLD,refrigerator
HOUSEHOLD,fridge
HOUSEHOLD,toaster
HOUSEHOLD,oven
HOUSEHOLD,coffee maker
HOUSEHOLD,hair dryer
HOUSEHOLD,toothbrush
HOUSEHOLD,water heater
HOUSEHOLD,washer
HOUSEHOLD,dish washer
HOUSEHOLD,toaster oven
HOUSEHOLD,heater
HOUSEHOLD,vacuum cleaner
HOUSEHOLD,drill
HOUSEHOLD,lawn mower
NATURE,bird
NATURE,birds
NATURE,chirp
NATURE,birdsong
NATURE,bird song
NATURE,wind
NATURE,rain
NATURE,water
NATURE,ocean
NATURE,stream
NATURE,river
NATURE,wave
NATURE,waves
NATURE,leaf
NATURE,rustling
NATURE,storm
NATURE,forest
NATURE,insect
NATURE,cricket
NATURE,frog
NATURE,thunder
NATURE,breeze
NATURE,waterfall
NATURE,brook
NATURE,dog
NATURE,cat
""".trimIndent()

internal object YamnetLabelBucketRules {
    internal fun orderedRulesFromSource(): List<Pair<MaskingBucket, List<String>>> {
        return parseBucketRuleLines(RULES_SOURCE)
    }

    internal fun classify(
        normalizedLabel: String,
        orderedBucketRules: List<Pair<MaskingBucket, List<String>>>
    ): MaskingBucket {
        val normalized = normalizedToken(normalizedLabel)
        for ((bucket, keywords) in orderedBucketRules) {
            if (keywords.any { it.isNotBlank() && normalized.contains(it.lowercase(Locale.ROOT)) }) {
                return bucket
            }
        }
        return MaskingBucket.UNKNOWN
    }

    internal fun normalizedLabel(label: String): String = label
        .lowercase(Locale.ROOT)
        .replace("_", " ")
        .replace("-", " ")

    internal fun parseBucketRuleLines(text: String): List<Pair<MaskingBucket, List<String>>> {
        val byBucket = LinkedHashMap<MaskingBucket, MutableSet<String>>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEach
            val split = line.split(",", limit = 2)
            if (split.size != 2) return@forEach

            val bucketName = split[0].trim().uppercase()
            val keyword = split[1].trim()
            if (bucketName == "BUCKET" || keyword.isBlank()) return@forEach

            val bucket = runCatching { MaskingBucket.valueOf(bucketName) }.getOrNull() ?: return@forEach
            val keywordSet = byBucket.getOrPut(bucket) { linkedSetOf() }
            keywordSet.add(normalizeLabelKeyword(keyword))
        }
        return orderedBucketRuleList(byBucket)
    }

    private fun orderedBucketRuleList(
        source: Map<MaskingBucket, Set<String>>
    ): List<Pair<MaskingBucket, List<String>>> = listOf(
        MaskingBucket.ALERT to (source[MaskingBucket.ALERT]?.toList() ?: emptyList()),
        MaskingBucket.VOICE to (source[MaskingBucket.VOICE]?.toList() ?: emptyList()),
        MaskingBucket.TRAFFIC to (source[MaskingBucket.TRAFFIC]?.toList() ?: emptyList()),
        MaskingBucket.HOUSEHOLD to (source[MaskingBucket.HOUSEHOLD]?.toList() ?: emptyList()),
        MaskingBucket.NATURE to (source[MaskingBucket.NATURE]?.toList() ?: emptyList())
    )

    private fun normalizedToken(value: String): String = value
        .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase(Locale.ROOT)

    private fun normalizeLabelKeyword(value: String): String =
        normalizedToken(value.lowercase(Locale.ROOT))
}

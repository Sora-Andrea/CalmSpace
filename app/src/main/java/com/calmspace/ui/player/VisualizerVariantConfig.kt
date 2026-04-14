package com.calmspace.ui.player

enum class VisualizerVariant {
    DEFAULT,
    ALTERNATIVE
}

object VisualizerVariantConfig {
    // switch for internal testing; Colt, please implement switch in settings page
    val activeVariant: VisualizerVariant = VisualizerVariant.DEFAULT
}

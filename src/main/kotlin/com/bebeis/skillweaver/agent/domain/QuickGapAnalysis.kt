package com.bebeis.skillweaver.agent.domain

data class QuickGapAnalysis(
    val hasSignificantGaps: Boolean,
    val identifiedGaps: List<String>,
    val strengthAreas: List<String>,
    val recommendedPreparation: String?
)

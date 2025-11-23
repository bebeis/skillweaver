package com.bebeis.skillweaver.agent.domain

data class DetailedGapAnalysis(
    val overallReadiness: String,
    val criticalGaps: List<GapDetail>,
    val minorGaps: List<String>,
    val strengths: List<String>,
    val preparationPlan: List<PreparationStep>,
    val estimatedPrepWeeks: Int
)

data class GapDetail(
    val area: String,
    val severity: String,
    val description: String,
    val recommendedAction: String
)

data class PreparationStep(
    val order: Int,
    val title: String,
    val estimatedHours: Int
)

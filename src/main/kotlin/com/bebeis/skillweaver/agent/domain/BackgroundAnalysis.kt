package com.bebeis.skillweaver.agent.domain

import java.time.LocalDate

data class BackgroundAnalysis(
    val existingRelevantSkills: List<String> = emptyList(),
    val knowledgeGaps: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val riskFactors: List<String> = emptyList(),
    val rawText: String? = null
)

data class DailyScheduleItem(
    val dayIndex: Int,
    val date: LocalDate,
    val allocatedMinutes: Int,
    val stepRef: Int?,
    val tasks: List<String> = emptyList()
)

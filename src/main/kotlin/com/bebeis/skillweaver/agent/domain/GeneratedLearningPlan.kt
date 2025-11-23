package com.bebeis.skillweaver.agent.domain

import java.time.LocalDate

data class GeneratedLearningPlan(
    val memberId: Long,
    val targetTechnologyKey: String,
    val targetTechnologyName: String,
    val title: String,
    val description: String,
    val totalEstimatedHours: Int,
    val startDate: LocalDate,
    val targetEndDate: LocalDate,
    val steps: List<GeneratedStep>,
    val metadata: PlanMetadata
)

data class GeneratedStep(
    val order: Int,
    val title: String,
    val description: String,
    val estimatedHours: Int,
    val keyTopics: List<String>,
    val resources: List<String> = emptyList()
)

data class PlanMetadata(
    val generatedPath: String, // "QUICK", "STANDARD", "DETAILED"
    val llmModel: String, // "GPT-4o-mini", "GPT-4o"
    val estimatedCost: Double,
    val generationTimeSeconds: Int,
    val analysisDepth: String, // "SIMPLE", "STANDARD", "DEEP"
    val gapAnalysisPerformed: Boolean,
    val resourcesEnriched: Boolean
)

package com.bebeis.skillweaver.agent.domain

/**
 * LLM가 각 단계별로 깊이를 선택하는 계획
 *
 * analysisMode: quick | standard | detailed | skip
 * gapMode:      none | quick | detailed | skip
 * curriculumMode: quick | standard | detailed | skip
 * resourceMode: quick | standard | detailed | skip (detailed만 실제 리소스 강화 액션이 존재)
 */
data class DepthPlan(
    val analysisMode: String,
    val gapMode: String,
    val curriculumMode: String,
    val resourceMode: String = "skip",
    val allowHybrid: Boolean = false,
    val hybridMix: List<String> = emptyList(),
    val rationale: String? = null
)

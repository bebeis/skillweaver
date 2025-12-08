package com.bebeis.skillweaver.agent.event

import org.springframework.context.ApplicationEvent

/**
 * Agent 실행 중 진행률 이벤트
 * Agent 내부에서 발행하고 AgentStreamController에서 수신하여 SSE로 전송
 */
data class AgentProgressEvent(
    val processId: String,
    val stage: ProgressStage,
    val message: String,
    val detail: String? = null,
    val progressPercent: Int? = null,
    val stepIndex: Int? = null,
    val totalSteps: Int? = null
) : ApplicationEvent(processId)

enum class ProgressStage {
    ANALYSIS_STARTED,
    DEEP_ANALYSIS,
    GAP_ANALYSIS,
    CURRICULUM_GENERATION,
    RESOURCE_ENRICHMENT,
    RESOURCE_STEP_PROGRESS,
    FINALIZING
}

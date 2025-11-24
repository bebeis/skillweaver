package com.bebeis.skillweaver.api.agent.dto

import com.bebeis.skillweaver.core.domain.agent.AgentRun
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.domain.agent.SseEventType
import java.time.LocalDateTime

data class AgentRunResponse(
    val agentRunId: Long,
    val memberId: Long,
    val agentType: AgentType,
    val status: AgentRunStatus,
    val parameters: String?,
    val result: String?,
    val learningPlanId: Long?,
    val errorMessage: String?,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val executionTimeMs: Long?,
    val estimatedCost: Double?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(agentRun: AgentRun): AgentRunResponse {
            return AgentRunResponse(
                agentRunId = agentRun.agentRunId!!,
                memberId = agentRun.memberId,
                agentType = agentRun.agentType,
                status = agentRun.status,
                parameters = agentRun.parameters,
                result = agentRun.result,
                learningPlanId = agentRun.learningPlanId,
                errorMessage = agentRun.errorMessage,
                startedAt = agentRun.startedAt,
                completedAt = agentRun.completedAt,
                executionTimeMs = agentRun.executionTimeMs,
                estimatedCost = agentRun.estimatedCost,
                createdAt = agentRun.createdAt,
                updatedAt = agentRun.updatedAt
            )
        }
    }
}

data class CreateAgentRunRequest(
    val agentType: AgentType,
    val parameters: String?
)

data class AgentRunListResponse(
    val runs: List<AgentRunResponse>,
    val total: Int
) {
    companion object {
        fun from(runs: List<AgentRun>): AgentRunListResponse {
            return AgentRunListResponse(
                runs = runs.map { AgentRunResponse.from(it) },
                total = runs.size
            )
        }
    }
}

data class AgentEventDto(
    val type: SseEventType,
    val agentRunId: Long? = null,
    val actionName: String? = null,
    val message: String? = null,
    val state: String? = null,
    val result: Any? = null,
    val timestamp: Long
)

package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.api.agent.dto.AgentRunListResponse
import com.bebeis.skillweaver.api.agent.dto.AgentRunResponse
import com.bebeis.skillweaver.api.agent.dto.CreateAgentRunRequest
import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.common.auth.AuthUser
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.service.agent.AgentRunService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/agent-runs")
class AgentRunController(
    private val agentRunService: AgentRunService,
    private val agentEventBroadcaster: AgentEventBroadcaster
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAgentRun(
        @AuthUser authMemberId: Long,
        @RequestParam memberId: Long,
        @RequestBody request: CreateAgentRunRequest
    ): ApiResponse<AgentRunResponse> {
        require(authMemberId == memberId) { "본인의 실행만 생성할 수 있습니다" }
        val agentRun = agentRunService.createRun(
            memberId = memberId,
            agentType = request.agentType,
            parameters = request.parameters
        )
        return ApiResponse.success(
            data = AgentRunResponse.from(agentRun),
            message = "Agent run created successfully"
        )
    }

    @GetMapping("/{agentRunId}")
    fun getAgentRun(
        @AuthUser authMemberId: Long,
        @PathVariable agentRunId: Long,
        @RequestParam memberId: Long
    ): ApiResponse<AgentRunResponse> {
        require(authMemberId == memberId) { "본인의 실행만 조회할 수 있습니다" }
        val agentRun = agentRunService.getRun(memberId, agentRunId)
        return ApiResponse.success(
            data = AgentRunResponse.from(agentRun),
            message = "Agent run retrieved successfully"
        )
    }

    @GetMapping
    fun getAgentRuns(
        @AuthUser authMemberId: Long,
        @RequestParam memberId: Long,
        @RequestParam(required = false) status: AgentRunStatus?
    ): ApiResponse<AgentRunListResponse> {
        require(authMemberId == memberId) { "본인의 실행만 조회할 수 있습니다" }
        val agentRuns = agentRunService.getRunsByMember(memberId, status)
        return ApiResponse.success(
            data = AgentRunListResponse.from(agentRuns),
            message = "Agent runs retrieved successfully"
        )
    }

    @PostMapping("/{agentRunId}/start")
    fun startAgentRun(
        @PathVariable agentRunId: Long
    ): ApiResponse<AgentRunResponse> {
        val agentRun = agentRunService.startRun(agentRunId)
        return ApiResponse.success(
            data = AgentRunResponse.from(agentRun),
            message = "Agent run started successfully"
        )
    }

    @PostMapping("/{agentRunId}/complete")
    fun completeAgentRun(
        @PathVariable agentRunId: Long,
        @RequestParam(required = false) result: String?,
        @RequestParam(required = false) learningPlanId: Long?,
        @RequestParam(required = false) cost: Double?,
        @RequestParam(required = false) executionTimeMs: Long?
    ): ApiResponse<AgentRunResponse> {
        val agentRun = agentRunService.completeRun(
            agentRunId = agentRunId,
            result = result,
            learningPlanId = learningPlanId,
            cost = cost,
            executionTimeMs = executionTimeMs
        )
        return ApiResponse.success(
            data = AgentRunResponse.from(agentRun),
            message = "Agent run completed successfully"
        )
    }

    @PostMapping("/{agentRunId}/fail")
    fun failAgentRun(
        @PathVariable agentRunId: Long,
        @RequestParam errorMessage: String
    ): ApiResponse<AgentRunResponse> {
        val agentRun = agentRunService.failRun(agentRunId, errorMessage)
        return ApiResponse.success(
            data = AgentRunResponse.from(agentRun),
            message = "Agent run failed"
        )
    }

    @GetMapping("/{agentRunId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeEvents(
        @PathVariable agentRunId: Long
    ): SseEmitter {
        agentRunService.getRunById(agentRunId)
        return agentEventBroadcaster.subscribe(agentRunId)
    }
}

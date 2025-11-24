package com.bebeis.skillweaver.core.service.agent

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.core.domain.agent.AgentRun
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import com.bebeis.skillweaver.core.storage.agent.AgentRunRepository
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AgentRunService(
    private val agentRunRepository: AgentRunRepository,
    private val memberRepository: MemberRepository
) {
    private val logger = LoggerFactory.getLogger(AgentRunService::class.java)

    @Transactional
    fun createRun(memberId: Long, agentType: AgentType, parameters: String?): AgentRun {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val agentRun = AgentRun(
            memberId = memberId,
            agentType = agentType,
            status = AgentRunStatus.PENDING,
            parameters = parameters
        )

        val saved = agentRunRepository.save(agentRun)
        logger.info("Agent run created: ${saved.agentRunId} for member: $memberId")
        return saved
    }

    @Transactional
    fun startRun(agentRunId: Long): AgentRun {
        val agentRun = agentRunRepository.findById(agentRunId)
            .orElseThrow { IllegalArgumentException("AgentRun not found: $agentRunId") }

        agentRun.start()
        logger.info("Agent run started: $agentRunId")
        return agentRun
    }

    @Transactional
    fun completeRun(
        agentRunId: Long,
        result: String?,
        learningPlanId: Long?,
        cost: Double?,
        executionTimeMs: Long?
    ): AgentRun {
        val agentRun = agentRunRepository.findById(agentRunId)
            .orElseThrow { IllegalArgumentException("AgentRun not found: $agentRunId") }

        agentRun.complete(result, learningPlanId, cost, executionTimeMs)
        logger.info("Agent run completed: $agentRunId, plan: $learningPlanId")
        return agentRun
    }

    @Transactional
    fun failRun(agentRunId: Long, errorMessage: String): AgentRun {
        val agentRun = agentRunRepository.findById(agentRunId)
            .orElseThrow { IllegalArgumentException("AgentRun not found: $agentRunId") }

        agentRun.fail(errorMessage)
        logger.error("Agent run failed: $agentRunId, error: $errorMessage")
        return agentRun
    }

    fun getRun(memberId: Long, agentRunId: Long): AgentRun {
        val agentRun = agentRunRepository.findById(agentRunId)
            .orElseThrow { IllegalArgumentException("AgentRun not found: $agentRunId") }

        if (agentRun.memberId != memberId) {
            throw IllegalArgumentException("Agent run does not belong to member: $memberId")
        }

        return agentRun
    }

    fun getRunsByMember(memberId: Long, status: AgentRunStatus?): List<AgentRun> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return if (status != null) {
            agentRunRepository.findByMemberIdAndStatus(memberId, status)
        } else {
            agentRunRepository.findByMemberId(memberId)
        }
    }

    fun getRunsByMemberAndType(memberId: Long, agentType: AgentType): List<AgentRun> {
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return agentRunRepository.findByMemberIdAndAgentType(memberId, agentType)
    }

    fun getRunByLearningPlan(learningPlanId: Long): AgentRun? {
        return agentRunRepository.findByLearningPlanId(learningPlanId)
    }
}

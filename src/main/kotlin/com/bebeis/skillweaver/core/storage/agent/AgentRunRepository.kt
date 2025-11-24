package com.bebeis.skillweaver.core.storage.agent

import com.bebeis.skillweaver.core.domain.agent.AgentRun
import com.bebeis.skillweaver.core.domain.agent.AgentRunStatus
import com.bebeis.skillweaver.core.domain.agent.AgentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AgentRunRepository : JpaRepository<AgentRun, Long> {
    fun findByMemberId(memberId: Long): List<AgentRun>
    fun findByMemberIdAndStatus(memberId: Long, status: AgentRunStatus): List<AgentRun>
    fun findByMemberIdAndAgentType(memberId: Long, agentType: AgentType): List<AgentRun>
    fun findByLearningPlanId(learningPlanId: Long): AgentRun?
}

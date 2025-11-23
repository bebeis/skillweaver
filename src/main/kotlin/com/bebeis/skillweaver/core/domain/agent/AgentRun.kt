package com.bebeis.skillweaver.core.domain.agent

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "agent_run")
class AgentRun(
    @Id
    @Column(name = "run_id", nullable = false, length = 100)
    val runId: String,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    val agentType: AgentType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: AgentRunStatus = AgentRunStatus.RUNNING,

    @Lob
    @Column(columnDefinition = "TEXT")
    val parameters: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    val result: String? = null,

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "started_at", nullable = false, updatable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null
) {
    init {
        require(runId.isNotBlank()) { "runId는 비어있을 수 없습니다." }
    }

    fun complete(result: String): AgentRun {
        return AgentRun(
            runId = this.runId,
            memberId = this.memberId,
            agentType = this.agentType,
            status = AgentRunStatus.COMPLETED,
            parameters = this.parameters,
            result = result,
            errorMessage = null,
            startedAt = this.startedAt,
            completedAt = LocalDateTime.now()
        )
    }

    fun fail(errorMessage: String): AgentRun {
        return AgentRun(
            runId = this.runId,
            memberId = this.memberId,
            agentType = this.agentType,
            status = AgentRunStatus.FAILED,
            parameters = this.parameters,
            result = null,
            errorMessage = errorMessage,
            startedAt = this.startedAt,
            completedAt = LocalDateTime.now()
        )
    }
}

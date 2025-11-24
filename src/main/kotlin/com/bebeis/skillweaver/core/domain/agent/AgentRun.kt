package com.bebeis.skillweaver.core.domain.agent

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "agent_run")
class AgentRun(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_run_id")
    val agentRunId: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    val agentType: AgentType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AgentRunStatus = AgentRunStatus.PENDING,

    @Lob
    @Column(columnDefinition = "TEXT")
    val parameters: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    var result: String? = null,

    @Column(name = "learning_plan_id")
    var learningPlanId: Long? = null,

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(name = "execution_time_ms")
    var executionTimeMs: Long? = null,

    @Column(name = "estimated_cost")
    var estimatedCost: Double? = null
) : BaseEntity() {

    fun start() {
        require(status == AgentRunStatus.PENDING) { "PENDING 상태에서만 시작할 수 있습니다." }
        status = AgentRunStatus.RUNNING
        startedAt = LocalDateTime.now()
    }

    fun complete(result: String?, planId: Long?, cost: Double?, timeMs: Long?) {
        require(status == AgentRunStatus.RUNNING) { "RUNNING 상태에서만 완료할 수 있습니다." }
        status = AgentRunStatus.COMPLETED
        this.result = result
        this.learningPlanId = planId
        this.estimatedCost = cost
        this.executionTimeMs = timeMs
        completedAt = LocalDateTime.now()
    }

    fun fail(error: String) {
        require(status == AgentRunStatus.RUNNING || status == AgentRunStatus.PENDING) {
            "RUNNING 또는 PENDING 상태에서만 실패 처리할 수 있습니다."
        }
        status = AgentRunStatus.FAILED
        errorMessage = error
        completedAt = LocalDateTime.now()
        executionTimeMs = startedAt?.let {
            java.time.Duration.between(it, LocalDateTime.now()).toMillis()
        }
    }

    fun isCompleted(): Boolean = status == AgentRunStatus.COMPLETED
    fun isFailed(): Boolean = status == AgentRunStatus.FAILED
    fun isRunning(): Boolean = status == AgentRunStatus.RUNNING
    fun isPending(): Boolean = status == AgentRunStatus.PENDING
}

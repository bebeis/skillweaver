package com.bebeis.skillweaver.agent.domain

data class LearningRequest(
    val memberId: Long,
    val targetTechnologyKey: String,
    val agentRunId: Long? = null
)

package com.bebeis.skillweaver.agent.domain

data class NoGapAnalysis(
    val skipped: Boolean = true,
    val reason: String = "Experienced developer - self-assessment capable"
)

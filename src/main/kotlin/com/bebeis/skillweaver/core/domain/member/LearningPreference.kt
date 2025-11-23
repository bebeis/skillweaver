package com.bebeis.skillweaver.core.domain.member

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class LearningPreference(
    val dailyMinutes: Int = 60,
    val preferKorean: Boolean = true,
    @Enumerated(EnumType.STRING)
    val learningStyle: LearningStyle = LearningStyle.PROJECT_BASED,
    val weekendBoost: Boolean = true
)

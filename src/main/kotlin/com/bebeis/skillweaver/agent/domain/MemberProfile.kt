package com.bebeis.skillweaver.agent.domain

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.TargetTrack

data class MemberProfile(
    val memberId: Long,
    val name: String,
    val targetTrack: TargetTrack,
    val experienceLevel: ExperienceLevel,
    val learningPreference: LearningPreference,
    val currentSkillCount: Int,
    val weeklyCapacityMinutes: Int
)

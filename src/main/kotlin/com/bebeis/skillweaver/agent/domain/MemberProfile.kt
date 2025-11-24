package com.bebeis.skillweaver.agent.domain

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import org.springframework.ai.tool.annotation.Tool

data class MemberProfile(
    val memberId: Long,
    val name: String,
    val targetTrack: TargetTrack,
    val experienceLevel: ExperienceLevel,
    val learningPreference: LearningPreference,
    val currentSkillCount: Int,
    val weeklyCapacityMinutes: Int
) {
    @Tool(description = "Get member's weekly learning capacity in hours")
    fun getWeeklyHoursCapacity(): Double = weeklyCapacityMinutes / 60.0

    @Tool(description = "Check if member prefers Korean language resources")
    fun prefersKoreanContent(): Boolean = learningPreference.preferKorean

    @Tool(description = "Get member's preferred learning style")
    fun getPreferredLearningStyle(): String = learningPreference.learningStyle.name

    @Tool(description = "Check if member can boost learning on weekends")
    fun hasWeekendAvailability(): Boolean = learningPreference.weekendBoost

    @Tool(description = "Get daily learning capacity in hours")
    fun getDailyHours(): Double = learningPreference.dailyMinutes / 60.0

    @Tool(description = "Check if member is experienced (has more than 5 skills)")
    fun isExperiencedMember(): Boolean = currentSkillCount > 5

    @Tool(description = "Check if member is a beginner level")
    fun isBeginnerLevel(): Boolean = experienceLevel == ExperienceLevel.BEGINNER

    @Tool(description = "Check if member is an advanced level")
    fun isAdvancedLevel(): Boolean = experienceLevel == ExperienceLevel.ADVANCED
}

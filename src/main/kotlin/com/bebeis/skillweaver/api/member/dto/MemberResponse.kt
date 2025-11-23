package com.bebeis.skillweaver.api.member.dto

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.LearningStyle
import com.bebeis.skillweaver.core.domain.member.Member
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import java.time.LocalDateTime

data class MemberResponse(
    val memberId: Long,
    val name: String,
    val email: String,
    val targetTrack: TargetTrack,
    val experienceLevel: ExperienceLevel,
    val learningPreference: LearningPreferenceResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                memberId = member.memberId!!,
                name = member.name,
                email = member.email,
                targetTrack = member.targetTrack,
                experienceLevel = member.experienceLevel,
                learningPreference = LearningPreferenceResponse.from(member.learningPreference),
                createdAt = member.createdAt,
                updatedAt = member.updatedAt
            )
        }
    }
}

data class LearningPreferenceResponse(
    val dailyMinutes: Int,
    val preferKorean: Boolean,
    val learningStyle: LearningStyle,
    val weekendBoost: Boolean
) {
    companion object {
        fun from(preference: LearningPreference): LearningPreferenceResponse {
            return LearningPreferenceResponse(
                dailyMinutes = preference.dailyMinutes,
                preferKorean = preference.preferKorean,
                learningStyle = preference.learningStyle,
                weekendBoost = preference.weekendBoost
            )
        }
    }
}

data class LoginResponse(
    val accessToken: String,
    val memberId: Long,
    val name: String,
    val email: String
)

data class RefreshTokenResponse(
    val accessToken: String
)

data class SignupResponse(
    val memberId: Long,
    val name: String,
    val email: String,
    val targetTrack: TargetTrack,
    val experienceLevel: ExperienceLevel,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): SignupResponse {
            return SignupResponse(
                memberId = member.memberId!!,
                name = member.name,
                email = member.email,
                targetTrack = member.targetTrack,
                experienceLevel = member.experienceLevel,
                createdAt = member.createdAt
            )
        }
    }
}

package com.bebeis.skillweaver.api.member.dto

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.LearningStyle
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class SignupRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,
    
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
    
    @field:NotNull(message = "목표 트랙은 필수입니다")
    val targetTrack: TargetTrack,
    
    @field:NotNull(message = "경험 수준은 필수입니다")
    val experienceLevel: ExperienceLevel,
    
    @field:Valid
    val learningPreference: LearningPreferenceRequest = LearningPreferenceRequest()
)

data class LearningPreferenceRequest(
    @field:Positive(message = "일일 학습 시간은 양수여야 합니다")
    val dailyMinutes: Int = 60,
    
    val preferKorean: Boolean = true,
    
    @field:NotNull(message = "학습 스타일은 필수입니다")
    val learningStyle: LearningStyle = LearningStyle.PROJECT_BASED,
    
    val weekendBoost: Boolean = true
) {
    fun toDomain(): LearningPreference {
        return LearningPreference(
            dailyMinutes = dailyMinutes,
            preferKorean = preferKorean,
            learningStyle = learningStyle,
            weekendBoost = weekendBoost
        )
    }
}

data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

data class UpdateMemberRequest(
    val name: String?,
    val targetTrack: TargetTrack?,
    val experienceLevel: ExperienceLevel?,
    @field:Valid
    val learningPreference: LearningPreferenceRequest?
)

package com.bebeis.skillweaver.api.member.dto

import com.bebeis.skillweaver.core.domain.member.skill.MemberSkill
import com.bebeis.skillweaver.core.domain.member.skill.SkillLevel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 회원 스킬 응답 DTO
 */
data class MemberSkillResponse(
    val memberSkillId: Long,
    val memberId: Long,
    val technologyId: Long?,
    val customName: String?,
    val skillName: String,
    val level: SkillLevel,
    val yearsOfUse: Double,
    val lastUsedAt: LocalDate?,
    val note: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(memberSkill: MemberSkill): MemberSkillResponse {
            val skillName = memberSkill.customName ?: "Technology#${memberSkill.technologyId}"
            
            return MemberSkillResponse(
                memberSkillId = memberSkill.memberSkillId!!,
                memberId = memberSkill.memberId,
                technologyId = memberSkill.technologyId,
                customName = memberSkill.customName,
                skillName = skillName,
                level = memberSkill.level,
                yearsOfUse = memberSkill.yearsOfUse,
                lastUsedAt = memberSkill.lastUsedAt,
                note = memberSkill.note,
                createdAt = memberSkill.createdAt,
                updatedAt = memberSkill.updatedAt
            )
        }
    }
}

/**
 * 회원 스킬 추가 요청 DTO
 */
data class AddMemberSkillRequest(
    val technologyId: Long?,
    val customName: String?,
    val level: SkillLevel,
    val yearsOfUse: Double = 0.0,
    val lastUsedAt: LocalDate? = null,
    val note: String? = null
) {
    init {
        require(technologyId != null || !customName.isNullOrBlank()) {
            "technologyId 또는 customName 중 하나는 반드시 제공되어야 합니다."
        }
    }
}

/**
 * 회원 스킬 수정 요청 DTO
 */
data class UpdateMemberSkillRequest(
    val level: SkillLevel?,
    val yearsOfUse: Double?,
    val lastUsedAt: LocalDate?,
    val note: String?
)

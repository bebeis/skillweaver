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
    val technologyName: String?,      // V4: Neo4j Technology name
    val customName: String?,
    val level: SkillLevel,
    val yearsOfUse: Double,
    val lastUsedAt: LocalDate?,
    val note: String?,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(memberSkill: MemberSkill): MemberSkillResponse {
            return MemberSkillResponse(
                memberSkillId = memberSkill.memberSkillId!!,
                technologyName = memberSkill.technologyName,
                customName = memberSkill.customName,
                level = memberSkill.level,
                yearsOfUse = memberSkill.yearsOfUse,
                lastUsedAt = memberSkill.lastUsedAt,
                note = memberSkill.note,
                updatedAt = memberSkill.updatedAt
            )
        }
    }
}

data class MemberSkillListResponse(
    val skills: List<MemberSkillResponse>,
    val totalCount: Int
)

/**
 * 회원 스킬 추가 요청 DTO
 */
data class AddMemberSkillRequest(
    val technologyName: String?,      // V4: Neo4j Technology name (예: "spring-boot")
    val customName: String?,
    val level: SkillLevel,
    val yearsOfUse: Double = 0.0,
    val lastUsedAt: LocalDate? = null,
    val note: String? = null
) {
    init {
        require(!technologyName.isNullOrBlank() || !customName.isNullOrBlank()) {
            "technologyName 또는 customName 중 하나는 반드시 제공되어야 합니다."
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

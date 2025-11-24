package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.conflict
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.member.dto.AddMemberSkillRequest
import com.bebeis.skillweaver.api.member.dto.MemberSkillResponse
import com.bebeis.skillweaver.api.member.dto.UpdateMemberSkillRequest
import com.bebeis.skillweaver.core.domain.member.skill.MemberSkill
import com.bebeis.skillweaver.core.domain.member.skill.SkillLevel
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.bebeis.skillweaver.core.storage.member.MemberSkillRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberSkillService(
    private val memberSkillRepository: MemberSkillRepository,
    private val memberRepository: MemberRepository,
    private val technologyRepository: TechnologyRepository
) {
    private val logger = LoggerFactory.getLogger(MemberSkillService::class.java)

    @Transactional
    fun addMemberSkill(memberId: Long, request: AddMemberSkillRequest): MemberSkillResponse {
        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        // 기술이 지정된 경우 존재 확인
        request.technologyId?.let { techId ->
            if (!technologyRepository.existsById(techId)) {
                notFound(ErrorCode.TECHNOLOGY_NOT_FOUND)
            }

            // 이미 등록된 스킬인지 확인
            memberSkillRepository.findByMemberIdAndTechnologyId(memberId, techId)?.let {
                conflict(ErrorCode.MEMBER_SKILL_ALREADY_EXISTS.message)
            }
        }

        val memberSkill = MemberSkill(
            memberId = memberId,
            technologyId = request.technologyId,
            customName = request.customName,
            level = request.level,
            yearsOfUse = request.yearsOfUse,
            lastUsedAt = request.lastUsedAt,
            note = request.note
        )

        val saved = memberSkillRepository.save(memberSkill)
        logger.info("Member skill added: ${saved.memberSkillId}")
        return MemberSkillResponse.from(saved)
    }

    fun getMemberSkills(
        memberId: Long, 
        category: TechnologyCategory? = null, 
        level: SkillLevel? = null
    ): List<MemberSkillResponse> {
        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        // Repository에서 Technology와 JOIN하여 필터링
        return memberSkillRepository.findByMemberIdWithFilters(memberId, category, level)
            .map { MemberSkillResponse.from(it) }
    }

    fun getMemberSkillsByLevel(memberId: Long, level: SkillLevel): List<MemberSkillResponse> {
        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return memberSkillRepository.findByMemberIdAndLevel(memberId, level)
            .map { MemberSkillResponse.from(it) }
    }

    fun getMemberSkill(memberId: Long, memberSkillId: Long): MemberSkillResponse {
        val memberSkill = memberSkillRepository.findByMemberIdAndMemberSkillId(memberId, memberSkillId)
            ?: notFound(ErrorCode.MEMBER_SKILL_NOT_FOUND)

        return MemberSkillResponse.from(memberSkill)
    }

    @Transactional
    fun updateMemberSkill(
        memberId: Long,
        memberSkillId: Long,
        request: UpdateMemberSkillRequest
    ): MemberSkillResponse {
        val memberSkill = memberSkillRepository.findByMemberIdAndMemberSkillId(memberId, memberSkillId)
            ?: notFound(ErrorCode.MEMBER_SKILL_NOT_FOUND)

        val updated = MemberSkill(
            memberSkillId = memberSkill.memberSkillId,
            memberId = memberSkill.memberId,
            technologyId = memberSkill.technologyId,
            customName = memberSkill.customName,
            level = request.level ?: memberSkill.level,
            yearsOfUse = request.yearsOfUse ?: memberSkill.yearsOfUse,
            lastUsedAt = request.lastUsedAt ?: memberSkill.lastUsedAt,
            note = request.note ?: memberSkill.note
        )

        val saved = memberSkillRepository.save(updated)
        logger.info("Member skill updated: ${saved.memberSkillId}")
        return MemberSkillResponse.from(saved)
    }

    @Transactional
    fun deleteMemberSkill(memberId: Long, memberSkillId: Long) {
        val memberSkill = memberSkillRepository.findByMemberIdAndMemberSkillId(memberId, memberSkillId)
            ?: notFound(ErrorCode.MEMBER_SKILL_NOT_FOUND)

        memberSkillRepository.delete(memberSkill)
        logger.info("Member skill deleted: $memberSkillId")
    }
}

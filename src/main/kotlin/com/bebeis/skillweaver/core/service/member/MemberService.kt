package com.bebeis.skillweaver.core.service.member

import com.bebeis.skillweaver.api.common.exception.ErrorCode
import com.bebeis.skillweaver.api.common.exception.notFound
import com.bebeis.skillweaver.api.member.dto.MemberResponse
import com.bebeis.skillweaver.api.member.dto.UpdateMemberRequest
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository
) {
    private val logger = LoggerFactory.getLogger(MemberService::class.java)

    fun getMember(memberId: Long): MemberResponse {
        val member = memberRepository.findById(memberId).orElse(null) ?: run {
            logger.warn("Member not found: $memberId")
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        return MemberResponse.from(member)
    }

    @Transactional
    fun updateMember(memberId: Long, request: UpdateMemberRequest): MemberResponse {
        val member = memberRepository.findById(memberId).orElse(null) ?: run {
            logger.warn("Member not found for update: $memberId")
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        val updatedMember = member.update(
            name = request.name,
            targetTrack = request.targetTrack,
            experienceLevel = request.experienceLevel,
            learningPreference = request.learningPreference?.toDomain()
        )

        val savedMember = memberRepository.save(updatedMember)
        logger.info("Member updated successfully: $memberId")

        return MemberResponse.from(savedMember)
    }

    @Transactional
    fun deleteMember(memberId: Long) {
        if (!memberRepository.existsById(memberId)) {
            logger.warn("Member not found for deletion: $memberId")
            notFound(ErrorCode.MEMBER_NOT_FOUND)
        }

        memberRepository.deleteById(memberId)
        logger.info("Member deleted successfully: $memberId")
    }
}

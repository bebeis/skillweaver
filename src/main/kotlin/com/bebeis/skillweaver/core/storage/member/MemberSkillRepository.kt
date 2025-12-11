package com.bebeis.skillweaver.core.storage.member

import com.bebeis.skillweaver.core.domain.member.skill.MemberSkill
import com.bebeis.skillweaver.core.domain.member.skill.SkillLevel
import org.springframework.data.jpa.repository.JpaRepository

interface MemberSkillRepository : JpaRepository<MemberSkill, Long> {
    fun findByMemberId(memberId: Long): List<MemberSkill>
    fun findByMemberIdAndMemberSkillId(memberId: Long, memberSkillId: Long): MemberSkill?
    fun findByMemberIdAndTechnologyName(memberId: Long, technologyName: String): MemberSkill?
    fun findByMemberIdAndLevel(memberId: Long, level: SkillLevel): List<MemberSkill>
}

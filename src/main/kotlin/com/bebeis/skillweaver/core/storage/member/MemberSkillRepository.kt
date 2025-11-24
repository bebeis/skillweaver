package com.bebeis.skillweaver.core.storage.member

import com.bebeis.skillweaver.core.domain.member.skill.MemberSkill
import com.bebeis.skillweaver.core.domain.member.skill.SkillLevel
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MemberSkillRepository : JpaRepository<MemberSkill, Long> {
    fun findByMemberId(memberId: Long): List<MemberSkill>
    fun findByMemberIdAndMemberSkillId(memberId: Long, memberSkillId: Long): MemberSkill?
    fun findByMemberIdAndTechnologyId(memberId: Long, technologyId: Long): MemberSkill?
    fun findByMemberIdAndLevel(memberId: Long, level: SkillLevel): List<MemberSkill>
    
    @Query("""
        SELECT ms FROM MemberSkill ms
        LEFT JOIN Technology t ON ms.technologyId = t.technologyId
        WHERE ms.memberId = :memberId
        AND (:category IS NULL OR t.category = :category)
        AND (:level IS NULL OR ms.level = :level)
    """)
    fun findByMemberIdWithFilters(
        memberId: Long,
        category: TechnologyCategory?,
        level: SkillLevel?
    ): List<MemberSkill>
}

package com.bebeis.skillweaver.core.domain.member.skill

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "member_skill")
class MemberSkill(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_skill_id")
    val memberSkillId: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * Neo4j Technology의 name 참조 (V4)
     * 정규화된 기술을 참조할 때 사용 (예: "spring-boot", "java")
     */
    @Column(name = "technology_name", length = 100)
    val technologyName: String? = null,

    /**
     * 커스텀 기술명 (Neo4j에 없는 기술)
     */
    @Column(name = "custom_name", length = 100)
    val customName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val level: SkillLevel,

    @Column(name = "years_of_use", nullable = false)
    val yearsOfUse: Double = 0.0,

    @Column(name = "last_used_at")
    val lastUsedAt: LocalDate? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    val note: String? = null
) : BaseEntity() {
    init {
        require(!technologyName.isNullOrBlank() || !customName.isNullOrBlank()) {
            "technologyName 또는 customName 중 하나는 반드시 제공되어야 합니다."
        }
        require(yearsOfUse >= 0) {
            "yearsOfUse는 0 이상이어야 합니다."
        }
    }

    /**
     * 기술명을 반환 (technologyName 또는 customName)
     */
    fun getSkillName(): String {
        return technologyName ?: customName ?: "Unknown"
    }

    /**
     * 정규화된 기술 여부 (Neo4j에 존재하는 기술인지)
     */
    fun isStandardTechnology(): Boolean {
        return !technologyName.isNullOrBlank()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberSkill

        return memberSkillId == other.memberSkillId
    }

    override fun hashCode(): Int {
        return memberSkillId?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "MemberSkill(memberSkillId=$memberSkillId, memberId=$memberId, technologyName=$technologyName, customName=$customName, level=$level)"
    }
}

package com.bebeis.skillweaver.core.domain.member.skill

import com.bebeis.skillweaver.core.domain.BaseEntity
import com.bebeis.skillweaver.core.domain.technology.Technology
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
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

    @Column(name = "technology_id")
    val technologyId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id", insertable = false, updatable = false)
    val technology: Technology? = null,

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
        require(technologyId != null || !customName.isNullOrBlank()) {
            "technologyId 또는 customName 중 하나는 반드시 제공되어야 합니다."
        }
        require(yearsOfUse >= 0) {
            "yearsOfUse는 0 이상이어야 합니다."
        }
    }

    /**
     * 기술명을 반환 (Technology 참조 또는 customName)
     */
    fun getSkillName(): String {
        return customName ?: technology?.displayName ?: "Technology#$technologyId"
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
        return "MemberSkill(memberSkillId=$memberSkillId, memberId=$memberId, technologyId=$technologyId, customName=$customName, level=$level)"
    }
}

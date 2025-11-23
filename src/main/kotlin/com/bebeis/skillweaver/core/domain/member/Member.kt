package com.bebeis.skillweaver.core.domain.member

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Member(
    @Column(nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val targetTrack: TargetTrack,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val experienceLevel: ExperienceLevel,

    @Column(nullable = false, length = 255, unique = true)
    val email: String,

    @Column(nullable = false, length = 255)
    val password: String,

    @Embedded
    val learningPreference: LearningPreference = LearningPreference(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val memberId: Long? = null,
) : BaseEntity() {

    fun weeklyCapacityMinutes(): Int {
        val base = learningPreference.dailyMinutes * 5
        val weekendBoost = if (learningPreference.weekendBoost) {
            learningPreference.dailyMinutes * 2
        } else {
            0
        }
        return base + weekendBoost
    }

    fun matchesPassword(rawPassword: String): Boolean {
        return PasswordEncoder.matches(rawPassword, this.password)
    }

    fun update(
        name: String? = null,
        targetTrack: TargetTrack? = null,
        experienceLevel: ExperienceLevel? = null,
        learningPreference: LearningPreference? = null
    ): Member {
        return Member(
            name = name ?: this.name,
            targetTrack = targetTrack ?: this.targetTrack,
            experienceLevel = experienceLevel ?: this.experienceLevel,
            email = this.email,
            password = this.password,
            learningPreference = learningPreference ?: this.learningPreference,
            memberId = this.memberId
        )
    }

    companion object {
        fun create(
            name: String,
            email: String,
            rawPassword: String,
            targetTrack: TargetTrack,
            experienceLevel: ExperienceLevel,
            learningPreference: LearningPreference = LearningPreference()
        ): Member {
            return Member(
                name = name,
                email = email,
                password = PasswordEncoder.encode(rawPassword),
                targetTrack = targetTrack,
                experienceLevel = experienceLevel,
                learningPreference = learningPreference
            )
        }
    }
}

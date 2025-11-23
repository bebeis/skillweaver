package com.bebeis.skillweaver.core.domain.member

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
) {

    // TODO: embabel tool로 노출
    fun weeklyCapacityMinutes(): Int {
        val base = learningPreference.dailyMinutes * 5
        val weekendBoost = if (learningPreference.weekendBoost) {
            learningPreference.dailyMinutes * 2
        } else {
            0
        }
        return base + weekendBoost
    }

    /**
     * 비밀번호 일치 여부 확인
     * @param rawPassword 평문 비밀번호
     * @param passwordEncoder 비밀번호 인코더 (BCrypt 등)
     * @return 일치 여부
     */
    fun matchesPassword(rawPassword: String, passwordEncoder: (String, String) -> Boolean): Boolean {
        return passwordEncoder(rawPassword, this.password)
    }

    /**
     * 비밀번호 변경
     * @param newPassword 새로운 평문 비밀번호
     * @param passwordEncoder 비밀번호 인코더
     * @return 비밀번호가 변경된 새로운 Member 인스턴스
     */
    fun changePassword(newPassword: String, passwordEncoder: (String) -> String): Member {
        return Member(
            name = this.name,
            targetTrack = this.targetTrack,
            experienceLevel = this.experienceLevel,
            email = this.email,
            password = passwordEncoder(newPassword),
            learningPreference = this.learningPreference,
            memberId = this.memberId
        )
    }
}

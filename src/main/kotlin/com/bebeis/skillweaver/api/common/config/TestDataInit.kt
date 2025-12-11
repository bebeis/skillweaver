package com.bebeis.skillweaver.api.common.config

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.Member
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 테스트용 회원 데이터 초기화
 * 
 * local, dev 프로파일에서만 동작합니다.
 */
@Component
@Profile("local", "dev")
class TestDataInit(
    private val memberRepository: MemberRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(TestDataInit::class.java)

    override fun run(vararg args: String?) {
        initTestMembers()
    }

    private fun initTestMembers() {
        // 테스트 회원 1: 백엔드 초급
        createMemberIfNotExists(
            email = "test@test.com",
            name = "테스트유저",
            password = "test1234",
            targetTrack = TargetTrack.BACKEND,
            experienceLevel = ExperienceLevel.BEGINNER,
            learningPreference = LearningPreference(
                dailyMinutes = 60,
                preferKorean = true,
                weekendBoost = true
            )
        )

        // 테스트 회원 2: 풀스택 중급
        createMemberIfNotExists(
            email = "dev@test.com",
            name = "개발자",
            password = "dev1234",
            targetTrack = TargetTrack.FULLSTACK,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPreference = LearningPreference(
                dailyMinutes = 90,
                preferKorean = true,
                weekendBoost = false
            )
        )

        // 테스트 회원 3: 프론트엔드 고급
        createMemberIfNotExists(
            email = "frontend@test.com",
            name = "프론트엔드개발자",
            password = "front1234",
            targetTrack = TargetTrack.FRONTEND,
            experienceLevel = ExperienceLevel.ADVANCED,
            learningPreference = LearningPreference(
                dailyMinutes = 120,
                preferKorean = false,
                weekendBoost = true
            )
        )

        logger.info("✅ Test member data initialization completed")
    }

    private fun createMemberIfNotExists(
        email: String,
        name: String,
        password: String,
        targetTrack: TargetTrack,
        experienceLevel: ExperienceLevel,
        learningPreference: LearningPreference
    ) {
        if (memberRepository.existsByEmail(email)) {
            logger.debug("Test member already exists: $email")
            return
        }

        val member = Member.create(
            name = name,
            email = email,
            rawPassword = password,
            targetTrack = targetTrack,
            experienceLevel = experienceLevel,
            learningPreference = learningPreference
        )

        val saved = memberRepository.save(member)
        logger.info("Test member created: ${saved.email} (ID: ${saved.memberId})")
    }
}

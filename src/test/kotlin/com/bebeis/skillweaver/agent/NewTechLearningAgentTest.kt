package com.bebeis.skillweaver.agent

import com.bebeis.skillweaver.agent.domain.LearningRequest
import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.LearningStyle
import com.bebeis.skillweaver.core.domain.member.Member
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.bebeis.skillweaver.core.storage.member.MemberSkillRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
@DisplayName("NewTechLearningAgent 단위 테스트")
class NewTechLearningAgentTest {

    @MockK
    private lateinit var memberRepository: MemberRepository

    @MockK
    private lateinit var technologyRepository: TechnologyRepository

    @MockK
    private lateinit var memberSkillRepository: MemberSkillRepository

    @InjectMockKs
    private lateinit var newTechLearningAgent: NewTechLearningAgent

    @Test
    @DisplayName("회원 프로필 추출 - 성공")
    fun extractMemberProfile_Success() {
        // given
        val memberId = 1L
        val request = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = "kotlin"
        )
        
        val member = Member(
            memberId = memberId,
            email = "test@example.com",
            password = "hashedPassword",
            name = "테스트 사용자",
            targetTrack = TargetTrack.BACKEND,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPreference = LearningPreference(
                dailyMinutes = 90,
                preferKorean = true,
                learningStyle = LearningStyle.VIDEO_FIRST,
                weekendBoost = true
            )
        )
        
        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { memberSkillRepository.findByMemberId(memberId) } returns emptyList()

        // when
        val result = newTechLearningAgent.extractMemberProfile(request, mockk(relaxed = true))

        // then
        assertNotNull(result)
        assertEquals(memberId, result.memberId)
        assertEquals("테스트 사용자", result.name)
        assertEquals(TargetTrack.BACKEND, result.targetTrack)
        assertEquals(ExperienceLevel.INTERMEDIATE, result.experienceLevel)
        assertEquals(0, result.currentSkillCount)
    }

    @Test
    @DisplayName("회원 프로필 추출 - 회원 없음 실패")
    fun extractMemberProfile_MemberNotFound() {
        // given
        val memberId = 999L
        val request = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = "kotlin"
        )
        
        every { memberRepository.findById(memberId) } returns Optional.empty()

        // when & then
        assertThrows<IllegalArgumentException> {
            newTechLearningAgent.extractMemberProfile(request, mockk(relaxed = true))
        }
    }

    @Test
    @DisplayName("주간 학습 가능 시간 계산 - 성공")
    fun weeklyCapacityMinutes_Calculation() {
        // given
        val memberId = 1L
        val request = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = "kotlin"
        )
        
        val member = Member(
            memberId = memberId,
            email = "test@example.com",
            password = "hashedPassword",
            name = "테스트 사용자",
            targetTrack = TargetTrack.BACKEND,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPreference = LearningPreference(
                dailyMinutes = 90,
                preferKorean = true,
                learningStyle = LearningStyle.VIDEO_FIRST,
                weekendBoost = true
            )
        )
        
        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { memberSkillRepository.findByMemberId(memberId) } returns emptyList()

        // when
        val result = newTechLearningAgent.extractMemberProfile(request, mockk(relaxed = true))

        // then
        // weeklyCapacityMinutes는 dailyMinutes * 7 = 90 * 7 = 630
        assertEquals(630, result.weeklyCapacityMinutes)
    }

    @Test
    @DisplayName("MemberProfile - 학습 스타일 확인")
    fun memberProfile_LearningStyleCheck() {
        // given
        val memberId = 1L
        val request = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = "kotlin"
        )
        
        val videoLearner = Member(
            memberId = memberId,
            email = "test@example.com",
            password = "hashedPassword",
            name = "비디오 우선 학습자",
            targetTrack = TargetTrack.BACKEND,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPreference = LearningPreference(
                dailyMinutes = 90,
                preferKorean = true,
                learningStyle = LearningStyle.VIDEO_FIRST,
                weekendBoost = true
            )
        )
        
        every { memberRepository.findById(memberId) } returns Optional.of(videoLearner)
        every { memberSkillRepository.findByMemberId(memberId) } returns emptyList()

        // when
        val result = newTechLearningAgent.extractMemberProfile(request, mockk(relaxed = true))

        // then
        assertEquals(LearningStyle.VIDEO_FIRST, result.learningPreference.learningStyle)
    }

    @Test
    @DisplayName("MemberProfile - 한국어 선호 여부 확인")
    fun memberProfile_KoreanPreference() {
        // given
        val memberId = 1L
        val request = LearningRequest(
            memberId = memberId,
            targetTechnologyKey = "kotlin"
        )
        
        val koreanLearner = Member(
            memberId = memberId,
            email = "test@example.com",
            password = "hashedPassword",
            name = "한국어 선호 학습자",
            targetTrack = TargetTrack.BACKEND,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            learningPreference = LearningPreference(
                dailyMinutes = 90,
                preferKorean = true,
                learningStyle = LearningStyle.DOC_FIRST,
                weekendBoost = true
            )
        )
        
        every { memberRepository.findById(memberId) } returns Optional.of(koreanLearner)
        every { memberSkillRepository.findByMemberId(memberId) } returns emptyList()

        // when
        val result = newTechLearningAgent.extractMemberProfile(request, mockk(relaxed = true))

        // then
        assertTrue(result.learningPreference.preferKorean)
    }
}

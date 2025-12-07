package com.bebeis.skillweaver.agent.specialist

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.embabel.agent.api.common.OperationContext
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TechResearchAgent 단위 테스트")
class TechResearchAgentTest {

    private val agent = TechResearchAgent()

    @Test
    @DisplayName("TechResearchResult 데이터 구조 확인")
    fun techResearchResult_Structure() {
        // given
        val result = TechResearchResult(
            technology = "Kotlin",
            currentVersion = "2.0.0",
            lastUpdated = "2024-12-01",
            keyTrends = listOf("Coroutines", "Multiplatform", "Compose"),
            industryAdoption = "Widely adopted for Android development",
            topLearningResources = listOf(
                LearningResourceInfo(
                    title = "Kotlin Official Documentation",
                    url = "https://kotlinlang.org/docs/",
                    type = "OFFICIAL_DOC"
                )
            ),
            commonPitfalls = listOf("Null safety confusion", "Coroutine scope misuse"),
            estimatedLearningWeeks = 4
        )

        // then
        assertEquals("Kotlin", result.technology)
        assertEquals("2.0.0", result.currentVersion)
        assertEquals(3, result.keyTrends.size)
        assertEquals(1, result.topLearningResources.size)
        assertEquals(2, result.commonPitfalls.size)
        assertEquals(4, result.estimatedLearningWeeks)
    }

    @Test
    @DisplayName("LearningResourceInfo 데이터 구조 확인")
    fun learningResourceInfo_Structure() {
        // given
        val resource = LearningResourceInfo(
            title = "Kotlin Crash Course",
            url = "https://youtube.com/watch?v=abc123",
            type = "COURSE"
        )

        // then
        assertEquals("Kotlin Crash Course", resource.title)
        assertTrue(resource.url.startsWith("https://"))
        assertEquals("COURSE", resource.type)
    }
}

@DisplayName("ResourceCuratorAgent 단위 테스트")
class ResourceCuratorAgentTest {

    private val agent = ResourceCuratorAgent()

    @Test
    @DisplayName("ResourceCurationRequest 구조 확인")
    fun resourceCurationRequest_Structure() {
        // given
        val request = ResourceCurationRequest(
            technology = "Spring Boot",
            learnerLevel = ExperienceLevel.INTERMEDIATE,
            preferKorean = true
        )

        // then
        assertEquals("Spring Boot", request.technology)
        assertEquals(ExperienceLevel.INTERMEDIATE, request.learnerLevel)
        assertTrue(request.preferKorean)
    }

    @Test
    @DisplayName("CuratedResources 결과 구조 확인")
    fun curatedResources_Structure() {
        // given
        val result = CuratedResources(
            technology = "React",
            learnerLevel = "BEGINNER",
            officialDocs = listOf(
                CuratedResource(
                    title = "React Documentation",
                    url = "https://react.dev/",
                    type = com.bebeis.skillweaver.core.domain.learning.ResourceType.DOC,
                    language = "EN",
                    description = "Official React documentation",
                    difficulty = "BEGINNER",
                    estimatedHours = 5
                )
            ),
            videoCourses = emptyList(),
            practiceResources = emptyList(),
            githubRepositories = emptyList(),
            totalResourceCount = 1
        )

        // then
        assertEquals("React", result.technology)
        assertEquals("BEGINNER", result.learnerLevel)
        assertEquals(1, result.officialDocs.size)
        assertEquals(1, result.totalResourceCount)
    }

    @Test
    @DisplayName("CuratedResource 상세 구조 확인")
    fun curatedResource_DetailedStructure() {
        // given
        val resource = CuratedResource(
            title = "Spring Boot Tutorial",
            url = "https://spring.io/guides",
            type = com.bebeis.skillweaver.core.domain.learning.ResourceType.TUTORIAL,
            language = "KO",
            description = "스프링 부트 입문 가이드",
            difficulty = "BEGINNER",
            estimatedHours = 3
        )

        // then
        assertEquals("Spring Boot Tutorial", resource.title)
        assertEquals("KO", resource.language)
        assertEquals("BEGINNER", resource.difficulty)
        assertEquals(3, resource.estimatedHours)
    }
}

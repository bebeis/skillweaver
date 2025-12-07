package com.bebeis.skillweaver.agent.tools

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore

@ExtendWith(MockKExtension::class)
@DisplayName("KnowledgeSearchTool 단위 테스트")
class KnowledgeSearchToolTest {

    @MockK
    private lateinit var vectorStore: VectorStore

    @InjectMockKs
    private lateinit var knowledgeSearchTool: KnowledgeSearchTool

    @Test
    @DisplayName("지식 검색 - 성공")
    fun searchKnowledge_Success() {
        // given
        val query = "Kotlin coroutines tutorial"
        val topK = 5
        
        val mockDocuments = listOf(
            Document("Kotlin 코루틴 가이드", mapOf(
                "technology" to "Kotlin",
                "type" to "resource",
                "source" to "kotlinlang.org"
            )),
            Document("코루틴 베스트 프랙티스", mapOf(
                "technology" to "Kotlin",
                "type" to "best-practices",
                "source" to "medium.com"
            ))
        )
        
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns mockDocuments

        // when
        val results = knowledgeSearchTool.searchKnowledge(query, topK = topK)

        // then
        verify(exactly = 1) { vectorStore.similaritySearch(any<SearchRequest>()) }
        
        assertEquals(2, results.size)
        // Document의 text는 생성자의 첫 번째 인자로 들어감
        // KnowledgeSearchTool은 formattedContent ?: text ?: ""를 사용
        assertNotNull(results[0].content)
        assertTrue(results[0].content.isNotEmpty())
        assertEquals("Kotlin", results[0].technology)
        assertEquals("resource", results[0].documentType)
        assertEquals("kotlinlang.org", results[0].source)
    }

    @Test
    @DisplayName("기술별 필터링 검색 - 성공")
    fun searchKnowledge_WithTechnologyFilter() {
        // given
        val query = "best practices"
        val technology = "Spring Boot"
        
        val mockDocuments = listOf(
            Document("Spring Boot 모범 사례", mapOf(
                "technology" to "Spring Boot",
                "type" to "best-practices",
                "source" to "spring.io"
            ))
        )
        
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns mockDocuments

        // when
        val results = knowledgeSearchTool.searchKnowledge(query, technology = technology)

        // then
        verify(exactly = 1) { vectorStore.similaritySearch(any<SearchRequest>()) }
        
        assertEquals(1, results.size)
        assertEquals("Spring Boot", results[0].technology)
    }

    @Test
    @DisplayName("로드맵 검색 - 성공")
    fun searchRoadmap_Success() {
        // given
        val technology = "React"
        
        val mockDocuments = listOf(
            Document("React 학습 로드맵", mapOf(
                "technology" to "React",
                "type" to "roadmap",
                "source" to "roadmap.sh"
            )),
            Document("React 추가 리소스", mapOf(
                "technology" to "React",
                "type" to "resource",
                "source" to "react.dev"
            ))
        )
        
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns mockDocuments

        // when
        val results = knowledgeSearchTool.searchRoadmap(technology)

        // then
        verify(exactly = 1) { vectorStore.similaritySearch(any<SearchRequest>()) }
        
        // roadmap 타입만 필터링되어야 함
        assertEquals(1, results.size)
        assertEquals("roadmap", results[0].documentType)
        assertEquals("React", results[0].technology)
    }

    @Test
    @DisplayName("베스트 프랙티스 검색 - 성공")
    fun searchBestPractices_Success() {
        // given
        val technology = "Docker"
        
        val mockDocuments = listOf(
            Document("Docker 멀티 스테이지 빌드", mapOf(
                "technology" to "Docker",
                "type" to "best-practices",
                "source" to "docker-docs"
            )),
            Document("Docker 이미지 최적화", mapOf(
                "technology" to "Docker",
                "type" to "best-practices",
                "source" to "docker-community"
            ))
        )
        
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns mockDocuments

        // when
        val results = knowledgeSearchTool.searchBestPractices(technology)

        // then
        verify(exactly = 1) { vectorStore.similaritySearch(any<SearchRequest>()) }
        
        assertEquals(2, results.size)
        assertTrue(results.all { it.technology == "Docker" })
    }

    @Test
    @DisplayName("빈 결과 처리 - 성공")
    fun searchKnowledge_EmptyResults() {
        // given
        val query = "non-existent technology"
        
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        // when
        val results = knowledgeSearchTool.searchKnowledge(query)

        // then
        verify(exactly = 1) { vectorStore.similaritySearch(any<SearchRequest>()) }
        
        assertTrue(results.isEmpty())
    }
}

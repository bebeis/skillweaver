package com.bebeis.skillweaver.agent.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore

@ExtendWith(MockKExtension::class)
@DisplayName("KnowledgeIngestionService 단위 테스트")
class KnowledgeIngestionServiceTest {

    @MockK
    private lateinit var vectorStore: VectorStore

    @InjectMockKs
    private lateinit var knowledgeIngestionService: KnowledgeIngestionService

    @Test
    @DisplayName("기술 로드맵 저장 - 성공")
    fun ingestTechRoadmap_Success() {
        // given
        val technology = "Kotlin"
        val content = "Kotlin 학습 로드맵: 1. 기초 문법..."
        val source = "https://kotlinlang.org"
        
        val documentSlot = slot<List<Document>>()
        every { vectorStore.add(capture(documentSlot)) } returns Unit

        // when
        knowledgeIngestionService.ingestTechRoadmap(technology, content, source)

        // then
        verify(exactly = 1) { vectorStore.add(any()) }
        
        val capturedDocument = documentSlot.captured.first()
        assertEquals(content, capturedDocument.text)
        assertEquals(technology, capturedDocument.metadata["technology"])
        assertEquals("roadmap", capturedDocument.metadata["type"])
        assertEquals(source, capturedDocument.metadata["source"])
    }

    @Test
    @DisplayName("학습 리소스 저장 - 성공")
    fun ingestLearningResource_Success() {
        // given
        val technology = "Spring Boot"
        val title = "Spring Boot 공식 가이드"
        val content = "Spring Boot makes it easy to create stand-alone..."
        val url = "https://spring.io/guides"
        
        val documentSlot = slot<List<Document>>()
        every { vectorStore.add(capture(documentSlot)) } returns Unit

        // when
        knowledgeIngestionService.ingestLearningResource(technology, title, content, url)

        // then
        verify(exactly = 1) { vectorStore.add(any()) }
        
        val capturedDocument = documentSlot.captured.first()
        assertTrue(capturedDocument.text?.contains(title) == true)
        assertTrue(capturedDocument.text?.contains(content) == true)
        assertEquals(technology, capturedDocument.metadata["technology"])
        assertEquals("resource", capturedDocument.metadata["type"])
        assertEquals(url, capturedDocument.metadata["url"])
    }

    @Test
    @DisplayName("베스트 프랙티스 저장 - 성공")
    fun ingestBestPractices_Success() {
        // given
        val technology = "Docker"
        val content = "Docker 베스트 프랙티스: 1. 멀티 스테이지 빌드 사용..."
        val source = "docker-docs"
        
        val documentSlot = slot<List<Document>>()
        every { vectorStore.add(capture(documentSlot)) } returns Unit

        // when
        knowledgeIngestionService.ingestBestPractices(technology, content, source)

        // then
        verify(exactly = 1) { vectorStore.add(any()) }
        
        val capturedDocument = documentSlot.captured.first()
        assertEquals(content, capturedDocument.text)
        assertEquals(technology, capturedDocument.metadata["technology"])
        assertEquals("best-practices", capturedDocument.metadata["type"])
    }

    @Test
    @DisplayName("커뮤니티 인사이트 저장 - 성공")
    fun ingestCommunityInsight_Success() {
        // given
        val technology = "React"
        val content = "React 18의 주요 변경 사항과 마이그레이션 가이드"
        val source = "reddit-reactjs"
        val category = "community-insight"
        
        val documentSlot = slot<List<Document>>()
        every { vectorStore.add(capture(documentSlot)) } returns Unit

        // when
        knowledgeIngestionService.ingestCommunityInsight(technology, content, source, category)

        // then
        verify(exactly = 1) { vectorStore.add(any()) }
        
        val capturedDocument = documentSlot.captured.first()
        assertEquals(content, capturedDocument.text)
        assertEquals(technology, capturedDocument.metadata["technology"])
        assertEquals(category, capturedDocument.metadata["type"])
    }

    @Test
    @DisplayName("일괄 문서 저장 - 성공")
    fun ingestBatch_Success() {
        // given
        val documents = listOf(
            KnowledgeIngestionService.KnowledgeDocument(
                content = "Content 1",
                technology = "Tech1",
                type = "roadmap",
                source = "source1"
            ),
            KnowledgeIngestionService.KnowledgeDocument(
                content = "Content 2",
                technology = "Tech2",
                type = "resource",
                source = "source2"
            )
        )
        
        val documentSlot = slot<List<Document>>()
        every { vectorStore.add(capture(documentSlot)) } returns Unit

        // when
        knowledgeIngestionService.ingestBatch(documents)

        // then
        verify(exactly = 1) { vectorStore.add(any()) }
        
        val capturedDocuments = documentSlot.captured
        assertEquals(2, capturedDocuments.size)
        assertEquals("Content 1", capturedDocuments[0].text)
        assertEquals("Tech1", capturedDocuments[0].metadata["technology"])
        assertEquals("Content 2", capturedDocuments[1].text)
        assertEquals("Tech2", capturedDocuments[1].metadata["technology"])
    }
}

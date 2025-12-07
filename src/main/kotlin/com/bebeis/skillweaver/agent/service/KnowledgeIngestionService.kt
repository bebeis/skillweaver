package com.bebeis.skillweaver.agent.service

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

/**
 * 지식 베이스에 문서를 임베딩하고 저장하는 서비스
 * 
 * VectorStore 빈이 존재할 때만 활성화됩니다.
 */
@Service
@ConditionalOnBean(VectorStore::class)
class KnowledgeIngestionService(
    private val vectorStore: VectorStore
) {
    private val logger = LoggerFactory.getLogger(KnowledgeIngestionService::class.java)
    
    /**
     * 기술별 학습 로드맵을 벡터 DB에 저장
     */
    fun ingestTechRoadmap(
        technology: String,
        content: String,
        source: String
    ) {
        val document = Document(
            content,
            mapOf(
                "technology" to technology,
                "type" to "roadmap",
                "source" to source
            )
        )
        vectorStore.add(listOf(document))
        logger.info("Ingested roadmap for technology: {} from source: {}", technology, source)
    }
    
    /**
     * 학습 리소스를 벡터 DB에 저장
     */
    fun ingestLearningResource(
        technology: String,
        title: String,
        content: String,
        url: String,
        resourceType: String = "resource"
    ) {
        val document = Document(
            "$title\n\n$content",
            mapOf(
                "technology" to technology,
                "type" to resourceType,
                "title" to title,
                "url" to url,
                "source" to url
            )
        )
        vectorStore.add(listOf(document))
        logger.info("Ingested resource: {} for technology: {}", title, technology)
    }
    
    /**
     * 베스트 프랙티스 문서를 벡터 DB에 저장
     */
    fun ingestBestPractices(
        technology: String,
        content: String,
        source: String
    ) {
        val document = Document(
            content,
            mapOf(
                "technology" to technology,
                "type" to "best-practices",
                "source" to source
            )
        )
        vectorStore.add(listOf(document))
        logger.info("Ingested best practices for technology: {}", technology)
    }
    
    /**
     * 커뮤니티 인사이트 저장
     */
    fun ingestCommunityInsight(
        technology: String,
        content: String,
        source: String,
        category: String = "community"
    ) {
        val document = Document(
            content,
            mapOf(
                "technology" to technology,
                "type" to category,
                "source" to source
            )
        )
        vectorStore.add(listOf(document))
        logger.info("Ingested community insight for technology: {} from: {}", technology, source)
    }
    
    /**
     * 여러 문서를 일괄 저장
     */
    fun ingestBatch(documents: List<KnowledgeDocument>) {
        val aiDocuments = documents.map { doc ->
            Document(
                doc.content,
                mapOf(
                    "technology" to doc.technology,
                    "type" to doc.type,
                    "source" to doc.source
                ) + doc.additionalMetadata
            )
        }
        vectorStore.add(aiDocuments)
        logger.info("Batch ingested {} documents", documents.size)
    }
    
    data class KnowledgeDocument(
        val content: String,
        val technology: String,
        val type: String,
        val source: String,
        val additionalMetadata: Map<String, Any> = emptyMap()
    )
}

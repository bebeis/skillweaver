package com.bebeis.skillweaver.agent.tools

import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * RAG 기반 지식 검색 도구 - 벡터 DB에서 기술 학습 관련 문서를 검색
 * 
 * VectorStore 빈이 존재할 때만 활성화됩니다.
 */
@Component
@Profile("rag")
class KnowledgeSearchTool(
    private val vectorStore: VectorStore
) {
    /**
     * 지식 베이스에서 기술 관련 학습 자료를 검색합니다.
     * 
     * @param query 검색 쿼리
     * @param technology 특정 기술로 필터링 (optional)
     * @param topK 반환할 최대 결과 수
     * @return 관련 지식 결과 목록
     */
    fun searchKnowledge(
        query: String,
        technology: String? = null,
        topK: Int = 5
    ): List<KnowledgeResult> {
        // Spring AI 1.0.3 - SearchRequest.builder() 패턴
        val requestBuilder = SearchRequest.builder()
            .query(query)
            .topK(topK)
        
        // 기술별 필터링
        if (technology != null) {
            requestBuilder.filterExpression("technology == '$technology'")
        }
        
        val request = requestBuilder.build()
        
        return vectorStore.similaritySearch(request)
            .map { doc ->
                KnowledgeResult(
                    content = doc.formattedContent ?: doc.text ?: "",
                    source = doc.metadata["source"]?.toString() ?: "unknown",
                    technology = doc.metadata["technology"]?.toString(),
                    documentType = doc.metadata["type"]?.toString() ?: "general",
                    relevanceScore = 0.0
                )
            }
    }
    
    /**
     * 특정 기술의 학습 로드맵을 검색합니다.
     */
    fun searchRoadmap(technology: String): List<KnowledgeResult> {
        return searchKnowledge(
            query = "$technology learning roadmap curriculum steps",
            technology = technology,
            topK = 3
        ).filter { it.documentType == "roadmap" }
    }
    
    /**
     * 특정 기술의 베스트 프랙티스와 주의사항을 검색합니다.
     */
    fun searchBestPractices(technology: String): List<KnowledgeResult> {
        return searchKnowledge(
            query = "$technology best practices common mistakes pitfalls tips",
            technology = technology,
            topK = 5
        )
    }
    
    data class KnowledgeResult(
        val content: String,
        val source: String,
        val technology: String?,
        val documentType: String,
        val relevanceScore: Double
    )
}

package com.bebeis.skillweaver.core.domain.knowledge

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 지식 베이스 문서 엔티티
 * 학습 로드맵, 베스트 프랙티스, 커뮤니티 인사이트 등 저장
 */
@Entity
@Table(name = "knowledge_documents")
class KnowledgeDocument(
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val technology: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val documentType: KnowledgeType,
    
    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,
    
    @Column(nullable = false)
    val source: String,
    
    @Column
    val vectorId: String? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    val updatedAt: LocalDateTime? = null
)

/**
 * 지식 문서 유형
 */
enum class KnowledgeType {
    ROADMAP,           // 학습 로드맵
    BEST_PRACTICE,     // 베스트 프랙티스
    COMMUNITY_INSIGHT, // 커뮤니티 인사이트
    OFFICIAL_DOC,      // 공식 문서
    TUTORIAL,          // 튜토리얼
    CASE_STUDY         // 성공 사례
}

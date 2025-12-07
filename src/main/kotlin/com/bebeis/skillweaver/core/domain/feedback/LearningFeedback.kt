package com.bebeis.skillweaver.core.domain.feedback

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 학습 피드백 엔티티
 * 사용자가 학습 계획이나 개별 단계에 대해 제공한 피드백 저장
 */
@Entity
@Table(name = "learning_feedbacks")
class LearningFeedback(
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val learningPlanId: Long,
    
    @Column
    val stepId: Long? = null,
    
    @Column(nullable = false)
    val memberId: Long,
    
    @Column(nullable = false)
    val rating: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val feedbackType: FeedbackType,
    
    @Column(columnDefinition = "TEXT")
    val comment: String? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
    }
}

/**
 * 피드백 유형
 */
enum class FeedbackType {
    HELPFUL,      // 도움이 됨
    TOO_EASY,     // 너무 쉬움
    TOO_HARD,     // 너무 어려움
    IRRELEVANT,   // 관련 없음
    TIME_ISSUE,   // 시간 예측이 맞지 않음
    RESOURCE_ISSUE, // 리소스 문제
    GENERAL       // 일반 피드백
}

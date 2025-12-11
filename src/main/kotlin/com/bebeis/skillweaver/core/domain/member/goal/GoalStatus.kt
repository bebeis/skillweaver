package com.bebeis.skillweaver.core.domain.member.goal

/**
 * 학습 목표 상태
 * 
 * V5: IN_PROGRESS 상태 추가
 * Lifecycle: PLANNING → ACTIVE → IN_PROGRESS → COMPLETED/ABANDONED
 */
enum class GoalStatus {
    PLANNING,      // 계획 중
    ACTIVE,        // 활성 (아직 시작 전, 학습 플랜 연동 전)
    IN_PROGRESS,   // V5 추가: 학습 진행 중 (학습 플랜 시작 후)
    COMPLETED,     // 완료
    ABANDONED      // 포기
}

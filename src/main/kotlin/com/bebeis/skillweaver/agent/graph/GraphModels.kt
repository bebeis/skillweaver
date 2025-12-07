package com.bebeis.skillweaver.agent.graph

/**
 * V3 GraphRAG 데이터 모델
 * 
 * 기술 관계 그래프를 표현하는 데이터 클래스들
 */

/**
 * 기술 노드
 */
data class TechNode(
    val name: String,
    val displayName: String,
    val category: TechCategory,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val description: String? = null
)

enum class TechCategory {
    LANGUAGE,       // Java, Kotlin, Python
    FRAMEWORK,      // Spring, React
    LIBRARY,        // JPA, Pandas
    TOOL,           // Docker, Git
    CONCEPT,        // DI, REST, SOLID
    DATABASE        // MySQL, Redis
}

enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * 기술 간 관계(엣지) 타입
 */
enum class TechRelation(val description: String) {
    DEPENDS_ON("requires knowledge of"),
    CONTAINS("includes/is part of"),
    RECOMMENDED_AFTER("should be learned after"),
    ALTERNATIVE_TO("is an alternative to"),
    EXTENDS("extends/builds upon"),
    USED_WITH("commonly used together")
}

/**
 * 기술 간 관계
 */
data class TechEdge(
    val from: String,
    val to: String,
    val relation: TechRelation,
    val weight: Double = 1.0  // 관계의 중요도
)

/**
 * 학습 경로 결과
 */
data class LearningPath(
    val from: String,
    val to: String,
    val steps: List<PathStep>,
    val totalDuration: String? = null
)

data class PathStep(
    val technology: String,
    val relation: TechRelation,
    val estimatedDuration: String? = null
)

/**
 * 선행 지식 조회 결과
 */
data class Prerequisites(
    val technology: String,
    val required: List<TechNode>,
    val recommended: List<TechNode>
)

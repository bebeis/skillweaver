package com.bebeis.skillweaver.agent.graph

/**
 * V3 GraphRAG 데이터 모델
 * 
 * 기술 관계 그래프를 표현하는 데이터 클래스들
 */

/**
 * 기술 노드
 * 
 * V4: RDB Technology 모델의 모든 속성을 통합
 */
data class TechNode(
    // === 필수 필드 ===
    val name: String,                              // unique identifier (URL-safe)
    val displayName: String,                       // 사용자에게 표시되는 이름
    val category: TechCategory,                    // 기술 분류
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    
    // === RDB Technology에서 이관 ===
    val ecosystem: String? = null,                 // JVM, JavaScript, Python 등
    val officialSite: String? = null,              // 공식 사이트 URL
    val active: Boolean = true,                    // 활성화 여부
    
    // === 학습 메타데이터 ===
    val learningRoadmap: String? = null,           // 학습 로드맵 설명
    val estimatedLearningHours: Int? = null,       // 예상 학습 시간
    val communityPopularity: Int? = null,          // 커뮤니티 인기도 (1-10)
    val jobMarketDemand: Int? = null,              // 취업 시장 수요 (1-10)
    
    // === TechnologyKnowledge에서 이관 ===
    val description: String? = null,               // 기술 요약 설명 (summary)
    val learningTips: String? = null,              // 학습 팁
    val useCases: List<String> = emptyList()       // 사용 사례 목록
)

/**
 * 기술 분류
 * 
 * V4: RDB TechnologyCategory와 통합 (호환성 유지)
 */
enum class TechCategory {
    // 기존 Graph 모델
    LANGUAGE,       // Java, Kotlin, Python
    FRAMEWORK,      // Spring, React
    LIBRARY,        // JPA, Pandas
    TOOL,           // Docker, Git
    CONCEPT,        // DI, REST, SOLID
    DATABASE,       // MySQL, Redis
    
    // RDB TechnologyCategory에서 추가 (호환성)
    DB,             // DATABASE의 별칭
    PLATFORM,       // AWS, GCP, Azure
    DEVOPS,         // CI/CD, Kubernetes
    API,            // REST, GraphQL
    ETC             // 기타
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

/**
 * 기술 노드 업데이트 요청
 * 
 * V4: 부분 업데이트를 위한 DTO (모든 필드 nullable)
 */
data class TechNodeUpdate(
    val displayName: String? = null,
    val category: TechCategory? = null,
    val difficulty: Difficulty? = null,
    val ecosystem: String? = null,
    val officialSite: String? = null,
    val active: Boolean? = null,
    val learningRoadmap: String? = null,
    val estimatedLearningHours: Int? = null,
    val communityPopularity: Int? = null,
    val jobMarketDemand: Int? = null,
    val description: String? = null,
    val learningTips: String? = null,
    val useCases: List<String>? = null
)


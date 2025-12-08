package com.bebeis.skillweaver.api.graph

import com.bebeis.skillweaver.agent.graph.TechGraphService
import com.bebeis.skillweaver.agent.graph.TechNode
import com.bebeis.skillweaver.api.common.ApiResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * V3 Graph API Controller
 * 
 * GraphRAG 기반의 빠르고 무료인 기술 탐색 API입니다.
 * LLM을 사용하지 않고 Neo4j Cypher 쿼리로 동작합니다.
 */
@RestController
@RequestMapping("/api/v1/graph")
@ConditionalOnBean(TechGraphService::class)
class GraphApiController(
    private val techGraphService: TechGraphService
) {
    /**
     * 기술 로드맵 조회
     * GET /api/v1/graph/roadmap/{technology}
     */
    @GetMapping("/roadmap/{technology}")
    fun getRoadmap(@PathVariable technology: String): ResponseEntity<ApiResponse<RoadmapResponse>> {
        if (!techGraphService.existsTechnology(technology)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$technology' not found in graph")
            )
        }
        
        val prerequisites = techGraphService.findPrerequisites(technology)
        val nextSteps = techGraphService.findRelatedTechnologies(technology)
        
        val response = RoadmapResponse(
            technology = technology,
            displayName = prerequisites.technology,
            prerequisites = PrerequisitesDto(
                required = prerequisites.required.map { it.toDto() },
                recommended = prerequisites.recommended.map { it.toDto() }
            ),
            nextSteps = nextSteps.map { it.toDto() }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 학습 경로 탐색
     * GET /api/v1/graph/path?from={from}&to={to}
     */
    @GetMapping("/path")
    fun getLearningPath(
        @RequestParam from: String,
        @RequestParam to: String
    ): ResponseEntity<ApiResponse<LearningPathResponse>> {
        val path = techGraphService.findLearningPath(from, to)
            ?: return ResponseEntity.status(404).body(
                ApiResponse.error("NO_PATH_FOUND", "Cannot find learning path from '$from' to '$to'")
            )
        
        val response = LearningPathResponse(
            from = path.from,
            to = path.to,
            totalSteps = path.steps.size,
            path = path.steps.mapIndexed { index, step ->
                PathStepDto(
                    step = index + 1,
                    technology = step.technology,
                    relation = step.relation.name
                )
            }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 연관 기술 추천
     * GET /api/v1/graph/recommendations/{technology}
     */
    @GetMapping("/recommendations/{technology}")
    fun getRecommendations(@PathVariable technology: String): ResponseEntity<ApiResponse<RecommendationsResponse>> {
        if (!techGraphService.existsTechnology(technology)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$technology' not found in graph")
            )
        }
        
        val related = techGraphService.findRelatedTechnologies(technology)
        
        val response = RecommendationsResponse(
            technology = technology,
            recommendations = related.map { 
                RecommendationDto(
                    name = it.name,
                    displayName = it.displayName,
                    relation = "USED_WITH",
                    category = it.category.name
                )
            }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 갭 분석
     * POST /api/v1/graph/gap-analysis
     */
    @PostMapping("/gap-analysis")
    fun analyzeGap(@RequestBody request: GapAnalysisRequest): ResponseEntity<ApiResponse<GapAnalysisResponse>> {
        if (!techGraphService.existsTechnology(request.targetTechnology)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Target technology '${request.targetTechnology}' not found in graph")
            )
        }
        
        val prerequisites = techGraphService.findPrerequisites(request.targetTechnology)
        val allRequired = prerequisites.required.map { it.name }.toSet()
        val known = request.knownTechnologies.toSet()
        val missing = allRequired - known
        
        val readinessScore = if (allRequired.isEmpty()) 1.0 
                             else (known.intersect(allRequired).size.toDouble() / allRequired.size)
        
        val response = GapAnalysisResponse(
            target = request.targetTechnology,
            known = request.knownTechnologies,
            missing = prerequisites.required
                .filter { it.name in missing }
                .map { MissingTechDto(it.name, it.displayName, "HIGH") },
            ready = missing.isEmpty(),
            readinessScore = readinessScore,
            message = if (missing.isEmpty()) {
                "모든 선행 지식을 갖추고 있습니다. ${request.targetTechnology} 학습을 시작할 수 있습니다."
            } else {
                "다음 기술을 먼저 학습하면 더 효과적입니다: ${missing.joinToString(", ")}"
            }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun TechNode.toDto() = TechNodeDto(
        name = this.name,
        displayName = this.displayName,
        category = this.category.name,
        difficulty = this.difficulty.name
    )
}

// DTOs
data class RoadmapResponse(
    val technology: String,
    val displayName: String,
    val prerequisites: PrerequisitesDto,
    val nextSteps: List<TechNodeDto>
)

data class PrerequisitesDto(
    val required: List<TechNodeDto>,
    val recommended: List<TechNodeDto>
)

data class TechNodeDto(
    val name: String,
    val displayName: String,
    val category: String,
    val difficulty: String
)

data class LearningPathResponse(
    val from: String,
    val to: String,
    val totalSteps: Int,
    val path: List<PathStepDto>
)

data class PathStepDto(
    val step: Int,
    val technology: String,
    val relation: String
)

data class RecommendationsResponse(
    val technology: String,
    val recommendations: List<RecommendationDto>
)

data class RecommendationDto(
    val name: String,
    val displayName: String,
    val relation: String,
    val category: String
)

data class GapAnalysisRequest(
    val knownTechnologies: List<String>,
    val targetTechnology: String
)

data class GapAnalysisResponse(
    val target: String,
    val known: List<String>,
    val missing: List<MissingTechDto>,
    val ready: Boolean,
    val readinessScore: Double,
    val message: String
)

data class MissingTechDto(
    val name: String,
    val displayName: String,
    val priority: String
)

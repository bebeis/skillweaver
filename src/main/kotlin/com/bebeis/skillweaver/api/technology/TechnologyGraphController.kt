package com.bebeis.skillweaver.api.technology

import com.bebeis.skillweaver.agent.graph.*
import com.bebeis.skillweaver.api.common.ApiResponse
import com.bebeis.skillweaver.api.technology.dto.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * V4 통합 기술 API Controller
 * 
 * 기존 TechnologyController와 GraphApiController를 통합합니다.
 * 모든 기술 데이터는 Neo4j Graph에서 관리됩니다.
 * 
 * Base Path: /api/v1/technologies
 */
@RestController
@RequestMapping("/api/v1/technologies")
@ConditionalOnBean(TechGraphService::class)
class TechnologyGraphController(
    private val techGraphService: TechGraphService
) {

    // ==========================================================================
    // CRUD 엔드포인트
    // ==========================================================================

    /**
     * 기술 목록 조회
     * GET /api/v1/technologies
     */
    @GetMapping
    fun getAllTechnologies(
        @RequestParam(required = false) category: TechCategory?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "100") limit: Int
    ): ApiResponse<TechnologyGraphListResponse> {
        val technologies = techGraphService.findAll(category, active, search, limit)
        
        val response = TechnologyGraphListResponse(
            technologies = technologies.map { TechnologyGraphResponse.from(it) },
            totalCount = technologies.size
        )
        
        return ApiResponse.success(response)
    }

    /**
     * 기술 상세 조회
     * GET /api/v1/technologies/{name}
     */
    @GetMapping("/{name}")
    fun getTechnology(
        @PathVariable name: String
    ): ResponseEntity<ApiResponse<TechnologyGraphDetailResponse>> {
        val tech = techGraphService.findByName(name)
            ?: return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found")
            )
        
        val prerequisites = try { techGraphService.findPrerequisites(name) } catch (e: Exception) { null }
        val related = try { techGraphService.findRelatedTechnologies(name) } catch (e: Exception) { emptyList() }
        
        val response = TechnologyGraphDetailResponse.from(tech, prerequisites, related)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 기술 생성
     * POST /api/v1/technologies
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTechnology(
        @RequestBody request: CreateTechnologyGraphRequest
    ): ResponseEntity<ApiResponse<TechnologyGraphResponse>> {
        // 중복 체크
        if (techGraphService.existsTechnology(request.name)) {
            return ResponseEntity.status(409).body(
                ApiResponse.error("TECHNOLOGY_ALREADY_EXISTS", "Technology '${request.name}' already exists")
            )
        }
        
        val techNode = request.toTechNode()
        val created = techGraphService.createTechnology(techNode)
        
        // 관계 생성
        request.relations.forEach { rel ->
            techGraphService.createRelationship(TechEdge(
                from = request.name,
                to = rel.to,
                relation = rel.relation,
                weight = rel.weight
            ))
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(TechnologyGraphResponse.from(created), "기술이 등록되었습니다")
        )
    }

    /**
     * 기술 수정
     * PUT /api/v1/technologies/{name}
     */
    @PutMapping("/{name}")
    fun updateTechnology(
        @PathVariable name: String,
        @RequestBody request: UpdateTechnologyGraphRequest
    ): ResponseEntity<ApiResponse<TechnologyGraphResponse>> {
        val updated = techGraphService.updateTechnology(name, request.toTechNodeUpdate())
            ?: return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found")
            )
        
        return ResponseEntity.ok(
            ApiResponse.success(TechnologyGraphResponse.from(updated), "기술 정보가 수정되었습니다")
        )
    }

    /**
     * 기술 삭제
     * DELETE /api/v1/technologies/{name}
     */
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTechnology(@PathVariable name: String): ResponseEntity<Void> {
        val deleted = techGraphService.deleteTechnology(name)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==========================================================================
    // 로드맵 & 학습 경로 엔드포인트 (기존 Graph API 통합)
    // ==========================================================================

    /**
     * 기술 로드맵 조회
     * GET /api/v1/technologies/{name}/roadmap
     */
    @GetMapping("/{name}/roadmap")
    fun getRoadmap(@PathVariable name: String): ResponseEntity<ApiResponse<RoadmapGraphResponse>> {
        if (!techGraphService.existsTechnology(name)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found in graph")
            )
        }
        
        val prerequisites = techGraphService.findPrerequisites(name)
        val nextSteps = techGraphService.findRelatedTechnologies(name)
        
        val response = RoadmapGraphResponse(
            technology = name,
            displayName = prerequisites.technology,
            prerequisites = PrerequisitesGraphDto.from(prerequisites),
            nextSteps = nextSteps.map { TechNodeSummaryDto.from(it) }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 학습 경로 탐색
     * GET /api/v1/technologies/path?from={from}&to={to}
     */
    @GetMapping("/path")
    fun getLearningPath(
        @RequestParam from: String,
        @RequestParam to: String
    ): ResponseEntity<ApiResponse<LearningPathGraphResponse>> {
        val path = techGraphService.findLearningPath(from, to)
            ?: return ResponseEntity.status(404).body(
                ApiResponse.error("NO_PATH_FOUND", "Cannot find learning path from '$from' to '$to'")
            )
        
        return ResponseEntity.ok(ApiResponse.success(LearningPathGraphResponse.from(path)))
    }

    /**
     * 연관 기술 추천
     * GET /api/v1/technologies/{name}/recommendations
     */
    @GetMapping("/{name}/recommendations")
    fun getRecommendations(@PathVariable name: String): ResponseEntity<ApiResponse<RecommendationsGraphResponse>> {
        if (!techGraphService.existsTechnology(name)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found in graph")
            )
        }
        
        val related = techGraphService.findRelatedTechnologies(name)
        
        val response = RecommendationsGraphResponse(
            technology = name,
            recommendations = related.map { RecommendationGraphDto.from(it) }
        )
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 갭 분석
     * POST /api/v1/technologies/gap-analysis
     */
    @PostMapping("/gap-analysis")
    fun analyzeGap(@RequestBody request: GapAnalysisGraphRequest): ResponseEntity<ApiResponse<GapAnalysisGraphResponse>> {
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
        
        val response = GapAnalysisGraphResponse(
            target = request.targetTechnology,
            known = request.knownTechnologies,
            missing = prerequisites.required
                .filter { it.name in missing }
                .map { MissingTechGraphDto(it.name, it.displayName, "HIGH") },
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

    // ==========================================================================
    // 관계 관리 엔드포인트
    // ==========================================================================

    /**
     * 기술 관계 조회
     * GET /api/v1/technologies/{name}/relationships
     */
    @GetMapping("/{name}/relationships")
    fun getRelationships(
        @PathVariable name: String,
        @RequestParam(required = false) relationType: TechRelation?
    ): ResponseEntity<ApiResponse<List<TechRelationshipGraphResponse>>> {
        if (!techGraphService.existsTechnology(name)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found")
            )
        }
        
        val relationships = techGraphService.findRelationships(name, relationType)
        val response = relationships.map { TechRelationshipGraphResponse.from(it) }
        
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 관계 생성
     * POST /api/v1/technologies/{name}/relationships
     */
    @PostMapping("/{name}/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRelationship(
        @PathVariable name: String,
        @RequestBody request: TechRelationRequest
    ): ResponseEntity<ApiResponse<TechRelationshipGraphResponse>> {
        if (!techGraphService.existsTechnology(name)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("TECHNOLOGY_NOT_FOUND", "Technology '$name' not found")
            )
        }
        
        if (name == request.to) {
            return ResponseEntity.status(400).body(
                ApiResponse.error("INVALID_RELATIONSHIP", "Cannot create relationship to self")
            )
        }
        
        val edge = TechEdge(
            from = name,
            to = request.to,
            relation = request.relation,
            weight = request.weight
        )
        
        val created = techGraphService.createRelationship(edge)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(TechRelationshipGraphResponse.from(created), "관계가 생성되었습니다")
        )
    }

    /**
     * 관계 삭제
     * DELETE /api/v1/technologies/{from}/relationships/{to}?relationType={type}
     */
    @DeleteMapping("/{from}/relationships/{to}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRelationship(
        @PathVariable from: String,
        @PathVariable to: String,
        @RequestParam relationType: TechRelation
    ): ResponseEntity<Void> {
        techGraphService.deleteRelationship(from, to, relationType)
        return ResponseEntity.noContent().build()
    }
}

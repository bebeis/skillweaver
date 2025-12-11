package com.bebeis.skillweaver.agent.service

import com.bebeis.skillweaver.agent.graph.TechGraphService
import com.bebeis.skillweaver.agent.graph.TechCategory
import com.bebeis.skillweaver.agent.graph.TechNode
import com.bebeis.skillweaver.agent.graph.TechEdge
import com.bebeis.skillweaver.agent.graph.TechRelation
import com.bebeis.skillweaver.agent.graph.Difficulty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

/**
 * V3 GraphRAG 시드 로더
 * 
 * JSON 시드 파일에서 기술 관계를 읽어 Neo4j에 로딩합니다.
 * skillweaver.graph.enabled=true, skillweaver.graph.seed-on-startup=true 일 때 동작합니다.
 */
@Service
@ConditionalOnProperty(name = ["skillweaver.graph.enabled"], havingValue = "true")
@ConditionalOnBean(Driver::class)
class GraphSeedRunner(
    private val neo4jDriver: Driver,
    private val objectMapper: ObjectMapper,
    @Value("\${skillweaver.graph.seed-on-startup:false}")
    private val seedOnStartup: Boolean
) {
    private val log = LoggerFactory.getLogger(GraphSeedRunner::class.java)

    @PostConstruct
    fun init() {
        if (seedOnStartup) {
            log.info("Graph seeding enabled, loading seed data...")
            loadGraphSeedData()
        }
    }

    fun loadGraphSeedData() {
        val resolver = PathMatchingResourcePatternResolver()
        val resources: Array<Resource> = resolver.getResources("classpath:knowledge-seed/*.json")
        
        log.info("Found ${resources.size} seed files for graph loading")
        
        var nodesCreated = 0
        var edgesCreated = 0
        var skippedRelations = 0
        
        // Phase 1: 모든 기술 노드 먼저 생성
        val availableTechnologies = mutableSetOf<String>()
        
        neo4jDriver.session().use { session ->
            // 모든 시드 파일에서 노드 생성
            for (resource in resources) {
                try {
                    val content = resource.inputStream.bufferedReader().readText()
                    val seedData: Map<String, Any> = objectMapper.readValue(content)
                    
                    val techName = seedData["technology"] as? String ?: continue
                    availableTechnologies.add(techName)
                    
                    val displayName = seedData["displayName"] as? String ?: techName
                    val category = (seedData["category"] as? String) ?: "CONCEPT"
                    val difficulty = (seedData["difficulty"] as? String) ?: "INTERMEDIATE"
                    
                    // V4: 확장 속성 추출
                    val ecosystem = seedData["ecosystem"] as? String
                    val officialSite = seedData["officialSite"] as? String
                    val active = seedData["active"] as? Boolean ?: true
                    val estimatedLearningHours = (seedData["estimatedLearningHours"] as? Number)?.toInt()
                    val communityPopularity = (seedData["communityPopularity"] as? Number)?.toInt()
                    val jobMarketDemand = (seedData["jobMarketDemand"] as? Number)?.toInt()
                    val learningTips = seedData["learningTips"] as? String
                    @Suppress("UNCHECKED_CAST")
                    val useCases = seedData["useCases"] as? List<String> ?: emptyList()
                    
                    // 노드 생성 (MERGE로 중복 방지) - V4: 모든 확장 속성 포함
                    session.run("""
                        MERGE (t:Technology {name: ${"$"}name})
                        SET t.displayName = ${"$"}displayName,
                            t.category = ${"$"}category,
                            t.difficulty = ${"$"}difficulty,
                            t.ecosystem = ${"$"}ecosystem,
                            t.officialSite = ${"$"}officialSite,
                            t.active = ${"$"}active,
                            t.estimatedLearningHours = ${"$"}estimatedLearningHours,
                            t.communityPopularity = ${"$"}communityPopularity,
                            t.jobMarketDemand = ${"$"}jobMarketDemand,
                            t.learningTips = ${"$"}learningTips,
                            t.useCases = ${"$"}useCases
                    """, mapOf(
                        "name" to techName,
                        "displayName" to displayName,
                        "category" to category,
                        "difficulty" to difficulty,
                        "ecosystem" to ecosystem,
                        "officialSite" to officialSite,
                        "active" to active,
                        "estimatedLearningHours" to estimatedLearningHours,
                        "communityPopularity" to communityPopularity,
                        "jobMarketDemand" to jobMarketDemand,
                        "learningTips" to learningTips,
                        "useCases" to useCases
                    ))
                    nodesCreated++
                    
                } catch (e: Exception) {
                    log.warn("Failed to load node from ${resource.filename}: ${e.message}")
                }
            }
            
            log.info("Created $nodesCreated technology nodes")
            
            // Phase 2: 관계 생성 (노드가 존재하는 경우만)
            for (resource in resources) {
                try {
                    val content = resource.inputStream.bufferedReader().readText()
                    val seedData: Map<String, Any> = objectMapper.readValue(content)
                    
                    val techName = seedData["technology"] as? String ?: continue
                    
                    @Suppress("UNCHECKED_CAST")
                    val relations = seedData["relations"] as? List<Map<String, String>> ?: emptyList()
                    
                    for (rel in relations) {
                        val toTech = rel["to"] ?: continue
                        val relType = rel["type"] ?: "DEPENDS_ON"
                        
                        // 대상 기술이 시드 데이터에 존재하는지 검증
                        if (!availableTechnologies.contains(toTech)) {
                            log.warn("Skipping relation $techName -> $toTech: target technology has no seed file")
                            skippedRelations++
                            continue
                        }
                        
                        // 관계 생성
                        session.run("""
                            MATCH (from:Technology {name: ${"$"}fromName})
                            MATCH (to:Technology {name: ${"$"}toName})
                            MERGE (from)-[:${relType}]->(to)
                        """, mapOf(
                            "fromName" to techName,
                            "toName" to toTech
                        ))
                        edgesCreated++
                    }
                    
                    log.debug("Loaded: $techName with ${relations.size} relations")
                    
                } catch (e: Exception) {
                    log.warn("Failed to load relations from ${resource.filename}: ${e.message}")
                }
            }
        }
        
        log.info("✅ Graph seed completed: $nodesCreated nodes, $edgesCreated edges")
        if (skippedRelations > 0) {
            log.warn("⚠️  Skipped $skippedRelations relations due to missing target technologies")
        }
    }
    
    /**
     * 그래프 데이터 초기화 (개발/테스트용)
     */
    fun clearGraphData() {
        neo4jDriver.session().use { session ->
            session.run("MATCH (n:Technology) DETACH DELETE n")
            log.info("Cleared all Technology nodes from Neo4j")
        }
    }
}

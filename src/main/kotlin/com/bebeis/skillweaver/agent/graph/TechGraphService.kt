package com.bebeis.skillweaver.agent.graph

import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * V3 GraphRAG Service
 * 
 * Neo4j를 사용하여 기술 간 관계를 그래프로 관리합니다.
 * 선행 기술 조회, 학습 경로 탐색 등을 지원합니다.
 */
@Service
@ConditionalOnProperty(name = ["skillweaver.graph.enabled"], havingValue = "true")
@ConditionalOnBean(Driver::class)
class TechGraphService(
    private val neo4jDriver: Driver
) {
    private val log = LoggerFactory.getLogger(TechGraphService::class.java)

    /**
     * 특정 기술의 선행 지식(prerequisites)을 조회합니다.
     */
    fun findPrerequisites(technology: String): Prerequisites {
        log.info("Finding prerequisites for: $technology")
        
        neo4jDriver.session().use { session ->
            // 필수 선행 지식 (DEPENDS_ON 관계)
            val requiredResult = session.run("""
                MATCH (t:Technology {name: ${"$"}tech})-[:DEPENDS_ON*1..3]->(prereq:Technology)
                RETURN DISTINCT prereq.name AS name, prereq.displayName AS displayName, 
                       prereq.category AS category, prereq.difficulty AS difficulty
                ORDER BY prereq.difficulty
            """, mapOf("tech" to technology))
            
            val required = requiredResult.list().map { record ->
                val name = record["name"].asString()
                TechNode(
                    name = name,
                    displayName = record["displayName"].asString(),
                    category = record["category"]?.takeIf { !it.isNull }?.let { TechCategory.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'category' in Neo4j"),
                    difficulty = record["difficulty"]?.takeIf { !it.isNull }?.let { Difficulty.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'difficulty' in Neo4j")
                )
            }
            
            // 권장 선행 지식 (RECOMMENDED_AFTER의 역관계)
            val recommendedResult = session.run("""
                MATCH (t:Technology {name: ${"$"}tech})<-[:RECOMMENDED_AFTER]-(prereq:Technology)
                RETURN prereq.name AS name, prereq.displayName AS displayName,
                       prereq.category AS category, prereq.difficulty AS difficulty
            """, mapOf("tech" to technology))
            
            val recommended = recommendedResult.list().map { record ->
                val name = record["name"].asString()
                TechNode(
                    name = name,
                    displayName = record["displayName"].asString(),
                    category = record["category"]?.takeIf { !it.isNull }?.let { TechCategory.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'category' in Neo4j"),
                    difficulty = record["difficulty"]?.takeIf { !it.isNull }?.let { Difficulty.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'difficulty' in Neo4j")
                )
            }
            
            return Prerequisites(
                technology = technology,
                required = required,
                recommended = recommended
            )
        }
    }

    /**
     * 두 기술 간의 최단 학습 경로를 찾습니다.
     */
    fun findLearningPath(from: String, to: String): LearningPath? {
        log.info("Finding learning path from $from to $to")
        
        neo4jDriver.session().use { session ->
            val result = session.run("""
                MATCH path = shortestPath(
                    (start:Technology {name: ${"$"}from})-[*..6]->(end:Technology {name: ${"$"}to})
                )
                RETURN nodes(path) AS nodes, relationships(path) AS rels
            """, mapOf("from" to from, "to" to to))
            
            if (!result.hasNext()) {
                log.warn("No path found from $from to $to")
                return null
            }
            
            val record = result.single()
            val nodes = record["nodes"].asList { it.asNode() }
            val rels = record["rels"].asList { it.asRelationship() }
            
            val steps = rels.mapIndexed { index, rel ->
                PathStep(
                    technology = nodes[index + 1]["name"].asString(),
                    relation = TechRelation.valueOf(rel.type()),
                    estimatedDuration = null
                )
            }
            
            return LearningPath(
                from = from,
                to = to,
                steps = steps,
                totalDuration = null
            )
        }
    }

    /**
     * 특정 기술과 함께 자주 사용되는 기술을 조회합니다.
     */
    fun findRelatedTechnologies(technology: String): List<TechNode> {
        log.info("Finding related technologies for: $technology")
        
        neo4jDriver.session().use { session ->
            val result = session.run("""
                MATCH (t:Technology {name: ${"$"}tech})-[:USED_WITH|CONTAINS|EXTENDS]-(related:Technology)
                RETURN DISTINCT related.name AS name, related.displayName AS displayName,
                       related.category AS category, related.difficulty AS difficulty
            """, mapOf("tech" to technology))
            
            return result.list().map { record ->
                val name = record["name"].asString()
                TechNode(
                    name = name,
                    displayName = record["displayName"].asString(),
                    category = record["category"]?.takeIf { !it.isNull }?.let { TechCategory.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'category' in Neo4j"),
                    difficulty = record["difficulty"]?.takeIf { !it.isNull }?.let { Difficulty.valueOf(it.asString()) }
                        ?: throw IllegalStateException("Technology '$name' is missing 'difficulty' in Neo4j")
                )
            }
        }
    }

    /**
     * 기술 그래프에 새로운 노드와 관계를 추가합니다.
     */
    fun addTechnologyWithRelations(
        tech: TechNode,
        relations: List<TechEdge>
    ) {
        neo4jDriver.session().use { session ->
            // 노드 생성
            session.run("""
                MERGE (t:Technology {name: ${"$"}name})
                SET t.displayName = ${"$"}displayName,
                    t.category = ${"$"}category,
                    t.difficulty = ${"$"}difficulty,
                    t.description = ${"$"}description
            """, mapOf(
                "name" to tech.name,
                "displayName" to tech.displayName,
                "category" to tech.category.name,
                "difficulty" to tech.difficulty.name,
                "description" to tech.description
            ))
            
            // 관계 생성
            relations.forEach { edge ->
                session.run("""
                    MATCH (from:Technology {name: ${"$"}fromName})
                    MATCH (to:Technology {name: ${"$"}toName})
                    MERGE (from)-[r:${edge.relation.name}]->(to)
                    SET r.weight = ${"$"}weight
                """, mapOf(
                    "fromName" to edge.from,
                    "toName" to edge.to,
                    "weight" to edge.weight
                ))
            }
        }
    }

    /**
     * 특정 기술이 그래프에 존재하는지 확인합니다.
     */
    fun existsTechnology(technology: String): Boolean {
        neo4jDriver.session().use { session ->
            val result = session.run("""
                MATCH (t:Technology {name: ${"$"}tech})
                RETURN count(t) > 0 AS exists
            """, mapOf("tech" to technology))
            
            return result.single()["exists"].asBoolean()
        }
    }
    
    // =====================================================================
    // V4 CRUD 메서드
    // =====================================================================
    
    /**
     * 기술을 이름으로 조회합니다.
     */
    fun findByName(name: String): TechNode? {
        log.debug("Finding technology by name: $name")
        
        neo4jDriver.session().use { session ->
            val result = session.run("""
                MATCH (t:Technology {name: ${"$"}name})
                RETURN t
            """, mapOf("name" to name))
            
            if (!result.hasNext()) {
                return null
            }
            
            val node = result.single()["t"].asNode()
            return nodeToTechNode(node)
        }
    }
    
    /**
     * 기술 목록을 필터와 함께 조회합니다.
     */
    fun findAll(
        category: TechCategory? = null,
        active: Boolean? = null,
        search: String? = null,
        limit: Int = 100
    ): List<TechNode> {
        log.debug("Finding all technologies with filters - category: $category, active: $active, search: $search")
        
        neo4jDriver.session().use { session ->
            val conditions = mutableListOf<String>()
            val params = mutableMapOf<String, Any>()
            
            category?.let {
                conditions.add("t.category = \$category")
                params["category"] = it.name
            }
            active?.let {
                conditions.add("t.active = \$active")
                params["active"] = it
            }
            search?.let {
                conditions.add("(toLower(t.name) CONTAINS toLower(\$search) OR toLower(t.displayName) CONTAINS toLower(\$search))")
                params["search"] = it
            }
            
            val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
            params["limit"] = limit
            
            val result = session.run("""
                MATCH (t:Technology)
                $whereClause
                RETURN t
                ORDER BY t.displayName
                LIMIT ${"$"}limit
            """, params)
            
            return result.list().map { record ->
                nodeToTechNode(record["t"].asNode())
            }
        }
    }
    
    /**
     * 새로운 기술을 생성합니다.
     */
    fun createTechnology(tech: TechNode): TechNode {
        log.info("Creating technology: ${tech.name}")
        
        neo4jDriver.session().use { session ->
            session.run("""
                CREATE (t:Technology {
                    name: ${"$"}name,
                    displayName: ${"$"}displayName,
                    category: ${"$"}category,
                    difficulty: ${"$"}difficulty,
                    ecosystem: ${"$"}ecosystem,
                    officialSite: ${"$"}officialSite,
                    active: ${"$"}active,
                    learningRoadmap: ${"$"}learningRoadmap,
                    estimatedLearningHours: ${"$"}estimatedLearningHours,
                    communityPopularity: ${"$"}communityPopularity,
                    jobMarketDemand: ${"$"}jobMarketDemand,
                    description: ${"$"}description,
                    learningTips: ${"$"}learningTips,
                    useCases: ${"$"}useCases
                })
            """, techNodeToParams(tech))
        }
        
        return tech
    }
    
    /**
     * 기존 기술을 업데이트합니다.
     */
    fun updateTechnology(name: String, update: TechNodeUpdate): TechNode? {
        log.info("Updating technology: $name")
        
        neo4jDriver.session().use { session ->
            val setStatements = mutableListOf<String>()
            val params = mutableMapOf<String, Any>("name" to name)
            
            update.displayName?.let { setStatements.add("t.displayName = \$displayName"); params["displayName"] = it }
            update.category?.let { setStatements.add("t.category = \$category"); params["category"] = it.name }
            update.difficulty?.let { setStatements.add("t.difficulty = \$difficulty"); params["difficulty"] = it.name }
            update.ecosystem?.let { setStatements.add("t.ecosystem = \$ecosystem"); params["ecosystem"] = it }
            update.officialSite?.let { setStatements.add("t.officialSite = \$officialSite"); params["officialSite"] = it }
            update.active?.let { setStatements.add("t.active = \$active"); params["active"] = it }
            update.learningRoadmap?.let { setStatements.add("t.learningRoadmap = \$learningRoadmap"); params["learningRoadmap"] = it }
            update.estimatedLearningHours?.let { setStatements.add("t.estimatedLearningHours = \$estimatedLearningHours"); params["estimatedLearningHours"] = it }
            update.communityPopularity?.let { setStatements.add("t.communityPopularity = \$communityPopularity"); params["communityPopularity"] = it }
            update.jobMarketDemand?.let { setStatements.add("t.jobMarketDemand = \$jobMarketDemand"); params["jobMarketDemand"] = it }
            update.description?.let { setStatements.add("t.description = \$description"); params["description"] = it }
            update.learningTips?.let { setStatements.add("t.learningTips = \$learningTips"); params["learningTips"] = it }
            update.useCases?.let { setStatements.add("t.useCases = \$useCases"); params["useCases"] = it }
            
            if (setStatements.isEmpty()) {
                return findByName(name)
            }
            
            session.run("""
                MATCH (t:Technology {name: ${"$"}name})
                SET ${setStatements.joinToString(", ")}
                RETURN t
            """, params)
        }
        
        return findByName(name)
    }
    
    /**
     * 기술을 삭제합니다.
     */
    fun deleteTechnology(name: String): Boolean {
        log.info("Deleting technology: $name")
        
        neo4jDriver.session().use { session ->
            val result = session.run("""
                MATCH (t:Technology {name: ${"$"}name})
                DETACH DELETE t
                RETURN count(t) AS deleted
            """, mapOf("name" to name))
            
            return result.single()["deleted"].asLong() > 0
        }
    }
    
    /**
     * 특정 기술의 관계를 조회합니다.
     */
    fun findRelationships(from: String, relationType: TechRelation? = null): List<TechEdge> {
        log.debug("Finding relationships from: $from, type: $relationType")
        
        neo4jDriver.session().use { session ->
            val relationPattern = relationType?.let { "[:${it.name}]" } ?: "[r]"
            
            val result = session.run("""
                MATCH (from:Technology {name: ${"$"}from})-$relationPattern->(to:Technology)
                RETURN from.name AS fromName, to.name AS toName, type(r) AS relType,
                       COALESCE(r.weight, 1.0) AS weight
            """, mapOf("from" to from))
            
            return result.list().map { record ->
                TechEdge(
                    from = record["fromName"].asString(),
                    to = record["toName"].asString(),
                    relation = TechRelation.valueOf(record["relType"].asString()),
                    weight = record["weight"].asDouble()
                )
            }
        }
    }
    
    /**
     * 관계를 생성합니다.
     */
    fun createRelationship(edge: TechEdge): TechEdge {
        log.info("Creating relationship: ${edge.from} -[${edge.relation}]-> ${edge.to}")
        
        neo4jDriver.session().use { session ->
            session.run("""
                MATCH (from:Technology {name: ${"$"}from})
                MATCH (to:Technology {name: ${"$"}to})
                MERGE (from)-[r:${edge.relation.name}]->(to)
                SET r.weight = ${"$"}weight
            """, mapOf(
                "from" to edge.from,
                "to" to edge.to,
                "weight" to edge.weight
            ))
        }
        
        return edge
    }
    
    /**
     * 관계를 삭제합니다.
     */
    fun deleteRelationship(from: String, to: String, relationType: TechRelation): Boolean {
        log.info("Deleting relationship: $from -[$relationType]-> $to")
        
        neo4jDriver.session().use { session ->
            session.run("""
                MATCH (from:Technology {name: ${"$"}from})-[r:${relationType.name}]->(to:Technology {name: ${"$"}to})
                DELETE r
            """, mapOf("from" to from, "to" to to))
            
            return true
        }
    }
    
    // =====================================================================
    // Private Helper Methods
    // =====================================================================
    
    private fun nodeToTechNode(node: org.neo4j.driver.types.Node): TechNode {
        return TechNode(
            name = node["name"].asString(),
            displayName = node["displayName"].asString(),
            category = node["category"]?.takeIf { !it.isNull }?.let { TechCategory.valueOf(it.asString()) }
                ?: throw IllegalStateException("Technology '${node["name"].asString()}' is missing 'category' in Neo4j"),
            difficulty = node["difficulty"]?.takeIf { !it.isNull }?.let { Difficulty.valueOf(it.asString()) } 
                ?: Difficulty.INTERMEDIATE,
            ecosystem = node["ecosystem"]?.takeIf { !it.isNull }?.asString(),
            officialSite = node["officialSite"]?.takeIf { !it.isNull }?.asString(),
            active = node["active"]?.takeIf { !it.isNull }?.asBoolean() ?: true,
            learningRoadmap = node["learningRoadmap"]?.takeIf { !it.isNull }?.asString(),
            estimatedLearningHours = node["estimatedLearningHours"]?.takeIf { !it.isNull }?.asInt(),
            communityPopularity = node["communityPopularity"]?.takeIf { !it.isNull }?.asInt(),
            jobMarketDemand = node["jobMarketDemand"]?.takeIf { !it.isNull }?.asInt(),
            description = node["description"]?.takeIf { !it.isNull }?.asString(),
            learningTips = node["learningTips"]?.takeIf { !it.isNull }?.asString(),
            useCases = node["useCases"]?.takeIf { !it.isNull }?.asList { it.asString() } ?: emptyList()
        )
    }
    
    private fun techNodeToParams(tech: TechNode): Map<String, Any?> {
        return mapOf(
            "name" to tech.name,
            "displayName" to tech.displayName,
            "category" to tech.category.name,
            "difficulty" to tech.difficulty.name,
            "ecosystem" to tech.ecosystem,
            "officialSite" to tech.officialSite,
            "active" to tech.active,
            "learningRoadmap" to tech.learningRoadmap,
            "estimatedLearningHours" to tech.estimatedLearningHours,
            "communityPopularity" to tech.communityPopularity,
            "jobMarketDemand" to tech.jobMarketDemand,
            "description" to tech.description,
            "learningTips" to tech.learningTips,
            "useCases" to tech.useCases
        )
    }
}


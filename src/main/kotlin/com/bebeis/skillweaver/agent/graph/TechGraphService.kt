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
}

package com.bebeis.skillweaver.agent.config

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Neo4j Driver 설정 - GraphRAG를 위한 Neo4j AuraDB 연결
 * 
 * skillweaver.graph.enabled=true 일 때만 빈이 등록됩니다.
 * 
 * 로컬 개발: docker-compose up -d neo4j
 * 클라우드: NEO4J_URI, NEO4J_USERNAME, NEO4J_PASSWORD 환경변수 설정
 */
@Configuration
@ConditionalOnProperty(name = ["skillweaver.graph.enabled"], havingValue = "true")
class Neo4jConfig(
    @Value("\${skillweaver.graph.neo4j.uri}")
    private val uri: String,
    
    @Value("\${skillweaver.graph.neo4j.username}")
    private val username: String,
    
    @Value("\${skillweaver.graph.neo4j.password}")
    private val password: String
) {
    private val log = LoggerFactory.getLogger(Neo4jConfig::class.java)

    @Bean
    fun neo4jDriver(): Driver {
        log.info("Connecting to Neo4j at: $uri")
        
        val driver = GraphDatabase.driver(
            uri,
            AuthTokens.basic(username, password)
        )
        
        // 연결 확인
        driver.verifyConnectivity()
        log.info("Neo4j connection established successfully")
        
        return driver
    }
}

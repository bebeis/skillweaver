package com.bebeis.skillweaver.agent.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class KnowledgeSeedRunner(
    private val knowledgeIngestionService: KnowledgeIngestionService,
    private val objectMapper: ObjectMapper
) : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(KnowledgeSeedRunner::class.java)
    private val resourceResolver = PathMatchingResourcePatternResolver()
    
    override fun run(vararg args: String?) {
        val shouldSeed = args.any { it == "--seed-knowledge" } || 
                        System.getProperty("spring.profiles.active")?.contains("dev") == true
        
        if (!shouldSeed) {
            logger.info("Knowledge seeding skipped. Use --seed-knowledge to seed manually.")
            return
        }
        
        try {
            seedKnowledgeBase()
        } catch (e: Exception) {
            logger.error("Failed to seed knowledge base", e)
        }
    }
    
    private fun seedKnowledgeBase() {
        logger.info("Starting knowledge base seeding...")
        
        val resources: Array<Resource> = resourceResolver.getResources("classpath:knowledge-seed/*.json")
        
        if (resources.isEmpty()) {
            logger.warn("No seed files found in classpath:knowledge-seed/")
            return
        }
        
        var totalDocuments = 0
        var successCount = 0
        
        resources.forEach { resource ->
            try {
                logger.info("Processing seed file: ${resource.filename}")
                val seedData: KnowledgeSeedData = objectMapper.readValue(resource.inputStream)
                
                seedData.documents.forEach { doc ->
                    try {
                        when (doc.type) {
                            "ROADMAP" -> {
                                knowledgeIngestionService.ingestTechRoadmap(
                                    technology = seedData.technology,
                                    content = doc.content,
                                    source = doc.source
                                )
                            }
                            "BEST_PRACTICE" -> {
                                knowledgeIngestionService.ingestLearningResource(
                                    technology = seedData.technology,
                                    title = "${seedData.displayName} Best Practices",
                                    content = doc.content,
                                    url = doc.source
                                )
                            }
                            "COMMUNITY_INSIGHT" -> {
                                knowledgeIngestionService.ingestLearningResource(
                                    technology = seedData.technology,
                                    title = "${seedData.displayName} Community Insights",
                                    content = doc.content,
                                    url = doc.source
                                )
                            }
                            else -> {
                                knowledgeIngestionService.ingestLearningResource(
                                    technology = seedData.technology,
                                    title = "${seedData.displayName} - ${doc.type}",
                                    content = doc.content,
                                    url = doc.source
                                )
                            }
                        }
                        successCount++
                        totalDocuments++
                    } catch (e: Exception) {
                        logger.error("Failed to ingest document: ${doc.type} for ${seedData.technology}", e)
                        totalDocuments++
                    }
                }
                
                logger.info("Completed seeding: ${resource.filename} (${seedData.documents.size} documents)")
                
            } catch (e: Exception) {
                logger.error("Failed to process seed file: ${resource.filename}", e)
            }
        }
        
        logger.info("Knowledge base seeding completed: $successCount/$totalDocuments documents ingested successfully")
    }
}

data class KnowledgeSeedData(
    val technology: String,
    val displayName: String,
    val documents: List<KnowledgeDocumentSeed>
)

data class KnowledgeDocumentSeed(
    val type: String,
    val source: String,
    val content: String
)

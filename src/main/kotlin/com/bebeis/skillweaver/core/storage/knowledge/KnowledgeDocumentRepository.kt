package com.bebeis.skillweaver.core.storage.knowledge

import com.bebeis.skillweaver.core.domain.knowledge.KnowledgeDocument
import com.bebeis.skillweaver.core.domain.knowledge.KnowledgeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeDocumentRepository : JpaRepository<KnowledgeDocument, Long> {
    
    fun findByTechnology(technology: String): List<KnowledgeDocument>
    
    fun findByTechnologyAndDocumentType(
        technology: String, 
        documentType: KnowledgeType
    ): List<KnowledgeDocument>
    
    fun findByDocumentType(documentType: KnowledgeType): List<KnowledgeDocument>
    
    fun findByVectorIdIsNotNull(): List<KnowledgeDocument>
    
    fun existsByTechnologyAndContentHash(technology: String, contentHash: String): Boolean
}

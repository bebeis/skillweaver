package com.bebeis.skillweaver.core.domain.technology

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table

@Entity
@Table(name = "technology_knowledge")
class TechnologyKnowledge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technology_knowledge_id")
    val technologyKnowledgeId: Long? = null,

    @Column(name = "technology_id", nullable = false, unique = true)
    val technologyId: Long,

    @Lob
    @Column(columnDefinition = "TEXT")
    val summary: String? = null,

    @Lob
    @Column(name = "learning_tips", columnDefinition = "TEXT")
    val learningTips: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    val sourceType: KnowledgeSource = KnowledgeSource.COMMUNITY
)

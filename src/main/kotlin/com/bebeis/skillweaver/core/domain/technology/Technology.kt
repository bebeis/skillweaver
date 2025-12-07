package com.bebeis.skillweaver.core.domain.technology

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "technology")
class Technology(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technology_id")
    val technologyId: Long? = null,

    @Column(name = "`key`", nullable = false, unique = true, length = 100)
    val key: String,

    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val category: TechnologyCategory,

    @Column(length = 100)
    val ecosystem: String? = null,

    @Column(name = "official_site", length = 255)
    val officialSite: String? = null,

    @Column(nullable = false)
    val active: Boolean = true,

    // ===== Phase 3: 학습 메타데이터 =====
    
    @Column(name = "learning_roadmap", columnDefinition = "TEXT")
    val learningRoadmap: String? = null,

    @Column(name = "estimated_learning_hours")
    val estimatedLearningHours: Int? = null,

    @ElementCollection
    @CollectionTable(
        name = "technology_prerequisites",
        joinColumns = [JoinColumn(name = "technology_id")]
    )
    @Column(name = "prerequisite_key", length = 100)
    val prerequisites: List<String> = emptyList(),

    @ElementCollection
    @CollectionTable(
        name = "technology_related",
        joinColumns = [JoinColumn(name = "technology_id")]
    )
    @Column(name = "related_technology_key", length = 100)
    val relatedTechnologies: List<String> = emptyList(),

    @Column(name = "community_popularity")
    val communityPopularity: Int? = null,  // 1-10

    @Column(name = "job_market_demand")
    val jobMarketDemand: Int? = null       // 1-10
) : BaseEntity()


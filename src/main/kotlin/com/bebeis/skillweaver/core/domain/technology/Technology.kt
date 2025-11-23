package com.bebeis.skillweaver.core.domain.technology

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
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
    val active: Boolean = true
)

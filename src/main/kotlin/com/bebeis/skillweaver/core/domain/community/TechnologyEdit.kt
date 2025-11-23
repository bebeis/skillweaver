package com.bebeis.skillweaver.core.domain.community

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "technology_edit")
class TechnologyEdit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technology_edit_id")
    val technologyEditId: Long? = null,

    @Column(name = "technology_id", nullable = false)
    val technologyId: Long,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Lob
    @Column(name = "proposed_summary", columnDefinition = "TEXT")
    val proposedSummary: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: EditStatus = EditStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

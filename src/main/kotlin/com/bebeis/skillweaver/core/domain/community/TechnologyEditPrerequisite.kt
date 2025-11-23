package com.bebeis.skillweaver.core.domain.community

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "technology_edit_prerequisite")
class TechnologyEditPrerequisite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technology_edit_prerequisite_id")
    val technologyEditPrerequisiteId: Long? = null,

    @Column(name = "edit_id", nullable = false)
    val editId: Long,

    @Column(name = "prerequisite", nullable = false, length = 100)
    val prerequisite: String
)

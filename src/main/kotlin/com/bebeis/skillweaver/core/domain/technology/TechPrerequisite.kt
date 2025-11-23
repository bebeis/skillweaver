package com.bebeis.skillweaver.core.domain.technology

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tech_prerequisite")
class TechPrerequisite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_prerequisite_id")
    val techPrerequisiteId: Long? = null,

    @Column(name = "knowledge_id", nullable = false)
    val knowledgeId: Long,

    @Column(name = "prerequisite_key", nullable = false, length = 100)
    val prerequisiteKey: String
)

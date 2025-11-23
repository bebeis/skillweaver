package com.bebeis.skillweaver.core.domain.technology

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tech_use_case")
class TechUseCase(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_use_case_id")
    val techUseCaseId: Long? = null,

    @Column(name = "knowledge_id", nullable = false)
    val knowledgeId: Long,

    @Column(name = "use_case", nullable = false, length = 255)
    val useCase: String
)

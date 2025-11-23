package com.bebeis.skillweaver.core.domain.technology

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "tech_relationship",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tech_relationship_from_to_type",
            columnNames = ["from_id", "to_id", "relation_type"]
        )
    ]
)
class TechRelationship(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_relationship_id")
    val techRelationshipId: Long? = null,

    @Column(name = "from_id", nullable = false)
    val fromId: Long,

    @Column(name = "to_id", nullable = false)
    val toId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    val relationType: RelationType,

    @Column(nullable = false)
    val weight: Int = 1
) {
    init {
        require(weight in 1..5) {
            "weight는 1~5 사이의 값이어야 합니다."
        }
        require(fromId != toId) {
            "fromId와 toId는 같을 수 없습니다."
        }
    }
}

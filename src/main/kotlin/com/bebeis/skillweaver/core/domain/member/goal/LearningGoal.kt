package com.bebeis.skillweaver.core.domain.member.goal

import com.bebeis.skillweaver.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDate


@Entity
@Table(name = "learning_goal")
class LearningGoal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_goal_id")
    val learningGoalId: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "due_date")
    val dueDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val priority: GoalPriority = GoalPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: GoalStatus = GoalStatus.ACTIVE
) : BaseEntity() {
    init {
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다." }
        require(description.isNotBlank()) { "설명은 비어있을 수 없습니다." }
    }

    fun isActive(): Boolean {
        return dueDate == null || !dueDate!!.isBefore(LocalDate.now())
    }
}

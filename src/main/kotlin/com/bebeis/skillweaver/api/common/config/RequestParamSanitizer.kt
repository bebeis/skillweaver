package com.bebeis.skillweaver.api.common.config

import com.bebeis.skillweaver.core.domain.learning.LearningPlanStatus
import com.bebeis.skillweaver.core.domain.member.goal.GoalPriority
import com.bebeis.skillweaver.core.domain.member.goal.GoalStatus
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.InitBinder
import java.beans.PropertyEditorSupport

@ControllerAdvice
class RequestParamSanitizer {

    @InitBinder
    fun sanitizeEnums(binder: WebDataBinder) {
        binder.registerCustomEditor(
            GoalStatus::class.java,
            NullIfUndefinedEnumEditor(GoalStatus::class.java)
        )
        binder.registerCustomEditor(
            GoalPriority::class.java,
            NullIfUndefinedEnumEditor(GoalPriority::class.java)
        )
        binder.registerCustomEditor(
            LearningPlanStatus::class.java,
            NullIfUndefinedEnumEditor(LearningPlanStatus::class.java)
        )
    }

    private class NullIfUndefinedEnumEditor<T : Enum<T>>(
        private val enumType: Class<T>
    ) : PropertyEditorSupport() {

        override fun setAsText(text: String?) {
            if (text.isNullOrBlank() || text.equals("undefined", ignoreCase = true)) {
                value = null
                return
            }

            val normalized = text.trim()
            val match = enumType.enumConstants.firstOrNull {
                it.name.equals(normalized, ignoreCase = true)
            } ?: throw IllegalArgumentException("No enum constant ${enumType.name}.$normalized")
            value = match
        }
    }
}

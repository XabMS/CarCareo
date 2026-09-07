package com.xabier.carcareo.ui.plan

import android.os.Parcelable
import com.xabier.carcareo.data.entity.MaintenanceTask
import kotlinx.parcelize.Parcelize

/**
 * Editable state for one task in the plan editor's bottom sheet. Numbers are kept
 * as strings so partial input survives; parsing happens on save. [Parcelize] so
 * an in-progress edit survives a screen rotation (rememberSaveable).
 */
@Parcelize
data class TaskDraft(
    val id: Long? = null,
    val name: String = "",
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val warnKmBefore: String = DEFAULT_WARN_KM.toString(),
    val warnDaysBefore: String = DEFAULT_WARN_DAYS.toString(),
    val notes: String = "",
    val active: Boolean = true,
    val nameError: Boolean = false,
    /** Another task on the same vehicle already uses this name. */
    val duplicateNameError: Boolean = false,
    val intervalError: Boolean = false,
) : Parcelable {
    val isEdit: Boolean get() = id != null

    val hasError: Boolean get() = nameError || duplicateNameError || intervalError

    companion object {
        const val DEFAULT_WARN_KM = 1_000
        const val DEFAULT_WARN_DAYS = 30

        fun from(task: MaintenanceTask) = TaskDraft(
            id = task.id,
            name = task.name,
            intervalKm = task.intervalKm?.toString().orEmpty(),
            intervalMonths = task.intervalMonths?.toString().orEmpty(),
            warnKmBefore = task.warnKmBefore.toString(),
            warnDaysBefore = task.warnDaysBefore.toString(),
            notes = task.notes.orEmpty(),
            active = task.active,
        )
    }
}

package com.xabier.carcareo.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.MaintenanceTaskRules
import com.xabier.carcareo.ui.navigation.Destinations
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanEditorUiState(
    val loading: Boolean = true,
    val vehicle: Vehicle? = null,
    val tasks: List<MaintenanceTask> = emptyList(),
)

class PlanEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: MaintenanceTaskRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<PlanEditorUiState> =
        combine(
            vehicleRepository.observe(vehicleId),
            taskRepository.observeForVehicle(vehicleId),
        ) { vehicle, tasks ->
            PlanEditorUiState(loading = false, vehicle = vehicle, tasks = tasks)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanEditorUiState(loading = true),
        )

    /**
     * Validates [draft] and persists it. Returns true on success; on failure the
     * caller re-renders the draft with its error flags set.
     */
    fun saveTask(draft: TaskDraft, onResult: (TaskDraft) -> Unit) {
        val name = draft.name.trim()
        val km = draft.intervalKm.trim().toIntOrNull()
        val months = draft.intervalMonths.trim().toIntOrNull()

        val nameError = name.isEmpty()
        // Record<->task links are serialized by task name within the vehicle
        // (spec 7), so two tasks sharing a name would make a backup ambiguous and
        // silently merge their history on import. Refuse it at the source.
        val duplicateNameError = !nameError && uiState.value.tasks.any {
            it.id != draft.id && it.name.equals(name, ignoreCase = true)
        }
        val intervalError = !MaintenanceTaskRules.intervalsValid(km, months)
        if (nameError || duplicateNameError || intervalError) {
            onResult(
                draft.copy(
                    nameError = nameError,
                    duplicateNameError = duplicateNameError,
                    intervalError = intervalError,
                ),
            )
            return
        }

        val warnKm = draft.warnKmBefore.trim().toIntOrNull()?.coerceAtLeast(0)
            ?: TaskDraft.DEFAULT_WARN_KM
        val warnDays = draft.warnDaysBefore.trim().toIntOrNull()?.coerceAtLeast(0)
            ?: TaskDraft.DEFAULT_WARN_DAYS

        viewModelScope.launch {
            if (draft.id == null) {
                taskRepository.add(
                    MaintenanceTask(
                        vehicleId = vehicleId,
                        name = name,
                        intervalKm = km,
                        intervalMonths = months,
                        warnKmBefore = warnKm,
                        warnDaysBefore = warnDays,
                        notes = draft.notes.trim().ifBlank { null },
                        active = draft.active,
                    ),
                )
            } else {
                val existing = uiState.value.tasks.firstOrNull { it.id == draft.id } ?: return@launch
                taskRepository.update(
                    existing.copy(
                        name = name,
                        intervalKm = km,
                        intervalMonths = months,
                        warnKmBefore = warnKm,
                        warnDaysBefore = warnDays,
                        notes = draft.notes.trim().ifBlank { null },
                        active = draft.active,
                    ),
                )
            }
            onResult(
                draft.copy(
                    nameError = false,
                    duplicateNameError = false,
                    intervalError = false,
                ),
            )
        }
    }

    fun deleteTask(task: MaintenanceTask) {
        viewModelScope.launch { taskRepository.delete(task) }
    }

    fun toggleActive(task: MaintenanceTask) {
        viewModelScope.launch { taskRepository.setActive(task, !task.active) }
    }

    fun move(task: MaintenanceTask, up: Boolean) {
        val ordered = uiState.value.tasks
        val index = ordered.indexOfFirst { it.id == task.id }
        if (index < 0) return
        val swapWith = if (up) index - 1 else index + 1
        val other = ordered.getOrNull(swapWith) ?: return
        viewModelScope.launch { taskRepository.swapOrder(task, other) }
    }

    /** Persists a drag-to-reorder: [orderedTasks] in their new visual order. */
    fun persistOrder(orderedTasks: List<MaintenanceTask>) {
        viewModelScope.launch { taskRepository.persistOrder(orderedTasks) }
    }

    /**
     * Applies the category template. [taskRepository.applyTemplate] already skips
     * names the plan has, so this both seeds an empty plan and "tops up" a
     * partial one with the tasks it is missing.
     */
    fun applyTemplate(spanish: Boolean) {
        val category = uiState.value.vehicle?.category ?: return
        viewModelScope.launch {
            taskRepository.applyTemplate(vehicleId, category, spanish)
        }
    }
}

package com.xabier.carcareo.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.PlanStatusCalculator
import com.xabier.carcareo.domain.TaskComputation
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.ui.navigation.Destinations
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One selectable task row on the log screen. */
data class LogTaskRow(
    val taskId: Long,
    val name: String,
    val computation: TaskComputation?,
)

data class LogMaintenanceUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val odometer: String = "",
    val lastConfirmedKm: Int = 0,
    val tasks: List<LogTaskRow> = emptyList(),
    val selectedTaskIds: Set<Long> = emptySet(),
    val workshop: String = "",
    val cost: String = "",
    val notes: String = "",
    val attachmentUri: String? = null,
    val extrasExpanded: Boolean = false,
    val odometerError: Boolean = false,
)

class LogMaintenanceViewModel(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val taskRepository: MaintenanceTaskRepository,
    private val recordRepository: MaintenanceRecordRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    // Present only on the edit route (log/{vehicleId}/{recordId}).
    private val recordId: Long? = savedStateHandle.get<Long>(Destinations.RECORD_ID_ARG)
        ?.takeIf { it > 0 }

    private val today: LocalDate = LocalDate.now()

    private val _ui = MutableStateFlow(LogMaintenanceUiState())
    val ui: StateFlow<LogMaintenanceUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val vehicle = vehicleRepository.get(vehicleId) ?: return@launch
            val tasks = taskRepository.getForVehicle(vehicleId)
            val records = recordRepository.observeForVehicle(vehicleId).first()
            val status = PlanStatusCalculator.forVehicle(vehicle, tasks, records, today)

            val activeRows = status.activeOrdered.map {
                LogTaskRow(it.task.id, it.task.name, it.computation)
            }

            val existing = recordId?.let { recordRepository.getWithTasks(it) }
            if (existing != null) {
                // Edit: prefill from the record, select the tasks it actually covers
                // (including any that are now inactive, which have no status row).
                val linkedIds = existing.tasks.map { it.id }.toSet()
                val activeIds = activeRows.mapTo(HashSet(), LogTaskRow::taskId)
                val extraRows = existing.tasks
                    .filter { it.id !in activeIds }
                    .map { LogTaskRow(it.id, it.name, computation = null) }
                _ui.update {
                    it.copy(
                        loading = false,
                        isEdit = true,
                        date = existing.record.date,
                        odometer = existing.record.odometerKm.toString(),
                        lastConfirmedKm = vehicle.lastConfirmedKm,
                        tasks = activeRows + extraRows,
                        selectedTaskIds = linkedIds,
                        workshop = existing.record.workshop.orEmpty(),
                        cost = existing.record.cost?.toPlainString().orEmpty(),
                        notes = existing.record.notes.orEmpty(),
                        attachmentUri = existing.record.attachmentUri,
                        extrasExpanded = existing.record.workshop != null ||
                            existing.record.cost != null ||
                            existing.record.notes != null ||
                            existing.record.attachmentUri != null,
                    )
                }
            } else {
                _ui.update {
                    it.copy(
                        loading = false,
                        odometer = status.currentKm.toString(),
                        lastConfirmedKm = vehicle.lastConfirmedKm,
                        tasks = activeRows,
                        // Pre-mark everything overdue or upcoming (spec P3).
                        selectedTaskIds = activeRows
                            .filter { r -> r.computation?.status != TaskStatus.OK }
                            .map { r -> r.taskId }
                            .toSet(),
                    )
                }
            }
        }
    }

    fun setDate(date: LocalDate) = _ui.update { it.copy(date = date) }

    fun setOdometer(value: String) =
        _ui.update { it.copy(odometer = value.filter(Char::isDigit), odometerError = false) }

    fun setWorkshop(value: String) = _ui.update { it.copy(workshop = value) }

    fun setCost(value: String) =
        _ui.update { it.copy(cost = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }

    fun setNotes(value: String) = _ui.update { it.copy(notes = value) }

    fun setAttachment(uri: String?) = _ui.update { it.copy(attachmentUri = uri) }

    fun toggleExtras() = _ui.update { it.copy(extrasExpanded = !it.extrasExpanded) }

    fun toggleTask(taskId: Long) = _ui.update {
        it.copy(
            selectedTaskIds = if (taskId in it.selectedTaskIds) {
                it.selectedTaskIds - taskId
            } else {
                it.selectedTaskIds + taskId
            },
        )
    }

    fun selectAllOverdue() = _ui.update { state ->
        val overdue = state.tasks
            .filter { it.computation?.status == TaskStatus.OVERDUE }
            .map { it.taskId }
        state.copy(selectedTaskIds = state.selectedTaskIds + overdue)
    }

    /**
     * Validates the odometer, saves the record and its task links, and confirms
     * the vehicle odometer (spec P3). A record with no tasks is valid.
     */
    fun save(onSaved: () -> Unit) {
        val state = _ui.value
        val km = state.odometer.trim().toIntOrNull()
        if (km == null || km < 0) {
            _ui.update { it.copy(odometerError = true) }
            return
        }

        val cost = state.cost.trim()
            .replace(',', '.')
            .toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ZERO }

        val record = MaintenanceRecord(
            id = recordId ?: 0,
            vehicleId = vehicleId,
            date = state.date,
            odometerKm = km,
            workshop = state.workshop.trim().ifBlank { null },
            cost = cost,
            notes = state.notes.trim().ifBlank { null },
            attachmentUri = state.attachmentUri,
        )

        viewModelScope.launch {
            recordRepository.save(record, state.selectedTaskIds.toList())
            onSaved()
        }
    }
}

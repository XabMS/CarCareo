package com.xabier.carcareo.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.ConfirmedOdometer
import com.xabier.carcareo.domain.KmChangeSeverity
import com.xabier.carcareo.domain.OdometerConfirmation
import com.xabier.carcareo.domain.OdometerEstimator
import com.xabier.carcareo.domain.OdometerUpdate
import com.xabier.carcareo.domain.PlanStatusCalculator
import com.xabier.carcareo.domain.RecordReading
import com.xabier.carcareo.domain.TaskComputation
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.ui.navigation.Destinations
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
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
    val vehicleName: String = "",
    /** Always set by the ViewModel from its clock; no default, so nothing here reads the clock. */
    val date: LocalDate,
    val odometer: String = "",
    /**
     * True while [odometer] is still the app's own guess for [date] (a new
     * record whose km the user has not typed). The screen must say so: saving
     * turns the figure into the vehicle's confirmed reading (spec 4.1).
     */
    val odometerIsEstimate: Boolean = false,
    val lastConfirmedKm: Int = 0,
    /** Null until the vehicle has loaded. */
    val lastConfirmedKmDate: LocalDate? = null,
    /** Edit only: the reading this record had when the form opened. */
    val originalReading: RecordReading? = null,
    val tasks: List<LogTaskRow> = emptyList(),
    val selectedTaskIds: Set<Long> = emptySet(),
    val workshop: String = "",
    val cost: String = "",
    val notes: String = "",
    val attachmentUri: String? = null,
    val extrasExpanded: Boolean = false,
    val odometerError: Boolean = false,
    val dateError: Boolean = false,
    val costError: Boolean = false,
    val majorDecreaseConfirmed: Boolean = false,
    val saving: Boolean = false,
    /** The last save attempt threw; the screen shows a message and clears it. */
    val saveFailed: Boolean = false,
) {
    /**
     * Would saving this record as it stands move the vehicle's confirmed
     * odometer? A back-dated record does not — it is history, not a new reading.
     */
    val confirmsOdometer: Boolean
        get() {
            val km = odometer.trim().toIntOrNull() ?: return false
            val confirmedOn = lastConfirmedKmDate ?: return false
            return OdometerConfirmation.shouldConfirmOnSave(
                saved = RecordReading(km = km, date = date),
                previous = originalReading,
                confirmed = ConfirmedOdometer(km = lastConfirmedKm, date = confirmedOn),
            )
        }

    /**
     * How the typed km compares with the confirmed odometer. Only meaningful for
     * a record that is not older than that reading: an odometer cannot have gone
     * down since. Work logged or edited at an *earlier* date naturally has a
     * lower mileage and lowers nothing, so it must not raise the decrease warning.
     */
    val kmSeverity: KmChangeSeverity
        get() {
            val km = odometer.trim().toIntOrNull() ?: return KmChangeSeverity.OK
            val confirmedOn = lastConfirmedKmDate ?: return KmChangeSeverity.OK
            if (date.isBefore(confirmedOn) && !confirmsOdometer) return KmChangeSeverity.OK
            return OdometerUpdate.classify(lastConfirmedKm, km)
        }
}

class LogMaintenanceViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val taskRepository: MaintenanceTaskRepository,
    private val recordRepository: MaintenanceRecordRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    // Present only on the edit route (log/{vehicleId}/{recordId}).
    private val recordId: Long? = savedStateHandle.get<Long>(Destinations.RECORD_ID_ARG)
        ?.takeIf { it > 0 }

    // Optional: a single task to pre-select, set when the user taps one plan row
    // instead of the "Log maintenance" button (log/{vehicleId}?taskId={taskId}).
    private val presetTaskId: Long? = savedStateHandle.get<Long>(Destinations.TASK_ID_ARG)
        ?.takeIf { it > 0 }

    /**
     * Read at each point of use, never cached in a field: this ViewModel can
     * outlive several midnights (see [com.xabier.carcareo.domain.todayFlow]), and
     * a save validated against a construction-time date would wrongly accept a
     * date that has since become the future. Unlike the garage and detail
     * screens this is not a flow on purpose — the form loads once, so that
     * re-deriving the pre-selected tasks can never discard the user's own ticks.
     */
    private fun today(): LocalDate = LocalDate.now(clock)

    private val _ui = MutableStateFlow(LogMaintenanceUiState(date = today()))
    val ui: StateFlow<LogMaintenanceUiState> = _ui.asStateFlow()

    /** Loaded once in init; the source of the km estimate for a picked date. */
    private var vehicle: Vehicle? = null

    init {
        viewModelScope.launch {
            val vehicle = vehicleRepository.get(vehicleId) ?: return@launch
            this@LogMaintenanceViewModel.vehicle = vehicle
            val tasks = taskRepository.getForVehicle(vehicleId)
            val records = recordRepository.observeForVehicle(vehicleId).first()
            val status = PlanStatusCalculator.forVehicle(vehicle, tasks, records, today())

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
                        vehicleName = vehicle.name,
                        date = existing.record.date,
                        odometer = existing.record.odometerKm.toString(),
                        lastConfirmedKm = vehicle.lastConfirmedKm,
                        lastConfirmedKmDate = vehicle.lastConfirmedKmDate,
                        originalReading = RecordReading(
                            km = existing.record.odometerKm,
                            date = existing.record.date,
                        ),
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
                // A row tap pre-selects just that task; otherwise pre-mark
                // everything overdue or upcoming (spec P3).
                val preselected = if (presetTaskId != null &&
                    activeRows.any { r -> r.taskId == presetTaskId }
                ) {
                    setOf(presetTaskId)
                } else {
                    activeRows
                        .filter { r -> r.computation?.status != TaskStatus.OK }
                        .map { r -> r.taskId }
                        .toSet()
                }
                _ui.update {
                    it.copy(
                        loading = false,
                        vehicleName = vehicle.name,
                        odometer = status.currentKm.toString(),
                        odometerIsEstimate = status.currentKmIsEstimate,
                        lastConfirmedKm = vehicle.lastConfirmedKm,
                        lastConfirmedKmDate = vehicle.lastConfirmedKmDate,
                        tasks = activeRows,
                        selectedTaskIds = preselected,
                    )
                }
            }

            // After a process death the form is rebuilt from the database above;
            // what the user had typed comes back on top of it.
            restoreDraft()
            // From here on every change to the form is mirrored into the saved
            // state, so a half-filled record survives the app being killed in the
            // background (e.g. while the user is off photographing the invoice).
            _ui.collect(::persistDraft)
        }
    }

    fun setDate(date: LocalDate) = _ui.update {
        // Until the user types a reading, the km field follows the date: the
        // estimate for "today" is simply wrong for work done last spring.
        val vehicle = vehicle
        if (it.isEdit || !it.odometerIsEstimate || vehicle == null) {
            it.copy(date = date, dateError = false)
        } else {
            it.copy(
                date = date,
                dateError = false,
                odometer = OdometerEstimator.kmAt(
                    lastConfirmedKm = vehicle.lastConfirmedKm,
                    lastConfirmedKmDate = vehicle.lastConfirmedKmDate,
                    annualKmEstimate = vehicle.annualKmEstimate,
                    date = date,
                ).toString(),
                odometerError = false,
            )
        }
    }

    fun setOdometer(value: String) = _ui.update {
        it.copy(
            odometer = value.filter(Char::isDigit),
            odometerIsEstimate = false,
            odometerError = false,
        )
    }

    fun setWorkshop(value: String) = _ui.update { it.copy(workshop = value) }

    fun setCost(value: String) = _ui.update {
        it.copy(
            cost = value.filter { c -> c.isDigit() || c == '.' || c == ',' },
            costError = false,
        )
    }

    fun setMajorDecreaseConfirmed(confirmed: Boolean) =
        _ui.update { it.copy(majorDecreaseConfirmed = confirmed) }

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

    fun consumeSaveFailed() = _ui.update { it.copy(saveFailed = false) }

    /**
     * Validates the odometer, saves the record and its task links, and confirms
     * the vehicle odometer (spec P3). A record with no tasks is valid.
     */
    fun save(onSaved: () -> Unit) {
        val state = _ui.value
        if (state.saving || state.loading) return

        val km = state.odometer.trim().toIntOrNull()
        val odometerError = km == null || km < 0

        val needsMajorConfirmation = state.kmSeverity == KmChangeSeverity.MAJOR_DECREASE &&
            !state.majorDecreaseConfirmed

        val dateError = state.date.isAfter(today())

        val trimmedCost = state.cost.trim()
        val cost = trimmedCost.replace(',', '.').toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
        val costError = trimmedCost.isNotEmpty() && cost == null

        if (odometerError || dateError || costError || needsMajorConfirmation) {
            _ui.update {
                it.copy(odometerError = odometerError, dateError = dateError, costError = costError)
            }
            return
        }

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

        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                recordRepository.save(record, state.selectedTaskIds.toList())
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // E.g. a ticked task was deleted meanwhile (foreign key). The
                // transaction rolled back; keep the form so nothing typed is lost.
                _ui.update { it.copy(saveFailed = true) }
            } finally {
                _ui.update { it.copy(saving = false) }
            }
        }
    }

    // --- draft persistence (process death) ---

    private fun persistDraft(state: LogMaintenanceUiState) {
        if (state.loading) return
        savedStateHandle[DRAFT_PRESENT] = true
        savedStateHandle[DRAFT_DATE] = state.date.toEpochDay()
        savedStateHandle[DRAFT_ODOMETER] = state.odometer
        savedStateHandle[DRAFT_ODOMETER_ESTIMATE] = state.odometerIsEstimate
        savedStateHandle[DRAFT_TASKS] = state.selectedTaskIds.toLongArray()
        savedStateHandle[DRAFT_WORKSHOP] = state.workshop
        savedStateHandle[DRAFT_COST] = state.cost
        savedStateHandle[DRAFT_NOTES] = state.notes
        savedStateHandle[DRAFT_ATTACHMENT] = state.attachmentUri
        savedStateHandle[DRAFT_EXTRAS] = state.extrasExpanded
        savedStateHandle[DRAFT_MAJOR_CONFIRMED] = state.majorDecreaseConfirmed
    }

    private fun restoreDraft() {
        if (savedStateHandle.get<Boolean>(DRAFT_PRESENT) != true) return
        _ui.update { loaded ->
            // Only tasks that still exist: one may have been deleted meanwhile.
            val knownTaskIds = loaded.tasks.mapTo(HashSet(), LogTaskRow::taskId)
            loaded.copy(
                date = savedStateHandle.get<Long>(DRAFT_DATE)?.let(LocalDate::ofEpochDay) ?: loaded.date,
                odometer = savedStateHandle[DRAFT_ODOMETER] ?: loaded.odometer,
                odometerIsEstimate = savedStateHandle[DRAFT_ODOMETER_ESTIMATE] ?: loaded.odometerIsEstimate,
                selectedTaskIds = savedStateHandle.get<LongArray>(DRAFT_TASKS)
                    ?.filterTo(HashSet()) { it in knownTaskIds }
                    ?: loaded.selectedTaskIds,
                workshop = savedStateHandle[DRAFT_WORKSHOP] ?: loaded.workshop,
                cost = savedStateHandle[DRAFT_COST] ?: loaded.cost,
                notes = savedStateHandle[DRAFT_NOTES] ?: loaded.notes,
                attachmentUri = savedStateHandle[DRAFT_ATTACHMENT],
                extrasExpanded = savedStateHandle[DRAFT_EXTRAS] ?: loaded.extrasExpanded,
                majorDecreaseConfirmed = savedStateHandle[DRAFT_MAJOR_CONFIRMED] ?: loaded.majorDecreaseConfirmed,
            )
        }
    }

    private companion object {
        const val DRAFT_PRESENT = "draft.present"
        const val DRAFT_DATE = "draft.date"
        const val DRAFT_ODOMETER = "draft.odometer"
        const val DRAFT_ODOMETER_ESTIMATE = "draft.odometerIsEstimate"
        const val DRAFT_TASKS = "draft.tasks"
        const val DRAFT_WORKSHOP = "draft.workshop"
        const val DRAFT_COST = "draft.cost"
        const val DRAFT_NOTES = "draft.notes"
        const val DRAFT_ATTACHMENT = "draft.attachment"
        const val DRAFT_EXTRAS = "draft.extras"
        const val DRAFT_MAJOR_CONFIRMED = "draft.majorConfirmed"
    }
}

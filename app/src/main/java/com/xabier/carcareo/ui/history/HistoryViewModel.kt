package com.xabier.carcareo.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.relation.RecordWithTasks
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.domain.CostSummary
import com.xabier.carcareo.ui.navigation.Destinations
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val entries: List<RecordWithTasks> = emptyList(),
    /** Unfiltered record count — for the shell's "N RECORDS" context line. */
    val totalRecords: Int = 0,
    /** Tasks that appear in at least one record — the filter options. */
    val filterableTasks: List<MaintenanceTask> = emptyList(),
    val activeFilterTaskId: Long? = null,
    val totalCost: BigDecimal = BigDecimal.ZERO,
    val last12MonthsCost: BigDecimal = BigDecimal.ZERO,
    val showCosts: Boolean = false,
)

class HistoryViewModel(
    savedStateHandle: SavedStateHandle,
    private val recordRepository: MaintenanceRecordRepository,
    taskRepository: MaintenanceTaskRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])
    private val filter = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> =
        combine(
            recordRepository.observeForVehicle(vehicleId),
            taskRepository.observeForVehicle(vehicleId),
            filter,
        ) { records, tasks, filterTaskId ->
            val allRecords = records.map { it.record }
            val shown = if (filterTaskId == null) {
                records
            } else {
                records.filter { rwt -> rwt.tasks.any { it.id == filterTaskId } }
            }
            HistoryUiState(
                loading = false,
                entries = shown,
                totalRecords = allRecords.size,
                filterableTasks = tasks
                    .filter { task -> records.any { rwt -> rwt.tasks.any { it.id == task.id } } },
                activeFilterTaskId = filterTaskId,
                totalCost = CostSummary.total(allRecords),
                last12MonthsCost = CostSummary.lastMonths(allRecords, months = 12),
                showCosts = CostSummary.hasAnyCost(allRecords),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(loading = true),
        )

    fun setFilter(taskId: Long?) {
        filter.value = taskId
    }

    fun delete(record: MaintenanceRecord) {
        // Deleting recalculates every state automatically: the record flow emits,
        // and PlanStatusCalculator re-runs wherever it is observed (spec P5).
        viewModelScope.launch { recordRepository.delete(record) }
    }
}

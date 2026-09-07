package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.OdometerReading
import com.xabier.carcareo.domain.PlanStatusCalculator
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.domain.VehiclePlanStatus
import com.xabier.carcareo.domain.odometerReading
import com.xabier.carcareo.domain.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class VehicleCard(
    val vehicle: Vehicle,
    val odometer: OdometerReading,
    /** null while there is no plan for this vehicle. */
    val planStatus: VehiclePlanStatus?,
)

/**
 * Derived from the [VehicleCard] list — drives the garage attention banner.
 * Null when no active vehicle has an overdue task.
 */
data class OverdueSummary(
    val taskCount: Int,
    val vehicleCount: Int,
    val topVehicleId: Long,
    val topVehicleName: String,
    val topTaskId: Long,
    val topTaskName: String,
)

data class GarageUiState(
    val loading: Boolean = true,
    val vehicles: List<VehicleCard> = emptyList(),
    val overdueSummary: OverdueSummary? = null,
)

class GarageViewModel(
    vehicleRepository: VehicleRepository,
    taskRepository: MaintenanceTaskRepository,
    recordRepository: MaintenanceRecordRepository,
) : ViewModel() {

    val uiState: StateFlow<GarageUiState> =
        combine(
            vehicleRepository.observeActive(),
            taskRepository.observeAll(),
            recordRepository.observeAllWithTasks(),
            // Part of the inputs, not a constant: the garage must re-compute when
            // the day rolls over, not only when the data changes.
            todayFlow(),
        ) { vehicles, allTasks, allRecords, today ->
            val tasksByVehicle = allTasks.groupBy { it.vehicleId }
            val recordsByVehicle = allRecords.groupBy { it.record.vehicleId }

            val cards = vehicles.map { v ->
                val tasks = tasksByVehicle[v.id].orEmpty()
                VehicleCard(
                    vehicle = v,
                    odometer = v.odometerReading(today),
                    planStatus = if (tasks.isEmpty()) {
                        null
                    } else {
                        PlanStatusCalculator.forVehicle(
                            vehicle = v,
                            tasks = tasks,
                            records = recordsByVehicle[v.id].orEmpty(),
                            today = today,
                        )
                    },
                )
            }

            GarageUiState(
                loading = false,
                vehicles = cards,
                overdueSummary = summariseOverdue(cards),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GarageUiState(loading = true),
        )
}

private fun summariseOverdue(cards: List<VehicleCard>): OverdueSummary? {
    val perVehicle = cards.mapNotNull { card ->
        val overdue = card.planStatus?.activeOrdered
            ?.filter { it.computation.status == TaskStatus.OVERDUE }
            .orEmpty()
        if (overdue.isEmpty()) null else card to overdue
    }
    if (perVehicle.isEmpty()) return null

    val (topCard, topOverdue) = perVehicle.minByOrNull { (_, list) ->
        list.minOf { it.computation.urgency }
    }!!
    val topTask = topOverdue.minByOrNull { it.computation.urgency }!!.task

    return OverdueSummary(
        taskCount = perVehicle.sumOf { it.second.size },
        vehicleCount = perVehicle.size,
        topVehicleId = topCard.vehicle.id,
        topVehicleName = topCard.vehicle.name,
        topTaskId = topTask.id,
        topTaskName = topTask.name,
    )
}

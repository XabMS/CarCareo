package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.OdometerReading
import com.xabier.carcareo.domain.PlanStatusCalculator
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

data class GarageUiState(
    val loading: Boolean = true,
    val vehicles: List<VehicleCard> = emptyList(),
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

            GarageUiState(
                loading = false,
                vehicles = vehicles.map { v ->
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
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GarageUiState(loading = true),
        )
}

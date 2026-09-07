package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.SavedStateHandle
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
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleDetailUiState(
    val loading: Boolean = true,
    val vehicle: Vehicle? = null,
    val odometer: OdometerReading? = null,
    /** Computed plan state: urgency-ordered active tasks, inactive tasks, worst status. */
    val planStatus: VehiclePlanStatus? = null,
)

class VehicleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: VehicleRepository,
    taskRepository: MaintenanceTaskRepository,
    recordRepository: MaintenanceRecordRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<VehicleDetailUiState> =
        combine(
            repository.observe(vehicleId),
            taskRepository.observeForVehicle(vehicleId),
            recordRepository.observeForVehicle(vehicleId),
            // See GarageViewModel: the day rolling over is an input, not a constant.
            todayFlow(),
        ) { vehicle, tasks, records, today ->
            VehicleDetailUiState(
                loading = false,
                vehicle = vehicle,
                odometer = vehicle?.odometerReading(today),
                planStatus = vehicle?.let {
                    PlanStatusCalculator.forVehicle(it, tasks, records, today)
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VehicleDetailUiState(loading = true),
        )

    /** Save a user-confirmed odometer value. Severity must be checked by the UI first. */
    fun confirmOdometer(km: Int, date: LocalDate) {
        viewModelScope.launch {
            repository.confirmOdometer(vehicleId, km, date)
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(vehicleId, archived)
        }
    }
}

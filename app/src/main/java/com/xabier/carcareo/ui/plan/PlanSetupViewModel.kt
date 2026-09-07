package com.xabier.carcareo.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.ui.navigation.Destinations
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanSetupUiState(
    val vehicle: Vehicle? = null,
    /** Other vehicles whose plan could be copied. Empty -> hide that option. */
    val otherVehicles: List<Vehicle> = emptyList(),
    val working: Boolean = false,
)

/**
 * Shown once, right after a new vehicle is created (spec P4): apply the category
 * template (default), copy another vehicle's plan, or start empty.
 */
class PlanSetupViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: MaintenanceTaskRepository,
    vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<PlanSetupUiState> =
        combine(
            vehicleRepository.observe(vehicleId),
            vehicleRepository.observeAll(),
        ) { vehicle, all ->
            PlanSetupUiState(
                vehicle = vehicle,
                otherVehicles = all.filter { it.id != vehicleId },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanSetupUiState(),
        )

    fun applyTemplate(spanish: Boolean, onDone: () -> Unit) {
        val category = uiState.value.vehicle?.category ?: return
        viewModelScope.launch {
            taskRepository.applyTemplate(vehicleId, category, spanish)
            onDone()
        }
    }

    fun duplicateFrom(sourceVehicleId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            taskRepository.duplicatePlan(sourceVehicleId, vehicleId)
            onDone()
        }
    }
}

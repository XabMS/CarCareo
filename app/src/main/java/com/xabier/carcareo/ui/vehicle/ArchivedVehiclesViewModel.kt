package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedVehiclesViewModel(
    private val repository: VehicleRepository,
) : ViewModel() {

    val archived: StateFlow<List<Vehicle>> =
        repository.observeArchived()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchive(id: Long) {
        viewModelScope.launch { repository.setArchived(id, archived = false) }
    }
}

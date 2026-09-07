package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backing state for the add / edit vehicle form (P2's "Edit vehicle" and the
 * garage FAB). Text fields are kept as strings so half-typed input is preserved;
 * parsing and validation happen on save.
 */
data class VehicleFormState(
    val id: Long? = null,
    val name: String = "",
    val category: VehicleCategory = VehicleCategory.MOTO_TERMICA,
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val plate: String = "",
    val lastConfirmedKm: String = "",
    val lastConfirmedKmDate: LocalDate = LocalDate.now(),
    val annualKmEstimate: String = DEFAULT_ANNUAL_KM.toString(),
    val purchaseDate: LocalDate? = null,
    val notes: String = "",
    /** Not user-editable here; carried so an edit-save doesn't un-archive. */
    val archived: Boolean = false,
    val nameError: Boolean = false,
    val kmError: Boolean = false,
    val loaded: Boolean = false,
    /** Bumped on every save attempt so the UI can react to a rejected submit. */
    val submitCount: Int = 0,
) {
    val isEdit: Boolean get() = id != null

    companion object {
        const val DEFAULT_ANNUAL_KM = 12_000
    }
}

class VehicleFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: VehicleRepository,
) : ViewModel() {

    // Present only on the edit route (vehicleForm/{vehicleId}); absent when adding.
    private val editingId: Long? = savedStateHandle[Destinations.VEHICLE_ID_ARG]

    private val _state = MutableStateFlow(VehicleFormState())
    val state: StateFlow<VehicleFormState> = _state.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                repository.get(editingId)?.let { v ->
                    _state.value = VehicleFormState(
                        id = v.id,
                        name = v.name,
                        category = v.category,
                        make = v.make.orEmpty(),
                        model = v.model.orEmpty(),
                        year = v.year?.toString().orEmpty(),
                        plate = v.plate.orEmpty(),
                        lastConfirmedKm = v.lastConfirmedKm.toString(),
                        lastConfirmedKmDate = v.lastConfirmedKmDate,
                        annualKmEstimate = v.annualKmEstimate.toString(),
                        purchaseDate = v.purchaseDate,
                        notes = v.notes.orEmpty(),
                        archived = v.archived,
                        loaded = true,
                    )
                }
            }
        } else {
            _state.update { it.copy(loaded = true) }
        }
    }

    fun edit(transform: (VehicleFormState) -> VehicleFormState) {
        _state.update(transform)
    }

    /**
     * Validates and persists. [onSaved] gets the vehicle id (new or existing) so
     * the caller can navigate on to the detail / plan screen.
     */
    fun save(onSaved: (Long) -> Unit) {
        val s = _state.value
        val name = s.name.trim()
        val km = s.lastConfirmedKm.trim().toIntOrNull()

        val nameError = name.isEmpty()
        val kmError = km == null || km < 0
        _state.update {
            it.copy(nameError = nameError, kmError = kmError, submitCount = it.submitCount + 1)
        }
        if (nameError || kmError) return

        val vehicle = Vehicle(
            id = s.id ?: 0,
            name = name,
            category = s.category,
            make = s.make.trim().ifBlank { null },
            model = s.model.trim().ifBlank { null },
            year = s.year.trim().toIntOrNull(),
            plate = s.plate.trim().ifBlank { null },
            lastConfirmedKm = km,
            lastConfirmedKmDate = s.lastConfirmedKmDate,
            annualKmEstimate = s.annualKmEstimate.trim().toIntOrNull()
                ?.takeIf { it > 0 }
                ?: VehicleFormState.DEFAULT_ANNUAL_KM,
            purchaseDate = s.purchaseDate,
            notes = s.notes.trim().ifBlank { null },
            archived = s.archived,
        )

        viewModelScope.launch {
            val id = if (s.id == null) {
                repository.add(vehicle)
            } else {
                repository.update(vehicle)
                s.id
            }
            onSaved(id)
        }
    }
}

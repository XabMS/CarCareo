package com.xabier.carcareo.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.domain.KmChangeSeverity
import com.xabier.carcareo.domain.OdometerUpdate
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.Clock
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
    val annualKmEstimate: String = defaultAnnualKm(VehicleCategory.MOTO_TERMICA).toString(),
    /** True once the user types in the km/year field, so a category switch stops overwriting it. */
    val annualKmEdited: Boolean = false,
    val purchaseDate: LocalDate? = null,
    val notes: String = "",
    /** Not user-editable here; carried so an edit-save doesn't un-archive. */
    val archived: Boolean = false,
    /** First reading of the vehicle (see `Vehicle.initialKm`); not user-editable. */
    val initialKm: Int? = null,
    val initialKmDate: LocalDate? = null,
    /** Edit only: the reading as stored, to detect and classify a change to it. */
    val originalKm: Int? = null,
    val originalKmDate: LocalDate? = null,
    val nameError: Boolean = false,
    val kmError: Boolean = false,
    /** The reading date is in the future. */
    val readDateError: Boolean = false,
    /** The purchase date is in the future, or after the reading date. */
    val purchaseDateError: Boolean = false,
    /** Ticked by the user to accept a reading far below the stored one. */
    val majorDecreaseConfirmed: Boolean = false,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    /** Bumped on every save attempt so the UI can react to a rejected submit. */
    val submitCount: Int = 0,
) {
    val isEdit: Boolean get() = id != null

    /** How the typed reading compares with the stored one (always OK for a new vehicle). */
    val kmSeverity: KmChangeSeverity
        get() {
            val previous = originalKm ?: return KmChangeSeverity.OK
            val typed = lastConfirmedKm.trim().toIntOrNull() ?: return KmChangeSeverity.OK
            return OdometerUpdate.classify(previous, typed)
        }

    val hasError: Boolean
        get() = nameError || kmError || readDateError || purchaseDateError

    companion object {
        /** Default yearly mileage estimate, by vehicle type. */
        fun defaultAnnualKm(category: VehicleCategory): Int = when (category) {
            VehicleCategory.MOTO_TERMICA -> 8_000
            VehicleCategory.COCHE_TERMICO, VehicleCategory.COCHE_ELECTRICO -> 10_000
        }
    }
}

class VehicleFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: VehicleRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
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
                        annualKmEdited = true,
                        purchaseDate = v.purchaseDate,
                        initialKm = v.initialKm,
                        initialKmDate = v.initialKmDate,
                        originalKm = v.lastConfirmedKm,
                        originalKmDate = v.lastConfirmedKmDate,
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

    /** Switches vehicle type, keeping the km/year default in sync unless the user set it. */
    fun setCategory(category: VehicleCategory) {
        _state.update { s ->
            s.copy(
                category = category,
                annualKmEstimate = if (s.annualKmEdited) s.annualKmEstimate
                else VehicleFormState.defaultAnnualKm(category).toString(),
            )
        }
    }

    fun setAnnualKm(text: String) {
        _state.update { it.copy(annualKmEstimate = text.filter(Char::isDigit), annualKmEdited = true) }
    }

    /**
     * Validates and persists. [onSaved] gets the vehicle id (new or existing) so
     * the caller can navigate on to the detail / plan screen.
     */
    fun save(onSaved: (Long) -> Unit) {
        val s = _state.value
        // A second tap while the insert is still running would add the vehicle twice.
        if (s.saving) return

        val today = LocalDate.now(clock)
        val name = s.name.trim()
        val km = s.lastConfirmedKm.trim().toIntOrNull()

        val nameError = name.isEmpty()
        val kmError = km == null || km < 0
        val readDateError = s.lastConfirmedKmDate.isAfter(today)
        val purchaseDateError = s.purchaseDate != null &&
            (s.purchaseDate.isAfter(today) || s.purchaseDate.isAfter(s.lastConfirmedKmDate))
        val needsMajorConfirmation =
            s.kmSeverity == KmChangeSeverity.MAJOR_DECREASE && !s.majorDecreaseConfirmed
        _state.update {
            it.copy(
                nameError = nameError,
                kmError = kmError,
                readDateError = readDateError,
                purchaseDateError = purchaseDateError,
                submitCount = it.submitCount + 1,
            )
        }
        if (nameError || kmError || readDateError || purchaseDateError || needsMajorConfirmation) return

        // The first reading is frozen at creation. While the vehicle still sits on
        // it (never updated since), correcting the reading here corrects it too.
        val stillOnInitialReading = s.initialKm == s.originalKm && s.initialKmDate == s.originalKmDate
        val keepInitial = s.isEdit && !stillOnInitialReading

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
                ?: VehicleFormState.defaultAnnualKm(s.category),
            purchaseDate = s.purchaseDate,
            initialKm = if (keepInitial) s.initialKm else km,
            initialKmDate = if (keepInitial) s.initialKmDate else s.lastConfirmedKmDate,
            notes = s.notes.trim().ifBlank { null },
            archived = s.archived,
        )

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = if (s.id == null) {
                    repository.add(vehicle)
                } else {
                    repository.update(vehicle)
                    s.id
                }
                onSaved(id)
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}

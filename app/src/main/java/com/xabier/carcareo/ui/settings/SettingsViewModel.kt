package com.xabier.carcareo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xabier.carcareo.data.backup.BackupError
import com.xabier.carcareo.data.backup.BackupException
import com.xabier.carcareo.data.backup.BackupRepository
import com.xabier.carcareo.data.backup.ImportMode
import com.xabier.carcareo.data.prefs.AppPreferences
import com.xabier.carcareo.data.prefs.NotificationPrefs
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.ui.format.CurrencySetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object Exported : SettingsEvent
    data class Imported(val vehicles: Int, val mode: ImportMode, val unresolvedLinks: Int = 0) : SettingsEvent
    data class ImportFailed(val error: BackupError?) : SettingsEvent
    data object ExportFailed : SettingsEvent
}

/**
 * An ADD import held back because the file contains vehicles whose names are
 * already in the garage — most likely the same backup imported twice. The UI
 * asks; [SettingsViewModel.confirmPendingImport] goes ahead.
 */
data class PendingDuplicateImport(val names: List<String>)

data class SettingsUiState(
    val busy: Boolean = false,
    val event: SettingsEvent? = null,
    /** Epoch millis of the last export, or 0 if never. Drives the backup notice. */
    val lastExportAt: Long = 0L,
    val pendingDuplicateImport: PendingDuplicateImport? = null,
    /** ISO 4217 code costs are shown in; null = follow the device locale. */
    val currencyCode: String? = null,
)

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val preferences: AppPreferences,
    vehicleRepository: VehicleRepository,
    /** Re-applies the WorkManager schedule after a toggle changes. */
    private val reschedule: (NotificationPrefs) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            lastExportAt = preferences.lastExportAt,
            currencyCode = preferences.currencyCode,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Count shown on the "Archived vehicles" row (design handoff §8). */
    val archivedCount: StateFlow<Int> =
        vehicleRepository.observeArchived()
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val notificationPrefs: StateFlow<NotificationPrefs> =
        preferences.observe().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), preferences.snapshot(),
        )

    fun consumeEvent() = _state.update { it.copy(event = null) }

    fun setCurrency(code: String?) {
        preferences.currencyCode = code
        CurrencySetting.code = code
        _state.update { it.copy(currencyCode = code) }
    }

    /**
     * The system refused to show notifications, so neither reminder can work:
     * switch both off rather than leave a toggle on that does nothing.
     */
    fun onNotificationPermissionDenied() {
        preferences.maintenanceAlertsEnabled = false
        preferences.odometerReminderEnabled = false
        reschedule(preferences.snapshot())
    }

    fun setMaintenanceAlerts(enabled: Boolean) {
        preferences.maintenanceAlertsEnabled = enabled
        reschedule(preferences.snapshot())
    }

    fun setOdometerReminder(enabled: Boolean) {
        preferences.odometerReminderEnabled = enabled
        reschedule(preferences.snapshot())
    }

    /** [sink] writes the produced JSON bytes to the chosen document. */
    fun export(sink: suspend (ByteArray) -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val event = try {
                val json = backupRepository.exportJson()
                sink(json.toByteArray(Charsets.UTF_8))
                preferences.lastExportAt = System.currentTimeMillis()
                SettingsEvent.Exported
            } catch (_: Exception) {
                SettingsEvent.ExportFailed
            }
            _state.update {
                it.copy(busy = false, event = event, lastExportAt = preferences.lastExportAt)
            }
        }
    }

    /** The file text of an import waiting on the duplicate-names question. */
    private var pendingImportText: String? = null

    /** [source] reads the chosen document's full text. */
    fun import(mode: ImportMode, source: suspend () -> String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val event: SettingsEvent? = try {
                val text = source()
                val duplicates = if (mode == ImportMode.ADD) {
                    backupRepository.namesAlreadyPresent(text)
                } else {
                    emptyList()
                }
                if (duplicates.isNotEmpty()) {
                    pendingImportText = text
                    _state.update {
                        it.copy(pendingDuplicateImport = PendingDuplicateImport(duplicates))
                    }
                    null
                } else {
                    runImport(text, mode)
                }
            } catch (e: BackupException) {
                SettingsEvent.ImportFailed(e.error)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SettingsEvent.ImportFailed(null)
            }
            _state.update { it.copy(busy = false, event = event ?: it.event) }
        }
    }

    /** The user chose to add the vehicles even though their names already exist. */
    fun confirmPendingImport() {
        val text = pendingImportText ?: return
        if (_state.value.busy) return
        pendingImportText = null
        _state.update { it.copy(busy = true, pendingDuplicateImport = null) }
        viewModelScope.launch {
            val event = try {
                runImport(text, ImportMode.ADD)
            } catch (e: BackupException) {
                SettingsEvent.ImportFailed(e.error)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                SettingsEvent.ImportFailed(null)
            }
            _state.update { it.copy(busy = false, event = event) }
        }
    }

    fun cancelPendingImport() {
        pendingImportText = null
        _state.update { it.copy(pendingDuplicateImport = null) }
    }

    private suspend fun runImport(text: String, mode: ImportMode): SettingsEvent {
        val result = backupRepository.importJson(text, mode)
        return SettingsEvent.Imported(result.vehicles, mode, result.unresolvedLinks)
    }
}

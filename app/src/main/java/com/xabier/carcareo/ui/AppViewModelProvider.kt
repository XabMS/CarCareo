package com.xabier.carcareo.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xabier.carcareo.CarCareoApp
import com.xabier.carcareo.ui.history.HistoryViewModel
import com.xabier.carcareo.ui.log.LogMaintenanceViewModel
import com.xabier.carcareo.ui.plan.PlanEditorViewModel
import com.xabier.carcareo.ui.plan.PlanSetupViewModel
import com.xabier.carcareo.notifications.WorkScheduler
import com.xabier.carcareo.ui.settings.SettingsViewModel
import com.xabier.carcareo.ui.vehicle.ArchivedVehiclesViewModel
import com.xabier.carcareo.ui.vehicle.GarageViewModel
import com.xabier.carcareo.ui.vehicle.VehicleDetailViewModel
import com.xabier.carcareo.ui.vehicle.VehicleFormViewModel

/**
 * One place that knows how to build every ViewModel from the [AppContainer].
 * This replaces a DI framework: `viewModel(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            GarageViewModel(
                vehicleRepository = app().container.vehicleRepository,
                taskRepository = app().container.maintenanceTaskRepository,
                recordRepository = app().container.maintenanceRecordRepository,
            )
        }
        initializer {
            VehicleDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = app().container.vehicleRepository,
                taskRepository = app().container.maintenanceTaskRepository,
                recordRepository = app().container.maintenanceRecordRepository,
            )
        }
        initializer {
            VehicleFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = app().container.vehicleRepository,
            )
        }
        initializer {
            ArchivedVehiclesViewModel(app().container.vehicleRepository)
        }
        initializer {
            PlanEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                taskRepository = app().container.maintenanceTaskRepository,
                vehicleRepository = app().container.vehicleRepository,
            )
        }
        initializer {
            PlanSetupViewModel(
                savedStateHandle = createSavedStateHandle(),
                taskRepository = app().container.maintenanceTaskRepository,
                vehicleRepository = app().container.vehicleRepository,
            )
        }
        initializer {
            LogMaintenanceViewModel(
                savedStateHandle = createSavedStateHandle(),
                vehicleRepository = app().container.vehicleRepository,
                taskRepository = app().container.maintenanceTaskRepository,
                recordRepository = app().container.maintenanceRecordRepository,
            )
        }
        initializer {
            HistoryViewModel(
                savedStateHandle = createSavedStateHandle(),
                recordRepository = app().container.maintenanceRecordRepository,
                taskRepository = app().container.maintenanceTaskRepository,
            )
        }
        initializer {
            val application = app()
            SettingsViewModel(
                backupRepository = application.container.backupRepository,
                preferences = application.container.appPreferences,
                reschedule = { prefs -> WorkScheduler.apply(application, prefs) },
            )
        }
    }
}

private fun CreationExtras.app(): CarCareoApp =
    this[APPLICATION_KEY] as CarCareoApp

package com.xabier.carcareo.di

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.DataHooks
import com.xabier.carcareo.data.backup.BackupRepository
import com.xabier.carcareo.data.prefs.AppPreferences
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.notifications.Notifications

/**
 * Manual DI container. Holds the app-wide singletons (database, repositories).
 * ViewModels reach it through [com.xabier.carcareo.CarCareoApp]. New repositories
 * are added here as later phases need them.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: AppDatabase = AppDatabase.get(context)

    val appPreferences: AppPreferences = AppPreferences(appContext)

    /** The Android-side effects of data changes; see [DataHooks]. */
    private val hooks = object : DataHooks {
        override fun onAttachmentUnused(uri: String) {
            // Persisted SAF grants are a capped resource; give back the ones no
            // record needs. Fails harmlessly if the grant is already gone.
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(
                    uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }

        override fun onVehicleAlertStale(vehicleId: Long) {
            appPreferences.clearAlert(vehicleId)
            Notifications.cancelMaintenanceAlert(appContext, vehicleId)
        }
    }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database, hooks)
    }

    val maintenanceTaskRepository: MaintenanceTaskRepository by lazy {
        MaintenanceTaskRepository(database.maintenanceTaskDao())
    }

    val maintenanceRecordRepository: MaintenanceRecordRepository by lazy {
        MaintenanceRecordRepository(database, hooks)
    }

    val backupRepository: BackupRepository by lazy { BackupRepository(database, hooks) }
}

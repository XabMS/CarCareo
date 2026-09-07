package com.xabier.carcareo.di

import android.content.Context
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.backup.BackupRepository
import com.xabier.carcareo.data.prefs.AppPreferences
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository

/**
 * Manual DI container. Holds the app-wide singletons (database, repositories).
 * ViewModels reach it through [com.xabier.carcareo.CarCareoApp]. New repositories
 * are added here as later phases need them.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.get(context)

    val appPreferences: AppPreferences = AppPreferences(context.applicationContext)

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database.vehicleDao())
    }

    val maintenanceTaskRepository: MaintenanceTaskRepository by lazy {
        MaintenanceTaskRepository(database.maintenanceTaskDao())
    }

    val maintenanceRecordRepository: MaintenanceRecordRepository by lazy {
        MaintenanceRecordRepository(database.maintenanceRecordDao(), database.vehicleDao())
    }

    val backupRepository: BackupRepository by lazy { BackupRepository(database) }
}

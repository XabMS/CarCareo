package com.xabier.carcareo.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xabier.carcareo.CarCareoApp
import com.xabier.carcareo.data.AppDatabase

/**
 * Monthly "update your odometer" nudge (spec F6). Skipped when there are no
 * active vehicles, or when the toggle is off.
 */
class OdometerReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CarCareoApp
        if (!app.container.appPreferences.odometerReminderEnabled) return Result.success()

        val hasVehicles = AppDatabase.get(applicationContext).vehicleDao().getAll()
            .any { !it.archived }
        if (hasVehicles) {
            Notifications.postOdometerReminder(applicationContext)
        }
        return Result.success()
    }
}

package com.xabier.carcareo.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xabier.carcareo.data.prefs.NotificationPrefs
import java.util.concurrent.TimeUnit

/**
 * Enqueues or cancels the two periodic workers to match the current toggles.
 * Called at app start and whenever a toggle changes in Settings.
 */
object WorkScheduler {

    private const val WORK_MAINTENANCE = "carcareo.maintenance_check"
    private const val WORK_ODOMETER = "carcareo.odometer_reminder"

    fun apply(context: Context, prefs: NotificationPrefs) {
        val wm = WorkManager.getInstance(context)

        if (prefs.maintenanceAlertsEnabled) {
            wm.enqueueUniquePeriodicWork(
                WORK_MAINTENANCE,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<MaintenanceCheckWorker>(1, TimeUnit.DAYS).build(),
            )
        } else {
            wm.cancelUniqueWork(WORK_MAINTENANCE)
        }

        if (prefs.odometerReminderEnabled) {
            wm.enqueueUniquePeriodicWork(
                WORK_ODOMETER,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<OdometerReminderWorker>(30, TimeUnit.DAYS).build(),
            )
        } else {
            wm.cancelUniqueWork(WORK_ODOMETER)
        }
    }
}

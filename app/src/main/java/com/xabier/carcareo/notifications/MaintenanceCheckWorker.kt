package com.xabier.carcareo.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xabier.carcareo.CarCareoApp
import com.xabier.carcareo.R
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.domain.PlanStatusCalculator
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.domain.VehiclePlanStatus
import java.time.LocalDate

/**
 * Daily check (spec F6). For every non-archived vehicle whose worst task status is
 * UPCOMING or OVERDUE, posts one grouped notification. Per-vehicle dedup avoids a
 * fresh notification every single day for the same situation: it only re-notifies
 * when the status gets worse, or after a week.
 */
class MaintenanceCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CarCareoApp
        val prefs = app.container.appPreferences
        if (!prefs.maintenanceAlertsEnabled) return Result.success()

        val db = AppDatabase.get(applicationContext)
        val today = LocalDate.now()
        val epochDay = today.toEpochDay()

        val vehicles = db.vehicleDao().getAll().filter { !it.archived }
        val tasksByVehicle = db.maintenanceTaskDao().getAll().groupBy { it.vehicleId }
        val recordsByVehicle = db.maintenanceRecordDao().getAllWithTasks()
            .groupBy { it.record.vehicleId }

        for (vehicle in vehicles) {
            val tasks = tasksByVehicle[vehicle.id].orEmpty()
            if (tasks.isEmpty()) continue

            val status = PlanStatusCalculator.forVehicle(
                vehicle = vehicle,
                tasks = tasks,
                records = recordsByVehicle[vehicle.id].orEmpty(),
                today = today,
            )
            val worst = status.vehicleStatus ?: continue

            val memory = prefs.lastAlert(vehicle.id)
            val notify = AlertDedup.shouldNotify(
                currentStatus = worst,
                lastStatusName = memory.status,
                lastEpochDay = memory.epochDay,
                todayEpochDay = epochDay,
            )
            if (!notify) continue

            Notifications.postMaintenanceAlert(
                context = applicationContext,
                vehicleId = vehicle.id,
                vehicleName = vehicle.name,
                line = alertLine(status),
            )
            prefs.rememberAlert(vehicle.id, epochDay, worst.name)
        }
        return Result.success()
    }

    private fun alertLine(status: VehiclePlanStatus): String {
        val needAttention = status.activeOrdered.count { it.computation.status != TaskStatus.OK }
        val top = status.activeOrdered.first().task.name
        return applicationContext.resources.getQuantityString(
            R.plurals.notif_maintenance_body, needAttention, needAttention, top,
        )
    }
}

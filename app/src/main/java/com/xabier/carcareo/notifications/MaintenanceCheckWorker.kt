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
 * UPCOMING or OVERDUE, posts one grouped notification. It runs daily so a task
 * that becomes due is reported within a day; the per-vehicle dedup ([AlertDedup])
 * is what keeps that from nagging: an unchanged situation is only repeated once
 * a week, a worse one is reported straight away.
 */
class MaintenanceCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CarCareoApp
        val prefs = app.container.appPreferences
        if (!prefs.maintenanceAlertsEnabled) return Result.success()
        // Nothing can be shown (permission denied / app notifications blocked), so
        // don't record alerts as delivered: they must still fire once it can.
        if (!Notifications.canPost(applicationContext)) return Result.success()

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
            if (worst == TaskStatus.OK) {
                // Back in order: forget the last alert, so the next problem is
                // reported at once instead of waiting out the weekly repeat.
                prefs.clearAlert(vehicle.id)
                Notifications.cancelMaintenanceAlert(applicationContext, vehicle.id)
                continue
            }

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

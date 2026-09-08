package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.relation.RecordWithTasks
import java.time.LocalDate

data class TaskWithStatus(
    val task: MaintenanceTask,
    val computation: TaskComputation,
)

data class VehiclePlanStatus(
    val currentKm: Int,
    val currentKmIsEstimate: Boolean,
    /** Active tasks, most urgent first (spec 4.3). */
    val activeOrdered: List<TaskWithStatus>,
    /** Deactivated tasks — kept for history, not counted in the vehicle status. */
    val inactive: List<MaintenanceTask>,
    /** Worst state among active tasks; null when there are none. */
    val vehicleStatus: TaskStatus?,
)

/**
 * Glue between the stored entities and the pure [MaintenanceCalculator]. This
 * layer knows about Room entity shapes; the arithmetic stays in the calculator.
 */
object PlanStatusCalculator {

    /**
     * When the baseline odometer is above this, a task with no records is treated
     * as "needs review" (amber) rather than OK: with the car already well used and
     * nothing logged, we genuinely don't know when the task was last done, so we
     * can't honestly count its interval from the baseline.
     *
     * This flags any vehicle first seen well above 0 km with no history — whether
     * bought used, or owned from new but only added to the app later. Only a
     * vehicle entered near delivery mileage stays green with no records. The user
     * clears the flag by logging past work (or deactivating the task).
     */
    const val USED_VEHICLE_KM_THRESHOLD = 1_000

    fun forVehicle(
        vehicle: Vehicle,
        tasks: List<MaintenanceTask>,
        records: List<RecordWithTasks>,
        today: LocalDate = LocalDate.now(),
    ): VehiclePlanStatus {
        val reading = vehicle.odometerReading(today)
        val baseline = baselineFor(vehicle)
        val flagNoHistory = baseline.km > USED_VEHICLE_KM_THRESHOLD
        val lastDoneByTask = mostRecentExecutionByTask(records)

        val (active, inactive) = tasks.partition { it.active }

        val computedActive = active.map { task ->
            val input = TaskInput(
                taskId = task.id,
                intervalKm = task.intervalKm,
                intervalMonths = task.intervalMonths,
                warnKmBefore = task.warnKmBefore,
                warnDaysBefore = task.warnDaysBefore,
                lastDone = lastDoneByTask[task.id],
            )
            TaskWithStatus(task, MaintenanceCalculator.compute(input, baseline, reading.km, today, flagNoHistory))
        }.sortedBy { it.computation.urgency }

        return VehiclePlanStatus(
            currentKm = reading.km,
            currentKmIsEstimate = reading.isEstimate,
            activeOrdered = computedActive,
            inactive = inactive.sortedBy { it.sortOrder },
            vehicleStatus = MaintenanceCalculator.worstStatus(
                computedActive.map { it.computation.status },
            ),
        )
    }

    /** Spec 4.2: acquisition date/km, else the first confirmed odometer. */
    fun baselineFor(vehicle: Vehicle): BaselineRef {
        val date = vehicle.purchaseDate ?: vehicle.lastConfirmedKmDate
        val km = OdometerEstimator.kmAt(
            lastConfirmedKm = vehicle.lastConfirmedKm,
            lastConfirmedKmDate = vehicle.lastConfirmedKmDate,
            annualKmEstimate = vehicle.annualKmEstimate,
            date = date,
        )
        return BaselineRef(date = date, km = km)
    }

    /**
     * For each task id, the most recent [Execution] across all records that
     * cover it: latest date wins, ties broken by the higher odometer.
     */
    fun mostRecentExecutionByTask(records: List<RecordWithTasks>): Map<Long, Execution> {
        val best = HashMap<Long, Execution>()
        for (rwt in records) {
            val exec = Execution(rwt.record.date, rwt.record.odometerKm)
            for (task in rwt.tasks) {
                val current = best[task.id]
                if (current == null ||
                    exec.date.isAfter(current.date) ||
                    (exec.date == current.date && exec.odometerKm > current.odometerKm)
                ) {
                    best[task.id] = exec
                }
            }
        }
        return best
    }
}

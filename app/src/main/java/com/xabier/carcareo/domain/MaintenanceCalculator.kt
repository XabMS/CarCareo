package com.xabier.carcareo.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The core of the app (spec section 4). Pure arithmetic — no Android, no Room,
 * no coroutines — so it can be exhaustively unit-tested. A bug here makes the
 * app lie to the user, so every branch is covered in MaintenanceCalculatorTest.
 *
 * Task state is never stored; it is always the output of [compute].
 */

enum class TaskStatus {
    /** At least one informed margin has run out. */
    OVERDUE,

    /** Within the warning window on km or on time, but not yet due. */
    UPCOMING,

    /** Comfortably within interval on every informed dimension. */
    OK,
}

/** One performed maintenance of a task: when, and at what odometer. */
data class Execution(val date: LocalDate, val odometerKm: Int)

/** Everything [MaintenanceCalculator] needs about one plan task. */
data class TaskInput(
    val taskId: Long,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val warnKmBefore: Int,
    val warnDaysBefore: Int,
    /** Most recent record linked to this task, or null if it was never done. */
    val lastDone: Execution?,
)

/**
 * Reference point used when a task has no history (spec 4.2): the vehicle's
 * acquisition, or failing that its first confirmed odometer.
 */
data class BaselineRef(val date: LocalDate, val km: Int)

/** Result of computing one task's state. */
data class TaskComputation(
    val taskId: Long,
    val status: TaskStatus,
    /** false -> UI shows "no previous record" instead of the due date (spec 4.2). */
    val hasHistory: Boolean,
    val lastDoneDate: LocalDate?,
    val lastDoneKm: Int?,
    val dueKm: Int?,
    val dueDate: LocalDate?,
    /** dueKm - currentKm. Negative = overrun. Null if no km interval. */
    val kmRemaining: Int?,
    /** dueDate - today, in days. Negative = overrun. Null if no time interval. */
    val daysRemaining: Long?,
    /**
     * Fraction of the interval still left, taken as the minimum across informed
     * dimensions. <= 0 means overdue. Used to order the plan by real urgency
     * (spec 4.3), most urgent first = ascending.
     */
    val urgency: Double,
)

object MaintenanceCalculator {

    private const val DAYS_PER_MONTH = 30.436875

    fun compute(
        input: TaskInput,
        baseline: BaselineRef,
        currentKm: Int,
        today: LocalDate,
    ): TaskComputation {
        val hasHistory = input.lastDone != null
        val fromDate = input.lastDone?.date ?: baseline.date
        val fromKm = input.lastDone?.odometerKm ?: baseline.km

        // --- km dimension ---
        val dueKm: Int?
        val kmRemaining: Int?
        val kmRatio: Double?
        if (input.intervalKm != null) {
            dueKm = fromKm + input.intervalKm
            kmRemaining = dueKm - currentKm
            kmRatio = kmRemaining.toDouble() / input.intervalKm
        } else {
            dueKm = null; kmRemaining = null; kmRatio = null
        }

        // --- time dimension ---
        val dueDate: LocalDate?
        val daysRemaining: Long?
        val timeRatio: Double?
        if (input.intervalMonths != null) {
            dueDate = fromDate.plusMonths(input.intervalMonths.toLong())
            daysRemaining = ChronoUnit.DAYS.between(today, dueDate)
            val windowDays = (input.intervalMonths * DAYS_PER_MONTH).coerceAtLeast(1.0)
            timeRatio = daysRemaining.toDouble() / windowDays
        } else {
            dueDate = null; daysRemaining = null; timeRatio = null
        }

        val overdue = (kmRemaining != null && kmRemaining <= 0) ||
            (daysRemaining != null && daysRemaining <= 0)
        val upcoming = (kmRemaining != null && kmRemaining <= input.warnKmBefore) ||
            (daysRemaining != null && daysRemaining <= input.warnDaysBefore)

        val status = when {
            overdue -> TaskStatus.OVERDUE
            upcoming -> TaskStatus.UPCOMING
            else -> TaskStatus.OK
        }

        val urgency = listOfNotNull(kmRatio, timeRatio).minOrNull() ?: Double.MAX_VALUE

        return TaskComputation(
            taskId = input.taskId,
            status = status,
            hasHistory = hasHistory,
            lastDoneDate = input.lastDone?.date,
            lastDoneKm = input.lastDone?.odometerKm,
            dueKm = dueKm,
            dueDate = dueDate,
            kmRemaining = kmRemaining,
            daysRemaining = daysRemaining,
            urgency = urgency,
        )
    }

    /** Computes every task and returns them ordered most-urgent-first (spec 4.3). */
    fun computePlan(
        inputs: List<TaskInput>,
        baseline: BaselineRef,
        currentKm: Int,
        today: LocalDate,
    ): List<TaskComputation> =
        inputs
            .map { compute(it, baseline, currentKm, today) }
            .sortedBy { it.urgency }

    /** Vehicle status = the worst state among the given (active) task states, or null. */
    fun worstStatus(statuses: Iterable<TaskStatus>): TaskStatus? =
        statuses.minByOrNull {
            when (it) {
                TaskStatus.OVERDUE -> 0
                TaskStatus.UPCOMING -> 1
                TaskStatus.OK -> 2
            }
        }
}

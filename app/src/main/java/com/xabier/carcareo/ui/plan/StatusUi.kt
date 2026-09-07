package com.xabier.carcareo.ui.plan

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xabier.carcareo.R
import com.xabier.carcareo.domain.TaskComputation
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.domain.VehiclePlanStatus
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.theme.taskStatusColors
import kotlin.math.abs
import kotlin.math.roundToInt

@get:StringRes
val TaskStatus.labelRes: Int
    get() = when (this) {
        TaskStatus.OVERDUE -> R.string.status_overdue
        TaskStatus.UPCOMING -> R.string.status_upcoming
        TaskStatus.OK -> R.string.status_ok
    }

/** Fill colour — status stripe, mini bars, timeline dots. */
@Composable
fun TaskStatus.color(): Color {
    val c = taskStatusColors(isSystemInDarkTheme())
    return when (this) {
        TaskStatus.OVERDUE -> c.overdue
        TaskStatus.UPCOMING -> c.upcoming
        TaskStatus.OK -> c.ok
    }
}

/** Text colour — labels and copy that must clear 4.5:1 at <=12sp. */
@Composable
fun TaskStatus.textColor(): Color {
    val c = taskStatusColors(isSystemInDarkTheme())
    return when (this) {
        TaskStatus.OVERDUE -> c.overdueText
        TaskStatus.UPCOMING -> c.upcomingText
        TaskStatus.OK -> c.okText
    }
}

private const val DAYS_PER_MONTH = 30.436875

/**
 * "faltan 1.200 km · 4 meses", or "3.000 km de más · 2 meses de más" when
 * overrun. Only informed dimensions appear. Empty string is never returned
 * because a task always has at least one interval.
 */
@Composable
fun TaskComputation.remainingSummary(): String {
    val parts = buildList {
        kmRemaining?.let { km ->
            add(
                if (km >= 0) stringResource(R.string.remaining_km, formatNumber(km))
                else stringResource(R.string.overrun_km, formatNumber(abs(km))),
            )
        }
        daysRemaining?.let { days ->
            val months = (abs(days) / DAYS_PER_MONTH).roundToInt().coerceAtLeast(if (days == 0L) 0 else 1)
            add(
                if (days >= 0) pluralStringResource(R.plurals.remaining_months, months, formatNumber(months))
                else pluralStringResource(R.plurals.overrun_months, months, formatNumber(months)),
            )
        }
    }
    return parts.joinToString(stringResource(R.string.remaining_separator))
}

/** Fraction of the plan's active tasks that are overdue / upcoming (for the stripe). */
fun VehiclePlanStatus.overdueFraction(): Float {
    val n = activeOrdered.size
    if (n == 0) return 0f
    return activeOrdered.count { it.computation.status == TaskStatus.OVERDUE }.toFloat() / n
}

fun VehiclePlanStatus.upcomingFraction(): Float {
    val n = activeOrdered.size
    if (n == 0) return 0f
    return activeOrdered.count { it.computation.status == TaskStatus.UPCOMING }.toFloat() / n
}

/** Interval consumption 0..1 for the mini bars: 1 - remaining ratio, clamped. */
fun TaskComputation.intervalConsumed(): Float =
    (1.0 - urgency).coerceIn(0.0, 1.0).toFloat()

/** Short tail for the garage one-liner: "Oil change · 1.200 km" (km if known, else months). */
@Composable
fun TaskComputation.shortTail(taskName: String): String {
    @Composable
    fun kmText() = kmRemaining?.let { km ->
        if (km >= 0) stringResource(R.string.remaining_km, formatNumber(km))
        else stringResource(R.string.overrun_km, formatNumber(-km))
    }

    @Composable
    fun monthsText() = daysRemaining?.let { days ->
        val months = (abs(days) / DAYS_PER_MONTH).roundToInt().coerceAtLeast(1)
        if (days >= 0) pluralStringResource(R.plurals.remaining_months, months, formatNumber(months))
        else pluralStringResource(R.plurals.overrun_months, months, formatNumber(months))
    }

    // Show the dimension that's actually breached first, so an overdue task
    // never reads "… km left".
    val kmOverrun = kmRemaining != null && kmRemaining < 0
    val timeOverrun = daysRemaining != null && daysRemaining < 0
    val metric = when {
        timeOverrun && !kmOverrun -> monthsText()
        else -> kmText() ?: monthsText()
    } ?: ""
    return listOf(taskName, metric).filter { it.isNotBlank() }
        .joinToString(stringResource(R.string.remaining_separator))
}

/**
 * The single garage-card line (spec P1): the most urgent task, prefixed by its
 * state. Null when the vehicle has no plan.
 */
@Composable
fun VehiclePlanStatus.garageLine(): String? {
    val top = activeOrdered.firstOrNull() ?: return null
    val tail = top.computation.shortTail(top.task.name)
    return if (top.computation.status == TaskStatus.OVERDUE) {
        stringResource(R.string.garage_line_overdue, tail)
    } else {
        stringResource(R.string.garage_line_next, tail)
    }
}

/**
 * The garage-card footer tail without the state prefix: "Oil change · 1.200 km".
 * The state word is shown separately as a coloured label in the redesign.
 */
@Composable
fun VehiclePlanStatus.garageTail(): String? {
    val top = activeOrdered.firstOrNull() ?: return null
    return top.computation.shortTail(top.task.name)
}

/** State word for the garage footer / buckets: OVERDUE / SOON / UP TO DATE. */
@get:StringRes
val TaskStatus.footerLabelRes: Int
    get() = when (this) {
        TaskStatus.OVERDUE -> R.string.status_overdue
        TaskStatus.UPCOMING -> R.string.status_soon
        TaskStatus.OK -> R.string.status_ok
    }

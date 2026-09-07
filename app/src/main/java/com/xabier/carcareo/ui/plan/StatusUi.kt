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

@Composable
fun TaskStatus.color(): Color {
    val c = taskStatusColors(isSystemInDarkTheme())
    return when (this) {
        TaskStatus.OVERDUE -> c.overdue
        TaskStatus.UPCOMING -> c.upcoming
        TaskStatus.OK -> c.ok
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

/** Short tail for the garage one-liner: "Oil change · 1.200 km" (km if known, else months). */
@Composable
private fun TaskComputation.shortTail(taskName: String): String {
    val metric = when {
        kmRemaining != null -> stringResource(R.string.remaining_km, formatNumber(abs(kmRemaining)))
        daysRemaining != null -> {
            val months = (abs(daysRemaining) / DAYS_PER_MONTH).roundToInt().coerceAtLeast(1)
            pluralStringResource(R.plurals.remaining_months, months, formatNumber(months))
        }
        else -> ""
    }
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

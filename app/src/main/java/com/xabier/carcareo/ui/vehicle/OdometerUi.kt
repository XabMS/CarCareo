package com.xabier.carcareo.ui.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xabier.carcareo.R
import com.xabier.carcareo.domain.OdometerReading
import com.xabier.carcareo.ui.format.formatNumber

/** "12.345 km" — the value with its unit. */
@Composable
fun OdometerReading.kmText(): String =
    stringResource(R.string.km_value, formatNumber(km))

/**
 * Secondary line describing freshness: "confirmed today" or "estimated N days/
 * months ago" (spec 4.1 — an estimate must never look like a confirmed fact).
 */
@Composable
fun OdometerReading.freshnessText(): String {
    if (!isEstimate) return stringResource(R.string.odometer_confirmed_today)
    val days = daysSinceConfirmed
    return if (days < 60) {
        pluralStringResource(R.plurals.estimated_days_ago, days.toInt(), formatNumber(days))
    } else {
        val months = (days / 30).toInt()
        pluralStringResource(R.plurals.estimated_months_ago, months, formatNumber(months.toLong()))
    }
}

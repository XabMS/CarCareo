package com.xabier.carcareo.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.ui.format.formatNumber

/**
 * Human-readable interval, e.g. "every 8.000 km · every 12 months". At least one
 * part is always present (enforced on save).
 */
@Composable
fun MaintenanceTask.intervalSummary(): String {
    val parts = buildList {
        intervalKm?.let { add(stringResource(R.string.interval_every_km, formatNumber(it))) }
        intervalMonths?.let {
            add(pluralStringResource(R.plurals.interval_every_months, it, formatNumber(it)))
        }
    }
    return parts.joinToString(stringResource(R.string.interval_separator))
}

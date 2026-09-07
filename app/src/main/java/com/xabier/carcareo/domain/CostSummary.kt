package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.MaintenanceRecord
import java.math.BigDecimal
import java.time.LocalDate

/** Cost roll-ups for the history header (spec P5). Pure and independent of Room. */
object CostSummary {

    fun total(records: List<MaintenanceRecord>): BigDecimal =
        records.fold(BigDecimal.ZERO) { acc, r -> acc + (r.cost ?: BigDecimal.ZERO) }

    /** Sum of costs for records dated within the last [months] months (inclusive). */
    fun lastMonths(
        records: List<MaintenanceRecord>,
        months: Long,
        today: LocalDate = LocalDate.now(),
    ): BigDecimal {
        val cutoff = today.minusMonths(months)
        return records
            .filter { !it.date.isBefore(cutoff) }
            .fold(BigDecimal.ZERO) { acc, r -> acc + (r.cost ?: BigDecimal.ZERO) }
    }

    fun hasAnyCost(records: List<MaintenanceRecord>): Boolean =
        records.any { it.cost != null }
}

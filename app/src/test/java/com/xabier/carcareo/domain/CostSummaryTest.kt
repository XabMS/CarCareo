package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.MaintenanceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CostSummaryTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun rec(date: LocalDate, cost: String?) = MaintenanceRecord(
        vehicleId = 1, date = date, odometerKm = 0,
        cost = cost?.let(::BigDecimal),
    )

    @Test
    fun `total ignores records without a cost`() {
        val records = listOf(
            rec(today, "100.00"),
            rec(today, null),
            rec(today, "49.50"),
        )
        assertEquals(BigDecimal("149.50"), CostSummary.total(records))
    }

    @Test
    fun `last 12 months excludes older records`() {
        val records = listOf(
            rec(today.minusMonths(3), "80.00"),
            rec(today.minusMonths(11), "20.00"),
            rec(today.minusMonths(18), "500.00"),
        )
        assertEquals(BigDecimal("100.00"), CostSummary.lastMonths(records, 12, today))
        assertEquals(BigDecimal("600.00"), CostSummary.total(records))
    }

    @Test
    fun `hasAnyCost detects at least one priced record`() {
        assertFalse(CostSummary.hasAnyCost(listOf(rec(today, null))))
        assertTrue(CostSummary.hasAnyCost(listOf(rec(today, null), rec(today, "1.00"))))
    }
}

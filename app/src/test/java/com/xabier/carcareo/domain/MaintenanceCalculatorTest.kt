package com.xabier.carcareo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The mandatory test battery from spec section 9 (cases 1-9, 13; the record-level
 * cases 10-12 are in [PlanStatusCalculatorTest]).
 */
class MaintenanceCalculatorTest {

    private val today = LocalDate.of(2026, 6, 1)
    private val baseline = BaselineRef(date = LocalDate.of(2020, 1, 1), km = 0)

    private fun input(
        km: Int? = null,
        months: Int? = null,
        warnKm: Int = 1_000,
        warnDays: Int = 30,
        lastDone: Execution? = null,
    ) = TaskInput(1, km, months, warnKm, warnDays, lastDone)

    // 1
    @Test
    fun `km-only, well within interval, is OK`() {
        val r = MaintenanceCalculator.compute(
            input(km = 10_000, lastDone = Execution(today.minusMonths(1), 30_000)),
            baseline, currentKm = 33_000, today = today,
        )
        assertEquals(TaskStatus.OK, r.status)
        assertEquals(40_000, r.dueKm)
        assertEquals(7_000, r.kmRemaining)
        assertNull(r.daysRemaining)
    }

    // 2
    @Test
    fun `months-only, past due, is OVERDUE`() {
        val r = MaintenanceCalculator.compute(
            input(months = 12, lastDone = Execution(today.minusMonths(14), 20_000)),
            baseline, currentKm = 25_000, today = today,
        )
        assertEquals(TaskStatus.OVERDUE, r.status)
        assertTrue(r.daysRemaining!! < 0)
        assertNull(r.kmRemaining)
    }

    // 3
    @Test
    fun `both intervals, due by km first, is OVERDUE`() {
        val r = MaintenanceCalculator.compute(
            input(km = 15_000, months = 12, lastDone = Execution(today.minusMonths(3), 100_000)),
            baseline, currentKm = 120_000, today = today,
        )
        assertEquals(TaskStatus.OVERDUE, r.status)
        assertTrue(r.kmRemaining!! < 0)   // -5_000
        assertTrue(r.daysRemaining!! > 0) // still ~9 months of time left
    }

    // 4
    @Test
    fun `both intervals, due by time first, is OVERDUE`() {
        val r = MaintenanceCalculator.compute(
            input(km = 15_000, months = 12, lastDone = Execution(today.minusMonths(13), 100_000)),
            baseline, currentKm = 105_000, today = today,
        )
        assertEquals(TaskStatus.OVERDUE, r.status)
        assertTrue(r.daysRemaining!! < 0)
        assertTrue(r.kmRemaining!! > 0) // 10_000 km still to go
    }

    // 5
    @Test
    fun `within the km warning window is UPCOMING`() {
        val r = MaintenanceCalculator.compute(
            input(km = 10_000, warnKm = 1_000, lastDone = Execution(today.minusMonths(1), 50_000)),
            baseline, currentKm = 59_500, today = today,
        )
        assertEquals(TaskStatus.UPCOMING, r.status)
        assertEquals(500, r.kmRemaining!!)
    }

    // 6
    @Test
    fun `within the days warning window is UPCOMING`() {
        val r = MaintenanceCalculator.compute(
            input(months = 12, warnDays = 30, lastDone = Execution(today.minusMonths(12).plusDays(20), 10_000)),
            baseline, currentKm = 12_000, today = today,
        )
        assertEquals(TaskStatus.UPCOMING, r.status)
        val days = r.daysRemaining!!
        assertTrue(days in 0L..30L)
    }

    // 7
    @Test
    fun `no prior record is flagged and invents no last-done date`() {
        val r = MaintenanceCalculator.compute(
            input(km = 15_000, months = 12, lastDone = null),
            baseline = BaselineRef(LocalDate.of(2025, 1, 1), km = 5_000),
            currentKm = 12_000, today = today,
        )
        assertFalse(r.hasHistory)
        assertNull(r.lastDoneDate)
        assertNull(r.lastDoneKm)
        // Due points still derived from the baseline so ordering works.
        assertEquals(20_000, r.dueKm)
    }

    // 13
    @Test
    fun `plan is ordered by real urgency, not by interval size`() {
        val chain = input(km = 1_000, lastDone = Execution(today.minusMonths(1), 9_800)).copy(taskId = 1)
        val oilChange = input(km = 15_000, lastDone = Execution(today.minusMonths(1), 5_000)).copy(taskId = 2)
        val timingBelt = input(km = 120_000, lastDone = Execution(today.minusMonths(1), 10_000)).copy(taskId = 3)

        val ordered = MaintenanceCalculator.computePlan(
            listOf(oilChange, timingBelt, chain),
            baseline, currentKm = 10_500, today = today,
        )
        // chain: due at 10_800, 300 left of 1_000 -> 0.30 (most urgent)
        // oil:   due at 20_000, 9_500 left of 15_000 -> 0.63
        // belt:  due at 130_000, 119_500 left of 120_000 -> ~1.0
        assertEquals(listOf(1L, 2L, 3L), ordered.map { it.taskId })
    }

    @Test
    fun `worst status picks OVERDUE over UPCOMING over OK`() {
        assertEquals(
            TaskStatus.OVERDUE,
            MaintenanceCalculator.worstStatus(listOf(TaskStatus.OK, TaskStatus.OVERDUE, TaskStatus.UPCOMING)),
        )
        assertEquals(
            TaskStatus.UPCOMING,
            MaintenanceCalculator.worstStatus(listOf(TaskStatus.OK, TaskStatus.UPCOMING)),
        )
        assertNull(MaintenanceCalculator.worstStatus(emptyList()))
    }
}

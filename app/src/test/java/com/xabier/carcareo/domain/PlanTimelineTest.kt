package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.relation.RecordWithTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanTimelineTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun task(id: Long, km: Int? = null, months: Int? = null) =
        MaintenanceTask(
            id = id, vehicleId = 1, name = "Task $id",
            intervalKm = km, intervalMonths = months, sortOrder = id.toInt(),
        )

    private fun record(date: LocalDate, km: Int, tasks: List<MaintenanceTask>) =
        RecordWithTasks(
            record = MaintenanceRecord(id = km.toLong(), vehicleId = 1, date = date, odometerKm = km),
            tasks = tasks,
        )

    private fun status(tasks: List<MaintenanceTask>, records: List<RecordWithTasks>, annual: Int = 10_000) =
        PlanStatusCalculator.forVehicle(
            vehicle = vehicle(lastKm = 52_000, lastDate = today, annual = annual),
            tasks = tasks,
            records = records,
            today = today,
        )

    @Test
    fun `window is the 12k minimum when every task is near`() {
        val a = task(1, km = 10_000)   // done @50k -> 8k left
        val t = buildPlanTimeline(
            status(listOf(a), listOf(record(today.minusMonths(1), 50_000, listOf(a)))),
            annualKmEstimate = 10_000,
        )
        assertEquals(PlanTimeline.MIN_WINDOW_KM, t.windowKm)
        assertEquals(1, t.markers.size)
        assertEquals(8_000, t.markers.single().kmFromNow)
    }

    @Test
    fun `window expands to the furthest upcoming task, rounded up to 1k`() {
        val far = task(1, km = 40_500)  // done @50k -> 38 500 left
        val t = buildPlanTimeline(
            status(listOf(far), listOf(record(today.minusMonths(1), 50_000, listOf(far)))),
            annualKmEstimate = 10_000,
        )
        assertEquals(39_000, t.windowKm)
    }

    @Test
    fun `window is capped at 60k even when a task is much further out`() {
        val near = task(1, km = 8_000)    // done @50k -> 6k left
        val mid = task(2, km = 62_000)    // done @50k -> 60k left, right at the cap
        val far = task(3, km = 90_000)    // done @50k -> 88k left, beyond the cap
        val recs = listOf(
            record(today.minusMonths(1), 50_000, listOf(near)),
            record(today.minusMonths(1), 50_000, listOf(mid)),
            record(today.minusMonths(1), 50_000, listOf(far)),
        )
        val t = buildPlanTimeline(status(listOf(near, mid, far), recs), annualKmEstimate = 10_000)

        assertEquals(PlanTimeline.MAX_WINDOW_KM, t.windowKm)
        assertEquals(setOf(6_000, 60_000), t.markers.map { it.kmFromNow }.toSet())
        assertTrue(t.later.any { it.task.id == far.id })
    }

    @Test
    fun `all-overdue tasks collapse to one pinned marker`() {
        val a = task(1, km = 1_000)    // done @49k -> 2k over
        val b = task(2, km = 500)      // done @50k -> 1.5k over
        val recs = listOf(
            record(today.minusMonths(2), 49_000, listOf(a)),
            record(today.minusMonths(1), 50_000, listOf(b)),
        )
        val t = buildPlanTimeline(status(listOf(a, b), recs), annualKmEstimate = 10_000)

        assertEquals(PlanTimeline.MIN_WINDOW_KM, t.windowKm)
        val marker = t.markers.single()
        assertTrue(marker.pinnedLeft && marker.kmFromNow == 0)
        assertEquals(2, marker.clusterCount)
        assertEquals(2, t.dueNow.size)
        assertTrue(t.soon.isEmpty() && t.later.isEmpty())
        // most urgent is b: 1.5k over its 500 interval
        assertEquals(1_500, marker.overByKm)
    }

    @Test
    fun `a month-only task is projected onto the axis via the yearly estimate`() {
        val timed = task(1, months = 12)  // baseline today -> ~365 days -> ~10k km at annual 10k
        val t = buildPlanTimeline(
            status(listOf(timed), records = emptyList(), annual = 10_000),
            annualKmEstimate = 10_000,
        )
        val marker = t.markers.single()
        assertTrue("expected ~10k, was ${marker.kmFromNow}", marker.kmFromNow in 9_000..11_000)
    }

    @Test
    fun `a month-only task with no yearly estimate has no marker but still bucketed`() {
        val timed = task(1, months = 12)
        val t = buildPlanTimeline(
            status(listOf(timed), records = emptyList(), annual = 0),
            annualKmEstimate = null,
        )
        assertTrue(t.markers.isEmpty())
        assertEquals(1, t.dueNow.size + t.soon.size + t.later.size)
    }

    @Test
    fun `forward markers are capped and spaced, labels alternate sides`() {
        // 8 forward tasks ~3k..17k out, 2k apart; none overdue. Only
        // MAX_FORWARD_MARKERS are kept.
        val tasks = (1L..8L).map { task(it, km = (it * 2_000 + 3_000).toInt()) }
        val recs = tasks.map { record(today.minusMonths(1), 50_000, listOf(it)) }
        val t = buildPlanTimeline(status(tasks, recs), annualKmEstimate = 10_000)

        assertEquals(PlanTimeline.MAX_FORWARD_MARKERS, t.markers.size)
        assertEquals(listOf(true, false, true), t.markers.map { it.labelAbove })
    }

    @Test
    fun `near-identical forward tasks share a single marker`() {
        // three tasks all ~8k out (no history -> same baseline due) collapse to one
        val tasks = listOf(task(1, km = 10_000), task(2, km = 10_000), task(3, km = 10_000))
        val recs = tasks.map { record(today.minusMonths(1), 50_000, listOf(it)) }
        val t = buildPlanTimeline(status(tasks, recs), annualKmEstimate = 10_000)
        assertEquals(1, t.markers.size)
    }
}

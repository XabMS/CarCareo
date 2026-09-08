package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.relation.RecordWithTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanStatusCalculatorTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun task(id: Long, km: Int? = null, months: Int? = null, active: Boolean = true) =
        MaintenanceTask(
            id = id, vehicleId = 1, name = "Task $id",
            intervalKm = km, intervalMonths = months, active = active, sortOrder = id.toInt(),
        )

    private fun record(id: Long, date: LocalDate, km: Int, tasks: List<MaintenanceTask>) =
        RecordWithTasks(
            record = MaintenanceRecord(id = id, vehicleId = 1, date = date, odometerKm = km),
            tasks = tasks,
        )

    // 10 — one record covering several tasks resets all of them
    @Test
    fun `a multi-task record updates the last execution of every task it covers`() {
        val oil = task(1, km = 15_000)
        val air = task(2, km = 30_000)
        val service = record(1, today.minusMonths(1), km = 42_000, tasks = listOf(oil, air))

        val byTask = PlanStatusCalculator.mostRecentExecutionByTask(listOf(service))

        assertEquals(Execution(today.minusMonths(1), 42_000), byTask[1])
        assertEquals(Execution(today.minusMonths(1), 42_000), byTask[2])
    }

    // 11 — dropping the newest record puts the task back in its previous state
    @Test
    fun `deleting the last record returns the task to the previous one`() {
        val v = vehicle(lastKm = 60_000, lastDate = today, annual = 12_000)
        val oil = task(1, km = 10_000)
        val older = record(1, LocalDate.of(2026, 1, 1), km = 40_000, tasks = listOf(oil))
        val newest = record(2, LocalDate.of(2026, 5, 1), km = 55_000, tasks = listOf(oil))

        fun statusWith(records: List<RecordWithTasks>) =
            PlanStatusCalculator.forVehicle(v, listOf(oil), records, today)
                .activeOrdered.single().computation

        val before = statusWith(listOf(older, newest))
        assertEquals(65_000, before.dueKm)          // 55_000 + 10_000
        assertEquals(TaskStatus.OK, before.status)

        // The newest record is deleted; the task must fall back to the older one,
        // not to the vehicle baseline and not to a stale computed state.
        val after = statusWith(listOf(older))
        assertEquals(LocalDate.of(2026, 1, 1), after.lastDoneDate)
        assertEquals(50_000, after.dueKm)           // 40_000 + 10_000
        assertEquals(TaskStatus.OVERDUE, after.status)
        assertTrue(after.hasHistory)
    }

    // 11b — with every record gone, the task falls back to the vehicle baseline
    @Test
    fun `no records means the task uses the vehicle baseline`() {
        val v = vehicle(lastKm = 60_000, lastDate = today, annual = 12_000,
            purchaseDate = LocalDate.of(2024, 1, 1))
        val status = PlanStatusCalculator.forVehicle(
            vehicle = v,
            tasks = listOf(task(1, km = 15_000)),
            records = emptyList(),
            today = today,
        )
        val comp = status.activeOrdered.single().computation
        assertFalse(comp.hasHistory)
        // baseline km at purchase date, extrapolated back from 60_000 today
        val expectedBaseKm = OdometerEstimator.kmAt(60_000, today, 12_000, LocalDate.of(2024, 1, 1))
        assertEquals(expectedBaseKm + 15_000, comp.dueKm)
    }

    // 12 — a record with no tasks changes nothing
    @Test
    fun `a record covering no tasks contributes nothing`() {
        val puncture = record(9, today, km = 44_000, tasks = emptyList())
        val byTask = PlanStatusCalculator.mostRecentExecutionByTask(listOf(puncture))
        assertTrue(byTask.isEmpty())
    }

    @Test
    fun `most recent execution wins on date then on odometer`() {
        val t = task(1, km = 10_000)
        val older = record(1, today.minusMonths(2), km = 20_000, tasks = listOf(t))
        val newer = record(2, today.minusMonths(1), km = 25_000, tasks = listOf(t))
        val sameDayHigher = record(3, today.minusMonths(1), km = 25_400, tasks = listOf(t))

        val byTask = PlanStatusCalculator.mostRecentExecutionByTask(listOf(older, newer, sameDayHigher))
        assertEquals(Execution(today.minusMonths(1), 25_400), byTask[1])
    }

    // A second-hand vehicle (high baseline km, no records) must not report its
    // unrecorded tasks as OK — they need review until the user logs or disables them.
    @Test
    fun `used vehicle flags unrecorded tasks as needing attention`() {
        val v = vehicle(lastKm = 233_000, lastDate = today, annual = 12_000)
        val status = PlanStatusCalculator.forVehicle(
            vehicle = v,
            tasks = listOf(task(1, km = 120_000, months = 84)),
            records = emptyList(),
            today = today,
        )
        val comp = status.activeOrdered.single().computation
        assertFalse(comp.hasHistory)
        assertEquals(TaskStatus.UPCOMING, comp.status)
        assertEquals(TaskStatus.UPCOMING, status.vehicleStatus)
    }

    // A vehicle entered near delivery mileage (no purchase date given, so the
    // baseline date falls back to lastConfirmedKmDate and the baseline km is just
    // lastConfirmedKm) keeps the spec 4.2 behaviour: no records -> OK.
    @Test
    fun `vehicle entered near delivery mileage leaves unrecorded tasks as OK`() {
        val v = vehicle(lastKm = 400, lastDate = today, annual = 12_000)
        val status = PlanStatusCalculator.forVehicle(
            vehicle = v,
            tasks = listOf(task(1, km = 15_000, months = 12)),
            records = emptyList(),
            today = today,
        )
        assertEquals(TaskStatus.OK, status.activeOrdered.single().computation.status)
    }

    // The flag keys off the baseline odometer, not off whether a purchase date
    // was entered: a car first added at high mileage with no history is amber
    // even without a purchase date.
    @Test
    fun `high-mileage vehicle with no purchase date still flags unrecorded tasks`() {
        val v = vehicle(lastKm = 90_000, lastDate = today, annual = 12_000)
        val status = PlanStatusCalculator.forVehicle(
            vehicle = v,
            tasks = listOf(task(1, km = 15_000, months = 12)),
            records = emptyList(),
            today = today,
        )
        assertEquals(TaskStatus.UPCOMING, status.activeOrdered.single().computation.status)
    }

    @Test
    fun `vehicle status is the worst among active tasks and ignores inactive ones`() {
        val v = vehicle(lastKm = 50_000, lastDate = today, annual = 10_000)
        // last done at 49_000 km, interval 500 -> due at 49_500, already past 50_000
        val overdueActive = task(1, km = 500)
        val freshInactive = task(2, km = 500, active = false)

        val status = PlanStatusCalculator.forVehicle(
            vehicle = v,
            tasks = listOf(overdueActive, freshInactive),
            records = listOf(
                record(1, today.minusMonths(1), km = 49_000, tasks = listOf(overdueActive)),
            ),
            today = today,
        )
        assertEquals(TaskStatus.OVERDUE, status.vehicleStatus)
        assertEquals(1, status.activeOrdered.size)
        assertEquals(listOf(2L), status.inactive.map { it.id })
    }
}

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

    // 11 — with the last record gone, the task falls back to the baseline
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

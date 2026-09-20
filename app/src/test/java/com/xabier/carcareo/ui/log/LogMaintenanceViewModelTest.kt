@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.xabier.carcareo.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.data.repository.MaintenanceRecordRepository
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogMaintenanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var taskRepository: MaintenanceTaskRepository
    private lateinit var recordRepository: MaintenanceRecordRepository
    private var vehicleId: Long = 0

    // The ViewModel takes its clock, so "today" is whatever this fixture says it
    // is. Nothing here depends on the wall clock — an earlier version hardcoded
    // this date and started failing the day it went past.
    private val zone: ZoneId = ZoneId.of("Europe/Madrid")
    private val today: LocalDate = LocalDate.of(2026, 9, 14)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone)

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        // Route Room's own background work through the same test dispatcher so
        // advanceUntilIdle() actually waits for it — otherwise Room's real
        // executor threads race the test's virtual clock.
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            // Everything is deliberately routed through one deterministic test
            // dispatcher (see the class doc), so "main thread" here just means
            // "the single thread advanceUntilIdle() drives" — not a real UI thread.
            .allowMainThreadQueries()
            .build()
        vehicleRepository = VehicleRepository(db.vehicleDao())
        taskRepository = MaintenanceTaskRepository(db.maintenanceTaskDao())
        recordRepository = MaintenanceRecordRepository(db)

        vehicleId = db.vehicleDao().insert(
            Vehicle(
                name = "Test",
                category = VehicleCategory.COCHE_TERMICO,
                lastConfirmedKm = 50_000,
                lastConfirmedKmDate = today,
                annualKmEstimate = 12_000,
            ),
        )
        db.maintenanceTaskDao().insert(
            MaintenanceTask(vehicleId = vehicleId, name = "Overdue task", intervalKm = 1_000, sortOrder = 0),
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): LogMaintenanceViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val vm = LogMaintenanceViewModel(
            handle,
            vehicleRepository,
            taskRepository,
            recordRepository,
            clock,
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `overdue task is pre-selected on load`() = runTest(testDispatcher) {
        val vm = viewModel()
        assertFalse(vm.ui.value.loading)
        assertTrue(vm.ui.value.tasks.isNotEmpty())
        assertEquals(vm.ui.value.tasks.map { it.taskId }.toSet(), vm.ui.value.selectedTaskIds)
    }

    @Test
    fun `minor decrease saves without confirmation`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setOdometer("48000") // ~4% below 50 000 -> MINOR_DECREASE
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
    }

    @Test
    fun `major decrease is blocked until confirmed`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setOdometer("40000") // 20% below 50 000 -> MAJOR_DECREASE
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)

        vm.setMajorDecreaseConfirmed(true)
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
    }

    @Test
    fun `future date blocks save`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setOdometer("51000")
        vm.setDate(today.plusDays(1))
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertTrue(vm.ui.value.dateError)
    }

    @Test
    fun `the date field defaults to the clock's today, not the wall clock`() = runTest(testDispatcher) {
        val vm = viewModel()
        assertEquals(today, vm.ui.value.date)
    }

    @Test
    fun `invalid cost blocks save, valid comma-decimal cost does not`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setOdometer("51000")
        vm.setCost("12.3.4".filter { it.isDigit() || it == '.' }) // simulate a malformed paste
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertTrue(vm.ui.value.costError)

        vm.setCost("12,50")
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
    }

    @Test
    fun `a second save while saving is a no-op`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setOdometer("51000")
        var completions = 0
        vm.save { completions++ }
        // Fire a second save before the first (still in-flight) completes.
        vm.save { completions++ }
        advanceUntilIdle()
        assertEquals(1, completions)
    }
}

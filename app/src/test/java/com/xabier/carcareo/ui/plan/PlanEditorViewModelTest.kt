@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.xabier.carcareo.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.data.repository.MaintenanceTaskRepository
import com.xabier.carcareo.data.repository.VehicleRepository
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class PlanEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var taskRepository: MaintenanceTaskRepository
    private lateinit var vehicleRepository: VehicleRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .allowMainThreadQueries()
            .build()
        taskRepository = MaintenanceTaskRepository(db.maintenanceTaskDao())
        vehicleRepository = VehicleRepository(db.vehicleDao())

        vehicleId = db.vehicleDao().insert(
            Vehicle(
                name = "Test",
                category = VehicleCategory.COCHE_TERMICO,
                lastConfirmedKm = 10_000,
                lastConfirmedKmDate = LocalDate.of(2026, 1, 1),
                annualKmEstimate = 12_000,
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): PlanEditorViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val vm = PlanEditorViewModel(handle, taskRepository, vehicleRepository)
        // uiState is stateIn(WhileSubscribed) — it only runs while something is
        // collecting it, exactly like the real screen's collectAsStateWithLifecycle().
        // The collector job itself must not outlive this helper: runTest() waits
        // for every coroutine it launched to finish, and collect{} never
        // completes on its own. Cancelling it here doesn't stop the sharing —
        // WhileSubscribed keeps the upstream combine alive for its 5s grace
        // window, and this test never advances virtual time far enough to
        // trigger that timeout, so .value keeps updating regardless.
        val subscriber = launch { vm.uiState.collect {} }
        runCurrent()
        subscriber.cancel()
        return vm
    }

    @Test
    fun `duplicate name is rejected`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.saveTask(TaskDraft(name = "Oil change", intervalKm = "10000")) {}
        runCurrent()

        var result: TaskDraft? = null
        vm.saveTask(TaskDraft(name = "oil change", intervalKm = "5000")) { result = it }
        runCurrent()

        assertTrue(result?.duplicateNameError == true)
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun `at least one interval is required`() = runTest(testDispatcher) {
        val vm = viewModel()
        var result: TaskDraft? = null
        vm.saveTask(TaskDraft(name = "Oil change", intervalKm = "", intervalMonths = "")) { result = it }
        runCurrent()

        assertTrue(result?.intervalError == true)
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `valid task is saved`() = runTest(testDispatcher) {
        val vm = viewModel()
        var result: TaskDraft? = null
        vm.saveTask(TaskDraft(name = "Oil change", intervalKm = "10000")) { result = it }
        runCurrent()

        assertFalse(result?.hasError == true)
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun `applyTemplate adds the category's template tasks`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.applyTemplate(spanish = false)
        runCurrent()

        assertTrue(vm.uiState.value.tasks.isNotEmpty())
    }

    @Test
    fun `a second saveTask while saving is a no-op`() = runTest(testDispatcher) {
        val vm = viewModel()
        var completions = 0
        val draft = TaskDraft(name = "Oil change", intervalKm = "10000")
        vm.saveTask(draft) { completions++ }
        vm.saveTask(draft) { completions++ }
        runCurrent()

        assertEquals(1, completions)
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun `existing task can be added directly to seed a repository-add scenario`() = runTest(testDispatcher) {
        // Sanity check the fixture wiring: a plain repository add is reflected
        // in the same uiState the ViewModel exposes.
        taskRepository.add(MaintenanceTask(vehicleId = vehicleId, name = "Coolant", intervalKm = 30_000))
        val vm = viewModel()
        assertEquals(1, vm.uiState.value.tasks.size)
    }
}

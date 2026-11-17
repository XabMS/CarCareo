@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.xabier.carcareo.ui.vehicle

import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.ui.navigation.Destinations
import java.time.LocalDate
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.data.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
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
class VehicleFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: VehicleRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .allowMainThreadQueries()
            .build()
        repository = VehicleRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun newVehicleViewModel() = VehicleFormViewModel(SavedStateHandle(), repository)

    @Test
    fun `annual km defaults by category`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        assertEquals("8000", vm.state.value.annualKmEstimate) // MOTO_TERMICA default

        vm.setCategory(VehicleCategory.COCHE_TERMICO)
        assertEquals("10000", vm.state.value.annualKmEstimate)
    }

    @Test
    fun `editing annual km stops it tracking the category default`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.setAnnualKm("15000")
        vm.setCategory(VehicleCategory.COCHE_TERMICO)
        assertEquals("15000", vm.state.value.annualKmEstimate)
    }

    @Test
    fun `blank name is rejected`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.edit { it.copy(lastConfirmedKm = "1000") }
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertTrue(vm.state.value.nameError)
    }

    @Test
    fun `invalid km is rejected`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.edit { it.copy(name = "Test", lastConfirmedKm = "not a number") }
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertTrue(vm.state.value.kmError)
    }

    @Test
    fun `valid vehicle saves and reaches the repository`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.edit { it.copy(name = "Test", lastConfirmedKm = "1000") }
        var savedId: Long? = null
        vm.save { savedId = it }
        advanceUntilIdle()

        assertTrue(savedId != null)
        assertEquals("Test", repository.get(savedId!!)?.name)
    }

    @Test
    fun `a second tap while saving does not add the vehicle twice`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.edit { it.copy(name = "Test", lastConfirmedKm = "1000") }
        var calls = 0
        vm.save { calls++ }
        vm.save { calls++ } // lands before the first insert has run
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(1, db.vehicleDao().getAll().size)
    }

    @Test
    fun `a new vehicle freezes its first reading`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        val readOn = LocalDate.now().minusDays(3)
        vm.edit { it.copy(name = "Test", lastConfirmedKm = "1234", lastConfirmedKmDate = readOn) }
        var id = 0L
        vm.save { id = it }
        advanceUntilIdle()

        val stored = repository.get(id)!!
        assertEquals(1234, stored.initialKm)
        assertEquals(readOn, stored.initialKmDate)
    }

    @Test
    fun `future reading date and misplaced purchase date are rejected`() = runTest(testDispatcher) {
        val vm = newVehicleViewModel()
        advanceUntilIdle()
        vm.edit {
            it.copy(name = "Test", lastConfirmedKm = "1000", lastConfirmedKmDate = LocalDate.now().plusDays(2))
        }
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertTrue(vm.state.value.readDateError)

        val readOn = LocalDate.now().minusDays(10)
        vm.edit { it.copy(lastConfirmedKmDate = readOn, purchaseDate = readOn.plusDays(1)) }
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertTrue(vm.state.value.purchaseDateError)
    }

    @Test
    fun `lowering the reading a lot when editing needs confirmation`() = runTest(testDispatcher) {
        val firstDate = LocalDate.now().minusYears(1)
        val id = repository.add(
            Vehicle(
                name = "Test",
                category = VehicleCategory.COCHE_TERMICO,
                lastConfirmedKm = 50_000,
                lastConfirmedKmDate = LocalDate.now().minusDays(5),
                initialKm = 40_000,
                initialKmDate = firstDate,
            ),
        )
        val vm = VehicleFormViewModel(SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to id)), repository)
        advanceUntilIdle()

        vm.edit { it.copy(lastConfirmedKm = "30000") }
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)

        vm.edit { it.copy(majorDecreaseConfirmed = true) }
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)

        val stored = repository.get(id)!!
        assertEquals(30_000, stored.lastConfirmedKm)
        // The vehicle had moved on from its first reading, so that stays frozen.
        assertEquals(40_000, stored.initialKm)
        assertEquals(firstDate, stored.initialKmDate)
    }
}

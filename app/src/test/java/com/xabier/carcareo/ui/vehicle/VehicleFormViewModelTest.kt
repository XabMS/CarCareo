@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.xabier.carcareo.ui.vehicle

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
        repository = VehicleRepository(db.vehicleDao())
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
}

package com.xabier.carcareo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.DataHooks
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VehicleRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: VehicleRepository
    private val released = mutableListOf<String>()
    private val staleAlerts = mutableListOf<Long>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = VehicleRepository(
            db,
            object : DataHooks {
                override fun onAttachmentUnused(uri: String) { released += uri }
                override fun onVehicleAlertStale(vehicleId: Long) { staleAlerts += vehicleId }
            },
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addVehicle(name: String): Long = repository.add(
        Vehicle(
            name = name,
            category = VehicleCategory.COCHE_TERMICO,
            lastConfirmedKm = 10_000,
            lastConfirmedKmDate = LocalDate.of(2026, 6, 1),
        ),
    )

    @Test
    fun `delete removes the vehicle with its plan and history, and only its own attachments`() = runTest {
        val doomed = addVehicle("Doomed")
        val kept = addVehicle("Kept")
        db.maintenanceTaskDao().insert(MaintenanceTask(vehicleId = doomed, name = "Oil", intervalKm = 10_000))
        val date = LocalDate.of(2026, 6, 2)
        db.maintenanceRecordDao().insert(
            MaintenanceRecord(vehicleId = doomed, date = date, odometerKm = 10_100, attachmentUri = "content://a"),
        )
        db.maintenanceRecordDao().insert(
            MaintenanceRecord(vehicleId = doomed, date = date, odometerKm = 10_200, attachmentUri = "content://shared"),
        )
        db.maintenanceRecordDao().insert(
            MaintenanceRecord(vehicleId = kept, date = date, odometerKm = 10_300, attachmentUri = "content://shared"),
        )

        repository.delete(doomed)

        assertNull(repository.get(doomed))
        assertTrue(db.maintenanceTaskDao().getForVehicle(doomed).isEmpty())
        assertTrue(db.maintenanceRecordDao().getForVehicle(doomed).isEmpty())
        assertEquals(1, db.maintenanceRecordDao().getForVehicle(kept).size)
        assertEquals(listOf("content://a"), released)
        assertEquals(listOf(doomed), staleAlerts)
    }

    @Test
    fun `archiving clears the vehicle's alert, unarchiving does not`() = runTest {
        val id = addVehicle("Sold")
        repository.setArchived(id, archived = true)
        assertTrue(repository.get(id)!!.archived)
        assertEquals(listOf(id), staleAlerts)

        repository.setArchived(id, archived = false)
        assertEquals(false, repository.get(id)!!.archived)
        assertEquals(listOf(id), staleAlerts)
    }

    @Test
    fun `confirmOdometer touches only the reading`() = runTest {
        val id = addVehicle("Daily")
        repository.confirmOdometer(id, 12_345, LocalDate.of(2026, 8, 1))
        val vehicle = repository.get(id)!!
        assertEquals(12_345, vehicle.lastConfirmedKm)
        assertEquals(LocalDate.of(2026, 8, 1), vehicle.lastConfirmedKmDate)
        assertEquals("Daily", vehicle.name)
    }
}

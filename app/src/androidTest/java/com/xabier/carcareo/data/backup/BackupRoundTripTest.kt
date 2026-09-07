package com.xabier.carcareo.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.RecordTaskCrossRef
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Spec section 9 integration test: export -> import produces equivalent data,
 * including the record<->task links.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private lateinit var db: AppDatabase
    private lateinit var backup: BackupRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        backup = BackupRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun exportThenReplaceImportIsEquivalent() = runTest {
        val vehicleDao = db.vehicleDao()
        val taskDao = db.maintenanceTaskDao()
        val recordDao = db.maintenanceRecordDao()

        val vId = vehicleDao.insert(
            Vehicle(
                name = "Hornet",
                category = VehicleCategory.MOTO_TERMICA,
                make = "Honda",
                lastConfirmedKm = 24_500,
                lastConfirmedKmDate = LocalDate.of(2026, 3, 14),
                annualKmEstimate = 9_000,
                purchaseDate = LocalDate.of(2022, 6, 1),
            ),
        )
        val oilId = taskDao.insert(
            MaintenanceTask(vehicleId = vId, name = "Oil + filter", intervalKm = 8_000, intervalMonths = 12, sortOrder = 0),
        )
        val airId = taskDao.insert(
            MaintenanceTask(vehicleId = vId, name = "Air filter", intervalKm = 12_000, sortOrder = 1),
        )
        val serviceId = recordDao.insert(
            MaintenanceRecord(vehicleId = vId, date = LocalDate.of(2026, 3, 14), odometerKm = 24_500, cost = BigDecimal("189.50")),
        )
        recordDao.insertCrossRefs(
            listOf(
                RecordTaskCrossRef(serviceId, oilId),
                RecordTaskCrossRef(serviceId, airId),
            ),
        )
        // Unplanned record with no task links (spec: must survive).
        recordDao.insert(
            MaintenanceRecord(vehicleId = vId, date = LocalDate.of(2025, 9, 1), odometerKm = 18_000),
        )

        val json = backup.exportJson()
        val imported = backup.importJson(json, ImportMode.REPLACE)
        assertEquals(1, imported)

        val vehicles = vehicleDao.getAll()
        assertEquals(1, vehicles.size)
        val v = vehicles.single()
        assertEquals("Hornet", v.name)
        assertEquals(LocalDate.of(2022, 6, 1), v.purchaseDate)

        val tasks = taskDao.getForVehicle(v.id).sortedBy { it.sortOrder }
        assertEquals(listOf("Oil + filter", "Air filter"), tasks.map { it.name })

        val records = recordDao.getWithTasksForVehicle(v.id)
        assertEquals(2, records.size)

        val service = records.first { it.record.odometerKm == 24_500 }
        assertEquals(BigDecimal("189.50"), service.record.cost)
        assertEquals(setOf("Oil + filter", "Air filter"), service.tasks.map { it.name }.toSet())

        val unplanned = records.first { it.record.odometerKm == 18_000 }
        assertTrue(unplanned.tasks.isEmpty())
    }
}

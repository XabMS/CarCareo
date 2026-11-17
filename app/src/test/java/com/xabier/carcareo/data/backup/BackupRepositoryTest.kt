package com.xabier.carcareo.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.RecordTaskCrossRef
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** JVM counterpart of the instrumented `BackupRoundTripTest`, so it runs in CI. */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

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

    private suspend fun seed(): Long {
        val vehicleId = db.vehicleDao().insert(
            Vehicle(
                name = "Moto",
                category = VehicleCategory.MOTO_TERMICA,
                lastConfirmedKm = 20_000,
                lastConfirmedKmDate = LocalDate.of(2026, 6, 1),
                annualKmEstimate = 8_000,
                initialKm = 3_000,
                initialKmDate = LocalDate.of(2024, 1, 10),
            ),
        )
        val oil = db.maintenanceTaskDao().insert(
            MaintenanceTask(vehicleId = vehicleId, name = "Aceite", intervalKm = 8_000, sortOrder = 0),
        )
        val older = db.maintenanceRecordDao().insert(
            MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2025, 3, 1), odometerKm = 11_000),
        )
        val newer = db.maintenanceRecordDao().insert(
            MaintenanceRecord(
                vehicleId = vehicleId, date = LocalDate.of(2026, 3, 1), odometerKm = 19_000,
                cost = BigDecimal("89.90"),
            ),
        )
        db.maintenanceRecordDao().insertCrossRefs(
            listOf(RecordTaskCrossRef(older, oil), RecordTaskCrossRef(newer, oil)),
        )
        return vehicleId
    }

    @Test
    fun `export then replace-import restores everything, first reading included`() = runTest {
        seed()
        val json = backup.exportJson()

        val result = backup.importJson(json, ImportMode.REPLACE)
        assertEquals(ImportResult(vehicles = 1, unresolvedLinks = 0), result)

        val vehicle = db.vehicleDao().getAll().single()
        assertEquals("Moto", vehicle.name)
        assertEquals(3_000, vehicle.initialKm)
        assertEquals(LocalDate.of(2024, 1, 10), vehicle.initialKmDate)

        val records = db.maintenanceRecordDao().getWithTasksForVehicle(vehicle.id)
        // Oldest first, each still linked to the task by name.
        assertEquals(listOf(11_000, 19_000), records.map { it.record.odometerKm })
        assertEquals(listOf("Aceite", "Aceite"), records.map { it.tasks.single().name })
        assertEquals(BigDecimal("89.90"), records.last().record.cost)
    }

    @Test
    fun `a file without the first-reading fields falls back to the confirmed reading`() = runTest {
        val json = """
            {"schemaVersion":1,"exportedAt":"2026-01-01T00:00:00Z","vehicles":[
              {"name":"Legacy","category":"COCHE_TERMICO","lastConfirmedKm":70000,
               "lastConfirmedKmDate":"2025-12-01","annualKmEstimate":10000}
            ]}
        """.trimIndent()

        backup.importJson(json, ImportMode.ADD)

        val vehicle = db.vehicleDao().getAll().single()
        assertEquals(70_000, vehicle.initialKm)
        assertEquals(LocalDate.of(2025, 12, 1), vehicle.initialKmDate)
    }

    @Test
    fun `names already in the garage are reported before an add-import`() = runTest {
        seed()
        val json = backup.exportJson()
        assertEquals(listOf("Moto"), backup.namesAlreadyPresent(json))

        db.vehicleDao().deleteAll()
        assertEquals(emptyList<String>(), backup.namesAlreadyPresent(json))
    }
}

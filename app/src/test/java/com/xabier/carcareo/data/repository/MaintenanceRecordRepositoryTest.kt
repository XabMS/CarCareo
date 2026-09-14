package com.xabier.carcareo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises [MaintenanceRecordRepository] against a real in-memory Room DB —
 * covers the odometer-confirmation behavior end-to-end and the B.1 transaction
 * fix (a `save`/vehicle-update pair is now atomic).
 */
@RunWith(RobolectricTestRunner::class)
class MaintenanceRecordRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MaintenanceRecordRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = MaintenanceRecordRepository(db)
        vehicleId = db.vehicleDao().insert(
            Vehicle(
                name = "Test",
                category = VehicleCategory.COCHE_TERMICO,
                lastConfirmedKm = 50_000,
                lastConfirmedKmDate = LocalDate.of(2026, 6, 1),
                annualKmEstimate = 12_000,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `save with a newer date confirms the odometer`() = runTest {
        repository.save(
            MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 6, 15), odometerKm = 51_000),
            emptyList(),
        )
        val vehicle = db.vehicleDao().get(vehicleId)!!
        assertEquals(51_000, vehicle.lastConfirmedKm)
        assertEquals(LocalDate.of(2026, 6, 15), vehicle.lastConfirmedKmDate)
    }

    @Test
    fun `save with a same-day lower reading does not rewind the odometer`() = runTest {
        repository.save(
            MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 6, 1), odometerKm = 49_000),
            emptyList(),
        )
        val vehicle = db.vehicleDao().get(vehicleId)!!
        assertEquals(50_000, vehicle.lastConfirmedKm)
    }

    @Test
    fun `delete rolls the odometer back to the newest remaining record`() = runTest {
        repository.save(
            MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 5, 1), odometerKm = 45_000),
            emptyList(),
        )
        val latest = MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 7, 1), odometerKm = 52_000)
        val latestId = repository.save(latest, emptyList())
        assertEquals(52_000, db.vehicleDao().get(vehicleId)!!.lastConfirmedKm)

        repository.delete(latest.copy(id = latestId))

        val vehicle = db.vehicleDao().get(vehicleId)!!
        assertEquals(45_000, vehicle.lastConfirmedKm)
    }

    @Test
    fun `concurrent saves never let a stale read overwrite the actually-newer reading`() = runTest {
        // If these two saves' read-then-write sequences interleave (no
        // transaction), whichever commits last can act on a stale vehicle
        // snapshot that never saw the other's write — e.g. the June 9th record
        // reads the original baseline (June 1st), sees itself as "newer" and
        // wins, even though the June 10th record is chronologically later.
        // With B.1's `db.withTransaction`, Room serializes the two full
        // read+write sequences, so whichever runs second always reads the
        // first's committed result and defers correctly.
        val newer = async {
            repository.save(
                MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 6, 10), odometerKm = 51_000),
                emptyList(),
            )
        }
        val olderButRacy = async {
            repository.save(
                MaintenanceRecord(vehicleId = vehicleId, date = LocalDate.of(2026, 6, 9), odometerKm = 100_000),
                emptyList(),
            )
        }
        awaitAll(newer, olderButRacy)

        val records = db.maintenanceRecordDao().getForVehicle(vehicleId)
        assertEquals(2, records.size)

        // Whichever save actually ran last, the vehicle must end up confirmed
        // at the chronologically later (June 10th) reading — never the
        // June 9th one, regardless of execution order.
        val vehicle = db.vehicleDao().get(vehicleId)!!
        assertEquals(51_000, vehicle.lastConfirmedKm)
        assertEquals(LocalDate.of(2026, 6, 10), vehicle.lastConfirmedKmDate)
    }
}

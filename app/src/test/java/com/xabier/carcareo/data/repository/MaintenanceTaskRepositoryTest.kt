package com.xabier.carcareo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceTask
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
 * Exercises [MaintenanceTaskRepository] against a real in-memory Room DB —
 * covers the B.2 transaction fix (`insertAppending`/`insertAllAppending`
 * closing the read-then-write `sortOrder` race).
 */
@RunWith(RobolectricTestRunner::class)
class MaintenanceTaskRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MaintenanceTaskRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = MaintenanceTaskRepository(db.maintenanceTaskDao())
        vehicleId = db.vehicleDao().insert(
            Vehicle(
                name = "Test",
                category = VehicleCategory.COCHE_TERMICO,
                lastConfirmedKm = 0,
                lastConfirmedKmDate = LocalDate.of(2026, 1, 1),
                annualKmEstimate = 12_000,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun task(name: String) = MaintenanceTask(vehicleId = vehicleId, name = name, intervalKm = 10_000)

    @Test
    fun `add assigns strictly increasing sortOrder`() = runTest {
        repository.add(task("Oil change"))
        repository.add(task("Air filter"))
        repository.add(task("Spark plugs"))

        val orders = repository.getForVehicle(vehicleId).sortedBy { it.sortOrder }.map { it.sortOrder }
        assertEquals(listOf(0, 1, 2), orders)
    }

    @Test
    fun `concurrent adds never collide on sortOrder`() = runTest {
        val names = (1..20).map { "Task $it" }
        val jobs = names.map { name -> async { repository.add(task(name)) } }
        awaitAll(*jobs.toTypedArray())

        val tasks = repository.getForVehicle(vehicleId)
        assertEquals(names.size, tasks.size)
        assertEquals(
            "every task must get a distinct sortOrder",
            tasks.size,
            tasks.map { it.sortOrder }.toSet().size,
        )
    }

    @Test
    fun `applyTemplate skips a task whose name the vehicle already has`() = runTest {
        // Every built-in template includes an oil-change-style task; adding one
        // under the exact name a template would use must not create a duplicate.
        val templateNames = com.xabier.carcareo.domain.template.MaintenanceTemplates
            .instantiate(VehicleCategory.COCHE_TERMICO, vehicleId, spanish = false)
            .map { it.name }
        val firstTemplateName = templateNames.first()
        repository.add(task(firstTemplateName))

        repository.applyTemplate(vehicleId, VehicleCategory.COCHE_TERMICO, spanish = false)

        val tasks = repository.getForVehicle(vehicleId)
        val matches = tasks.count { it.name.equals(firstTemplateName, ignoreCase = true) }
        assertEquals("template must not duplicate an existing name", 1, matches)
        assertEquals("the pre-added task plus the rest of the template", templateNames.size, tasks.size)
    }
}

package com.xabier.carcareo.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun sample() = BackupFile(
        schemaVersion = 1,
        exportedAt = "2026-09-06T21:00:00Z",
        vehicles = listOf(
            BackupVehicle(
                name = "Hornet",
                category = "MOTO_TERMICA",
                make = "Honda",
                lastConfirmedKm = 24_500,
                lastConfirmedKmDate = "2026-03-14",
                annualKmEstimate = 9_000,
                purchaseDate = "2022-06-01",
                tasks = listOf(
                    BackupTask(name = "Aceite motor y filtro", intervalKm = 8_000, intervalMonths = 12),
                    BackupTask(name = "Filtro de aire", intervalKm = 12_000),
                ),
                records = listOf(
                    BackupRecord(
                        date = "2026-03-14",
                        odometerKm = 24_500,
                        cost = "189.50",
                        taskNames = listOf("Aceite motor y filtro", "Filtro de aire"),
                    ),
                    BackupRecord(date = "2025-09-01", odometerKm = 18_000, taskNames = emptyList()),
                ),
            ),
        ),
    )

    @Test
    fun `round trip preserves data including record-task links`() {
        val original = sample()
        val restored = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original, restored)

        val record = restored.vehicles.single().records.first()
        assertEquals(listOf("Aceite motor y filtro", "Filtro de aire"), record.taskNames)
        assertEquals("189.50", record.cost)
    }

    @Test
    fun `output is human readable and language neutral`() {
        val json = BackupCodec.encode(sample())
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("MOTO_TERMICA"))     // enum as constant
        assertTrue(json.contains("2026-03-14"))       // ISO date
        assertTrue(json.contains("189.50"))           // dot decimal
        assertTrue(json.contains("\n"))               // pretty printed
    }

    @Test
    fun `malformed json is rejected`() {
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode("{ not json") }
        assertEquals(BackupError.MALFORMED, e.error)
    }

    @Test
    fun `unknown category is rejected as malformed`() {
        val json = BackupCodec.encode(
            sample().let { it.copy(vehicles = listOf(it.vehicles[0].copy(category = "SPACESHIP"))) },
        )
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(json) }
        assertEquals(BackupError.MALFORMED, e.error)
    }

    @Test
    fun `task without any interval is rejected`() {
        val json = BackupCodec.encode(
            sample().let {
                it.copy(
                    vehicles = listOf(
                        it.vehicles[0].copy(
                            tasks = listOf(BackupTask(name = "Nothing", intervalKm = null, intervalMonths = null)),
                        ),
                    ),
                )
            },
        )
        assertThrows(BackupException::class.java) { BackupCodec.decode(json) }
    }

    @Test
    fun `two tasks with the same name in one vehicle are rejected`() {
        // Records link to tasks by name within the vehicle, so this file cannot be
        // imported without guessing which "Aceite motor" a record meant.
        val json = BackupCodec.encode(
            sample().let {
                it.copy(
                    vehicles = listOf(
                        it.vehicles[0].copy(
                            tasks = listOf(
                                BackupTask(name = "Aceite motor", intervalKm = 8_000),
                                BackupTask(name = "aceite MOTOR", intervalMonths = 12),
                            ),
                        ),
                    ),
                )
            },
        )
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(json) }
        assertEquals(BackupError.MALFORMED, e.error)
    }

    @Test
    fun `newer schema version is rejected distinctly`() {
        val json = BackupCodec.encode(sample().copy(schemaVersion = 99))
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(json) }
        assertEquals(BackupError.UNSUPPORTED_VERSION, e.error)
    }
}

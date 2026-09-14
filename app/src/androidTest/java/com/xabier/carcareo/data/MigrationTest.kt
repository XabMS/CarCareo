package com.xabier.carcareo.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers [MIGRATION_1_2] — the unique `(vehicleId, name)` index on
 * `maintenance_task`. No known install should have a duplicate (the UI and
 * backup import both already reject them), but the migration heals one
 * anyway rather than assume it; this pins that healing behavior down.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun duplicateTaskNamesAreMergedAndCrossRefsRepointed() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO vehicle (id, name, category, lastConfirmedKm, lastConfirmedKmDate, annualKmEstimate, archived) " +
                    "VALUES (1, 'Test', 'COCHE_TERMICO', 1000, '2026-01-01', 10000, 0)",
            )
            // Two "Oil change" rows for the same vehicle — id 10 is the survivor (lowest id).
            execSQL(
                "INSERT INTO maintenance_task (id, vehicleId, name, intervalKm, warnKmBefore, warnDaysBefore, active, sortOrder) " +
                    "VALUES (10, 1, 'Oil change', 10000, 1000, 30, 1, 0)",
            )
            execSQL(
                "INSERT INTO maintenance_task (id, vehicleId, name, intervalKm, warnKmBefore, warnDaysBefore, active, sortOrder) " +
                    "VALUES (11, 1, 'Oil change', 10000, 1000, 30, 1, 1)",
            )
            execSQL(
                "INSERT INTO maintenance_record (id, vehicleId, date, odometerKm) VALUES (100, 1, '2026-01-01', 1000)",
            )
            // The record's link points at the duplicate that should be deleted (id 11).
            execSQL("INSERT INTO record_task_cross_ref (recordId, taskId) VALUES (100, 11)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        migrated.query("SELECT id FROM maintenance_task WHERE vehicleId = 1 AND name = 'Oil change'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(10L, cursor.getLong(0))
        }
        migrated.query("SELECT taskId FROM record_task_cross_ref WHERE recordId = 100").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(10L, cursor.getLong(0))
        }
        migrated.close()
    }

    @Test
    fun alreadyUniqueTaskNamesSurviveUntouched() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO vehicle (id, name, category, lastConfirmedKm, lastConfirmedKmDate, annualKmEstimate, archived) " +
                    "VALUES (1, 'Test', 'COCHE_TERMICO', 1000, '2026-01-01', 10000, 0)",
            )
            execSQL(
                "INSERT INTO maintenance_task (id, vehicleId, name, intervalKm, warnKmBefore, warnDaysBefore, active, sortOrder) " +
                    "VALUES (10, 1, 'Oil change', 10000, 1000, 30, 1, 0)",
            )
            execSQL(
                "INSERT INTO maintenance_task (id, vehicleId, name, intervalKm, warnKmBefore, warnDaysBefore, active, sortOrder) " +
                    "VALUES (11, 1, 'Air filter', 12000, 1000, 30, 1, 1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        migrated.query("SELECT COUNT(*) FROM maintenance_task WHERE vehicleId = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        migrated.close()
    }
}

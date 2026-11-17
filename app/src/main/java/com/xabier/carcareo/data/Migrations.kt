package com.xabier.carcareo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the unique `(vehicleId, name)` constraint on `maintenance_task` (spec 3.2:
 * records link to tasks by name within a vehicle, so a duplicate name is not a
 * valid state). No known install should have a duplicate — the UI and backup
 * import both already reject them — but the migration heals one anyway rather
 * than assume it: any record still pointing at a "loser" duplicate is repointed
 * to the surviving row before that duplicate is deleted, so no history is lost
 * to the `ON DELETE CASCADE` on `record_task_cross_ref.taskId`.
 */
object MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE record_task_cross_ref SET taskId = (
                SELECT MIN(t2.id) FROM maintenance_task t2, maintenance_task t1
                WHERE t1.id = record_task_cross_ref.taskId
                  AND t2.vehicleId = t1.vehicleId AND t2.name = t1.name
            )
            WHERE taskId NOT IN (
                SELECT MIN(id) FROM maintenance_task GROUP BY vehicleId, name
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            DELETE FROM maintenance_task WHERE id NOT IN (
                SELECT MIN(id) FROM maintenance_task GROUP BY vehicleId, name
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_maintenance_task_vehicleId_name` " +
                "ON `maintenance_task` (`vehicleId`, `name`)",
        )
    }
}

/**
 * Adds the frozen "first reading" columns on `vehicle` (see `Vehicle.initialKm`).
 * The true first reading of an existing vehicle is no longer known, so it is
 * back-filled with the best fact available: the current confirmed odometer.
 * From here on it stays put, which is the point of the columns.
 */
object MIGRATION_2_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `initialKm` INTEGER")
        db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `initialKmDate` TEXT")
        db.execSQL(
            "UPDATE `vehicle` SET `initialKm` = `lastConfirmedKm`, " +
                "`initialKmDate` = `lastConfirmedKmDate`",
        )
    }
}

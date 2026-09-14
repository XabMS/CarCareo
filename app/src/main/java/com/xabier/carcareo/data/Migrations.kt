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

package com.xabier.carcareo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One line of a vehicle's maintenance plan. See section 3.2 of the spec.
 *
 * Validation rule (enforced in the domain layer, not by Room): at least one of
 * [intervalKm] / [intervalMonths] must be set. If both are set, whichever comes
 * first wins.
 */
@Entity(
    tableName = "maintenance_task",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId")],
)
data class MaintenanceTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    /** Margin before the km due point at which the task turns "upcoming". */
    val warnKmBefore: Int = 1_000,
    /** Margin before the date due point at which the task turns "upcoming". */
    val warnDaysBefore: Int = 30,
    val notes: String? = null,
    /** Deactivate a task without losing its history. */
    val active: Boolean = true,
    /** Manual ordering inside the plan editor. */
    val sortOrder: Int = 0,
)

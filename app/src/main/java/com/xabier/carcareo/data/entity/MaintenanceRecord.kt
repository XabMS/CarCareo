package com.xabier.carcareo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One maintenance intervention that actually happened. See section 3.3.
 *
 * Every record carries an [odometerKm]: saving a record confirms the vehicle's
 * odometer at [date]. A record may cover zero tasks (unplanned work: puncture,
 * breakdown) and still be valid history.
 */
@Entity(
    tableName = "maintenance_record",
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
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometerKm: Int,
    val workshop: String? = null,
    val cost: BigDecimal? = null,
    val notes: String? = null,
    /** Persisted SAF URI of an invoice photo / PDF. */
    val attachmentUri: String? = null,
)

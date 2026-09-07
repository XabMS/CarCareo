package com.xabier.carcareo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A vehicle the user owns. See section 3.1 of the spec.
 *
 * [lastConfirmedKm] / [lastConfirmedKmDate] are the only odometer facts we treat
 * as real; everything shown elsewhere in the app that looks like "current km" is
 * an estimate derived from these two plus [annualKmEstimate].
 */
@Entity(tableName = "vehicle")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: VehicleCategory,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val plate: String? = null,
    val lastConfirmedKm: Int,
    val lastConfirmedKmDate: LocalDate,
    /** Used to estimate the current km between confirmations. Spec default: 12000. */
    val annualKmEstimate: Int = 12_000,
    val purchaseDate: LocalDate? = null,
    val notes: String? = null,
    /** Sold vehicle: hidden from the garage but never deleted. */
    val archived: Boolean = false,
)

enum class VehicleCategory {
    MOTO_TERMICA,
    COCHE_TERMICO,
    COCHE_ELECTRICO,
}

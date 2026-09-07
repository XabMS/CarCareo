package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import java.time.LocalDate

/** Minimal [Vehicle] for domain tests — only the fields the calculations read. */
fun vehicle(
    id: Long = 1,
    lastKm: Int,
    lastDate: LocalDate,
    annual: Int = 12_000,
    purchaseDate: LocalDate? = null,
    category: VehicleCategory = VehicleCategory.COCHE_TERMICO,
): Vehicle = Vehicle(
    id = id,
    name = "Test",
    category = category,
    lastConfirmedKm = lastKm,
    lastConfirmedKmDate = lastDate,
    annualKmEstimate = annual,
    purchaseDate = purchaseDate,
)

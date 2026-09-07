package com.xabier.carcareo.domain

import com.xabier.carcareo.data.entity.Vehicle
import java.time.LocalDate

/**
 * The vehicle's odometer as of [today]: the confirmed value if confirmed today,
 * otherwise an estimate. See [OdometerReading] and spec 4.1.
 */
fun Vehicle.odometerReading(today: LocalDate = LocalDate.now()): OdometerReading {
    val km = OdometerEstimator.estimatedKm(
        lastConfirmedKm = lastConfirmedKm,
        lastConfirmedKmDate = lastConfirmedKmDate,
        annualKmEstimate = annualKmEstimate,
        today = today,
    )
    return OdometerReading(
        km = km,
        // Anything not confirmed *today* is an estimate, even if the interpolated
        // value happens to equal the last confirmed one.
        isEstimate = today.isAfter(lastConfirmedKmDate),
        confirmedOn = lastConfirmedKmDate,
        asOf = today,
    )
}

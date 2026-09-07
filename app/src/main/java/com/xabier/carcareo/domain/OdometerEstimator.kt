package com.xabier.carcareo.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * Odometer estimation — spec section 4.1.
 *
 * Between two user-confirmed odometer readings we don't know the real mileage,
 * so we linearly interpolate from the last confirmed value using the vehicle's
 * declared annual estimate. This is pure arithmetic with no Android / Room
 * dependency so it can be unit-tested in isolation.
 */
object OdometerEstimator {

    private const val DAYS_PER_YEAR = 365.0

    /**
     * Estimated current km:
     *   lastConfirmedKm + round(annualKmEstimate * daysSince / 365)
     *
     * Never decreases: a confirmation date in the future (clock skew, bad input)
     * or a negative annual estimate can only yield the confirmed value, not less.
     */
    fun estimatedKm(
        lastConfirmedKm: Int,
        lastConfirmedKmDate: LocalDate,
        annualKmEstimate: Int,
        today: LocalDate,
    ): Int {
        val days = ChronoUnit.DAYS.between(lastConfirmedKmDate, today)
        if (days <= 0L) return lastConfirmedKm
        val added = (annualKmEstimate.toDouble() * days / DAYS_PER_YEAR).roundToLong()
        return lastConfirmedKm + added.coerceAtLeast(0L).toInt()
    }

    /**
     * Interpolated km at an arbitrary [date], which may be BEFORE the confirmed
     * date (extrapolating backwards). Floored at 0. Used to guess the odometer at
     * vehicle acquisition when a task has no history (spec 4.2) — NOT for the
     * "current km" shown to the user, which must never decrease ([estimatedKm]).
     */
    fun kmAt(
        lastConfirmedKm: Int,
        lastConfirmedKmDate: LocalDate,
        annualKmEstimate: Int,
        date: LocalDate,
    ): Int {
        val days = ChronoUnit.DAYS.between(lastConfirmedKmDate, date)
        val delta = (annualKmEstimate.toDouble() * days / DAYS_PER_YEAR).roundToLong()
        return (lastConfirmedKm + delta).coerceAtLeast(0L).toInt()
    }
}

/**
 * A km value ready for display, carrying whether it is a real (confirmed) figure
 * or an estimate, and how stale the confirmation is. The UI must present these
 * two cases differently (spec 4.1): never show an estimate as if it were fact.
 */
data class OdometerReading(
    val km: Int,
    val isEstimate: Boolean,
    val confirmedOn: LocalDate,
    val asOf: LocalDate,
) {
    val daysSinceConfirmed: Long
        get() = ChronoUnit.DAYS.between(confirmedOn, asOf).coerceAtLeast(0L)
}

package com.xabier.carcareo.domain

import java.time.LocalDate

/** The odometer fact currently stored on a vehicle. */
data class ConfirmedOdometer(val km: Int, val date: LocalDate)

/** A record's contribution to the odometer: when it happened and at what reading. */
data class RecordReading(val km: Int, val date: LocalDate)

/**
 * Decides whether saving or deleting a maintenance record should move the
 * vehicle's confirmed odometer (spec 3.3 / 4.1). Pure, so the awkward cases below
 * are pinned down by unit tests instead of by hand on a device.
 */
object OdometerConfirmation {

    /**
     * Should [saved] become the vehicle's confirmed odometer?
     *
     * @param previous the record as it was stored before this save, or null when
     *   it is a brand-new record.
     */
    fun shouldConfirmOnSave(
        saved: RecordReading,
        previous: RecordReading?,
        confirmed: ConfirmedOdometer,
    ): Boolean {
        // Editing the very record the confirmation came from: always re-apply, so
        // correcting a typo (50 000 -> 40 000) actually fixes the odometer instead
        // of leaving the vehicle on a value no record backs any more.
        if (previous != null && previous.date == confirmed.date && previous.km == confirmed.km) {
            return true
        }
        return when {
            saved.date.isAfter(confirmed.date) -> true
            // Same day: a higher reading is newer knowledge and wins. A lower one is
            // back-filled paperwork for something earlier that day, and must never
            // rewind "current km".
            saved.date == confirmed.date -> saved.km >= confirmed.km
            else -> false
        }
    }

    /**
     * After [deleted] is removed, the odometer the vehicle should fall back to, or
     * null to leave it alone (spec section 9, case 11).
     *
     * Only rolls back when the confirmation actually came from the deleted record;
     * a manual "update km" entered later must survive. When no records remain there
     * is no better fact available, so the last value stays put — it simply becomes
     * a manual reading again.
     */
    fun rollbackAfterDelete(
        deleted: RecordReading,
        confirmed: ConfirmedOdometer,
        newestRemaining: RecordReading?,
    ): RecordReading? {
        val cameFromDeleted = deleted.date == confirmed.date && deleted.km == confirmed.km
        return if (cameFromDeleted) newestRemaining else null
    }
}

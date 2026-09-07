package com.xabier.carcareo.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Saving and deleting records moves the vehicle's confirmed odometer, and getting
 * that wrong makes "current km" drift away from reality in ways the user cannot
 * see. These pin down the cases that are easy to get backwards.
 */
class OdometerConfirmationTest {

    private val day = LocalDate.of(2026, 6, 1)
    private val confirmed = ConfirmedOdometer(km = 50_000, date = day)

    // --- saving ---

    @Test
    fun `a more recent record confirms the odometer`() {
        val saved = RecordReading(km = 51_000, date = day.plusDays(10))
        assertTrue(OdometerConfirmation.shouldConfirmOnSave(saved, null, confirmed))
    }

    @Test
    fun `back-filled older paperwork does not touch the odometer`() {
        val saved = RecordReading(km = 30_000, date = day.minusMonths(3))
        assertFalse(OdometerConfirmation.shouldConfirmOnSave(saved, null, confirmed))
    }

    @Test
    fun `on the confirmation day a higher reading wins`() {
        val saved = RecordReading(km = 50_400, date = day)
        assertTrue(OdometerConfirmation.shouldConfirmOnSave(saved, null, confirmed))
    }

    @Test
    fun `on the confirmation day a lower reading does not rewind current km`() {
        val saved = RecordReading(km = 49_000, date = day)
        assertFalse(OdometerConfirmation.shouldConfirmOnSave(saved, null, confirmed))
    }

    @Test
    fun `editing the record the confirmation came from corrects it downwards`() {
        // The stored record said 50 000 on `day` — which is exactly what the vehicle
        // is confirmed at — and the user is fixing a typo to 40 000.
        val previous = RecordReading(km = 50_000, date = day)
        val saved = RecordReading(km = 40_000, date = day)
        assertTrue(OdometerConfirmation.shouldConfirmOnSave(saved, previous, confirmed))
    }

    @Test
    fun `editing some other old record still does not rewind`() {
        val previous = RecordReading(km = 20_000, date = day.minusYears(2))
        val saved = RecordReading(km = 21_000, date = day.minusYears(2))
        assertFalse(OdometerConfirmation.shouldConfirmOnSave(saved, previous, confirmed))
    }

    // --- deleting ---

    @Test
    fun `deleting the record that set the odometer rolls back to the previous one`() {
        val deleted = RecordReading(km = 50_000, date = day)
        val previousRecord = RecordReading(km = 44_000, date = day.minusMonths(4))

        val rollback = OdometerConfirmation.rollbackAfterDelete(deleted, confirmed, previousRecord)

        assertEquals(previousRecord, rollback)
    }

    @Test
    fun `deleting an older record leaves the odometer alone`() {
        val deleted = RecordReading(km = 44_000, date = day.minusMonths(4))
        val newest = RecordReading(km = 50_000, date = day)

        assertNull(OdometerConfirmation.rollbackAfterDelete(deleted, confirmed, newest))
    }

    @Test
    fun `deleting the only record leaves the last known value in place`() {
        // Nothing better to fall back to: the reading simply becomes a manual one.
        val deleted = RecordReading(km = 50_000, date = day)
        assertNull(OdometerConfirmation.rollbackAfterDelete(deleted, confirmed, null))
    }
}

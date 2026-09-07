package com.xabier.carcareo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OdometerEstimatorTest {

    private val confirmedOn = LocalDate.of(2026, 1, 1)

    @Test
    fun `zero days elapsed returns the confirmed value`() {
        // Spec test case 8.
        val km = OdometerEstimator.estimatedKm(
            lastConfirmedKm = 20_000,
            lastConfirmedKmDate = confirmedOn,
            annualKmEstimate = 12_000,
            today = confirmedOn,
        )
        assertEquals(20_000, km)
    }

    @Test
    fun `365 days elapsed adds exactly the annual estimate`() {
        // Spec test case 9.
        val km = OdometerEstimator.estimatedKm(
            lastConfirmedKm = 20_000,
            lastConfirmedKmDate = confirmedOn,
            annualKmEstimate = 12_000,
            today = confirmedOn.plusDays(365),
        )
        assertEquals(32_000, km)
    }

    @Test
    fun `half a year elapsed adds roughly half the annual estimate`() {
        val km = OdometerEstimator.estimatedKm(
            lastConfirmedKm = 0,
            lastConfirmedKmDate = confirmedOn,
            annualKmEstimate = 12_000,
            today = confirmedOn.plusDays(182),
        )
        // 12000 * 182 / 365 = 5983.56 -> 5984
        assertEquals(5_984, km)
    }

    @Test
    fun `never decreases when the confirmation date is in the future`() {
        val km = OdometerEstimator.estimatedKm(
            lastConfirmedKm = 15_000,
            lastConfirmedKmDate = confirmedOn.plusDays(10),
            annualKmEstimate = 12_000,
            today = confirmedOn,
        )
        assertEquals(15_000, km)
    }

    @Test
    fun `reading is flagged as estimate only after the confirmation day`() {
        val vehicle = vehicle(lastKm = 10_000, lastDate = confirmedOn, annual = 10_000)

        val sameDay = vehicle.odometerReading(confirmedOn)
        assertFalse(sameDay.isEstimate)
        assertEquals(10_000, sameDay.km)

        val later = vehicle.odometerReading(confirmedOn.plusDays(365))
        assertTrue(later.isEstimate)
        assertEquals(20_000, later.km)
        assertEquals(365, later.daysSinceConfirmed)
    }
}

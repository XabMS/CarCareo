package com.xabier.carcareo.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceTaskRulesTest {

    @Test
    fun `km only is valid`() {
        assertTrue(MaintenanceTaskRules.intervalsValid(intervalKm = 8_000, intervalMonths = null))
    }

    @Test
    fun `months only is valid`() {
        assertTrue(MaintenanceTaskRules.intervalsValid(intervalKm = null, intervalMonths = 24))
    }

    @Test
    fun `both set is valid`() {
        assertTrue(MaintenanceTaskRules.intervalsValid(intervalKm = 8_000, intervalMonths = 12))
    }

    @Test
    fun `neither set is invalid`() {
        assertFalse(MaintenanceTaskRules.intervalsValid(intervalKm = null, intervalMonths = null))
    }

    @Test
    fun `zero or negative interval is invalid`() {
        assertFalse(MaintenanceTaskRules.intervalsValid(intervalKm = 0, intervalMonths = null))
        assertFalse(MaintenanceTaskRules.intervalsValid(intervalKm = -1, intervalMonths = null))
        assertFalse(MaintenanceTaskRules.intervalsValid(intervalKm = 8_000, intervalMonths = 0))
    }
}

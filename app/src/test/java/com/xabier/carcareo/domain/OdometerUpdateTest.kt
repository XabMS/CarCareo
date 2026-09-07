package com.xabier.carcareo.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OdometerUpdateTest {

    @Test
    fun `equal or higher reading is OK`() {
        assertEquals(KmChangeSeverity.OK, OdometerUpdate.classify(20_000, 20_000))
        assertEquals(KmChangeSeverity.OK, OdometerUpdate.classify(20_000, 25_000))
    }

    @Test
    fun `small decrease is a minor warning`() {
        // 19_500 is 2.5% below 20_000 -> minor
        assertEquals(KmChangeSeverity.MINOR_DECREASE, OdometerUpdate.classify(20_000, 19_500))
    }

    @Test
    fun `decrease of more than 10 percent needs explicit confirmation`() {
        // 17_000 is 15% below 20_000 -> major
        assertEquals(KmChangeSeverity.MAJOR_DECREASE, OdometerUpdate.classify(20_000, 17_000))
    }

    @Test
    fun `exactly 10 percent below is still minor`() {
        assertEquals(KmChangeSeverity.MINOR_DECREASE, OdometerUpdate.classify(20_000, 18_000))
    }
}

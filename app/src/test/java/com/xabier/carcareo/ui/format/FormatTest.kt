package com.xabier.carcareo.ui.format

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    private val amount = BigDecimal("1234.5")

    @Test
    fun `no currency chosen follows the locale`() {
        assertEquals("$1,234.50", formatCost(amount, Locale.US, currencyCode = null))
    }

    @Test
    fun `a chosen currency overrides the locale's, keeping the locale's number style`() {
        val text = formatCost(amount, Locale.US, currencyCode = "EUR")
        assertTrue(text, text.contains("1,234.50"))
        assertTrue(text, text.contains("€") || text.contains("EUR"))
        assertTrue(text, !text.contains("$"))
    }

    @Test
    fun `fraction digits follow the chosen currency`() {
        val text = formatCost(amount, Locale.US, currencyCode = "JPY")
        assertTrue(text, text.contains("1,235") || text.contains("1,234"))
        assertTrue(text, !text.contains("."))
    }

    @Test
    fun `an unknown code falls back to the locale`() {
        assertEquals("$1,234.50", formatCost(amount, Locale.US, currencyCode = "NOPE"))
    }
}

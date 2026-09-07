package com.xabier.carcareo.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware formatting helpers (spec 2.1: use the system locale, never fixed
 * patterns). The app's locale always follows the device, so [Locale.getDefault]
 * is the right source. Callers append units / build sentences via string
 * resources and plurals, so these return bare values.
 */

/** Grouped integer, e.g. "12.345" (es) / "12,345" (en). */
fun formatNumber(value: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value.toLong())

fun formatNumber(value: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

/** Localized medium date, e.g. "14 mar 2026" / "Mar 14, 2026". */
fun formatDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** Currency in the device's locale (symbol + grouping + fraction digits). */
fun formatCost(amount: BigDecimal, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getCurrencyInstance(locale).format(amount)

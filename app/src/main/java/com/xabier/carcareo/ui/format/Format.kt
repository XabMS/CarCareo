package com.xabier.carcareo.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
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

/** Day + short month, e.g. "14 mar" / "Mar 14" — for the history year tables. */
fun formatDayMonth(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern(dayMonthPattern(locale), locale))

private fun dayMonthPattern(locale: Locale): String =
    if (locale.language == "en") "MMM d" else "d MMM"

/**
 * The currency costs are shown in. Null follows the device locale, which is the
 * default but not always right: an en-US phone in Spain would print "$".
 * Set from [com.xabier.carcareo.data.prefs.AppPreferences.currencyCode] at app
 * start and whenever the user changes it in Settings.
 */
object CurrencySetting {
    @Volatile
    var code: String? = null
}

/** Currency amount: symbol, grouping and fraction digits localized for [locale]. */
fun formatCost(
    amount: BigDecimal,
    locale: Locale = Locale.getDefault(),
    currencyCode: String? = CurrencySetting.code,
): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    val currency = currencyCode?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
    if (currency != null) {
        format.currency = currency
        // Follow the currency's own minor units (JPY has none, EUR has two).
        format.minimumFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        format.maximumFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
    }
    return format.format(amount)
}

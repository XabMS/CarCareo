package com.xabier.carcareo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// "Workshop ledger" type. Two families: IBM Plex Sans for prose / titles / list
// rows, IBM Plex Mono for anything numeric plus status and section labels.
//
// The Material 3 scale is mapped to Plex Sans so stock components (menus,
// dialogs, switches) inherit the family. The design's bespoke styles live in
// [LedgerText]; numeric ones enable tabular figures so columns align.

private val tnum = listOf("tnum", "lnum")

private fun mono(
    size: Int,
    weight: FontWeight,
    letterSpacingEm: Float = 0f,
    tabular: Boolean = true,
) = TextStyle(
    fontFamily = PlexMono,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = letterSpacingEm.em,
    fontFeatureSettings = if (tabular) tnum.joinToString(", ") else null,
)

private fun sans(size: Int, weight: FontWeight, letterSpacingEm: Float = 0f) = TextStyle(
    fontFamily = PlexSans,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = letterSpacingEm.em,
)

val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = PlexSans),
        displayMedium = displayMedium.copy(fontFamily = PlexSans),
        displaySmall = displaySmall.copy(fontFamily = PlexSans),
        headlineLarge = headlineLarge.copy(fontFamily = PlexSans),
        headlineMedium = headlineMedium.copy(fontFamily = PlexSans),
        headlineSmall = headlineSmall.copy(fontFamily = PlexSans),
        titleLarge = titleLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = PlexSans),
        bodyMedium = bodyMedium.copy(fontFamily = PlexSans),
        bodySmall = bodySmall.copy(fontFamily = PlexSans),
        labelLarge = labelLarge.copy(fontFamily = PlexSans),
        labelMedium = labelMedium.copy(fontFamily = PlexSans),
        labelSmall = labelSmall.copy(fontFamily = PlexSans),
    )
}

/**
 * The design's named styles (handoff §"Typography"). Callers uppercase the label
 * / section / button styles with a locale-aware `.uppercase()` rather than
 * carrying separate strings.
 */
object LedgerText {
    val screenTitle = sans(20, FontWeight.SemiBold)
    val garageTitle = sans(30, FontWeight.SemiBold, letterSpacingEm = -0.02f)

    val sectionLabel = mono(11, FontWeight.SemiBold, letterSpacingEm = 0.13f, tabular = false)
    val contextLine = mono(11, FontWeight.Normal, letterSpacingEm = 0.06f, tabular = false)

    val rowTitle = sans(15, FontWeight.Normal)
    val rowTitleLg = sans(16, FontWeight.Normal)
    val rowMeta = mono(11, FontWeight.Normal)

    val bigNumber = mono(36, FontWeight.SemiBold, letterSpacingEm = -0.02f)
    val bigNumberUnit = mono(16, FontWeight.Medium)
    val cardNumber = mono(20, FontWeight.SemiBold)
    val cardNumberLg = mono(22, FontWeight.SemiBold)

    val buttonLabel = mono(13, FontWeight.SemiBold, letterSpacingEm = 0.08f, tabular = false)
    val buttonLabelSm = mono(12, FontWeight.SemiBold, letterSpacingEm = 0.08f, tabular = false)
    val navLabel = mono(10, FontWeight.Normal, letterSpacingEm = 0.06f, tabular = false)
    val tag = mono(9, FontWeight.SemiBold, letterSpacingEm = 0.1f, tabular = false)

    val supporting = sans(12, FontWeight.Normal)
    val mono10 = mono(10, FontWeight.Normal, letterSpacingEm = 0.1f)
    val mono10Label = mono(10, FontWeight.SemiBold, letterSpacingEm = 0.12f, tabular = false)
}

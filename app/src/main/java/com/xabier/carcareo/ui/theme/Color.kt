package com.xabier.carcareo.ui.theme

import androidx.compose.ui.graphics.Color

// "Workshop ledger" redesign. The light scheme is a hand-picked warm paper
// palette (design handoff §"Theme changes"); Material You dynamic colour is off
// so the paper ground and the traffic-light statuses stay the identity.
//
// The dark scheme is unchanged — a dark counterpart to the redesign is not
// designed yet, so dark users keep the previous "workshop green" look.

// --- Light: paper ---
val md_light_primary = Color(0xFF8A4A22)          // accent: text-only actions, data icons
val md_light_onPrimary = Color(0xFFF6F1E7)
val md_light_primaryContainer = Color(0xFFF1EADC)
val md_light_onPrimaryContainer = Color(0xFF17140F)
val md_light_secondary = Color(0xFF6B6153)
val md_light_onSecondary = Color(0xFFF6F1E7)
val md_light_background = Color(0xFFF6F1E7)        // page ground ("paper")
val md_light_onBackground = Color(0xFF17140F)
val md_light_surface = Color(0xFFF6F1E7)
val md_light_onSurface = Color(0xFF17140F)         // titles, primary values, ink
val md_light_surfaceVariant = Color(0xFFF1EADC)
val md_light_onSurfaceVariant = Color(0xFF6B6153)  // all meta / mono labels <=12sp
val md_light_surfaceContainerLowest = Color(0xFFFFFCF6) // every framed block, table, bar
val md_light_surfaceContainerHigh = Color(0xFFF1EADC)   // footer bands, selected/inactive rows
val md_light_outline = Color(0xFFDED5C4)           // 1dp card borders, section rules
val md_light_outlineVariant = Color(0xFFEDE4D4)    // hairline dividers inside a table
val md_light_error = Color(0xFFA32A1E)
val md_light_onError = Color(0xFFF6F1E7)

// --- Dark: unchanged "workshop green" ---
val md_dark_primary = Color(0xFF8BD5B0)
val md_dark_onPrimary = Color(0xFF003825)
val md_dark_primaryContainer = Color(0xFF005237)
val md_dark_onPrimaryContainer = Color(0xFFA7F2CB)
val md_dark_secondary = Color(0xFFB3CCC0)
val md_dark_onSecondary = Color(0xFF1E352C)
val md_dark_background = Color(0xFF191C1A)
val md_dark_onBackground = Color(0xFFE1E3DF)
val md_dark_surface = Color(0xFF191C1A)
val md_dark_onSurface = Color(0xFFE1E3DF)
val md_dark_surfaceVariant = Color(0xFF3F4943)
val md_dark_onSurfaceVariant = Color(0xFFBFC9C2)
val md_dark_outline = Color(0xFF899390)
val md_dark_error = Color(0xFFFFB4AB)
val md_dark_onError = Color(0xFF690005)

// Task-state colours, used by the garage stripe, mini bars and status dots.
// Light: the "fill" variants (design distinguishes fill from text for contrast).
val StatusOverdueLight = Color(0xFFA32A1E)
val StatusUpcomingLight = Color(0xFF8A6A12)
val StatusOkLight = Color(0xFF3D6B3A)

val StatusOverdueDark = Color(0xFFFFB4AB)
val StatusUpcomingDark = Color(0xFFFFB871)
val StatusOkDark = Color(0xFF8BD5B0)

// Status *text* colours (>=4.5:1 at <=12sp against paper) — light only.
val StatusOverdueTextLight = Color(0xFFA32A1E)
val StatusUpcomingTextLight = Color(0xFF7A5C0A)
val StatusOkTextLight = Color(0xFF3D6B3A)

/**
 * Roles the Material 3 [androidx.compose.material3.ColorScheme] has no slot for
 * but the "Workshop ledger" design needs. Provided via [LocalCarCareoExtraColors]
 * and read through `MaterialTheme.extraColors`.
 */
data class CarCareoExtraColors(
    val ink: Color,
    val bodyOnCard: Color,
    val soonText: Color,
    val soonFill: Color,
    val ok: Color,
    val overdue: Color,
    val alertGround: Color,
    val infoGround: Color,
    val infoAccent: Color,
    val decorativeOutline: Color,
    val chipOutline: Color,
)

val LightExtraColors = CarCareoExtraColors(
    ink = Color(0xFF17140F),
    bodyOnCard = Color(0xFF3B342A),
    soonText = Color(0xFF7A5C0A),
    soonFill = Color(0xFF8A6A12),
    ok = Color(0xFF3D6B3A),
    overdue = Color(0xFFA32A1E),
    alertGround = Color(0xFFFBEEEA),
    infoGround = Color(0xFFF6F0E0),
    infoAccent = Color(0xFF8A6A12),
    decorativeOutline = Color(0xFFA79C88),
    chipOutline = Color(0xFFC8BEAC),
)

// Dark placeholder — maps onto the existing dark roles so nothing crashes if a
// redesigned composable is shown in dark mode before a dark palette exists.
val DarkExtraColors = CarCareoExtraColors(
    ink = md_dark_onSurface,
    bodyOnCard = md_dark_onSurface,
    soonText = StatusUpcomingDark,
    soonFill = StatusUpcomingDark,
    ok = StatusOkDark,
    overdue = StatusOverdueDark,
    alertGround = md_dark_surfaceVariant,
    infoGround = md_dark_surfaceVariant,
    infoAccent = StatusUpcomingDark,
    decorativeOutline = md_dark_outline,
    chipOutline = md_dark_outline,
)

package com.xabier.carcareo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    surfaceContainerLowest = md_light_surfaceContainerLowest,
    surfaceContainerLow = md_light_surfaceContainerLowest,
    surfaceContainer = md_light_surfaceContainerHigh,
    surfaceContainerHigh = md_light_surfaceContainerHigh,
    surfaceContainerHighest = md_light_surfaceContainerHigh,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    error = md_light_error,
    onError = md_light_onError,
)

private val DarkColors = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline,
    error = md_dark_error,
    onError = md_dark_onError,
)

/** Status colours resolved for the active theme. Exposed via [taskStatusColors]. */
data class TaskStatusColors(
    val overdue: Color,
    val upcoming: Color,
    val ok: Color,
    /** Text-safe variants (>=4.5:1 at <=12sp). Equal to the fills in dark. */
    val overdueText: Color,
    val upcomingText: Color,
    val okText: Color,
)

fun taskStatusColors(darkTheme: Boolean): TaskStatusColors =
    if (darkTheme) {
        TaskStatusColors(
            StatusOverdueDark, StatusUpcomingDark, StatusOkDark,
            StatusOverdueDark, StatusUpcomingDark, StatusOkDark,
        )
    } else {
        TaskStatusColors(
            StatusOverdueLight, StatusUpcomingLight, StatusOkLight,
            StatusOverdueTextLight, StatusUpcomingTextLight, StatusOkTextLight,
        )
    }

val LocalCarCareoExtraColors = staticCompositionLocalOf { LightExtraColors }

/** `MaterialTheme.extraColors` — the design roles Material 3 has no slot for. */
val MaterialTheme.extraColors: CarCareoExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCarCareoExtraColors.current

@Composable
fun CarCareoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // "Workshop ledger" turns Material You off: the paper ground and the
    // traffic-light statuses are the identity and dynamic colour overrides both.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCarCareoExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

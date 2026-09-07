package com.xabier.carcareo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale

/**
 * Whether the UI is currently in Spanish. Used only where a non-resource string
 * has to be picked in code — namely instantiating a bilingual plan template
 * (spec 2.1). Everything else goes through `stringResource`.
 */
@Composable
@ReadOnlyComposable
fun isSpanishUi(): Boolean = Locale.current.language == "es"

/**
 * Uppercase a display string using the *composition* locale, so it recomposes on
 * a locale change and satisfies the `NonObservableLocale` lint. The "Workshop
 * ledger" design uppercases most mono labels, section headers and button text.
 */
@Composable
@ReadOnlyComposable
fun String.upcase(): String =
    uppercase(java.util.Locale.forLanguageTag(Locale.current.toLanguageTag()))

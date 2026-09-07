package com.xabier.carcareo.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.xabier.carcareo.R

// "Workshop ledger" redesign — two bundled families (SIL OFL 1.1):
//   IBM Plex Sans  — prose, titles, list rows
//   IBM Plex Mono  — every number, unit, date, plate, status label, section label
// Weights 400 / 500 / 600 only; the design never uses more.

val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

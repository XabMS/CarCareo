// Top-level build file. Plugins are declared here with `apply false` so the
// versions resolve once for the whole build; each module opts in in its own file.
// AGP 9 has built-in Kotlin, so there is no kotlin.android plugin. We add the
// Compose compiler plugin and KSP (for Room).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
}

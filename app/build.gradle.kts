import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

// Release signing secrets live in local.properties (gitignored), never in the repo.
val signingProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseStoreFile: String? = signingProps.getProperty("CARCAREO_STORE_FILE")
val hasReleaseSigning = releaseStoreFile != null && file(releaseStoreFile).exists()

android {
    namespace = "com.xabier.carcareo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xabier.carcareo"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingProps.getProperty("CARCAREO_STORE_PASSWORD")
                keyAlias = signingProps.getProperty("CARCAREO_KEY_ALIAS")
                keyPassword = signingProps.getProperty("CARCAREO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Falls back to unsigned if local.properties has no keystore; the
            // release APK then won't install (see CARCAREO_* keys in local.properties).
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // minSdk 26 already ships java.time, so no core-library desugaring is required.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // The spec requires zero hardcoded UI text: make it fail the build, not just warn.
        error += "HardcodedText"
        abortOnError = true
        checkReleaseBuilds = true
        // "A newer version exists" is not a code-quality gate; versions are pinned
        // deliberately for the duration of this build.
        disable += listOf(
            "NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion",
            // Adaptive icons live in the conventional mipmap-anydpi-v26 folder.
            "ObsoleteSdkInt",
        )
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// Kotlin's jvmTarget defaults to android.compileOptions.targetCompatibility (17).

// Room writes its generated schema JSON here; needed later for migration tests.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Just the ~40-icon core set. The ~5 000-icon material-icons-extended is not
    // used — the handful of non-core glyphs we need are vendored in
    // ui/icon/MaterialIconsSubset.kt.
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.reorderable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

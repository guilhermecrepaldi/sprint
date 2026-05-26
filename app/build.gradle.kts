plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.strava_matematica"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.strava_matematica"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://SEU_SERVIDOR.com/\"")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── On-device OCR ────────────────────────────────────────────────────────
    // ML Kit Digital Ink Recognition — free, offline after first model download (~30 MB).
    // Handles high school math answers: "x = 5", "42", "1/2", "-3", etc.
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")

    // MyScript iink SDK (~50 MB AAR) — uncomment after setup in settings.gradle.kts.
    // See app/src/main/java/.../recognizer/IinkRecognizer.kt for the full setup guide.
    // Handles ALL math notation including fractions, integrals, trig symbols.
    // implementation("com.myscript.iink:engine:2.1.+")
    // implementation("com.myscript.iink:engine-uireferenceimplementation:2.1.+")
}

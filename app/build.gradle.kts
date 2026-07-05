import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Leia a URL do servidor de local.properties (não commitar credenciais).
// Tablet real: API_BASE_URL_DEBUG=http://192.168.x.x:8000/
// Release: API_BASE_URL_RELEASE=https://api.seudominio.com/
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
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
        val debugUrl = localProps.getProperty("API_BASE_URL_DEBUG", "http://10.0.2.2:8000/")
        buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            val releaseUrl = localProps.getProperty("API_BASE_URL_RELEASE", "https://api.sprint.app/")
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
        }
    }
}

dependencies {
    val roomVersion = "2.8.4"

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
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // MyScript iink SDK (~50 MB AAR) — uncomment after setup in settings.gradle.kts.
    // See app/src/main/java/.../recognizer/IinkRecognizer.kt for the full setup guide.
    // Handles ALL math notation including fractions, integrals, trig symbols.
    // implementation("com.myscript.iink:engine:2.1.+")
    // implementation("com.myscript.iink:engine-uireferenceimplementation:2.1.+")
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MyScript iink SDK — uncomment after registering at developer.myscript.com
        // and adding myscript-certificate.json to app/src/main/assets/.
        // maven { url = uri("https://developer.myscript.com/sdk/android/maven") }
    }
}

rootProject.name = "LoveClass"
include(":app")

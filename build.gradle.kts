// The Android Gradle Plugin goes on the classpath only when Android modules are built,
// so the shared core and the macOS app also build on machines without the Android SDK
// (pass -PdesktopOnly=true).
buildscript {
    val desktopOnly = providers.gradleProperty("desktopOnly").orNull == "true"
    dependencies {
        if (!desktopOnly) classpath(libs.android.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

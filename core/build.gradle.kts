import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

val withAndroid = providers.gradleProperty("desktopOnly").orNull != "true"
if (withAndroid) {
    // Configured by name because the Android plugin is only on the classpath for Android builds.
    apply(plugin = "com.android.kotlin.multiplatform.library")
    val kotlinExtensions = (kotlin as ExtensionAware).extensions
    (kotlinExtensions.findByName("android") ?: kotlinExtensions.getByName("androidLibrary")).withGroovyBuilder {
        setProperty("namespace", "app.anitv.core")
        setProperty("compileSdk", libs.versions.android.compileSdk.get().toInt())
        setProperty("minSdk", libs.versions.android.minSdk.get().toInt())
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        if (withAndroid) getByName("androidMain").dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

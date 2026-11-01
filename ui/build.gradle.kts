import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

val withAndroid = providers.gradleProperty("desktopOnly").orNull != "true"
if (withAndroid) {
    // Configured by name because the Android plugin is only on the classpath for Android builds.
    apply(plugin = "com.android.kotlin.multiplatform.library")
    val kotlinExtensions = (kotlin as ExtensionAware).extensions
    (kotlinExtensions.findByName("android") ?: kotlinExtensions.getByName("androidLibrary")).withGroovyBuilder {
        setProperty("namespace", "app.anitv.ui")
        setProperty("compileSdk", libs.versions.android.compileSdk.get().toInt())
        setProperty("minSdk", libs.versions.android.minSdk.get().toInt())
    }
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            api(libs.compose.icons.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.jna)
            implementation(libs.kotlinx.coroutines.swing)
        }
        if (withAndroid) getByName("androidMain").dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

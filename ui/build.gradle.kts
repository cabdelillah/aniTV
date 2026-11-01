import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

val withAndroid = providers.gradleProperty("desktopOnly").orNull != "true"
if (withAndroid) apply(plugin = "com.android.library")

kotlin {
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    if (withAndroid) androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

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

if (withAndroid) {
    extensions.getByName("android").withGroovyBuilder {
        setProperty("namespace", "app.anitv.ui")
        setProperty("compileSdk", libs.versions.android.compileSdk.get().toInt())
        getProperty("defaultConfig").withGroovyBuilder {
            setProperty("minSdk", libs.versions.android.minSdk.get().toInt())
        }
        getProperty("compileOptions").withGroovyBuilder {
            setProperty("sourceCompatibility", JavaVersion.VERSION_17)
            setProperty("targetCompatibility", JavaVersion.VERSION_17)
        }
    }
}

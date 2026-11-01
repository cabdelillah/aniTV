import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(projects.ui)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "app.anitv.desktop.MainKt"
        jvmArgs += listOf("-Dapple.awt.application.appearance=system", "-Dapple.awt.application.name=aniTV")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "aniTV"
            packageVersion = "1.0.0"
            description = "Anime streaming for Android, Android TV and macOS"
            modules("java.net.http", "jdk.unsupported")

            macOS {
                bundleID = "app.anitv.desktop"
                appCategory = "public.app-category.entertainment"
                dockName = "aniTV"
            }
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}

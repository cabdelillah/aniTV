import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

val withAndroid = providers.gradleProperty("desktopOnly").orNull != "true"
if (withAndroid) apply(plugin = "com.android.library")

kotlin {
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    if (withAndroid) androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

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

if (withAndroid) {
    extensions.getByName("android").withGroovyBuilder {
        setProperty("namespace", "app.anitv.core")
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

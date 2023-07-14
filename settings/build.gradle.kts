plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        dependencies{
            implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "settings"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(project(":api"))
                implementation ("io.github.shawxingkwok:kt-util:1.0.0-SNAPSHOT")
                implementation ("io.github.shawxingkwok:android-util-core:1.0.0-SNAPSHOT")
                implementation(libs.kotlin.serialization)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "pers.shawxingkwok.settings"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
}
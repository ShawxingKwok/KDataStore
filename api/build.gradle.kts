plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    explicitApi()
    targetHierarchy.default()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        dependencies{
            implementation ("androidx.datastore:datastore-preferences:1.0.0")
            implementation ("io.github.shawxingkwok:android-util-core:1.0.0-SNAPSHOT")
            implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "api"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
//                implementation(libs.androidx.datastore.preferences.core)
//                api(libs.androidx.datastore.core.okio)
                implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                // TODO remove suffix
                implementation ("io.github.shawxingkwok:kt-util:1.0.0-SNAPSHOT")
                //noinspection GradleDependency
//                implementation ("androidx.datastore:datastore-preferences-core:1.0.0")
                implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
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
    namespace = "pers.apollokwok.kdatastore"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
}
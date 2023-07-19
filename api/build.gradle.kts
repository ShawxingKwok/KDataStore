@file:Suppress("UnstableApiUsage")

import org.gradle.internal.impldep.org.junit.runner.RunWith

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publish)
    id ("kotlinx-serialization")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()
    explicitApiWarning()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    android{
        publishLibraryVariants("release")
    }

    /*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "api"
        }
    }
    */

    sourceSets {
        val commonMain by getting {
            dependencies {
//                implementation(libs.androidx.datastore.preferences.core)
//                api(libs.androidx.datastore.core.okio)
                implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                implementation ("io.github.shawxingkwok:kt-util:1.0.0-SNAPSHOT")
                implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                // TODO remove suffix
                //noinspection GradleDependency
//                implementation ("androidx.datastore:datastore-preferences-core:1.0.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val androidMain by getting {
            dependencies{
                implementation ("androidx.datastore:datastore-preferences:1.0.0")
                implementation ("io.github.shawxingkwok:android-util-core:1.0.0-SNAPSHOT")
                implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
            }
        }
    }
}

android {
    namespace = "pers.shawxingkwok.kdatastore"
    compileSdk = libs.versions.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// publish
mavenPublishing {
    // TODO remove suffix
    coordinates("io.github.shawxingkwok", "kdatastore", "1.0.0-SNAPSHOT")

    pom {
        name.set("KDataStore")
        description.set("Personal data store extension")
        inceptionYear.set("2023")
        url.set("https://github.com/ShawxingKwok/${name.get()}/")

        scm{
            connection.set("scm:git:git://github.com/ShawxingKwok/${name.get()}.git")
            developerConnection.set("scm:git:ssh://git@github.com/ShawxingKwok/${name.get()}.git")
        }
    }
}
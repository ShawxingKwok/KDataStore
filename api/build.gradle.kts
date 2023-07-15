@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.nativecoroutines)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publish)
}

kotlin {
    explicitApiWarning()

    android()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
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
                implementation(libs.kotlin.test)
            }
        }

        val androidMain by getting{
            dependencies{
                implementation ("androidx.datastore:datastore-preferences:1.0.0")
                implementation ("io.github.shawxingkwok:android-util-core:1.0.0-SNAPSHOT")
                implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
            }
        }

        val androidUnitTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "pers.shawxingkwok.kdatastore"
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = 21
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
        name.set("Personal andorid data store extension")
        description.set("")
        inceptionYear.set("2023")
    }
}
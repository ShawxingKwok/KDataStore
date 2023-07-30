@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.publish)
    alias(libs.plugins.kotlin.serialization)
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
//                implementation(libs.androidx.datastore.core.okio)
                implementation(libs.kt.coroutines.core)
                implementation(libs.shawxingkwok.kt.util)
                implementation(libs.kt.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kt.test)
            }
        }

        val androidMain by getting {
            dependencies{
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.shawxingkwok.android.util.core)
                implementation(libs.androidx.lifecycle.livedata.ktx)
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
    val isSNAPSHOT = true
    val version = "1.0.0"
    val artifactId = "kdatastore"

    coordinates("io.github.shawxingkwok", artifactId, if (isSNAPSHOT) "$version-SNAPSHOT" else version)

    pom {
        val repo = "KDataStore"
        name.set(repo)
        description.set("Personal data store extension")
        inceptionYear.set("2023")

        url.set("https://github.com/ShawxingKwok/$repo/")
        scm{
            connection.set("scm:git:git://github.com/ShawxingKwok/$repo.git")
            developerConnection.set("scm:git:ssh://git@github.com/ShawxingKwok/$repo.git")
        }
    }
}
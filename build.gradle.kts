plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.nativecoroutines) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.publish) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
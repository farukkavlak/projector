plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "local.projector.panel"
    compileSdk = 34

    defaultConfig {
        applicationId = "local.projector.panel"
        minSdk = 30
        targetSdk = 30
        versionCode = 10
        versionName = "0.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

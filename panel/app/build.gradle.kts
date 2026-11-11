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
        versionCode = 16
        versionName = "0.16"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "app.kolab.manager"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.kolab.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-diagnostics-alpha"
    }
    buildFeatures { aidl = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

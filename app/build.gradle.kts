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
        versionCode = 4
        versionName = "0.4.0-root-test"
    }
    buildFeatures { aidl = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

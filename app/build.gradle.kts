plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.widgethaus.openaodnotify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.widgethaus.openaodnotify"
        minSdk = 34
        targetSdk = 35
        versionCode = 2
        versionName = "1.3-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
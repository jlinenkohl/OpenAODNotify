plugins {
    alias(libs.plugins.android.application)
}

// Short git commit hash, used to make debug builds distinguishable from one another
// (release versionName intentionally omits this - releases are tagged in git instead).
fun gitShortHash(): String {
    return try {
        fun run(vararg cmd: String): String {
            val process = ProcessBuilder(*cmd)
                .directory(project.rootDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            return output
        }

        val hash = run("git", "rev-parse", "--short=7", "HEAD").ifEmpty { "nogit" }

        // Flag builds made from an uncommitted working tree, since those won't
        // correspond to any commit a teammate (or future you) could check out.
        val isDirty = run("git", "status", "--porcelain").isNotEmpty()
        if (isDirty) "$hash-dirty" else hash
    } catch (e: Exception) {
        "nogit"
    }
}

android {
    signingConfigs {
        create("release") {
            storeFile = file("/home/jlinenkohl/Documents/KeePassX/com.widgethaus-github.jks")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
            keyAlias = "com.widgethaus-github"
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
        }
    }

    namespace = "com.widgethaus.openaodnotify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.widgethaus.openaodnotify"
        minSdk = 34
        targetSdk = 35
        versionCode = 4
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Expose version name as a string resource
        resValue("string", "app_version", "v$versionName")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        debug {
            // e.g. "1.4.1-debug+a1b2c3d" so every debug build is distinguishable at a glance.
            versionNameSuffix = "-debug+${gitShortHash()}"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Use 'findByName' and a null-check to prevent AGP 9 from failing the build
            // when environment variables are missing (e.g., when clicking IDE Run buttons).
            signingConfigs.findByName("release")?.let { releaseConfig ->
                if (releaseConfig.storePassword != null) {
                    signingConfig = releaseConfig
                }
            }
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
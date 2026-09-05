import java.io.FileInputStream
import java.util.Properties

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
        versionCode = 5
        versionName = "1.4.2"

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

// Fails the build loudly if the release APK isn't signed with our actual release
// certificate. Signing is otherwise silent (baked into packageRelease); this makes
// it an explicit, visible, mandatory step - required as part of the release protocol
// in .github/copilot-instructions.md so a misconfigured/missing keystore is caught
// immediately instead of shipping an unsigned or debug-signed APK.
val expectedReleaseCertSha256 = "60E6240414681F8CAF90659E626948E03874F73EBAD4D80956E20867A03C7F6C"

tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Verifies the assembled release APK is signed with the expected release certificate."
    dependsOn("assembleRelease")

    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (!apk.exists()) {
            throw GradleException("Release APK not found at ${apk.path} - assembleRelease may have failed.")
        }

        val localProps = project.rootProject.file("local.properties")
        val sdkDirFromProps = if (localProps.exists()) {
            val p = Properties()
            FileInputStream(localProps).use { stream -> p.load(stream) }
            p.getProperty("sdk.dir")
        } else null

        val sdkDir = sdkDirFromProps ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
            ?: throw GradleException("Could not locate Android SDK (checked local.properties sdk.dir, ANDROID_HOME, ANDROID_SDK_ROOT).")

        val apksigner = file(sdkDir).resolve("build-tools").listFiles()
            ?.filter { it.resolve("apksigner").exists() }
            ?.maxByOrNull { it.name }
            ?.resolve("apksigner")
            ?: throw GradleException("Could not find apksigner under $sdkDir/build-tools/*/")

        val process = ProcessBuilder(apksigner.path, "verify", "--print-certs", apk.path)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw GradleException("apksigner verify FAILED for ${apk.name} - the APK is not validly signed!\n$output")
        }

        val actualSha256 = Regex("SHA-256 digest:\\s*([0-9a-fA-F]+)").find(output)?.groupValues?.get(1)?.uppercase()
            ?: throw GradleException("Could not parse a SHA-256 certificate digest from apksigner output:\n$output")

        if (actualSha256 != expectedReleaseCertSha256) {
            throw GradleException(
                "Release APK is signed, but with an UNEXPECTED certificate!\n" +
                "  Expected SHA-256: $expectedReleaseCertSha256\n" +
                "  Actual SHA-256:   $actualSha256\n" +
                "This likely means the wrong keystore/local.properties was used, or the signing " +
                "certificate was intentionally rotated (if so, update expectedReleaseCertSha256 in app/build.gradle.kts)."
            )
        }

        println("Release APK signing verified: ${apk.name} is signed with the expected release certificate ($actualSha256).")
    }
}
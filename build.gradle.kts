// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
}

// Release signing credentials are read from the untracked, gitignored local.properties
// file (or CI environment variables) - never hardcode secrets here, this file is committed.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}
val RELEASE_STORE_PASSWORD by extra(localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: "")
val RELEASE_KEY_PASSWORD by extra(localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "")


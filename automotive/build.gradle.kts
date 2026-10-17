import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigning = Properties().apply {
    rootProject.file(".private/release-signing.properties").takeIf { it.isFile }
        ?.inputStream()?.use { load(it) }
}
fun releaseSigningValue(environmentName: String, propertyName: String): String =
    providers.environmentVariable(environmentName).orNull
        ?: releaseSigning.getProperty(propertyName, "")

android {
    namespace = "com.shilapi.xcertplay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.shilapi.xcertplay"
        minSdk = 28
        targetSdk = 37
        versionCode = 1201
        versionName = "1.2.1"

    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(
                releaseSigningValue("ANDROID_KEYSTORE_PATH", "storeFile")
                    .ifEmpty { ".private/release-signing/dwcoffee-release.jks" },
            )
            storePassword = releaseSigningValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
            keyAlias = releaseSigningValue("ANDROID_KEY_ALIAS", "keyAlias")
            keyPassword = releaseSigningValue("ANDROID_KEY_PASSWORD", "keyPassword")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.app.automotive)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

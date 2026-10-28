import java.util.Properties
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.davoyans.idea3d"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.davoyans.idea3d"
        minSdk = 26
        targetSdk = 36
        versionCode = 71
        versionName = "0.10.30"
    }
    signingConfigs {
        create("release") {
            val storePath = localProps.getProperty("RELEASE_STORE_FILE", "")
            if (storePath.isNotEmpty()) storeFile = file(storePath)
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

android.applicationVariants.all {
    outputs.all {
        (this as BaseVariantOutputImpl).outputFileName =
            "idea-3d-${android.defaultConfig.versionName}.apk"
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.foundation:foundation:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    testImplementation("junit:junit:4.13.2")
}

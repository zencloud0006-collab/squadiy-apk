import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val blobReadWriteToken: String by lazy {
    val envFile = rootProject.file(".env")
    val fromEnvFile = if (envFile.exists()) {
        Properties().apply { envFile.inputStream().use { load(it) } }.getProperty("BLOB_READ_WRITE_TOKEN")
    } else null
    val raw = fromEnvFile ?: System.getenv("BLOB_READ_WRITE_TOKEN") ?: ""
    raw.trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trim()
        .let { if (it == "BLOB_READ_WRITE_TOKEN_PLACEHOLDER") "" else it }
}

val mediaTokenEndpointUrl: String by lazy {
    val envFile = rootProject.file(".env")
    val fromEnvFile = if (envFile.exists()) {
        Properties().apply { envFile.inputStream().use { load(it) } }.getProperty("MEDIA_TOKEN_ENDPOINT_URL")
    } else null
    val fromProp = providers.gradleProperty("MEDIA_TOKEN_ENDPOINT_URL").orNull
    val raw = fromEnvFile ?: fromProp ?: System.getenv("MEDIA_TOKEN_ENDPOINT_URL") ?: ""
    raw.trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trim()
}

android {
    namespace = "com.bsdcampus.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bsdcampus.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BLOB_READ_WRITE_TOKEN", "\"${blobReadWriteToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "MEDIA_TOKEN_ENDPOINT_URL", "\"${mediaTokenEndpointUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    // implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.recaptcha)
    implementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
}

tasks.withType<Test> {
    failFast = true
    timeout.set(Duration.ofMinutes(2))
    testLogging {
        events("passed", "failed", "skipped")
    }
}

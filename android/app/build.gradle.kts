import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun loadProperties(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

val appVersion: String = loadProperties("version.properties").getProperty("versionName")
val (major, minor, patch) = appVersion.split(".").map { it.toInt() }

// Local builds read keystore.properties. CI passes the same values as environment variables.
val keystore = loadProperties("keystore.properties")
fun signingValue(key: String, env: String): String? = keystore.getProperty(key) ?: System.getenv(env)
val storeFilePath = signingValue("storeFile", "PAGECAST_KEYSTORE_FILE")

android {
    namespace = "com.nahueltaibo.pagecast"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nahueltaibo.pagecast"
        minSdk = 26
        targetSdk = 36
        // Play needs a strictly increasing code. This keeps it in step with versionName.
        versionCode = major * 1_000_000 + minor * 1_000 + patch
        versionName = appVersion
    }

    signingConfigs {
        if (storeFilePath != null) {
            create("upload") {
                storeFile = file(storeFilePath)
                storePassword = signingValue("storePassword", "PAGECAST_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "PAGECAST_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "PAGECAST_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-session:1.7.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in JVM unit tests.
    testImplementation("org.json:json:20240303")
    // The same pull parser Android ships, for feed parsing tests.
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}

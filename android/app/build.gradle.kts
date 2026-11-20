import java.util.Properties

plugins {
    id("com.android.application")
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

dependencies {
    testImplementation("junit:junit:4.13.2")
}

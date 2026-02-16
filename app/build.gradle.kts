import org.gradle.kotlin.dsl.annotationProcessor

plugins {
    alias(libs.plugins.android.application)
}

// ---- Versioning from tag (forgiving) -----------------------------------------

fun parseSemverForgiving(tag: String): Triple<Int, Int, Int> {
    val cleaned = tag.removePrefix("v")
    val parts = cleaned.split(".").filter { it.isNotBlank() }
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return Triple(major, minor, patch)
}

val releaseTag = System.getenv("GITHUB_REF_NAME")
    ?: System.getenv("RELEASE_TAG")
    ?: "v0.0.1"   // local fallback

val (maj, min, pat) = parseSemverForgiving(releaseTag)

// Ensure it's always a positive integer
val computedCode = (maj * 10000 + min * 100 + pat).coerceAtLeast(1)
val computedName = "${maj}.${min}.${pat}"

// ---- Signing: CI env overrides, else local gradle.properties ------------------

fun propOrNull(name: String): String? = project.findProperty(name) as String?

android {
    namespace = "eu.embodyagile.bodhisattvafriend"
    compileSdk = 36

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "eu.embodyagile.bodhisattvafriend.simple"
        minSdk = 31
        targetSdk = 36

        versionCode = computedCode
        versionName = computedName

        buildConfigField("boolean", "IMPORTEXPORT", "false")
        buildConfigField("boolean", "CHANGE_PRACTICE", "false")
        buildConfigField("boolean", "PRACTICEMANAGEMENT", "false")

        resValue("bool", "config_feature_importexport", "false")
        resValue("bool", "config_feature_change_practice", "false")
        resValue("bool", "config_feature_practicemanagement", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Prefer CI env vars if present
            val envStoreFile = System.getenv("SIGNING_STORE_FILE")
            if (!envStoreFile.isNullOrBlank()) {
                storeFile = file(envStoreFile)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            } else {
                // Fallback to local properties (recommended: put these in ~/.gradle/gradle.properties)
                val storeFilePath = propOrNull("RELEASE_STORE_FILE")
                val storePass = propOrNull("RELEASE_STORE_PASSWORD")
                val keyAliasVal = propOrNull("RELEASE_KEY_ALIAS")
                val keyPass = propOrNull("RELEASE_KEY_PASSWORD")

                if (!storeFilePath.isNullOrBlank()) {
                    storeFile = file(storeFilePath)
                    storePassword = storePass
                    keyAlias = keyAliasVal
                    keyPassword = keyPass
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "einkMeditation (Debug)")
        }
        release {
            // keep as-is
            signingConfig = signingConfigs.getByName("release")
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
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

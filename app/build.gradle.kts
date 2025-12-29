import org.gradle.kotlin.dsl.annotationProcessor

plugins {
    alias(libs.plugins.android.application)
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "eu.embodyagile.bodhisattvafriend"
    compileSdk = 36
    flavorDimensions += "mode"
    buildFeatures {
        buildConfig = true
    }

    productFlavors {
        create("simple") {
            dimension = "mode"
            applicationIdSuffix = ".simple"
            versionNameSuffix = "-simple"

            buildConfigField("boolean", "IMPORTEXPORT", "false")
            buildConfigField("boolean", "CHANGE_PRACTICE", "false")
            buildConfigField("boolean", "PRACTICEMANAGEMENT", "false")
            // Optional: use in XML via R.bool.*
            resValue("bool", "config_feature_importexport", "false")
            resValue("bool", "config_feature_change_practice", "false")
            resValue("bool", "config_feature_practicemanagement", "false")

            // Optional: different app name on device
            resValue("string", "app_name", "einkMeditation")
        }

        create("labs") {
            dimension = "mode"
            applicationIdSuffix = ".labs"
            versionNameSuffix = "-labs"

            buildConfigField("boolean", "IMPORTEXPORT", "true")
            buildConfigField("boolean", "CHANGE_PRACTICE", "true")
            buildConfigField("boolean", "PRACTICEMANAGEMENT", "true")

            resValue("bool", "config_feature_importexport", "true")
            resValue("bool", "config_feature_change_practice", "true")
            resValue("bool", "config_feature_practicemanagement", "true")

            resValue("string", "app_name", "Bodhisattva Friend (Labs)")
        }
    }

    defaultConfig {
        applicationId = "eu.embodyagile.bodhisattvafriend"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    val roomVersion = "2.6.1"

    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")


    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
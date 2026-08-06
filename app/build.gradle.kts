plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val localProps = Properties().apply {
    val f = File(rootDir, "local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.madeby.JAI"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.madeby.JAI"
        
        // Supports Android 9 (API 28) up to Android 16 (API 36)
        minSdk = 28
        targetSdk = 36
        
        versionCode = 7
        versionName = "1.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = System.getenv("STORE_PASSWORD") ?: localProps.getProperty("STORE_PASSWORD") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: localProps.getProperty("KEY_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: localProps.getProperty("KEY_ALIAS") ?: "key0"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signs release builds with your JAI Labs key
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

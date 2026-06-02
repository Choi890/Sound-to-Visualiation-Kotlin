plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.soundvisualization.accessibility"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.soundvisualization.accessibility"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    androidResources {
        localeFilters += listOf("ko", "en")
        noCompress += listOf("tflite", "wasm", "onnx")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("com.google.mediapipe:tasks-audio:0.10.35")
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    debugImplementation("androidx.compose.ui:ui-tooling")
}

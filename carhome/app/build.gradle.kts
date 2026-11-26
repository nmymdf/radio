plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.archiekuo.carhome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.archiekuo.carhome"
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "0.6"
        // 車機是 ARM；雲端模擬器測試另外打包 x86_64（-Pabis=x86_64）
        ndk { abiFilters += (project.findProperty("abis") as String? ?: "arm64-v8a,armeabi-v7a").split(",") }
    }
    signingConfigs {
        create("release") {
            storeFile = file("carhome.jks")
            storePassword = "archiekuo"
            keyAlias = "carhome"
            keyPassword = "archiekuo"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("org.mozilla.geckoview:geckoview:131.0.20241011205646")
}

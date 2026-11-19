plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.pla.agent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pla.agent"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Coroutines (Untuk Background Process)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // OkHttp (Untuk Download Model & Browsing Internet)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Room Database (Untuk Menyimpan Memori & Hasil Belajar AI)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coil (Untuk Menampilkan Gambar Avatar Karakter AI)
    implementation("io.coil-kt:coil:2.5.0")

    // Llama.cpp (Mesin AI GGUF via JitPack)
    implementation("com.github.shubham0204:llama-cpp-android:0.1.0")
}
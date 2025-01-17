plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.newsworth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.narmtech.newsworth"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packagingOptions {
        // Resolve conflicts by picking the first occurrence of conflicting libraries
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst ("lib/arm64-v8a/libc++_shared.so")
        pickFirst ("lib/x86/libc++_shared.so")
        pickFirst ("lib/x86_64/libc++_shared.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.espresso.core)
    implementation(libs.play.services.basement)
    implementation(libs.common)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")

    // Material Design 3
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // Kotlin Coroutines (optional)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")


    // FFmpeg
    implementation("com.arthenica:mobile-ffmpeg-full-gpl:4.4")
    implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}

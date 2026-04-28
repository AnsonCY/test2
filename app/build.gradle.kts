plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "hkcc.helper"
    compileSdk = 36

    defaultConfig {
        applicationId = "hkcc.helper"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase
        buildConfigField("String", "SUPABASE_URL", "\"https://hwzztdpusrlntnjhdsop.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable_QocrPjKbu2Sk07WkiGAn4g_kW1wNiIH\"")
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
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.1")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.1.1")

    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-android:3.0.0")
    implementation("io.ktor:ktor-client-logging:3.0.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")

    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
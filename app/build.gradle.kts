import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")

}

android {
    namespace = "com.kt.flash.light.led.banner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kt.flash.light.led.banner"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val formattedDate = SimpleDateFormat("MM.dd.yyyy").format(Date())
        base.archivesName.set("ProjectBase_v${versionName}(${versionCode})_${formattedDate}")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    buildFeatures {
        compose = false
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    flavorDimensions += "default"
    productFlavors {
        create("appDebug") {
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "banner_splash", "\"ca-app-pub-3940256099942544/2014213617\"")
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_splash_2f", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "native_full_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_full_splash_2", "\"ca-app-pub-3940256099942544/2247696110\"")


            buildConfigField("Boolean", "build_debug", "true")

        }

        create("appProduct") {
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "banner_splash", "\"ca-app-pub-3940256099942544/2014213617\"")
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_splash_2f", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "native_full_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_full_splash_2", "\"ca-app-pub-3940256099942544/2247696110\"")


            buildConfigField("Boolean", "build_debug", "false")

        }
    }

}

dependencies {
    implementation(project(":ads"))

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))

    // Activity
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    //Gson
    implementation("com.google.code.gson:gson:2.13.2")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Core KTX
    implementation("androidx.core:core-ktx:1.17.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")

    // Material
    implementation("com.google.android.material:material:1.10.0")

    // Unit Test
    testImplementation("junit:junit:4.13.2")

    //shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    //Gson
    implementation("com.google.code.gson:gson:2.13.2")
    //AdMob
    implementation("com.google.android.gms:play-services-ads:24.9.0")

    // Android Test
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")

    //kamui
    implementation("com.karumi:dexter:6.2.3")


    // Debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    // hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // In-App Update
    implementation("com.google.android.play:app-update-ktx:2.1.0")

}

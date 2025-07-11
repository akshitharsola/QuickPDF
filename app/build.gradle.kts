plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.quickpdf.reader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quickpdf.reader"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Production keystore configuration
            storeFile = file(findProperty("QUICKPDF_KEYSTORE_FILE") ?: "quickpdf-production.keystore")
            storePassword = findProperty("QUICKPDF_KEYSTORE_PASSWORD") as String? ?: "QuickPDF2024@Secure!"
            keyAlias = findProperty("QUICKPDF_KEY_ALIAS") as String? ?: "quickpdf-prod"
            keyPassword = findProperty("QUICKPDF_KEY_PASSWORD") as String? ?: "QuickPDF2024@Secure!"
            
            // Enable v1 and v2 signing for maximum compatibility
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            // Use debug signing for development
            signingConfig = signingConfigs.getByName("debug")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use production signing for release builds
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.kotlinx.coroutines.android)
    
    // PhotoView for professional zoom and pan functionality
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    
    // Room database - temporarily disabled for initial testing
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // annotationProcessor(libs.androidx.room.compiler)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
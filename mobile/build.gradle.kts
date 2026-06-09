import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps =
    Properties().apply {
        if (keystorePropsFile.exists()) {
            keystorePropsFile.inputStream().use { load(it) }
        }
    }
val hasReleaseSigning = keystoreProps.getProperty("storeFile") != null

val callingVersionCode =
    (findProperty("calling.versionCode") as String).toInt()
val callingVersionName = findProperty("calling.versionName") as String

android {
    namespace = "dev.arpan.calling"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.arpan.calling"
        minSdk = 30
        targetSdk = 36
        versionCode = callingVersionCode
        versionName = callingVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("storeSafe") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_STORE_SAFE", "true")
            resValue("string", "app_name", "Calling")
        }
        create("personal") {
            dimension = "distribution"
            applicationIdSuffix = ".personal"
            versionNameSuffix = "-personal"
            buildConfigField("boolean", "IS_STORE_SAFE", "false")
            resValue("string", "app_name", "Calling Personal")
        }
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig =
                if (hasReleaseSigning) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.wearable)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

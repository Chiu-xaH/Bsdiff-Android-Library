plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = libs.versions.libraryPackageName.get() + ".core"
    compileSdk = Integer.parseInt(libs.versions.maxSdkVersion.get())

    defaultConfig {
        minSdk =  Integer.parseInt(libs.versions.minSdkVersion.get())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // 使用28及其以上版本即可，根据自己Android Studio下载的NDK版本设置，为了启用16KB页支持
    ndkVersion = "29.0.13113456"
}

dependencies {}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = libs.versions.libraryPackageName.get()
                version = libs.versions.libraryVersionName.get()
                artifactId = "core"
                from(components["release"])
            }
        }
    }
}
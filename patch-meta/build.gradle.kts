plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.xah.patch.meta"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // JSON解析 用于差分包元数据解析
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core"))
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.xah.bsdiff"
                artifactId = "patch-meta"
                version = "2.0-alpha01"

                from(components["release"])
            }
        }
    }
}
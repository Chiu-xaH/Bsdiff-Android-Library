plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = libs.versions.libraryPackageName.get() + ".patch"
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
    implementation(project(":core"))
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = libs.versions.libraryPackageName.get()
                version = libs.versions.libraryVersionName.get()
                artifactId = "patch"
                from(components["release"])
            }
        }
    }
}
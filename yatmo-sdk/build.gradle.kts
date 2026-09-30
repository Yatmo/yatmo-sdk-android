plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

group = "com.yatmo"
version = "1.0.0"

android {
    namespace = "com.yatmo.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$version\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // MapLibre Android: reads the same style JSON as the Yatmo web plugin.
    api("org.maplibre.gl:android-sdk:11.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")

    // Optional Jetpack Compose wrapper (YatmoMap). Consumers without Compose can ignore it.
    compileOnly("androidx.compose.ui:ui:1.6.8")
    compileOnly("androidx.compose.foundation:foundation:1.6.8")
    compileOnly("androidx.compose.runtime:runtime:1.6.8")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.yatmo"
            artifactId = "yatmo-sdk"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
}

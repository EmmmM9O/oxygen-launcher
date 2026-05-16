plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

dependencies { 
  implementation(project(":core"))
}

kotlin { jvmToolchain(17) }

android {
  namespace = "io.github.emmmm9o.oxygenlauncher"
  buildToolsVersion = "35.0.0"
  compileSdk = 35
  ndkVersion = "29.0.14206865"
  ndkPath = "/home/stellarcus/Android/android-ndk-r29/"

  packaging {
    jniLibs {
      useLegacyPackaging = true
      pickFirsts += listOf("**/libbytehook.so")
    }
  }

  defaultConfig {
    applicationId = "io.github.emmmm9o.oxygenlauncher"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true
    ndk { abiFilters += listOf("arm64-v8a") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }

  buildTypes {
    all {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles("proguard-rules.pro")
    }
  }
}

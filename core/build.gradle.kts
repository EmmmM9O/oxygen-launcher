plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
}

kotlin {
  androidTarget()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(libs.commons.compress)
        implementation(libs.xz)
        implementation(libs.bytehook)
        api(libs.kotlinx.coroutines.core)
      }
    }
    val androidMain by getting {
      dependencies {
        api(libs.androidx.lifecycle.runtime.ktx)
        api(libs.androidx.appcompat)
        api(libs.kotlinx.coroutines.android)
      }
    }
  }
}

android {
  namespace = "io.github.emmmm9o.oxygenlauncher.core"
  buildToolsVersion = "35.0.0"
  compileSdk = 35
  ndkVersion = "29.0.14206865"
  ndkPath = "/home/stellarcus/Android/android-ndk-r29/"

  buildFeatures { prefab = true }

  packaging {
    jniLibs {
      useLegacyPackaging = true
      pickFirsts += listOf("**/libbytehook.so")
    }
  }

  defaultConfig {
    minSdk = 24
    multiDexEnabled = true
    ndk { abiFilters += listOf("arm64-v8a") }
    externalNativeBuild { cmake { arguments += "-DANDROID_STL=c++_shared" } }
  }

  externalNativeBuild {
    cmake {
      path = file("src/androidMain/jni/CMakeLists.txt")
      version = "4.2.1"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildTypes {
    all {
      isMinifyEnabled = false
      isShrinkResources = false
    }
  }
}

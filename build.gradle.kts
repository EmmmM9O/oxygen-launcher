import java.io.FileInputStream
import java.util.Properties

val props =
    Properties().apply {
      val file = File("local.properties")
      if (file.exists()) load(FileInputStream(file))
    }

plugins {
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
  extra.set("local", props)
}

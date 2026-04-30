package oxygen.util

import android.os.Build

object OS {
  val isARM = propNoNull("os.arch").startsWith("arm") || propNoNull("os.arch").startsWith("aarch64")

  val is64Bit = propNoNull("os.arch").contains("64") || propNoNull("os.arch").startsWith("armv8")

  fun getDeviceArchitecture(): Arch {
    if (isARM) return if (is64Bit) Arch.ARM64 else Arch.ARM
    return if (is64Bit) Arch.X86_64 else Arch.X86
  }

  fun archAsString(arch: Arch): String =
      when (arch) {
        Arch.ARM64 -> "arm64"
        Arch.ARM -> "arm"
        Arch.X86_64 -> "x86_64"
        Arch.X86 -> "x86"
        else -> "UNSUPPORTED_ARCH"
      }

  fun prop(name: String): String? = System.getProperty(name)

  fun propNoNull(name: String): String = prop(name) ?: ""

  fun manufacturer(): String =
      Build.MANUFACTURER.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
      }

  fun deviceName(): String = "${manufacturer()} ${Build.PRODUCT} ${Build.MODEL}"

  fun socName(): String =
      runCatching {
            Runtime.getRuntime().exec("getprop ro.soc.model").inputStream.bufferedReader().use {
              it.readLine()
            }
          }
          .getOrNull()
          .takeUnless { it.isNullOrBlank() } ?: Build.HARDWARE

  fun filesDir(): Fi = OSFilesDir()

  fun cacheDir(): Fi = OSCacheDir()

  fun runtimeDir(): Fi = OSRuntimeDir()

  fun nativeLibDir(): Fi = OSNativeLibDir()
}

expect fun OSFilesDir(): Fi

expect fun OSCacheDir(): Fi

expect fun OSRuntimeDir(): Fi

expect fun OSNativeLibDir(): Fi

enum class Arch {
  ARM64,
  ARM,
  X86,
  X86_64,
}

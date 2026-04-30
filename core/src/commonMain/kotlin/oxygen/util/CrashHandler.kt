package oxygen.util

import android.os.Build
import java.io.*
import java.text.*
import java.util.*
import oxygen.launcher.*

object CrashHandler {
  fun formatMemory(bytes: Long): String = "${bytes / (1024 * 1024)} MB"

  fun createReport(exception: Throwable): String {
    val runtime = Runtime.getRuntime()
    return """
Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss a", Locale.getDefault()).format(Date())}
Android:  ${Build.VERSION.RELEASE}
API level: ${Build.VERSION.SDK_INT}
Device: ${OS.deviceName()} 
CPU: ${OS.socName()} 
Memory: ${formatMemory(runtime.totalMemory() - runtime.freeMemory())} / ${formatMemory(runtime.totalMemory())} / ${formatMemory(runtime.maxMemory())}
Cores: ${runtime.availableProcessors()}
OS : ${System.getProperty("os.name")} v${System.getProperty("os.version")}
JVM : ${System.getProperty("java.vm.name")} v${System.getProperty("java.vm.version")} by${System.getProperty("java.vm.vendor")}
Oxygen Launcher has crashed.


${exception.trace()}
    """
        .trimIndent()
  }

  fun log(exception: Throwable) {
    val file = OLPath.crashDir.child("crash_${System.currentTimeMillis()}.txt")
    file.parent().mkdirs()

    try {
      file.writer(false).apply {
        write(createReport(exception))
        flush()
        close()
      }
    } catch (e: Exception) {
      Log.err(e)
    }
  }
}

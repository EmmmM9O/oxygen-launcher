package oxygen.launcher

import android.app.*
import android.content.*
import android.content.res.*
import android.net.*
import android.os.*
import android.telephony.*
import android.view.*
import android.widget.*
import io.github.emmmm9o.oxygenlauncher.*
import java.io.*
import java.lang.Thread.*
import java.util.*
import kotlinx.coroutines.*
import oxygen.*
import oxygen.bridge.*
import oxygen.input.*
import oxygen.surfaceview.*
import oxygen.util.*

class MainActivity : AndroidActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    init()
    Log.info("[OxygenL][ANDROID] Android:  ${Build.VERSION.RELEASE}")
    Log.info("[OxygenL][ANDROID] API level: ${Build.VERSION.SDK_INT}")
    Log.info("[OxygenL][ANDROID] Device:  ${OS.deviceName()}")
    Log.info("[OxygenL][ANDROID] CPU:  ${OS.socName()}")
    Build.SUPPORTED_ABIS.forEachIndexed { index, abi ->
      Log.info("[OxygenL][ABI${index + 1}] : $abi")
    }
    Log.info(
        "[OxygenL][OS] : ${System.getProperty("os.name")} v${System.getProperty("os.version")}"
    )
    Log.info(
        "[OxygenL][JVM] : ${System.getProperty("java.vm.name")} v${System.getProperty("java.vm.version")} by${System.getProperty("java.vm.vendor")}"
    )
    val runtime = Runtime.getRuntime()
    Log.info(
        "[OxygenL][RAM] : ${formatMemory(runtime.totalMemory() - runtime.freeMemory())} / ${formatMemory(runtime.totalMemory())} / ${formatMemory(runtime.maxMemory())}"
    )
    Log.info("[OxygenL][CPU] Cores : ${runtime.availableProcessors()}")
    JreManager.init()
    JreManager.install()
    Log.info("[OxygenL][IN JRE]:RELEASE\n${JreManager.getRelease(OLPath.javaPath)}")

    if (!OLPath.jarFile.exists()) {
      Log.info("${OLPath.jarFile.absolutePath()} Not exists")
      AlertDialog.Builder(this)
          .setTitle(R.string.jar_unfound)
          .setMessage(getString(R.string.jar_unfound_message, OLPath.jarFile.absolutePath()))
          .setPositiveButton(R.string.button_open) { dialog, _ ->
            openFolder(OLPath.jarFile.absolutePath())
          }
          .setPositiveButton(getString(R.string.button_confirm)) { dialog, _ -> dialog.dismiss() }
          .show()
      return
    }
    runJVM()
  }
}

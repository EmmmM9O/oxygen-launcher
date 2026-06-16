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
import androidx.activity.result.contract.*

class MainActivity : AndroidActivity() {
  private var dialog: AlertDialog? = null
  private val pickFileLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      contentResolver.openInputStream(uri)!!.use { input ->
        OLPath.gameJar.write().use { output -> input.copyTo(output) }
      }
    }
  }
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
    if(JreManager.java == false){
      dialog = AlertDialog.Builder(this)
                  .setTitle(R.string.java)
                              .setMessage(R.string.java_install)
                                          .setCancelable(false) 
                                          .show()
    }
    JreManager.install()
    dialog?.dismiss()
    Log.info("[OxygenL][IN JRE]:RELEASE\n${JreManager.getRelease(OLPath.javaPath)}")

    if (!OLPath.gameJar.exists() && !Core.settings.launcher.skipCheckJar) {
      Log.info("${OLPath.gameJar.absolutePath()} Not exists")
      AlertDialog.Builder(this)
          .setTitle(R.string.jar_unfound)
          .setMessage(getString(R.string.jar_unfound_message, OLPath.gameJar.absolutePath()))
          .setPositiveButton(R.string.button_ignore) { dialog, _ ->
            Core.settings.launcher = Core.settings.launcher.copy(skipCheckJar = true)
	    dialog.dismiss()
          }
          .setPositiveButton(R.string.button_import) { dialog, _ ->
            pickFileLauncher.launch(arrayOf("application/java-archive"))
	    dialog.dismiss()
          }
          .setPositiveButton(R.string.button_open) { dialog, _ ->
            openFolder(OLPath.gameJar.parent().absolutePath())
	    dialog.dismiss()
          }
          .setPositiveButton(getString(R.string.button_confirm)) { dialog, _ -> dialog.dismiss() }
          .show()
      return
    }
    runJVM()
  }
  override fun onDestroy() {
    super.onDestroy()
    dialog?.dismiss()
    dialog = null
  }
  override fun handleCrash(code: Int) {
    val dia = AlertDialog.Builder(this)
          .setTitle(R.string.crash_title)
          .setMessage(getString(R.string.crash_message, code))
          .setPositiveButton(R.string.button_replace) { dialog, _ ->
            pickFileLauncher.launch(arrayOf("application/java-archive"))
          }
          .setPositiveButton(R.string.button_log) { dialog, _ ->
            openFolder(OLPath.logFile.absolutePath())
          }
          .setPositiveButton(getString(R.string.button_confirm)) { dialog, _ -> dialog.dismiss() }
    .show()
    dia.setOnDismissListener {
      killProcess()
    }
  }
}

package oxygen.launcher

import android.annotation.TargetApi
import android.app.*
import android.content.*
import android.content.res.*
import android.net.*
import android.os.*
import android.system.Os
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.io.*
import java.lang.Thread.*
import java.util.*
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import oxygen.*
import oxygen.bridge.*
import oxygen.input.*
import oxygen.surfaceview.*
import oxygen.util.*

open class AndroidActivity : AppCompatActivity(), Platform {
  lateinit var clipboard: ClipboardManager
  var hideStatusBar = false
  var useImmersiveMode = false
  val dispather = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  lateinit var handler: Handler
  lateinit var view: RenderSurfaceView
  lateinit var input: AndroidInput
  @Volatile var surfaceCreated = false

  fun init() {
    val errHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      Log.err(error)
      CrashHandler.log(error)

      if (errHandler != null) {
        errHandler.uncaughtException(thread, error)
      } else {
        System.exit(1)
      }
    }

    this.handler = Handler()
    Core.platform = this
    AndroidCore.context = this
    this.clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Core.files = AndroidFiles(assets, getExternalFilesDir(null)!!.getAbsolutePath())
    OLPath.init()
    loadFileLogger(OLPath.logFile)
    hideStatusBar(this.hideStatusBar)
    useImmersiveMode(this.useImmersiveMode)
    if (this.useImmersiveMode && getVersion() >= Build.VERSION_CODES.KITKAT) {
      try {
        val rootView = this.window.decorView
        rootView.setOnSystemUiVisibilityChangeListener {
          this.handler.post { useImmersiveMode(true) }
        }
      } catch (e: Throwable) {
        Log.err("[OxygenL] Failed to create AndroidVisibilityListener \n ${e.trace()}")
      }
    }
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

    lifecycleScope.launch(dispather) {
      Core.bridge = OxygenBridge()
      Core.bridge.execute()
      Core.launcher = JvmLauncher()
      Core.launcher.launch()
      finishAffinity()
    }
    while (!surfaceCreated) {}
    Log.info("[OxygenL] Create Surface")
    view = RenderSurfaceView(this, FillResolutionStrategy(), JvmRenderer(Core.bridge))
    input = AndroidInput(this, view)
    Core.input = input
    try {
      requestWindowFeature(Window.FEATURE_NO_TITLE)
    } catch (ex: Exception) {
      Log.err("[OxygenL] Content already displayed, cannot request FEATURE_NO_TITLE ${ex.trace()}")
    }

    window.apply {
      setFlags(
          WindowManager.LayoutParams.FLAG_FULLSCREEN,
          WindowManager.LayoutParams.FLAG_FULLSCREEN,
      )
      clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
    }

    setContentView(view, createLayoutParams())
  }

  protected fun createLayoutParams() =
      FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
          )
          .apply { gravity = Gravity.CENTER }

  fun formatMemory(bytes: Long): String = "${bytes / (1024 * 1024)} MB"

  fun loadFileLogger(file: Fi) {
    file.parent().mkdirs()

    try {
      val writer = file.writer(false)
      Core.logWriter = writer
      val log = Log.logger
      Log.logger = LogHandler { level, text ->
        log.log(level, text)

        try {
          writer.write("[${Character.toUpperCase(level.name[0])}] $text\n")
          writer.flush()
        } catch (e: IOException) {
          e.printStackTrace()
          // ignore it
        }
      }
    } catch (e: Exception) {
      // handle log file not being found
      Log.err(e)
    }
  }

  override fun getVersion(): Int {
    return android.os.Build.VERSION.SDK_INT
  }

  override fun getNativeHeap(): Long {
    return Debug.getNativeHeapAllocatedSize()
  }

  override fun getClipboardText(): String? {
    val clip = clipboard.primaryClip ?: return null
    val text = clip.getItemAt(0).text ?: return null
    return text.toString()
  }

  override fun setClipboardText(contents: String): Unit {
    val data = ClipData.newPlainText(contents, contents)
    clipboard.setPrimaryClip(data)
  }

  override fun openFolder(file: String): Boolean {
    Log.info(file)
    val selectedUri = Uri.parse(file)
    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(selectedUri, "resource/folder") }

    return if (intent.resolveActivityInfo(packageManager, 0) != null) {
      startActivity(intent)
      true
    } else {
      runOnUiThread {
        Toast.makeText(
                this,
                "Unable to open folder (missing valid file manager?)\n$file",
                Toast.LENGTH_LONG,
            )
            .show()
      }
      false
    }
  }

  override fun openURI(URI: String): Boolean {
    return try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URI)))
      true
    } catch (e: ActivityNotFoundException) {
      false
    }
  }

  override fun killProcess() {
    android.os.Process.killProcess(android.os.Process.myPid())
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (Core.jvmInit) Core.bridge.onWindowFocusChanged(hasFocus)
    useImmersiveMode(this.useImmersiveMode)
    hideStatusBar(this.hideStatusBar)
  }

  @TargetApi(19)
  fun useImmersiveMode(use: Boolean) {
    if (!use || getVersion() < Build.VERSION_CODES.KITKAT) return

    window.decorView.systemUiVisibility =
        (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
  }

  protected fun hideStatusBar(hide: Boolean) {
    if (!hide) return

    window.decorView.systemUiVisibility = 0x1
  }

  override fun onPause() {
    if (Core.jvmInit) Core.bridge.onPause()
    super.onPause()
  }

  override fun onResume() {
    if (Core.jvmInit) Core.bridge.onResume()
    super.onResume()
  }

  override fun onDestroy() {
    Log.info("[Oxygen Launcher] Destory")
    if (Core.jvmInit) Core.bridge.onDestroy()
    dispather.close()
    super.onDestroy()
    System.exit(0)
  }

  override fun onConfigurationChanged(config: Configuration) {
    super.onConfigurationChanged(config)
    // TODO
    if (Core.jvmInit) Core.bridge.onConfigurationChanged("")
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    // TODO
    if (Core.jvmInit) Core.bridge.onActivityResult(requestCode, resultCode, "")
  }

  override fun symlink(target: String, link: String) {
    Os.symlink(target, link)
  }

  override fun openAssets(path: String): InputStream = getAssets().open(path)

  override fun createSurface(): Unit {
    surfaceCreated = true
  }

  override fun finishing(): Boolean = isFinishing()
}

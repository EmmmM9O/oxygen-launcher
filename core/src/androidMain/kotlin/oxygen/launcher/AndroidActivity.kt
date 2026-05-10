package oxygen.launcher

import android.*
import android.annotation.TargetApi
import android.app.*
import android.content.*
import android.content.pm.*
import android.content.res.*
import android.net.*
import android.os.*
import android.os.Build.*
import android.provider.OpenableColumns
import android.system.Os
import android.telephony.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.*
import androidx.core.content.*
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
  val dispather = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  lateinit var handler: Handler
  lateinit var view: RenderSurfaceView
  lateinit var input: AndroidInput
  @Volatile var surfaceCreated = false
  private val eventListeners = mutableMapOf<Int, AndroidEventListener>()
  private val cacheFiles = mutableMapOf<String, Uri>()
  private var lastEventNumber = 43
  var loopJob: Job? = null

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
    Core.settings = Settings()
    val lc = Core.settings.launcher
    createWakeLock(lc.useWakelock)
    hideStatusBar(lc.hideStatusBar)
    useImmersiveMode(lc.useImmersiveMode)
    if (lc.useImmersiveMode && getVersion() >= Build.VERSION_CODES.KITKAT) {
      try {
        val rootView = this.window.decorView
        rootView.setOnSystemUiVisibilityChangeListener {
          this.handler.post { useImmersiveMode(true) }
        }
      } catch (e: Throwable) {
        Log.err("[OxygenL] Failed to create AndroidVisibilityListener \n ${e.trace()}")
      }
    }
  }

  fun runJVM() {
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
    Log.info("Open folder $file")
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
    useImmersiveMode(Core.settings.launcher.useImmersiveMode)
    hideStatusBar(Core.settings.launcher.hideStatusBar)
  }

  protected fun createWakeLock(use: Boolean) {
    if (!use) return
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    loopJob?.cancel()
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

  override fun symlink(target: String, link: String) {
    Os.symlink(target, link)
  }

  override fun openAssets(path: String): InputStream = getAssets().open(path)

  override fun createSurface(): Unit {
    surfaceCreated = true
  }

  override fun finishing(): Boolean = isFinishing()

  override fun hide(): Unit {
    moveTaskToBack(true)
  }

  override fun beginForceLandscape(): Unit {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
  }

  override fun endForceLandscape(): Unit {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER)
  }

  override fun startLoop(): Unit {
    loopJob?.cancel()
    loopJob =
        lifecycleScope.launch(Dispatchers.Main) {
          Core.bridge.loop()
          kotlinx.coroutines.yield()
        }
  }

  override fun endLoop(): Unit {
    loopJob?.cancel()
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray,
  ) {
    if (Core.jvmInit) Core.bridge.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  fun getUniqueFileName(uri: Uri): String {
    val originalName = getFileName(uri) ?: "unknown"
    val extension = originalName.substringAfterLast('.', "")
    val baseName = originalName.substringBeforeLast('.')
    val timestamp = System.currentTimeMillis()

    return if (extension.isNotEmpty()) {
      "${baseName}_${timestamp}.${extension}"
    } else {
      "${baseName}_${timestamp}"
    }
  }

  private fun getFileName(uri: Uri): String? {
    var name: String? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (cursor.moveToFirst() && nameIndex >= 0) {
        name = cursor.getString(nameIndex)
      }
    }
    return name
  }

  override fun haveExternalPermission(): Boolean =
      ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
          PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
              PackageManager.PERMISSION_GRANTED

  override fun getExternalPermission(code: Int): Unit {
    val perms = mutableListOf<String>()
    if (
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
    ) {
      perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    if (
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
    ) {
      perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    ActivityCompat.requestPermissions(this, perms.toTypedArray(), code)
  }

  override fun showFileChooser(
      open: Boolean,
      title: String,
      cons: (String) -> Unit,
      error: (String, String) -> Unit,
      finish: () -> Unit,
      vararg extensions: String,
  ) {
    try {
      val extension = extensions[0]

      val intent = Intent(if (open) Intent.ACTION_OPEN_DOCUMENT else Intent.ACTION_CREATE_DOCUMENT)
      intent.addCategory(Intent.CATEGORY_OPENABLE)
      intent.setType(
          if (extension == "zip" && !open && extensions.size == 1) "application/zip" else "*/*"
      )
      intent.putExtra(Intent.EXTRA_TITLE, "export.$extension")

      addResultListener(
          { startActivityForResult(intent, it) },
          { code, resultIntent ->
            if (code == Activity.RESULT_OK && resultIntent != null && resultIntent.data != null) {
              val uri = resultIntent.data!!

              if (uri.path?.contains("(invalid)") == true) return@addResultListener

              handler.post {
                OLPath.oxygenCacheDir.mkdirs()
                val file = OLPath.oxygenCacheDir.child(getUniqueFileName(uri))
                Log.info("Open file ${uri.path} cache to ${file.absolutePath()}")
                cacheFiles[file.absolutePath()] = uri
                if (open) {
                  contentResolver.openInputStream(uri)!!.use { input ->
                    file.write().use { output -> input.copyTo(output) }
                  }
                }
                cons(file.absolutePath())
                finish()
                if (file.exists()) {
                  if (!open)
                      contentResolver.openOutputStream(uri)!!.use { output ->
                        file.file().inputStream().use { input -> input.copyTo(output) }
                      }
                  file.delete()
                }
              }
            } else {
              handler.post(finish)
            }
          },
      )
    } catch (err: Throwable) {
      error(err.finalMessage()!!, err.neatError()!!)
    }
  }

  override fun postCacheFile(uri: String): Unit {
    val target = cacheFiles[uri]
    if (target == null) {
      Log.warn("try to post $uri but not found target")
      return
    }
    val file = Fi(uri)
    if (!file.exists()) {
      Log.warn("try to post $uri but not found the file")
      return
    }
    contentResolver.openOutputStream(target)!!.use { output ->
      file.file().inputStream().use { input -> input.copyTo(output) }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    synchronized(eventListeners) { eventListeners[requestCode]?.invoke(resultCode, data) }
    if (Core.jvmInit) Core.bridge.onActivityResult(requestCode, resultCode, "")
  }

  fun addResultListener(runner: (Int) -> Unit, listener: AndroidEventListener) {
    synchronized(eventListeners) {
      val id = lastEventNumber++
      eventListeners[id] = listener
      runner(id)
    }
  }

  fun interface AndroidEventListener : (Int, Intent?) -> Unit {
    override fun invoke(resultCode: Int, data: Intent?): Unit
  }
}

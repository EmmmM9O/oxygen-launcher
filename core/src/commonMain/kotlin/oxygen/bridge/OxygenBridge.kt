package oxygen.bridge

import java.io.*
import oxygen.*
import oxygen.input.*
import oxygen.util.*

class OxygenBridge {
  external fun setBridge(bridge: OxygenBridge): Unit

  external fun setupExitTrap(bridge: OxygenBridge): Unit

  external fun redirectStdio(path: String): Int

  external fun setLdLibraryPath(path: String): Unit

  external fun dlopen(path: String): Long

  external fun chdir(path: String): Int

  external fun setenv(key: String, value: String): Unit

  external fun getenv(key: String): String

  // Callback
  external fun loop(): Unit

  external fun onWindowFocusChanged(hasFocus: Boolean): Unit

  external fun onPause(): Unit

  external fun onResume(): Unit

  external fun onDestroy(): Unit

  external fun onConfigurationChanged(config: String): Unit

  external fun onExit(): Unit

  external fun onActivityResult(requestCode: Int, resultCode: Int, data: String): Unit

  external fun onSurfaceCreated(surface: Object): Unit

  external fun onSurfaceChanged(surface: Object, width: Int, height: Int): Unit

  external fun onSurfaceDestroyed(): Unit

  external fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray,
  )

  // Input callback
  external fun handleTouch(/*MotionEvent*/ intData: IntArray, floatData: FloatArray): Boolean

  external fun handleGenericMotion(
      /*MotionEvent*/ intData: IntArray,
      floatData: FloatArray,
  ): Boolean

  external fun handleKey(keyCode: Int, /*KeyEvent*/ intData: IntArray, characters: String): Boolean

  @Keep
  fun log(log: String) {
    FLog.log(log)
  }

  @Keep
  fun exit(code: Int) {
    if (code != 0) {
      // TODO crash activity
      Log.err("JVM crashed! code ${code}")
    }
    Core.platform.killProcess()
  }

  @Keep fun getVersion(): Int = Core.platform.getVersion()

  @Keep fun getNativeHeap(): Long = Core.platform.getNativeHeap()

  @Keep fun getClipboardText(): String? = Core.platform.getClipboardText()

  @Keep
  fun setClipboardText(contents: String): Unit {
    Core.platform.setClipboardText(contents)
  }

  @Keep fun openFolder(file: String): Boolean = Core.platform.openFolder(file)

  @Keep fun openURI(URI: String): Boolean = Core.platform.openURI(URI)

  @Keep
  fun createsurface(create: Boolean): Unit {
    Core.platform.createSurface(create)
  }

  @Keep fun isFinishing(): Boolean = Core.platform.finishing()

  @Keep
  fun showFileChooser(
      open: Boolean,
      title: String,
      cons: Long,
      error: Long,
      release: Long,
      extensions: Array<String>,
  ) {
    Core.platform.showFileChooser(
        open,
        title,
        { str -> FuncUtils.callStrCons(str, cons) },
        { str1, str2 -> FuncUtils.callStrCons2(str1, str2, error) },
        {
          FuncUtils.callVoid(release)
          FuncUtils.release(cons)
          FuncUtils.release(error)
          FuncUtils.release(release)
        },
        *extensions,
    )
  }

  @Keep fun haveExternalPermission(): Boolean = Core.platform.haveExternalPermission()

  @Keep
  fun getExternalPermission(code: Int): Unit {
    Core.platform.getExternalPermission(code)
  }

  @Keep
  fun hide(): Unit {
    Core.platform.hide()
  }

  @Keep
  fun beginForceLandscape(): Unit {
    Core.platform.beginForceLandscape()
  }

  @Keep
  fun endForceLandscape(): Unit {
    Core.platform.endForceLandscape()
  }

  @Keep
  fun postCacheFile(uri: String): Unit {
    Core.platform.postCacheFile(uri)
  }

  @Keep
  fun setAllSettings(json: String): Unit {
    Core.settings.loadFrom(json)
    Core.settings.flush()
  }

  @Keep fun getAllSettings(): String = Core.settings.toJsonString()

  @Keep
  fun setGameSettings(json: String): Unit {
    Core.settings.apply {
      game = asObj(json)
      flush()
    }
  }

  @Keep fun getGameSettings(): String = Core.settings.let { it.toJsonString(it.game) }

  @Keep
  fun setGameDefault(json: String) {
    Core.settings.setGameDefault(json)
  }

  @Keep
  fun startLoop() {
    Core.platform.startLoop()
  }

  @Keep
  fun endLoop() {
    Core.platform.endLoop()
  }

  @Keep
  fun setupInput() {
    Core.platform.setupInput()
  }

  // Input
  @Keep
  fun getTextInput(
      title: String,
      message: String,
      text: String,
      numeric: Boolean,
      multiline: Boolean,
      maxLength: Int,
      allowEmpty: Boolean,
      onAccepted: Long,
      onCanceled: Long,
      release: Long,
  ): Unit {
    Core.input?.getTextInput(
        TextInputConfig(
            title,
            message,
            text,
            numeric,
            multiline,
            maxLength,
            allowEmpty,
            { str -> FuncUtils.callStrCons(str, onAccepted) },
            { FuncUtils.callVoid(onCanceled) },
            {
              FuncUtils.callVoid(release)
              FuncUtils.release(onAccepted)
              FuncUtils.release(onCanceled)
              FuncUtils.release(release)
            },
        )
    )
  }

  @Keep fun isShowingTextInput(): Boolean = Core.input?.isShowingTextInput() ?: false

  @Keep
  fun setOnscreenKeyboardVisible(visible: Boolean) {
    Core.input?.setOnscreenKeyboardVisible(visible)
  }

  @Keep
  fun vibrate(milliseconds: Int) {
    Core.input?.vibrate(milliseconds)
  }

  @Keep
  fun vibrate(pattern: LongArray, repeat: Int) {
    Core.input?.vibrate(pattern, repeat)
  }

  @Keep
  fun cancelVibrate() {
    Core.input?.cancelVibrate()
  }

  fun execute() {
    setBridge(this)
    if (Core.settings.launcher.redirectStdio) redirectStdio(OLPath.logFile.absolutePath())
  }

  companion object {
    init {
      System.loadLibrary("oxygen")
    }
  }
}

object FuncUtils {
  external fun callVoid(ptr: Long): Unit

  external fun callStrCons(text: String, ptr: Long): Unit

  external fun callStrCons2(str1: String, str2: String, ptr: Long): Unit

  external fun release(ptr: Long)
}

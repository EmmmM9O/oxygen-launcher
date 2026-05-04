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

  // Input callback
  external fun handleTouch(/*MotionEvent*/ intData: IntArray, floatData: FloatArray): Boolean

  external fun handleGenericMotion(
      /*MotionEvent*/ intData: IntArray,
      floatData: FloatArray,
  ): Boolean

  external fun handleKey(keyCode: Int, /*KeyEvent*/ intData: IntArray): Boolean

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
  fun createsurface(): Unit {
    Core.platform.createSurface()
  }

  @Keep fun isFinishing(): Boolean = Core.platform.finishing()

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
  ): Unit {
    Core.input.getTextInput(
        TextInputConfig(
            title,
            message,
            text,
            numeric,
            multiline,
            maxLength,
            allowEmpty,
            { str -> CallStrCons(str, onAccepted) },
            { CallVoid(onCanceled) },
        )
    )
  }

  external fun CallVoid(ptr: Long): Unit

  external fun CallStrCons(text: String, ptr: Long): Unit

  @Keep fun isShowingTextInput(): Boolean = Core.input.isShowingTextInput()

  @Keep
  fun setOnscreenKeyboardVisible(visible: Boolean) {
    Core.input.setOnscreenKeyboardVisible(visible)
  }

  @Keep
  fun vibrate(milliseconds: Int) {
    Core.input.vibrate(milliseconds)
  }

  @Keep
  fun vibrate(pattern: LongArray, repeat: Int) {
    Core.input.vibrate(pattern, repeat)
  }

  @Keep
  fun cancelVibrate() {
    Core.input.cancelVibrate()
  }

  fun execute() {
    setBridge(this)
    val errorCode = redirectStdio(OLPath.logFile.absolutePath())
  }

  companion object {
    init {
      System.loadLibrary("oxygen")
    }
  }
}

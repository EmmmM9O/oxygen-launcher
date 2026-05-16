package oxygen.util

import java.io.*

// Thanks to sdl there are some unused functions
interface Platform {
  fun killProcess()

  fun getVersion(): Int

  fun getNativeHeap(): Long

  fun getClipboardText(): String? = ""

  fun setClipboardText(contents: String): Unit {}

  fun openFolder(file: String): Boolean = true

  fun openURI(URI: String): Boolean = true

  fun symlink(target: String, link: String): Unit

  fun openAssets(path: String): InputStream

  fun createSurface(create: Boolean): Unit

  fun finishing(): Boolean

  fun beginForceLandscape(): Unit {}

  fun endForceLandscape(): Unit {}

  fun showFileChooser(
      open: Boolean,
      title: String,
      cons: (String) -> Unit,
      error: (String, String) -> Unit,
      finish: () -> Unit,
      vararg extensions: String,
  ) {}

  fun postCacheFile(uri: String): Unit {}

  fun haveExternalPermission(): Boolean = false

  fun getExternalPermission(code: Int): Unit {}

  fun hide(): Unit {}

  fun startLoop(): Unit

  fun endLoop(): Unit

  fun setupInput(): Unit {}
}

package oxygen.util

import java.io.*

interface Platform {
  fun killProcess()

  fun getVersion(): Int

  fun getNativeHeap(): Long

  fun getClipboardText(): String?

  fun setClipboardText(contents: String): Unit

  fun openFolder(file: String): Boolean

  fun openURI(URI: String): Boolean

  fun symlink(target: String, link: String): Unit

  fun openAssets(path: String): InputStream

  fun createSurface(): Unit

  fun finishing(): Boolean

  fun beginForceLandscape(): Unit

  fun endForceLandscape(): Unit

  fun showFileChooser(
      open: Boolean,
      title: String,
      cons: (String) -> Unit,
      error: (String, String) -> Unit,
      finish: () -> Unit,
      vararg extensions: String,
  )

  fun postCacheFile(uri: String): Unit

  fun haveExternalPermission(): Boolean

  fun getExternalPermission(code: Int): Unit

  fun hide(): Unit
}

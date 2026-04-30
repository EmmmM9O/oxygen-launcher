package oxygen.util

import android.app.*
import android.content.res.*
import android.os.*
import java.io.*
import oxygen.*

class AndroidFiles(val assets: AssetManager, localpath: String) : Files {
  val localpath: String
  val sdcard = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"

  init {
    this.localpath = if (localpath.endsWith("/")) localpath else localpath + "/"
  }

  override fun get(path: String, type: FileType): Fi =
      AndroidFi(if (type == FileType.internal) assets else null, path, type)

  override fun getExternalStoragePath(): String = sdcard

  override fun getLocalStoragePath(): String = localpath
}

class AndroidFi : Fi {
  val assets: AssetManager?

  constructor(assets: AssetManager?, fileName: String, type: FileType) : super(fileName, type) {
    this.assets = assets
  }

  constructor(assets: AssetManager?, file: File, type: FileType) : super(file, type) {
    this.assets = assets
  }

  override fun child(name: String): Fi =
      if (file.getPath().length == 0) AndroidFi(assets, File(name), type)
      else AndroidFi(assets, File(file, name), type)

  override fun sibling(name: String): Fi =
      if (file.getPath().length == 0) throw RuntimeException("Cannot get the sibling of the root.")
      else Core.files.get(File(file.getParent(), name).getPath(), type)

  override fun parent(): Fi =
      AndroidFi(
          assets,
          file.getParentFile() ?: if (type == FileType.absolute) File("/") else File(""),
          type,
      )

  override fun read(): InputStream =
      if (type == FileType.internal)
          try {
            assets!!.open(file.getPath())
          } catch (ex: IOException) {
            throw RuntimeException("Error reading file: $file ($type)", ex)
          }
      else super.read()

  override fun isDirectory(): Boolean =
      if (type == FileType.internal)
          try {
            assets!!.list(file.getPath())!!.size > 0
          } catch (ex: IOException) {
            false
          }
      else super.isDirectory()

  override fun exists(): Boolean =
      if (type == FileType.internal)
          file.getPath().let {
            try {
              assets!!.open(it).close()
              true
            } catch (ex: Exception) {
              try {
                assets!!.list(file.getPath())!!.size > 0
              } catch (ex: IOException) {
                false
              }
              false
            }
          }
      else super.exists()

  override fun list(): Array<Fi> =
      if (type == FileType.internal)
          try {
            assets!!.list(file.getPath())!!.map(this::child).toTypedArray()
          } catch (ex: Exception) {
            throw RuntimeException("Error listing children: $file ($type)", ex)
          }
      else super.list()

  override fun length(): Long =
      if (type == FileType.local)
          runCatching { assets!!.openFd(file.path).use { it.length } }.getOrElse { super.length() }
      else super.length()

  override fun file(): File =
      if (type == FileType.local) File(Core.files.getLocalStoragePath(), file.getPath())
      else super.file()

  fun getAssetFileDescriptor(): AssetFileDescriptor? = assets?.openFd(path())
}

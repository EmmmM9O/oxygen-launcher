package oxygen.launcher

import java.io.*
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import oxygen.*
import oxygen.util.*

object JreManager {
  var java = false

  fun init() {
    java = isLatest(OLPath.javaPath, Core.files.classpath("assets/app_runtime/java/jre"))
  }

  fun isLatest(targetDir: Fi, srcDir: Fi): Boolean =
      srcDir.child("version").takeIf(Fi::exists)?.readString()?.toLong()?.let {
        targetDir.child("version").takeIf(Fi::exists)?.readString()?.toLong()?.equals(it) ?: false
      } ?: true

  fun install() {
    if (!java) {
      installJava(OLPath.javaPath, "app_runtime/java/jre")
      java = true
    }
  }

  fun getRelease(dir: Fi): String = dir.child("release").readString()

  fun installJava(targetDir: Fi, srcDir: String) {
    targetDir.deleteDirectory()
    targetDir.mkdirs()
    val universalPath = "$srcDir/universal.tar.xz"
    val archPath = "$srcDir/bin-${OS.archAsString(OS.getDeviceArchitecture())}.tar.xz"
    val version = Core.files.classpath("assets/$srcDir/version").readString()
    Log.info("Install Jre from $srcDir version $version")
    uncompressTarXZ(Core.platform.openAssets(universalPath), targetDir.file())
    uncompressTarXZ(Core.platform.openAssets(archPath), targetDir.file())
    targetDir.child("version").writeString(version)
    patchJava(targetDir)
  }

  fun uncompressTarXZ(tarFileInputStream: InputStream, dest: File) {
    dest.mkdirs()

    TarArchiveInputStream(XZCompressorInputStream(tarFileInputStream)).use { tarIn ->
      var tarEntry: TarArchiveEntry? = tarIn.nextTarEntry

      while (tarEntry != null) {
        if (tarEntry.size <= 20480) {
          try {
            Thread.sleep(25)
          } catch (ignored: InterruptedException) {
            // ignored
          }
        }

        val destPath = File(dest, tarEntry.name)

        when {
          tarEntry.isSymbolicLink -> {
            destPath.parentFile?.mkdirs()
            try {
              val target = tarEntry.linkName.replace("..", dest.absolutePath)
              val link = File(dest, tarEntry.name).absolutePath
              Core.platform.symlink(target, link)
            } catch (e: Throwable) {
              Log.warn(e.message!!)
            }
          }

          tarEntry.isDirectory -> {
            destPath.mkdirs()
            destPath.setExecutable(true)
          }

          !destPath.exists() || destPath.length() != tarEntry.size -> {
            destPath.parentFile?.mkdirs()
            destPath.createNewFile()

            FileOutputStream(destPath).use { os ->
              val buffer = ByteArray(1024)
              var byteCount: Int
              while (tarIn.read(buffer).also { byteCount = it } != -1) {
                os.write(buffer, 0, byteCount)
              }
            }
          }
        }

        tarEntry = tarIn.nextTarEntry
      }
    }
  }

  fun patchJava(targetDir: Fi) {
    // NO Needed
  }
}

package oxygen.util

import oxygen.*

object OLPath {
  val filesDir = OS.filesDir()
  val cacheDir = OS.cacheDir()
  val oxygenDir = filesDir.child("oxygen")
  val settingsFile = oxygenDir.child("settings.json")
  val oxygenCacheDir = oxygenDir.child("cache")
  val jarFile = oxygenDir.child("game.jar")
  val logFile = oxygenDir.child("last_log.txt")
  val crashDir = oxygenDir.child("crashes")
  val runtimeDir = OS.runtimeDir()
  val javaPath = runtimeDir.child("java")
  val nativeLibDir = OS.nativeLibDir()

  fun init() {
    initPath(oxygenDir)
    initPath(crashDir)
    initPath(runtimeDir)
    initPath(javaPath)
    initPath(oxygenCacheDir)
    oxygenCacheDir.emptyDirectory()
  }

  fun initPath(path: Fi) {
    if (!path.exists()) path.mkdirs()
  }
}

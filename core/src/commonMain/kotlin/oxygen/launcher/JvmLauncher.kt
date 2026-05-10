package oxygen.launcher

import oxygen.*
import oxygen.util.*

class JvmLauncher : Launcher() {
  override fun exit() {}

  override fun chdir(): String = OLPath.filesDir.absolutePath()

  override suspend fun launch(): Int {
    FLog.log("===Launch JVM===")
    // TODO
    return launchJvm(
        listOf("-jar", OLPath.jarFile.absolutePath()),
        Core.settings.launcher.userArgs,
    )
  }
}

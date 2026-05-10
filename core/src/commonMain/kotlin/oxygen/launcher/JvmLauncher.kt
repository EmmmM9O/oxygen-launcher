package oxygen.launcher

import oxygen.util.*

class JvmLauncher : Launcher() {
  override fun exit() {}

  override fun chdir(): String = OLPath.filesDir.absolutePath()

  override suspend fun launch(): Int {
    FLog.log("===Launch JVM===")
    // TODO
    return launchJvm(listOf("-jar", OLPath.jarFile.absolutePath()), "-Xms512M -Xmx4096M -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:MaxGCPauseMillis=130 -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=75 -XX:G1HeapWastePercent=5 -XX:+DisableExplicitGC -XX:+PerfDisableSharedMem")
  }
}

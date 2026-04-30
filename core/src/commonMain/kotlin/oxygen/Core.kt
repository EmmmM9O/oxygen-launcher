package oxygen

import java.io.*
import oxygen.bridge.*
import oxygen.launcher.*
import oxygen.util.*

object Core {
  lateinit var files: Files
  lateinit var bridge: OxygenBridge
  lateinit var logWriter: Writer
  lateinit var launcher: Launcher
  lateinit var platform: Platform
  var jvmInit = false
}

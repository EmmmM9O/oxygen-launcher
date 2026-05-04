package oxygen

import java.io.*
import oxygen.bridge.*
import oxygen.input.*
import oxygen.launcher.*
import oxygen.util.*

object Core {
  lateinit var files: Files
  lateinit var bridge: OxygenBridge
  lateinit var logWriter: Writer
  lateinit var launcher: Launcher
  lateinit var platform: Platform
  lateinit var input: Input
  var jvmInit = false
}

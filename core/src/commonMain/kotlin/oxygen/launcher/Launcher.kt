package oxygen.launcher

import com.oracle.dalvik.VMLauncher
import java.io.File
import java.util.Locale
import java.util.TimeZone
import oxygen.*
import oxygen.util.*

abstract class Launcher {
  abstract suspend fun launch(): Int

  abstract fun chdir(): String

  abstract fun exit(): Unit

  protected suspend fun launchJvm(jvmArgs: List<String>, userArgs: String): Int {
    Core.bridge.setLdLibraryPath(getRuntimeLibraryPath())
    FLog.log("===Env Map===")
    setEnv()

    FLog.log("===DLOPEN Java Runtime===")
    dlopenJavaRuntime()

    return launchJavaVM(jvmArgs = jvmArgs, userArgs = userArgs)
  }

  private suspend fun launchJavaVM(jvmArgs: List<String>, userArgs: String): Int {
    val args = getJavaArgs(userArgs).toMutableList()
    progressFinalUserArgs(args)

    args.addAll(jvmArgs)
    args.add(0, "$runtimeHome/bin/java")

    FLog.log("===JVM Args===")
    val iterator = args.iterator()
    while (iterator.hasNext()) {
      val arg = iterator.next()
      FLog.log("JVMArgs: $arg")
    }

    Core.bridge.setupExitTrap(Core.bridge)
    Core.bridge.chdir(chdir())

    Core.jvmInit = true
    val exitCode = VMLauncher.launchJVM(args.toTypedArray())
    Core.jvmInit = false
    Log.info("Java Exit code: $exitCode")
    return exitCode
  }

  protected open fun progressFinalUserArgs(args: MutableList<String>) {
    args.purgeArg("-Xms")
    args.purgeArg("-Xmx")
    args.purgeArg("-d32")
    args.purgeArg("-d64")
    args.purgeArg("-Xint")
    args.purgeArg("-XX:+UseTransparentHugePages")
    args.purgeArg("-XX:+UseLargePagesInMetaspace")
    args.purgeArg("-XX:+UseLargePages")
    args.add("--enable-native-access=ALL-UNNAMED")
  }

  protected fun MutableList<String>.purgeArg(argStart: String) {
    removeIf { arg: String -> arg.startsWith(argStart) }
  }

  private fun getJavaArgs(
      userArgumentsString: String,
  ): List<String> {
    val userArguments = parseJavaArguments(userArgumentsString).toMutableList()
    val overridableArguments =
        mutableMapOf<String, String>()
            .apply {
              put("oxygen.home", OLPath.filesDir.absolutePath())
              put("java.home", getJavaHome())
              put("java.io.tmpdir", OLPath.cacheDir.absolutePath())
              put("user.language", System.getProperty("user.language"))
              put("user.country", Locale.getDefault().country)
              put("user.timezone", TimeZone.getDefault().id)
              put("os.name", "Linux")
              put("os.version", "Android-${Core.platform.getVersion()}")
              put("oxygenlauncher.nativedir", OLPath.nativeLibDir.absolutePath())
              fun prop(name: String): String? = System.getProperty(name)
              listOf("os.arch").forEach { name -> prop(name)?.let { put(name, it) } }
            }
            .map { entry -> "-D${entry.key}=${entry.value}" }
    val additionalArguments =
        overridableArguments.filter { arg ->
          val stripped = arg.substringBefore('=')
          val overridden = userArguments.any { it.startsWith(stripped) }
          if (overridden) {
            Log.info("Arg skipped: $arg")
          }
          !overridden
        }

    userArguments += additionalArguments
    return userArguments
  }

  private val runtimeHome: String by lazy { OLPath.javaPath.absolutePath() }

  private fun getJavaHome() = runtimeHome

  protected fun getJavaLibDir(): String {
    val architecture =
        OS.getDeviceArchitecture().let {
          if (it == Arch.X86_64) "i386/i486/i586" else OS.archAsString(it)
        }

    var libDir = "/lib"
    architecture.split("/").forEach { arch ->
      val file = File(runtimeHome, "lib/$arch")
      if (file.exists() && file.isDirectory()) {
        libDir = "/lib/$arch"
      }
    }
    return libDir
  }

  private fun getJvmLibDir(): String {
    val jvmLibDir: String
    val path = getJavaLibDir()
    val jvmFile = File("$runtimeHome$path/server/libjvm.so")
    jvmLibDir = if (jvmFile.exists()) "/server" else "/client"
    return jvmLibDir
  }

  protected fun getRuntimeLibraryPath(): String {
    val javaLibDir = getJavaLibDir()
    val jvmLibDir = getJvmLibDir()

    val libName = if (OS.is64Bit) "lib64" else "lib"
    val paths = buildList {
      add("$runtimeHome$javaLibDir/jli")
      add("$runtimeHome$javaLibDir$jvmLibDir")
      add("/system/$libName")
      add("/vendor/$libName")
      add("/vendor/$libName/hw")
      add("/system_ext/$libName")
      add(OLPath.nativeLibDir.absolutePath())
    }
    return paths.joinToString(":")
  }

  protected fun getLibraryPath(): String {
    val libDirName = if (OS.is64Bit) "lib64" else "lib"
    val path =
        listOfNotNull(
            "/system/$libDirName",
            "/vendor/$libDirName",
            "/vendor/$libDirName/hw",
            "/system_ext/$libDirName",
            OLPath.nativeLibDir.absolutePath(),
        )
    return path.joinToString(":")
  }

  private fun setEnv() {
    val envMap = initEnv()
    envMap.forEach { (key, value) ->
      FLog.log("Added env: $key = $value")
      runCatching { Core.bridge.setenv(key, value) }
          .onFailure {
            Log.err("Unable to set environment variable.")
            Log.err(it)
          }
    }
  }

  protected open fun initEnv(): MutableMap<String, String> {
    val env: MutableMap<String, String> = mutableMapOf()
    setJavaEnv { env }
    return env
  }

  private fun setJavaEnv(envMap: () -> MutableMap<String, String>) {
    val path = listOfNotNull("$runtimeHome/bin", Core.bridge.getenv("PATH"))
    envMap().let { map ->
      map["LAUNCHER_NATIVEDIR"] = OLPath.nativeLibDir.absolutePath()
      map["JAVA_HOME"] = getJavaHome()
      map["HOME"] = OLPath.filesDir.absolutePath()
      map["TMPDIR"] = OLPath.cacheDir.absolutePath()
      map["LD_LIBRARY_PATH"] = getLibraryPath()
      map["PATH"] = path.joinToString(":")
    }
  }

  private fun dlopenJavaRuntime() {
    var javaLibDir = "$runtimeHome${getJavaLibDir()}"
    val jliLibDir =
        if (File("$javaLibDir/jli/libjli.so").exists()) "$javaLibDir/jli" else javaLibDir

    val jvmLibDir = "$javaLibDir${getJvmLibDir()}"

    Core.bridge.apply {
      dlopen("$jliLibDir/libjli.so")
      dlopen("$jvmLibDir/libjvm.so")
      // dlopen("$javaLibDir/libfreetype.so")
      dlopen("$javaLibDir/libverify.so")
      dlopen("$javaLibDir/libjava.so")
      dlopen("$javaLibDir/libnet.so")
      dlopen("$javaLibDir/libnio.so")
      // dlopen("$javaLibDir/libawt.so")
      // dlopen("$javaLibDir/libawt_headless.so")
      // dlopen("$javaLibDir/libfontmanager.so")
      locateLibs(File(runtimeHome)).forEach { file -> dlopen(file.absolutePath) }
    }
  }

  private fun locateLibs(path: File): List<File> {
    val children = path.listFiles() ?: return emptyList()
    return children.flatMap { file ->
      when {
        file.isFile && file.name.endsWith(".so") -> listOf(file)
        file.isDirectory -> locateLibs(file)
        else -> emptyList()
      }
    }
  }
}

/**
 * [Modified from
 * PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/98947f2/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/JREUtils.java#L411-L456)
 * from ZalithLauncher
 */
fun parseJavaArguments(args: String): List<String> {
  val parsedArguments = mutableListOf<String>()
  var cleanedArgs = args.trim().replace(" ", "")
  val separators = listOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")

  for (prefix in separators) {
    while (true) {
      val start = cleanedArgs.indexOf(prefix)
      if (start == -1) break

      val end =
          separators
              .mapNotNull { sep ->
                val i = cleanedArgs.indexOf(sep, start + prefix.length)
                if (i != -1) i else null
              }
              .minOrNull() ?: cleanedArgs.length

      val parsedSubstring = cleanedArgs.substring(start, end)
      cleanedArgs = cleanedArgs.replace(parsedSubstring, "")

      if (parsedSubstring.indexOf('=') == parsedSubstring.lastIndexOf('=')) {
        val last = parsedArguments.lastOrNull()
        if (last != null && (last.endsWith(',') || parsedSubstring.contains(','))) {
          parsedArguments[parsedArguments.lastIndex] = last + parsedSubstring
        } else {
          parsedArguments.add(parsedSubstring)
        }
      } else {
        Log.warn("Removed improper arguments: $parsedSubstring")
      }
    }
  }

  return parsedArguments
}

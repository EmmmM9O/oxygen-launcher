package oxygen.util

import android.util.Log
import java.io.*
import oxygen.*

object Log {
  var level = LogLevel.debug
  var logger: LogHandler = AndroidLogHandler()

  fun log(level: LogLevel, str: String) {
    logger.log(level, str)
  }

  fun debug(str: String) {
    log(LogLevel.debug, str)
  }

  fun info(str: String) {
    log(LogLevel.info, str)
  }

  fun warn(str: String) {
    log(LogLevel.warn, str)
  }

  fun err(str: String) {
    log(LogLevel.err, str)
  }

  fun getStackTrace(th: Throwable): String =
      StringWriter().let {
        PrintWriter(it).let(th::printStackTrace)
        it.toString()
      }

  fun err(th: Throwable) {
    err(getStackTrace(th))
  }
}

fun Throwable.trace(): String = oxygen.util.Log.getStackTrace(this)

enum class LogLevel {
  debug,
  info,
  warn,
  err,
  none,
}

fun interface LogHandler {
  fun log(level: LogLevel, str: String)
}

class AndroidLogHandler : LogHandler {
  override fun log(level: LogLevel, str: String) {
    when (level) {
      LogLevel.debug -> Log.d("Oxygen", str)
      LogLevel.info -> Log.i("Oxygen", str)
      LogLevel.warn -> Log.w("Oxygen", str)
      LogLevel.err -> Log.e("Oxygen", str)
      LogLevel.none -> {}
    }
  }
}

object FLog {
  fun log(log: String) {
    try {
      Core.logWriter.write("$log\n")
      Core.logWriter.flush()
    } catch (e: IOException) {
      e.printStackTrace()
      // ignore it
    }
  }
}

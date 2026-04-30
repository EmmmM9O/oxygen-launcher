package oxygen.util

import java.io.*
import java.nio.*

object Streams {
  fun close(c: Closeable?) {
    if (c != null) {
      try {
        c.close()
      } catch (ignored: Throwable) {}
    }
  }
}

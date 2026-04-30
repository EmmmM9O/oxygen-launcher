package oxygen.util

import java.io.*
import java.nio.*
import oxygen.*

open class Fi {
  protected var file: File
  protected var type: FileType

  constructor(fileName: String) {
    this.file = File(fileName)
    this.type = FileType.absolute
  }

  constructor(fileName: String, type: FileType) {
    this.file = File(fileName)
    this.type = type
  }

  constructor(file: File) {
    this.file = file
    this.type = FileType.absolute
  }

  constructor(file: File, type: FileType) {
    this.file = file
    this.type = type
  }

  fun path(): String = file.getPath().replace('\\', '/')

  fun absolutePath(): String = file.getAbsolutePath().replace('\\', '/')

  fun name(): String = file.getName().takeIf(String::isNotEmpty) ?: file.getPath()

  fun extEquals(ext: String): Boolean = extension().equals(ext, ignoreCase = true)

  fun extension(): String =
      name().let {
        val dotIndex = it.lastIndexOf('.')
        if (dotIndex == -1) "" else it.substring(dotIndex + 1)
      }

  fun nameWithoutExtension(): String =
      name().let {
        val dotIndex = it.lastIndexOf('.')
        if (dotIndex == -1) it else it.substring(0, dotIndex)
      }

  fun pathWithoutExtension(): String =
      path().let {
        val dotIndex = it.lastIndexOf('.')
        if (dotIndex == -1) it else it.substring(0, dotIndex)
      }

  fun type(): FileType = type

  open fun file(): File =
      if (type == FileType.external) File(Core.files.getExternalStoragePath(), file.getPath())
      else file

  open fun length(): Long {
    if (type == FileType.classpath || (type == FileType.internal && !file.exists())) {
      val input = read()
      try {
        return input.available().toLong()
      } catch (ignored: Exception) {} finally {
        Streams.close(input)
      }
      return 0
    }
    return file().length()
  }

  fun estimateLength(): Int = length().toInt().takeIf { it != 0 } ?: 512

  open fun read(): InputStream =
      if (
          type == FileType.classpath ||
              (type == FileType.internal && !file().exists()) ||
              (type == FileType.local && !file().exists())
      )
          Fi::class.java.getResourceAsStream("/${file.getPath().replace('\\', '/')}")
              ?: throw RuntimeException("File not found: $file ($type)")
      else
          try {
            FileInputStream(file())
          } catch (ex: Exception) {
            if (file().isDirectory())
                throw RuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw RuntimeException("Error writing file: $file ($type)", ex)
          }

  fun reader(charset: String): Reader {
    val stream = read()
    try {
      return InputStreamReader(stream, charset)
    } catch (ex: UnsupportedEncodingException) {
      Streams.close(stream)
      throw RuntimeException("Error reading file: $this", ex)
    }
  }

  fun readString(charset: String?): String {
    val output = StringBuilder(estimateLength())
    val reader =
        try {
          if (charset == null) InputStreamReader(read()) else InputStreamReader(read(), charset)
        } catch (ex: IOException) {
          throw RuntimeException("Error reading layout file: $this", ex)
        }
    val buffer = CharArray(256)
    while (true) {
      val length = reader.read(buffer)
      if (length == -1) break
      output.append(buffer, 0, length)
    }
    Streams.close(reader)
    return output.toString()
  }

  fun readString(): String = readString("UTF-8")

  fun mkdirs(): Boolean {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot mkdirs with an $type file: $file")
    return file().mkdirs()
  }

  open fun exists(): Boolean =
      when (type) {
        FileType.internal ->
            file().exists() ||
                Fi::class.java.getResource("/${file?.path?.replace('\\', '/')}") != null
        FileType.classpath ->
            Fi::class.java.getResource("/${file?.path?.replace('\\', '/')}") != null
        else -> file().exists()
      }

  fun delete(): Boolean {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot delete with an $type file: $file")
    return file().delete()
  }

  fun deleteDirectory(): Boolean {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot delete with an $type file: $file")
    return deleteDirectory(file())
  }

  fun emptyDirectory() {
    emptyDirectory(false)
  }

  fun emptyDirectory(preserveTree: Boolean) {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot delete with an $type file: $file")
    emptyDirectory(file(), preserveTree)
  }

  open fun isDirectory(): Boolean = if (type == FileType.classpath) false else file().isDirectory()

  open fun child(name: String): Fi =
      if (file.getPath().length == 0) Fi(File(name), type) else Fi(File(file, name), type)

  open fun sibling(name: String): Fi =
      if (file.getPath().length == 0) throw RuntimeException("Cannot get the sibling of the root.")
      else Fi(File(file.getParent(), name), type)

  open fun parent(): Fi =
      Fi(file.getParentFile() ?: if (type == FileType.absolute) File("/") else File(""), type)

  fun write(): OutputStream = write(false)

  fun write(append: Boolean): OutputStream {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot write to an $type file: $file")
    parent().mkdirs()
    try {
      return FileOutputStream(file(), append)
    } catch (ex: Exception) {
      if (file().isDirectory())
          throw RuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
      throw RuntimeException("Error writing file: $file ($type)", ex)
    }
  }

  fun writer(append: Boolean): Writer = writer(append, "UTF-8")

  fun writer(append: Boolean, charset: String?): Writer {
    if (type == FileType.classpath || type == FileType.internal)
        throw RuntimeException("Cannot write to an $type file: $file")
    parent().mkdirs()
    try {
      val output = FileOutputStream(file(), append)
      if (charset == null) return OutputStreamWriter(output)
      else return OutputStreamWriter(output, charset)
    } catch (ex: IOException) {
      if (file().isDirectory())
          throw RuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
      throw RuntimeException("Error writing file: $file ($type)", ex)
    }
  }

  fun writeString(string: String) {
    writeString(string, false)
  }

  fun writeString(string: String, append: Boolean) {
    writeString(string, append, "UTF-8")
  }

  fun writeString(string: String, append: Boolean, charset: String) {
    runCatching { writer(append, charset).use { it.write(string) } }
        .onFailure { throw RuntimeException("Error writing file: $file ($type)", it) }
  }

  open fun list(): Array<Fi> =
      if (type == FileType.classpath)
          throw RuntimeException("Cannot list a classpath directory: $file")
      else file().list()?.map(this::child)?.toTypedArray() ?: emptyArray()

  companion object {
    fun get(path: String): Fi = Fi(path)

    fun emptyDirectory(file: File, preserveTree: Boolean) {
      if (file.exists()) {
        file.listFiles()?.forEach { value ->
          if (!value.isDirectory()) value.delete()
          else if (preserveTree) emptyDirectory(value, true) else deleteDirectory(value)
        }
      }
    }

    fun deleteDirectory(file: File): Boolean {
      emptyDirectory(file, false)
      return file.delete()
    }
  }
}

enum class FileType {
  classpath,
  internal,
  external,
  absolute,
  local,
}

interface Files {
  fun get(path: String, type: FileType): Fi

  fun classpath(path: String): Fi = get(path, FileType.classpath)

  fun internal(path: String): Fi = get(path, FileType.internal)

  fun external(path: String): Fi = get(path, FileType.external)

  fun absolue(path: String): Fi = get(path, FileType.absolute)

  fun local(path: String): Fi = get(path, FileType.local)

  fun getExternalStoragePath(): String

  fun getLocalStoragePath(): String
}

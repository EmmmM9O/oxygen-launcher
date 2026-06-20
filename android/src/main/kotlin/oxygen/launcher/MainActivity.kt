package oxygen.launcher

import android.app.*
import android.content.*
import android.content.res.*
import android.net.*
import android.os.*
import android.telephony.*
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.*
import androidx.core.app.*
import androidx.core.content.*
import io.github.emmmm9o.oxygenlauncher.*
import java.io.*
import java.lang.Thread.*
import java.net.*
import java.util.*
import java.util.zip.*
import kotlin.concurrent.*
import kotlinx.coroutines.*
import org.json.*
import oxygen.*
import oxygen.bridge.*
import oxygen.input.*
import oxygen.surfaceview.*
import oxygen.util.*

class MainActivity : AndroidActivity() {

  private var receiver: BroadcastReceiver? = null
  private var downloadId: Long = 0
  private val pickFileLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
          contentResolver.openInputStream(uri)!!.use { input ->
            OLPath.gameJarTmp.write().use { output -> input.copyTo(output) }
          }
          mergeGame()
        }
      }

  fun mergeGame() {
    mergeJarFiles(
        listOf(
            Core.files.classpath("assets/app_runtime/mdt/mindustry-addon.jar"),
            OLPath.gameJarTmp,
        ),
        OLPath.gameJar,
    )
    OLPath.gameJarTmp.delete()
    Core.files.classpath("assets/app_runtime/mods/oxygen-launcher-ui.jar").copyTo(OLPath.modsDir)
    Toast.makeText(applicationContext, R.string.finish, Toast.LENGTH_LONG).show()
    finish()
  }

  fun mergeJarFiles(jarFiles: List<Fi>, outputJar: Fi) {
    val addedEntries = HashSet<String>()
    ZipOutputStream(BufferedOutputStream(outputJar.write())).use { zos ->
      zos.setMethod(ZipOutputStream.DEFLATED)
      for (jarFile in jarFiles) {
        ZipInputStream(BufferedInputStream(jarFile.read())).use { zis ->
          var entry = zis.nextEntry
          while (entry != null) {
            if (!addedEntries.contains(entry.name)) {
              zos.putNextEntry(ZipEntry(entry.name))
              zis.copyTo(zos)
              zos.closeEntry()
              addedEntries.add(entry.name)
            }
            entry = zis.nextEntry
          }
        }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(
              "loading",
              getString(R.string.java),
              NotificationManager.IMPORTANCE_LOW,
          )
      val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun createProgressNotification(): Notification {
    return NotificationCompat.Builder(this, "loading")
        .setContentTitle("Install Java")
        .setContentText("Please wait")
        .setSmallIcon(android.R.drawable.ic_popup_sync)
        .setOngoing(true)
        .setProgress(0, 0, true)
        .build()
  }

  private fun createCompleteNotification(): Notification {
    return NotificationCompat.Builder(this, "loading")
        .setContentTitle("Install Java")
        .setContentText("Finished")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setOngoing(false)
        .setProgress(0, 0, false)
        .build()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    init()
    Log.info("[OxygenL][ANDROID] Android:  ${Build.VERSION.RELEASE}")
    Log.info("[OxygenL][ANDROID] API level: ${Build.VERSION.SDK_INT}")
    Log.info("[OxygenL][ANDROID] Device:  ${OS.deviceName()}")
    Log.info("[OxygenL][ANDROID] CPU:  ${OS.socName()}")
    Build.SUPPORTED_ABIS.forEachIndexed { index, abi ->
      Log.info("[OxygenL][ABI${index + 1}] : $abi")
    }
    Log.info(
        "[OxygenL][OS] : ${System.getProperty("os.name")} v${System.getProperty("os.version")}"
    )
    Log.info(
        "[OxygenL][JVM] : ${System.getProperty("java.vm.name")} v${System.getProperty("java.vm.version")} by${System.getProperty("java.vm.vendor")}"
    )
    val runtime = Runtime.getRuntime()
    Log.info(
        "[OxygenL][RAM] : ${formatMemory(runtime.totalMemory() - runtime.freeMemory())} / ${formatMemory(runtime.totalMemory())} / ${formatMemory(runtime.maxMemory())}"
    )
    Log.info("[OxygenL][CPU] Cores : ${runtime.availableProcessors()}")
    JreManager.init()
    var flag = false
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (!JreManager.java) {
      createNotificationChannel()
      notificationManager.notify(1, createProgressNotification())
      flag = true
    }
    JreManager.install()
    if (flag) notificationManager.notify(1, createCompleteNotification())
    Log.info("[OxygenL][IN JRE]:RELEASE\n${JreManager.getRelease(OLPath.javaPath)}")

    if (!OLPath.gameJar.exists() && !Core.settings.launcher.skipCheckJar) {
      Log.info("${OLPath.gameJar.absolutePath()} Not exists")
      AlertDialog.Builder(this)
          .setTitle(getString(R.string.jar_unfound_message, OLPath.gameJar.absolutePath()))
          .setItems(
              listOf(
                      R.string.button_ignore,
                      R.string.button_import,
                      R.string.download,
                      R.string.button_open,
                      R.string.button_confirm,
                  )
                  .map(::getString)
                  .toTypedArray()
          ) { dialog, which ->
            when (which) {
              0 -> {
                Core.settings.launcher = Core.settings.launcher.copy(skipCheckJar = true)
                finish()
              }
              1 -> {
                Toast.makeText(this, R.string.wait, Toast.LENGTH_LONG).show()
                pickFileLauncher.launch(arrayOf("application/java-archive"))
              }
              2 -> {
                getGameUri {
                  val request =
                      DownloadManager.Request(Uri.parse(it))
                          .setTitle("Downloading Mindustry")
                          .setNotificationVisibility(
                              DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                          )
                          .setDestinationUri(Uri.fromFile(OLPath.gameJarTmp.file()))
                          .setAllowedOverMetered(true)
                  var dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                  downloadId = dm.enqueue(request)
                  receiver =
                      object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                          val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                          if (id == downloadId) {
                            checkDownloadStatus()
                          }
                        }
                      }
                  registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                }
              }
              3 -> {
                openFolderAndFinish(this, OLPath.gameJar.parent().absolutePath())
              }
              4 -> {
                finish()
              }
            }
            dialog.dismiss()
          }
          .show()
      return
    }
    runJVM()
  }

  fun getLatestUri(callback: (String) -> Unit): Unit {
    thread {
      try {
        val url = URL("https://api.github.com/repos/Anuken/Mindustry/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")

        val json = connection.inputStream.bufferedReader().readText()
        val jsonObject = JSONObject(json)
        val assets = jsonObject.getJSONArray("assets")

        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val name = asset.getString("name")
          if (name.equals("Mindustry.jar")) {
            callback(asset.getString("browser_download_url"))
            return@thread
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun getGameUri(callback: (String) -> Unit): Unit {
    getLatestUri {
      callback(
          "${if(Core.settings.launcher.enableMirror) Core.settings.launcher.mirror else ""}$it"
      )
    }
  }

  fun checkDownloadStatus() {
    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    val query = DownloadManager.Query().setFilterById(downloadId)
    val cursor = dm.query(query)

    if (cursor.moveToFirst()) {
      val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
      when (status) {
        DownloadManager.STATUS_SUCCESSFUL -> {
          Toast.makeText(this, "Download Successful", Toast.LENGTH_SHORT).show()
          mergeGame()
        }
        DownloadManager.STATUS_FAILED -> {
          Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
      }
    }
    cursor.close()
  }

  fun openFolderAndFinish(context: Activity, filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, getMimeType(filePath))
          flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    context.startActivity(Intent.createChooser(intent, getString(R.string.chooser)))
    context.finish()
  }

  fun getMimeType(path: String): String {
    val extension = MimeTypeMap.getFileExtensionFromUrl(path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
  }

  override fun onDestroy() {
    super.onDestroy()
    receiver?.let(::unregisterReceiver)
  }

  override fun handleCrash(code: Int) {
    val dia =
        AlertDialog.Builder(this)
            .setTitle(R.string.crash_title)
            .setMessage(getString(R.string.crash_message, code))
            .setPositiveButton(R.string.button_replace) { dialog, _ ->
              pickFileLauncher.launch(arrayOf("application/java-archive"))
            }
            .setPositiveButton(R.string.button_log) { dialog, _ ->
              openFolder(OLPath.logFile.absolutePath())
            }
            .setPositiveButton(getString(R.string.button_confirm)) { dialog, _ -> dialog.dismiss() }
            .show()
    dia.setOnDismissListener { killProcess() }
  }
}

package com.coolappstore.evercallrecorder.by.svhp.system

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val apkUrl: String?
)

suspend fun fetchLatestRelease(apiUrl: String): ReleaseInfo? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout    = 10_000
        }
        if (connection.responseCode != 200) return@withContext null
        val body = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val tag  = json.optString("tag_name", "")
        val assets = json.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
        }
        ReleaseInfo(tagName = tag.trimStart('v', 'V'), apkUrl = apkUrl)
    } catch (_: Exception) { null }
}

fun isNewerVersion(latest: String, current: String): Boolean {
    fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
    val l = parts(latest); val c = parts(current)
    val len = maxOf(l.size, c.size)
    for (i in 0 until len) {
        val lp = l.getOrElse(i) { 0 }; val cp = c.getOrElse(i) { 0 }
        if (lp > cp) return true; if (lp < cp) return false
    }
    return false
}

private const val APK_FILE_NAME         = "EverCallRecorder_update.apk"
private const val PREFS_UPDATE          = "update_checker"
private const val KEY_DOWNLOADED_VERSION = "downloaded_version"

fun getApkDestinationFile(): File =
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)

/** Returns true if the APK for [version] is already fully downloaded and ready to install. */
fun isApkReadyToInstall(context: Context, version: String): Boolean {
    val file = getApkDestinationFile()
    if (!file.exists() || file.length() == 0L) return false
    val saved = context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
        .getString(KEY_DOWNLOADED_VERSION, null)
    return saved == version
}

/** Persists which version is in the APK file. Call this after a successful download. */
fun saveDownloadedVersion(context: Context, version: String) {
    context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
        .edit().putString(KEY_DOWNLOADED_VERSION, version).apply()
}

/** Clears the cached version record and deletes the APK file. */
fun clearDownloadedApk(context: Context) {
    context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
        .edit().remove(KEY_DOWNLOADED_VERSION).apply()
    runCatching { getApkDestinationFile().delete() }
}

fun enqueueApkDownload(context: Context, apkUrl: String): Long? {
    return try {
        val file = getApkDestinationFile()
        // Delete only if it exists (version check is done by caller before reaching here)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Ever Call Recorder Update")
            setDescription("Downloading latest version…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    } catch (_: Exception) { null }
}

fun installApkAndScheduleDelete(context: Context, file: File) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val packageInstaller = context.packageManager.packageInstaller
            val params    = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session   = packageInstaller.openSession(sessionId)

            FileInputStream(file).use { fis ->
                session.openWrite("package", 0, file.length()).use { os ->
                    fis.copyTo(os)
                    session.fsync(os)
                }
            }

            val installResultAction = "${context.packageName}.INSTALL_RESULT"
            val resultReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                    val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                    if (status == PackageInstaller.STATUS_SUCCESS) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, IntentFilter(installResultAction), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(resultReceiver, IntentFilter(installResultAction))
            }

            val intent = Intent(installResultAction)
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            } else {
                PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            session.commit(pi.intentSender)
        } else {
            installApkLegacy(context, file)
        }
    } catch (e: Exception) {
        try { installApkLegacy(context, file) } catch (_: Exception) {
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

private fun installApkLegacy(context: Context, file: File) {
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } else {
        Uri.fromFile(file)
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
    Thread { Thread.sleep(90_000); try { file.delete() } catch (_: Exception) {} }.start()
}

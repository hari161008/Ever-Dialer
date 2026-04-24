package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
            readTimeout = 10_000
        }
        val code = connection.responseCode
        if (code != 200) return@withContext null
        val body = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val tag = json.optString("tag_name", "")
        val assets = json.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
        }
        ReleaseInfo(tagName = tag.trimStart('v', 'V'), apkUrl = apkUrl)
    } catch (e: Exception) {
        null
    }
}

fun isNewerVersion(latest: String, current: String): Boolean {
    fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
    val l = parts(latest)
    val c = parts(current)
    val len = maxOf(l.size, c.size)
    for (i in 0 until len) {
        val lp = l.getOrElse(i) { 0 }
        val cp = c.getOrElse(i) { 0 }
        if (lp > cp) return true
        if (lp < cp) return false
    }
    return false
}

fun downloadAndInstallApk(context: Context, apkUrl: String) {
    try {
        val fileName = "EverDialer_update.apk"
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Ever Dialer Update")
            setDescription("Downloading latest version…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        Toast.makeText(context, "Downloading update…", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                    installApk(context, file)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun installApk(context: Context, file: File) {
    try {
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

        // Schedule cleanup: delete APK after 60 s (app will be relaunched after install)
        Thread {
            Thread.sleep(60_000)
            try { file.delete() } catch (_: Exception) {}
        }.start()
    } catch (e: Exception) {
        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

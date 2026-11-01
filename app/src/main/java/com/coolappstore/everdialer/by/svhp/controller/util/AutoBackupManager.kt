package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoBackupManager {

    const val ACTION_AUTO_BACKUP_TRIGGER = "com.coolappstore.everdialer.by.svhp.AUTO_BACKUP_TRIGGER"
    private const val REQUEST_CODE_AUTO_BACKUP = 4129

    /**
     * Schedules the next automatic backup alarm based on the user's interval configuration.
     * Cancels existing scheduled alarms if auto backup is disabled.
     */
    fun scheduleAutoBackup(context: Context) {
        val prefs = PreferenceManager(context)
        if (!prefs.isAutoBackupEnabled()) {
            cancelAutoBackup(context)
            return
        }

        val intervalValue = prefs.getAutoBackupIntervalValue().coerceAtLeast(1)
        val intervalUnit = prefs.getAutoBackupIntervalUnit() // "minutes", "hours", "days"

        val durationMillis = when (intervalUnit.lowercase(Locale.ROOT)) {
            "minutes" -> intervalValue * 60 * 1000L
            "days" -> intervalValue * 24 * 60 * 60 * 1000L
            else -> intervalValue * 60 * 60 * 1000L // default hours
        }

        val triggerAt = System.currentTimeMillis() + durationMillis
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = getPendingIntent(context)

        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
            return
        } catch (_: Exception) {
            // Fall through to exact / idle alarms
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Cancels any scheduled automatic backup.
     */
    fun cancelAutoBackup(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(context))
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            action = ACTION_AUTO_BACKUP_TRIGGER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE_AUTO_BACKUP, intent, flags)
    }

    /**
     * Executes the automatic backup process and triggers auto-delete.
     * Runs asynchronously on Dispatchers.IO.
     */
    fun performAutoBackup(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferenceManager(context)
                if (!prefs.isAutoBackupEnabled()) return@launch

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "EverDialer_AutoBackup_$timestamp.everdialer"

                val folderUriStr = prefs.getAutoBackupFolderUri()
                var outputStream: OutputStream? = null

                if (!folderUriStr.isNullOrBlank()) {
                    try {
                        val treeUri = Uri.parse(folderUriStr)
                        val dir = DocumentFile.fromTreeUri(context, treeUri)
                        if (dir != null && dir.exists() && dir.canWrite()) {
                            val newDoc = dir.createFile("application/octet-stream", fileName)
                            if (newDoc != null) {
                                outputStream = context.contentResolver.openOutputStream(newDoc.uri)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (outputStream == null) {
                    val localDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
                    if (!localDir.exists()) localDir.mkdirs()
                    val targetFile = File(localDir, fileName)
                    outputStream = targetFile.outputStream()
                }

                outputStream.use { out ->
                    BackupManager.writeBackup(
                        context = context,
                        outputStream = out,
                        backupSettings = prefs.isAutoBackupItemSettings(),
                        backupCallingCards = prefs.isAutoBackupItemCallingCards(),
                        backupNotes = prefs.isAutoBackupItemNotes(),
                        backupContactGroups = prefs.isAutoBackupItemGroups(),
                        backupRecordings = prefs.isAutoBackupItemRecordings(),
                        backupContacts = prefs.isAutoBackupItemContacts(),
                        backupCallLogs = prefs.isAutoBackupItemCallLogs(),
                        backupSms = prefs.isAutoBackupItemSms(),
                        backupLiquidGlass = prefs.isAutoBackupItemSettings()
                    )
                }

                // Perform auto delete for older backups
                performAutoDelete(context)

                // Reschedule for next run
                scheduleAutoBackup(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Deletes backups based on age (time) and/or count limits configured by the user.
     */
    fun performAutoDelete(context: Context) {
        val prefs = PreferenceManager(context)
        val timeEnabled = prefs.isAutoDeleteBackupsTimeEnabled()
        val timeValue = prefs.getAutoDeleteBackupsTimeValue()
        val timeUnit = prefs.getAutoDeleteBackupsTimeUnit() // "hours", "days"

        val countEnabled = prefs.isAutoDeleteBackupsCountEnabled()
        val maxCount = prefs.getAutoDeleteBackupsCountValue()

        if (!timeEnabled && !countEnabled) return

        val now = System.currentTimeMillis()
        val timeThresholdMs = now - when (timeUnit.lowercase(Locale.ROOT)) {
            "hours" -> timeValue * 60 * 60 * 1000L
            else -> timeValue * 24 * 60 * 60 * 1000L
        }

        // Clean user's selected SAF directory if configured
        val folderUriStr = prefs.getAutoBackupFolderUri()
        if (!folderUriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(folderUriStr)
                val dir = DocumentFile.fromTreeUri(context, treeUri)
                if (dir != null && dir.exists()) {
                    val files = dir.listFiles().filter {
                        val name = it.name.orEmpty()
                        name.endsWith(".everdialer", ignoreCase = true)
                    }

                    if (timeEnabled) {
                        files.filter { it.lastModified() in 1 until timeThresholdMs }.forEach {
                            it.delete()
                        }
                    }

                    if (countEnabled) {
                        val remaining = dir.listFiles().filter {
                            it.name.orEmpty().endsWith(".everdialer", ignoreCase = true)
                        }.sortedByDescending { it.lastModified() }

                        if (remaining.size > maxCount) {
                            remaining.drop(maxCount).forEach { it.delete() }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Clean local app backup dir
        try {
            val localDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
            if (localDir.exists() && localDir.isDirectory) {
                val files = localDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".everdialer", ignoreCase = true)
                } ?: emptyArray()

                if (timeEnabled) {
                    files.filter { it.lastModified() in 1 until timeThresholdMs }.forEach {
                        it.delete()
                    }
                }

                if (countEnabled) {
                    val remaining = localDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".everdialer", ignoreCase = true)
                    }?.sortedByDescending { it.lastModified() } ?: emptyList()

                    if (remaining.size > maxCount) {
                        remaining.drop(maxCount).forEach { it.delete() }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

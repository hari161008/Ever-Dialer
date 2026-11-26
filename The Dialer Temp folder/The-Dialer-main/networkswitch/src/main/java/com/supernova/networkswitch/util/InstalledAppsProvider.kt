package com.supernova.networkswitch.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A single launchable app, ready to render in the "Switch Based On App Launched" picker. */
data class LaunchableAppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?
)

/**
 * Loads the list of launchable (has a Home-screen launcher entry) apps installed on the device,
 * for use by the app picker in "Switch Based On App Launched".
 */
object InstalledAppsProvider {

    suspend fun loadLaunchableApps(context: Context): List<LaunchableAppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = try {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        resolvedActivities
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val icon = try {
                        drawableToImageBitmap(resolveInfo.loadIcon(packageManager))
                    } catch (e: Exception) {
                        null
                    }
                    LaunchableAppInfo(packageName = packageName, appName = label, icon = icon)
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}

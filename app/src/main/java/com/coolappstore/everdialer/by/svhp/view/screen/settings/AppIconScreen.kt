package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

internal const val KEY_SELECTED_APP_ICON = "selected_app_icon"

data class AppIconEntry(
    val key: String,
    val label: String,
    val aliasName: String?,
    @DrawableRes val previewRes: Int
)

internal fun buildIcons(context: android.content.Context) = listOf(
    AppIconEntry("default",  "Default", "MainActivityDefaultIcon",      context.resources.getIdentifier("ic_launcher",              "mipmap", context.packageName)),
    AppIconEntry("phone",    "Phone",   "MainActivityPhoneIcon",        context.resources.getIdentifier("ic_launcher_phone",        "mipmap", context.packageName)),
    AppIconEntry("google",   "Google",  "MainActivityGoogleDialerIcon", context.resources.getIdentifier("ic_launcher_google_dialer","mipmap", context.packageName)),
    AppIconEntry("nothing",  "NOTHING", "MainActivityNothingIcon",      context.resources.getIdentifier("ic_launcher_nothing",      "mipmap", context.packageName))
)

internal fun applyIcon(context: android.content.Context, icons: List<AppIconEntry>, entry: AppIconEntry) {
    val pm  = context.packageManager
    val pkg = context.packageName

    val allAliasComponents = icons.mapNotNull { it.aliasName }.distinct()
        .map { ComponentName(pkg, "$pkg.$it") }

    val target = ComponentName(pkg, "$pkg.${entry.aliasName}")

    // Enable only the selected alias; disable all others
    allAliasComponents.forEach { component ->
        val state = if (component == target)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
    }
}

private fun loadBitmapFromRes(context: android.content.Context, @DrawableRes resId: Int): Bitmap? {
    if (resId == 0) return null
    return try {
        val drawable = context.resources.getDrawable(resId, context.theme)
        when (drawable) {
            is BitmapDrawable         -> drawable.bitmap
            is AdaptiveIconDrawable   -> {
                val bmp = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, 192, 192)
                drawable.draw(canvas)
                bmp
            }
            else -> drawable.toBitmap(192, 192)
        }
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppIconScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs   = koinInject<PreferenceManager>()

    val icons = remember { buildIcons(context) }

    var selectedKey by remember {
        mutableStateOf(prefs.getString(KEY_SELECTED_APP_ICON, "default") ?: "default")
    }

    val iconBitmaps = remember {
        icons.associate { entry ->
            entry.key to loadBitmapFromRes(context, entry.previewRes)?.asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Icon") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(icons) { _, entry ->
                val isSelected = selectedKey == entry.key
                val bitmap     = iconBitmaps[entry.key]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedKey = entry.key
                            prefs.setString(KEY_SELECTED_APP_ICON, entry.key)
                            applyIcon(context, icons, entry)
                        }
                        .padding(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = entry.label,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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
    AppIconEntry("custom_phone", "Vertical phone", "MainActivityCustomPhoneIcon", context.resources.getIdentifier("ic_launcher_custom_phone", "mipmap", context.packageName)),
    AppIconEntry("google",   "Google",  "MainActivityGoogleDialerIcon", context.resources.getIdentifier("ic_launcher_google_dialer","mipmap", context.packageName)),
    AppIconEntry("nothing",  "NOTHING", "MainActivityNothingIcon",      context.resources.getIdentifier("ic_launcher_nothing",      "mipmap", context.packageName)),
    AppIconEntry("lineageos", "LineageOS Dialer", "MainActivityLineageOSIcon", context.resources.getIdentifier("ic_launcher_lineageos", "mipmap", context.packageName))
)

// Curated App Name presets. "default" reuses the same alias as the Default app icon in
// combination with whatever icon is currently selected — see aliasNameFor() below, which
// computes a combined icon+name alias so changing one never resets the other.
internal fun buildAppNamePresets(context: android.content.Context) = listOf(
    AppIconEntry("default",          "Ever Dialer (Default)", null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_call",        "Call",                  null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_dial",        "Dial",                  null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_truephone",   "True Phone",             null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_phonedialer", "Phone Dialer",           null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_phone",       "Phone",                  null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)),
    AppIconEntry("name_dialer",      "Dialer",                 null, context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName))
)

private fun iconSuffix(iconKey: String): String = when (iconKey) {
    "default"      -> "Default"
    "phone"        -> "Phone"
    "custom_phone" -> "CustomPhone"
    "google"       -> "Google"
    "nothing"      -> "Nothing"
    "lineageos"    -> "LineageOS"
    else           -> "Default"
}

private fun plainIconAliasName(iconKey: String): String = when (iconKey) {
    "default"      -> "MainActivityDefaultIcon"
    "phone"        -> "MainActivityPhoneIcon"
    "custom_phone" -> "MainActivityCustomPhoneIcon"
    "google"       -> "MainActivityGoogleDialerIcon"
    "nothing"      -> "MainActivityNothingIcon"
    "lineageos"    -> "MainActivityLineageOSIcon"
    else           -> "MainActivityDefaultIcon"
}

private fun nameSuffix(nameKey: String): String? = when (nameKey) {
    "name_call"        -> "Call"
    "name_dial"        -> "Dial"
    "name_truephone"   -> "TruePhone"
    "name_phonedialer" -> "PhoneDialer"
    "name_phone"       -> "Phone"
    "name_dialer"      -> "Dialer"
    else               -> null // "default" — no separate name alias needed
}

/** Resolves the exact activity-alias short name for a given (icon, name) combination. */
internal fun aliasNameFor(iconKey: String, nameKey: String): String {
    val nSuffix = nameSuffix(nameKey) ?: return plainIconAliasName(iconKey)
    return "MainActivityName$nSuffix${iconSuffix(iconKey)}"
}

private val ALL_ICON_KEYS = listOf("default", "phone", "custom_phone", "google", "nothing", "lineageos")
private val ALL_NAME_KEYS = listOf("default", "name_call", "name_dial", "name_truephone", "name_phonedialer", "name_phone", "name_dialer")

private fun allLauncherAliasNames(): List<String> =
    ALL_ICON_KEYS.flatMap { icon -> ALL_NAME_KEYS.map { name -> aliasNameFor(icon, name) } }.distinct()

/**
 * Only one launcher activity-alias may ever be enabled at a time — enabling more than one
 * would show multiple separate launcher icons for this app. This disables every possible
 * icon×name combo alias, then enables only [targetAliasName].
 */
internal fun applyLauncherAlias(context: android.content.Context, targetAliasName: String) {
    val pm  = context.packageManager
    val pkg = context.packageName

    val allAliasComponents = allLauncherAliasNames().map { ComponentName(pkg, "$pkg.$it") }
    val target = ComponentName(pkg, "$pkg.$targetAliasName")

    allAliasComponents.forEach { component ->
        val state = if (component == target)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
    }
}

internal fun applyIcon(context: android.content.Context, prefs: PreferenceManager, entry: AppIconEntry) {
    val currentNameKey = prefs.getString(PreferenceManager.KEY_APP_NAME_PRESET, "default") ?: "default"
    applyLauncherAlias(context, aliasNameFor(entry.key, currentNameKey))
}

internal fun applyAppNamePreset(context: android.content.Context, prefs: PreferenceManager, entry: AppIconEntry) {
    val currentIconKey = prefs.getString(KEY_SELECTED_APP_ICON, "default") ?: "default"
    applyLauncherAlias(context, aliasNameFor(currentIconKey, entry.key))
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
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("App Icon") },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + navBarBottom),
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
                            applyIcon(context, prefs, entry)
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

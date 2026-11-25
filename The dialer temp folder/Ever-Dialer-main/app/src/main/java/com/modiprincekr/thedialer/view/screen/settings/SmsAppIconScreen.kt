package com.modiprincekr.thedialer.view.screen.settings

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
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private fun loadSmsBitmapFromRes(context: android.content.Context, resId: Int): Bitmap? {
    if (resId == 0) return null
    return try {
        val drawable = context.resources.getDrawable(resId, context.theme)
        when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is AdaptiveIconDrawable -> {
                val bmp = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, 192, 192)
                drawable.draw(canvas)
                bmp
            }
            else -> drawable.toBitmap(192, 192)
        }
    } catch (_: Exception) { null }
}

data class SmsAppIconEntry(
    val key: String,
    val label: String,
    @DrawableRes val previewRes: Int
)

internal fun buildSmsIcons(context: android.content.Context) = listOf(
    SmsAppIconEntry("default", "Default", context.resources.getIdentifier("ic_launcher_sms", "mipmap", context.packageName)),
    SmsAppIconEntry("google", "Google", context.resources.getIdentifier("ic_launcher_sms_google", "mipmap", context.packageName)),
    SmsAppIconEntry("nothing", "NOTHING", context.resources.getIdentifier("ic_launcher_sms_nothing", "mipmap", context.packageName)),
    SmsAppIconEntry("lineageos", "LineageOS", context.resources.getIdentifier("ic_launcher_sms_lineageos", "mipmap", context.packageName)),
    SmsAppIconEntry("windows_phone", "Windows Phone", context.resources.getIdentifier("ic_launcher_sms_windows_phone", "mipmap", context.packageName)),
    SmsAppIconEntry("bubble_blue", "Bubble Blue", context.resources.getIdentifier("ic_launcher_sms_bubble_blue", "mipmap", context.packageName)),
    SmsAppIconEntry("purple", "Purple Velvet", context.resources.getIdentifier("ic_launcher_sms_purple", "mipmap", context.packageName)),
    SmsAppIconEntry("amoled", "GrapheneOS", context.resources.getIdentifier("ic_launcher_sms_amoled", "mipmap", context.packageName)),
    SmsAppIconEntry("coral", "Coral Sunset", context.resources.getIdentifier("ic_launcher_sms_coral", "mipmap", context.packageName)),
    SmsAppIconEntry("neon_green", "Neon Mint", context.resources.getIdentifier("ic_launcher_sms_neon_green", "mipmap", context.packageName)),
    SmsAppIconEntry("yellow", "Yellow", context.resources.getIdentifier("ic_launcher_sms_yellow", "mipmap", context.packageName))
)

internal fun updateSmsLauncherAliasFull(
    context: android.content.Context,
    enabled: Boolean,
    appName: String,
    iconKey: String
) {
    val pm = context.packageManager
    val nameKeyMap = mapOf(
        "Ever SMS" to "EverSms",
        "Ever Messages" to "EverMessages",
        "SMS" to "Sms",
        "Messages" to "Messages",
        "Chats" to "Chats"
    )
    val appNames = listOf("Ever SMS", "Ever Messages", "SMS", "Messages", "Chats")
    val icons = listOf("default", "google", "nothing", "lineageos", "windows_phone", "bubble_blue", "purple", "amoled", "coral", "neon_green", "yellow")

    val nameSuffix = nameKeyMap[appName] ?: "EverSms"
    val iconSuffix = when (iconKey) {
        "google" -> "_Google"
        "nothing" -> "_Nothing"
        "lineageos" -> "_LineageOS"
        "windows_phone" -> "_WindowsPhone"
        "bubble_blue" -> "_BubbleBlue"
        "purple" -> "_Purple"
        "amoled" -> "_Amoled"
        "coral" -> "_Coral"
        "neon_green" -> "_NeonGreen"
        "yellow" -> "_Yellow"
        else -> ""
    }
    val targetAlias = "${context.packageName}.SmsLauncherAlias${nameSuffix}${iconSuffix}"

    // Disable legacy alias
    val oldAlias = "${context.packageName}.SmsLauncherAlias"
    try {
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, oldAlias),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (_: Exception) {}

    // Update all combinations
    for (name in appNames) {
        val curNameSuffix = nameKeyMap[name] ?: "EverSms"
        for (icon in icons) {
            val curIconSuffix = when (icon) {
                "google" -> "_Google"
                "nothing" -> "_Nothing"
                "lineageos" -> "_LineageOS"
                "windows_phone" -> "_WindowsPhone"
                "bubble_blue" -> "_BubbleBlue"
                "purple" -> "_Purple"
                "amoled" -> "_Amoled"
                "coral" -> "_Coral"
                "neon_green" -> "_NeonGreen"
                "yellow" -> "_Yellow"
                else -> ""
            }
            val aliasComponentStr = "${context.packageName}.SmsLauncherAlias${curNameSuffix}${curIconSuffix}"
            try {
                val shouldEnable = enabled && (aliasComponentStr == targetAlias)
                val newState = if (shouldEnable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                val current = pm.getComponentEnabledSetting(ComponentName(context.packageName, aliasComponentStr))
                if (current != newState) {
                    pm.setComponentEnabledSetting(
                        ComponentName(context.packageName, aliasComponentStr),
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SmsAppIconScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()

    val icons = remember { buildSmsIcons(context) }
    var selectedKey by remember(settingsVer) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_SMS_SELECTED_APP_ICON, "default") ?: "default")
    }

    val iconBitmaps = remember {
        icons.associate { entry ->
            entry.key to loadSmsBitmapFromRes(context, entry.previewRes)?.asImageBitmap()
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "SMS App Icon",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(icons) { _, entry ->
                val isSelected = selectedKey == entry.key
                val bitmap = iconBitmaps[entry.key]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedKey = entry.key
                            prefs.setString(PreferenceManager.KEY_SMS_SELECTED_APP_ICON, entry.key)
                            val smsLauncherIconEnabled = prefs.getBoolean(PreferenceManager.KEY_SMS_LAUNCHER_ICON_ENABLED, true)
                            val smsAppName = prefs.getString(PreferenceManager.KEY_SMS_LAUNCHER_APP_NAME, "Ever SMS") ?: "Ever SMS"
                            updateSmsLauncherAliasFull(context, smsLauncherIconEnabled, smsAppName, entry.key)
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

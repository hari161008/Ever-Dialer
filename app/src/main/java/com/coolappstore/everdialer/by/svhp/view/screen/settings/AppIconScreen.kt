package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private const val KEY_SELECTED_APP_ICON = "selected_app_icon"
private const val KEY_CUSTOM_PHONE_BG_COLOR = "custom_phone_bg_color"

data class AppIconEntry(
    val key: String,
    val label: String,
    val aliasName: String?,
    val isCustomColor: Boolean = false
)

private val APP_ICONS = listOf(
    AppIconEntry(key = "default",      label = "Default",       aliasName = null),
    AppIconEntry(key = "phone",        label = "Phone",         aliasName = "MainActivityPhoneIcon"),
    AppIconEntry(key = "custom_phone", label = "Custom Phone",  aliasName = "MainActivityPhoneIcon", isCustomColor = true)
)

private fun applyIcon(context: android.content.Context, entry: AppIconEntry) {
    val pm = context.packageManager
    val pkg = context.packageName
    val mainComponent = ComponentName(pkg, "$pkg.MainActivity")
    val allAliasComponents = APP_ICONS.mapNotNull { it.aliasName }.distinct()
        .map { ComponentName(pkg, "$pkg.$it") }

    if (entry.aliasName == null) {
        pm.setComponentEnabledSetting(mainComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        allAliasComponents.forEach { comp ->
            pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    } else {
        val targetComponent = ComponentName(pkg, "$pkg.${entry.aliasName}")
        pm.setComponentEnabledSetting(targetComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        allAliasComponents.filter { it != targetComponent }.forEach { comp ->
            pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        pm.setComponentEnabledSetting(mainComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}

private fun drawableToBitmap(context: android.content.Context, componentName: ComponentName): Bitmap? {
    return try {
        val drawable = context.packageManager.getActivityIcon(componentName)
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
    } catch (e: Exception) { null }
}

private fun parseHexColor(hex: String): Color? = try {
    val cleaned = hex.trimStart('#').take(6)
    if (cleaned.length == 6) Color(android.graphics.Color.parseColor("#$cleaned")) else null
} catch (e: Exception) { null }

private fun colorToHex(color: Color): String = String.format("%06X", color.toArgb() and 0xFFFFFF)

@Composable
private fun SliderRow(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(16.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
            modifier = Modifier.weight(1f))
        Text((value * 255).toInt().toString().padStart(3), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun ColorPickerDialog(initialColor: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    var red   by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue  by remember { mutableFloatStateOf(initialColor.blue) }
    val currentColor = Color(red, green, blue)
    var hexInput by remember { mutableStateOf(colorToHex(currentColor)) }
    var hexError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(red, green, blue) {
        hexInput = colorToHex(Color(red, green, blue))
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Background Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(currentColor))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val v = raw.trimStart('#').take(6).uppercase()
                        hexInput = v
                        val parsed = parseHexColor(v)
                        if (parsed != null) { red = parsed.red; green = parsed.green; blue = parsed.blue; hexError = false }
                        else hexError = v.isNotEmpty()
                    },
                    label = { Text("Hex") },
                    prefix = { Text("#") },
                    isError = hexError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
                SliderRow("R", red,   Color.Red)              { red   = it }
                SliderRow("G", green, Color(0xFF00C853))      { green = it }
                SliderRow("B", blue,  Color(0xFF2196F3))      { blue  = it }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(currentColor) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppIconScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val defaultIconGreen = Color(0xFF6C7C4F)

    var selectedKey by remember {
        mutableStateOf(prefs.getString(KEY_SELECTED_APP_ICON, "default") ?: "default")
    }

    var customBgColor by remember {
        val saved = prefs.getString(KEY_CUSTOM_PHONE_BG_COLOR, null)
        mutableStateOf(
            if (saved != null) Color(saved.toLongOrNull()?.toInt() ?: defaultIconGreen.toArgb())
            else defaultIconGreen
        )
    }

    var showColorPicker by remember { mutableStateOf(false) }

    val iconBitmaps = remember {
        APP_ICONS.associate { entry ->
            val componentName = if (entry.aliasName == null)
                ComponentName(context.packageName, "${context.packageName}.MainActivity")
            else
                ComponentName(context.packageName, "${context.packageName}.${entry.aliasName}")
            entry.key to drawableToBitmap(context, componentName)?.asImageBitmap()
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = customBgColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { picked ->
                customBgColor = picked
                prefs.setString(KEY_CUSTOM_PHONE_BG_COLOR, picked.toArgb().toLong().toString())
                showColorPicker = false
            }
        )
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
            itemsIndexed(APP_ICONS) { _, entry ->
                val isSelected = selectedKey == entry.key
                val bitmap = iconBitmaps[entry.key]
                val isPhoneStyle = entry.key == "phone" || entry.key == "custom_phone"
                // Phone icon slightly bigger: 40dp vs 32dp
                val callIconSize = if (isPhoneStyle) 40.dp else 32.dp
                val iconBoxSize  = if (isPhoneStyle) 72.dp else 64.dp
                val bitmapSize   = if (isPhoneStyle) 76.dp else 72.dp
                val bgColor = if (entry.isCustomColor) customBgColor else defaultIconGreen

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedKey = entry.key
                            prefs.setString(KEY_SELECTED_APP_ICON, entry.key)
                            applyIcon(context, entry)
                            if (entry.isCustomColor) showColorPicker = true
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
                        if (bitmap != null && !entry.isCustomColor) {
                            Image(bitmap = bitmap, contentDescription = entry.label,
                                modifier = Modifier.size(bitmapSize).clip(RoundedCornerShape(16.dp)))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(iconBoxSize)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(callIconSize)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

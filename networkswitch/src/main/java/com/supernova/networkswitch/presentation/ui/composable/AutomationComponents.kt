package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supernova.networkswitch.domain.model.AppAutomationMode
import com.supernova.networkswitch.domain.model.AutomationMode
import com.supernova.networkswitch.presentation.ui.components.NetworkSwitchCardShape
import com.supernova.networkswitch.util.LaunchableAppInfo

/**
 * A pair of circular radio buttons for choosing between "Mode A" and "Mode B", used by every
 * automation rule below.
 */
@Composable
fun ModeABSelector(
    label: String,
    selected: AutomationMode,
    onSelected: (AutomationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val modeAInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = modeAInteractionSource,
                        indication = null,
                        onClick = { onSelected(AutomationMode.MODE_A) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == AutomationMode.MODE_A,
                    onClick = { onSelected(AutomationMode.MODE_A) }
                )
                Text(text = "Mode A", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            val modeBInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = modeBInteractionSource,
                        indication = null,
                        onClick = { onSelected(AutomationMode.MODE_B) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == AutomationMode.MODE_B,
                    onClick = { onSelected(AutomationMode.MODE_B) }
                )
                Text(text = "Mode B", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Section header row shared by the three automation cards below. */
@Composable
private fun AutomationCardHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

/**
 * "Switch Based On Screen State" — off by default. When on, shows two independent Mode A / Mode B
 * pickers: one for when the screen is off, one for when it's on.
 */
@Composable
fun ScreenStateAutomationCard(
    enabled: Boolean,
    screenOffMode: AutomationMode,
    screenOnMode: AutomationMode,
    onEnabledChange: (Boolean) -> Unit,
    onScreenOffModeChange: (AutomationMode) -> Unit,
    onScreenOnModeChange: (AutomationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NetworkSwitchCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            AutomationCardHeader(
                icon = Icons.Filled.PhoneAndroid,
                title = "Switch Based On Screen State",
                subtitle = "Automatically switch modes when the screen turns on or off",
                enabled = enabled,
                onEnabledChange = onEnabledChange
            )

            if (enabled) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ModeABSelector(
                            label = "When screen is off",
                            selected = screenOffMode,
                            onSelected = onScreenOffModeChange
                        )
                        ModeABSelector(
                            label = "When screen is on",
                            selected = screenOnMode,
                            onSelected = onScreenOnModeChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Switch based on Battery Saver state" — off by default. When on, shows a single Mode A / Mode B
 * picker applied whenever Battery Saver is turned on.
 */
@Composable
fun BatterySaverAutomationCard(
    enabled: Boolean,
    mode: AutomationMode,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AutomationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NetworkSwitchCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            AutomationCardHeader(
                icon = Icons.Filled.BatterySaver,
                title = "Switch based on Battery Saver state",
                subtitle = "Automatically switch mode whenever Battery Saver turns on",
                enabled = enabled,
                onEnabledChange = onEnabledChange
            )

            if (enabled) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ModeABSelector(
                            label = "When Battery Saver is on",
                            selected = mode,
                            onSelected = onModeChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Switch Based On App Launched" — off by default. When on, shows an app picker: for every
 * launchable app, a None / Mode A / Mode B segmented selector. Requires the special "Usage
 * Access" permission to detect which app is currently open.
 */
@Composable
fun AppLaunchAutomationCard(
    enabled: Boolean,
    hasUsageAccess: Boolean,
    isLoadingApps: Boolean,
    apps: List<LaunchableAppInfo>,
    appModes: Map<String, AppAutomationMode>,
    onEnabledChange: (Boolean) -> Unit,
    onGrantUsageAccessClick: () -> Unit,
    onAppModeChange: (packageName: String, mode: AppAutomationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuredCount = remember(appModes) { appModes.count { it.value != AppAutomationMode.NONE } }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NetworkSwitchCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            AutomationCardHeader(
                icon = Icons.Filled.Apps,
                title = "Switch Based On App Launched",
                subtitle = "Automatically switch mode when a chosen app is opened",
                enabled = enabled,
                onEnabledChange = onEnabledChange
            )

            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))

                if (!hasUsageAccess) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Usage Access required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This app needs Usage Access permission to detect which app is currently open.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(onClick = onGrantUsageAccessClick, shape = RoundedCornerShape(14.dp)) {
                                Text("Grant Usage Access")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoadingApps) { showPicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select Apps",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLoadingApps) "Loading apps…"
                                       else if (configuredCount == 0) "No apps configured yet"
                                       else "$configuredCount app${if (configuredCount == 1) "" else "s"} configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isLoadingApps) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Select apps")
                        }
                    }
                }
            }
        }
    }

    if (showPicker && enabled && !isLoadingApps) {
        AppPickerDialog(
            apps = apps,
            appModes = appModes,
            onAppModeChange = onAppModeChange,
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Floating popup (separate from the settings card) that lists every launchable app with a search
 * bar on top, so the user can quickly find an app and assign it a Mode A / Mode B / None rule.
 */
@Composable
private fun AppPickerDialog(
    apps: List<LaunchableAppInfo>,
    appModes: Map<String, AppAutomationMode>,
    onAppModeChange: (packageName: String, mode: AppAutomationMode) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No apps match \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppModeRow(
                                app = app,
                                mode = appModes[app.packageName] ?: AppAutomationMode.NONE,
                                onModeChange = { newMode -> onAppModeChange(app.packageName, newMode) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppModeRow(
    app: LaunchableAppInfo,
    mode: AppAutomationMode,
    onModeChange: (AppAutomationMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (app.icon != null) {
                Image(
                    painter = BitmapPainter(app.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                AppAutomationMode.NONE to "None",
                AppAutomationMode.MODE_A to "Mode A",
                AppAutomationMode.MODE_B to "Mode B"
            )
            options.forEachIndexed { index, (optionMode, optionLabel) ->
                SegmentedButton(
                    selected = mode == optionMode,
                    onClick = { onModeChange(optionMode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(optionLabel, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

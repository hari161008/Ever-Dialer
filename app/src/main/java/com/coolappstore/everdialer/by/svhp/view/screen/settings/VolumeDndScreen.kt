package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolappstore.everdialer.by.svhp.controller.VolumeDndAccessibilityService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun VolumeDndScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var volumeDndEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, false)) }
    var volumeDndSequence by remember {
        mutableStateOf(
            prefs.getString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE)
                ?: PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
        )
    }
    var volumeDndLockScreenOnly by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, false))
    }
    var volumeDndTimeoutMs by remember {
        mutableStateOf(
            prefs.getInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, PreferenceManager.DEFAULT_VOLUME_DND_TIMEOUT_MS).toString()
        )
    }
    var showPermissionCardDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAccessibilityGranted by remember { mutableStateOf(VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)) }
    var isDndGranted by remember { mutableStateOf(VolumeDndAccessibilityService.isDndAccessGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                isDndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showPermissionCardDialog && (!isAccessibilityGranted || !isDndGranted)) {
        AlertDialog(
            onDismissRequest = { showPermissionCardDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = { Text("Permissions Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "To detect volume button sequences and toggle Do Not Disturb (DND), please grant the following permissions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isAccessibilityGranted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.Accessibility, null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Accessibility Service", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Required to capture volume key combinations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (!isDndGranted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.DoNotDisturbOn, null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Do Not Disturb Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Required to toggle system DND state", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { VolumeDndAccessibilityService.openDndAccessSettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPermissionCardDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Volume DND",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSearchEntryPoint(navigator = navigator)

            RivoAnimatedSection(delayMs = 0L) {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Volume DND",
                        supporting = "Toggle Do Not Disturb (DND) using a volume button sequence",
                        leadingIcon = Icons.Outlined.DoNotDisturbOn,
                        iconContainerColor = Color(0xFF7C4DFF),
                        checked = volumeDndEnabled,
                        modifier = Modifier.settingsSearchHighlight("volume_dnd", highlightedKey) { highlightedKey = null },
                        onCheckedChange = { newValue ->
                            volumeDndEnabled = newValue
                            prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, newValue)
                            if (newValue) {
                                isAccessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                                isDndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
                                if (!isAccessibilityGranted || !isDndGranted) {
                                    showPermissionCardDialog = true
                                }
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Missing permissions warning banner if any permission is missing
                        if (!isAccessibilityGranted || !isDndGranted) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            "Permissions Required",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        "Volume DND requires Accessibility Service to capture volume button presses and Do Not Disturb permission to toggle DND.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isAccessibilityGranted) {
                                            Button(
                                                onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                            ) {
                                                Text("Accessibility", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (!isDndGranted) {
                                            Button(
                                                onClick = { VolumeDndAccessibilityService.openDndAccessSettings(context) },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                            ) {
                                                Text("DND Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        CardDivider()

                        // Volume Combination Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Volume button combination",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (volumeDndSequence != PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE) {
                                        TextButton(
                                            onClick = {
                                                volumeDndSequence = PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
                                                prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Reset (UUDD)", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Text(
                                    text = "Click volume buttons in this order with under ${volumeDndTimeoutMs}ms delay to trigger DND:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Dynamic Sequence Badges
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        horizontalArrangement = if (volumeDndSequence.isEmpty()) Arrangement.Center else Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (volumeDndSequence.isEmpty()) {
                                            Text(
                                                "No keys added (tap below to add)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            volumeDndSequence.forEachIndexed { _, char ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (char == 'U') MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    contentColor = if (char == 'U') MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                                    shadowElevation = 1.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (char == 'U') Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = if (char == 'U') "UP" else "DOWN",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Horizontal action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (volumeDndSequence.length < 16) {
                                                val updated = volumeDndSequence + "U"
                                                volumeDndSequence = updated
                                                prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Vol Up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            if (volumeDndSequence.length < 16) {
                                                val updated = volumeDndSequence + "D"
                                                volumeDndSequence = updated
                                                prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Vol Down", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (volumeDndSequence.isNotEmpty()) {
                                                val updated = volumeDndSequence.dropLast(1)
                                                volumeDndSequence = updated
                                                prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                        enabled = volumeDndSequence.isNotEmpty()
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.Backspace, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Delay Timeout Settings Card
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = "Trigger Delay Timeout",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(
                                            text = "${volumeDndTimeoutMs.ifEmpty { "600" }} ms",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Maximum delay in milliseconds between button clicks. Smaller numbers require faster clicks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val currentTimeoutFloat = volumeDndTimeoutMs.toFloatOrNull()?.coerceIn(100f, 3000f) ?: 600f
                                Slider(
                                    value = currentTimeoutFloat,
                                    onValueChange = { newMs ->
                                        val rounded = (newMs / 50).toInt() * 50
                                        volumeDndTimeoutMs = rounded.toString()
                                        prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, rounded)
                                    },
                                    valueRange = 100f..3000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = volumeDndTimeoutMs,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(5)
                                        volumeDndTimeoutMs = digits
                                        val num = digits.toIntOrNull()
                                        if (num != null && num in 100..5000) {
                                            prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, num)
                                        }
                                    },
                                    label = { Text("Delay (milliseconds)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    trailingIcon = {
                                        Text("ms", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf(
                                        300 to "300ms (Fastest)",
                                        400 to "400ms (Fast)",
                                        600 to "600ms (Default)",
                                        800 to "800ms (Normal)",
                                        1200 to "1.2s (Relaxed)"
                                    )
                                    val currentMs = volumeDndTimeoutMs.toIntOrNull()
                                    presets.forEach { (presetMs, label) ->
                                        val isSelected = currentMs == presetMs
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                volumeDndTimeoutMs = presetMs.toString()
                                                prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, presetMs)
                                            },
                                            label = {
                                                Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                            border = null
                                        )
                                    }
                                }
                            }
                        }

                        // Lock screen only card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        val newVal = !volumeDndLockScreenOnly
                                        volumeDndLockScreenOnly = newVal
                                        prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, newVal)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.ScreenLockPortrait,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Enable only in lock screen & screen off",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Only triggers when screen is off or on lock screen; ignored on home screen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = volumeDndLockScreenOnly,
                                    onCheckedChange = { newVal ->
                                        volumeDndLockScreenOnly = newVal
                                        prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, newVal)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

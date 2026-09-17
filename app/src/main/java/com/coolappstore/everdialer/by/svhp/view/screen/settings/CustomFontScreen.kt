package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CustomFontScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs: PreferenceManager = koinInject()

    val settingsVersion by prefs.settingsChanged.collectAsState()

    // Read current font path / ID from prefs
    val savedFontValue = remember(settingsVersion) {
        prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, AppFontConstants.FONT_ID_SYSTEM)
            ?: AppFontConstants.FONT_ID_SYSTEM
    }

    var selectedFontId by remember(savedFontValue) {
        mutableStateOf(if (savedFontValue.isEmpty()) AppFontConstants.FONT_ID_SYSTEM else savedFontValue)
    }

    var displayScale by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DISPLAY_SCALE, 1.0f))
    }

    var fontSizeScale by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f))
    }

    // Selected font family for live preview
    val activePreviewFamily: FontFamily = remember(selectedFontId) {
        AppFontHelper.resolveFontFamily(selectedFontId)
    }

    // Info dialog state
    var inspectingFontItem by remember { mutableStateOf<AppFontItem?>(null) }

    // External font file picker
    val fontPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fontFile = File(context.filesDir, "custom_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        fontFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, fontFile.absolutePath)
                    selectedFontId = fontFile.absolutePath
                    Toast.makeText(context, "Custom font loaded", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load font file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val customFile = remember(settingsVersion) {
        val file = File(context.filesDir, "custom_font.ttf")
        if (file.exists()) file else null
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Custom Font",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Live Preview Container ───────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header row with chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.FontDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = AppFontHelper.getFontDisplayName(selectedFontId),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Live Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Preview Heading
                        Text(
                            text = "The quick brown fox jumps over the lazy dog.",
                            fontFamily = activePreviewFamily,
                            fontSize = (19 * fontSizeScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = (26 * fontSizeScale).sp
                        )

                        // Preview Body Paragraph
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Sphinx of black quartz, judge my vow. Ever Dialer pairs Material You expressive typography with dynamic color theming across all screens.",
                                    fontFamily = activePreviewFamily,
                                    fontSize = (14 * fontSizeScale).sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = (21 * fontSizeScale).sp
                                )
                                Text(
                                    text = "+1 (555) 019-2834 • 0123456789",
                                    fontFamily = activePreviewFamily,
                                    fontSize = (15 * fontSizeScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // ── Display & Font Scaling Sliders ─────────────────────────
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Display & Font Size",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        displayScale = 1.0f
                                        fontSizeScale = 1.0f
                                        prefs.setFloat(PreferenceManager.KEY_DISPLAY_SCALE, 1.0f)
                                        prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
                                    },
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Reset scale",
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            // Display Scaling Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Display Scaling",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${(displayScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = displayScale,
                                onValueChange = { displayScale = it },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_DISPLAY_SCALE, displayScale)
                                },
                                valueRange = 0.70f..1.40f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            // Font Size Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Font Size",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${(fontSizeScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = fontSizeScale,
                                onValueChange = { fontSizeScale = it },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, fontSizeScale)
                                },
                                valueRange = 0.70f..1.40f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ── Custom Font File (.ttf) Section (Placed Above Fonts) ───────────
            RivoAnimatedSection(delayMs = 40L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoSectionHeader(title = "Custom Font File")

                    val isCustomFileActive = customFile != null && selectedFontId == customFile.absolutePath

                    RivoExpressiveCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isCustomFileActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { fontPickerLauncher.launch("font/ttf") },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "Pick .ttf file",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (customFile != null) {
                                            selectedFontId = customFile.absolutePath
                                            prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, customFile.absolutePath)
                                        } else {
                                            fontPickerLauncher.launch("font/ttf")
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (customFile != null) "Imported Font (${customFile.name})" else "Pick external .ttf file",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCustomFileActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCustomFileActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (customFile != null) "Tap to activate or replace" else "Load any TrueType font from device storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (customFile != null) {
                                IconButton(
                                    onClick = {
                                        customFile.delete()
                                        selectedFontId = AppFontConstants.FONT_ID_SYSTEM
                                        prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete imported font",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                RadioButton(
                                    selected = isCustomFileActive,
                                    onClick = {
                                        selectedFontId = customFile.absolutePath
                                        prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, customFile.absolutePath)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Built-in Fonts Section ──────────────────────────────────────────
            RivoAnimatedSection(delayMs = 80L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoSectionHeader(title = "Fonts")

                    RivoExpressiveCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BuiltInAppFonts.forEachIndexed { index, fontItem ->
                                val isSelected = (selectedFontId == fontItem.id) ||
                                        (fontItem.id == AppFontConstants.FONT_ID_SYSTEM &&
                                                (selectedFontId.isEmpty() || selectedFontId == "system" || (selectedFontId.startsWith("/") && !File(selectedFontId).exists())))

                                val targetBg = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    Color.Transparent
                                }
                                val animatedBg by animateColorAsState(targetBg, label = "fontItemBg")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(animatedBg)
                                        .clickable {
                                            selectedFontId = fontItem.id
                                            val pathToSave = if (fontItem.id == AppFontConstants.FONT_ID_SYSTEM) {
                                                null
                                            } else {
                                                fontItem.id
                                            }
                                            prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, pathToSave)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Info button on the left of each font
                                    IconButton(
                                        onClick = { inspectingFontItem = fontItem },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "About & License for ${fontItem.name}",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Font title and designer
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fontItem.name,
                                            fontFamily = fontItem.fontFamily,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${fontItem.styleTag} • ${fontItem.designer.split(",").firstOrNull() ?: fontItem.designer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // RadioButton
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFontId = fontItem.id
                                            val pathToSave = if (fontItem.id == AppFontConstants.FONT_ID_SYSTEM) {
                                                null
                                            } else {
                                                fontItem.id
                                            }
                                            prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, pathToSave)
                                        }
                                    )
                                }

                                if (index < BuiltInAppFonts.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Font Info & License Dialog ───────────────────────────────────────────
    inspectingFontItem?.let { fontItem ->
        FontInfoLicenseDialog(
            fontItem = fontItem,
            onDismiss = { inspectingFontItem = null }
        )
    }
}

@Composable
fun FontInfoLicenseDialog(
    fontItem: AppFontItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showFullLicense by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = fontItem.name,
                        fontFamily = fontItem.fontFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = fontItem.styleTag,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Designer info
                Column {
                    Text(
                        text = "DESIGNER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = fontItem.designer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // About section
                Column {
                    Text(
                        text = "ABOUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = fontItem.about,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                // Upstream link
                if (fontItem.upstreamUrl.isNotEmpty() && fontItem.upstreamUrl.startsWith("http")) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fontItem.upstreamUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View on Google Fonts / Project")
                    }
                }

                // License Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LICENSE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showFullLicense = !showFullLicense },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showFullLicense) "Hide text" else "Show license text",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Text(
                        text = fontItem.copyrightNotice,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fontItem.licenseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AnimatedVisibility(visible = showFullLicense) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Font License", fontItem.licenseText))
                                            Toast.makeText(context, "License copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(
                                    text = fontItem.licenseText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Close")
            }
        }
    )
}

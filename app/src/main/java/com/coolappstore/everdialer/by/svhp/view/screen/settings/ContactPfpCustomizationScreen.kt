package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.BackgroundMediaManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactPfpCustomizationScreen(
    navigator: DestinationsNavigator,
    isIncoming: Boolean = true,
    contactKey: String? = null,
    contactDisplayName: String? = null,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    LaunchedEffect(Unit) {
        BackgroundMediaManager.autoCleanInBackground(context, prefs)
    }

    val target = if (isIncoming) CustomBackgroundTarget.INCOMING else CustomBackgroundTarget.ONGOING
    val basePrefix = target.prefix
    val prefix = if (!contactKey.isNullOrEmpty()) "contact_${contactKey}_$basePrefix" else basePrefix

    val settingsVersion by prefs.settingsChanged.collectAsState()

    var pfpType by remember(settingsVersion) {
        mutableStateOf(prefs.getString("${prefix}_custom_pfp_type", "none") ?: "none")
    }
    var pfpPath by remember(settingsVersion) {
        mutableStateOf(prefs.getString("${prefix}_custom_pfp_path", "") ?: "")
    }
    var pfpZoom by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_zoom", 1f))
    }
    var pfpPanX by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_pan_x", 0f))
    }
    var pfpPanY by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_pan_y", 0f))
    }
    var pfpDim by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_dim", 0f))
    }
    var pfpBlur by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_blur", 0f))
    }
    var pfpVideoSpeed by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_video_speed", 1.0f))
    }

    var overrideExisting by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean("${prefix}_custom_pfp_override_existing", false))
    }
    var showForNoPfp by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean("${prefix}_custom_pfp_show_for_no_pfp", true))
    }

    var showOptionsPopup by remember { mutableStateOf(false) }
    var editorMediaState by remember { mutableStateOf<Triple<File, Boolean, String>?>(null) }

    val pfpFile = remember(pfpPath) { if (pfpPath.isNotEmpty()) File(pfpPath) else null }
    val hasCustomPfp = (pfpType == "wallpaper" || pfpType == "picture" || pfpType == "video") && pfpFile != null && pfpFile.exists()

    val pfpLabel = when {
        pfpType == "wallpaper" -> "Device Wallpaper"
        pfpType == "picture"   -> "Custom Picture"
        pfpType == "video"     -> "Custom Video"
        else                   -> "None (Default Face Icon)"
    }

    // Editor Dialog
    editorMediaState?.let { (file, isVideo, type) ->
        CustomBackgroundEditorDialog(
            target = target,
            mediaFile = file,
            isVideo = isVideo,
            bgType = type,
            prefixOverride = prefix,
            isPfpEditor = true,
            initialZoom = pfpZoom,
            initialPanX = pfpPanX,
            initialPanY = pfpPanY,
            initialDim = pfpDim,
            initialBlur = pfpBlur,
            initialVideoSpeed = pfpVideoSpeed,
            onDismiss = {
                editorMediaState?.let { (f, _, _) ->
                    scope.launch(Dispatchers.IO) {
                        BackgroundMediaManager.cleanupFileIfInCache(context, f)
                    }
                }
                editorMediaState = null
            },
            onSaveSuccess = {
                pfpType = type
                pfpPath = prefs.getString("${prefix}_custom_pfp_path", "") ?: ""
                pfpZoom = prefs.getFloat("${prefix}_custom_pfp_zoom", 1f)
                pfpPanX = prefs.getFloat("${prefix}_custom_pfp_pan_x", 0f)
                pfpPanY = prefs.getFloat("${prefix}_custom_pfp_pan_y", 0f)
                pfpDim = prefs.getFloat("${prefix}_custom_pfp_dim", 0f)
                pfpBlur = prefs.getFloat("${prefix}_custom_pfp_blur", 0f)
                pfpVideoSpeed = prefs.getFloat("${prefix}_custom_pfp_video_speed", 1.0f)
                editorMediaState = null
                BackgroundMediaManager.autoCleanInBackground(context, prefs)
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Contact PFP Customisation", fontWeight = FontWeight.Bold)
                        Text(
                            if (contactDisplayName != null) "For $contactDisplayName"
                            else if (isIncoming) "Incoming Call UI" else "Ongoing Call UI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    SettingsBackIconButton(onClick = { navigator.navigateUp() })
                },
                actions = {
                    if (hasCustomPfp) {
                        IconButton(
                            onClick = {
                                pfpType = "none"
                                pfpPath = ""
                                prefs.setString("${prefix}_custom_pfp_type", "none")
                                prefs.setString("${prefix}_custom_pfp_path", "")
                                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                                Toast.makeText(context, "Contact PFP reset to default", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset Contact PFP",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSearchEntryPoint(navigator = navigator)

            // ── Live Preview Container ───────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Context pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = if (isIncoming) "Incoming Call Preview" else "Ongoing Call Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Circular Avatar Box Preview
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                    .clickable { showOptionsPopup = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCustomPfp && pfpFile != null) {
                                    if (pfpType == "video") {
                                        LoopingVideoPlayer(
                                            videoFile = pfpFile,
                                            videoSpeed = pfpVideoSpeed,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = pfpZoom
                                                    scaleY = pfpZoom
                                                    translationX = pfpPanX * 0.35f
                                                    translationY = pfpPanY * 0.35f
                                                }
                                                .then(if (pfpBlur > 0f) Modifier.blur(pfpBlur.dp) else Modifier)
                                        )
                                    } else {
                                        AsyncImage(
                                            model = pfpFile,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = pfpZoom
                                                    scaleY = pfpZoom
                                                    translationX = pfpPanX * 0.35f
                                                    translationY = pfpPanY * 0.35f
                                                }
                                                .then(if (pfpBlur > 0f) Modifier.blur(pfpBlur.dp) else Modifier)
                                        )
                                    }
                                    if (pfpDim > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = pfpDim))
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = contactDisplayName ?: "Jane Doe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "+1 (555) 019-2834",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = if (hasCustomPfp) "Tap avatar or options below to modify" else "Showing default face vector icon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Contact PFP Container & Checkboxes ──────────────────────────────
            RivoAnimatedSection(delayMs = 25L) {
                Column {
                    Text(
                        "Contact PFP Customisation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )

                    RivoExpressiveCard {
                        // Clickable container for Contact PFP
                        RivoListItem(
                            headline = "Contact PFP",
                            supporting = "Currently: $pfpLabel",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF00BCD4),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contact_pfp_picker", highlightedKey) { highlightedKey = null },
                            onClick = { showOptionsPopup = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Checkbox 1: Override if contact has a PFP (default: unchecked)
                        RivoCheckboxListItem(
                            headline = "Override if the contact has a PFP",
                            supporting = "Show custom contact PFP even when the contact has their own photo",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF9C27B0),
                            checked = overrideExisting,
                            onCheckedChange = {
                                overrideExisting = it
                                prefs.setBoolean("${prefix}_custom_pfp_override_existing", it)
                            },
                            modifier = Modifier.settingsSearchHighlight("contact_pfp_override_existing", highlightedKey) { highlightedKey = null }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Checkbox 2: If contact/number has no PFP then show custom contact PFP (default: checked)
                        RivoCheckboxListItem(
                            headline = "If the number or contact has no PFP then show the custom contact PFP",
                            supporting = "Apply custom contact PFP for callers and numbers without a profile photo",
                            leadingIcon = Icons.Outlined.Face,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = showForNoPfp,
                            onCheckedChange = {
                                showForNoPfp = it
                                prefs.setBoolean("${prefix}_custom_pfp_show_for_no_pfp", it)
                            },
                            modifier = Modifier.settingsSearchHighlight("contact_pfp_show_for_no_pfp", highlightedKey) { highlightedKey = null }
                        )
                    }
                }
            }

            // ── Information & Explanation Box ────────────────────────────────────
            RivoAnimatedSection(delayMs = 50L) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "When Contact PFP is set to None, contacts and numbers without a profile picture will show the default face vector icon.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showOptionsPopup) {
        CustomBackgroundOptionsPopup(
            target = target,
            currentType = pfpType,
            dialogTitle = "Choose Contact PFP",
            dialogSubtitle = if (isIncoming) "Incoming Call Screen" else "Ongoing Call Screen",
            noneSubtitle = "Default face vector icon",
            onDismiss = { showOptionsPopup = false },
            onSelectNone = {
                pfpType = "none"
                pfpPath = ""
                prefs.setString("${prefix}_custom_pfp_type", "none")
                prefs.setString("${prefix}_custom_pfp_path", "")
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                Toast.makeText(context, "Default face icon applied", Toast.LENGTH_SHORT).show()
            },
            onOpenEditor = { file, isVideo, type ->
                editorMediaState = Triple(file, isVideo, type)
                showOptionsPopup = false
            }
        )
    }
}

package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.BackgroundMediaManager
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.WallpaperExportHelper
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomBackgroundPickerScreen(
    navigator: DestinationsNavigator,
    isIncoming: Boolean = true,
    contactKey: String? = null,
    contactDisplayName: String? = null
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        BackgroundMediaManager.autoCleanInBackground(context, prefs)
    }

    val target = if (isIncoming) CustomBackgroundTarget.INCOMING else CustomBackgroundTarget.ONGOING
    val basePrefix = target.prefix
    val prefix = if (!contactKey.isNullOrEmpty()) "contact_${contactKey}_$basePrefix" else basePrefix

    // Track reactive settings
    val settingsVersion by prefs.settingsChanged.collectAsState()

    var bgType by remember(settingsVersion) {
        mutableStateOf(prefs.getString("${prefix}_bg_type", "none") ?: "none")
    }
    var bgPath by remember(settingsVersion) {
        mutableStateOf(prefs.getString("${prefix}_bg_path", "") ?: "")
    }
    var bgZoom by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_zoom", 1f))
    }
    var bgPanX by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_pan_x", 0f))
    }
    var bgPanY by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_pan_y", 0f))
    }
    var bgDim by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_dim", 0f))
    }
    var bgBlur by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_blur", 0f))
    }
    var bgVideoSpeed by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_bg_video_speed", 1.0f))
    }
    val showContactPfp = remember(settingsVersion) {
        if (isIncoming) prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, true)
        else prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, true)
    }
    val showPhoneNumber = remember(settingsVersion) {
        if (isIncoming) prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_PHONE_NUMBER, true)
        else prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_PHONE_NUMBER, true)
    }
    val isDualSim = remember { prefs.getActiveSimCount() >= 2 }

    val defaultPfpType = if (isIncoming) prefs.getString(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_TYPE, "none") else prefs.getString(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_TYPE, "none")
    val defaultPfpPath = if (isIncoming) prefs.getString(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PATH, "") else prefs.getString(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PATH, "")
    val defaultPfpZoom = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_ZOOM, 1f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_ZOOM, 1f)
    val defaultPfpPanX = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PAN_X, 0f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PAN_X, 0f)
    val defaultPfpPanY = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PAN_Y, 0f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PAN_Y, 0f)
    val defaultPfpDim = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_DIM, 0f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_DIM, 0f)
    val defaultPfpBlur = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_BLUR, 0f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_BLUR, 0f)
    val defaultPfpVideoSpeed = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_VIDEO_SPEED, 1.0f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_VIDEO_SPEED, 1.0f)
    val defaultPfpOverride = if (isIncoming) prefs.getBoolean(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_OVERRIDE_EXISTING, true) else prefs.getBoolean(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_OVERRIDE_EXISTING, true)
    val defaultPfpShowForNoPfp = if (isIncoming) prefs.getBoolean(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_SHOW_FOR_NO_PFP, true) else prefs.getBoolean(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_SHOW_FOR_NO_PFP, true)
    val defaultPfpSize = if (isIncoming) prefs.getFloat(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_SIZE, 0.5f) else prefs.getFloat(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_SIZE, 0.5f)
    val defaultPfpShape = if (isIncoming) prefs.getString(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_SHAPE, "circle") else prefs.getString(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_SHAPE, "circle")

    val isContactSpecific = !contactKey.isNullOrEmpty()
    val contactSpecificPfpType = if (isContactSpecific) prefs.getString("${prefix}_custom_pfp_type", null) else null
    val hasPerContactPfpConfigured = !contactSpecificPfpType.isNullOrEmpty() && contactSpecificPfpType != "none"

    val pfpType = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) contactSpecificPfpType!! else (defaultPfpType ?: "none")
    }
    val pfpPath = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getString("${prefix}_custom_pfp_path", "") ?: "" else (defaultPfpPath ?: "")
    }
    val pfpZoom = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_zoom", 1f) else (defaultPfpZoom ?: 1f)
    }
    val pfpPanX = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_pan_x", 0f) else (defaultPfpPanX ?: 0f)
    }
    val pfpPanY = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_pan_y", 0f) else (defaultPfpPanY ?: 0f)
    }
    val pfpDim = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_dim", 0f) else (defaultPfpDim ?: 0f)
    }
    val pfpBlur = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_blur", 0f) else (defaultPfpBlur ?: 0f)
    }
    val pfpVideoSpeed = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_video_speed", 1.0f) else (defaultPfpVideoSpeed ?: 1.0f)
    }
    val pfpOverrideExisting = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getBoolean("${prefix}_custom_pfp_override_existing", true) else (defaultPfpOverride ?: true)
    }
    val pfpShowForNoPfp = remember(settingsVersion) {
        if (hasPerContactPfpConfigured) prefs.getBoolean("${prefix}_custom_pfp_show_for_no_pfp", true) else (defaultPfpShowForNoPfp ?: true)
    }
    val pfpSize = remember(settingsVersion) {
        (if (hasPerContactPfpConfigured) prefs.getFloat("${prefix}_custom_pfp_size", defaultPfpSize ?: 0.5f) else (defaultPfpSize ?: 0.5f)).coerceIn(0.1f, 1.0f)
    }
    val pfpShape = remember(settingsVersion) {
        (if (hasPerContactPfpConfigured) prefs.getString("${prefix}_custom_pfp_shape", defaultPfpShape ?: "circle") else (defaultPfpShape ?: "circle")) ?: "circle"
    }
    val previewAvatarSize = if (pfpSize <= 0.50f) {
        (80.dp * (pfpSize / 0.50f)).coerceAtLeast(14.dp)
    } else {
        80.dp + (185.dp - 80.dp) * ((pfpSize - 0.50f) / 0.50f)
    }
    val previewIconSize = (previewAvatarSize * 0.50f).coerceAtLeast(12.dp)
    val isPfpCircle = pfpShape != "square"
    val pfpAvatarShape = if (isPfpCircle) CircleShape else RoundedCornerShape(if (pfpSize >= 0.95f) 0.dp else 10.dp)
    val customPfpFile = remember(pfpPath) { if (pfpPath.isNotEmpty()) File(pfpPath) else null }
    val hasPreviewCustomPfp = (pfpType == "wallpaper" || pfpType == "picture" || pfpType == "video") && customPfpFile != null && customPfpFile.exists() && (pfpShowForNoPfp || pfpOverrideExisting)

    var elementsThemeMode by remember(settingsVersion) {
        mutableStateOf(
            prefs.getString(
                "${prefix}_elements_theme",
                prefs.getString(PreferenceManager.KEY_INCOMING_ELEMENTS_THEME, "auto") ?: "auto"
            ) ?: "auto"
        )
    }
    var showElementsThemePopup by remember { mutableStateOf(false) }

    val autoRefreshKey = "${prefix}_auto_refresh_wallpaper"
    var autoRefreshWallpaper by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean(autoRefreshKey, false))
    }

    var fontColorMode by remember(settingsVersion) {
        mutableStateOf(
            prefs.getString(
                "${prefix}_font_color_mode",
                if (isIncoming) prefs.getString(PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE, "default") ?: "default"
                else prefs.getString(PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE, "default") ?: "default"
            ) ?: "default"
        )
    }
    var customFontColorInt by remember(settingsVersion) {
        mutableIntStateOf(
            prefs.getInt(
                "${prefix}_font_color",
                if (isIncoming) prefs.getInt(PreferenceManager.KEY_INCOMING_FONT_COLOR, android.graphics.Color.WHITE)
                else prefs.getInt(PreferenceManager.KEY_ONGOING_FONT_COLOR, android.graphics.Color.WHITE)
            )
        )
    }

    var showOptionsPopup by remember { mutableStateOf(false) }
    var isLoadingWallpaper by remember { mutableStateOf(false) }
    var editorMediaState by remember {
        mutableStateOf<Triple<File, Boolean, String>?>(null)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = File(context.cacheDir, "picked_image_${System.currentTimeMillis()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        editorMediaState = Triple(tempFile, false, "picture")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = File(context.cacheDir, "picked_video_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        editorMediaState = Triple(tempFile, true, "video")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val systemFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val cr = context.contentResolver
                    val mime = cr.getType(uri) ?: ""
                    val uriStr = uri.toString().lowercase()
                    val isVideo = mime.startsWith("video") || uriStr.endsWith(".mp4") || uriStr.endsWith(".mkv") || uriStr.endsWith(".webm") || uriStr.endsWith(".mov") || uriStr.endsWith(".3gp")
                    val ext = if (isVideo) ".mp4" else ".png"
                    val tempFile = File(context.cacheDir, "picked_file_${System.currentTimeMillis()}$ext")
                    cr.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (tempFile.exists() && tempFile.length() > 0) {
                            editorMediaState = Triple(tempFile, isVideo, if (isVideo) "video" else "picture")
                        } else {
                            Toast.makeText(context, "Could not open selected file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Editor Dialog
    editorMediaState?.let { (file, isVideo, type) ->
        CustomBackgroundEditorDialog(
            target = target,
            mediaFile = file,
            isVideo = isVideo,
            bgType = type,
            prefixOverride = prefix,
            initialZoom = bgZoom,
            initialPanX = bgPanX,
            initialPanY = bgPanY,
            initialDim = bgDim,
            initialBlur = bgBlur,
            initialVideoSpeed = bgVideoSpeed,
            onDismiss = {
                editorMediaState?.let { (file, _, _) ->
                    scope.launch(Dispatchers.IO) {
                        BackgroundMediaManager.cleanupFileIfInCache(context, file)
                    }
                }
                editorMediaState = null
            },
            onSaveSuccess = {
                bgType = type
                bgPath = prefs.getString("${prefix}_bg_path", "") ?: ""
                bgZoom = prefs.getFloat("${prefix}_bg_zoom", 1f)
                bgPanX = prefs.getFloat("${prefix}_bg_pan_x", 0f)
                bgPanY = prefs.getFloat("${prefix}_bg_pan_y", 0f)
                bgDim = prefs.getFloat("${prefix}_bg_dim", 0f)
                bgBlur = prefs.getFloat("${prefix}_bg_blur", 0f)
                bgVideoSpeed = prefs.getFloat("${prefix}_bg_video_speed", 1.0f)
                editorMediaState = null
                BackgroundMediaManager.autoCleanInBackground(context, prefs)
            }
        )
    }

    val isFreeform = remember(settingsVersion) {
        CallButtonPrefs.isFreeformEnabled(prefs)
    }
    val freeformPositions = remember(settingsVersion) {
        CallButtonPrefs.getFreeformPositions(prefs)
    }
    val activeButtons = remember(settingsVersion) {
        CallButtonPrefs.getActiveActionIds(prefs)
    }
    val hangupWidthFraction = remember(settingsVersion) {
        prefs.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.8f)
    }

    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

    val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val appThemePref = prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto"
    val appIsDark = when (appThemePref) {
        "light", "white" -> false
        "dark", "black" -> true
        else -> systemDarkTheme
    }
    val isIncomingElementsDark = when (elementsThemeMode) {
        "light" -> false
        "dark" -> true
        else -> appIsDark
    }

    val isSaturatedActive = remember(settingsVersion, appIsDark) { prefs.isSaturatedForTheme(appIsDark) }
    val solidIcons = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
    val solidIconsDarkStyle = remember(settingsVersion, appIsDark) { prefs.getSolidIconsStyle(appIsDark) }
    val isSaturatedSolidBrightDark = (appIsDark || isIncomingElementsDark) && isSaturatedActive && solidIcons && (solidIconsDarkStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT)

    val previewElemBg = when {
        isSaturatedActive -> MaterialTheme.colorScheme.primary
        hasCustomBg -> if (isIncomingElementsDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.85f)
        isIncomingElementsDark -> Color(0xFF23262D)
        else -> colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f)
    }
    val previewElemFg = when {
        isSaturatedSolidBrightDark -> Color.Black
        isSaturatedActive -> MaterialTheme.colorScheme.onPrimary
        hasCustomBg -> if (isIncomingElementsDark) Color.White else Color(0xFF191C20)
        isIncomingElementsDark -> Color(0xFFE2E2E6)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val previewHandleBg = if (isSaturatedSolidBrightDark) Color.Black else if (isIncomingElementsDark && hasCustomBg) Color.White else (if (isIncomingElementsDark) Color(0xFF383A40) else Color.White)

    val effectiveTextColor = if (fontColorMode == "custom") Color(customFontColorInt)
        else if (hasCustomBg) Color.White
        else MaterialTheme.colorScheme.onSurface
    val effectiveSubtleColor = if (fontColorMode == "custom") Color(customFontColorInt).copy(alpha = 0.85f)
        else if (hasCustomBg) Color.White.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    val textShadow = if (hasCustomBg || fontColorMode == "custom") Shadow(
        color = Color.Black.copy(alpha = 0.80f),
        blurRadius = 8f,
        offset = Offset(0f, 2f)
    ) else null

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (!contactKey.isNullOrEmpty()) "${contactDisplayName ?: "Contact"} — ${if (isIncoming) "Incoming" else "Ongoing"}"
                            else if (isIncoming) "Incoming Call Background" else "Ongoing Call Background",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (!contactKey.isNullOrEmpty()) "Custom background for this contact"
                            else if (isIncoming) "Customize incoming call screen" else "Customize ongoing call screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasCustomBg) {
                        IconButton(onClick = {
                            prefs.remove("${prefix}_bg_type")
                            prefs.remove("${prefix}_bg_path")
                            bgType = "none"
                            bgPath = ""
                            Toast.makeText(context, "Background reset to default", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Reset Background",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Live Preview Mockup Frame ────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color(0xFF101216),
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .width(185.dp)
                                .height(370.dp)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background Layer
                                if (hasCustomBg && bgFile != null) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bgType == "video") {
                                            LoopingVideoPlayer(
                                                videoFile = bgFile,
                                                videoSpeed = bgVideoSpeed,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = bgZoom
                                                        scaleY = bgZoom
                                                        translationX = bgPanX * 0.45f
                                                        translationY = bgPanY * 0.45f
                                                    }
                                                    .then(if (bgBlur > 0f) Modifier.blur((bgBlur * 0.6f).dp) else Modifier)
                                            )
                                        } else {
                                            AsyncImage(
                                                model = bgFile,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = bgZoom
                                                        scaleY = bgZoom
                                                        translationX = bgPanX * 0.45f
                                                        translationY = bgPanY * 0.45f
                                                    }
                                                    .then(if (bgBlur > 0f) Modifier.blur((bgBlur * 0.6f).dp) else Modifier)
                                            )
                                        }

                                        if (bgDim > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = bgDim))
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.surface,
                                                        MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Realistic Camera Punch Hole
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .align(Alignment.TopCenter)
                                        .offset(y = 7.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )

                                // Real UI Overlay matching CallActivity
                                if (isIncoming) {
                                    // ── INCOMING CALL SCREEN REAL PREVIEW ──────────────
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 22.dp, bottom = 14.dp, start = if (pfpSize >= 0.95f) 0.dp else 8.dp, end = if (pfpSize >= 0.95f) 0.dp else 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Caller Info Area
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            val isPfpLarge = showContactPfp && pfpSize >= 0.85f
                                            if (showContactPfp) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(previewAvatarSize)
                                                        .clip(pfpAvatarShape)
                                                        .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (hasPreviewCustomPfp && customPfpFile != null) {
                                                        if (pfpType == "video") {
                                                            LoopingVideoPlayer(
                                                                videoFile = customPfpFile,
                                                                videoSpeed = pfpVideoSpeed,
                                                                isCircular = isPfpCircle,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .graphicsLayer {
                                                                        scaleX = pfpZoom
                                                                        scaleY = pfpZoom
                                                                        translationX = pfpPanX * 0.2f
                                                                        translationY = pfpPanY * 0.2f
                                                                    }
                                                                    .then(if (pfpBlur > 0f) Modifier.blur(pfpBlur.dp) else Modifier)
                                                            )
                                                        } else {
                                                            AsyncImage(
                                                                model = customPfpFile,
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .graphicsLayer {
                                                                        scaleX = pfpZoom
                                                                        scaleY = pfpZoom
                                                                        translationX = pfpPanX * 0.2f
                                                                        translationY = pfpPanY * 0.2f
                                                                        clip = true
                                                                        shape = pfpAvatarShape
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
                                                            Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(previewIconSize)
                                                        )
                                                    }

                                                    val isPfpLarge = pfpSize >= 0.85f
                                                    if (isPfpLarge) {
                                                        Column(
                                                            modifier = Modifier
                                                                .align(if (isPfpCircle) Alignment.Center else Alignment.TopStart)
                                                                .padding(start = if (isPfpCircle) 16.dp else 12.dp, top = if (isPfpCircle) 0.dp else previewAvatarSize * 0.18f, end = if (isPfpCircle) 16.dp else 12.dp),
                                                            horizontalAlignment = if (isPfpCircle) Alignment.CenterHorizontally else Alignment.Start,
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                "Jane Doe",
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    shadow = textShadow,
                                                                    fontSize = 15.sp
                                                                ),
                                                                color = effectiveTextColor,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = if (isPfpCircle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                                                            )
                                                            if (showPhoneNumber) {
                                                                Text(
                                                                    "+1 (555) 234-5678",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        shadow = textShadow,
                                                                        fontSize = 10.sp
                                                                    ),
                                                                    color = effectiveTextColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = if (isPfpCircle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = if (isPfpCircle) Arrangement.Center else Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            ) {
                                                                if (isDualSim) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(3.dp),
                                                                        color = Color(0xFF1E88E5),
                                                                        modifier = Modifier.size(width = 12.dp, height = 14.dp)
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                                                        }
                                                                    }
                                                                    if (isPfpCircle) Spacer(Modifier.width(4.dp))
                                                                }
                                                                Text(
                                                                    "Incoming",
                                                                    color = effectiveSubtleColor,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = textShadow)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (!isPfpLarge) {
                                                    Spacer(Modifier.height(8.dp))
                                                }
                                            }

                                            if (!isPfpLarge) {
                                                Text(
                                                    "Jane Doe",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        shadow = textShadow,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = effectiveTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (showPhoneNumber) {
                                                    Text(
                                                        "+1 (555) 234-5678",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            shadow = textShadow,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = effectiveTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isDualSim) {
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = Color(0xFF1E88E5),
                                                            modifier = Modifier.size(width = 12.dp, height = 14.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        "Incoming",
                                                        color = effectiveSubtleColor,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = textShadow)
                                                    )
                                                }
                                            }
                                        }

                                        // Real Swipe To Answer Section (matching NewSwipeToAnswer)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            // Message quick-reply pill
                                            Surface(
                                                shape = CircleShape,
                                                color = previewElemBg,
                                                modifier = Modifier.height(26.dp).width(86.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(Icons.Default.ChatBubble, null, tint = previewElemFg, modifier = Modifier.size(11.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Message", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                                }
                                            }

                                            // Real Swipe Pill
                                            Box(
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .fillMaxWidth(0.95f)
                                                    .clip(CircleShape)
                                                    .background(previewElemBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Decline", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                    Text("Answer", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                }

                                                // Draggable Phone Handle in Center
                                                Surface(
                                                    shape = CircleShape,
                                                    color = previewHandleBg,
                                                    shadowElevation = 3.dp,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // ── ONGOING CALL SCREEN REAL PREVIEW ──────────────
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Top info
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 26.dp, start = if (pfpSize >= 0.95f) 0.dp else 8.dp, end = if (pfpSize >= 0.95f) 0.dp else 8.dp)
                                        ) {
                                            val isPfpLarge = showContactPfp && pfpSize >= 0.85f
                                            if (showContactPfp) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(previewAvatarSize)
                                                        .clip(pfpAvatarShape)
                                                        .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (hasPreviewCustomPfp && customPfpFile != null) {
                                                        if (pfpType == "video") {
                                                            LoopingVideoPlayer(
                                                                videoFile = customPfpFile,
                                                                videoSpeed = pfpVideoSpeed,
                                                                isCircular = isPfpCircle,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .graphicsLayer {
                                                                        scaleX = pfpZoom
                                                                        scaleY = pfpZoom
                                                                        translationX = pfpPanX * 0.2f
                                                                        translationY = pfpPanY * 0.2f
                                                                    }
                                                                    .then(if (pfpBlur > 0f) Modifier.blur(pfpBlur.dp) else Modifier)
                                                            )
                                                        } else {
                                                            AsyncImage(
                                                                model = customPfpFile,
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .graphicsLayer {
                                                                        scaleX = pfpZoom
                                                                        scaleY = pfpZoom
                                                                        translationX = pfpPanX * 0.2f
                                                                        translationY = pfpPanY * 0.2f
                                                                        clip = true
                                                                        shape = pfpAvatarShape
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
                                                            Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(previewIconSize)
                                                        )
                                                    }

                                                    val isPfpLarge = pfpSize >= 0.85f
                                                    if (isPfpLarge) {
                                                        Column(
                                                            modifier = Modifier
                                                                .align(if (isPfpCircle) Alignment.Center else Alignment.TopStart)
                                                                .padding(start = if (isPfpCircle) 16.dp else 12.dp, top = if (isPfpCircle) 0.dp else previewAvatarSize * 0.18f, end = if (isPfpCircle) 16.dp else 12.dp),
                                                            horizontalAlignment = if (isPfpCircle) Alignment.CenterHorizontally else Alignment.Start,
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                "Jane Doe",
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    shadow = textShadow,
                                                                    fontSize = 15.sp
                                                                ),
                                                                color = effectiveTextColor,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = if (isPfpCircle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                                                            )
                                                            if (showPhoneNumber) {
                                                                Text(
                                                                    "+1 (555) 234-5678",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        shadow = textShadow,
                                                                        fontSize = 10.sp
                                                                    ),
                                                                    color = effectiveTextColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = if (isPfpCircle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = if (isPfpCircle) Arrangement.Center else Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            ) {
                                                                if (isDualSim) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(3.dp),
                                                                        color = Color(0xFF1E88E5),
                                                                        modifier = Modifier.size(width = 12.dp, height = 14.dp)
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                                                        }
                                                                    }
                                                                    if (isPfpCircle) Spacer(Modifier.width(4.dp))
                                                                }
                                                                Text(
                                                                    "00:45",
                                                                    color = effectiveSubtleColor,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = textShadow)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (!isPfpLarge) {
                                                    Spacer(Modifier.height(6.dp))
                                                }
                                            }

                                            if (!isPfpLarge) {
                                                Text(
                                                    "Jane Doe",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        shadow = textShadow,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = effectiveTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (showPhoneNumber) {
                                                    Text(
                                                        "+1 (555) 234-5678",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            shadow = textShadow,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = effectiveTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.height(3.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isDualSim) {
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = Color(0xFF1E88E5),
                                                            modifier = Modifier.size(width = 12.dp, height = 14.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        "00:45",
                                                        color = effectiveSubtleColor,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = textShadow)
                                                    )
                                                }
                                            }
                                        }

                                        // Real Ongoing Call Bottom Control Sheet (matching CallActivity)
                                        val btnBg = if (hasCustomBg) Color.Black.copy(0.40f) else MaterialTheme.colorScheme.surfaceVariant
                                        val btnFg = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface

                                        if (isFreeform) {
                                            // Real Freeform Button Layout matching user's custom positions
                                            val freeformButtonIds = activeButtons + CallButtonPrefs.ID_HANGUP
                                            val density = androidx.compose.ui.platform.LocalDensity.current
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                                color = if (hasCustomBg) Color.Black.copy(alpha = 0.50f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                BoxWithConstraints(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .padding(4.dp)
                                                ) {
                                                    val containerWidthPx = with(density) { maxWidth.toPx() }
                                                    val containerHeightPx = with(density) { maxHeight.toPx() }
                                                    val tileWidthPx = with(density) { 34.dp.toPx() }
                                                    val tileHeightPx = with(density) { 38.dp.toPx() }

                                                    freeformButtonIds.forEachIndexed { index, id ->
                                                        val (fx, fy) = freeformPositions[id] ?: CallButtonPrefs.defaultFreeformFraction(id, index, freeformButtonIds.size)
                                                        Box(
                                                            modifier = Modifier.offset {
                                                                val cx = (fx * containerWidthPx - tileWidthPx / 2f).coerceIn(0f, (containerWidthPx - tileWidthPx).coerceAtLeast(0f))
                                                                val cy = (fy * containerHeightPx - tileHeightPx / 2f).coerceIn(0f, (containerHeightPx - tileHeightPx).coerceAtLeast(0f))
                                                                androidx.compose.ui.unit.IntOffset(kotlin.math.round(cx).toInt(), kotlin.math.round(cy).toInt())
                                                            }
                                                        ) {
                                                            if (id == CallButtonPrefs.ID_HANGUP) {
                                                                Surface(
                                                                    shape = CircleShape,
                                                                    color = Color(0xFFD32F2F),
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                            } else {
                                                                val spec = CallButtonPrefs.specFor(id)
                                                                if (spec != null) {
                                                                    MiniOngoingButton(spec.icon, spec.label, btnBg, btnFg, effectiveSubtleColor)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                                color = if (hasCustomBg) Color.Black.copy(alpha = 0.50f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val previewButtonIds = if (activeButtons.isNotEmpty()) activeButtons.take(6)
                                                        else listOf("mute", "dialpad", "speaker", "add", "hold", "note")

                                                    previewButtonIds.chunked(3).forEach { rowIds ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceEvenly
                                                        ) {
                                                            rowIds.forEach { id ->
                                                                val spec = CallButtonPrefs.specFor(id)
                                                                if (spec != null) {
                                                                    MiniOngoingButton(spec.icon, spec.label, btnBg, btnFg, effectiveSubtleColor)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(Modifier.height(2.dp))

                                                    // Real Hang Up Button with configured width
                                                    val isCircleHangup = hangupWidthFraction <= 0.1f
                                                    Surface(
                                                        shape = if (isCircleHangup) CircleShape else RoundedCornerShape(16.dp),
                                                        color = Color(0xFFD32F2F),
                                                        modifier = if (isCircleHangup) Modifier.size(32.dp)
                                                                   else Modifier
                                                                        .fillMaxWidth(hangupWidthFraction.coerceIn(0.40f, 0.95f))
                                                                        .height(32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Customize / Adjust button if custom bg active
                        if (hasCustomBg && bgFile != null) {
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    editorMediaState = Triple(bgFile, bgType == "video", bgType)
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Adjust Zoom, Blur & Dim", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // ── Incoming UI Elements Theme ────────────────────────────────
            if (isIncoming) {
                item {
                    RivoAnimatedSection(delayMs = 10L) {
                        Column {
                            Text(
                                "Incoming UI Elements Theme",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            )

                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    val currentThemeTitle = when (elementsThemeMode) {
                                        "light" -> "Light mode"
                                        "dark" -> "Dark mode"
                                        else -> "App preference (Default)"
                                    }
                                    val currentThemeSubtitle = when (elementsThemeMode) {
                                        "light" -> "Force light styling for slider & message button"
                                        "dark" -> "Force dark styling for slider & message button"
                                        else -> "Follows appearance settings"
                                    }
                                    val currentThemeIcon = when (elementsThemeMode) {
                                        "light" -> Icons.Outlined.LightMode
                                        "dark" -> Icons.Outlined.DarkMode
                                        else -> Icons.Outlined.BrightnessAuto
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    currentThemeIcon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                currentThemeTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                currentThemeSubtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showElementsThemePopup = true },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Change Elements Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Text & Font Color Customizer ─────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 15L) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FormatColorText,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Caller Info Font Color",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // 2 Options: Default vs Custom
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Default Option
                                    Surface(
                                        onClick = {
                                            fontColorMode = "default"
                                            val keyMode = if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE else PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE
                                            prefs.setString(keyMode, "default")
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (fontColorMode == "default") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (fontColorMode == "default") androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (fontColorMode == "default") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Default (Adaptive)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (fontColorMode == "default") FontWeight.Bold else FontWeight.Medium,
                                                color = if (fontColorMode == "default") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Custom Option
                                    Surface(
                                        onClick = {
                                            fontColorMode = "custom"
                                            val keyMode = if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE else PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE
                                            val keyColor = if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR else PreferenceManager.KEY_ONGOING_FONT_COLOR
                                            prefs.setString(keyMode, "custom")
                                            prefs.setInt(keyColor, customFontColorInt)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (fontColorMode == "custom") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (fontColorMode == "custom") androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            // Color indicator dot
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(customFontColorInt),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(16.dp)
                                            ) {}
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Custom Color",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (fontColorMode == "custom") FontWeight.Bold else FontWeight.Medium,
                                                color = if (fontColorMode == "custom") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Interactive Color Picker (Visible when "custom" selected)
                                AnimatedVisibility(
                                    visible = fontColorMode == "custom",
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        InteractiveColorPicker(
                                            initialColor = Color(customFontColorInt),
                                            onColorChanged = { newColor ->
                                                val argb = newColor.toArgb()
                                                customFontColorInt = argb
                                                val keyMode = if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE else PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE
                                                val keyColor = if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR else PreferenceManager.KEY_ONGOING_FONT_COLOR
                                                prefs.setString(keyMode, "custom")
                                                prefs.setInt(keyColor, argb)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Background Source Options ─────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 25L) {
                    Column {
                        Text(
                            "Background Source",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )

                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                val currentSourceIcon = when (bgType) {
                                    "wallpaper" -> Icons.Outlined.PhoneAndroid
                                    "picture" -> Icons.Outlined.Image
                                    "video" -> Icons.Outlined.Videocam
                                    else -> Icons.Outlined.NotInterested
                                }
                                val currentSourceTitle = when (bgType) {
                                    "wallpaper" -> "Device Wallpaper"
                                    "picture" -> "Custom Picture"
                                    "video" -> "Custom Video"
                                    else -> "None (Default)"
                                }
                                val currentSourceSubtitle = when (bgType) {
                                    "wallpaper" -> "Active device system wallpaper"
                                    "picture" -> "Custom photo selected"
                                    "video" -> "Custom looping video selected"
                                    else -> "Using default solid background"
                                }
                                val currentContainerColor = MaterialTheme.colorScheme.primaryContainer
                                val currentIconColor = MaterialTheme.colorScheme.primary

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = currentContainerColor,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                currentSourceIcon,
                                                contentDescription = null,
                                                tint = currentIconColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            currentSourceTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            currentSourceSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showOptionsPopup = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Choose Background", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }

                                if (bgType == "wallpaper") {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val next = !autoRefreshWallpaper
                                                autoRefreshWallpaper = next
                                                prefs.setBoolean(autoRefreshKey, next)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Outlined.Sync,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    "Auto refresh wallpaper every app start",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "Automatically refreshes the system wallpaper whenever you open the app",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Switch(
                                            checked = autoRefreshWallpaper,
                                            onCheckedChange = {
                                                autoRefreshWallpaper = it
                                                prefs.setBoolean(autoRefreshKey, it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptionsPopup) {
        CustomBackgroundOptionsPopup(
            target = target,
            currentType = bgType,
            onDismiss = { showOptionsPopup = false },
            onSelectNone = {
                bgType = "none"
                prefs.setString("${prefix}_bg_type", "none")
                prefs.setString("${prefix}_bg_path", "")
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                Toast.makeText(context, "Default background applied", Toast.LENGTH_SHORT).show()
            },
            onOpenEditor = { file, isVideo, type ->
                editorMediaState = Triple(file, isVideo, type)
                showOptionsPopup = false
            }
        )
    }

    if (showElementsThemePopup) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showElementsThemePopup = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showElementsThemePopup = false }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Incoming UI Elements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Slider & quick reply theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Option 1: App Preference
                        BackgroundOptionItem(
                            icon = Icons.Outlined.BrightnessAuto,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            title = "App preference (Default)",
                            subtitle = "Follows appearance settings ($appThemePref)",
                            isSelected = elementsThemeMode == "auto" || elementsThemeMode == "app" || elementsThemeMode.isEmpty(),
                            onClick = {
                                elementsThemeMode = "auto"
                                val key = if (!contactKey.isNullOrEmpty()) "contact_${contactKey}_incoming_elements_theme" else PreferenceManager.KEY_INCOMING_ELEMENTS_THEME
                                prefs.setString(key, "auto")
                                showElementsThemePopup = false
                            }
                        )

                        // Option 2: Light Mode
                        BackgroundOptionItem(
                            icon = Icons.Outlined.LightMode,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            title = "Light mode",
                            subtitle = "Always show light elements",
                            isSelected = elementsThemeMode == "light",
                            onClick = {
                                elementsThemeMode = "light"
                                val key = if (!contactKey.isNullOrEmpty()) "contact_${contactKey}_incoming_elements_theme" else PreferenceManager.KEY_INCOMING_ELEMENTS_THEME
                                prefs.setString(key, "light")
                                showElementsThemePopup = false
                            }
                        )

                        // Option 3: Dark Mode
                        BackgroundOptionItem(
                            icon = Icons.Outlined.DarkMode,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            title = "Dark mode",
                            subtitle = "Always show dark elements",
                            isSelected = elementsThemeMode == "dark",
                            onClick = {
                                elementsThemeMode = "dark"
                                val key = if (!contactKey.isNullOrEmpty()) "contact_${contactKey}_incoming_elements_theme" else PreferenceManager.KEY_INCOMING_ELEMENTS_THEME
                                prefs.setString(key, "dark")
                                showElementsThemePopup = false
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showElementsThemePopup = false }) {
                                Text("Cancel", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun colorLerp(start: Color, stop: Color, fraction: Float): Color = androidx.compose.ui.graphics.lerp(start, stop, fraction)

/**
 * Miniature Feature Button for Ongoing Call Screen Preview
 */
@Composable
private fun MiniOngoingButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    labelColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = labelColor
        )
    }
}

/**
 * Interactive 2D HSV Color Picker with touch spectrum canvas and pointer
 */
@Composable
internal fun InteractiveColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val hsv = remember(initialColor) {
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArr)
        hsvArr
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceIn(0f, 1f)) }

    fun updateColor(newHue: Float, newSat: Float, newVal: Float) {
        hue = newHue
        saturation = newSat
        value = newVal
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(newHue, newSat, newVal))
        onColorChanged(Color(colorInt))
    }

    val currentPureHueColor = remember(hue) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    }
    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 2D Saturation-Value Canvas with pointer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            val sat = (offset.x / size.width).coerceIn(0f, 1f)
                            val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            updateColor(hue, sat, v)
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val sat = (change.position.x / size.width).coerceIn(0f, 1f)
                            val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            updateColor(hue, sat, v)
                        }
                    }
            ) {
                // Horizontal White -> Hue Gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, currentPureHueColor)
                    )
                )
                // Vertical Transparent -> Black Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Pointer circle
                val pointerX = saturation * size.width
                val pointerY = (1f - value) * size.height

                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = 12.dp.toPx(),
                    center = Offset(pointerX, pointerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(pointerX, pointerY)
                )
                drawCircle(
                    color = currentColor,
                    radius = 7.dp.toPx(),
                    center = Offset(pointerX, pointerY)
                )
            }
        }

        // Rainbow Hue Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hue Spectrum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                        )
                    )
            ) {
                Slider(
                    value = hue,
                    onValueChange = { updateColor(it, saturation, value) },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxSize().offset(y = (-8).dp)
                )
            }
        }

        // Color Hex and Live Preview Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = currentColor,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(28.dp)
                ) {}
                Text(
                    text = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text("Selected Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Preset Color Swatches
        Text("Quick Swatches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val swatches = listOf(
            Color(0xFFFFFFFF), // Pure White
            Color(0xFFFFD54F), // Amber
            Color(0xFFFF7043), // Coral
            Color(0xFFE91E63), // Pink
            Color(0xFFBA68C8), // Purple
            Color(0xFF64B5F6), // Blue
            Color(0xFF4DD0E1), // Cyan
            Color(0xFF81C784), // Green
            Color(0xFFAED581), // Lime
            Color(0xFFB0BEC5)  // Cool Grey
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(swatches) { swatch ->
                val isSelected = (swatch.toArgb() and 0xFFFFFF) == (currentColor.toArgb() and 0xFFFFFF)
                Surface(
                    onClick = {
                        val swatchHsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.toArgb(), swatchHsv)
                        updateColor(swatchHsv[0], swatchHsv[1], swatchHsv[2])
                    },
                    shape = CircleShape,
                    color = swatch,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                             else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = if (swatch == Color.White) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundChoiceCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                 else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

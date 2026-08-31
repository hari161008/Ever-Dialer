package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Context
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.BackgroundMediaManager
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

private fun colorLerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

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
    val defaultPfpSize = 0.5f
    var pfpSize by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat("${prefix}_custom_pfp_size", defaultPfpSize).coerceIn(0.1f, 1.0f))
    }
    var pfpShape by remember(settingsVersion) {
        mutableStateOf(prefs.getString("${prefix}_custom_pfp_shape", "circle") ?: "circle")
    }

    var showOptionsPopup by remember { mutableStateOf(false) }
    var showShapeDialog by remember { mutableStateOf(false) }
    var editorMediaState by remember { mutableStateOf<Triple<File, Boolean, String>?>(null) }

    val pfpFile = remember(pfpPath) { if (pfpPath.isNotEmpty()) File(pfpPath) else null }
    val hasCustomPfp = (pfpType == "wallpaper" || pfpType == "picture" || pfpType == "video") && pfpFile != null && pfpFile.exists()

    val pfpLabel = when {
        pfpType == "wallpaper" -> "Device Wallpaper"
        pfpType == "picture"   -> "Custom Picture"
        pfpType == "video"     -> "Custom Video"
        else                   -> "None (Default Face Icon)"
    }

    // Call background configs for authentic preview backdrop
    val bgType = remember(settingsVersion) { prefs.getString("${prefix}_bg_type", "none") ?: "none" }
    val bgPath = remember(settingsVersion) { prefs.getString("${prefix}_bg_path", "") ?: "" }
    val bgZoom = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_zoom", 1f) }
    val bgPanX = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_pan_x", 0f) }
    val bgPanY = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_pan_y", 0f) }
    val bgDim = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_dim", 0f) }
    val bgBlur = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_blur", 0f) }
    val bgVideoSpeed = remember(settingsVersion) { prefs.getFloat("${prefix}_bg_video_speed", 1.0f) }
    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

    val fontColorMode = remember(settingsVersion) {
        prefs.getString(
            "${prefix}_font_color_mode",
            if (isIncoming) prefs.getString(PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE, "default") ?: "default"
            else prefs.getString(PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE, "default") ?: "default"
        ) ?: "default"
    }
    val customFontColorInt = remember(settingsVersion) {
        prefs.getInt(
            "${prefix}_font_color",
            if (isIncoming) prefs.getInt(PreferenceManager.KEY_INCOMING_FONT_COLOR, android.graphics.Color.WHITE)
            else prefs.getInt(PreferenceManager.KEY_ONGOING_FONT_COLOR, android.graphics.Color.WHITE)
        )
    }
    val elementsThemeMode = remember(settingsVersion) {
        prefs.getString(
            "${prefix}_elements_theme",
            prefs.getString(PreferenceManager.KEY_INCOMING_ELEMENTS_THEME, "auto") ?: "auto"
        ) ?: "auto"
    }

    val isDualSim = remember {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            (tm?.phoneCount ?: 1) > 1
        } catch (_: Exception) { false }
    }

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

    val isFreeform = remember(settingsVersion) { CallButtonPrefs.isFreeformEnabled(prefs) }
    val freeformPositions = remember(settingsVersion) { CallButtonPrefs.getFreeformPositions(prefs) }
    val activeButtons = remember(settingsVersion) { CallButtonPrefs.getActiveActionIds(prefs) }
    val hangupWidthFraction = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.8f) }

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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Live Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, bottom = 8.dp)
                    )

                    // Authentic Full-Fidelity Phone Frame Preview
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF101216),
                        border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .width(185.dp)
                            .height(370.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .clickable {
                                if (hasCustomPfp && pfpFile != null && (pfpType == "picture" || pfpType == "video" || pfpType == "wallpaper")) {
                                    editorMediaState = Triple(pfpFile, pfpType == "video", pfpType)
                                } else {
                                    showOptionsPopup = true
                                }
                            }
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

                            // Dynamic Avatar sizing & shape calculations
                            val isCircle = pfpShape != "square"
                            // Base size at 50% = 80.dp (matches old 60%), Max size at 100% = 185.dp (full width), Min at 10% = 14.dp
                            val previewAvatarSize = if (pfpSize <= 0.50f) {
                                (80.dp * (pfpSize / 0.50f)).coerceAtLeast(14.dp)
                            } else {
                                80.dp + (185.dp - 80.dp) * ((pfpSize - 0.50f) / 0.50f)
                            }
                            val previewIconSize = (previewAvatarSize * 0.50f).coerceAtLeast(12.dp)
                            val avatarShape = if (isCircle) CircleShape else RoundedCornerShape(if (pfpSize >= 0.95f) 0.dp else 10.dp)

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
                                        Box(
                                            modifier = Modifier
                                                .size(previewAvatarSize)
                                                .clip(avatarShape)
                                                .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasCustomPfp && pfpFile != null) {
                                                if (pfpType == "video") {
                                                    LoopingVideoPlayer(
                                                        videoFile = pfpFile,
                                                        videoSpeed = pfpVideoSpeed,
                                                        isCircular = isCircle,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer {
                                                                scaleX = pfpZoom
                                                                scaleY = pfpZoom
                                                                translationX = pfpPanX * 0.2f
                                                                translationY = pfpPanY * 0.2f
                                                                clip = true
                                                                shape = avatarShape
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
                                                                translationX = pfpPanX * 0.2f
                                                                translationY = pfpPanY * 0.2f
                                                                clip = true
                                                                shape = avatarShape
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

                                            val isSquareLarge = (!isCircle) && pfpSize >= 0.85f
                                            if (isSquareLarge) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(start = 12.dp, top = previewAvatarSize * 0.18f, end = 12.dp),
                                                    horizontalAlignment = Alignment.Start,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        contactDisplayName ?: "Jane Doe",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            shadow = textShadow,
                                                            fontSize = 15.sp
                                                        ),
                                                        color = effectiveTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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

                                        val isSquareLarge = (!isCircle) && pfpSize >= 0.85f
                                        if (!isSquareLarge) {
                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                contactDisplayName ?: "Jane Doe",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow,
                                                    fontSize = 15.sp
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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

                                    // Real Swipe To Answer Section
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
                                        Box(
                                            modifier = Modifier
                                                .size(previewAvatarSize)
                                                .clip(avatarShape)
                                                .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasCustomPfp && pfpFile != null) {
                                                if (pfpType == "video") {
                                                    LoopingVideoPlayer(
                                                        videoFile = pfpFile,
                                                        videoSpeed = pfpVideoSpeed,
                                                        isCircular = isCircle,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer {
                                                                scaleX = pfpZoom
                                                                scaleY = pfpZoom
                                                                translationX = pfpPanX * 0.2f
                                                                translationY = pfpPanY * 0.2f
                                                                clip = true
                                                                shape = avatarShape
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
                                                                translationX = pfpPanX * 0.2f
                                                                translationY = pfpPanY * 0.2f
                                                                clip = true
                                                                shape = avatarShape
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

                                            val isSquareLarge = (!isCircle) && pfpSize >= 0.85f
                                            if (isSquareLarge) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(start = 12.dp, top = previewAvatarSize * 0.18f, end = 12.dp),
                                                    horizontalAlignment = Alignment.Start,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        contactDisplayName ?: "Jane Doe",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            shadow = textShadow,
                                                            fontSize = 15.sp
                                                        ),
                                                        color = effectiveTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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

                                        val isSquareLarge = (!isCircle) && pfpSize >= 0.85f
                                        if (!isSquareLarge) {
                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                contactDisplayName ?: "Jane Doe",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow,
                                                    fontSize = 15.sp
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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

                                    // Real Ongoing Call Bottom Control Sheet
                                    val btnBg = if (hasCustomBg) Color.Black.copy(0.40f) else MaterialTheme.colorScheme.surfaceVariant
                                    val btnFg = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface

                                    if (isFreeform) {
                                        val freeformButtonIds = activeButtons + CallButtonPrefs.ID_HANGUP
                                        val density = LocalDensity.current
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

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (hasCustomPfp) "Tap preview to open Contact PFP editor" else "Tap preview to choose Contact PFP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Contact PFP Container & Options ──────────────────────────────
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

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Feature 3: PFP Size Slider
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "PFP Size",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Adjust display size (50% is standard, 100% touches screen edges)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        "${(pfpSize * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = pfpSize,
                                onValueChange = {
                                    pfpSize = it
                                    prefs.setFloat("${prefix}_custom_pfp_size", it)
                                },
                                valueRange = 0.10f..1.0f,
                                steps = 17,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Feature 4: PFP Shape Chooser
                        RivoListItem(
                            headline = "PFP Shape",
                            supporting = if (pfpShape == "square") "Square" else "Circle (Default)",
                            leadingIcon = if (pfpShape == "square") Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF673AB7),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contact_pfp_shape", highlightedKey) { highlightedKey = null },
                            onClick = { showShapeDialog = true }
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

    if (showShapeDialog) {
        AlertDialog(
            onDismissRequest = { showShapeDialog = false },
            title = { Text("Choose PFP Shape", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("circle", "Circle (Default)"),
                        Pair("square", "Square")
                    ).forEach { (shapeKey, shapeLabel) ->
                        val isSelected = pfpShape == shapeKey
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pfpShape = shapeKey
                                    prefs.setString("${prefix}_custom_pfp_shape", shapeKey)
                                    showShapeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(
                                        if (shapeKey == "square") Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                                        null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        shapeLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShapeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

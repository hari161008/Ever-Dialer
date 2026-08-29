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
    isIncoming: Boolean = true
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    val target = if (isIncoming) CustomBackgroundTarget.INCOMING else CustomBackgroundTarget.ONGOING
    val prefix = target.prefix

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
    val showContactPfp = remember(settingsVersion) {
        if (isIncoming) prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, true)
        else prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, true)
    }

    var fontColorMode by remember(settingsVersion) {
        mutableStateOf(
            prefs.getString(
                if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE else PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE,
                "default"
            ) ?: "default"
        )
    }
    var customFontColorInt by remember(settingsVersion) {
        mutableIntStateOf(
            prefs.getInt(
                if (isIncoming) PreferenceManager.KEY_INCOMING_FONT_COLOR else PreferenceManager.KEY_ONGOING_FONT_COLOR,
                android.graphics.Color.WHITE
            )
        )
    }

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

    // Editor Dialog
    editorMediaState?.let { (file, isVideo, type) ->
        CustomBackgroundEditorDialog(
            target = target,
            mediaFile = file,
            isVideo = isVideo,
            bgType = type,
            initialZoom = bgZoom,
            initialPanX = bgPanX,
            initialPanY = bgPanY,
            initialDim = bgDim,
            initialBlur = bgBlur,
            onDismiss = { editorMediaState = null },
            onSaveSuccess = {
                bgType = type
                bgPath = prefs.getString("${prefix}_bg_path", "") ?: ""
                bgZoom = prefs.getFloat("${prefix}_bg_zoom", 1f)
                bgPanX = prefs.getFloat("${prefix}_bg_pan_x", 0f)
                bgPanY = prefs.getFloat("${prefix}_bg_pan_y", 0f)
                bgDim = prefs.getFloat("${prefix}_bg_dim", 0f)
                bgBlur = prefs.getFloat("${prefix}_bg_blur", 0f)
                editorMediaState = null
            }
        )
    }

    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

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
                        Text("Custom Background", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isIncoming) "Incoming Call Screen" else "Ongoing Call Screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    SettingsBackIconButton(onClick = { navigator.navigateUp() })
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
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Live Call UI Real Preview Window ────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Live Preview",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, bottom = 8.dp)
                        )

                        // Real Phone Mockup Frame
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color(0xFF101216),
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .width(220.dp)
                                .height(390.dp)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background Layer
                                if (hasCustomBg && bgFile != null) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bgType == "video") {
                                            LoopingVideoPlayer(
                                                videoFile = bgFile,
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
                                    // Default dialer background
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

                                // Soft contrast scrim at top
                                if (hasCustomBg) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .align(Alignment.TopCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent)
                                                )
                                            )
                                    )
                                }

                                // Realistic Camera Punch Hole
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopCenter)
                                        .offset(y = 8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )

                                // Real UI Overlay matching CallActivity
                                if (isIncoming) {
                                    // ── INCOMING CALL SCREEN REAL PREVIEW ──────────────
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 28.dp, bottom = 20.dp, start = 12.dp, end = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Caller Info Area
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            if (showContactPfp) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(CircleShape)
                                                        .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(38.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(10.dp))
                                            }

                                            Text(
                                                "Jane Doe",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "+1 (555) 234-5678",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF1E88E5),
                                                    modifier = Modifier.size(width = 14.dp, height = 16.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                                    }
                                                }
                                                Text(
                                                    "Incoming",
                                                    color = effectiveSubtleColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow)
                                                )
                                            }
                                        }

                                        // Real Swipe To Answer Section (matching NewSwipeToAnswer)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        ) {
                                            // Message quick-reply pill
                                            Surface(
                                                shape = CircleShape,
                                                color = if (hasCustomBg) Color.Black.copy(0.45f) else colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f),
                                                modifier = Modifier.height(26.dp).width(90.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(Icons.Default.ChatBubble, null, tint = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(11.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Message", color = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                }
                                            }

                                            // Real Swipe Pill
                                            Box(
                                                modifier = Modifier
                                                    .height(52.dp)
                                                    .fillMaxWidth(0.92f)
                                                    .clip(CircleShape)
                                                    .background(if (hasCustomBg) Color.Black.copy(0.50f) else colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Decline", color = if (hasCustomBg) Color(0xFFFF8A80) else MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                    Text("Answer", color = if (hasCustomBg) Color(0xFFB9F6CA) else MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                }

                                                // Draggable Phone Handle in Center
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.White,
                                                    shadowElevation = 4.dp,
                                                    modifier = Modifier.size(38.dp)
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 28.dp, bottom = 14.dp, start = 12.dp, end = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Top info
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            if (showContactPfp) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(CircleShape)
                                                        .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                            }

                                            Text(
                                                "Jane Doe",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "+1 (555) 234-5678",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = textShadow
                                                ),
                                                color = effectiveTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "00:45",
                                                color = effectiveSubtleColor,
                                                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow)
                                            )
                                        }

                                        // Real Ongoing Call Buttons Grid (2 rows x 3 columns)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            val btnBg = if (hasCustomBg) Color.Black.copy(0.40f) else MaterialTheme.colorScheme.surfaceVariant
                                            val btnFg = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface

                                            // Row 1
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                MiniOngoingButton(Icons.Default.MicOff, "Mute", btnBg, btnFg, effectiveSubtleColor)
                                                MiniOngoingButton(Icons.Default.Dialpad, "Keypad", btnBg, btnFg, effectiveSubtleColor)
                                                MiniOngoingButton(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", btnBg, btnFg, effectiveSubtleColor)
                                            }

                                            // Row 2
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                MiniOngoingButton(Icons.Default.Add, "Add", btnBg, btnFg, effectiveSubtleColor)
                                                MiniOngoingButton(Icons.Default.Pause, "Hold", btnBg, btnFg, effectiveSubtleColor)
                                                MiniOngoingButton(Icons.Default.EditNote, "Notes", btnBg, btnFg, effectiveSubtleColor)
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            // Hang Up Button
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color(0xFFD32F2F),
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .height(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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

            // ── Text & Font Color Customizer ─────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 15L) {
                    Column {
                        Text(
                            "Caller Info Font Color",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )

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
                            "Choose Background",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )

                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Option 1: None (Default)
                                BackgroundChoiceCard(
                                    icon = Icons.Outlined.NotInterested,
                                    iconTint = Color(0xFFE53935),
                                    iconContainerColor = Color(0xFFE53935).copy(alpha = 0.12f),
                                    title = "None (Default)",
                                    subtitle = "Use the default dialer theme background",
                                    isSelected = bgType == "none" || bgType.isEmpty(),
                                    onClick = {
                                        bgType = "none"
                                        prefs.setString("${prefix}_bg_type", "none")
                                        prefs.setString("${prefix}_bg_path", "")
                                        Toast.makeText(context, "Default background applied", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                // Option 2: Device Wallpaper
                                BackgroundChoiceCard(
                                    icon = Icons.Outlined.PhoneAndroid,
                                    iconTint = Color(0xFF2196F3),
                                    iconContainerColor = Color(0xFF2196F3).copy(alpha = 0.12f),
                                    title = "Device Wallpaper",
                                    subtitle = if (isLoadingWallpaper) "Extracting wallpaper..." else "Use your current system wallpaper",
                                    isSelected = bgType == "wallpaper",
                                    onClick = {
                                        if (!isLoadingWallpaper) {
                                            isLoadingWallpaper = true
                                            scope.launch(Dispatchers.IO) {
                                                val wallpaperFile = WallpaperExportHelper.extractWallpaperToFile(context)
                                                withContext(Dispatchers.Main) {
                                                    isLoadingWallpaper = false
                                                    if (wallpaperFile != null) {
                                                        editorMediaState = Triple(wallpaperFile, false, "wallpaper")
                                                    } else {
                                                        Toast.makeText(context, "Could not extract system wallpaper", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )

                                // Option 3: Custom Picture
                                BackgroundChoiceCard(
                                    icon = Icons.Outlined.Image,
                                    iconTint = Color(0xFF4CAF50),
                                    iconContainerColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
                                    title = "Custom Picture",
                                    subtitle = "Choose a photo from gallery & adjust framing",
                                    isSelected = bgType == "picture",
                                    onClick = {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                )

                                // Option 4: Custom Video
                                BackgroundChoiceCard(
                                    icon = Icons.Outlined.Videocam,
                                    iconTint = Color(0xFF9C27B0),
                                    iconContainerColor = Color(0xFF9C27B0).copy(alpha = 0.12f),
                                    title = "Custom Video",
                                    subtitle = "Choose a looping video background",
                                    isSelected = bgType == "video",
                                    onClick = {
                                        videoPickerLauncher.launch("video/*")
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
private fun InteractiveColorPicker(
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

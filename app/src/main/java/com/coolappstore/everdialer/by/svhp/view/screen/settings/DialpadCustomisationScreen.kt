package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.toArgb
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
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun DialpadCustomisationScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    val settingsVersion by prefs.settingsChanged.collectAsState()

    var heightScale by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, 1.0f).coerceIn(0.80f, 1.30f))
    }
    var bgType by remember(settingsVersion) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_DIALPAD_BG_TYPE, "none") ?: "none")
    }
    var bgPath by remember(settingsVersion) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_DIALPAD_BG_PATH, "") ?: "")
    }
    var bgZoom by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_ZOOM, 1f))
    }
    var bgPanX by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_PAN_X, 0f))
    }
    var bgPanY by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_PAN_Y, 0f))
    }
    var bgDim by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_DIM, 0f))
    }
    var bgBlur by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_BLUR, 0f))
    }
    var bgVideoSpeed by remember(settingsVersion) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_VIDEO_SPEED, 1.0f))
    }

    var isCircleButtons by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, false))
    }

    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper") || ((bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists())

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isSaturatedContainer = remember(settingsVersion) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_CONTAINER }

    val bgLuminance = remember(settingsVersion, bgPath, bgDim) {
        if (hasCustomBg && bgFile != null && bgFile.exists()) {
            BackgroundMediaManager.getOrCalculateDialpadLuminance(prefs, bgFile, bgType == "video", bgDim)
        } else 0.2f
    }
    val isBrightBg = hasCustomBg && bgLuminance > 0.48f

    val baseKeyContainer = if (isSaturatedActive && isSaturatedContainer) {
        androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.35f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val previewKeyBg = if (hasCustomBg) {
        if (isDark) {
            baseKeyContainer
        } else if (isBrightBg) {
            // Bright wallpaper: darken button slightly (by 16%) so it pops out clearly without changing its hue
            androidx.compose.ui.graphics.lerp(baseKeyContainer, Color.Black, 0.16f)
        } else {
            baseKeyContainer
        }
    } else {
        baseKeyContainer
    }

    val previewKeyBgLuminance = remember(previewKeyBg) {
        androidx.core.graphics.ColorUtils.calculateLuminance(previewKeyBg.toArgb())
    }

    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val previewKeyText = remember(previewKeyBg, onPrimaryContainer) {
        if (previewKeyBgLuminance > 0.45) {
            if (androidx.core.graphics.ColorUtils.calculateLuminance(onPrimaryContainer.toArgb()) < 0.45) onPrimaryContainer
            else Color(0xFF1C1B1F)
        } else {
            if (androidx.core.graphics.ColorUtils.calculateLuminance(onPrimaryContainer.toArgb()) >= 0.45) onPrimaryContainer
            else Color.White
        }
    }

    val previewKeySubText = remember(previewKeyText) {
        previewKeyText.copy(alpha = 0.75f)
    }

    val previewContainerBg = if (hasCustomBg) {
        if (isDark) Color.Black.copy(alpha = if (isBrightBg) 0.50f else 0.35f)
        else Color.White.copy(alpha = if (isBrightBg) 0.60f else 0.45f)
    } else MaterialTheme.colorScheme.surfaceContainerLow

    val previewBorder = BorderStroke(
        0.5.dp,
        if (hasCustomBg) {
            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
        } else Color.Transparent
    )

    val previewTextPrimary = if (hasCustomBg) {
        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    } else MaterialTheme.colorScheme.onSurface

    val previewTextSecondary = if (hasCustomBg) {
        if (isDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    } else MaterialTheme.colorScheme.onSurfaceVariant

    val previewIconTint = if (hasCustomBg) {
        if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    } else MaterialTheme.colorScheme.onSurfaceVariant

    var showOptionsPopup by remember { mutableStateOf(false) }
    var editorMediaState by remember { mutableStateOf<Triple<File, Boolean, String>?>(null) }

    LaunchedEffect(Unit) {
        BackgroundMediaManager.autoCleanInBackground(context, prefs)
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Customise Dialpad",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── TOP CONTAINER: REAL PREVIEW OF DIALPAD SCREEN ───────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, bottom = 8.dp)
                    )

                    // Phone frame mockup container (real phone proportions ~9:19)
                    Card(
                        modifier = Modifier
                            .width(225.dp)
                            .height(465.dp)
                            .shadow(14.dp, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Custom Background Layer
                            if (hasCustomBg) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (bgType == "wallpaper") {
                                        val wallpaperDrawable = remember {
                                            try {
                                                android.app.WallpaperManager.getInstance(context).drawable
                                            } catch (_: Throwable) { null }
                                        }
                                        if (wallpaperDrawable != null) {
                                            AsyncImage(
                                                model = wallpaperDrawable,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
                                                                MaterialTheme.colorScheme.surface
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Wallpaper,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(36.dp),
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Device Wallpaper",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        if (bgDim > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = bgDim))
                                            )
                                        }
                                    } else if (bgFile != null) {
                                        if (bgType == "video") {
                                            LoopingVideoPlayer(
                                                videoFile = bgFile,
                                                videoSpeed = bgVideoSpeed,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = bgZoom
                                                        scaleY = bgZoom
                                                        translationX = bgPanX * 0.28f
                                                        translationY = bgPanY * 0.28f
                                                    }
                                                    .then(if (bgBlur > 0f) Modifier.blur((bgBlur * 0.5f).dp) else Modifier)
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
                                                        translationX = bgPanX * 0.28f
                                                        translationY = bgPanY * 0.28f
                                                    }
                                                    .then(if (bgBlur > 0f) Modifier.blur((bgBlur * 0.5f).dp) else Modifier)
                                            )
                                        }
                                    }
                                    if (bgDim > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = bgDim))
                                        )
                                    }
                                }
                            }

                            // Dialpad UI Elements inside Preview
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top Sheet Handle
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = if (hasCustomBg) (if (isBrightBg) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.6f)) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(width = 32.dp, height = 3.5.dp)
                                    ) {}
                                }

                                // Search Bar Row (Search field + Dismiss circular X button)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        color = previewContainerBg,
                                        border = previewBorder
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = previewIconTint.copy(alpha = 0.75f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Search contacts...",
                                                fontSize = 9.5.sp,
                                                color = previewTextSecondary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Circular Close Button (as on actual screen)
                                    Surface(
                                        shape = CircleShape,
                                        color = previewContainerBg,
                                        border = previewBorder,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = previewIconTint,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                // Sample Recent Contacts Card (as shown in edge50_dialpad.png)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = previewContainerBg,
                                    border = previewBorder
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Item 1
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE91E63)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "A",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "Account Info",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = previewTextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "*199#",
                                                    fontSize = 7.5.sp,
                                                    color = previewTextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        // Item 2
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFF5722)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "S",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "Sim Rupees",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = previewTextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "*111#",
                                                    fontSize = 7.5.sp,
                                                    color = previewTextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Real Dialpad Card Mockup (faithfully replicating actual Dialpad screen)
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = previewContainerBg,
                                    border = previewBorder,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 6.dp,
                                                end = 6.dp,
                                                top = (5 * heightScale).dp,
                                                bottom = (10 * heightScale).dp
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy((4 * heightScale).dp)
                                    ) {
                                        // Typing Box with Backspace Button (matching real dialpad)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height((28 * heightScale).coerceIn(24f, 36f).dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (hasCustomBg) (if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.65f))
                                                        else MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                    .border(0.5.dp, if (hasCustomBg) (if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)) else Color.Transparent, RoundedCornerShape(16.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "1#",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = previewTextPrimary
                                                    )
                                                    Text(
                                                        "|",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Backspace Button
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (hasCustomBg) (if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.70f)) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                border = previewBorder,
                                                modifier = Modifier.size((28 * heightScale).coerceIn(24f, 36f).dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Outlined.Backspace,
                                                        contentDescription = null,
                                                        tint = previewIconTint,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height((2 * heightScale).dp))

                                        // 4 Keypad Rows with actual Digits AND Sub-letters
                                        val keyData = listOf(
                                            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                                            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                                            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                                            listOf("*" to "", "0" to "+", "#" to "")
                                        )
                                        val keyH = (26 * heightScale).coerceIn(20f, 36f).dp
                                        val keyW = if (isCircleButtons) keyH else 58.dp
                                        val keyShape = if (isCircleButtons) CircleShape else RoundedCornerShape(13.dp)

                                        for (row in keyData) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for ((digit, sub) in row) {
                                                    Surface(
                                                        shape = keyShape,
                                                        color = previewKeyBg,
                                                        border = previewBorder,
                                                        modifier = Modifier
                                                            .width(keyW)
                                                            .height(keyH)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = digit,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = previewKeyText,
                                                                lineHeight = 12.sp
                                                            )
                                                            if (sub.isNotBlank()) {
                                                                Text(
                                                                    text = sub,
                                                                    fontSize = 5.5.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    letterSpacing = 0.4.sp,
                                                                    color = previewKeySubText,
                                                                    lineHeight = 6.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height((2 * heightScale).dp))

                                        // Action Row (Add Contact pill, Green Call Pill, empty balance space)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Add Contact Button
                                            Surface(
                                                shape = keyShape,
                                                color = previewKeyBg,
                                                border = previewBorder,
                                                modifier = Modifier
                                                    .width(keyW)
                                                    .height(keyH)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Outlined.PersonAdd,
                                                        contentDescription = null,
                                                        tint = previewIconTint,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            // Green Call Button (realistic call pill)
                                            Surface(
                                                shape = if (isCircleButtons) CircleShape else RoundedCornerShape(percent = 50),
                                                color = Color(0xFF2E7D32),
                                                shadowElevation = 2.dp,
                                                modifier = Modifier
                                                    .width(if (isCircleButtons) keyH else 82.dp)
                                                    .height(keyH)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.Call,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            // Spacer to balance Add Contact button
                                            Box(
                                                modifier = Modifier
                                                    .width(keyW)
                                                    .height(keyH)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── SLIDER CONTAINER: NUMBERPAD HEIGHT ──────────────────────────
            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "Number Pad Height",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF2196F3).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Height,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Dialpad Container Height",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Scale dialpad buttons and container",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Reset button on top right corner of the container with icon only
                                IconButton(
                                    onClick = {
                                        heightScale = 1.0f
                                        prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, 1.0f)
                                    },
                                    enabled = heightScale != 1.0f,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.RestartAlt,
                                        contentDescription = "Reset height to 100%",
                                        tint = if (heightScale != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val newScale = (((heightScale * 100f).roundToInt() - 1) / 100f).coerceIn(0.80f, 1.30f)
                                        heightScale = newScale
                                        prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, newScale)
                                    },
                                    enabled = heightScale > 0.80f,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease height by 1%", modifier = Modifier.size(16.dp))
                                }

                                Slider(
                                    value = heightScale,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        heightScale = rounded
                                        prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, rounded)
                                    },
                                    valueRange = 0.80f..1.30f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val newScale = (((heightScale * 100f).roundToInt() + 1) / 100f).coerceIn(0.80f, 1.30f)
                                        heightScale = newScale
                                        prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, newScale)
                                    },
                                    enabled = heightScale < 1.30f,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase height by 1%", modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${(heightScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── BUTTONS STYLE CONTAINER ─────────────────────────────────────
            RivoAnimatedSection(delayMs = 60L) {
                Column {
                    Text(
                        "Buttons Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Circle buttons",
                            supporting = "Use circular buttons on the dialpad",
                            leadingIcon = Icons.Outlined.Lens,
                            iconContainerColor = Color(0xFF9C27B0),
                            checked = isCircleButtons,
                            onCheckedChange = {
                                isCircleButtons = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, it)
                            }
                        )
                    }
                }
            }

            // ── CUSTOM BACKGROUND CONTAINER ─────────────────────────────────
            RivoAnimatedSection(delayMs = 80L) {
                Column {
                    Text(
                        "Custom Background",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        val bgLabel = when (bgType) {
                            "wallpaper" -> "Device Wallpaper"
                            "picture" -> "Custom Picture"
                            "video" -> "Custom Video"
                            else -> "None (Default)"
                        }

                        RivoListItem(
                            headline = "Dialpad Background",
                            supporting = "Currently: $bgLabel",
                            leadingIcon = Icons.Outlined.Wallpaper,
                            iconContainerColor = Color(0xFF9C27B0),
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { showOptionsPopup = true }
                        )

                        if (hasCustomBg) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (bgType != "wallpaper") {
                                    OutlinedButton(
                                        onClick = {
                                            bgFile?.let { file ->
                                                editorMediaState = Triple(file, bgType == "video", bgType)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit / Adjust")
                                    }
                                }

                                Button(
                                    onClick = {
                                        bgType = "none"
                                        prefs.setString(PreferenceManager.KEY_DIALPAD_BG_TYPE, "none")
                                        prefs.setString(PreferenceManager.KEY_DIALPAD_BG_PATH, "")
                                        BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                                        Toast.makeText(context, "Default background restored", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Background Option Popup
    if (showOptionsPopup) {
        CustomBackgroundOptionsPopup(
            target = CustomBackgroundTarget.DIALPAD,
            currentType = bgType,
            dialogTitle = "Choose Dialpad Background",
            dialogSubtitle = "Personalise the dialpad with wallpaper, photo, or video",
            noneSubtitle = "Default solid dialpad theme",
            onDismiss = { showOptionsPopup = false },
            onSelectNone = {
                bgType = "none"
                prefs.setString(PreferenceManager.KEY_DIALPAD_BG_TYPE, "none")
                prefs.setString(PreferenceManager.KEY_DIALPAD_BG_PATH, "")
                prefs.setFloat(PreferenceManager.KEY_DIALPAD_BG_LUMINANCE, -1f)
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                Toast.makeText(context, "Default background applied", Toast.LENGTH_SHORT).show()
                showOptionsPopup = false
            },
            onSelectWallpaper = {
                bgType = "wallpaper"
                bgPath = ""
                prefs.setString(PreferenceManager.KEY_DIALPAD_BG_TYPE, "wallpaper")
                prefs.setString(PreferenceManager.KEY_DIALPAD_BG_PATH, "")
                prefs.setFloat(PreferenceManager.KEY_DIALPAD_BG_LUMINANCE, -1f)
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                Toast.makeText(context, "Current device wallpaper applied", Toast.LENGTH_SHORT).show()
                showOptionsPopup = false
            },
            onOpenEditor = { file, isVideo, type ->
                editorMediaState = Triple(file, isVideo, type)
                showOptionsPopup = false
            }
        )
    }

    // Media Editor Dialog (Zoom, Pan, Dim, Blur, Speed)
    editorMediaState?.let { (file, isVideo, type) ->
        CustomBackgroundEditorDialog(
            target = CustomBackgroundTarget.DIALPAD,
            mediaFile = file,
            isVideo = isVideo,
            bgType = type,
            prefixOverride = "dialpad",
            initialZoom = bgZoom,
            initialPanX = bgPanX,
            initialPanY = bgPanY,
            initialDim = bgDim,
            initialBlur = bgBlur,
            initialVideoSpeed = bgVideoSpeed,
            onDismiss = {
                scope.launch(Dispatchers.IO) {
                    BackgroundMediaManager.cleanupFileIfInCache(context, file)
                }
                editorMediaState = null
            },
            onSaveSuccess = {
                bgType = type
                bgPath = prefs.getString(PreferenceManager.KEY_DIALPAD_BG_PATH, "") ?: ""
                bgZoom = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_ZOOM, 1f)
                bgPanX = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_PAN_X, 0f)
                bgPanY = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_PAN_Y, 0f)
                bgDim = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_DIM, 0f)
                bgBlur = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_BLUR, 0f)
                bgVideoSpeed = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_VIDEO_SPEED, 1.0f)
                editorMediaState = null
                BackgroundMediaManager.autoCleanInBackground(context, prefs)
            }
        )
    }
}

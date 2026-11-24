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

    val isCircleButtons = remember(settingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, false)
    }

    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

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

                    // Phone frame mockup container
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .height(440.dp)
                            .shadow(12.dp, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Custom Background Layer
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
                                        color = if (hasCustomBg) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(width = 28.dp, height = 3.dp)
                                    ) {}
                                }

                                // Search Bar Mockup
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (hasCustomBg) Color.Black.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(0.5.dp, if (hasCustomBg) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                                            tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Search contacts...",
                                            fontSize = 9.sp,
                                            color = if (hasCustomBg) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                }

                                // Sample Contact Result Mockup
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (hasCustomBg) Color.Black.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(0.5.dp, if (hasCustomBg) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "A",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Alex Morgan",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                "+1 234 567 8900",
                                                fontSize = 7.5.sp,
                                                color = if (hasCustomBg) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Numberpad Container Card (scales with heightScale)
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (hasCustomBg) Color.Black.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(0.5.dp, if (hasCustomBg) Color.White.copy(alpha = 0.15f) else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 5.dp,
                                                end = 5.dp,
                                                top = (4 * heightScale).dp,
                                                bottom = (8 * heightScale).dp
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy((3 * heightScale).dp)
                                    ) {
                                        // Typing Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((22 * heightScale).coerceIn(18f, 28f).dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (hasCustomBg) Color.Black.copy(alpha = 0.20f)
                                                    else MaterialTheme.colorScheme.surfaceContainer
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "987 654 3210",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Keypad Rows (1-2-3, 4-5-6, 7-8-9, *-0-#)
                                        val rows = listOf(
                                            listOf("1", "2", "3"),
                                            listOf("4", "5", "6"),
                                            listOf("7", "8", "9"),
                                            listOf("*", "0", "#")
                                        )
                                        val keyH = (19 * heightScale).coerceIn(15f, 25f).dp
                                        val keyCorner = if (isCircleButtons) 50 else 30

                                        for (row in rows) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                for (digit in row) {
                                                    Surface(
                                                        shape = RoundedCornerShape(percent = keyCorner),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (hasCustomBg) 0.82f else 1f),
                                                        modifier = Modifier
                                                            .width(if (isCircleButtons) keyH else 56.dp)
                                                            .height(keyH)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = digit,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Action Row (Add Contact, Call, Backspace)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Add Contact Mockup
                                            Surface(
                                                shape = RoundedCornerShape(percent = keyCorner),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (hasCustomBg) 0.82f else 1f),
                                                modifier = Modifier
                                                    .width(if (isCircleButtons) keyH else 56.dp)
                                                    .height(keyH)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Outlined.PersonAdd,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            // Call Button
                                            Surface(
                                                shape = RoundedCornerShape(percent = keyCorner),
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier
                                                    .width(if (isCircleButtons) keyH else 56.dp)
                                                    .height(keyH)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.Call,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            // Backspace Mockup
                                            Surface(
                                                shape = RoundedCornerShape(percent = keyCorner),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (hasCustomBg) 0.82f else 1f),
                                                modifier = Modifier
                                                    .width(if (isCircleButtons) keyH else 56.dp)
                                                    .height(keyH)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Outlined.Backspace,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(11.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${(heightScale * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Slider(
                                value = heightScale,
                                onValueChange = {
                                    val rounded = (it * 100f).roundToInt() / 100f
                                    heightScale = rounded
                                    prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, rounded)
                                },
                                valueRange = 0.80f..1.30f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "80% (Compact)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        heightScale = 1.0f
                                        prefs.setFloat(PreferenceManager.KEY_DIALPAD_HEIGHT_SCALE, 1.0f)
                                    },
                                    enabled = heightScale != 1.0f
                                ) {
                                    Icon(
                                        Icons.Outlined.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset (100%)", style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    "130% (Tall)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)
                Toast.makeText(context, "Default background applied", Toast.LENGTH_SHORT).show()
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

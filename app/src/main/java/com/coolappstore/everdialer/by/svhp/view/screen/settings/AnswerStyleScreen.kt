package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AnswerStyleScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val isDark = isSystemInDarkTheme()

    val settingsVersion by prefs.settingsChanged.collectAsState()

    val currentAnswerStyle = remember(settingsVersion) {
        prefs.getString(PreferenceManager.KEY_INCOMING_ANSWER_STYLE, PreferenceManager.ANSWER_STYLE_MODERN)
            ?: PreferenceManager.ANSWER_STYLE_MODERN
    }

    // Incoming background preferences
    val bgType = remember(settingsVersion) { prefs.getString(PreferenceManager.KEY_INCOMING_BG_TYPE, "none") ?: "none" }
    val bgPath = remember(settingsVersion) { prefs.getString(PreferenceManager.KEY_INCOMING_BG_PATH, "") ?: "" }
    val bgZoom = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_ZOOM, 1f) }
    val bgPanX = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_X, 0f) }
    val bgPanY = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_Y, 0f) }
    val bgDim = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_DIM, 0f) }
    val bgBlur = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_BLUR, 0f) }
    val bgVideoSpeed = remember(settingsVersion) { prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_VIDEO_SPEED, 1.0f) }
    val showContactPfp = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, true) }
    val showPhoneNumber = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_PHONE_NUMBER, true) }

    val bgFile = remember(bgPath) { if (bgPath.isNotEmpty()) File(bgPath) else null }
    val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

    val elementsThemeMode = remember(settingsVersion) {
        prefs.getString(PreferenceManager.KEY_INCOMING_ELEMENTS_THEME, "auto") ?: "auto"
    }
    val isIncomingElementsDark = when (elementsThemeMode) {
        "light" -> false
        "dark" -> true
        else -> isDark
    }

    val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
    val solidIcons = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
    val solidIconsDarkStyle = remember(settingsVersion, isDark) { prefs.getSolidIconsStyle(isDark) }
    val isSaturatedSolidBrightDark = (isDark || isIncomingElementsDark) && isSaturatedActive && solidIcons && (solidIconsDarkStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT)

    val previewElemBg = when {
        isSaturatedActive -> MaterialTheme.colorScheme.primary
        isIncomingElementsDark -> if (hasCustomBg) Color.Black.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh
        else -> colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f)
    }
    val previewElemFg = when {
        isSaturatedSolidBrightDark -> Color.Black
        isSaturatedActive -> MaterialTheme.colorScheme.onPrimary
        isIncomingElementsDark -> Color.White
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val previewHandleBg = if (isSaturatedSolidBrightDark) Color.Black else if (isIncomingElementsDark && hasCustomBg) Color.White else (if (isIncomingElementsDark) Color(0xFF383A40) else Color.White)

    val effectiveTextColor = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface
    val textShadow = if (hasCustomBg) androidx.compose.ui.graphics.Shadow(
        color = Color.Black.copy(alpha = 0.80f),
        blurRadius = 8f,
        offset = androidx.compose.ui.geometry.Offset(0f, 2f)
    ) else null

    // Idle bounce animation for classic style hint in preview
    val infiniteTransition = rememberInfiniteTransition(label = "previewClassicBounce")
    val previewBounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "previewBounceY"
    )

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Answer style",
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
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Live Preview Mockup Frame ────────────────────────────
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

                            // Camera Punch Hole
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = 7.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )

                            // Incoming Call UI Mockup
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 22.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Caller Info Area
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    if (showContactPfp) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(if (hasCustomBg) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }

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
                                            "+1 (555) 019-2834",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                shadow = textShadow,
                                                fontSize = 10.sp
                                            ),
                                            color = effectiveTextColor.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }

                                    Text(
                                        "Incoming call",
                                        color = effectiveTextColor.copy(alpha = 0.65f),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, shadow = textShadow)
                                    )
                                }

                                // ── Answer Gesture Preview Area ───────────
                                if (currentAnswerStyle == PreferenceManager.ANSWER_STYLE_CLASSIC) {
                                    // ── Google Classic (LineageOS vertical swipe) ──
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        // Quick message pill
                                        Surface(
                                            shape = CircleShape,
                                            color = previewElemBg,
                                            modifier = Modifier.height(24.dp).width(80.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.ChatBubble, null, tint = previewElemFg, modifier = Modifier.size(10.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Message", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold))
                                            }
                                        }

                                        // Vertical swipe area
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Swipe up hint
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.graphicsLayer { translationY = previewBounceY }
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowUp, null, tint = previewElemFg, modifier = Modifier.size(13.dp))
                                                Text("Swipe up to answer", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold))
                                            }

                                            // Center Puck
                                            Surface(
                                                shape = CircleShape,
                                                color = previewHandleBg,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            // Swipe down hint
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Swipe down to decline", color = previewElemFg, style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold))
                                                Icon(Icons.Default.KeyboardArrowDown, null, tint = previewElemFg, modifier = Modifier.size(13.dp))
                                            }
                                        }
                                    }
                                } else {
                                    // ── Google Modern Style (Horizontal swipe pill) ──
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        // Quick message pill
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
                            }
                        }
                    }
                }
            }

            // ── Style Options Section ────────────────────────────────
            RivoAnimatedSection(delayMs = 25L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Answer Styles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )

                    RivoExpressiveCard {
                        // Option 1: Google modern style (Default)
                        val isModernSelected = currentAnswerStyle == PreferenceManager.ANSWER_STYLE_MODERN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_INCOMING_ANSWER_STYLE, PreferenceManager.ANSWER_STYLE_MODERN)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isModernSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Swipe,
                                        contentDescription = null,
                                        tint = if (isModernSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Google modern style",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Default • Swipe horizontally to answer or decline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isModernSelected,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_INCOMING_ANSWER_STYLE, PreferenceManager.ANSWER_STYLE_MODERN)
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Option 2: Google classic (LineageOS style)
                        val isClassicSelected = currentAnswerStyle == PreferenceManager.ANSWER_STYLE_CLASSIC
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_INCOMING_ANSWER_STYLE, PreferenceManager.ANSWER_STYLE_CLASSIC)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isClassicSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.SwipeVertical,
                                        contentDescription = null,
                                        tint = if (isClassicSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Google classic",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "LineageOS style • Swipe up or down to answer or decline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isClassicSelected,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_INCOMING_ANSWER_STYLE, PreferenceManager.ANSWER_STYLE_CLASSIC)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

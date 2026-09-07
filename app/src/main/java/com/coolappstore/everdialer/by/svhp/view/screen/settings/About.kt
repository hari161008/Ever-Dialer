package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.view.components.DonateOptionDialog
import com.coolappstore.everdialer.by.svhp.EVERLASTING_TWEAK_URL
import com.coolappstore.everdialer.by.svhp.GITHUB_URL
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.TELEGRAM_CHANNEL_URL
import com.coolappstore.everdialer.by.svhp.TELEGRAM_DEV_URL
import com.coolappstore.everdialer.by.svhp.TELEGRAM_SUPPORT_URL
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.openLink
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton
import com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur

private val ColorBlue     = Color(0xFF2196F3)
private val ColorGreen    = Color(0xFF4CAF50)
private val ColorDeepPurp = Color(0xFF7C4DFF)
private val ColorOrange   = Color(0xFFFF9800)
private val ColorCyan     = Color(0xFF00BCD4)
private val ColorTeal     = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AboutAppScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    // Hidden easter egg: tapping "Made By Hari :)" 3 times quickly toggles a secret flag that hides
    // the Rate and Review section (and its heading) from Settings, and also hides the manual
    // "Hide Rate And Review" toggle in Appearance so there's no visible trace of it :)
    var hariTapCount by remember { mutableStateOf(0) }
    var lastHariTapTime by remember { mutableStateOf(0L) }
    val selectedAppNameKey = prefs.getString(PreferenceManager.KEY_APP_NAME_PRESET, "default") ?: "default"
    val displayAppName = buildAppNamePresets(context).firstOrNull { it.key == selectedAppNameKey }?.label
        ?.substringBefore(" (Default)")
        ?: com.coolappstore.everdialer.by.svhp.APP_NAME
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showBmacDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.75f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(450),
        label = "logoAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
                title = "About Ever Dialer",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSearchEntryPoint(navigator = navigator)

            // ── Unified Hero Card with Material Expressive Dual-Pane Shape ────
            RivoAnimatedSection(delayMs = 40L) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top row: Dual-pane header (App Icon on left, Title & Badges on right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Left pane: App Icon with expressive container
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(scale)
                                    .alpha(alpha),
                                shadowElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(R.mipmap.ic_launcher)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Ever Dialer Icon",
                                        modifier = Modifier.size(68.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            // Right pane: Title, Tagline and Badges
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = displayAppName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "Modern. Fast. Reliable.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(4.dp))

                                // Chips row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Version chip
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "v$APP_VERSION",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Open Source badge
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.VerifiedUser,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "GPL-3.0",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom row: Quick stats row (App Build & License)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutStatCard(
                                label = "Installed Build",
                                value = "v$APP_VERSION",
                                icon = Icons.Outlined.PhoneAndroid,
                                modifier = Modifier.weight(1f)
                            )
                            AboutStatCard(
                                label = "License",
                                value = "GNU GPL v3",
                                icon = Icons.Outlined.Code,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Developer & Project Section ────────────────────────────
            RivoAnimatedSection(delayMs = 120L) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RivoSectionHeader(title = "Developer & Source")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Made By Hari :)",
                            supporting = "Lead Developer · Contact on Telegram",
                            leadingIcon = Icons.Outlined.Person,
                            iconContainerColor = ColorBlue,
                            modifier = Modifier.settingsSearchHighlight("made_by_hari", highlightedKey) { highlightedKey = null },
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastHariTapTime > 1500L) hariTapCount = 0
                                lastHariTapTime = now
                                hariTapCount++
                                if (hariTapCount >= 3) {
                                    hariTapCount = 0
                                    val newSecretState = !prefs.getBoolean(
                                        PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET,
                                        false
                                    )
                                    prefs.setBoolean(
                                        PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET,
                                        newSecretState
                                    )
                                }
                                openLink(context, TELEGRAM_DEV_URL)
                            }
                        )
                    }

                    // Huge modern Donate container with animated Mount Fuji vector elements in the background
                    DonateContainer(
                        modifier = Modifier.settingsSearchHighlight("donate", highlightedKey) { highlightedKey = null },
                        onDonateClick = { showDonateDialog = true },
                        onBuyMeACoffeeClick = { showBmacDialog = true }
                    )

                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Source Code",
                            supporting = "GitHub Repository · Free & Open Source",
                            leadingIcon = Icons.Outlined.Code,
                            iconContainerColor = ColorGreen,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.settingsSearchHighlight("source_code", highlightedKey) { highlightedKey = null },
                            onClick = { openLink(context, GITHUB_URL) }
                        )
                    }
                }
            }

            // ── Community & Support Links Section ───────────────────────
            RivoAnimatedSection(delayMs = 190L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoSectionHeader(title = "Community & Ecosystem")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Telegram App Support Group",
                            supporting = "Bug Reports | Feature Requests | Announcements | Support",
                            leadingIcon = Icons.Outlined.Groups,
                            iconContainerColor = ColorDeepPurp,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.settingsSearchHighlight("telegram_support", highlightedKey) { highlightedKey = null },
                            onClick = { openLink(context, TELEGRAM_SUPPORT_URL) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        RivoListItem(
                            headline = "App Recommending Channel in Telegram",
                            supporting = "Discover | Explore | Cool Apps | Updates",
                            leadingIcon = Icons.Outlined.StarOutline,
                            iconContainerColor = ColorCyan,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.settingsSearchHighlight("telegram_channel", highlightedKey) { highlightedKey = null },
                            onClick = { openLink(context, TELEGRAM_CHANNEL_URL) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        RivoListItem(
                            headline = "My Other App (Everlasting Android Tweak)",
                            supporting = "Tweaks | System Tools | Modify | Customize",
                            leadingIcon = Icons.Outlined.Build,
                            iconContainerColor = ColorTeal,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.settingsSearchHighlight("other_app_link", highlightedKey) { highlightedKey = null },
                            onClick = { openLink(context, EVERLASTING_TWEAK_URL) }
                        )
                    }
                }
            }

            // ── Subtle Footer ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ever Dialer • Material You • Built with ♥",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        if (showDonateDialog) {
            DonateOptionDialog(
                title = "Donate",
                description = "Choose how you'd like to open the donation page:",
                icon = Icons.Default.Favorite,
                iconTint = MaterialTheme.colorScheme.primary,
                onDismiss = { showDonateDialog = false },
                onOpenBrowser = {
                    showDonateDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DonateWebViewConfig.DEFAULT_DONATE_URL)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                onOpenInApp = {
                    showDonateDialog = false
                    DonateWebViewConfig.targetUrl = DonateWebViewConfig.DEFAULT_DONATE_URL
                    DonateWebViewConfig.targetTitle = "Donate"
                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.DonateWebViewScreenDestination)
                }
            )
        }

        if (showBmacDialog) {
            DonateOptionDialog(
                title = "Buy Me A Coffee",
                description = "Choose how you'd like to open Buy Me A Coffee:",
                icon = Icons.Filled.LocalCafe,
                iconTint = Color(0xFFFF9800),
                onDismiss = { showBmacDialog = false },
                onOpenBrowser = {
                    showBmacDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DonateWebViewConfig.BUY_ME_A_COFFEE_URL)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                onOpenInApp = {
                    showBmacDialog = false
                    DonateWebViewConfig.targetUrl = DonateWebViewConfig.BUY_ME_A_COFFEE_URL
                    DonateWebViewConfig.targetTitle = "Buy Me A Coffee"
                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.DonateWebViewScreenDestination)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonateContainer(
    modifier: Modifier = Modifier,
    onDonateClick: () -> Unit,
    onBuyMeACoffeeClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "donateLandscapeMotion")
    val cloud1Offset by infiniteTransition.animateFloat(
        initialValue = -140f,
        targetValue = 750f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud1"
    )
    val cloud2Offset by infiniteTransition.animateFloat(
        initialValue = -180f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud2"
    )
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )
    val mistOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mistOffset"
    )
    val petalProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "petals"
    )

    val primaryCol = MaterialTheme.colorScheme.primary
    val secondaryCol = MaterialTheme.colorScheme.secondary
    val tertiaryCol = MaterialTheme.colorScheme.tertiary
    val surfaceCol = MaterialTheme.colorScheme.surfaceContainerHigh
    val isDark = isSystemInDarkTheme()

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
        ) {
            // ── Background Multi-Layer 2D Mount Fuji Landscape ──
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
            ) {
                val w = size.width
                val h = size.height

                // 1. Subtle Atmospheric Radial Glow (no hard circles)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryCol.copy(alpha = if (isDark) 0.22f else 0.14f),
                            tertiaryCol.copy(alpha = if (isDark) 0.10f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.24f),
                        radius = h * 0.75f
                    ),
                    center = Offset(w * 0.82f, h * 0.24f),
                    radius = h * 0.75f
                )

                // 2. Celestial 4-Point Sparkle Stars in the sky
                fun drawSparkleStar(cx: Float, cy: Float, starSize: Float, alpha: Float) {
                    val sPath = Path().apply {
                        moveTo(cx, cy - starSize)
                        quadraticTo(cx, cy, cx + starSize, cy)
                        quadraticTo(cx, cy, cx, cy + starSize)
                        quadraticTo(cx, cy, cx - starSize, cy)
                        quadraticTo(cx, cy, cx, cy - starSize)
                        close()
                    }
                    drawPath(sPath, color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
                }

                drawSparkleStar(w * 0.58f, h * 0.12f, 7f, starTwinkle * 0.75f)
                drawSparkleStar(w * 0.74f, h * 0.08f, 9f, (1.25f - starTwinkle) * 0.85f)
                drawSparkleStar(w * 0.92f, h * 0.14f, 6.5f, starTwinkle * 0.65f)
                drawSparkleStar(w * 0.84f, h * 0.26f, 5f, (1.1f - starTwinkle) * 0.55f)

                // 3. Drifting Minimalist 2D Vector Clouds
                fun drawCloud(x: Float, y: Float, scale: Float, alpha: Float) {
                    val cColor = (if (isDark) Color(0xFFE0E5EC) else Color.White).copy(alpha = alpha)
                    val r = 14f * scale
                    drawCircle(color = cColor, center = Offset(x, y), radius = r)
                    drawCircle(color = cColor, center = Offset(x + r * 1.25f, y - r * 0.38f), radius = r * 1.35f)
                    drawCircle(color = cColor, center = Offset(x + r * 2.6f, y), radius = r * 0.95f)
                    drawRoundRect(
                        color = cColor,
                        topLeft = Offset(x - r * 0.5f, y),
                        size = androidx.compose.ui.geometry.Size(r * 3.6f, r * 1.15f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                    )
                }

                drawCloud((cloud1Offset % (w + 260f)) - 130f, h * 0.16f, 1.05f, if (isDark) 0.14f else 0.22f)
                drawCloud(((cloud2Offset + 200f) % (w + 280f)) - 140f, h * 0.32f, 0.75f, if (isDark) 0.09f else 0.15f)
                drawCloud(((cloud1Offset * 0.65f + 340f) % (w + 260f)) - 120f, h * 0.07f, 0.6f, if (isDark) 0.07f else 0.12f)

                // 4. Distant Background Mountain Ridge (atmospheric depth)
                val distPeakX = w * 0.64f
                val distPeakY = h * 0.42f
                val distPath = Path().apply {
                    moveTo(w * 0.42f, h + 10f)
                    cubicTo(
                        w * 0.50f, h * 0.78f,
                        w * 0.56f, h * 0.55f,
                        distPeakX, distPeakY
                    )
                    cubicTo(
                        w * 0.70f, h * 0.50f,
                        w * 0.76f, h * 0.60f,
                        w * 0.88f, distPeakY + h * 0.10f
                    )
                    lineTo(w * 1.10f, h + 10f)
                    close()
                }
                drawPath(
                    distPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tertiaryCol.copy(alpha = if (isDark) 0.24f else 0.15f),
                            surfaceCol.copy(alpha = if (isDark) 0.45f else 0.30f)
                        ),
                        startY = distPeakY,
                        endY = h
                    )
                )

                // 5. Majestic Foreground Mount Fuji (Faceted 2D Vector)
                val baseLeft = w * 0.46f
                val baseRight = w * 1.15f
                val peakLeft = w * 0.77f
                val peakMid = w * 0.81f
                val peakRight = w * 0.86f
                val peakTop = h * 0.18f
                val baseMid = w * 0.78f
                val baseBottom = h + 15f

                // Left Facet (Sunlit Slope)
                val leftSlopePath = Path().apply {
                    moveTo(baseLeft, baseBottom)
                    cubicTo(
                        w * 0.56f, h * 0.82f,
                        w * 0.68f, h * 0.48f,
                        peakLeft, peakTop
                    )
                    lineTo(peakMid, peakTop)
                    cubicTo(
                        w * 0.80f, h * 0.45f,
                        w * 0.79f, h * 0.75f,
                        baseMid, baseBottom
                    )
                    close()
                }
                drawPath(
                    leftSlopePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryCol.copy(alpha = if (isDark) 0.38f else 0.25f),
                            surfaceCol.copy(alpha = if (isDark) 0.58f else 0.42f)
                        ),
                        startY = peakTop,
                        endY = baseBottom
                    )
                )

                // Right Facet (Shaded Slope)
                val rightSlopePath = Path().apply {
                    moveTo(baseMid, baseBottom)
                    cubicTo(
                        w * 0.79f, h * 0.75f,
                        w * 0.80f, h * 0.45f,
                        peakMid, peakTop
                    )
                    lineTo(peakRight, peakTop + h * 0.015f)
                    cubicTo(
                        w * 0.94f, h * 0.50f,
                        w * 1.04f, h * 0.80f,
                        baseRight, baseBottom
                    )
                    close()
                }
                drawPath(
                    rightSlopePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            secondaryCol.copy(alpha = if (isDark) 0.48f else 0.34f),
                            surfaceCol.copy(alpha = if (isDark) 0.72f else 0.54f)
                        ),
                        startY = peakTop,
                        endY = baseBottom
                    )
                )

                // 6. Crisp 2D Vector Snow Cap with geometric crevasses
                // Left Snow Facet (bright white)
                val leftSnowPath = Path().apply {
                    moveTo(peakLeft, peakTop)
                    lineTo(peakMid, peakTop)
                    lineTo(peakMid, peakTop + h * 0.17f)
                    lineTo(w * 0.79f, peakTop + h * 0.14f)
                    lineTo(w * 0.77f, peakTop + h * 0.19f)
                    lineTo(w * 0.75f, peakTop + h * 0.13f)
                    lineTo(w * 0.73f, peakTop + h * 0.18f)
                    close()
                }
                drawPath(
                    leftSnowPath,
                    color = Color.White.copy(alpha = if (isDark) 0.80f else 0.95f)
                )

                // Right Snow Facet (shaded snow)
                val rightSnowPath = Path().apply {
                    moveTo(peakMid, peakTop)
                    lineTo(peakRight, peakTop + h * 0.015f)
                    lineTo(w * 0.90f, peakTop + h * 0.18f)
                    lineTo(w * 0.87f, peakTop + h * 0.13f)
                    lineTo(w * 0.85f, peakTop + h * 0.19f)
                    lineTo(w * 0.83f, peakTop + h * 0.14f)
                    lineTo(peakMid, peakTop + h * 0.17f)
                    close()
                }
                drawPath(
                    rightSnowPath,
                    color = (if (isDark) Color(0xFFD6E4F0) else Color(0xFFEBF2F8)).copy(alpha = if (isDark) 0.65f else 0.80f)
                )

                // 7. Stylized 2D Evergreen / Pine Trees along the foothills
                fun drawPineTree(x: Float, baseY: Float, treeW: Float, treeH: Float, color: Color) {
                    val trunkW = treeW * 0.18f
                    val trunkH = treeH * 0.18f
                    drawRect(
                        color = color.copy(alpha = color.alpha * 0.85f),
                        topLeft = Offset(x - trunkW / 2, baseY - trunkH),
                        size = androidx.compose.ui.geometry.Size(trunkW, trunkH)
                    )
                    // Tier 3
                    val p3 = Path().apply {
                        moveTo(x, baseY - treeH * 0.70f)
                        lineTo(x + treeW / 2, baseY - trunkH)
                        lineTo(x - treeW / 2, baseY - trunkH)
                        close()
                    }
                    drawPath(p3, color = color)
                    // Tier 2
                    val p2 = Path().apply {
                        moveTo(x, baseY - treeH * 0.85f)
                        lineTo(x + treeW * 0.42f, baseY - treeH * 0.35f)
                        lineTo(x - treeW * 0.42f, baseY - treeH * 0.35f)
                        close()
                    }
                    drawPath(p2, color = color)
                    // Tier 1 (top)
                    val p1 = Path().apply {
                        moveTo(x, baseY - treeH)
                        lineTo(x + treeW * 0.30f, baseY - treeH * 0.55f)
                        lineTo(x - treeW * 0.30f, baseY - treeH * 0.55f)
                        close()
                    }
                    drawPath(p1, color = color)
                }

                val treeBaseColor = if (isDark) Color(0xFF1E3329) else Color(0xFF2E533F)
                val treeTint = treeBaseColor.copy(alpha = if (isDark) 0.32f else 0.22f)
                val treeTintAlt = treeBaseColor.copy(alpha = if (isDark) 0.22f else 0.15f)

                drawPineTree(w * 0.52f, h * 0.98f, 18f, 30f, treeTintAlt)
                drawPineTree(w * 0.58f, h * 0.94f, 22f, 36f, treeTint)
                drawPineTree(w * 0.65f, h * 0.99f, 26f, 44f, treeTintAlt)
                drawPineTree(w * 0.72f, h * 0.92f, 22f, 38f, treeTint)
                drawPineTree(w * 0.79f, h * 0.98f, 28f, 48f, treeTint)
                drawPineTree(w * 0.86f, h * 0.92f, 24f, 40f, treeTintAlt)
                drawPineTree(w * 0.93f, h * 0.96f, 26f, 44f, treeTint)
                drawPineTree(w * 1.01f, h * 0.93f, 20f, 34f, treeTintAlt)

                // 8. Drifting Foothill Mist Ribbons
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            surfaceCol.copy(alpha = if (isDark) 0.40f else 0.28f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(w * 0.48f + mistOffset, h * 0.84f),
                    size = androidx.compose.ui.geometry.Size(w * 0.56f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            surfaceCol.copy(alpha = if (isDark) 0.50f else 0.35f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(w * 0.55f - mistOffset * 0.7f, h * 0.92f),
                    size = androidx.compose.ui.geometry.Size(w * 0.52f, 14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                )

                // 9. Floating Cherry Blossom (Sakura) Petals & Warm Sparks
                val petalColors = listOf(
                    Color(0xFFFF80AB),
                    Color(0xFFFF4081),
                    Color(0xFFFFB2DD),
                    Color(0xFFFFD54F)
                )
                for (i in 0..5) {
                    val pT = (petalProgress + i * 0.17f) % 1.0f
                    val pX = w * (0.95f - pT * 0.55f) + kotlin.math.sin(pT * 6.28f + i) * 22f
                    val pY = h * 0.06f + pT * (h * 0.88f)
                    val pAlpha = (kotlin.math.sin(pT * 3.14f) * (if (isDark) 0.65f else 0.78f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = petalColors[i % petalColors.size].copy(alpha = pAlpha),
                        center = Offset(pX, pY),
                        radius = 3.2f + (i % 3) * 1.2f
                    )
                }
            }

            // ── Foreground Modern Material You Content ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SUPPORT DEVELOPMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Text(
                    text = "Donate",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Support this open-source project and future updates. Every contribution matters!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.68f)
                )

                Spacer(Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDonateClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Contribute",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    BuyMeACoffeeButton(
                        onClick = onBuyMeACoffeeClick
                    )
                }
            }
        }
    }
}

/**
 * Premium animated "Buy Me A Coffee" button with:
 * - Dynamic Steaming Coffee Cup icon (smooth sinusoidal steam curls)
 * - Satin shimmer sweep gliding across the surface periodically
 * - Tactile spring-based press physics
 * - Warm golden-amber gradient harmonized with Material You
 */
@Composable
fun BuyMeACoffeeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth spring press scale micro-interaction
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bmacPressScale"
    )

    // Specular satin shimmer sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "bmacShimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bmacShimmerSweep"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.28f)
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFDD00),
                            Color(0xFFFFC000),
                            Color(0xFFFFA500)
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    // Satin sheen sweep reflection band
                    if (shimmerPhase in -0.3f..1.3f) {
                        val sweepWidth = size.width * 0.45f
                        val startX = size.width * shimmerPhase - sweepWidth / 2
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + sweepWidth, size.height)
                        )
                        drawRect(brush = shimmerBrush)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Steaming Coffee Cup Icon with animated steam wisps
                SteamingCoffeeCup(
                    modifier = Modifier.size(18.dp),
                    cupColor = Color(0xFF231600)
                )

                Text(
                    text = "Buy Me A Coffee",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF231600),
                    letterSpacing = 0.2.sp
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF231600),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Custom vector Coffee Cup with delicately animated rising steam wisps.
 */
@Composable
fun SteamingCoffeeCup(
    modifier: Modifier = Modifier,
    cupColor: Color = Color(0xFF231600)
) {
    val steamTransition = rememberInfiniteTransition(label = "steamTransition")
    val steamTime by steamTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Coffee Cup Body (occupies bottom portion: from y = h * 0.44 to h * 0.94)
        val cupTop = h * 0.44f
        val cupBottom = h * 0.94f
        val cupLeft = w * 0.10f
        val cupRight = w * 0.72f
        val cupWidth = cupRight - cupLeft

        val cupPath = Path().apply {
            moveTo(cupLeft, cupTop)
            lineTo(cupRight, cupTop)
            cubicTo(
                cupRight, cupBottom,
                cupRight - cupWidth * 0.15f, cupBottom,
                cupLeft + cupWidth * 0.5f, cupBottom
            )
            cubicTo(
                cupLeft + cupWidth * 0.15f, cupBottom,
                cupLeft, cupBottom,
                cupLeft, cupTop
            )
            close()
        }
        drawPath(cupPath, color = cupColor)

        // Cup Handle (on the right side)
        val handleCenterY = cupTop + (cupBottom - cupTop) * 0.42f
        val handleRadius = (cupBottom - cupTop) * 0.32f
        drawArc(
            color = cupColor,
            startAngle = -75f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cupRight - handleRadius * 0.4f, handleCenterY - handleRadius),
            size = androidx.compose.ui.geometry.Size(handleRadius * 1.35f, handleRadius * 2f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Saucer Base Line
        drawLine(
            color = cupColor,
            start = Offset(cupLeft - w * 0.05f, cupBottom + 1.dp.toPx()),
            end = Offset(cupRight + w * 0.08f, cupBottom + 1.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Rising Animated Steam Wisps
        val wispOffsets = listOf(0.30f to 0.0f, 0.52f to 0.5f)
        for ((xRatio, phaseOffset) in wispOffsets) {
            val progress = (steamTime + phaseOffset) % 1.0f
            val currentY = cupTop - progress * (cupTop * 0.95f)
            val sway = kotlin.math.sin(progress * 6.28318f) * (w * 0.09f)
            val currentX = w * xRatio + sway
            val alpha = (kotlin.math.sin(progress * 3.14159f)).coerceIn(0f, 1f) * 0.85f

            val steamWispPath = Path().apply {
                val segmentLen = cupTop * 0.35f
                moveTo(currentX, currentY + segmentLen * 0.5f)
                cubicTo(
                    currentX - sway * 0.6f, currentY + segmentLen * 0.2f,
                    currentX + sway * 0.6f, currentY - segmentLen * 0.2f,
                    currentX, currentY - segmentLen * 0.5f
                )
            }
            drawPath(
                path = steamWispPath,
                color = cupColor.copy(alpha = alpha),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun AboutStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

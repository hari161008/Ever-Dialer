package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

private val ColorBlue     = Color(0xFF2196F3)
private val ColorGreen    = Color(0xFF4CAF50)
private val ColorDeepPurp = Color(0xFF7C4DFF)
private val ColorOrange   = Color(0xFFFF9800)
private val ColorCyan     = Color(0xFF00BCD4)
private val ColorTeal     = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
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
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("About Ever Dialer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
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

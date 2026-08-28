package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.EVERLASTING_TWEAK_URL
import com.coolappstore.everdialer.by.svhp.GITHUB_URL
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.TELEGRAM_CHANNEL_URL
import com.coolappstore.everdialer.by.svhp.TELEGRAM_DEV_URL
import com.coolappstore.everdialer.by.svhp.TELEGRAM_SUPPORT_URL
import com.coolappstore.everdialer.by.svhp.controller.util.openLink
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val ColorBlue      = Color(0xFF2196F3)
private val ColorGreen     = Color(0xFF4CAF50)
private val ColorDeepPurp  = Color(0xFF7C4DFF)
private val ColorOrange    = Color(0xFFFF9800)
private val ColorCyan      = Color(0xFF00BCD4)
private val ColorTeal      = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AboutAppScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val context = LocalContext.current
    val prefs = org.koin.compose.koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager>()
    // Hidden easter egg: tapping this row 3 times quickly toggles a secret flag that hides
    // the Rate and Review section (and its heading) from Settings, and also hides the manual
    // "Hide Rate And Review" toggle in Appearance so there's no visible trace of it :)
    var hariTapCount by remember { mutableStateOf(0) }
    var lastHariTapTime by remember { mutableStateOf(0L) }
    val selectedAppNameKey = prefs.getString(
        com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_APP_NAME_PRESET, "default"
    ) ?: "default"
    val displayAppName = buildAppNamePresets(context).firstOrNull { it.key == selectedAppNameKey }?.label
        ?.substringBefore(" (Default)")
        ?: com.coolappstore.everdialer.by.svhp.APP_NAME
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.65f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "logoAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("About Ever Dialer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
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
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp + navBarBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)
            Spacer(modifier = Modifier.height(20.dp))

            // ── App Icon (plain, no surrounding elements) ────────────
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.mipmap.ic_launcher)
                    .crossfade(true)
                    .build(),
                contentDescription = "Ever Dialer",
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .alpha(alpha),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                displayAppName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(alpha)
            )
            Text(
                "Modern. Fast. Reliable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Version badge
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "v$APP_VERSION",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Developer & Source Code card ──────────────────────────
            RivoAnimatedSection(delayMs = 150L) {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Made By Hari :)",
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
                                    com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET,
                                    false
                                )
                                prefs.setBoolean(
                                    com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET,
                                    newSecretState
                                )
                            }
                            openLink(context, TELEGRAM_DEV_URL)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    RivoListItem(
                        headline = "Source Code",
                        supporting = "GitHub Repository",
                        leadingIcon = Icons.Outlined.Code,
                        iconContainerColor = ColorGreen,
                        modifier = Modifier.settingsSearchHighlight("source_code", highlightedKey) { highlightedKey = null },
                        onClick = { openLink(context, GITHUB_URL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Community links card ──────────────────────────────────
            RivoAnimatedSection(delayMs = 220L) {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Telegram App Support Group",
                        supporting = "Bug Reports | Feature request | Announcements | Support",
                        leadingIcon = Icons.Outlined.Groups,
                        iconContainerColor = ColorDeepPurp,
                        modifier = Modifier.settingsSearchHighlight("telegram_support", highlightedKey) { highlightedKey = null },
                        onClick = { openLink(context, TELEGRAM_SUPPORT_URL) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    RivoListItem(
                        headline = "App Recommending Channel in Telegram",
                        supporting = "Discover | Explore | Cool Apps | Support",
                        leadingIcon = Icons.Outlined.StarOutline,
                        iconContainerColor = ColorCyan,
                        modifier = Modifier.settingsSearchHighlight("telegram_channel", highlightedKey) { highlightedKey = null },
                        onClick = { openLink(context, TELEGRAM_CHANNEL_URL) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    RivoListItem(
                        headline = "My Other App (Everlasting Android Tweak)",
                        supporting = "Tweaks | Tools | Modify | Customize",
                        leadingIcon = Icons.Outlined.Build,
                        iconContainerColor = ColorTeal,
                        modifier = Modifier.settingsSearchHighlight("other_app_link", highlightedKey) { highlightedKey = null },
                        onClick = { openLink(context, EVERLASTING_TWEAK_URL) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.*
import com.coolappstore.everdialer.by.svhp.liquidglass.backdrops.layerBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.backdrops.rememberLayerBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import com.coolappstore.everdialer.by.svhp.controller.util.LiquidGlassElementConfig
import com.coolappstore.everdialer.by.svhp.controller.util.BlurElementConfig

private data class EffectElement(
    val key: String,
    val headline: String,
    val supporting: String,
    val icon: ImageVector,
    val iconColor: Color
)

private val LG_ELEMENTS = listOf(
    EffectElement(
        key = PreferenceManager.KEY_LG_BOTTOM_NAV,
        headline = "Bottom Navigation Bar",
        supporting = "Apply liquid glass to the pill-style bottom navigation bar",
        icon = Icons.Outlined.ViewStream,
        iconColor = Color(0xFF00BCD4)
    ),
    EffectElement(
        key = PreferenceManager.KEY_LG_DROPDOWN_MENU,
        headline = "Dropdown Menu",
        supporting = "Apply liquid glass to context and overflow dropdown menus",
        icon = Icons.Outlined.MoreVert,
        iconColor = Color(0xFF9C27B0)
    ),
    EffectElement(
        key = PreferenceManager.KEY_LG_DIALPAD_CALL_BUTTON,
        headline = "Dialpad Call Button",
        supporting = "Apply liquid glass to the call button on the dialpad",
        icon = Icons.Outlined.Dialpad,
        iconColor = Color(0xFF4CAF50)
    ),
    EffectElement(
        key = PreferenceManager.KEY_LG_CONTACTS_FAB,
        headline = "Contacts Add Button",
        supporting = "Apply liquid glass to the add contact floating action button",
        icon = Icons.Outlined.PersonAdd,
        iconColor = Color(0xFF2196F3)
    ),
    EffectElement(
        key = PreferenceManager.KEY_LG_RECENTS_FAB,
        headline = "Recents Dialpad Button",
        supporting = "Apply liquid glass to the dialpad button on recents screen",
        icon = Icons.Outlined.History,
        iconColor = Color(0xFFFF9800)
    ),
)

private val BLUR_ELEMENTS = listOf(
    EffectElement(
        key = PreferenceManager.KEY_BLUR_BOTTOM_NAV,
        headline = "Bottom Navigation Bar",
        supporting = "Apply blur to the pill-style bottom navigation bar",
        icon = Icons.Outlined.ViewStream,
        iconColor = Color(0xFF00BCD4)
    ),
    EffectElement(
        key = PreferenceManager.KEY_BLUR_DROPDOWN_MENU,
        headline = "Dropdown Menu",
        supporting = "Apply blur to context and overflow dropdown menus",
        icon = Icons.Outlined.MoreVert,
        iconColor = Color(0xFF9C27B0)
    ),
    EffectElement(
        key = PreferenceManager.KEY_BLUR_DIALPAD_CALL_BUTTON,
        headline = "Dialpad Call Button",
        supporting = "Apply blur to the call button on the dialpad",
        icon = Icons.Outlined.Dialpad,
        iconColor = Color(0xFF4CAF50)
    ),
    EffectElement(
        key = PreferenceManager.KEY_BLUR_CONTACTS_FAB,
        headline = "Contacts Add Button",
        supporting = "Apply blur to the add contact floating action button",
        icon = Icons.Outlined.PersonAdd,
        iconColor = Color(0xFF2196F3)
    ),
    EffectElement(
        key = PreferenceManager.KEY_BLUR_RECENTS_FAB,
        headline = "Recents Dialpad Button",
        supporting = "Apply blur to the dialpad button on recents screen",
        icon = Icons.Outlined.History,
        iconColor = Color(0xFFFF9800)
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun LiquidGlassElementsScreen(
    navigator: DestinationsNavigator,
    initialTab: Int = 0
) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(initialTab) }

    var isLgMainEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false))
    }
    var isBlurMainEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false))
    }

    val lgStates = remember {
        LG_ELEMENTS.associate { el ->
            el.key to mutableStateOf(prefs.getBoolean(el.key, true))
        }
    }
    val blurStates = remember {
        BLUR_ELEMENTS.associate { el ->
            el.key to mutableStateOf(prefs.getBoolean(el.key, true))
        }
    }

    var configuringLgElement by remember { mutableStateOf<EffectElement?>(null) }
    var configuringBlurElement by remember { mutableStateOf<EffectElement?>(null) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
                title = "Liquid & Blur Effects",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + navBarBottom)
        ) {

            // ── Pill tabs on top ──────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "Material You Liquid Glass" to Icons.Outlined.Lens,
                            "Blur Effects" to Icons.Outlined.BlurOn
                        )
                        tabs.forEachIndexed { index, (label, icon) ->
                            val isSelected = selectedTab == index
                            val bgAnim by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "pillTabBg_$index"
                            )
                            val fgAnim by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "pillTabFg_$index"
                            )
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = bgAnim,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable {
                                        if (selectedTab != index) {
                                            performAppHaptic(
                                                context,
                                                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                                            )
                                            selectedTab = index
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = fgAnim, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = fgAnim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ── Liquid Glass Tab ──────────────────────────────────────────
                item(key = "lg_master") {
                    RivoAnimatedSection(delayMs = 20L) {
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Material Liquid You Glass",
                                supporting = "Apply a liquid glass refraction effect across selected UI elements",
                                leadingIcon = Icons.Outlined.Lens,
                                iconContainerColor = Color(0xFF00BCD4),
                                checked = isLgMainEnabled,
                                onCheckedChange = { newValue ->
                                    isLgMainEnabled = newValue
                                    prefs.setBoolean(PreferenceManager.KEY_LIQUID_GLASS, newValue)
                                }
                            )
                        }
                    }
                }

                item(key = "lg_header") {
                    RivoAnimatedSection(delayMs = 30L) {
                        Text(
                            text = "Elements",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                }

                item(key = "lg_elements") {
                    RivoAnimatedSection(delayMs = 40L) {
                        RivoExpressiveCard {
                            LG_ELEMENTS.forEachIndexed { index, element ->
                                val checked by lgStates[element.key]!!
                                RivoSwitchListItem(
                                    headline = element.headline,
                                    supporting = element.supporting,
                                    leadingIcon = element.icon,
                                    iconContainerColor = element.iconColor,
                                    checked = checked,
                                    enabled = isLgMainEnabled,
                                    onCheckedChange = { newValue ->
                                        lgStates[element.key]!!.value = newValue
                                        prefs.setBoolean(element.key, newValue)
                                    },
                                    onArrowClick = {
                                        configuringLgElement = element
                                    }
                                )
                                if (index < LG_ELEMENTS.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isLgMainEnabled) 0.5f else 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Blur Effects Tab ──────────────────────────────────────────
                item(key = "blur_master") {
                    RivoAnimatedSection(delayMs = 20L) {
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Material Blur Effects",
                                supporting = "Apply frosted glass blur across selected UI elements",
                                leadingIcon = Icons.Outlined.BlurOn,
                                iconContainerColor = Color(0xFF5C6BC0),
                                checked = isBlurMainEnabled,
                                onCheckedChange = { newValue ->
                                    isBlurMainEnabled = newValue
                                    prefs.setBoolean(PreferenceManager.KEY_BLUR_EFFECTS, newValue)
                                }
                            )
                        }
                    }
                }

                item(key = "blur_header") {
                    RivoAnimatedSection(delayMs = 30L) {
                        Text(
                            text = "Elements",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                }

                item(key = "blur_elements") {
                    RivoAnimatedSection(delayMs = 40L) {
                        RivoExpressiveCard {
                            BLUR_ELEMENTS.forEachIndexed { index, element ->
                                val checked by blurStates[element.key]!!
                                RivoSwitchListItem(
                                    headline = element.headline,
                                    supporting = element.supporting,
                                    leadingIcon = element.icon,
                                    iconContainerColor = element.iconColor,
                                    checked = checked,
                                    enabled = isBlurMainEnabled,
                                    onCheckedChange = { newValue ->
                                        blurStates[element.key]!!.value = newValue
                                        prefs.setBoolean(element.key, newValue)
                                    },
                                    onArrowClick = {
                                        configuringBlurElement = element
                                    }
                                )
                                if (index < BLUR_ELEMENTS.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isBlurMainEnabled) 0.5f else 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (configuringLgElement != null) {
        LiquidGlassConfigDialog(
            element = configuringLgElement!!,
            prefs = prefs,
            onDismiss = { configuringLgElement = null }
        )
    }

    if (configuringBlurElement != null) {
        BlurConfigDialog(
            element = configuringBlurElement!!,
            prefs = prefs,
            onDismiss = { configuringBlurElement = null }
        )
    }
}

@Composable
private fun EffectElementPreview(
    element: EffectElement,
    blurRadius: Float,
    refractionHeight: Float? = null,
    refractionAmount: Float? = null,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = false,
    saturation: Float,
    brightness: Float = 0f
) {
    val previewBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current.density
    val isAndroidS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val previewShape = remember(element.key) {
        when {
            element.key.contains("fab") || element.key.contains("dialpad_button") -> CircleShape
            element.key.contains("nav") || element.key.contains("action") || element.key.contains("call_button") -> RoundedCornerShape(22.dp)
            else -> RoundedCornerShape(16.dp)
        }
    }

    val previewSizeModifier = remember(element.key) {
        when {
            element.key.contains("fab") || element.key.contains("dialpad_button") -> Modifier.size(56.dp)
            element.key.contains("nav") || element.key.contains("action") || element.key.contains("call_button") -> Modifier.height(44.dp).fillMaxWidth(0.72f)
            else -> Modifier.height(48.dp).fillMaxWidth(0.82f)
        }
    }

    val effectModifier = if (isAndroidS) {
        Modifier.drawBackdrop(
            backdrop = previewBackdrop,
            shape = { previewShape },
            effects = {
                colorControls(brightness = brightness, saturation = saturation)
                if (blurRadius > 0f) blur(blurRadius * density)
                if (refractionHeight != null && refractionAmount != null) {
                    lens(
                        refractionHeight = refractionHeight * density,
                        refractionAmount = refractionAmount * density,
                        chromaticAberration = chromaticAberration,
                        depthEffect = depthEffect
                    )
                }
            },
            highlight = { Highlight.Default }
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        // Background sample layer with rich text and shapes to show off distortion & blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3F51B5),
                            Color(0xFF009688),
                            Color(0xFFFF9800),
                            Color(0xFFE91E63)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EVER DIALER • 123 456 7890",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Sample background text behind ${element.headline.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-time Live Preview",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFFEB3B)))
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFE91E63)))
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    }
                }
            }
        }

        // Overlay element placed on top
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = previewShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                tonalElevation = 2.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .then(previewSizeModifier)
                    .then(effectModifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        element.icon,
                        contentDescription = null,
                        tint = element.iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!element.key.contains("fab") && !element.key.contains("dialpad_button")) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = element.headline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassConfigDialog(
    element: EffectElement,
    prefs: PreferenceManager,
    onDismiss: () -> Unit
) {
    val initialConfig = remember(element.key) { prefs.getLiquidGlassConfig(element.key) }
    var blurRadius by remember(element.key) { mutableFloatStateOf(initialConfig.blurRadiusDp) }
    var refractionHeight by remember(element.key) { mutableFloatStateOf(initialConfig.refractionHeightDp) }
    var refractionAmount by remember(element.key) { mutableFloatStateOf(initialConfig.refractionAmountDp) }
    var saturation by remember(element.key) { mutableFloatStateOf(initialConfig.saturation) }
    var brightness by remember(element.key) { mutableFloatStateOf(initialConfig.brightness) }
    var chromaticAberration by remember(element.key) { mutableStateOf(initialConfig.chromaticAberration) }
    var depthEffect by remember(element.key) { mutableStateOf(initialConfig.depthEffect) }

    fun syncPrefs() {
        prefs.setLiquidGlassConfig(
            element.key,
            LiquidGlassElementConfig(
                blurRadiusDp = blurRadius,
                refractionHeightDp = refractionHeight,
                refractionAmountDp = refractionAmount,
                chromaticAberration = chromaticAberration,
                depthEffect = depthEffect,
                saturation = saturation,
                brightness = brightness
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    element.icon,
                    contentDescription = null,
                    tint = element.iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${element.headline} Glass",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live visual preview with element on top of background text
                EffectElementPreview(
                    element = element,
                    blurRadius = blurRadius,
                    refractionHeight = refractionHeight,
                    refractionAmount = refractionAmount,
                    chromaticAberration = chromaticAberration,
                    depthEffect = depthEffect,
                    saturation = saturation,
                    brightness = brightness
                )

                // Blur Radius
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blur Radius", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.1f", blurRadius)} dp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = blurRadius,
                        onValueChange = {
                            blurRadius = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0f..32f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Refraction Height
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Refraction Height", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.1f", refractionHeight)} dp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = refractionHeight,
                        onValueChange = {
                            refractionHeight = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Refraction Amount
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Refraction Amount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.1f", refractionAmount)} dp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = refractionAmount,
                        onValueChange = {
                            refractionAmount = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0f..300f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Saturation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.2f", saturation)}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Brightness
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Brightness", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            String.format("%.2f", brightness),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = -0.5f..0.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Chromatic Aberration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chromatic Aberration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Color dispersion on refraction edges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = chromaticAberration,
                        onCheckedChange = {
                            chromaticAberration = it
                            syncPrefs()
                        }
                    )
                }

                // Depth Effect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Depth Effect", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Simulates 3D curved lens refraction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = depthEffect,
                        onCheckedChange = {
                            depthEffect = it
                            syncPrefs()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    prefs.resetLiquidGlassConfig(element.key)
                    val def = prefs.getLiquidGlassDefaultConfig(element.key)
                    blurRadius = def.blurRadiusDp
                    refractionHeight = def.refractionHeightDp
                    refractionAmount = def.refractionAmountDp
                    saturation = def.saturation
                    brightness = def.brightness
                    chromaticAberration = def.chromaticAberration
                    depthEffect = def.depthEffect
                }
            ) {
                Text("Reset")
            }
        }
    )
}

@Composable
private fun BlurConfigDialog(
    element: EffectElement,
    prefs: PreferenceManager,
    onDismiss: () -> Unit
) {
    val initialConfig = remember(element.key) { prefs.getBlurConfig(element.key) }
    var blurRadius by remember(element.key) { mutableFloatStateOf(initialConfig.blurRadiusDp) }
    var saturation by remember(element.key) { mutableFloatStateOf(initialConfig.saturation) }

    fun syncPrefs() {
        prefs.setBlurConfig(
            element.key,
            BlurElementConfig(
                blurRadiusDp = blurRadius,
                saturation = saturation
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    element.icon,
                    contentDescription = null,
                    tint = element.iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${element.headline} Blur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live visual preview with element on top of background text
                EffectElementPreview(
                    element = element,
                    blurRadius = blurRadius,
                    saturation = saturation
                )

                // Blur Radius
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blur Radius", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.1f", blurRadius)} dp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = blurRadius,
                        onValueChange = {
                            blurRadius = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Saturation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "${String.format("%.2f", saturation)}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            syncPrefs()
                        },
                        onValueChangeFinished = { syncPrefs() },
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    prefs.resetBlurConfig(element.key)
                    val def = prefs.getBlurDefaultConfig(element.key)
                    blurRadius = def.blurRadiusDp
                    saturation = def.saturation
                }
            ) {
                Text("Reset")
            }
        }
    )
}


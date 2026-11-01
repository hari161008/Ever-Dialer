package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
}

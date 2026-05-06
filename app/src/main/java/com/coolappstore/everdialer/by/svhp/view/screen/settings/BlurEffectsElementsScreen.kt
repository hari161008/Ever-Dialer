package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private data class BlurElement(
    val key: String,
    val headline: String,
    val supporting: String,
    val icon: ImageVector,
    val iconColor: Color
)

private val BLUR_ELEMENTS = listOf(
    BlurElement(
        key = PreferenceManager.KEY_BLUR_BOTTOM_NAV,
        headline = "Bottom Navigation Bar",
        supporting = "Apply blur to the pill-style bottom navigation bar",
        icon = Icons.Outlined.ViewStream,
        iconColor = Color(0xFF00BCD4)
    ),
    BlurElement(
        key = PreferenceManager.KEY_BLUR_DROPDOWN_MENU,
        headline = "Dropdown Menu",
        supporting = "Apply blur to context and overflow dropdown menus",
        icon = Icons.Outlined.MoreVert,
        iconColor = Color(0xFF9C27B0)
    ),
    BlurElement(
        key = PreferenceManager.KEY_BLUR_DIALPAD_CALL_BUTTON,
        headline = "Dialpad Call Button",
        supporting = "Apply blur to the call button on the dialpad",
        icon = Icons.Outlined.Dialpad,
        iconColor = Color(0xFF4CAF50)
    ),
    BlurElement(
        key = PreferenceManager.KEY_BLUR_CONTACTS_FAB,
        headline = "Contacts Add Button",
        supporting = "Apply blur to the add contact floating action button",
        icon = Icons.Outlined.PersonAdd,
        iconColor = Color(0xFF2196F3)
    ),
    BlurElement(
        key = PreferenceManager.KEY_BLUR_RECENTS_FAB,
        headline = "Recents Dialpad Button",
        supporting = "Apply blur to the dialpad button on recents screen",
        icon = Icons.Outlined.History,
        iconColor = Color(0xFFFF9800)
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BlurEffectsElementsScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()

    val states = remember {
        BLUR_ELEMENTS.associate { el ->
            el.key to mutableStateOf(prefs.getBoolean(el.key, false))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blur Effect Elements") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Text(
                        text = "Select which elements use the blur effect. Requires \"Material Blur Effects\" to be enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }

            item {
                RivoAnimatedSection(delayMs = 40L) {
                    RivoExpressiveCard {
                        BLUR_ELEMENTS.forEachIndexed { index, element ->
                            val checked by states[element.key]!!
                            RivoSwitchListItem(
                                headline = element.headline,
                                supporting = element.supporting,
                                leadingIcon = element.icon,
                                iconContainerColor = element.iconColor,
                                checked = checked,
                                onCheckedChange = { newValue ->
                                    states[element.key]!!.value = newValue
                                    prefs.setBoolean(element.key, newValue)
                                }
                            )
                            if (index < BLUR_ELEMENTS.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

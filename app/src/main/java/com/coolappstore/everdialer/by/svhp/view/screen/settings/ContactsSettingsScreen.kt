package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PfpShapeChooserDialog
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PfpShapeRegistry
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

private val ColorBlue   = Color(0xFF2196F3)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ContactsSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var showPfpShapeDialog by remember { mutableStateOf(false) }

    var contactsPfpShape by remember(settingsVer) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_CONTACTS_PFP_SHAPE, "circle") ?: "circle")
    }
    var animateShapeRotation by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACTS_PFP_SHAPE_ANIMATE, false))
    }

    if (showContactsToDisplayDialog) {
        val contactsVM: ContactsViewModel = koinActivityViewModel()
        val accounts by contactsVM.availableAccounts.collectAsState()
        val groups by contactsVM.contactGroups.collectAsState()
        val selectedAccountKey by contactsVM.selectedAccountKey.collectAsState()
        val selectedGroupId by contactsVM.selectedGroupId.collectAsState()
        val contacts by contactsVM.allContacts.collectAsState()

        LaunchedEffect(Unit) {
            contactsVM.fetchAvailableAccounts()
            contactsVM.fetchContactGroups()
        }

        ContactsToDisplaySheet(
            accounts = accounts,
            groups = groups,
            selectedAccountKey = selectedAccountKey,
            selectedGroupId = selectedGroupId,
            totalCount = accounts.sumOf { it.contactCount }.takeIf { it > 0 } ?: contacts.size,
            onSelectAccount = { key ->
                contactsVM.setAccountFilter(key)
                showContactsToDisplayDialog = false
            },
            onSelectGroup = { groupId ->
                contactsVM.setGroupFilter(groupId)
                showContactsToDisplayDialog = false
            },
            onDismiss = { showContactsToDisplayDialog = false },
            contactsVM = contactsVM
        )
    }

    if (showPfpShapeDialog) {
        PfpShapeChooserDialog(
            currentShapeKey = contactsPfpShape,
            animateRotation = animateShapeRotation,
            onAnimateRotationChange = {
                animateShapeRotation = it
                prefs.setBoolean(PreferenceManager.KEY_CONTACTS_PFP_SHAPE_ANIMATE, it)
            },
            onSelectShape = {
                contactsPfpShape = it
                prefs.setString(PreferenceManager.KEY_CONTACTS_PFP_SHAPE, it)
            },
            onDismiss = { showPfpShapeDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Contacts",
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
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Display",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Contacts to display",
                            supporting = "Choose which accounts' contacts are shown",
                            leadingIcon = Icons.Outlined.Contacts,
                            iconContainerColor = ColorBlue,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contacts_to_display", highlightedKey) { highlightedKey = null },
                            onClick = { showContactsToDisplayDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 30L) {
                Column {
                    Text(
                        "Profile Picture",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "PFP Shape",
                            supporting = PfpShapeRegistry.getDisplayLabel(contactsPfpShape),
                            leadingIcon = if (contactsPfpShape.contains("square")) Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contacts_pfp_shape", highlightedKey) { highlightedKey = null },
                            onClick = { showPfpShapeDialog = true }
                        )
                    }
                }
            }
        }
    }
}

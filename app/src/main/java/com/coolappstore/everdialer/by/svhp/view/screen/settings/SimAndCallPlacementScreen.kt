package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ColorGreen   = Color(0xFF4CAF50)
private val ColorIndigo  = Color(0xFF3F51B5)
private val ColorTeal    = Color(0xFF009688)
private val ColorAmber   = Color(0xFFFF9800)
private val ColorBlue    = Color(0xFF2196F3)
private val ColorRed     = Color(0xFFE53935)
private val ColorBluGrey = Color(0xFF607D8B)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SimAndCallPlacementScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf { scrollState.value > 0 }
    }

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var defaultSim by remember {
        val saved = prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault())
        mutableStateOf(if (saved == 0 && prefs.getBoolean(PreferenceManager.KEY_USE_SIM_FROM_CALL_LOG, false)) 3 else saved)
    }
    var showSimDialog by remember { mutableStateOf(false) }
    val activeSimCount = remember { prefs.getActiveSimCount() }
    val hasTwoSims = remember {
        activeSimCount >= 2 || run {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            try { (tm?.callCapablePhoneAccounts?.size ?: 0) >= 2 } catch (_: Throwable) { false }
        }
    }
    var showSimButtonsInDialpad by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIM_BUTTONS_IN_DIALPAD, false))
    }
    var confirmPlacingCall by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, false))
    }
    var showSimColorDialog by remember { mutableStateOf(false) }
    var missedCallNotification by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, false))
    }

    if (showContactsToDisplayDialog) {
        val contactsVM: ContactsViewModel = org.koin.compose.viewmodel.koinActivityViewModel()
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

    if (showSimDialog) {
        val simOptions = remember(hasTwoSims) {
            buildList {
                add(0 to "Ask every time")
                add(1 to "SIM 1")
                if (hasTwoSims) {
                    add(2 to "SIM 2")
                    add(3 to "Use SIM based on call logs")
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showSimDialog = false },
            title = { Text("Default SIM") },
            text = {
                Column {
                    simOptions.forEach { (index, label) ->
                        val selectOption = {
                            defaultSim = index
                            prefs.setInt(PreferenceManager.KEY_DEFAULT_SIM, index)
                            prefs.setBoolean(PreferenceManager.KEY_USE_SIM_FROM_CALL_LOG, index == 3)
                            showSimDialog = false
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = selectOption)
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultSim == index,
                                onClick = selectOption
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSimColorDialog) {
        SimColorsCustomizationDialog(
            onDismissRequest = { showSimColorDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Sim And Call Placement",
                onBackClick = { navigator.navigateUp() }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { scrollState.animateScrollTo(0) } }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSearchEntryPoint(navigator = navigator)

            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    SimCallSectionLabel("Accounts & Placement")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Default SIM",
                            supporting = when (defaultSim) {
                                0 -> "Ask every time"
                                1 -> "SIM 1"
                                2 -> "SIM 2"
                                3 -> "Use SIM based on call logs"
                                else -> "Ask every time"
                            },
                            leadingIcon = Icons.Outlined.SimCard,
                            iconContainerColor = ColorGreen,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight(
                                if (highlightedKey == "use_sim_from_call_log") "use_sim_from_call_log" else "default_sim",
                                highlightedKey
                            ) { highlightedKey = null },
                            onClick = { showSimDialog = true }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Confirm placing a call",
                            supporting = "Ask for confirmation before placing any outgoing call",
                            leadingIcon = Icons.Outlined.CheckCircle,
                            iconContainerColor = ColorIndigo,
                            checked = confirmPlacingCall,
                            modifier = Modifier.settingsSearchHighlight("confirm_placing_call", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                confirmPlacingCall = it
                                prefs.setBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, it)
                            }
                        )
                        if (hasTwoSims) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Show SIM buttons instead of dial button",
                                supporting = "Shows SIM 1 and SIM 2 buttons in place of the call button on the dialpad. Useful when a default SIM is set to easily place calls from either SIM without changing defaults.",
                                leadingIcon = Icons.Outlined.Dialpad,
                                iconContainerColor = ColorTeal,
                                checked = showSimButtonsInDialpad,
                                modifier = Modifier.settingsSearchHighlight("show_sim_buttons_in_dialpad", highlightedKey) { highlightedKey = null },
                                onCheckedChange = {
                                    showSimButtonsInDialpad = it
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_SIM_BUTTONS_IN_DIALPAD, it)
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoListItem(
                                headline = "Customize SIM Colors",
                                supporting = "Choose custom colors for SIM 1 and SIM 2",
                                leadingIcon = Icons.Outlined.Palette,
                                iconContainerColor = ColorAmber,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("customize_sim_colors", highlightedKey) { highlightedKey = null },
                                onClick = { showSimColorDialog = true }
                            )
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoListItem(
                            headline = "Contacts to display",
                            supporting = "Choose which accounts' contacts are shown",
                            leadingIcon = Icons.Outlined.Contacts,
                            iconContainerColor = ColorBlue,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contacts_to_display", highlightedKey) { highlightedKey = null },
                            onClick = { showContactsToDisplayDialog = true }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Missed Call Notification",
                            supporting = if (missedCallNotification) "Showing missed call notifications through Ever Dialer" else "Missed call notifications disabled in Ever Dialer",
                            leadingIcon = Icons.AutoMirrored.Filled.CallMissed,
                            iconContainerColor = ColorRed,
                            checked = missedCallNotification,
                            modifier = Modifier.settingsSearchHighlight("missed_call_notification", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                missedCallNotification = it
                                prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, it)
                                if (it) {
                                    com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(context)
                                } else {
                                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)?.cancel(
                                        com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.MISSED_CALLS_NOTIF_ID
                                    )
                                }
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 60L) {
                Column {
                    SimCallSectionLabel("System Accounts")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Open System Additional Settings",
                            supporting = "Manage phone accounts in Android system settings",
                            leadingIcon = Icons.Outlined.Settings,
                            iconContainerColor = ColorBluGrey,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        component = ComponentName(
                                            "com.android.phone",
                                            "com.android.phone.settings.PhoneAccountSettingsActivity"
                                        )
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Couldn't open system settings", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SimCallSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}

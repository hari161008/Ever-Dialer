package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CallAccountsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorGreen   = Color(0xFF4CAF50)
private val ColorTeal    = Color(0xFF009688)
private val ColorAmber   = Color(0xFFFFC107)
private val ColorBlue    = Color(0xFF2196F3)
private val ColorPink    = Color(0xFFE91E63)
private val ColorOrange  = Color(0xFFFF9800)

// ─── Contacts to Display Dialog ───────────────────────────────────────────────

data class ContactSourceItem(
    val key: String,
    val label: String,
    val subLabel: String? = null
)

@Composable
fun ContactsToDisplayDialog(
    onDismiss: () -> Unit,
    prefs: PreferenceManager
) {
    val context = LocalContext.current

    // Build sources list: Google accounts + SIMs + WhatsApp
    val sources = remember {
        val list = mutableListOf<ContactSourceItem>()

        // Google accounts
        try {
            val accountManager = AccountManager.get(context)
            val googleAccounts = accountManager.getAccountsByType("com.google")
            googleAccounts.forEach { account ->
                list.add(ContactSourceItem(
                    key = "google_${account.name}",
                    label = "Google",
                    subLabel = account.name
                ))
            }
            if (googleAccounts.isEmpty()) {
                list.add(ContactSourceItem(key = "google_none", label = "Google", subLabel = "No Google accounts"))
            }
        } catch (_: Exception) {
            list.add(ContactSourceItem(key = "google_none", label = "Google", subLabel = "No Google accounts"))
        }

        // SIM accounts
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(SubscriptionManager::class.java)
                val subs = subManager?.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    subs.forEach { sub ->
                        val simName = sub.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${sub.simSlotIndex + 1}"
                        list.add(ContactSourceItem(
                            key = "sim_${sub.subscriptionId}",
                            label = "SIM",
                            subLabel = simName
                        ))
                    }
                } else {
                    list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
                }
            } else {
                list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
            }
        } catch (_: Exception) {
            list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
        }

        // WhatsApp
        list.add(ContactSourceItem(key = "whatsapp", label = "WhatsApp"))

        list
    }

    // Load saved enabled keys
    val savedKeys = remember {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, null)
        if (raw.isNullOrBlank()) sources.map { it.key }.toSet()
        else raw.split(",").toSet()
    }
    val checkedKeys = remember { mutableStateOf(savedKeys) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contacts to display") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                sources.forEach { source ->
                    val isChecked = source.key in checkedKeys.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                checkedKeys.value = if (checked) {
                                    checkedKeys.value + source.key
                                } else {
                                    checkedKeys.value - source.key
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = source.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (source.subLabel != null) {
                                Text(
                                    text = source.subLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.setString(
                    PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS,
                    checkedKeys.value.joinToString(",")
                )
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallSettingsScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var pocketModePrevention by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) }
    var directCallOnTap by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)) }
    var showContactsToDisplayDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "callSettingsAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    if (showContactsToDisplayDialog) {
        ContactsToDisplayDialog(
            onDismiss = { showContactsToDisplayDialog = false },
            prefs = prefs
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .alpha(screenAlpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Caller Accounts ───────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        CallSettingsSectionLabel("Accounts")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Call Accounts",
                                supporting = "SIM cards and calling accounts",
                                leadingIcon = Icons.Outlined.SimCard,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { navigator.navigate(CallAccountsScreenDestination) }
                            )
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
                                onClick = { showContactsToDisplayDialog = true }
                            )
                        }
                    }
                }
            }

            // ── Call Behavior ─────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        CallSettingsSectionLabel("Call Behavior")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline   = "Proximity Sensor on in background",
                                supporting = "Turn off screen when phone is near ear during a call",
                                leadingIcon = Icons.Outlined.Sensors,
                                iconContainerColor = ColorTeal,
                                checked = proximityBg,
                                onCheckedChange = {
                                    proximityBg = it
                                    prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_BG, it)
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline   = "Pocket Mode Prevention",
                                supporting = "Block accidental answer/decline when phone is in pocket",
                                leadingIcon = Icons.Outlined.Sensors,
                                iconContainerColor = ColorAmber,
                                checked = pocketModePrevention,
                                onCheckedChange = {
                                    pocketModePrevention = it
                                    prefs.setBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, it)
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline   = "Direct Call on Tap",
                                supporting = "Tap a call log entry to call directly instead of viewing contact info",
                                leadingIcon = Icons.Outlined.Call,
                                iconContainerColor = ColorGreen,
                                checked = directCallOnTap,
                                onCheckedChange = {
                                    directCallOnTap = it
                                    prefs.setBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, it)
                                }
                            )
                        }
                    }
                }
            }

            // ── Sound & Vibration ─────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 120L) {
                    Column {
                        CallSettingsSectionLabel("Sound & Vibration")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Sound & Vibration",
                                supporting = "Ringtones and dialpad tones",
                                leadingIcon = Icons.Outlined.VolumeUp,
                                iconContainerColor = ColorBlue,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CallSettingsSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

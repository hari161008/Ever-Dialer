package com.modiprincekr.thedialer.view.screen.settings

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.modal.`interface`.IContactsRepository
import com.modiprincekr.thedialer.modal.data.Contact
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pattern
import com.modiprincekr.thedialer.view.components.RivoAvatar
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.RivoListItem
import com.modiprincekr.thedialer.view.components.RivoSwitchListItem
import com.modiprincekr.thedialer.view.components.settingsSearchHighlight
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Refactored "Passwords & Security" settings screen.
 *
 * Flattens the previous nested bottom sheet hierarchy by directly exposing:
 * - System Biometrics
 * - PIN
 * - Password
 * - Pattern (with inline expandable "Visible Pattern" toggle)
 *
 * Keeps "Lock App on Open" and "Lock Call Actions" toggles at the bottom,
 * enabled only when a lock method is configured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun BiometricScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs: PreferenceManager = koinInject()
    val contactsRepo: IContactsRepository = koinInject()
    val context = LocalContext.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var biometricsType by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: "") }
    var patternVisible by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_PATTERN_VISIBLE, true)) }
    var appLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)) }
    var callLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)) }
    var callLockMode by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "all") ?: "all") }
    var callLockNumbers by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_NUMBERS, "") ?: "") }
    var backupType by remember { mutableStateOf(prefs.getBiometricsBackupType()) }

    var showContactPicker by remember { mutableStateOf(false) }
    var allContacts by remember { mutableStateOf(emptyList<Contact>()) }

    // Active credential setup sheet type: "pin" | "password" | "pattern" | null
    var activeSetupType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        allContacts = withContext(Dispatchers.IO) {
            contactsRepo.getContacts()
        }
    }

    val selectedNumbers = remember(callLockNumbers) {
        if (callLockNumbers.isBlank()) emptySet()
        else callLockNumbers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val selectedContactCount = remember(selectedNumbers, allContacts) {
        allContacts.count { c ->
            c.phoneNumbers.any { n ->
                selectedNumbers.any { s ->
                    n.filter(Char::isDigit).takeLast(10) == s.filter(Char::isDigit).takeLast(10)
                }
            }
        }
    }

    val systemBiometricsAvailable = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar(
                title = "Passwords & Security",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.modiprincekr.thedialer.view.components.SettingsSearchEntryPoint(navigator = navigator)

            // ── DIRECT UNLOCK METHODS LIST (Flattened hierarchy) ──────────────
            Text(
                text = "Unlock Methods",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
            )
            RivoExpressiveCard {
                // 1. System Biometrics Option
                RivoListItem(
                    headline = "System Biometrics",
                    supporting = if (systemBiometricsAvailable) {
                        "Fingerprint or device credentials"
                    } else {
                        "Not available on this device"
                    },
                    leadingIcon = Icons.Default.Fingerprint,
                    iconContainerColor = Color(0xFF6750A4),
                    trailingIcon = if (biometricsType == "system") Icons.Default.CheckCircle else null,
                    modifier = Modifier.settingsSearchHighlight("auth_method_system", highlightedKey) { highlightedKey = null },
                    onClick = {
                        if (systemBiometricsAvailable) {
                            val hasPin = (prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "").isNotEmpty()
                            val hasPattern = (prefs.getString(PreferenceManager.KEY_BIOMETRICS_PATTERN, "") ?: "").isNotEmpty()
                            val hasPassword = (prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "").isNotEmpty()
                            if (!hasPin && !hasPattern && !hasPassword) {
                                Toast.makeText(context, "Please set a PIN, Password, or Pattern first.", Toast.LENGTH_SHORT).show()
                            } else {
                                biometricsType = "system"
                                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "system")
                                if (!appLockEnabled) {
                                    appLockEnabled = true
                                    prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                                }
                            }
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 2. PIN Option
                RivoListItem(
                    headline = "PIN",
                    supporting = if (biometricsType == "pin") "Active unlock PIN" else "Set a numeric PIN",
                    leadingIcon = Icons.Default.Pin,
                    iconContainerColor = Color(0xFF2196F3),
                    trailingIcon = if (biometricsType == "pin") Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    modifier = Modifier.settingsSearchHighlight("auth_method_pin", highlightedKey) { highlightedKey = null },
                    onClick = {
                        activeSetupType = "pin"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 3. Password Option
                RivoListItem(
                    headline = "Password",
                    supporting = if (biometricsType == "password") "Active unlock password" else "Set an alphanumeric password",
                    leadingIcon = Icons.Default.Key,
                    iconContainerColor = Color(0xFF4CAF50),
                    trailingIcon = if (biometricsType == "password") Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    modifier = Modifier.settingsSearchHighlight("auth_method_password", highlightedKey) { highlightedKey = null },
                    onClick = {
                        activeSetupType = "password"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 4. Pattern Option (with expandable visibility toggle)
                Column(modifier = Modifier.animateContentSize()) {
                    RivoListItem(
                        headline = "Pattern",
                        supporting = if (biometricsType == "pattern") "Active pattern lock" else "Draw a 3x3 pattern lock",
                        leadingIcon = Icons.Default.Pattern,
                        iconContainerColor = Color(0xFFFF9800),
                        trailingIcon = if (biometricsType == "pattern") Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                        modifier = Modifier.settingsSearchHighlight("auth_method_pattern", highlightedKey) { highlightedKey = null },
                        onClick = {
                            val hasPattern = (prefs.getString(PreferenceManager.KEY_BIOMETRICS_PATTERN, "") ?: "").isNotEmpty()
                            if (hasPattern && biometricsType != "pattern") {
                                biometricsType = "pattern"
                                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pattern")
                                if (!appLockEnabled) {
                                    appLockEnabled = true
                                    prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                                }
                            } else {
                                activeSetupType = "pattern"
                            }
                        }
                    )

                    // ── Inline Expandable Section for Pattern Visibility ──
                    AnimatedVisibility(
                        visible = biometricsType == "pattern",
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Visible Pattern",
                                supporting = "Make pattern line visible when drawing",
                                leadingIcon = if (patternVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                iconContainerColor = Color(0xFFFF9800).copy(alpha = 0.7f),
                                checked = patternVisible,
                                onCheckedChange = { isChecked ->
                                    patternVisible = isChecked
                                    prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_PATTERN_VISIBLE, isChecked)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoListItem(
                                headline = "Change Pattern",
                                supporting = "Redraw a new unlock pattern",
                                leadingIcon = Icons.Default.Edit,
                                iconContainerColor = Color(0xFFFF9800).copy(alpha = 0.5f),
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    activeSetupType = "pattern"
                                }
                            )
                        }
                    }
                }

                // Option to Remove Lock when one is set
                if (biometricsType.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    RivoListItem(
                        headline = "Remove Lock",
                        supporting = "Turn off app lock and passwords",
                        leadingIcon = Icons.Default.LockOpen,
                        iconContainerColor = MaterialTheme.colorScheme.error,
                        trailingIcon = null,
                        onClick = {
                            biometricsType = ""
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "")
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, "")
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "")
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_PATTERN, "")
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_BACKUP_TYPE, "")
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)
                            appLockEnabled = false
                            callLockEnabled = false
                        }
                    )
                }
            }

            // ── Toggles: Visible & Enabled only when a lock method is configured ──
            AnimatedVisibility(
                visible = biometricsType.isNotEmpty(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Protection Scope",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Lock App on Open",
                            supporting = "Require authentication when opening The Dialer",
                            leadingIcon = Icons.Default.Lock,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = appLockEnabled,
                            modifier = Modifier.settingsSearchHighlight("lock_app_open", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                appLockEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Lock Call Actions",
                            supporting = "Require authentication to answer or reject incoming calls",
                            leadingIcon = Icons.Default.PhonePaused,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = callLockEnabled,
                            modifier = Modifier.settingsSearchHighlight("lock_call_actions", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                callLockEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, it)
                            }
                        )
                    }

                    // ── Call lock scope ────────────────────────────────────
                    AnimatedVisibility(
                        visible = callLockEnabled,
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Lock Scope",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            )

                            // Segmented control
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = callLockMode == "all",
                                    onClick = {
                                        callLockMode = "all"
                                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "all")
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                                ) { Text("All Calls", maxLines = 1) }
                                SegmentedButton(
                                    selected = callLockMode == "specified",
                                    onClick = {
                                        callLockMode = "specified"
                                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "specified")
                                        showContactPicker = true
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                                ) { Text("Specified", maxLines = 1) }
                                SegmentedButton(
                                    selected = callLockMode == "skip_specified",
                                    onClick = {
                                        callLockMode = "skip_specified"
                                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "skip_specified")
                                        showContactPicker = true
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                                ) { Text("Exclude", maxLines = 1) }
                            }

                            // Description card
                            AnimatedContent(targetState = callLockMode, label = "lockModeDesc") { mode ->
                                RivoExpressiveCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        "all" -> Icons.Default.Lock
                                                        "specified" -> Icons.Default.Person
                                                        else -> Icons.Default.PersonOff
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = when (mode) {
                                                "all" -> "Lock required for every incoming call"
                                                "specified" -> if (selectedContactCount > 0)
                                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} will require authentication to answer"
                                                else "Choose contacts that require authentication to answer"
                                                else -> if (selectedContactCount > 0)
                                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} excluded from lock"
                                                else "Choose contacts to skip the authentication lock"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Edit selection button
                            AnimatedVisibility(
                                visible = callLockMode != "all",
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit  = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                FilledTonalButton(
                                    onClick = { showContactPicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (selectedContactCount > 0)
                                            "Edit Selection  ·  $selectedContactCount selected"
                                        else "Select Contacts"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Credential Setup Full-Screen Dialog ─────────────────────────────────
    if (activeSetupType != null) {
        CredentialSetupDialog(
            type = activeSetupType!!,
            isPatternVisible = patternVisible,
            onConfirm = { credential ->
                when (activeSetupType) {
                    "pin" -> {
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, credential)
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_BACKUP_TYPE, "pin")
                        backupType = "pin"
                        if (biometricsType != "system") {
                            biometricsType = "pin"
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pin")
                        }
                        if (!appLockEnabled) {
                            appLockEnabled = true
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                        }
                    }
                    "password" -> {
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, credential)
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_BACKUP_TYPE, "password")
                        backupType = "password"
                        if (biometricsType != "system") {
                            biometricsType = "password"
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "password")
                        }
                        if (!appLockEnabled) {
                            appLockEnabled = true
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                        }
                    }
                    "pattern" -> {
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PATTERN, credential)
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_BACKUP_TYPE, "pattern")
                        backupType = "pattern"
                        if (biometricsType != "system") {
                            biometricsType = "pattern"
                            prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pattern")
                        }
                        if (!appLockEnabled) {
                            appLockEnabled = true
                            prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true)
                        }
                    }
                }
                activeSetupType = null
            },
            onDismiss = {
                activeSetupType = null
            }
        )
    }

    // ── Contact Picker ─────────────────────────────────────────────────────
    if (showContactPicker) {
        ContactPickerDialog(
            contacts = allContacts,
            initialSelectedNumbers = selectedNumbers,
            onDone = { newNumbers ->
                val joined = newNumbers.joinToString(",")
                callLockNumbers = joined
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_NUMBERS, joined)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
}

// ─── Contact Picker Dialog ────────────────────────────────────────────────────

@Composable
private fun ContactPickerDialog(
    contacts: List<Contact>,
    initialSelectedNumbers: Set<String>,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumbers by remember {
        mutableStateOf(initialSelectedNumbers.map { it.filter(Char::isDigit).takeLast(10) }.toSet())
    }
    var searchQuery by remember { mutableStateOf("") }

    fun normalise(n: String) = n.filter(Char::isDigit).takeLast(10)

    fun isContactSelected(contact: Contact) =
        contact.phoneNumbers.any { normalise(it) in selectedNumbers }

    fun toggleContact(contact: Contact) {
        val nums = contact.phoneNumbers.map(::normalise).filter { it.isNotEmpty() }.toSet()
        selectedNumbers = if (isContactSelected(contact)) selectedNumbers - nums
        else selectedNumbers + nums
    }

    fun selectAll() {
        selectedNumbers = contacts.flatMap { c -> c.phoneNumbers.map(::normalise) }.filter { it.isNotEmpty() }.toSet()
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts.sortedBy { it.name.lowercase() }
        else contacts.filter { c ->
            c.name.contains(searchQuery, ignoreCase = true) ||
            c.phoneNumbers.any { it.contains(searchQuery) }
        }.sortedBy { it.name.lowercase() }
    }

    val selectedContactCount = contacts.count(::isContactSelected)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Column {
                        com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar(
                            title = "Select Contacts",
                            onBackClick = onDismiss,
                            actions = {
                                TextButton(onClick = ::selectAll) {
                                    Text("Select All", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search contacts…") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (contacts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredContacts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.SearchOff, contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(filteredContacts, key = { it.id }) { contact ->
                                val checked = isContactSelected(contact)
                                val rowBg by animateColorAsState(
                                    if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    tween(180), label = "cb${contact.id}"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .clickable { toggleContact(contact) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    RivoAvatar(
                                        name = contact.name,
                                        photoUri = contact.photoUri,
                                        modifier = Modifier.size(46.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name.ifBlank { "Unknown" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (contact.phoneNumbers.isNotEmpty()) {
                                            Text(
                                                text = contact.phoneNumbers.first(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { toggleContact(contact) }
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedContactCount > 0,
                        enter = slideInVertically { it } + fadeIn(tween(200)),
                        exit  = slideOutVertically { it } + fadeOut(tween(150)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                    ) {
                        Button(
                            onClick = {
                                val finalNumbers = contacts
                                    .filter(::isContactSelected)
                                    .flatMap { c -> c.phoneNumbers.map { it.filter(Char::isDigit).takeLast(10) } }
                                    .filter { it.isNotEmpty() }
                                    .toSet()
                                onDone(finalNumbers)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(52.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Done  ·  $selectedContactCount selected",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

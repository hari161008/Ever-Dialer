package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.DialogProperties
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BiometricScreen(navigator: DestinationsNavigator) {
    val prefs: PreferenceManager = koinInject()
    val contactsRepo: IContactsRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var biometricsType by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: "") }
    var appLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)) }
    var callLockEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)) }
    var callLockMode by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_MODE, "all") ?: "all") }
    var callLockNumbers by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK_NUMBERS, "") ?: "") }
    var showContactPicker by remember { mutableStateOf(false) }
    var allContacts by remember { mutableStateOf(emptyList<Contact>()) }

    LaunchedEffect(Unit) {
        allContacts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            contactsRepo.getContacts()
        }
    }

    val selectedNumbers = remember(callLockNumbers) {
        if (callLockNumbers.isBlank()) emptySet()
        else callLockNumbers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val selectedContactCount = remember(selectedNumbers, allContacts) {
        allContacts.count { c -> c.phoneNumbers.any { n -> selectedNumbers.any { s -> n.filter(Char::isDigit).takeLast(10) == s.filter(Char::isDigit).takeLast(10) } } }
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPasswordSetup by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible && !isClosing) 1f else 0f,
        animationSpec = if (isClosing) tween(260) else tween(320),
        label = "alpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible && !isClosing) 0.dp else if (isClosing) 40.dp else 24.dp,
        animationSpec = if (isClosing) tween(270) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "offsetY"
    )
    LaunchedEffect(Unit) { visible = true }

    fun navigateBack() {
        isClosing = true
        scope.launch { delay(260); navigator.navigateUp() }
    }

    val systemBiometricsAvailable = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    val typeLabel = when (biometricsType) {
        "system" -> "System Biometrics"
        "pin"    -> "Custom PIN"
        "password" -> "Custom Password"
        else     -> "Not Set"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentication", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .offset(y = offsetY)
                .alpha(alpha)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Authentication Method card ─────────────────────────────────
            SectionLabel("Authentication Method")
            RivoExpressiveCard {
                RivoListItem(
                    headline = "Authentication Method",
                    supporting = typeLabel,
                    leadingIcon = Icons.Default.Fingerprint,
                    iconContainerColor = Color(0xFF6750A4),
                    trailingIcon = Icons.Default.ChevronRight,
                    onClick = { showTypeSheet = true }
                )
            }

            // ── Toggles — only visible when a method is configured ─────────
            AnimatedVisibility(
                visible = biometricsType.isNotEmpty(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Authentication")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Lock App on Open",
                            supporting = "Require authentication when opening Ever Dialer",
                            leadingIcon = Icons.Default.LockOpen,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = appLockEnabled,
                            onCheckedChange = {
                                appLockEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, it)
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline = "Lock Call Actions",
                            supporting = "Require authentication to answer or reject incoming calls",
                            leadingIcon = Icons.Default.PhonePaused,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = callLockEnabled,
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
                            SectionLabel("Lock Scope")

                            // Modern segmented control
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
                                                "all" -> "Biometric required for every incoming call"
                                                "specified" -> if (selectedContactCount > 0)
                                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} will require biometric to answer"
                                                else "Choose contacts that require biometric to answer"
                                                else -> if (selectedContactCount > 0)
                                                    "$selectedContactCount contact${if (selectedContactCount != 1) "s" else ""} excluded from biometric lock"
                                                else "Choose contacts to skip the biometric lock"
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

    // ── Type Chooser Bottom Sheet ──────────────────────────────────────────
    if (showTypeSheet) {
        BiometricTypeSheet(
            systemAvailable = systemBiometricsAvailable,
            currentType = biometricsType,
            onSelect = { type ->
                showTypeSheet = false
                when (type) {
                    "system" -> {
                        biometricsType = "system"
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "system")
                        if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                    }
                    "pin"      -> showPinSetup = true
                    "password" -> showPasswordSetup = true
                    ""         -> {
                        biometricsType = ""
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, "")
                        prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "")
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, false)
                        prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_CALL_LOCK, false)
                        appLockEnabled = false; callLockEnabled = false
                    }
                }
            },
            onDismiss = { showTypeSheet = false }
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onConfirm = { pin ->
                biometricsType = "pin"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "pin")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PIN, pin)
                if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false }
        )
    }

    if (showPasswordSetup) {
        PasswordSetupDialog(
            onConfirm = { password ->
                biometricsType = "password"
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_TYPE, "password")
                prefs.setString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, password)
                if (!appLockEnabled) { appLockEnabled = true; prefs.setBoolean(PreferenceManager.KEY_BIOMETRICS_APP_LOCK, true) }
                showPasswordSetup = false
            },
            onDismiss = { showPasswordSetup = false }
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
    // working copy — normalised to last-10-digit strings for matching
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
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("Select Contacts", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                TextButton(onClick = ::selectAll) {
                                    Text("Select All", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                        // Search bar
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

                    // ── Floating Done pill ─────────────────────────────────
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

// ─── Biometric Type Selector Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiometricTypeSheet(
    systemAvailable: Boolean,
    currentType: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(width = 36.dp, height = 4.dp)
                ) {}
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            // System Biometrics option
            BiometricOptionRow(
                icon = Icons.Default.Fingerprint,
                iconTint = Color(0xFF6750A4),
                title = "System Biometrics",
                subtitle = if (systemAvailable) "Fingerprint, face unlock, or device credentials"
                           else "Not available on this device",
                isSelected = currentType == "system",
                enabled = systemAvailable,
                onClick = { if (systemAvailable) onSelect("system") }
            )

            HorizontalDivider(
                Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Text(
                "Custom Biometrics",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
            )

            BiometricOptionRow(
                icon = Icons.Default.Pin,
                iconTint = Color(0xFF2196F3),
                title = "PIN",
                subtitle = "Set a numeric PIN of any length",
                isSelected = currentType == "pin",
                onClick = { onSelect("pin") }
            )

            BiometricOptionRow(
                icon = Icons.Default.Key,
                iconTint = Color(0xFF4CAF50),
                title = "Password",
                subtitle = "Set a custom alphanumeric password",
                isSelected = currentType == "password",
                onClick = { onSelect("password") }
            )

            if (currentType.isNotEmpty()) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Surface(
                    onClick = { onSelect("") },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Remove Biometric Lock", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BiometricOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else Color.Transparent,
        spring(stiffness = Spring.StiffnessMediumLow), label = "optBg"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit  = scaleOut() + fadeOut()
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── PIN Setup Dialog ────────────────────────────────────────────────────────

@Composable
fun PinDialogContent(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set PIN",
    isVerify: Boolean = false,
    expectedPin: String = "",
    showCloseButton: Boolean = true
) {
    var phase by remember { mutableIntStateOf(if (isVerify) 2 else 0) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var shakeState by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val shake by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = keyframes {
            durationMillis = 400
            0.dp at 0; (-12).dp at 60; 12.dp at 120; (-8).dp at 200; 8.dp at 280; 0.dp at 400
        },
        label = "shake",
        finishedListener = { shakeState = 0 }
    )

    fun vibError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(80, 180))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(80, 180))
            }
        } catch (_: Exception) {}
    }

    fun vibSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(40, 120))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(40, 120))
            }
        } catch (_: Exception) {}
    }

    fun onDigit(d: String) { if (pin.length < 12) pin += d }
    fun onBackspace() { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
    fun onSubmit() {
        when {
            pin.length < 4 -> { shakeState++; vibError() }
            isVerify -> {
                if (pin == expectedPin) { vibSuccess(); onConfirm(pin) }
                else { pin = ""; shakeState++; vibError() }
            }
            phase == 0 -> { firstPin = pin; pin = ""; phase = 1 }
            phase == 1 -> {
                if (pin == firstPin) { vibSuccess(); onConfirm(pin) }
                else { pin = ""; firstPin = ""; phase = 0; shakeState++; vibError() }
            }
        }
    }

    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = when {
                            isVerify -> title
                            phase == 0 -> "Set PIN"
                            else -> "Confirm PIN"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isVerify -> "Enter your PIN to verify"
                            phase == 0 -> "At least 4 digits"
                            else -> "Re-enter to confirm"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showCloseButton) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── PIN dots ────────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = if (shakeState > 0) shake else 0.dp)
        ) {
            val maxDots = pin.length.coerceAtLeast(4).coerceAtMost(12)
            repeat(maxDots) { i ->
                val filled = i < pin.length
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.4f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dot$i"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (filled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(150),
                    label = "dotColor$i"
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(Modifier.height(44.dp))

        // ── Numpad ──────────────────────────────────────────────────────────
        PinNumpad(
            onDigit = ::onDigit,
            onBackspace = ::onBackspace,
            onSubmit = ::onSubmit,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun PinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set PIN",
    isVerify: Boolean = false,
    expectedPin: String = "",
    showCloseButton: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            PinDialogContent(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                title = title,
                isVerify = isVerify,
                expectedPin = expectedPin,
                showCloseButton = showCloseButton
            )
        }
    }
}

@Composable
private fun PinNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "→")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.85f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "ks_$key"
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "⌫" -> Surface(
                                onClick = onBackspace,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(64.dp).scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            "→" -> Surface(
                                onClick = onSubmit,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp).scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Confirm",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            else -> Surface(
                                onClick = { onDigit(key) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(64.dp).scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Medium
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

// ─── Password Setup Dialog ───────────────────────────────────────────────────

@Composable
fun PasswordDialogContent(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set Password",
    isVerify: Boolean = false,
    expectedPassword: String = "",
    showCloseButton: Boolean = true
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Column(
        Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isVerify) title else "Set Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isVerify) "Enter your password to verify" else "Letters, numbers & symbols",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showCloseButton) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorText = "" },
            label = { Text(if (isVerify) "Password" else "Enter Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = errorText.isNotEmpty()
        )

        if (!isVerify) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorText = "" },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = errorText.isNotEmpty()
            )
        }

        AnimatedVisibility(visible = errorText.isNotEmpty()) {
            Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showCloseButton) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            }
            Button(
                onClick = {
                    when {
                        isVerify -> {
                            if (password == expectedPassword) onConfirm(password)
                            else errorText = "Incorrect password"
                        }
                        password.length < 4 -> errorText = "Password must be at least 4 characters"
                        password != confirmPassword -> errorText = "Passwords don't match"
                        else -> onConfirm(password)
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirm", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PasswordSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Set Password",
    isVerify: Boolean = false,
    expectedPassword: String = "",
    showCloseButton: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = showCloseButton,
            dismissOnClickOutside = showCloseButton
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            PasswordDialogContent(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                title = title,
                isVerify = isVerify,
                expectedPassword = expectedPassword,
                showCloseButton = showCloseButton
            )
        }
    }
}


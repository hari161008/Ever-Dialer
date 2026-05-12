package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.view.components.SimPickerDialog
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.tiles.SingleTile
import com.coolappstore.everdialer.by.svhp.view.components.tiles.TileGroup
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Locale
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.drawPlainBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.blur
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import androidx.activity.compose.BackHandler
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun DialPadScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialNumber: String? = null
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // Lock the window so the keyboard never pushes the bottom sheet up.
    // WindowCompat.setDecorFitsSystemWindows(false) in MainActivity normally causes
    // the sheet to resize with the IME; overriding softInputMode here prevents that.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val prevMode = window?.attributes?.softInputMode ?: 0
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            window?.setSoftInputMode(prevMode)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { navigator.navigateUp() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
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
        DialPadContent(
            initialNumber = initialNumber,
            navigator = navigator,
            onDismiss = { navigator.navigateUp() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadContent(
    initialNumber: String? = null,
    navigator: DestinationsNavigator? = null,
    onDismiss: (() -> Unit)? = null,
    showHeader: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val allContacts by contactsVM.allContacts.collectAsState()
    var number by remember { mutableStateOf(initialNumber ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    val soundPool = remember { buildDtmfSoundPool(context) }

    val t9Enabled = prefs.getBoolean(PreferenceManager.KEY_T9_DIALING, true)
    var showSimPicker by remember { mutableStateOf(false) }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    // Helper: place a call respecting the default SIM preference
    fun placeCallWithSimPreference(num: String) {
        val accounts = telecomManager.callCapablePhoneAccounts
        if (accounts.size > 1) {
            val simPref = prefs.getInt("default_sim", 0)
            when {
                simPref == 1 && accounts.size >= 1 -> makeCall(context, num, accounts[0])
                simPref == 2 && accounts.size >= 2 -> makeCall(context, num, accounts[1])
                else -> showSimPicker = true
            }
        } else {
            makeCall(context, num)
        }
    }

    val clipText = remember {
        clipboard.getText()?.text?.filter { it.isDigit() || it == '+' } ?: ""
    }
    var showClipboardBanner by remember { mutableStateOf(clipText.length in 7..15) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var searchFieldFocused by remember { mutableStateOf(false) }

    // When search field is focused, intercept back press to dismiss keyboard and restore dialpad
    BackHandler(enabled = searchFieldFocused) {
        focusManager.clearFocus()
        searchQuery = ""
    }
    var openDialpadDefault by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, true))
    }

    // Search results from search bar
    val searchResults by remember(searchQuery, number, allContacts, t9Enabled) {
        derivedStateOf {
            val q = searchQuery.trim()
            val n = number
            when {
                q.isNotEmpty() -> allContacts.filter { c ->
                    c.name.contains(q, ignoreCase = true) ||
                    c.phoneNumbers.any { it.contains(q) }
                }.take(5)
                n.isNotEmpty() -> allContacts.filter { contact ->
                    val matchesNumber = contact.phoneNumbers.any { it.replace(" ", "").contains(n) }
                    val matchesName = if (t9Enabled) {
                        val t9Name = T9Matcher.convertNameToT9(contact.name)
                        t9Name.contains(n)
                    } else false
                    matchesNumber || matchesName
                }.take(3)
                else -> emptyList()
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (number.isNotEmpty()) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "numberScale"
    )

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (hasPhoneState) {
                placeCallWithSimPreference(number)
            } else makeCall(context, number)
        }
    }

    if (showSimPicker) {
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, number, handle)
                showSimPicker = false
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape: side-by-side layout — left=search+search results, right=dialpad keys+number+actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left panel: search bar + results only
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        showKeyboardOnFocus = false
                    )
                )

                // Number display — below search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (number.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DialpadNumberDisplay(
                        number = number,
                        fontSize = if (number.length > 11) 24 else 30
                    )
                }

                // Search results
                AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                            expandVertically(tween(420, easing = FastOutSlowInEasing)),
                    exit  = fadeOut(tween(280, easing = FastOutLinearInEasing)) +
                            shrinkVertically(tween(320, easing = FastOutLinearInEasing))
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            searchResults.forEach { contact ->
                                SingleTile(
                                    title    = contact.name,
                                    subtitle = contact.phoneNumbers.firstOrNull(),
                                    photoUri = contact.photoUri,
                                    onAvatarClick = {
                                        navigator?.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                    },
                                    onClick  = {
                                        val num = contact.phoneNumbers.firstOrNull() ?: return@SingleTile
                                        makeCall(context, num)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }

            // Right panel: dialpad keys + action buttons below
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))
                    val subKeys = mapOf("1" to "   ","2" to "ABC","3" to "DEF","4" to "GHI","5" to "JKL","6" to "MNO","7" to "PQRS","8" to "TUV","9" to "WXYZ","0" to "+")
                    keys.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { key ->
                                DialPadKey(
                                    number = key,
                                    letters = subKeys[key] ?: "",
                                    soundPool = soundPool,
                                    context = context,
                                    onClick = { digit -> number += digit },
                                    compact = true
                                )
                            }
                        }
                    }
                    // Action row — below the keys
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FadeScaleBox(visible = number.isNotEmpty()) {
                            DialerActionExpressive(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                        type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
                                    }
                                    context.startActivity(intent)
                                },
                                icon = Icons.Default.PersonAdd,
                                contentDescription = "Add Contact",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                        DialerActionExpressive(
                            onClick = {
                                if (number.isNotEmpty()) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                            placeCallWithSimPreference(number)
                                        } else makeCall(context, number)
                                    } else {
                                        callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                                    }
                                }
                            },
                            icon = Icons.Default.Call,
                            contentDescription = "Call",
                            containerColor = Color(0xFF34A853),
                            contentColor = Color.White,
                            modifier = Modifier.width(96.dp).height(64.dp),
                            isLarge = true
                        )
                        FadeScaleBox(visible = number.isNotEmpty()) {
                            DialerActionExpressive(
                                onLongClick = {
                                    number = ""
                                },
                                onClick = {
                                    number = number.dropLast(1)
                                },
                                icon = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Prevent keyboard from auto-opening on composition
        LaunchedEffect(Unit) { focusManager.clearFocus() }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Layout: search bar fixed at top, scrollable middle (results/pills/clipboard),
        // dialpad card fixed at bottom. Nothing moves when results appear.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {

        // ── Search bar — always visible at top of screen ───────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onFocusChanged { focusState -> searchFieldFocused = focusState.isFocused },
            placeholder = { Text("Search contacts...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                showKeyboardOnFocus = false
            )
        )

        // ── Middle section: results / pills / clipboard (scrollable, fills space) ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {

        // Search results
        AnimatedVisibility(
            visible = searchResults.isNotEmpty(),
            enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                    expandVertically(tween(420, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(280, easing = FastOutLinearInEasing)) +
                    shrinkVertically(tween(320, easing = FastOutLinearInEasing))
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        searchResults.forEach { contact ->
                            SingleTile(
                                title    = contact.name,
                                subtitle = contact.phoneNumbers.firstOrNull(),
                                photoUri = contact.photoUri,
                                onAvatarClick = {
                                    navigator?.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                },
                                onClick  = {
                                    val num = contact.phoneNumbers.firstOrNull() ?: return@SingleTile
                                    makeCall(context, num)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Unknown number pills
        AnimatedVisibility(
            visible = number.isNotEmpty() && searchResults.isEmpty() && searchQuery.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, number)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create contact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, number)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to contact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // Clipboard banner
        AnimatedVisibility(
            visible = showClipboardBanner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Text(text = clipText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); number = clipText; showClipboardBanner = false }) {
                        Text("Use", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showClipboardBanner = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        } // end scrollable middle Column

        // ── Dialpad card — hides with animation when search is active ──
        val showDialpad = !searchFieldFocused && searchQuery.isEmpty()
        AnimatedVisibility(
            visible = showDialpad,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing))
        ) {

        Spacer(modifier = Modifier.height(4.dp))

        // ── Dialpad card — always at bottom, never moves ───────────────
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // Scale based on the SMALLER of width-derived and height-derived factors
            // so the dialpad always fits on screen regardless of device size.
            val refWidth = 360f
            val availableWidth = maxWidth.value
            val widthScale = (availableWidth / refWidth).coerceIn(0.6f, 1.4f)

            // Height budget: total screen height minus search bar (~64dp) minus spacing (~24dp)
            // The dialpad card needs: header(~56dp) + 4 key rows + action row(~72dp) + padding(~40dp)
            // Reference key height = 68dp, so 4 rows = 272dp + overhead ~168dp = ~440dp total card
            val cardHeightBudget = (screenHeight.value - 64f - 24f).coerceAtLeast(200f)
            val refCardHeight = 440f
            val heightScale = (cardHeightBudget / refCardHeight).coerceIn(0.55f, 1.4f)

            val scaleFactor = minOf(widthScale, heightScale)

            val keyWidth: Dp  = (100 * scaleFactor).dp
            val keyHeight: Dp = (68 * scaleFactor).dp
            val actionSize: Dp = (64 * scaleFactor).dp
            val callW: Dp  = (108 * scaleFactor).dp
            val callH: Dp  = (72 * scaleFactor).dp
            val keySpacing: Dp = (8 * scaleFactor).dp

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = (16 * scaleFactor).coerceIn(6f, 16f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(keySpacing)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (number.isNotEmpty())
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DialpadNumberDisplay(
                            number = number,
                            fontSize = ((if (number.length > 11) 28 else 36) * scaleFactor).coerceIn(18f, 40f).toInt()
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RivoDropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            RivoDropdownMenuItem(
                                text     = if (openDialpadDefault) "✓  Open dialpad by default" else "Open dialpad by default",
                                icon     = Icons.Default.Dialpad,
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick  = {
                                    openDialpadDefault = !openDialpadDefault
                                    prefs.setBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, openDialpadDefault)
                                }
                            )
                        }
                    }
                }

                // Dialpad keys
                val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))
                val subKeys = mapOf("1" to "   ","2" to "ABC","3" to "DEF","4" to "GHI","5" to "JKL","6" to "MNO","7" to "PQRS","8" to "TUV","9" to "WXYZ","0" to "+")

                keys.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { key ->
                            DialPadKey(
                                number = key,
                                letters = subKeys[key] ?: "",
                                soundPool = soundPool,
                                context = context,
                                onClick = { digit -> number += digit },
                                overrideWidth = keyWidth,
                                overrideHeight = keyHeight,
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FadeScaleBox(visible = number.isNotEmpty()) {
                        DialerActionExpressive(
                            onClick = {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, number)
                                }
                                context.startActivity(intent)
                            },
                            icon = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(actionSize)
                        )
                    }

                    val lgBackdrop = LocalLiquidGlassBackdrop.current
                    val lgDialpadEnabled = remember(settingsState) {
                        prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) &&
                        prefs.getBoolean(PreferenceManager.KEY_LG_DIALPAD_CALL_BUTTON, false)
                    }
                    val blurDialpadEnabled = remember(settingsState) {
                        prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) &&
                        prefs.getBoolean(PreferenceManager.KEY_BLUR_DIALPAD_CALL_BUTTON, false) &&
                        !lgDialpadEnabled
                    }
                    DialerActionExpressive(
                        onClick = {
                            if (number.isNotEmpty()) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                        placeCallWithSimPreference(number)
                                    } else makeCall(context, number)
                                } else {
                                    callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                                }
                            }
                        },
                        icon = Icons.Default.Call,
                        contentDescription = "Call",
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White,
                        modifier = Modifier.width(callW).height(callH),
                        isLarge = true,
                        liquidGlassBackdrop = lgBackdrop,
                        liquidGlassEnabled = lgDialpadEnabled,
                        blurEnabled = blurDialpadEnabled
                    )

                    FadeScaleBox(visible = number.isNotEmpty()) {
                        DialerActionExpressive(
                            onLongClick = { number = "" },
                            onClick = { number = number.dropLast(1) },
                            icon = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(actionSize)
                        )
                    }
                }
            }
        }
        } // end BoxWithConstraints (dialpad card)

        } // end AnimatedVisibility (dialpad card)

        Spacer(modifier = Modifier.height(8.dp))
        } // end outer Column
        } // end BoxWithConstraints (screen)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerActionExpressive(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    modifier: Modifier = Modifier.size(64.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isLarge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    liquidGlassBackdrop: com.coolappstore.everdialer.by.svhp.liquidglass.Backdrop? = null,
    liquidGlassEnabled: Boolean = false,
    blurEnabled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current
    val wrappedOnClick: () -> Unit = {
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onClick()
    }
    val wrappedOnLongClick: (() -> Unit)? = if (onLongClick != null) ({
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onLongClick()
    }) else null
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (if (isLarge) 18.dp else 14.dp) else (if (isLarge) 28.dp else 24.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "ButtonShape"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "ButtonScale"
    )
    val useLiquidGlass = liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && liquidGlassBackdrop != null
    val buttonShape = RoundedCornerShape(cornerRadius)
    val useBackdropBlur = blurEnabled && !useLiquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    if (useLiquidGlass && liquidGlassBackdrop != null) {
        Box(
            modifier = modifier
                .scale(scale)
                .drawBackdrop(
                    backdrop = liquidGlassBackdrop,
                    shape = { buttonShape },
                    effects = {
                        val d = density
                        colorControls(saturation = 1.3f)
                        blur(2f * d)
                        lens(refractionHeight = 18f * d, refractionAmount = 52f * d)
                    },
                    highlight = { Highlight.Default }
                )
                .combinedClickable(
                    onClick = wrappedOnClick,
                    onLongClick = wrappedOnLongClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Surface(
                shape = buttonShape,
                color = containerColor.copy(alpha = 0.5f),
                contentColor = contentColor,
                modifier = Modifier.matchParentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
                }
            }
        }
    } else if (useBackdropBlur && liquidGlassBackdrop != null) {
        Surface(
            modifier = modifier
                .scale(scale)
                .drawPlainBackdrop(
                    backdrop = liquidGlassBackdrop,
                    shape    = { buttonShape },
                    effects  = { blur(30f * density) }
                )
                .combinedClickable(
                    onClick = wrappedOnClick,
                    onLongClick = wrappedOnLongClick,
                    interactionSource = interactionSource,
                    indication = null
                ),
            shape = buttonShape,
            color = containerColor.copy(alpha = 0.72f),
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
            }
        }
    } else {
        Surface(
            modifier = modifier
                .scale(scale)
                .combinedClickable(onClick = wrappedOnClick, onLongClick = wrappedOnLongClick, interactionSource = interactionSource, indication = null),
            shape = buttonShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
            }
        }
    }
}

@Composable
fun DialPadKey(number: String, letters: String, soundPool: SoundPool, context: Context, onClick: (String) -> Unit, compact: Boolean = false, overrideWidth: Dp? = null, overrideHeight: Dp? = null, scaleFactor: Float = 1f) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current
    val cornerRadius by animateDpAsState(if (isPressed) 14.dp else 28.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "ButtonShapeAnimation")
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "DialKeyScale")
    val bgColor by animateColorAsState(
        if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = Spring.StiffnessMedium), "DialKeyColor"
    )
    val keyWidth = overrideWidth ?: if (compact) 82.dp else 100.dp
    val keyHeight = overrideHeight ?: if (compact) 52.dp else 68.dp
    val mainFontSize = (if (compact) 18f else 22f) * scaleFactor.coerceIn(0.6f, 1.4f)
    val subFontSize = (10f * scaleFactor.coerceIn(0.6f, 1.4f))
    Surface(
        onClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, false)) playDtmf(context, number, soundPool)
            onClick(number)
        },
        modifier = Modifier.size(width = keyWidth, height = keyHeight).scale(scale),
        shape = RoundedCornerShape(cornerRadius),
        color = bgColor,
        interactionSource = interactionSource
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(text = number, style = MaterialTheme.typography.headlineMedium.copy(fontSize = mainFontSize.sp), fontWeight = FontWeight.Medium)
            if (letters.isNotBlank()) {
                Text(text = letters, style = MaterialTheme.typography.labelSmall.copy(fontSize = subFontSize.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
        }
    }
}

/**
 * Renders a phone number with smooth per-character animations:
 * - New chars slide up + fade + scale in from below
 * - Deleted chars slide down + fade + scale out
 * - Existing chars animate their horizontal position smoothly when neighbours appear/disappear
 *
 * Uses a stable monotonically-increasing ID per character insertion so Compose can
 * distinguish "same char shifted left" from "new char at this slot".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadNumberDisplay(
    number: String,
    fontSize: Int
) {
    val easeOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val textColor   = MaterialTheme.colorScheme.onSurface
    val textStyle   = MaterialTheme.typography.displaySmall.copy(
        fontSize   = fontSize.sp,
        fontWeight = FontWeight.Light
    )

    // Stable list of (uniqueId, char) — each insertion gets a fresh monotonic id
    // so position shifts use animateItem, not a full recompose.
    val idCounter  = remember { mutableStateOf(0) }
    val stableChars = remember { mutableStateListOf<Pair<Int, Char>>() }

    LaunchedEffect(number) {
        val current = stableChars.map { it.second }.joinToString("")
        if (number.length > current.length) {
            // Characters appended
            val added = number.drop(current.length)
            added.forEach { ch ->
                stableChars.add(Pair(idCounter.value++, ch))
            }
        } else if (number.length < current.length) {
            // Characters removed from end (backspace)
            val removeCount = current.length - number.length
            repeat(removeCount) {
                if (stableChars.isNotEmpty()) stableChars.removeAt(stableChars.lastIndex)
            }
        } else if (number != current) {
            // Full replacement (e.g. paste) — rebuild
            stableChars.clear()
            number.forEach { ch -> stableChars.add(Pair(idCounter.value++, ch)) }
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        userScrollEnabled     = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            items = stableChars,
            key   = { _, pair -> pair.first }
        ) { _, pair ->
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }

            val offsetY by animateDpAsState(
                targetValue  = if (appeared) 0.dp else 20.dp,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "charOffY"
            )
            val alpha by animateFloatAsState(
                targetValue  = if (appeared) 1f else 0f,
                animationSpec = tween(360, easing = easeOutExpo),
                label = "charAlpha"
            )
            val scale by animateFloatAsState(
                targetValue  = if (appeared) 1f else 0.55f,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "charScale"
            )

            Text(
                text     = pair.second.toString(),
                style    = textStyle,
                color    = textColor,
                modifier = Modifier
                    .animateItem(
                        placementSpec  = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                        fadeInSpec     = tween(360, easing = easeOutExpo),
                        fadeOutSpec    = tween(220)
                    )
                    .offset(y = offsetY)
                    .alpha(alpha)
                    .scale(scale)
            )
        }
    }
}

object T9Matcher {
    fun convertNameToT9(name: String): String {
        return name.uppercase(Locale.getDefault()).map { char ->
            when (char) {
                'A', 'B', 'C' -> '2'
                'D', 'E', 'F' -> '3'
                'G', 'H', 'I' -> '4'
                'J', 'K', 'L' -> '5'
                'M', 'N', 'O' -> '6'
                'P', 'Q', 'R', 'S' -> '7'
                'T', 'U', 'V' -> '8'
                'W', 'X', 'Y', 'Z' -> '9'
                else -> '0'
            }
        }.joinToString("")
    }
}

private fun buildDtmfSoundPool(context: Context): SoundPool {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    return SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build()
}

private fun playDtmf(context: Context, key: String, soundPool: SoundPool) {
    val toneType = when (key) {
        "0" -> android.media.ToneGenerator.TONE_DTMF_0
        "1" -> android.media.ToneGenerator.TONE_DTMF_1
        "2" -> android.media.ToneGenerator.TONE_DTMF_2
        "3" -> android.media.ToneGenerator.TONE_DTMF_3
        "4" -> android.media.ToneGenerator.TONE_DTMF_4
        "5" -> android.media.ToneGenerator.TONE_DTMF_5
        "6" -> android.media.ToneGenerator.TONE_DTMF_6
        "7" -> android.media.ToneGenerator.TONE_DTMF_7
        "8" -> android.media.ToneGenerator.TONE_DTMF_8
        "9" -> android.media.ToneGenerator.TONE_DTMF_9
        "*" -> android.media.ToneGenerator.TONE_DTMF_S
        "#" -> android.media.ToneGenerator.TONE_DTMF_P
        else -> return
    }
    try {
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_DTMF, 80)
        toneGen.startTone(toneType, 150)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ toneGen.release() }, 200)
    } catch (_: Exception) {}
}

@Composable
private fun FadeScaleBox(visible: Boolean, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            content()
        }
    }
}

package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private val ColorBluGrey = Color(0xFF607D8B)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun BlockedNumbersScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsRepo: IContactsRepository = koinInject()
    val callLogRepo: ICallLogRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var blockedList by remember {
        mutableStateOf(BlockedNumbersManager.getBlockedList(context, prefs))
    }
    var blockedWithInfo by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(true) }
    var listSearchQuery by remember { mutableStateOf("") }
    var showAddBlockDialog by remember { mutableStateOf(false) }

    var showBlockedCallsInLogs by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_BLOCKED_CALLS_IN_CALL_LOGS, false))
    }

    var canUseSystem by remember { mutableStateOf(BlockedNumbersManager.canUseSystemBlockList(context)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                canUseSystem = BlockedNumbersManager.canUseSystemBlockList(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navigateToSystemBlocker = {
        if (!BlockedNumbersManager.openSystemBlockedNumbers(context)) {
            val intent = com.coolappstore.everdialer.by.svhp.controller.util.DefaultDialerManager.createDefaultDialerIntent(context)
                ?: android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val reloadBlockedList: () -> Unit = {
        val currentList = BlockedNumbersManager.getBlockedList(context, prefs)
        blockedList = currentList
        coroutineScope.launch(Dispatchers.IO) {
            val resolved = currentList.map { number ->
                val contact = try { contactsRepo.getContactByNumber(number) } catch (_: Exception) { null }
                Triple(number, contact?.name ?: number, contact?.photoUri)
            }
            withContext(Dispatchers.Main) {
                blockedWithInfo = resolved
                isListLoading = false
            }
        }
    }

    LaunchedEffect(blockedList) {
        reloadBlockedList()
    }

    val filteredBlocked = remember(blockedWithInfo, listSearchQuery) {
        if (listSearchQuery.isBlank()) blockedWithInfo
        else blockedWithInfo.filter { (num, name, _) ->
            name.contains(listSearchQuery, ignoreCase = true) || num.contains(listSearchQuery)
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Blocked Numbers",
                onBackClick = { navigator.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBlockDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Block a number")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Blocking card or Red Alert Container if turned off
            if (canUseSystem) {
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "System Blocked Numbers",
                                supporting = "Manage numbers blocked across all phone and messaging apps",
                                leadingIcon = Icons.Outlined.Security,
                                iconContainerColor = MaterialTheme.colorScheme.primary,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { BlockedNumbersManager.openSystemBlockedNumbers(context) }
                            )
                        }
                    }
                }
            } else {
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            onClick = { navigateToSystemBlocker() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SecurityUpdateWarning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Turn on System Blocker",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "System blocker is turned off. Tap here to turn on the system blocker and protect against unwanted calls across your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Call logs toggle
            item {
                RivoAnimatedSection(delayMs = 40L) {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Show blocked numbers in call logs",
                            supporting = "Display calls from blocked numbers in call history",
                            leadingIcon = Icons.Outlined.History,
                            iconContainerColor = ColorBluGrey,
                            checked = showBlockedCallsInLogs,
                            onCheckedChange = {
                                showBlockedCallsInLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_BLOCKED_CALLS_IN_CALL_LOGS, it)
                            }
                        )
                    }
                }
            }

            // Search Bar if multiple blocked numbers
            if (blockedList.size > 2) {
                item {
                    RivoAnimatedSection(delayMs = 80L) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = listSearchQuery,
                                onValueChange = { listSearchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search blocked list…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (listSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { listSearchQuery = "" }) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // Blocked Numbers List / Empty State
            item {
                RivoAnimatedSection(delayMs = 120L) {
                    RivoExpressiveCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Blocked Numbers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "${blockedList.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = { showAddBlockDialog = true },
                                modifier = Modifier.size(34.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Outlined.Add, "Add number", modifier = Modifier.size(18.dp))
                            }
                        }

                        if (isListLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (blockedList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.PersonOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "No numbers blocked",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FilledTonalButton(
                                        onClick = { showAddBlockDialog = true },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Block a number", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else if (filteredBlocked.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No matching blocked numbers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                filteredBlocked.forEachIndexed { index, (number, name, photoUri) ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RivoAvatar(
                                                name = name,
                                                photoUri = photoUri,
                                                modifier = Modifier.size(40.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                if (name != number) {
                                                    Text(
                                                        number,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    BlockedNumbersManager.unblock(context, prefs, number)
                                                    reloadBlockedList()
                                                    Toast.makeText(context, "Unblocked $number", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "Unblock",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
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
        }
    }

    // Add Block Dialog
    if (showAddBlockDialog) {
        var recentNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var contactNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var addSearchQuery by remember { mutableStateOf("") }
        var isAddLoading by remember { mutableStateOf(true) }
        var blockedNumbersTab by remember { mutableIntStateOf(0) }
        var blockedNumberInput by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            isAddLoading = true
            withContext(Dispatchers.IO) {
                val contactsList: List<Contact> = try { contactsRepo.getContacts() } catch (_: Exception) { emptyList() }
                val numberToContact = HashMap<String, Pair<String, String?>>()
                for (c in contactsList) {
                    for (p in c.phoneNumbers) {
                        val clean = p.replace(" ", "").replace("-", "").trim()
                        numberToContact[clean] = Pair(c.name, c.photoUri)
                        numberToContact[p] = Pair(c.name, c.photoUri)
                    }
                }

                contactNumbers = contactsList
                    .filter { it.phoneNumbers.isNotEmpty() }
                    .flatMap { c -> c.phoneNumbers.map { num -> Triple(num, c.name, c.photoUri) } }
                    .distinctBy { it.first }
                    .sortedBy { it.second }

                val logs: List<CallLogEntry> = try { callLogRepo.getCallLogs() } catch (_: Exception) { emptyList() }
                val seen = HashSet<String>()
                val recentRes = ArrayList<Triple<String, String, String?>>()
                for (log in logs) {
                    val num = log.number
                    if (num.isBlank() || !seen.add(num)) continue
                    val clean = num.replace(" ", "").replace("-", "").trim()
                    val matched = numberToContact[clean] ?: numberToContact[num]
                    val name = matched?.first ?: (if (!log.name.isNullOrBlank()) log.name else num)
                    val photo = matched?.second
                    recentRes.add(Triple(num, name, photo))
                }
                recentNumbers = recentRes
            }
            isAddLoading = false
        }

        val filteredRecents = remember(recentNumbers, addSearchQuery) {
            if (addSearchQuery.isBlank()) recentNumbers
            else recentNumbers.filter { (num, name, _) ->
                name.contains(addSearchQuery, ignoreCase = true) || num.contains(addSearchQuery)
            }
        }
        val filteredContacts = remember(contactNumbers, addSearchQuery) {
            if (addSearchQuery.isBlank()) contactNumbers
            else contactNumbers.filter { (num, name, _) ->
                name.contains(addSearchQuery, ignoreCase = true) || num.contains(addSearchQuery)
            }
        }

        fun blockNumber(number: String) {
            BlockedNumbersManager.block(context, prefs, number)
            reloadBlockedList()
            Toast.makeText(context, "Blocked $number", Toast.LENGTH_SHORT).show()
        }

        val maxDialogHeightDp = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        Dialog(onDismissRequest = { showAddBlockDialog = false }) {
            ProvideScaledDensity {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .heightIn(max = maxDialogHeightDp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dialog Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                "Block a Number",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showAddBlockDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                            }
                        }

                        // Search Bar (for Recent / Contacts tabs)
                        if (blockedNumbersTab != 2) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextField(
                                    value = addSearchQuery,
                                    onValueChange = { addSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search name or number…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        AnimatedVisibility(
                                            visible = addSearchQuery.isNotBlank(),
                                            enter = fadeIn() + scaleIn(),
                                            exit = fadeOut() + scaleOut()
                                        ) {
                                            IconButton(onClick = { addSearchQuery = "" }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // Expressive Pill Tabs
                        val tabItems = listOf(
                            Triple("Recents", Icons.Outlined.History, 0),
                            Triple("Contacts", Icons.Outlined.Contacts, 1),
                            Triple("Manual", Icons.Outlined.Edit, 2)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tabItems.forEach { (label, icon, index) ->
                                    val selected = blockedNumbersTab == index
                                    val bgColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        label = "tabBg"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabTxt"
                                    )
                                    Surface(
                                        onClick = { blockedNumbersTab = index },
                                        shape = RoundedCornerShape(50),
                                        color = bgColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab Content Area
                        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 180.dp, max = 340.dp)) {
                            AnimatedContent(
                                targetState = blockedNumbersTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                                    } else {
                                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                                    }
                                },
                                label = "blockedNumbersTabContent"
                            ) { tab ->
                                when (tab) {
                                    0 -> {
                                        Crossfade(targetState = isAddLoading, label = "recentsLoading") { loading ->
                                            if (loading) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(36.dp),
                                                            strokeWidth = 3.5.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            "Loading call logs…",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else if (filteredRecents.isEmpty()) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(36.dp))
                                                        Text(
                                                            if (addSearchQuery.isBlank()) "No call logs found." else "No results for \"$addSearchQuery\"",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(filteredRecents, key = { it.first }) { (number, name, photoUri) ->
                                                        val alreadyBlocked = blockedList.contains(number)
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                RivoAvatar(
                                                                    name = name,
                                                                    photoUri = photoUri,
                                                                    modifier = Modifier.size(38.dp),
                                                                    shape = RoundedCornerShape(12.dp)
                                                                )
                                                                Spacer(Modifier.width(12.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        name,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1
                                                                    )
                                                                    if (name != number) {
                                                                        Text(
                                                                            number,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                }
                                                                if (alreadyBlocked) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(50),
                                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                                            Text("Blocked", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                                        }
                                                                    }
                                                                } else {
                                                                    FilledTonalButton(
                                                                        onClick = { blockNumber(number) },
                                                                        shape = RoundedCornerShape(50),
                                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                        modifier = Modifier.height(34.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Block, null, modifier = Modifier.size(14.dp))
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("Block", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        Crossfade(targetState = isAddLoading, label = "contactsLoading") { loading ->
                                            if (loading) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(36.dp),
                                                            strokeWidth = 3.5.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            "Loading contacts…",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else if (filteredContacts.isEmpty()) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Outlined.Contacts, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(36.dp))
                                                        Text(
                                                            if (addSearchQuery.isBlank()) "No contacts found." else "No results for \"$addSearchQuery\"",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(filteredContacts, key = { it.first }) { (number, name, photoUri) ->
                                                        val alreadyBlocked = blockedList.contains(number)
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                RivoAvatar(
                                                                    name = name,
                                                                    photoUri = photoUri,
                                                                    modifier = Modifier.size(38.dp),
                                                                    shape = RoundedCornerShape(12.dp)
                                                                )
                                                                Spacer(Modifier.width(12.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        name,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1
                                                                    )
                                                                    if (name != number) {
                                                                        Text(
                                                                            number,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                }
                                                                if (alreadyBlocked) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(50),
                                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                                            Text("Blocked", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                                        }
                                                                    }
                                                                } else {
                                                                    FilledTonalButton(
                                                                        onClick = { blockNumber(number) },
                                                                        shape = RoundedCornerShape(50),
                                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                        modifier = Modifier.height(34.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Block, null, modifier = Modifier.size(14.dp))
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("Block", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = blockedNumberInput,
                                                onValueChange = { blockedNumberInput = it },
                                                label = { Text("Phone number to block") },
                                                placeholder = { Text("+1 234 567 8900") },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                },
                                                trailingIcon = {
                                                    AnimatedVisibility(
                                                        visible = blockedNumberInput.isNotBlank(),
                                                        enter = fadeIn() + scaleIn(),
                                                        exit = fadeOut() + scaleOut()
                                                    ) {
                                                        IconButton(onClick = { blockedNumberInput = "" }) {
                                                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                            )

                                            Button(
                                                onClick = {
                                                    val num = blockedNumberInput.trim()
                                                    if (num.isNotBlank()) {
                                                        blockNumber(num)
                                                        blockedNumberInput = ""
                                                    }
                                                },
                                                enabled = blockedNumberInput.trim().isNotBlank(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                )
                                            ) {
                                                Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Block Number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showAddBlockDialog = false },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Done", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

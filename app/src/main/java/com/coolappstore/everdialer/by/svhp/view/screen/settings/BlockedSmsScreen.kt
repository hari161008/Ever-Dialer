package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

private val ColorRed = Color(0xFFE53935)
private val ColorBlue = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun BlockedSmsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val coroutineScope = rememberCoroutineScope()

    var blockedList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newNumberToBlock by remember { mutableStateOf("") }

    val reloadBlockedList: () -> Unit = {
        coroutineScope.launch(Dispatchers.IO) {
            val list = BlockedNumbersManager.getBlockedList(context, prefs)
            withContext(Dispatchers.Main) {
                blockedList = list
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadBlockedList()
    }

    val filteredList = remember(blockedList, searchQuery) {
        if (searchQuery.isBlank()) blockedList
        else blockedList.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val canUseSystem = remember(context) { BlockedNumbersManager.canUseSystemBlockList(context) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Blocked SMS",
                onBackClick = { navigator.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newNumberToBlock = ""
                    showAddBlockDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Block number or sender")
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
            // System blocking option if available
            if (canUseSystem) {
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "System Blocked Numbers",
                                supporting = "Manage numbers blocked across all phone and messaging apps",
                                leadingIcon = Icons.Outlined.Security,
                                iconContainerColor = ColorBlue,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    BlockedNumbersManager.openSystemBlockedNumbers(context)
                                }
                            )
                        }
                    }
                }
            }

            // Search filter if there are multiple blocked senders
            if (blockedList.size > 4) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search blocked numbers or senders") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Blocked List
            item {
                RivoAnimatedSection(delayMs = 40L) {
                    Column {
                        RivoSectionHeader("Blocked Senders (${blockedList.size})")

                        RivoExpressiveCard {
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            } else if (blockedList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.Block,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "No blocked SMS senders",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Messages and notifications from blocked numbers will be suppressed.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                newNumberToBlock = ""
                                                showAddBlockDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Block a number or sender")
                                        }
                                    }
                                }
                            } else if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No matching senders found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column {
                                    filteredList.forEachIndexed { index, sender ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            RivoAvatar(
                                                name = sender,
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sender,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Messages and notifications blocked",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    BlockedNumbersManager.unblock(context, prefs, sender)
                                                    reloadBlockedList()
                                                    smsVM.refreshConversations()
                                                    Toast.makeText(context, "Unblocked $sender", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Unblock", style = MaterialTheme.typography.labelMedium)
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

    if (showAddBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block a number or sender") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the phone number or sender ID (e.g. alphanumeric SMS header) to block.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newNumberToBlock,
                        onValueChange = { newNumberToBlock = it },
                        label = { Text("Number or Sender ID") },
                        placeholder = { Text("e.g. +1234567890 or VM-ALERTS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newNumberToBlock.trim()
                        if (trimmed.isNotBlank()) {
                            BlockedNumbersManager.block(context, prefs, trimmed)
                            reloadBlockedList()
                            smsVM.refreshConversations()
                            Toast.makeText(context, "Blocked $trimmed", Toast.LENGTH_SHORT).show()
                            showAddBlockDialog = false
                        }
                    }
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

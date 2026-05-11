package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoScrollAnimatedItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsGridEffect
import com.coolappstore.everdialer.by.svhp.view.components.SimPickerDialog
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.abs

@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun FavoritesScreen(navController: NavController, navigator: DestinationsNavigator) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val favorites = remember(allContacts) { allContacts.filter { it.isFavorite } }
    val scope = rememberCoroutineScope()
    val prefs = koinInject<PreferenceManager>()
    val notesEnabled = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)
    val context = LocalContext.current

    var showSimPicker by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            pendingCallNumber?.let { makeCall(context, it) }
        }
    }

    if (showSimPicker && pendingCallNumber != null) {
        val telecomManager = remember { context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager }
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, pendingCallNumber!!, handle)
                showSimPicker = false
            }
        )
    }

    val pillNav = remember { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull() ?: continue
                        if (!down.pressed) continue
                        val startX = down.position.x
                        val startY = down.position.y
                        val startTime = System.currentTimeMillis()
                        var triggered = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            val elapsed = System.currentTimeMillis() - startTime
                            if (!triggered && elapsed >= 150L &&
                                abs(dx) > 700f &&
                                abs(dx) > abs(dy) * 5.5f
                            ) {
                                triggered = true
                                if (dx < 0) {
                                    scope.launch {
                                        navController.navigate(RecentScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                } else {
                                    if (notesEnabled) {
                                        scope.launch {
                                            navController.navigate(NotesScreenDestination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                        }
                                    }
                                }
                            }
                            if (!change.pressed) break
                        }
                    }
                }
            },
        topBar = { TopBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (favorites.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Favorites Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Star a contact to add them here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                val gridState = rememberLazyGridState()
                ScrollHapticsGridEffect(gridState = gridState)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favorites) { contact ->
                        RivoScrollAnimatedItem {
                            FavoriteContactCard(
                                contact = contact,
                                contactsVM = contactsVM,
                                navigator = navigator,
                                context = context,
                                onClick = {
                                    val directCall = prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)
                                    val phoneNumber = contact.phoneNumbers.firstOrNull()
                                    if (directCall && phoneNumber != null) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                            val hasPState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                                            if (hasPState) {
                                                val tm = context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager
                                                if (tm.callCapablePhoneAccounts.size > 1) {
                                                    pendingCallNumber = phoneNumber
                                                    showSimPicker = true
                                                } else makeCall(context, phoneNumber)
                                            } else makeCall(context, phoneNumber)
                                        } else {
                                            pendingCallNumber = phoneNumber
                                            callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                                        }
                                    } else {
                                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteContactCard(
    contact: Contact,
    contactsVM: ContactsViewModel,
    navigator: DestinationsNavigator,
    context: android.content.Context,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "cardAlpha")
    LaunchedEffect(Unit) { visible = true }

    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed || showMenu) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                RivoAvatar(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Text(
                text = contact.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    RivoDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        RivoDropdownMenuItem(
            text = "Call",
            icon = Icons.Default.Call,
            iconTint = Color(0xFF4CAF50),
            onClick = {
                showMenu = false
                onClick()
            }
        )
        val phoneNumber = contact.phoneNumbers.firstOrNull()
        if (!phoneNumber.isNullOrEmpty()) {
            RivoDropdownMenuItem(
                text = "Send SMS",
                icon = Icons.Default.Message,
                iconTint = Color(0xFF009688),
                onClick = {
                    showMenu = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("sms:$phoneNumber")
                    }
                    context.startActivity(intent)
                }
            )
        }
        RivoDropdownMenuItem(
            text = "View Details",
            icon = Icons.Default.Info,
            iconTint = Color(0xFF2196F3),
            onClick = {
                showMenu = false
                navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        RivoDropdownMenuItem(
            text = "Remove from Favourites",
            icon = Icons.Default.Favorite,
            iconTint = Color(0xFFF44336),
            isDestructive = true,
            onClick = {
                showMenu = false
                contactsVM.toggleFavorite(contact)
            }
        )
    }
}

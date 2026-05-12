package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.QrCodeUtils
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallWithSimPreference
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactDetailsScreen(
    contactId: String? = null,
    phoneNumber: String? = null,
    navController: NavController,
    navigator: DestinationsNavigator
) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()

    val contacts by contactsViewModel.allContacts.collectAsState()
    val allLogs by callLogViewModel.allCallLogs.collectAsState()

    val contact = remember(contactId, phoneNumber, contacts) {
        if (contactId != null && contactId != "null") contacts.find { it.id == contactId }
        else if (phoneNumber != null) contacts.find { c -> c.phoneNumbers.any { n -> n.replace(" ", "").contains(phoneNumber.replace(" ", "")) } }
        else null
    }

    val displayPhone = phoneNumber ?: contact?.phoneNumbers?.firstOrNull() ?: "Unknown"
    val displayName = contact?.name ?: phoneNumber ?: "Unknown"
    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    val prefs = koinInject<PreferenceManager>()
    val simPref = remember { prefs.getInt("default_sim", 0) }

    var showSimPicker by remember { mutableStateOf(false) }
    var showNumberPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }

    val contactLogs = remember(contact, phoneNumber, allLogs) {
        allLogs.filter { log ->
            (contact != null && (log.contactId == contact.id || contact.phoneNumbers.any { n -> log.number.replace(" ", "").contains(n.replace(" ", "")) })) ||
            (phoneNumber != null && log.number.replace(" ", "").contains(phoneNumber.replace(" ", "")))
        }
    }

    val isFavorite = contact?.isFavorite ?: false
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    // Entrance / exit animation
    var screenVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    fun navigateBack() {
        isClosing = true
        scope.launch {
            kotlinx.coroutines.delay(420)
            navigator.navigateUp()
        }
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (screenVisible && !isClosing) 1f else 0f,
        animationSpec = if (isClosing) tween(380, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        else tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "screenAlpha"
    )
    val screenOffsetY by animateDpAsState(
        targetValue = if (screenVisible && !isClosing) 0.dp else if (isClosing) 80.dp else 56.dp,
        animationSpec = if (isClosing) tween(400, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        else spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
        label = "screenOffsetY"
    )
    LaunchedEffect(Unit) { screenVisible = true }
    BackHandler { navigateBack() }

    val initiateCall = { number: String ->
        placeCallWithSimPreference(context, number, simPref) {
            pendingNumber = number; showSimPicker = true
        }
    }

    if (showNumberPicker && contact != null) {
        NumberPickerDialog(numbers = contact.phoneNumbers, onDismissRequest = { showNumberPicker = false }, onNumberSelected = { showNumberPicker = false; initiateCall(it) })
    }
    if (showSimPicker && pendingNumber != null) {
        SimPickerDialog(onDismissRequest = { showSimPicker = false }, onSimSelected = { handle -> makeCall(context, pendingNumber!!, handle); showSimPicker = false })
    }
    if (showQrDialog) {
        QrCodeDialog(name = displayName, phone = displayPhone, email = contact?.emails?.firstOrNull(), onDismiss = { showQrDialog = false })
    }
    if (showNoteEditor) {
        NoteEditorDialog(contactName = displayName, phoneNumber = displayPhone, onDismiss = { showNoteEditor = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = { navigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) { Icon(Icons.Outlined.QrCode2, "QR Code") }
                    if (contact != null) {
                        IconButton(onClick = { contactsViewModel.toggleFavorite(contact) }) {
                            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (isFavorite) Color.Red else LocalContentColor.current)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_EDIT).apply { data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id.toLong()) }
                            context.startActivity(intent)
                        }) { Icon(Icons.Default.Edit, "Edit") }
                    } else if (phoneNumber != null && phoneNumber != "Unknown") {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply { type = ContactsContract.RawContacts.CONTENT_TYPE; putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber) }
                            context.startActivity(intent)
                        }) { Icon(Icons.Default.PersonAdd, "Add Contact") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().alpha(screenAlpha).offset(y = screenOffsetY)) {
            Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                AsyncImage(model = contact?.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(50.dp), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface))))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(240.dp).background(brush = Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), Color.Transparent))).blur(60.dp))
                            RivoAvatar(name = displayName, photoUri = contact?.photoUri, modifier = Modifier.size(140.dp), shape = CircleShape)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = displayName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RivoExpressiveButton(icon = Icons.Default.Call, label = "Call", containerColor = MaterialTheme.colorScheme.primaryContainer, onClick = {
                            if (contact != null && contact.phoneNumbers.size > 1) showNumberPicker = true
                            else if (displayPhone != "Unknown") initiateCall(displayPhone)
                        })
                        RivoExpressiveButton(icon = Icons.AutoMirrored.Filled.Message, label = "Text", containerColor = MaterialTheme.colorScheme.secondaryContainer, onClick = {
                            if (displayPhone != "Unknown") context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$displayPhone")))
                        })
                    }
                }

                // Contact Info
                item {
                    RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                        if (contact != null) {
                            contact.phoneNumbers.forEachIndexed { index, number ->
                                RivoListItem(headline = number, supporting = "Mobile", leadingIcon = Icons.Default.Phone, onClick = { initiateCall(number) })
                                if (index < contact.phoneNumbers.size - 1 || contact.emails.isNotEmpty()) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            contact.emails.forEachIndexed { index, email ->
                                RivoListItem(headline = email, supporting = "Email", leadingIcon = Icons.Default.Email, onClick = {
                                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                })
                                if (index < contact.emails.size - 1) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        } else if (phoneNumber != null && phoneNumber != "Unknown") {
                            RivoListItem(headline = phoneNumber, supporting = "Unknown Number", leadingIcon = Icons.Default.Phone, onClick = { initiateCall(phoneNumber) })
                        }
                    }
                }

                // Notes section (between Contact Info and Recent Activity)
                item {
                    var currentNote by remember(displayName, displayPhone) {
                        mutableStateOf(NoteManager.readNote(context, displayName, displayPhone))
                    }
                    LaunchedEffect(showNoteEditor) {
                        if (!showNoteEditor) currentNote = NoteManager.readNote(context, displayName, displayPhone)
                    }

                    RivoExpressiveCard(title = "Notes", icon = Icons.Default.Note) {
                        if (currentNote.isNotBlank()) {
                            // Inline preview with clickable links
                            val annotated = buildClickableAnnotatedString(currentNote)
                            ClickableText(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                onClick = { offset ->
                                    annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                                        val url = if (ann.item.startsWith("http")) ann.item else "https://${ann.item}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        TextButton(
                            onClick = { showNoteEditor = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (currentNote.isBlank()) "Add note..." else "Edit note")
                        }
                    }
                }

                // Events & More
                if (contact != null && (contact.events.isNotEmpty() || contact.addresses.isNotEmpty())) {
                    item {
                        RivoExpressiveCard(title = "Events & More", icon = Icons.Default.Event) {
                            contact.events.forEachIndexed { index, event ->
                                val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                RivoListItem(headline = event.date, supporting = event.label ?: if (isBirthday) "Birthday" else "Event", leadingIcon = if (isBirthday) Icons.Outlined.Cake else Icons.Outlined.Event, onClick = {})
                                if (index < contact.events.size - 1 || contact.addresses.isNotEmpty()) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            contact.addresses.forEachIndexed { index, address ->
                                RivoListItem(headline = address, supporting = "Address", leadingIcon = Icons.Default.LocationOn, onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")))
                                })
                                if (index < contact.addresses.size - 1) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // Recent Activity
                if (contactLogs.isNotEmpty()) {
                    item {
                        RivoExpressiveCard(title = "Recent Activity", icon = Icons.Default.History) {
                            Column(modifier = Modifier.animateContentSize()) {
                                contactLogs.take(3).forEachIndexed { index, log ->
                                    CallLogTileSimple(log)
                                    if (index < 2 && index < contactLogs.size - 1) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (contactLogs.size > 3) {
                                    TextButton(onClick = { navController.navigate("call_log_detail_screen?contactId=${contactId ?: "null"}&phoneNumber=${phoneNumber ?: "null"}") }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Show full history")
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            ScrollToTopButton(visible = showButton, onClick = { scope.launch { listState.animateScrollToItem(0) } })
        }
    }
}

@Composable
fun QrCodeDialog(name: String, phone: String?, email: String?, onDismiss: () -> Unit) {
    val vCard = remember(name, phone, email) { QrCodeUtils.generateVCard(name, phone, email) }
    val qrBitmap = remember(vCard) { QrCodeUtils.generateQrCode(vCard, 600) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Contact QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                qrBitmap?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Text(phone ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Close") }
            }
        }
    }
}

private fun buildClickableAnnotatedString(text: String): AnnotatedString {
    val urlPattern = android.util.Patterns.WEB_URL
    return buildAnnotatedString {
        var lastIdx = 0
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            append(text.substring(lastIdx, start))
            pushStringAnnotation("URL", matcher.group())
            withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF1E88E5), textDecoration = TextDecoration.Underline)) {
                append(text.substring(start, end))
            }
            pop()
            lastIdx = end
        }
        append(text.substring(lastIdx))
    }
}

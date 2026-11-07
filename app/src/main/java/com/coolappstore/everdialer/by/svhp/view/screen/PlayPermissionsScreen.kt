package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultDialerManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity

private data class PermissionItemState(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val granted: Boolean,
    val onClick: () -> Unit
)

@Composable
fun PlayPermissionsScreen(
    onGrantPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler { onDismiss() }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTrigger++
    }

    val openOverlaySettings = {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    }

    // Permission check states
    val phoneGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
    }

    val contactsGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    val callLogGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }

    val defaultDialerGranted = remember(refreshTrigger) {
        DefaultDialerManager.isDefaultDialer(context)
    }

    val overlayGranted = remember(refreshTrigger) {
        Settings.canDrawOverlays(context)
    }

    val defaultSmsGranted = remember(refreshTrigger) {
        DefaultSmsManager.isDefaultSms(context)
    }

    val smsGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    val notificationsGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val permissionsList = remember(
        phoneGranted, contactsGranted, callLogGranted,
        defaultDialerGranted, overlayGranted, defaultSmsGranted,
        smsGranted, notificationsGranted
    ) {
        val list = mutableListOf(
            PermissionItemState(
                icon = Icons.Outlined.Phone,
                label = "Phone Calls & State",
                description = "Essential to make and answer calls, display active in-call controls, and monitor call state.",
                granted = phoneGranted,
                onClick = { onGrantPermissions() }
            ),
            PermissionItemState(
                icon = Icons.Outlined.Contacts,
                label = "Contacts",
                description = "Required to display your contacts list, enable T9 dialer search, and identify caller names.",
                granted = contactsGranted,
                onClick = { onGrantPermissions() }
            ),
            PermissionItemState(
                icon = Icons.Outlined.History,
                label = "Call History",
                description = "Required to display recent incoming, outgoing, and missed call logs with quick redial.",
                granted = callLogGranted,
                onClick = { onGrantPermissions() }
            ),
            PermissionItemState(
                icon = Icons.Outlined.PhoneAndroid,
                label = "Default Phone App",
                description = "Sets Ever Dialer as your device's default dialer for seamless in-call management.",
                granted = defaultDialerGranted,
                onClick = { onRequestDefaultDialer() }
            ),
            PermissionItemState(
                icon = Icons.Outlined.Layers,
                label = "Display Over Other Apps",
                description = "Enables floating ongoing call bubble, floating SMS quick replies, and missed call popups.",
                granted = overlayGranted,
                onClick = { openOverlaySettings() }
            ),
            PermissionItemState(
                icon = Icons.AutoMirrored.Outlined.Chat,
                label = "Default SMS App",
                description = "Sets Ever Dialer as your default messaging handler to send and receive text messages.",
                granted = defaultSmsGranted,
                onClick = { DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context) }
            ),
            PermissionItemState(
                icon = Icons.Outlined.MarkChatRead,
                label = "SMS & Messaging",
                description = "Allows reading and sending text messages, viewing message threads, and sending quick replies.",
                granted = smsGranted,
                onClick = {
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS
                        )
                    )
                }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItemState(
                    icon = Icons.Outlined.Notifications,
                    label = "Notifications",
                    description = "Required to display incoming call alerts on screen and ongoing call notification controls.",
                    granted = notificationsGranted,
                    onClick = { onGrantPermissions() }
                )
            )
        }
        list
    }

    val grantedCount = permissionsList.count { it.granted }
    val totalCount = permissionsList.size
    val isComplete = grantedCount == totalCount

    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f,
        animationSpec = tween(600),
        label = "progress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val primaryCont = MaterialTheme.colorScheme.primaryContainer
    val secondaryCont = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryCont = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimaryCont = MaterialTheme.colorScheme.onPrimaryContainer

    val grantNextAccess = {
        when {
            !phoneGranted || !contactsGranted || !callLogGranted -> onGrantPermissions()
            !defaultDialerGranted -> onRequestDefaultDialer()
            !overlayGranted -> openOverlaySettings()
            !defaultSmsGranted -> DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context)
            !smsGranted -> {
                smsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            }
            !notificationsGranted -> onGrantPermissions()
            else -> onDismiss()
        }
    }

    ProvideScaledDensity {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Hero header (matching Call Recorder design) ───────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to primaryCont,
                                    0.5f to secondaryCont,
                                    1.0f to tertiaryCont
                                )
                            )
                        )
                ) {
                    // Decorative circles (clean, no blur)
                    // Large ring top-right
                    Box(
                        Modifier
                            .size(130.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 36.dp, y = (-36).dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.10f))
                    )
                    // Medium circle top-right inner
                    Box(
                        Modifier
                            .size(70.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.16f))
                    )
                    // Large ring bottom-left
                    Box(
                        Modifier
                            .size(110.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-32).dp, y = 32.dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.09f))
                    )
                    // Small accent circle bottom-left inner
                    Box(
                        Modifier
                            .size(50.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-8).dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.14f))
                    )
                    // Tiny dot centre-right
                    Box(
                        Modifier
                            .size(18.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = (-28).dp, y = (-20).dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.22f))
                    )
                    // Tiny dot top-left
                    Box(
                        Modifier
                            .size(12.dp)
                            .offset(x = 28.dp, y = 32.dp)
                            .clip(CircleShape)
                            .background(onPrimaryCont.copy(alpha = 0.18f))
                    )

                    // Skip button in header
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .zIndex(2f),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        contentColor = onPrimaryCont
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Circle icon badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(onPrimaryCont.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = onPrimaryCont,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "App Permissions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = onPrimaryCont,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ever Dialer requires the following permissions to handle phone calls, contacts, floating heads, and messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimaryCont.copy(alpha = 0.78f),
                            lineHeight = 16.sp
                        )
                    }
                }

                // ── Progress strip (matching Call Recorder design) ─────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Setup Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$grantedCount / $totalCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }

                // ── Permission rows (matching Call Recorder design) ────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    permissionsList.forEach { item ->
                        PermissionRow(
                            icon = item.icon,
                            label = item.label,
                            description = item.description,
                            granted = item.granted,
                            onClick = item.onClick
                        )
                    }
                }

                // ── Action button (matching Call Recorder design) ──────────────────
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isComplete) {
                                    onDismiss()
                                } else {
                                    grantNextAccess()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            val icon = if (isComplete) Icons.Outlined.Check else Icons.Outlined.LockOpen
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isComplete) "Continue" else "Grant Access",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    description: String,
    granted: Boolean,
    statusOverride: String? = null,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    val bgColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        animationSpec = tween(400),
        label = "bg"
    )
    val iconBgColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(400),
        label = "iconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (granted) primary else error,
        animationSpec = tween(400),
        label = "iconTint"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (!granted) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVar,
                    lineHeight = 15.sp
                )
            }
            Surface(
                shape = CircleShape,
                color = if (granted) primary.copy(alpha = 0.12f) else error.copy(alpha = 0.12f),
                modifier = Modifier.then(if (!granted) Modifier.clickable { onClick() } else Modifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = if (granted) primary else error,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = statusOverride ?: if (granted) "Granted" else "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (granted) primary else error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

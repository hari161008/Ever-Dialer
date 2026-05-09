package com.coolappstore.everdialer.by.svhp.view.components.tiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    photoUri: String? = null,
    icon: ImageVector? = null,
    iconContainerColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    isMissedCall: Boolean = false,
    phoneNumber: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val prefs   = koinInject<PreferenceManager>()
    var showMenu              by remember { mutableStateOf(false) }
    var isPressed             by remember { mutableStateOf(false) }
    var horizontalDragDetected by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue    = if (showMenu) 0.97f else if (isPressed) 0.97f else 1f,
        animationSpec  = spring(stiffness = Spring.StiffnessMedium),
        label          = "TileScale"
    )

    val numberForMenu = phoneNumber ?: subtitle?.filter { it.isDigit() || it == '+' }
        ?.takeIf { it.length >= 5 } ?: subtitle

    Box(modifier = modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (horizontalDragDetected) return@combinedClickable
                        isPressed = false
                        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                            performAppHaptic(
                                context,
                                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            )
                        }
                        onClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        horizontalDragDetected = false
                        val downPos = down.position
                        do {
                            val event   = awaitPointerEvent()
                            val current = event.changes.firstOrNull() ?: break
                            val dx = kotlin.math.abs(current.position.x - downPos.x)
                            val dy = kotlin.math.abs(current.position.y - downPos.y)
                            if (dx > 28.dp.toPx() && dx > dy * 1.3f) horizontalDragDetected = true
                            if (!current.pressed) break
                        } while (true)
                        isPressed = false
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RivoAvatar(
                name               = title,
                photoUri           = photoUri,
                icon               = icon,
                iconContainerColor = iconContainerColor,
                modifier           = Modifier.size(42.dp),
                shape              = RoundedCornerShape(14.dp)
            )

            Column(
                modifier            = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text     = title,
                        style    = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color    = if (isMissedCall) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailing != null) {
                        titleTrailing()
                    }
                }
                if (supportingContent != null) {
                    supportingContent()
                } else if (subtitle != null) {
                    Text(
                        text     = subtitle,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    trailingContent()
                }
            }
        }

        RivoDropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            RivoDropdownMenuItem(
                text     = "Call",
                icon     = Icons.Default.Call,
                iconTint = Color(0xFF4CAF50),
                onClick  = {
                    showMenu = false
                    onClick()
                }
            )
            if (!numberForMenu.isNullOrEmpty()) {
                RivoDropdownMenuItem(
                    text     = "Copy number",
                    icon     = Icons.Default.ContentCopy,
                    iconTint = Color(0xFF2196F3),
                    onClick  = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", numberForMenu))
                        Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                    }
                )
                RivoDropdownMenuItem(
                    text     = "Add to contacts",
                    icon     = Icons.Default.PersonAdd,
                    iconTint = Color(0xFF9C27B0),
                    onClick  = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, numberForMenu)
                        }
                        context.startActivity(intent)
                    }
                )
                RivoDropdownMenuItem(
                    text     = "Send SMS",
                    icon     = Icons.Default.Message,
                    iconTint = Color(0xFF009688),
                    onClick  = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("sms:$numberForMenu")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun TileGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        shape    = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
    }
}

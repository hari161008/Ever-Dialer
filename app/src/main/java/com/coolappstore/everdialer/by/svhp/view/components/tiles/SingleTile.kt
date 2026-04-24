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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar

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
    isMissedCall: Boolean = false,
    phoneNumber: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TileScale"
    )

    val numberForMenu = phoneNumber ?: subtitle?.filter { it.isDigit() || it == '+' }
        ?.takeIf { it.length >= 5 } ?: subtitle

    Box(modifier = modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        isPressed = false
                        onClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RivoAvatar(
                name = title,
                photoUri = photoUri,
                icon = icon,
                iconContainerColor = iconContainerColor,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMissedCall)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supportingContent != null) {
                    supportingContent()
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Call") },
                leadingIcon = { Icon(Icons.Default.Call, null) },
                onClick = {
                    showMenu = false
                    onClick()
                }
            )
            if (!numberForMenu.isNullOrEmpty()) {
                DropdownMenuItem(
                    text = { Text("Copy number") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", numberForMenu))
                        Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to contacts") },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                    onClick = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, numberForMenu)
                        }
                        context.startActivity(intent)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Send SMS") },
                    leadingIcon = { Icon(Icons.Default.Message, null) },
                    onClick = {
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
    }
}

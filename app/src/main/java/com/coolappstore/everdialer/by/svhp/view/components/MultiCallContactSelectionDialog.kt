package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Dialog shown when tapping to view contact details for a merged conference call or a 2-person
 * call (one active, one on hold). Allows the user to choose which contact's details to open.
 */
@Composable
fun MultiCallContactSelectionDialog(
    name1: String?,
    number1: String,
    contactId1: String?,
    photoUri1: String? = null,
    name2: String?,
    number2: String,
    contactId2: String?,
    photoUri2: String? = null,
    isMergedCall: Boolean,
    onSelectContact: (contactId: String?, number: String) -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (isMergedCall) "Call merge of 2 people" else "2 people in call"
    val icon = if (isMergedCall) Icons.Default.CallMerge else Icons.Default.Group

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select contact to view details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Participant 1
                ParticipantItemCard(
                    displayName = name1?.takeIf { it.isNotBlank() } ?: number1,
                    phoneNumber = number1,
                    photoUri = photoUri1,
                    tagLabel = if (isMergedCall) "Participant 1" else "Active line",
                    tagContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    tagContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        onDismiss()
                        onSelectContact(contactId1, number1)
                    }
                )

                // Participant 2
                ParticipantItemCard(
                    displayName = name2?.takeIf { it.isNotBlank() } ?: number2,
                    phoneNumber = number2,
                    photoUri = photoUri2,
                    tagLabel = if (isMergedCall) "Participant 2" else "On hold",
                    tagContainerColor = if (isMergedCall) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    tagContentColor = if (isMergedCall) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        onDismiss()
                        onSelectContact(contactId2, number2)
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ParticipantItemCard(
    displayName: String,
    phoneNumber: String,
    photoUri: String?,
    tagLabel: String,
    tagContainerColor: Color,
    tagContentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (displayName.isNotEmpty() && displayName != phoneNumber) {
                    Text(
                        text = displayName.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagContainerColor,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = tagLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = tagContentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                if (phoneNumber.isNotBlank() && phoneNumber != displayName) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

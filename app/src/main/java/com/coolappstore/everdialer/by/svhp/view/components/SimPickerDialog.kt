package com.coolappstore.everdialer.by.svhp.view.components

import android.telecom.PhoneAccountHandle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.view.screen.SimCardIconWithNumber
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.everdialer.by.svhp.controller.util.SimAccountInfo
import com.coolappstore.everdialer.by.svhp.controller.util.getOrderedSimAccounts

/**
 * Modern Material 3 Expressive SIM Picker Dialog.
 *
 * Guarantees strict hardware SIM slot ordering (SIM 1 / slot 0 first, SIM 2 / slot 1 second),
 * displays carrier network names, SIM phone numbers, and user-configured SIM colors.
 */
@Composable
fun SimPickerDialog(
    onDismissRequest: () -> Unit,
    onSimSelected: (PhoneAccountHandle) -> Unit,
    availableAccounts: List<PhoneAccountHandle>? = null,
    title: String = "Call via",
    number: String? = null,
    contactName: String? = null
) {
    val context = LocalContext.current

    val simAccounts: List<SimAccountInfo> = remember(availableAccounts) {
        getOrderedSimAccounts(context, availableAccounts)
    }

    if (simAccounts.isNotEmpty()) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        tonalElevation = 6.dp,
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 24.dp)
                        ) {
                            // Header badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(100),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Choose SIM",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Contact / Number target indicator if present
                            val targetInfo = when {
                                !contactName.isNullOrBlank() && !number.isNullOrBlank() -> "$contactName • $number"
                                !contactName.isNullOrBlank() -> contactName
                                !number.isNullOrBlank() -> number
                                else -> null
                            }
                            if (targetInfo != null) {
                                Text(
                                    text = targetInfo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // List of SIM accounts, guaranteed strictly ordered
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                simAccounts.forEach { acc ->
                                    val simColor = Color(acc.colorInt)
                                    val slotNum = acc.slotIndex + 1
                                    val isKnownSim = acc.slotIndex in 0..1
                                    val rawName = acc.displayName.trim()

                                    // Display name guarantee: prominently display the SIM / carrier name
                                    val titleText = if (isKnownSim) {
                                        if (rawName.equals("SIM $slotNum", ignoreCase = true) || rawName.equals("Slot $slotNum", ignoreCase = true)) {
                                            if (!acc.carrierName.isNullOrBlank() && !acc.carrierName.equals(rawName, ignoreCase = true)) {
                                                "SIM $slotNum • ${acc.carrierName}"
                                            } else {
                                                "SIM $slotNum"
                                            }
                                        } else if (rawName.startsWith("SIM $slotNum", ignoreCase = true)) {
                                            rawName
                                        } else {
                                            "SIM $slotNum • $rawName"
                                        }
                                    } else {
                                        rawName
                                    }

                                    // Subtitle: SIM phone number and/or network carrier info
                                    val subtitleText = when {
                                        !acc.number.isNullOrBlank() && !acc.carrierName.isNullOrBlank() && !titleText.contains(acc.carrierName, ignoreCase = true) ->
                                            "${acc.carrierName} • ${acc.number}"
                                        !acc.number.isNullOrBlank() ->
                                            acc.number
                                        !acc.carrierName.isNullOrBlank() && !titleText.contains(acc.carrierName, ignoreCase = true) ->
                                            acc.carrierName
                                        isKnownSim ->
                                            "Slot $slotNum"
                                        else ->
                                            "Calling account"
                                    }

                                    val containerColor = if (acc.slotIndex == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    val onContainerColor = if (acc.slotIndex == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

                                    Surface(
                                        onClick = { onSimSelected(acc.handle) },
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // SIM Slot Badge container using primary/secondary container color
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = containerColor,
                                                tonalElevation = 2.dp,
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (acc.slotIndex in 0..1) {
                                                        SimCardIconWithNumber(
                                                            simSlotNumber = "${acc.slotIndex + 1}",
                                                            tint = onContainerColor,
                                                            isLarge = false
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Phone,
                                                            contentDescription = null,
                                                            tint = onContainerColor,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            // SIM / Carrier name and phone number
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = titleText,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = subtitleText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            // Action call button container using primary/secondary container color
                                            Surface(
                                                shape = CircleShape,
                                                color = containerColor,
                                                tonalElevation = 2.dp,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = "Call via ${acc.displayName}",
                                                        tint = onContainerColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Dismiss button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onDismissRequest,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
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
    } else {
        SideEffect {
            onDismissRequest()
        }
    }
}

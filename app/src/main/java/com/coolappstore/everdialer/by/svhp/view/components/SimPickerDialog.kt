package com.coolappstore.everdialer.by.svhp.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.controller.util.getSimSlotForAccountHandle

@Composable
fun SimPickerDialog(
    onDismissRequest: () -> Unit,
    onSimSelected: (PhoneAccountHandle) -> Unit,
    availableAccounts: List<PhoneAccountHandle>? = null,
    title: String = "Call via"
) {
    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }

    val phoneAccounts = remember(availableAccounts) {
        if (!availableAccounts.isNullOrEmpty()) {
            availableAccounts
        } else if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val raw = telecomManager.callCapablePhoneAccounts
                val seen = HashSet<String>()
                raw.filter { handle ->
                    val info = try {
                        telecomManager.getPhoneAccount(handle)
                    } catch (_: Throwable) {
                        null
                    }
                    val isEnabled = info != null && info.isEnabled
                    if (!isEnabled) return@filter false

                    val key = info.label?.toString().orEmpty() + "|" + info.address?.toString().orEmpty()
                    seen.add(key)
                }
            } catch (_: Throwable) {
                emptyList()
            }
        } else emptyList()
    }

    if (phoneAccounts.isNotEmpty()) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(phoneAccounts) { handle ->
                                    val info = try { telecomManager?.getPhoneAccount(handle) } catch (_: Throwable) { null }
                                    val label = info?.label?.toString() ?: "SIM"
                                    val isSimAccount = info?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true
                                    val slotIndex = getSimSlotForAccountHandle(context, handle)
                                    val isSim1 = slotIndex == 0 || label.contains("1") || phoneAccounts.indexOf(handle) == 0
                                    val address = info?.address?.schemeSpecificPart
                                    val subtitle = if (!address.isNullOrBlank()) address else info?.shortDescription?.toString()?.takeIf { it.isNotBlank() }

                                    Surface(
                                        onClick = { onSimSelected(handle) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSim1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (isSimAccount && slotIndex in 0..1) {
                                                        Text(
                                                            text = "${slotIndex + 1}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSim1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    } else if (isSimAccount) {
                                                        Icon(
                                                            Icons.Default.SimCard,
                                                            contentDescription = null,
                                                            tint = if (isSim1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.Phone,
                                                            contentDescription = null,
                                                            tint = if (isSim1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (!subtitle.isNullOrBlank()) {
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            TextButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Cancel")
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

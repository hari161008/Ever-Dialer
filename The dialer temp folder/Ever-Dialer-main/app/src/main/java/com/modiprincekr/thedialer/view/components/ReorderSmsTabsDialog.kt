package com.modiprincekr.thedialer.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.modiprincekr.thedialer.controller.SmsViewModel
import com.modiprincekr.thedialer.modal.data.SmsFilterTab
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ReorderSmsTabsDialog(
    onDismissRequest: () -> Unit,
    smsVM: SmsViewModel = koinActivityViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val currentOrder by smsVM.filterOrder.collectAsState()
    val tabs = remember(currentOrder) { mutableStateListOf<SmsFilterTab>().apply { addAll(currentOrder) } }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                Icons.Outlined.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Reorder SMS Filters",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Customize the order of SMS filter pills displayed at the top of your messages screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                val maxListHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.5f).coerceAtLeast(240.dp)
                val scrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            key(tab.id) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tab.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val item = tabs.removeAt(index)
                                                        tabs.add(index - 1, item)
                                                        smsVM.setFilterOrder(tabs.toList())
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (index < tabs.lastIndex) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val item = tabs.removeAt(index)
                                                        tabs.add(index + 1, item)
                                                        smsVM.setFilterOrder(tabs.toList())
                                                    }
                                                },
                                                enabled = index < tabs.lastIndex,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (index < tabs.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    smsVM.resetFilterOrder()
                    tabs.clear()
                    tabs.addAll(SmsFilterTab.DEFAULT_ORDER)
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    )
}

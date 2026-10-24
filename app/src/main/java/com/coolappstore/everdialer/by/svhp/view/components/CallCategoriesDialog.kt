package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.CallCategoryItem
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject

@Composable
fun CallCategoriesDialog(
    onDismissRequest: () -> Unit,
    onAddClick: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rowHeightDp = 52.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val categoriesList = remember {
        mutableStateListOf<CallCategoryItem>().apply {
            addAll(prefs.getAllCallCategories())
        }
    }

    var hiddenCategories by remember {
        mutableStateOf(prefs.getHiddenCallCategories())
    }

    fun persistOrder() {
        prefs.setCallCategoryOrder(categoriesList.map { it.id })
    }

    fun resetToDefault() {
        prefs.resetCallCategoriesToDefault()
        categoriesList.clear()
        categoriesList.addAll(prefs.getAllCallCategories())
        hiddenCategories = prefs.getHiddenCallCategories()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(
                    onClick = onAddClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category"
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Choose which category pills are visible, and drag the handle to reorder them in the call logs section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val maxListHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.55f).coerceAtLeast(240.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    categoriesList.forEach { item ->
                        val isDragging = draggedKey == item.id
                        val isChecked = !hiddenCategories.contains(item.id)

                        key(item.id) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (isDragging) 6.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .zIndex(if (isDragging) 2f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(rowHeightDp)
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (item.isCustom) {
                                        IconButton(
                                            onClick = {
                                                prefs.deleteCustomCallCategory(item.id)
                                                categoriesList.removeAll { it.id == item.id }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete ${item.name}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            prefs.setCallCategoryHidden(item.id, !checked)
                                            hiddenCategories = prefs.getHiddenCallCategories()
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )

                                    Spacer(Modifier.width(4.dp))

                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = "Reorder ${item.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .pointerInput(item.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedKey = item.id
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedKey = null
                                                        dragOffsetY = 0f
                                                        persistOrder()
                                                    },
                                                    onDragCancel = {
                                                        draggedKey = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val currentIdx = categoriesList.indexOfFirst { it.id == item.id }
                                                        if (currentIdx != -1) {
                                                            val threshold = rowHeightPx * 0.6f
                                                            if (dragOffsetY > threshold && currentIdx < categoriesList.lastIndex) {
                                                                val elem = categoriesList.removeAt(currentIdx)
                                                                categoriesList.add(currentIdx + 1, elem)
                                                                dragOffsetY -= rowHeightPx
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                val elem = categoriesList.removeAt(currentIdx)
                                                                categoriesList.add(currentIdx - 1, elem)
                                                                dragOffsetY += rowHeightPx
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = { resetToDefault() }) {
                Text("Default")
            }
        }
    )
}

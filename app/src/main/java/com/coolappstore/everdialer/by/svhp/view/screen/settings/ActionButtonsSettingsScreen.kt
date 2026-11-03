package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoIconBox
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private fun Modifier.immediateDrag(
    key: Any?,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this.pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        onDragStart()
        val pointerId = down.id
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId }
            if (change == null) {
                onDragEnd()
                break
            }
            if (change.pressed) {
                val delta = change.positionChange()
                if (delta != Offset.Zero) onDrag(delta)
                change.consume()
            } else {
                change.consume()
                onDragEnd()
                break
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ActionButtonsSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()

    var hangupWidth by remember {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.5f).coerceIn(0.1f, 1.0f))
    }

    val buttonOrder = remember {
        mutableStateListOf<String>().apply { addAll(CallButtonPrefs.getOrder(prefs)) }
    }
    val enabledMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            CallButtonPrefs.ALL_IDS.forEach { put(it, CallButtonPrefs.isEnabled(prefs, it)) }
        }
    }
    var showButtonsMenu by remember { mutableStateOf(false) }

    var freeformEnabled by remember { mutableStateOf(CallButtonPrefs.isFreeformEnabled(prefs)) }
    val freeformPositions = remember {
        mutableStateMapOf<String, Offset>().apply {
            CallButtonPrefs.getFreeformPositions(prefs).forEach { (id, xy) -> put(id, Offset(xy.first, xy.second)) }
        }
    }

    var showNamesEnabled by remember { mutableStateOf(CallButtonPrefs.isShowNamesEnabled(prefs)) }
    var elementSize by remember { mutableFloatStateOf(CallButtonPrefs.getElementSize(prefs)) }
    var containerHeight by remember { mutableFloatStateOf(CallButtonPrefs.getContainerHeight(prefs)) }

    fun resetButtonLayout() {
        buttonOrder.clear()
        buttonOrder.addAll(CallButtonPrefs.DEFAULT_ORDER.split(",").map { it.trim() })
        val defaultDisabled = CallButtonPrefs.DEFAULT_DISABLED.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        CallButtonPrefs.ALL_IDS.forEach { enabledMap[it] = it !in defaultDisabled }
        CallButtonPrefs.setOrder(prefs, buttonOrder)
        CallButtonPrefs.setDisabled(prefs, defaultDisabled)
        containerHeight = CallButtonPrefs.CONTAINER_HEIGHT_DEFAULT
        CallButtonPrefs.setContainerHeight(prefs, CallButtonPrefs.CONTAINER_HEIGHT_DEFAULT)
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Action Buttons",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // ── TOP PINNED NON-SCROLLABLE PREVIEW ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                RivoExpressiveCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        FeatureButtonsPreview(
                            buttonOrder = buttonOrder,
                            enabledMap = enabledMap,
                            hangupWidth = hangupWidth,
                            freeformEnabled = freeformEnabled,
                            showNamesEnabled = showNamesEnabled,
                            elementSize = elementSize,
                            containerHeight = containerHeight,
                            freeformPositions = freeformPositions,
                            showButtonsMenu = showButtonsMenu,
                            onShowButtonsMenuChange = { showButtonsMenu = it },
                            onFreeformPositionsChanged = {
                                CallButtonPrefs.setFreeformPositions(
                                    prefs,
                                    freeformPositions.mapValues { (_, offset) -> offset.x to offset.y }
                                )
                            },
                            onOrderChanged = { CallButtonPrefs.setOrder(prefs, buttonOrder) },
                            onResetLayout = {
                                resetButtonLayout()
                                freeformPositions.clear()
                                CallButtonPrefs.setFreeformPositions(prefs, emptyMap())
                            }
                        )
                    }
                }
            }

            // ── SCROLLABLE SETTINGS & SIZING OPTIONS BELOW ────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Button Options & Toggles
                RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        Text(
                            "Layout & Behavior",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Freeform",
                                supporting = "Drag and drop buttons anywhere in the preview, instead of snapping to the grid",
                                leadingIcon = Icons.Default.DashboardCustomize,
                                iconContainerColor = Color(0xFF673AB7),
                                checked = freeformEnabled,
                                onCheckedChange = {
                                    freeformEnabled = it
                                    CallButtonPrefs.setFreeformEnabled(prefs, it)
                                }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Show Names",
                                supporting = "Show or hide the text label below each button icon",
                                leadingIcon = Icons.Default.TextFields,
                                iconContainerColor = Color(0xFF2196F3),
                                checked = showNamesEnabled,
                                onCheckedChange = {
                                    showNamesEnabled = it
                                    CallButtonPrefs.setShowNamesEnabled(prefs, it)
                                }
                            )
                        }
                    }
                }

                // Card 2: Sizing Controls
                RivoAnimatedSection(delayMs = 20L) {
                    Column {
                        Text(
                            "Sizing",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // 1. Container Height Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RivoIconBox(
                                        icon = Icons.Default.Height,
                                        iconContainerColor = Color(0xFF009688),
                                        size = 40.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Container Height",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Adjust the height of the container in the ongoing call screen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = containerHeight,
                                        onValueChange = { containerHeight = it },
                                        onValueChangeFinished = {
                                            CallButtonPrefs.setContainerHeight(prefs, containerHeight)
                                        },
                                        valueRange = CallButtonPrefs.CONTAINER_HEIGHT_MIN..CallButtonPrefs.CONTAINER_HEIGHT_MAX,
                                        steps = 19,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Compact", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(containerHeight * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Tall", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                // 2. Icon Size Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RivoIconBox(
                                        icon = Icons.Default.PhotoSizeSelectLarge,
                                        iconContainerColor = Color(0xFF2196F3),
                                        size = 40.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Icon Size",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Adjust the size of the ongoing call icons",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = elementSize,
                                        onValueChange = { elementSize = it },
                                        onValueChangeFinished = {
                                            CallButtonPrefs.setElementSize(prefs, elementSize)
                                        },
                                        valueRange = CallButtonPrefs.ELEMENT_SIZE_MIN..CallButtonPrefs.ELEMENT_SIZE_MAX,
                                        steps = 14,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(elementSize * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Large", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                // 3. Hang Up Button Width
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RivoIconBox(
                                        icon = Icons.Default.CallEnd,
                                        iconContainerColor = Color(0xFFD32F2F),
                                        size = 40.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Hang Up Button Width",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Adjust the width of the hang up button",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Live mini preview of hang up
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isCircle = hangupWidth <= 0.1f
                                    Surface(
                                        shape = if (isCircle) CircleShape else RoundedCornerShape(24.dp),
                                        color = Color(0xFFD32F2F),
                                        modifier = if (isCircle) Modifier.size(52.dp)
                                            else Modifier.fillMaxWidth(hangupWidth.coerceIn(0.1f, 1.0f)).height(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CallEnd,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                if (hangupWidth > 0.5f) {
                                                    Text(
                                                        "End Call",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = hangupWidth,
                                        onValueChange = { hangupWidth = it },
                                        onValueChangeFinished = {
                                            prefs.setFloat(PreferenceManager.KEY_HANGUP_WIDTH, hangupWidth)
                                        },
                                        valueRange = 0.1f..1.0f,
                                        steps = 8,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFD32F2F),
                                            activeTrackColor = Color(0xFFD32F2F),
                                            inactiveTrackColor = Color(0xFFD32F2F).copy(alpha = 0.3f)
                                        )
                                    )
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Narrow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(hangupWidth * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Full Width", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live preview of the ongoing-call button layout.
 * Header:
 * - "Preview" label
 * - "Reset Layout" button with pill-style background
 * - Plus icon with circular background beside Reset Layout
 * - Dropdown menu using Switch toggle design instead of checkboxes
 */
@Composable
private fun FeatureButtonsPreview(
    buttonOrder: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    enabledMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    hangupWidth: Float,
    freeformEnabled: Boolean,
    showNamesEnabled: Boolean,
    elementSize: Float,
    containerHeight: Float = 1.0f,
    freeformPositions: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Offset>,
    showButtonsMenu: Boolean,
    onShowButtonsMenuChange: (Boolean) -> Unit,
    onFreeformPositionsChanged: () -> Unit,
    onOrderChanged: () -> Unit,
    onResetLayout: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val gridIds = buttonOrder.filter { it != CallButtonPrefs.ID_HANGUP && (enabledMap[it] ?: true) }

    var selectedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(freeformEnabled, gridIds) {
        if (freeformEnabled || selectedId !in gridIds) selectedId = null
    }

    // ── Header Row: Preview label + Pill Reset Layout + Plus Button with Circular background ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reset Layout with a pill-style background
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onResetLayout() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Layout",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Reset Layout",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Plus icon with a circular background beside Reset Layout
            Box {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { onShowButtonsMenuChange(true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Show/hide buttons",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                DropdownMenu(
                    expanded = showButtonsMenu,
                    onDismissRequest = { onShowButtonsMenuChange(false) },
                    modifier = Modifier.widthIn(min = 240.dp)
                ) {
                    buttonOrder.forEach { id ->
                        val spec = CallButtonPrefs.specFor(id) ?: return@forEach
                        val locked = id in CallButtonPrefs.ALWAYS_ENABLED
                        val itemEnabled = enabledMap[id] ?: true
                        DropdownMenuItem(
                            text = {
                                Text(
                                    spec.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (itemEnabled) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    spec.icon,
                                    contentDescription = null,
                                    tint = if (itemEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = itemEnabled,
                                    onCheckedChange = null,
                                    enabled = !locked,
                                    modifier = Modifier.scale(0.8f)
                                )
                            },
                            enabled = !locked,
                            onClick = {
                                if (!locked) {
                                    val newVal = !(enabledMap[id] ?: true)
                                    enabledMap[id] = newVal
                                    CallButtonPrefs.setEnabled(prefs, id, newVal)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    // ── Preview Canvas ──
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2E2622),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .defaultMinSize(minHeight = 170.dp + 150.dp * (containerHeight - 0.6f))
        ) {
            val previewVerticalPadding = (10.dp + 42.dp * (containerHeight - 0.6f)).coerceIn(8.dp, 60.dp)
            val rowSpacing = (8.dp + 26.dp * (containerHeight - 0.6f)).coerceIn(6.dp, 40.dp)
            val hangupSpacing = (12.dp + 34.dp * (containerHeight - 0.6f)).coerceIn(8.dp, 50.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = previewVerticalPadding, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (freeformEnabled) {
                    FreeformButtonsArea(
                        gridIds = gridIds + CallButtonPrefs.ID_HANGUP,
                        enabledMap = enabledMap,
                        freeformPositions = freeformPositions,
                        onPositionsChanged = onFreeformPositionsChanged,
                        elementSize = elementSize,
                        containerHeight = containerHeight,
                        showNamesEnabled = showNamesEnabled
                    )
                } else {
                    gridIds.chunked(3).forEachIndexed { rowIndex, rowIds ->
                        if (rowIndex > 0) Spacer(Modifier.height(rowSpacing))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowIds.forEach { id ->
                                val spec = CallButtonPrefs.specFor(id) ?: return@forEach
                                val isSelected = selectedId == id

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width((56.dp * elementSize + 20.dp).coerceAtLeast(76.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (selectedId == null) {
                                                selectedId = id
                                            } else if (selectedId == id) {
                                                selectedId = null
                                            } else {
                                                val firstId = selectedId!!
                                                val i1 = buttonOrder.indexOf(firstId)
                                                val i2 = buttonOrder.indexOf(id)
                                                if (i1 != -1 && i2 != -1) {
                                                    buttonOrder[i1] = id
                                                    buttonOrder[i2] = firstId
                                                    onOrderChanged()
                                                }
                                                selectedId = null
                                            }
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.16f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.size(56.dp * elementSize)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                spec.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                                modifier = Modifier.size(24.dp * elementSize)
                                            )
                                        }
                                    }
                                    if (showNamesEnabled) {
                                        Text(
                                            spec.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(hangupSpacing))

                    val isCircle = hangupWidth <= 0.1f
                    Surface(
                        shape = if (isCircle) CircleShape else RoundedCornerShape(28.dp),
                        color = Color(0xFFD32F2F),
                        modifier = if (isCircle) Modifier.size(56.dp)
                            else Modifier.fillMaxWidth(hangupWidth.coerceIn(0.1f, 1.0f)).height(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                if (hangupWidth > 0.5f) {
                                    Text("End Call", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        if (freeformEnabled)
            "Drag any button — including Hang Up — anywhere in the preview to place it."
        else
            "Tap a button, then tap another to swap their positions. Hang Up always stays last.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FreeformButtonsArea(
    gridIds: List<String>,
    enabledMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    freeformPositions: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Offset>,
    onPositionsChanged: () -> Unit,
    elementSize: Float,
    containerHeight: Float = 1.0f,
    showNamesEnabled: Boolean
) {
    val density = LocalDensity.current
    val rows = if (gridIds.isEmpty()) 1 else ((gridIds.size + 2) / 3)
    val areaHeight = ((rows * 75).dp + 150.dp * (containerHeight - 0.6f)).coerceAtLeast(100.dp)
    val tileWidth = (56.dp * elementSize + 20.dp).coerceAtLeast(76.dp)
    val tileWidthPx = with(density) { tileWidth.toPx() }
    val tileHeightPx = with(density) { 88.dp.toPx() }

    fun defaultFraction(id: String, index: Int): Offset {
        val (x, y) = CallButtonPrefs.defaultFreeformFraction(id, index, gridIds.size)
        return Offset(x, y)
    }

    var draggingId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(areaHeight)
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        gridIds.forEachIndexed { index, id ->
            val spec = CallButtonPrefs.specFor(id) ?: return@forEachIndexed
            val isHangup = id == CallButtonPrefs.ID_HANGUP
            val fraction = freeformPositions[id] ?: defaultFraction(id, index)
            val isDragging = draggingId == id

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        val cx = fraction.x * containerWidthPx - tileWidthPx / 2f
                        val cy = fraction.y * containerHeightPx - tileHeightPx / 2f
                        IntOffset(cx.roundToInt(), cy.roundToInt())
                    }
                    .width(tileWidth)
                    .immediateDrag(
                        key = id,
                        onDragStart = {
                            draggingId = id
                        },
                        onDrag = { delta ->
                            if (containerWidthPx > 0f && containerHeightPx > 0f) {
                                val current = freeformPositions[id] ?: defaultFraction(id, index)
                                val newX = (current.x + delta.x / containerWidthPx).coerceIn(0f, 1f)
                                val newY = (current.y + delta.y / containerHeightPx).coerceIn(0f, 1f)
                                freeformPositions[id] = Offset(newX, newY)
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            onPositionsChanged()
                        }
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isHangup) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp * elementSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            spec.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp * elementSize)
                        )
                    }
                }
                if (showNamesEnabled) {
                    Text(
                        spec.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

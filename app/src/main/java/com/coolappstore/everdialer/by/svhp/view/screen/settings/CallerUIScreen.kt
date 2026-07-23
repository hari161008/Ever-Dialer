package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonSpec
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** State for the floating "ghost" copy of a button shown while it's being dragged. */
private data class DragGhostState(
    val spec: CallButtonSpec,
    val windowPosition: Offset
)

/**
 * Drag detection that claims the gesture the instant a finger goes down, instead of waiting for
 * touch-slop to be exceeded in a particular direction (as `detectDragGestures` / long-press
 * variants do). That wait was the root cause of drags being cut short or refusing to start
 * inside a scrollable settings list: the ancestor `LazyColumn` runs its own scroll-gesture
 * detector at the same time, and whichever one crosses its slop threshold first "wins" the
 * gesture — the list routinely won before our long-press/slop check even finished, especially
 * on any drag with a vertical component. Consuming the initial pointer-down here means the
 * ancestor scrollable never gets a chance to claim the gesture at all.
 */
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
@Destination<RootGraph>
@Composable
fun CallerUIScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()

    var hangupWidth by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.5f).coerceIn(0.1f, 1.0f)) }

    // ── Feature Buttons state ────────────────────────────────────────────
    val buttonOrder = remember {
        mutableStateListOf<String>().apply { addAll(CallButtonPrefs.getOrder(prefs)) }
    }
    val enabledMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            CallButtonPrefs.ALL_IDS.forEach { put(it, CallButtonPrefs.isEnabled(prefs, it)) }
        }
    }
    var showButtonsMenu by remember { mutableStateOf(false) }

    // Freeform layout — when on, buttons can be dropped anywhere in the preview instead of
    // snapping into the fixed 3-per-row grid. Off (unticked) by default.
    var freeformEnabled by remember { mutableStateOf(CallButtonPrefs.isFreeformEnabled(prefs)) }
    val freeformPositions = remember {
        mutableStateMapOf<String, Offset>().apply {
            CallButtonPrefs.getFreeformPositions(prefs).forEach { (id, xy) -> put(id, Offset(xy.first, xy.second)) }
        }
    }

    // Drag-ghost overlay state: while a button is being dragged, a floating copy of it is drawn
    // in a layer above *everything* (including the card that would otherwise clip it), so it can
    // be dragged anywhere on screen — not just within the small preview card's bounds.
    var dragGhost by remember { mutableStateOf<DragGhostState?>(null) }
    var overlayRootWindowOffset by remember { mutableStateOf(Offset.Zero) }

    // True while any button in the preview is actively being dragged. Used to freeze the outer
    // settings list's scrolling for the duration of the drag, so the screen never scrolls out
    // from under a finger that's mid-drag (which previously made drags feel like they got cut
    // short / auto-dropped before reaching where the user intended).
    var isDraggingAnyButton by remember { mutableStateOf(false) }

    fun resetButtonLayout() {
        buttonOrder.clear()
        buttonOrder.addAll(CallButtonPrefs.DEFAULT_ORDER.split(",").map { it.trim() })
        val defaultDisabled = CallButtonPrefs.DEFAULT_DISABLED.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        CallButtonPrefs.ALL_IDS.forEach { enabledMap[it] = it !in defaultDisabled }
        CallButtonPrefs.setOrder(prefs, buttonOrder)
        CallButtonPrefs.setDisabled(prefs, defaultDisabled)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caller UI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .onGloballyPositioned { overlayRootWindowOffset = it.positionInWindow() }
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            // Disabled while a Feature Button is being dragged so the list can never scroll out
            // from under the drag — see [isDraggingAnyButton].
            userScrollEnabled = !isDraggingAnyButton
        ) {

            // ── Feature Buttons ───────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 40L) {
                    Column {
                        Text(
                            "Feature Buttons",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Widgets,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Ongoing Call Buttons",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Drag to reorder, use the menu to show or hide",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { showButtonsMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Show/hide buttons")
                                        }
                                        DropdownMenu(
                                            expanded = showButtonsMenu,
                                            onDismissRequest = { showButtonsMenu = false }
                                        ) {
                                            buttonOrder.forEach { id ->
                                                val spec = CallButtonPrefs.specFor(id) ?: return@forEach
                                                val locked = id in CallButtonPrefs.ALWAYS_ENABLED
                                                val itemEnabled = enabledMap[id] ?: true
                                                DropdownMenuItem(
                                                    text = { Text(spec.label) },
                                                    leadingIcon = {
                                                        Icon(
                                                            spec.icon,
                                                            contentDescription = null,
                                                            tint = if (itemEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Checkbox(
                                                            checked = itemEnabled,
                                                            onCheckedChange = null,
                                                            enabled = !locked
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

                                Spacer(Modifier.height(16.dp))

                                // ── Draggable preview of the ongoing call button layout ──
                                FeatureButtonsPreview(
                                    buttonOrder = buttonOrder,
                                    enabledMap = enabledMap,
                                    hangupWidth = hangupWidth,
                                    freeformEnabled = freeformEnabled,
                                    onFreeformEnabledChanged = {
                                        freeformEnabled = it
                                        CallButtonPrefs.setFreeformEnabled(prefs, it)
                                    },
                                    freeformPositions = freeformPositions,
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
                                    },
                                    onDragGhostChange = { dragGhost = it },
                                    onDragActiveChanged = { isDraggingAnyButton = it }
                                )
                            }
                        }
                    }
                }
            }

            // ── Hang Up Button ────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        Text(
                            "Hang Up Button",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CallEnd,
                                                contentDescription = null,
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "Customise Width",
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

                                Spacer(Modifier.height(20.dp))

                                // Live preview
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isCircle = hangupWidth <= 0.1f
                                    Surface(
                                        shape = if (isCircle) CircleShape else RoundedCornerShape(28.dp),
                                        color = Color(0xFFD32F2F),
                                        modifier = if (isCircle) Modifier.size(64.dp)
                                            else Modifier.fillMaxWidth(hangupWidth.coerceIn(0.1f, 1.0f)).height(64.dp)
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
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                if (hangupWidth > 0.5f) {
                                                    Text(
                                                        "End Call",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Narrow",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(hangupWidth * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Full Width",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating drag-ghost — deliberately rendered as a sibling of (not nested inside) the
        // scrollable content and the card that clips it, at the top of this Box's z-order, so a
        // dragged button is visible anywhere on screen instead of being clipped to the card.
        dragGhost?.let { ghost ->
            val density = LocalDensity.current
            val localPos = ghost.windowPosition - overlayRootWindowOffset
            val sizeDp = 64.dp
            val sizePx = with(density) { sizeDp.toPx() }
            Box(
                modifier = Modifier
                    .offset { IntOffset((localPos.x - sizePx / 2f).roundToInt(), (localPos.y - sizePx / 2f).roundToInt()) }
                    .size(sizeDp)
                    .zIndex(100f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(ghost.spec.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        }
    }
}

/**
 * Draggable, live preview of the ongoing-call button layout — laid out exactly like the real
 * call screen (a 3-per-row grid of circular icon buttons with a label underneath, plus the red
 * Hang Up pill at the bottom) so what you see here is what you'll see on an actual call.
 *
 * Bug fix: the previous version only accepted drags starting on a tiny 20dp drag-handle icon,
 * detected via `detectDragGesturesAfterLongPress` nested inside the settings screen's scrolling
 * list — the outer scroll routinely won the gesture before the long-press even registered, so
 * dragging effectively never worked. This version makes the *entire* button tile a drag target
 * and reorders by continuously comparing the dragged tile's live position against every other
 * tile's on-screen position (via [onGloballyPositioned]) rather than assuming a fixed 1-column
 * layout, so it works correctly across the multi-column grid too.
 *
 * Hang Up is intentionally excluded from the draggable grid: [CallButtonPrefs.getOrder] always
 * forces it back to the last position and [CallButtonPrefs.getActiveActionIds] excludes it
 * entirely, since the real call screen always renders it separately as the dedicated end-call
 * action — so it's shown here the same way, as a fixed preview matching the current width
 * setting rather than a reorderable tile.
 */
@Composable
private fun FeatureButtonsPreview(
    buttonOrder: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    enabledMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    hangupWidth: Float,
    freeformEnabled: Boolean,
    onFreeformEnabledChanged: (Boolean) -> Unit,
    freeformPositions: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Offset>,
    onFreeformPositionsChanged: () -> Unit,
    onOrderChanged: () -> Unit,
    onResetLayout: () -> Unit,
    onDragGhostChange: (DragGhostState?) -> Unit,
    onDragActiveChanged: (Boolean) -> Unit
) {
    val gridIds = buttonOrder.filter { it != CallButtonPrefs.ID_HANGUP }

    var draggingId by remember { mutableStateOf<String?>(null) }
    // Absolute (window-coordinate) center of the floating ghost while a drag is in progress.
    // Deliberately kept at this stable parent scope (not inside the per-tile Column below) so it
    // survives tiles being reordered/recomposed mid-drag instead of resetting to zero.
    var currentGhostCenter by remember { mutableStateOf(Offset.Zero) }
    // Each tile's last-measured on-screen bounds (in window coordinates), used both to hit-test
    // the dragged tile's current position against every other tile, and to know where to start
    // the floating ghost from.
    val tileBounds = remember { mutableStateMapOf<String, Rect>() }

    // ── Freeform toggle — sits above the "Preview" heading. When on, buttons can be dropped
    // anywhere inside the preview area instead of snapping into the fixed 3-per-row grid.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onFreeformEnabledChanged(!freeformEnabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Freeform",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Drag and drop buttons anywhere in the preview, instead of snapping to the grid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = freeformEnabled, onCheckedChange = onFreeformEnabledChanged)
    }
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onResetLayout) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset Layout")
        }
    }
    Spacer(Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF2E2622) // approximates the ongoing-call screen's dark overlay so the preview reads the same as the real thing
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (freeformEnabled) {
                FreeformButtonsArea(
                    gridIds = gridIds,
                    enabledMap = enabledMap,
                    freeformPositions = freeformPositions,
                    onDragActiveChanged = onDragActiveChanged,
                    onPositionsChanged = onFreeformPositionsChanged
                )
            } else {
                gridIds.chunked(3).forEachIndexed { rowIndex, rowIds ->
                    if (rowIndex > 0) Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowIds.forEach { id ->
                            val spec = CallButtonPrefs.specFor(id) ?: return@forEach
                            val isEnabled = enabledMap[id] ?: true
                            val isDragging = draggingId == id

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        tileBounds[id] = Rect(
                                            offset = coords.positionInWindow(),
                                            size = coords.size.toSize()
                                        )
                                    }
                                    // The original tile fades out while its floating ghost (rendered
                                    // above everything, unclipped by this card) takes over showing
                                    // where it's being dragged to.
                                    .alpha(if (isDragging) 0f else 1f)
                                    .immediateDrag(
                                        key = id,
                                        onDragStart = {
                                            draggingId = id
                                            onDragActiveChanged(true)
                                            currentGhostCenter = tileBounds[id]?.center ?: Offset.Zero
                                            onDragGhostChange(DragGhostState(spec, currentGhostCenter))
                                        },
                                        onDrag = { delta ->
                                            currentGhostCenter += delta
                                            onDragGhostChange(DragGhostState(spec, currentGhostCenter))

                                            val targetId = tileBounds
                                                .filterKeys { it != id }
                                                .minByOrNull { (_, rect) -> (rect.center - currentGhostCenter).getDistance() }
                                                ?.key
                                            if (targetId != null) {
                                                val targetRect = tileBounds.getValue(targetId)
                                                if ((targetRect.center - currentGhostCenter).getDistance() < targetRect.width / 2f) {
                                                    val fromIndex = buttonOrder.indexOf(id)
                                                    val toIndex = buttonOrder.indexOf(targetId)
                                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                        val item = buttonOrder.removeAt(fromIndex)
                                                        buttonOrder.add(toIndex, item)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            onDragActiveChanged(false)
                                            onDragGhostChange(null)
                                            onOrderChanged()
                                        }
                                    )
                                    .width(76.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isEnabled) Color.White.copy(alpha = 0.16f)
                                            else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            spec.icon,
                                            contentDescription = null,
                                            tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    spec.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isEnabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                        // Pad out the row with invisible spacers so a partial last row still aligns
                        // left-to-right the same way the real call screen's SpaceEvenly row does.
                        repeat(3 - rowIds.size) {
                            Spacer(modifier = Modifier.width(76.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Fixed (non-draggable) Hang Up preview, matching the current width setting below.
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

    Spacer(Modifier.height(8.dp))
    Text(
        if (freeformEnabled)
            "Drag a button anywhere in the preview to place it. Hang Up always stays fixed at the bottom, matching the real call screen."
        else
            "Drag a button anywhere to reorder it. Hang Up always stays last, matching the real call screen.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Freeform drag area — buttons can be dropped anywhere within these bounds rather than snapping
 * into the fixed 3-per-row grid. Positions are stored as fractions (0f..1f) of this area's size
 * so the layout scales correctly across screen sizes; a button with no stored position yet
 * defaults to where it would sit in the normal grid, so switching Freeform on doesn't jumble
 * the layout the user already had.
 */
@Composable
private fun FreeformButtonsArea(
    gridIds: List<String>,
    enabledMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    freeformPositions: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Offset>,
    onDragActiveChanged: (Boolean) -> Unit,
    onPositionsChanged: () -> Unit
) {
    val density = LocalDensity.current
    val rows = if (gridIds.isEmpty()) 1 else ((gridIds.size + 2) / 3)
    val areaHeight = (rows * 96).dp.coerceAtLeast(120.dp)
    val tileWidthPx = with(density) { 76.dp.toPx() }
    val tileHeightPx = with(density) { 88.dp.toPx() }

    fun defaultFraction(index: Int): Offset {
        val (x, y) = CallButtonPrefs.defaultFreeformFraction(index, gridIds.size)
        return Offset(x, y)
    }

    var draggingId by remember { mutableStateOf<String?>(null) }

    // BoxWithConstraints resolves its size synchronously on first composition (unlike
    // onGloballyPositioned, whose callback only fires *after* the first layout pass) — so the
    // draggable area's pixel size is correct from the very first frame instead of momentarily
    // being zero, which previously made drags silently no-op if a user touched down too early.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(areaHeight)
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        gridIds.forEachIndexed { index, id ->
            val spec = CallButtonPrefs.specFor(id) ?: return@forEachIndexed
            val isEnabled = enabledMap[id] ?: true
            val fraction = freeformPositions[id] ?: defaultFraction(index)
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
                    .width(76.dp)
                    .immediateDrag(
                        key = id,
                        onDragStart = {
                            draggingId = id
                            onDragActiveChanged(true)
                        },
                        onDrag = { delta ->
                            if (containerWidthPx > 0f && containerHeightPx > 0f) {
                                val current = freeformPositions[id] ?: defaultFraction(index)
                                val newX = (current.x + delta.x / containerWidthPx).coerceIn(0f, 1f)
                                val newY = (current.y + delta.y / containerHeightPx).coerceIn(0f, 1f)
                                freeformPositions[id] = Offset(newX, newY)
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            onDragActiveChanged(false)
                            onPositionsChanged()
                        }
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isEnabled) Color.White.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            spec.icon,
                            contentDescription = null,
                            tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    spec.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEnabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

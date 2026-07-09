package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                                            tint = if (itemEnabled) spec.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                                    onOrderChanged = { CallButtonPrefs.setOrder(prefs, buttonOrder) }
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
    onOrderChanged: () -> Unit
) {
    val gridIds = buttonOrder.filter { it != CallButtonPrefs.ID_HANGUP }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragTotal by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    // Each tile's last-measured on-screen bounds, used to hit-test the dragged tile's current
    // position against every other tile while dragging.
    val tileBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF2E2622) // approximates the ongoing-call screen's dark overlay so the preview reads the same as the real thing
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                                    tileBounds[id] = androidx.compose.ui.geometry.Rect(
                                        offset = coords.positionInWindow(),
                                        size = coords.size.toSize()
                                    )
                                }
                                .graphicsLayer {
                                    translationX = if (isDragging) dragTotal.x else 0f
                                    translationY = if (isDragging) dragTotal.y else 0f
                                    scaleX = if (isDragging) 1.08f else 1f
                                    scaleY = if (isDragging) 1.08f else 1f
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .pointerInput(id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingId = id
                                            dragTotal = androidx.compose.ui.geometry.Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragTotal += amount
                                            val myBounds = tileBounds[id] ?: return@detectDragGesturesAfterLongPress
                                            val currentCenter = myBounds.center + dragTotal
                                            val targetId = tileBounds
                                                .filterKeys { it != id }
                                                .minByOrNull { (_, rect) -> (rect.center - currentCenter).getDistance() }
                                                ?.key
                                            if (targetId != null) {
                                                val targetRect = tileBounds.getValue(targetId)
                                                if ((targetRect.center - currentCenter).getDistance() < targetRect.width / 2f) {
                                                    val fromIndex = buttonOrder.indexOf(id)
                                                    val toIndex = buttonOrder.indexOf(targetId)
                                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                        val item = buttonOrder.removeAt(fromIndex)
                                                        buttonOrder.add(toIndex, item)
                                                        // Re-anchor so the drag continues smoothly from here
                                                        // instead of jumping once tiles reflow.
                                                        dragTotal = currentCenter - myBounds.center
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragTotal = androidx.compose.ui.geometry.Offset.Zero
                                            onOrderChanged()
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragTotal = androidx.compose.ui.geometry.Offset.Zero
                                        }
                                    )
                                }
                                .width(76.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isEnabled) spec.color.copy(alpha = if (isDragging) 0.9f else 0.75f)
                                        else Color.White.copy(alpha = 0.10f),
                                tonalElevation = if (isDragging) 6.dp else 0.dp,
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
        "Long-press and drag a button to reorder it. Hang Up always stays last, matching the real call screen.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

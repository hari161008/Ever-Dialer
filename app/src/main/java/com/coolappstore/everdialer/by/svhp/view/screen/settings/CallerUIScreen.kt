package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

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

    var showOngoingCallUIWhenAnswered by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true))
    }

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

    // True while any button in the preview is actively being dragged. Used to freeze the outer
    // settings list's scrolling for the duration of the drag, so the screen never scrolls out
    // from under a finger that's mid-drag (which previously made drags feel like they got cut
    // short / auto-dropped before reaching where the user intended).
    var isDraggingAnyButton by remember { mutableStateOf(false) }

    // Show Names — whether the text label is shown below each Feature Button / Hang Up icon
    // on the real ongoing-call screen. On (ticked) by default.
    var showNamesEnabled by remember { mutableStateOf(CallButtonPrefs.isShowNamesEnabled(prefs)) }

    // Element Size — scale factor applied to each Feature Button / Hang Up icon's size on the
    // real ongoing-call screen.
    var elementSize by remember { mutableFloatStateOf(CallButtonPrefs.getElementSize(prefs)) }

    fun resetButtonLayout() {
        buttonOrder.clear()
        buttonOrder.addAll(CallButtonPrefs.DEFAULT_ORDER.split(",").map { it.trim() })
        val defaultDisabled = CallButtonPrefs.DEFAULT_DISABLED.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        CallButtonPrefs.ALL_IDS.forEach { enabledMap[it] = it !in defaultDisabled }
        CallButtonPrefs.setOrder(prefs, buttonOrder)
        CallButtonPrefs.setDisabled(prefs, defaultDisabled)
    }

    var ongoingBgType by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_ONGOING_BG_TYPE, "none") ?: "none")
    }
    var showContactPfp by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, true))
    }

    var showBgOptionsPopup by remember { mutableStateOf(false) }
    var editorMediaState by remember {
        mutableStateOf<Triple<java.io.File, Boolean, String>?>(null)
    }

    val bgLabel = when (ongoingBgType) {
        "wallpaper" -> "Device Wallpaper"
        "picture"   -> "Custom Picture"
        "video"     -> "Custom Video"
        else        -> "None (Default)"
    }

    if (showBgOptionsPopup) {
        com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundOptionsPopup(
            target = com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundTarget.ONGOING,
            currentType = ongoingBgType,
            onDismiss = { showBgOptionsPopup = false },
            onSelectNone = {
                ongoingBgType = "none"
                prefs.setString(PreferenceManager.KEY_ONGOING_BG_TYPE, "none")
                prefs.setString(PreferenceManager.KEY_ONGOING_BG_PATH, "")
            },
            onOpenEditor = { file, isVideo, bgType ->
                showBgOptionsPopup = false
                editorMediaState = Triple(file, isVideo, bgType)
            }
        )
    }

    editorMediaState?.let { (file, isVideo, bgType) ->
        com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundEditorDialog(
            target = com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundTarget.ONGOING,
            mediaFile = file,
            isVideo = isVideo,
            bgType = bgType,
            initialZoom = prefs.getFloat(PreferenceManager.KEY_ONGOING_BG_ZOOM, 1f),
            initialPanX = prefs.getFloat(PreferenceManager.KEY_ONGOING_BG_PAN_X, 0f),
            initialPanY = prefs.getFloat(PreferenceManager.KEY_ONGOING_BG_PAN_Y, 0f),
            initialDim = prefs.getFloat(PreferenceManager.KEY_ONGOING_BG_DIM, 0f),
            initialBlur = prefs.getFloat(PreferenceManager.KEY_ONGOING_BG_BLUR, 0f),
            onDismiss = { editorMediaState = null },
            onSaveSuccess = {
                ongoingBgType = bgType
                editorMediaState = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Ongoing Call UI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            // Disabled while a Feature Button is being dragged so the list can never scroll out
            // from under the drag — see [isDraggingAnyButton].
            userScrollEnabled = !isDraggingAnyButton
        ) {
            item {
                com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)
            }

            // ── Show ongoing call UI when the call is answered ───────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Show ongoing call UI when the call is answered",
                            supporting = "Display the full screen in-call screen when a call is answered",
                            leadingIcon = Icons.Outlined.Call,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = showOngoingCallUIWhenAnswered,
                            onCheckedChange = {
                                showOngoingCallUIWhenAnswered = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, it)
                            }
                        )
                    }
                }
            }

            // ── Custom Background & Contact Photo ───────────────
            item {
                RivoAnimatedSection(delayMs = 20L) {
                    Column {
                        Text(
                            "Appearance & Background",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            com.coolappstore.everdialer.by.svhp.view.components.RivoListItem(
                                headline = "Choose Custom Background",
                                supporting = "Currently: $bgLabel",
                                leadingIcon = Icons.Outlined.Wallpaper,
                                iconContainerColor = Color(0xFF9C27B0),
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { showBgOptionsPopup = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Show Contact PFP",
                                supporting = "Display the contact avatar photo on the ongoing call screen",
                                leadingIcon = Icons.Outlined.AccountCircle,
                                iconContainerColor = Color(0xFF00BCD4),
                                checked = showContactPfp,
                                onCheckedChange = {
                                    showContactPfp = it
                                    prefs.setBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, it)
                                }
                            )
                        }
                    }
                }
            }

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
                                    showNamesEnabled = showNamesEnabled,
                                    onShowNamesEnabledChanged = {
                                        showNamesEnabled = it
                                        CallButtonPrefs.setShowNamesEnabled(prefs, it)
                                    },
                                    elementSize = elementSize,
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
                                    onDragActiveChanged = { isDraggingAnyButton = it }
                                )
                            }
                        }
                    }
                }
            }

            // ── Element Size ─────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 50L) {
                    Column {
                        Text(
                            "Element Size",
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
                                                Icons.Default.PhotoSizeSelectLarge,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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

                                Spacer(Modifier.height(20.dp))

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
                                        value = elementSize,
                                        onValueChange = { elementSize = it },
                                        onValueChangeFinished = {
                                            CallButtonPrefs.setElementSize(prefs, elementSize)
                                        },
                                        valueRange = CallButtonPrefs.ELEMENT_SIZE_MIN..CallButtonPrefs.ELEMENT_SIZE_MAX,
                                        steps = 14,
                                        modifier = Modifier.weight(1f)
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
                                        "Small",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(elementSize * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Large",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
}

/**
 * Draggable, live preview of the ongoing-call button layout — laid out exactly like the real
 * call screen (a 3-per-row grid of circular icon buttons with a label underneath, plus the red
 * Hang Up pill at the bottom) so what you see here is what you'll see on an actual call.
 *
 * Only *enabled* buttons are included in [gridIds] — a button unticked in the show/hide menu is
 * fully removed from the preview (and the real call screen) rather than shown dimmed out.
 *
 * With Freeform off, reordering is tap-to-select-then-tap-to-swap: tapping a tile highlights it
 * (see [selectedId]), and tapping a second tile swaps the two tiles' positions outright and
 * clears the selection. This replaces continuous free-dragging for the fixed grid, which doesn't
 * suit a layout that always snaps back to fixed slots anyway — tap-to-swap is unambiguous about
 * which two positions are being exchanged, with no room for a drag to land in the wrong spot.
 * Freeform's own tiles (see [FreeformButtonsArea]) keep continuous dragging, since there the
 * final position *is* the point.
 *
 * Hang Up is intentionally excluded from the tap-to-swap grid — [CallButtonPrefs.getOrder]
 * always forces it back to the last position and [CallButtonPrefs.getActiveActionIds] excludes
 * it entirely, since the real call screen always renders it separately as the dedicated end-call
 * action. With Freeform on, Hang Up instead becomes a draggable tile inside
 * [FreeformButtonsArea] (see there), so it can be positioned anywhere too.
 */
@Composable
private fun FeatureButtonsPreview(
    buttonOrder: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    enabledMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    hangupWidth: Float,
    freeformEnabled: Boolean,
    onFreeformEnabledChanged: (Boolean) -> Unit,
    showNamesEnabled: Boolean,
    onShowNamesEnabledChanged: (Boolean) -> Unit,
    elementSize: Float,
    freeformPositions: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Offset>,
    onFreeformPositionsChanged: () -> Unit,
    onOrderChanged: () -> Unit,
    onResetLayout: () -> Unit,
    onDragActiveChanged: (Boolean) -> Unit
) {
    // Disabled (unticked) buttons are dropped entirely — not shown dimmed/blanked out.
    val gridIds = buttonOrder.filter { it != CallButtonPrefs.ID_HANGUP && (enabledMap[it] ?: true) }

    // Tap-to-select-then-tap-to-swap reordering for the non-Freeform grid: tap a tile to
    // highlight it, then tap a second tile to swap their positions outright.
    var selectedId by remember { mutableStateOf<String?>(null) }
    // Selection doesn't survive a switch to Freeform (or the set of tiles changing), so it can't
    // point at a tile that's no longer in the grid.
    LaunchedEffect(freeformEnabled, gridIds) {
        if (freeformEnabled || selectedId !in gridIds) selectedId = null
    }

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
    Spacer(Modifier.height(4.dp))

    // ── Show Names toggle — whether the text label is shown below each button icon on the
    // real ongoing-call screen. On (ticked) by default.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onShowNamesEnabledChanged(!showNamesEnabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Show Names",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Show or hide the text label below each button icon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = showNamesEnabled, onCheckedChange = onShowNamesEnabledChanged)
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
                // Hang Up joins the draggable canvas in Freeform mode, so it can be positioned
                // anywhere too instead of staying pinned below as a fixed preview.
                FreeformButtonsArea(
                    gridIds = gridIds + CallButtonPrefs.ID_HANGUP,
                    enabledMap = enabledMap,
                    freeformPositions = freeformPositions,
                    onDragActiveChanged = onDragActiveChanged,
                    onPositionsChanged = onFreeformPositionsChanged,
                    elementSize = elementSize,
                    showNamesEnabled = showNamesEnabled
                )
            } else {
                gridIds.chunked(3).forEachIndexed { rowIndex, rowIds ->
                    if (rowIndex > 0) Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowIds.forEach { id ->
                            val spec = CallButtonPrefs.specFor(id) ?: return@forEach
                            val isSelected = selectedId == id

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    // Width must grow with elementSize too, otherwise once the
                                    // 56.dp*elementSize icon circle exceeds this fixed 76.dp
                                    // column it gets width-clamped by the parent while height
                                    // keeps growing unconstrained — making the icon stretch
                                    // vertically only instead of scaling uniformly.
                                    .width((56.dp * elementSize + 20.dp).coerceAtLeast(76.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val current = selectedId
                                        when {
                                            current == null -> selectedId = id
                                            current == id -> selectedId = null
                                            else -> {
                                                // Swap the two selected tiles' positions outright,
                                                // instead of shifting everything in between.
                                                val fromIndex = buttonOrder.indexOf(current)
                                                val toIndex = buttonOrder.indexOf(id)
                                                if (fromIndex != -1 && toIndex != -1) {
                                                    val tmp = buttonOrder[fromIndex]
                                                    buttonOrder[fromIndex] = buttonOrder[toIndex]
                                                    buttonOrder[toIndex] = tmp
                                                    onOrderChanged()
                                                }
                                                selectedId = null
                                            }
                                        }
                                    }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.16f),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                        // Pad out the row with invisible spacers so a partial last row still aligns
                        // left-to-right the same way the real call screen's SpaceEvenly row does.
                        repeat(3 - rowIds.size) {
                            Spacer(modifier = Modifier.width((56.dp * elementSize + 20.dp).coerceAtLeast(76.dp)))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Fixed (non-draggable) Hang Up preview, matching the current width setting
                // below. Only shown outside Freeform — in Freeform, Hang Up is a draggable tile
                // inside FreeformButtonsArea above instead.
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

    Spacer(Modifier.height(8.dp))
    Text(
        if (freeformEnabled)
            "Drag any button — including Hang Up — anywhere in the preview to place it."
        else
            "Tap a button, then tap another to swap their positions. Hang Up always stays last, matching the real call screen.",
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
    onPositionsChanged: () -> Unit,
    elementSize: Float,
    showNamesEnabled: Boolean
) {
    val density = LocalDensity.current
    val rows = if (gridIds.isEmpty()) 1 else ((gridIds.size + 2) / 3)
    val areaHeight = (rows * 96).dp.coerceAtLeast(120.dp)
    // Width must track elementSize the same way the grid layout's tile column does, otherwise
    // the 56.dp*elementSize icon circle gets width-clamped once it exceeds a fixed 76.dp tile
    // while its height keeps growing unconstrained — stretching the icon vertically only.
    val tileWidth = (56.dp * elementSize + 20.dp).coerceAtLeast(76.dp)
    val tileWidthPx = with(density) { tileWidth.toPx() }
    val tileHeightPx = with(density) { 88.dp.toPx() }

    fun defaultFraction(id: String, index: Int): Offset {
        val (x, y) = CallButtonPrefs.defaultFreeformFraction(id, index, gridIds.size)
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

        // gridIds is pre-filtered to already-enabled buttons plus (optionally) Hang Up, so every
        // tile rendered here is always shown at full strength — nothing dimmed/blanked out.
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
                            onDragActiveChanged(true)
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
                            onDragActiveChanged(false)
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

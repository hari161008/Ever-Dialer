package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.screen.settings.CardDivider
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorPurple = Color(0xFF9C27B0)
private val ColorIndigo = Color(0xFF3F51B5)

private data class ContactInfoElementOption(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val contactInfoElementOptions = listOf(
    ContactInfoElementOption("quick_actions",         "Quick Actions",                                Icons.Default.Call),
    ContactInfoElementOption("contact_info",          "Contact Info",                                 Icons.Default.Info),
    ContactInfoElementOption("social",                "Social",                                       Icons.Default.Share),
    ContactInfoElementOption("description",           "Description (Synced Notes)",                   Icons.Default.Description),
    ContactInfoElementOption("notes",                 "Note (Phone Only)",                            Icons.Default.Note),
    ContactInfoElementOption("events",                "Events & More",                                Icons.Default.Event),
    ContactInfoElementOption("recent_activity",       "Recent Activity",                              Icons.Default.History),
    ContactInfoElementOption("choose_sim",            "Choose Sim",                                   Icons.Default.SimCard),
    ContactInfoElementOption("calling_backgrounds",   "Background And Contact PFP Customisation",     Icons.Default.Wallpaper),
    ContactInfoElementOption("ringtone",              "Ringtone",                                     Icons.Default.MusicNote),
    ContactInfoElementOption("choose_default_number", "Choose Default Number",                        Icons.Default.Numbers),
    ContactInfoElementOption("saved_in",              "Saved In",                                     Icons.Default.Storage)
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ContactInfoSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var hideDuplicateNumbersInContact by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HIDE_DUPLICATE_NUMBERS_IN_CONTACT, true))
    }

    var contactInfoPfpSize by remember {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CONTACT_INFO_PFP_SIZE, 0.60f).coerceIn(0.10f, 1.0f))
    }
    var contactInfoPfpShape by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_CONTACT_INFO_PFP_SHAPE, "circle") ?: "circle")
    }
    var showPfpShapeDialog by remember { mutableStateOf(false) }

    var showContactInfoElementsDialog by remember { mutableStateOf(false) }
    var contactInfoShowQuickActions        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_QUICK_ACTIONS, true)) }
    var contactInfoShowContactInfo         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CONTACT_INFO, true)) }
    var contactInfoShowSocial              by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SOCIAL, true)) }
    var contactInfoShowDescription         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_DESCRIPTION, true)) }
    var contactInfoShowNotes               by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_NOTES, true)) }
    var contactInfoShowEvents              by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_EVENTS, true)) }
    var contactInfoShowRecentActivity      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RECENT_ACTIVITY, true)) }
    var contactInfoShowChooseSim           by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_SIM, true)) }
    var contactInfoShowCallingBackgrounds  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CALLING_BACKGROUNDS, true)) }
    var contactInfoShowRingtone            by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RINGTONE, true)) }
    var contactInfoShowChooseDefaultNumber by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_DEFAULT_NUMBER, true)) }
    var contactInfoShowSavedIn             by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SAVED_IN, true)) }

    val contactInfoElementOrder = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString(PreferenceManager.KEY_CONTACT_INFO_ORDER, null)
            val savedKeys = PreferenceManager.parseContactInfoOrder(saved)
            addAll(savedKeys)
        }
    }
    fun persistContactInfoOrder() {
        prefs.setString(PreferenceManager.KEY_CONTACT_INFO_ORDER, contactInfoElementOrder.joinToString(","))
    }
    fun resetContactInfoElementsToDefault() {
        val defaults = PreferenceManager.DEFAULT_CONTACT_INFO_ORDER.split(",").map { it.trim() }.filter { it.isNotBlank() }
        contactInfoElementOrder.clear()
        contactInfoElementOrder.addAll(defaults)
        persistContactInfoOrder()
        contactInfoShowQuickActions        = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_QUICK_ACTIONS, true)
        contactInfoShowContactInfo         = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CONTACT_INFO, true)
        contactInfoShowSocial              = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SOCIAL, true)
        contactInfoShowDescription         = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_DESCRIPTION, true)
        contactInfoShowNotes               = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_NOTES, true)
        contactInfoShowEvents              = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_EVENTS, true)
        contactInfoShowRecentActivity      = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RECENT_ACTIVITY, true)
        contactInfoShowChooseSim           = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_SIM, true)
        contactInfoShowCallingBackgrounds  = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CALLING_BACKGROUNDS, true)
        contactInfoShowRingtone            = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RINGTONE, true)
        contactInfoShowChooseDefaultNumber = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_DEFAULT_NUMBER, true)
        contactInfoShowSavedIn             = true; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SAVED_IN, true)
    }

    if (showContactInfoElementsDialog) {
        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val rowHeightDp = 52.dp
        val rowHeightPx = with(density) { rowHeightDp.toPx() }
        var draggedKey by remember { mutableStateOf<String?>(null) }
        var dragOffsetY by remember { mutableStateOf(0f) }

        fun contactInfoChecked(key: String): Boolean = when (key) {
            "quick_actions"         -> contactInfoShowQuickActions
            "contact_info"          -> contactInfoShowContactInfo
            "social"                -> contactInfoShowSocial
            "description"           -> contactInfoShowDescription
            "notes"                 -> contactInfoShowNotes
            "events"                -> contactInfoShowEvents
            "recent_activity"       -> contactInfoShowRecentActivity
            "choose_sim"            -> contactInfoShowChooseSim
            "calling_backgrounds"   -> contactInfoShowCallingBackgrounds
            "ringtone"              -> contactInfoShowRingtone
            "choose_default_number" -> contactInfoShowChooseDefaultNumber
            "saved_in"              -> contactInfoShowSavedIn
            else                    -> true
        }
        fun setContactInfoChecked(key: String, value: Boolean) {
            when (key) {
                "quick_actions"         -> { contactInfoShowQuickActions = value;        prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_QUICK_ACTIONS, value) }
                "contact_info"          -> { contactInfoShowContactInfo = value;         prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CONTACT_INFO, value) }
                "social"                -> { contactInfoShowSocial = value;              prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SOCIAL, value) }
                "description"           -> { contactInfoShowDescription = value;         prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_DESCRIPTION, value) }
                "notes"                 -> { contactInfoShowNotes = value;               prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_NOTES, value) }
                "events"                -> { contactInfoShowEvents = value;              prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_EVENTS, value) }
                "recent_activity"       -> { contactInfoShowRecentActivity = value;      prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RECENT_ACTIVITY, value) }
                "choose_sim"            -> { contactInfoShowChooseSim = value;           prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_SIM, value) }
                "calling_backgrounds"   -> { contactInfoShowCallingBackgrounds = value;  prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CALLING_BACKGROUNDS, value) }
                "ringtone"              -> { contactInfoShowRingtone = value;            prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RINGTONE, value) }
                "choose_default_number" -> { contactInfoShowChooseDefaultNumber = value; prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_DEFAULT_NUMBER, value) }
                "saved_in"              -> { contactInfoShowSavedIn = value;             prefs.setBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SAVED_IN, value) }
            }
        }

        AlertDialog(
            onDismissRequest = { showContactInfoElementsDialog = false },
            icon = { Icon(Icons.Default.Contacts, null, tint = ColorIndigo) },
            title = { Text("Contact Info Elements") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Choose which elements are visible, and drag the handle to reorder them in contact details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column {
                        contactInfoElementOrder.forEach { itemKey ->
                            val option = contactInfoElementOptions.firstOrNull { it.key == itemKey } ?: return@forEach
                            val isDragging = draggedKey == itemKey
                            key(itemKey) {
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
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = contactInfoChecked(itemKey),
                                            onCheckedChange = { setContactInfoChecked(itemKey, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder ${option.label}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .pointerInput(itemKey) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedKey = itemKey
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                            persistContactInfoOrder()
                                                        },
                                                        onDragCancel = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val currentIdx = contactInfoElementOrder.indexOf(itemKey)
                                                            if (currentIdx != -1) {
                                                                val threshold = rowHeightPx * 0.6f
                                                                if (dragOffsetY > threshold && currentIdx < contactInfoElementOrder.lastIndex) {
                                                                    val item = contactInfoElementOrder.removeAt(currentIdx)
                                                                    contactInfoElementOrder.add(currentIdx + 1, item)
                                                                    dragOffsetY -= rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                    val item = contactInfoElementOrder.removeAt(currentIdx)
                                                                    contactInfoElementOrder.add(currentIdx - 1, item)
                                                                    dragOffsetY -= rowHeightPx
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
                TextButton(onClick = { showContactInfoElementsDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { resetContactInfoElementsToDefault() }) { Text("Default") }
            }
        )
    }

    if (showPfpShapeDialog) {
        AlertDialog(
            onDismissRequest = { showPfpShapeDialog = false },
            title = { Text("Choose PFP Shape", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("circle", "Circle (Default)"),
                        Pair("square", "Square")
                    ).forEach { (shapeKey, shapeLabel) ->
                        val isSelected = contactInfoPfpShape == shapeKey
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    contactInfoPfpShape = shapeKey
                                    prefs.setString(PreferenceManager.KEY_CONTACT_INFO_PFP_SHAPE, shapeKey)
                                    showPfpShapeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        if (shapeKey == "square") Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        shapeLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPfpShapeDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Contact Info",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Contact Display",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Hide Duplicate Numbers In A Contact",
                            supporting = "Show only one of multiple identical or matching numbers saved under the same contact",
                            leadingIcon = Icons.Outlined.FilterNone,
                            iconContainerColor = ColorPurple,
                            checked = hideDuplicateNumbersInContact,
                            modifier = Modifier.settingsSearchHighlight("hide_duplicate_numbers_in_contact", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                hideDuplicateNumbersInContact = it
                                prefs.setBoolean(PreferenceManager.KEY_HIDE_DUPLICATE_NUMBERS_IN_CONTACT, it)
                            }
                        )
                        CardDivider()
                        RivoListItem(
                            headline = "Contact Info Elements",
                            supporting = "Toggle and drag to reorder elements in contact details",
                            leadingIcon = Icons.Default.Contacts,
                            iconContainerColor = ColorIndigo,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("contact_info_elements", highlightedKey) { highlightedKey = null },
                            onClick = { showContactInfoElementsDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 20L) {
                Column {
                    Text(
                        "Contact Profile Picture",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        // PFP Size Slider
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF009688).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.PhotoSizeSelectLarge,
                                                contentDescription = null,
                                                tint = Color(0xFF009688),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "PFP Size",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        "${(contactInfoPfpSize * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = contactInfoPfpSize,
                                onValueChange = {
                                    contactInfoPfpSize = it
                                    prefs.setFloat(PreferenceManager.KEY_CONTACT_INFO_PFP_SIZE, it)
                                },
                                valueRange = 0.10f..1.0f,
                                steps = 17,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }

                        CardDivider()

                        // PFP Shape Chooser
                        RivoListItem(
                            headline = "PFP Shape",
                            supporting = if (contactInfoPfpShape == "square") "Square" else "Circle (Default)",
                            leadingIcon = if (contactInfoPfpShape == "square") Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF673AB7),
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { showPfpShapeDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 40L) {
                SwipeActionConfigSection(sectionKey = "contacts")
            }
        }
    }
}

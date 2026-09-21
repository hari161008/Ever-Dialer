package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

data class SwipeActionOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

data class SwipeSectionInfo(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val options: List<SwipeActionOption>
)

private val SWIPE_OPTION_NONE = SwipeActionOption("none", "None", Icons.Outlined.Block)

private val CALL_LOGS_SWIPE_OPTIONS = listOf(
    SwipeActionOption("select", "Select", Icons.Default.CheckBox),
    SwipeActionOption("call_back", "Call back", Icons.Default.Call),
    SwipeActionOption("view_contact", "View contact", Icons.Default.Person),
    SwipeActionOption("edit_contact", "Edit contact", Icons.Default.Edit),
    SwipeActionOption("copy_number", "Copy number", Icons.Default.ContentCopy),
    SwipeActionOption("add_to_contacts", "Add contact", Icons.Default.PersonAdd),
    SwipeActionOption("share", "Share contact", Icons.Default.Share),
    SwipeActionOption("call_chat_via", "Call/Chat Via", Icons.AutoMirrored.Filled.Chat),
    SwipeActionOption("send_text", "Send text", Icons.AutoMirrored.Filled.Message),
    SwipeActionOption("search_truecaller", "Search Truecaller", Icons.Default.Search),
    SwipeActionOption("move_contact", "Move contact", Icons.Default.DriveFileMove),
    SwipeActionOption("toggle_favorite", "Add/Remove Favourites", Icons.Default.Favorite),
    SwipeActionOption("block_number", "Block/Unblock number", Icons.Default.Block),
    SwipeActionOption("fake_call", "Fake Call", Icons.Outlined.PhoneCallback),
    SwipeActionOption("delete_call_log", "Delete from call log", Icons.Default.Delete)
)

private val CONTACTS_SWIPE_OPTIONS = listOf(
    SwipeActionOption("select", "Select", Icons.Default.CheckBox),
    SwipeActionOption("call", "Call", Icons.Default.Call),
    SwipeActionOption("view_contact", "View contact", Icons.Default.Person),
    SwipeActionOption("edit_contact", "Edit contact", Icons.Default.Edit),
    SwipeActionOption("copy_number", "Copy number", Icons.Default.ContentCopy),
    SwipeActionOption("share_contact", "Share contact", Icons.Default.Share),
    SwipeActionOption("call_chat_via", "Call/Chat Via", Icons.AutoMirrored.Filled.Chat),
    SwipeActionOption("send_text", "Send text", Icons.AutoMirrored.Filled.Message),
    SwipeActionOption("move_contact", "Move contact", Icons.Default.DriveFileMove),
    SwipeActionOption("toggle_favorite", "Add/Remove Favourites", Icons.Default.Favorite),
    SwipeActionOption("block_contact", "Block/Unblock contact", Icons.Default.Block),
    SwipeActionOption("fake_call", "Fake Call", Icons.Outlined.PhoneCallback),
    SwipeActionOption("delete_contact", "Delete contact", Icons.Default.Delete)
)

private val RECORDINGS_SWIPE_OPTIONS = listOf(
    SwipeActionOption("select", "Select", Icons.Default.CheckBox),
    SwipeActionOption("share", "Share", Icons.Default.Share),
    SwipeActionOption("view_info", "View Info", Icons.Default.Info),
    SwipeActionOption("toggle_favourite", "Add/Remove Favourite", Icons.Default.Favorite),
    SwipeActionOption("delete", "Delete", Icons.Default.Delete)
)

private val NOTES_SWIPE_OPTIONS = listOf(
    SwipeActionOption("select", "Select", Icons.Default.CheckBox),
    SwipeActionOption("share", "Share", Icons.Default.Share),
    SwipeActionOption("delete", "Delete", Icons.Default.Delete)
)

private val SWIPE_SECTIONS = listOf(
    SwipeSectionInfo("call_logs", "Call Logs", Icons.Outlined.History, Color(0xFF2196F3), CALL_LOGS_SWIPE_OPTIONS),
    SwipeSectionInfo("contacts", "Contacts", Icons.Outlined.Person, Color(0xFF4CAF50), CONTACTS_SWIPE_OPTIONS),
    SwipeSectionInfo("recordings", "Call Recordings", Icons.Outlined.Mic, Color(0xFFE91E63), RECORDINGS_SWIPE_OPTIONS),
    SwipeSectionInfo("notes", "Notes", Icons.Outlined.Description, Color(0xFFFF9800), NOTES_SWIPE_OPTIONS)
)

private fun getActionLabel(section: SwipeSectionInfo, key: String): String {
    if (key == "none") return "None"
    return section.options.firstOrNull { it.key == key }?.label ?: "None"
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SwipeActionsScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val scrollState = rememberScrollState()

    val colorScheme = MaterialTheme.colorScheme
    val swipeSections = remember(colorScheme) {
        listOf(
            SwipeSectionInfo("call_logs", "Call Logs", Icons.Outlined.History, colorScheme.primary, CALL_LOGS_SWIPE_OPTIONS),
            SwipeSectionInfo("contacts", "Contacts", Icons.Outlined.Person, colorScheme.secondary, CONTACTS_SWIPE_OPTIONS),
            SwipeSectionInfo("recordings", "Call Recordings", Icons.Outlined.Mic, colorScheme.tertiary, RECORDINGS_SWIPE_OPTIONS),
            SwipeSectionInfo("notes", "Notes", Icons.Outlined.Description, colorScheme.primary, NOTES_SWIPE_OPTIONS)
        )
    }

    var activeDialogSection by remember { mutableStateOf<SwipeSectionInfo?>(null) }
    var activeDialogDirection by remember { mutableStateOf<String?>(null) } // "left" or "right" or null

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Swipe Actions",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            swipeSections.forEachIndexed { index, section ->
                val leftKey = remember(settingsVer, section.key) { prefs.getSwipeAction(section.key, "left") }
                val rightKey = remember(settingsVer, section.key) { prefs.getSwipeAction(section.key, "right") }
                val leftLabel = getActionLabel(section, leftKey)
                val rightLabel = getActionLabel(section, rightKey)

                RivoAnimatedSection(delayMs = (index * 25).toLong()) {
                    Column {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            // Section header row - tapping it opens floating popup
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        activeDialogSection = section
                                        activeDialogDirection = null
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = section.iconColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            section.icon,
                                            contentDescription = null,
                                            tint = section.iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Left: $leftLabel • Right: $rightLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            CardDivider()

                            // Left Action Row
                            RivoListItem(
                                headline = "Left action",
                                supporting = leftLabel,
                                leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    activeDialogSection = section
                                    activeDialogDirection = "left"
                                }
                            )

                            CardDivider()

                            // Right Action Row
                            RivoListItem(
                                headline = "Right action",
                                supporting = rightLabel,
                                leadingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    activeDialogSection = section
                                    activeDialogDirection = "right"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Floating Popup
    if (activeDialogSection != null) {
        val currentSection = activeDialogSection!!
        SwipeActionFloatingDialog(
            section = currentSection,
            initialDirection = activeDialogDirection,
            onDismiss = {
                activeDialogSection = null
                activeDialogDirection = null
            },
            onSaveAction = { direction, actionKey ->
                prefs.setSwipeAction(currentSection.key, direction, actionKey)
            },
            currentLeftKey = prefs.getSwipeAction(currentSection.key, "left"),
            currentRightKey = prefs.getSwipeAction(currentSection.key, "right")
        )
    }
}

@Composable
private fun SwipeActionFloatingDialog(
    section: SwipeSectionInfo,
    initialDirection: String?,
    onDismiss: () -> Unit,
    onSaveAction: (direction: String, actionKey: String) -> Unit,
    currentLeftKey: String,
    currentRightKey: String
) {
    var selectedDirection by remember { mutableStateOf(initialDirection) }

    Dialog(onDismissRequest = onDismiss) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedDirection,
                        label = "SwipeDialogContent"
                    ) { direction ->
                        if (direction == null) {
                            // Step 1: Direction Picker (Left action vs Right action)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = section.iconColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                section.icon,
                                                contentDescription = null,
                                                tint = section.iconColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Choose swipe gesture to configure",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // Left Action Card
                                SwipeDirectionChoiceRow(
                                    title = "Left action",
                                    currentChoice = getActionLabel(section, currentLeftKey),
                                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = { selectedDirection = "left" }
                                )

                                Spacer(Modifier.height(10.dp))

                                // Right Action Card
                                SwipeDirectionChoiceRow(
                                    title = "Right action",
                                    currentChoice = getActionLabel(section, currentRightKey),
                                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                    iconColor = MaterialTheme.colorScheme.secondary,
                                    onClick = { selectedDirection = "right" }
                                )

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = onDismiss) {
                                        Text("Close")
                                    }
                                }
                            }
                        } else {
                            // Step 2: Options Picker for the selected direction
                            val activeCurrentKey = if (direction == "left") currentLeftKey else currentRightKey
                            val directionTitle = if (direction == "left") "Left action" else "Right action"
                            val allOptions = remember(section) { listOf(SWIPE_OPTION_NONE) + section.options }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { selectedDirection = null },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = directionTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                Spacer(Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    allOptions.forEach { option ->
                                        val isSelected = option.key == activeCurrentKey
                                        SwipeOptionItem(
                                            option = option,
                                            isSelected = isSelected,
                                            onClick = {
                                                onSaveAction(direction, option.key)
                                                selectedDirection = null
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { selectedDirection = null }) {
                                        Text("Back")
                                    }
                                    TextButton(onClick = onDismiss) {
                                        Text("Done")
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

@Composable
private fun SwipeDirectionChoiceRow(
    title: String,
    currentChoice: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = currentChoice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SwipeOptionItem(
    option: SwipeActionOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = spring(),
        label = "optionContainerColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

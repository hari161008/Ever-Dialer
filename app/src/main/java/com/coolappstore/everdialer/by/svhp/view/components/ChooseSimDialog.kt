package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager

/** One selectable "Choose Sim" option shown in [ChooseSimDialog]. */
data class SimChoiceOption(
    val value: String,
    val label: String,
    val subLabel: String? = null,
    val icon: ImageVector
)

/** All available "Choose Sim" options, in display order. */
val SIM_CHOICE_OPTIONS = listOf(
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SETTINGS, "According to Settings", "Use the app-wide default SIM setting", Icons.Default.Tune),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_ASK, "Ask Every Time", "Show the SIM picker on every call", Icons.Default.HelpOutline),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SIM1, "SIM 1", null, Icons.Default.SimCard),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_SIM2, "SIM 2", null, Icons.Default.SimCard),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_LAST_FOR_CONTACT, "Last Used SIM for This Contact", "Reuse the SIM from the most recent call with them", Icons.Default.History),
    SimChoiceOption(PreferenceManager.SIM_CHOICE_LAST_IN_CALL, "Last Used SIM in Previous Call", "Reuse the SIM from the last call made from the app", Icons.Default.PhoneCallback)
)

/** Returns the display label for a stored "Choose Sim" preference value. */
fun simChoiceLabel(value: String): String =
    SIM_CHOICE_OPTIONS.firstOrNull { it.value == value }?.label ?: SIM_CHOICE_OPTIONS.first().label

/**
 * Shared header for the "Choose Sim" / "Choose Default Number" floating popups: a big rounded
 * icon badge above a bold title, centered, matching the rest of the app's "expressive" M3 look
 * (RivoExpressiveCard headers, RivoIconBox tinted badges) instead of a plain left-aligned label.
 */
@Composable
private fun RivoChoiceDialogHeader(title: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/**
 * One selectable row inside a "Choose Sim" / "Choose Default Number" popup — a rounded card that
 * tints with the primary color and grows a filled check badge when selected, rather than a plain
 * list row, so the current choice reads clearly at a glance.
 */
@Composable
private fun RivoChoiceRow(
    icon: ImageVector,
    label: String,
    subLabel: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val prefs = org.koin.compose.koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val circleIcons = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CIRCLE_ICONS, false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = if (circleIcons) CircleShape else RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                if (subLabel != null) {
                    Text(
                        subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/**
 * Floating popup listing every "Choose Sim" option for a contact, with the currently selected
 * option checked off. Redesigned to match the app's rounded, tinted "expressive" M3 look — a
 * centered icon badge + title header, and individually rounded, tinted choice cards instead of a
 * plain list.
 */
@Composable
fun ChooseSimDialog(
    currentChoice: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RivoChoiceDialogHeader(title = "Choose Sim", icon = Icons.Default.SimCard)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SIM_CHOICE_OPTIONS.forEach { option ->
                        RivoChoiceRow(
                            icon = option.icon,
                            label = option.label,
                            subLabel = option.subLabel,
                            selected = option.value == currentChoice,
                            onClick = { onSelect(option.value) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cancel") }
            }
        }
    }
}

/**
 * Floating popup for Contact Info → "Choose Default Number" (shown only for contacts saved with
 * 2+ phone numbers, right below "Choose Sim"). Picking a number makes the header call button call
 * it directly instead of prompting with the number picker every time, and is also used to resolve
 * which number WhatsApp/Telegram/Google Meet's quick actions should use. "Ask Every Time" clears
 * the preference back to the original prompt-every-time behavior. Same visual language as
 * [ChooseSimDialog].
 */
@Composable
fun ChooseDefaultNumberDialog(
    numbers: List<String>,
    currentChoice: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RivoChoiceDialogHeader(title = "Choose Default Number", icon = Icons.Default.Numbers)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RivoChoiceRow(
                        icon = Icons.Default.HelpOutline,
                        label = "Ask Every Time",
                        subLabel = "Show the number picker on every call",
                        selected = currentChoice == null,
                        onClick = { onSelect(null) }
                    )
                    numbers.forEach { number ->
                        RivoChoiceRow(
                            icon = Icons.Default.Phone,
                            label = number,
                            selected = number == currentChoice,
                            onClick = { onSelect(number) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cancel") }
            }
        }
    }
}

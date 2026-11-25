package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.NetworkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkModeSelector(
    label: String,
    selectedMode: NetworkMode,
    onModeSelected: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier,
    availableModes: List<NetworkMode> = NetworkMode.values().toList()
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedMode.displayName,
                onValueChange = { },
                readOnly = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModes.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (mode == selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

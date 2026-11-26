package com.modiprincekr.thedialer.view.screen.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modiprincekr.thedialer.controller.UssdCategory
import com.modiprincekr.thedialer.controller.UssdCodeItem
import com.modiprincekr.thedialer.controller.UssdCountry
import com.modiprincekr.thedialer.controller.UssdRepository
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.RivoAnimatedSection
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.components.rivoHeadingColor
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun UssdCodesScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var selectedCountryCode by remember(settingsVer) {
        mutableStateOf(prefs.getSelectedUssdCountry(context))
    }
    val currentCountry = remember(selectedCountryCode) {
        UssdRepository.getCountryByCode(selectedCountryCode)
    }

    var searchQuery by remember { mutableStateOf("") }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<UssdCodeItem?>(null) }
    var itemToDelete by remember { mutableStateOf<UssdCodeItem?>(null) }

    val customCodes = remember(settingsVer) { prefs.getCustomUssdCodes() }
    val carrierCodes = remember(selectedCountryCode) {
        UssdRepository.getCarrierCodesForCountry(selectedCountryCode)
    }
    val standardCodes = remember { UssdRepository.STANDARD_CODES }
    val diagnosticCodes = remember { UssdRepository.DIAGNOSTIC_CODES }
    val hardwareCodes = remember { UssdRepository.HARDWARE_CODES }

    fun filterCodes(list: List<UssdCodeItem>): List<UssdCodeItem> {
        val q = searchQuery.trim()
        if (q.isEmpty()) return list
        return list.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.code.contains(q, ignoreCase = true) ||
            (it.brand?.contains(q, ignoreCase = true) == true) ||
            (it.description?.contains(q, ignoreCase = true) == true)
        }
    }

    fun onCopyCode(code: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        clipboardManager.setText(AnnotatedString(code))
        Toast.makeText(context, "Copied $code", Toast.LENGTH_SHORT).show()
    }

    fun onExecuteCode(code: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        UssdRepository.executeCode(context, code)
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "USSD & Secret Codes",
                onBackClick = { navigator.navigateUp() },
                endActions = {
                    IconButton(
                        onClick = {
                            itemToEdit = null
                            showAddEditDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Custom Code",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Controls: Country Selector & Search Bar
            RivoAnimatedSection(delayMs = 0L) {
                RivoExpressiveCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Country selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Carrier Country",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentCountry.flag} ${currentCountry.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            FilledTonalButton(
                                onClick = { showCountryDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change")
                            }
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, code, or brand...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Section 1: My Custom Codes
            val filteredCustom = filterCodes(customCodes)
            if (filteredCustom.isNotEmpty() || searchQuery.isEmpty()) {
                RivoAnimatedSection(delayMs = 15L) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "My Custom Codes (${customCodes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor()
                            )
                            TextButton(
                                onClick = {
                                    itemToEdit = null
                                    showAddEditDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Code")
                            }
                        }

                        RivoExpressiveCard {
                            if (filteredCustom.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No custom codes saved yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap '+ Add Code' to save your own helplines, bank services, or USSD shortcuts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    filteredCustom.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        UssdCodeRowItem(
                                            item = item,
                                            onExecute = { onExecuteCode(item.code) },
                                            onCopy = { onCopyCode(item.code) },
                                            onEdit = {
                                                itemToEdit = item
                                                showAddEditDialog = true
                                            },
                                            onDelete = { itemToDelete = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Country Carrier Codes
            val filteredCarriers = filterCodes(carrierCodes)
            if (filteredCarriers.isNotEmpty()) {
                RivoAnimatedSection(delayMs = 30L) {
                    Column {
                        Text(
                            text = "${currentCountry.flag} ${currentCountry.name} Carrier Codes",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                        )
                        Text(
                            text = "Carrier codes change from time to time. Tap to dial, long-press to copy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )

                        // Group carrier codes by brand
                        val byCarrier = filteredCarriers.groupBy { it.brand ?: "Other" }
                        byCarrier.forEach { (carrierName, codes) ->
                            Text(
                                text = carrierName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 6.dp)
                            )
                            RivoExpressiveCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    codes.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        UssdCodeRowItem(
                                            item = item,
                                            onExecute = { onExecuteCode(item.code) },
                                            onCopy = { onCopyCode(item.code) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Standard Network Codes
            val filteredStandard = filterCodes(standardCodes)
            if (filteredStandard.isNotEmpty()) {
                RivoAnimatedSection(delayMs = 45L) {
                    Column {
                        Text(
                            text = "Standard Network Codes (All Carriers)",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                filteredStandard.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                    UssdCodeRowItem(
                                        item = item,
                                        onExecute = { onExecuteCode(item.code) },
                                        onCopy = { onCopyCode(item.code) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Android System & Diagnostics
            val filteredDiag = filterCodes(diagnosticCodes)
            if (filteredDiag.isNotEmpty()) {
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        Text(
                            text = "Android System & Diagnostics",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                filteredDiag.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                    UssdCodeRowItem(
                                        item = item,
                                        onExecute = { onExecuteCode(item.code) },
                                        onCopy = { onCopyCode(item.code) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Brand Hardware Test Menus
            val filteredHardware = filterCodes(hardwareCodes)
            if (filteredHardware.isNotEmpty()) {
                RivoAnimatedSection(delayMs = 75L) {
                    Column {
                        Text(
                            text = "Brand Hardware Test Menus",
                            style = MaterialTheme.typography.labelLarge,
                            color = rivoHeadingColor(),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )

                        val byBrand = filteredHardware.groupBy { it.brand ?: "Other" }
                        byBrand.forEach { (brandName, codes) ->
                            Text(
                                text = brandName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 6.dp)
                            )
                            RivoExpressiveCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    codes.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        UssdCodeRowItem(
                                            item = item,
                                            onExecute = { onExecuteCode(item.code) },
                                            onCopy = { onCopyCode(item.code) }
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

    // Country Picker Dialog
    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = {
                Text(
                    text = "Select Country for Carrier Codes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(UssdRepository.SUPPORTED_COUNTRIES) { country ->
                        val isSelected = country.code.equals(selectedCountryCode, ignoreCase = true)
                        Surface(
                            onClick = {
                                selectedCountryCode = country.code
                                prefs.setSelectedUssdCountry(country.code)
                                showCountryDialog = false
                                Toast.makeText(context, "Carrier country set to ${country.name}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = country.flag,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = country.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = country.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add / Edit Custom Code Dialog
    if (showAddEditDialog) {
        val isEditing = itemToEdit != null
        var inputTitle by remember { mutableStateOf(itemToEdit?.title ?: "") }
        var inputCode by remember { mutableStateOf(itemToEdit?.code ?: "") }
        var inputDesc by remember { mutableStateOf(itemToEdit?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Edit Custom Code" else "Add Custom Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Title / Name *") },
                        placeholder = { Text("e.g. Main Balance, Bank Care") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("Code or Number *") },
                        placeholder = { Text("e.g. *123#, 1800123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        label = { Text("Description / Note (Optional)") },
                        placeholder = { Text("e.g. Primary SIM pack check") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = inputTitle.trim()
                        val c = inputCode.trim()
                        val d = inputDesc.trim().ifBlank { null }
                        if (t.isNotBlank() && c.isNotBlank()) {
                            if (isEditing && itemToEdit != null) {
                                prefs.updateCustomUssdCode(itemToEdit!!.id, t, c, d)
                                Toast.makeText(context, "Code updated", Toast.LENGTH_SHORT).show()
                            } else {
                                prefs.addCustomUssdCode(t, c, d)
                                Toast.makeText(context, "Code saved", Toast.LENGTH_SHORT).show()
                            }
                            showAddEditDialog = false
                            itemToEdit = null
                        } else {
                            Toast.makeText(context, "Title and Code cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (isEditing) "Update" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddEditDialog = false
                        itemToEdit = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Custom Code Confirmation Dialog
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Custom Code?") },
            text = { Text("Are you sure you want to remove '${item.title}' (${item.code})?") },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.deleteCustomUssdCode(item.id)
                        itemToDelete = null
                        Toast.makeText(context, "Code deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UssdCodeRowItem(
    item: UssdCodeItem,
    onExecute: () -> Unit,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onExecute,
                onLongClick = onCopy
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.brand != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = item.brand,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.code,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.isCustom && onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (item.isCustom && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onExecute,
                modifier = Modifier.size(36.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Dial / Run",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

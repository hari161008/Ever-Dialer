package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.SmsCustomCategory
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CreateSmsCategoryScreen(navigator: DestinationsNavigator, categoryId: String? = null) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    val existingCategory = remember(categoryId) {
        categoryId?.let { id -> prefs.getCustomSmsCategories().firstOrNull { it.id == id } }
    }

    var categoryName by remember { mutableStateOf(existingCategory?.name ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedContactKeys = remember { mutableStateListOf<String>() }

    LaunchedEffect(allContacts, existingCategory) {
        if (existingCategory != null && selectedContactKeys.isEmpty()) {
            val phones = existingCategory.contactPhones.toSet()
            val matchedKeys = allContacts.filter { c ->
                c.phoneNumbers.any { phones.contains(it) }
            }.map { it.id.ifBlank { it.name } }
            selectedContactKeys.addAll(matchedKeys)
        }
    }

    val filteredContacts = remember(allContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            allContacts
        } else {
            val q = searchQuery.trim().lowercase()
            allContacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                contact.phoneNumbers.any { it.contains(q) }
            }
        }
    }

    fun handleSave() {
        val trimmedName = categoryName.trim()
        if (trimmedName.isBlank()) {
            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        // Collect all phone numbers of selected contacts
        val selectedPhones = allContacts
            .filter { selectedContactKeys.contains(it.id) || selectedContactKeys.contains(it.name) }
            .flatMap { it.phoneNumbers }
            .distinct()

        val id = categoryId ?: ("custom_" + System.currentTimeMillis())
        val category = SmsCustomCategory(
            id = id,
            name = trimmedName,
            contactPhones = selectedPhones
        )
        prefs.saveCustomSmsCategory(category)
        Toast.makeText(context, if (categoryId != null) "Category '$trimmedName' updated" else "Category '$trimmedName' created", Toast.LENGTH_SHORT).show()
        navigator.navigateUp()
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = if (existingCategory != null) "Edit Category" else "New Category",
                onBackClick = { navigator.navigateUp() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { handleSave() },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text(if (existingCategory != null) "Update Category" else "Save Category") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp + navBarBottom)
        ) {
            // Category Name Card
            item(key = "name_card") {
                RivoSectionHeader("Category Details")
                RivoExpressiveCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Category Name") },
                            placeholder = { Text("e.g. Work, Family, Banking") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Choose Contacts Section
            item(key = "contacts_header") {
                RivoSectionHeader("Choose Contacts (${selectedContactKeys.size} selected)")
            }

            // Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Contact List
            if (filteredContacts.isEmpty()) {
                item(key = "empty_contacts") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No contacts matching '$searchQuery'" else "No contacts found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item(key = "contacts_card") {
                    RivoExpressiveCard {
                        Column {
                            filteredContacts.forEachIndexed { index, contact ->
                                val key = contact.id.ifBlank { contact.name }
                                val isSelected = selectedContactKeys.contains(key)

                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) {
                                                selectedContactKeys.remove(key)
                                            } else {
                                                selectedContactKeys.add(key)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RivoAvatar(
                                        name = contact.name,
                                        photoUri = contact.photoUri,
                                        size = 40.dp
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name.ifBlank { "Unknown" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (contact.phoneNumbers.isNotEmpty()) {
                                            Text(
                                                text = contact.phoneNumbers.first(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedContactKeys.add(key)
                                            } else {
                                                selectedContactKeys.remove(key)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

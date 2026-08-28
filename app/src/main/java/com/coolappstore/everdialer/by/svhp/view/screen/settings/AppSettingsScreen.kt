package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollToTopButton
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CallSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ColorTeal = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val showButton by remember { derivedStateOf { scrollState.value > 0 } }

    // Row to scroll to and flash on arrival, coming from Settings' search. Cleared once consumed.
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var integrateNotes by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, true)) }
    var deleteNotesWithRecording by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, false)) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { scrollState.animateScrollTo(0) } }
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

                com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)

                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Call Settings",
                        supporting = "Accounts, sensor, pocket mode, and sound",
                        leadingIcon = Icons.Outlined.Call,
                        iconContainerColor = ColorTeal,
                        trailingIcon = Icons.Default.ChevronRight,
                        modifier = Modifier.settingsSearchHighlight("nav_call_settings", highlightedKey) { highlightedKey = null },
                        onClick = { navigator.navigate(CallSettingsScreenDestination()) }
                    )
                    CardDivider()
                    RivoListItem(
                        headline = "4G/5G Switcher",
                        supporting = "Quickly toggle your network mode",
                        leadingIcon = Icons.Outlined.SignalCellularAlt,
                        iconContainerColor = Color(0xFF00897B),
                        trailingIcon = Icons.Default.ChevronRight,
                        modifier = Modifier.settingsSearchHighlight("network_switcher", highlightedKey) { highlightedKey = null },
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(context, com.supernova.networkswitch.presentation.ui.activity.MainActivity::class.java)
                                )
                            } catch (_: Exception) {}
                        }
                    )
                    CardDivider()
                    RivoSwitchListItem(
                        headline   = "Integrate Notes Section",
                        supporting = if (integrateNotes)
                                         "Call recording notes stay separate from the app's Notes section"
                                     else
                                         "Call recording notes are merged into the app's Notes section",
                        leadingIcon = Icons.Default.Note,
                        iconContainerColor = Color(0xFFE53935),
                        checked = integrateNotes,
                        modifier = Modifier.settingsSearchHighlight("integrate_notes", highlightedKey) { highlightedKey = null },
                        onCheckedChange = {
                            integrateNotes = it
                            prefs.setBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, it)
                        }
                    )
                    AnimatedVisibility(visible = integrateNotes) {
                        Column {
                            CardDivider()
                            RivoSwitchListItem(
                                headline   = "Delete Notes With Recording",
                                supporting = "Also delete the linked note in Notes when its call recording is deleted",
                                leadingIcon = Icons.Default.DeleteSweep,
                                iconContainerColor = Color(0xFF6D4C41),
                                checked = deleteNotesWithRecording,
                                modifier = Modifier.settingsSearchHighlight("delete_notes_with_recording", highlightedKey) { highlightedKey = null },
                                onCheckedChange = {
                                    deleteNotesWithRecording = it
                                    prefs.setBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, it)
                                }
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

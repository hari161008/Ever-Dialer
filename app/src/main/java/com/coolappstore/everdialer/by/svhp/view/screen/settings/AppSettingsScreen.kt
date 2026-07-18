package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
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
fun AppSettingsScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    var integrateNotes by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Call Settings",
                        supporting = "Accounts, sensor, pocket mode, and sound",
                        leadingIcon = Icons.Outlined.Call,
                        iconContainerColor = ColorTeal,
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { navigator.navigate(CallSettingsScreenDestination) }
                    )
                    CardDivider()
                    RivoListItem(
                        headline = "4G/5G Switcher",
                        supporting = "Quickly toggle your network mode",
                        leadingIcon = Icons.Outlined.SignalCellularAlt,
                        iconContainerColor = Color(0xFF00897B),
                        trailingIcon = Icons.Default.ChevronRight,
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
                        onCheckedChange = {
                            integrateNotes = it
                            prefs.setBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, it)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

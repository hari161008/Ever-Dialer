package com.modiprincekr.thedialer.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.RivoListItem
import com.modiprincekr.thedialer.view.components.RivoSectionHeader
import com.modiprincekr.thedialer.view.components.ScrollToTopButton
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ColorBlue    = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallAccountsScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar(
                title = "Call Settings",
                onBackClick = { navigator.navigateUp() }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showButton,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Manage Calling Accounts",
                        supporting = "SIM cards and calling accounts",
                        leadingIcon = Icons.Outlined.SimCard,
                        iconContainerColor = ColorBlue,
                        onClick = {
                            val intent = android.content.Intent(android.telecom.TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                                try {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                } catch (_: Throwable) {}
                            }
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

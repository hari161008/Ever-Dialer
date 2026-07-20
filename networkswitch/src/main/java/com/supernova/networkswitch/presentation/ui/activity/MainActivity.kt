package com.supernova.networkswitch.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.supernova.networkswitch.R
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import com.supernova.networkswitch.presentation.ui.composable.AppLaunchAutomationCard
import com.supernova.networkswitch.presentation.ui.composable.BatterySaverAutomationCard
import com.supernova.networkswitch.presentation.ui.composable.CompatibilityCard
import com.supernova.networkswitch.presentation.ui.composable.MasterSwitchCard
import com.supernova.networkswitch.presentation.ui.composable.MasterSwitchOffBanner
import com.supernova.networkswitch.presentation.ui.composable.NetworkModeConfigCard
import com.supernova.networkswitch.presentation.ui.composable.NetworkSwitchFloatingHint
import com.supernova.networkswitch.presentation.ui.composable.NetworkToggleCard
import com.supernova.networkswitch.presentation.ui.composable.QuickSettingsHintCard
import com.supernova.networkswitch.presentation.ui.composable.ScreenStateAutomationCard

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                NetworkSwitchGraph.init(applicationContext)
                return MainViewModel(
                    NetworkSwitchGraph.checkCompatibilityUseCase,
                    NetworkSwitchGraph.requestShizukuPermissionUseCase,
                    NetworkSwitchGraph.getCurrentNetworkModeUseCase,
                    NetworkSwitchGraph.toggleNetworkModeUseCase,
                    NetworkSwitchGraph.updateControlMethodUseCase,
                    NetworkSwitchGraph.getToggleModeConfigUseCase,
                    NetworkSwitchGraph.updateToggleModeConfigUseCase,
                    NetworkSwitchGraph.preferencesRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        NetworkSwitchGraph.init(applicationContext)
        com.supernova.networkswitch.service.AutomationServiceController.sync(applicationContext)
        com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult = {
            runOnUiThread { viewModel.refreshAllData() }
        }
        
        setContent {
            NetworkSwitchTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSettingsClick = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult != null) {
            com.supernova.networkswitch.data.source.ShizukuNetworkControlDataSource.onPermissionResult = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val compatibilityState = viewModel.compatibilityState

    LaunchedEffect(viewModel.configSaved) {
        if (viewModel.configSaved) {
            Toast.makeText(context, context.getString(R.string.configuration_saved), Toast.LENGTH_SHORT).show()
            viewModel.resetConfigSavedFlag()
        }
    }

    // Re-check Usage Access whenever the screen resumes (e.g. coming back from Settings after
    // granting it), and load the app list once if "Switch Based On App Launched" is on.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageAccessStatus(context)
                if (viewModel.appLaunchConfig.enabled && viewModel.installedApps.isEmpty()) {
                    viewModel.loadInstalledApps(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (viewModel.showFloatingHintPopup) {
        NetworkSwitchFloatingHint(onDismiss = { viewModel.dismissFloatingHintPopup() })
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ns_app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Red banner shown whenever the feature is turned off
            if (!viewModel.masterEnabled) {
                MasterSwitchOffBanner()
            }

            // Universal toggle for the whole feature — off by default
            MasterSwitchCard(
                enabled = viewModel.masterEnabled,
                onEnabledChange = { viewModel.setMasterEnabled(context, it) }
            )

            if (viewModel.masterEnabled) {
                // Compatibility Status Card
                CompatibilityCard(
                    compatibilityState = compatibilityState,
                    currentControlMethod = viewModel.selectedMethod,
                    onRetryClick = { viewModel.retryCompatibilityCheck() }
                )

                // Network Toggle Card (show if compatible)
                if (compatibilityState is CompatibilityState.Compatible) {
                    NetworkToggleCard(
                        currentMode = viewModel.currentNetworkMode,
                        toggleButtonText = viewModel.getToggleButtonText(),
                        isLoading = viewModel.isLoading,
                        onToggleClick = { viewModel.toggleNetworkMode() }
                    )
                }

                // Network Mode Configuration — inlined below the Network Mode container
                NetworkModeConfigCard(
                    modeA = viewModel.configModeA,
                    modeB = viewModel.configModeB,
                    isSaving = viewModel.isSavingConfig,
                    onModeASelected = { viewModel.updateConfigModeA(it) },
                    onModeBSelected = { viewModel.updateConfigModeB(it) },
                    onSaveClick = { viewModel.saveNetworkModeConfig() }
                )

                // "Switch Based On Screen State" — off by default
                ScreenStateAutomationCard(
                    enabled = viewModel.screenStateConfig.enabled,
                    screenOffMode = viewModel.screenStateConfig.screenOffMode,
                    screenOnMode = viewModel.screenStateConfig.screenOnMode,
                    onEnabledChange = { viewModel.setScreenStateEnabled(context, it) },
                    onScreenOffModeChange = { viewModel.setScreenOffMode(it) },
                    onScreenOnModeChange = { viewModel.setScreenOnMode(it) }
                )

                // "Switch based on Battery Saver state" — off by default
                BatterySaverAutomationCard(
                    enabled = viewModel.batterySaverConfig.enabled,
                    mode = viewModel.batterySaverConfig.mode,
                    onEnabledChange = { viewModel.setBatterySaverEnabled(context, it) },
                    onModeChange = { viewModel.setBatterySaverMode(it) }
                )

                // "Switch Based On App Launched" — off by default
                AppLaunchAutomationCard(
                    enabled = viewModel.appLaunchConfig.enabled,
                    hasUsageAccess = viewModel.hasUsageAccess,
                    isLoadingApps = viewModel.isLoadingInstalledApps,
                    apps = viewModel.installedApps,
                    appModes = viewModel.appLaunchConfig.appModes,
                    onEnabledChange = { viewModel.setAppLaunchEnabled(context, it) },
                    onGrantUsageAccessClick = { viewModel.requestUsageAccess(context) },
                    onAppModeChange = { packageName, mode -> viewModel.setAppMode(packageName, mode) }
                )

                // Quick Settings Tip Card
                QuickSettingsHintCard()
            }
        }
    }
}

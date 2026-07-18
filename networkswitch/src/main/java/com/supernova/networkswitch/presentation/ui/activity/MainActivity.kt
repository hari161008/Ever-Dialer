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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.supernova.networkswitch.R
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import com.supernova.networkswitch.presentation.ui.composable.CompatibilityCard
import com.supernova.networkswitch.presentation.ui.composable.NetworkModeConfigCard
import com.supernova.networkswitch.presentation.ui.composable.NetworkToggleCard
import com.supernova.networkswitch.presentation.ui.composable.QuickSettingsHintCard

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
            
            // Quick Settings Tip Card
            QuickSettingsHintCard()
        }
    }
}

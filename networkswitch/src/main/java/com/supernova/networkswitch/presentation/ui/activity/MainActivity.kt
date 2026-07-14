package com.supernova.networkswitch.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.supernova.networkswitch.R
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import com.supernova.networkswitch.presentation.ui.composable.CompatibilityCard
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
                    NetworkSwitchGraph.getCurrentNetworkModeUseCase,
                    NetworkSwitchGraph.toggleNetworkModeUseCase,
                    NetworkSwitchGraph.updateControlMethodUseCase,
                    NetworkSwitchGraph.getToggleModeConfigUseCase,
                    NetworkSwitchGraph.preferencesRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkSwitchGraph.init(applicationContext)
        
        setContent {
            NetworkSwitchTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSettingsClick = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    onNetworkModeConfigClick = {
                        startActivity(Intent(this@MainActivity, NetworkModeConfigActivity::class.java))
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onNetworkModeConfigClick: () -> Unit
) {
    val compatibilityState = viewModel.compatibilityState
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ns_app_name)) },
                actions = {
                    IconButton(onClick = onNetworkModeConfigClick) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Network Mode Configuration"
                        )
                    }
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
            
            // Quick Settings Tip Card
            QuickSettingsHintCard()
        }
    }
}

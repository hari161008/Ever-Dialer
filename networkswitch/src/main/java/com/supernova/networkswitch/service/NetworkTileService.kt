package com.supernova.networkswitch.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.MasterSwitchStore
import kotlinx.coroutines.*

class NetworkTileService : TileService() {

    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase
        get() { NetworkSwitchGraph.init(applicationContext); return NetworkSwitchGraph.getCurrentNetworkModeUseCase }

    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase
        get() { NetworkSwitchGraph.init(applicationContext); return NetworkSwitchGraph.toggleNetworkModeUseCase }

    private val getToggleModeConfigUseCase: GetToggleModeConfigUseCase
        get() { NetworkSwitchGraph.init(applicationContext); return NetworkSwitchGraph.getToggleModeConfigUseCase }

    private val preferencesRepository: PreferencesRepository
        get() { NetworkSwitchGraph.init(applicationContext); return NetworkSwitchGraph.preferencesRepository }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentNetworkMode: NetworkMode? = null
    private var toggleConfig: ToggleModeConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        NetworkSwitchGraph.init(applicationContext)

        if (!MasterSwitchStore.isEnabled()) {
            qsTile?.apply {
                state = Tile.STATE_UNAVAILABLE
                label = "4G/5G Switcher"
                subtitle = "Turned off in app settings"
                updateTile()
            }
            return
        }

        serviceScope.launch {
            try {
                // Observe toggle configuration changes
                preferencesRepository.observeToggleModeConfig().collect { newConfig ->
                    if (!MasterSwitchStore.isEnabled()) return@collect
                    toggleConfig = newConfig
                    refreshNetworkState()
                }
            } catch (_: Exception) {
                // Handle errors silently
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        // Clean up any ongoing operations when tile becomes inactive
    }

    override fun onClick() {
        super.onClick()

        if (!MasterSwitchStore.isEnabled()) return

        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        serviceScope.launch {
            try {
                toggleNetworkModeUseCase(subId)
                    .onSuccess { newMode ->
                        currentNetworkMode = newMode
                        withContext(Dispatchers.Main) {
                            updateTile()
                        }
                    }
                    .onFailure {
                        refreshNetworkState()
                    }
            } catch (_: Exception) {
                refreshNetworkState()
            }
        }
    }

    private suspend fun refreshNetworkState() {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        
        try {
            getCurrentNetworkModeUseCase(subId)
                .onSuccess { networkMode ->
                    currentNetworkMode = networkMode
                    withContext(Dispatchers.Main) {
                        updateTile()
                    }
                }
        } catch (_: Exception) {
            // Handle errors silently
        }
    }
    
    private fun updateTile() {
        try {
            qsTile?.apply {
                val config = toggleConfig
                
                if (config != null) {
                    state = Tile.STATE_ACTIVE
                    
                    // Show current and next modes
                    val currentMode = config.getCurrentMode()
                    val nextMode = config.getNextMode()
                    label = currentMode.displayName
                    subtitle = "${nextMode.displayName}"
                } else {
                    state = Tile.STATE_INACTIVE
                    label = "Network Mode"
                    subtitle = "Config not loaded"
                }
                updateTile()
            }
        } catch (_: Exception) {
            // Handle tile update errors silently
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

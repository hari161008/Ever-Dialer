package com.supernova.networkswitch.presentation.viewmodel

import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.supernova.networkswitch.domain.model.AppAutomationMode
import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.AutomationMode
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.usecase.CheckCompatibilityUseCase
import com.supernova.networkswitch.domain.usecase.RequestShizukuPermissionUseCase
import com.supernova.networkswitch.domain.usecase.GetCurrentNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.ToggleNetworkModeUseCase
import com.supernova.networkswitch.domain.usecase.UpdateControlMethodUseCase
import com.supernova.networkswitch.domain.usecase.GetToggleModeConfigUseCase
import com.supernova.networkswitch.domain.usecase.UpdateToggleModeConfigUseCase
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.service.AutomationServiceController
import com.supernova.networkswitch.util.AutomationSwitchStore
import com.supernova.networkswitch.util.InstalledAppsProvider
import com.supernova.networkswitch.util.LaunchableAppInfo
import com.supernova.networkswitch.util.MasterSwitchStore
import com.supernova.networkswitch.util.UsageAccessHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
class MainViewModel constructor(
    private val checkCompatibilityUseCase: CheckCompatibilityUseCase,
    private val requestShizukuPermissionUseCase: RequestShizukuPermissionUseCase,
    private val getCurrentNetworkModeUseCase: GetCurrentNetworkModeUseCase,
    private val toggleNetworkModeUseCase: ToggleNetworkModeUseCase,
    private val updateControlMethodUseCase: UpdateControlMethodUseCase,
    private val getToggleModeConfigUseCase: GetToggleModeConfigUseCase,
    private val updateToggleModeConfigUseCase: UpdateToggleModeConfigUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // Current control method selection
    var selectedMethod by mutableStateOf(ControlMethod.SHIZUKU)
        private set
    
    // Current network mode
    var currentNetworkMode by mutableStateOf<NetworkMode?>(null)
        private set
    
    // Toggle mode configuration
    var toggleModeConfig by mutableStateOf(ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY))
        private set

    // ── Network mode configuration editor (inlined from the former standalone
    // NetworkModeConfigActivity/ViewModel — now surfaced directly on the main screen) ──
    var configModeA by mutableStateOf(NetworkMode.LTE_ONLY)
        private set
    var configModeB by mutableStateOf(NetworkMode.NR_ONLY)
        private set
    var isSavingConfig by mutableStateOf(false)
        private set
    var configSaved by mutableStateOf(false)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    // Compatibility state using domain model
    var compatibilityState by mutableStateOf<CompatibilityState>(CompatibilityState.Pending)
        private set

    // Master kill switch for the whole 4G/5G Switcher feature. Defaults to OFF.
    var masterEnabled by mutableStateOf(MasterSwitchStore.isEnabled())
        private set

    // Floating popup shown right after the master toggle is switched on, with instructions for
    // what to do if the 4G/5G switching itself ends up breaking the connection.
    var showFloatingHintPopup by mutableStateOf(false)
        private set

    // ── "Switch Based On Screen State" ──
    var screenStateConfig by mutableStateOf(ScreenStateAutomationConfig())
        private set

    // ── "Switch based on Battery Saver state" ──
    var batterySaverConfig by mutableStateOf(BatterySaverAutomationConfig())
        private set

    // ── "Switch Based On App Launched" ──
    var appLaunchConfig by mutableStateOf(AppLaunchAutomationConfig())
        private set
    var installedApps by mutableStateOf<List<LaunchableAppInfo>>(emptyList())
        private set
    var isLoadingInstalledApps by mutableStateOf(false)
        private set
    var hasUsageAccess by mutableStateOf(false)
        private set

    init {
        observeMasterSwitch()
        observeControlMethodPreference()
        loadToggleModeConfig()
        loadAutomationConfigs()
        checkCompatibility()
        refreshNetworkState()
    }

    /** Observes the master switch so the UI (and any state derived from it) stays in sync. */
    private fun observeMasterSwitch() {
        viewModelScope.launch {
            MasterSwitchStore.enabled.collectLatest { isEnabled ->
                val justTurnedOn = isEnabled && !masterEnabled
                masterEnabled = isEnabled
                if (justTurnedOn) {
                    checkCompatibility()
                    refreshNetworkState()
                } else if (!isEnabled) {
                    compatibilityState = CompatibilityState.Incompatible("4G/5G Switcher is turned off")
                    currentNetworkMode = null
                }
            }
        }
    }

    /** Turns the whole feature on/off. Nothing below this gate runs while it is off. */
    fun setMasterEnabled(context: android.content.Context, isEnabled: Boolean) {
        MasterSwitchStore.setEnabled(context, isEnabled)
        if (isEnabled) {
            showFloatingHintPopup = true
        }
        AutomationServiceController.sync(context)
    }

    /** Dismisses the floating popup shown after turning the master toggle on. */
    fun dismissFloatingHintPopup() {
        showFloatingHintPopup = false
    }
    
    /**
     * Observe control method preference changes
     */
    private fun observeControlMethodPreference() {
        viewModelScope.launch {
            preferencesRepository.observeControlMethod().collectLatest { preferredMethod ->
                if (selectedMethod != preferredMethod) {
                    selectedMethod = preferredMethod
                    checkCompatibility()
                    loadToggleModeConfig()
                    refreshNetworkState()
                }
            }
        }
        
        // Also observe toggle mode config changes
        viewModelScope.launch {
            preferencesRepository.observeToggleModeConfig().collectLatest { newConfig ->
                toggleModeConfig = newConfig
                configModeA = newConfig.modeA
                configModeB = newConfig.modeB
                // Force UI update when config changes
                refreshNetworkState()
            }
        }
    }
    
    /**
     * Check compatibility using domain use case
     */
    private fun checkCompatibility() {
        if (!MasterSwitchStore.isEnabled()) {
            compatibilityState = CompatibilityState.Incompatible("4G/5G Switcher is turned off")
            return
        }
        viewModelScope.launch {
            compatibilityState = CompatibilityState.Pending
            compatibilityState = checkCompatibilityUseCase()
        }
    }
    
    /**
     * Switch control method
     */
    fun switchToMethod(method: ControlMethod) {
        viewModelScope.launch {
            updateControlMethodUseCase(method)
        }
    }
    
    /**
     * Retry compatibility check. Explicitly re-requests Shizuku permission first —
     * so tapping "Retry" always re-prompts the user rather than just re-reading the
     * same denied state.
     */
    fun retryCompatibilityCheck() {
        requestShizukuPermissionUseCase()
        checkCompatibility()
    }
    
    /**
     * Refresh all data when app resumes
     */
    fun refreshAllData() {
        checkCompatibility()
        refreshNetworkState()
    }
    
    /**
     * Load toggle mode configuration
     */
    private fun loadToggleModeConfig() {
        viewModelScope.launch {
            try {
                toggleModeConfig = getToggleModeConfigUseCase()
            } catch (e: Exception) {
                // Use default configuration if loading fails
                toggleModeConfig = ToggleModeConfig(NetworkMode.LTE_ONLY, NetworkMode.NR_ONLY)
            }
            configModeA = toggleModeConfig.modeA
            configModeB = toggleModeConfig.modeB
        }
    }

    /** Updates the draft "Mode A" selection in the inline config editor (not yet saved). */
    fun updateConfigModeA(mode: NetworkMode) {
        configModeA = mode
    }

    /** Updates the draft "Mode B" selection in the inline config editor (not yet saved). */
    fun updateConfigModeB(mode: NetworkMode) {
        configModeB = mode
    }

    /** Persists the currently selected Mode A / Mode B pair as the new toggle configuration. */
    fun saveNetworkModeConfig() {
        if (configModeA == configModeB) return

        isSavingConfig = true
        viewModelScope.launch {
            try {
                updateToggleModeConfigUseCase(ToggleModeConfig(configModeA, configModeB))
                configSaved = true
            } catch (e: Exception) {
                // Handle error (could show toast or snackbar)
            } finally {
                isSavingConfig = false
            }
        }
    }

    fun resetConfigSavedFlag() {
        configSaved = false
    }
    
    /**
     * Toggle network mode using configurable modes
     */
    fun toggleNetworkMode() {
        if (isLoading || !MasterSwitchStore.isEnabled()) return
        
        isLoading = true
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            toggleNetworkModeUseCase(subId)
                .onSuccess { newMode ->
                    currentNetworkMode = newMode
                }
                .onFailure {
                    // On failure, refresh state to get current status
                    refreshNetworkState()
                }
            
            isLoading = false
        }
    }
    
    /**
     * Get display text for current network mode and next toggle mode
     */
    fun getToggleButtonText(): String {
        val nextMode = toggleModeConfig.getNextMode()
        return "Switch to ${nextMode.displayName}"
    }
    
    /**
     * Refresh current network state
     */
    private fun refreshNetworkState() {
        if (!MasterSwitchStore.isEnabled()) {
            currentNetworkMode = null
            return
        }
        viewModelScope.launch {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            
            getCurrentNetworkModeUseCase(subId)
                .onSuccess { mode ->
                    currentNetworkMode = mode
                }
                .onFailure {
                    currentNetworkMode = null
                }
        }
    }

    // ───────────────────────── Automation: Screen State / Battery Saver / App Launch ─────────────────────────

    private fun loadAutomationConfigs() {
        viewModelScope.launch {
            try {
                screenStateConfig = NetworkSwitchGraph.getScreenStateConfigUseCase()
            } catch (e: Exception) { /* keep default */ }
        }
        viewModelScope.launch {
            try {
                batterySaverConfig = NetworkSwitchGraph.getBatterySaverConfigUseCase()
            } catch (e: Exception) { /* keep default */ }
        }
        viewModelScope.launch {
            try {
                appLaunchConfig = NetworkSwitchGraph.getAppLaunchConfigUseCase()
            } catch (e: Exception) { /* keep default */ }
        }

        // Keep this screen in sync if the config is changed elsewhere (e.g. the background
        // automation service persisting nothing here, but future multi-entry-point safety).
        viewModelScope.launch {
            preferencesRepository.observeScreenStateConfig().collectLatest { screenStateConfig = it }
        }
        viewModelScope.launch {
            preferencesRepository.observeBatterySaverConfig().collectLatest { batterySaverConfig = it }
        }
        viewModelScope.launch {
            preferencesRepository.observeAppLaunchConfig().collectLatest { appLaunchConfig = it }
        }
    }

    /** Turns "Switch Based On Screen State" on/off. Off by default. */
    fun setScreenStateEnabled(context: android.content.Context, enabled: Boolean) {
        screenStateConfig = screenStateConfig.copy(enabled = enabled)
        AutomationSwitchStore.setScreenStateEnabled(context, enabled)
        viewModelScope.launch { NetworkSwitchGraph.updateScreenStateConfigUseCase(screenStateConfig) }
        AutomationServiceController.sync(context)
    }

    /** Sets which mode is applied when the screen turns off. */
    fun setScreenOffMode(mode: AutomationMode) {
        screenStateConfig = screenStateConfig.copy(screenOffMode = mode)
        viewModelScope.launch { NetworkSwitchGraph.updateScreenStateConfigUseCase(screenStateConfig) }
    }

    /** Sets which mode is applied when the screen turns on. */
    fun setScreenOnMode(mode: AutomationMode) {
        screenStateConfig = screenStateConfig.copy(screenOnMode = mode)
        viewModelScope.launch { NetworkSwitchGraph.updateScreenStateConfigUseCase(screenStateConfig) }
    }

    /** Turns "Switch based on Battery Saver state" on/off. Off by default. */
    fun setBatterySaverEnabled(context: android.content.Context, enabled: Boolean) {
        batterySaverConfig = batterySaverConfig.copy(enabled = enabled)
        AutomationSwitchStore.setBatterySaverEnabled(context, enabled)
        viewModelScope.launch { NetworkSwitchGraph.updateBatterySaverConfigUseCase(batterySaverConfig) }
        AutomationServiceController.sync(context)
    }

    /** Sets which mode is applied whenever Battery Saver is on. */
    fun setBatterySaverMode(mode: AutomationMode) {
        batterySaverConfig = batterySaverConfig.copy(mode = mode)
        viewModelScope.launch { NetworkSwitchGraph.updateBatterySaverConfigUseCase(batterySaverConfig) }
    }

    /** Turns "Switch Based On App Launched" on/off. Off by default. */
    fun setAppLaunchEnabled(context: android.content.Context, enabled: Boolean) {
        appLaunchConfig = appLaunchConfig.copy(enabled = enabled)
        AutomationSwitchStore.setAppLaunchEnabled(context, enabled)
        viewModelScope.launch { NetworkSwitchGraph.updateAppLaunchConfigUseCase(appLaunchConfig) }
        AutomationServiceController.sync(context)
        if (enabled) {
            refreshUsageAccessStatus(context)
            if (installedApps.isEmpty()) loadInstalledApps(context)
        }
    }

    /** Sets the automation mode (None / Mode A / Mode B) for a specific app package. */
    fun setAppMode(packageName: String, mode: AppAutomationMode) {
        val updatedModes = appLaunchConfig.appModes.toMutableMap()
        if (mode == AppAutomationMode.NONE) {
            updatedModes.remove(packageName)
        } else {
            updatedModes[packageName] = mode
        }
        appLaunchConfig = appLaunchConfig.copy(appModes = updatedModes)
        viewModelScope.launch { NetworkSwitchGraph.updateAppLaunchConfigUseCase(appLaunchConfig) }
    }

    /** Loads the list of launchable apps for the app picker. */
    fun loadInstalledApps(context: android.content.Context) {
        if (isLoadingInstalledApps) return
        isLoadingInstalledApps = true
        viewModelScope.launch {
            try {
                installedApps = InstalledAppsProvider.loadLaunchableApps(context.applicationContext)
            } catch (e: Exception) {
                installedApps = emptyList()
            } finally {
                isLoadingInstalledApps = false
            }
        }
    }

    /** Re-checks whether the special "Usage Access" permission has been granted. */
    fun refreshUsageAccessStatus(context: android.content.Context) {
        hasUsageAccess = UsageAccessHelper.hasUsageAccess(context)
    }

    /** Sends the user to the system "Usage Access" settings screen to grant the permission. */
    fun requestUsageAccess(context: android.content.Context) {
        UsageAccessHelper.openUsageAccessSettings(context)
    }
}

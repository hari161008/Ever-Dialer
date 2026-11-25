package com.supernova.networkswitch.domain.repository

import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for network control operations
 */
interface NetworkControlRepository {
    /**
     * Check if network control is compatible with current device/method
     */
    suspend fun checkCompatibility(method: ControlMethod): CompatibilityState
    
    /**
     * Get current network mode
     */
    suspend fun getCurrentNetworkMode(subId: Int): NetworkMode?
    
    /**
     * Set network mode
     */
    suspend fun setNetworkMode(subId: Int, mode: NetworkMode): Result<Unit>
    
    /**
     * Reset connections - useful when switching control methods
     */
    suspend fun resetConnections()

    /**
     * Explicitly (re-)triggers the Shizuku permission dialog. Used by the "Retry"
     * action so a manual tap always re-prompts, even if the one-time automatic
     * request already fired (and was denied) earlier this session.
     */
    fun requestShizukuPermission()
}

/**
 * Repository interface for app preferences
 */
interface PreferencesRepository {
    /**
     * Get preferred control method
     */
    suspend fun getControlMethod(): ControlMethod
    
    /**
     * Set preferred control method
     */
    suspend fun setControlMethod(method: ControlMethod)
    
    /**
     * Observe control method changes
     */
    fun observeControlMethod(): Flow<ControlMethod>
    
    /**
     * Get toggle mode configuration
     */
    suspend fun getToggleModeConfig(): ToggleModeConfig
    
    /**
     * Set toggle mode configuration
     */
    suspend fun setToggleModeConfig(config: ToggleModeConfig)
    
    /**
     * Observe toggle mode configuration changes
     */
    fun observeToggleModeConfig(): Flow<ToggleModeConfig>

    /** Get "Switch Based On Screen State" configuration */
    suspend fun getScreenStateConfig(): ScreenStateAutomationConfig

    /** Set "Switch Based On Screen State" configuration */
    suspend fun setScreenStateConfig(config: ScreenStateAutomationConfig)

    /** Observe "Switch Based On Screen State" configuration changes */
    fun observeScreenStateConfig(): Flow<ScreenStateAutomationConfig>

    /** Get "Switch based on Battery Saver state" configuration */
    suspend fun getBatterySaverConfig(): BatterySaverAutomationConfig

    /** Set "Switch based on Battery Saver state" configuration */
    suspend fun setBatterySaverConfig(config: BatterySaverAutomationConfig)

    /** Observe "Switch based on Battery Saver state" configuration changes */
    fun observeBatterySaverConfig(): Flow<BatterySaverAutomationConfig>

    /** Get "Switch Based On App Launched" configuration */
    suspend fun getAppLaunchConfig(): AppLaunchAutomationConfig

    /** Set "Switch Based On App Launched" configuration */
    suspend fun setAppLaunchConfig(config: AppLaunchAutomationConfig)

    /** Observe "Switch Based On App Launched" configuration changes */
    fun observeAppLaunchConfig(): Flow<AppLaunchAutomationConfig>
}

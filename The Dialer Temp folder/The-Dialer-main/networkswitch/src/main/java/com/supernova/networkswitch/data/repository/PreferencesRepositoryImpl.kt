package com.supernova.networkswitch.data.repository

import com.supernova.networkswitch.data.source.PreferencesDataSource
import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of PreferencesRepository
 */
class PreferencesRepositoryImpl constructor(
    private val preferencesDataSource: PreferencesDataSource
) : PreferencesRepository {
    
    override suspend fun getControlMethod(): ControlMethod {
        return preferencesDataSource.getControlMethod()
    }
    
    override suspend fun setControlMethod(method: ControlMethod) {
        preferencesDataSource.setControlMethod(method)
    }
    
    override fun observeControlMethod(): Flow<ControlMethod> {
        return preferencesDataSource.observeControlMethod()
    }
    
    override suspend fun getToggleModeConfig(): ToggleModeConfig {
        return preferencesDataSource.getToggleModeConfig()
    }
    
    override suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        preferencesDataSource.setToggleModeConfig(config)
    }
    
    override fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return preferencesDataSource.observeToggleModeConfig()
    }

    override suspend fun getScreenStateConfig(): ScreenStateAutomationConfig {
        return preferencesDataSource.getScreenStateConfig()
    }

    override suspend fun setScreenStateConfig(config: ScreenStateAutomationConfig) {
        preferencesDataSource.setScreenStateConfig(config)
    }

    override fun observeScreenStateConfig(): Flow<ScreenStateAutomationConfig> {
        return preferencesDataSource.observeScreenStateConfig()
    }

    override suspend fun getBatterySaverConfig(): BatterySaverAutomationConfig {
        return preferencesDataSource.getBatterySaverConfig()
    }

    override suspend fun setBatterySaverConfig(config: BatterySaverAutomationConfig) {
        preferencesDataSource.setBatterySaverConfig(config)
    }

    override fun observeBatterySaverConfig(): Flow<BatterySaverAutomationConfig> {
        return preferencesDataSource.observeBatterySaverConfig()
    }

    override suspend fun getAppLaunchConfig(): AppLaunchAutomationConfig {
        return preferencesDataSource.getAppLaunchConfig()
    }

    override suspend fun setAppLaunchConfig(config: AppLaunchAutomationConfig) {
        preferencesDataSource.setAppLaunchConfig(config)
    }

    override fun observeAppLaunchConfig(): Flow<AppLaunchAutomationConfig> {
        return preferencesDataSource.observeAppLaunchConfig()
    }
}

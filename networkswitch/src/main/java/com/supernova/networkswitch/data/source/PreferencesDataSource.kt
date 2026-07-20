package com.supernova.networkswitch.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.supernova.networkswitch.domain.model.AppAutomationMode
import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.AutomationMode
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.domain.model.ToggleModeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
class PreferencesDataSource constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val CONTROL_METHOD_KEY = stringPreferencesKey("control_method")
        private val TOGGLE_MODE_A_KEY = intPreferencesKey("toggle_mode_a")
        private val TOGGLE_MODE_B_KEY = intPreferencesKey("toggle_mode_b")
        private val TOGGLE_NEXT_IS_B_KEY = booleanPreferencesKey("toggle_next_is_b")

        private val SCREEN_STATE_ENABLED_KEY = booleanPreferencesKey("screen_state_enabled")
        private val SCREEN_OFF_MODE_KEY = stringPreferencesKey("screen_off_mode")
        private val SCREEN_ON_MODE_KEY = stringPreferencesKey("screen_on_mode")

        private val BATTERY_SAVER_ENABLED_KEY = booleanPreferencesKey("battery_saver_enabled")
        private val BATTERY_SAVER_MODE_KEY = stringPreferencesKey("battery_saver_mode")

        private val APP_LAUNCH_ENABLED_KEY = booleanPreferencesKey("app_launch_enabled")
        private val APP_LAUNCH_MODES_KEY = stringSetPreferencesKey("app_launch_modes")

        private const val DEFAULT_CONTROL_METHOD = "SHIZUKU"
        
        private val DEFAULT_MODE_A = NetworkMode.LTE_ONLY
        private val DEFAULT_MODE_B = NetworkMode.NR_ONLY
        private const val DEFAULT_NEXT_IS_B = true

        private const val APP_LAUNCH_ENTRY_SEPARATOR = "::"
    }
    
    private fun parseControlMethod(methodString: String?): ControlMethod {
        return try {
            ControlMethod.valueOf(methodString ?: DEFAULT_CONTROL_METHOD)
        } catch (e: IllegalArgumentException) {
            ControlMethod.SHIZUKU
        }
    }
    
    suspend fun getControlMethod(): ControlMethod {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }.first()
    }
    
    suspend fun setControlMethod(method: ControlMethod) {
        dataStore.edit { preferences ->
            preferences[CONTROL_METHOD_KEY] = method.name
        }
    }
    
    fun observeControlMethod(): Flow<ControlMethod> {
        return dataStore.data.map { preferences ->
            parseControlMethod(preferences[CONTROL_METHOD_KEY])
        }
    }
    
    suspend fun getToggleModeConfig(): ToggleModeConfig {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }.first()
    }
    
    suspend fun setToggleModeConfig(config: ToggleModeConfig) {
        dataStore.edit { preferences ->
            preferences[TOGGLE_MODE_A_KEY] = config.modeA.value
            preferences[TOGGLE_MODE_B_KEY] = config.modeB.value
            preferences[TOGGLE_NEXT_IS_B_KEY] = config.nextModeIsB
        }
    }
    
    fun observeToggleModeConfig(): Flow<ToggleModeConfig> {
        return dataStore.data.map { preferences ->
            val modeAValue = preferences[TOGGLE_MODE_A_KEY] ?: DEFAULT_MODE_A.value
            val modeBValue = preferences[TOGGLE_MODE_B_KEY] ?: DEFAULT_MODE_B.value
            val nextIsB = preferences[TOGGLE_NEXT_IS_B_KEY] ?: DEFAULT_NEXT_IS_B
            
            val modeA = NetworkMode.fromValue(modeAValue) ?: DEFAULT_MODE_A
            val modeB = NetworkMode.fromValue(modeBValue) ?: DEFAULT_MODE_B
            
            ToggleModeConfig(modeA, modeB, nextIsB)
        }
    }

    private fun parseAutomationMode(value: String?, default: AutomationMode): AutomationMode {
        return try {
            if (value == null) default else AutomationMode.valueOf(value)
        } catch (e: IllegalArgumentException) {
            default
        }
    }

    private fun mapToScreenStateConfig(preferences: Preferences): ScreenStateAutomationConfig {
        return ScreenStateAutomationConfig(
            enabled = preferences[SCREEN_STATE_ENABLED_KEY] ?: false,
            screenOffMode = parseAutomationMode(preferences[SCREEN_OFF_MODE_KEY], AutomationMode.MODE_A),
            screenOnMode = parseAutomationMode(preferences[SCREEN_ON_MODE_KEY], AutomationMode.MODE_B)
        )
    }

    suspend fun getScreenStateConfig(): ScreenStateAutomationConfig {
        return dataStore.data.map { mapToScreenStateConfig(it) }.first()
    }

    suspend fun setScreenStateConfig(config: ScreenStateAutomationConfig) {
        dataStore.edit { preferences ->
            preferences[SCREEN_STATE_ENABLED_KEY] = config.enabled
            preferences[SCREEN_OFF_MODE_KEY] = config.screenOffMode.name
            preferences[SCREEN_ON_MODE_KEY] = config.screenOnMode.name
        }
    }

    fun observeScreenStateConfig(): Flow<ScreenStateAutomationConfig> {
        return dataStore.data.map { mapToScreenStateConfig(it) }
    }

    private fun mapToBatterySaverConfig(preferences: Preferences): BatterySaverAutomationConfig {
        return BatterySaverAutomationConfig(
            enabled = preferences[BATTERY_SAVER_ENABLED_KEY] ?: false,
            mode = parseAutomationMode(preferences[BATTERY_SAVER_MODE_KEY], AutomationMode.MODE_A)
        )
    }

    suspend fun getBatterySaverConfig(): BatterySaverAutomationConfig {
        return dataStore.data.map { mapToBatterySaverConfig(it) }.first()
    }

    suspend fun setBatterySaverConfig(config: BatterySaverAutomationConfig) {
        dataStore.edit { preferences ->
            preferences[BATTERY_SAVER_ENABLED_KEY] = config.enabled
            preferences[BATTERY_SAVER_MODE_KEY] = config.mode.name
        }
    }

    fun observeBatterySaverConfig(): Flow<BatterySaverAutomationConfig> {
        return dataStore.data.map { mapToBatterySaverConfig(it) }
    }

    private fun encodeAppModes(appModes: Map<String, AppAutomationMode>): Set<String> {
        return appModes
            .filterValues { it != AppAutomationMode.NONE }
            .map { (packageName, mode) -> "$packageName$APP_LAUNCH_ENTRY_SEPARATOR${mode.name}" }
            .toSet()
    }

    private fun decodeAppModes(raw: Set<String>?): Map<String, AppAutomationMode> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val result = mutableMapOf<String, AppAutomationMode>()
        raw.forEach { entry ->
            val separatorIndex = entry.lastIndexOf(APP_LAUNCH_ENTRY_SEPARATOR)
            if (separatorIndex <= 0) return@forEach
            val packageName = entry.substring(0, separatorIndex)
            val modeString = entry.substring(separatorIndex + APP_LAUNCH_ENTRY_SEPARATOR.length)
            val mode = try {
                AppAutomationMode.valueOf(modeString)
            } catch (e: IllegalArgumentException) {
                AppAutomationMode.NONE
            }
            if (mode != AppAutomationMode.NONE) {
                result[packageName] = mode
            }
        }
        return result
    }

    private fun mapToAppLaunchConfig(preferences: Preferences): AppLaunchAutomationConfig {
        return AppLaunchAutomationConfig(
            enabled = preferences[APP_LAUNCH_ENABLED_KEY] ?: false,
            appModes = decodeAppModes(preferences[APP_LAUNCH_MODES_KEY])
        )
    }

    suspend fun getAppLaunchConfig(): AppLaunchAutomationConfig {
        return dataStore.data.map { mapToAppLaunchConfig(it) }.first()
    }

    suspend fun setAppLaunchConfig(config: AppLaunchAutomationConfig) {
        dataStore.edit { preferences ->
            preferences[APP_LAUNCH_ENABLED_KEY] = config.enabled
            preferences[APP_LAUNCH_MODES_KEY] = encodeAppModes(config.appModes)
        }
    }

    fun observeAppLaunchConfig(): Flow<AppLaunchAutomationConfig> {
        return dataStore.data.map { mapToAppLaunchConfig(it) }
    }
}

package com.supernova.networkswitch.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fast, synchronous mirror of whether each of the three automation rules ("Switch Based On
 * Screen State", "Switch based on Battery Saver state", "Switch Based On App Launched") is
 * enabled — analogous to [MasterSwitchStore] but for these sub-features.
 *
 * The detailed configuration for each rule (which modes, which apps) lives in the DataStore-backed
 * [com.supernova.networkswitch.domain.repository.PreferencesRepository]. This store exists purely
 * so that call sites which cannot suspend — [AutomationBootReceiver] deciding whether to start the
 * background service after boot, and [AutomationServiceController] deciding whether the service
 * should be running at all — can read/write the "is this rule on" flag instantly.
 *
 * All three default to OFF, matching the toggles' default state in the UI.
 */
object AutomationSwitchStore {
    private const val PREFS_NAME = "network_switch_automation_flags"
    private const val KEY_SCREEN_STATE_ENABLED = "screen_state_enabled"
    private const val KEY_BATTERY_SAVER_ENABLED = "battery_saver_enabled"
    private const val KEY_APP_LAUNCH_ENABLED = "app_launch_enabled"

    @Volatile
    private var initialized = false
    private lateinit var prefs: android.content.SharedPreferences

    private val _screenStateEnabled = MutableStateFlow(false)
    val screenStateEnabled: StateFlow<Boolean> get() = _screenStateEnabled

    private val _batterySaverEnabled = MutableStateFlow(false)
    val batterySaverEnabled: StateFlow<Boolean> get() = _batterySaverEnabled

    private val _appLaunchEnabled = MutableStateFlow(false)
    val appLaunchEnabled: StateFlow<Boolean> get() = _appLaunchEnabled

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _screenStateEnabled.value = prefs.getBoolean(KEY_SCREEN_STATE_ENABLED, false)
        _batterySaverEnabled.value = prefs.getBoolean(KEY_BATTERY_SAVER_ENABLED, false)
        _appLaunchEnabled.value = prefs.getBoolean(KEY_APP_LAUNCH_ENABLED, false)
        initialized = true
    }

    fun isAnyEnabled(context: Context): Boolean {
        init(context)
        return _screenStateEnabled.value || _batterySaverEnabled.value || _appLaunchEnabled.value
    }

    fun setScreenStateEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean(KEY_SCREEN_STATE_ENABLED, enabled).apply()
        _screenStateEnabled.value = enabled
    }

    fun setBatterySaverEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean(KEY_BATTERY_SAVER_ENABLED, enabled).apply()
        _batterySaverEnabled.value = enabled
    }

    fun setAppLaunchEnabled(context: Context, enabled: Boolean) {
        init(context)
        prefs.edit().putBoolean(KEY_APP_LAUNCH_ENABLED, enabled).apply()
        _appLaunchEnabled.value = enabled
    }
}

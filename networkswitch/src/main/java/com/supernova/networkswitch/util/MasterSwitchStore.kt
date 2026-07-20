package com.supernova.networkswitch.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Master kill switch for the entire 4G/5G Switcher feature.
 *
 * This is intentionally backed by plain synchronous SharedPreferences (separate from the
 * DataStore used for the other network-switch preferences) so the enabled/disabled state can be
 * read instantly from *any* call site — including non-suspend entry points such as the Quick
 * Settings tile and the Shizuku permission request — without first having to touch the Shizuku or
 * root data sources.
 *
 * Defaults to OFF. Every entry point into network-switching functionality (compatibility checks,
 * reading/setting the network mode, requesting Shizuku permission, the Quick Settings tile) must
 * check [isEnabled] before doing anything else. When it is false, nothing below this gate is
 * allowed to run — this is enforced centrally in [com.supernova.networkswitch.data.repository.NetworkControlRepositoryImpl],
 * which is the single gateway every other component (ViewModels, Quick Settings tile) goes
 * through to reach the Shizuku/root data sources.
 */
object MasterSwitchStore {
    private const val PREFS_NAME = "network_switch_master_switch"
    private const val KEY_ENABLED = "master_enabled"
    private const val DEFAULT_ENABLED = false

    @Volatile
    private var initialized = false
    private lateinit var prefs: android.content.SharedPreferences

    private val _enabled = MutableStateFlow(DEFAULT_ENABLED)
    val enabled: StateFlow<Boolean> get() = _enabled

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
        initialized = true
    }

    /** Synchronous read of the current state. Safe to call from anywhere once [init] has run. */
    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(context: Context, value: Boolean) {
        init(context)
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }
}

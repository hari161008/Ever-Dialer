package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)

    private val _settingsChanged = MutableStateFlow(0)
    val settingsChanged: StateFlow<Int> = _settingsChanged.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settingsChanged.value += 1
    }

    init { prefs.registerOnSharedPreferenceChangeListener(listener) }

    fun getBoolean(key: String, defaultValue: Boolean) = prefs.getBoolean(key, defaultValue)
    fun setBoolean(key: String, value: Boolean)        { prefs.edit().putBoolean(key, value).apply() }
    fun getString(key: String, defaultValue: String?)  = prefs.getString(key, defaultValue)
    fun setString(key: String, value: String?)         { prefs.edit().putString(key, value).apply() }
    fun getInt(key: String, defaultValue: Int)         = prefs.getInt(key, defaultValue)
    fun setInt(key: String, value: Int)                { prefs.edit().putInt(key, value).apply() }
    fun getFloat(key: String, defaultValue: Float)     = prefs.getFloat(key, defaultValue)
    fun setFloat(key: String, value: Float)            { prefs.edit().putFloat(key, value).apply() }

    companion object {
        const val KEY_DYNAMIC_COLORS        = "dynamic_colors"
        const val KEY_AMOLED_MODE           = "amoled_mode"
        const val KEY_SHOW_FIRST_LETTER     = "show_first_letter"
        const val KEY_COLORFUL_AVATARS      = "colorful_avatars"
        const val KEY_SHOW_PICTURE          = "show_picture"
        const val KEY_ICON_ONLY_NAV         = "icon_only_nav"
        const val KEY_DTMF_TONE             = "dtmf_tone"
        const val KEY_DIALPAD_VIBRATION     = "dialpad_vibration"
        const val KEY_SPEED_DIAL            = "speed_dial"
        const val KEY_T9_DIALING            = "t9_dialing"
        const val KEY_BLOCK_UNKNOWN         = "block_unknown_callers"
        const val KEY_BLOCK_HIDDEN          = "block_hidden_callers"
        const val KEY_OPEN_DIALPAD_DEFAULT  = "open_dialpad_default"
        const val KEY_APP_HAPTICS              = "app_haptics_enabled"
        const val KEY_APP_HAPTICS_STRENGTH     = "app_haptics_strength"
        const val KEY_HAPTICS_CUSTOM_INTENSITY = "haptics_custom_intensity"
        const val KEY_NOTES_ENABLED         = "notes_enabled"
        const val KEY_CUSTOM_FONT_PATH      = "custom_font_path"
        const val KEY_CUSTOM_FONT_SIZE      = "custom_font_size"
        const val KEY_THEME_MODE            = "theme_mode"
        const val KEY_BLOCKED_CONTACTS      = "blocked_contacts"
        const val KEY_SHOW_INCOMING_CALL_UI = "show_incoming_call_ui"
        const val KEY_SHOW_CALLER_UI        = "show_caller_ui"
        const val KEY_SILENCE_UNKNOWN       = "silence_unknown_callers"
        const val KEY_PROXIMITY_BG          = "proximity_sensor_bg"
        const val KEY_SCROLL_HAPTICS        = "scroll_haptics_enabled"
        const val KEY_HAPTICS_STRENGTH      = "app_haptics_strength"
        const val KEY_CALL_UI_SHOW_TODAY    = "call_ui_show_today"
        const val KEY_CALL_UI_SHOW_MISSED   = "call_ui_show_missed"
        const val KEY_CALL_UI_SHOW_OUTGOING = "call_ui_show_outgoing"
        const val KEY_CALL_UI_SHOW_CALL_TIME = "call_ui_show_call_time"
        const val KEY_AUTO_UPDATE_CHECK     = "auto_update_check"
        const val KEY_PILL_NAV              = "pill_style_nav"
        const val KEY_FIRST_LAUNCH_DONE     = "first_launch_done"
        // Hangup button width fraction (0.4f .. 1.0f)
        const val KEY_HANGUP_WIDTH          = "hangup_button_width"
        // Dialer role popup shown after welcome
        const val KEY_DIALER_POPUP_SHOWN    = "dialer_popup_shown"
        const val KEY_SCROLL_ANIMATION      = "scroll_animation_enabled"
    }
}

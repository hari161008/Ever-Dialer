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
    fun setBoolean(key: String, value: Boolean)        { prefs.edit().putBoolean(key, value).apply(); _settingsChanged.value += 1 }
    fun getString(key: String, defaultValue: String?)  = prefs.getString(key, defaultValue)
    fun setString(key: String, value: String?)         { prefs.edit().putString(key, value).apply(); _settingsChanged.value += 1 }
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
        const val KEY_POCKET_MODE_PREVENTION = "pocket_mode_prevention"
        const val KEY_DIRECT_CALL_ON_TAP     = "direct_call_on_tap"
        const val KEY_CONTACTS_DISPLAY_ACCOUNTS = "contacts_display_accounts"
        const val KEY_LIQUID_GLASS              = "liquid_glass_ui"
        const val KEY_LG_BOTTOM_NAV            = "lg_bottom_nav"
        const val KEY_LG_DROPDOWN_MENU         = "lg_dropdown_menu"
        const val KEY_LG_DIALPAD_CALL_BUTTON   = "lg_dialpad_call_button"
        const val KEY_LG_CONTACTS_FAB          = "lg_contacts_fab"
        const val KEY_LG_RECENTS_FAB           = "lg_recents_fab"
        const val KEY_BLUR_EFFECTS            = "blur_effects_ui"
        // Material Blur effect elements
        const val KEY_BLUR_BOTTOM_NAV          = "blur_bottom_nav"
        const val KEY_BLUR_DROPDOWN_MENU       = "blur_dropdown_menu"
        const val KEY_BLUR_DIALPAD_CALL_BUTTON = "blur_dialpad_call_button"
        const val KEY_BLUR_CONTACTS_FAB        = "blur_contacts_fab"
        const val KEY_BLUR_RECENTS_FAB         = "blur_recents_fab"
        const val KEY_DEFAULT_TAB              = "default_tab"
    }
}

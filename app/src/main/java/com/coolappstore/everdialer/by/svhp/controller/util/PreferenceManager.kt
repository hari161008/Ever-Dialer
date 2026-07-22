package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)

    /** Number of currently active SIM subscriptions (0, 1, 2+). Never throws. */
    fun getActiveSimCount(): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1) return 1
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    appContext, android.Manifest.permission.READ_PHONE_STATE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return 1
            val sm = appContext.getSystemService(android.telephony.SubscriptionManager::class.java)
            val list = if (android.os.Build.VERSION_CODES.N <= android.os.Build.VERSION.SDK_INT) {
                sm?.activeSubscriptionInfoList
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SubscriptionManager.from(appContext).activeSubscriptionInfoList
            }
            list?.size ?: 1
        } catch (_: Exception) { 1 }
    }

    /** "Show SIM badges in call logs" should only default to on for dual-SIM (or more)
     *  devices — on a single-SIM device the badge is pure clutter since every entry
     *  would show the same "SIM 1" chip. */
    fun getShowSimsInCallLogsDefault(): Boolean = getActiveSimCount() >= 2

    /** Default-SIM-for-calling behavior should only default to "always ask" on
     *  dual-SIM (or more) devices. On a single-SIM device there is nothing to ask —
     *  default straight to that one SIM so every call just goes out immediately. */
    fun getDefaultSimAskEveryTimeDefault(): Boolean = getActiveSimCount() >= 2

    /** Default value for the "default_sim" pref (0 = ask every time, 1 = SIM 1, 2 = SIM 2).
     *  On a single-SIM device there's nothing to ask about, so default straight to that
     *  one SIM instead of nagging with an "Ask every time" dialog on every call. */
    fun getDefaultSimIndexDefault(): Int = if (getActiveSimCount() == 1) 1 else 0

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
    fun setInt(key: String, value: Int)                { prefs.edit().putInt(key, value).apply(); _settingsChanged.value += 1 }
    fun getFloat(key: String, defaultValue: Float)     = prefs.getFloat(key, defaultValue)
    fun setFloat(key: String, value: Float)            { prefs.edit().putFloat(key, value).apply(); _settingsChanged.value += 1 }

    /** Returns true if an incoming call from [phoneNumber] should be gated behind biometric. */
    fun shouldGateCallWithBiometric(phoneNumber: String?): Boolean {
        if (!getBoolean(KEY_BIOMETRICS_CALL_LOCK, false)) return false
        if ((getString(KEY_BIOMETRICS_TYPE, "") ?: "").isEmpty()) return false
        val mode = getString(KEY_BIOMETRICS_CALL_LOCK_MODE, "all") ?: "all"
        if (mode == "all") return true
        if (phoneNumber.isNullOrBlank()) return mode == "skip_specified"
        val stored = getString(KEY_BIOMETRICS_CALL_LOCK_NUMBERS, "") ?: ""
        if (stored.isBlank()) return mode == "skip_specified"
        val incoming = phoneNumber.filter { it.isDigit() }.takeLast(10)
        val match = stored.split(",").any { raw ->
            val n = raw.trim().filter { it.isDigit() }.takeLast(10)
            n.isNotEmpty() && (incoming.endsWith(n) || n.endsWith(incoming))
        }
        return if (mode == "specified") match else !match
    }

    companion object {
        const val KEY_DEFAULT_SIM           = "default_sim"
        const val KEY_DYNAMIC_COLORS        = "dynamic_colors"
        const val KEY_AMOLED_MODE           = "amoled_mode"
        const val KEY_SHOW_FIRST_LETTER     = "show_first_letter"
        const val KEY_COLORFUL_AVATARS      = "colorful_avatars"
        const val KEY_SHOW_PICTURE          = "show_picture"
        const val KEY_ICON_ONLY_NAV         = "icon_only_nav"
        const val KEY_DTMF_TONE             = "dtmf_tone"
        const val KEY_DIALPAD_TONE_STYLE    = "dialpad_tone_style" // "standard" | "piano" | "water_drop" | "mechanical" | "scifi"
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
        // When true (default), the app's Notes section and the call recording notes
        // (inside Ever Call Recorder's recording playback screen) are kept as two
        // independent notes sections. When false, they're merged: a contact's Notes
        // card also surfaces any notes attached to that contact's call recordings.
        const val KEY_INTEGRATE_NOTES       = "integrate_notes_section"
        const val KEY_DELETE_NOTES_WITH_RECORDING = "delete_notes_with_recording"
        const val KEY_CUSTOM_FONT_PATH      = "custom_font_path"
        const val KEY_CUSTOM_FONT_SIZE      = "custom_font_size"
        const val KEY_THEME_MODE            = "theme_mode"
        const val KEY_BLOCKED_CONTACTS      = "blocked_contacts"
        const val KEY_SHOW_INCOMING_CALL_UI = "show_incoming_call_ui"
        const val KEY_SHOW_CALLER_UI        = "show_caller_ui"
        const val KEY_SILENCE_UNKNOWN       = "silence_unknown_callers"
        const val KEY_PROXIMITY_BG          = "proximity_sensor_bg"
        const val KEY_SCROLL_HAPTICS        = "scroll_haptics_enabled"
        const val KEY_SCROLL_CM_PER_HAPTIC  = "scroll_cm_per_haptic"   // cm scrolled before each haptic tick
        const val KEY_SCROLL_HAPTICS_PER_CM = "scroll_haptics_per_cm"  // haptic ticks per cm
        const val KEY_SCROLL_HAPTIC_STRENGTH = "scroll_haptic_strength" // vibration amplitude 1–255
        const val KEY_HAPTICS_STRENGTH      = "app_haptics_strength"
        const val KEY_CALL_UI_SHOW_TODAY    = "call_ui_show_today"
        const val KEY_CALL_UI_SHOW_MISSED   = "call_ui_show_missed"
        const val KEY_CALL_UI_SHOW_OUTGOING = "call_ui_show_outgoing"
        const val KEY_CALL_UI_SHOW_CALL_TIME = "call_ui_show_call_time"
        const val KEY_AUTO_UPDATE_CHECK     = "auto_update_check"
        const val KEY_PILL_NAV              = "pill_style_nav"
        const val KEY_SHOW_SIMS_IN_CALL_LOGS = "show_sims_in_call_logs"
        // Call Time Format in call logs — false = 12-hour (default), true = 24-hour
        const val KEY_CALL_TIME_FORMAT_24H  = "call_time_format_24h"
        // Whether the ongoing-call screen smoothly slides away when a call ends. Default true;
        // when false, the call screen should close immediately instead.
        const val KEY_HANGUP_ANIMATION      = "hangup_animation"
        const val KEY_FIRST_LAUNCH_DONE     = "first_launch_done"
        // Hangup button width fraction (0.4f .. 1.0f)
        const val KEY_HANGUP_WIDTH          = "hangup_button_width"
        // Feature Buttons — ongoing call screen button customization
        // Comma-separated ordered list of button ids (see CallButtonPrefs.ALL_IDS)
        const val KEY_CALL_BUTTONS_ORDER    = "call_buttons_order"
        // Comma-separated list of button ids that are hidden from the call screen
        const val KEY_CALL_BUTTONS_DISABLED = "call_buttons_disabled"
        // Dialer role popup shown after welcome
        const val KEY_DIALER_POPUP_SHOWN    = "dialer_popup_shown"
        const val KEY_TELEGRAM_SHOWN        = "telegram_shown"
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
        const val KEY_AUTO_SPEAKER             = "auto_speaker"
        const val KEY_FAVORITES_ORDER          = "favorites_order"
        const val KEY_FLOATING_CALL            = "floating_ongoing_call"
        // Tab Sections visibility
        const val KEY_TAB_SHOW_FAVORITES       = "tab_show_favorites"
        const val KEY_TAB_SHOW_CALLS           = "tab_show_calls"
        const val KEY_TAB_SHOW_CONTACTS        = "tab_show_contacts"
        const val KEY_TAB_SHOW_RECORDINGS      = "tab_show_recordings"
        const val KEY_TAB_SHOW_NOTES           = "tab_show_notes"
        // Comma-separated list of tab keys (favorites, calls, contacts, recordings, notes)
        // describing the order tabs appear in the bottom navigation bar.
        const val KEY_TAB_ORDER                = "tab_order"
        const val DEFAULT_TAB_ORDER            = "favorites,calls,contacts,recordings,notes"
        // Biometrics
        const val KEY_BIOMETRICS_TYPE          = "biometrics_type"         // "system" | "pin" | "password" | ""
        const val KEY_BIOMETRICS_PIN           = "biometrics_pin"
        const val KEY_BIOMETRICS_PASSWORD      = "biometrics_password"
        const val KEY_BIOMETRICS_APP_LOCK      = "biometrics_app_lock"
        const val KEY_BIOMETRICS_CALL_LOCK     = "biometrics_call_lock"
        const val KEY_BIOMETRICS_CALL_LOCK_MODE    = "biometrics_call_lock_mode"    // "all" | "specified" | "skip_specified"
        const val KEY_BIOMETRICS_CALL_LOCK_NUMBERS = "biometrics_call_lock_numbers" // comma-separated phone numbers
        // Raise to Answer
        const val KEY_RAISE_TO_ANSWER_ENABLED      = "raise_to_answer_enabled"
        const val KEY_RAISE_TO_ANSWER_ANY_ANGLE    = "raise_to_answer_any_angle"
        const val KEY_RAISE_TO_ANSWER_DECLINE_FLIP = "raise_to_answer_decline_flip"
        const val KEY_RAISE_TO_ANSWER_BEEP         = "raise_to_answer_beep"
        const val KEY_RAISE_TO_ANSWER_VIBRATE      = "raise_to_answer_vibrate"
        // Auto Redial
        const val KEY_AUTO_REDIAL_ENABLED      = "auto_redial_enabled"
        // Updates — version tag of the APK currently sitting in Downloads (if any)
        const val KEY_DOWNLOADED_UPDATE_VERSION = "downloaded_update_version"
        // Fake Call — JSON-encoded list of scheduled fake calls
        const val KEY_FAKE_CALLS                = "fake_calls"
        // Fake Call — show a "Fake Call" entry in the dialpad's long-press context menu
        const val KEY_FAKE_CALL_IN_CONTEXT_MENU = "fake_call_in_context_menu"
        // Fake Call — remembers the last-used time/timer/day settings (everything except
        // name and number) so creating a new fake call starts from what was used last,
        // instead of always resetting to hardcoded defaults.
        const val KEY_FAKE_CALL_LAST_MODE          = "fake_call_last_mode"          // "clock" | "timer"
        const val KEY_FAKE_CALL_LAST_HOUR           = "fake_call_last_hour"
        const val KEY_FAKE_CALL_LAST_MINUTE         = "fake_call_last_minute"
        const val KEY_FAKE_CALL_LAST_DAYS           = "fake_call_last_days"         // comma-separated Calendar.DAY_OF_WEEK ints
        const val KEY_FAKE_CALL_LAST_TIMER_AMOUNT   = "fake_call_last_timer_amount"
        const val KEY_FAKE_CALL_LAST_TIMER_UNIT     = "fake_call_last_timer_unit"   // "seconds" | "minutes"
        // Default Message app for the incoming-call screen's Message quick action.
        // Values: "sms" (default), "whatsapp", "telegram", "ask"
        const val KEY_DEFAULT_MESSAGE_APP = "default_message_app"

        const val KEY_CONTACTS_HIDER_CODE         = "contacts_hider_code"          // numeric secret code string
        const val KEY_CONTACTS_HIDER_IDS          = "contacts_hider_ids"           // comma-separated contact IDs
        const val KEY_CONTACTS_HIDER_HIDE_NAMES   = "contacts_hider_hide_names"    // bool
        const val KEY_CONTACTS_HIDER_HIDE_MENU    = "contacts_hider_hide_menu"     // bool

        // Search filter (Dialpad / Calls / Contacts / Favourites search bars) — the "Filter"
        // button beside the search bar. All four default to true (checked) so search behaves
        // as broadly as possible until the user deliberately narrows it down. Persisted here
        // (rather than in-memory) so the chosen filter survives the app being closed and
        // reopened.
        const val KEY_SEARCH_FILTER_CONTACTS        = "search_filter_contacts"
        const val KEY_SEARCH_FILTER_NON_CONTACTS    = "search_filter_non_contacts"
        const val KEY_SEARCH_FILTER_RECORDINGS      = "search_filter_recordings"
        const val KEY_SEARCH_FILTER_CONTACT_NOTES   = "search_filter_contact_notes"
        const val KEY_SEARCH_FILTER_RECORDING_NOTES = "search_filter_recording_notes"
    }
}

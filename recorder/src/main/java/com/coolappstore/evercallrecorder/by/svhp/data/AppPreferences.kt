/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.edit
import androidx.core.net.toUri
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioCodec
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioSource
import java.io.File

class AppPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "evercallrecorder_prefs"
        // Default accent color: original green (ARGB packed as Int)
        // Equivalent to Color(0xFF386B20): alpha=255, R=56, G=107, B=32
        val DEFAULT_ACCENT_ARGB: Int = (255 shl 24) or (0x38 shl 16) or (0x6B shl 8) or 0x20

        // The custom font chosen in Ever Dialer's own settings (Settings → Interface) is stored
        // in that app's "rivo_prefs" SharedPreferences, under these keys. Both modules ship in the
        // same app process, so the recorder module reads that same file/key here directly to stay
        // in sync with whatever font the user picked, instead of ignoring it and always falling
        // back to the system default font.
        private const val RIVO_PREFS_NAME = "rivo_prefs"
        private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
    }

    object DefaultsValue {
        const val DISCLAIMER_ACCEPTED = false
        val RECORDING_FOLDER_URI: String? = null
        val STORAGE_MODE: String? = null
        // Universal master switch: recording (and everything that monitors calls to make it
        // possible — phone state listening, the app-call notification listener, the recording
        // foreground service) is entirely off until the user explicitly turns this on.
        const val CALL_RECORDING_ENABLED = false
        const val VIBRATION_ENABLED = true
        const val AUTO_RECORD_INCOMING = true
        const val AUTO_RECORD_OUTGOING = true
        const val RECORD_ON_ANSWER = true
        const val IGNORE_ANONYMOUS_INCOMING = false
        const val IGNORE_CROSS_COUNTRY_INCOMING = false
        const val IGNORE_CROSS_COUNTRY_OUTGOING = false
        val IGNORE_CONTACTS_MODE_INCOMING = IgnoreContactsMode.NONE
        val IGNORE_CONTACTS_MODE_OUTGOING = IgnoreContactsMode.NONE
        val IGNORED_CONTACTS_INCOMING = emptySet<String>()
        val IGNORED_CONTACTS_OUTGOING = emptySet<String>()
        const val LOGGING_ENABLED = false
        const val DEBUG_ENABLED = false
        const val DEBUG_CALLER_NUMBER = ""
        val AUDIO_SOURCE = ScrcpyAudioSource.VOICE_CALL.cliKey
        val AUDIO_CODEC = ScrcpyAudioCodec.OPUS.cliKey
        val AUDIO_BITRATE = ScrcpyAudioCodec.OPUS.defaultBitRate
        // Default: contact name first (falls back to blank when unknown), then date, then
        // call direction — replaces the old phone-number-first default so recordings list/sort
        // by who they're with at a glance instead of a raw number.
        const val FILE_NAME_TEMPLATE = "{contact_name}_{date}_{direction}"
        val THEME_MODE = ThemeMode.SYSTEM
        const val DYNAMIC_COLOR = true
        const val SHOW_TOASTS = true
        const val RECORDING_NOTIFICATIONS_ENABLED = true
        const val SHIZUKU_AUTO_MANAGE = false
        const val SHIZUKU_START_ON_RECORD = false
        const val SHIZUKU_KEEP_ALIVE = false
        const val SHIZUKU_AUTH_KEY = ""
        // Accent color default: original green
        val ACCENT_COLOR: Int = DEFAULT_ACCENT_ARGB
        // Auto-delete defaults
        const val AUTO_DELETE_BY_TIME_ENABLED = false
        const val AUTO_DELETE_BY_TIME_VALUE   = 7
        const val AUTO_DELETE_BY_TIME_UNIT    = "days"   // "hours" | "days"
        const val AUTO_DELETE_BY_SPACE_ENABLED = false
        const val AUTO_DELETE_BY_SPACE_VALUE   = 500
        const val AUTO_DELETE_BY_SPACE_UNIT    = "mb"    // "mb" | "gb"
        const val AUTO_UPDATE_CHECK            = true
        const val WELCOME_SHOWN                = false
        // App lock defaults
        const val APP_LOCK_ENABLED = false
        val APP_LOCK_METHOD = AppLockMethod.NONE
        val APP_LOCK_SECRET_HASH: String? = null
        val APP_LOCK_SALT: String? = null
        // Record calls from apps defaults — both OFF (unticked) until the user opts in.
        const val RECORD_WHATSAPP_CALLS = false
        const val RECORD_TELEGRAM_CALLS = false
        // Call detection defaults to the original, proven phone-state broadcast method.
        val CALL_DETECTION_MODE = CallDetectionMode.PHONE_STATE
        // Post-call file-actions notification (Open/Share/Delete) — on by default, purely additive.
        const val POST_RECORDING_FILE_ACTIONS_NOTIFICATION = true
    }

    enum class Key(val id: String) {
        DISCLAIMER_ACCEPTED("disclaimer_accepted"),
        RECORDING_FOLDER_URI("recording_folder_uri"),
        CALL_RECORDING_ENABLED("call_recording_enabled"),
        STORAGE_MODE("storage_mode"),
        VIBRATION_ENABLED("vibration_enabled"),
        AUTO_RECORD_INCOMING("auto_record_incoming"),
        AUTO_RECORD_OUTGOING("auto_record_outgoing"),
        RECORD_ON_ANSWER("record_on_answer"),
        IGNORE_ANONYMOUS_INCOMING("ignore_anonymous_incoming"),
        IGNORE_CROSS_COUNTRY_INCOMING("ignore_cross_country_incoming"),
        IGNORE_CROSS_COUNTRY_OUTGOING("ignore_cross_country_outgoing"),
        IGNORE_CONTACTS_MODE_INCOMING("ignore_contacts_mode_incoming"),
        IGNORE_CONTACTS_MODE_OUTGOING("ignore_contacts_mode_outgoing"),
        IGNORED_CONTACTS_INCOMING("ignored_contacts_incoming"),
        IGNORED_CONTACTS_OUTGOING("ignored_contacts_outgoing"),
        LOGGING_ENABLED("logging_enabled"),
        DEBUG_ENABLED("debug_enabled"),
        DEBUG_CALLER_NUMBER("debug_caller_number"),
        AUDIO_SOURCE("audio_source"),
        AUDIO_CODEC("audio_codec"),
        AUDIO_BITRATE("audio_bitrate"),
        FILE_NAME_TEMPLATE("file_name_template"),
        THEME_MODE("theme_mode"),
        DYNAMIC_COLOR("dynamic_color"),
        SHOW_TOASTS("show_toasts"),
        RECORDING_NOTIFICATIONS_ENABLED("recording_notifications_enabled"),
        SHIZUKU_AUTO_MANAGE("shizuku_auto_manage"),
        SHIZUKU_START_ON_RECORD("shizuku_start_on_record"),
        SHIZUKU_KEEP_ALIVE("shizuku_keep_alive"),
        SHIZUKU_AUTH_KEY("shizuku_auth_key"),
        ACCENT_COLOR("accent_color"),
        AUTO_DELETE_BY_TIME_ENABLED("auto_delete_by_time_enabled"),
        AUTO_DELETE_BY_TIME_VALUE("auto_delete_by_time_value"),
        AUTO_DELETE_BY_TIME_UNIT("auto_delete_by_time_unit"),
        AUTO_DELETE_BY_SPACE_ENABLED("auto_delete_by_space_enabled"),
        AUTO_DELETE_BY_SPACE_VALUE("auto_delete_by_space_value"),
        AUTO_DELETE_BY_SPACE_UNIT("auto_delete_by_space_unit"),
        AUTO_UPDATE_CHECK("auto_update_check"),
        WELCOME_SHOWN("welcome_shown"),
        APP_LOCK_ENABLED("app_lock_enabled"),
        APP_LOCK_METHOD("app_lock_method"),
        APP_LOCK_SECRET_HASH("app_lock_secret_hash"),
        APP_LOCK_SALT("app_lock_salt"),
        RECORD_WHATSAPP_CALLS("record_whatsapp_calls"),
        RECORD_TELEGRAM_CALLS("record_telegram_calls"),
        CALL_DETECTION_MODE("call_detection_mode"),
        POST_RECORDING_FILE_ACTIONS_NOTIFICATION("post_recording_file_actions_notification");
    }

    enum class IgnoreContactsMode(val key: String) {
        NONE("none"), ALL("all"), SELECTED("selected");
        companion object {
            fun fromKey(key: String?): IgnoreContactsMode =
                entries.firstOrNull { it.key == key } ?: throw IllegalArgumentException("Unknown IgnoreContactsMode key: $key")
        }
    }

    /**
     * Where call recordings are written to.
     *
     * - [SAF_FOLDER] - a user-chosen folder, accessed through the Storage Access Framework.
     *   Recordings can be browsed and shared with any file manager.
     * - [PRIVATE]    - the app's own private internal storage. Only this app can access these
     *   files (no other app, and no file manager, can read them without root), offering extra privacy.
     */
    enum class StorageMode(val key: String) {
        SAF_FOLDER("saf_folder"), PRIVATE("private");
        companion object {
            fun fromKey(key: String?): StorageMode? = entries.firstOrNull { it.key == key }
        }
    }

    enum class ThemeMode(val key: String) {
        SYSTEM("system"), LIGHT("light"), DARK("dark"), WHITE("white"), BLACK("black"), AUTO_WB("auto_wb");
        companion object {
            fun fromKey(key: String?): ThemeMode =
                entries.firstOrNull { it.key == key } ?: throw IllegalArgumentException("Unknown ThemeMode key: $key")
        }
    }

    /**
     * How incoming/outgoing calls are detected.
     *
     * - [PHONE_STATE]     - the original method: a manifest [android.content.BroadcastReceiver] listening
     *   for the system's PHONE_STATE broadcast. Works everywhere, but on some OEMs this broadcast can
     *   arrive late or be throttled, and it never carries the number for anonymous/private callers.
     * - [IN_CALL_SERVICE] - an additional detection method using Android's Telecom [android.telecom.InCallService]
     *   API, which observes calls directly instead of waiting on a broadcast, and can also pick up
     *   self-managed (VoIP, e.g. WhatsApp/Telegram) calls without the notification-listener workaround.
     *   Requires the MANAGE_ONGOING_CALLS AppOp, granted through Shizuku from Settings.
     *
     * Default is [PHONE_STATE] so existing installs keep behaving exactly as before unless the user
     * opts in to the new method.
     */
    enum class CallDetectionMode(val key: String) {
        PHONE_STATE("phone_state"), IN_CALL_SERVICE("in_call_service");
        companion object {
            fun fromKey(key: String?): CallDetectionMode =
                entries.firstOrNull { it.key == key } ?: PHONE_STATE
        }
    }

    /**
     * How the App Lock screen verifies it is really the owner opening the app.
     *
     * - [NONE]      - App Lock is disabled, the app opens straight to the home screen.
     * - [PIN]       - A numeric code (4 or more digits) chosen by the user.
     * - [PASSWORD]  - An alphanumeric password chosen by the user.
     * - [BIOMETRIC] - The device's own fingerprint/face unlock, via [androidx.biometric].
     */
    enum class AppLockMethod(val key: String) {
        NONE("none"), PIN("pin"), PASSWORD("password"), BIOMETRIC("biometric");
        companion object {
            fun fromKey(key: String?): AppLockMethod = entries.firstOrNull { it.key == key } ?: NONE
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBoolean(key: Key, default: Boolean = false) = prefs.getBoolean(key.id, default)
    private fun setBoolean(key: Key, value: Boolean) = prefs.edit { putBoolean(key.id, value) }
    private fun getString(key: Key, default: String? = null) = prefs.getString(key.id, default)
    private fun setString(key: Key, value: String?) = prefs.edit { putString(key.id, value) }
    private fun getInt(key: Key, default: Int = 0) = prefs.getInt(key.id, default)
    private fun setInt(key: Key, value: Int) = prefs.edit { putInt(key.id, value) }
    private fun getStringSet(key: Key, default: Set<String> = emptySet()) = prefs.getStringSet(key.id, default)?.toSet().orEmpty()
    private fun setStringSet(key: Key, value: Set<String>) = prefs.edit { putStringSet(key.id, value) }

    fun isDisclaimerAccepted() = getBoolean(Key.DISCLAIMER_ACCEPTED, DefaultsValue.DISCLAIMER_ACCEPTED)
    fun setDisclaimerAccepted(accepted: Boolean) = setBoolean(Key.DISCLAIMER_ACCEPTED, accepted)
    /**
     * The universal master switch for call recording. Off by default. While off, nothing in
     * this module should monitor calls, listen for phone state, or run any background service —
     * see [isAnyAppCallRecordingEnabled] callers and the call-monitoring entry points, which all
     * check this first.
     */
    fun isCallRecordingEnabled() = getBoolean(Key.CALL_RECORDING_ENABLED, DefaultsValue.CALL_RECORDING_ENABLED)
    fun setCallRecordingEnabled(enabled: Boolean) {
        setBoolean(Key.CALL_RECORDING_ENABLED, enabled)
        com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard.sync(context)
    }
    fun getRecordingFolderUri(): Uri? = getString(Key.RECORDING_FOLDER_URI, DefaultsValue.RECORDING_FOLDER_URI)?.toUri()
    fun setRecordingFolderUri(uri: Uri?) = setString(Key.RECORDING_FOLDER_URI, uri?.toString())
    /**
     * Returns the user's chosen [StorageMode] for recordings, or null if they have not made a
     * choice yet (e.g. a fresh install that has not completed onboarding's storage step).
     *
     * For backward compatibility with installs from before this setting existed: if no mode was
     * explicitly chosen but a SAF recording folder URI is already saved, this returns
     * [StorageMode.SAF_FOLDER] automatically so existing users are not asked again.
     */
    fun getStorageMode(): StorageMode? {
        val stored = StorageMode.fromKey(getString(Key.STORAGE_MODE, DefaultsValue.STORAGE_MODE))
        if (stored != null) return stored
        return if (getRecordingFolderUri() != null) StorageMode.SAF_FOLDER else null
    }
    fun setStorageMode(mode: StorageMode) = setString(Key.STORAGE_MODE, mode.key)
    /** True if the user chose to keep recordings in the app's own private internal storage. */
    fun isPrivateStorageEnabled() = getStorageMode() == StorageMode.PRIVATE
    fun isVibrationEnabled() = getBoolean(Key.VIBRATION_ENABLED, DefaultsValue.VIBRATION_ENABLED)
    fun setVibrationEnabled(enabled: Boolean) = setBoolean(Key.VIBRATION_ENABLED, enabled)
    fun isAutoRecordIncomingEnabled() = getBoolean(Key.AUTO_RECORD_INCOMING, DefaultsValue.AUTO_RECORD_INCOMING)
    fun setAutoRecordIncomingEnabled(enabled: Boolean) = setBoolean(Key.AUTO_RECORD_INCOMING, enabled)
    fun isAutoRecordOutgoingEnabled() = getBoolean(Key.AUTO_RECORD_OUTGOING, DefaultsValue.AUTO_RECORD_OUTGOING)
    fun setAutoRecordOutgoingEnabled(enabled: Boolean) = setBoolean(Key.AUTO_RECORD_OUTGOING, enabled)
    fun isRecordOnAnswerEnabled() = getBoolean(Key.RECORD_ON_ANSWER, DefaultsValue.RECORD_ON_ANSWER)
    fun setRecordOnAnswerEnabled(enabled: Boolean) = setBoolean(Key.RECORD_ON_ANSWER, enabled)
    fun isIgnoreAnonymousIncomingEnabled() = getBoolean(Key.IGNORE_ANONYMOUS_INCOMING, DefaultsValue.IGNORE_ANONYMOUS_INCOMING)
    fun setIgnoreAnonymousIncomingEnabled(enabled: Boolean) = setBoolean(Key.IGNORE_ANONYMOUS_INCOMING, enabled)
    fun isIgnoreCrossCountryIncomingEnabled() = getBoolean(Key.IGNORE_CROSS_COUNTRY_INCOMING, DefaultsValue.IGNORE_CROSS_COUNTRY_INCOMING)
    fun setIgnoreCrossCountryIncomingEnabled(enabled: Boolean) = setBoolean(Key.IGNORE_CROSS_COUNTRY_INCOMING, enabled)
    fun isIgnoreCrossCountryOutgoingEnabled() = getBoolean(Key.IGNORE_CROSS_COUNTRY_OUTGOING, DefaultsValue.IGNORE_CROSS_COUNTRY_OUTGOING)
    fun setIgnoreCrossCountryOutgoingEnabled(enabled: Boolean) = setBoolean(Key.IGNORE_CROSS_COUNTRY_OUTGOING, enabled)
    fun getIgnoreContactsModeIncoming() = IgnoreContactsMode.fromKey(getString(Key.IGNORE_CONTACTS_MODE_INCOMING, DefaultsValue.IGNORE_CONTACTS_MODE_INCOMING.key))
    fun setIgnoreContactsModeIncoming(mode: IgnoreContactsMode) = setString(Key.IGNORE_CONTACTS_MODE_INCOMING, mode.key)
    fun getIgnoreContactsModeOutgoing() = IgnoreContactsMode.fromKey(getString(Key.IGNORE_CONTACTS_MODE_OUTGOING, DefaultsValue.IGNORE_CONTACTS_MODE_OUTGOING.key))
    fun setIgnoreContactsModeOutgoing(mode: IgnoreContactsMode) = setString(Key.IGNORE_CONTACTS_MODE_OUTGOING, mode.key)
    fun getIgnoredContactsIncoming() = getStringSet(Key.IGNORED_CONTACTS_INCOMING, DefaultsValue.IGNORED_CONTACTS_INCOMING)
    fun setIgnoredContactsIncoming(numbers: Set<String>) = setStringSet(Key.IGNORED_CONTACTS_INCOMING, numbers)
    fun getIgnoredContactsOutgoing() = getStringSet(Key.IGNORED_CONTACTS_OUTGOING, DefaultsValue.IGNORED_CONTACTS_OUTGOING)
    fun setIgnoredContactsOutgoing(numbers: Set<String>) = setStringSet(Key.IGNORED_CONTACTS_OUTGOING, numbers)
    fun isLoggingEnabled() = getBoolean(Key.LOGGING_ENABLED, DefaultsValue.LOGGING_ENABLED)
    fun setLoggingEnabled(enabled: Boolean) = setBoolean(Key.LOGGING_ENABLED, enabled)
    fun isDebugEnabled() = getBoolean(Key.DEBUG_ENABLED, DefaultsValue.DEBUG_ENABLED)
    fun setDebugEnabled(enabled: Boolean) = setBoolean(Key.DEBUG_ENABLED, enabled)
    fun getDebugCallerNumber() = getString(Key.DEBUG_CALLER_NUMBER, DefaultsValue.DEBUG_CALLER_NUMBER) ?: DefaultsValue.DEBUG_CALLER_NUMBER
    fun setDebugCallerNumber(number: String) = setString(Key.DEBUG_CALLER_NUMBER, number)
    fun getAudioSource() = getString(Key.AUDIO_SOURCE, DefaultsValue.AUDIO_SOURCE) ?: DefaultsValue.AUDIO_SOURCE
    fun setAudioSource(source: String) = setString(Key.AUDIO_SOURCE, source)
    fun getAudioCodec() = getString(Key.AUDIO_CODEC, DefaultsValue.AUDIO_CODEC) ?: DefaultsValue.AUDIO_CODEC
    fun setAudioCodec(codec: String) = setString(Key.AUDIO_CODEC, codec)
    fun getAudioBitRate() = getInt(Key.AUDIO_BITRATE, DefaultsValue.AUDIO_BITRATE)
    fun setAudioBitRate(bitRate: Int) = setInt(Key.AUDIO_BITRATE, bitRate)
    fun getFileNameTemplate() = getString(Key.FILE_NAME_TEMPLATE, DefaultsValue.FILE_NAME_TEMPLATE) ?: DefaultsValue.FILE_NAME_TEMPLATE
    fun setFileNameTemplate(template: String) = setString(Key.FILE_NAME_TEMPLATE, template)
    fun getThemeMode() = ThemeMode.fromKey(getString(Key.THEME_MODE, DefaultsValue.THEME_MODE.key))
    fun setThemeMode(mode: ThemeMode) = setString(Key.THEME_MODE, mode.key)
    fun isDynamicColorEnabled() = getBoolean(Key.DYNAMIC_COLOR, DefaultsValue.DYNAMIC_COLOR)
    fun setDynamicColorEnabled(enabled: Boolean) = setBoolean(Key.DYNAMIC_COLOR, enabled)
    fun isShowToastsEnabled() = getBoolean(Key.SHOW_TOASTS, DefaultsValue.SHOW_TOASTS)
    fun setShowToastsEnabled(enabled: Boolean) = setBoolean(Key.SHOW_TOASTS, enabled)
    fun isRecordingNotificationsEnabled() = getBoolean(Key.RECORDING_NOTIFICATIONS_ENABLED, DefaultsValue.RECORDING_NOTIFICATIONS_ENABLED)
    fun setRecordingNotificationsEnabled(enabled: Boolean) = setBoolean(Key.RECORDING_NOTIFICATIONS_ENABLED, enabled)
    fun isShizukuAutoManageEnabled() = getBoolean(Key.SHIZUKU_AUTO_MANAGE, DefaultsValue.SHIZUKU_AUTO_MANAGE)
    fun setShizukuAutoManageEnabled(enabled: Boolean) = setBoolean(Key.SHIZUKU_AUTO_MANAGE, enabled)
    fun isShizukuStartOnRecordEnabled() = getBoolean(Key.SHIZUKU_START_ON_RECORD, DefaultsValue.SHIZUKU_START_ON_RECORD)
    fun setShizukuStartOnRecordEnabled(enabled: Boolean) = setBoolean(Key.SHIZUKU_START_ON_RECORD, enabled)
    fun isShizukuKeepAliveEnabled() = getBoolean(Key.SHIZUKU_KEEP_ALIVE, DefaultsValue.SHIZUKU_KEEP_ALIVE)
    fun setShizukuKeepAliveEnabled(enabled: Boolean) = setBoolean(Key.SHIZUKU_KEEP_ALIVE, enabled)
    fun getShizukuAuthKey() = getString(Key.SHIZUKU_AUTH_KEY, DefaultsValue.SHIZUKU_AUTH_KEY) ?: DefaultsValue.SHIZUKU_AUTH_KEY
    fun setShizukuAuthKey(key: String) = setString(Key.SHIZUKU_AUTH_KEY, key)
    /** Returns the custom accent color as ARGB-packed Int (used when dynamic color is disabled). */
    fun getAccentColor(): Int = getInt(Key.ACCENT_COLOR, DefaultsValue.ACCENT_COLOR)
    /** Stores the custom accent color as ARGB-packed Int. */
    fun setAccentColor(argb: Int) = setInt(Key.ACCENT_COLOR, argb)

    // ── Auto-delete by time ──────────────────────────────────────────────────
    fun isAutoDeleteByTimeEnabled() = getBoolean(Key.AUTO_DELETE_BY_TIME_ENABLED, DefaultsValue.AUTO_DELETE_BY_TIME_ENABLED)
    fun setAutoDeleteByTimeEnabled(enabled: Boolean) = setBoolean(Key.AUTO_DELETE_BY_TIME_ENABLED, enabled)
    fun getAutoDeleteByTimeValue() = getInt(Key.AUTO_DELETE_BY_TIME_VALUE, DefaultsValue.AUTO_DELETE_BY_TIME_VALUE)
    fun setAutoDeleteByTimeValue(value: Int) = setInt(Key.AUTO_DELETE_BY_TIME_VALUE, value)
    fun getAutoDeleteByTimeUnit() = getString(Key.AUTO_DELETE_BY_TIME_UNIT, DefaultsValue.AUTO_DELETE_BY_TIME_UNIT) ?: DefaultsValue.AUTO_DELETE_BY_TIME_UNIT
    fun setAutoDeleteByTimeUnit(unit: String) = setString(Key.AUTO_DELETE_BY_TIME_UNIT, unit)

    // ── Auto-delete by space ─────────────────────────────────────────────────
    fun isAutoDeleteBySpaceEnabled() = getBoolean(Key.AUTO_DELETE_BY_SPACE_ENABLED, DefaultsValue.AUTO_DELETE_BY_SPACE_ENABLED)
    fun setAutoDeleteBySpaceEnabled(enabled: Boolean) = setBoolean(Key.AUTO_DELETE_BY_SPACE_ENABLED, enabled)
    fun getAutoDeleteBySpaceValue() = getInt(Key.AUTO_DELETE_BY_SPACE_VALUE, DefaultsValue.AUTO_DELETE_BY_SPACE_VALUE)
    fun setAutoDeleteBySpaceValue(value: Int) = setInt(Key.AUTO_DELETE_BY_SPACE_VALUE, value)
    fun getAutoDeleteBySpaceUnit() = getString(Key.AUTO_DELETE_BY_SPACE_UNIT, DefaultsValue.AUTO_DELETE_BY_SPACE_UNIT) ?: DefaultsValue.AUTO_DELETE_BY_SPACE_UNIT
    fun setAutoDeleteBySpaceUnit(unit: String) = setString(Key.AUTO_DELETE_BY_SPACE_UNIT, unit)
    fun isAutoUpdateCheckEnabled() = getBoolean(Key.AUTO_UPDATE_CHECK, DefaultsValue.AUTO_UPDATE_CHECK)
    fun setAutoUpdateCheckEnabled(enabled: Boolean) = setBoolean(Key.AUTO_UPDATE_CHECK, enabled)
    fun isWelcomeShown() = getBoolean(Key.WELCOME_SHOWN, DefaultsValue.WELCOME_SHOWN)
    fun setWelcomeShown(shown: Boolean) = setBoolean(Key.WELCOME_SHOWN, shown)

    // ── App Lock ──────────────────────────────────────────────────────────────
    /** Whether the App Lock screen should gate access to the app. */
    fun isAppLockEnabled() = getBoolean(Key.APP_LOCK_ENABLED, DefaultsValue.APP_LOCK_ENABLED)
    /** The currently configured unlock method (meaningless when App Lock is disabled). */
    fun getAppLockMethod(): AppLockMethod = AppLockMethod.fromKey(getString(Key.APP_LOCK_METHOD, DefaultsValue.APP_LOCK_METHOD.key))

    /**
     * Enables App Lock with the [AppLockMethod.PIN] or [AppLockMethod.PASSWORD] method, hashing
     * and storing [secret] with a freshly generated salt. The plaintext secret is never persisted.
     */
    fun setAppLockSecret(method: AppLockMethod, secret: String) {
        val salt = com.coolappstore.evercallrecorder.by.svhp.utils.AppLockCrypto.generateSalt()
        val hash = com.coolappstore.evercallrecorder.by.svhp.utils.AppLockCrypto.hash(secret, salt)
        setString(Key.APP_LOCK_SALT, salt)
        setString(Key.APP_LOCK_SECRET_HASH, hash)
        setString(Key.APP_LOCK_METHOD, method.key)
        setBoolean(Key.APP_LOCK_ENABLED, true)
    }

    /** Enables App Lock with [AppLockMethod.BIOMETRIC]. No secret needs to be stored. */
    fun setAppLockBiometric() {
        setString(Key.APP_LOCK_SALT, null)
        setString(Key.APP_LOCK_SECRET_HASH, null)
        setString(Key.APP_LOCK_METHOD, AppLockMethod.BIOMETRIC.key)
        setBoolean(Key.APP_LOCK_ENABLED, true)
    }

    /** Disables App Lock and wipes any stored PIN/password hash and salt. */
    fun clearAppLock() {
        setBoolean(Key.APP_LOCK_ENABLED, false)
        setString(Key.APP_LOCK_METHOD, AppLockMethod.NONE.key)
        setString(Key.APP_LOCK_SECRET_HASH, null)
        setString(Key.APP_LOCK_SALT, null)
    }

    /**
     * Checks [secret] (a typed-in PIN or password) against the stored hash.
     * Always returns false when App Lock isn't using [AppLockMethod.PIN] or [AppLockMethod.PASSWORD].
     */
    fun verifyAppLockSecret(secret: String): Boolean {
        val method = getAppLockMethod()
        if (method != AppLockMethod.PIN && method != AppLockMethod.PASSWORD) return false
        val hash = getString(Key.APP_LOCK_SECRET_HASH, DefaultsValue.APP_LOCK_SECRET_HASH) ?: return false
        val salt = getString(Key.APP_LOCK_SALT, DefaultsValue.APP_LOCK_SALT) ?: return false
        return com.coolappstore.evercallrecorder.by.svhp.utils.AppLockCrypto.verify(secret, salt, hash)
    }

    // ── Record calls from apps (WhatsApp / Telegram VoIP calls) ────────────────
    /**
     * Whether calls placed/received through WhatsApp should be automatically recorded,
     * the same way normal phone calls are. Off (unticked) by default.
     */
    fun isRecordWhatsAppCallsEnabled() = getBoolean(Key.RECORD_WHATSAPP_CALLS, DefaultsValue.RECORD_WHATSAPP_CALLS)
    fun setRecordWhatsAppCallsEnabled(enabled: Boolean) {
        setBoolean(Key.RECORD_WHATSAPP_CALLS, enabled)
        com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard.sync(context)
    }

    /**
     * Whether calls placed/received through Telegram should be automatically recorded,
     * the same way normal phone calls are. Off (unticked) by default.
     */
    fun isRecordTelegramCallsEnabled() = getBoolean(Key.RECORD_TELEGRAM_CALLS, DefaultsValue.RECORD_TELEGRAM_CALLS)
    fun setRecordTelegramCallsEnabled(enabled: Boolean) {
        setBoolean(Key.RECORD_TELEGRAM_CALLS, enabled)
        com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard.sync(context)
    }

    /** True if at least one "record calls from apps" target is enabled. */
    fun isAnyAppCallRecordingEnabled() = isRecordWhatsAppCallsEnabled() || isRecordTelegramCallsEnabled()

    // ── Call detection method ───────────────────────────────────────────────
    fun getCallDetectionMode(): CallDetectionMode =
        CallDetectionMode.fromKey(getString(Key.CALL_DETECTION_MODE, DefaultsValue.CALL_DETECTION_MODE.key))
    fun setCallDetectionMode(mode: CallDetectionMode) {
        setString(Key.CALL_DETECTION_MODE, mode.key)
        com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard.sync(context)
    }

    // ── Post-call file actions notification (Open/Share/Delete) ────────────
    fun isPostRecordingFileActionsNotificationEnabled() =
        getBoolean(Key.POST_RECORDING_FILE_ACTIONS_NOTIFICATION, DefaultsValue.POST_RECORDING_FILE_ACTIONS_NOTIFICATION)
    fun setPostRecordingFileActionsNotificationEnabled(enabled: Boolean) =
        setBoolean(Key.POST_RECORDING_FILE_ACTIONS_NOTIFICATION, enabled)

    // ── Custom font (shared with Ever Dialer's Settings → Interface) ───────────
    /**
     * Returns the [FontFamily] for whatever custom font the user set in Ever Dialer's own
     * Settings → Interface screen, or [FontFamily.Default] if none was set or the font file no
     * longer exists.
     */
    fun getCustomFontFamily(): FontFamily {
        val path = context.getSharedPreferences(RIVO_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_FONT_PATH, null) ?: return FontFamily.Default
        val file = File(path)
        if (!file.exists()) return FontFamily.Default
        return try {
            FontFamily(Typeface.createFromFile(file))
        } catch (_: Exception) {
            FontFamily.Default
        }
    }
}

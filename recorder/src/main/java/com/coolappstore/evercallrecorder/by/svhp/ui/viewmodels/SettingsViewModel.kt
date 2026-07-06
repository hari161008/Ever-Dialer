/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.evercallrecorder.by.svhp.BuildConfig
import com.coolappstore.evercallrecorder.by.svhp.services.call.CallSessionManager
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger

enum class DebugAction { RINGING, OFFHOOK, IDLE }

interface SettingsActions {
    fun setCallRecordingEnabled(enabled: Boolean)
    fun setAutoRecordIncoming(enabled: Boolean)
    fun setAutoRecordOutgoing(enabled: Boolean)
    fun setRecordOnAnswer(enabled: Boolean)
    fun setVibrationEnabled(enabled: Boolean)
    fun setIgnoreAnonymousIncoming(enabled: Boolean)
    fun setIgnoreCrossCountryIncoming(enabled: Boolean)
    fun setIgnoreCrossCountryOutgoing(enabled: Boolean)
    fun setIgnoreContactsModeIncoming(modeEnum: AppPreferences.IgnoreContactsMode)
    fun setIgnoreContactsModeOutgoing(modeEnum: AppPreferences.IgnoreContactsMode)
    fun setAudioSource(source: String)
    fun setAudioCodec(codec: String)
    fun setAudioBitRate(bitRate: Int)
    fun setThemeMode(mode: AppPreferences.ThemeMode)
    fun setDynamicColorEnabled(enabled: Boolean)
    fun setShowToastsEnabled(enabled: Boolean)
    fun setRecordingNotificationsEnabled(enabled: Boolean)
    fun setAppLanguage(languageCode: String)
    fun setLoggingEnabled(enabled: Boolean)
    fun setDebugEnabled(enabled: Boolean)
    fun setDebugCallerNumber(number: String)
    fun triggerDebugAction(action: DebugAction)
    fun exportLogs(uri: android.net.Uri)
    fun getAppVersion(): String
    fun setShizukuAutoManageEnabled(enabled: Boolean)
    fun setShizukuStartOnRecordEnabled(enabled: Boolean)
    fun setShizukuKeepAliveEnabled(enabled: Boolean)
    fun setShizukuAuthKey(key: String)
    fun setFileNameTemplate(template: String)
    fun setAccentColor(argb: Int)
    fun setAutoDeleteByTimeEnabled(enabled: Boolean)
    fun setAutoDeleteByTimeValue(value: Int)
    fun setAutoDeleteByTimeUnit(unit: String)
    fun setAutoDeleteBySpaceEnabled(enabled: Boolean)
    fun setAutoDeleteBySpaceValue(value: Int)
    fun setAutoDeleteBySpaceUnit(unit: String)
    fun setAutoUpdateCheckEnabled(enabled: Boolean)
    fun setAppLockPin(pin: String)
    fun setAppLockPassword(password: String)
    fun setAppLockBiometric()
    fun disableAppLock()
    fun verifyAppLockSecret(secret: String): Boolean
    fun setRecordCallsFromApp(target: com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallTarget, enabled: Boolean)
}

class SettingsViewModel(application: Application) : AndroidViewModel(application), SettingsActions {

    private val appContext = application.applicationContext
    val preferences = AppPreferences(appContext)

    private val _updateTrigger = MutableStateFlow(0)
    val updateTrigger: StateFlow<Int> = _updateTrigger.asStateFlow()

    override fun getAppVersion(): String {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            val base = "Version ${packageInfo.versionName} (${packageInfo.longVersionCode})"
            val ciBuild = BuildConfig.CI_BUILD_NUMBER
            if (ciBuild.lowercase() == "local") "$base - Local Build" else "$base - CI Run #$ciBuild"
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) { "Unknown Version" }
    }

    /** Returns only the raw version name (e.g. "1.2.3") for update comparison. */
    fun getRawVersionName(): String {
        return try {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "0"
        } catch (_: Exception) { "0" }
    }

    fun refresh() { _updateTrigger.update { it + 1 } }

    override fun setCallRecordingEnabled(enabled: Boolean) { preferences.setCallRecordingEnabled(enabled); refresh() }
    override fun setAutoRecordIncoming(enabled: Boolean) { preferences.setAutoRecordIncomingEnabled(enabled); refresh() }
    override fun setAutoRecordOutgoing(enabled: Boolean) { preferences.setAutoRecordOutgoingEnabled(enabled); refresh() }
    override fun setRecordOnAnswer(enabled: Boolean) { preferences.setRecordOnAnswerEnabled(enabled); refresh() }
    override fun setVibrationEnabled(enabled: Boolean) { preferences.setVibrationEnabled(enabled); refresh() }
    override fun setIgnoreAnonymousIncoming(enabled: Boolean) {
        preferences.setIgnoreAnonymousIncomingEnabled(enabled)
        if (!enabled) preferences.setIgnoreCrossCountryIncomingEnabled(false)
        refresh()
    }
    override fun setIgnoreCrossCountryIncoming(enabled: Boolean) { preferences.setIgnoreCrossCountryIncomingEnabled(enabled); refresh() }
    override fun setIgnoreCrossCountryOutgoing(enabled: Boolean) { preferences.setIgnoreCrossCountryOutgoingEnabled(enabled); refresh() }
    override fun setIgnoreContactsModeIncoming(modeEnum: AppPreferences.IgnoreContactsMode) { preferences.setIgnoreContactsModeIncoming(modeEnum); refresh() }
    override fun setIgnoreContactsModeOutgoing(modeEnum: AppPreferences.IgnoreContactsMode) { preferences.setIgnoreContactsModeOutgoing(modeEnum); refresh() }
    override fun setAudioSource(source: String) { preferences.setAudioSource(source); refresh() }
    override fun setAudioCodec(codec: String) {
        preferences.setAudioCodec(codec)
        ScrcpyAudioCodec.fromKey(codec).let { preferences.setAudioBitRate(it.defaultBitRate) }
        refresh()
    }
    override fun setAudioBitRate(bitRate: Int) { preferences.setAudioBitRate(bitRate); refresh() }
    override fun setFileNameTemplate(template: String) { preferences.setFileNameTemplate(template); refresh() }
    override fun setThemeMode(mode: AppPreferences.ThemeMode) { preferences.setThemeMode(mode); refresh() }
    override fun setDynamicColorEnabled(enabled: Boolean) { preferences.setDynamicColorEnabled(enabled); refresh() }
    override fun setShowToastsEnabled(enabled: Boolean) { preferences.setShowToastsEnabled(enabled); refresh() }
    override fun setRecordingNotificationsEnabled(enabled: Boolean) {
        preferences.setRecordingNotificationsEnabled(enabled)
        com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingNotificationHelper(getApplication())
            .createNotificationChannels()
        refresh()
    }
    override fun setAccentColor(argb: Int) { preferences.setAccentColor(argb); refresh() }
    override fun setAutoDeleteByTimeEnabled(enabled: Boolean)  { preferences.setAutoDeleteByTimeEnabled(enabled); refresh() }
    override fun setAutoDeleteByTimeValue(value: Int)          { preferences.setAutoDeleteByTimeValue(value); refresh() }
    override fun setAutoDeleteByTimeUnit(unit: String)         { preferences.setAutoDeleteByTimeUnit(unit); refresh() }
    override fun setAutoDeleteBySpaceEnabled(enabled: Boolean) { preferences.setAutoDeleteBySpaceEnabled(enabled); refresh() }
    override fun setAutoDeleteBySpaceValue(value: Int)         { preferences.setAutoDeleteBySpaceValue(value); refresh() }
    override fun setAutoDeleteBySpaceUnit(unit: String)        { preferences.setAutoDeleteBySpaceUnit(unit); refresh() }
    override fun setAutoUpdateCheckEnabled(enabled: Boolean)   { preferences.setAutoUpdateCheckEnabled(enabled); refresh() }
    override fun setAppLockPin(pin: String) { preferences.setAppLockSecret(AppPreferences.AppLockMethod.PIN, pin); refresh() }
    override fun setAppLockPassword(password: String) { preferences.setAppLockSecret(AppPreferences.AppLockMethod.PASSWORD, password); refresh() }
    override fun setAppLockBiometric() { preferences.setAppLockBiometric(); refresh() }
    override fun disableAppLock() { preferences.clearAppLock(); refresh() }
    override fun verifyAppLockSecret(secret: String): Boolean = preferences.verifyAppLockSecret(secret)
    override fun setRecordCallsFromApp(target: com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallTarget, enabled: Boolean) {
        when (target) {
            com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallTarget.WHATSAPP -> preferences.setRecordWhatsAppCallsEnabled(enabled)
            com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallTarget.TELEGRAM -> preferences.setRecordTelegramCallsEnabled(enabled)
        }
        refresh()
    }
    override fun setAppLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                         else LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
        refresh()
    }
    override fun setShizukuAutoManageEnabled(enabled: Boolean) { preferences.setShizukuAutoManageEnabled(enabled); refresh() }
    override fun setShizukuStartOnRecordEnabled(enabled: Boolean) { preferences.setShizukuStartOnRecordEnabled(enabled); refresh() }
    override fun setShizukuKeepAliveEnabled(enabled: Boolean) { preferences.setShizukuKeepAliveEnabled(enabled); refresh() }
    override fun setShizukuAuthKey(key: String) { preferences.setShizukuAuthKey(key); refresh() }
    override fun setLoggingEnabled(enabled: Boolean) {
        preferences.setLoggingEnabled(enabled)
        if (!enabled) AppLogger.clearLogs()
        refresh()
    }
    override fun setDebugEnabled(enabled: Boolean) { preferences.setDebugEnabled(enabled); refresh() }
    override fun setDebugCallerNumber(number: String) { preferences.setDebugCallerNumber(number) }
    override fun triggerDebugAction(action: DebugAction) {
        viewModelScope.launch {
            val actionType = when (action) {
                DebugAction.IDLE    -> CallSessionManager.ACTION_DEBUG_IDLE
                DebugAction.RINGING -> CallSessionManager.ACTION_DEBUG_RINGING
                DebugAction.OFFHOOK -> CallSessionManager.ACTION_DEBUG_OFFHOOK
            }
            CallSessionManager.getInstance(appContext).handleDebugAction(actionType)
        }
    }
    override fun exportLogs(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            AppLogger.exportReport(appContext, uri)
        }
    }
}

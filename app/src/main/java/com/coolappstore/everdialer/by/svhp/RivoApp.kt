package com.coolappstore.everdialer.by.svhp

import android.app.Application
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.feature.AppFeatures
import com.coolappstore.everdialer.by.svhp.view.screen.settings.applyIcon
import com.coolappstore.everdialer.by.svhp.view.screen.settings.buildIcons
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RivoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppFeatures.init(this)
        startKoin {
            androidContext(this@RivoApp)
            modules(appModule)
        }
        restoreSavedAppIcon()
        try {
            val appLang = PreferenceManager(this).getAppLanguage()
            com.coolappstore.everdialer.by.svhp.controller.util.AppLanguageManager.applyLocale(this, appLang, recreateActivity = false)
        } catch (_: Throwable) {}
        com.coolappstore.everdialer.by.svhp.controller.FakeCallConnectionService.ensureRegistered(this)
        try {
            val syncManager = org.koin.core.context.GlobalContext.get().get<com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager>()
            val prefs = PreferenceManager(this)
            if (prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false)) {
                com.coolappstore.everdialer.by.svhp.controller.PcSyncService.start(this)
                syncManager.start()
            }
        } catch (_: Throwable) {}
        initMissedCallBadgeObserver()
    }

    private fun initMissedCallBadgeObserver() {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                contentResolver.registerContentObserver(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    true,
                    object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(this@RivoApp)
                        }
                    }
                )
                com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(this)
            }
        } catch (_: Throwable) {}
    }

    private fun restoreSavedAppIcon() {
        try {
            val prefs = PreferenceManager(this)
            val savedKey = prefs.getString(com.coolappstore.everdialer.by.svhp.view.screen.settings.KEY_SELECTED_APP_ICON, "default") ?: "default"
            val icons = buildIcons(this)
            val entry = icons.find { it.key == savedKey } ?: icons.first()
            applyIcon(this, prefs, entry)
        } catch (_: Exception) {}
    }
}

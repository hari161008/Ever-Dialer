package com.coolappstore.everdialer.by.svhp

import com.coolappstore.evercallrecorder.by.svhp.ShizuApplication
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.screen.settings.applyIcon
import com.coolappstore.everdialer.by.svhp.view.screen.settings.buildIcons
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

// Extends ShizuApplication (Ever Call Recorder's Application class) so its own
// startup init (AppLogger, etc.) still runs now that Recorder is bundled in-app.
class RivoApp : ShizuApplication() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RivoApp)
            modules(appModule)
        }
        restoreSavedAppIcon()
        com.coolappstore.everdialer.by.svhp.controller.FakeCallConnectionService.ensureRegistered(this)
        initMissedCallBadgeObserver()
    }

    private fun initMissedCallBadgeObserver() {
        try {
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
        } catch (_: Exception) {}
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

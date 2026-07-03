package com.coolappstore.everdialer.by.svhp

import com.coolappstore.evercallrecorder.by.svhp.ShizuApplication
import com.coolappstore.everdialer.by.svhp.view.screen.settings.KEY_SELECTED_APP_ICON
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
    }

    private fun restoreSavedAppIcon() {
        try {
            val prefs = getSharedPreferences("rivo_prefs", MODE_PRIVATE)
            val savedKey = prefs.getString(KEY_SELECTED_APP_ICON, "default") ?: "default"
            val icons = buildIcons(this)
            val entry = icons.find { it.key == savedKey } ?: icons.first()
            applyIcon(this, icons, entry)
        } catch (_: Exception) {}
    }
}

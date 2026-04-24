package com.coolappstore.everdialer.by.svhp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RivoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RivoApp)
            modules(appModule)
        }
    }
}

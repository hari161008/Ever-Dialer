package com.coolappstore.evercallrecorder.by.svhp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.coolappstore.evercallrecorder.by.svhp.EXTRA_OPEN_SETTINGS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val openSettingsDirectly = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
        setContent {
            AppNavigationScreen(openSettingsDirectly = openSettingsDirectly)
        }
    }
}

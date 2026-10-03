package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import java.util.Locale

data class AppLanguage(
    val code: String,
    val title: String,
    val nativeTitle: String,
    val subtitle: String
)

object AppLanguageManager {

    val SUPPORTED_LANGUAGES = listOf(
        AppLanguage(
            code = PreferenceManager.LANGUAGE_SYSTEM,
            title = "System default",
            nativeTitle = "System default",
            subtitle = "Follow system language setting"
        ),
        AppLanguage(
            code = PreferenceManager.LANGUAGE_ENGLISH,
            title = "English",
            nativeTitle = "English",
            subtitle = "United States / International"
        ),
        AppLanguage(
            code = PreferenceManager.LANGUAGE_ARABIC,
            title = "Arabic",
            nativeTitle = "العربية",
            subtitle = "Middle East / North Africa"
        ),
        AppLanguage(
            code = PreferenceManager.LANGUAGE_HINDI,
            title = "Hindi",
            nativeTitle = "हिन्दी",
            subtitle = "India"
        ),
        AppLanguage(
            code = PreferenceManager.LANGUAGE_TAMIL,
            title = "Tamil",
            nativeTitle = "தமிழ்",
            subtitle = "India / Sri Lanka / Singapore / Malaysia"
        )
    )

    fun getLanguageByCode(code: String): AppLanguage {
        return SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()
    }

    /**
     * Determines whether the given [langCode] requires Right-to-Left (RTL) layout.
     * When set to system default, checks the active system default locale.
     */
    fun isRtl(context: Context? = null, langCode: String): Boolean {
        return when (langCode.lowercase(Locale.ROOT)) {
            PreferenceManager.LANGUAGE_ARABIC -> true
            PreferenceManager.LANGUAGE_SYSTEM -> {
                val locale = Locale.getDefault()
                TextUtilsCompat.getLayoutDirectionFromLocale(locale) == ViewCompat.LAYOUT_DIRECTION_RTL
            }
            else -> false
        }
    }

    /**
     * Wraps a base [Context] with the configuration for [langCode].
     * Useful in [Activity.attachBaseContext].
     */
    fun wrapContext(baseContext: Context, langCode: String): Context {
        if (langCode == PreferenceManager.LANGUAGE_SYSTEM || langCode.isBlank()) {
            return baseContext
        }
        return try {
            val locale = Locale.forLanguageTag(langCode)
            Locale.setDefault(locale)
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            baseContext.createConfigurationContext(config)
        } catch (_: Throwable) {
            baseContext
        }
    }

    /**
     * Applies the desired app language across both Android OS level (Android 13+ LocaleManager
     * and AppCompatDelegate) and the current Activity/Context.
     */
    fun applyLocale(context: Context, langCode: String, recreateActivity: Boolean = true) {
        try {
            if (langCode == PreferenceManager.LANGUAGE_SYSTEM) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                            LocaleList.getEmptyLocaleList()
                    } catch (_: Throwable) {}
                }
            } else {
                val locale = Locale.forLanguageTag(langCode)
                Locale.setDefault(locale)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                            LocaleList.forLanguageTags(langCode)
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        if (recreateActivity && context is Activity) {
            try {
                context.recreate()
            } catch (_: Throwable) {}
        }
    }
}

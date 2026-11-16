package com.anacatavc.oneuiextrawidgets.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    SPANISH("es"),
    ENGLISH("en")
}

object LocaleHelper {

    fun setLanguage(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLanguage(): AppLanguage {
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            currentTag.startsWith("es") -> AppLanguage.SPANISH
            currentTag.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    fun isSpanishActive(): Boolean {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val activeLocale = if (!appLocales.isEmpty) {
            appLocales[0] ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }
        return activeLocale.language.lowercase(Locale.ROOT) == "es"
    }
}

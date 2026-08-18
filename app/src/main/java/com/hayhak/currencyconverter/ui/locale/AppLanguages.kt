package com.hayhak.currencyconverter.ui.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguage(
    val tag: String,
    val nativeName: String
)

object AppLanguages {
    /** Empty tag = follow system language. */
    const val SYSTEM = ""

    val all = listOf(
        AppLanguage("en", "English"),
        AppLanguage("zh", "中文"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("es", "Español"),
        AppLanguage("fr", "Français"),
        AppLanguage("ar", "العربية"),
        AppLanguage("bn", "বাংলা"),
        AppLanguage("pt", "Português"),
        AppLanguage("ru", "Русский"),
        AppLanguage("ur", "اردو"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("ja", "日本語"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("ko", "한국어"),
        AppLanguage("it", "Italiano"),
        AppLanguage("th", "ไทย"),
        AppLanguage("pl", "Polski"),
        AppLanguage("uk", "Українська")
    )

    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) SYSTEM else locales.toLanguageTags().substringBefore(",")
    }

    fun apply(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

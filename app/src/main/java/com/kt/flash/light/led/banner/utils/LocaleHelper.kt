package com.kt.flash.light.led.banner.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun normalizeLanguageCode(code: String): String = when (code.lowercase(Locale.ROOT)) {
        "vn" -> "vi"
        "kr" -> "ko"
        "in" -> "id"
        "" -> "en"
        else -> code
    }

    fun onAttach(context: Context?, defaultLanguage: String): Context? = setLocale(context, defaultLanguage)

    fun setLocale(context: Context?, language: String): Context? {
        if (context == null) return null
        val locale = Locale.forLanguageTag(normalizeLanguageCode(language))
        Locale.setDefault(locale)
        // Copy Configuration; changing the shared instance can leave other Activities stale.
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}

package com.kt.flash.light.led.banner.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.data.model.Language


class SpManager(private val preferences: SharedPreferences) {
    companion object {
        private const val KEY_APPS_FLYER_PAID = "apps_flyer_paid_install"
        private const val KEY_CACHED_AD_CONFIG_ORGANIC = "cached_ad_config_organic"
        private const val KEY_CACHED_AD_CONFIG_PAID = "cached_ad_config_paid"


        private var instance: SpManager? = null

        fun getInstance(context: Context): SpManager {
            if (instance == null) {
                instance = SpManager(PreferenceManager.getDefaultSharedPreferences(context))
            }
            return instance!!
        }

        fun getInstance(): SpManager {
            if (instance == null) {
                instance = App.context?.let {
                    PreferenceManager.getDefaultSharedPreferences(
                        it
                    )
                }?.let { SpManager(it) }
            }
            return instance!!
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    private val attributionPreferences by lazy {
        requireNotNull(App.context).getSharedPreferences("install_attribution", Context.MODE_PRIVATE)
    }

    fun getPaidUser(): Boolean {
//        if (BuildConfig.DEBUG) return true
        return preferences.getBoolean(KEY_APPS_FLYER_PAID, false)
    }

    fun setPaidUser(isPaid: Boolean) {
        preferences.edit { putBoolean(KEY_APPS_FLYER_PAID, isPaid) }
    }

    fun getCachedAdConfigOrganic(): String? = preferences.getString(KEY_CACHED_AD_CONFIG_ORGANIC, null)

    fun setCachedAdConfigOrganic(json: String) = putString(KEY_CACHED_AD_CONFIG_ORGANIC, json)

    fun getCachedAdConfigPaid(): String? = getString(KEY_CACHED_AD_CONFIG_PAID, "")

    fun setCachedAdConfigPaid(json: String) = putString(KEY_CACHED_AD_CONFIG_PAID, json)

    fun getInt(key: String, defaultValue: Int): Int = preferences.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        preferences.edit {
            putInt(key, value)
        }
    }

    fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        preferences.edit {
            putLong(key, value)
        }
    }

    fun getString(key: String, defaultValue: String): String? =
        preferences.getString(key, defaultValue)

    fun putString(key: String, value: String) {
        preferences.edit {
            putString(key, value)
        }
    }

    fun saveLanguage(language: Language) {
        preferences.edit {
            putString(Constant.KEY_SP_CURRENT_LANGUAGE, language.toJson())
        }
    }

    fun getLanguage(): Language {
        val language = preferences.getString(Constant.KEY_SP_CURRENT_LANGUAGE, "")?.toLanguageModel()
            ?: Language("en", R.drawable.ic_english, R.string.english)
        return language.copy(languageCode = LocaleHelper.normalizeLanguageCode(language.languageCode))
    }

    fun setLanguageChosen() {
        preferences.edit { putBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, true) }
    }

    fun isLanguageChosen(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, false)
    }

    fun saveApplicationStop(isStop: Boolean) {
        preferences.edit { putBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, isStop) }
    }

    fun isApplicationStopped(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, false)
    }

}

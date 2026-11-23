package com.kt.flash.light.led.banner.utils

import android.os.Handler
import android.os.Looper
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.domain.layer.AdConfig
import com.kt.flash.light.led.banner.domain.layer.AdConfigMerger

class FirebaseConfigManager {
    companion object {
        private const val ORGANIC_CONFIG_KEY = "ad_config_organic"
        private const val FULL_ADS_CONFIG_KEY = "ad_config_user_paid"
        private const val FETCH_CACHE_SECONDS = 15 * 60L
        const val KEY_VERSION_FORCE = "version_force"
        const val KEY_IS_FORCE_UPDATE = "is_force_update"

        const val USE_MOCK_CONFIG_FOR_TESTING = false
        private var mInstance: FirebaseConfigManager = FirebaseConfigManager()

        fun instance(): FirebaseConfigManager {
            return mInstance
        }
    }

    private var isFetchDone = false
    private var fetchStarted = false
    var versionForce = "1.0.0"
    var isForceUpdate = false
    private val pendingAction: MutableList<() -> Unit> = mutableListOf()

    private fun loadCachedConfigs() {
        if(USE_MOCK_CONFIG_FOR_TESTING) return
        try {
            val prefs = SpManager.getInstance()
            val organicJson = prefs.getCachedAdConfigOrganic()
            if (!organicJson.isNullOrEmpty()) {
               val parsed = Gson().fromJson(organicJson, AdConfig::class.java)
                if (parsed != null) rawAdConfigOrganic = parsed
            }

            val paidJson = prefs.getCachedAdConfigPaid()
            if(!paidJson.isNullOrEmpty()) {
                val parsed = Gson().fromJson(paidJson, AdConfig::class.java)
                if(parsed != null) rawAdConfigPaid = parsed
            }
        } catch (e: Exception) {
            Logger.e("loadCache : ${e.message}")
        }

    }

    private var rawAdConfigOrganic = AdConfig(
        configAds = true,
        intervalBetweenInterstitial = 20000L,
        enableNativeCustom = true,
        enableBannerSplash = true,
        enableInterSplash = true,
        enableInterBack = true,
        enableNativeLanguage = true,
        enableNativeLanguageSelect = true,
        enableNativeOnB1 = true,
        enableNativeOnB12 = true,
        enableNativeOnB34 = true,
        enableNativeOnB3 = true,
        enableNativePermission = true,
        enableInterHome = true,
        enableNativeMain = true,
        enableOpenApp = true,
        enableInterMain = true,
        enableAppOpenMain = true,
        enableRewardUnlock = true,
        enableBannerMain = true,
        enableBannerAll = true
    )
    private var rawAdConfigPaid = AdConfig(
        configAds = true,
        intervalBetweenInterstitial = 15000L,
        enableNativeCustom = true,
        enableBannerSplash = true,
        enableInterSplash = true,
        enableInterBack = true,
        enableNativeLanguage = true,
        enableNativeLanguageSelect = true,
        enableNativeOnB1 = true,
        enableNativeOnB12 = true,
        enableNativeOnB34 = true,
        enableNativeOnB3 = true,
        enableNativePermission = true,
        enableInterHome = true,
        enableNativeMain = true,
        enableOpenApp = true,
        enableInterMain = true,
        enableAppOpenMain = true,
        enableRewardUnlock = true,
        enableBannerMain = true,
        enableBannerAll = true,

        enableNativeFullSplash = true,
        enableNativeFullSplash2f = true,
        enableInterSplash2 = true,
        enableNativeLanguage2f = true,
        enableNativeLanguageSelect2f = true,
        enableNativeOnB12f = true,
        enableNativeOnB32f = true
    )
    private val disabledAdConfig = AdConfig(configAds = false)

    private fun getEffectiveAdConfig(base: AdConfig, isPaid: Boolean): AdConfig {
        val interval = base.intervalBetweenInterstitial.takeIf { it > 0L }
            ?: remoteValue("interval_between_interstitial")?.coerceAtLeast(0L)
            ?: if (isPaid) 15_000L else 20_000L
        val effectiveConfig = if (base.configAds) base else AdConfigMerger.disableAllAds(base)
        return effectiveConfig.copy(intervalBetweenInterstitial = interval)
    }

    val adConfig: AdConfig
        get() {
            val isPaid = App.context?.let { SpManager.getInstance(it).getPaidUser() } == true
            val targetConfig = if (isPaid) rawAdConfigPaid else rawAdConfigOrganic
            return getEffectiveAdConfig(targetConfig, isPaid)
        }

    val intervalBetweenInterstitialMs: Long
        get() = adConfig.intervalBetweenInterstitial

    val interClick: Int
        get() = remoteSeconds("inter_click", 2L)
            .coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

    val nativeDismissableSeconds: Long
        get() = remoteSeconds("dismissable", 3L).coerceIn(0L, 3600L)
    val nativeAutoSkipSeconds: Long
        get() = remoteSeconds("auto_skip", 7L)
            .coerceIn(1L, 3600L).coerceAtLeast(nativeDismissableSeconds)

    private fun remoteSeconds(key: String, fallback: Long): Long = remoteValue(key) ?: fallback

    private fun remoteValue(key: String): Long? =
        FirebaseRemoteConfig.getInstance().all[key]?.asString()?.toLongOrNull()

    fun doAfterFetch(action: () -> Unit) {
        if (isFetchDone) {
            action()
        } else {
            pendingAction.add(action)
        }
    }

    fun fetch(onFetchComplete: (() -> Unit)? = null) {
        if (isFetchDone) {
            onFetchComplete?.invoke()
            return
        }

        onFetchComplete?.let { pendingAction.add(it) }
        if (fetchStarted) return
        fetchStarted = true
        loadCachedConfigs()
        val handler = Handler(Looper.getMainLooper())
        val remote = FirebaseRemoteConfig.getInstance()
        val fallback = Runnable { completeFetch() }

        handler.postDelayed(fallback, 10_000L)
        remote.fetch(FETCH_CACHE_SECONDS).addOnCompleteListener { task ->
            if (isFetchDone) return@addOnCompleteListener
            if (task.isSuccessful) {
                remote.activate().addOnCompleteListener {
                    handler.removeCallbacks(fallback)
                    completeFetch()
                }
            } else {
                handler.removeCallbacks(fallback)
                Logger.e("Remote Config fetch failed: ${task.exception?.message}")
                completeFetch()
            }
        }
    }

    private fun completeFetch() {
        if (isFetchDone) return
        readActivatedConfig()
        finishFetch()
    }

    private fun readActivatedConfig() {
        val remote = FirebaseRemoteConfig.getInstance()
        versionForce = remote.getString(KEY_VERSION_FORCE)
        isForceUpdate = remote.getBoolean(KEY_IS_FORCE_UPDATE)
        val prefs = SpManager.getInstance()
        val organicJson = remote.getString(ORGANIC_CONFIG_KEY)
        val paidJson = remote.getString(FULL_ADS_CONFIG_KEY)
        val organic = parseConfig(organicJson, ORGANIC_CONFIG_KEY)
        if (organic != null) {
            rawAdConfigOrganic = organic
            prefs.setCachedAdConfigOrganic(organicJson)
        }
        val paid =
            parseConfig(paidJson, FULL_ADS_CONFIG_KEY, organicJson.takeIf { organic != null })
        if (paid != null) {
            rawAdConfigPaid = paid
            prefs.setCachedAdConfigPaid(paidJson)
        }
    }

    private fun parseConfig(json: String, key: String, organicJson: String? = null): AdConfig? {
        if (json.isBlank()) return null
        return try {
            if (organicJson == null) AdConfigMerger.parse(json)
            else AdConfigMerger.merge(organicJson, json)
        } catch (error: Exception) {
            Logger.e("Invalid $key; using cached/default config: ${error.message}")
            null
        }
    }

    private fun finishFetch() {
        if (isFetchDone) return
        isFetchDone = true
        val actions = pendingAction.toList()
        pendingAction.clear()
        actions.forEach { it() }
    }
}

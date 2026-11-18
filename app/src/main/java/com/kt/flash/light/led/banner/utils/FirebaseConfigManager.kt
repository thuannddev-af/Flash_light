package com.kt.flash.light.led.banner.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.CustomSignals
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.domain.layer.AdConfig

class FirebaseConfigManager {
    companion object {
        val KEY_ADS_STATUS_ENABLE = BuildConfig.ADS_CONFIG_KEY
        const val KEY_VERSION_FORCE = "version_force"
        const val KEY_IS_FORCE_UPDATE = "is_force_update"
        private var mInstance: FirebaseConfigManager = FirebaseConfigManager()

        fun instance(): FirebaseConfigManager {
            return mInstance
        }
    }

    private var isFetchDone = false
    var versionForce = "1.0.0"
    var isForceUpdate = false
    private val pendingAction: MutableList<() -> Unit> = mutableListOf()
    var adConfig: AdConfig = readLocalAdConfig()

    private fun readLocalAdConfig(): AdConfig {
        val resource = if (BuildConfig.ADS_FLAVOR == "organic") {
            R.raw.ad_config_organic
        } else {
            R.raw.ad_config_user_paid
        }
        return try {
            val context = requireNotNull(App.context)
            val json = context.resources.openRawResource(resource).bufferedReader().use { it.readText() }
            mergeAdConfig(AdConfig(), json)
        } catch (error: Exception) {
            Logger.e("Cannot read local ${BuildConfig.ADS_CONFIG_KEY}: ${error.message}")
            AdConfig()
        }
    }

    private fun mergeAdConfig(defaults: AdConfig, json: String): AdConfig {
        val gson = Gson()
        val merged = gson.toJsonTree(defaults).asJsonObject
        val incoming = JsonParser.parseString(json).asJsonObject
        merged.entrySet().toList().forEach { (key, _) ->
            incoming.get(key)?.let { value ->
                require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) {
                    "$key must be a Boolean"
                }
                merged.addProperty(key, value.asBoolean)
            }
        }
        return gson.fromJson(merged, AdConfig::class.java)
    }

    val intervalBetweenInterstitialMs: Long
        get() = remoteSeconds("interval_between_interstitial", 30_000L).coerceAtLeast(0L)

    val interClick: Int
        get() = remoteSeconds("inter_click", 2L)
            .coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

    val nativeDismissableSeconds: Long
        get() = remoteSeconds("dismissable", 3L).coerceIn(0L, 3600L)
    val nativeAutoSkipSeconds: Long
        get() = remoteSeconds("auto_skip", 7L)
            .coerceIn(1L, 3600L).coerceAtLeast(nativeDismissableSeconds)

    private fun remoteSeconds(key: String, fallback: Long): Long {
        val value = FirebaseRemoteConfig.getInstance().all[key]?.asString()
        return value?.toLongOrNull() ?: fallback
    }

    fun doAfterFetch(action: () -> Unit) {
        if(isFetchDone) {
            action()
        } else {
            pendingAction.add(action)
        }
    }
    private var fetchStarted = false

    fun fetch(onFetchCompplete: (() -> Unit)? = null) {
        if (isFetchDone) {
            onFetchCompplete?.invoke()
            return
        }

        onFetchCompplete?.let { pendingAction.add(it) }
        if (fetchStarted) return
        fetchStarted = true
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val remote = FirebaseRemoteConfig.getInstance()
        val fallback = Runnable {
            if (!isFetchDone) {
                Logger.e("Remote Config timeout: continue with activated/default values")
                readActivatedConfig()
                finishFetch()
            }
        }

        handler.postDelayed(fallback, 10_000L)

        val signals = CustomSignals.Builder()
            .put("ads_flavor", BuildConfig.ADS_FLAVOR)
            .build()
        remote.setCustomSignals(signals).addOnCompleteListener { signalTask ->
            if (isFetchDone) return@addOnCompleteListener
            if (!signalTask.isSuccessful) {
                handler.removeCallbacks(fallback)
                Logger.e("Remote Config: failed to set ads_flavor=${BuildConfig.ADS_FLAVOR}")
                readActivatedConfig()
                finishFetch()
                return@addOnCompleteListener
            }
            Logger.e("Remote Config: ads_flavor=${BuildConfig.ADS_FLAVOR}")
            // Fetch a fresh response so installing another flavor over the same package
            // cannot reuse a response fetched with the previous flavor's signal.
            remote.fetch(0L).addOnCompleteListener fetchComplete@{ task ->
                if (isFetchDone) return@fetchComplete
                if (task.isSuccessful) {
                    remote.activate().addOnCompleteListener {
                        if (!isFetchDone) {
                            handler.removeCallbacks(fallback)
                            readActivatedConfig()
                            finishFetch()
                        }
                    }
                } else {
                    handler.removeCallbacks(fallback)
                    Logger.e("Remote Config fetch failed: continue with activated/default values")
                    readActivatedConfig()
                    finishFetch()
                }
            }
        }
    }

    private fun readActivatedConfig() {
        val remote = FirebaseRemoteConfig.getInstance()
        versionForce = remote.getString(KEY_VERSION_FORCE)
        isForceUpdate = remote.getBoolean(KEY_IS_FORCE_UPDATE)
        val json = remote.getString(KEY_ADS_STATUS_ENABLE)
        adConfig = readLocalAdConfig()
        if (json.isNotBlank()) try {
            adConfig = mergeAdConfig(adConfig, json)
        } catch (error: Exception) {
            Logger.e("Invalid $KEY_ADS_STATUS_ENABLE; using local config: ${error.message}")
        }
        Logger.e("Remote Config applied: key=$KEY_ADS_STATUS_ENABLE, ads_flavor=${BuildConfig.ADS_FLAVOR}, native_full_splash=${adConfig.enableNativeFullSplash}")
    }

    private fun finishFetch() {
        if (isFetchDone) return
        isFetchDone = true
        val actions = pendingAction.toList()
        pendingAction.clear()
        actions.forEach { it() }
    }
}

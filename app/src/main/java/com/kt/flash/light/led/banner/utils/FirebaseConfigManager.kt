package com.kt.flash.light.led.banner.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.flash.light.led.banner.domain.layer.AdConfig

class FirebaseConfigManager {
    companion object {
        const val KEY_ADS_STATUS_ENABLE = "config_ads"
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
    var adConfig: AdConfig = AdConfig()

    val intervalBetweenInterstitialMs: Long
        get() = remoteSeconds("interval_between_interstitial", 30_000L).coerceAtLeast(0L)

    val interClick: Int
        get() = remoteSeconds("inter_click", 2L)
            .coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

    // Seconds from native appearance; auto_skip includes the dismissable period.
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
        remote.fetch(15 * 60L).addOnCompleteListener { task ->
            if (isFetchDone) return@addOnCompleteListener
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

    private fun readActivatedConfig() {
        val remote = FirebaseRemoteConfig.getInstance()
        versionForce = remote.getString(KEY_VERSION_FORCE)
        isForceUpdate = remote.getBoolean(KEY_IS_FORCE_UPDATE)
        val json = remote.getString(KEY_ADS_STATUS_ENABLE)
        if (json.isBlank()) return
        try {
            Gson().fromJson(json, AdConfig::class.java)?.let { adConfig = it }
        } catch (error: Exception) {
            Logger.e("Invalid config_ads: ${error.message}")
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
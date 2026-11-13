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

    private val mMaxTryTime = 5
    private var mTrytime = 0

    private var isFetchDone = false
    var versionForce = "1.0.0"
    var isForceUpdate = false
    private val pendingAction: MutableList<() -> Unit> = mutableListOf()
    var adConfig: AdConfig = AdConfig()

    fun doAfterFetch(action: () -> Unit) {
        if(isFetchDone) {
            action()
        } else {
            pendingAction.add(action)
        }
    }
    fun fetch(onFetchCompplete: (() -> Unit)? = null) {
        val longCache = 15 * 60L
        FirebaseRemoteConfig.getInstance().fetch(longCache).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Logger.e("load config success")
                FirebaseRemoteConfig.getInstance().activate()
                versionForce = FirebaseRemoteConfig.getInstance().getString(KEY_VERSION_FORCE)
                isForceUpdate = FirebaseRemoteConfig.getInstance().getBoolean(KEY_IS_FORCE_UPDATE)
                val json = FirebaseRemoteConfig.getInstance().getString(KEY_ADS_STATUS_ENABLE)
                Logger.e("FireBaseConfigManager : $json")
                if (json.isEmpty()) {

                } else {
                    try {
                        adConfig = Gson().fromJson(json, AdConfig::class.java)

                    } catch (e: Exception) {

                        Logger.e(e.message)
                    }
                }
            } else {

                Logger.e("load config fail")
            }
        }
    }

    private fun onFetchFinished(onFetchComplete: (() -> Unit)?) {
        isFetchDone = true
        onFetchComplete?.invoke()
        pendingAction.forEach { it() }
        pendingAction.clear()
    }
    private fun tryFetchAgain(onFetchCompplete: (() -> Unit)?) {
        ++mTrytime
        if(mTrytime < mMaxTryTime) {
            fetch(onFetchCompplete)
        } else {
            mTrytime = 0
            onFetchFinished(onFetchCompplete)
        }
    }
}
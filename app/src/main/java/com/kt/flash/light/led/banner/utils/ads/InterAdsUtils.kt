package com.kt.flash.light.led.banner.utils.ads

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import arrow.core.raise.nullable
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.kt.flash.light.led.banner.utils.Logger

object InterAdsUtils {

    fun loadInterAds(
        activity: Activity, idAdsPriority: String? = null, idAds: String, adPlacement: String, isEnable: Boolean, isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null, onAdLoadFail:(() -> Unit)? = null
    ) {
        InterstitialAdsHelper.getInstance(adPlacement).setInterstitialAdConfig(
            InterstitialAdConfig(
                idAdsPriority = idAdsPriority,
                idAds = idAds,
                canShowAds = isEnable,
                canReloadAds = false,
                adPlacement = adPlacement,
                reloadIfFirstFail = true,
            )
        )
        loadInter(activity, adPlacement, isLoadAndShow, onAdLoaded, onAdLoadFail)
    }

    fun showInterAds(
        activity: Activity, lifecycleOwner: LifecycleOwner, adPlacement: String, isReloadWhenClose: Boolean = false, action: () -> Unit ) {
        InterstitialAdsHelper.getInstance(adPlacement)
            .forceShowInterstitial(activity, lifecycleOwner, object : InterstitialAdShowCallBack {
                override fun onNextAction() {

                }

                override fun onAdClose() {
                    checkReloadInter(activity, adPlacement, isReloadWhenClose)
                    action.invoke()
                }

                override fun onInterstitialShow() {
                    Logger.e("InterAds: onInterstitialShow $adPlacement")
                }

                override fun onAdClicked() {

                }

                override fun onAdImpression() {

                }

                override fun onAdFailedToShow(adError: AdError) {
                    checkReloadInter(activity, adPlacement, isReloadWhenClose)
                    action.invoke()
                }
            })
        }

    fun loadAndShowInterAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        action: () -> Unit
    ) {
        loadInterAds(activity, idAds = idAds, adPlacement = adPlacement, isEnable = isEnable, isLoadAndShow = true, onAdLoaded = {
            showInterAds(activity, lifecycleOwner, adPlacement, action = action)
        }, onAdLoadFail = {action.invoke()})
    }
    private fun loadInter(
        activity: Activity,
        adPlacement: String,
        isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null,
        onAdLoadFail: (() -> Unit)? = null
    ) {
        InterstitialAdsHelper.getInstance(adPlacement).requestInterAds(activity, isLoadAndShow, object : InterstitialAdRequestCallBack {
            override fun onAdLoaded(data : ContentAd) {
                Logger.e("$adPlacement : onAdLoaded")
                onAdLoaded?.invoke()
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("$adPlacement : onAdFailedToLoad: ${loadAdError.message}")
                onAdLoadFail?.invoke()
            }
        })
    }
    private fun checkReloadInter(context: Activity, adPlacement: String, isReloadWhenClose: Boolean) {
        if(isReloadWhenClose) {
            loadInter(context, adPlacement)
        }
    }
    fun cancelAllAds(isPurchased: Boolean) {
        InterstitialAdsHelper.getInstance(AdPlacement.INTER_MAIN).cancelRequestAndShowAllAds(isPurchased)
        InterstitialAdsHelper.getInstance(AdPlacement.INTER_SPLASH).cancelRequestAndShowAllAds(isPurchased)
    }

}
package com.kt.flash.light.led.banner.utils.ads

import android.R
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.draw.BuildDrawCacheParams
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.Logger
import java.lang.annotation.Native

object NativeAdsUtils {
    private val processLifecycleOwner = ProcessLifecycleOwner.get()
    var nativeLanguage: NativeAdHelper? = null
        private set
    var nativeOnBoarding: NativeAdHelper? = null
        private set
    var nativeFullScreen: NativeAdHelper? = null
        private set
    var nativeFullscreen2: NativeAdHelper? = null

    fun destroyAll() {
        nativeLanguage?.destroy()
        nativeOnBoarding?.destroy()
        nativeFullScreen?.destroy()
        nativeFullscreen2?.destroy()

        nativeLanguage = null
        nativeOnBoarding = null
        nativeFullScreen = null
        nativeFullscreen2 = null
    }

    fun preLoadNativeLanguage(appActivity: AppCompatActivity) {
        val isEnbale = FirebaseConfigManager.instance()
            .adConfig.enableNativeLanguage
        if(!isEnbale) return

        if(nativeLanguage != null) return //nghĩa là đã reload

        val config = NativeAdConfig(
            idAds = BuildConfig.native_full_splash,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_language",
            reloadIfFirstFail = true
        )

        nativeLanguage = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }
        nativeLanguage?.requestAds(NativeAdParam.Request)
        //LOGGGGER
    }
    fun preLoadNativeOnBoarding(appActivity: AppCompatActivity) {
        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnBoarding

        if(!isEnable) return

        if(nativeOnBoarding != null) return

        val config = NativeAdConfig (
            idAds = BuildConfig.native_full_splash, //tí thêm thành onboarding
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_onboarding",
            reloadIfFirstFail = true
        )

        nativeOnBoarding = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeOnBoarding?.requestAds(NativeAdParam.Request)
        Logger.e("preloadNativeOnboarding")
    }

    fun preLoadNativeFullScreen(appActivity: AppCompatActivity) {
        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeFullscreen

        if(!isEnable) return

        if(nativeFullScreen != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_full_splash, //tí thêm thành onboarding
            idAdsPriority = BuildConfig.native_full_splash_2,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_fullscreen",
            reloadIfFirstFail = true
        )

        nativeFullScreen = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }
        nativeFullScreen?.requestAds(NativeAdParam.Request)
        Logger.e("preloadNativeFullScreen")
    }
    fun preloadNativeFullscreen2(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeFullscreen

        if (!isEnable) return

        if (nativeFullscreen2 != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_full_splash,
            idAdsPriority = BuildConfig.native_full_splash_2,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_fullscreen_2",
            reloadIfFirstFail = true
        )

        nativeFullscreen2 = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeFullscreen2?.requestAds(NativeAdParam.Request)

        Logger.e("preloadNativeFullscreen2()")
    }

}
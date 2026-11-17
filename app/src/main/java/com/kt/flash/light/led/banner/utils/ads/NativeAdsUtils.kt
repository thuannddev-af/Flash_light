package com.kt.flash.light.led.banner.utils.ads

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.core.view.isVisible
import com.ads.admob.helper.adnative.params.AdNativeState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.domain.layer.AdConfig
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NativeAdsUtils {
    fun showNative(
        fragment: Fragment,
        placement: String,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout,
        preloadedHelper: NativeAdHelper? = null
    ): NativeAdHelper? {
        require(placement == AdPlacement.NATIVE_CUSTOM) {
            "Add native config mapping for placement: $placement"
        }
        container.isVisible = adConfig.enableNativeCustom
        if (!adConfig.enableNativeCustom) return null

        val owner = fragment.viewLifecycleOwner
        val helper = preloadedHelper ?: NativeAdHelper(
            fragment.requireActivity(), owner,
            NativeAdConfig(
                idAds = BuildConfig.native_custom,
                canShowAds = true,
                canReloadAds = false,
                layoutId = R.layout.layout_native_medium,
                adPlacement = placement,
                reloadIfFirstFail = true
            )
        ).apply { adVisibility = AdOptionVisibility.GONE }

        helper.setShimmerLayoutView(shimmer)
        helper.setNativeContentView(container)
        helper.getAdNativeState().onEach { state ->
            if (state is AdNativeState.Fail || state is AdNativeState.Cancel) {
                container.isVisible = false
            }
        }.launchIn(owner.lifecycleScope)
        if (preloadedHelper == null) {
            owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    helper.destroy()
                    owner.lifecycle.removeObserver(this)
                }
            })
            helper.requestAds(NativeAdParam.Request)
        }
        return helper
    }
    var nativeCustom: NativeAdHelper? = null
        private set
    private var nativeCustomOwner: AppCompatActivity? = null

    private val processLifecycleOwner = ProcessLifecycleOwner.get()
    var nativeLanguage: NativeAdHelper? = null
        private set
    var nativeOnBoarding: NativeAdHelper? = null
        private set
    var nativeOnBoarding3: NativeAdHelper? = null
        private set
    var nativeFullScreen: NativeAdHelper? = null
        private set
    var nativeFullscreen2: NativeAdHelper? = null

    val adConfig get() = FirebaseConfigManager.instance().adConfig

    fun destroyAll() {
        nativeCustom?.destroy()
        nativeCustom = null
        nativeCustomOwner = null
        nativeLanguage?.destroy()
        nativeOnBoarding?.destroy()
        nativeOnBoarding3?.destroy()
        nativeFullScreen?.destroy()
        nativeFullscreen2?.destroy()

        nativeLanguage = null
        nativeOnBoarding = null
        nativeOnBoarding3 = null
        nativeFullScreen = null
        nativeFullscreen2 = null
    }

    fun preLoadNativeLanguage(appActivity: AppCompatActivity) {
        val isEnbale = FirebaseConfigManager.instance()
            .adConfig.enableNativeLanguage
        if (!isEnbale) return

        if (nativeLanguage != null) return //nghĩa là đã reload

        val adConfig = FirebaseConfigManager.instance().adConfig

        val config = NativeAdConfig(
            idAdsPriority = if(adConfig.enableNativeLanguage2f) {
                BuildConfig.native_language_2f
            } else {
                null
            },
            idAds = BuildConfig.native_language,
            canShowAds = true,
            canReloadAds = false,
            layoutId = R.layout.layout_native_ad_view_type1,
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

    fun preLoadNativeLanguageSelect(appActivity: AppCompatActivity): NativeAdHelper? {
        val adConfig = FirebaseConfigManager.instance().adConfig
        if (!adConfig.enableNativeLanguageSelect) return null

        val config = NativeAdConfig(
            idAds = BuildConfig.native_language_select,
            idAdsPriority = if (adConfig.enableNativeLanguageSelect2f) {
                BuildConfig.native_language_select_2f
            } else null,
            canShowAds = true,
            canReloadAds = false,
            layoutId = R.layout.layout_native_ad_view_type1,
            adPlacement = "native_language_select",
            reloadIfFirstFail = true
        )
        return NativeAdHelper(appActivity, appActivity, config).apply {
            adVisibility = AdOptionVisibility.GONE
            requestAds(NativeAdParam.Request)
        }
    }

    fun preLoadNativeOnBoarding1(appActivity: AppCompatActivity) {
        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnB1

        if (!isEnable) return

        if (nativeOnBoarding != null) return

        val config = NativeAdConfig(
            idAdsPriority = if(adConfig.enableNativeOnB12f){
                BuildConfig.native_obd1_2f
            }else{null},
            idAds = BuildConfig.native_obd1,
            canShowAds = true,
            canReloadAds = false,
            layoutId = R.layout.layout_native_ad_view_type1,
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

    fun preLoadNativeOnBoarding3(appActivity: AppCompatActivity) {
        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnB3

        if (!isEnable) return

        if (nativeOnBoarding3 != null) return

        val config = NativeAdConfig(
            idAdsPriority = if(adConfig.enableNativeOnB3){
                BuildConfig.native_obd3_2f
            }else{null},
            idAds = BuildConfig.native_obd3,
            canShowAds = true,
            canReloadAds = false,
            layoutId = R.layout.layout_native_ad_view_type1,
            adPlacement = "native_onboarding3",
            reloadIfFirstFail = true
        )

        nativeOnBoarding3 = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeOnBoarding?.requestAds(NativeAdParam.Request)
        Logger.e("preloadNativeOnboarding3")
    }

    fun preLoadNativeFullScreenBetween12(appActivity: AppCompatActivity) {
        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnB12

        if (!isEnable) return

        if (nativeFullScreen != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_obd12_full, //tí thêm thành onboarding
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

    fun preloadNativeFullscreenBetween34(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnB34

        if (!isEnable) return

        if (nativeFullscreen2 != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_obd34_full,
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

    private fun setNative(
        activity: FragmentActivity,
        config: NativeAdConfig,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        val nativeAdHelper = NativeAdHelper(
            activity, lifecycleOwner, config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        activity.lifecycleScope.launch(Dispatchers.Main) {
            nativeAdHelper.setNativeContentView(nativeContentView)
            nativeAdHelper.setShimmerLayoutView(shimmerContainerNative)
            activity.lifecycleScope.launch(Dispatchers.IO) {
                nativeAdHelper.requestAds(NativeAdParam.Request)
            }
        }
    }

    fun loadAndShowNative(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        val config = NativeAdConfig(
            BuildConfig.native_permission,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableNativePermission,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_permission"
        )
        setNative(activity, config, lifecycleOwner, nativeContentView, shimmerContainerNative)
    }

    fun preLoadNativeCustom(appActivity: AppCompatActivity) {
        if (nativeCustomOwner === appActivity && nativeCustom != null) return
        nativeCustom?.destroy()
        nativeCustom = null
        nativeCustomOwner = null
        if (!adConfig.enableNativeCustom) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_custom,
            canShowAds = true,
            canReloadAds = false,
            layoutId = R.layout.layout_native_medium,
            adPlacement = "native_custom",
            reloadIfFirstFail = true
        )
        nativeCustomOwner = appActivity
        nativeCustom = NativeAdHelper(appActivity, appActivity, config).apply {
            adVisibility = AdOptionVisibility.GONE
            requestAds(NativeAdParam.Request)
        }
    }

    fun destroyNativeCustom(appActivity: AppCompatActivity) {
        if (nativeCustomOwner !== appActivity) return
        nativeCustom?.destroy()
        nativeCustom = null
        nativeCustomOwner = null
    }

}

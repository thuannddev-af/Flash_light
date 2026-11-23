package com.kt.flash.light.led.banner.utils.ads

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.facebook.shimmer.ShimmerFrameLayout
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.SpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object NativeAdsUtils {

    var nativeCustom: NativeAdHelper? = null
        private set
    private var nativeCustomOwner: AppCompatActivity? = null
    private val availableCustom = java.util.ArrayDeque<NativeAdHelper>()
    private val allCustom = linkedSetOf<NativeAdHelper>()

    private data class NativeBinding(
        val helper: NativeAdHelper,
        val placement: String,
        var visible: Boolean = true
    )

    private data class NativeAllBinding(
        val helper: NativeAdHelper,
        val owner: LifecycleOwner,
        val adUnitId: String,
        val placement: String,
        val layoutId: Int,
        var visible: Boolean = true
    )

    private fun clearNativeCustom() {
        allCustom.toList().forEach { it.destroy() }
        allCustom.clear()
        availableCustom.clear()
        nativeCustom = null
        nativeCustomOwner = null
    }

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

    private val nativeCustomLayout: Int
        get() = if (SpManager.getInstance().getPaidUser() == true) {
            R.layout.layout_native_medium_full_ads
        } else {
            R.layout.layout_native_medium
        }

    @androidx.annotation.LayoutRes
    private fun nativeType1Layout(
        @androidx.annotation.LayoutRes fullAdsLayout: Int = R.layout.layout_native_ad_view_type1
    ): Int = if (SpManager.getInstance().getPaidUser() == true) {
        fullAdsLayout
    } else {
        R.layout.layout_native_ad_view_type1_reload
    }

    fun destroyAll() {
        clearNativeCustom()
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
            idAdsPriority = if (adConfig.enableNativeLanguage2f) {
                BuildConfig.native_language_2f
            } else {
                null
            },
            idAds = BuildConfig.native_language,
            canShowAds = true,
            canReloadAds = false,
            layoutId = nativeType1Layout(),
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
            layoutId = nativeType1Layout(R.layout.layout_native_ad_view_type_select_lang_2f),
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
            idAdsPriority = if (adConfig.enableNativeOnB12f) {
                BuildConfig.native_obd1_2f
            } else {
                null
            },
            idAds = BuildConfig.native_obd1,
            canShowAds = true,
            canReloadAds = false,
            layoutId = nativeType1Layout(),
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
            idAdsPriority = if (adConfig.enableNativeOnB32f) {
                BuildConfig.native_obd3_2f
            } else {
                null
            },
            idAds = BuildConfig.native_obd3,
            canShowAds = true,
            canReloadAds = false,
            layoutId = nativeType1Layout(),
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

        nativeOnBoarding3?.requestAds(NativeAdParam.Request)
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
            layoutId = nativeType1Layout(com.ads.admob.R.layout.layout_native_ad_view_type1),
            adPlacement = "native_permission"
        )
        setNative(activity, config, lifecycleOwner, nativeContentView, shimmerContainerNative)
    }

//    fun loadAndShowNativeAll(
//        fragment: Fragment,
//        adUnitId: String,
//        placement: String,
//        container: FrameLayout,
//        shimmer: ShimmerFrameLayout,
//        canShowAds: Boolean,
//        @androidx.annotation.LayoutRes layoutId: Int = R.layout.layout_native_medium
//    ): NativeAdHelper? = loadAndShowNativeAll(
//        fragment.requireActivity(), fragment.viewLifecycleOwner,
//        adUnitId, placement, container, shimmer, canShowAds, layoutId
//    )

    fun loadAndShowNativeAll(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner = activity,
        adUnitId: String,
        placement: String,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout,
        canShowAds: Boolean,
        @androidx.annotation.LayoutRes layoutId: Int = R.layout.layout_native_medium
    ): NativeAdHelper? {
        check(container.getTag(R.id.native_ads_binding) == null) {
            "Use only one native binding API for each container"
        }
        val resolvedLayoutId = if (
            placement == AdPlacement.NATIVE_CUSTOM && layoutId == R.layout.layout_native_medium
        ) nativeCustomLayout else layoutId
        val existing = container.getTag(R.id.native_ads_all_binding) as? NativeAllBinding
        if (!canShowAds || lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
            container.isVisible = false
            return null
        }
        if (existing != null) {
            require(
                existing.owner === lifecycleOwner && existing.adUnitId == adUnitId &&
                        existing.placement == placement && existing.layoutId == resolvedLayoutId
            ) {
                "This container is already bound to another native configuration"
            }
            container.isVisible = existing.visible
            return existing.helper
        }
        val config = NativeAdConfig(
            idAds = adUnitId,
            canShowAds = canShowAds,
            canReloadAds = false,
            layoutId = resolvedLayoutId,
            adPlacement = placement,
            reloadIfFirstFail = true
        )
        val helper = NativeAdHelper(activity, lifecycleOwner, config).apply {
            adVisibility = AdOptionVisibility.GONE
            setShimmerLayoutView(shimmer)
            setNativeContentView(container)
        }
        val binding =
            NativeAllBinding(helper, lifecycleOwner, adUnitId, placement, resolvedLayoutId)
        container.setTag(R.id.native_ads_all_binding, binding)
        container.isVisible = true
        helper.getAdNativeState().onEach { state ->
            when (state) {
                is AdNativeState.Fail, is AdNativeState.Cancel -> binding.visible = false
                is AdNativeState.Loading, is AdNativeState.Loaded -> binding.visible = true
                else -> Unit
            }
            container.isVisible = canShowAds && binding.visible
        }.launchIn(lifecycleOwner.lifecycleScope)
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (container.getTag(R.id.native_ads_all_binding) === binding) {
                    container.setTag(R.id.native_ads_all_binding, null)
                }
                helper.destroy()
                owner.lifecycle.removeObserver(this)
            }
        })
        helper.requestAds(NativeAdParam.Request)
        return helper
    }

    private fun createNativeCustom(
        activity: AppCompatActivity,
        owner: LifecycleOwner
    ): NativeAdHelper {
        val config = NativeAdConfig(
            idAds = BuildConfig.native_custom,
            canShowAds = true,
            canReloadAds = false,
            layoutId = nativeCustomLayout,
            adPlacement = AdPlacement.NATIVE_CUSTOM,
            reloadIfFirstFail = true
        )
        return NativeAdHelper(activity, owner, config).apply {
            adVisibility = AdOptionVisibility.GONE
        }
    }

    fun preLoadNativeCustom(appActivity: AppCompatActivity, count: Int = 3) {
        if (nativeCustomOwner === appActivity) return
        clearNativeCustom()
        if (!adConfig.enableNativeCustom) return
        nativeCustomOwner = appActivity
        repeat(count.coerceIn(1, 3)) {
            val helper = createNativeCustom(appActivity, appActivity)
            availableCustom.addLast(helper)
            allCustom.add(helper)
            helper.requestAds(NativeAdParam.Request)
        }
        nativeCustom = availableCustom.peekFirst()
    }

    fun showNativeCustom(
        fragment: Fragment,
        placement: String,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout
    ): NativeAdHelper? {
        require(placement == AdPlacement.NATIVE_CUSTOM) {
            "Add native config mapping for placement: $placement"
        }
        container.isVisible = adConfig.enableNativeCustom
        if (!adConfig.enableNativeCustom) return null

        val existing = container.getTag(R.id.native_ads_binding) as? NativeBinding
        if (existing != null) {
            require(existing.placement == placement)
            container.isVisible = existing.visible
            return existing.helper
        }
        val activity = fragment.requireActivity() as AppCompatActivity
        val owner = fragment.viewLifecycleOwner
        val preloaded = if (nativeCustomOwner === activity) availableCustom.pollFirst() else null
        nativeCustom = availableCustom.peekFirst()
        val helper = preloaded ?: createNativeCustom(activity, owner)
        if (nativeCustomOwner === activity) allCustom.add(helper)
        val binding = NativeBinding(helper, placement)
        container.setTag(R.id.native_ads_binding, binding)

        helper.updateActivity(activity, owner)
        helper.setShimmerLayoutView(shimmer)
        helper.setNativeContentView(container)
        helper.getAdNativeState().onEach { state ->
            when (state) {
                is AdNativeState.Fail, is AdNativeState.Cancel -> binding.visible = false
                is AdNativeState.Loaded, is AdNativeState.Loading -> binding.visible = true
                else -> Unit
            }
            container.isVisible = binding.visible
        }.launchIn(owner.lifecycleScope)
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (container.getTag(R.id.native_ads_binding) === binding) {
                    container.setTag(R.id.native_ads_binding, null)
                }
                allCustom.remove(helper)
                helper.destroy()
                owner.lifecycle.removeObserver(this)
            }
        })
        if (preloaded == null) helper.requestAds(NativeAdParam.Request)
        return helper
    }

    fun destroyNativeCustom(appActivity: AppCompatActivity) {
        if (nativeCustomOwner === appActivity) clearNativeCustom()
    }
}

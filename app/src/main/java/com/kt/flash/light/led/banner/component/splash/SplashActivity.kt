package com.kt.flash.light.led.banner.component.splash

import android.os.Bundle
import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.interstitial.InterstitialAdSplashConfig
import com.ads.admob.helper.interstitial.InterstitialAdSplashHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.component.language.LanguageActivity
import com.kt.flash.light.led.banner.databinding.ActivitySplashBinding
import com.kt.flash.light.led.banner.component.onboarding.OnboardingActivity
import com.kt.flash.light.led.banner.domain.layer.FullscreenAdConfig
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.Utils
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.BannerAdsUtils
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    private var progressAnimator: ValueAnimator? = null
    private var progress = 0
    private var navigating = false
    private var splashAdsStarted = false

    private var interClosed = false
    private var nativeFullShown = false


    @Inject
    lateinit var spManager: SpManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateProgress(savedInstanceState?.getInt(KEY_PROGRESS) ?: 0)
        showBanner()
    }

    override fun onConnectivityChanged(connected: Boolean) {
        if (connected) {
            if (!splashAdsStarted && !navigating) {
                splashAdsStarted = true
                App.instance?.initConsentManager(this) {
                    runOnUiThread {
                        if (!navigating && !isFinishing && !isDestroyed) {
                            initNativeFullSplash()
                            loadInterSplash()
                        }
                    }
                }
            }
            startProgressBar()
        } else {
            progressAnimator?.cancel()
            progressAnimator = null
        }
    }

    // Visual progress only. The interstitial helper decides when Splash finishes.
    private fun startProgressBar() {
        if (progressAnimator?.isStarted == true || navigating || progress >= 99) return
        progressAnimator = ValueAnimator.ofInt(progress, 99).apply {
            duration = PROGRESS_DURATION_MS * (99 - progress) / 99
            interpolator = LinearInterpolator()
            addUpdateListener { updateProgress(it.animatedValue as Int) }
            start()
        }
    }

    private fun updateProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        viewBaseActivity.progressBar.progress = progress
        viewBaseActivity.progressPercent.text = getString(R.string.splash_progress_percent, progress)
    }

    private fun gotoMainScreen() {
        if (navigating || isFinishing || isDestroyed) return
        if (!Utils.isConnected(this)) return
        navigating = true
        progressAnimator?.cancel()
        progressAnimator = null
        updateProgress(100)
        if(spManager.isLanguageChosen()) {
            OnboardingActivity.start(this)
        }else {
            LanguageActivity.start(this, isFromSplash = true)
        }
        finish()
    }
    override fun onPause() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_PROGRESS, progress)
        super.onSaveInstanceState(outState)
    }

    private fun showBanner() {
        BannerAdsUtils.initBannerSplash(this, this, viewBaseActivity.frAdsBanner)
    }

    private fun loadInterSplash() {
        interAdSplashHelper.holdShow = false
        interAdSplashHelper.requestAds(InterstitialAdParam.Request)
    }

    private val interAdSplashHelper by lazy {initInterAdSplash()}
    private fun initInterAdSplash(): InterstitialAdSplashHelper {
        val config = InterstitialAdSplashConfig(
            idAds = BuildConfig.inter_splash,
            idAdsPriority = BuildConfig.inter_splash_2f,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableInterSplash,
            canReloadAds = false,
            timeDelay = 2000L,
            timeOut = 25000L,
            showReady = true,
            adPlacement = AdPlacement.INTER_SPLASH,
            reloadIfFirstFail = true
        )
        return InterstitialAdSplashHelper(
            activity = this,
            lifecycleOwner = this,
            config = config
        ).apply {
            registerAdListener(interAdCallBack)
        }
    }

    private val interAdCallBack = object : InterstitialAdCallback {
        override fun onNextAction() {
            Log.d("SPLASH_TEST", "onNextAction -> Chuyển màn")
            if(interClosed) return
            gotoMainScreen()
        }

        override fun onAdClose() {
            Log.d("SPLASH_TEST", "onAdClose -> Hiển thị native fullscreen")
            if(interClosed) return
            interClosed = true
            showNativeFullSplash()
        }

        override fun onInterstitialShow() {
        }

        override fun onAdLoaded(data: ContentAd) {
            Log.d("SPLASH_TEST", "Inter load thành công")

        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            Log.e("SPLASH_TEST", "Inter load thất bại: ${loadAdError.message}")

        }

        override fun onAdClicked() {
        }

        override fun onAdImpression() {
        }

        override fun onAdFailedToShow(adError: AdError) {

        }
    }

    private fun loadNativeSplash() {
        val isEnable = FirebaseConfigManager.instance().adConfig.enableNativeSplash
        if(!isEnable) {
            interAdSplashHelper.releaseHoldShow()
            return
        }

        val config = NativeAdConfig(
            idAds = BuildConfig.native_full_splash,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_splash",
            reloadIfFirstFail = false
        )

        val nativeHelper = NativeAdHelper(
            activity = this,
            lifecycleOwner = this,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
            setNativeContentView(viewBaseActivity.frAdsBanner)
        }
    }

    private fun initNativeFullSplash() {
//        val config = listOf(
//            FullscreenAdConfig(
//                tag = "fs1",
//                afterNonFullscreenCount = 1,
//                adHelper = NativeAdsUtils.nativeFullScreen
//            ),
//            FullscreenAdConfig(
//                tag = "fs2",
//                afterNonFullscreenCount = 2,
//                adHelper = NativeAdsUtils.nativeFullscreen2
//            )
//        )
//
//        config.forEach { config ->
//            config.adHelper?.let { adHelper ->
//                adHelper.getAdNativeState()
//                    .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
//                    .onEach { state ->
//                        when {
//                            state == AdNativeState.Fail -> {
//                                Logger.e("AdNativeState fail")
//                            }
//                            state == adHelper.nativeAd?.let { AdNativeState.Loaded(it) } -> {
//                                adHelper.updateActivity(this@SplashActivity, this@SplashActivity)
//                            }
//                        }
//                    }
//                    .launchIn(lifecycleScope)
//            }
//        }
        NativeAdsUtils.preloadNativeFullscreen2(this)
        viewBaseActivity.closeNativeFull.setOnClickListener {
            // Keep the overlay visible until the next Activity covers Splash.
            gotoMainScreen()
        }
    }
    private fun showNativeFullSplash() {
        if(nativeFullShown || navigating || isFinishing || isDestroyed) return
        val helper = NativeAdsUtils.nativeFullscreen2
        val nativeAd = helper?.nativeAd

        if(helper == null || nativeAd == null) {
            Log.w("SPLASH_TEST", "Native fullscreen chưa sẵn sàng hoặc đang bị tắt -> chuyển màn")
            gotoMainScreen()
            return
        }
        nativeFullShown = true
        helper.updateActivity(this, this)
        viewBaseActivity.frAdsNative.visibility = View.VISIBLE

        helper.setShimmerLayoutView(
            viewBaseActivity.shimmerContainerNative.root
        )
        helper.setNativeContentView(
            viewBaseActivity.nativeFullContent
        )
    }
    private fun loadLanguageNativeAd() {
        NativeAdsUtils.preLoadNativeLanguage(this)
    }
     private companion object {
        const val PROGRESS_DURATION_MS = 7000L
        const val KEY_PROGRESS = "splash_progress"
    }
}

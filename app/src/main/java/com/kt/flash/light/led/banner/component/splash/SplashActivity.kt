package com.kt.flash.light.led.banner.component.splash

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.interstitial.InterstitialAdSplashConfig
import com.ads.admob.helper.interstitial.InterstitialAdSplashHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.language.LanguageActivity
import com.kt.flash.light.led.banner.component.onboarding.OnboardingActivity
import com.kt.flash.light.led.banner.databinding.ActivitySplashBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.Utils
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.BannerAdsUtils
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    private var progressAnimator: ValueAnimator? = null
    private var progress = 0
    private var navigating = false
    private var pendingNavigation = false
    private var splashAdsStarted = false
    private var interBackdrop: View? = null

    private var interClosed = false
    private var nativeFullShown = false
    private var nativeFullSplashHelper: NativeAdHelper? = null
    private var nativeCountdownJob: Job? = null
    private var interWaitJob: Job? = null
    private var interStageFinished = false


    @Inject
    lateinit var spManager: SpManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNavigation = savedInstanceState?.getBoolean(KEY_PENDING_NAVIGATION) ?: false
        updateProgress(savedInstanceState?.getInt(KEY_PROGRESS) ?: 0)
        showBanner()
    }

    override fun onConnectivityChanged(connected: Boolean) {
        if (connected) {
            if (pendingNavigation) {
                gotoMainScreen()
                return
            }
            if (!splashAdsStarted && !navigating) {
                splashAdsStarted = true
                Log.d("SPLASH_TEST", "Waiting for consent")
                App.instance?.initConsentManager(this) {
                    runOnUiThread {
                        Log.d("SPLASH_TEST", "Consent returned; waiting for Remote Config")
                        FirebaseConfigManager.instance().doAfterFetch {
                            Log.d("SPLASH_TEST", "Remote Config ready; starting Splash ads")
                            if (!navigating && !isFinishing && !isDestroyed) {
                                initNativeFullSplash()
                                loadInterSplash()
                                loadLanguageNativeAd()
                                loadNativeOnboarding(this)
                            }
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
        viewBaseActivity.progressPercent.text =
            getString(R.string.splash_progress_percent, progress)
    }

    private fun gotoMainScreen() {
        if (navigating || isFinishing || isDestroyed) return
        pendingNavigation = true
        if (!Utils.isConnected(this) ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            Log.d("SPLASH_TEST", "Navigation pending: waiting for network and resumed Splash")
            return
        }
        pendingNavigation = false
        navigating = true
        interWaitJob?.cancel()
        nativeCountdownJob?.cancel()
        progressAnimator?.cancel()
        progressAnimator = null
        updateProgress(100)
        if (spManager.isLanguageChosen()) {
            OnboardingActivity.start(this)
        } else {
            LanguageActivity.start(this, isFromSplash = true)
        }
        finish()
    }

    override fun onPause() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onPause()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (pendingNavigation) gotoMainScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_PROGRESS, progress)
        outState.putBoolean(KEY_PENDING_NAVIGATION, pendingNavigation)
        super.onSaveInstanceState(outState)
    }

    private fun showBanner() {
        BannerAdsUtils.initBannerSplash(this, this, viewBaseActivity.frAdsBanner)
    }

    private fun loadInterSplash() {
        if (interStageFinished || navigating) return
        Log.d("SPLASH_TEST", "Inter request started")
        interWaitJob?.cancel()
        interWaitJob = lifecycleScope.launch {
            delay(25_000L)
            if (!interStageFinished && !navigating) {
                Log.w("SPLASH_TEST", "Inter callback timeout -> continue Splash")
                interStageFinished = true
                interAdSplashHelper.cancel()
                hideInterBackdrop()
                gotoMainScreen()
            }
        }
        //window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        interAdSplashHelper.holdShow = false
        interAdSplashHelper.requestAds(InterstitialAdParam.Request)
    }

    private val interAdSplashHelper by lazy { initInterAdSplash() }
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
            if (interStageFinished) return
            interStageFinished = true
            interWaitJob?.cancel()
            hideInterBackdrop()
            Log.d("SPLASH_TEST", "onNextAction -> Chuyển màn")
            if (interClosed) return
            gotoMainScreen()
        }

        override fun onAdClose() {
            interStageFinished = true
            interWaitJob?.cancel()
            hideInterBackdrop()
            Log.d("SPLASH_TEST", "onAdClose -> Hiển thị native fullscreen")
            if (interClosed) return
            interClosed = true
            showNativeFullSplash()
        }

        override fun onInterstitialShow() {
            interWaitJob?.cancel()
            Log.d("SPLASH_TEST", "Inter shown; stop waiting timeout")
            showInterBackdrop()
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
            hideInterBackdrop()
            if (!interStageFinished) {
                interStageFinished = true
                interWaitJob?.cancel()
                gotoMainScreen()
            }

        }
    }

    private fun showInterBackdrop() {
        if (interBackdrop != null || isFinishing || isDestroyed) return
        val decor = window.decorView as? android.view.ViewGroup ?: return
        interBackdrop = View(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            elevation = 100f * resources.displayMetrics.density
            decor.addView(
                this, android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            bringToFront()
        }
    }

    private fun loadNativeOnboarding(appActivity: AppCompatActivity) {
            NativeAdsUtils.preLoadNativeFullScreenBetween12(this)
            //NativeAdsUtils.preLoadNativeOnBoarding1(this)
    }

    private fun hideInterBackdrop() {
        interBackdrop?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        interBackdrop = null
    }

    override fun onDestroy() {
        interWaitJob?.cancel()
        hideInterBackdrop()
        super.onDestroy()
    }

    private fun initNativeFullSplash() {
        viewBaseActivity.closeNativeFull.setOnClickListener {
            if (!viewBaseActivity.closeNativeFull.isEnabled) return@setOnClickListener
            gotoMainScreen()
        }
        if (nativeFullSplashHelper != null) return
        val adConfig = FirebaseConfigManager.instance().adConfig
        if (!adConfig.enableNativeFullSplash) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_full_splash,
            idAdsPriority = if (adConfig.enableNativeFullSplash2f) {
                BuildConfig.native_full_splash_2f
            } else {
                null
            },
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_full_splash",
            reloadIfFirstFail = true
        )
        nativeFullSplashHelper = NativeAdHelper(this, this, config).apply {
            adVisibility = AdOptionVisibility.GONE
            requestAds(com.ads.admob.helper.adnative.params.NativeAdParam.Request)
        }
    }

    private fun showNativeFullSplash() {
        if (nativeFullShown || navigating || isFinishing || isDestroyed) return
        val helper = nativeFullSplashHelper
        val nativeAd = helper?.nativeAd

        if (helper == null || nativeAd == null) {
            gotoMainScreen()
            return
        }
        nativeFullShown = true
        viewBaseActivity.closeNativeFull.isEnabled = false
        viewBaseActivity.closeNativeFull.visibility = View.GONE
        helper.updateActivity(this, this)
        viewBaseActivity.frAdsNative.visibility = View.VISIBLE

        helper.setShimmerLayoutView(
            viewBaseActivity.shimmerContainerNative.root
        )
        helper.setNativeContentView(
            viewBaseActivity.nativeFullContent
        )
        startNativeCountdown()
    }

    private fun startNativeCountdown() {
        if (nativeCountdownJob != null) return
        val config = FirebaseConfigManager.instance()
        val dismissable = config.nativeDismissableSeconds
        val autoSkip = config.nativeAutoSkipSeconds
        var elapsed = 0L
        nativeCountdownJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (!navigating) {
                    val canDismiss = elapsed >= dismissable
                    viewBaseActivity.closeNativeFull.isEnabled = canDismiss
                    viewBaseActivity.closeNativeFull.visibility =
                        if (canDismiss) View.VISIBLE else View.GONE
                    if (canDismiss) viewBaseActivity.closeNativeFull.bringToFront()
                    viewBaseActivity.nativeFullCountdown.visibility =
                        if (canDismiss) View.GONE else View.VISIBLE
                    viewBaseActivity.nativeFullCountdown.text =
                        (dismissable - elapsed).coerceAtLeast(0).toString()
                    if (elapsed >= autoSkip) {
                        gotoMainScreen()
                        return@repeatOnLifecycle
                    }
                    delay(1000L)
                    elapsed++
                }
            }
        }
    }

    private fun loadLanguageNativeAd() {
        NativeAdsUtils.preLoadNativeLanguage(this)
    }

    private companion object {
        const val PROGRESS_DURATION_MS = 7000L
        const val KEY_PROGRESS = "splash_progress"
        const val KEY_PENDING_NAVIGATION = "splash_pending_navigation"
    }
}

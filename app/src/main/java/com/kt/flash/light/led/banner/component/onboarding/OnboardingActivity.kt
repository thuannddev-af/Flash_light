package com.kt.flash.light.led.banner.component.onboarding

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.ads.admob.helper.adnative.params.AdNativeState
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.component.permission.PermissionActivity
import com.kt.flash.light.led.banner.databinding.ActivityOnboardingBinding
import com.kt.flash.light.led.banner.domain.layer.FullscreenAdConfig
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    private val onBoardingAdapter = OnBoardingAdapter()
    private val viewModel: OnBoardingViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

    private var nativeCountdownJob: Job? = null
    private var navigating = false

    private val pageCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.currentPosition = position
            initMultiNAtiveAd(position)
        }
    }

    override fun provideViewBinding() = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun initViews() = with(viewBaseActivity) {
        //setFullscreen()
        vpOnBoarding.adapter = onBoardingAdapter
        tabMediator = TabLayoutMediator(tabDots, vpOnBoarding) { _, _ -> }.also { it.attach() }
        vpOnBoarding.registerOnPageChangeCallback(pageCallback)
        btnNext.isEnabled = false
        btnNext.setOnClickListener { nextAction() }
        btnClose.visibility = android.view.View.GONE
        frAdsNative.visibility = android.view.View.VISIBLE
        btnClose.setOnClickListener { nextAction() }
        NativeAdsUtils.preLoadNativeOnBoarding1(this@OnboardingActivity)
        NativeAdsUtils.preLoadNativeOnBoarding3(this@OnboardingActivity)
        NativeAdsUtils.preLoadNativeFullScreenBetween12(this@OnboardingActivity)
        NativeAdsUtils.preloadNativeFullscreenBetween34(this@OnboardingActivity)
        initNativeOnboarding()
    }

    override fun initData() {
        super.initData()
        viewModel.getListOnBoarding()
    }

    override fun initObserver() {
        viewModel.listOnBoarding.onEach { pages ->
            onBoardingAdapter.setData(ArrayList(pages))
            viewBaseActivity.btnNext.isEnabled = pages.isNotEmpty()
            if (pages.isNotEmpty()) {
                val position = viewModel.currentPosition.coerceIn(pages.indices)
                viewBaseActivity.vpOnBoarding.setCurrentItem(position, false)
                initNativeFullScreen()
                initMultiNAtiveAd(position)
            }
        }.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).launchIn(lifecycleScope)
    }

    private fun nextAction() {
        if (navigating || onBoardingAdapter.itemCount == 0) return
        nativeCountdownJob?.cancel()
        val currentPage = viewBaseActivity.vpOnBoarding.currentItem
        if (currentPage < onBoardingAdapter.itemCount - 1) {
            viewBaseActivity.vpOnBoarding.setCurrentItem(currentPage + 1, true)
        } else {
            navigating = true
            PermissionActivity.start(this)
            finish()
        }
    }

    override fun onDestroy() {
        nativeCountdownJob?.cancel()
        viewBaseActivity.vpOnBoarding.unregisterOnPageChangeCallback(pageCallback)
        tabMediator?.detach()
        tabMediator = null
        viewBaseActivity.vpOnBoarding.adapter = null
        super.onDestroy()
    }

    private fun initNativeOnboarding() {
        NativeAdsUtils.nativeOnBoarding?.apply {
            updateActivity(this@OnboardingActivity, this@OnboardingActivity)
            setNativeContentView(viewBaseActivity.frAdsNative)
            setShimmerLayoutView(viewBaseActivity.shimmerContainerNative.shimmerContainerNative)
        }
    }

    private fun startNativeCountdown(tag: String) {
        nativeCountdownJob?.cancel()
        val config = FirebaseConfigManager.instance()
        val dismissable = config.nativeDismissableSeconds
        val autoSkip = config.nativeAutoSkipSeconds
        var elapsed = 0L
        nativeCountdownJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (!navigating) {
                    val current = onBoardingAdapter.dataSet.getOrNull(
                        viewBaseActivity.vpOnBoarding.currentItem
                    )
                    if (current?.imageId != 0 || current.tag != tag) return@repeatOnLifecycle
                    viewBaseActivity.btnClose.isVisible = elapsed >= dismissable
                    if (elapsed >= dismissable) viewBaseActivity.btnClose.bringToFront()
                    if (elapsed >= autoSkip) {
                        nextAction()
                        return@repeatOnLifecycle
                    }
                    delay(1000L)
                    elapsed++
                }
            }
        }
    }

    private fun initMultiNAtiveAd(position: Int) {
        nativeCountdownJob?.cancel()
        val page = onBoardingAdapter.dataSet.getOrNull(position) ?: return
        val fullscreen = page.imageId == 0
        val contentIndex = onBoardingAdapter.dataSet.take(position + 1).count { it.imageId != 0 }
        viewBaseActivity.btnNext.isVisible = !fullscreen
        viewBaseActivity.tabDots.isVisible = !fullscreen
        viewBaseActivity.btnClose.isVisible = false

        val helper = if(fullscreen) {
            null
        } else{
            when(contentIndex) {
                1 -> NativeAdsUtils.nativeOnBoarding
                3 -> NativeAdsUtils.nativeOnBoarding3
                else -> null
            }
        }
        viewBaseActivity.frAdsNative.isVisible = helper != null

        helper?.apply {
            updateActivity(this@OnboardingActivity, this@OnboardingActivity)
            setShimmerLayoutView(viewBaseActivity.shimmerContainerNative.shimmerContainerNative)
            setNativeContentView(viewBaseActivity.frAdsNative)
        }

        if(fullscreen) {
            startNativeCountdown(page.tag)
        }
    }
    private fun initNativeFullScreen() {
        val configs = listOf(
            FullscreenAdConfig(
                tag = "fs1",
                afterNonFullscreenCount = 1,
                adHelper = NativeAdsUtils.nativeFullScreen
            ),
            FullscreenAdConfig(
                tag = "fs2",
                afterNonFullscreenCount = 3,
                adHelper = NativeAdsUtils.nativeFullscreen2
            )
        )

        configs.forEach { config ->
            config.adHelper?.let { adHelper ->
                adHelper.getAdNativeState()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .onEach { state ->
                        when {
                            state == AdNativeState.Fail -> {
                                Logger.e("Fail")
                            }
                            state == adHelper.nativeAd?.let { AdNativeState.Loaded(it) } -> {
                                adHelper.updateActivity(this@OnboardingActivity, this@OnboardingActivity)
                                onBoardingAdapter.setNativeAdHelper(config.tag, adHelper)
                                val insertPos = onBoardingAdapter.findInsertPosition(config.afterNonFullscreenCount)
                                if(viewModel.currentPosition < insertPos && !onBoardingAdapter.hasFullscreenItem(config.tag)) {
                                    onBoardingAdapter.insertFullscreenAt(insertPos, config.tag)
                                }
                            }
                        }
                    }
                    .launchIn(lifecycleScope)
            }
        }
    }
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}

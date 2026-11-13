package com.kt.flash.light.led.banner.component.onboarding

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.component.permission.PermissionActivity
import com.kt.flash.light.led.banner.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    private val onBoardingAdapter = OnBoardingAdapter()
    private val viewModel: OnBoardingViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null
    private var navigating = false

    private val pageCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.currentPosition = position
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
    }

    override fun initData() {
        viewModel.getListOnBoarding()
    }

    override fun initObserver() {
        viewModel.listOnBoarding.onEach { pages ->
            onBoardingAdapter.setData(ArrayList(pages))
            viewBaseActivity.btnNext.isEnabled = pages.isNotEmpty()
            if (pages.isNotEmpty()) {
                val position = viewModel.currentPosition.coerceIn(pages.indices)
                viewBaseActivity.vpOnBoarding.setCurrentItem(position, false)
            }
        }.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).launchIn(lifecycleScope)
    }

    private fun nextAction() {
        if (navigating || onBoardingAdapter.itemCount == 0) return
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
        viewBaseActivity.vpOnBoarding.unregisterOnPageChangeCallback(pageCallback)
        tabMediator?.detach()
        tabMediator = null
        viewBaseActivity.vpOnBoarding.adapter = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}

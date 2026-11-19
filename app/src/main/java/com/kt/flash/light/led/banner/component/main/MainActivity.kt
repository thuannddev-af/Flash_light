package com.kt.flash.light.led.banner.component.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.permission.PermissionActivity
import com.kt.flash.light.led.banner.databinding.ActivityMainBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.BannerAdsUtils
import com.kt.flash.light.led.banner.utils.ads.InterAdsUtils
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils

class MainActivity : BaseActivity<ActivityMainBinding>() {
    private var redirectingForPermission = false
    private var tabHistory = TabHistory()
    private var interTabClickCount = 0L
    private var interClickStep = 2L
    private var clicksUntilNextInter = 2L
    override fun provideViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            tabHistory.visit(position)
            updateTab(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showBannerMain()
        InterAdsUtils.onHomeEntered()
        NativeAdsUtils.preLoadNativeCustom(this)
        window.setBackgroundDrawableResource(android.R.color.black)

       viewBaseActivity.tvFlash.isSelected = true
        viewBaseActivity.tvEdge.isSelected = true
        viewBaseActivity.tvLed.isSelected = true
        viewBaseActivity.tvAlert.isSelected = true

        interTabClickCount = savedInstanceState?.getLong(KEY_INTER_TAB_CLICKS) ?: 0L
        interClickStep = savedInstanceState?.getLong(KEY_INTER_CLICK_STEP)
            ?: FirebaseConfigManager.instance().interClick.toLong()
        clicksUntilNextInter = savedInstanceState?.getLong(KEY_NEXT_INTER_CLICKS)
            ?: interClickStep


        InterAdsUtils.loadInterAds(
            activity = this,
            idAds = BuildConfig.inter_home,
            adPlacement = AdPlacement.INTER_HOME,
            isEnable = FirebaseConfigManager.instance().adConfig.enableInterHome,
            showByTime = 1
        )

        tabHistory =
            TabHistory(savedInstanceState?.getIntArray(KEY_TAB_HISTORY)?.toList().orEmpty())
        with(viewBaseActivity) {
            mainPager.adapter = MainPagerAdapter(this@MainActivity)
            mainPager.setCurrentItem(tabHistory.current, false)
            mainPager.registerOnPageChangeCallback(pageCallback)
            tabFlash.setOnClickListener {
                openInterTab(MainPagerAdapter.FLASH_PAGE)
            }
            tabEdge.setOnClickListener {
                openInterTab(MainPagerAdapter.EDGE_PAGE)
            }
            tabLed.setOnClickListener { openInterTab(MainPagerAdapter.LED_PAGE) }
            tabAlert.setOnClickListener { openInterTab(MainPagerAdapter.ALERT_PAGE) }
            updateTab(mainPager.currentItem)
        }


    }

    private var switchingTab = false
    private fun openInterTab(possion: Int) {
        if (switchingTab) return
        interTabClickCount++
        android.util.Log.d("INTER_HOME_CLICKS", "click=$interTabClickCount / $clicksUntilNextInter")
        if (interTabClickCount < clicksUntilNextInter ||
            !FirebaseConfigManager.instance().adConfig.enableInterHome) {
            viewBaseActivity.mainPager.currentItem = possion
            return
        }
        switchingTab = true
        viewBaseActivity.root.visibility = View.INVISIBLE
        var adShown = false
        InterAdsUtils.showInterAds(
            activity = this,
            lifecycleOwner = this,
            adPlacement = AdPlacement.INTER_HOME,
            isReloadWhenClose = true,
            onAdShown = { adShown = true }
        ) {
            if (adShown) {
                interTabClickCount = 0L
                clicksUntilNextInter += interClickStep
            }
            switchingTab = false
            if (!isFinishing && !isDestroyed) {
                viewBaseActivity.mainPager.currentItem = possion
                viewBaseActivity.root.visibility = View.VISIBLE
            }
        }
    }

    override fun onBack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
            return
        }
        val previousPage = tabHistory.previous()
        if (previousPage == null) {
            finish()
        } else {
            viewBaseActivity.mainPager.setCurrentItem(previousPage, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(KEY_TAB_HISTORY, tabHistory.snapshot())
        outState.putLong(KEY_INTER_TAB_CLICKS, interTabClickCount)
        outState.putLong(KEY_INTER_CLICK_STEP, interClickStep)
        outState.putLong(KEY_NEXT_INTER_CLICKS, clicksUntilNextInter)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (!redirectingForPermission &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            redirectingForPermission = true
            PermissionActivity.start(this)
            finish()
        }
    }

    private fun updateTab(position: Int) {
        with(viewBaseActivity) {
            tabFlash.isSelected = position == MainPagerAdapter.FLASH_PAGE
            flashSettingsButton.isVisible = tabFlash.isSelected
            tabEdge.isSelected = position == MainPagerAdapter.EDGE_PAGE
            tabLed.isSelected = position == MainPagerAdapter.LED_PAGE
            screenTitle.isVisible = !tabLed.isSelected
            tabAlert.isSelected = position == MainPagerAdapter.ALERT_PAGE
            bottomTabs.invalidate()
            screenTitle.setText(
                when (position) {
                    MainPagerAdapter.EDGE_PAGE -> R.string.screen_light
                    MainPagerAdapter.LED_PAGE -> R.string.led
                    MainPagerAdapter.ALERT_PAGE -> R.string.setting
                    else -> R.string.flashlight_tab
                }
            )
        }
    }

    override fun onDestroy() {
        NativeAdsUtils.destroyNativeCustom(this)
        viewBaseActivity.mainPager.unregisterOnPageChangeCallback(pageCallback)
        viewBaseActivity.mainPager.adapter = null
        super.onDestroy()
    }

    private fun showBannerMain(){
        BannerAdsUtils.initBannerAll(this, this, viewBaseActivity.frAdsBanner)
    }
    companion object {
        private const val KEY_TAB_HISTORY = "main_tab_history"
        private const val KEY_INTER_TAB_CLICKS = "inter_tab_clicks"
        private const val KEY_INTER_CLICK_STEP = "inter_click_step"
        private const val KEY_NEXT_INTER_CLICKS = "next_inter_clicks"
        fun startNewTask(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
        }

        fun start(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
        }
    }
}

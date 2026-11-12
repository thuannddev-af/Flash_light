package com.kt.flash.light.led.banner.component.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun provideViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateTab(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(viewBaseActivity) {
            mainPager.adapter = MainPagerAdapter(this@MainActivity)
            mainPager.registerOnPageChangeCallback(pageCallback)
            tabFlash.setOnClickListener { mainPager.currentItem = MainPagerAdapter.FLASH_PAGE }
            tabEdge.setOnClickListener { mainPager.currentItem = MainPagerAdapter.EDGE_PAGE }
            tabLed.setOnClickListener { mainPager.currentItem = MainPagerAdapter.LED_PAGE }
            tabAlert.setOnClickListener { mainPager.currentItem = MainPagerAdapter.ALERT_PAGE }
            updateTab(mainPager.currentItem)
        }
    }

    private fun updateTab(position: Int) {
        with(viewBaseActivity) {
            tabFlash.isSelected = position == MainPagerAdapter.FLASH_PAGE
            tabEdge.isSelected = position == MainPagerAdapter.EDGE_PAGE
            tabLed.isSelected = position == MainPagerAdapter.LED_PAGE
            tabAlert.isSelected = position == MainPagerAdapter.ALERT_PAGE
            screenTitle.setText(when (position) {
                MainPagerAdapter.EDGE_PAGE -> R.string.screen_light
                MainPagerAdapter.LED_PAGE -> R.string.led
                MainPagerAdapter.ALERT_PAGE -> R.string.setting
                else -> R.string.app_name
            })
        }
    }

    override fun onDestroy() {
        viewBaseActivity.mainPager.unregisterOnPageChangeCallback(pageCallback)
        viewBaseActivity.mainPager.adapter = null
        super.onDestroy()
    }
    companion object {
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

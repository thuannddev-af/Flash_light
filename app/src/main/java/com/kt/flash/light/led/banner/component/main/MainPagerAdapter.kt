package com.kt.flash.light.led.banner.component.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.flash.light.led.banner.component.alertfragment.AlertFragment
import com.kt.flash.light.led.banner.component.edgefragment.EdgeFragment
import com.kt.flash.light.led.banner.component.flashfragment.FlashFragment
import com.kt.flash.light.led.banner.component.ledfragment.LedFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = PAGE_COUNT

    override fun createFragment(position: Int): Fragment = when (position) {
        FLASH_PAGE -> FlashFragment()
        EDGE_PAGE -> EdgeFragment()
        LED_PAGE -> LedFragment()
        ALERT_PAGE -> AlertFragment()
        else -> error("Unknown page: $position")
    }

    companion object {
        const val PAGE_COUNT = 4
        const val FLASH_PAGE = 0
        const val EDGE_PAGE = 1
        const val LED_PAGE = 2
        const val ALERT_PAGE = 3
    }
}

package com.kt.flash.light.led.banner.component.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kt.flash.light.led.banner.component.edgefragment.EdgeFragment
import com.kt.flash.light.led.banner.component.flashfragment.FlashFragment
import com.kt.flash.light.led.banner.component.ledfragment.LedFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        FLASH_PAGE -> FlashFragment()
        EDGE_PAGE -> EdgeFragment()
        LED_PAGE -> LedFragment()
        else -> error("Unknown page: $position")
    }

    companion object {
        const val FLASH_PAGE = 0
        const val EDGE_PAGE = 1
        const val LED_PAGE = 2
    }
}

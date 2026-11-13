package com.kt.flash.light.led.banner.utils.ads

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.BannerCollapsibleGravity
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager

object BannerAdsUtils {
    var bannerAdMain: BannerAdHelper? = null

    private fun setBanner(
        activity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner,
        bannerContentView: FrameLayout,
        config: BannerAdConfig
    ) {
        val bannerAdHelper =
            BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
        bannerAdHelper.setBannerContentView(bannerContentView)
        bannerAdHelper.requestAds(BannerAdParam.Request)
    }

    fun initBannerSplash(
        activity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner,
        bannerContentView: FrameLayout
    ) {
        val config = BannerAdConfig(
            idAds = BuildConfig.banner_splash,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableBannerSplash,
            canReloadAds = false,
            adPlacement = "banner_splash",
            reloadIfFirstFail = true
        )
//            .apply { collapsibleGravity = BannerCollapsibleGravity.BOTTOM }
        bannerAdMain = BannerAdHelper(activity, lifecycleOwner, config)
        bannerAdMain?.setBannerContentView(bannerContentView)
        bannerAdMain?.requestAds(BannerAdParam.Request)
    }

    fun cancelAllAds(isPurchased: Boolean) {
        bannerAdMain?.cancelRequestAndShowAllAds(isPurchased)
        bannerAdMain?.cancel()
    }
}
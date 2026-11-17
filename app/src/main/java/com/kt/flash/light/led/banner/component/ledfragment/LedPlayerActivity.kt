package com.kt.flash.light.led.banner.component.ledfragment

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityLedPlayerBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.BannerAdsUtils
import com.kt.flash.light.led.banner.utils.ads.InterAdsUtils

class LedPlayerActivity : BaseActivity<ActivityLedPlayerBinding>() {
    private var closing = false
    private var interBackRequested = false

    override fun provideViewBinding() = ActivityLedPlayerBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)
        ViewCompat.setFitsSystemWindows(viewBaseActivity.root, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        intent.getBundleExtra("settings")?.let { settings ->
            viewBaseActivity.ledBanner.apply {
                message = settings.getString("message", "HELLO WORLD")
                fontPixels = settings.getInt("font", 64)
                seconds = settings.getInt("speed", 5)
                textColor = settings.getInt("color", textColor)
                direction = settings.getInt("direction", 1)
                effect = settings.getInt("effect", 0)
                refresh()
            }
        }
        InterAdsUtils.preLoadInterBack(this)
        viewBaseActivity.closePlayer.setOnClickListener {
            onBack()
        }
        showBannerAll()
    }
    override fun onResume() {
        super.onResume()
        viewBaseActivity.ledBanner.running = !closing
        if (closing) viewBaseActivity.root.post { showPortraitInterBack() }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (closing) viewBaseActivity.root.post { showPortraitInterBack() }
    }
    override fun onPause() { viewBaseActivity.ledBanner.running = false; super.onPause() }
    private fun showBannerAll() {
        BannerAdsUtils.initBannerAll(this, this, viewBaseActivity.frAdsBanner)
    }
    override fun onBack() {
        if (closing || isFinishing || isDestroyed) return
        closing = true
        if (!FirebaseConfigManager.instance().adConfig.enableInterBack) {
            finishImmediately()
            return
        }
        // Keep the colored screen hidden through rotation, ad display and finishing.
        viewBaseActivity.root.visibility = View.INVISIBLE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewBaseActivity.root.post { showPortraitInterBack() }
    }
    @Suppress("DEPRECATION")
    private fun finishImmediately() {
        if (isFinishing || isDestroyed) return
        super.onBack()
        overridePendingTransition(0, 0)
    }
    private fun showPortraitInterBack() {
        if (!closing || interBackRequested || isFinishing || isDestroyed) return
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        interBackRequested = true
        InterAdsUtils.showInterBack(this, this) { finishImmediately() }
    }
}

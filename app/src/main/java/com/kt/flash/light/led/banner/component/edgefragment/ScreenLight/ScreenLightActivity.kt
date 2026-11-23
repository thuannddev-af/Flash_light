package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import androidx.lifecycle.Lifecycle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityScreenLightBinding
import com.kt.flash.light.led.banner.utils.ads.BannerAdsUtils
import com.kt.flash.light.led.banner.utils.ads.InterAdsUtils
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager

class ScreenLightActivity : BaseActivity<ActivityScreenLightBinding>() {
    private var closing = false
    private var interBackRequested = false
    override fun provideViewBinding() = ActivityScreenLightBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        ViewCompat.setFitsSystemWindows(viewBaseActivity.root, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewBaseActivity.screenColor.setBackgroundColor(intent.getIntExtra("color", Color.CYAN))
        InterAdsUtils.preLoadInterBack(this)
        viewBaseActivity.closeScreen.setOnClickListener {
            onBack()
        }
        //showBannerAll()
    }

    override fun onBack() {
        if (closing || isFinishing || isDestroyed) return
        closing = true
        if (!FirebaseConfigManager.instance().adConfig.enableInterBack) {
            finishImmediately()
            return
        }
        viewBaseActivity.root.visibility = View.INVISIBLE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewBaseActivity.root.post { showPortraitInterBack() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (closing) viewBaseActivity.root.post { showPortraitInterBack() }
    }

    override fun onResume() {
        super.onResume()
        if (closing) viewBaseActivity.root.post { showPortraitInterBack() }
    }

    private fun showPortraitInterBack() {
        if (!closing || interBackRequested || isFinishing || isDestroyed) return
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        interBackRequested = true
        InterAdsUtils.showInterBack(this, this) { finishImmediately() }
    }

    @Suppress("DEPRECATION")
    private fun finishImmediately() {
        if (isFinishing || isDestroyed) return
        super.onBack()
        overridePendingTransition(0, 0)
    }

//    private fun showBannerAll() {
//        BannerAdsUtils.initBannerAll(this, this, viewBaseActivity.frAdsBanner)
//    }
}

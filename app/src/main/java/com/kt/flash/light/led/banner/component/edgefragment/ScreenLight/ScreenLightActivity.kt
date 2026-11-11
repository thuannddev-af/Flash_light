package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kt.flash.light.led.banner.base.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityScreenLightBinding

class ScreenLightActivity : BaseActivity<ActivityScreenLightBinding>() {
    override fun provideViewBinding() = ActivityScreenLightBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setFitsSystemWindows(viewBaseActivity.root, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply {
            screenBrightness = (intent.getIntExtra("brightness", 80) / 100f).coerceIn(.01f, 1f)
        }
        viewBaseActivity.screenColor.setBackgroundColor(intent.getIntExtra("color", Color.CYAN))
        viewBaseActivity.closeScreen.setOnClickListener { finish() }
    }
}

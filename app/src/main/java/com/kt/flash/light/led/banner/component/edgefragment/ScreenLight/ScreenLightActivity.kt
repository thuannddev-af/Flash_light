package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityScreenLightBinding

class ScreenLightActivity : BaseActivity<ActivityScreenLightBinding>() {
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
        // Full-screen light inherits the Edge slider's per-window brightness.
        window.attributes = window.attributes.apply {
            screenBrightness = (intent.getIntExtra("brightness", 80) / 100f).coerceIn(0f, 1f)
        }
        viewBaseActivity.screenColor.setBackgroundColor(intent.getIntExtra("color", Color.CYAN))
        viewBaseActivity.closeScreen.setOnClickListener { finish() }
    }
}

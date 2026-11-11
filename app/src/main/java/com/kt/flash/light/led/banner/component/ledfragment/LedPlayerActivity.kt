package com.kt.flash.light.led.banner.component.ledfragment

import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kt.flash.light.led.banner.base.BaseActivity
import com.kt.flash.light.led.banner.databinding.ActivityLedPlayerBinding

class LedPlayerActivity : BaseActivity<ActivityLedPlayerBinding>() {
    override fun provideViewBinding() = ActivityLedPlayerBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        viewBaseActivity.closePlayer.setOnClickListener { finish() }
    }
    override fun onResume() { super.onResume(); viewBaseActivity.ledBanner.running = true }
    override fun onPause() { viewBaseActivity.ledBanner.running = false; super.onPause() }
}

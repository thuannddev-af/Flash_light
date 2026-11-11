package com.kt.flash.light.led.banner.base

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.viewbinding.ViewBinding

abstract class BaseActivity <V : ViewBinding> : AppCompatActivity(){
    lateinit var viewBaseActivity: V

    open fun onBack() {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBaseActivity = provideViewBinding()
        setContentView(viewBaseActivity.root)
        hideSystemUI()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    }
    private fun hideSystemUI() {
        applyFitsSystemWindows(viewBaseActivity.root.rootView, true)
//        WindowCompat.setDecorFitsSystemWindows(window, true)
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }

    private fun applyFitsSystemWindows(view: View?, fitSystemWindows: Boolean) {
        if (view is ViewGroup) {
            ViewCompat.setFitsSystemWindows(view, fitSystemWindows)
            for (i in 0 until view.childCount) {
                applyFitsSystemWindows(view.getChildAt(i), fitSystemWindows)
            }
        }
    }
    abstract fun provideViewBinding(): V
    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}
}
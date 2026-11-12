package com.kt.flash.light.led.banner.component.splash

import android.os.Bundle
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.component.language.LanguageActivity
import com.kt.flash.light.led.banner.databinding.ActivitySplashBinding
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.component.onboarding.OnboardingActivity
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.Utils
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    private var progressAnimator: ValueAnimator? = null
    private var progress = 0
    private var navigating = false



    @Inject
    lateinit var spManager: SpManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateProgress(savedInstanceState?.getInt(KEY_PROGRESS) ?: 0)
    }

//    override fun initViews() {
//        super.initViews()
//        updateProgress(savedInstanceState?.getInt(KEY_PROGRESS) ?: 0)
//    }

    override fun onConnectivityChanged(connected: Boolean) {
        if (connected) {
            startProgressBar()
        } else {
            progressAnimator?.cancel()
            progressAnimator = null
        }
    }

    // Animate from the current value to 100, then open MainActivity.
    private fun startProgressBar() {
        if (progressAnimator?.isStarted == true || navigating) return
        if (progress >= 100) {
            gotoMainScreen()
            return
        }
        // Intro animation only; replace with updateProgress(...) from real loading callbacks later.
        progressAnimator = ValueAnimator.ofInt(progress, 100).apply {
            duration = INTRO_DURATION_MS * (100 - progress) / 100
            interpolator = LinearInterpolator()
            addUpdateListener { updateProgress(it.animatedValue as Int) }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) gotoMainScreen()
                }
            })
            start()
        }
    }

    private fun updateProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        viewBaseActivity.progressBar.progress = progress
        viewBaseActivity.progressPercent.text = getString(R.string.splash_progress_percent, progress)
    }

    private fun gotoMainScreen() {
        if (navigating || isFinishing || isDestroyed) return
        if (!Utils.isConnected(this)) return
        navigating = true
        if(spManager.isLanguageChosen()) {
            OnboardingActivity.start(this)
        }else {
            LanguageActivity.start(this, isFromSplash = true)
        }
        finish()
    }
    override fun onPause() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_PROGRESS, progress)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val INTRO_DURATION_MS = 3000L
        const val KEY_PROGRESS = "splash_progress"
    }
}

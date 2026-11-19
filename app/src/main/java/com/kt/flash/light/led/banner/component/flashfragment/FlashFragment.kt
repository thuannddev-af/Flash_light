package com.kt.flash.light.led.banner.component.flashfragment

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.component.lightcontroller.Light
import com.kt.flash.light.led.banner.component.lightcontroller.LightController
import com.kt.flash.light.led.banner.databinding.FragmentFlashBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FlashFragment : BaseFragment<FragmentFlashBinding>() {
    private var lightOn = false
    private var activeMode = FLASH_MODE
    private var onSteps = 1
    private var offSteps = 1
    private var modes: List<View> = emptyList()
    private var torch: LightController? = null
    private var nativeCustomHelper: NativeAdHelper? = null

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentFlashBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeMode = savedInstanceState?.getInt(KEY_FRAME, FLASH_MODE)
            ?.takeIf { it in FLASH_MODE..DJ_MODE } ?: FLASH_MODE
        lightOn = false
        onSteps = savedInstanceState?.getInt(KEY_ON_STEPS, 1) ?: 1
        offSteps = savedInstanceState?.getInt(KEY_OFF_STEPS, 1) ?: 1
    }

    override fun initViews() {
        torch = LightController(requireContext()) { message ->
            lightOn = false
            if (view != null) renderState()
            context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
        }
        with(viewBaseFragment) {
            modes = listOf(modeFlash, modeSos, modeDj)
            modeFlash.setOnClickListener { selectMode(FLASH_MODE) }
            modeSos.setOnClickListener { selectMode(SOS_MODE) }
            modeDj.setOnClickListener { selectMode(DJ_MODE) }
            lampButton.setOnClickListener { toggleLight() }
            sosButton.setOnClickListener { toggleLight() }
            djButton.setOnClickListener { toggleLight() }
            bindTimingSlider(onTimeSlider, onTimeValue, onSteps - 1, minimumSteps = 1) {
                onSteps = it
            }
            bindTimingSlider(offTimeSlider, offTimeValue, offSteps, minimumSteps = 0) {
                offSteps = it
            }
        }
        renderState()
    }

    private fun bindTimingSlider(
        slider: SeekBar,
        value: TextView,
        progress: Int,
        minimumSteps: Int,
        setSteps: (Int) -> Unit
    ) {
        fun showValue(steps: Int) {
            value.text = if (steps == 0) getString(R.string.flash_continuous)
            else getString(R.string.flash_time_seconds, steps / 2.0)
        }

        slider.progress = progress
        showValue(progress + minimumSteps)
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var dragging = false

            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val steps = progress + minimumSteps
                setSteps(steps)
                showValue(steps)
                if (!dragging) applyFlashTiming()
            }

            override fun onStartTrackingTouch(bar: SeekBar) {
                dragging = true
            }

            override fun onStopTrackingTouch(bar: SeekBar) {
                dragging = false
                applyFlashTiming()
            }
        })
    }

    private fun applyFlashTiming() {
        if (lightOn && activeMode == FLASH_MODE) {
            torch?.updateFlashTiming(onSteps * STEP_MS, offSteps * STEP_MS)
        }
    }

    private fun selectMode(mode: Int) {
        if (activeMode == mode) return
        torch?.stop()
        lightOn = false
        activeMode = mode
        renderState()
    }

    private fun toggleLight() {
        if (lightOn) {
            stopLight()
            return
        }
        enableMode(activeMode)
    }

    private fun enableMode(mode: Int) {
        if (!isResumed || view == null) return
        activeMode = mode
        lightOn = true
        renderState()
        torch?.start(mode, onSteps * STEP_MS, offSteps * STEP_MS)
    }

    private fun stopLight() {
        torch?.stop()
        lightOn = false
        if (view != null) renderState()
    }

    override fun onResume() {
        super.onResume()
        torch?.register()
        initNativeNew()
    }

    override fun onPause() {
        stopLight()
        torch?.release()
        super.onPause()
    }

    private fun showFrame(mode: Int) {
        viewBaseFragment.flLight.isVisible = mode == FLASH_MODE
        viewBaseFragment.sosLight.isVisible = mode == SOS_MODE
        viewBaseFragment.djMode.isVisible = mode == DJ_MODE
        viewBaseFragment.flashTiming.isVisible = mode == FLASH_MODE
    }

    private fun renderState() {
        showFrame(activeMode)
        modes.forEachIndexed { index, view ->
            view.isSelected = index == activeMode
        }
        val flashOn = lightOn && activeMode == FLASH_MODE
        val sosOn = lightOn && activeMode == SOS_MODE
        val djOn = lightOn && activeMode == DJ_MODE

        with(viewBaseFragment) {
            lampButton.isSelected = flashOn
            lampButton.contentDescription =
                getString(if (flashOn) R.string.turn_off else R.string.turn_on)
            lampGlow.isVisible = flashOn
            lampStatus.setText(if (flashOn) R.string.on else R.string.off)

            sosButton.isSelected = sosOn
            sosButton.contentDescription =
                getString(if (sosOn) R.string.turn_off else R.string.turn_on)
            sosGlow.isVisible = sosOn
            sosStatus.setText(if (sosOn) R.string.on else R.string.off)

            djButton.isSelected = djOn
            djButton.contentDescription =
                getString(if (djOn) R.string.turn_off else R.string.turn_on)
            djGlow.isVisible = djOn
            djStatus.setText(if (djOn) R.string.on else R.string.off)

        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_LIGHT, lightOn)
        outState.putInt(KEY_FRAME, activeMode)
        outState.putInt(KEY_ON_STEPS, onSteps)
        outState.putInt(KEY_OFF_STEPS, offSteps)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        torch?.release()
        torch = null
        modes = emptyList()
        nativeCustomHelper?.destroy()
        nativeCustomHelper = null
        super.onDestroyView()
    }

//    private fun initNativeCustom() {
//        if (nativeCustomHelper != null) return
//        val enabled = FirebaseConfigManager.instance().adConfig.enableNativeCustom
//        viewBaseFragment.frAdsNative.isVisible = enabled
//        if (!enabled) return
//
//        val helper = NativeAdsUtils.nativeCustom ?: return
//        helper.setShimmerLayoutView(viewBaseFragment.shimmerContainerNative.root)
//        helper.setNativeContentView(viewBaseFragment.frAdsNative)
//        nativeCustomHelper = helper
//        helper.getAdNativeState().onEach { state ->
//            if (state is AdNativeState.Fail || state is AdNativeState.Cancel) {
//                viewBaseFragment.frAdsNative.isVisible = false
//            }
//        }.launchIn(viewLifecycleOwner.lifecycleScope)
//    }

    private fun initNativeNew() {
        NativeAdsUtils.showNativeCustom(this, AdPlacement.NATIVE_CUSTOM, viewBaseFragment.frAdsNative, viewBaseFragment.shimmerContainerNative.root)
    }
    private companion object {
        const val FLASH_MODE = Light.FLASH
        const val SOS_MODE = Light.SOS
        const val DJ_MODE = Light.DJ
        const val KEY_LIGHT = "light_on"
        const val KEY_FRAME = "active_frame"
        const val KEY_ON_STEPS = "flash_on_steps"
        const val KEY_OFF_STEPS = "flash_off_steps"
        const val STEP_MS = 500L
    }
}

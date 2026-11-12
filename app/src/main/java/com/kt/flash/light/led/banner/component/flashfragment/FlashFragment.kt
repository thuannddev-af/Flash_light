package com.kt.flash.light.led.banner.component.flashfragment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.databinding.FragmentFlashBinding

class FlashFragment : BaseFragment<FragmentFlashBinding>() {
    private var lightOn = false
    private var activeMode = FLASH_MODE
    private var modes: List<View> = emptyList()

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentFlashBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeMode = savedInstanceState?.getInt(KEY_FRAME, FLASH_MODE)
            ?.takeIf { it in FLASH_MODE..DJ_MODE } ?: FLASH_MODE
        lightOn = savedInstanceState?.getBoolean(KEY_LIGHT) ?: false
    }

    override fun initViews() {
        with(viewBaseFragment) {
            modes = listOf(modeFlash, modeSos, modeDj)
            modeFlash.setOnClickListener { toggleMode(FLASH_MODE) }
            modeSos.setOnClickListener { toggleMode(SOS_MODE) }
            modeDj.setOnClickListener { toggleMode(DJ_MODE) }
            lampButton.setOnClickListener { toggleMode(FLASH_MODE) }
            sosButton.setOnClickListener { toggleMode(SOS_MODE) }
            djButton.setOnClickListener { toggleMode(DJ_MODE) }
        }
        renderState()
    }

    private fun toggleMode(mode: Int) {
        lightOn = if (activeMode == mode) !lightOn else true
        activeMode = mode
        renderState()
    }

    // Frame visibility is independent of ON/OFF: keep the current frame when turned off.
    private fun showFrame(mode: Int) {
        viewBaseFragment.flLight.isVisible = mode == FLASH_MODE
        viewBaseFragment.sosLight.isVisible = mode == SOS_MODE
        viewBaseFragment.djMode.isVisible = mode == DJ_MODE
    }

    private fun renderState() {
        showFrame(activeMode)
        // Selected state controls the yellow border in bg_mode.xml.
        modes.forEachIndexed { index, view ->
            view.isSelected = lightOn && index == activeMode
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
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        modes = emptyList()
        super.onDestroyView()
    }

    private companion object {
        const val FLASH_MODE = 0
        const val SOS_MODE = 1
        const val DJ_MODE = 2
        const val KEY_LIGHT = "light_on"
        const val KEY_FRAME = "active_frame"
    }
}

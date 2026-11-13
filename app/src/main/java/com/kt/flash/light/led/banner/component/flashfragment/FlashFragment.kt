package com.kt.flash.light.led.banner.component.flashfragment

import android.os.Bundle
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.component.lightcontroller.Light
import com.kt.flash.light.led.banner.component.lightcontroller.LightController
import com.kt.flash.light.led.banner.databinding.FragmentFlashBinding

class FlashFragment : BaseFragment<FragmentFlashBinding>() {
    private var lightOn = false
    private var activeMode = FLASH_MODE
    private var modes: List<View> = emptyList()
    private var torch: LightController? = null

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentFlashBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeMode = savedInstanceState?.getInt(KEY_FRAME, FLASH_MODE)
            ?.takeIf { it in FLASH_MODE..DJ_MODE } ?: FLASH_MODE
        lightOn = false
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
        }
        renderState()
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
        torch?.start(mode)
    }

    private fun stopLight() {
        torch?.stop()
        lightOn = false
        if (view != null) renderState()
    }

    override fun onResume() {
        super.onResume()
        torch?.register()
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
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        torch?.release()
        torch = null
        modes = emptyList()
        super.onDestroyView()
    }

    private companion object {
        const val FLASH_MODE = Light.FLASH
        const val SOS_MODE = Light.SOS
        const val DJ_MODE = Light.DJ
        const val KEY_LIGHT = "light_on"
        const val KEY_FRAME = "active_frame"
    }
}

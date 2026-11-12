package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.databinding.ViewEdgeLightBinding

import com.kt.flash.light.led.banner.component.colorpicker.showColorPickerDialog

class ScreenLightController(private val activity: AppCompatActivity, private val binding: ViewEdgeLightBinding, state: Bundle?) {
    private var color = state?.getInt("edge_color", Color.rgb(75, 193, 234)) ?: Color.rgb(75, 193, 234)
    private var brightness = state?.getInt("edge_brightness", 80)?.coerceIn(0, 100) ?: 80
    private var opacity = state?.getInt("edge_opacity", 100)?.coerceIn(0, 100) ?: 100
    private val selectedHsv = FloatArray(3).apply {
        Color.colorToHSV(color, this)
        this[0] = state?.getFloat("edge_hue", this[0]) ?: this[0]
        this[1] = state?.getFloat("edge_saturation", this[1]) ?: this[1]
    }
    private var picker: Dialog? = null
    private var active = false
    private var previousWindowBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

    init {
        binding.colorPreview.setOnClickListener { showPicker() }
        binding.fullScreen.setOnClickListener {
            activity.startActivity(Intent(activity, ScreenLightActivity::class.java)
                .putExtra("color", color)
                .putExtra("brightness", brightness))
        }
        binding.brightnessSlider.progress = brightness
        binding.brightnessSlider.onProgress {
            brightness = it
            binding.brightnessValue.text = activity.getString(R.string.brightness_percent, brightness)
            applyWindowBrightness()
        }
        render()
    }

    fun setActive(value: Boolean) {
        if (active == value) return
        if (value) previousWindowBrightness = activity.window.attributes.screenBrightness
        active = value
        if (value) {
            applyWindowBrightness()
        } else {
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = previousWindowBrightness
            }
        }
    }

    private fun applyWindowBrightness() {
        if (!active) return
        val level = (brightness / 100f).coerceIn(0f, 1f)
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = level
        }
        picker?.window?.let { window ->
            window.attributes = window.attributes.apply { screenBrightness = level }
        }
    }

    private fun render() {
        color = Color.HSVToColor((opacity * 255f / 100).toInt(), selectedHsv)
        binding.colorPreview.background.mutate().setTint(color)
        binding.brightnessValue.text = activity.getString(R.string.brightness_percent, brightness)
        if (binding.brightnessSlider.progress != brightness) binding.brightnessSlider.progress = brightness
    }

    fun saveState(out: Bundle) {
        out.putInt("edge_color", color)
        out.putInt("edge_brightness", brightness)
        out.putInt("edge_opacity", opacity)
        out.putFloat("edge_hue", selectedHsv[0])
        out.putFloat("edge_saturation", selectedHsv[1])
    }

    fun destroy() { setActive(false); picker?.dismiss(); picker = null }

    private fun showPicker() {
        if (picker?.isShowing == true) return
        picker = showColorPickerDialog(activity, selectedHsv, opacity) { hsv, selectedOpacity ->
            hsv.copyInto(selectedHsv)
            opacity = selectedOpacity
            render()
        }
        applyWindowBrightness()
    }
}

private fun SeekBar.onProgress(action: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { action(progress) }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
}

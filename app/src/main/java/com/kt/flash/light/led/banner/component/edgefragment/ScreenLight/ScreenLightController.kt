package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
    private var previewColor = color

    init {
        binding.colorPreview.setOnClickListener { showPicker() }
        binding.fullScreen.setOnClickListener {
            activity.startActivity(Intent(activity, ScreenLightActivity::class.java)
                .putExtra("color", previewColor))
        }
        binding.brightnessSlider.progress = brightness
        binding.brightnessSlider.onProgress {
            brightness = it
            render()
        }
        render()
    }

    private fun render() {
        color = Color.HSVToColor((opacity * 255f / 100).toInt(), selectedHsv)
        // Scale a copy so restoring state or dragging repeatedly never compounds the dimming.
        val previewHsv = selectedHsv.copyOf()
        previewHsv[2] *= brightness / 100f
        previewColor = Color.HSVToColor((opacity * 255f / 100).toInt(), previewHsv)
        binding.colorPreview.background.mutate().setTint(previewColor)
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

    fun destroy() { picker?.dismiss(); picker = null }

    private fun showPicker() {
        if (picker?.isShowing == true) return
        picker = showColorPickerDialog(activity, selectedHsv, opacity) { hsv, selectedOpacity ->
            hsv.copyInto(selectedHsv)
            opacity = selectedOpacity
            render()
        }
    }
}

private fun SeekBar.onProgress(action: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { action(progress) }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
}

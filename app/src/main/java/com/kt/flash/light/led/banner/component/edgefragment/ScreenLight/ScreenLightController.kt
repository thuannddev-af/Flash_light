package com.kt.flash.light.led.banner.component.edgefragment.ScreenLight

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.WindowManager
import android.view.Gravity
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.databinding.DialogEdgeColorBinding
import com.kt.flash.light.led.banner.databinding.ViewEdgeLightBinding
import java.util.Locale

class ScreenLightController(private val activity: AppCompatActivity, private val binding: ViewEdgeLightBinding, state: Bundle?) {
    private var color = state?.getInt("edge_color", Color.rgb(75, 193, 234)) ?: Color.rgb(75, 193, 234)
    private var brightness = state?.getInt("edge_brightness", 80) ?: 80
    private var active = false
    private var picker: Dialog? = null

    init {
        binding.colorPreview.setOnClickListener { showPicker() }
        binding.fullScreen.setOnClickListener {
            activity.startActivity(Intent(activity, ScreenLightActivity::class.java)
                .putExtra("color", color).putExtra("brightness", brightness))
        }
        binding.brightnessSlider.progress = brightness
        binding.brightnessSlider.onProgress { brightness = it; render() }
        render()
    }

    fun setActive(value: Boolean) { active = value; render() }

    private fun render() {
        binding.colorPreview.background.mutate().setTint(color)
        binding.brightnessValue.text = activity.getString(R.string.brightness_percent, brightness)
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = if (active) (brightness / 100f).coerceAtLeast(.01f) else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    fun saveState(out: Bundle) {
        out.putInt("edge_color", color)
        out.putInt("edge_brightness", brightness)
    }

    fun destroy() { picker?.dismiss(); picker = null }

    private fun showPicker() {
        if (picker?.isShowing == true) return
        val ui = DialogEdgeColorBinding.inflate(activity.layoutInflater)
        val dialog = Dialog(activity)
        picker = dialog
        dialog.setContentView(ui.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        Color.colorToHSV(color, ui.colorPlane.hsv)
        var syncing = false
        var valid = true
        fun syncFields() {
            syncing = true
            val picked = Color.HSVToColor(ui.colorPlane.hsv)
            ui.inputHex.setText(String.format(Locale.ROOT, "#%06X", picked and 0xFFFFFF))
            ui.inputRed.setText(Color.red(picked).toString())
            ui.inputGreen.setText(Color.green(picked).toString())
            ui.inputBlue.setText(Color.blue(picked).toString())
            ui.hueSlider.progress = ui.colorPlane.hsv[0].toInt()
            ui.valueSlider.progress = (ui.colorPlane.hsv[2] * 100).toInt()
            ui.valueSlider.progressDrawable = colorTrack(intArrayOf(Color.BLACK,
                Color.HSVToColor(floatArrayOf(ui.colorPlane.hsv[0], ui.colorPlane.hsv[1], 1f))))
            listOf(ui.inputHex, ui.inputRed, ui.inputGreen, ui.inputBlue).forEach { it.error = null }
            valid = true
            ui.colorPlane.invalidate()
            syncing = false
        }
        ui.hueSlider.progressDrawable = colorTrack(
            intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED))
        ui.colorPlane.onColorChanged = { syncFields() }
        ui.hueSlider.onProgress { if (!syncing) { ui.colorPlane.hsv[0] = it.toFloat(); syncFields() } }
        ui.valueSlider.onProgress { if (!syncing) { ui.colorPlane.hsv[2] = it / 100f; syncFields() } }
        fun watch(input: EditText, hex: Boolean) {
            input.doAfterTextChanged {
                if (syncing) return@doAfterTextChanged
                val parsed = if (hex) {
                    val raw = ui.inputHex.text.toString().removePrefix("#")
                    if (raw.matches(Regex("[0-9a-fA-F]{6}"))) Color.parseColor("#$raw") else null
                } else {
                    val rgb = listOf(ui.inputRed, ui.inputGreen, ui.inputBlue).map { field -> field.text.toString().toIntOrNull() }
                    if (rgb.all { channel -> channel != null && channel in 0..255 }) Color.rgb(rgb[0]!!, rgb[1]!!, rgb[2]!!) else null
                }
                valid = parsed != null
                input.error = if (valid) null else activity.getString(R.string.invalid_color)
                if (parsed != null) {
                    listOf(ui.inputHex, ui.inputRed, ui.inputGreen, ui.inputBlue).forEach { it.error = null }
                    Color.colorToHSV(parsed, ui.colorPlane.hsv)
                    // Keep the edited field intact so typing does not move the cursor.
                    syncing = true
                    if (hex) {
                        ui.inputRed.setText(Color.red(parsed).toString())
                        ui.inputGreen.setText(Color.green(parsed).toString())
                        ui.inputBlue.setText(Color.blue(parsed).toString())
                    } else ui.inputHex.setText(String.format(Locale.ROOT, "#%06X", parsed and 0xFFFFFF))
                    ui.hueSlider.progress = ui.colorPlane.hsv[0].toInt()
                    ui.valueSlider.progress = (ui.colorPlane.hsv[2] * 100).toInt()
                    ui.valueSlider.progressDrawable = colorTrack(intArrayOf(Color.BLACK,
                        Color.HSVToColor(floatArrayOf(ui.colorPlane.hsv[0], ui.colorPlane.hsv[1], 1f))))
                    ui.colorPlane.invalidate()
                    syncing = false
                }
            }
        }
        watch(ui.inputHex, true)
        listOf(ui.inputRed, ui.inputGreen, ui.inputBlue).forEach { watch(it, false) }
        syncFields()
        ui.chooseColor.setOnClickListener {
            if (valid) {
                color = Color.HSVToColor(ui.colorPlane.hsv)
                render()
                dialog.dismiss()
            } else ui.inputHex.error = activity.getString(R.string.invalid_color)
        }
        dialog.show()
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * .9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun colorTrack(colors: IntArray): LayerDrawable {
        val thickness = activity.resources.getDimensionPixelSize(R.dimen.size8)
        val gradient = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = thickness.toFloat()
        }
        return LayerDrawable(arrayOf(gradient)).apply {
            setLayerHeight(0, thickness)
            setLayerGravity(0, Gravity.CENTER_VERTICAL or Gravity.FILL_HORIZONTAL)
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

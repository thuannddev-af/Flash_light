package com.kt.flash.light.led.banner.component.colorpicker

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.WindowManager
import android.view.Gravity
import android.widget.EditText
import android.widget.SeekBar
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.databinding.DialogEdgeColorBinding
import java.util.Locale

fun showColorPickerDialog(
    activity: android.app.Activity,
    initialHsv: FloatArray,
    initialOpacity: Int,
    onChoose: (FloatArray, Int) -> Unit
): Dialog {
        val ui = DialogEdgeColorBinding.inflate(activity.layoutInflater)
        val dialog = Dialog(activity)
        dialog.setContentView(ui.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        initialHsv.copyInto(ui.colorPlane.hsv)
        var syncing = false
        var valid = true
        var draftOpacity = initialOpacity.coerceIn(0, 100)
        fun syncOpacityTrack() {
            val opaque = Color.HSVToColor(ui.colorPlane.hsv)
            ui.opacitySlider.progress = draftOpacity
            ViewCompat.setStateDescription(ui.opacitySlider, "$draftOpacity%")
            ui.opacitySlider.progressDrawable = colorTrack(activity, 
                intArrayOf(opaque and 0x00FFFFFF, opaque), checkerboard = true)
        }
        fun syncThumbColors() {
            // Only the picker thumbs change color. Fragment brightness keeps ic_control yellow.
            val tint = ColorStateList.valueOf(Color.HSVToColor(ui.colorPlane.hsv))
            ui.hueSlider.thumbTintList = tint
            ui.opacitySlider.thumbTintList = tint
        }
        fun syncFields() {
            syncing = true
            val picked = Color.HSVToColor(ui.colorPlane.hsv)
            ui.inputHex.setText(String.format(Locale.ROOT, "#%06X", picked and 0xFFFFFF))
            ui.inputRed.setText(Color.red(picked).toString())
            ui.inputGreen.setText(Color.green(picked).toString())
            ui.inputBlue.setText(Color.blue(picked).toString())
            ui.hueSlider.progress = ui.colorPlane.hsv[0].toInt()
            syncOpacityTrack()
            listOf(ui.inputHex, ui.inputRed, ui.inputGreen, ui.inputBlue).forEach { it.error = null }
            valid = true
            ui.colorPlane.invalidate()
            syncThumbColors()
            syncing = false
        }
        ui.hueSlider.progressDrawable = colorTrack(activity, 
            intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED))
        ui.colorPlane.onColorChanged = { syncFields() }
        ui.hueSlider.onProgress { if (!syncing) { ui.colorPlane.hsv[0] = it.toFloat(); syncFields() } }
        ui.opacitySlider.onProgress {
            if (!syncing) {
                draftOpacity = it
                syncThumbColors()
                ViewCompat.setStateDescription(ui.opacitySlider, "$it%")
            }
        }
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
                    syncOpacityTrack()
                    ui.colorPlane.invalidate()
                    syncThumbColors()
                    syncing = false
                }
            }
        }
        watch(ui.inputHex, true)
        listOf(ui.inputRed, ui.inputGreen, ui.inputBlue).forEach { watch(it, false) }
        syncFields()
        ui.chooseColor.setOnClickListener {
            if (valid) {
                onChoose(ui.colorPlane.hsv.copyOf(), draftOpacity)
                dialog.dismiss()
            } else ui.inputHex.error = activity.getString(R.string.invalid_color)
        }
        dialog.show()
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * .9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
}

private fun colorTrack(activity: android.app.Activity, colors: IntArray, checkerboard: Boolean = false): LayerDrawable {
        val thickness = activity.resources.getDimensionPixelSize(R.dimen.size8)
        val gradient = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = thickness.toFloat()
        }
        val layers = if (checkerboard) arrayOf(
            checkNotNull(ContextCompat.getDrawable(activity, R.drawable.bg_opacity_checkerboard)), gradient
        ) else arrayOf(gradient)
        return LayerDrawable(layers).apply {
            for (index in layers.indices) {
                setLayerHeight(index, thickness)
                setLayerGravity(index, Gravity.CENTER_VERTICAL or Gravity.FILL_HORIZONTAL)
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

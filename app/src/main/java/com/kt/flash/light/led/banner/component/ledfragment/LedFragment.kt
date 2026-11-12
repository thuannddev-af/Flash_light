package com.kt.flash.light.led.banner.component.ledfragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.widget.doAfterTextChanged
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.BaseFragment
import com.kt.flash.light.led.banner.component.colorpicker.showColorPickerDialog
import com.kt.flash.light.led.banner.databinding.FragmentLedBinding
import kotlin.math.roundToInt

class LedFragment : BaseFragment<FragmentLedBinding>() {
    private var message = "HELLO WORLD"
    private var fontSize = 64
    private var speed = 5
    private var color = Color.rgb(255, 213, 79)
    private var direction = 1
    private var effect = 0
    private var picker: Dialog? = null

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentLedBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            message = it.getString("message", "HELLO WORLD")
            fontSize = it.getInt("font", 64).coerceIn(24, 128)
            speed = it.getInt("speed", 5).coerceIn(1, 20)
            color = it.getInt("color", color)
            direction = it.getInt("direction", 1).coerceIn(0, 3)
            effect = it.getInt("effect", 0).coerceIn(0, 2)
        }
    }

    override fun initViews() {
        with(viewBaseFragment) {
            ledInput.setText(message)
            fontSlider.progress = fontSize - 24
            speedSlider.progress = speed - 1
            ledInput.doAfterTextChanged { message = it.toString(); render() }
            fontSlider.listen { fontSize = it + 24; render() }
            speedSlider.listen { speed = it + 1; render() }
            listOf(
                directionRight,
                directionLeft,
                directionDown,
                directionUp
            ).forEachIndexed { index, view ->
                view.setOnClickListener { direction = index; render() }
            }
            listOf(effectGlow, effectBlink, effectNeon).forEachIndexed { index, view ->
                view.setOnClickListener { effect = index; render() }
            }
            val colors = listOf(
                0xFF37C4ED.toInt(),
                Color.WHITE,
                0xFFFFD54F.toInt(),
                0xFF81C784.toInt(),
                0xFFBA68C8.toInt(),
                0xFFEF7070.toInt()
            )
            listOf(
                colorCyan,
                colorWhite,
                colorYellow,
                colorGreen,
                colorPurple,
                colorRed
            ).forEachIndexed { index, view ->
                view.setOnClickListener { color = colors[index]; render() }
            }
            customColor.setOnClickListener { showColorPicker() }
            ledFullScreen.setOnClickListener {
                startActivity(
                    Intent(
                        requireContext(),
                        LedPlayerActivity::class.java
                    ).putExtra("settings", settings())
                )
            }
        }
        render()
    }

    private fun render() = with(viewBaseFragment) {
        fontValue.text = getString(R.string.font_px, fontSize)
        speedValue.text = getString(R.string.speed_seconds, speed)
        listOf(
            directionRight,
            directionLeft,
            directionDown,
            directionUp
        ).forEachIndexed { i, view -> view.isSelected = i == direction }
        listOf(effectGlow, effectBlink, effectNeon).forEachIndexed { i, view ->
            view.isSelected = i == effect
        }
        val colors = listOf(
            0xFF37C4ED.toInt(),
            Color.WHITE,
            0xFFFFD54F.toInt(),
            0xFF81C784.toInt(),
            0xFFBA68C8.toInt(),
            0xFFEF7070.toInt()
        )
        listOf(
            colorCyan,
            colorWhite,
            colorYellow,
            colorGreen,
            colorPurple,
            colorRed
        ).forEachIndexed { i, view -> view.isSelected = color == colors[i] }
        customColor.isSelected = color !in colors
        ledPreview.apply {
            message = this@LedFragment.message
            fontPixels = fontSize
            seconds = speed
            textColor = color
            direction = this@LedFragment.direction
            effect = this@LedFragment.effect
            refresh()
        }
    }

    private fun showColorPicker() {
        if (picker?.isShowing == true) return
        val hsv = FloatArray(3).apply { Color.colorToHSV(color, this) }
        val opacity = (Color.alpha(color) * 100f / 255).roundToInt()
        picker =
            showColorPickerDialog(requireActivity(), hsv, opacity) { selectedHsv, selectedOpacity ->
                color = Color.HSVToColor((selectedOpacity * 255f / 100).roundToInt(), selectedHsv)
                render()
            }
    }

    private fun settings() = Bundle().apply {
        putString("message", message); putInt("font", fontSize); putInt("speed", speed)
        putInt("color", color); putInt("direction", direction); putInt("effect", effect)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putAll(settings()); super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume(); viewBaseFragment.ledPreview.running = true
    }

    override fun onPause() {
        viewBaseFragment.ledPreview.running = false; super.onPause()
    }

    override fun onDestroyView() {
        picker?.dismiss(); picker = null; viewBaseFragment.ledPreview.running =
            false; super.onDestroyView()
    }
}

private fun SeekBar.listen(action: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) =
            action(progress)

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
}

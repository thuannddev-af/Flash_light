package com.kt.flash.light.led.banner.component.ledfragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.kt.flash.light.led.banner.BuildConfig
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.component.colorpicker.showColorPickerDialog
import com.kt.flash.light.led.banner.databinding.FragmentLedBinding
import com.kt.flash.light.led.banner.databinding.ViewLedPaletteBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

class LedFragment : BaseFragment<FragmentLedBinding>() {
    private var message = "HELLO WORLD"
    private var fontSize = 64
    private var speed = 5
    private var color = Color.rgb(255, 213, 79)
    private var direction = 1
    private var effect = 0
    private var edgeTabSelected = false
    private var edgeEnabled = false
    private var edgeColor = 0xFF37C4ED.toInt()
    private var edgeStyle = 0
    private var picker: Dialog? = null
    private var nativeAdHelper : NativeAdHelper? = null

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
            edgeTabSelected = it.getBoolean("edge_tab")
            edgeEnabled = it.getBoolean("edge_enabled")
            edgeColor = it.getInt("edge_color", edgeColor)
            edgeStyle = it.getInt("edge_style").coerceIn(edgeStyles.indices)
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
            ledTab.setOnClickListener { selectEdgeTab(false) }
            edgeTab.setOnClickListener { selectEdgeTab(true) }
            edgeContent.edgeToggle.setOnClickListener {
                edgeEnabled = !edgeEnabled
                renderEdge()
            }
            bindPalette(ledPalette, { color }) {
                color = it
                render()
            }
            bindPalette(edgeContent.edgePalette, { edgeColor }) {
                edgeColor = it
                renderEdge()
            }
            edgeContent.edgePalette.editColor.isVisible = true
            bindEdgeStyles()
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
        renderEdge()
        selectEdgeTab(edgeTabSelected)
    }

    private fun selectEdgeTab(selected: Boolean) = with(viewBaseFragment) {
        edgeTabSelected = selected
        ledTab.isSelected = !selected
        edgeTab.isSelected = selected
        ledContent.isVisible = !selected
        edgeContent.root.isVisible = selected
        ledPreview.running = !selected && isResumed
    }

    private fun renderEdge() = with(viewBaseFragment.edgeContent) {
        edgeToggleIcon.setImageResource(if (edgeEnabled) R.drawable.ic_on else R.drawable.ic_off)
        ViewCompat.setStateDescription(edgeToggle, getString(if (edgeEnabled) R.string.on else R.string.off))
        edgePreview.setStyle(this@LedFragment.edgeStyles[edgeStyle], edgeColor)
        edgePreview.isVisible = edgeEnabled
        renderPalette(edgePalette, edgeColor)
        repeat(edgeStyles.childCount) { index ->
            edgeStyles.getChildAt(index).isSelected = index == edgeStyle
        }
    }

    private fun bindEdgeStyles() {
        val size = resources.getDimensionPixelSize(R.dimen.size48)
        val spacing = resources.getDimensionPixelSize(R.dimen.size8)
        val row = viewBaseFragment.edgeContent.edgeStyles
        edgeStyles.forEachIndexed { index, icon ->
            val button = AppCompatImageButton(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_mode)
                setImageResource(icon)
                scaleType = ImageView.ScaleType.CENTER
                contentDescription = getString(R.string.led_edge_border_option, index + 1)
                setOnClickListener {
                    edgeStyle = index
                    renderEdge()
                }
            }
            row.addView(button, LinearLayout.LayoutParams(size, size).apply {
                if (index > 0) marginStart = spacing
            })
        }
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
        renderPalette(ledPalette, color)
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

    private val paletteColors = listOf(
        0xFF37C4ED.toInt(),
        Color.WHITE,
        0xFFFFD54F.toInt(),
        0xFF81C784.toInt(),
        0xFFBA68C8.toInt(),
        0xFFEF7070.toInt()
    )

    private val edgeStyles = listOf(
        R.drawable.ic_led_edge_butterfly,
        R.drawable.ic_led_edge_style_0,
        R.drawable.ic_led_edge_style_1,
        R.drawable.ic_led_edge_style_2,
        R.drawable.ic_led_edge_style_3,
        R.drawable.ic_led_edge_style_4,
        R.drawable.ic_led_edge_style_5,
        R.drawable.ic_led_edge_style_6,
        R.drawable.ic_led_edge_style_7,
        R.drawable.ic_led_edge_style_8,
        R.drawable.ic_led_edge_style_9,
        R.drawable.ic_led_edge_style_10
    )

    private fun ViewLedPaletteBinding.swatches() =
        listOf(colorCyan, colorWhite, colorYellow, colorGreen, colorPurple, colorRed)

    private fun bindPalette(
        palette: ViewLedPaletteBinding,
        currentColor: () -> Int,
        onColorChanged: (Int) -> Unit
    ) {
        palette.swatches().zip(paletteColors).forEach { (swatch, preset) ->
            swatch.setOnClickListener { onColorChanged(preset) }
        }
        palette.customColor.setOnClickListener { showColorPicker(currentColor(), onColorChanged) }
        palette.editColor.setOnClickListener { showColorPicker(currentColor(), onColorChanged) }
    }

    private fun renderPalette(palette: ViewLedPaletteBinding, color: Int) {
        palette.swatches().zip(paletteColors).forEach { (swatch, preset) ->
            swatch.isSelected = color == preset
        }
        palette.customColor.isSelected = color !in paletteColors
    }

    private fun showColorPicker(color: Int, onColorChanged: (Int) -> Unit) {
        if (picker?.isShowing == true) return
        val hsv = FloatArray(3).apply { Color.colorToHSV(color, this) }
        val opacity = (Color.alpha(color) * 100f / 255).roundToInt()
        picker =
            showColorPickerDialog(requireActivity(), hsv, opacity) { selectedHsv, selectedOpacity ->
                onColorChanged(
                    Color.HSVToColor((selectedOpacity * 255f / 100).roundToInt(), selectedHsv)
                )
            }
    }

    private fun settings() = Bundle().apply {
        putString("message", message); putInt("font", fontSize); putInt("speed", speed)
        putInt("color", color); putInt("direction", direction); putInt("effect", effect)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putAll(settings())
        outState.putBoolean("edge_tab", edgeTabSelected)
        outState.putBoolean("edge_enabled", edgeEnabled)
        outState.putInt("edge_color", edgeColor)
        outState.putInt("edge_style", edgeStyle)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume();
        viewBaseFragment.ledPreview.running = !edgeTabSelected
        initNativeNew()
        showNativeAll()
    }

    override fun onPause() {
        viewBaseFragment.ledPreview.running = false; super.onPause()
    }

    override fun onDestroyView() {
        nativeAdHelper?.destroy()
        nativeAdHelper = null
        picker?.dismiss(); picker = null; viewBaseFragment.ledPreview.running =
            false; super.onDestroyView()


    }

    private fun initNativeCustom(){
        if(nativeAdHelper != null) return
        val enable = FirebaseConfigManager.instance().adConfig.enableNativeCustom
        viewBaseFragment.frAdsNative.isVisible = enable
        if(!enable) return

        val helper = NativeAdsUtils.nativeCustom ?: return
        helper.setShimmerLayoutView(viewBaseFragment.shimmerContainerNative.root)
        helper.setNativeContentView(viewBaseFragment.frAdsNative)
        nativeAdHelper = helper
        helper.getAdNativeState().onEach { state ->
            if(state is AdNativeState.Fail || state is AdNativeState.Cancel) {
                viewBaseFragment.frAdsNative.isVisible = false
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun initNativeNew() {
        NativeAdsUtils.showNativeCustom(this, AdPlacement.NATIVE_CUSTOM, viewBaseFragment.frAdsNative, viewBaseFragment.shimmerContainerNative.root)
    }

    private fun showNativeAll() {
        NativeAdsUtils.loadAndShowNativeAll(
            requireActivity(),
            adUnitId = BuildConfig.native_custom,
            placement = AdPlacement.NATIVE_CUSTOM,
            container = viewBaseFragment.frAdsNative2,
            shimmer = viewBaseFragment.shimmerContainerNative1.root,
            canShowAds = NativeAdsUtils.adConfig.enableNativeCustom,
            layoutId = R.layout.layout_native_medium
        )
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

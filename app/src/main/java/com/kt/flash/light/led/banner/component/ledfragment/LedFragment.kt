package com.kt.flash.light.led.banner.component.ledfragment

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.LinearLayout
import android.widget.ImageView
import android.view.Gravity
import android.widget.TextView
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
import com.kt.flash.light.led.banner.component.dialog.EdgeDisplayModeDialog
import com.kt.flash.light.led.banner.databinding.FragmentLedBinding
import com.kt.flash.light.led.banner.databinding.DialogEditBorderColorsBinding
import com.kt.flash.light.led.banner.customview.LedEdgeSettings
import com.kt.flash.light.led.banner.databinding.ViewLedPaletteBinding
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt
import java.util.Locale

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
    private var edgeColors: List<Int> = emptyList()
    private var edgeColorEditor: Dialog? = null
    private var displayModeDialog: EdgeDisplayModeDialog? = null
    private var pendingHomeOverlay = false
    private var pendingAppsOverlay = false
    private val overlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(requireContext())) checkEdgeUsageAccess()
        else showEdgeModeMessage(R.string.edge_mode_overlay_unavailable)
    }
    private val usagePermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (EdgeOverlayService.hasUsageAccess(requireContext())) startEdgeOverlay()
        else showEdgeModeMessage(R.string.edge_mode_usage_access)
    }
    private val edgeColorPrefs by lazy {
        requireContext().getSharedPreferences("led_edge_colors", android.content.Context.MODE_PRIVATE)
    }
    private var edgeStyle = 0
    private var edgeEffects = LedEdgeSettings()
    private var edgeStyleGroup = 0
    private var visibleEdgeStyleGroup = 0
    private val edgeStyleButtons = mutableListOf<AppCompatImageButton>()
    private val edgeGroupButtons = mutableListOf<TextView>()
    private var picker: Dialog? = null
    private var nativeAdHelper : NativeAdHelper? = null

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentLedBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeColors = edgeColorPrefs.getString("palette", null)
            ?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() }
            ?: paletteColors
        edgeColor = edgeColorPrefs.getInt("selected", edgeColors.first())
            .takeIf { it in edgeColors } ?: edgeColors.first()
        savedInstanceState?.let {
            pendingHomeOverlay = it.getBoolean("pending_home_overlay")
            pendingAppsOverlay = it.getBoolean("pending_apps_overlay")
            message = it.getString("message", "HELLO WORLD")
            fontSize = it.getInt("font", 64).coerceIn(24, 128)
            speed = it.getInt("speed", 5).coerceIn(1, 20)
            color = it.getInt("color", color)
            direction = it.getInt("direction", 1).coerceIn(0, 3)
            effect = it.getInt("effect", 0).coerceIn(0, 2)
            edgeTabSelected = it.getBoolean("edge_tab")
            edgeEnabled = it.getBoolean("edge_enabled")
            edgeEffects = LedEdgeSettings.fromBundle(it.getBundle("edge_effects"))
            edgeStyleGroup = it.getInt("edge_style_group").coerceIn(borderGroups.indices)
            edgeStyle = it.getInt("edge_style_in_group").coerceIn(borderGroups[edgeStyleGroup].indices)
            visibleEdgeStyleGroup = it.getInt("edge_visible_group", edgeStyleGroup).coerceIn(borderGroups.indices)
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
            with(edgeContent.edgePalette) {
                presetColors.isVisible = false
                editableColors.isVisible = true
                editColor.isVisible = true
                editColor.setOnClickListener { showEdgeColorEditor() }
            }
            renderEdgeColors()
            bindEdgeStyles()
            bindEdgeEffects()
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
        edgePreview.setStyle(edgeSettings())
        edgePreview.isVisible = edgeEnabled
        edgeStyleButtons.forEachIndexed { index, button ->
            button.isSelected = visibleEdgeStyleGroup == edgeStyleGroup && index == edgeStyle
        }
    }

    private fun renderEdgeColors() {
        drawEdgeColorGrid(
            viewBaseFragment.edgeContent.edgePalette.editableColors,
            edgeColors,
            selected = edgeColor,
            onAdd = {
                showColorPicker(edgeColor) { added -> saveEdgeColors(edgeColors + added, added) }
            },
            onSelect = { saveEdgeColors(edgeColors, it) }
        )
    }

    private fun saveEdgeColors(colors: List<Int>, selected: Int) {
        edgeColors = colors.distinct()
        edgeColor = selected.takeIf { it in edgeColors } ?: edgeColors.first()
        edgeColorPrefs.edit()
            .putString("palette", edgeColors.joinToString(","))
            .putInt("selected", edgeColor)
            .apply()
        renderEdgeColors()
        renderEdge()
    }

    private fun showEdgeColorEditor() {
        if (edgeColorEditor?.isShowing == true) return
        val ui = DialogEditBorderColorsBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        val draft = edgeColors.toMutableList()
        var selected = edgeColor
        fun refresh() {
            ui.choose.isEnabled = draft.isNotEmpty()
            drawEdgeColorGrid(
                ui.colors, draft, selected,
                onAdd = {
                    showColorPicker(selected) { added ->
                        if (added !in draft) draft.add(added)
                        selected = added
                        refresh()
                    }
                },
                onSelect = { selected = it; refresh() },
                onRemove = {
                    draft.remove(it)
                    if (it == selected) selected = draft.firstOrNull() ?: edgeColor
                    refresh()
                }
            )
        }
        ui.choose.setOnClickListener {
            if (draft.isNotEmpty()) saveEdgeColors(draft, selected)
            dialog.dismiss()
        }
        ui.cancel.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(ui.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setOnDismissListener { edgeColorEditor = null }
        edgeColorEditor = dialog
        refresh()
        dialog.show()
        dialog.window?.setLayout(
            minOf(resources.displayMetrics.widthPixels - colorDp(32), colorDp(360)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun drawEdgeColorGrid(
        grid: LinearLayout,
        colors: List<Int>,
        selected: Int,
        onAdd: () -> Unit,
        onSelect: (Int) -> Unit,
        onRemove: ((Int) -> Unit)? = null
    ) {
        grid.removeAllViews()
        (listOf<Int?>(null) + colors).chunked(5).forEach { colorsInRow ->
            val row = LinearLayout(grid.context)
            grid.addView(row, LinearLayout.LayoutParams(-1, colorDp(54)))
            repeat(5) { index ->
                val tile = FrameLayout(grid.context)
                row.addView(tile, LinearLayout.LayoutParams(0, -1, 1f))
                if (index >= colorsInRow.size) return@repeat
                val swatch = colorsInRow[index]
                val hex = swatch?.let { String.format(Locale.ROOT, "#%08X", it) }.orEmpty()
                val circle = TextView(grid.context).apply {
                    gravity = Gravity.CENTER
                    text = if (swatch == null) "+" else ""
                    textSize = 23f
                    setTextColor(Color.WHITE)
                    background = colorCircle(swatch ?: 0xFF36394A.toInt(), swatch == selected)
                    contentDescription = if (swatch == null) getString(R.string.edge_add_color)
                    else getString(R.string.edge_select_color, hex)
                    isSelected = swatch == selected
                    isFocusable = true
                    setOnClickListener { if (swatch == null) onAdd() else onSelect(swatch) }
                }
                tile.addView(circle, FrameLayout.LayoutParams(colorDp(36), colorDp(36), Gravity.CENTER))
                if (swatch != null && onRemove != null) {
                    val remove = ImageView(grid.context).apply {
                        setImageResource(R.drawable.ic_close_2)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        contentDescription = getString(R.string.edge_remove_color, hex)
                        isFocusable = true
                        setOnClickListener { onRemove(swatch) }
                    }
                    tile.addView(remove, FrameLayout.LayoutParams(
                        colorDp(22), colorDp(22), Gravity.TOP or Gravity.END
                    ))
                }
            }
        }
    }

    private fun colorCircle(fill: Int, selected: Boolean = false) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        if (selected) setStroke(colorDp(2), 0xFFAF8318.toInt())
    }

    private fun colorDp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private fun edgeSettings() = edgeEffects.copy(
        icon = borderGroups[edgeStyleGroup][edgeStyle],
        color = edgeColor
    )

    private fun bindEdgeEffects() = with(viewBaseFragment.edgeContent) {
        val runLabels = listOf(
            R.string.edge_run_none, R.string.edge_run_rotation,
            R.string.edge_run_forward, R.string.edge_run_reverse,
            R.string.edge_run_crawler, R.string.edge_run_reverse_crawler,
            R.string.edge_run_snake, R.string.edge_run_reverse_snake
        )
        addEdgeChoices(edgeRunOptions, runLabels, 2, edgeEffects.runStyle) { index ->
            edgeEffects = edgeEffects.copy(runStyle = index)
        }
        val notchLabels = listOf(
            R.string.edge_notch_full, R.string.edge_notch_water,
            R.string.edge_notch_full_left, R.string.edge_notch_full_center
        )
        addEdgeChoices(edgeNotchOptions, notchLabels, 4, edgeEffects.notchStyle, listOf(
            R.drawable.ic_style_full, R.drawable.ic_style_water,
            R.drawable.ic_style_full2, R.drawable.ic_style_full3
        )) { index ->
            edgeEffects = edgeEffects.copy(notchStyle = index)
        }

        edgeTopRadius.progress = edgeEffects.topRadius
        edgeBottomRadius.progress = edgeEffects.bottomRadius
        edgeBorderWidth.progress = edgeEffects.borderWidth
        edgeColorSpeed.progress = edgeEffects.colorSpeed
        edgeTopRadius.listen { edgeEffects = edgeEffects.copy(topRadius = it); renderEdge() }
        edgeBottomRadius.listen { edgeEffects = edgeEffects.copy(bottomRadius = it); renderEdge() }
        edgeBorderWidth.listen { edgeEffects = edgeEffects.copy(borderWidth = it); renderEdge() }
        edgeColorSpeed.listen { edgeEffects = edgeEffects.copy(colorSpeed = it); renderEdge() }
        edgeApply.setOnClickListener {
            displayModeDialog = EdgeDisplayModeDialog(
                requireContext(), EdgeOverlayService.homeActive, EdgeOverlayService.appsActive,
                ::applyEdgeDisplayMode
            ).also { it.show() }
        }
    }

    private fun applyEdgeDisplayMode(homeEnabled: Boolean, appsEnabled: Boolean) {
        val context = requireContext()
        pendingHomeOverlay = homeEnabled
        pendingAppsOverlay = appsEnabled
        if (!homeEnabled && !appsEnabled) {
            context.stopService(Intent(context, EdgeOverlayService::class.java))
        } else if (!Settings.canDrawOverlays(context)) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.fromParts("package", context.packageName, null)
            )
            try {
                overlayPermission.launch(permissionIntent)
            } catch (_: ActivityNotFoundException) {
                showEdgeModeMessage(R.string.edge_mode_overlay_unavailable)
            }
        } else {
            checkEdgeUsageAccess()
        }
    }

    private fun checkEdgeUsageAccess() {
        if (pendingHomeOverlay != pendingAppsOverlay &&
            !EdgeOverlayService.hasUsageAccess(requireContext())
        ) {
            showEdgeModeMessage(R.string.edge_mode_usage_access)
            try {
                usagePermission.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                return
            }
        } else {
            startEdgeOverlay()
        }
    }

    private fun startEdgeOverlay() {
        val context = requireContext()
        try {
            val service = Intent(context, EdgeOverlayService::class.java)
                .putExtras(edgeSettings().toBundle())
                .putExtra(EdgeOverlayService.HOME, pendingHomeOverlay)
                .putExtra(EdgeOverlayService.APPS, pendingAppsOverlay)
            ContextCompat.startForegroundService(context, service)
        } catch (_: IllegalStateException) {
            showEdgeModeMessage(R.string.edge_mode_overlay_unavailable)
        } catch (_: SecurityException) {
            showEdgeModeMessage(R.string.edge_mode_overlay_unavailable)
        }
    }

    private fun showEdgeModeMessage(message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun addEdgeChoices(
        container: LinearLayout,
        labels: List<Int>,
        columns: Int,
        selected: Int,
        icons: List<Int> = emptyList(),
        onSelected: (Int) -> Unit
    ) {
        val spacing = resources.getDimensionPixelSize(R.dimen.size8)
        val buttons = mutableListOf<TextView>()
        labels.chunked(columns).forEachIndexed { rowIndex, rowLabels ->
            val row = LinearLayout(requireContext())
            container.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = spacing })
            rowLabels.forEachIndexed { column, label ->
                val index = rowIndex * columns + column
                val button = TextView(requireContext()).apply {
                    setText(label)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isSelected = index == selected
                    setBackgroundResource(R.drawable.bg_led_border_group)
                    setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
                    icons.getOrNull(index)?.let {
                        val icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, it)
                        val height = resources.getDimensionPixelSize(R.dimen.size40)
                        icon?.setBounds(0, 0, height * 44 / 61, height)
                        setCompoundDrawables(null, icon, null, null)
                        setPadding(0, colorDp(4), 0, 0)
                    }
                    setOnClickListener {
                        buttons.forEachIndexed { position, option ->
                            option.isSelected = position == index
                            option.setTextColor(if (option.isSelected) Color.BLACK else Color.WHITE)
                        }
                        onSelected(index)
                        renderEdge()
                    }
                }
                buttons.add(button)
                val height = resources.getDimensionPixelSize(
                    if (icons.isEmpty()) R.dimen.size40 else R.dimen.size72
                )
                row.addView(button, LinearLayout.LayoutParams(0, height, 1f).apply {
                    if (column > 0) marginStart = spacing
                })
            }
        }
    }

    private fun bindEdgeStyles() {
        val spacing = resources.getDimensionPixelSize(R.dimen.size8)
        val padding = resources.getDimensionPixelSize(R.dimen.size12)
        val groups = viewBaseFragment.edgeContent.edgeStyleGroups
        val groupNames = listOf(
            R.string.led_edge_border_arrow,
            R.string.led_edge_border_commerce,
            R.string.led_edge_border_games,
            R.string.led_edge_border_weather_nature
        )
        groupNames.forEachIndexed { group, name ->
            val button = TextView(requireContext()).apply {
                setText(name)
                setBackgroundResource(R.drawable.bg_led_border_group)
                setPaddingRelative(padding, 0, padding, 0)
                gravity = Gravity.CENTER
                isFocusable = true
                setOnClickListener { showEdgeStyleGroup(group) }
            }
            edgeGroupButtons.add(button)
            groups.addView(button, LinearLayout.LayoutParams(-2, resources.getDimensionPixelSize(R.dimen.size40)).apply {
                if (group > 0) marginStart = spacing
            })
        }
        showEdgeStyleGroup(visibleEdgeStyleGroup)
    }

    private fun showEdgeStyleGroup(group: Int) {
        visibleEdgeStyleGroup = group
        edgeGroupButtons.forEachIndexed { index, button ->
            button.isSelected = index == group
            button.setTextColor(if (button.isSelected) Color.BLACK else Color.WHITE)
        }

        val grid = viewBaseFragment.edgeContent.edgeStyles
        val spacing = resources.getDimensionPixelSize(R.dimen.size8)
        val size = resources.getDimensionPixelSize(R.dimen.size40)
        grid.removeAllViews()
        edgeStyleButtons.clear()
        borderGroups[group].chunked(5).forEachIndexed { rowIndex, icons ->
            val row = LinearLayout(requireContext())
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                if (rowIndex > 0) topMargin = spacing
            })
            icons.forEachIndexed { column, icon ->
                val index = rowIndex * 5 + column
                val button = AppCompatImageButton(requireContext()).apply {
                    setBackgroundResource(R.drawable.bg_led_border_option)
                    setImageResource(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(0, 0, 0, 0)
                    contentDescription = getString(R.string.led_edge_border_option, index + 1)
                    setOnClickListener {
                        edgeStyleGroup = group
                        edgeStyle = index
                        renderEdge()
                    }
                }
                edgeStyleButtons.add(button)
                row.addView(button, LinearLayout.LayoutParams(0, size, 1f).apply {
                    if (column > 0) marginStart = spacing
                })
            }
        }
        renderEdge()
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

    private val borderGroups = listOf(
        listOf(
            R.drawable.bd1_1, R.drawable.bd1_2, R.drawable.bd1_3, R.drawable.bd1_4,
            R.drawable.bd1_5, R.drawable.bd1_6, R.drawable.bd1_7, R.drawable.bd1_8,
            R.drawable.bd1_9, R.drawable.bd1_10, R.drawable.bd1_11, R.drawable.bd1_12,
            R.drawable.bd1_13, R.drawable.bd1_14, R.drawable.bd1_15
        ),
        listOf(
            R.drawable.bd2_1, R.drawable.bd2_2, R.drawable.bd2_3, R.drawable.bd2_4,
            R.drawable.bd2_5, R.drawable.bd2_6, R.drawable.bd2_7, R.drawable.bd2_8,
            R.drawable.bd2_9, R.drawable.bd2_10
        ),
        listOf(
            R.drawable.bd3_1, R.drawable.bd3_2, R.drawable.bd3_3, R.drawable.bd3_4,
            R.drawable.bd3_5, R.drawable.bd3_6, R.drawable.bd3_7, R.drawable.bd3_8,
            R.drawable.bd3_9, R.drawable.bd3_10
        ),
        listOf(
            R.drawable.bd4_1, R.drawable.bd4_2, R.drawable.bd4_3, R.drawable.bd4_4,
            R.drawable.bd4_5, R.drawable.bd4_6, R.drawable.bd4_7, R.drawable.bd4_8,
            R.drawable.bd4_9, R.drawable.bd4_10
        )
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
        outState.putBoolean("pending_home_overlay", pendingHomeOverlay)
        outState.putBoolean("pending_apps_overlay", pendingAppsOverlay)
        outState.putAll(settings())
        outState.putBoolean("edge_tab", edgeTabSelected)
        outState.putBoolean("edge_enabled", edgeEnabled)
        outState.putBundle("edge_effects", edgeEffects.toBundle())
        outState.putInt("edge_style_group", edgeStyleGroup)
        outState.putInt("edge_style_in_group", edgeStyle)
        outState.putInt("edge_visible_group", visibleEdgeStyleGroup)
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
        displayModeDialog?.dismiss()
        displayModeDialog = null
        edgeColorEditor?.dismiss()
        edgeStyleButtons.clear()
        edgeGroupButtons.clear()
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

package com.kt.flash.light.led.banner.customview

import android.os.Bundle
import com.kt.flash.light.led.banner.R

internal data class LedEdgeSettings(
    val icon: Int = R.drawable.bd1_1,
    val color: Int = 0xFF37C4ED.toInt(),
    val runStyle: Int = 0,
    val topRadius: Int = 16,
    val bottomRadius: Int = 16,
    val borderWidth: Int = 8,
    val colorSpeed: Int = 4,
    val notchStyle: Int = 0
) {
    fun toBundle() = Bundle().apply {
        putInt("icon", icon)
        putInt("color", color)
        putInt("run", runStyle)
        putInt("top", topRadius)
        putInt("bottom", bottomRadius)
        putInt("width", borderWidth)
        putInt("speed", colorSpeed)
        putInt("notch", notchStyle)
    }

    companion object {
        fun fromBundle(bundle: Bundle?): LedEdgeSettings {
            val defaults = LedEdgeSettings()
            return if (bundle == null) defaults else LedEdgeSettings(
                icon = bundle.getInt("icon", defaults.icon),
                color = bundle.getInt("color", defaults.color),
                runStyle = bundle.getInt("run", defaults.runStyle),
                topRadius = bundle.getInt("top", defaults.topRadius),
                bottomRadius = bundle.getInt("bottom", defaults.bottomRadius),
                borderWidth = bundle.getInt("width", defaults.borderWidth),
                colorSpeed = bundle.getInt("speed", defaults.colorSpeed),
                notchStyle = bundle.getInt("notch", defaults.notchStyle)
            )
        }
    }
}

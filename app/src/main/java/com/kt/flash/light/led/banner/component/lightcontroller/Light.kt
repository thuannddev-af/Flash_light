package com.kt.flash.light.led.banner.component.lightcontroller

import com.kt.flash.light.led.banner.data.model.LightMode


internal object Light {
    const val FLASH = 0
    const val SOS = 1
    const val DJ = 2
    private const val UNIT = 200L
    fun steps(mode: Int): List<LightMode> = when (mode) {
        SOS -> listOf(
            //s
            LightMode(true, 200),
            LightMode(false, 200),
            LightMode(true, 200),
            LightMode(false, 200),
            LightMode(true, 200),
            LightMode(false, 600),

            //o
            LightMode(true, 600),
            LightMode(false, 200),
            LightMode(true, 600),
            LightMode(false, 200),
            LightMode(true, 600),
            LightMode(false, 600),

            //s
            LightMode(true, 200),
            LightMode(false, 200),
            LightMode(true, 200),
            LightMode(false, 200),
            LightMode(true, 200),
            LightMode(false, 1400)
        )
        DJ -> listOf(LightMode(true, 150), LightMode(false, 150))
        else -> emptyList()
    }
}

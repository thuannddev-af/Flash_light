package com.kt.flash.light.led.banner.data.model

data class OnBoarding(
    val imageId: Int,
    @androidx.annotation.StringRes val title: Int,
    @androidx.annotation.StringRes val descriptionRes: Int = 0,
    val tag: String = ""
)

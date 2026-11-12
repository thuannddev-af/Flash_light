package com.kt.flash.light.led.banner.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.kt.flash.light.led.banner.utils.Permission

@Keep
data class PermissionModel(
    @DrawableRes
    val iconRes: Int,
    @StringRes
    val title: Int,
    @StringRes
    val content: Int,
    val permissions: List<Permission>,
    var isAllowed: Boolean = false,
)
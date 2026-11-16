package com.kt.flash.light.led.banner.domain.layer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("native_full_splash") @Expose var enableNativeFullSplash: Boolean = true,
    @SerializedName("native_full_splash_2f") @Expose var enableNativeFullSplash2f: Boolean = true,
    @SerializedName("banner_splash") @Expose var enableBannerSplash: Boolean = true,
    @SerializedName("inter_splash") @Expose var enableInterSplash: Boolean = true,
    @SerializedName("inter_splash_2f") @Expose var enableInterSplash2: Boolean = true,


    @SerializedName("native_language") @Expose var enableNativeLanguage: Boolean = true,
    @SerializedName("native_language_2f") @Expose var enableNativeLanguage2f: Boolean = true,
    @SerializedName("native_language_select") @Expose var enableNativeLanguageSelect: Boolean = true,
    @SerializedName("native_language_select_2f") @Expose var enableNativeLanguageSelect2f: Boolean = true,


    @SerializedName("native_obd1") @Expose var enableNativeOnB1: Boolean = true,
    @SerializedName("native_obd1_2f") @Expose var enableNativeOnB12f: Boolean = true,
    @SerializedName("native_obd12_full") @Expose var enableNativeOnB12: Boolean = true,
    @SerializedName("native_obd34_full") @Expose var enableNativeOnB34: Boolean = true,
    @SerializedName("native_obd3") @Expose var enableNativeOnB3: Boolean = true,
    @SerializedName("native_obd3_2f") @Expose var enableNativeOnB32f: Boolean = true,
    @SerializedName("native_permission") @Expose var enableNativePermission: Boolean = true,


    @SerializedName("inter_home") @Expose var enableInterHome: Boolean = true,




    @SerializedName("native_main") @Expose var enableNativeMain: Boolean = true,
    @SerializedName("appopen_resume") @Expose var enableOpenApp: Boolean = true,
    @SerializedName("inter_main") @Expose var enableInterMain: Boolean = true,
    @SerializedName("app_open_main") @Expose var enableAppOpenMain: Boolean = true,
    @SerializedName("reward_unlock") @Expose var enableRewardUnlock: Boolean = true,
    @SerializedName("banner_main") @Expose var enableBannerMain: Boolean = true,

)

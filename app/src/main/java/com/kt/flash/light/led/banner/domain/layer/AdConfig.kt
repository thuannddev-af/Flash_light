package com.kt.flash.light.led.banner.domain.layer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("config_ads") @Expose var configAds: Boolean = true,
    @SerializedName("interval_between_interstitial") @Expose val intervalBetweenInterstitial: Long = 20000L,
    @SerializedName("native_custom") @Expose var enableNativeCustom: Boolean = false,
    @SerializedName("native_full_splash") @Expose var enableNativeFullSplash: Boolean = false,
    @SerializedName("native_full_splash_2f") @Expose var enableNativeFullSplash2f: Boolean = false,
    @SerializedName("banner_splash") @Expose var enableBannerSplash: Boolean = false,
    @SerializedName("inter_splash") @Expose var enableInterSplash: Boolean = false,
    @SerializedName("inter_splash_2f") @Expose var enableInterSplash2: Boolean = false,
    @SerializedName("inter_back") @Expose var enableInterBack: Boolean = false,


    @SerializedName("native_language") @Expose var enableNativeLanguage: Boolean = false,
    @SerializedName("native_language_2f") @Expose var enableNativeLanguage2f: Boolean = false,
    @SerializedName("native_language_select") @Expose var enableNativeLanguageSelect: Boolean = false,
    @SerializedName("native_language_select_2f") @Expose var enableNativeLanguageSelect2f: Boolean = false,


    @SerializedName("native_obd1") @Expose var enableNativeOnB1: Boolean = false,
    @SerializedName("native_obd1_2f") @Expose var enableNativeOnB12f: Boolean = false,
    @SerializedName("native_obd12_full") @Expose var enableNativeOnB12: Boolean = false,
    @SerializedName("native_obd34_full") @Expose var enableNativeOnB34: Boolean = false,
    @SerializedName("native_obd3") @Expose var enableNativeOnB3: Boolean = false,
    @SerializedName("native_obd3_2f") @Expose var enableNativeOnB32f: Boolean = false,
    @SerializedName("native_permission") @Expose var enableNativePermission: Boolean = false,


    @SerializedName("inter_home") @Expose var enableInterHome: Boolean = false,




    @SerializedName("native_main") @Expose var enableNativeMain: Boolean = false,
    @SerializedName("app_open_resume") @Expose var enableOpenApp: Boolean = false,
    @SerializedName("inter_main") @Expose var enableInterMain: Boolean = false,
    @SerializedName("app_open_main") @Expose var enableAppOpenMain: Boolean = false,
    @SerializedName("reward_unlock") @Expose var enableRewardUnlock: Boolean = false,
    @SerializedName("banner_main") @Expose var enableBannerMain: Boolean = false,
    @SerializedName("banner_all") @Expose var enableBannerAll: Boolean = false,


    )

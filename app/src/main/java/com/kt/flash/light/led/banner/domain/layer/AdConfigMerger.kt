package com.kt.flash.light.led.banner.domain.layer

import com.google.gson.Gson
import com.google.gson.JsonParser

internal object AdConfigMerger {
    private val gson = Gson()
    val NON_AD_FLAGS = setOf(
        "config_ads", "language_reopen", "onboarding_reopen", "disable_back"
    )

    fun
            parse(baseJson: String) : AdConfig =
        gson.fromJson(baseJson, AdConfig::class.java)
            ?: throw IllegalArgumentException("null")



    fun merge(organicJson: String, paidJson: String): AdConfig {
        val organic = JsonParser.parseString(organicJson).asJsonObject
        val paid = JsonParser.parseString(paidJson).asJsonObject
        paid.entrySet().forEach { (key, value) -> organic.add(key, value) }
        return Gson().fromJson(organic, AdConfig::class.java)
    }

    fun disableAllAds(config: AdConfig) : AdConfig {
        val json = gson.toJsonTree(config).asJsonObject
        val adKeys = json.entrySet()
            .filter { (key, value) ->
                key !in NON_AD_FLAGS && value.isJsonPrimitive && value.asJsonPrimitive.isBoolean
            }
            .map { it.key }
        adKeys.forEach { json.addProperty(it, false) }
        return  gson.fromJson(json, AdConfig::class.java)
    }
}

package com.ads.admob.config

class AfAdjustConfig private constructor(
    val adjustToken: String,
    val environmentProduct: Boolean,
    val adRevenueKey: String,
    val adRevenueMintegralKey:String,
    val onAttributionChanged: ((network: String?, compaign: String?) -> Unit)?
) {
    class Build(
        private var adjustToken: String,
        private var environmentProduct: Boolean = true,
        private var adRevenueKey: String = "",
        private var adRevenueMintegralKey: String = "",
        private var onAttributionChanged: ((network: String?, compaign: String?) -> Unit)? = null
    ) {
        fun adRevenueKey(key: String) = apply {
            adRevenueKey = key
        }

        fun onAttributionChanged(listener: (network: String?, campaign: String?) -> Unit) = apply {
            onAttributionChanged = listener
        }
        fun environmentProduct(product: Boolean) = apply { environmentProduct = product }
        fun build() = AfAdjustConfig(adjustToken, environmentProduct, adRevenueKey,adRevenueMintegralKey, onAttributionChanged)
    }

}
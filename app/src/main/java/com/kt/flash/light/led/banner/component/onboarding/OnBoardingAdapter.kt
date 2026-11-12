package com.kt.flash.light.led.banner.component.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ads.admob.helper.adnative.NativeAdHelper

import com.kt.flash.light.led.banner.base.adapter.BaseAdapter
import com.kt.flash.light.led.banner.data.model.OnBoarding
import com.kt.flash.light.led.banner.databinding.ItemOnBoardingBinding

class OnBoardingAdapter : BaseAdapter<OnBoarding, ItemOnBoardingBinding>() {

    private val nativeAdHelpers = mutableMapOf<String, NativeAdHelper>()

    fun setNativeAdHelper(tag: String, adHelper: NativeAdHelper) {
        nativeAdHelpers[tag] = adHelper
        val pos = dataSet.indexOfFirst { it.tag == tag }
        if (pos >= 0) notifyItemChanged(pos)
    }

    fun findInsertPosition(afterNonFullscreenCount: Int): Int {
        var count = 0
        for (i in dataSet.indices) {
            if (dataSet[i].imageId != 0) {
                count++
                if (count == afterNonFullscreenCount) return i + 1
            }
        }
        return dataSet.size
    }

    fun insertFullscreenAt(position: Int, tag: String) {
        dataSet.add(position, OnBoarding(imageId = 0, title = 0, tag = tag))
        notifyItemInserted(position)
    }

    fun hasFullscreenItem(tag: String): Boolean = dataSet.any { it.tag == tag }

    override fun binData(viewBinding: ItemOnBoardingBinding, item: OnBoarding, position: Int) {
        if (item.imageId == 0) {
            viewBinding.frAdsNative.isVisible = true
            viewBinding.clOnboarding.isVisible = false
            nativeAdHelpers[item.tag]?.let { adHelper ->
                adHelper.setNativeContentView(viewBinding.frAdsNative)
                adHelper.setShimmerLayoutView(viewBinding.shimmerContainerNative.shimmerContainerNative)
            }
        } else {
            viewBinding.frAdsNative.isVisible = false
            viewBinding.clOnboarding.isVisible = true
            viewBinding.imgBoarding.setImageResource(item.imageId)
            viewBinding.tvTitle.text = viewBinding.root.context.resources.getString(item.title)
            viewBinding.tvDescription.text = item.tag
        }
    }

    override fun provideViewBinding(parent: ViewGroup): ItemOnBoardingBinding = ItemOnBoardingBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
}

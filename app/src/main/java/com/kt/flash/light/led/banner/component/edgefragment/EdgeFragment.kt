package com.kt.flash.light.led.banner.component.edgefragment

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.kt.flash.light.led.banner.utils.ads.NativeAdsUtils
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.component.edgefragment.ScreenLight.ScreenLightController
import com.kt.flash.light.led.banner.databinding.FragmentEdgeBinding
import com.kt.flash.light.led.banner.utils.ads.AdPlacement

class EdgeFragment : BaseFragment<FragmentEdgeBinding>() {
    private var controller: ScreenLightController? = null
    private var edgeState = Bundle()
    private var nativeCustomHelper: NativeAdHelper? = null

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentEdgeBinding.inflate(layoutInflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeState = savedInstanceState?.getBundle(KEY_STATE) ?: Bundle()
    }

    override fun initViews() {
        controller = ScreenLightController(
            requireActivity() as AppCompatActivity,
            viewBaseFragment.edgeContent,
            edgeState
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        controller?.saveState(edgeState)
        outState.putBundle(KEY_STATE, Bundle(edgeState))
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        initNativeNew()
    }

//    private fun initNativeCustom() {
//        if (nativeCustomHelper != null) return
//        nativeCustomHelper = NativeAdsUtils.showNative(
//            fragment = this,
//            placement = com.kt.flash.light.led.banner.utils.ads.AdPlacement.NATIVE_CUSTOM,
//            container = viewBaseFragment.frAdsNative,
//            shimmer = viewBaseFragment.shimmerContainerNative.root,
//            preloadedHelper = NativeAdsUtils.nativeCustom
//        )
//    }

    private fun initNativeNew() {
        NativeAdsUtils.showNative(this, AdPlacement.NATIVE_CUSTOM, viewBaseFragment.frAdsNative, viewBaseFragment.shimmerContainerNative.root)
    }
    override fun onDestroyView() {
        nativeCustomHelper = null
        controller?.apply {
            saveState(edgeState)
            destroy()
        }
        controller = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_STATE = "edge_state"
    }
}

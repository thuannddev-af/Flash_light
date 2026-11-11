package com.kt.flash.light.led.banner.component.edgefragment

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.kt.flash.light.led.banner.base.BaseFragment
import com.kt.flash.light.led.banner.component.edgefragment.ScreenLight.ScreenLightController
import com.kt.flash.light.led.banner.databinding.FragmentEdgeBinding

class EdgeFragment : BaseFragment<FragmentEdgeBinding>() {
    private var controller: ScreenLightController? = null
    private var edgeState = Bundle()

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

    override fun onResume() {
        super.onResume()
        controller?.setActive(true)
    }

    override fun onPause() {
        controller?.setActive(false)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        controller?.saveState(edgeState)
        outState.putBundle(KEY_STATE, Bundle(edgeState))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        controller?.apply {
            saveState(edgeState)
            setActive(false)
            destroy()
        }
        controller = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_STATE = "edge_state"
    }
}

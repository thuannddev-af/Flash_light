package com.kt.flash.light.led.banner.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment <V : ViewBinding>: Fragment(){
    private var binding: V? = null
    val viewBaseFragment: V
        get() = checkNotNull(binding) { "Fragment binding is available only between onCreateView and onDestroyView" }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = provideViewBinding(container)
        return viewBaseFragment.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBaseFragment.root.isClickable = true
        initViews()
        initData()
        initObserver()
    }
    abstract fun provideViewBinding(container: ViewGroup?): V
    open fun initViews() {}
    open fun initData() {}

    open fun initObserver() {}

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

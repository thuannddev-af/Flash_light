package com.kt.flash.light.led.banner.base.activity

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.kt.flash.light.led.banner.utils.Utils
import com.kt.flash.light.led.banner.component.dialog.NoInternetDialog

abstract class BaseActivity <V : ViewBinding> : AppCompatActivity(){
    lateinit var viewBaseActivity: V
    private var networkDialog: NoInternetDialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastConnected: Boolean? = null
    private val connectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onResume() {
        super.onResume()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                refreshConnection(this)
            }
            override fun onLost(network: Network) { refreshConnection(this) }
        }
        networkCallback = callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback
            )
        }
        refreshConnection(callback)
    }

    private fun refreshConnection(callback: ConnectivityManager.NetworkCallback) {
        runOnUiThread {
            if (networkCallback !== callback || isFinishing || isDestroyed) return@runOnUiThread
            val connected = Utils.isConnected(this)
            if (connected) {
                networkDialog?.dismiss()
                networkDialog = null
            } else if (networkDialog?.isShowing != true) {
                networkDialog = NoInternetDialog(this, onCancelClick = { finish() }).also { it.show() }
            }
            if (lastConnected != connected) {
                lastConnected = connected
                onConnectivityChanged(connected)
            }
        }
    }

    protected open fun onConnectivityChanged(connected: Boolean) {}

    override fun onPause() {
        val callback = networkCallback
        networkCallback = null
        if (callback != null) connectivityManager.unregisterNetworkCallback(callback)
        lastConnected = null
        networkDialog?.dismiss()
        networkDialog = null
        super.onPause()
    }

    open fun onBack() {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBaseActivity = provideViewBinding()
        setContentView(viewBaseActivity.root)
        hideSystemUI()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        initViews()
        initObserver()
        initData()
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    }
    private fun hideSystemUI() {
        applyFitsSystemWindows(viewBaseActivity.root.rootView, true)
//        WindowCompat.setDecorFitsSystemWindows(window, true)
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }
    fun setFullscreen() {
        applyFitsSystemWindows(viewBaseActivity.root.rootView, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }
    private fun applyFitsSystemWindows(view: View?, fitSystemWindows: Boolean) {
        if (view is ViewGroup) {
            ViewCompat.setFitsSystemWindows(view, fitSystemWindows)
            for (i in 0 until view.childCount) {
                applyFitsSystemWindows(view.getChildAt(i), fitSystemWindows)
            }
        }
    }



    abstract fun provideViewBinding(): V
    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}
}

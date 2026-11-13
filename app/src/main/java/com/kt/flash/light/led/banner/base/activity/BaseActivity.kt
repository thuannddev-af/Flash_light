package com.kt.flash.light.led.banner.base.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewbinding.ViewBinding
import com.kt.flash.light.led.banner.utils.Utils
import com.kt.flash.light.led.banner.component.dialog.NoInternetDialog
import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.utils.LocaleHelper
import com.kt.flash.light.led.banner.utils.SpManager

abstract class BaseActivity <V : ViewBinding> : AppCompatActivity(){
    lateinit var viewBaseActivity: V

    lateinit var language: Language

    private var networkDialog: NoInternetDialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastConnected: Boolean? = null

    private val connectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
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

    override fun attachBaseContext(newBase: Context) {
        language = SpManager.getInstance(this).getLanguage()
        val context = LocaleHelper.setLocale(newBase, language.languageCode)
        super.attachBaseContext(context)
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
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFullscreenWindow()
        viewBaseActivity = provideViewBinding()
        setContentView(viewBaseActivity.root)
        hideSystemUI()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        initViews()
        initObserver()
        initData()
    }

    private fun configureFullscreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    }
    private fun hideSystemUI() {
        applyFitsSystemWindows(viewBaseActivity.root, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::viewBaseActivity.isInitialized) hideSystemUI()
    }

    fun setFullscreen() {
        hideSystemUI()
    }
    private fun applyFitsSystemWindows(view: View?, fitSystemWindows: Boolean) {
        if (view is ViewGroup) {
            ViewCompat.setFitsSystemWindows(view, fitSystemWindows)
            for (i in 0 until view.childCount) {
                applyFitsSystemWindows(view.getChildAt(i), fitSystemWindows)
            }
        }
    }

    fun replaceFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            replace(id, fragment, fragment::class.java.simpleName)
        }
    }

    fun addFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            add(id, fragment, fragment::class.java.simpleName)
        }
    }


    abstract fun provideViewBinding(): V
    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}
}

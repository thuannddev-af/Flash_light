package com.kt.flash.light.led.banner

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.utils.SpManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Singleton


@HiltAndroidApp
@Singleton
class App : Application(), Application.ActivityLifecycleCallbacks {
    @Inject
    lateinit var spManager: SpManager
    private var isColdStart = true
    private var fristActivityCreated = false

    companion object {
        var context: Context? = null
        private var mInstance: App? = null
        val instance get() = mInstance

        var appResumeAdHelper: AppResumeAdHelper? = null
            private set
    }

//    private fun initAppOpenAd() : AppResumeAdHelper {
//        val listClassInValid = mutableListOf<Class<*>>()
//        listClassInValid.add(AdActivity::class.java)
//        listClassInValid.add(SplashActivity::class.java)
//        val config = AppResumeAdConfig(
//            idAds = BuildConfig.appopen_resume,
//            listClassInValid = listClassInValid,
//            canShowAds = true,
//            networkProvider = NetworkProvider.ADMOB,
//            adPlacement = AdPlacement.OPEN_RESUME,
//            reloadIfFirstFail = true
//        )
//        return AppResumeAdHelper(
//            application = this,
//            lifecycleOwner = ProcessLifecycleOwner.get(),
//            config = config
//        )
//    }

    fun destroyAppOpenAd() {
        appResumeAdHelper?.unregisterAllAdListener()
        appResumeAdHelper?.destroyAds()
        appResumeAdHelper = null
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isColdStart) {
            isColdStart = false
            //handleColdStart(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            destroyAppOpenAd()
        }
    }
}
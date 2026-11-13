package com.kt.flash.light.led.banner

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.NetworkProvider
import com.ads.admob.data.ContentAd
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.helper.appoppen.AppResumeAdConfig
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AppOpenAdCallBack
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.FirebaseApp
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.component.splash.SplashActivity
import com.kt.flash.light.led.banner.utils.Constant
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.LocaleHelper
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
import com.kt.flash.light.led.banner.utils.getDeviceLanguage
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

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        context = applicationContext
        FirebaseApp.initializeApp(this)
        FirebaseTrackingManager.initialize(this)

        FirebaseConfigManager.instance().fetch()
        registerActivityLifecycleCallbacks(this)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
    val locale = getDeviceLanguage()
    val language = spManager.getLanguage()
    LocaleHelper.onAttach(this, language.languageCode)
    super.onConfigurationChanged(newConfig)
}
    fun initConsentManager(activity: Activity, action: () -> Unit) {
        val consentManager = ConsentManager.getInstance(activity)
        consentManager.initReleaseConsent(onConsentResponse = object : OnConsentResponse {
            override fun onResponse(errorMessage: String?) {
                Logger.e("onResponse: $errorMessage -- ${consentManager.canRequestAds} ")
                initOpenResume()
                action.invoke()
            }

            override fun onPolicyRequired(isRequired: Boolean) {
                Logger.e("onPolicyRequired: $isRequired")
                spManager.putBoolean(Constant.KEY_SP_IS_SHOW_UMP_SETTING, isRequired)
            }
        })
    }

    private fun initOpenResume() {
        appResumeAdHelper = initAppOpenAd()
        appResumeAdHelper?.registerAdListener(object : AppOpenAdCallBack {
            override fun onAppOpenAdShow() {
                TODO("Not yet implemented")
            }

            override fun onAppOpenAdClose() {
                TODO("Not yet implemented")
            }

            override fun onAdLoaded(data: ContentAd) {
                TODO("Not yet implemented")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                TODO("Not yet implemented")
            }

            override fun onAdClicked() {
                TODO("Not yet implemented")
            }

            override fun onAdImpression() {
                TODO("Not yet implemented")
            }

            override fun onAdFailedToShow(adError: AdError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun initAppOpenAd() : AppResumeAdHelper {
        val listClassInValid = mutableListOf<Class<*>>()
        listClassInValid.add(AdActivity::class.java)
        listClassInValid.add(SplashActivity::class.java)
        val config = AppResumeAdConfig(
            idAds = BuildConfig.banner_splash,
            listClassInValid = listClassInValid,
            canShowAds = true,
            networkProvider = NetworkProvider.ADMOB,
            adPlacement = AdPlacement.OPEN_RESUME,
            reloadIfFirstFail = true
        )
        return AppResumeAdHelper(
            application = this,
            lifecycleOwner = ProcessLifecycleOwner.get(),
            config = config
        )
    }
    override fun attachBaseContext(base: Context?) {
        val context = LocaleHelper.onAttach(base, java.util.Locale.getDefault().language)
        super.attachBaseContext(context)
    }
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

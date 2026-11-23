package com.kt.flash.light.led.banner

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.data.ContentAd
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.kt.flash.light.led.banner.utils.Constant
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.LocaleHelper
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.ads.AdWindowUtils
import com.kt.flash.light.led.banner.utils.ads.AppOpenAdsUtils
import com.kt.flash.light.led.banner.utils.getDeviceLanguage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Singleton


@HiltAndroidApp
@Singleton
class App : Application(), Application.ActivityLifecycleCallbacks {
    private var resumeHost = java.lang.ref.WeakReference<Activity>(null)

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
        initAdmob()

        initAppsFlyer()
        FirebaseConfigManager.instance().fetch()
        registerActivityLifecycleCallbacks(this)
    }

    private fun initAppsFlyer() {
        val devKey = BuildConfig.APPSFLYER_DEV_KEY
        if (devKey.isBlank()) {
            if (BuildConfig.DEBUG)
                Log.i(
                "INSTALL_SOURCE",
                "AppsFlyer skipped: APPSFLYER_DEV_KEY is empty")
            return
        }

        val listener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>?) {
                val status = conversionData?.get("af_status")?.toString()
                if (BuildConfig.DEBUG) {
                    val source = conversionData?.get("media_source") ?: "unavailable"
                    Log.i("INSTALL_SOURCE", "AppsFlyer af_status=$status, media_source=$source")
                }
                //////////////////////////////////////
                val isPaid = status == "Non-organic"
                spManager. setPaidUser(isPaid)
                runCatching {
                    FirebaseAnalytics.getInstance(applicationContext)
                        .setUserProperty("user_source", if (isPaid) "paid" else "organic")
                }
            }

            override fun onConversionDataFail(errorMessage: String?) {
                if (BuildConfig.DEBUG) Log.w(
                    "INSTALL_SOURCE",
                    "AppsFlyer conversion failed: $errorMessage"
                )
            }

            override fun onAppOpenAttribution(attributionData: MutableMap<String, String>?) {
                Logger.e("AppsFlyer onAppOpenAttribution")
            }

            override fun onAttributionFailure(errorMessage: String?) {
                Logger.e("AppsFlyer app-open attribution failed: $errorMessage")
            }
        }

        try {
            AppsFlyerLib.getInstance().apply {
                init(devKey, listener, this@App)
                start(this@App)
            }
        } catch (error: Exception) {
            Logger.e("AppsFlyer initialization failed: ${error.message}")
        }
    }

    private fun initAdmob() {
        val config = AfAdConfig.Builder(
            afAdjustConfig = AfAdjustConfig.Build(adjustToken = "").build()
        )
            .application(this)
            .intervalBetweenInterstitial(0L)
            .buildVariantProduce(!BuildConfig.DEBUG)
            .build()

        AdmobFactory.INSTANCE.initAdmob(this, config, object : AdmobCallBack {
            override fun initialized() {
                Logger.e("Admob initialized")
            }
        })
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
                FirebaseConfigManager.instance().doAfterFetch { initOpenResume() }
                action.invoke()
            }

            override fun onPolicyRequired(isRequired: Boolean) {
                Logger.e("onPolicyRequired: $isRequired")
                spManager.putBoolean(Constant.KEY_SP_IS_SHOW_UMP_SETTING, isRequired)
            }
        })
    }

    private fun initOpenResume() {
        if (appResumeAdHelper != null) return
        appResumeAdHelper = AppOpenAdsUtils.initResume(this)
        appResumeAdHelper?.registerAdListener(object : AppOpenAdCallBack {
            override fun onAppOpenAdShow() {
                resumeHost.get()?.let { AppOpenAdsUtils.showResumeBackdrop(it) }
                AdmobManager.adsShowFullScreen()
                Logger.e("Resume app-open shown")
            }

            override fun onAppOpenAdClose() {
                AppOpenAdsUtils.hideResumeBackdrop()
                AdmobManager.adsFullScreenDismiss()
                Logger.e("Resume app-open closed; helper preloads the next ad")
            }

            override fun onAdLoaded(data: ContentAd) {
                Logger.e("Resume app-open loaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("Resume app-open load failed: ${loadAdError.message}")
            }

            override fun onAdClicked() {
            }

            override fun onAdImpression() {
            }

            override fun onAdFailedToShow(adError: AdError) {
                AppOpenAdsUtils.hideResumeBackdrop()
                AdmobManager.adsFullScreenDismiss()
                Logger.e("Resume app-open show failed: ${adError.message}")
            }

        })
    }

    override fun attachBaseContext(base: Context?) {
        val context = LocaleHelper.onAttach(base, java.util.Locale.getDefault().language)
        super.attachBaseContext(context)
    }

    fun destroyAppOpenAd() {
        AppOpenAdsUtils.hideResumeBackdrop()
        appResumeAdHelper?.unregisterAllAdListener()
        appResumeAdHelper?.destroyAds()
        appResumeAdHelper = null
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is AdActivity) resumeHost = java.lang.ref.WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        AdWindowUtils.applyCutoutSafeArea(activity)
        if (activity !is AdActivity) resumeHost = java.lang.ref.WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        AdWindowUtils.applyCutoutSafeArea(activity)
        if (isColdStart) {
            isColdStart = false
            //handleColdStart(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        AppOpenAdsUtils.releaseResumeBackdrop(activity)
        if (resumeHost.get() === activity) resumeHost.clear()
        // Resume ads follow the application lifecycle, not a single MainActivity instance.
    }
}

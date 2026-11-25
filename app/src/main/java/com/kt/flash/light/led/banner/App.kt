package com.kt.flash.light.led.banner

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.adjust.sdk.Adjust
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.config.NetworkProvider
import com.ads.admob.data.ContentAd
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.helper.appoppen.AppResumeAdConfig
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.kt.flash.light.led.banner.component.splash.SplashActivity
import com.kt.flash.light.led.banner.utils.Constant
import com.kt.flash.light.led.banner.utils.FirebaseConfigManager
import com.kt.flash.light.led.banner.utils.LocaleHelper
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.UserSource
import com.kt.flash.light.led.banner.utils.ads.AdPlacement
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
//            idAds = BuildConfig.app_open_resume,
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

        FirebaseConfigManager.instance().fetch()
        registerActivityLifecycleCallbacks(this)
    }

    private fun initAdjust(adjustToken: String) {
        if (adjustToken.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.i("INSTALL_SOURCE", "Adjust skipped: app token is empty")
            }
            return
        }

        val paidTrackers = setOf("tokentoken") // Add paid campaign tracker tokens here. =setOf("token")
        Adjust.getAttributionWithTimeout(applicationContext, 5_000L) { attribution ->      //chờ nguồn cài 5 giây

            val network = attribution?.network.orEmpty().trim()
            val tracker = attribution?.trackerToken.orEmpty().trim()
            val isPaid = when {
                network.equals("Organic", ignoreCase = true) -> false
                tracker.isNotEmpty() && tracker in paidTrackers -> true
                else -> null
            }

            if (isPaid != null) {
                spManager.setPaidUser(isPaid)
                runCatching {
                    FirebaseAnalytics.getInstance(applicationContext)
                        .setUserProperty("user_source", if (isPaid) "paid" else "organic")
                }
            }
            if (BuildConfig.DEBUG) {
                Log.i("INSTALL_SOURCE", "Adjust network=$network, tracker=$tracker, paid=$isPaid")
            }
        }
    }

    private fun initAdmob() {
        val adjustConfig = AfAdjustConfig.Build(
            adjustToken = "bzmujs6ffegw",
            adRevenueKey = "",
            environmentProduct = true
        )
            .onAttributionChanged { network, _ ->
                UserSource.onAttribution(this, network)
            }
            .build()

        val afAdjustConfig = AfAdConfig.Builder(afAdjustConfig = adjustConfig)
            .application(this)
            .intervalBetweenInterstitial(0L)
            .buildVariantProduce(!BuildConfig.DEBUG)
            .mediationProvider(NetworkProvider.ADMOB)

            .build()

        AdmobFactory.INSTANCE.initAdmob(this, afAdjustConfig, object : AdmobCallBack {
            override fun initialized() {
                Logger.e("Admob initialized")
                initAdjust(adjustConfig.adjustToken)
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

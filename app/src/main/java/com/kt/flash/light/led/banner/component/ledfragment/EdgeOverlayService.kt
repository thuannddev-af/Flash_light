package com.kt.flash.light.led.banner.component.ledfragment

import android.app.NotificationChannel
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import android.view.View
import androidx.core.app.NotificationCompat
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.customview.LedEdgePreviewView
import com.kt.flash.light.led.banner.customview.LedEdgeSettings

class EdgeOverlayService : Service() {
    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private var overlay: LedEdgePreviewView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
    private val power by lazy { getSystemService(PowerManager::class.java) }
    private val usage by lazy { getSystemService(UsageStatsManager::class.java) }
    private val visibilityCheck = Runnable { updateVisibility() }
    private var homePackage: String? = null
    private var foregroundPackage: String? = null
    private var lastQuery = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val view = overlay ?: LedEdgePreviewView(this, null).also {
                it.visibility = View.INVISIBLE
                windows.addView(it, windowParameters())
                overlay = it
            }
            view.setStyle(LedEdgeSettings.fromBundle(intent?.extras), useDisplayCutout = true)
            homeActive = intent?.getBooleanExtra(HOME, false) == true
            appsActive = intent?.getBooleanExtra(APPS, false) == true
            homePackage = packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName?.takeUnless { it == "android" }
            foregroundPackage = null
            lastQuery = 0
            updateVisibility()
        } catch (_: WindowManager.BadTokenException) {
            stopSelf()
        } catch (_: SecurityException) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updateVisibility() {
        handler.removeCallbacks(visibilityCheck)
        val view = overlay ?: return
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        var show = false
        if (power.isInteractive && !keyguard.isKeyguardLocked) {
            show = if (homeActive && appsActive) true else {
                updateForegroundPackage()
                val foreground = foregroundPackage
                val launcher = homePackage
                foreground != null && launcher != null &&
                    if (foreground == launcher) homeActive else appsActive
            }
        }
        view.visibility = if (show) View.VISIBLE else View.INVISIBLE
        handler.postDelayed(visibilityCheck, if (power.isInteractive) 600 else 1500)
    }

    @Suppress("DEPRECATION")
    private fun updateForegroundPackage() {
        if (!hasUsageAccess(this)) {
            foregroundPackage = null
            lastQuery = 0
            return
        }
        val now = System.currentTimeMillis()
        val from = lastQuery.takeIf { it in 1..now } ?: (now - 300_000)
        val events = usage.queryEvents(from, now) ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackage = event.packageName
            }
        }
        lastQuery = now
    }

    override fun onDestroy() {
        handler.removeCallbacks(visibilityCheck)
        overlay?.let { windows.removeViewImmediate(it) }
        overlay = null
        homeActive = false
        appsActive = false
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun windowParameters() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        alpha = 0.75f
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun notification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.edge_lighting), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stop = PendingIntent.getService(
            this, 0, Intent(this, EdgeOverlayService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_edge_on)
            .setContentTitle(getString(R.string.edge_mode_overlay_notification))
            .setOngoing(true)
            .addAction(0, getString(R.string.edge_mode_stop_overlay), stop)
            .build()
    }

    companion object {
        internal const val HOME = "edge_overlay_home"
        internal const val APPS = "edge_overlay_apps"
        internal var homeActive = false
            private set
        internal var appsActive = false
            private set
        private const val CHANNEL = "edge_lighting_overlay"
        private const val NOTIFICATION_ID = 4301
        private const val STOP = "stop_edge_overlay"

        @Suppress("DEPRECATION")
        internal fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            return ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }
    }
}

package com.kt.flash.light.led.banner.utils.ads

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdActivity
import java.util.WeakHashMap

object AdWindowUtils {
    private val configured = WeakHashMap<View, Boolean>()

    fun applyCutoutSafeArea(activity: Activity) {
        if (activity !is AdActivity) return
        val decor = activity.window.decorView
        if (configured.put(decor, true) == null) {
            val left = decor.paddingLeft
            val top = decor.paddingTop
            val right = decor.paddingRight
            val bottom = decor.paddingBottom
            decor.setBackgroundColor(Color.BLACK)
            ViewCompat.setOnApplyWindowInsetsListener(decor) { view, insets ->
                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                view.setPadding(
                    left + cutout.left,
                    top + cutout.top,
                    right + cutout.right,
                    bottom + cutout.bottom
                )
                // Leave other insets available for the SDK to handle its system bars.
                insets
            }
        }
        decor.post { ViewCompat.requestApplyInsets(decor) }
    }
}

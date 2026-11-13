package com.kt.flash.light.led.banner.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.kt.flash.light.led.banner.R
import kotlin.math.ceil

/** Draw only the selected tab's outer shadow; the existing XML still draws the tab contents. */
class GlowTabBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val blur = resources.getDimension(R.dimen.tab_glow_blur)
    private val corner = resources.getDimension(R.dimen.size12)
    private val shadowPadding = ceil(blur * 2).toInt()
    private var shadow: Bitmap? = null
    private var tabWidth = 0
    private var tabHeight = 0

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        for (index in 0 until childCount) {
            val tab = getChildAt(index)
            if (!tab.isSelected || tab.width == 0 || tab.height == 0) continue
            if (shadow == null || tabWidth != tab.width || tabHeight != tab.height) {
                tabWidth = tab.width
                tabHeight = tab.height
                shadow = createShadow(tabWidth, tabHeight)
            }
            shadow?.let {
                canvas.drawBitmap(it, tab.left + tab.translationX - shadowPadding,
                    tab.top + tab.translationY - shadowPadding, null)
            }
        }
        super.dispatchDraw(canvas)
    }

    private fun createShadow(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width + shadowPadding * 2, height + shadowPadding * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = shadowPadding.toFloat()
        val radius = corner.coerceAtMost(minOf(width, height) / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            setShadowLayer(blur, 0f, 0f, ContextCompat.getColor(context, R.color.tab_glow_shadow))
        }
        canvas.drawRoundRect(inset, inset, inset + width, inset + height, radius, radius, paint)
        paint.clearShadowLayer()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRoundRect(inset, inset, inset + width, inset + height, radius, radius, paint)
        return bitmap
    }

    override fun onDetachedFromWindow() {
        shadow = null
        super.onDetachedFromWindow()
    }
}

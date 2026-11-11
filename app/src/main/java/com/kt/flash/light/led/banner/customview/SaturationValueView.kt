package com.kt.flash.light.led.banner.customview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** The only custom drawing is the interactive HSV color surface. Layout stays in XML. */
class SaturationValueView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    val hsv = floatArrayOf(195f, .68f, .92f)
    var onColorChanged: (() -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerRadius get() = resources.displayMetrics.density * 8f

    init { isClickable = true; isFocusable = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f)), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.displayMetrics.density * 2
        paint.color = Color.WHITE
        val x = (hsv[1] * width).coerceIn(markerRadius, (width - markerRadius).coerceAtLeast(markerRadius))
        val y = ((1 - hsv[2]) * height).coerceIn(markerRadius, (height - markerRadius).coerceAtLeast(markerRadius))
        canvas.drawCircle(x, y, markerRadius, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
            if (width == 0 || height == 0) return false
            parent.requestDisallowInterceptTouchEvent(true)
            hsv[1] = (event.x / width).coerceIn(0f, 1f)
            hsv[2] = 1 - (event.y / height).coerceIn(0f, 1f)
            invalidate()
            onColorChanged?.invoke()
            if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}

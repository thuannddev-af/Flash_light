package com.kt.flash.light.led.banner.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class LedBannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var message = "HELLO WORLD"
    var fontPixels = 64
    var seconds = 5
    var textColor = Color.rgb(255, 213, 79)
    var direction = 1 // right, left, down, up
        set(value) {
            if (field == value) return
            field = value
            resetPosition()
        }
    var effect = 0 // glow, blink, neon
    var running = false
        set(value) { field = value; previousFrame = 0L; invalidate() }
    private var previousFrame = 0L
    private var elapsed = 0f
    private var centerX = Float.NaN
    private var centerY = Float.NaN
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("font/plusjakartasans_medium.ttf", Typeface.BOLD) }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    fun refresh() { contentDescription = message; invalidate() }

    private fun resetPosition() {
        centerX = Float.NaN
        centerY = Float.NaN
        previousFrame = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        val delta = if (running && previousFrame != 0L) (now - previousFrame).coerceAtMost(64) / 1000f else 0f
        elapsed += delta
        previousFrame = now
        if (width == 0 || height == 0) return

        paint.textSize = fontPixels * resources.displayMetrics.density / 2f
        paint.color = textColor
        val blinkOpacity = if (effect == 1 && (elapsed * 2).toInt() % 2 == 1) 35f / 255 else 1f
        paint.alpha = (Color.alpha(textColor) * blinkOpacity).toInt()
        val glow = if (effect == 2) 8f + 6f * sin(elapsed * 3) else 8f
        paint.setShadowLayer(if (effect == 1) 0f else glow * resources.displayMetrics.density, 0f, 0f, textColor)
        val textWidth = paint.measureText(message)
        val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
        if (centerX.isNaN()) centerX = width / 2f
        if (centerY.isNaN()) centerY = height / 2f
        val vertical = direction >= 2
        val travel = if (vertical) height + textHeight else width + textWidth
        val sign = if (direction == 0 || direction == 2) 1f else -1f
        // Integrate only this frame's movement: changing speed never recalculates past travel.
        val distance = delta * travel / seconds.coerceAtLeast(1) * sign
        if (vertical) {
            centerY = wrapPosition(centerY + distance, -textHeight / 2f, travel)
        } else {
            centerX = wrapPosition(centerX + distance, -textWidth / 2f, travel)
        }
        val textX = centerX - textWidth / 2f
        val baseline = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        val save = canvas.save()
        canvas.clipRect(0, 0, width, height)
        if (vertical) {
            for (copy in -1..1) canvas.drawText(message, textX, baseline + copy * travel, paint)
        } else {
            for (copy in -1..1) canvas.drawText(message, textX + copy * travel, baseline, paint)
        }
        canvas.restoreToCount(save)
        if (running && isShown) postInvalidateOnAnimation()
    }

    private fun wrapPosition(position: Float, start: Float, length: Float): Float {
        if (position >= start && position <= start + length) return position
        return start + ((position - start) % length + length) % length
    }
}

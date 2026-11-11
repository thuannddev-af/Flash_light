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
    var effect = 0 // glow, blink, neon
    var running = false
        set(value) { field = value; previousFrame = 0L; invalidate() }
    private var previousFrame = 0L
    private var elapsed = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD) }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    fun refresh() { elapsed = 0f; previousFrame = 0L; contentDescription = message; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        if (running && previousFrame != 0L) elapsed += (now - previousFrame).coerceAtMost(64) / 1000f
        previousFrame = now
        if (width == 0 || height == 0) return
        // Font size is expressed in design pixels, scaled to the device density.
        paint.textSize = fontPixels * resources.displayMetrics.density / 2f
        paint.color = textColor
        paint.alpha = if (effect == 1 && (elapsed * 2).toInt() % 2 == 1) 35 else 255
        val glow = if (effect == 2) 8f + 6f * sin(elapsed * 3) else 8f
        paint.setShadowLayer(if (effect == 1) 0f else glow * resources.displayMetrics.density, 0f, 0f, textColor)
        val textWidth = paint.measureText(message)
        val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
        val centerX = (width - textWidth) / 2f
        val baseline = height / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        val phase = elapsed / seconds.coerceAtLeast(1)
        val vertical = direction >= 2
        val travel = if (vertical) height + textHeight else width + textWidth
        val sign = if (direction == 0 || direction == 2) 1f else -1f
        val offset = (phase * travel) % travel * sign
        val save = canvas.save()
        canvas.clipRect(0, 0, width, height)
        if (vertical) {
            for (copy in -1..1) canvas.drawText(message, centerX, baseline + offset + copy * travel, paint)
        } else {
            for (copy in -1..1) canvas.drawText(message, centerX + offset + copy * travel, baseline, paint)
        }
        canvas.restoreToCount(save)
        if (running && isShown) postInvalidateOnAnimation()
    }
}

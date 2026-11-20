package com.kt.flash.light.led.banner.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin

class LedEdgePreviewView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var settings = LedEdgeSettings()
    private var icon = loadIcon(settings.icon)
    private val density = resources.displayMetrics.density
    private val path = Path()
    private val perimeter = PathMeasure()
    private val position = FloatArray(2)
    private val tangent = FloatArray(2)
    private val startedAt = SystemClock.uptimeMillis()
    private var geometryDirty = true
    private var useDisplayCutout = false

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            geometryDirty = true
            invalidate()
            insets
        }
    }

    private fun loadIcon(resource: Int) =
        checkNotNull(AppCompatResources.getDrawable(context, resource)).mutate()

    internal fun setStyle(updated: LedEdgeSettings, useDisplayCutout: Boolean = false) {
        if (updated.icon != settings.icon) icon = loadIcon(updated.icon)
        settings = updated
        this.useDisplayCutout = useDisplayCutout
        geometryDirty = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        geometryDirty = true
    }

    private fun updatePath(inset: Float) {
        geometryDirty = false
        path.reset()
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        if (right <= left || bottom <= top) {
            perimeter.setPath(path, false)
            return
        }

        val maxRadius = minOf(right - left, bottom - top) / 2
        val topRadius = (settings.topRadius * density).coerceAtMost(maxRadius)
        val bottomRadius = (settings.bottomRadius * density).coerceAtMost(maxRadius)
        val cutout = if (useDisplayCutout) ViewCompat.getRootWindowInsets(this)?.displayCutout else null
        val physicalNotch = cutout?.boundingRects?.firstOrNull {
            it.top <= 0 && it.left > 0 && it.right < width
        }
        val notchDepth = when (settings.notchStyle) {
            1 -> 24 * density
            2, 3 -> 32 * density
            else -> 0f
        }
        val notchCenter = when (settings.notchStyle) {
            2 -> width * 0.2f
            else -> width / 2f
        }
        val notchHalf = if (settings.notchStyle == 1) 13 * density else 43 * density
        val notchLeft = ((physicalNotch?.left?.toFloat() ?: (notchCenter - notchHalf)) - inset)
            .coerceAtLeast(left + topRadius)
        val notchRight = ((physicalNotch?.right?.toFloat() ?: (notchCenter + notchHalf)) + inset)
            .coerceAtMost(right - topRadius)
        val deep = ((physicalNotch?.bottom?.toFloat() ?: notchDepth) + inset)
            .coerceAtMost(bottom - bottomRadius)

        path.moveTo(left + topRadius, top)
        if ((settings.notchStyle != 0 || physicalNotch != null) && notchRight > notchLeft) {
            val shoulder = minOf(10 * density, (notchRight - notchLeft) / 3)
            path.lineTo(notchLeft, top)
            path.cubicTo(notchLeft + shoulder, top, notchLeft, deep, notchLeft + shoulder, deep)
            path.lineTo(notchRight - shoulder, deep)
            path.cubicTo(notchRight, deep, notchRight - shoulder, top, notchRight, top)
        }
        path.lineTo(right - topRadius, top)
        path.quadTo(right, top, right, top + topRadius)
        path.lineTo(right, bottom - bottomRadius)
        path.quadTo(right, bottom, right - bottomRadius, bottom)
        path.lineTo(left + bottomRadius, bottom)
        path.quadTo(left, bottom, left, bottom - bottomRadius)
        path.lineTo(left, top + topRadius)
        path.quadTo(left, top, left + topRadius, top)
        path.close()
        perimeter.setPath(path, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val halfIcon = ((4 + settings.borderWidth) * density).roundToInt().coerceAtLeast(1)
        if (geometryDirty) updatePath(halfIcon * 1.42f)
        val length = perimeter.length
        if (length <= 0) return

        val elapsed = (SystemClock.uptimeMillis() - startedAt) / 1000f
        val count = (length / maxOf(36 * density, halfIcon * 2.4f)).roundToInt().coerceAtLeast(1)
        val run = settings.runStyle
        val direction = if (run > 1 && run % 2 == 1) -1 else 1
        val speed = when (run) {
            4, 5 -> 42f
            6, 7 -> 80f
            2, 3 -> 125f
            else -> 0f
        }
        val offset = elapsed * speed * density * direction
        val base = ColorUtils.setAlphaComponent(settings.color, 255)
        val alpha = Color.alpha(settings.color)
        icon.setBounds(-halfIcon, -halfIcon, halfIcon, halfIcon)

        repeat(count) { index ->
            val distance = (((index + 0.5f) * length / count + offset) % length + length) % length
            if (perimeter.getPosTan(distance, position, tangent)) {
                val shimmer = if (settings.colorSpeed == 0) 0f else
                    (1f + sin(index * 0.6f - elapsed * settings.colorSpeed)) * 0.22f
                icon.setTint(ColorUtils.blendARGB(base, Color.WHITE, shimmer))
                val snake = if (run == 6 || run == 7) {
                    (1f - (index % 10) / 10f).coerceAtLeast(0.15f)
                } else 1f
                icon.alpha = (alpha * snake).roundToInt()
                val rotation = Math.toDegrees(atan2(tangent[1], tangent[0]).toDouble()).toFloat()
                val saved = canvas.save()
                canvas.translate(position[0], position[1])
                canvas.rotate(rotation + if (run == 1) elapsed * 150f else 0f)
                icon.draw(canvas)
                canvas.restoreToCount(saved)
            }
        }
        if (isShown && (run != 0 || settings.colorSpeed > 0)) postInvalidateOnAnimation()
    }
}

package com.kt.flash.light.led.banner.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.kt.flash.light.led.banner.R
import kotlin.math.roundToInt

class LedEdgePreviewView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var iconResource = R.drawable.ic_led_edge_butterfly
    private var icon = loadIcon(iconResource)
    private val step = 36 * resources.displayMetrics.density
    private val halfIcon = (12 * resources.displayMetrics.density).roundToInt()

    private fun loadIcon(resource: Int) =
        checkNotNull(AppCompatResources.getDrawable(context, resource)).mutate()

    fun setStyle(resource: Int, color: Int) {
        if (resource != iconResource) {
            iconResource = resource
            icon = loadIcon(resource)
        }
        icon.setTint(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)))
        icon.alpha = Color.alpha(color)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = halfIcon.toFloat()
        val right = width - inset
        val bottom = height - inset
        if (right <= inset || bottom <= inset) return

        drawSide(canvas, inset, inset, right, inset, 0f)
        drawSide(canvas, right, inset, right, bottom, 90f)
        drawSide(canvas, right, bottom, inset, bottom, 180f)
        drawSide(canvas, inset, bottom, inset, inset, 270f)
    }

    private fun drawSide(canvas: Canvas, x: Float, y: Float, endX: Float, endY: Float, rotation: Float) {
        val length = maxOf(kotlin.math.abs(endX - x), kotlin.math.abs(endY - y))
        val count = (length / step).roundToInt().coerceAtLeast(1)
        icon.setBounds(-halfIcon, -halfIcon, halfIcon, halfIcon)
        repeat(count) { index ->
            val fraction = (index + 0.5f) / count
            val saveCount = canvas.save()
            canvas.translate(x + (endX - x) * fraction, y + (endY - y) * fraction)
            canvas.rotate(rotation)
            icon.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }
}

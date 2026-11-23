package com.banphim.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Orientation {
        VERTICAL,
        HORIZONTAL
    }

    var orientation: Orientation = Orientation.VERTICAL
        set(value) {
            field = value
            invalidate()
        }

    var colors: List<Int> = listOf(
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#E0E0E0"),
        Color.parseColor("#424242"),
        Color.parseColor("#111111"),
        Color.parseColor("#E53935"),
        Color.parseColor("#FB8C00"),
        Color.parseColor("#FDD835"),
        Color.parseColor("#43A047"),
        Color.parseColor("#1E88E5"),
        Color.parseColor("#8E24AA")
    )
        set(value) {
            field = value
            invalidate()
        }

    var onColorSelected: ((Int) -> Unit)? = null

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#66000000")
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val clipPath = Path()
    private val boundsRect = RectF()

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (colors.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = minOf(w, h) / 2f

        boundsRect.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addRoundRect(boundsRect, radius, radius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        val count = colors.size
        if (orientation == Orientation.VERTICAL) {
            val step = h / count
            for (i in 0 until count) {
                segmentPaint.color = colors[i]
                canvas.drawRect(0f, i * step, w, (i + 1) * step, segmentPaint)
            }
        } else {
            val step = w / count
            for (i in 0 until count) {
                segmentPaint.color = colors[i]
                canvas.drawRect(i * step, 0f, (i + 1) * step, h, segmentPaint)
            }
        }

        canvas.restore()
        val inset = borderPaint.strokeWidth / 2f
        boundsRect.inset(inset, inset)
        canvas.drawRoundRect(boundsRect, radius, radius, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (colors.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pickColorAt(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                pickColorAt(event.x, event.y)
                hide()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                hide()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun pickColorAt(x: Float, y: Float) {
        val ratio = if (orientation == Orientation.VERTICAL) {
            if (height <= 0) 0f else y / height.toFloat()
        } else {
            if (width <= 0) 0f else x / width.toFloat()
        }
        val index = (ratio * colors.size).toInt().coerceIn(0, colors.size - 1)
        onColorSelected?.invoke(colors[index])
    }
}

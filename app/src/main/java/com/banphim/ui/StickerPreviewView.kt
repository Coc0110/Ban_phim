package com.banphim.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.banphim.drawing.PenStyle
import com.banphim.drawing.PenStyleRenderer

class StickerPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var penStyle: PenStyle = PenStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    var showGrid: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var outlineEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var outlineColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.parseColor("#33888888")
    }
    private val gridPaint = Paint().apply {
        strokeWidth = 1f * density
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val cardRect = RectF()
    private val clipPath = Path()
    private val letteringPath = Path()

    fun effectiveCanvasColor(): Int {
        if (!outlineEnabled) return Color.WHITE
        val lum = (0.299 * Color.red(outlineColor) +
            0.587 * Color.green(outlineColor) +
            0.114 * Color.blue(outlineColor)) / 255.0
        return if (lum > 0.62) Color.parseColor("#1C1E22") else Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val canvasColor = effectiveCanvasColor()
        val isDark = canvasColor != Color.WHITE
        val corner = 16f * density
        cardRect.set(1f * density, 1f * density, w - 1f * density, h - 1f * density)

        clipPath.reset()
        clipPath.addRoundRect(cardRect, corner, corner, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        bgPaint.color = canvasColor
        canvas.drawRect(cardRect, bgPaint)

        if (showGrid) {
            gridPaint.color = if (isDark) Color.argb(20, 255, 255, 255) else Color.argb(20, 0, 0, 0)
            val step = 24f * density
            var x = step
            while (x < w) {
                canvas.drawLine(x, 0f, x, h, gridPaint)
                x += step
            }
            var y = step
            while (y < h) {
                canvas.drawLine(0f, y, w, y, gridPaint)
                y += step
            }
        }

        buildAlooPath(w, h)
        val strokeW = 7f * density
        if (outlineEnabled) {
            PenStyleRenderer.drawOutline(
                canvas = canvas,
                path = letteringPath,
                color = outlineColor,
                baseStrokeWidth = strokeW,
                outlineExtraWidth = 5.5f * density,
                style = penStyle,
                mainPaint = outlinePaint,
                workPaint = glowPaint
            )
        }
        PenStyleRenderer.drawStroke(
            canvas = canvas,
            path = letteringPath,
            color = Color.parseColor("#1E88E5"),
            strokeWidth = strokeW,
            style = penStyle,
            mainPaint = mainPaint,
            glowPaint = glowPaint
        )

        canvas.restore()
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
    }

    private fun buildAlooPath(w: Float, h: Float) {
        letteringPath.reset()
        val left = w * 0.10f
        val span = w * 0.78f
        fun x(fraction: Float) = left + span * fraction

        // Slanted hand-drawn capital "A" rising toward the top-right.
        letteringPath.moveTo(x(0.05f), h * 0.80f)
        letteringPath.cubicTo(x(0.08f), h * 0.64f, x(0.11f), h * 0.45f, x(0.15f), h * 0.44f)
        letteringPath.cubicTo(x(0.19f), h * 0.43f, x(0.22f), h * 0.60f, x(0.25f), h * 0.72f)
        // Sweeping crossbar extending slightly past the left leg.
        letteringPath.moveTo(x(0.01f), h * 0.66f)
        letteringPath.cubicTo(x(0.08f), h * 0.64f, x(0.17f), h * 0.60f, x(0.24f), h * 0.58f)

        // Tall cursive "l" flowing into three connected, ascending "o" loops.
        letteringPath.moveTo(x(0.26f), h * 0.67f)
        letteringPath.cubicTo(x(0.28f), h * 0.54f, x(0.27f), h * 0.34f, x(0.31f), h * 0.34f)
        letteringPath.cubicTo(x(0.35f), h * 0.34f, x(0.31f), h * 0.57f, x(0.35f), h * 0.63f)
        letteringPath.cubicTo(x(0.38f), h * 0.68f, x(0.41f), h * 0.60f, x(0.43f), h * 0.54f)

        // First "o"
        letteringPath.cubicTo(x(0.44f), h * 0.46f, x(0.52f), h * 0.45f, x(0.53f), h * 0.53f)
        letteringPath.cubicTo(x(0.54f), h * 0.62f, x(0.46f), h * 0.65f, x(0.43f), h * 0.58f)
        letteringPath.cubicTo(x(0.41f), h * 0.53f, x(0.45f), h * 0.47f, x(0.51f), h * 0.48f)

        // Second "o"
        letteringPath.cubicTo(x(0.56f), h * 0.40f, x(0.64f), h * 0.39f, x(0.65f), h * 0.47f)
        letteringPath.cubicTo(x(0.66f), h * 0.56f, x(0.58f), h * 0.59f, x(0.55f), h * 0.52f)
        letteringPath.cubicTo(x(0.53f), h * 0.47f, x(0.57f), h * 0.41f, x(0.63f), h * 0.42f)

        // Third "o"
        letteringPath.cubicTo(x(0.68f), h * 0.34f, x(0.76f), h * 0.33f, x(0.77f), h * 0.41f)
        letteringPath.cubicTo(x(0.78f), h * 0.50f, x(0.70f), h * 0.53f, x(0.67f), h * 0.46f)
        letteringPath.cubicTo(x(0.65f), h * 0.41f, x(0.69f), h * 0.35f, x(0.74f), h * 0.36f)

        // Hand-drawn 5-pointed star above the last "o".
        val sx = x(0.75f)
        val sy = h * 0.21f
        val rOuter = h * 0.085f
        val rInner = h * 0.038f
        val starTilt = -0.22
        for (i in 0..10) {
            val idx = i % 10
            val r = if (idx % 2 == 0) rOuter else rInner
            val angle = starTilt - Math.PI / 2.0 + idx * (Math.PI / 5.0)
            val px = sx + (kotlin.math.cos(angle) * r).toFloat()
            val py = sy + (kotlin.math.sin(angle) * r).toFloat()
            if (i == 0) letteringPath.moveTo(px, py) else letteringPath.lineTo(px, py)
        }
        letteringPath.moveTo(sx - 1f * density, sy)
        letteringPath.lineTo(sx + 1f * density, sy + 1f * density)

        // Exclamation mark on the right.
        letteringPath.moveTo(x(0.92f), h * 0.21f)
        letteringPath.cubicTo(x(0.91f), h * 0.31f, x(0.90f), h * 0.40f, x(0.90f), h * 0.46f)
        letteringPath.moveTo(x(0.89f), h * 0.55f)
        letteringPath.cubicTo(x(0.88f), h * 0.57f, x(0.90f), h * 0.59f, x(0.91f), h * 0.57f)
    }
}

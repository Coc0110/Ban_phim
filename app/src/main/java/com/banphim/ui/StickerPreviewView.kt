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
    private val aloooPath = Path()

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

        buildAloooPath(w, h)
        val strokeW = 7f * density
        val inkColor = Color.parseColor("#1E88E5")

        if (outlineEnabled) {
            outlinePaint.color = outlineColor
            outlinePaint.strokeWidth = strokeW + 6f * density
            canvas.drawPath(aloooPath, outlinePaint)
        }

        PenStyleRenderer.drawStroke(
            canvas = canvas,
            path = aloooPath,
            color = inkColor,
            strokeWidth = strokeW,
            style = penStyle,
            mainPaint = mainPaint,
            glowPaint = glowPaint
        )

        canvas.restore()
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
    }

    private fun buildAloooPath(w: Float, h: Float) {
        aloooPath.reset()
        val baseY = h * 0.70f
        val topY = h * 0.28f
        val midY = h * 0.52f
        val startX = w * 0.14f
        val unit = w * 0.105f

        // 'A'
        aloooPath.moveTo(startX, baseY)
        aloooPath.lineTo(startX + unit * 0.55f, topY)
        aloooPath.lineTo(startX + unit * 1.1f, baseY)
        aloooPath.moveTo(startX + unit * 0.22f, midY + 4f * density)
        aloooPath.lineTo(startX + unit * 0.90f, midY + 4f * density)

        // 'l'
        val lx = startX + unit * 1.55f
        aloooPath.moveTo(lx, topY)
        aloooPath.lineTo(lx, baseY)

        // 'o' 'o' 'o'
        val r = unit * 0.36f
        val oy = baseY - r
        aloooPath.addCircle(startX + unit * 2.35f, oy, r, Path.Direction.CW)
        aloooPath.addCircle(startX + unit * 3.35f, oy, r, Path.Direction.CW)
        aloooPath.addCircle(startX + unit * 4.35f, oy, r, Path.Direction.CW)

        // '!!'
        val ex1 = startX + unit * 5.25f
        val ex2 = startX + unit * 5.85f
        aloooPath.moveTo(ex1, topY)
        aloooPath.lineTo(ex1, baseY - 12f * density)
        aloooPath.moveTo(ex1, baseY - 2f * density)
        aloooPath.lineTo(ex1, baseY)

        aloooPath.moveTo(ex2, topY)
        aloooPath.lineTo(ex2, baseY - 12f * density)
        aloooPath.moveTo(ex2, baseY - 2f * density)
        aloooPath.lineTo(ex2, baseY)
    }
}

package com.banphim.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.banphim.R
import com.banphim.drawing.PenStyle
import com.banphim.drawing.PenStyleRenderer

class BrushSampleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var penStyle: PenStyle = PenStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    var isBrushSelected: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val bgColor = ContextCompat.getColor(context, R.color.input_bg)
    private val defaultStrokeColor = ContextCompat.getColor(context, R.color.text_primary)
    private val defaultBorderColor = ContextCompat.getColor(context, R.color.card_stroke)
    private val selectedBorderColor = ContextCompat.getColor(context, R.color.send_bg)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
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
    private val rect = RectF()
    private val wavePath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val corner = 12f * density
        val inset = 2f * density
        rect.set(inset, inset, w - inset, h - inset)
        canvas.drawRoundRect(rect, corner, corner, bgPaint)

        borderPaint.color = if (isBrushSelected) selectedBorderColor else defaultBorderColor
        borderPaint.strokeWidth = (if (isBrushSelected) 2.2f else 1f) * density
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        val padX = w * 0.18f
        val midY = h * 0.5f
        val amp = h * 0.18f
        wavePath.reset()
        wavePath.moveTo(padX, midY)
        wavePath.cubicTo(
            padX + (w - 2 * padX) * 0.25f, midY - amp,
            padX + (w - 2 * padX) * 0.45f, midY + amp,
            w * 0.5f, midY
        )
        wavePath.cubicTo(
            padX + (w - 2 * padX) * 0.65f, midY - amp,
            padX + (w - 2 * padX) * 0.80f, midY + amp,
            w - padX, midY - amp * 0.2f
        )

        PenStyleRenderer.drawStroke(
            canvas = canvas,
            path = wavePath,
            color = defaultStrokeColor,
            strokeWidth = 4.5f * density,
            style = penStyle,
            mainPaint = mainPaint,
            glowPaint = glowPaint
        )
    }
}

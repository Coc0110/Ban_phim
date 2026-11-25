package com.banphim.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposePathEffect
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.Paint
import android.graphics.Path

object PenStyleRenderer {

    fun drawStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        style: PenStyle,
        mainPaint: Paint,
        glowPaint: Paint
    ) {
        if (style == PenStyle.GLOW) {
            glowPaint.color = Color.argb(85, Color.red(color), Color.green(color), Color.blue(color))
            glowPaint.strokeWidth = strokeWidth * 2.1f
            glowPaint.pathEffect = null
            canvas.drawPath(path, glowPaint)
        }

        mainPaint.color = color
        mainPaint.strokeWidth = strokeWidth
        mainPaint.pathEffect = when (style) {
            PenStyle.NORMAL, PenStyle.GLOW -> null
            PenStyle.DASHED -> DashPathEffect(floatArrayOf(strokeWidth * 1.4f, strokeWidth * 1.4f), 0f)
            PenStyle.CHALK -> ComposePathEffect(
                DiscretePathEffect(strokeWidth * 0.45f, strokeWidth * 0.22f),
                DashPathEffect(floatArrayOf(strokeWidth * 0.6f, strokeWidth * 0.22f), 0f)
            )
        }
        canvas.drawPath(path, mainPaint)
    }
}

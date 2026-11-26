package com.banphim.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposePathEffect
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PenStyleRenderer {

    private const val FOUNTAIN_NIB_ANGLE_RAD = -Math.PI / 4.0

    fun drawStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        style: PenStyle,
        mainPaint: Paint,
        glowPaint: Paint
    ) {
        when (style) {
            PenStyle.NORMAL -> {
                mainPaint.color = color
                mainPaint.alpha = 255
                mainPaint.strokeWidth = strokeWidth
                mainPaint.pathEffect = null
                canvas.drawPath(path, mainPaint)
            }
            PenStyle.BRUSH -> drawBrushStroke(canvas, path, color, strokeWidth, 0f, glowPaint)
            PenStyle.PENCIL -> drawPencilStroke(canvas, path, color, strokeWidth, mainPaint)
            PenStyle.FOUNTAIN -> drawFountainStroke(canvas, path, color, strokeWidth, 0f, glowPaint)
        }
    }

    fun drawOutline(
        canvas: Canvas,
        path: Path,
        color: Int,
        baseStrokeWidth: Float,
        outlineExtraWidth: Float,
        style: PenStyle,
        mainPaint: Paint,
        workPaint: Paint
    ) {
        if (outlineExtraWidth <= 0f) return
        when (style) {
            PenStyle.NORMAL -> {
                mainPaint.color = color
                mainPaint.alpha = 255
                mainPaint.strokeWidth = baseStrokeWidth + outlineExtraWidth
                mainPaint.pathEffect = null
                canvas.drawPath(path, mainPaint)
            }
            PenStyle.BRUSH -> drawBrushStroke(canvas, path, color, baseStrokeWidth, outlineExtraWidth, workPaint)
            PenStyle.PENCIL -> {
                mainPaint.color = color
                mainPaint.alpha = 255
                mainPaint.strokeWidth = baseStrokeWidth + outlineExtraWidth
                mainPaint.pathEffect = DiscretePathEffect(
                    max(1f, baseStrokeWidth * 0.28f),
                    max(0.5f, baseStrokeWidth * 0.14f)
                )
                canvas.drawPath(path, mainPaint)
                mainPaint.pathEffect = null
            }
            PenStyle.FOUNTAIN -> drawFountainStroke(canvas, path, color, baseStrokeWidth, outlineExtraWidth, workPaint)
        }
    }

    private fun drawBrushStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        outlineExtra: Float,
        workPaint: Paint
    ) {
        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        val tan = FloatArray(2)

        val prevStyle = workPaint.style
        val prevCap = workPaint.strokeCap
        workPaint.color = color
        workPaint.pathEffect = null
        workPaint.style = Paint.Style.FILL

        var drewAny = false
        do {
            val length = pm.length
            if (length < 1f) {
                if (pm.getPosTan(0f, pos, tan)) {
                    canvas.drawCircle(pos[0], pos[1], (strokeWidth + outlineExtra) * 0.5f, workPaint)
                    drewAny = true
                }
                continue
            }

            drewAny = true
            val step = max(1f, strokeWidth * 0.10f)
            var d = 0f
            while (d <= length) {
                if (pm.getPosTan(d, pos, tan)) {
                    val t = d / length
                    // Calligraphy brush: thin upstrokes (tanY < 0), bold downstrokes (tanY > 0)
                    val downFactor = (tan[1] + 1f) * 0.5f
                    val smoothDir = downFactor * downFactor * (3f - 2f * downFactor)
                    val dirWeight = 0.26f + 1.24f * smoothDir

                    val taperIn = min(1f, t / 0.08f)
                    val taperOut = min(1f, (1f - t) / 0.10f)
                    val taper = 0.35f + 0.65f * min(taperIn, taperOut)

                    val diameter = strokeWidth * dirWeight * taper + outlineExtra
                    canvas.drawCircle(pos[0], pos[1], diameter * 0.5f, workPaint)
                }
                d += step
            }
            if (pm.getPosTan(length, pos, tan)) {
                val downFactor = (tan[1] + 1f) * 0.5f
                val smoothDir = downFactor * downFactor * (3f - 2f * downFactor)
                val diameter = strokeWidth * (0.26f + 1.24f * smoothDir) * 0.35f + outlineExtra
                canvas.drawCircle(pos[0], pos[1], diameter * 0.5f, workPaint)
            }
        } while (pm.nextContour())

        if (!drewAny) {
            workPaint.style = Paint.Style.STROKE
            workPaint.strokeWidth = strokeWidth + outlineExtra
            canvas.drawPath(path, workPaint)
        }

        workPaint.style = prevStyle
        workPaint.strokeCap = prevCap
    }

    private fun drawPencilStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        mainPaint: Paint
    ) {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val baseAlpha = Color.alpha(color)

        // Soft pigment core with subtle paper-edge irregularity
        mainPaint.color = Color.argb((baseAlpha * 0.68f).toInt(), r, g, b)
        mainPaint.strokeWidth = strokeWidth
        mainPaint.pathEffect = DiscretePathEffect(
            max(1f, strokeWidth * 0.28f),
            max(0.5f, strokeWidth * 0.14f)
        )
        canvas.drawPath(path, mainPaint)

        // Coarse paper-tooth grain pass
        mainPaint.color = Color.argb((baseAlpha * 0.45f).toInt(), r, g, b)
        mainPaint.strokeWidth = max(1f, strokeWidth * 0.52f)
        mainPaint.pathEffect = ComposePathEffect(
            DiscretePathEffect(max(1f, strokeWidth * 0.22f), max(0.6f, strokeWidth * 0.22f)),
            DashPathEffect(floatArrayOf(max(1f, strokeWidth * 0.35f), max(0.8f, strokeWidth * 0.22f)), 0f)
        )
        canvas.drawPath(path, mainPaint)

        // Fine wax/graphite flecks
        mainPaint.color = Color.argb((baseAlpha * 0.32f).toInt(), r, g, b)
        mainPaint.strokeWidth = max(1f, strokeWidth * 0.32f)
        mainPaint.pathEffect = ComposePathEffect(
            DiscretePathEffect(max(1f, strokeWidth * 0.18f), max(0.8f, strokeWidth * 0.28f)),
            DashPathEffect(
                floatArrayOf(max(0.8f, strokeWidth * 0.20f), max(1f, strokeWidth * 0.28f)),
                strokeWidth * 0.15f
            )
        )
        canvas.drawPath(path, mainPaint)

        mainPaint.alpha = 255
        mainPaint.pathEffect = null
    }

    private fun drawFountainStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        outlineExtra: Float,
        workPaint: Paint
    ) {
        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        val nibHalfLen = strokeWidth * 0.56f
        val nibDx = (cos(FOUNTAIN_NIB_ANGLE_RAD) * nibHalfLen).toFloat()
        val nibDy = (sin(FOUNTAIN_NIB_ANGLE_RAD) * nibHalfLen).toFloat()
        val nibThickness = max(1.4f, strokeWidth * 0.24f) + outlineExtra

        val prevStyle = workPaint.style
        val prevCap = workPaint.strokeCap
        workPaint.color = color
        workPaint.style = Paint.Style.STROKE
        workPaint.strokeCap = Paint.Cap.ROUND
        workPaint.strokeWidth = nibThickness
        workPaint.pathEffect = null

        var drewAny = false
        do {
            val length = pm.length
            if (length < 1f) {
                if (pm.getPosTan(0f, pos, null)) {
                    canvas.drawLine(
                        pos[0] - nibDx, pos[1] - nibDy,
                        pos[0] + nibDx, pos[1] + nibDy,
                        workPaint
                    )
                    drewAny = true
                }
                continue
            }

            drewAny = true
            val step = max(1f, strokeWidth * 0.11f)
            var d = 0f
            while (d <= length) {
                if (pm.getPosTan(d, pos, null)) {
                    canvas.drawLine(
                        pos[0] - nibDx, pos[1] - nibDy,
                        pos[0] + nibDx, pos[1] + nibDy,
                        workPaint
                    )
                }
                d += step
            }
            if (pm.getPosTan(length, pos, null)) {
                canvas.drawLine(
                    pos[0] - nibDx, pos[1] - nibDy,
                    pos[0] + nibDx, pos[1] + nibDy,
                    workPaint
                )
            }
        } while (pm.nextContour())

        if (!drewAny) {
            workPaint.strokeWidth = strokeWidth + outlineExtra
            canvas.drawPath(path, workPaint)
        }

        workPaint.style = prevStyle
        workPaint.strokeCap = prevCap
    }
}

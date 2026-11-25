package com.banphim.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.banphim.R
import kotlin.math.cos
import kotlin.math.sin

class GridPaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val paperColor: Int = ContextCompat.getColor(context, R.color.paper_bg)
    val gridLineColor: Int = ContextCompat.getColor(context, R.color.paper_grid_line)

    private val density = resources.displayMetrics.density
    private val stepPx = 28f * density
    private val isDarkPaper = (0.299 * Color.red(paperColor) +
        0.587 * Color.green(paperColor) +
        0.114 * Color.blue(paperColor)) < 128.0

    private val gridPaint = Paint().apply {
        color = gridLineColor
        strokeWidth = 1f * density
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 7f * density
    }

    private val doodlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3.2f * density
    }

    private val reusablePath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(paperColor)

        var x = stepPx
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += stepPx
        }
        var y = stepPx
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += stepPx
        }

        if (width > 0 && height > 0) {
            drawDoodles(canvas)
        }
    }

    private fun drawDoodles(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Hearts
        drawHeart(canvas, w * 0.10f, h * 0.28f, 28f * density, -14f,
            Color.parseColor("#E6C24D"), Color.parseColor("#6EC6E6"))
        drawHeart(canvas, w * 0.86f, h * 0.22f, 22f * density, 12f,
            Color.parseColor("#F48FB1"), Color.parseColor("#FFAB91"))
        drawHeart(canvas, w * 0.88f, h * 0.78f, 20f * density, -10f,
            Color.parseColor("#CE93D8"), Color.parseColor("#80DEEA"))

        // Stars
        drawStar(canvas, w * 0.14f, h * 0.72f, 24f * density, 15f,
            Color.parseColor("#FFD54F"), Color.parseColor("#64B5F6"))
        drawStar(canvas, w * 0.85f, h * 0.50f, 19f * density, -18f,
            Color.parseColor("#81C784"), Color.parseColor("#FFF176"))
        drawStar(canvas, w * 0.48f, h * 0.90f, 17f * density, 8f,
            Color.parseColor("#FF8A65"), Color.parseColor("#F06292"))

        // Squiggles & loops
        drawSquiggle(canvas, w * 0.06f, h * 0.50f, 54f * density, -8f,
            Color.parseColor("#80CBC4"), Color.parseColor("#64B5F6"))
        drawSquiggle(canvas, w * 0.74f, h * 0.36f, 48f * density, 18f,
            Color.parseColor("#FFB74D"), Color.parseColor("#F48FB1"))
        drawLoop(canvas, w * 0.22f, h * 0.88f, 22f * density, -12f,
            Color.parseColor("#BA68C8"), Color.parseColor("#4FC3F7"))

        // Sparkles & spiral
        drawSparkle(canvas, w * 0.90f, h * 0.64f, 16f * density,
            Color.parseColor("#FFD54F"), Color.parseColor("#FF8A65"))
        drawSparkle(canvas, w * 0.08f, h * 0.14f, 14f * density,
            Color.parseColor("#81D4FA"), Color.parseColor("#A5D6A7"))
        drawSpiral(canvas, w * 0.14f, h * 0.40f, 16f * density,
            Color.parseColor("#F48FB1"), Color.parseColor("#CE93D8"))
        drawSmiley(canvas, w * 0.86f, h * 0.89f, 17f * density,
            Color.parseColor("#FFB74D"), Color.parseColor("#81C784"))
    }

    private fun strokeWithHalo(canvas: Canvas, path: Path, cx: Float, cy: Float, radius: Float, c1: Int, c2: Int) {
        val alphaMain = if (isDarkPaper) 90 else 115
        val alphaHalo = if (isDarkPaper) 26 else 34
        val start = Color.argb(alphaMain, Color.red(c1), Color.green(c1), Color.blue(c1))
        val end = Color.argb(alphaMain, Color.red(c2), Color.green(c2), Color.blue(c2))
        val haloStart = Color.argb(alphaHalo, Color.red(c1), Color.green(c1), Color.blue(c1))
        val haloEnd = Color.argb(alphaHalo, Color.red(c2), Color.green(c2), Color.blue(c2))

        haloPaint.shader = LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius, haloStart, haloEnd, Shader.TileMode.CLAMP)
        doodlePaint.shader = LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius, start, end, Shader.TileMode.CLAMP)

        canvas.drawPath(path, haloPaint)
        canvas.drawPath(path, doodlePaint)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, angleDeg: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        reusablePath.moveTo(0f, size * 0.35f)
        reusablePath.cubicTo(-size * 0.9f, -size * 0.25f, -size * 0.55f, -size * 0.95f, 0f, -size * 0.35f)
        reusablePath.cubicTo(size * 0.55f, -size * 0.95f, size * 0.9f, -size * 0.25f, 0f, size * 0.5f)

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, size, c1, c2)
        canvas.restore()
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, angleDeg: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        val points = 5
        val order = intArrayOf(0, 2, 4, 1, 3)
        for (i in order.indices) {
            val theta = Math.toRadians((order[i] * 360.0 / points) - 90.0)
            val px = (radius * cos(theta)).toFloat()
            val py = (radius * sin(theta)).toFloat()
            if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
        }
        reusablePath.close()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, radius, c1, c2)
        canvas.restore()
    }

    private fun drawSquiggle(canvas: Canvas, cx: Float, cy: Float, widthPx: Float, angleDeg: Float, c1: Int, c2: Int) {
        val h = widthPx * 0.22f
        val seg = widthPx / 3f
        reusablePath.reset()
        reusablePath.moveTo(-widthPx / 2f, 0f)
        reusablePath.quadTo(-widthPx / 2f + seg * 0.5f, -h, -widthPx / 2f + seg, 0f)
        reusablePath.quadTo(-widthPx / 2f + seg * 1.5f, h, -widthPx / 2f + seg * 2f, 0f)
        reusablePath.quadTo(-widthPx / 2f + seg * 2.5f, -h, widthPx / 2f, 0f)

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, widthPx * 0.5f, c1, c2)
        canvas.restore()
    }

    private fun drawLoop(canvas: Canvas, cx: Float, cy: Float, size: Float, angleDeg: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        reusablePath.moveTo(-size, size * 0.2f)
        reusablePath.cubicTo(-size * 0.2f, -size, size * 0.7f, -size * 0.8f, 0f, size * 0.1f)
        reusablePath.cubicTo(-size * 0.5f, size * 0.7f, size * 0.5f, size * 0.7f, size, -size * 0.2f)

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, size, c1, c2)
        canvas.restore()
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, r: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        reusablePath.moveTo(0f, -r)
        reusablePath.quadTo(0f, 0f, r, 0f)
        reusablePath.quadTo(0f, 0f, 0f, r)
        reusablePath.quadTo(0f, 0f, -r, 0f)
        reusablePath.quadTo(0f, 0f, 0f, -r)

        canvas.save()
        canvas.translate(cx, cy)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, r, c1, c2)
        canvas.restore()
    }

    private fun drawSpiral(canvas: Canvas, cx: Float, cy: Float, maxR: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        val turns = 2.2f
        val steps = 36
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val theta = t * turns * 2f * Math.PI.toFloat()
            val r = t * maxR
            val px = r * cos(theta)
            val py = r * sin(theta)
            if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
        }

        canvas.save()
        canvas.translate(cx, cy)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, maxR, c1, c2)
        canvas.restore()
    }

    private fun drawSmiley(canvas: Canvas, cx: Float, cy: Float, r: Float, c1: Int, c2: Int) {
        reusablePath.reset()
        reusablePath.addCircle(0f, 0f, r, Path.Direction.CW)
        reusablePath.moveTo(-r * 0.35f, -r * 0.25f)
        reusablePath.lineTo(-r * 0.35f, -r * 0.10f)
        reusablePath.moveTo(r * 0.35f, -r * 0.25f)
        reusablePath.lineTo(r * 0.35f, -r * 0.10f)
        reusablePath.moveTo(-r * 0.42f, r * 0.20f)
        reusablePath.quadTo(0f, r * 0.62f, r * 0.42f, r * 0.20f)

        canvas.save()
        canvas.translate(cx, cy)
        strokeWithHalo(canvas, reusablePath, 0f, 0f, r, c1, c2)
        canvas.restore()
    }
}

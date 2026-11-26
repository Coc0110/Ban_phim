package com.banphim.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

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
            shaderDirty = true
            invalidate()
        }

    private val stops = intArrayOf(
        Color.WHITE,
        Color.parseColor("#FF1744"),
        Color.parseColor("#FF9100"),
        Color.parseColor("#FFEA00"),
        Color.parseColor("#00E676"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#2979FF"),
        Color.parseColor("#651FFF"),
        Color.parseColor("#F50057"),
        Color.BLACK
    )

    var thumbProgress: Float = 0.5f
        private set

    var selectedColor: Int = Color.BLACK
        private set

    var autoHideOnRelease: Boolean = true
    var onColorSelected: ((Int) -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#66000000")
        strokeWidth = 1.5f * resources.displayMetrics.density
    }
    private val thumbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbOuterRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#AA000000")
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val thumbInnerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val clipPath = Path()
    private val barRect = RectF()
    private var shaderDirty = true
    private val deadzonePx = 16f * resources.displayMetrics.density
    private val maxShadeDragPx = 120f * resources.displayMetrics.density

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        if (visibility != GONE) {
            visibility = GONE
            onDismissed?.invoke()
        }
    }

    fun setSelectedColor(color: Int) {
        selectedColor = color
        thumbProgress = findNearestProgress(color)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shaderDirty = true
    }

    private fun thumbRadiusOf(w: Float, h: Float): Float = minOf(w, h) * 0.42f

    /**
     * Che do NGANG: le hai dau thanh = ban kinh nut + nua net vien,
     * de nut khi keo het coi van nam tron trong view (khong bi cat / tran).
     */
    private fun horizontalInset(w: Float, h: Float): Float =
        (thumbRadiusOf(w, h) + thumbOuterRingPaint.strokeWidth / 2f).coerceAtMost(w / 2f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val pad = minOf(w, h) * 0.15f
        val barLeft: Float
        val barTop: Float
        val barRight: Float
        val barBottom: Float

        if (orientation == Orientation.VERTICAL) {
            barLeft = pad
            barRight = w - pad
            barTop = pad
            barBottom = h - pad
        } else {
            val inset = horizontalInset(w, h)
            barLeft = inset
            barRight = w - inset
            barTop = pad
            barBottom = h - pad
        }

        if (shaderDirty) {
            barPaint.shader = if (orientation == Orientation.VERTICAL) {
                LinearGradient(0f, barTop, 0f, barBottom, stops, null, Shader.TileMode.CLAMP)
            } else {
                LinearGradient(barLeft, 0f, barRight, 0f, stops, null, Shader.TileMode.CLAMP)
            }
            shaderDirty = false
        }

        barRect.set(barLeft, barTop, barRight, barBottom)
        val cornerRadius = minOf(barRect.width(), barRect.height()) / 2f
        clipPath.reset()
        clipPath.addRoundRect(barRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(clipPath, barPaint)
        canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, borderPaint)

        val thumbRadius = thumbRadiusOf(w, h)
        val cx: Float
        val cy: Float
        if (orientation == Orientation.VERTICAL) {
            cx = w / 2f
            val travelTop = barTop + thumbRadius * 0.5f
            val travelBottom = barBottom - thumbRadius * 0.5f
            cy = travelTop + thumbProgress * (travelBottom - travelTop)
        } else {
            cy = h / 2f
            // Tam nut chay tu dau den cuoi thanh mau
            cx = barLeft + thumbProgress * (barRight - barLeft)
        }

        thumbFillPaint.color = selectedColor
        canvas.drawCircle(cx, cy, thumbRadius, thumbFillPaint)
        canvas.drawCircle(cx, cy, thumbRadius - thumbInnerRingPaint.strokeWidth * 0.5f, thumbInnerRingPaint)
        canvas.drawCircle(cx, cy, thumbRadius, thumbOuterRingPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                updateFromTouch(event.x, event.y)
                if (autoHideOnRelease) hide()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (autoHideOnRelease) hide()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(x: Float, y: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val progress: Float
        val perpDelta: Float
        if (orientation == Orientation.VERTICAL) {
            progress = (y / h).coerceIn(0f, 1f)
            perpDelta = (w / 2f) - x
        } else {
            // Tinh theo dung doan thanh mau (da tru le), de nut bam sat ngon tay
            val inset = horizontalInset(w, h)
            val span = (w - 2f * inset).coerceAtLeast(1f)
            progress = ((x - inset) / span).coerceIn(0f, 1f)
            perpDelta = y - (h / 2f)
        }

        thumbProgress = progress
        var color = interpolateStops(progress)

        if (abs(perpDelta) > deadzonePx) {
            val factor = ((abs(perpDelta) - deadzonePx) / maxShadeDragPx).coerceIn(0f, 1f)
            color = if (perpDelta > 0f) {
                blendColors(color, Color.BLACK, factor * 0.85f)
            } else {
                blendColors(color, Color.WHITE, factor * 0.85f)
            }
        }

        selectedColor = color
        invalidate()
        onColorSelected?.invoke(color)
    }

    private fun interpolateStops(t: Float): Int {
        val scaled = t.coerceIn(0f, 1f) * (stops.size - 1)
        val idx = scaled.toInt().coerceIn(0, stops.size - 2)
        val frac = scaled - idx
        return blendColors(stops[idx], stops[idx + 1], frac)
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(c1) * inv + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun findNearestProgress(color: Int): Float {
        var bestT = 0.5f
        var bestDist = Int.MAX_VALUE
        val samples = 60
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val c = interpolateStops(t)
            val dr = Color.red(color) - Color.red(c)
            val dg = Color.green(color) - Color.green(c)
            val db = Color.blue(color) - Color.blue(c)
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestT = t
            }
        }
        return bestT
    }
}
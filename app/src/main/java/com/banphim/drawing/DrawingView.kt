package com.banphim.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.banphim.R

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val history = StrokeHistory()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val gridPaint = Paint().apply {
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val previewFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val previewRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = Color.WHITE
    }

    private var currentPenColor: Int = Color.parseColor("#FFF176")
    private var currentCanvasColor: Int = Color.parseColor("#1C1E22")
    private var currentStrokeWidth: Float = 10f * resources.displayMetrics.density
    private var currentPenStyle: PenStyle = PenStyle.NORMAL
    private var showGrid: Boolean = true
    private var brushPreviewVisible: Boolean = false

    private var activePath: Path? = null
    private var lastX = 0f
    private var lastY = 0f

    var onHistoryChanged: (() -> Unit)? = null
    var onDrawStart: (() -> Unit)? = null

    init {
        setBackgroundColor(currentCanvasColor)
    }

    fun setPenColor(color: Int) {
        currentPenColor = color
        if (brushPreviewVisible) invalidate()
    }

    fun getPenColor(): Int = currentPenColor

    fun setCanvasColor(color: Int) {
        currentCanvasColor = color
        setBackgroundColor(color)
        invalidate()
    }

    fun getCanvasColor(): Int = currentCanvasColor

    fun setStrokeWidth(widthPx: Float) {
        currentStrokeWidth = widthPx
        if (brushPreviewVisible) invalidate()
    }

    fun getStrokeWidth(): Float = currentStrokeWidth

    fun setPenStyle(style: PenStyle) {
        currentPenStyle = style
    }

    fun setShowGrid(show: Boolean) {
        if (showGrid != show) {
            showGrid = show
            invalidate()
        }
    }

    fun setBrushPreviewVisible(visible: Boolean) {
        if (brushPreviewVisible != visible) {
            brushPreviewVisible = visible
            invalidate()
        }
    }

    fun undo() {
        if (history.undo()) {
            invalidate()
            onHistoryChanged?.invoke()
        }
    }

    fun redo() {
        if (history.redo()) {
            invalidate()
            onHistoryChanged?.invoke()
        }
    }

    fun clear() {
        if (history.clear()) {
            invalidate()
            onHistoryChanged?.invoke()
        }
    }

    fun getStrokes(): List<Stroke> = history.strokes()

    fun canUndo(): Boolean = history.canUndo()

    fun canRedo(): Boolean = history.canRedo()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = resources.getDimensionPixelSize(R.dimen.keyboard_height)
        val measuredWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showGrid) {
            drawSubtleGrid(canvas)
        }
        for (stroke in history.strokes()) {
            drawStyledStroke(canvas, stroke.path, stroke.color, stroke.strokeWidth, stroke.style)
        }
        activePath?.let { path ->
            drawStyledStroke(canvas, path, currentPenColor, currentStrokeWidth, currentPenStyle)
        }
        if (brushPreviewVisible) {
            val cx = width / 2f
            val cy = height / 2f
            val r = currentStrokeWidth / 2f
            previewFillPaint.color = currentPenColor
            canvas.drawCircle(cx, cy, r, previewFillPaint)
            canvas.drawCircle(cx, cy, r, previewRingPaint)
        }
    }

    private fun drawStyledStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        style: PenStyle
    ) {
        if (style == PenStyle.GLOW) {
            glowPaint.color = Color.argb(75, Color.red(color), Color.green(color), Color.blue(color))
            glowPaint.strokeWidth = strokeWidth * 2.0f
            canvas.drawPath(path, glowPaint)
        }
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.pathEffect = if (style == PenStyle.DASHED) {
            DashPathEffect(floatArrayOf(strokeWidth * 1.4f, strokeWidth * 1.4f), 0f)
        } else {
            null
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSubtleGrid(canvas: Canvas) {
        val step = 28f * resources.displayMetrics.density
        val luminance = (0.299 * Color.red(currentCanvasColor) +
            0.587 * Color.green(currentCanvasColor) +
            0.114 * Color.blue(currentCanvasColor)) / 255.0
        gridPaint.color = if (luminance < 0.5) {
            Color.argb(18, 255, 255, 255)
        } else {
            Color.argb(18, 0, 0, 0)
        }
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDrawStart?.invoke()
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                activePath = path
                lastX = x
                lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val path = activePath ?: return false
                val midX = (lastX + x) / 2f
                val midY = (lastY + y) / 2f
                path.quadTo(lastX, lastY, midX, midY)
                lastX = x
                lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val path = activePath ?: return false
                path.lineTo(x, y)
                val bounds = RectF()
                path.computeBounds(bounds, true)
                history.push(Stroke(path, currentPenColor, currentStrokeWidth, bounds, currentPenStyle))
                activePath = null
                invalidate()
                onHistoryChanged?.invoke()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePath = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

package com.banphim.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    // Net tay: xoa diem anh tren lop trong suot (khong xoa nen / luoi)
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
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
    private val previewDarkRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = Color.parseColor("#AA000000")
    }

    private var currentPenColor: Int = Color.parseColor("#FFF176")
    private var currentCanvasColor: Int = Color.parseColor("#1C1E22")
    private var currentStrokeWidth: Float = 10f * resources.displayMetrics.density
    private var currentEraserWidth: Float = 22f * resources.displayMetrics.density
    private var currentPenStyle: PenStyle = PenStyle.NORMAL
    private var eraserMode: Boolean = false
    private var showGrid: Boolean = true
    private var brushPreviewVisible: Boolean = false
    private var colorPreviewVisible: Boolean = false

    private var activePath: Path? = null
    private var activeIsEraser: Boolean = false
    private var lastX = 0f
    private var lastY = 0f

    var onHistoryChanged: (() -> Unit)? = null
    var onDrawStart: (() -> Unit)? = null

    init {
        setBackgroundColor(currentCanvasColor)
    }

    fun setPenColor(color: Int) {
        currentPenColor = color
        if (brushPreviewVisible || colorPreviewVisible) invalidate()
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

    /** Co tay rieng, khong anh huong co but ve. */
    fun setEraserWidth(widthPx: Float) {
        currentEraserWidth = widthPx
        if (brushPreviewVisible) invalidate()
    }

    fun getEraserWidth(): Float = currentEraserWidth

    /** true: dang dung but tay; false: dang dung but ve. */
    fun setEraserMode(enabled: Boolean) {
        if (eraserMode != enabled) {
            eraserMode = enabled
            if (brushPreviewVisible) invalidate()
        }
    }

    fun isEraserMode(): Boolean = eraserMode

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

    /**
     * Hien o mau lon giua canvas khi dang keo thanh chon mau but, de nguoi dung xem truoc mau
     * ma ngon tay khong che mat (ngon tay thuong che kin nut chon mau tren thanh).
     */
    fun setColorPreviewVisible(visible: Boolean) {
        if (colorPreviewVisible != visible) {
            colorPreviewVisible = visible
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

    /** Khoi phuc cac net da bi xoa (dung cho nut "Hoan tac" sau khi bam Xoa). */
    fun restoreStrokes(strokes: List<Stroke>) {
        if (strokes.isEmpty()) return
        for (stroke in strokes) {
            history.push(stroke)
        }
        invalidate()
        onHistoryChanged?.invoke()
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

        val strokes = history.strokes()
        val erasingNow = activePath != null && activeIsEraser
        // Chi can lop trong suot khi co net tay (de CLEAR chi xoa muc, khong xoa nen/luoi)
        val needsLayer = erasingNow || strokes.any { it.isEraser }
        val layerCount = if (needsLayer) {
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        } else {
            -1
        }

        for (stroke in strokes) {
            if (stroke.isEraser) {
                drawEraserPath(canvas, stroke.path, stroke.strokeWidth)
            } else {
                drawStyledStroke(canvas, stroke.path, stroke.color, stroke.strokeWidth, stroke.style)
            }
        }
        activePath?.let { path ->
            if (activeIsEraser) {
                drawEraserPath(canvas, path, currentEraserWidth)
            } else {
                drawStyledStroke(canvas, path, currentPenColor, currentStrokeWidth, currentPenStyle)
            }
        }

        if (needsLayer) {
            canvas.restoreToCount(layerCount)
        }

        val density = resources.displayMetrics.density
        if (brushPreviewVisible) {
            val cx = width / 2f
            val cy = height / 2f
            if (eraserMode) {
                // Tay: vong tron trang mo, co hai vien de thay ro tren moi nen
                val r = currentEraserWidth / 2f
                previewFillPaint.color = Color.argb(90, 255, 255, 255)
                canvas.drawCircle(cx, cy, r, previewFillPaint)
                canvas.drawCircle(cx, cy, r + 1f * density, previewDarkRingPaint)
                canvas.drawCircle(cx, cy, r - 1f * density, previewRingPaint)
            } else {
                val r = currentStrokeWidth / 2f
                previewFillPaint.color = currentPenColor
                canvas.drawCircle(cx, cy, r, previewFillPaint)
                canvas.drawCircle(cx, cy, r, previewRingPaint)
            }
        }
        if (colorPreviewVisible) {
            val cx = width / 2f
            val cy = height / 2f
            val r = 30f * density
            previewFillPaint.color = currentPenColor
            canvas.drawCircle(cx, cy, r, previewFillPaint)
            // Hai vien (toi + trang) de thay ro tren ca nen sang lan nen toi
            canvas.drawCircle(cx, cy, r + 1f * density, previewDarkRingPaint)
            canvas.drawCircle(cx, cy, r - 1f * density, previewRingPaint)
        }
    }

    private fun drawStyledStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float,
        style: PenStyle
    ) {
        PenStyleRenderer.drawStroke(canvas, path, color, strokeWidth, style, paint, glowPaint)
    }

    private fun drawEraserPath(canvas: Canvas, path: Path, strokeWidth: Float) {
        eraserPaint.strokeWidth = strokeWidth
        canvas.drawPath(path, eraserPaint)
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
                activeIsEraser = eraserMode
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
                val strokeW = if (activeIsEraser) currentEraserWidth else currentStrokeWidth
                history.push(
                    Stroke(path, currentPenColor, strokeW, bounds, currentPenStyle, activeIsEraser)
                )
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
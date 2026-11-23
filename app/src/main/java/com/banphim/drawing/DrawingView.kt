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

    private var currentPenColor: Int = Color.parseColor("#111111")
    private var currentCanvasColor: Int = Color.WHITE
    private var currentStrokeWidth: Float = 12f * resources.displayMetrics.density

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
    }

    fun getPenColor(): Int = currentPenColor

    fun setCanvasColor(color: Int) {
        currentCanvasColor = color
        setBackgroundColor(color)
        invalidate()
    }

    fun getCanvasColor(): Int = currentCanvasColor

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
        for (stroke in history.strokes()) {
            paint.color = stroke.color
            paint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, paint)
        }
        activePath?.let { path ->
            paint.color = currentPenColor
            paint.strokeWidth = currentStrokeWidth
            canvas.drawPath(path, paint)
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
                history.push(Stroke(path, currentPenColor, currentStrokeWidth, bounds))
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

package com.banphim.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BrushSizeSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var minStrokePx = 3f * density
    private var maxStrokePx = 26f * density

    var progress: Float = 0.35f
        private set

    var onSizeChanged: ((Float) -> Unit)? = null
    var onDragStateChanged: ((Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFFFFF")
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f * resources.displayMetrics.density
    }
    private val activeTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE082")
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f * resources.displayMetrics.density
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#66000000")
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    /** Mau phan thanh da keo. But ve: vang (mac dinh), but tay: xanh nhat. */
    var accentColor: Int
        get() = activeTrackPaint.color
        set(value) {
            activeTrackPaint.color = value
            invalidate()
        }

    /** Doi khoang kich co (dp). Goi truoc setStrokeWidthPx. Mac dinh 3dp - 26dp. */
    fun setRangeDp(minDp: Float, maxDp: Float) {
        minStrokePx = minDp * density
        maxStrokePx = maxDp * density
        invalidate()
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
        onDragStateChanged?.invoke(false)
    }

    fun setStrokeWidthPx(strokePx: Float) {
        val clamped = strokePx.coerceIn(minStrokePx, maxStrokePx)
        progress = (clamped - minStrokePx) / (maxStrokePx - minStrokePx)
        invalidate()
    }

    fun currentStrokeWidthPx(): Float = minStrokePx + progress * (maxStrokePx - minStrokePx)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val thumbRadius = h * 0.32f
        val startX = thumbRadius + paddingLeft
        val endX = w - thumbRadius - paddingRight
        val cy = h / 2f
        val thumbX = startX + progress * (endX - startX)

        canvas.drawLine(startX, cy, endX, cy, trackPaint)
        canvas.drawLine(startX, cy, thumbX, cy, activeTrackPaint)
        canvas.drawCircle(thumbX, cy, thumbRadius, thumbPaint)
        canvas.drawCircle(thumbX, cy, thumbRadius, thumbBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDragStateChanged?.invoke(true)
                updateProgress(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateProgress(event.x)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                updateProgress(event.x)
                onDragStateChanged?.invoke(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgress(x: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val thumbRadius = h * 0.32f
        val startX = thumbRadius + paddingLeft
        val endX = (w - thumbRadius - paddingRight).coerceAtLeast(startX + 1f)
        progress = ((x - startX) / (endX - startX)).coerceIn(0f, 1f)
        invalidate()
        onSizeChanged?.invoke(currentStrokeWidthPx())
    }
}
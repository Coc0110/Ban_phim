package com.banphim.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator

/**
 * Cong tac ON/OFF dang vien thuoc: nut tron truot qua lai, mau nen doi dan.
 *
 * - Doi trang thai bang setOn(true/false). View KHONG tu dao trang thai khi bam,
 *   ban tu quyet dinh trong setOnClickListener.
 * - Doc trang thai bang isOn.
 */
class PillToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var on = false
    val isOn: Boolean get() = on

    // 0 = OFF, 1 = ON
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private val argb = ArgbEvaluator()
    private val density = resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(6f * density, 0f, 2f * density, 0x55000000)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 17f, resources.displayMetrics
        )
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        // Can software layer de ve bong do (setShadowLayer) cua nut tron
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        isFocusable = true
    }

    /** Dat trang thai. animate = false de nhay thang, khong chay hieu ung. */
    fun setOn(value: Boolean, animate: Boolean = true) {
        if (value == on && animator?.isRunning != true) return
        on = value
        animator?.cancel()
        val target = if (value) 1f else 0f

        if (!animate || !isLaidOut) {
            progress = target
            invalidate()
            return
        }

        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 260L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((184 * density).toInt(), widthMeasureSpec),
            resolveSize((58 * density).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val t = progress

        // Nen: gradient doi mau theo progress
        val c1 = argb.evaluate(t, OFF_START, ON_START) as Int
        val c2 = argb.evaluate(t, OFF_END, ON_END) as Int
        trackPaint.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, trackPaint)

        // Nut tron trang
        val pad = 6f * density
        val d = h - 2f * pad
        val r = d / 2f
        val cx = pad + r + (w - 2f * pad - d) * t
        val cy = h / 2f
        canvas.drawCircle(cx, cy, r, knobPaint)

        // Chu ON / OFF nam o phia doi dien nut tron, mo dan qua lai
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        textPaint.alpha = (255 * t).toInt()
        canvas.drawText("ON", (w - h + pad) / 2f, textY, textPaint)
        textPaint.alpha = (255 * (1f - t)).toInt()
        canvas.drawText("OFF", (w + h - pad) / 2f, textY, textPaint)

        // Icon ban phim tren nut tron
        iconPaint.color = argb.evaluate(t, OFF_START, ON_START) as Int
        drawKeyboardIcon(canvas, cx, cy, r * 0.55f)
    }

    /** s = nua chieu rong cua icon */
    private fun drawKeyboardIcon(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        // Khung
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = s * 0.16f
        rect.set(cx - s, cy - s * 0.65f, cx + s, cy + s * 0.65f)
        canvas.drawRoundRect(rect, s * 0.22f, s * 0.22f, iconPaint)

        // Cac phim
        iconPaint.style = Paint.Style.FILL
        val k = s * 0.09f
        for (i in 0..3) {
            val x = cx + s * (-0.57f + 0.38f * i)
            canvas.drawRect(x - k, cy - s * 0.30f - k, x + k, cy - s * 0.30f + k, iconPaint)
            canvas.drawRect(x - k, cy - k, x + k, cy + k, iconPaint)
        }
        // Thanh dai (space)
        canvas.drawRect(cx - s * 0.5f, cy + s * 0.30f - k, cx + s * 0.5f, cy + s * 0.30f + k, iconPaint)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = on
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        val ON_START = Color.parseColor("#168A4C")
        val ON_END = Color.parseColor("#3DCB7F")
        val OFF_START = Color.parseColor("#5B6675")
        val OFF_END = Color.parseColor("#9AA5B4")
    }
}

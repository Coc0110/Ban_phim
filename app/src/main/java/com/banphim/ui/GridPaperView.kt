package com.banphim.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.banphim.R

class GridPaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val paperColor: Int = ContextCompat.getColor(context, R.color.paper_bg)
    val gridLineColor: Int = ContextCompat.getColor(context, R.color.paper_grid_line)

    private val gridPaint = Paint().apply {
        color = gridLineColor
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val stepPx = 28f * resources.displayMetrics.density

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
    }
}

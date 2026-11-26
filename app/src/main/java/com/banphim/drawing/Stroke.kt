package com.banphim.drawing

import android.graphics.Path
import android.graphics.RectF

enum class PenStyle {
    NORMAL,
    PENCIL,
    FOUNTAIN
}

data class Stroke(
    val path: Path,
    val color: Int,
    val strokeWidth: Float,
    val bounds: RectF,
    val style: PenStyle = PenStyle.NORMAL
)

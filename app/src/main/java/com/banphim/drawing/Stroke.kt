package com.banphim.drawing

import android.graphics.Path
import android.graphics.RectF

data class Stroke(
    val path: Path,
    val color: Int,
    val strokeWidth: Float,
    val bounds: RectF
)

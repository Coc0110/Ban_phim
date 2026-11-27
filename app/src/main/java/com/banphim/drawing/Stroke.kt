package com.banphim.drawing

import android.graphics.Path
import android.graphics.RectF

enum class PenStyle {
    NORMAL,
    PENCIL,
    FOUNTAIN
}

/**
 * Mot net trong lich su. isEraser = true: day la net TAY (xoa diem anh cua cac net ve truoc no),
 * dung chung StrokeHistory nen undo/redo hoat dong nhu voi net ve thuong.
 */
data class Stroke(
    val path: Path,
    val color: Int,
    val strokeWidth: Float,
    val bounds: RectF,
    val style: PenStyle = PenStyle.NORMAL,
    val isEraser: Boolean = false
)
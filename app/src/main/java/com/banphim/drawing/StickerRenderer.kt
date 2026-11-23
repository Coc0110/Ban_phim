package com.banphim.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max

object StickerRenderer {

    fun renderBitmap(
        strokes: List<Stroke>,
        maxSizePx: Int = 512,
        outlineWidthPx: Float = 8f,
        paddingPx: Float = 16f
    ): Bitmap? {
        if (strokes.isEmpty()) return null

        var minLeft = Float.POSITIVE_INFINITY
        var minTop = Float.POSITIVE_INFINITY
        var maxRight = Float.NEGATIVE_INFINITY
        var maxBottom = Float.NEGATIVE_INFINITY
        var maxStrokeWidth = 0f
        for (stroke in strokes) {
            minLeft = minOf(minLeft, stroke.bounds.left)
            minTop = minOf(minTop, stroke.bounds.top)
            maxRight = maxOf(maxRight, stroke.bounds.right)
            maxBottom = maxOf(maxBottom, stroke.bounds.bottom)
            if (stroke.strokeWidth > maxStrokeWidth) {
                maxStrokeWidth = stroke.strokeWidth
            }
        }
        val unionBounds = RectF(minLeft, minTop, maxRight, maxBottom)

        val expand = maxStrokeWidth / 2f + outlineWidthPx + paddingPx
        unionBounds.inset(-expand, -expand)

        val contentWidth = max(1f, unionBounds.width())
        val contentHeight = max(1f, unionBounds.height())
        val scale = minOf(1f, maxSizePx.toFloat() / max(contentWidth, contentHeight))

        val outWidth = max(1, ceil(contentWidth * scale).toInt())
        val outHeight = max(1, ceil(contentHeight * scale).toInt())

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.translate(-unionBounds.left, -unionBounds.top)

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        if (outlineWidthPx > 0f) {
            for (stroke in strokes) {
                outlinePaint.strokeWidth = stroke.strokeWidth + outlineWidthPx * 2f
                canvas.drawPath(stroke.path, outlinePaint)
            }
        }

        for (stroke in strokes) {
            strokePaint.color = stroke.color
            strokePaint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, strokePaint)
        }

        return bitmap
    }

    fun renderToPngFile(strokes: List<Stroke>, cacheDir: File): File? =
        renderToFile(strokes, cacheDir, "image/png")

    @Suppress("DEPRECATION")
    fun renderToFile(strokes: List<Stroke>, cacheDir: File, mimeType: String): File? {
        val bitmap = renderBitmap(strokes) ?: return null
        val stickerDir = File(cacheDir, "stickers").apply { mkdirs() }
        stickerDir.listFiles()?.forEach { old ->
            if (old.isFile && System.currentTimeMillis() - old.lastModified() > 60 * 60 * 1000L) {
                old.delete()
            }
        }

        val isWebp = mimeType.equals("image/webp", ignoreCase = true)
        val ext = if (isWebp) "webp" else "png"
        val format = if (isWebp) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP
            }
        } else {
            Bitmap.CompressFormat.PNG
        }

        val outFile = File(stickerDir, "sticker_${System.currentTimeMillis()}.$ext")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(format, 100, fos)
        }
        bitmap.recycle()
        return outFile
    }
}

package com.banphim.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object StickerRenderer {

    fun renderBitmap(
        strokes: List<Stroke>,
        maxSizePx: Int = 512,
        outlineWidthPx: Float = 8f,
        paddingPx: Float = 16f,
        outlineColor: Int = Color.WHITE
    ): Bitmap? {
        // Net tay khong tao ra muc, chi xoa muc -> khong tinh vao khung cua nhan dan
        val inkStrokes = strokes.filter { !it.isEraser }
        if (inkStrokes.isEmpty()) return null
        val hasEraser = inkStrokes.size != strokes.size

        var minLeft = Float.POSITIVE_INFINITY
        var minTop = Float.POSITIVE_INFINITY
        var maxRight = Float.NEGATIVE_INFINITY
        var maxBottom = Float.NEGATIVE_INFINITY
        var maxStrokeWidth = 0f
        for (stroke in inkStrokes) {
            minLeft = minOf(minLeft, stroke.bounds.left)
            minTop = minOf(minTop, stroke.bounds.top)
            maxRight = maxOf(maxRight, stroke.bounds.right)
            maxBottom = maxOf(maxBottom, stroke.bounds.bottom)
            if (stroke.strokeWidth > maxStrokeWidth) {
                maxStrokeWidth = stroke.strokeWidth
            }
        }
        val unionBounds = RectF(minLeft, minTop, maxRight, maxBottom)

        val expand = maxStrokeWidth + outlineWidthPx + paddingPx
        unionBounds.inset(-expand, -expand)

        val contentWidth = max(1f, unionBounds.width())
        val contentHeight = max(1f, unionBounds.height())
        val scale = minOf(1f, maxSizePx.toFloat() / max(contentWidth, contentHeight))

        val outWidth = max(1, ceil(contentWidth * scale).toInt())
        val outHeight = max(1, ceil(contentHeight * scale).toInt())

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = outlineColor
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // ---- Khong co net tay: ve nhu cu ----
        if (!hasEraser) {
            val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            canvas.translate(-unionBounds.left, -unionBounds.top)

            if (outlineWidthPx > 0f) {
                for (stroke in strokes) {
                    PenStyleRenderer.drawOutline(
                        canvas = canvas,
                        path = stroke.path,
                        color = outlineColor,
                        baseStrokeWidth = stroke.strokeWidth,
                        outlineExtraWidth = outlineWidthPx * 2f,
                        style = stroke.style,
                        mainPaint = outlinePaint,
                        workPaint = glowPaint
                    )
                }
            }

            for (stroke in strokes) {
                PenStyleRenderer.drawStroke(
                    canvas = canvas,
                    path = stroke.path,
                    color = stroke.color,
                    strokeWidth = stroke.strokeWidth,
                    style = stroke.style,
                    mainPaint = strokePaint,
                    glowPaint = glowPaint
                )
            }
            return bitmap
        }

        // ---- Co net tay: ve muc ra lop rieng, tay tren lop do, roi dung lai vien quanh phan muc con lai ----
        val ink = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val inkCanvas = Canvas(ink)
        inkCanvas.scale(scale, scale)
        inkCanvas.translate(-unionBounds.left, -unionBounds.top)

        val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        // Ve theo dung thu tu: net tay chi xoa nhung net ve TRUOC no
        for (stroke in strokes) {
            if (stroke.isEraser) {
                eraserPaint.strokeWidth = stroke.strokeWidth
                inkCanvas.drawPath(stroke.path, eraserPaint)
            } else {
                PenStyleRenderer.drawStroke(
                    canvas = inkCanvas,
                    path = stroke.path,
                    color = stroke.color,
                    strokeWidth = stroke.strokeWidth,
                    style = stroke.style,
                    mainPaint = strokePaint,
                    glowPaint = glowPaint
                )
            }
        }

        // Tay het sach -> khong co gi de gui
        val inkBounds = alphaBounds(ink) ?: run {
            ink.recycle()
            return null
        }

        // Cat gon theo phan muc con lai (chua cho cho vien + le)
        val outlineRadius = if (outlineWidthPx > 0f) outlineWidthPx * scale else 0f
        val margin = ceil(outlineRadius + paddingPx * scale).toInt()
        val cropLeft = max(0, inkBounds.left - margin)
        val cropTop = max(0, inkBounds.top - margin)
        val cropRight = min(outWidth, inkBounds.right + margin)
        val cropBottom = min(outHeight, inkBounds.bottom + margin)

        val result = Bitmap.createBitmap(
            max(1, cropRight - cropLeft),
            max(1, cropBottom - cropTop),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.translate(-cropLeft.toFloat(), -cropTop.toFloat())
        if (outlineRadius > 0f) {
            drawDilatedOutline(canvas, ink, outlineRadius, outlineColor)
        }
        canvas.drawBitmap(ink, 0f, 0f, null)
        ink.recycle()
        return result
    }

    /**
     * Dung vien bang cach "no" phan muc ra: ve nhieu ban sao cua lop muc, to mau vien,
     * dich di theo cac huong tren hai vong tron ban kinh radius va radius/2.
     */
    private fun drawDilatedOutline(canvas: Canvas, ink: Bitmap, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        val steps = 24
        for (ring in floatArrayOf(1f, 0.5f)) {
            for (i in 0 until steps) {
                val angle = 2.0 * Math.PI * i / steps
                canvas.drawBitmap(
                    ink,
                    (cos(angle) * radius * ring).toFloat(),
                    (sin(angle) * radius * ring).toFloat(),
                    paint
                )
            }
        }
    }

    /** Khung bao cua cac diem anh con nhin thay (alpha > 0), hoac null neu bitmap trong. */
    private fun alphaBounds(bitmap: Bitmap): Rect? {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var left = w
        var top = h
        var right = -1
        var bottom = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((pixels[row + x] ushr 24) != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        return if (right < 0) null else Rect(left, top, right + 1, bottom + 1)
    }

    fun renderToPngFile(strokes: List<Stroke>, cacheDir: File): File? =
        renderToFile(strokes, cacheDir, "image/png")

    @Suppress("DEPRECATION")
    fun renderToFile(
        strokes: List<Stroke>,
        cacheDir: File,
        mimeType: String,
        outlineWidthPx: Float = 8f,
        outlineColor: Int = Color.WHITE
    ): File? {
        val bitmap = renderBitmap(
            strokes = strokes,
            outlineWidthPx = outlineWidthPx,
            outlineColor = outlineColor
        ) ?: return null
        val stickerDir = File(cacheDir, "stickers").apply { mkdirs() }
        stickerDir.listFiles()?.forEach { old ->
            if (old.isFile && System.currentTimeMillis() - old.lastModified() > 60 * 60 * 1000L) {
                old.delete()
            }
        }

        val isGif = mimeType.equals("image/gif", ignoreCase = true)
        val isWebp = mimeType.equals("image/webp", ignoreCase = true)
        val ext = when {
            isGif -> "gif"
            isWebp -> "webp"
            else -> "png"
        }

        val outFile = File(stickerDir, "sticker_${System.currentTimeMillis()}.$ext")
        FileOutputStream(outFile).use { fos ->
            if (isGif) {
                GifEncoder.encode(bitmap, fos)
            } else {
                val format = if (isWebp) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                } else {
                    Bitmap.CompressFormat.PNG
                }
                bitmap.compress(format, 100, fos)
            }
        }
        bitmap.recycle()
        return outFile
    }
}
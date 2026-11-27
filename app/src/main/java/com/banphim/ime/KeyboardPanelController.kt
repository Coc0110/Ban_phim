package com.banphim.ime

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageButton
import androidx.core.widget.ImageViewCompat
import com.banphim.R
import com.banphim.drawing.BrushSizeSliderView
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.DrawingView
import com.banphim.drawing.PenStyle
import com.banphim.drawing.Stroke
import com.banphim.drawing.StickerRenderer
import java.io.File

/**
 * Noi day toan bo giao dien R.layout.keyboard_drawing: thanh cong cu, popup chon mau,
 * thanh kich co but ve / but tay, undo/redo, xoa, nut gui.
 *
 * Dung chung cho ban phim that (DrawingKeyboardService) va ban phim thu trong man hinh cai dat.
 * Moi noi dung "gui nhan dan" do ben goi quyet dinh qua onSend.
 */
class KeyboardPanelController(
    root: View,
    private val prefs: SharedPreferences,
    private val onSend: () -> Unit
) {

    val drawingView: DrawingView = root.findViewById(R.id.drawingView)
    private val btnUndo: ImageButton = root.findViewById(R.id.btnUndo)
    private val btnRedo: ImageButton = root.findViewById(R.id.btnRedo)
    private val btnClear: ImageButton = root.findViewById(R.id.btnClear)
    private val btnBrushSize: ImageButton = root.findViewById(R.id.btnBrushSize)
    private val btnEraser: ImageButton = root.findViewById(R.id.btnEraser)
    private val sliderBrushSize: BrushSizeSliderView = root.findViewById(R.id.sliderBrushSize)
    private val sliderEraserSize: BrushSizeSliderView = root.findViewById(R.id.sliderEraserSize)
    private val btnPenColor: View = root.findViewById(R.id.btnPenColor)
    private val btnBgColor: View = root.findViewById(R.id.btnBgColor)
    private val stripPenColor: ColorStripView = root.findViewById(R.id.stripPenColor)
    private val stripBgColor: ColorStripView = root.findViewById(R.id.stripBgColor)
    private val btnSend: View = root.findViewById(R.id.btnSend)

    private val density = root.resources.displayMetrics.density

    // Vien highlight cho cong cu dang chon: but ve = vang, but tay = xanh nhat
    private val penRing = makeToolRing(Color.parseColor("#FFE082"))
    private val eraserRing = makeToolRing(Color.parseColor("#80D8FF"))
    private val brushOriginalBg = btnBrushSize.background
    private val eraserOriginalBg = btnEraser.background

    init {
        stripPenColor.orientation = ColorStripView.Orientation.HORIZONTAL
        stripBgColor.orientation = ColorStripView.Orientation.VERTICAL

        // Thanh co tay rieng: khoang to hon, mau xanh nhat de phan biet voi thanh co but ve
        sliderEraserSize.setRangeDp(8f, 56f)
        sliderEraserSize.accentColor = Color.parseColor("#80D8FF")

        applyUserPreferences()
        sliderBrushSize.setStrokeWidthPx(drawingView.getStrokeWidth())
        sliderEraserSize.setStrokeWidthPx(drawingView.getEraserWidth())

        updateSwatchColor(btnPenColor, drawingView.getPenColor())
        updateSwatchColor(btnBgColor, drawingView.getCanvasColor())
        updateHistoryButtons()
        updateToolHighlight()

        drawingView.onHistoryChanged = { updateHistoryButtons() }
        drawingView.onDrawStart = { closeAllPopups() }

        // Bam icon but ve: dang o but tay -> chuyen ve but ve; da la but ve -> bat/tat thanh co but
        btnBrushSize.setOnClickListener {
            stripPenColor.hide()
            stripBgColor.hide()
            sliderEraserSize.hide()
            if (drawingView.isEraserMode()) {
                selectTool(eraser = false)
            } else if (sliderBrushSize.visibility == View.VISIBLE) {
                sliderBrushSize.hide()
            } else {
                sliderBrushSize.setStrokeWidthPx(drawingView.getStrokeWidth())
                sliderBrushSize.show()
            }
        }

        sliderBrushSize.onSizeChanged = { widthPx ->
            drawingView.setStrokeWidth(widthPx)
            prefs.edit().putFloat("brush_size_px", widthPx).apply()
        }

        sliderBrushSize.onDragStateChanged = { dragging ->
            drawingView.setBrushPreviewVisible(dragging)
        }

        // Bam nut tay: chua chon -> chon but tay; da chon -> bat/tat thanh co tay
        btnEraser.setOnClickListener {
            stripPenColor.hide()
            stripBgColor.hide()
            sliderBrushSize.hide()
            if (!drawingView.isEraserMode()) {
                selectTool(eraser = true)
            } else if (sliderEraserSize.visibility == View.VISIBLE) {
                sliderEraserSize.hide()
            } else {
                sliderEraserSize.setStrokeWidthPx(drawingView.getEraserWidth())
                sliderEraserSize.show()
            }
        }

        sliderEraserSize.onSizeChanged = { widthPx ->
            drawingView.setEraserWidth(widthPx)
            prefs.edit().putFloat("eraser_size_px", widthPx).apply()
        }

        sliderEraserSize.onDragStateChanged = { dragging ->
            drawingView.setBrushPreviewVisible(dragging)
        }

        btnPenColor.setOnClickListener {
            // Chon mau but ma dang o but tay -> tu chuyen ve but ve
            selectTool(eraser = false)
            sliderBrushSize.hide()
            sliderEraserSize.hide()
            stripBgColor.hide()
            btnPenColor.visibility = View.GONE
            stripPenColor.setSelectedColor(drawingView.getPenColor())
            stripPenColor.show()
        }

        btnBgColor.setOnClickListener {
            sliderBrushSize.hide()
            sliderEraserSize.hide()
            stripPenColor.hide()
            btnBgColor.visibility = View.INVISIBLE
            stripBgColor.setSelectedColor(drawingView.getCanvasColor())
            stripBgColor.show()
        }

        stripPenColor.onDismissed = {
            btnPenColor.visibility = View.VISIBLE
            drawingView.setColorPreviewVisible(false)
        }

        stripBgColor.onDismissed = {
            btnBgColor.visibility = View.VISIBLE
        }

        stripPenColor.onColorSelected = { color ->
            // Ngon tay che nut chon mau tren thanh -> hien o mau lon giua canvas de xem truoc
            drawingView.setColorPreviewVisible(true)
            drawingView.setPenColor(color)
            updateSwatchColor(btnPenColor, color)
            prefs.edit().putInt("pen_color", color).apply()
        }

        stripBgColor.onColorSelected = { color ->
            drawingView.setCanvasColor(color)
            updateOverlayContrast()
            updateSwatchColor(btnBgColor, color)
            prefs.edit().putInt("canvas_color", color).apply()
        }

        btnUndo.setOnClickListener {
            closeAllPopups()
            drawingView.undo()
        }
        btnRedo.setOnClickListener {
            closeAllPopups()
            drawingView.redo()
        }

        btnClear.setOnClickListener {
            closeAllPopups()
            drawingView.clear()
        }

        btnSend.setOnClickListener {
            closeAllPopups()
            onSend()
        }
    }

    /** Doc lai cai dat tu prefs (kieu but, o luoi, mau, co but, co tay) va ap vao canvas. */
    fun applyUserPreferences() {
        if (prefs.contains("pen_color")) {
            drawingView.setPenColor(prefs.getInt("pen_color", drawingView.getPenColor()))
        }
        if (prefs.contains("canvas_color")) {
            drawingView.setCanvasColor(prefs.getInt("canvas_color", drawingView.getCanvasColor()))
        }
        if (prefs.contains("brush_size_px")) {
            drawingView.setStrokeWidth(prefs.getFloat("brush_size_px", drawingView.getStrokeWidth()))
        }
        if (prefs.contains("eraser_size_px")) {
            drawingView.setEraserWidth(prefs.getFloat("eraser_size_px", drawingView.getEraserWidth()))
        }
        val styleName = prefs.getString("pen_style", PenStyle.NORMAL.name) ?: PenStyle.NORMAL.name
        val style = runCatching { PenStyle.valueOf(styleName) }.getOrDefault(PenStyle.NORMAL)
        drawingView.setPenStyle(style)
        drawingView.setShowGrid(prefs.getBoolean("show_grid", true))
        updateOverlayContrast()
    }

    fun closeAllPopups() {
        sliderBrushSize.hide()
        sliderEraserSize.hide()
        stripPenColor.hide()
        stripBgColor.hide()
        drawingView.setBrushPreviewVisible(false)
        drawingView.setColorPreviewVisible(false)
    }

    private fun selectTool(eraser: Boolean) {
        drawingView.setEraserMode(eraser)
        updateToolHighlight()
    }

    /** Vien sang quanh nut cua cong cu dang chon (but ve hoac but tay). */
    private fun updateToolHighlight() {
        val eraser = drawingView.isEraserMode()
        btnBrushSize.background = if (!eraser) penRing else brushOriginalBg
        btnEraser.background = if (eraser) eraserRing else eraserOriginalBg
    }

    private fun makeToolRing(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 10f * density
        setColor(Color.parseColor("#141518"))
        setStroke((1.5f * density).toInt().coerceAtLeast(1), color)
    }

    /**
     * Cac nut noi tren canvas (nut Xoa, the "Hoan tac") phai doc duoc tren moi mau nen:
     * nen sang -> icon/the mau toi, nen toi -> icon/the mau sang.
     */
    private fun updateOverlayContrast() {
        val c = drawingView.getCanvasColor()
        val luminance = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0
        val onLightCanvas = luminance >= 0.5
        val darkInk = Color.parseColor("#141518")
        val inkColor = if (onLightCanvas) darkInk else Color.WHITE

        ImageViewCompat.setImageTintList(btnClear, ColorStateList.valueOf(inkColor))
    }

    private fun updateSwatchColor(swatch: View, color: Int) {
        val layer = swatch.background?.mutate() as? LayerDrawable
        val fill = layer?.findDrawableByLayerId(R.id.swatch_fill) as? GradientDrawable
        if (fill != null) {
            fill.setColor(color)
        } else {
            swatch.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    private fun updateHistoryButtons() {
        val hasStrokes = drawingView.canUndo()
        val hasRedo = drawingView.canRedo()
        btnUndo.isEnabled = hasStrokes
        btnUndo.alpha = if (hasStrokes) 1.0f else 0.35f
        btnRedo.isEnabled = hasRedo
        btnRedo.alpha = if (hasRedo) 1.0f else 0.35f
        btnClear.visibility = if (hasStrokes) View.VISIBLE else View.GONE
    }

    companion object {
        /**
         * Ve nhan dan ra file, doc cai dat vien tu prefs. Dung chung cho ban phim that
         * (gui vao o nhap) va ban phim thu (hien trong man hinh cai dat).
         */
        fun renderSticker(
            context: Context,
            prefs: SharedPreferences,
            strokes: List<Stroke>,
            mimeType: String
        ): File? {
            val outlineEnabled = prefs.getBoolean("sticker_outline_enabled", true)
            val outlineWidth = if (outlineEnabled) 8f else 0f
            val outlineColor = prefs.getInt("sticker_outline_color", Color.WHITE)
            return StickerRenderer.renderToFile(
                strokes = strokes,
                cacheDir = context.cacheDir,
                mimeType = mimeType,
                outlineWidthPx = outlineWidth,
                outlineColor = outlineColor
            )
        }
    }
}
package com.banphim.ime

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageButton
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
 * thanh kich co but, undo/redo, xoa, nut gui.
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
    private val sliderBrushSize: BrushSizeSliderView = root.findViewById(R.id.sliderBrushSize)
    private val btnPenColor: View = root.findViewById(R.id.btnPenColor)
    private val btnBgColor: View = root.findViewById(R.id.btnBgColor)
    private val stripPenColor: ColorStripView = root.findViewById(R.id.stripPenColor)
    private val stripBgColor: ColorStripView = root.findViewById(R.id.stripBgColor)
    private val btnSend: View = root.findViewById(R.id.btnSend)

    init {
        stripPenColor.orientation = ColorStripView.Orientation.HORIZONTAL
        stripBgColor.orientation = ColorStripView.Orientation.VERTICAL

        applyUserPreferences()
        sliderBrushSize.setStrokeWidthPx(drawingView.getStrokeWidth())

        updateSwatchColor(btnPenColor, drawingView.getPenColor())
        updateSwatchColor(btnBgColor, drawingView.getCanvasColor())
        updateHistoryButtons()

        drawingView.onHistoryChanged = { updateHistoryButtons() }
        drawingView.onDrawStart = { closeAllPopups() }

        btnBrushSize.setOnClickListener {
            stripPenColor.hide()
            stripBgColor.hide()
            if (sliderBrushSize.visibility == View.VISIBLE) {
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

        btnPenColor.setOnClickListener {
            sliderBrushSize.hide()
            stripBgColor.hide()
            btnPenColor.visibility = View.GONE
            stripPenColor.setSelectedColor(drawingView.getPenColor())
            stripPenColor.show()
        }

        btnBgColor.setOnClickListener {
            sliderBrushSize.hide()
            stripPenColor.hide()
            btnBgColor.visibility = View.INVISIBLE
            stripBgColor.setSelectedColor(drawingView.getCanvasColor())
            stripBgColor.show()
        }

        stripPenColor.onDismissed = {
            btnPenColor.visibility = View.VISIBLE
        }

        stripBgColor.onDismissed = {
            btnBgColor.visibility = View.VISIBLE
        }

        stripPenColor.onColorSelected = { color ->
            drawingView.setPenColor(color)
            updateSwatchColor(btnPenColor, color)
            prefs.edit().putInt("pen_color", color).apply()
        }

        stripBgColor.onColorSelected = { color ->
            drawingView.setCanvasColor(color)
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

    /** Doc lai cai dat tu prefs (kieu but, o luoi, mau, co but) va ap vao canvas. */
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
        val styleName = prefs.getString("pen_style", PenStyle.NORMAL.name) ?: PenStyle.NORMAL.name
        val style = runCatching { PenStyle.valueOf(styleName) }.getOrDefault(PenStyle.NORMAL)
        drawingView.setPenStyle(style)
        drawingView.setShowGrid(prefs.getBoolean("show_grid", true))
    }

    fun closeAllPopups() {
        sliderBrushSize.hide()
        stripPenColor.hide()
        stripBgColor.hide()
        drawingView.setBrushPreviewVisible(false)
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
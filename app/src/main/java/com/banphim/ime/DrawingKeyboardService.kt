package com.banphim.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.banphim.BuildConfig
import com.banphim.R
import com.banphim.drawing.BrushSizeSliderView
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.DrawingView
import com.banphim.drawing.StickerRenderer

class DrawingKeyboardService : InputMethodService() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnBrushSize: ImageButton
    private lateinit var sliderBrushSize: BrushSizeSliderView
    private lateinit var btnPenColor: View
    private lateinit var btnBgColor: View
    private lateinit var stripPenColor: ColorStripView
    private lateinit var stripBgColor: ColorStripView

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_drawing, null)
        val padH = (10 * resources.displayMetrics.density).toInt()
        val padV = (8 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(padH, padV, padH, padV + navBottom)
            insets
        }

        drawingView = root.findViewById(R.id.drawingView)
        btnUndo = root.findViewById(R.id.btnUndo)
        btnRedo = root.findViewById(R.id.btnRedo)
        btnClear = root.findViewById(R.id.btnClear)
        btnBrushSize = root.findViewById(R.id.btnBrushSize)
        sliderBrushSize = root.findViewById(R.id.sliderBrushSize)
        btnPenColor = root.findViewById(R.id.btnPenColor)
        btnBgColor = root.findViewById(R.id.btnBgColor)
        stripPenColor = root.findViewById(R.id.stripPenColor)
        stripBgColor = root.findViewById(R.id.stripBgColor)

        stripPenColor.orientation = ColorStripView.Orientation.HORIZONTAL
        stripBgColor.orientation = ColorStripView.Orientation.VERTICAL

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        if (prefs.contains("pen_color")) {
            drawingView.setPenColor(prefs.getInt("pen_color", drawingView.getPenColor()))
        }
        if (prefs.contains("canvas_color")) {
            drawingView.setCanvasColor(prefs.getInt("canvas_color", drawingView.getCanvasColor()))
        }
        if (prefs.contains("brush_size_px")) {
            drawingView.setStrokeWidth(prefs.getFloat("brush_size_px", drawingView.getStrokeWidth()))
        }
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

        root.findViewById<View>(R.id.btnSend).setOnClickListener {
            closeAllPopups()
            sendSticker()
        }

        return root
    }

    private fun closeAllPopups() {
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

    private fun sendSticker() {
        val strokes = drawingView.getStrokes()
        if (strokes.isEmpty()) return

        val mimeType = StickerCommitter.preferredMimeType(currentInputEditorInfo)
        val renderMime = mimeType ?: StickerCommitter.MIME_WEBP
        val file = StickerRenderer.renderToFile(strokes, cacheDir, renderMime) ?: return
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(applicationContext, authority, file)
        }.getOrNull()

        val committed = contentUri != null && mimeType != null && StickerCommitter.commitSticker(
            currentInputConnection,
            currentInputEditorInfo,
            contentUri,
            mimeType
        )

        if (committed) {
            drawingView.clear()
        } else {
            if (contentUri != null) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newUri(contentResolver, "Sticker", contentUri))
            }
            Toast.makeText(this, R.string.toast_unsupported_field, Toast.LENGTH_SHORT).show()
        }
    }
}

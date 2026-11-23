package com.banphim.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.FileProvider
import com.banphim.BuildConfig
import com.banphim.R
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.DrawingView
import com.banphim.drawing.StickerRenderer

class DrawingKeyboardService : InputMethodService() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnPenColor: View
    private lateinit var btnBgColor: View
    private lateinit var stripPenColor: ColorStripView
    private lateinit var stripBgColor: ColorStripView

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_drawing, null)

        drawingView = root.findViewById(R.id.drawingView)
        btnUndo = root.findViewById(R.id.btnUndo)
        btnRedo = root.findViewById(R.id.btnRedo)
        btnClear = root.findViewById(R.id.btnClear)
        btnPenColor = root.findViewById(R.id.btnPenColor)
        btnBgColor = root.findViewById(R.id.btnBgColor)
        stripPenColor = root.findViewById(R.id.stripPenColor)
        stripBgColor = root.findViewById(R.id.stripBgColor)

        stripPenColor.orientation = ColorStripView.Orientation.HORIZONTAL
        stripBgColor.orientation = ColorStripView.Orientation.VERTICAL

        updateSwatchColor(btnPenColor, drawingView.getPenColor())
        updateSwatchColor(btnBgColor, drawingView.getCanvasColor())
        updateHistoryButtons()

        drawingView.onHistoryChanged = { updateHistoryButtons() }
        drawingView.onDrawStart = {
            stripPenColor.hide()
            stripBgColor.hide()
        }

        root.findViewById<ImageButton>(R.id.btnBackKeyboard).setOnClickListener {
            switchBackToPreviousKeyboard()
        }

        btnPenColor.setOnClickListener {
            stripBgColor.hide()
            if (stripPenColor.visibility == View.VISIBLE) stripPenColor.hide() else stripPenColor.show()
        }

        btnBgColor.setOnClickListener {
            stripPenColor.hide()
            if (stripBgColor.visibility == View.VISIBLE) stripBgColor.hide() else stripBgColor.show()
        }

        stripPenColor.onColorSelected = { color ->
            drawingView.setPenColor(color)
            updateSwatchColor(btnPenColor, color)
        }

        stripBgColor.onColorSelected = { color ->
            drawingView.setCanvasColor(color)
            updateSwatchColor(btnBgColor, color)
        }

        btnUndo.setOnClickListener { drawingView.undo() }
        btnRedo.setOnClickListener { drawingView.redo() }
        btnClear.setOnClickListener { drawingView.clear() }

        root.findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            sendSticker()
        }

        return root
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
        btnClear.isEnabled = hasStrokes
        btnClear.alpha = if (hasStrokes) 1.0f else 0.35f
        btnRedo.isEnabled = hasRedo
        btnRedo.alpha = if (hasRedo) 1.0f else 0.35f
    }

    private fun switchBackToPreviousKeyboard() {
        val switched = try {
            switchToPreviousInputMethod()
        } catch (_: Exception) {
            false
        }
        if (!switched) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
    }

    private fun sendSticker() {
        val strokes = drawingView.getStrokes()
        if (strokes.isEmpty()) return

        val file = StickerRenderer.renderToPngFile(strokes, cacheDir) ?: return
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(applicationContext, authority, file)
        }.getOrNull()

        val committed = contentUri != null && StickerCommitter.commitSticker(
            currentInputConnection,
            currentInputEditorInfo,
            contentUri
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

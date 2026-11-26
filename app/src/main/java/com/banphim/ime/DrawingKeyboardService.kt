package com.banphim.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.banphim.BuildConfig
import com.banphim.R

class DrawingKeyboardService : InputMethodService() {

    private lateinit var panel: KeyboardPanelController

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

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        panel = KeyboardPanelController(root, prefs) { sendSticker() }
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::panel.isInitialized) {
            panel.applyUserPreferences()
        }
    }

    private fun sendSticker() {
        val drawingView = panel.drawingView
        val strokes = drawingView.getStrokes()
        if (strokes.isEmpty()) return

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)

        val mimeType = StickerCommitter.preferredMimeType(currentInputEditorInfo)
        val renderMime = mimeType ?: StickerCommitter.MIME_WEBP
        val file = KeyboardPanelController.renderSticker(this, prefs, strokes, renderMime) ?: return
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
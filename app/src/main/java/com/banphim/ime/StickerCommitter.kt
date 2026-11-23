package com.banphim.ime

import android.content.ClipDescription
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

object StickerCommitter {

    private const val MIME_PNG = "image/png"
    private const val MIME_IMAGE_ANY = "image/*"

    fun supportsPng(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return mimeTypes.any { it.equals(MIME_PNG, ignoreCase = true) || it.equals(MIME_IMAGE_ANY, ignoreCase = true) }
    }

    fun commitSticker(
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        contentUri: Uri
    ): Boolean {
        if (inputConnection == null || !supportsPng(editorInfo)) return false
        val info = InputContentInfoCompat(
            contentUri,
            ClipDescription("Drawing Sticker", arrayOf(MIME_PNG)),
            null
        )
        return InputConnectionCompat.commitContent(
            inputConnection,
            editorInfo!!,
            info,
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            null
        )
    }
}

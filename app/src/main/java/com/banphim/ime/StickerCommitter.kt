package com.banphim.ime

import android.content.ClipDescription
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

object StickerCommitter {

    const val MIME_GIF = "image/gif"
    const val MIME_WEBP = "image/webp"
    const val MIME_PNG = "image/png"
    private const val MIME_IMAGE_ANY = "image/*"
    private const val MIME_ANY = "*/*"

    fun preferredMimeType(editorInfo: EditorInfo?): String? {
        if (editorInfo == null) return null
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        val acceptsWildcard = mimeTypes.any {
            it.equals(MIME_IMAGE_ANY, ignoreCase = true) || it.equals(MIME_ANY, ignoreCase = true)
        }
        val acceptsWebp = acceptsWildcard || mimeTypes.any { it.equals(MIME_WEBP, ignoreCase = true) }
        val acceptsPng = acceptsWildcard || mimeTypes.any { it.equals(MIME_PNG, ignoreCase = true) }
        val acceptsGif = acceptsWildcard || mimeTypes.any { it.equals(MIME_GIF, ignoreCase = true) }

        if (acceptsWebp) return MIME_WEBP
        if (acceptsPng) return MIME_PNG
        if (acceptsGif) return MIME_GIF
        return null
    }

    fun supportsPng(editorInfo: EditorInfo?): Boolean = preferredMimeType(editorInfo) != null

    fun commitSticker(
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        contentUri: Uri,
        mimeType: String = MIME_WEBP
    ): Boolean {
        if (inputConnection == null || preferredMimeType(editorInfo) == null) return false
        val info = InputContentInfoCompat(
            contentUri,
            ClipDescription("Drawing Sticker", arrayOf(mimeType)),
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

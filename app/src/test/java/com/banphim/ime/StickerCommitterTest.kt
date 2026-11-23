package com.banphim.ime

import android.net.Uri
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StickerCommitterTest {
    @Test
    fun `supportsPng returns true when editor accepts image png or wildcard`() {
        val infoPng = EditorInfo().apply {
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/png"))
        }
        val infoWild = EditorInfo().apply {
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/*"))
        }
        val infoNone = EditorInfo()

        assertTrue(StickerCommitter.supportsPng(infoPng))
        assertTrue(StickerCommitter.supportsPng(infoWild))
        assertFalse(StickerCommitter.supportsPng(infoNone))
    }

    @Test
    fun `preferredMimeType prefers webp over png when supported`() {
        val infoBoth = EditorInfo().apply {
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/png", "image/webp"))
        }
        val infoPngOnly = EditorInfo().apply {
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/png"))
        }
        val infoWild = EditorInfo().apply {
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/*"))
        }
        val infoNone = EditorInfo()

        assertEquals("image/webp", StickerCommitter.preferredMimeType(infoBoth))
        assertEquals("image/png", StickerCommitter.preferredMimeType(infoPngOnly))
        assertEquals("image/webp", StickerCommitter.preferredMimeType(infoWild))
        assertNull(StickerCommitter.preferredMimeType(infoNone))
    }

    @Test
    fun `preferredMimeType chooses gif for Messenger to preserve transparency`() {
        val messengerInfo = EditorInfo().apply {
            packageName = "com.facebook.orca"
            EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/png", "image/webp", "image/gif"))
        }
        assertEquals("image/gif", StickerCommitter.preferredMimeType(messengerInfo))
    }

    @Test
    fun `commitSticker returns false when inputConnection is null or unsupported`() {
        val uri = Uri.parse("content://com.banphim.fileprovider/stickers/test.png")
        assertFalse(StickerCommitter.commitSticker(null, EditorInfo(), uri, "image/png"))
    }
}

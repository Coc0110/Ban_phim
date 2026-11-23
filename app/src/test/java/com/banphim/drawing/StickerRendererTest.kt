package com.banphim.drawing

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class StickerRendererTest {
    @Test
    fun `empty strokes returns null`() {
        assertNull(StickerRenderer.renderBitmap(emptyList()))
    }

    @Test
    fun `single tap dot with zero-area bounds renders non-empty transparent bitmap`() {
        val dot = Stroke(
            path = Path().apply {
                moveTo(50f, 50f)
                lineTo(50f, 50f)
            },
            color = Color.BLACK,
            strokeWidth = 12f,
            bounds = RectF(50f, 50f, 50f, 50f)
        )
        val bmp = StickerRenderer.renderBitmap(listOf(dot))
        assertNotNull(bmp)
        assertTrue(bmp!!.width > 0 && bmp.height > 0)
        assertEquals(Color.TRANSPARENT, bmp.getPixel(0, 0))
    }

    @Test
    fun `large drawing is scaled within maxSizePx and saved to PNG`() {
        val wide = Stroke(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(1200f, 600f)
            },
            color = Color.RED,
            strokeWidth = 10f,
            bounds = RectF(0f, 0f, 1200f, 600f)
        )
        val bmp = StickerRenderer.renderBitmap(listOf(wide), maxSizePx = 512)!!
        assertTrue(bmp.width <= 512 && bmp.height <= 512)

        val dir = createTempDirectory("stickers").toFile()
        val file = StickerRenderer.renderToPngFile(listOf(wide), dir)
        assertNotNull(file)
        assertTrue(file!!.exists() && file.length() > 0L)
        dir.deleteRecursively()
    }
}

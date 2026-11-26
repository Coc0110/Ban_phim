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

    @Test
    fun `combining horizontal line and dot strokes includes both in bitmap bounds`() {
        val hLine = Stroke(
            path = Path().apply { moveTo(10f, 50f); lineTo(100f, 50f) },
            color = Color.BLACK,
            strokeWidth = 10f,
            bounds = RectF(10f, 50f, 100f, 50f)
        )
        val dot = Stroke(
            path = Path().apply { moveTo(250f, 200f); lineTo(250f, 200f) },
            color = Color.BLACK,
            strokeWidth = 10f,
            bounds = RectF(250f, 200f, 250f, 200f)
        )
        val bmp = StickerRenderer.renderBitmap(listOf(hLine, dot))!!
        assertTrue("Expected width > 240 to cover both strokes, was ${bmp.width}", bmp.width > 240)
        assertTrue("Expected height > 150 to cover both strokes, was ${bmp.height}", bmp.height > 150)
    }

    @Test
    fun `renderToFile writes webp file when mimeType is image webp`() {
        val stroke = Stroke(
            path = Path().apply { moveTo(0f, 0f); lineTo(50f, 50f) },
            color = Color.BLUE,
            strokeWidth = 10f,
            bounds = RectF(0f, 0f, 50f, 50f)
        )
        val dir = createTempDirectory("stickers_webp").toFile()
        val file = StickerRenderer.renderToFile(listOf(stroke), dir, "image/webp")
        assertNotNull(file)
        assertTrue(file!!.name.endsWith(".webp"))
        assertTrue(file.exists() && file.length() > 0L)
        dir.deleteRecursively()
    }

    @Test
    fun `renderToFile writes gif file when mimeType is image gif`() {
        val stroke = Stroke(
            path = Path().apply { moveTo(0f, 0f); lineTo(50f, 50f) },
            color = Color.RED,
            strokeWidth = 10f,
            bounds = RectF(0f, 0f, 50f, 50f)
        )
        val dir = createTempDirectory("stickers_gif").toFile()
        val file = StickerRenderer.renderToFile(listOf(stroke), dir, "image/gif")
        assertNotNull(file)
        assertTrue(file!!.name.endsWith(".gif"))
        assertTrue(file.exists() && file.length() > 0L)
        dir.deleteRecursively()
    }

    @Test
    fun `renderBitmap supports custom outline color and disabled outline`() {
        val stroke = Stroke(
            path = Path().apply { moveTo(10f, 10f); lineTo(60f, 10f) },
            color = Color.BLUE,
            strokeWidth = 10f,
            bounds = RectF(10f, 10f, 60f, 10f),
            style = PenStyle.BRUSH
        )
        val noOutline = StickerRenderer.renderBitmap(listOf(stroke), outlineWidthPx = 0f)
        val customOutline = StickerRenderer.renderBitmap(listOf(stroke), outlineWidthPx = 8f, outlineColor = Color.YELLOW)
        assertNotNull(noOutline)
        assertNotNull(customOutline)
        assertTrue(customOutline!!.width > noOutline!!.width)
    }

    @Test
    fun `renderBitmap supports pencil and fountain pen styles`() {
        val pencilStroke = Stroke(
            path = Path().apply { moveTo(10f, 10f); lineTo(80f, 30f) },
            color = Color.MAGENTA,
            strokeWidth = 12f,
            bounds = RectF(10f, 10f, 80f, 30f),
            style = PenStyle.PENCIL
        )
        val fountainStroke = Stroke(
            path = Path().apply { moveTo(10f, 40f); lineTo(80f, 70f) },
            color = Color.BLUE,
            strokeWidth = 12f,
            bounds = RectF(10f, 40f, 80f, 70f),
            style = PenStyle.FOUNTAIN
        )
        val bmp = StickerRenderer.renderBitmap(listOf(pencilStroke, fountainStroke))
        assertNotNull(bmp)
        assertTrue(bmp!!.width > 0 && bmp.height > 0)
    }
}

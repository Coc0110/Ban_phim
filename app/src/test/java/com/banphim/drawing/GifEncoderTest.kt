package com.banphim.drawing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class GifEncoderTest {
    @Test
    fun `encode produces valid transparent GIF89a decodable by BitmapFactory`() {
        val bmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        for (x in 8..15) {
            for (y in 8..15) {
                bmp.setPixel(x, y, Color.RED)
            }
        }

        val out = ByteArrayOutputStream()
        GifEncoder.encode(bmp, out)
        val bytes = out.toByteArray()

        assertTrue(bytes.size > 20)
        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        assertEquals(0x3B.toByte(), bytes.last())

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(decoded)
        assertEquals(24, decoded.width)
        assertEquals(24, decoded.height)
    }
}

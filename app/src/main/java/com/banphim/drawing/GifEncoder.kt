package com.banphim.drawing

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

object GifEncoder {

    private const val TRANSPARENT_INDEX = 0
    private const val MIN_CODE_SIZE = 8
    private const val CLEAR_CODE = 256
    private const val EOI_CODE = 257
    private const val MAX_TABLE_SIZE = 4096

    fun encode(bitmap: Bitmap, out: OutputStream) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val palette = buildPalette()
        val indices = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            indices[i] = if (Color.alpha(c) < 128) {
                TRANSPARENT_INDEX.toByte()
            } else {
                nearestPaletteIndex(c, palette).toByte()
            }
        }

        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShortLE(out, w)
        writeShortLE(out, h)
        out.write(0xF7) // Global Color Table (256 entries), 8-bit color res
        out.write(TRANSPARENT_INDEX)
        out.write(0x00)

        // Global Color Table (256 * 3 bytes)
        for (rgb in palette) {
            out.write((rgb shr 16) and 0xFF)
            out.write((rgb shr 8) and 0xFF)
            out.write(rgb and 0xFF)
        }

        // Netscape 2.0 Application Extension (infinite loop for animated GIF)
        out.write(0x21)
        out.write(0xFF)
        out.write(0x0B)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(0x03)
        out.write(0x01)
        writeShortLE(out, 0)
        out.write(0x00)

        val compressed = lzwCompress(indices)
        writeFrame(out, w, h, compressed, delayCs = 50)
        writeFrame(out, w, h, compressed, delayCs = 50)

        out.write(0x3B) // GIF Trailer
        out.flush()
    }

    private fun writeFrame(
        out: OutputStream,
        w: Int,
        h: Int,
        compressed: ByteArray,
        delayCs: Int
    ) {
        // Graphic Control Extension (transparent index 0)
        out.write(0x21)
        out.write(0xF9)
        out.write(0x04)
        out.write(0x09) // Disposal=2, Transparent=1
        writeShortLE(out, delayCs)
        out.write(TRANSPARENT_INDEX)
        out.write(0x00)

        // Image Descriptor
        out.write(0x2C)
        writeShortLE(out, 0)
        writeShortLE(out, 0)
        writeShortLE(out, w)
        writeShortLE(out, h)
        out.write(0x00)

        // LZW Image Data
        out.write(MIN_CODE_SIZE)
        var offset = 0
        while (offset < compressed.size) {
            val chunk = minOf(255, compressed.size - offset)
            out.write(chunk)
            out.write(compressed, offset, chunk)
            offset += chunk
        }
        out.write(0x00) // Block terminator
    }

    private fun buildPalette(): IntArray {
        val palette = IntArray(256)
        palette[0] = 0x000000 // Transparent placeholder
        var idx = 1
        // 6x6x6 RGB cube (indices 1..216)
        for (r in 0..5) {
            for (g in 0..5) {
                for (b in 0..5) {
                    palette[idx++] = Color.rgb(r * 51, g * 51, b * 51)
                }
            }
        }
        // 39 grayscale ramp entries (indices 217..255)
        val grayCount = 256 - idx
        for (i in 0 until grayCount) {
            val v = (i * 255f / (grayCount - 1)).roundToInt().coerceIn(0, 255)
            palette[idx++] = Color.rgb(v, v, v)
        }
        return palette
    }

    private fun nearestPaletteIndex(color: Int, palette: IntArray): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // Fast candidate from 6x6x6 cube
        val cr = (r / 51f).roundToInt().coerceIn(0, 5)
        val cg = (g / 51f).roundToInt().coerceIn(0, 5)
        val cb = (b / 51f).roundToInt().coerceIn(0, 5)
        var bestIdx = 1 + cr * 36 + cg * 6 + cb
        var bestDist = colorDistSq(r, g, b, palette[bestIdx])

        // Also check grayscale ramp (217..255) for crisp grays/whites/blacks
        for (i in 217..255) {
            val d = colorDistSq(r, g, b, palette[i])
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun colorDistSq(r: Int, g: Int, b: Int, rgb: Int): Int {
        val dr = r - ((rgb shr 16) and 0xFF)
        val dg = g - ((rgb shr 8) and 0xFF)
        val db = b - (rgb and 0xFF)
        return dr * dr + dg * dg + db * db
    }

    private fun lzwCompress(indices: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var bitBuffer = 0
        var bitCount = 0

        fun emit(code: Int, codeSize: Int) {
            bitBuffer = bitBuffer or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                out.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
            }
        }

        var codeSize = MIN_CODE_SIZE + 1
        var nextCode = EOI_CODE + 1
        val table = HashMap<Long, Int>(4096)

        emit(CLEAR_CODE, codeSize)
        if (indices.isEmpty()) {
            emit(EOI_CODE, codeSize)
            if (bitCount > 0) out.write(bitBuffer and 0xFF)
            return out.toByteArray()
        }

        var prefix = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val k = indices[i].toInt() and 0xFF
            val key = (prefix.toLong() shl 8) or k.toLong()
            val existing = table[key]
            if (existing != null) {
                prefix = existing
            } else {
                emit(prefix, codeSize)
                if (nextCode < MAX_TABLE_SIZE) {
                    table[key] = nextCode
                    if (nextCode == (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                    nextCode++
                } else {
                    emit(CLEAR_CODE, codeSize)
                    table.clear()
                    codeSize = MIN_CODE_SIZE + 1
                    nextCode = EOI_CODE + 1
                }
                prefix = k
            }
        }
        emit(prefix, codeSize)
        emit(EOI_CODE, codeSize)
        if (bitCount > 0) out.write(bitBuffer and 0xFF)
        return out.toByteArray()
    }

    private fun writeShortLE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }
}

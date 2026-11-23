package com.banphim.drawing

import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StrokeHistoryTest {
    private fun dummyStroke(color: Int = 0xFF000000.toInt()) = Stroke(
        path = Path().apply {
            moveTo(0f, 0f)
            lineTo(10f, 10f)
        },
        color = color,
        strokeWidth = 10f,
        bounds = RectF(0f, 0f, 10f, 10f)
    )

    @Test
    fun `undo and redo move strokes between stacks`() {
        val history = StrokeHistory()
        val s1 = dummyStroke(1)
        val s2 = dummyStroke(2)
        history.push(s1)
        history.push(s2)

        assertTrue(history.undo())
        assertEquals(listOf(s1), history.strokes())
        assertTrue(history.canRedo())

        assertTrue(history.redo())
        assertEquals(listOf(s1, s2), history.strokes())
        assertFalse(history.canRedo())
    }

    @Test
    fun `pushing new stroke after undo clears redo stack`() {
        val history = StrokeHistory()
        history.push(dummyStroke(1))
        history.undo()
        assertTrue(history.canRedo())

        history.push(dummyStroke(3))
        assertFalse(history.canRedo())
    }

    @Test
    fun `undo and redo on empty stacks return false`() {
        val history = StrokeHistory()
        assertFalse(history.undo())
        assertFalse(history.redo())
        assertFalse(history.clear())
    }
}

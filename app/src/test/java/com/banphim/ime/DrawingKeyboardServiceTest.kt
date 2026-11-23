package com.banphim.ime

import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import com.banphim.R
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.DrawingView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class DrawingKeyboardServiceTest {
    @Test
    fun `touching drawing view collapses open color strips`() {
        val service = Robolectric.buildService(DrawingKeyboardService::class.java).create().get()
        val root = service.onCreateInputView()

        val btnBg = root.findViewById<View>(R.id.btnBgColor)
        val stripBg = root.findViewById<ColorStripView>(R.id.stripBgColor)
        val drawingView = root.findViewById<DrawingView>(R.id.drawingView)

        btnBg.performClick()
        assertEquals(View.VISIBLE, stripBg.visibility)

        drawingView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
        assertEquals(View.GONE, stripBg.visibility)
    }

    @Test
    fun `send on unsupported field preserves strokes and shows toast`() {
        val service = Robolectric.buildService(DrawingKeyboardService::class.java).create().get()
        val root = service.onCreateInputView()

        val drawingView = root.findViewById<DrawingView>(R.id.drawingView)
        drawingView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0))
        drawingView.dispatchTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 30f, 30f, 0))
        assertEquals(1, drawingView.getStrokes().size)

        root.findViewById<ImageButton>(R.id.btnSend).performClick()

        assertEquals(1, drawingView.getStrokes().size)
        assertTrue(ShadowToast.shownToastCount() >= 1)
    }

    @Test
    fun `onEvaluateInputViewShown returns true even when hardware keyboard is present`() {
        val service = Robolectric.buildService(DrawingKeyboardService::class.java).create().get()
        assertTrue(service.onEvaluateInputViewShown())
    }
}

package com.banphim.drawing

import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.banphim.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrawingViewTest {
    @Test
    fun `action cancel discards active stroke without pushing to history`() {
        val view = DrawingView(ApplicationProvider.getApplicationContext())
        view.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0))
        view.dispatchTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_MOVE, 40f, 40f, 0))
        view.dispatchTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_CANCEL, 50f, 50f, 0))

        assertFalse(view.canUndo())
        assertEquals(0, view.getStrokes().size)
    }

    @Test
    fun `action down move up pushes stroke and invokes callbacks`() {
        val view = DrawingView(ApplicationProvider.getApplicationContext())
        var drawStarted = false
        var historyChangedCount = 0
        view.onDrawStart = { drawStarted = true }
        view.onHistoryChanged = { historyChangedCount++ }

        view.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0))
        view.dispatchTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_MOVE, 30f, 30f, 0))
        view.dispatchTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_UP, 30f, 30f, 0))

        assertTrue(drawStarted)
        assertEquals(1, historyChangedCount)
        assertTrue(view.canUndo())
        assertEquals(1, view.getStrokes().size)
    }

    @Test
    fun `drawing view measures to keyboard_height when height spec is unspecified or at_most`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = DrawingView(context)
        val expectedHeight = context.resources.getDimensionPixelSize(R.dimen.keyboard_height)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        assertEquals(expectedHeight, view.measuredHeight)
    }
}

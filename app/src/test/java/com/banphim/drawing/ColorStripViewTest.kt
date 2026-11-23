package com.banphim.drawing

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorStripViewTest {
    @Test
    fun `tapping vertical strip selects color at y position`() {
        val strip = ColorStripView(ApplicationProvider.getApplicationContext())
        strip.orientation = ColorStripView.Orientation.VERTICAL
        strip.colors = listOf(Color.BLACK, Color.RED, Color.BLUE)
        strip.measure(100, 300)
        strip.layout(0, 0, 100, 300)

        var picked: Int? = null
        strip.onColorSelected = { picked = it }

        strip.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 150f, 0))
        assertEquals(Color.RED, picked)
    }

    @Test
    fun `show and hide toggle visibility`() {
        val strip = ColorStripView(ApplicationProvider.getApplicationContext())
        strip.show()
        assertEquals(View.VISIBLE, strip.visibility)
        strip.hide()
        assertEquals(View.GONE, strip.visibility)
    }
}

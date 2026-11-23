package com.banphim.drawing

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorStripViewTest {
    @Test
    fun `tapping vertical strip selects color along gradient and updates thumb`() {
        val strip = ColorStripView(ApplicationProvider.getApplicationContext())
        strip.orientation = ColorStripView.Orientation.VERTICAL
        strip.measure(100, 300)
        strip.layout(0, 0, 100, 300)

        var picked: Int? = null
        strip.onColorSelected = { picked = it }

        strip.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 150f, 0))
        assertNotEquals(null, picked)
        assertNotEquals(Color.WHITE, picked)
        assertNotEquals(Color.BLACK, picked)
        assertTrue(strip.thumbProgress in 0.4f..0.6f)
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

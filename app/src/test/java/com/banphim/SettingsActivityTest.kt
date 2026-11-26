package com.banphim

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import com.banphim.drawing.ColorStripView
import com.banphim.ui.StickerPreviewView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {
    @Test
    fun `changing brush, grid, and outline settings persists and updates live preview`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.brushBrush).performClick()
        activity.findViewById<SwitchCompat>(R.id.switchShowGrid).isChecked = false

        val stripOutline = activity.findViewById<ColorStripView>(R.id.stripOutlineColor)
        stripOutline.measure(300, 40)
        stripOutline.layout(0, 0, 300, 40)
        // Tap far right (black) -> preview background should be white
        stripOutline.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 295f, 20f, 0))
        val preview = activity.findViewById<StickerPreviewView>(R.id.stickerPreview)
        assertEquals(Color.WHITE, preview.effectiveCanvasColor())

        // Tap far left (white) -> light outline auto-switches preview background to dark
        stripOutline.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5f, 20f, 0))
        assertEquals(Color.parseColor("#1C1E22"), preview.effectiveCanvasColor())

        activity.findViewById<SwitchCompat>(R.id.switchStickerOutline).isChecked = false

        val prefs = activity.getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        assertEquals("BRUSH", prefs.getString("pen_style", "NORMAL"))
        assertFalse(prefs.getBoolean("show_grid", true))
        assertFalse(prefs.getBoolean("sticker_outline_enabled", true))
    }
}

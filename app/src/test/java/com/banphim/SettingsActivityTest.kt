package com.banphim

import android.content.Context
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {
    @Test
    fun `changing brush, grid, and outline settings persists`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.brushPencil).performClick()
        activity.findViewById<SwitchCompat>(R.id.switchShowGrid).isChecked = false
        activity.findViewById<SwitchCompat>(R.id.switchStickerOutline).isChecked = false

        val prefs = activity.getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        assertEquals("PENCIL", prefs.getString("pen_style", "NORMAL"))
        assertFalse(prefs.getBoolean("show_grid", true))
        assertFalse(prefs.getBoolean("sticker_outline_enabled", true))
    }
}

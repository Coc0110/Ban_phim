package com.banphim

import android.content.Context
import android.graphics.Color
import android.widget.RadioButton
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
    fun `changing pen style, grid, and sticker outline persists to shared preferences`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        activity.findViewById<RadioButton>(R.id.rbPenGlow).performClick()
        activity.findViewById<SwitchCompat>(R.id.switchShowGrid).isChecked = false
        activity.findViewById<SwitchCompat>(R.id.switchStickerOutline).isChecked = false
        activity.findViewById<android.view.View>(R.id.outlineColorBlack).performClick()

        val prefs = activity.getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        assertEquals("GLOW", prefs.getString("pen_style", "NORMAL"))
        assertFalse(prefs.getBoolean("show_grid", true))
        assertFalse(prefs.getBoolean("sticker_outline_enabled", true))
        assertEquals(Color.BLACK, prefs.getInt("sticker_outline_color", Color.WHITE))
    }
}

package com.banphim

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @Test
    fun `main activity displays setup cards, status badges, test input, and ad slot`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(activity.findViewById<Button>(R.id.btnEnableKeyboard))
        assertNotNull(activity.findViewById<Button>(R.id.btnSwitchKeyboard))
        assertNotNull(activity.findViewById<TextView>(R.id.tvStep1Status))
        assertNotNull(activity.findViewById<TextView>(R.id.tvStep2Status))
        assertNotNull(activity.findViewById<EditText>(R.id.etTestInput))
        assertNotNull(activity.findViewById<ImageView>(R.id.ivStickerPreview))
        assertNotNull(activity.findViewById<View>(R.id.adBannerSlot))
    }
}

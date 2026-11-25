package com.banphim

import android.graphics.Color
import android.view.View
import android.widget.Button
import com.banphim.ui.GridPaperView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @Test
    fun `main activity displays banner toggle, settings button, grid paper, and ad slot`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(activity.findViewById<Button>(R.id.btnToggleKeyboard))
        assertNotNull(activity.findViewById<Button>(R.id.btnOpenSettings))
        assertNotNull(activity.findViewById<View>(R.id.adBannerSlot))
        assertNotNull(activity.findViewById<GridPaperView>(R.id.gridPaperBg))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `light mode uses warm cream paper and dark grid lines`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val paper = activity.findViewById<GridPaperView>(R.id.gridPaperBg)
        assertEquals(Color.parseColor("#FBF6E9"), paper.paperColor)
        assertEquals(0, Color.red(paper.gridLineColor))
        assertTrue(Color.alpha(paper.gridLineColor) > 0)
    }

    @Test
    @Config(qualifiers = "night")
    fun `dark mode uses dark paper and yellow grid lines`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val paper = activity.findViewById<GridPaperView>(R.id.gridPaperBg)
        assertEquals(Color.parseColor("#141518"), paper.paperColor)
        assertTrue(Color.red(paper.gridLineColor) > 200 && Color.green(paper.gridLineColor) > 180)
    }
}

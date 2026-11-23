package com.banphim

import android.widget.Button
import android.widget.EditText
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @Test
    fun `main activity displays setup buttons and test input`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(activity.findViewById<Button>(R.id.btnEnableKeyboard))
        assertNotNull(activity.findViewById<Button>(R.id.btnSwitchKeyboard))
        assertNotNull(activity.findViewById<EditText>(R.id.etTestInput))
    }
}

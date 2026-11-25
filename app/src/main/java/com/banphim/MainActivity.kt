package com.banphim

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleKeyboard: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggleKeyboard = findViewById(R.id.btnToggleKeyboard)
        btnToggleKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnOpenSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKeyboardStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            refreshKeyboardStatus()
        }
    }

    private fun refreshKeyboardStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        val isEnabled = imm?.enabledInputMethodList?.any { it.packageName == packageName } == true
        btnToggleKeyboard.text = getString(
            if (isEnabled) R.string.btn_keyboard_enabled else R.string.btn_enable_keyboard
        )
        btnToggleKeyboard.setTextColor(
            ContextCompat.getColor(
                this,
                if (isEnabled) R.color.status_done_text else R.color.send_bg
            )
        )
    }
}

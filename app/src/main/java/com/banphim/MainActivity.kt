package com.banphim

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStep1Status: TextView
    private lateinit var tvStep2Status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStep1Status = findViewById(R.id.tvStep1Status)
        tvStep2Status = findViewById(R.id.tvStep2Status)

        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnSwitchKeyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        val etTestInput = findViewById<EditText>(R.id.etTestInput)
        val ivPreview = findViewById<ImageView>(R.id.ivStickerPreview)

        ViewCompat.setOnReceiveContentListener(
            etTestInput,
            arrayOf("image/webp", "image/png", "image/*")
        ) { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            val uriContent = split.first
            if (uriContent != null && uriContent.clip.itemCount > 0) {
                val uri = uriContent.clip.getItemAt(0).uri
                ivPreview.setImageURI(uri)
                ivPreview.visibility = View.VISIBLE
            }
            split.second
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
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        val isSelected = defaultIme.startsWith("$packageName/")

        updateBadge(
            tvStep1Status,
            isEnabled,
            getString(R.string.status_enabled),
            getString(R.string.status_not_enabled)
        )
        updateBadge(
            tvStep2Status,
            isSelected,
            getString(R.string.status_selected),
            getString(R.string.status_not_selected)
        )
    }

    private fun updateBadge(badge: TextView, done: Boolean, doneText: String, pendingText: String) {
        badge.text = if (done) doneText else pendingText
        badge.setBackgroundResource(if (done) R.drawable.badge_done else R.drawable.badge_pending)
        badge.setTextColor(
            ContextCompat.getColor(
                this,
                if (done) R.color.status_done_text else R.color.status_pending_text
            )
        )
    }
}

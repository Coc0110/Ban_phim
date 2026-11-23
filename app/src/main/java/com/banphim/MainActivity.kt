package com.banphim

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            arrayOf("image/png", "image/*")
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
}

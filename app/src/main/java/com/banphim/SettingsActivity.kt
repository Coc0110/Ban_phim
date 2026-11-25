package com.banphim

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.banphim.drawing.PenStyle

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btnBackSettings).setOnClickListener { finish() }

        val rgPenStyle = findViewById<RadioGroup>(R.id.rgPenStyle)
        when (prefs.getString("pen_style", PenStyle.NORMAL.name)) {
            PenStyle.GLOW.name -> rgPenStyle.check(R.id.rbPenGlow)
            PenStyle.DASHED.name -> rgPenStyle.check(R.id.rbPenDashed)
            else -> rgPenStyle.check(R.id.rbPenNormal)
        }
        rgPenStyle.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.rbPenGlow -> PenStyle.GLOW
                R.id.rbPenDashed -> PenStyle.DASHED
                else -> PenStyle.NORMAL
            }
            prefs.edit().putString("pen_style", style.name).apply()
        }

        val switchShowGrid = findViewById<SwitchCompat>(R.id.switchShowGrid)
        switchShowGrid.isChecked = prefs.getBoolean("show_grid", true)
        switchShowGrid.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_grid", isChecked).apply()
        }

        val switchOutline = findViewById<SwitchCompat>(R.id.switchStickerOutline)
        val outlineColorSection = findViewById<View>(R.id.outlineColorSection)
        val outlineEnabled = prefs.getBoolean("sticker_outline_enabled", true)
        switchOutline.isChecked = outlineEnabled
        outlineColorSection.visibility = if (outlineEnabled) View.VISIBLE else View.GONE
        switchOutline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sticker_outline_enabled", isChecked).apply()
            outlineColorSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val swatches = listOf(
            findViewById<View>(R.id.outlineColorWhite) to Color.WHITE,
            findViewById<View>(R.id.outlineColorBlack) to Color.BLACK,
            findViewById<View>(R.id.outlineColorYellow) to Color.parseColor("#FFD54F"),
            findViewById<View>(R.id.outlineColorPink) to Color.parseColor("#FF80AB"),
            findViewById<View>(R.id.outlineColorBlue) to Color.parseColor("#4FC3F7")
        )
        var selectedColor = prefs.getInt("sticker_outline_color", Color.WHITE)

        fun refreshSwatches() {
            for ((view, color) in swatches) {
                val layer = view.background?.mutate() as? LayerDrawable
                val fill = layer?.findDrawableByLayerId(R.id.swatch_fill) as? GradientDrawable
                fill?.setColor(color)
                val selected = color == selectedColor
                view.scaleX = if (selected) 1.18f else 1.0f
                view.scaleY = if (selected) 1.18f else 1.0f
                view.alpha = if (selected) 1.0f else 0.72f
            }
        }

        for ((view, color) in swatches) {
            view.setOnClickListener {
                selectedColor = color
                prefs.edit().putInt("sticker_outline_color", color).apply()
                refreshSwatches()
            }
        }
        refreshSwatches()
    }
}

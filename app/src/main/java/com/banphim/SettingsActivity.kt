package com.banphim

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.PenStyle
import com.banphim.ui.BrushSampleView
import com.banphim.ui.StickerPreviewView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)
        val preview = findViewById<StickerPreviewView>(R.id.stickerPreview)

        findViewById<Button>(R.id.btnBackSettings).setOnClickListener { finish() }

        val brushSamples = listOf(
            findViewById<BrushSampleView>(R.id.brushNormal) to PenStyle.NORMAL,
            findViewById<BrushSampleView>(R.id.brushBrush) to PenStyle.BRUSH,
            findViewById<BrushSampleView>(R.id.brushPencil) to PenStyle.PENCIL,
            findViewById<BrushSampleView>(R.id.brushFountain) to PenStyle.FOUNTAIN
        )
        var currentStyle = runCatching {
            PenStyle.valueOf(prefs.getString("pen_style", PenStyle.NORMAL.name) ?: PenStyle.NORMAL.name)
        }.getOrDefault(PenStyle.NORMAL)

        fun refreshBrushSamples() {
            for ((view, style) in brushSamples) {
                view.penStyle = style
                view.isBrushSelected = style == currentStyle
            }
            preview.penStyle = currentStyle
        }

        for ((view, style) in brushSamples) {
            view.setOnClickListener {
                currentStyle = style
                prefs.edit().putString("pen_style", style.name).apply()
                refreshBrushSamples()
            }
        }
        refreshBrushSamples()

        val switchShowGrid = findViewById<SwitchCompat>(R.id.switchShowGrid)
        val showGrid = prefs.getBoolean("show_grid", true)
        switchShowGrid.isChecked = showGrid
        preview.showGrid = showGrid
        switchShowGrid.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_grid", isChecked).apply()
            preview.showGrid = isChecked
        }

        val switchOutline = findViewById<SwitchCompat>(R.id.switchStickerOutline)
        val outlineColorSection = findViewById<View>(R.id.outlineColorSection)
        val outlineEnabled = prefs.getBoolean("sticker_outline_enabled", true)
        switchOutline.isChecked = outlineEnabled
        outlineColorSection.visibility = if (outlineEnabled) View.VISIBLE else View.GONE
        preview.outlineEnabled = outlineEnabled
        switchOutline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sticker_outline_enabled", isChecked).apply()
            outlineColorSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            preview.outlineEnabled = isChecked
        }

        val stripOutline = findViewById<ColorStripView>(R.id.stripOutlineColor)
        stripOutline.orientation = ColorStripView.Orientation.HORIZONTAL
        stripOutline.autoHideOnRelease = false
        val savedOutlineColor = prefs.getInt("sticker_outline_color", Color.WHITE)
        stripOutline.setSelectedColor(savedOutlineColor)
        preview.outlineColor = savedOutlineColor

        stripOutline.onColorSelected = { color ->
            prefs.edit().putInt("sticker_outline_color", color).apply()
            preview.outlineColor = color
        }
    }
}

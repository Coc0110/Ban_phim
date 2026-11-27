package com.banphim

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.banphim.drawing.ColorStripView
import com.banphim.drawing.DrawingView
import com.banphim.drawing.PenStyle
import com.banphim.ime.KeyboardPanelController
import com.banphim.ime.StickerCommitter
import com.banphim.ui.BrushSampleView

class SettingsActivity : AppCompatActivity() {

    // Vung ma khi cham vao thi ScrollView khong duoc cuop cu chi (de ve / keo thanh mau)
    private var noScrollZones: List<View> = emptyList()
    private val hitRect = Rect()

    // Khung xem nhan dan: truot de len khung ve khi bam Gui
    private lateinit var canvasCard: ViewGroup
    private lateinit var stickerPreviewPanel: View
    private lateinit var stickerPreviewImage: ImageView

    // Nut Back cua he thong: khi dang xem nhan dan thi truot ve khung ve thay vi thoat man hinh
    private val previewBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideStickerPreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("banphim_prefs", Context.MODE_PRIVATE)

        findViewById<View>(R.id.btnBackSettings).setOnClickListener { finish() }

        // ---- Ban phim thu: dung chung giao dien + chuc nang voi ban phim that ----
        val keyboardRoot = findViewById<View>(R.id.keyboardRoot)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val panel = KeyboardPanelController(keyboardRoot, prefs) {
            showTestSticker(prefs, drawingView)
        }

        // Khung xem nhan dan nam de len khung ve (ben trong canvasCard, nen tu bo goc theo khung ve)
        canvasCard = keyboardRoot.findViewById(R.id.canvasCard)
        stickerPreviewPanel = layoutInflater.inflate(R.layout.test_sticker_preview, canvasCard, false)
        stickerPreviewImage = stickerPreviewPanel.findViewById(R.id.testStickerImage)
        stickerPreviewPanel.findViewById<View>(R.id.btnTestBack).setOnClickListener {
            hideStickerPreview()
        }
        canvasCard.addView(stickerPreviewPanel)
        onBackPressedDispatcher.addCallback(this, previewBackCallback)

        // ---- Kieu but ----
        val brushSamples = listOf(
            findViewById<BrushSampleView>(R.id.brushNormal) to PenStyle.NORMAL,
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
        }

        for ((view, style) in brushSamples) {
            view.setOnClickListener {
                currentStyle = style
                prefs.edit().putString("pen_style", style.name).apply()
                refreshBrushSamples()
                panel.applyUserPreferences()
            }
        }
        refreshBrushSamples()

        // ---- Nen o luoi ----
        val switchShowGrid = findViewById<SwitchCompat>(R.id.switchShowGrid)
        switchShowGrid.isChecked = prefs.getBoolean("show_grid", true)
        switchShowGrid.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_grid", isChecked).apply()
            panel.applyUserPreferences()
        }

        // ---- Vien nhan dan (ap dung khi bam Gui trong ban phim thu) ----
        val switchOutline = findViewById<SwitchCompat>(R.id.switchStickerOutline)
        val outlineColorSection = findViewById<View>(R.id.outlineColorSection)
        val outlineEnabled = prefs.getBoolean("sticker_outline_enabled", true)
        switchOutline.isChecked = outlineEnabled
        outlineColorSection.visibility = if (outlineEnabled) View.VISIBLE else View.GONE
        switchOutline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sticker_outline_enabled", isChecked).apply()
            outlineColorSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val stripOutline = findViewById<ColorStripView>(R.id.stripOutlineColor)
        stripOutline.orientation = ColorStripView.Orientation.HORIZONTAL
        stripOutline.autoHideOnRelease = false
        val outlineChip = findViewById<View>(R.id.outlineColorChip)
        fun updateOutlineChip(color: Int) {
            (outlineChip.background.mutate() as? GradientDrawable)?.setColor(color)
        }
        val savedOutlineColor = prefs.getInt("sticker_outline_color", Color.WHITE)
        stripOutline.setSelectedColor(savedOutlineColor)
        updateOutlineChip(savedOutlineColor)
        stripOutline.onColorSelected = { color ->
            prefs.edit().putInt("sticker_outline_color", color).apply()
            updateOutlineChip(color)
        }

        noScrollZones = listOf(keyboardRoot, stripOutline)
    }

    /**
     * Gia lap "gui nhan dan": ve ra file, truot khung xem nhan dan de len khung ve,
     * roi xoa canvas nhu ban phim that.
     */
    private fun showTestSticker(prefs: SharedPreferences, drawingView: DrawingView) {
        val strokes = drawingView.getStrokes()
        if (strokes.isEmpty()) return

        val file = KeyboardPanelController.renderSticker(
            this, prefs, strokes, StickerCommitter.MIME_PNG
        ) ?: return
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return

        stickerPreviewImage.setImageBitmap(bitmap)

        val distance = canvasCard.width.toFloat().coerceAtLeast(1f)
        stickerPreviewPanel.animate().cancel()
        stickerPreviewPanel.translationX = distance
        stickerPreviewPanel.visibility = View.VISIBLE
        previewBackCallback.isEnabled = true
        stickerPreviewPanel.animate()
            .translationX(0f)
            .setDuration(300L)
            .setInterpolator(DecelerateInterpolator())
            // Xoa canvas sau khi khung xem da phu kin, de khong thay canvas trong giua chung
            .withEndAction { drawingView.clear() }
            .start()
    }

    /** Truot khung xem nhan dan ra khoi man hinh de lo lai khung ve. */
    private fun hideStickerPreview() {
        previewBackCallback.isEnabled = false
        val distance = canvasCard.width.toFloat().coerceAtLeast(1f)
        stickerPreviewPanel.animate().cancel()
        stickerPreviewPanel.animate()
            .translationX(distance)
            .setDuration(260L)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { stickerPreviewPanel.visibility = View.GONE }
            .start()
    }

    /**
     * Trong ScrollView, vuot doc se bi ScrollView cuop lam gian doan net ve.
     * Khi ngon tay cham xuong vung ban phim thu (hoac thanh mau vien), chan ScrollView cuop cu chi.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            for (zone in noScrollZones) {
                if (zone.getGlobalVisibleRect(hitRect) &&
                    hitRect.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    zone.parent?.requestDisallowInterceptTouchEvent(true)
                    break
                }
            }
        }
        return handled
    }
}
# Drawing Sticker Keyboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight native Android IME (`InputMethodService`) that provides a minimal drawing pad with Undo/Redo/Clear, pen and canvas-background color strips, and one-tap transparent-background sticker sending via `InputConnectionCompat.commitContent()`.

**Architecture:** Native Kotlin Android app using traditional `View`s and XML layouts for instant IME cold-start and native `Service` lifecycle compatibility. Pure logic (`StrokeHistory`) and Android-graphics components (`StickerRenderer`, `ColorStripView`, `StickerCommitter`, `MainActivity`) are unit-tested on the JVM with JUnit4 + Robolectric so every task has a fast red-green-refactor cycle without requiring an emulator.

**Tech Stack:** Kotlin, Android SDK (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`), AndroidX Core (`core-ktx`, `InputConnectionCompat`, `EditorInfoCompat`, `FileProvider`), JUnit4, Robolectric.

**Spec:** `docs/superpowers/specs/2026-11-23-drawing-sticker-keyboard-design.md`

## Global Constraints

- Native Kotlin + traditional `View` / Custom `Canvas` & XML (no Jetpack Compose, no Flutter/React Native).
- Minimal permissions: no `SYSTEM_ALERT_WINDOW`, no Accessibility service, no storage/network/contacts permissions.
- Keyboard controls limited to: Top-left (Back to previous keyboard), Top-right (Canvas background color circle + vertical strip; Pen color circle to its left + horizontal strip), Bottom-left (Undo, Redo, Clear), Bottom-right (Send).
- Exported stickers must have a transparent background (`ARGB_8888`, never including the preview canvas background color) and a thin white outline for Dark Mode legibility.
- Auto-clear canvas only after a *successful* sticker send.

## Review Focus

- **Single-tap dot (zero-area stroke) on Send:** A tap without dragging produces a stroke whose path bounds have width/height = 0; `StickerRenderer` must expand bounds by stroke + outline radius so `Bitmap.createBitmap` never throws `IllegalArgumentException` (Task 3).
- **Send tapped on empty canvas:** Tapping Send with zero strokes must return `null` / no-op without sending a blank image or clearing state (Task 3 & Task 5).
- **Unsupported chat field (`commitContent` rejected or MIME mismatch):** When the target `EditorInfo` does not accept `image/png` or `commitContent` returns `false`, `StickerCommitter` must copy to clipboard / show a Toast and *not* clear the user's drawing (Task 5).
- **Undo/Redo stack consistency:** Drawing a new stroke after Undo must clear `redoStack`; calling Undo/Redo on empty stacks must be a safe no-op (Task 2).
- **Open color strip dismissed on canvas touch:** Starting to draw on `DrawingView` while a color strip is open must collapse the open strip cleanly (Task 5).

---

### Task 1: Android Project & Gradle Scaffolding + IME Registration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/method.xml`
- Create: `app/src/main/res/xml/file_paths.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Test: `app/src/test/java/com/banphim/ProjectConfigTest.kt`

**Interfaces:**
- Produces: Package `com.banphim`, `FileProvider` authority `"com.banphim.fileprovider"`, cache sticker directory `"stickers/"` in `file_paths.xml`, IME service declaration `.ime.DrawingKeyboardService`.

- [ ] **Step 1: Write the failing smoke test**

Create `app/src/test/java/com/banphim/ProjectConfigTest.kt`:
```kotlin
package com.banphim

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigTest {
    @Test
    fun `package and provider constants match spec`() {
        assertEquals("com.banphim.fileprovider", BuildConfig.APPLICATION_ID + ".fileprovider")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest` (or `gradle testDebugUnitTest`)
Expected: FAIL because Gradle build files and `AndroidManifest.xml` do not exist yet.

- [ ] **Step 3: Create Gradle build files, wrapper config, Manifest, and XML resources**

- `settings.gradle.kts`: `rootProject.name = "BanPhim"`, `include(":app")`, `pluginManagement` + `dependencyResolutionManagement` with `google()` and `mavenCentral()`.
- `build.gradle.kts`: `com.android.application` (8.7.3) and `org.jetbrains.kotlin.android` (2.0.21) with `apply false`.
- `app/build.gradle.kts`: `namespace = "com.banphim"`, `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`, `buildFeatures { buildConfig = true }`, dependencies: `androidx.core:core-ktx:1.15.0`, `androidx.appcompat:appcompat:1.7.0`, `junit:junit:4.13.2`, `org.robolectric:robolectric:4.14.1`, `androidx.test:core-ktx:1.6.1`. Enable `testOptions.unitTests.isIncludeAndroidResources = true`.
- `app/src/main/AndroidManifest.xml`: Declare `MainActivity` (`LAUNCHER`) and `DrawingKeyboardService` with `BIND_INPUT_METHOD` permission, `android.view.InputMethod` intent filter, `meta-data` pointing to `@xml/method`, and `FileProvider` (`authorities="${applicationId}.fileprovider"`, `grantUriPermissions="true"`, `@xml/file_paths`).
- `app/src/main/res/xml/method.xml`: `<input-method>` with `settingsActivity="com.banphim.MainActivity"`.
- `app/src/main/res/xml/file_paths.xml`: `<cache-path name="stickers" path="stickers/" />`.
- `app/src/main/res/values/strings.xml` and `colors.xml`: Vietnamese app name `"Bàn phím Vẽ Sticker"` and UI labels.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.ProjectConfigTest"`
Expected: PASS (note: requires Android SDK configured via `ANDROID_HOME` / `local.properties`).

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ app/
git commit -m "chore: scaffold Android IME project and FileProvider config"
```

---

### Task 2: Stroke Model, History Stack (Undo/Redo/Clear) & `DrawingView`

**Files:**
- Create: `app/src/main/java/com/banphim/drawing/Stroke.kt`
- Create: `app/src/main/java/com/banphim/drawing/StrokeHistory.kt`
- Create: `app/src/main/java/com/banphim/drawing/DrawingView.kt`
- Test: `app/src/test/java/com/banphim/drawing/StrokeHistoryTest.kt`

**Interfaces:**
- Produces:
  - `data class Stroke(val path: Path, val color: Int, val strokeWidth: Float, val bounds: RectF)`
  - `class StrokeHistory` with `push(stroke: Stroke)`, `undo(): Boolean`, `redo(): Boolean`, `clear(): Boolean`, `canUndo(): Boolean`, `canRedo(): Boolean`, `strokes(): List<Stroke>`
  - `class DrawingView(context: Context, attrs: AttributeSet? = null)` with `setPenColor(color: Int)`, `setCanvasColor(color: Int)`, `undo()`, `redo()`, `clear()`, `getStrokes(): List<Stroke>`, `canUndo(): Boolean`, `canRedo(): Boolean`, `var onHistoryChanged: (() -> Unit)?`, `var onDrawStart: (() -> Unit)?`

- [ ] **Step 1: Write the failing unit tests for `StrokeHistory`**

Create `app/src/test/java/com/banphim/drawing/StrokeHistoryTest.kt`:
```kotlin
package com.banphim.drawing

import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StrokeHistoryTest {
    private fun dummyStroke(color: Int = 0xFF000000.toInt()) =
        Stroke(Path().apply { moveTo(0f, 0f); lineTo(10f, 10f) }, color, 10f, RectF(0f, 0f, 10f, 10f))

    @Test
    fun `undo and redo move strokes between stacks`() {
        val history = StrokeHistory()
        val s1 = dummyStroke(1)
        val s2 = dummyStroke(2)
        history.push(s1)
        history.push(s2)

        assertTrue(history.undo())
        assertEquals(listOf(s1), history.strokes())
        assertTrue(history.canRedo())

        assertTrue(history.redo())
        assertEquals(listOf(s1, s2), history.strokes())
        assertFalse(history.canRedo())
    }

    @Test
    fun `pushing new stroke after undo clears redo stack`() {
        val history = StrokeHistory()
        history.push(dummyStroke(1))
        history.undo()
        assertTrue(history.canRedo())

        history.push(dummyStroke(3))
        assertFalse(history.canRedo())
    }

    @Test
    fun `undo and redo on empty stacks return false`() {
        val history = StrokeHistory()
        assertFalse(history.undo())
        assertFalse(history.redo())
        assertFalse(history.clear())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.StrokeHistoryTest"`
Expected: FAIL with unresolved reference `Stroke` / `StrokeHistory`.

- [ ] **Step 3: Implement `Stroke.kt`, `StrokeHistory.kt`, and `DrawingView.kt`**

- `Stroke.kt`: immutable data class holding `Path`, `color: Int`, `strokeWidth: Float`, `bounds: RectF`.
- `StrokeHistory.kt`: backed by `ArrayDeque<Stroke>` for `undoStack` and `redoStack`. `push` clears `redoStack`.
- `DrawingView.kt`: custom `View` using `StrokeHistory`, quadratic Bezier smoothing (`quadTo` between midpoints) on `ACTION_DOWN`/`ACTION_MOVE`/`ACTION_UP`, invokes `onDrawStart` on `ACTION_DOWN` and `onHistoryChanged` after `push`/`undo`/`redo`/`clear`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.StrokeHistoryTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/banphim/drawing/ app/src/test/java/com/banphim/drawing/StrokeHistoryTest.kt
git commit -m "feat: add StrokeHistory and DrawingView with undo/redo/clear"
```

---

### Task 3: Transparent Sticker Renderer (Background Removal, White Outline & Tight Cropping)

**Files:**
- Create: `app/src/main/java/com/banphim/drawing/StickerRenderer.kt`
- Test: `app/src/test/java/com/banphim/drawing/StickerRendererTest.kt`

**Interfaces:**
- Consumes: `Stroke` from Task 2.
- Produces: `object StickerRenderer` with:
  - `fun renderBitmap(strokes: List<Stroke>, maxSizePx: Int = 512, outlineWidthPx: Float = 8f, paddingPx: Float = 16f): Bitmap?`
  - `fun renderToPngFile(strokes: List<Stroke>, cacheDir: File): File?`

- [ ] **Step 1: Write the failing unit tests for `StickerRenderer`**

Create `app/src/test/java/com/banphim/drawing/StickerRendererTest.kt`:
```kotlin
package com.banphim.drawing

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class StickerRendererTest {
    @Test
    fun `empty strokes returns null`() {
        assertNull(StickerRenderer.renderBitmap(emptyList()))
    }

    @Test
    fun `single tap dot with zero-area bounds renders non-empty transparent bitmap`() {
        val dot = Stroke(Path().apply { moveTo(50f, 50f); lineTo(50f, 50f) }, Color.BLACK, 12f, RectF(50f, 50f, 50f, 50f))
        val bmp = StickerRenderer.renderBitmap(listOf(dot))
        assertNotNull(bmp)
        assertTrue(bmp!!.width > 0 && bmp.height > 0)
        assertEquals(Color.TRANSPARENT, bmp.getPixel(0, 0))
    }

    @Test
    fun `large drawing is scaled within maxSizePx and saved to PNG`() {
        val wide = Stroke(Path().apply { moveTo(0f, 0f); lineTo(1200f, 600f) }, Color.RED, 10f, RectF(0f, 0f, 1200f, 600f))
        val bmp = StickerRenderer.renderBitmap(listOf(wide), maxSizePx = 512)!!
        assertTrue(bmp.width <= 512 && bmp.height <= 512)

        val dir = createTempDirectory("stickers").toFile()
        val file = StickerRenderer.renderToPngFile(listOf(wide), dir)
        assertNotNull(file)
        assertTrue(file!!.exists() && file.length() > 0L)
        dir.deleteRecursively()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.StickerRendererTest"`
Expected: FAIL with unresolved reference `StickerRenderer`.

- [ ] **Step 3: Implement `StickerRenderer.kt`**

- Compute union `RectF` of all `Stroke.bounds`, expand by `maxStrokeWidth / 2f + outlineWidthPx + paddingPx` (guarantees > 0 width/height even for single-tap dots).
- Scale uniformly if `max(contentWidth, contentHeight) > maxSizePx`.
- Create `Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)` (transparent by default), translate/scale `Canvas`, draw each stroke first with white `Paint` (`strokeWidth + outlineWidthPx * 2`, `Cap.ROUND`, `Join.ROUND`), then draw each stroke with its own `color` and `strokeWidth`.
- `renderToPngFile`: write to `File(cacheDir, "stickers/sticker_${System.currentTimeMillis()}.png")` using `Bitmap.CompressFormat.PNG`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.StickerRendererTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/banphim/drawing/StickerRenderer.kt app/src/test/java/com/banphim/drawing/StickerRendererTest.kt
git commit -m "feat: add StickerRenderer for transparent PNG export with white outline"
```

---

### Task 4: Color Strip Picker View (Vertical & Horizontal Drag/Tap)

**Files:**
- Create: `app/src/main/java/com/banphim/drawing/ColorStripView.kt`
- Create: `app/src/main/res/drawable/circle_swatch_bg.xml`
- Test: `app/src/test/java/com/banphim/drawing/ColorStripViewTest.kt`

**Interfaces:**
- Produces: `class ColorStripView(context: Context, attrs: AttributeSet? = null)` with:
  - `enum class Orientation { VERTICAL, HORIZONTAL }`
  - `var orientation: Orientation`
  - `var colors: List<Int>` (default curated spectrum including black, white, grays, and vivid hues)
  - `var onColorSelected: ((Int) -> Unit)?`
  - `fun show()` / `fun hide()`

- [ ] **Step 1: Write the failing unit tests for `ColorStripView`**

Create `app/src/test/java/com/banphim/drawing/ColorStripViewTest.kt`:
```kotlin
package com.banphim.drawing

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorStripViewTest {
    @Test
    fun `tapping vertical strip selects color at y position`() {
        val strip = ColorStripView(ApplicationProvider.getApplicationContext())
        strip.orientation = ColorStripView.Orientation.VERTICAL
        strip.colors = listOf(Color.BLACK, Color.RED, Color.BLUE)
        strip.measure(100, 300)
        strip.layout(0, 0, 100, 300)

        var picked: Int? = null
        strip.onColorSelected = { picked = it }

        strip.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 150f, 0))
        assertEquals(Color.RED, picked)
    }

    @Test
    fun `show and hide toggle visibility`() {
        val strip = ColorStripView(ApplicationProvider.getApplicationContext())
        strip.show()
        assertEquals(View.VISIBLE, strip.visibility)
        strip.hide()
        assertEquals(View.GONE, strip.visibility)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.ColorStripViewTest"`
Expected: FAIL with unresolved reference `ColorStripView`.

- [ ] **Step 3: Implement `ColorStripView.kt` and `circle_swatch_bg.xml`**

- Draw rounded pill strip containing evenly-spaced color segments along `VERTICAL` (top-to-bottom) or `HORIZONTAL` (right-to-left or left-to-right).
- On `ACTION_DOWN` and `ACTION_MOVE`, map touch coordinate to color index (clamped `0 until colors.size`) and invoke `onColorSelected`. On `ACTION_UP`, invoke `hide()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.drawing.ColorStripViewTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/banphim/drawing/ColorStripView.kt app/src/main/res/drawable/circle_swatch_bg.xml app/src/test/java/com/banphim/drawing/ColorStripViewTest.kt
git commit -m "feat: add ColorStripView for vertical/horizontal color selection"
```

---

### Task 5: `DrawingKeyboardService` (`InputMethodService`) & `StickerCommitter`

**Files:**
- Create: `app/src/main/java/com/banphim/ime/StickerCommitter.kt`
- Create: `app/src/main/java/com/banphim/ime/DrawingKeyboardService.kt`
- Create: `app/src/main/res/layout/keyboard_drawing.xml`
- Test: `app/src/test/java/com/banphim/ime/StickerCommitterTest.kt`

**Interfaces:**
- Consumes: `DrawingView`, `StickerRenderer`, `ColorStripView`.
- Produces:
  - `object StickerCommitter` with `fun supportsPng(editorInfo: EditorInfo?): Boolean` and `fun commitSticker(inputConnection: InputConnection?, editorInfo: EditorInfo?, contentUri: Uri): Boolean`
  - `class DrawingKeyboardService : InputMethodService()`

- [ ] **Step 1: Write the failing unit tests for `StickerCommitter`**

Create `app/src/test/java/com/banphim/ime/StickerCommitterTest.kt`:
```kotlin
package com.banphim.ime

import android.net.Uri
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StickerCommitterTest {
    @Test
    fun `supportsPng returns true when editor accepts image png or wildcard`() {
        val infoPng = EditorInfo().apply { EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/png")) }
        val infoWild = EditorInfo().apply { EditorInfoCompat.setContentMimeTypes(this, arrayOf("image/*")) }
        val infoNone = EditorInfo()

        assertTrue(StickerCommitter.supportsPng(infoPng))
        assertTrue(StickerCommitter.supportsPng(infoWild))
        assertFalse(StickerCommitter.supportsPng(infoNone))
    }

    @Test
    fun `commitSticker returns false when inputConnection is null or unsupported`() {
        val uri = Uri.parse("content://com.banphim.fileprovider/stickers/test.png")
        assertFalse(StickerCommitter.commitSticker(null, EditorInfo(), uri))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.ime.StickerCommitterTest"`
Expected: FAIL with unresolved reference `StickerCommitter`.

- [ ] **Step 3: Implement `StickerCommitter.kt`, `keyboard_drawing.xml`, and `DrawingKeyboardService.kt`**

- `StickerCommitter.kt`: checks `EditorInfoCompat.getContentMimeTypes(editorInfo)` for `"image/png"` or `"image/*"`, constructs `InputContentInfoCompat(contentUri, ClipDescription("Drawing Sticker", arrayOf("image/png")), null)`, calls `InputConnectionCompat.commitContent(inputConnection, editorInfo, info, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null)`.
- `keyboard_drawing.xml`: `FrameLayout` with `DrawingView` full-bleed, plus:
  - Top-left: Back to previous keyboard `ImageButton`
  - Top-right: Background color circle `View` (with contrasting border) + vertical `ColorStripView` anchored below it; Pen color circle `View` to the left of Background button + horizontal `ColorStripView` anchored to its left
  - Bottom-left: `LinearLayout` with Undo, Redo, Clear `ImageButton`s
  - Bottom-right: Send `ImageButton`
- `DrawingKeyboardService.kt`: wires all buttons, updates Undo/Redo/Clear `isEnabled` + `alpha` on `onHistoryChanged`, collapses open color strips on `onDrawStart`, switches IME on Back (`switchToPreviousInputMethod()` with `showInputMethodPicker()` fallback), and on Send renders PNG -> `FileProvider.getUriForFile` -> `StickerCommitter.commitSticker`: if `true`, `drawingView.clear()`; if `false` (unsupported field), copies URI to `ClipboardManager` and shows a brief Vietnamese `Toast` without clearing the drawing.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.ime.StickerCommitterTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/banphim/ime/ app/src/main/res/layout/keyboard_drawing.xml app/src/test/java/com/banphim/ime/StickerCommitterTest.kt
git commit -m "feat: implement DrawingKeyboardService and StickerCommitter"
```

---

### Task 6: Minimal Onboarding `MainActivity` & Full Verification

**Files:**
- Create: `app/src/main/java/com/banphim/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`
- Test: `app/src/test/java/com/banphim/MainActivityTest.kt`

**Interfaces:**
- Consumes: `InputMethodManager`, `Settings.ACTION_INPUT_METHOD_SETTINGS`.
- Produces: `MainActivity` with 2-step setup buttons and a rich-content-enabled test `EditText` (`EditorInfoCompat.setContentMimeTypes(..., arrayOf("image/png"))`).

- [ ] **Step 1: Write the failing unit test for `MainActivity`**

Create `app/src/test/java/com/banphim/MainActivityTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.banphim.MainActivityTest"`
Expected: FAIL with unresolved reference `MainActivity`.

- [ ] **Step 3: Implement `MainActivity.kt` and `activity_main.xml`**

- `btnEnableKeyboard` opens `Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)`.
- `btnSwitchKeyboard` calls `(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()`.
- `etTestInput` overrides `onCreateInputConnection` to advertise `image/png` via `EditorInfoCompat.setContentMimeTypes` and displays a preview `ImageView` when a sticker is committed.

- [ ] **Step 4: Run all unit tests and verify build**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: All tests across `ProjectConfigTest`, `StrokeHistoryTest`, `StickerRendererTest`, `ColorStripViewTest`, `StickerCommitterTest`, and `MainActivityTest` PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/banphim/MainActivity.kt app/src/main/res/layout/activity_main.xml app/src/test/java/com/banphim/MainActivityTest.kt
git commit -m "feat: add minimal onboarding MainActivity with test input"
```

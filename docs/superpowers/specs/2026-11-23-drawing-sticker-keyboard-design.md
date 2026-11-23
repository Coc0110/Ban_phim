# Drawing Sticker Keyboard (Android IME) — Design Document

**Date:** 2026-11-23  
**Status:** Ready for review (builder check complete)

---

## 1. Intent & Why

Users want a lightweight, zero-clutter way to hand-draw text or quick doodles inside any Android chat app (Messenger, Zalo, Telegram, etc.) and send them immediately as background-removed stickers. Rather than replacing the user's daily typing keyboard or requiring a complex drawing studio, this app acts as a dedicated drawing pad IME that sits alongside the system's default keyboard and switches back in one tap.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **Dedicated Drawing IME:** Register as a standard Android `InputMethodService` so users can switch to it from Android's built-in keyboard switcher whenever they want to draw a sticker.
- **Minimal Drawing Pad UI:** Keep the keyboard uncluttered with only the essential controls:
  1. **Top-left:** Return to previous/default keyboard button.
  2. **Top-right:** Canvas background color button (small bordered circle showing current canvas color; tapping opens a vertical color strip downward to drag/pick color).
  3. **Top-right (to the left of the background button):** Pen color button (tapping opens a horizontal color strip to the left to pick pen color).
  4. **Bottom-left *(placement call)*:** Undo, Redo, and Clear buttons.
  5. **Bottom-right:** Send button.
- **Automatic Background Removal (Transparent Sticker):** Export only the user's drawn strokes onto a transparent background (ignoring the preview canvas background color) and send directly into the active chat input field.
- **Minimal Setup:** A simple, clear onboarding screen to enable the keyboard in Android Settings with no unnecessary configuration.

### Non-Goals
- Full QWERTY or alphanumeric typing support.
- Handwriting-to-text recognition (OCR) or font conversion.
- Cloud sticker packs, user accounts, or social features.
- Multi-layer editing, brush types, or complex drawing studio tools.

### Anti-Goals
- Slow cold-start or laggy stroke rendering when switching keyboards.
- Overlay/Accessibility hacks (`SYSTEM_ALERT_WINDOW`) to inject buttons onto third-party keyboards.
- Asking for unrelated permissions (contacts, storage, location, network).

---

## 3. Platform Constraints (Android IME)

1. **Keyboard Switching:** Android does not allow an app to inject a custom button onto another keyboard's UI (e.g., Gboard, Samsung Keyboard). Users switch *into* this drawing keyboard using Android's system keyboard switcher icon, and tap the **top-left button** inside this keyboard to return immediately to their previous keyboard (`switchToPreviousInputMethod()`).
2. **Sending Stickers:** An IME cannot intercept the chat app's own Send button. Therefore, the **Send button** lives on the bottom-right of the drawing pad itself. Tapping it renders the transparent sticker and pushes it into the chat app via `InputConnectionCompat.commitContent()` (the standard Android Rich Content IME API).

---

## 4. Approaches Considered

1. **Kotlin + Traditional Android `View` / Custom `Canvas` & XML (Chosen)**
   - Uses `InputMethodService` with a lightweight XML layout and a custom `DrawingView` (`android.graphics.Canvas` + `Path`).
   - **Trade-offs:** View animations for the expanding color strips are written imperatively, but APK size is minimal, keyboard startup is near-instant, `InputMethodService` lifecycle integration is native and rock-solid, and `Bitmap`/`Canvas` sticker export is direct with zero translation overhead.
2. **Kotlin + Jetpack Compose in `InputMethodService` (Rejected)**
   - Declarative UI for animations, but `InputMethodService` is a `Service` (not an `Activity`), requiring manual `LifecycleOwner`/`SavedStateRegistryOwner` wiring, higher memory/first-frame startup cost, and still dropping to `android.graphics.Canvas`/`Bitmap` for export.
3. **Cross-platform Frameworks (Flutter / React Native) (Dropped)**
   - Unsuitable for an Android `InputMethodService` due to engine startup latency and poor IME API integration.

---

## 5. Architecture & Behavior

### 5.1 Keyboard UI & Controls (`InputMethodService`)
- **Drawing Surface:** Fills the keyboard window. Supports smooth touch drawing (quadratic Bezier smoothing between touch points). Displays strokes over the currently selected **canvas background color** (for comfortable viewing while drawing; does *not* appear in the exported sticker).
- **Top-Left — Back to Keyboard Button:** Calls `switchToPreviousInputMethod()`. If no previous IME is recorded by the system, falls back to `InputMethodManager.showInputMethodPicker()`.
- **Top-Right — Canvas Background Color Button:** A small circle showing the active canvas background color, surrounded by a visible contrasting border so it never blends into the canvas. Tapping toggles a **vertical color strip** expanding downward from the button; the user can drag or tap along the strip to change the canvas preview background color. Tapping outside or completing selection collapses the strip.
- **Left of Background Button — Pen Color Button:** A small circle showing the active pen color (with a subtle border). Tapping toggles a **horizontal color strip** expanding to the left; the user can drag or tap along the strip to pick a pen color.
- **Bottom-Left — Undo, Redo, and Clear Buttons *(placement call)*:**
  - `DrawingView` maintains a `strokes` list and a `redoStack` (each stroke stores its `Path` and pen color).
  - **Undo:** Moves the most recent stroke from `strokes` to `redoStack` and redraws. Disabled/dimmed when `strokes` is empty.
  - **Redo:** Restores the most recent stroke from `redoStack` back to `strokes` and redraws. Drawing a new stroke clears `redoStack`. Disabled/dimmed when `redoStack` is empty.
  - **Clear:** Clears all current strokes on the canvas (and `redoStack`). Disabled/dimmed when `strokes` is empty.
- **Bottom-Right — Send Button:** Triggers sticker export and `commitContent()`. After a successful send, the canvas automatically clears its strokes and `redoStack` so it is ready for the next drawing *(builder call: auto-clear on send)*.

### 5.2 Transparent Sticker Export & `commitContent`
- **Background Removal:** Render only the recorded `Path` strokes onto a fresh `Bitmap.Config.ARGB_8888` transparent bitmap (never include the preview canvas background color).
- **Dark-Mode Legibility *(Proposed/Builder Call)*:** Draw a subtle thin white outline (halo/stroke) behind the colored strokes during export so dark ink (e.g., black) remains readable on Dark Mode chat bubbles, while light ink remains readable on Light Mode bubbles.
- **Cropping & Sizing:** Trim transparent margins to the bounding box of the strokes plus a small padding, and scale down if larger than a reasonable sticker dimension (e.g., max 512×512 px) before encoding to PNG (or WebP).
- **Delivery:** Save temporarily to app cache and pass via `FileProvider` (`FLAG_GRANT_READ_URI_PERMISSION`) using `InputConnectionCompat.commitContent()` when `EditorInfoCompat.getContentMimeTypes(editorInfo)` accepts `image/png` (or `image/*` / `image/webp`).
- **Unsupported Field Fallback *(Builder Call)*:** If the active input field does not support rich content (`commitContent` returns false or MIME types don't match), show a brief Toast informing the user that the current app/field does not support direct sticker insertion (and optionally copy to clipboard).

### 5.3 Onboarding (`MainActivity`)
- Minimal single-screen launcher Activity with two clear actions:
  1. Enable the keyboard in Android Settings (`Settings.ACTION_INPUT_METHOD_SETTINGS`).
  2. Switch/test the keyboard (`InputMethodManager.showInputMethodPicker()` + a small test input box).

---

## 6. Left to the Builder

- Gradle / Android SDK versions (`compileSdk`, `targetSdk`, `minSdk` — e.g., `minSdk = 26`).
- Exact color spectrum / palette implementation for the vertical and horizontal color strips (continuous hue/luminance gradient vs. curated swatches).
- Exact keyboard height, button dimensions, touch slop, stroke width, and white-outline width.
- `FileProvider` authority naming, cache cleanup policy for temporary sticker files, and unit/instrumentation tests for bitmap cropping/transparency and `commitContent` MIME negotiation.

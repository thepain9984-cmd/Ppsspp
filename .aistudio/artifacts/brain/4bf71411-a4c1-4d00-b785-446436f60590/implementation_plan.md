# Custom Controls - On-Screen Floating Gamepad Overlay

Custom Controls is an advanced Android utility and floating gamepad overlay that enables mobile gamers to control touch-based games using classic console-style controls (D-Pad, 360-degree analog joystick, PlayStation-style action glyphs, bumpers, and triggers). It uses an Android `AccessibilityService` (`dispatchGesture`) to simulate low-latency screen taps, holds, and sweeps directly over underlying games, paired with a floating `WindowManager` overlay HUD and an interactive in-app target mapping playground.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The core design directions have been aligned based on user preferences. Review the critical architectural choices below before proceeding with execution.

- **Gamepad Aesthetic (Confirmed)**: Classic PSP-style translucent smoked slate glass (`#141824` at adjustable 20%–95% alpha) with crisp geometric glyphs (Square, Cross, Circle, Triangle) and subtle frosted rim lighting.
- **Touch Dispatch Architecture (Confirmed)**: Low-latency dual-mode touch dispatch using Android Accessibility `dispatchGesture`:
  - Instant taps: 1–25ms duration `Path` dispatch on mapped screen targets.
  - Hold & Press: Chained `StrokeDescription` gestures for continuous button holding while the finger rests on the virtual button.
  - 360° Analog Joystick: Dynamic gesture path interpolation tracing the touch thumb vector relative to the joystick target center, smoothly sweeping within configured touch radius.
- **Mapping & Test Experience (Confirmed)**: Full interactive test playground in the main app featuring target coordinate markers, real-time simulated touch ripples, draggable mapped anchors, and visual gesture verification.
- **Profile Storage Strategy**: Lightweight, type-safe Jetpack DataStore / SharedPreferences JSON serialization to persist multiple game presets (e.g. "Genshin Impact", "Call of Duty", "Emulated JRPG", "Default").

---

## 1. Overview & Core Concept

- **What It Does**: Runs as a lightweight foreground overlay service that floats a full console-grade controller over any active game. Tapping or dragging on-screen controller elements dynamically dispatches corresponding hardware touch events to mapped coordinates on the user's screen.
- **Target Audience**: Mobile gamers, retro emulator players (PSP, PS1, N64, GBA), and accessibility-focused users seeking ergonomic, consistent physical-style gamepad layouts without needing physical Bluetooth hardware.
- **Key Value**: Universal touchscreen mapping without requiring root access, ADB pairing, or kernel modifications—leveraging official Android Accessibility APIs and `WindowManager` floating views.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Permission Onboarding Flow**:
   - Welcome dashboard with clear, step-by-step permission cards:
     - **Overlay Permission (`SYSTEM_ALERT_WINDOW`)**: One-tap direct intent to system overlay settings with live status check.
     - **Accessibility Service (`GameTouchAccessibilityService`)**: Direct intent to Accessibility settings with guided dialog explaining *why* it's required (gesture simulation).
     - **Battery Optimization / Foreground Service**: Guide to ensure the overlay isn't killed during heavy 3D gameplay.
2. **Main Dashboard & Game Profiles**:
   - List of game profiles with active selection, duplicate, rename, and add new preset.
   - Quick settings: Master Overlay Toggle (Active / Inactive), Transparency Slider (10% to 100%), Button Scale Slider (75% to 150%), Haptic Feedback Toggle.
   - Profile editor for button positions, target touch positions, and deadzone sensitivity.
3. **Interactive Target Mapping Playground**:
   - A dedicated full-screen mock game canvas in the app where users can position both the **Virtual Gamepad Controls** and the **Target Touch Markers**.
   - Live visual touch ripples appear when pressing buttons so users can instantly verify touch precision before launching real games.
4. **In-Game Floating Overlay HUD**:
   - Compact floating pill/bubble widget docked to screen edges.
   - Tap pill to expand/collapse gamepad overlay.
   - Long-press or gear icon to toggle **Edit Mode** directly in-game: drag target dots onto the game's actual attack/jump/joystick buttons, then lock layout.

### Visual Identity & Theme

- **Aesthetic Direction**: Sleek Cyber-Tactile & Classic PSP aesthetic. Dark charcoal surfaces with high-contrast geometric markers, subtle neon cyan accents (`#00E5FF`) for active/pressed states, and soft frosted glass blur (`#10141E` overlay).
- **Color Palette**:
  - `Background`: Deep Midnight Slate (`#0B0E14`)
  - `Surface / Cards`: Translucent Graphite (`#161B26`)
  - `Primary Accent`: Electric Cyan (`#00E5FF`)
  - `Glyph Accents`: PlayStation Quad Colors (Cross: Blue `#2979FF`, Circle: Coral `#FF5252`, Triangle: Emerald `#00E676`, Square: Magenta `#E040FB`)
  - `Overlay Alpha`: Adjustable 0.20 to 0.95 with smooth alpha blending
- **Typography & Icons**:
  - Bold, clean sans-serif typography with high legibility.
  - Custom vector canvas rendering for authentic D-Pad cross, concave analog stick cap, and trigger bumpers with crisp tactile feedback.

---

## 3. Key Product Decisions & Trade-Offs

- **Accessibility Gesture Dispatch vs. Virtual Input Device**:
  - *Chosen Approach*: `AccessibilityService.dispatchGesture(GestureDescription, ...)`
  - *Why*: Standard Android security prevents background services from injecting `MotionEvent` without root or root-level `uinput`. `dispatchGesture` is the official, safe, non-root mechanism available across Android 8.0–14+ for programmatic touch input.
  - *Mitigations*: To prevent gesture queue latency, gestures are dispatched with minimal duration (1–20ms) and cancelled immediately on touch up.
- **Overlay Rendering: WindowManager with Compose View**:
  - *Chosen Approach*: `ComposeView` wrapped in a native Android foreground `Service` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
  - *Why*: Allows full Jetpack Compose reactive state, smooth animations, and high performance while staying decoupled from the main activity lifecycle.
- **Dual Overlay Layers**:
  - *Control Layer*: Receives gamepad touches (`FLAG_NOT_TOUCH_MODAL`, `FLAG_WATCH_OUTSIDE_TOUCH`).
  - *Target Indicator Layer*: Shown only during "Mapping/Edit Mode" so players can drag target crosshairs over game buttons.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Custom Controls App                           │
├──────────────────────────────┬─────────────────────────────────────────┤
│        UI Layer              │             Services Layer              │
│  ┌────────────────────────┐  │  ┌────────────────────────────────────┐ │
│  │ MainActivity           │  │  │ OverlayService                     │ │
│  │ - Dashboard & Status   │  │  │ - Floating Bubble & WindowManager  │ │
│  │ - Profile Management   │  │  │ - Gamepad HUD ComposeView          │ │
│  │ - Mapping Playground   │  │  │ - Target Mapping Overlay           │ │
│  │ - Permission Guide     │  │  └─────────────────┬──────────────────┘ │
│  └───────────┬────────────┘  │                    │                    │
│              │               │                    │ Events & Taps      │
│              ▼               │                    ▼                    │
│  ┌────────────────────────┐  │  ┌────────────────────────────────────┐ │
│  │ GamepadRepository      │  │  │ GameTouchAccessibilityService      │ │
│  │ - SharedPreferences /  │◄─┼──┤ - AccessibilityService             │ │
│  │   DataStore Profiles   │  │  │ - dispatchGesture(Path, duration)  │ │
│  │ - Active Layout State  │  │  │ - Continuous Stroke Hold & Drag    │ │
│  └────────────────────────┘  │  └─────────────────┬──────────────────┘ │
└──────────────────────────────┴────────────────────┼────────────────────┘
                                                    │
                                                    ▼
                                   ┌─────────────────────────────────┐
                                   │      Android OS / Game App      │
                                   │  - Hardware Touch Synthesizer   │
                                   │  - Game screen receives taps    │
                                   └─────────────────────────────────┘
```

### Component Breakdown

1. **`GameTouchAccessibilityService.kt`**:
   - Extends `AccessibilityService`.
   - Implements `tap(x, y)`, `pressDown(x, y)`, `moveTo(x, y)`, and `release(x, y)`.
   - Uses `GestureDescription.Builder` with `StrokeDescription(path, 0, duration, willContinue)`.
   - Exposes a thread-safe singleton or bound binder for ultra-low latency gesture dispatch from the overlay.
2. **`OverlayService.kt`**:
   - Manages floating `WindowManager` instances (`TYPE_APPLICATION_OVERLAY`).
   - Hosts floating menu button, full gamepad HUD, and drag-and-drop mapping markers.
   - Supports minimize to floating bubble, opacity control, and layout presets.
3. **`GamepadRepository.kt` & Models**:
   - `GamepadProfile`: Profile name, button mapping entries list (`ControlKey`, screen position `x, y`, target position `targetX, targetY`, size, enabled).
   - Default layouts configured for standard landscape / portrait gaming.
4. **Gamepad UI Components**:
   - `DPadView`: 4-way cross with directional touch detection and multi-press diagonals.
   - `AnalogJoystickView`: 360-degree floating analog thumbstick with spring return and vector angle calculations.
   - `ActionButtonCluster`: PlayStation glyph cluster (Triangle, Circle, Cross, Square).
   - `ShoulderTriggers`: L1, R1, L2, R2 bumpers.
   - `MappingPlaygroundScreen`: Visual calibration screen with interactive mock target targets and simulated game canvas.

---

## 5. Implementation Steps

1. **Manifest & Configuration**:
   - Declare `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `VIBRATE`.
   - Configure `<service android:name=".service.GameTouchAccessibilityService" ...>` with `accessibility_service_config.xml`.
   - Configure `<service android:name=".service.OverlayService" ...>`.
2. **Accessibility & Touch Engine**:
   - Create `GameTouchAccessibilityService.kt` with robust `dispatchGesture` stroke queue handling.
3. **Data Layer**:
   - Create data models (`GamepadProfile`, `ButtonMapping`, `ControlKey`).
   - Implement `GamepadProfileRepository` with default profiles and JSON persistence.
4. **Overlay Service & Views**:
   - Implement `OverlayService.kt` with `WindowManager` parameters and lifecycle controls.
   - Build custom Compose gamepad widgets (Analog Joystick, D-Pad, Glyphs, Triggers).
5. **Main Application UI**:
   - Permission status banner & setup guides.
   - Profile manager (create, switch, delete).
   - Interactive Touch Playground with live feedback.
   - Step-by-step setup documentation dialog for Android Studio & real-device deployment.

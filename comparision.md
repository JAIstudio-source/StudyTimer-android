# StudyTimer vs. Study Circle: Deep UI & Resource Comparison

> **Analysis Date:** November 3, 2026  
> **Source App Under Development:** **StudyTimer** (`com.madeby.JAI`) — 100% Native Android (Kotlin)  
> **Benchmarked App:** **Study Circle** (Extracted Flutter Core + Native Android Host & Assets)  
> **Goal:** Identify every UI component, visual asset, audio effect, and UX workflow from the comparison app that can be adapted to make StudyTimer significantly more polished, intuitive, and feature-complete.

---

## Quick Navigation / Phase Index

* [Phase 1: Architecture & Technical Foundations](#phase-1-architecture--technical-foundations)
* [Phase 2: Main Timer Screen & Interaction Comparison (Small to Big)](#phase-2-main-timer-screen--interaction-comparison)
* [Phase 3: Ambient White Noise & Study Soundscapes (11 Atmospheric Loops + 5 Chimes)](#phase-3-ambient-white-noise--study-soundscapes)
* [Phase 4: Aesthetic Paper & Desk Textures (Minimalist Backgrounds)](#phase-4-aesthetic-paper--desk-textures)
* [Phase 5: Home-Screen Widgets (Matrix Progress Grid & Launcher Task List)](#phase-5-home-screen-widgets)
* [Phase 6: Student Avatar Maker System (Layered Vector Customizer)](#phase-6-student-avatar-maker-system)
* [Phase 7: Study Mascot, Companions & Emotional UX](#phase-7-study-mascot-companions--emotional-ux)
* [Phase 8: Mindful Breaks & Wellbeing UX ("Breathe" & "Mood Check-in")](#phase-8-mindful-breaks--wellbeing-ux)
* [Phase 9: OEM Background Reliability Guides (Xiaomi / Samsung / Vivo / Huawei / Asus)](#phase-9-oem-background-reliability-guides)
* [Phase 10: Tabular Numeric Typography & Squircle Curvature](#phase-10-tabular-numeric-typography--squircle-curvature)
* [Phase 11: Android 13+ Per-App Language Selector](#phase-11-android-13-per-app-language-selector)
* [Phase 12: Usable Files Inventory (Ready for Immediate Import)](#phase-12-usable-files-inventory)
* [Phase 13: Actionable Implementation Roadmap (Choose What to Build First)](#phase-13-actionable-implementation-roadmap)

---

## Phase 1: Architecture & Technical Foundations

```
+------------------------------------+------------------------------------+
| StudyTimer (Our App)               | Study Circle (Comparison App)      |
+------------------------------------+------------------------------------+
| * 100% Native Kotlin               | * Hybrid Flutter Engine            |
| * Low-latency Canvas Renderers     | * Heavy runtime libapp.so          |
| * Custom View Hierarchy            | * High memory footprint            |
| * Native Background Services       | * Platform channel serialization   |
| * OLED / Battery Optimized         | * Battery heavy during animations  |
+------------------------------------+------------------------------------+
```

### Analysis & Verdict
* **StudyTimer holds a massive technical advantage** in battery consumption, memory overhead, frame stability, and foreground service reliability.
* **Study Circle’s advantage lies purely in aesthetic polish:** softer color balancing, friendly micro-illustrations, calm study desk textures, and multi-sensory feedback (ambient soundscapes + tactile gestures).
* **The Strategy:** Keep StudyTimer’s high-performance native Kotlin core, while borrowing Study Circle’s best UI patterns, audio assets, visual widgets, and emotional design.

---

## Phase 2: Main Timer Screen & Interaction Comparison

A side-by-side comparison of the core timer experience (based on `FocusPanelBuilder.kt` vs `timerStyle1.jpg` – `timerStyle4.jpg`):

```
+------------------------------------------+------------------------------------------+
| StudyTimer Focus Screen                  | Study Circle Timer Screen                |
+------------------------------------------+------------------------------------------+
| [Profile / Streak]        [Settings Gear]| [(X) Close]      Timer          [...]    |
| [• Physics] [Target Goal ✕] [Chapters ▾] |                                          |
|                                          |            /------------\                |
|             .------------.               |          /       0        \              |
|           /       25       \             |         | 50   [25min]  10 |   <-- Arc    |
|          |      00 : 00     |            |         |         \        |     Slider   |
|           \   FOCUS FLOW   /             |          \ 40    (o)    20 /            |
|             '------------'               |            \-----30-----/               |
|                                          |                                          |
|                                          |          [ Timer | Stopwatch ]   <-- Pill |
|   [ Pause ]   [  Start  ]   [ Finish(H) ]|                                          |
|                                          |    (♫)         (▶)         (✓)   <-- Circ |
|                                          |  Ambient      Start       Done     Btns  |
|                                          |                                          |
|                                          |   > Members in studying           <-- Room|
+------------------------------------------+------------------------------------------+
```

### Key Differences & Improvements to Adapt

#### 1. Interactive Dial Drag to Adjust Time (Major UX Win)
* **Study Circle:** The clock face has a **circular thumb knob** on the perimeter arc. Users can touch and drag clockwise/counter-clockwise to set their focus time directly (e.g. from 10m to 45m) with smooth visual feedback and haptic clicks.
* **StudyTimer Current:** Setting time requires opening the modal bottom sheet (`TimerModeBottomSheet.kt`) or digging into Settings.
* **Improvement to Adapt:** Add an optional touch gesture on `TimerRingView.kt` so dragging around the ring smoothly sets the countdown minutes in 5-minute intervals when the timer is idle!

#### 2. Three Circular Floating Action Buttons (Spacious & Clean)
* **Study Circle:** Uses 3 balanced circular floating buttons:
  - **Left (♫):** `Ambient` — One-tap access to background white noise (Rain, Fire, Waves, Forest).
  - **Center (▶):** `Start` — Large prominent hero button.
  - **Right (✓):** `Done` — Completes the session with celebration.
* **StudyTimer Current:** Uses 3 rectangular text buttons (`[Pause]`, `[Start]`, `[Finish Hold]`). While functional, they look utilitarian.
* **Improvement to Adapt:** Offer an alternative **"Minimal Floating Controls"** style in `FocusPanelBuilder.kt` featuring floating circular icon action buttons with clear icon semantics.

#### 3. Quick Segmented Toggle `[ Timer | Stopwatch ]`
* **Study Circle:** Features a clean segmented pill button right below the clock to swap instantly between Countdown and Stopwatch modes.
* **StudyTimer Current:** Mode toggle is packed inside the session capsule or modal bottom sheet.
* **Improvement to Adapt:** Allow a quick swipe or segmented pill to toggle between Pomodoro and Stopwatch directly from the main view.

#### 4. Live Study Room Bottom Pill (`> Members in studying`)
* **Study Circle:** Shows an expandable bottom drawer pill displaying active study group members directly under the timer.
* **StudyTimer Current:** Study rooms are isolated in the `Community` tab (`CommunityPanelBuilder.kt`).
* **Improvement to Adapt:** Surface a subtle glass pill at the bottom of the Focus panel showing `"👥 14 studying now"` that expands into a mini study room drawer.

---

## Phase 3: Ambient White Noise & Study Soundscapes

StudyTimer currently only supports ambient **video wallpapers** (MP4 video loop shaders), but has **zero background audio**. Students studying for exams frequently desire soothing background white noise to mask room distractions.

### Extracted Soundscape Library (Found in `comparision/assets/flutter_assets/assets/audio/`)

All files are high-fidelity, seamless looping audio clips:

| Audio File | Duration/Size | Mood / Sound Atmosphere | Best Use in StudyTimer |
| :--- | :--- | :--- | :--- |
| `rain3.mp3` | 660 KB | Soothing steady rainfall against a window | Deep study, rainy day mode |
| `bonfire1.mp3` | 1.44 MB | Crackling fireplace / campfire embers | Cozy evening study session |
| `wave1.mp3` | 2.32 MB | Gentle rhythmic ocean surf waves | Stress reduction, calm focus |
| `forest.mp3` | 999 KB | Gentle woodland breeze & distant birds | Daytime study, fresh energy |
| `clock.mp3` | 80 KB | Rhythmic mechanical clock ticking | Strict exam simulation, pace keeping |
| `writing.mp3` | 218 KB | Pencil sketching and writing on paper | Library study room atmosphere |
| `water.mp3` | 844 KB | Flowing stream / mountain brook | Continuous flow state |
| `grassland1.mp3`| 1.20 MB | Meadow wind and summer crickets | Outdoor tranquility |
| `boat.mp3` | 939 KB | Wooden boat drifting on gentle water | Meditative break or wind-down |
| `applause.mp3` | 101 KB | Enthusiastic applause sound | Milestone & daily goal celebration |
| `chime.mp3` | 67 KB | Tibetan meditation bell | Session start / break start |

### Extracted Alarm Completion Chimes (`comparision/res/raw/` and `assets/audio/`)
* `alarm1.mp3` (30 KB): Short digital double-pulse chime (crisp, modern).
* `alarm2.mp3` (193 KB): Melodic uplifting marimba sequence (energetic).
* `alarm3.mp3` (94 KB): Gentle ambient zen gong (calm, non-intrusive).
* `alarm4.mp3` (164 KB): Classical academic focus bell (clear).
* `alarm5.mp3` (60 KB): Soft acoustic harmonic pulse (relaxing).

### Improvements to Adapt
* [ ] **Build an `AmbientSoundManager.kt`**:
  - Independent looping audio engine utilizing `SoundPool` or `MediaPlayer` with fade-in/fade-out transitions.
  - Mixable with user's phone audio (allows Spotify/YouTube music to play simultaneously or independently).
* [ ] **Add an "Ambient Sound" Pill / Button on Timer**:
  - Tapping opens a bottom sheet allowing the user to select Rain, Bonfire, Waves, Clock, or Pencil Scratch with an independent volume slider.
* [ ] **Replace Default System Ping with Study Chimes**:
  - Replace `RingtoneManager.getDefaultUri()` with user-selectable gentle chimes (`alarm1` to `alarm5`).

---

## Phase 4: Aesthetic Paper & Desk Textures

StudyTimer's current wallpaper system (`assets/wallpapers/`) features scenic nature photography and video shaders (`dark_night.webp`, `tokyo_mountain.webp`, `bright_sky.webp`). 

Many students, however, prefer **low-contrast, distraction-free "analog desk" textures** that feel like physical study stationery:

### Extracted Aesthetic Textures (`comparision/assets/flutter_assets/assets/images/`)
1. **`background4.png` — Technical Blue Graph Paper**:
   - Classic engineer/math grid notebook texture. Minimal, high contrast for text.
2. **`background2.png` — Japanese Desk Wood Grain**:
   - Warm, horizontal birch/oak wooden desk surface. Warm and organic.
3. **`background3.png` — Textured Plaster with Plant Shadow**:
   - Sunlight filtering through Monstera leaves onto a white wall. High aesthetic Instagram/Pinterest studygram vibe.
4. **`background6.png` — Fine Art Watercolor Sketchbook**:
   - Tactile, heavy cold-pressed paper tooth.
5. **`background1.png` — Subtle Hexagonal Mesh**:
   - Clean, geometric modern mesh.
6. **`background5.png` — Mosaic Dot Grid**:
   - Architect dot grid paper.

### Improvements to Adapt
* [ ] Import these 6 textured patterns into `app/src/main/assets/wallpapers/` and add them to `presets.json` as a new **"Minimalist Desk & Paper"** wallpaper category.
* [ ] Support high-contrast dark text rendering when a paper texture is active.

---

## Phase 5: Home-Screen Widgets (Matrix Progress & Tasks)

### 1. Matrix Progress Grid Widget (`challenge_widget_preview.png`)
```
+-------------------------------------------------------+
|  Challenge                                       66%  |
|  40min / 1h                                           |
|                                                       |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
|  [■][■][■][■][■][■][■][■][■][■][■][■][■][ ][ ][ ][ ]  |
+-------------------------------------------------------+
```
* **Why it works:** Progress bars are boring; **visual block filling** (like GitHub contribution tiles) triggers strong psychological completion rewards.
* **Widget specs:** 6 rows × 20 columns = 120 micro-blocks.
* **Scope options:** Can represent Daily Goal (e.g. 1h 30m / 2h), Weekly Goal, or Monthly Goal.

### 2. To-Do / Task List Widget (`task_widget_preview.png`)
```
+-------------------------------------------------------+
|  To-Do                                            10  |
|                                                       |
|  Revise Organic Chemistry Notes                       |
|  ---------------------------------------------------  |
|  Calculus Problem Set 4                               |
|  ---------------------------------------------------  |
|  English Literature Essay Draft                       |
+-------------------------------------------------------+
```
* **Why it works:** Students can check their academic agenda straight from the phone's home screen without opening the app.
* Displays pending count pill ("10") and items directly synced with `PlannerStateManager.kt`.

### Improvements to Adapt
* [ ] **Create `MatrixGoalWidgetProvider.kt`**:
  - Draws a dynamic 6x20 block bitmap or grid layout representing today's study target.
* [ ] **Create `TasksWidgetProvider.kt`**:
  - Uses `RemoteViewsService` to display active Planner tasks and lecture schedules.
* [ ] Retain existing `StudyWidgetProvider.kt` (Timer controls) as the 3rd widget option.

---

## Phase 6: Student Avatar Maker System

StudyTimer currently only allows users to upload a square image from their device gallery via `AvatarCropDialogHelper.kt`. Many students prefer anonymity or do not have a professional photo.

### Comparison App Discovery: Modular 2D Avatar Engine
In `comparision/assets/flutter_assets/assets/images/character/`, Study Circle includes a **complete vector modular avatar builder**:
* **Base Outline & Head:** `outline.PNG`, `inner.PNG`, `faceless.PNG`
* **Skin Tones:** `skin-0.PNG`, `skin-1.PNG`, `skin-2.PNG`
* **Hairstyles (38 unique styles):** `hair-0` through `hair-38` (each with black outline `.PNG` + tintable color layer `-color.PNG`)
* **Clothing (16 outfits):** `clothes-1` through `clothes-16` (hoodies, shirts, jackets, sweaters, outline + color)
* **Facial Expressions / Moods (12 moods):** `mood-0` through `mood-11` (smiling, focused, reading, serious, playful)
* **Eyeglasses (6 frames):** `glasses-1` through `glasses-6` (round, square, rimless, nerdy)
* **Accessories (13 items):** `accessory-1` through `accessory-13` (headphones, earbuds, hats, hair clips)

```
Layer Stacking Order:
[1. Background Canvas] -> [2. Skin Tone] -> [3. Inner Base] -> [4. Mood / Face] ->
[5. Clothes Color] -> [6. Clothes Outline] -> [7. Hair Color] -> [8. Hair Outline] ->
[9. Glasses] -> [10. Accessories] -> [11. Head Outline]
```

### Improvements to Adapt
* [ ] **Build an in-app `AvatarCreatorDialog.kt`**:
  - Allows users to customize and generate their own illustrated student avatar for Study Rooms and Leaderboards.
  - Combines layers into a single flattened Bitmap cached in internal storage.
  - Dramatically improves user identity in Leaderboards and Study Rooms!

---

## Phase 7: Study Mascot, Companions & Emotional UX

StudyTimer has a powerful technical foundation, but lacks friendly emotional engagement.

### 1. Animated Mascot Companions
Found in `comparision/assets/flutter_assets/assets/images/`:
* **`timerCompanion2.PNG`:** Hedgehog sipping hot green tea (calm study companion).
* **`timerCompanion3.PNG`:** Sleeping hedgehog (break timer state).
* **`timerCompanion4.PNG`:** Mascot peeking over the top curve of the timer ring.
* **`timerCompanion5.PNG`:** Cheering mascot surrounded by golden sparkles.
* **`sleep1.png` – `sleep5.png`:** 5-frame resting animation with animated zzz snot bubble.
* **`wellDone1.png` – `wellDone10.png`:** 10-frame party popper confetti celebration animation!

### Improvements to Adapt
* [ ] **Timer Companion Mode:** Add an optional "Study Buddy" toggle that sits atop the `TimerRingView.kt`, changing expressions based on study state (Studying -> focused, Break -> sleeping hedgehog, Goal Reached -> cheering).
* [ ] **Enhanced Milestone Celebration:** Integrate the 10-frame confetti party popper into `CelebrationEngine.kt` when completing a Pomodoro or leveling up streak.

---

## Phase 8: Mindful Breaks & Wellbeing UX

Study sessions require healthy breaks to prevent cognitive burnout. Study Circle includes dedicated assets for mindful break routines:

### Extracted Wellbeing Features
* **Box Breathing Exercise (`breatheIcon.png`):**
  - Clean low-poly nature scape icon used for a 4-4-4-4 Box Breathing routine during Pomodoro breaks.
* **Daily Mood Check-in ("How Are You?", `hruIcon.png`):**
  - Cute golden retriever mascot icon used for a quick 1-tap mood log before/after a study block (Productive, Neutral, Exhausted).

### Improvements to Adapt
* [ ] **Mindful Breathing During Breaks:** In `BreakAndLectureDialogHelper.kt`, offer a "1-Minute Box Breathing" button that expands a calming breathing ring animation.
* [ ] **Session Mood / Energy Tag:** When finishing a session, allow a 1-tap rating of focus quality (🔥 High Focus, ⚡ Normal, 💤 Tired) to correlate in `StatsPanelBuilder.kt`.

---

## Phase 9: OEM Background Reliability Guides

Xiaomi (MIUI/HyperOS), Samsung (OneUI), Vivo, Huawei, and Asus aggressively kill background countdowns and window manager overlays unless users manually configure battery settings.

### Extracted High-Resolution OEM Guides (`comparision/res/drawable/`)
* **`xiaomi.png` (MIUI/HyperOS):** 3-step annotated visual instructions:
  1. Open App Info -> Enable **Autostart**.
  2. In **Other permissions**, enable "Display pop-up windows while running in the background" (critical for `BlockerOverlayWindowManager.kt`).
  3. In **Battery saver**, select **No restrictions**.
* **`samsung.png` (OneUI):** 4-step annotated visual instructions:
  1. Device Care -> Battery -> App Power Monitor.
  2. Tap **Unmonitored apps** -> **Add app**.
* **`vivo_auto_start.png` & `vivo_power_save.png` (FuntouchOS/OriginOS):**
  - Enable Autostart and set High Background Power Consumption to Allowed.
* **`huawei_autostart.png` & `huawei_powersaving.png` (EMUI):**
  - Launch Manager -> Manage Manually (Auto-launch, Secondary launch, Run in background).
* **`asus_autostart.png` & `asus_notification.png` (ZenUI):**
  - Mobile Manager -> Auto-start Manager.

### Improvements to Adapt
* [ ] **Create `OemOptimizationHelper.kt`**:
  - Automatically detect device brand via `Build.MANUFACTURER.lowercase()`.
  - When setting up App Blocker or during onboarding, show a clean bottom sheet with the manufacturer's exact visual guide.
  - Include a 1-tap button launching the manufacturer's specific settings activity:
    - Xiaomi: `com.miui.securitycenter / com.miui.permcenter.autostart.AutoStartManagementActivity`
    - Samsung: `com.samsung.android.sm.ui.battery.BatteryActivity`
    - Vivo: `com.vivo.permissionmanager.activity.PurviewTabActivity`
    - Huawei: `com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity`
    - Asus: `com.asus.mobilemanager.autostart.AutoStartActivity`

---

## Phase 10: Tabular Numeric Typography & Squircle Curvature

### 1. Tabular Numeric Font (`roboto_medium_numbers.ttf`, 3.3 KB)
* Standard variable-width fonts have proportional spacing: digit `1` is narrower than `0` or `8`. As seconds count down (`10:11` -> `10:10`), the entire timer text wobbles horizontally on the screen.
* `roboto_medium_numbers.ttf` has monospace advance widths for numbers `0-9`, `:`, `.`, and `%`.
* Extremely lightweight: only 3,316 bytes.
* **Improvement to Adapt:** Apply this font directly to `timerTextPaint` in `TimerRingView.kt` to completely eliminate clock text jitter.

### 2. Apple-Style Squircle Bézier Curves (`shape_corner.svg`)
* Standard rounded rectangles have an abrupt curve transition. Continuous curvature squircles provide an ultra-premium, modern look.
* **Improvement to Adapt:** Adopt squircle curvature in `Soft3DBubbleDrawable.kt` and dialog sheets.

---

## Phase 11: Android 13+ Per-App Language Selector

* **Extracted Config:** `comparision/res/xml/locales_config.xml`:
  ```xml
  <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
      <locale android:name="en" />
      <locale android:name="ja" />
      <locale android:name="id" />
      <locale android:name="vi" />
      <locale android:name="ko" />
      <locale android:name="es" />
      <locale android:name="ar" />
      <locale android:name="bn" />
      <locale android:name="fr" />
      <locale android:name="pt" />
  </locale-config>
  ```
* **Improvement to Adapt:**
  1. Copy `locales_config.xml` to `app/src/main/res/xml/locales_config.xml`.
  2. Declare `android:localeConfig="@xml/locales_config"` in `AndroidManifest.xml` `<application>`.
  3. Android 13+ will automatically surface the native language switcher in system App Info!

---

## Phase 12: Usable Files Inventory

The following files from `comparision/` are clean, high-value, and ready to be directly imported into StudyTimer:

| Category | Source File in `comparision/` | Target Destination in StudyTimer |
| :--- | :--- | :--- |
| **Study Chimes (5)** | `res/raw/alarm1.mp3` ... `alarm5.mp3` | `app/src/main/res/raw/chime_alarm_1.mp3` ... `5.mp3` |
| **Ambient Audio (8)** | `assets/flutter_assets/assets/audio/rain3.mp3` | `app/src/main/res/raw/ambient_rain.mp3` |
| | `assets/flutter_assets/assets/audio/bonfire1.mp3` | `app/src/main/res/raw/ambient_bonfire.mp3` |
| | `assets/flutter_assets/assets/audio/wave1.mp3` | `app/src/main/res/raw/ambient_waves.mp3` |
| | `assets/flutter_assets/assets/audio/forest.mp3` | `app/src/main/res/raw/ambient_forest.mp3` |
| | `assets/flutter_assets/assets/audio/clock.mp3` | `app/src/main/res/raw/ambient_clock.mp3` |
| | `assets/flutter_assets/assets/audio/writing.mp3` | `app/src/main/res/raw/ambient_writing.mp3` |
| | `assets/flutter_assets/assets/audio/water.mp3` | `app/src/main/res/raw/ambient_water.mp3` |
| | `assets/flutter_assets/assets/audio/applause.mp3`| `app/src/main/res/raw/celebrate_applause.mp3` |
| **Tabular Font (1)** | `res/font/roboto_medium_numbers.ttf` | `app/src/main/res/font/roboto_medium_numbers.ttf` |
| **Paper Wallpapers (6)**| `assets/flutter_assets/assets/images/background1..6.png` | `app/src/main/assets/wallpapers/paper_*.png` |
| **OEM Guides (6)** | `res/drawable/{xiaomi,samsung,vivo_*,huawei_*,asus_*}.png` | `app/src/main/res/drawable/guide_*.png` |
| **Squircle Geometry (1)**| `res/raw/shape_corner.svg` | `app/src/main/res/drawable/shape_squircle.xml` |
| **Locales Config (1)** | `res/xml/locales_config.xml` | `app/src/main/res/xml/locales_config.xml` |
| **Avatar Parts (80+)** | `assets/flutter_assets/assets/images/character/*` | `app/src/main/assets/avatar_parts/` |
| **Mascots & Confetti (15)**| `assets/flutter_assets/assets/images/{timerCompanion*,wellDone*,sleep*}` | `app/src/main/res/drawable/` |

---

## Phase 13: Actionable Implementation Roadmap

You can use the steps below to direct next actions:

### Package A: Immediate Sensory Polish (Audio & Anti-Jitter Typography) [COMPLETED]
- [x] **A1:** Imported 5 study chimes with non-copy names (`timer_bell_pulse.mp3`, `timer_bell_harmony.mp3`, `timer_bell_zen.mp3`, `timer_bell_classic.mp3`, `timer_bell_soft.mp3`) + Tabular font (`timer_numbers_tabular.ttf`).
- [x] **A2:** Applied `timer_numbers_tabular.ttf` to `studyTimerDisplay` and `breakTimerDisplay` via `FontStyleManager.kt` to eliminate countdown jitter/width jumps.
- [x] **A3:** Added "Timer Sound" ON/OFF toggle switch, "Vibrate When Finished" toggle switch, and clean tone picker bottom sheet (`ChimePickerBottomSheet.kt`) with interactive `(▶)` audio preview.

### Package B: Ambient White Noise Player (Deep Study Atmosphere) [COMPLETED]
- [x] **B1:** Imported 7 ambient loops + 1 celebration sound with clean names (`ambient_gentle_rain.mp3`, `ambient_cozy_fire.mp3`, `ambient_ocean_waves.mp3`, `ambient_forest_breeze.mp3`, `ambient_clock_tick.mp3`, `ambient_pencil_scratch.mp3`, `ambient_mountain_stream.mp3`, `celebration_applause.mp3`).
- [x] **B2:** Built `StudyAudioManager.kt` providing looping background playback, volume controls, and session lifecycle hooks in `TimerService.kt`.
- [x] **B3:** Added "Background Focus Sounds" ON/OFF master toggle in Settings, interactive `AmbientSoundBottomSheet.kt` with live audio previews and volume slider, and dynamic quick-pill on the main Focus timer screen capsule (`🌧️ Rain` / `♫ Sounds`) that hides automatically when sounds are disabled. Zero technical jargon used. 

### Package C: OEM Reliability & Background Protection Dialog [COMPLETED]
- [x] **C1:** Imported 8 OEM device walkthrough screenshots into `app/src/main/res/drawable/guide_oem_*.png` with clean, distinct names (Xiaomi, Samsung, Vivo, Huawei, Asus).
- [x] **C2:** Built `OemOptimizationHelper.kt` to auto-detect device brand (`Build.MANUFACTURER`), provide human-friendly step-by-step checklists, and dispatch deep intents directly to OEM autostart / battery managers with graceful fallbacks.
- [x] **C3:** Built `OemGuideBottomSheet.kt` with brand switcher, step cards, visual screenshot previews, and a 1-tap "Open Settings" button. Linked into **Settings → Background Reliability** and `SystemDialogHelper.kt`. Zero technical jargon used.

### Package D: Multi-Widget System (Matrix Progress & Tasks) [COMPLETED]
- [x] **D1:** Built `MatrixGoalWidgetProvider.kt` with dynamic 6x4 (24-hour) micro-block matrix generation on `Bitmap` canvas, showing live daily focus hours, target percentage, streak count, and 1-tap app focus launcher.
- [x] **D2:** Built `TasksWidgetProvider.kt` presenting today's top pending Planner goals with interactive check-off broadcasts directly from the launcher and 1-tap jump to the full Planner.
- [x] **D3:** Declared both providers in `AndroidManifest.xml` with `@xml/widget_matrix_info` and `@xml/widget_tasks_info`, connected to real-time `PlannerStateManager` and `StudyWidgetProvider.refresh(context)`. Zero technical jargon used.

### Package E: Minimalist Paper & Desk Wallpapers [COMPLETED]
- [x] **E1:** Curated existing wallpaper collection: removed duplicate/repetitive skies and meadows (`bright_sky`, `peaceful_field`).
- [x] **E2:** Imported and differentiated 6 minimalist stationery desk backgrounds with tailored contrast, tint, and high-efficiency WebP compression (`stationery_blueprint.webp`, `stationery_warm_parchment.webp`, `stationery_nordic_mint.webp`, `stationery_graph_notebook.webp`, `stationery_dot_journal.webp`, `stationery_textured_linen.webp` + matching thumbnails in `thumbs/`).
- [x] **E3:** Registered under new `"Minimalist Desk"` collection in `presets.json` and `WallpaperManager.kt`. Zero visual slop or direct copying.

### Package F: Supernova Particle Bloom Celebration [COMPLETED] & Avatar Roadmap
- [x] **F1 (Decision):** Defer external avatar character layers to next major release to protect against copyright/IP infringement and prevent cartoon UI slop in StudyTimer's minimal design. Planned for an original "Academic Crest" or "Zen Botanical Growth" system.
- [x] **F2 (Celebration Upgrade):** Replaced generic cartoon confetti with the **Supernova Particle Bloom** engine in `CelebrationEngine.kt` — 100% procedural Canvas math with 5 geometric shapes (4-point radiant stars, diamonds, sparks, halo rings, and light streaks) with 360-degree radial explosion physics, deceleration drag, and smooth alpha decay.
- [x] **F3 (Audio & Haptics):** Integrated `StudyAudioManager.playCelebrationSound(activity)` (`celebration_applause.mp3`) with gentle haptic confirmation when hitting daily study goals and streak milestones. Built non-blocking `triggerSupernovaBurst()` overlay for on-demand milestone blooms. Zero external images or third-party assets used.

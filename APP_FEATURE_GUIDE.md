# StudyTimer App Feature & Architecture Reference Guide

This document is the **strict source of truth** for all screens, UI elements, data flows, and design constraints in StudyTimer. Every UI redesign or enhancement **must strictly preserve and enhance existing functionality**, never stripping or misunderstanding features.

---

## 1. Core Architecture & Navigation

The app is built as a single `MainActivity` with programmatic view building (no XML view binding overhead) using specialized builder classes:
* `FocusPanelBuilder.kt` — Focus / Timer Screen
* `PlannerPanelBuilder.kt` — Planner Screen (Tasks, Timeline, Schedule)
* `StatsPanelBuilder.kt` — Insights / Analytics Screen
* `CommunityPanelBuilder.kt` & `LeaderboardPanelBuilder.kt` — Social & Study Rooms
* `SettingsPanelBuilder.kt` — Settings & Customization Hub

### Global Navigation (`StudyBottomNavBar.kt`)
* 4 Main Destinations:
  1. **Focus** (`AppPanel.FOCUS`)
  2. **Planner** (`AppPanel.PLANNER`)
  3. **Insights** (`AppPanel.STATS`)
  4. **Community** (`AppPanel.COMMUNITY`)
* Sliding spring-animated indicator pill (`indicatorView`) matching the user's active theme accent.
* Floating pill layout with safe-insets handling (supports 3-button nav and gesture nav bars).

### Universal Header (`UniversalHeaderHelper.kt`)
* Present on primary tabs:
  * **Left:** Profile pill (contains User Circular Avatar or Initial Badge + User Name + Streak flame pill e.g. `🔥 14d`). Tapping navigates to Profile Settings (`AppSettingsTab.PROFILE`).
  * **Center:** Screen-centered title for Planner, Insights, Community.
  * **Right:** Context actions + Live Wallpaper icon (if enabled) + **Settings Gear Icon** (routes intelligently to the relevant settings tab).

---

## 2. Screen 1: Focus Screen (`FocusPanelBuilder.kt`)

### Feature Breakdown & UI Elements:
1. **Universal Top Header:**
   * Shows Profile Name (e.g. "Courage") + Streak badge.
   * Settings icon on the right.
2. **Session Context Pill Row (`sessionPillsRow`):**
   * **Subject Pill:** Displays current selected subject (e.g. "Maths") with a colored dot and chevron.
     * *Feature:* **Tied to custom subject colors!** Each subject in `SubjectTagManager` has an assigned color Hex. The pill border/tint reflects the active subject's color. Tapping opens `SubjectDialogHelper.showSubjectPickerDialog()`.
   * **Chapter / Sub-Topic Pill:** Displays selected sub-topic/chapter (e.g. "📖 Chapters ▾").
     * Tapping opens `SubjectDialogHelper.showSubTopicPickerDialog()`.
   * **Timer Mode Pill:** Displays current mode (e.g. `Subject Wise`, `Pomodoro 25m`, `Stopwatch`, `Lecture`).
     * Tapping opens `TimerModeBottomSheet`.
   * **Active Goal Pill:** If a Planner goal is active, shows the active goal title with a quick clear button.
3. **Status Badge (`statusBadge`):**
   * Dynamic pill displaying current state:
     * `IDLE`: "Ready when you are" (`ready_to_track`)
     * `STUDYING`: "Learning Time" / "Lecture in progress" / "OVERTIME"
     * `PAUSED`: "Paused"
     * `BREAK`: "Break in Progress"
     * `LECTURE_ENDED`: "Lecture Ended"
4. **Timer Display & Ring:**
   * `TimerRingView`: Circular progress ring animating countdown or elapsed time.
   * `studyTimerDisplay`: Primary large digital clock (`HH:mm:ss`).
   * `breakTimerDisplay`: Break countdown/countup timer (shown when active).
   * Gesture hint text: `"Double-tap to pause • Tap for fullscreen"`.
5. **Session Control Actions (`controlActionContainer`):**
   * **Idle State:** Single prominent `Start` button.
   * **Running / Studying State:**
     * `Pause` button.
     * `HoldRingButton` (`stopBtn`): Long-press to finish/save session (prevents accidental stops).
     * Quick break trigger buttons.

---

## 3. Screen 2: Planner Screen (`PlannerPanelBuilder.kt`)

### Feature Breakdown:
* **Planner Segments / Views:**
  * Today's Plan & Checklist (Tasks / Chapters with subject tags, priority badges, estimates).
  * Day Timeline (24-hour visual block view of scheduled study sessions and lectures).
  * History & Completion log.
* **Integrations:**
  * Quick-add task / event modal.
  * Lecture schedule synchronization.
  * Direct linking of planner tasks to the timer session.

---

## 4. Screen 3: Insights / Analytics Screen (`StatsPanelBuilder.kt`)

### Feature Breakdown:
* **Time Range Selector:** Daily, Weekly, Monthly, All-time.
* **Key Metric Cards:**
  * Total Focus Time.
  * Daily Average.
  * Current & Longest Streak (`StreakCalculator`).
  * Completion rate against daily targets.
* **Visual Data Charts:**
  * `SubjectPieChartView`: Breakdown of time spent across different subjects (using each subject's custom color).
  * Bar/Line activity charts for hourly distribution and daily study volume.
  * Fullscreen Heatmap view (`AppPanel.HEATMAP`).
* **Scorecard Export:** `StudyScorecardExportHelper` to generate shareable graphic summaries.

---

## 5. Screen 4: Community Screen (`CommunityPanelBuilder.kt` & `LeaderboardPanelBuilder.kt`)

### Feature Breakdown:
* **Study Rooms:** Live virtual study rooms (`StudyRoomManager`) where users study together with realtime counters.
* **Global & Friends Leaderboard:**
  * Daily, Weekly, Monthly rankings.
  * User profile cards with streak, badges, rank, and study hours.
  * Admin / moderation safeguards (`ProfanityFilter`, `ProfileSyncService`).

---

## 6. Theming & Personalization Engine

* **Themes (`ThemeCoordinator.kt`):**
  * Support for OLED Black, Midnight Navy, Deep Carbon, Glassmorphic, Soft 3D Bubble, and Pure White Pomodoro mode.
* **Dynamic Typography (`FontStyleManager.kt`):**
  * Built-in multi-font engine! Users can choose their preferred typeface family in Settings:
    * *Modern Sans:* System Default, Sans Medium, Sans Condensed, Sans Heavy, Sans Minimal, Ultra Thin.
    * *Monospace:* Clean Mono, Bold Console, Sans Mono, Matrix Terminal.
    * *Serif:* Classic Editorial, Bold Academic, Literary Italic, Compact Chronicle.
    * *Creative & Chill:* Casual Chill, Punchy Casual, Aesthetic Script, Warm Handwriting.
    * *Geometric:* Nordic Condensed, Tech Headline, Small Caps.
  * *Crucial Rule:* Always respect `themeCoordinator.getAppTypeface()` or `FontStyleManager.applyRecursively()` so user-selected typography remains active across all rebuilt views.

---

## 7. Strict Redesign & Enhancement Principles

1. **Verify Before Critiquing:**
   * Always check the underlying Kotlin source before assuming a visual trait is a bug or clutter.
   * Understand dynamic states (e.g. subject colors, timer modes, theme accents, custom fonts).
2. **Elevate, Don't Strip:**
   * Never remove user actions, subject selectors, streak pills, or mode triggers.
   * Make existing elements feel more tactile, cohesive, and intentional (e.g. better hierarchy, refined micro-spacing, subtle glass tints, smoother borders, polished icon baselines).
3. **Screen-by-Screen Progression:**
   * Focus on one screen at a time.
   * Present enhancements clearly: visual hierarchy, spacing, contrast, micro-interactions.
   * Validate after each step with live screenshots via ADB.

# StudyTimer UI/UX Implementation Plan
**Source Document:** [UI_UX_DEEP_AUDIT_REPORT.md](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/UI_UX_DEEP_AUDIT_REPORT.md)  
**Target:** StudyTimer Android App  
**Status:** Complete (All 6 Phases Fully Implemented & Verified with BUILD SUCCESSFUL)  
**Date:** October 25, 2026  

---

## 1. Overview & Strategy

This plan organizes the 13 prioritized audit findings and structural improvements into **6 logical, non-breaking execution phases**. Each phase is self-contained, keeps the app compilable and runnable at every step, and addresses UX issues from highest behavioral friction (critical bugs & system back handling) to visual polish and codebase hygiene.

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: Critical Fixes & Core System Navigation            │
│ (Auto-Break Key Desync, System Back Stack, Settings Back)   │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 2: Focus & Timer Interaction Integrity                │
│ (Anchored Button Layout, Hold Ring Hint, Break Dialog)      │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 3: Planner & Goals Experience Overhaul                │
│ (Early Goal Success State, Direct Edit/Delete, Exam Reloc)  │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 4: Timer Modes & Settings Information Architecture    │
│ (Subject Tagging Unification, Move Study Adj, White Themes) │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 5: Insights & Analytics Harmonization                 │
│ (Global Time Filter, Card Grouping, Navigation Cleanup)     │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 6: Accessibility, Community Polish & Dead Code Pruning│
│ (48dp Touch Targets, Dark Contrast, Guest CTA, Dead Code)   │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Critical Fixes & Core System Navigation

### Goals
- Resolve the functional desynchronization bug between `TimerModeBottomSheet`, `SettingsPanelBuilder`, and `TimerService`.
- Establish Android standard back-gesture / back-stack handling across all screens, subtabs, and dialogs.
- Clean up duplicate back navigation buttons.

### 1.1 Unify Auto-Break Preference Key (Audit §4.1)
* **Target Files:**
  - [`TimerModeBottomSheet.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/TimerModeBottomSheet.kt)
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
  - [`TimerService.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/TimerService.kt)
* **Implementation Steps:**
  1. Define a single canonical constant in `DataModels.kt` or `MainActivity.kt`: `const val PREF_POMODORO_AUTO_BREAK = "pomodoro_auto_break"`.
  2. In `TimerModeBottomSheet.kt` (lines 378-380), replace `"auto_break_enabled"` with `PREF_POMODORO_AUTO_BREAK`. Add a one-time migration to read the old `"auto_break_enabled"` if present and copy to `"pomodoro_auto_break"`.
  3. Ensure toggling in either bottom sheet or Settings updates both shared preferences and immediately notifies running `TimerService` via local broadcast / intent action.
* **Verification:**
  - Toggle "Auto-Start Breaks" in Timer Mode sheet -> open Settings -> toggle state matches.
  - Pomodoro timer finishes countdown -> auto-starts break according to toggle.

### 1.2 Implement Android System Back Navigation Dispatcher (Audit §2.1)
* **Target Files:**
  - [`MainActivity.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/MainActivity.kt)
* **Implementation Steps:**
  1. In `MainActivity.onCreate()`, register `onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { ... })`.
  2. Implement hierarchical navigation logic:
     - **Level 1 (Sub-dialogs / Modals):** If any active custom fullscreen dialog or bottom sheet is showing (e.g. Exam Countdown, Habit Matrix, Subject Pie Fullscreen), dismiss the dialog.
     - **Level 2 (Settings Navigation):** If current panel is `AppPanel.SETTINGS`:
       - If `currentSettingsTab != AppSettingsTab.HUB`, route back to `AppSettingsTab.HUB`.
       - If `currentSettingsTab == AppSettingsTab.HUB`, navigate back to `settingsOriginPanel` (the panel the user was on before entering settings).
     - **Level 3 (Insights Sub-views):** If in Heatmap Fullscreen or Calendar History sub-view inside Stats, return to primary Stats Overview.
     - **Level 4 (Top-level Navigation):** If on `AppPanel.PLANNER`, `AppPanel.STATS`, or `AppPanel.COMMUNITY`, return to `AppPanel.FOCUS`.
     - **Level 5 (Exit Prevention):** If on `AppPanel.FOCUS`:
       - If timer is active (`STUDYING` or `BREAK`), show Toast/Snackbar: *"Timer is active. Press back again to minimize."*
       - If idle, allow default finish / double-tap within 2000ms to exit.
* **Verification:**
  - Open Settings -> enter "Timer & Focus" -> press system back -> returns to Settings Hub.
  - Press system back again -> returns to originating screen (e.g. Focus).
  - Open Planner -> press system back -> returns to Focus.

### 1.3 Eliminate Redundant Settings Back FAB (Audit §2.2)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
* **Implementation Steps:**
  1. Remove `settingsBackFab` from Settings Hub layout.
  2. Ensure the top-left app bar navigation chevron (`←`) consistently handles back navigation to the originating panel.
* **Verification:**
  - Settings Hub layout has a single clean top back button and respects system back gesture without bottom-right FAB clutter.

---

## Phase 2: Focus & Timer Interaction Integrity

### Goals
- Stabilize button placement so controls never jump or shift positions between Idle, Active, and Paused states.
- Improve discoverability of hold gestures.
- Clean up gesture hints and custom break options.

### 2.1 Anchor Focus Screen Action Buttons (Audit §3.1)
* **Target Files:**
  - [`FocusPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/FocusPanelBuilder.kt)
  - [`MainActivity.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/MainActivity.kt)
* **Implementation Steps:**
  1. **Refactor Button Container:**
     - Replace dynamic hiding/swapping with a stable 3-slot anchored layout:
       - **Slot 1 (Left / Primary Action):** `[Start]` in Idle, `[Pause]` in Studying, `[Resume]` in Paused. The primary interaction button **always** occupies Slot 1.
       - **Slot 2 (Center / Auxiliary):** `[Break]` button. Enabled when appropriate; in Idle, replaced by a subtle "Take a Quick Break" trigger or break pill.
       - **Slot 3 (Right / Terminal Action):** `[Finish]` hold button. Appears smoothly with fade transition when session starts, anchored strictly on the right.
  2. **Idle State Simplification:**
     - In Idle state, present one prominent, centered `[Start Focus]` button.
     - Move the manual break picker to a dedicated chip or mode selector above the timer ring (or as a secondary text link below).
* **Verification:**
  - Start timer -> button transitions to `[Pause]` in same position -> tap Pause -> button changes to `[Resume]` in exact same location (zero horizontal jump).
  - `[Finish]` button stays locked in the right column throughout active & paused states.

### 2.2 Improve "Hold to Finish" Gesture Discoverability (Audit §3.2)
* **Target Files:**
  - [`FocusPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/FocusPanelBuilder.kt)
* **Implementation Steps:**
  1. Change default button label from `"Finish"` to `"Hold to Finish"`.
  2. On touch-down: Immediately render a radial progress sweep around the button icon from 0% to 100% over the 500ms duration.
  3. On short tap (abort): Play a gentle pulse with micro-haptic tick and display a clear inline badge: *"Hold for 0.5s to finish session"*.
* **Verification:**
  - Tapping Finish shows clear immediate visual feedback explaining the hold requirement.
  - Holding displays a smooth circular progress indicator completing at 500ms.

### 2.3 Gesture Hint Collision & Responsive Layout (Audit §3.3)
* **Target Files:**
  - [`FocusPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/FocusPanelBuilder.kt)
* **Implementation Steps:**
  1. Relocate `"Double-tap to pause • Tap for fullscreen"` hint from hardcoded `bottomMargin = dp(76)` to below the button action bar, or integrate into an onboarding tooltip that dismisses after first 3 sessions.
  2. Ensure break timer display (`00:00:00`) has dedicated layout constraints that never overlap text labels on compact screen heights.
* **Verification:**
  - Test on small/standard screens: no text overlap between status badges, break timer digits, and gesture hints.

### 2.4 Respect Configured Custom Break in Quick Break Dialog (Audit §4.4)
* **Target Files:**
  - [`BreakAndLectureDialogHelper.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/BreakAndLectureDialogHelper.kt)
* **Implementation Steps:**
  1. Fetch `pomodoro_short_break` preference from `SharedPreferences`.
  2. If the user's custom break duration (e.g. 7m, 12m) is not in `[5, 10, 15, 20, 30]`, dynamically prepend it to the option list as `"${customBreak}m (Your Default)"` with accent highlight.
  3. Add a "Custom..." option allowing any arbitrary minute input.
* **Verification:**
  - Set custom break to 8 min in Settings -> open Quick Break dialog -> "8m (Your Default)" appears as selected top chip.

---

## Phase 3: Planner & Goals Experience Overhaul

### Goals
- Eliminate psychological penalties for early goal completion.
- Streamline goal creation, editing, and deletion.
- Relocate forward-looking planning tools (Exam Countdown) to the Planner tab.

### 3.1 Affirmative Early Goal Completion (Replace "Red ✕") (Audit §5.1)
* **Target Files:**
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. Locate goal completion checkbox evaluation at lines 461-470 and 544-551 (`isDeficit = true`, `chipColor = redColor`).
  2. Redefine visual states:
     - **Target Met (>= 100%):** Vibrant Emerald Green checkmark `✓` + `Target Reached`.
     - **Early Manual Completion (< 100%):** Soft Teal or Amber checkmark `✓` with badge `Completed (45/60m)`. Never use red failure cross for a user-checked completed task.
     - **Red ✕ Retained Only For:** Explicitly skipped or missed goals in historical archive view.
* **Verification:**
  - Create a 60m goal -> study for 30m -> check it off -> displays affirmative checkmark with early-finish pill, no red crosses.

### 3.2 Direct Goal Edit & Delete Flow (Audit §5.2)
* **Target Files:**
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. **Hit-box Separation:**
     - Left checkbox area (48x48dp): Toggles completion status only.
     - Right card body: Opens quick action bottom sheet or direct edit dialog.
  2. **Add 3-Dot Action Menu / Bottom Sheet:**
     - Tapping card options or long-pressing provides:
       - `[▶ Focus Now]` — Sets as active focus task.
       - `[✏️ Edit Goal]` — Directly opens `showEditSessionGoalDialog()`.
       - `[📊 View History]` — Opens the detailed calendar streak modal.
       - `[🗑️ Delete Goal]` — Shows confirmation dialog with delete action.
* **Verification:**
  - User can edit or delete a goal in 1-2 taps directly from Planner without navigating through deep analytics screens.

### 3.3 De-duplicate "+ Add Goal" Triggers (Audit §5.3)
* **Target Files:**
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. Standardize on the primary Floating Action Button (FAB) for goal creation.
  2. Remove redundant `+ Add Goal` chip button inside the top summary card.
  3. Keep the empty-state button only when 0 goals exist.
  4. Ensure RecyclerView / ScrollView bottom padding is `dp(96)` so the FAB never obstructs the bottom card or quote.
* **Verification:**
  - Planner header is clean; FAB floats comfortably without overlapping content; clicking FAB opens goal creation dialog.

### 3.4 Progressive Disclosure in "New Study Goal" Dialog (Audit §5.4)
* **Target Files:**
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. Refactor `showNewSessionGoalDialog()`:
     - **Primary View (Always Visible):**
       - Goal Title input.
       - Target Duration input + quick preset chips (`30m`, `45m`, `1h`, `2h`).
       - Primary `[Save Goal]` button.
     - **Secondary View (Collapsible Accordion):**
       - Toggle header: `"More Options ▾"` (Subject & Chapter tag, Days recurrence, Notification reminder).
       - Collapsed by default so 90% of quick goals can be saved in 5 seconds.
* **Verification:**
  - Opening dialog shows clean 2-input view without vertical scrolling required. Expanding "More Options" reveals recurrence and reminders.

### 3.5 Relocate Exam Countdown to Planner Header (Audit §6.2)
* **Target Files:**
  - [`CalendarTimeline.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/CalendarTimeline.kt)
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. Extract the Target Exam & Milestone countdown component from `CalendarTimeline.kt` into a reusable helper: `ExamCountdownHelper.kt`.
  2. Embed a pinned countdown banner at the top of `PlannerPanelBuilder.kt` (e.g. `"🎯 Midterms • 12 Days Left • 24h Study Target"`).
  3. Retain a link in Calendar Timeline for historical reference, but make Planner the primary hub for adding, editing, and tracking exam dates.
* **Verification:**
  - Open Planner -> exam countdown prominently displayed at top of study schedule.

---

## Phase 4: Timer Modes & Settings Information Architecture

### Goals
- Clean up timer mode concepts (make subject tagging universal).
- Relocate misplaced controls to appropriate domain tabs.
- Disambiguate confusing switches and terminology.

### 4.1 Consolidate Timer Modes & Universalize Subject Tagging (Audit §4.2)
* **Target Files:**
  - [`TimerModeBottomSheet.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/TimerModeBottomSheet.kt)
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
  - [`DataModels.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/DataModels.kt)
* **Implementation Steps:**
  1. Reorganize Timer Mode selection into 3 genuine counting engines:
     - **Stopwatch** (Count up)
     - **Pomodoro** (Interval countdown with rest cycles)
     - **Lecture / Class Schedule** (Fixed timetable block)
  2. Transform **Subject Tagging** from an exclusive mode into an omnipresent toggle / capsule on the Focus screen that works seamlessly with Stopwatch, Pomodoro, and Lecture modes.
  3. Ensure backward compatibility: if stored mode was `"SUBJECT"`, transparently map it to `"COUNTDOWN"` with subject tagging active.
* **Verification:**
  - User can tag a subject regardless of whether they choose Stopwatch or Pomodoro.
  - Timer mode selector clearly shows 3 options without conceptual overlap.

### 4.2 Move "Adjust Today's Study Time" Out of Display & Gestures (Audit §8.1)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
  - [`StatsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/StatsPanelBuilder.kt)
* **Implementation Steps:**
  1. Remove "Adjust Today's Study Time" from "DISPLAY & GESTURES" in `SettingsPanelBuilder.kt`.
  2. Relocate to:
     - Settings -> **"Insights & Data Management"** subtab under "Data Corrections".
     - Provide a direct entry point in `StatsPanelBuilder.kt` (under Today's Hero Card: `"Manual Time Adjustment"`).
* **Verification:**
  - "Display & Gestures" contains only visual/gesture toggles (Flip-to-Focus, Landscape, Pure White digits).
  - Time adjustment is easily discoverable where daily logs and stats are inspected.

### 4.3 Disambiguate "White Theme" vs "White Digits" (Audit §8.2)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
* **Implementation Steps:**
  1. Move `"White Theme for Pomodoro"` (`pomodoro_pure_white_theme`) to **Theme & Appearance Settings** under "Special Modes" as `"Paper White Minimalist Theme"`.
  2. In Timer Settings, rename `"White Timer"` (`pureWhiteTimer`) to `"Pure White Digits"`, with description: *"Keep clock numbers pure white regardless of active theme or wallpaper."*
* **Verification:**
  - No two conflicting "White" toggles on the same screen; clear distinction between full screen theme vs clock digit color.

### 4.4 Standardize "Freedom Mode" Terminology (Audit §4.3)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
* **Implementation Steps:**
  1. Replace discordant labels with unified terminology:
     - Toggle Title: `"Extended Study (Freedom Mode)"`
     - Description: *"Allows continuous sessions up to 24 hours without forced break intervals."*
  2. Align all subtitle references to match "Extended Study / Freedom Mode".
* **Verification:**
  - Subtitle descriptions and the switch header use identical, clear terminology.

### 4.5 Split "Help & Support" from "Legal & Privacy" (Audit §8.3)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
* **Implementation Steps:**
  1. Create two distinct sections on Settings Hub:
     - **Help & Support Card:** App Guide & Walkthrough, Send Feedback / Report Bug, Check for Updates & Changelog.
     - **About & Legal Card:** Version info, Privacy Policy, Terms of Service, Account Deletion.
* **Verification:**
  - Feedback and Guide are immediately accessible without digging inside legal accordions.

---

## Phase 5: Insights & Analytics Harmonization

### Goals
- Unify disjointed time filters across all charts.
- Alleviate scroll fatigue with organized sub-views.
- Resolve dead navigation components.

### 5.1 Global Unified Time Period Filter (Audit §6.1)
* **Target Files:**
  - [`StatsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/StatsPanelBuilder.kt)
* **Implementation Steps:**
  1. Add a single top sticky filter bar: `[Today | 7 Days | 30 Days | All Time]`.
  2. Bind the global filter state to:
     - Subject Breakdown Pie Chart
     - Focus Time Bar Chart
     - Focus Pattern Grid
  3. When the user selects `7 Days`, all charts simultaneously update to show 7-day data, preventing contradictory data displays.
* **Verification:**
  - Switching to "7 Days" immediately updates both Pie Chart and Bar Chart in tandem.

### 5.2 Alleviate Scroll Fatigue with Sectioned Tabs / Cards (Audit §6.1 & §6.3)
* **Target Files:**
  - [`StatsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/StatsPanelBuilder.kt)
  - [`InsightsPillNavBar.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/InsightsPillNavBar.kt)
* **Implementation Steps:**
  1. Integrate a sleek 2-pill segment control at top of Insights:
     - `[Summary & Charts]` — Hero card, Bar chart, Subject breakdown.
     - `[History & Patterns]` — Calendar timeline, Focus heatmap, 24h pattern grid, Lifetime stats.
  2. Remove the awkward ad-hoc `"← Back to Overview"` text banner.
  3. Delete or re-purpose `InsightsPillNavBar.kt` to power this 2-tab view cleanly.
* **Verification:**
  - Users switch between Summary and History with a smooth swipe/tap instead of scrolling through 8 massive vertical cards.

### 5.3 Contextual Gear Icon Predictability (Audit §2.3)
* **Target Files:**
  - [`UniversalHeaderHelper.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/UniversalHeaderHelper.kt)
* **Implementation Steps:**
  1. Make tapping the top header gear icon (`⚙️`) **always** open `AppSettingsTab.HUB` with a smooth slide transition.
  2. If panel-specific settings are needed on Insights or Planner, place an in-content filter/options chip in the panel's own action bar.
* **Verification:**
  - Tapping Gear from any screen opens Settings Hub predictably; system back returns to the previous screen.

---

## Phase 6: Accessibility, Community Polish & Dead Code Pruning

### Goals
- Meet WCAG 48x48dp minimum touch targets.
- Fix low contrast text alphas in Dark/Eclipse modes.
- Provide a welcoming Community preview for guest users.
- Clean up unused legacy code and enums.

### 6.1 WCAG 48dp Minimum Touch Targets (Audit §9.1)
* **Target Files:**
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
  - [`PlannerPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerPanelBuilder.kt)
* **Implementation Steps:**
  1. In `SettingsPanelBuilder.kt`: Ensure stepper buttons (`−` and `+`) have `minWidth = dp(48)` and `minHeight = dp(48)` with centered text alignment.
  2. In `PlannerPanelBuilder.kt`: Set overflow button (`⋯`) touch target to `48x48dp` using `TouchDelegate` or direct frame sizing.
* **Verification:**
  - Touch targets pass Android Accessibility Scanner audits; easy single-tap actuation without missing or mis-tapping.

### 6.2 Dark / Eclipse Theme Text Contrast Alphas (Audit §9.2 & §7.2)
* **Target Files:**
  - [`StatsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/StatsPanelBuilder.kt)
  - [`SettingsPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt)
  - [`CommunityPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/CommunityPanelBuilder.kt)
* **Implementation Steps:**
  1. Update secondary text alphas from `0.45f`/`0.50f` to a minimum of `0.70f` across dark surfaces.
  2. In `CommunityPanelBuilder.kt`: Increase unselected tab text alpha in `[Leaderboard | Study Rooms]` segment from `0.65f` to `0.85f` on OLED/Eclipse themes.
* **Verification:**
  - Subtitles and inactive tabs remain crisp and legible in low-brightness and AMOLED black themes.

### 6.3 Guest / Offline Community Empty State (Audit §7.1)
* **Target Files:**
  - [`LeaderboardPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/LeaderboardPanelBuilder.kt)
  - [`CommunityPanelBuilder.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/CommunityPanelBuilder.kt)
* **Implementation Steps:**
  1. For unauthenticated guests, replace blank podiums and error spinners with an inviting preview card:
     - Illustration/Icon + Headline: *"Join the StudyTimer Global Community"*.
     - Bullet highlights: Live group study rooms, weekly subject leaderboards, study streaks.
     - Direct `[Sign In with Google]` button + `[Explore Public Rooms as Guest]` button.
* **Verification:**
  - Offline/guest users see an engaging value-proposition preview rather than broken or empty state containers.

### 6.4 Dead Code & Legacy Enum Cleanup (Audit §10)
* **Target Files:**
  - [`InsightsPillNavBar.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/InsightsPillNavBar.kt) (delete if not adopted in Phase 5)
  - [`DataModels.kt`](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/DataModels.kt)
* **Implementation Steps:**
  1. Remove unused enum values: `AppStatsTab.PLANNER`, `AppSettingsTab.AMBIENCE`.
  2. Remove deprecated alias `AppPanel.LEADERBOARD` (replace remaining references with `AppPanel.COMMUNITY`).
  3. Clean up unreferenced drawable XML files in `app/src/main/res/drawable/`.
* **Verification:**
  - `./gradlew compileReleaseKotlin` passes with 0 warnings/errors.

---

## 2. Summary of Implementation Milestones

| Phase | Key Deliverables | Risk Level | Est. Scope |
|:---:|---|:---:|:---:|
| **1** | Fix Auto-Break Key Desync, System Back Stack Dispatcher, Remove Redundant Settings Back FAB | Low | 3 files |
| **2** | Anchored Action Buttons (No jumping), Hold to Finish radial ring, Gesture hint layout, Configured break duration preset | Medium | 4 files |
| **3** | Affirmative Goal Completion (No Red ✕), Direct Goal Edit/Delete Action Sheet, New Goal Progressive Disclosure, Relocate Exam Countdown to Planner | Medium | 3 files |
| **4** | Universal Subject Tagging (Remove "Subject Wise" mode), Move Study Time Adjustment, Disambiguate White Switches, Standardize Freedom Mode | Low | 4 files |
| **5** | Unified Global Time Filter on Insights, 2-Tab Segment (Summary vs History), Clean Gear Icon Navigation | Medium | 3 files |
| **6** | WCAG 48dp Touch Targets, Dark Mode Text Contrast Alphas, Guest Community Welcome Card, Dead Code Pruning | Low | 5 files |

---

## 3. Verification & Quality Protocol

For each phase during actual execution:
1. **Compilation Check:** Run `./gradlew compileReleaseKotlin` to ensure clean build with zero warnings.
2. **Behavioral Testing:**
   - Test hardware/gesture Back navigation across all navigation depths.
   - Verify SharedPreferences consistency across Focus sheet and Settings.
   - Test button touch targets using Android Developer Accessibility Scanner.
3. **Release Integrity:** Follow repository conventions outlined in [AGENTS.md](file:///d:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/AGENTS.md) when building and releasing.

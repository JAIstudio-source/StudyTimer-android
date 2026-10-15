# StudyTimer Feature Roadmap (v3.2.0)

## Phase 1: Core UX & Frictionless Focus (Completed)
- [x] Mechanical Haptics on Ring Button (Progressive frequency and amplitude tick ramping)
- [x] Task-to-Timer Linkage (Planner goal "Focus Now" shortcut into active focus chip)
- [x] Active Task Refinements:
  - [x] Removed emoji prefix before active task name on focus badge
  - [x] Active task can only be removed (`✕`) when timer is not actively running (`STUDYING` or `BREAK`)
  - [x] 100% dedicated time allocation to active task (eliminated waterfall spillover / auto-add to other tasks)
- [x] Historical Goal Notes (Tapping past goal history marks displays session record and historical notes)
- [x] Pomodoro Loop & Overtime Flow (Continuous overtime countup into negative seconds, 25/5, 50/10, 90/20 presets, auto-break toggle)
- [-] Ambient Soundscapes (Explicitly skipped per user instruction)

## Phase 2: Accountability, Motivation & Micro-Interactions (Completed)
- [x] **Flip-to-Focus Physical Gesture:** Proximity + Accelerometer detection to flip phone face-down to start/pause timer.
- [x] **Streak Freeze System (Burnout Prevention):** 2 free rest/freeze days per month to preserve streaks during emergencies.
- [x] **Subject Sub-Topics & Weekly Quotas:** Hierarchical sub-topics under subjects and weekly subject hour targets.
- [x] **Live Study Rooms (YPT-style):** 6-character private room codes with live study/break presence.

## Phase 3: Subject Breakdown & Architecture Decoupling
- [x] **Subject Multi-Period Focus Breakdown (Pie Chart & Modal):**
  - [x] Replaced 24h chronotype with dynamic multi-period focus breakdown: [Today | This Week | This Month | All Time].
  - [x] Interactive `SubjectPieChartView` showing subject percentage, hours, daily average, and weekly quota progress.
  - [x] Fullscreen period-aware modal with previous/next week and month navigation, sub-topic logs, and detailed breakdown.
- [x] **MainActivity Architectural Decoupling:**
  - [x] Extracted account, deletion, and sync conflict dialogs into [AccountDialogHelper.kt](file:///D:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/AccountDialogHelper.kt).
  - [x] Extracted update checks, custom alerts, and battery optimization into [SystemDialogHelper.kt](file:///D:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SystemDialogHelper.kt).
  - [x] Extracted planner styling and themes into [PlannerThemeHelper.kt](file:///D:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/PlannerThemeHelper.kt).
  - [x] Extracted tactile micro-haptics into [HapticFeedbackHelper.kt](file:///D:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/HapticFeedbackHelper.kt).
  - [x] Extracted subject selection and Pomodoro prompt into [SubjectDialogHelper.kt](file:///D:/made%20app/My%20app/Studytimer%20main/StudyTimer-app/app/src/main/java/com/madeby/JAI/SubjectDialogHelper.kt).
- [-] **Strict Mode / Distraction App Whitelist:** Excluded per user directive.

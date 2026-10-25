# StudyTimer Comprehensive Screen & Menu Architecture Directory

A complete, granular catalog of every panel, sub-screen, navigation pill, bottom sheet, context menu, and interactive modal dialog in **StudyTimer**.

---

## 1. Top-Level App Panels (Bottom Navigation)

The main navigation bar provides instant switching between 4 core study hubs plus an on-demand Settings surface:

| Panel | Type | Description |
|---|---|---|
| **FOCUS** (`AppPanel.FOCUS`) | Primary | Main study dashboard: interactive timer ring/bubbles, session pills, quick actions, ambient sounds, flip-to-focus & live room banner |
| **PLANNER** (`AppPanel.PLANNER`) | Primary | Daily & weekly task management, checklists, time targets, progress bars, recurring goals & calendar history |
| **INSIGHTS / STATS** (`AppPanel.STATS`) | Primary | Deep analytics dashboard: today's summary, historical trend bars, subject pie/donut chart, heatmap, burnout streak freeze & timeline |
| **COMMUNITY** (`AppPanel.COMMUNITY`) | Primary | Virtual study rooms, real-time participant presence, live study statuses & global competitive leaderboards |
| **SETTINGS** (`AppPanel.SETTINGS`) | Secondary | Hub-and-spoke system control center: timer configurations, visual themes, backups, personal student profile & developer tools |

---

## 2. Persistent Universal Header

Rendered across top-level screens for unified navigation and status awareness:
- **Left: Profile & Streak Capsule (🔥 `X`d)**
  - Displays user avatar (or initial letter badge) + display name + active study streak days.
  - **Action**: Tap opens **Profile Settings** (`AppSettingsTab.PROFILE`).
- **Center: Screen Title** (`FOCUS`, `PLANNER`, `INSIGHTS`, `COMMUNITY`).
- **Right Context Actions**:
  - Contextual action buttons (e.g., Planner overflow `⋮`, Fullscreen heatmap button).
  - **Settings Gear (`⚙️`)**: Opens **Settings Hub** (`AppSettingsTab.HUB`).

---

## 3. Screen-by-Screen Menu & Sub-Menu Breakdown

### A. FOCUS PANEL (Main Timer Screen)

#### 1. Interactive Session Capsule Row (Top Pills)
- **Subject Selection Pill** (🎨 Dot + Subject Name + `▾`)
  - **Sub-Menu / Modal**: Opens **Subject Picker Dialog** (`showSubjectPickerDialog`).
    - Select from existing subjects.
    - Quick-create new subject via **Add Custom Subject Dialog** (`showAddCustomSubjectDialog`).
    - Long-press or manage button opens **Manage Subjects Dialog** (`showManageSubjectsDialog`) & **Edit Subject Dialog**.
- **Chapters / Sub-Topic Pill** (📖 Topic Label + `▾`)
  - Appears when subject tagging is active.
  - **Sub-Menu / Modal**: Opens **Sub-Topic Picker Dialog** (`showSubTopicPickerDialog`).
    - Choose chapter / sub-topic.
    - Add custom chapter, rename, or delete existing chapters.
- **Active Task / Goal Pill** (🎯 Goal Title + `✕`)
  - Replaces or accompanies topic pill when a task is pinned from Planner.
  - Tap opens **Planner Panel** directly to review session goal.
  - `✕` button clears active goal.
- **Timer Mode Pill** (e.g., `Stopwatch ▾`, `25/5 Pomodoro ▾`, `Lecture ▾`)
  - **Sub-Menu / Sheet**: Opens **Timer Mode Bottom Sheet** (`TimerModeBottomSheet.show`).
    - **Countdown / Pomodoro**: Classic interval study sessions.
    - **Stopwatch**: Count-up uninterrupted focus.
    - **Subject-Wise**: Goal-targeted subject block.
    - **Lecture Mode**: Continuous note/class tracking with subject tag auto-save.

#### 2. Connected Room Live Banner
- When connected to a Study Room:
  - Displays room name, live member counter (`🟢 X studying`), and study room quick controls.
  - Tap opens **Study Room Detail Modal** in Community.

#### 3. Main Center Timer Visualizer
- **Visual Display**: Circular SVG ring / 3D floating bubble mode / Classic digital HUD.
- **Center Time Display**: Interactive tap / long-press:
  - In Countdown Mode: Tap allows quick duration adjustment or custom minute picker dialog.
- **Tag Subtitle**: Active subject & topic indicator.

#### 4. Timer Controls & Secondary Quick Action Strip
- **Main Action Bar**:
  - `▶ Start` / `⏸ Pause` / `⏹ Finish/Stop` / `⏭ Skip Break`
- **Quick Action Buttons**:
  - **Ambient Sound Pill** (`🎵 Lo-Fi / Rain / White Noise / Off`):
    - **Sub-Menu / Dialog**: Opens **Ambient Sound Selector Sheet** with volume slider & soundscapes.
  - **Flip to Focus Toggle** (`📱 Flip Mode`):
    - Activates proximity/sensor-driven face-down study mode.
  - **Fullscreen Zen Mode** (`⛶ Fullscreen`):
    - Switches to immersive distraction-free fullscreen OLED clock.

---

### B. PLANNER PANEL (Task & Goal Management)

#### 1. Top Header Actions
- **Overflow Menu Button (`⋮`)**:
  - **Sub-Menu / Dialog**: **Planner Options Menu** (`showPlannerOptionsMenu`)
    1. **Weekly Goal Tracker**: Opens **Planner Matrix Dialog** (`showPlannerMatrixDialog`) — 7-day grid of goal completions.
    2. **Yesterday's Goals**: Opens **Edit Yesterday's Goals Dialog** (`showEditYesterdayGoalsDialog`) — retroactive checks & log adjustment.
    3. **Goal Colors & Themes**: Opens **Planner Theme Picker Dialog** (`showPlannerThemePickerDialog`) — custom palette accents.
    4. **Clear Completed Goals**: One-tap archive of checked items.

#### 2. Overview Progress Bar & Daily Targets
- **Daily Target Capsule**: Shows completed vs total minutes planned.
- **Interactive Progress Bar**: Visual percentage completion of all session goals for today.

#### 3. Goal Item Cards & Context Menus
- **Goal Item Checkbox**: Toggle complete/incomplete with haptic feedback.
- **Item Tap / Long-Press Action Menu** (`showGoalActionMenu`):
  - **Edit Goal**: Opens **Add/Edit Session Goal Dialog** (Title, Subject, Chapter, Target Mins, Scheduled Days, Reminder Time).
  - **Goal History & Calendar**: Opens **Goal History Dialog** (`showGoalHistoryDialog`) — monthly calendar showing every day this specific goal was checked.
  - **Set as Active Focus Task**: Pins this goal directly to the timer capsule on the Focus panel.
  - **Delete Goal**: Confirmation dialog to delete goal.

#### 4. Floating Action Button (`+`)
- **Modal**: Opens **Add Session Goal Dialog** (`showAddSessionGoalDialog`):
  - Title input field.
  - Subject tag selector chip row.
  - Chapter / sub-topic selector.
  - Target duration picker (minutes).
  - Daily reminder toggle & TimePicker dialog.
  - Day-of-week recurrence selector (Mon–Sun).

---

### C. INSIGHTS / STATS PANEL (Analytics & History)

#### 1. Segment Switcher Tabs
- **`Overview`**: High-level metrics, charts, heatmaps, and highlight cards.
- **`Timeline & History`**: Detailed chronological calendar, session logs, and daily timeline viewer.

#### 2. Overview Sub-Screen Elements & Menus
- **Hero Metric Card**:
  - Today's study time, remaining goal duration badge, break duration badge, and 7-day average.
  - Filter chips: `7 Days`, `30 Days`, `All Time`.
- **Subject Breakdown Donut/Pie Chart**:
  - Filter Period Tabs: `Today`, `This Week`, `This Month`, `All Time`.
  - **Interactive Slices**: Tap slice to elevate it and view subject emoji, title, duration, and percentage inside the center HUD.
  - **"View Details ›" Header Action**: Opens **Subject Breakdown Modal** (`showPieChartDetailsModal`) with detailed tabular duration logs.
- **Study Trend Bar Chart**:
  - Bar rows with daily comparison against daily target line.
  - Tap any bar: Opens **Day Details Dialog** (`showDayDialog`) listing individual focus blocks.
- **Focus Heatmap Card**:
  - Year-long contribution grid.
  - Tap any cell: Opens **Day Details Dialog**.
  - **Fullscreen Action (`⛶`)**: Opens **Fullscreen Heatmap Sub-Panel** (`AppPanel.HEATMAP`) with subject-filter chips (`All Subjects`, individual subjects).
- **Daily Study Rhythm Card** (24-Hour Focus Distribution):
  - Segment: `Last 7 Days` vs `Last 30 Days`.
  - Dismiss `✕` button: Permanently hides card until re-enabled in Settings.
- **Detailed History Calendar Banner**:
  - Quick launcher into Timeline & History calendar.
- **Study Scorecard Banner**:
  - Tap opens **Study Scorecard Export Dialog** (`showExportDialog`) to generate shareable PNG/image summary cards.
- **Pinned Highlights Metric Grid**:
  - **Customize › Button**: Opens **Customize Highlights Dialog** (`showCustomizeHighlightsDialog`) to choose which metrics appear.
  - Highlight Cards (each tap opens a deep-dive analysis modal):
    1. **Weekly Trend Card** → Opens **Weekly Trend Detail Dialog** (`showWeeklyTrendDetailDialog`).
    2. **Active Days / Streak Card** → Opens **Active Days Detail Dialog** (`showActiveDaysDetailDialog`).
    3. **Best Day Card** → Opens **Best Day Detail Dialog** (`showBestDayDetailDialog`).
    4. **Record Week Card** → Opens **Record Week Detail Dialog** (`showRecordWeekDetailDialog`).
    5. **Goal Success Rate Card** → Opens **Goal Success Detail Dialog** (`showGoalSuccessDetailDialog`).
    6. **Average Session Duration Card** → Opens **Avg Session Detail Dialog** (`showAvgSessionDetailDialog`).
- **Burnout Prevention / Streak Freeze Protection**:
  - Shows freeze status (`X / 2 Left This Month`).
  - Action: Opens **Streak Freeze Dialog** (`showStreakFreezeDialog`) to manually apply a rest day.

#### 3. Timeline & History Sub-Screen Elements & Menus
- **Month Calendar Grid**:
  - Visual dots indicating days studied.
  - Month navigation chevrons (`‹`, `›`).
- **Day Timeline View**:
  - 24-hour visual vertical track displaying exact study blocks, break blocks, and tagged subjects.
  - Tap session block: View session details (Start time, End time, Duration, Subject, Sub-topic).
  - Delete or edit session capability.

---

### D. COMMUNITY PANEL (Study Rooms & Leaderboard)

#### 1. Segment Switcher Tabs
- **`Study Rooms`** (Virtual Co-Working Rooms)
- **`Leaderboard`** (Global & Friends Rankings)

#### 2. Study Rooms Sub-Screen Elements & Menus
- **Active Room Header / Quick Join Banner**.
- **Room List Cards**:
  - Room Title, Emoji, Topic tag, member count, and "Join" button.
- **Create Room Button (`+ Create Room`)**:
  - Opens **Create Study Room Dialog** (Room Name, Icon/Emoji, Description, Max Capacity, Password/Public toggle).
- **Inside Room Interface / Sheet**:
  - Real-time study status of active members (`Studying Physics`, `On Break`, `Idle`).
  - Send study reaction / encouragement cheers (`👏`, `🔥`, `☕`).
  - Leave Room button.

#### 3. Leaderboard Sub-Screen Elements & Menus
- **Period Filter Switcher**:
  - `Daily` | `Weekly` | `Monthly`
- **User Rank Card (Sticky at bottom)**:
  - Personal rank, avatar, user name, study hours, and streak count.
- **Leaderboard Rankings List**:
  - Tap any user card: Opens **User Public Profile Modal** (Bio, target exams, study streak, subject breakdown).

---

### E. SETTINGS (Hub & Spoke Architecture)

#### 1. Settings Hub (`AppSettingsTab.HUB`)
- **Top User Profile Summary Card**:
  - Avatar, Display Name, Bio, Cloud Sync Status. Tap opens **Profile Settings**.
- **Category Navigation Cards**:
  1. **Timer & Focus** → `AppSettingsTab.TIMER`
  2. **Planner & Goals** → `AppSettingsTab.PLANNER`
  3. **Insights & Stats** → `AppSettingsTab.ANALYTICS`
  4. **Theme & Appearance** → `AppSettingsTab.THEME`
  5. **Cloud & Backups** → `AppSettingsTab.CLOUD`
  6. **Community & Privacy** → `AppSettingsTab.COMMUNITY`
  7. **Developer Tools** → `AppSettingsTab.DEVELOPER` (Appears when unlocked)

---

#### 2. Settings Sub-Menus in Detail

##### I. Timer & Focus Settings (`AppSettingsTab.TIMER`)
- **Focus Durations**:
  - Study duration slider / picker (Default: 25 mins).
  - Short break duration slider / picker (Default: 5 mins).
  - Long break duration slider / picker (Default: 15 mins).
  - Long break interval (After every `X` Pomodoro cycles).
- **Automation & Behaviors**:
  - Auto-start breaks toggle.
  - Auto-start next study session toggle.
  - Continuous Stopwatch auto-lap toggle.
- **Strict Study Enforcement**:
  - Strict Mode (Block leaving app during timer).
  - Flip to focus sensitivity settings.
- **Audio & Haptics**:
  - Sound effects toggle (Bell, chime, digital buzzer).
  - Vibration / Haptic intensity selector (Light, Medium, Strong, Off).

##### II. Planner & Goals Settings (`AppSettingsTab.PLANNER`)
- **Daily Focus Targets**:
  - Daily study goal duration picker (`e.g. 4h 00m`).
- **Goal Notifications**:
  - Evening goal review reminder toggle & time selector.
  - Morning motivation reminder toggle & time selector.
- **Archive & History**:
  - Auto-archive completed goals toggle.
  - Export planner history to CSV/JSON.

##### III. Insights & Stats Settings (`AppSettingsTab.ANALYTICS`)
- **Chart Customization**:
  - Heatmap display toggle (`show_focus_heatmap`).
  - Daily Study Rhythm distribution toggle (`show_focus_pattern`).
  - Subject Breakdown chart toggle (`show_subject_pie_chart`).
  - Donut Chart vs Solid Pie Chart mode selector (`use_donut_chart`).
- **Highlights Metric Selector**:
  - Choose which of the 6 core metric widgets appear on the Overview dashboard.

##### IV. Theme & Appearance Settings (`AppSettingsTab.THEME`)
- **Base Theme Mode**:
  - Dark Mode (OLED / Pitch Black `#000000`).
  - Slate Dark Mode (Charcoal `#111625`).
  - Light Mode.
  - Follow System Dark/Light setting.
- **Timer Visual Styles**:
  - Minimalistic Stroke Ring.
  - 3D Glassmorphic Glow Ring.
  - Floating Physics Bubbles.
  - Classic Retro Digital Clock.
- **Accent Color Palette**:
  - Indigo / Violet / Electric Emerald / Amber / Coral / Sky Blue / Custom Hex.
- **Font Style Selector**:
  - Opens **Font Selection Dialog** (`showFontSelectionDialog`) with live typography previews.

##### V. Cloud & Backups Settings (`AppSettingsTab.CLOUD`)
- **Google Account Sync**:
  - Sign in / Sign out with Google.
  - Auto-sync on WiFi toggle.
  - "Sync Now" button with timestamp of last successful cloud backup.
- **Manual Backup & Recovery**:
  - **Export Backup to File**: Creates timestamped `.json` snapshot of all subjects, sessions, and planner goals.
  - **Import / Restore Backup**: File picker to restore app state from file.
  - **Export Sessions to CSV**: Spreadsheet-compatible study log export.

##### VI. Profile Settings (`AppSettingsTab.PROFILE`)
- **Avatar Customization**:
  - Choose Photo from Gallery launcher (`pickAvatarLauncher`).
  - Take Photo with Camera.
  - Preset Avatar Icons grid.
  - Remove Custom Avatar / Use Initials.
- **Identity & Study Information**:
  - Display Name input dialog.
  - Username / Tag.
  - Bio / Motto input dialog (`e.g. "Pre-Med student | 6 hours daily"`).
  - Target Exam / Goal tag picker (e.g., USMLE, MCAT, JEE, NEET, Bar, Finals).

##### VII. Community & Privacy Settings (`AppSettingsTab.COMMUNITY`)
- **Public Visibility**:
  - Show profile on global leaderboards toggle.
  - Share active subject status with study room members toggle.
  - Anonymous mode toggle.

##### VIII. Developer Tools Sub-Menu (`AppSettingsTab.DEVELOPER`)
- *Unlocked by tapping version code 7 times in About section.*
- **Database & State Debugging**:
  - Inspect SQLite database / Room inspect.
  - Dump SharedPreferences cache.
  - Seed mock data (Generate 30 days of study history).
  - Clear all cache without deleting account.
- **Hardware & Sensor Calibration**:
  - Test Proximity / Gyroscope for Flip-to-Focus.
  - Test Notification Channels & Foreground Services.

---

## 4. Global Modals & Dialog System Quick Reference

| Dialog Name | Trigger Location | Purpose |
|---|---|---|
| **Subject Picker Dialog** | Focus Pill, Settings | Select or switch active study subject |
| **Add / Edit Subject Dialog** | Subject Picker | Configure subject name, color palette, emoji |
| **Manage Subjects Dialog** | Subject Picker | Reorder, archive, or delete subjects |
| **Sub-Topic Picker Dialog** | Focus Pill, Subject Dialog | Manage chapters and sub-topics |
| **Timer Mode Sheet** | Focus Mode Pill | Switch Countdown, Stopwatch, Subject, Lecture |
| **Ambient Sound Sheet** | Focus Bottom Bar | Select background white noise, rain, lofi |
| **Add / Edit Goal Dialog** | Planner FAB (`+`), Goal Card | Create or edit task, subject, target, reminder |
| **Planner Options Dialog** | Planner Header `⋮` | Matrix view, yesterday's goals, color theme |
| **Goal History Calendar** | Goal Card Context Menu | View 30-day completion calendar for specific task |
| **Study Scorecard Export Dialog**| Stats Panel Banner | Generate shareable PNG summary graphic |
| **Day Details Dialog** | Heatmap cell tap, Bar tap | View timeline breakdown for a specific date |
| **Subject Breakdown Modal** | Stats Chart Header | View numerical duration tables per subject |
| **Streak Freeze Dialog** | Stats Streak Freeze Card | Apply monthly rest day freeze |
| **Streak Up Animation Dialog** | On session completion | Fullscreen celebratory streak level-up modal |
| **Battery Optimization Dialog**| On first launch / Settings | Prompts exempting app from background killer |
| **In-App Update Dialog** | Automated version check | Download and install new release APKs |

---

*Generated from active codebase source files in `com.madeby.JAI`.*

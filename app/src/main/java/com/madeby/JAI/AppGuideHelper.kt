package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Interactive Feature Discovery & App Guide dialog.
 * Surfaces all major StudyTimer features in plain, student-friendly language
 * with full-card tap targets, split-second highlight feedback, and 1-tap deep links.
 */
object AppGuideHelper {

    var pendingHighlightKey: String? = null
    private var isNudgeShowing = false

    private enum class GuideCategory(val label: String) {
        ALL("All"),
        FOCUS("Focus"),
        HABITS("Habits"),
        INSIGHTS("Progress"),
        STYLE("Style & Backup")
    }

    private data class GuideFeature(
        val category: GuideCategory,
        val title: String,
        val description: String,
        val statusBadge: String? = null,
        val statusActive: Boolean = false,
        val actionLabel: String? = null,
        val onAction: ((MainActivity, Dialog) -> Unit)? = null
    )

    fun show(host: MainActivity) {
        if (host.isFinishing || host.isDestroyed) return

        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val theme = host.themeCoordinator
        val primary = theme.primaryColor
        val onPrimary = ThemeCoordinator.getContrastColor(primary)
        fun dp(v: Int): Int = host.dp(v)
        fun tinted(c: Int, a: Int): Int = host.tintedColor(c, a)

        val isBlockerOn = AppBlockerManager.isEnabled(host)
        val blockedCount = AppBlockerManager.getBlockedPackages(host).size
        val isFlipOn = FlipToFocusManager.isEnabled(host)

        val features = listOf(
            GuideFeature(
                category = GuideCategory.HABITS,
                title = "App Blocker",
                description = "Blocks distracting apps like Instagram or games while your study timer is running.",
                statusBadge = if (isBlockerOn) "On ($blockedCount apps)" else "Off",
                statusActive = isBlockerOn,
                actionLabel = "Set Up",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    AppBlockerSettingsSheet.show(act) {
                        act.buildCurrentPanel()
                    }
                }
            ),
            GuideFeature(
                category = GuideCategory.FOCUS,
                title = "Flip-to-Focus",
                description = "Put your phone face down on the desk to start studying, and pick it up to pause.",
                statusBadge = if (isFlipOn) "On" else "Off",
                statusActive = isFlipOn,
                actionLabel = "Turn On",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    pendingHighlightKey = "flip_to_focus"
                    act.currentSettingsTab = AppSettingsTab.TIMER
                    act.settingsOriginPanel = act.currentPanel
                    act.settingsOriginWasSubTab = false
                    act.navigateToPanel(AppPanel.SETTINGS)
                }
            ),
            GuideFeature(
                category = GuideCategory.FOCUS,
                title = "4 Study Timer Modes",
                description = "Pick 25/5 Pomodoro, a countdown timer, a stopwatch, or your daily class timetable.",
                actionLabel = "Pick Mode",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    if (act.currentTimerState == TimerState.IDLE) {
                        TimerModeBottomSheet.show(act) {
                            act.buildCurrentPanel()
                        }
                    } else {
                        act.navigateToPanel(AppPanel.FOCUS)
                    }
                }
            ),
            GuideFeature(
                category = GuideCategory.FOCUS,
                title = "Study Sounds",
                description = "Play calming rain, fireplace, ocean waves, or soft bells while you study.",
                actionLabel = "Choose",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    AmbientSoundBottomSheet.show(act)
                }
            ),
            GuideFeature(
                category = GuideCategory.FOCUS,
                title = "Desk Clock & Fullscreen",
                description = "Turn your phone sideways for a clean desk clock, or tap the timer to hide buttons."
            ),
            GuideFeature(
                category = GuideCategory.HABITS,
                title = "Daily Study Goals & Chapters",
                description = "Add today's tasks, link them to your subjects and chapters, and track how much is left.",
                actionLabel = "Open",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    act.navigateToPanel(AppPanel.PLANNER)
                }
            ),
            GuideFeature(
                category = GuideCategory.INSIGHTS,
                title = "Study Heatmap & Subject Chart",
                description = "See your daily study hours, subject breakdown, and your full-year study streak grid.",
                actionLabel = "View",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    act.currentStatsTab = AppStatsTab.OVERVIEW
                    act.navigateToPanel(AppPanel.STATS)
                }
            ),
            GuideFeature(
                category = GuideCategory.INSIGHTS,
                title = "Streak Freeze",
                description = "Save your study streak on sick or rest days using your monthly Streak Freezes.",
                actionLabel = "Open",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    StatsPanelBuilder(act).showStreakFreezeDialog()
                }
            ),
            GuideFeature(
                category = GuideCategory.INSIGHTS,
                title = "Exam Countdown & History",
                description = "Add your upcoming exam dates and check your day-by-day study log.",
                actionLabel = "Open",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    act.currentStatsTab = AppStatsTab.TIMELINE
                    act.navigateToPanel(AppPanel.STATS)
                }
            ),
            GuideFeature(
                category = GuideCategory.STYLE,
                title = "Themes, Wallpapers & Fonts",
                description = "Switch between Light and Dark mode, clean or glass cards, colors, fonts, and wallpapers.",
                actionLabel = "Customize",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    act.currentSettingsTab = AppSettingsTab.THEME
                    act.settingsOriginPanel = act.currentPanel
                    act.settingsOriginWasSubTab = false
                    act.navigateToPanel(AppPanel.SETTINGS)
                }
            ),
            GuideFeature(
                category = GuideCategory.STYLE,
                title = "Google Backup & Home Widgets",
                description = "Back up your study history with Google so you never lose it, or add home screen widgets.",
                actionLabel = "Open",
                onAction = { act, dlg ->
                    dlg.dismiss()
                    act.currentSettingsTab = AppSettingsTab.CLOUD
                    act.settingsOriginPanel = act.currentPanel
                    act.settingsOriginWasSubTab = false
                    act.navigateToPanel(AppPanel.SETTINGS)
                }
            )
        )

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.createDialogBackground(24f)
            setPadding(dp(18), dp(18), dp(18), dp(16))
        }

        // Header
        val titleCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), 0, dp(2), dp(4))
        }
        titleCol.addView(TextView(host).apply {
            text = "Explore StudyTimer"
            setTextColor(theme.textColor)
            textSize = 19f
            typeface = theme.getAppTypeface(isBold = true)
        })
        titleCol.addView(TextView(host).apply {
            text = "Tap any card to jump straight to that feature"
            setTextColor(theme.textColor)
            alpha = 0.65f
            textSize = 12f
            setPadding(0, dp(2), 0, 0)
        })
        root.addView(titleCol)

        // Category Filter Row
        var selectedCategory = GuideCategory.ALL
        val chipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val chipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            addView(chipRow)
        }
        root.addView(chipScroll)

        val scrollView = ScrollView(host).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val listContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listContainer)
        root.addView(scrollView)

        var isNavigating = false

        fun renderList() {
            listContainer.removeAllViews()
            val visible = if (selectedCategory == GuideCategory.ALL) {
                features
            } else {
                features.filter { it.category == selectedCategory }
            }

            for (item in visible) {
                val normalBgColor = if (theme.isDarkMode()) 0x16FFFFFF.toInt() else 0xFFF8FAFC.toInt()
                val normalStrokeColor = if (theme.isDarkMode()) 0x26FFFFFF.toInt() else 0xFFE2E8F0.toInt()
                val cardBg = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(normalBgColor)
                    setStroke(dp(1), normalStrokeColor)
                }

                val card = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = cardBg
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(9))
                    }
                    if (item.onAction != null) {
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { view ->
                            if (isNavigating) return@setOnClickListener
                            isNavigating = true
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            cardBg.setColor(tinted(primary, 36))
                            cardBg.setStroke(dp(2), primary)
                            view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90L).withEndAction {
                                view.postDelayed({
                                    if (!host.isFinishing && !host.isDestroyed) {
                                        item.onAction.invoke(host, dialog)
                                    }
                                }, 80L)
                            }.start()
                        }
                    }
                }

                val topRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                topRow.addView(TextView(host).apply {
                    text = item.title
                    setTextColor(theme.textColor)
                    textSize = 14f
                    typeface = theme.getAppTypeface(isBold = true)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(8)
                    }
                })

                if (item.statusBadge != null) {
                    val badgeColor = if (item.statusActive) 0xFF10B981.toInt() else theme.textColor
                    topRow.addView(TextView(host).apply {
                        text = item.statusBadge
                        textSize = 10.5f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(if (item.statusActive) 0xFF10B981.toInt() else tinted(theme.textColor, 175))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(10).toFloat()
                            setColor(tinted(badgeColor, if (item.statusActive) 28 else 18))
                        }
                        setPadding(dp(8), dp(3), dp(8), dp(3))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (item.actionLabel != null) marginEnd = dp(6)
                        }
                    })
                }

                if (item.actionLabel != null && item.onAction != null) {
                    topRow.addView(TextView(host).apply {
                        text = item.actionLabel
                        textSize = 11.5f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(tinted(primary, 28))
                            setStroke(dp(1), tinted(primary, 90))
                        }
                        setPadding(dp(10), dp(4), dp(10), dp(4))
                    })
                }

                card.addView(topRow)

                card.addView(TextView(host).apply {
                    text = item.description
                    setTextColor(theme.textColor)
                    alpha = 0.72f
                    textSize = 12f
                    setLineSpacing(0f, 1.18f)
                    setPadding(0, dp(5), 0, 0)
                })

                listContainer.addView(card)
            }
        }

        fun renderChips() {
            chipRow.removeAllViews()
            for (cat in GuideCategory.values()) {
                val isSelected = (cat == selectedCategory)
                val chip = TextView(host).apply {
                    text = cat.label
                    textSize = 11.5f
                    typeface = theme.getAppTypeface(isBold = isSelected)
                    setTextColor(if (isSelected) onPrimary else tinted(theme.textColor, 210))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setColor(primary)
                        } else {
                            setColor(if (theme.isDarkMode()) 0x1AFFFFFF.toInt() else 0xFFF1F5F9.toInt())
                            setStroke(dp(1), if (theme.isDarkMode()) 0x26FFFFFF.toInt() else 0xFFE2E8F0.toInt())
                        }
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (selectedCategory != cat) {
                            selectedCategory = cat
                            renderChips()
                            renderList()
                            scrollView.scrollTo(0, 0)
                        }
                    }
                }
                chipRow.addView(chip)
            }
        }

        renderChips()
        renderList()

        val closeBtn = TextView(host).apply {
            text = "Got It"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(onPrimary)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(primary)
            }
            setPadding(0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(12), 0, 0)
            }
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
            }
        }
        root.addView(closeBtn)

        dialog.setOnDismissListener {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_seen_app_guide", true)
                .apply()
        }
        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (host.resources.displayMetrics.widthPixels * 0.94f).toInt(),
                (host.resources.displayMetrics.heightPixels * 0.84f).toInt()
            )
        }
        dialog.show()
    }

    /**
     * Shows a lightweight, one-time contextual tip after a real completed study session.
     * Enforces global cooldowns and never repeats once actually shown.
     */
    fun maybeShowPostSessionNudge(host: MainActivity, savedStudySecs: Long) {
        if (savedStudySecs < 60L || host.isFinishing || host.isDestroyed || isNudgeShowing) return

        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("guide_completed_sessions_count", 0) + 1
        val activeSubject = SubjectTagManager.getSelectedSubject(host)
        val generalCount = if (activeSubject.id == "general") {
            prefs.getInt("guide_general_sessions_count", 0) + 1
        } else {
            prefs.getInt("guide_general_sessions_count", 0)
        }
        prefs.edit()
            .putInt("guide_completed_sessions_count", count)
            .putInt("guide_general_sessions_count", generalCount)
            .apply()

        val lastNudgeCount = prefs.getInt("guide_last_nudge_session_count", -10)
        if (count - lastNudgeCount < 2) return
        if (!canShowNudge(host, prefs, force = false)) return

        val timerMode = prefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
        val activeSubTopic = SubjectTagManager.getSelectedSubTopic(host, activeSubject.id)

        val nudgeId: String? = when {
            !prefs.getBoolean("nudge_shown_app_blocker", false) && !AppBlockerManager.isEnabled(host) -> "APP_BLOCKER"
            count >= 2 && activeSubject.id != "general" && activeSubTopic.isNullOrBlank() && !prefs.getBoolean("nudge_shown_chapter_tracking", false) -> "CHAPTER_TRACKING"
            count >= 3 && timerMode == "SUBJECT" && !prefs.getBoolean("nudge_shown_pomodoro_mode", false) -> "POMODORO_MODE"
            count >= 4 && !AuthManager.isLoggedIn(host) && !prefs.getBoolean("nudge_shown_cloud_backup", false) -> "CLOUD_BACKUP"
            count >= 5 && !prefs.getBoolean("nudge_shown_insights_heatmap", false) -> "INSIGHTS_HEATMAP"
            generalCount >= 3 && !prefs.getBoolean("nudge_shown_subject_tags", false) -> "SUBJECT_TAGS"
            count >= 7 && !prefs.getBoolean("nudge_shown_flip_to_focus", false) && !FlipToFocusManager.isEnabled(host) -> "FLIP_TO_FOCUS"
            count >= 9 && !prefs.getBoolean("nudge_shown_home_widgets", false) -> "HOME_WIDGETS"
            count >= 11 && savedStudySecs >= 1500L && !prefs.getBoolean("nudge_shown_landscape_clock", false) -> "LANDSCAPE_CLOCK"
            else -> null
        }

        if (nudgeId == null) return

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed) return@postDelayed
            if (!canShowNudge(host, prefs, force = false)) return@postDelayed
            prefs.edit().putInt("guide_last_nudge_session_count", count).apply()
            triggerNudgeById(host, nudgeId, force = false)
        }, 550L)
    }

    fun maybeShowPlannerSubjectNudge(host: MainActivity, hasGoalsWithoutSubjectOrTopic: Boolean, force: Boolean = false) {
        if (!force && !hasGoalsWithoutSubjectOrTopic) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_planner_subject", false)) return
        if (!canShowNudge(host, prefs, force)) return

        val delay = if (force) 0L else 450L
        host.window.decorView.postDelayed({
            if (!force && (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.PLANNER)) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Link goals to a subject",
                message = "Pick a subject or chapter when adding a goal so your study timer counts toward it automatically.",
                actionText = "Add Goal",
                prefFlag = "nudge_shown_planner_subject",
                force = force
            ) { act ->
                PlannerPanelBuilder(act).showAddSessionGoalDialog()
            }
        }, delay)
    }

    fun maybeShowPauseSoundNudge(host: MainActivity, force: Boolean = false) {
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && (StudyAudioManager.isPlayingAmbient() || prefs.getBoolean("nudge_shown_pause_sounds", false))) return
        if (!canShowNudge(host, prefs, force)) return

        val delay = if (force) 0L else 450L
        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Add calm study sounds?",
                message = "Play gentle rain, fireplace, or ocean sounds in the background while you study.",
                actionText = "Choose Sound",
                prefFlag = "nudge_shown_pause_sounds",
                force = force
            ) { act ->
                AmbientSoundBottomSheet.show(act)
            }
        }, delay)
    }

    fun maybeShowStreakFreezeNudge(host: MainActivity, streak: Int, force: Boolean = false) {
        if (!force && streak < 3) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_streak_freeze", false)) return
        if (!canShowNudge(host, prefs, force)) return

        val displayStreak = if (streak > 0) streak else 3
        val delay = if (force) 0L else 550L
        host.window.decorView.postDelayed({
            if (!force && (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS)) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Protect your $displayStreak-day streak",
                message = "Taking a rest or sick day? Use a Streak Freeze from Today's Focus so your streak stays safe.",
                actionText = "See Freeze",
                prefFlag = "nudge_shown_streak_freeze",
                force = force
            ) { act ->
                StatsPanelBuilder(act).showStreakFreezeDialog()
            }
        }, delay)
    }

    fun maybeShowExamCountdownNudge(host: MainActivity, force: Boolean = false, onAddExam: () -> Unit) {
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_exam_countdown", false)) return
        if (!canShowNudge(host, prefs, force)) return

        val delay = if (force) 0L else 450L
        host.window.decorView.postDelayed({
            if (!force && (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS)) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Have an upcoming exam?",
                message = "Add your exam date here to see a live countdown right above your study history.",
                actionText = "Add Exam",
                prefFlag = "nudge_shown_exam_countdown",
                force = force
            ) { _ -> onAddExam() }
        }, delay)
    }

    fun maybeShowBlockerPermissionNudge(host: MainActivity, force: Boolean = false) {
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_blocker_permission", false)) return
        if (!canShowNudge(host, prefs, force, bypassCooldown = true)) return

        val delay = if (force) 0L else 350L
        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed) return@postDelayed
            showNudgeCard(
                host = host,
                title = "App Blocker was paused",
                message = "Display over other apps permission was removed in device settings. Re-grant it to keep blocking distractions.",
                actionText = "Fix Permission",
                prefFlag = "nudge_shown_blocker_permission",
                force = force
            ) { act ->
                AppBlockerSettingsSheet.show(act) { act.buildCurrentPanel() }
            }
        }, delay)
    }

    fun maybeShowTimelineEditNudge(host: MainActivity, todayFocusSecs: Long, force: Boolean = false) {
        if (!force && todayFocusSecs > 0L) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_timeline_edit", false)) return
        if (!canShowNudge(host, prefs, force)) return

        val delay = if (force) 0L else 500L
        host.window.decorView.postDelayed({
            if (!force && (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS)) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Studied offline from a book?",
                message = "Tap any day in the monthly calendar below to log or adjust missed study sessions manually.",
                actionText = null,
                prefFlag = "nudge_shown_timeline_edit",
                force = force,
                onAction = null
            )
        }, delay)
    }

    fun maybeShowWeeklyShareNudge(host: MainActivity, weeklyFocusSecs: Long, force: Boolean = false) {
        val isSunday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
        if (!force && (!isSunday || weeklyFocusSecs < 18_000L)) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_weekly_share", false)) return
        if (!canShowNudge(host, prefs, force)) return

        val delay = if (force) 0L else 550L
        host.window.decorView.postDelayed({
            if (!force && (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS)) return@postDelayed
            showNudgeCard(
                host = host,
                title = "Share your weekly scorecard",
                message = "Export a clean summary card of your weekly study hours and subject breakdown.",
                actionText = "Create Card",
                prefFlag = "nudge_shown_weekly_share",
                force = force
            ) { act ->
                act.showStudyScorecardExportDialog()
            }
        }, delay)
    }

    fun maybeShowBreakHealthNudge(host: MainActivity, continuousStudySecs: Long, force: Boolean = false) {
        if (!force && continuousStudySecs < 5400L) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean("nudge_shown_break_health", false)) return
        if (!force && (host.isFinishing || host.isDestroyed || isNudgeShowing)) return
        if (!force && prefs.getBoolean("guide_nudges_muted", false)) return

        showNudgeCard(
            host = host,
            title = "90+ mins without a break",
            message = "A short 5-minute break helps memory retention and prevents burnout during long study blocks.",
            actionText = "Take 5m Break",
            prefFlag = "nudge_shown_break_health",
            force = force
        ) { act ->
            act.handleStartBreakOnly(300L)
        }
    }

    data class NudgeTestItem(val id: String, val label: String, val description: String)

    fun getAllNudgeTestItems(): List<NudgeTestItem> = listOf(
        NudgeTestItem("APP_BLOCKER", "App Blocker Setup", "Post-session distraction blocker tip"),
        NudgeTestItem("INSIGHTS_HEATMAP", "Insights & Heatmap", "Post-session progress overview tip"),
        NudgeTestItem("POMODORO_MODE", "Pomodoro / Timer Modes", "Try 25/5 Pomodoro & Countdown modes"),
        NudgeTestItem("CHAPTER_TRACKING", "Chapter-Level Tracking", "Track time per chapter instead of whole subject"),
        NudgeTestItem("LANDSCAPE_CLOCK", "Landscape Desk Clock", "Rotate phone sideways for desk clock"),
        NudgeTestItem("FLIP_TO_FOCUS", "Flip-to-Focus", "Face-down hands-free study tip"),
        NudgeTestItem("SUBJECT_TAGS", "Subject Tags", "Separate General time into subjects"),
        NudgeTestItem("CLOUD_BACKUP", "Google Cloud Backup", "Sign in to back up logs & streaks"),
        NudgeTestItem("HOME_WIDGETS", "Home Screen Widgets", "Add StudyTimer widgets to home screen"),
        NudgeTestItem("PLANNER_SUBJECT", "Planner Goal Linking", "Link Planner goals to subjects/chapters"),
        NudgeTestItem("PAUSE_SOUNDS", "Ambient Study Sounds", "Rain, fireplace & ocean focus sounds"),
        NudgeTestItem("STREAK_FREEZE", "Streak Freeze", "Protect 3+ day streak on rest days"),
        NudgeTestItem("EXAM_COUNTDOWN", "Exam Countdown", "Add exam date above study history"),
        NudgeTestItem("BLOCKER_PERMISSION", "Blocker Permission Recovery", "Alert when overlay permission is revoked"),
        NudgeTestItem("TIMELINE_EDIT", "Offline / Book Session Log", "Log missed offline sessions in Timeline"),
        NudgeTestItem("WEEKLY_SHARE", "Weekly Scorecard Share", "Export shareable weekly summary card"),
        NudgeTestItem("BREAK_HEALTH", "90-Min Break Health", "Suggest 5-min break after 90m continuous study")
    )

    fun resetAllNudgeFlags(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("guide_nudges_muted", false)
            putLong("guide_last_nudge_shown_ms", 0L)
            putInt("guide_last_nudge_session_count", -10)
            for (k in prefs.all.keys) {
                if (k.startsWith("nudge_shown_")) {
                    remove(k)
                }
            }
            apply()
        }
    }

    fun triggerNudgeById(host: MainActivity, id: String, force: Boolean = true) {
        when (id) {
            "APP_BLOCKER" -> showNudgeCard(
                host, "Want fewer distractions next time?",
                "Turn on App Blocker to block distracting apps while your study timer is running.",
                "Set Up Blocker", "nudge_shown_app_blocker", force
            ) { act -> AppBlockerSettingsSheet.show(act) { act.buildCurrentPanel() } }

            "INSIGHTS_HEATMAP" -> showNudgeCard(
                host, "Your study progress is ready",
                "Open Insights to see your daily study hours, subject pie chart, and study streak grid.",
                "View Insights", "nudge_shown_insights_heatmap", force
            ) { act ->
                act.currentStatsTab = AppStatsTab.OVERVIEW
                act.navigateToPanel(AppPanel.STATS)
            }

            "POMODORO_MODE" -> showNudgeCard(
                host, "Try Pomodoro or Countdown?",
                "Switch between 25/5 Pomodoro rounds, custom countdowns, or stopwatch from the timer mode menu.",
                "Pick Mode", "nudge_shown_pomodoro_mode", force
            ) { act ->
                if (act.currentTimerState == TimerState.IDLE) {
                    TimerModeBottomSheet.show(act) { act.buildCurrentPanel() }
                } else {
                    act.navigateToPanel(AppPanel.FOCUS)
                }
            }

            "CHAPTER_TRACKING" -> showNudgeCard(
                host, "Track time by chapter",
                "Tap the Chapters pill below your subject to log exact hours for each chapter or topic.",
                "Pick Chapter", "nudge_shown_chapter_tracking", force
            ) { act ->
                val activeSub = SubjectTagManager.getSelectedSubject(act)
                SubjectDialogHelper(act).showSubTopicPickerDialog(activeSub) {
                    act.navigateToPanel(AppPanel.FOCUS)
                }
            }

            "LANDSCAPE_CLOCK" -> showNudgeCard(
                host, "Tip: Desk Clock mode",
                "Rotate your phone sideways while studying from a book for a clean, distraction-free desk clock.",
                null, "nudge_shown_landscape_clock", force, null
            )

            "FLIP_TO_FOCUS" -> showNudgeCard(
                host, "Tip: Try Flip-to-Focus",
                "Put your phone face down on the desk to start studying hands-free, and pick it up to pause.",
                "Show Me", "nudge_shown_flip_to_focus", force
            ) { act ->
                pendingHighlightKey = "flip_to_focus"
                act.currentSettingsTab = AppSettingsTab.TIMER
                act.settingsOriginPanel = act.currentPanel
                act.settingsOriginWasSubTab = false
                act.navigateToPanel(AppPanel.SETTINGS)
            }

            "SUBJECT_TAGS" -> showNudgeCard(
                host, "Track subjects separately",
                "Pick a subject for your sessions so your Insights chart shows how much time you spend on each one.",
                "Pick Subject", "nudge_shown_subject_tags", force
            ) { act -> act.showSubjectPickerDialog() }

            "CLOUD_BACKUP" -> showNudgeCard(
                host, "Keep your study history safe",
                "Sign in with Google to back up your study logs and streaks so you never lose them.",
                "Back Up", "nudge_shown_cloud_backup", force
            ) { act ->
                act.currentSettingsTab = AppSettingsTab.CLOUD
                act.settingsOriginPanel = act.currentPanel
                act.settingsOriginWasSubTab = false
                act.navigateToPanel(AppPanel.SETTINGS)
            }

            "HOME_WIDGETS" -> showNudgeCard(
                host, "Add a home screen widget",
                "Long-press your phone's home screen and add StudyTimer widgets to see today's hours and goals at a glance.",
                null, "nudge_shown_home_widgets", force, null
            )

            "PLANNER_SUBJECT" -> maybeShowPlannerSubjectNudge(host, true, force = force)
            "PAUSE_SOUNDS" -> maybeShowPauseSoundNudge(host, force = force)
            "STREAK_FREEZE" -> maybeShowStreakFreezeNudge(host, 3, force = force)
            "EXAM_COUNTDOWN" -> maybeShowExamCountdownNudge(host, force = force) {
                host.currentStatsTab = AppStatsTab.TIMELINE
                host.navigateToPanel(AppPanel.STATS)
            }
            "BLOCKER_PERMISSION" -> maybeShowBlockerPermissionNudge(host, force = force)
            "TIMELINE_EDIT" -> maybeShowTimelineEditNudge(host, 0L, force = force)
            "WEEKLY_SHARE" -> maybeShowWeeklyShareNudge(host, 20_000L, force = force)
            "BREAK_HEALTH" -> maybeShowBreakHealthNudge(host, 5400L, force = force)
        }
    }

    private fun canShowNudge(
        host: MainActivity,
        prefs: android.content.SharedPreferences,
        force: Boolean,
        bypassCooldown: Boolean = false
    ): Boolean {
        if (host.isFinishing || host.isDestroyed || isNudgeShowing) return false
        if (force) return true
        if (prefs.getBoolean("guide_nudges_muted", false)) return false
        if (host.currentTimerState == TimerState.STUDYING) return false
        if (!bypassCooldown) {
            val lastShown = prefs.getLong("guide_last_nudge_shown_ms", 0L)
            if (System.currentTimeMillis() - lastShown < 43_200_000L) return false // 12-hour cooldown
        }
        return true
    }

    private fun showNudgeCard(
        host: MainActivity,
        title: String,
        message: String,
        actionText: String?,
        prefFlag: String? = null,
        force: Boolean = false,
        onAction: ((MainActivity) -> Unit)?
    ) {
        if (isNudgeShowing || host.isFinishing || host.isDestroyed) return
        isNudgeShowing = true

        if (!force && prefFlag != null) {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                .putBoolean(prefFlag, true)
                .putLong("guide_last_nudge_shown_ms", System.currentTimeMillis())
                .apply()
        }

        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener {
            isNudgeShowing = false
        }

        val theme = host.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val onPrimary = ThemeCoordinator.getContrastColor(primary)
        fun dp(v: Int): Int = host.dp(v)

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.createDialogBackground(22f)
            setPadding(dp(18), dp(14), dp(18), dp(16))
        }

        // Top badge + dismiss row
        val badgeRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        badgeRow.addView(TextView(host).apply {
            text = "QUICK TIP"
            textSize = 10.5f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(if (isDark) primary else 0xFF0369A1.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(host.tintedColor(primary, if (isDark) 34 else 22))
            }
            setPadding(dp(9), dp(4), dp(9), dp(4))
        })
        badgeRow.addView(View(host).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        badgeRow.addView(TextView(host).apply {
            text = "✕"
            textSize = 13f
            gravity = Gravity.CENTER
            minWidth = dp(40)
            minHeight = dp(40)
            contentDescription = "Dismiss tip"
            setTextColor(if (isDark) 0xAAFFFFFF.toInt() else 0xFF475569.toInt())
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
            }
        })
        root.addView(badgeRow)

        root.addView(TextView(host).apply {
            text = title
            setTextColor(theme.textColor)
            textSize = 16f
            typeface = theme.getAppTypeface(isBold = true)
        })

        root.addView(TextView(host).apply {
            text = message
            setTextColor(if (isDark) 0xCCFFFFFF.toInt() else 0xFF334155.toInt())
            textSize = 13f
            setLineSpacing(0f, 1.24f)
            setPadding(0, dp(6), 0, dp(16))
        })

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val hasAction = !actionText.isNullOrBlank() && onAction != null
        btnRow.addView(TextView(host).apply {
            text = "Mute tips"
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(44)
            setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(dp(4), dp(8), dp(8), dp(8))
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("guide_nudges_muted", true).apply()
                dialog.dismiss()
                AppToast.show(host, "Tips muted. You can re-enable them in Guide.")
            }
        })
        btnRow.addView(View(host).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        btnRow.addView(TextView(host).apply {
            text = if (hasAction) "Not Now" else "Got It"
            textSize = 13f
            gravity = Gravity.CENTER
            minHeight = dp(44)
            minWidth = dp(48)
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(if (!hasAction) onPrimary else (if (isDark) 0xBBFFFFFF.toInt() else 0xFF475569.toInt()))
            if (!hasAction) {
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(primary)
                }
                setPadding(dp(18), dp(10), dp(18), dp(10))
            } else {
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
            }
        })

        if (hasAction) {
            btnRow.addView(TextView(host).apply {
                text = actionText
                textSize = 13f
                gravity = Gravity.CENTER
                minHeight = dp(44)
                minWidth = dp(48)
                typeface = theme.getAppTypeface(isBold = true)
                setTextColor(onPrimary)
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(primary)
                }
                setPadding(dp(16), dp(10), dp(16), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(6)
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    dialog.dismiss()
                    onAction?.invoke(host)
                }
            })
        }

        root.addView(btnRow)
        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.18f)
            setGravity(Gravity.BOTTOM)
            attributes = attributes?.apply {
                y = dp(76)
            }
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.translationY = dp(22).toFloat()
        root.alpha = 0f
        dialog.show()
        root.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}

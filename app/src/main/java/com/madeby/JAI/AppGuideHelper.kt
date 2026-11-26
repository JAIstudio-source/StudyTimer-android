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
     * Enforces at least a 1-session gap between post-session tips and never repeats once shown.
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

        val title: String
        val message: String
        val actionText: String
        val prefFlag: String
        val onAction: (MainActivity) -> Unit

        when {
            !prefs.getBoolean("nudge_shown_app_blocker", false) && !AppBlockerManager.isEnabled(host) -> {
                prefFlag = "nudge_shown_app_blocker"
                title = "Want fewer distractions next time?"
                message = "Turn on App Blocker to block distracting apps while your study timer is running."
                actionText = "Set Up Blocker"
                onAction = { act ->
                    AppBlockerSettingsSheet.show(act) { act.buildCurrentPanel() }
                }
            }
            count >= 3 && !prefs.getBoolean("nudge_shown_insights_heatmap", false) -> {
                prefFlag = "nudge_shown_insights_heatmap"
                title = "Your study progress is ready"
                message = "Open Insights to see your daily study hours, subject pie chart, and study streak grid."
                actionText = "View Insights"
                onAction = { act ->
                    act.currentStatsTab = AppStatsTab.OVERVIEW
                    act.navigateToPanel(AppPanel.STATS)
                }
            }
            count >= 5 && !prefs.getBoolean("nudge_shown_flip_to_focus", false) && !FlipToFocusManager.isEnabled(host) -> {
                prefFlag = "nudge_shown_flip_to_focus"
                title = "Tip: Try Flip-to-Focus"
                message = "Put your phone face down on the desk to start studying hands-free, and pick it up to pause."
                actionText = "Show Me"
                onAction = { act ->
                    pendingHighlightKey = "flip_to_focus"
                    act.currentSettingsTab = AppSettingsTab.TIMER
                    act.settingsOriginPanel = act.currentPanel
                    act.settingsOriginWasSubTab = false
                    act.navigateToPanel(AppPanel.SETTINGS)
                }
            }
            generalCount >= 3 && !prefs.getBoolean("nudge_shown_subject_tags", false) -> {
                prefFlag = "nudge_shown_subject_tags"
                title = "Track subjects separately"
                message = "Pick a subject for your sessions so your Insights chart shows how much time you spend on each one."
                actionText = "Pick Subject"
                onAction = { act ->
                    act.showSubjectPickerDialog()
                }
            }
            count >= 5 && !AuthManager.isLoggedIn(host) && !prefs.getBoolean("nudge_shown_cloud_backup", false) -> {
                prefFlag = "nudge_shown_cloud_backup"
                title = "Keep your study history safe"
                message = "Sign in with Google to back up your study logs and streaks so you never lose them."
                actionText = "Back Up"
                onAction = { act ->
                    act.currentSettingsTab = AppSettingsTab.CLOUD
                    act.settingsOriginPanel = act.currentPanel
                    act.settingsOriginWasSubTab = false
                    act.navigateToPanel(AppPanel.SETTINGS)
                }
            }
            else -> return
        }

        prefs.edit()
            .putBoolean(prefFlag, true)
            .putInt("guide_last_nudge_session_count", count)
            .apply()

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed) return@postDelayed
            showNudgeCard(host, title, message, actionText, onAction)
        }, 550L)
    }

    fun maybeShowPlannerSubjectNudge(host: MainActivity, hasGoalsWithoutSubjectOrTopic: Boolean) {
        if (!hasGoalsWithoutSubjectOrTopic || host.isFinishing || host.isDestroyed || isNudgeShowing) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("nudge_shown_planner_subject", false)) return
        prefs.edit().putBoolean("nudge_shown_planner_subject", true).apply()

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.PLANNER) return@postDelayed
            showNudgeCard(
                host,
                title = "Link goals to a subject",
                message = "Pick a subject or chapter when adding a goal so your study timer counts toward it automatically.",
                actionText = null,
                onAction = null
            )
        }, 450L)
    }

    fun maybeShowPauseSoundNudge(host: MainActivity) {
        if (host.isFinishing || host.isDestroyed || isNudgeShowing) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("nudge_shown_pause_sounds", false)) return
        prefs.edit().putBoolean("nudge_shown_pause_sounds", true).apply()

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed) return@postDelayed
            showNudgeCard(
                host,
                title = "Add calm study sounds?",
                message = "Play gentle rain, fireplace, or ocean sounds in the background while you study.",
                actionText = "Choose Sound",
                onAction = { act ->
                    AmbientSoundBottomSheet.show(act)
                }
            )
        }, 450L)
    }

    fun maybeShowStreakFreezeNudge(host: MainActivity, streak: Int) {
        if (streak < 3 || host.isFinishing || host.isDestroyed || isNudgeShowing) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("nudge_shown_streak_freeze", false)) return
        prefs.edit().putBoolean("nudge_shown_streak_freeze", true).apply()

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS) return@postDelayed
            showNudgeCard(
                host,
                title = "Protect your $streak-day streak",
                message = "Taking a rest or sick day? Use a Streak Freeze from Today's Focus so your streak stays safe.",
                actionText = "See Freeze",
                onAction = { act ->
                    StatsPanelBuilder(act).showStreakFreezeDialog()
                }
            )
        }, 500L)
    }

    fun maybeShowExamCountdownNudge(host: MainActivity, onAddExam: () -> Unit) {
        if (host.isFinishing || host.isDestroyed || isNudgeShowing) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("nudge_shown_exam_countdown", false)) return
        prefs.edit().putBoolean("nudge_shown_exam_countdown", true).apply()

        host.window.decorView.postDelayed({
            if (host.isFinishing || host.isDestroyed || host.currentPanel != AppPanel.STATS) return@postDelayed
            showNudgeCard(
                host,
                title = "Have an upcoming exam?",
                message = "Add your exam date here to see a live countdown right above your study history.",
                actionText = "Add Exam",
                onAction = { _ -> onAddExam() }
            )
        }, 450L)
    }

    private fun showNudgeCard(
        host: MainActivity,
        title: String,
        message: String,
        actionText: String?,
        onAction: ((MainActivity) -> Unit)?
    ) {
        if (isNudgeShowing || host.isFinishing || host.isDestroyed) return
        isNudgeShowing = true

        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            isNudgeShowing = false
        }

        val theme = host.themeCoordinator
        val primary = theme.primaryColor
        val onPrimary = ThemeCoordinator.getContrastColor(primary)
        fun dp(v: Int): Int = host.dp(v)

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.createDialogBackground(22f)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }

        root.addView(TextView(host).apply {
            text = title
            setTextColor(theme.textColor)
            textSize = 16.5f
            typeface = theme.getAppTypeface(isBold = true)
        })

        root.addView(TextView(host).apply {
            text = message
            setTextColor(theme.textColor)
            alpha = 0.78f
            textSize = 13f
            setLineSpacing(0f, 1.2f)
            setPadding(0, dp(8), 0, dp(16))
        })

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val hasAction = !actionText.isNullOrBlank() && onAction != null
        btnRow.addView(TextView(host).apply {
            text = if (hasAction) "Not Now" else "Got It"
            textSize = 13f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(if (!hasAction) onPrimary else theme.textColor)
            alpha = if (!hasAction) 1f else 0.65f
            if (!hasAction) {
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(primary)
                }
                setPadding(dp(16), dp(9), dp(16), dp(9))
            } else {
                setPadding(dp(14), dp(9), dp(14), dp(9))
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
                typeface = theme.getAppTypeface(isBold = true)
                setTextColor(onPrimary)
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(primary)
                }
                setPadding(dp(16), dp(9), dp(16), dp(9))
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
            setGravity(Gravity.BOTTOM)
            attributes = attributes?.apply {
                y = dp(24)
            }
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }
}

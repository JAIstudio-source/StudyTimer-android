package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusPanelBuilder(private val host: MainActivity) {

    fun build() {
        with(host) {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val isLandscape = isLandscapeMode()

            if (isLandscape) {
                buildLandscapeLayout(sharedPrefs)
            } else {
                buildPortraitLayout(sharedPrefs)
            }
        }
    }

    /**
     * Dedicated Portrait Focus layout.
     * Preserves the full Universal Header, interactive session capsule, spacious Pomodoro ring,
     * and portrait 3-button action controls.
     */
    private fun buildPortraitLayout(sharedPrefs: SharedPreferences) {
        with(host) {
            landscapeHeaderView = null
            landscapeDockView = null
            landscapeSubjectPillView = null
            landscapeGoalProgressBar = null
            landscapeTodayFocusText = null
            landscapeGoalPercentText = null
            panelContainer.setOnClickListener(null)
            if (!isPortraitFullscreenActive) rootLayout.setOnClickListener(null)

            val isPomoWhite = isPomodoroPureWhiteActive()
            val isWallpaperActive = WallpaperManager.getWallpaperType(this) != "NONE"
            val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(this)
            val isBrightSurface = isPomoWhite || (isWallpaperActive && isBrightUnderTimer) || !themeCoordinator.isDarkMode()

            // 1. Universal Top Header (Profile + Streak on left, Sound button + Settings gear on right)
            val isAmbientEnabled = StudyAudioManager.isAmbientEnabled(this)
            val soundButton: View? = if (isAmbientEnabled) {
                AppCompatImageView(this).apply {
                    setImageDrawable(ContextCompat.getDrawable(this@with, R.drawable.ic_music_note))
                    val iconBg = if (isPomoWhite) {
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(0xFFF1F5F9.toInt())
                        }
                    } else {
                        themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, if (themeCoordinator.isDarkMode()) 65 else 90))
                    }
                    background = iconBg
                    setColorFilter(if (isPomoWhite) 0xFF0F172A.toInt() else themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    contentDescription = "Background focus sounds"
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        AmbientSoundBottomSheet.show(host) {
                            host.buildCurrentPanel()
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                }
            } else null

            navHeader = UniversalHeaderHelper.createHeader(this, extraRightView = soundButton).apply {
                visibility = View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(2), 0, dp(4))
                }
            }

            // 2. Active Subject & Task Header (Unified Session Capsule)
            val timerModeSetting = sharedPrefs.getString("timer_mode", "COUNTDOWN") ?: "COUNTDOWN"
            val showSubjectTagging = sharedPrefs.getBoolean("enable_subject_tagging", true)

            val controlsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                visibility = View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(2), 0, dp(4))
                }
            }
            extraControlsContainer = controlsContainer

            val isLectureRunning = timerModeSetting == "LECTURE" &&
                (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.PAUSED) &&
                sharedPrefs.getBoolean("lecture_mode_enabled", false)
            val lectureSubId = sharedPrefs.getString("active_lecture_subject_id", null)
            val activeSubject = if (isLectureRunning && lectureSubId != null) {
                SubjectTagManager.resolveSubject(this, lectureSubId)
            } else {
                SubjectTagManager.getSelectedSubject(this)
            }

            val subjectColor = try {
                Color.parseColor(activeSubject.colorHex)
            } catch (_: Exception) {
                themeCoordinator.primaryColor
            }

            val modeLabel = when (timerModeSetting) {
                "STOPWATCH" -> "Stopwatch"
                "SUBJECT" -> "Subject Wise"
                "COUNTDOWN" -> {
                    val pStudy = sharedPrefs.safeLong("study_interval_minutes", 25L).toInt()
                    val pBreak = sharedPrefs.safeLong("break_interval_minutes", 5L).toInt()
                    "$pStudy/$pBreak Pomodoro"
                }
                "LECTURE" -> "Lecture"
                else -> "Focus"
            }

            val activeSubTopic = SubjectTagManager.getSelectedSubTopic(this, activeSubject.id)
            val activeGoalTitle = sharedPrefs.getString("active_planner_goal_title", null)

            // INTERACTIVE GLASS CAPSULE ROW (DISTINCT PILL BUTTONS)
            val sessionScrollWrapper = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isFillViewport = false
                clipToPadding = false
                setPadding(dp(4), dp(2), dp(4), dp(2))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(2)
                    bottomMargin = dp(4)
                }
            }

            val sessionPillsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // 1. Subject Pill Button
            if (showSubjectTagging) {
                val subjectPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Selected study subject: ${activeSubject.name}. Tap to change subject."

                    val pillBg = if (isBrightSurface) {
                        GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(0xF2FFFFFF.toInt())
                            setStroke(dp(1.5f), tintedColor(subjectColor, 180))
                        }
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(if (themeCoordinator.isDarkMode()) 0x331E293B.toInt() else 0x25FFFFFF.toInt())
                            setStroke(dp(1.5f), tintedColor(subjectColor, 220))
                        }
                    }
                    background = pillBg
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(38)
                    ).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (isLectureRunning) {
                            AppToast.show(host, "Subject tag is locked to the scheduled class")
                        } else {
                            host.showSubjectPickerDialog()
                        }
                    }
                }

                val dotContainer = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                        rightMargin = dp(6)
                    }
                }
                val outerRing = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(tintedColor(subjectColor, 60))
                    }
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                val innerDot = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(subjectColor)
                    }
                    val dotSize = dp(7)
                    layoutParams = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER)
                }
                dotContainer.addView(outerRing)
                dotContainer.addView(innerDot)
                subjectPill.addView(dotContainer)

                val subjectText = TextView(this).apply {
                    text = activeSubject.name
                    textSize = 12.5f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    val isStudying = host.currentTimerState == TimerState.STUDYING
                    setTextColor(
                        if (isBrightSurface) 0xFF0F172A.toInt()
                        else if (isStudying) Color.parseColor("#10B981")
                        else 0xFFFFFFFF.toInt()
                    )
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(110)
                }
                subjectPill.addView(subjectText)

                val dropdownChevron = TextView(this).apply {
                    text = " ▾"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isBrightSurface) 0xFF475569.toInt() else subjectColor)
                }
                subjectPill.addView(dropdownChevron)
                sessionPillsRow.addView(subjectPill)
            }

            // 2. Middle Section: Active Planner Goal or Sub-topic Pill
            if (!activeGoalTitle.isNullOrBlank()) {
                val goalPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    val pillBg = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(if (isBrightSurface) 0xF2FFFFFF.toInt() else 0x25FFFFFF.toInt())
                        setStroke(dp(1), if (isBrightSurface) 0xFFCBD5E1.toInt() else tintedColor(themeCoordinator.accentColor, 180))
                    }
                    background = pillBg
                    setPadding(dp(10), dp(6), dp(8), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(38)
                    ).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        navigateToPanel(AppPanel.PLANNER)
                    }
                }
                val goalIcon = ImageView(this).apply {
                    setImageResource(R.drawable.ic_target)
                    setColorFilter(themeCoordinator.accentColor)
                    layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                        setMargins(0, 0, dp(4), 0)
                    }
                }
                goalPill.addView(goalIcon)

                val goalText = TextView(this).apply {
                    text = activeGoalTitle
                    textSize = 11.5f
                    typeface = themeCoordinator.getAppTypeface(isBold = false)
                    setTextColor(if (isBrightSurface) 0xFF0F172A.toInt() else themeCoordinator.accentColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(85)
                }
                goalPill.addView(goalText)

                val clearGoalBtn = TextView(this).apply {
                    text = " ✕"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isBrightSurface) 0xFF64748B.toInt() else tintedColor(themeCoordinator.textColor, 140))
                    setPadding(dp(4), 0, dp(4), 0)
                    setOnClickListener {
                        if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                            AppToast.show(host, "Pause or stop timer to remove active task")
                            return@setOnClickListener
                        }
                        host.clearActivePlannerGoal()
                    }
                }
                goalPill.addView(clearGoalBtn)
                sessionPillsRow.addView(goalPill)
            } else if (showSubjectTagging) {
                val hasSubTopic = !activeSubTopic.isNullOrBlank()
                val topicLabel = if (hasSubTopic) activeSubTopic else "Chapters"
                val topicPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Select chapter / sub-topic. Current: $topicLabel"
                    val pillBg = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(if (isBrightSurface) 0xF2FFFFFF.toInt() else 0x22FFFFFF.toInt())
                        setStroke(dp(1), if (hasSubTopic) tintedColor(themeCoordinator.primaryColor, 180) else (if (isBrightSurface) 0xFFCBD5E1.toInt() else tintedColor(themeCoordinator.textColor, 45)))
                    }
                    background = pillBg
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(38)
                    ).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        SubjectDialogHelper(host).showSubTopicPickerDialog(activeSubject) {
                            host.navigateToPanel(AppPanel.FOCUS)
                        }
                    }
                }

                val topicText = TextView(this).apply {
                    text = topicLabel
                    textSize = 12f
                    typeface = themeCoordinator.getAppTypeface(isBold = hasSubTopic)
                    setTextColor(if (isBrightSurface) (if (hasSubTopic) themeCoordinator.primaryColor else 0xFF0F172A.toInt()) else (if (hasSubTopic) themeCoordinator.primaryColor else 0xFFFFFFFF.toInt()))
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(85)
                }
                topicPill.addView(topicText)

                val topicChevron = TextView(this).apply {
                    text = " ▾"
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (hasSubTopic) themeCoordinator.primaryColor else (if (isBrightSurface) 0xFF64748B.toInt() else tintedColor(themeCoordinator.textColor, 130)))
                }
                topicPill.addView(topicChevron)
                sessionPillsRow.addView(topicPill)
            }

            // 3. Mode Pill Button (Only visible when no timer is running)
            val isTimerIdle = (currentTimerState == TimerState.IDLE)
            val modePill = LinearLayout(this).apply {
                tag = "mode_pill"
                visibility = if (isTimerIdle) View.VISIBLE else View.GONE
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                contentDescription = "Timer Mode: $modeLabel. Tap to switch mode."
                val pillBg = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isBrightSurface) 0xF2FFFFFF.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0xFFCBD5E1.toInt() else tintedColor(themeCoordinator.primaryColor, 130))
                }
                background = pillBg
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(38)
                )
                setOnClickListener {
                    if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK || currentTimerState == TimerState.PAUSED) {
                        AppToast.show(this@FocusPanelBuilder.host, "Stop or finish current session to switch timer mode")
                        return@setOnClickListener
                    }
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    TimerModeBottomSheet.show(host) {
                        host.buildCurrentPanel()
                    }
                }
            }

            val modeText = TextView(this).apply {
                text = "$modeLabel ▾"
                textSize = 12f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(if (isBrightSurface) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt())
            }
            modePill.addView(modeText)
            sessionPillsRow.addView(modePill)

            sessionScrollWrapper.addView(sessionPillsRow)
            host.modeOptionsPill = sessionScrollWrapper
            controlsContainer.addView(sessionScrollWrapper)

            // 3. Center Clocks & Ring Wrapper
            val centerClocksWrapper = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            val savedTimerMode = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
            val isPomodoro = (savedTimerMode == "COUNTDOWN")
            timerRing = TimerRingView(this).apply {
                visibility = if (isPomodoro) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
            }

            statusBadge = TextView(this).apply {
                textSize = 14f
                letterSpacing = 0.04f
                setPadding(dp(18), dp(6), dp(18), dp(6))
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                gravity = Gravity.CENTER
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 18f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            statusBadgeContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                    setMargins(0, dp(6), 0, 0)
                }
                addView(statusBadge)

                if (savedTimerMode == "LECTURE") {
                    val timetableBtn = TextView(host).apply {
                        text = "Class Timetable"
                        textSize = 13f
                        typeface = themeCoordinator.getAppTypeface(isBold = true)
                        setTextColor(themeCoordinator.accentColor)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, 120), 20f)
                        setPadding(dp(16), dp(6), dp(16), dp(6))
                        gravity = Gravity.CENTER
                        setOnClickListener { showLectureScheduleManagerDialog() }
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dp(14), 0, 0)
                        }
                    }
                    addView(timetableBtn)
                }
            }

            val fontId = FontStyleManager.getSelectedFontId(this)
            val selectedTypeface = if (fontId == "sans_default" || fontId == "tabular_clock") {
                FontStyleManager.getTabularClockTypeface(this)
            } else {
                FontStyleManager.getTypeface(this)
            }

            studyTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = if (isPomodoro) 66f else 78f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 5)
                if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled() && !isPomoWhite) {
                    setShadowLayer(16f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }

            breakTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = 22f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(28))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }

            val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    host.dismissGestureHint()
                    togglePortraitFullscreenMode()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    host.dismissGestureHint()
                    if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                        handlePause()
                        try { centerClocksWrapper.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                        return true
                    } else if (currentTimerState == TimerState.PAUSED) {
                        handleStateToggle()
                        try { centerClocksWrapper.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                        return true
                    }
                    return false
                }
            })
            centerClocksWrapper.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }

            centerClocksWrapper.addView(timerRing)
            centerClocksWrapper.addView(statusBadgeContainer)
            centerClocksWrapper.addView(studyTimerDisplay)
            centerClocksWrapper.addView(breakTimerDisplay)

            val isGestureHintDismissed = sharedPrefs.getBoolean("gesture_hint_dismissed", false)
            val gestureHint = TextView(this).apply {
                text = "Double-tap to pause • Tap for fullscreen"
                textSize = 11.5f
                letterSpacing = 0.04f
                setTextColor(if (isBrightSurface) 0xFF475569.toInt() else tintedColor(themeCoordinator.textColor, if (themeCoordinator.isDarkMode()) 95 else 115))
                gravity = Gravity.CENTER
                visibility = if (isGestureHintDismissed || currentTimerState != TimerState.IDLE) View.GONE else View.VISIBLE
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply {
                    bottomMargin = dp(8)
                }
            }
            host.gestureHintView = gestureHint
            centerClocksWrapper.addView(gestureHint)

            // 4. Primary Action Controls
            controlActionContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(8), 0, dp(78))
                }
            }

            mainBtn = Button(this).apply {
                textSize = 14.5f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                minimumHeight = dp(48)
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener { handleStateToggle() }
                applyBubbleTouchAnimation(this)
            }

            pauseBtn = Button(this).apply {
                text = "Pause"
                textSize = 14f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                minimumHeight = dp(48)
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener { handlePause() }
                applyBubbleTouchAnimation(this)
            }

            stopBtn = HoldRingButton(this).apply {
                text = "Finish"
                ringColor = themeCoordinator.primaryColor
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                isSoundEffectsEnabled = false
                background = outlinedButtonBackground()
                setPadding(dp(12), dp(10), dp(12), dp(10))
                minimumHeight = dp(48)
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            }
            attachHoldToEndListener(stopBtn)

            controlActionContainer.addView(pauseBtn)
            controlActionContainer.addView(mainBtn)
            controlActionContainer.addView(stopBtn)

            panelContainer.addView(navHeader)
            panelContainer.addView(extraControlsContainer)
            panelContainer.addView(centerClocksWrapper)
            panelContainer.addView(controlActionContainer)

            updateVisualStyles()
            applyTrueFullscreenMode()
        }
    }

    /**
     * Dedicated Landscape Focus layout.
     * Pure, clean, cardless interface:
     * - Top: Exactly 3 buttons (Portrait, Subject, Settings)
     * - Center: Desk clock (ring + digits + quote)
     * - Bottom: Exactly 3 buttons (Resume/Pause, Break, Finish)
     * - Zero cards, zero bottom nav bar.
     */
    private fun buildLandscapeLayout(sharedPrefs: SharedPreferences) {
        with(host) {
            val isPomoWhite = isPomodoroPureWhiteActive()

            // Safe stubs for unused portrait/dock views
            navHeader = View(this).apply { visibility = View.GONE }
            statusBadgeContainer = LinearLayout(this).apply { visibility = View.GONE }
            statusBadge = TextView(this).apply { visibility = View.GONE }
            extraControlsContainer = null
            modeOptionsPill = null
            gestureHintView = null
            landscapeDockView = null
            landscapeGoalProgressBar = null
            landscapeTodayFocusText = null
            landscapeGoalPercentText = null

            val timerModeSetting = sharedPrefs.getString("timer_mode", "COUNTDOWN") ?: "COUNTDOWN"
            val showSubjectTagging = sharedPrefs.getBoolean("enable_subject_tagging", true)
            val isPomodoro = (timerModeSetting == "COUNTDOWN")

            val isLectureRunning = timerModeSetting == "LECTURE" &&
                (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.PAUSED) &&
                sharedPrefs.getBoolean("lecture_mode_enabled", false)
            val lectureSubId = sharedPrefs.getString("active_lecture_subject_id", null)
            val activeSubject = if (isLectureRunning && lectureSubId != null) {
                SubjectTagManager.resolveSubject(this, lectureSubId)
            } else {
                SubjectTagManager.getSelectedSubject(this)
            }

            val subjectColor = try {
                Color.parseColor(activeSubject.colorHex)
            } catch (_: Exception) {
                themeCoordinator.primaryColor
            }

            val isWallpaperActive = WallpaperManager.getWallpaperType(this) != "NONE"
            val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(this)
            val isBrightSurface = isPomoWhite || (isWallpaperActive && isBrightUnderTimer) || !themeCoordinator.isDarkMode()

            val textColor = if (isBrightSurface) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
            val subTextColor = if (isBrightSurface) 0xFF64748B.toInt() else 0x99FFFFFF.toInt()

            panelContainer.removeAllViews()
            panelContainer.orientation = LinearLayout.VERTICAL
            panelContainer.setPadding(0, 0, 0, 0)

            // ==========================================
            // 1. TOP BAR: EXACTLY 3 BUTTONS (NO CARD)
            // ==========================================
            val topHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply {
                    setMargins(dp(20), dp(8), dp(20), dp(4))
                }
            }
            landscapeHeaderView = topHeader

            // Button 1 (Left): Exit to Portrait
            val exitPortraitBtn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                }
                setPadding(dp(14), dp(7), dp(16), dp(7))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    handler.postDelayed({
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }, 1200L)
                }

                val icon = TextView(host).apply {
                    text = "⤺"
                    textSize = 14f
                    setTextColor(textColor)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, dp(6), 0)
                }
                val label = TextView(host).apply {
                    text = "Portrait"
                    textSize = 12f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(textColor)
                }
                addView(icon)
                addView(label)
                applyBubbleTouchAnimation(this)
            }
            topHeader.addView(exitPortraitBtn)

            // Spacer 1
            val leftSpacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            topHeader.addView(leftSpacer)

            // Button 2 (Center): Active Subject Pill
            val subjectPill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x25000000.toInt() else tintedColor(themeCoordinator.primaryColor, 120))
                }
                setPadding(dp(16), dp(7), dp(16), dp(7))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (currentTimerState == TimerState.IDLE) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        showSubjectPickerDialog()
                    }
                }
                applyBubbleTouchAnimation(this)

                if (showSubjectTagging) {
                    val dot = View(host).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(subjectColor)
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                            marginEnd = dp(8)
                        }
                    }
                    addView(dot)
                }

                val subjName = TextView(host).apply {
                    text = "${activeSubject.name} ▾"
                    textSize = 12f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(textColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(220)
                }
                addView(subjName)
            }
            landscapeSubjectPillView = subjectPill
            topHeader.addView(subjectPill)

            // Spacer 2
            val rightSpacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            topHeader.addView(rightSpacer)

            // Button 3 (Right): Settings
            val settingsBtn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                }
                setPadding(dp(14), dp(7), dp(14), dp(7))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navigateToPanel(AppPanel.SETTINGS)
                }

                val gearIcon = TextView(host).apply {
                    text = "⚙️"
                    textSize = 13.5f
                    setPadding(0, 0, dp(4), 0)
                }
                val gearLabel = TextView(host).apply {
                    text = "Settings"
                    textSize = 12f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(textColor)
                }
                addView(gearIcon)
                addView(gearLabel)
                applyBubbleTouchAnimation(this)
            }
            topHeader.addView(settingsBtn)

            panelContainer.addView(topHeader)

            // ==========================================
            // 2. CENTER CLOCK AREA (DESK CLOCK EXPERIENCE)
            // ==========================================
            val centerArea = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    setMargins(dp(20), dp(4), dp(20), dp(4))
                }
            }

            timerRing = TimerRingView(this).apply {
                visibility = if (isPomodoro) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(dp(210), dp(210), Gravity.CENTER)
            }

            val fontId = FontStyleManager.getSelectedFontId(this)
            val selectedTypeface = if (fontId == "sans_default" || fontId == "tabular_clock") {
                FontStyleManager.getTabularClockTypeface(this)
            } else {
                FontStyleManager.getTypeface(this)
            }

            studyTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = if (isPomodoro) 48f else 72f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(2))
                if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled() && !isPomoWhite) {
                    setShadowLayer(16f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }

            breakTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = 16f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
                visibility = View.GONE
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
                    topMargin = dp(46)
                }
            }

            val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    toggleLandscapeControls()
                    return true
                }
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                        handlePause()
                        try { centerArea.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                        return true
                    } else if (currentTimerState == TimerState.PAUSED) {
                        handleStateToggle()
                        try { centerArea.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                        return true
                    }
                    return false
                }
            })
            centerArea.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }

            centerArea.addView(timerRing)
            centerArea.addView(studyTimerDisplay)
            centerArea.addView(breakTimerDisplay)

            // Quote at bottom of center area
            val quoteView = TextView(this).apply {
                val quote = DailyQuotes.getTodayQuote()
                text = quote
                textSize = 10.5f
                typeface = themeCoordinator.getAppTypeface(isBold = false)
                setTextColor(subTextColor)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_HORIZONTAL
                maxWidth = dp(420)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply {
                    bottomMargin = dp(4)
                }
            }
            centerArea.addView(quoteView)

            panelContainer.addView(centerArea)

            // ==========================================
            // 3. BOTTOM BAR: EXACTLY 3 BUTTONS (NO CARD)
            // ==========================================
            controlActionContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = null // PURE FLOATING BUTTONS - NO CARD!
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(16)
                    topMargin = dp(4)
                }
            }

            val isIdle = (currentTimerState == TimerState.IDLE)
            val btnHeight = dp(46)
            val btnWidth = dp(132)

            pauseBtn = Button(this).apply {
                textSize = 13.5f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                minimumHeight = btnHeight
                minWidth = btnWidth
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(btnWidth, btnHeight).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    if (currentTimerState == TimerState.IDLE || currentTimerState == TimerState.PAUSED || currentTimerState == TimerState.BREAK) {
                        handleStateToggle()
                    } else {
                        handlePause()
                    }
                }
                applyBubbleTouchAnimation(this)
            }

            mainBtn = Button(this).apply {
                text = "Break"
                textSize = 13f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                minimumHeight = btnHeight
                minWidth = btnWidth
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(btnWidth, btnHeight).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    if (currentTimerState == TimerState.STUDYING) {
                        handleStateToggle()
                    } else if (currentTimerState == TimerState.BREAK) {
                        handlePause()
                    } else if (currentTimerState == TimerState.PAUSED) {
                        showBreakDurationDialog()
                    }
                }
                applyBubbleTouchAnimation(this)
            }

            stopBtn = HoldRingButton(this).apply {
                text = "Finish"
                ringColor = themeCoordinator.primaryColor
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                isSoundEffectsEnabled = false
                background = outlinedButtonBackground()
                setPadding(dp(12), dp(6), dp(12), dp(6))
                minimumHeight = btnHeight
                minWidth = btnWidth
                layoutParams = LinearLayout.LayoutParams(btnWidth, btnHeight)
            }
            attachHoldToEndListener(stopBtn)

            controlActionContainer.addView(pauseBtn)
            controlActionContainer.addView(mainBtn)
            controlActionContainer.addView(stopBtn)
            panelContainer.addView(controlActionContainer)

            updateVisualStyles()
            applyTrueFullscreenMode()
        }
    }

    private fun attachHoldToEndListener(button: HoldRingButton) {
        with(host) {
            button.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        holdStartTime = SystemClock.uptimeMillis()
                        isHoldingStop = true
                        v.isPressed = true
                        button.progress = 0f
                        handler.removeCallbacks(holdToEndRunnable)
                        handler.post(holdToEndRunnable)
                        performMicroHapticTick(v, 0f)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val elapsed = SystemClock.uptimeMillis() - holdStartTime
                        if (elapsed < 500L) {
                            AppToast.show(host, "Hold to finish session")
                            try { v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                            v.animate().translationX(-8f).setDuration(40).withEndAction {
                                v.animate().translationX(8f).setDuration(40).withEndAction {
                                    v.animate().translationX(0f).setDuration(40).start()
                                }.start()
                            }.start()
                        }
                        resetHoldToEnd()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        resetHoldToEnd()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.x < 0f || event.y < 0f || event.x > v.width || event.y > v.height) {
                            resetHoldToEnd()
                        }
                        true
                    }
                    else -> true
                }
            }
        }
    }
}

package com.madeby.JAI

import android.content.Context
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FocusPanelBuilder(private val host: MainActivity) {

    fun build() {
        with(host) {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
            val isLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled
            val isPomoWhite = isPomodoroPureWhiteActive()

            // 1. Universal Top Header (Profile + Streak on left, Settings gear on right)
            navHeader = UniversalHeaderHelper.createHeader(this).apply {
                visibility = if (isLandscape) View.GONE else View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, if (isLandscape) dp(4) else dp(2), 0, if (isLandscape) dp(4) else dp(6))
                }
            }

            // 2. Active Subject & Task Header (Consolidated, Minimal)
            val timerModeSetting = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
            val showSubjectTagging = sharedPrefs.getBoolean("enable_subject_tagging", true) && timerModeSetting != "STOPWATCH"

            val controlsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                visibility = if (isLandscape) View.GONE else View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(4), 0, dp(6))
                }
            }
            extraControlsContainer = controlsContainer

            val subjectRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            if (showSubjectTagging) {
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

                val subjectTagBtn = TextView(this).apply {
                    text = if (isLectureRunning) "🔒 ${activeSubject.iconEmoji} ${activeSubject.name}" else "${activeSubject.iconEmoji} ${activeSubject.name}  ▾"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    if (!themeCoordinator.isDarkMode() || isPomoWhite) {
                        val softBg = Color.argb(32, Color.red(subjectColor), Color.green(subjectColor), Color.blue(subjectColor))
                        val strokeCol = Color.argb(160, Color.red(subjectColor), Color.green(subjectColor), Color.blue(subjectColor))
                        val textColorDark = run {
                            val hsv = FloatArray(3)
                            Color.colorToHSV(subjectColor, hsv)
                            hsv[1] = (hsv[1] * 1.15f).coerceAtMost(1f)
                            hsv[2] = (hsv[2] * 0.72f).coerceAtLeast(0.2f)
                            Color.HSVToColor(hsv)
                        }
                        setTextColor(textColorDark)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(18).toFloat()
                            setColor(softBg)
                            setStroke(dp(1), strokeCol)
                        }
                    } else {
                        setTextColor(themeCoordinator.textColor)
                        background = themeCoordinator.createGlassChip(tintedColor(subjectColor, 90), 18f)
                    }
                    setPadding(dp(18), dp(7), dp(18), dp(7))
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        if (isLectureRunning) {
                            AppToast.show(host, "Subject tag is locked to the scheduled class")
                        } else {
                            host.showSubjectPickerDialog()
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                subjectRow.addView(subjectTagBtn)
            }
            controlsContainer.addView(subjectRow)

            // Subtitle: Active Task/Goal (if armed from Planner)
            val activeGoalTitle = sharedPrefs.getString("active_planner_goal_title", null)
            if (!activeGoalTitle.isNullOrBlank()) {
                val taskSubtitleRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = if (isPomoWhite) {
                        GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, if (themeCoordinator.isDarkMode()) 60 else 120), 14f)
                    }
                    setPadding(dp(12), dp(4), dp(8), dp(4))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(6), 0, 0)
                    }
                }

                val taskText = TextView(this).apply {
                    val displayTitle = if (activeGoalTitle.length > 26) activeGoalTitle.take(24) + "…" else activeGoalTitle
                    text = "🎯 $displayTitle"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else themeCoordinator.textColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setOnClickListener {
                        navigateToPanel(AppPanel.PLANNER)
                    }
                }
                taskSubtitleRow.addView(taskText)

                val clearGoalBtn = TextView(this).apply {
                    text = "  ✕"
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isPomoWhite) 0xFF64748B.toInt() else tintedColor(themeCoordinator.textColor, 140))
                    setPadding(dp(4), 0, dp(4), 0)
                    setOnClickListener {
                        if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                            AppToast.show(host, "Pause or stop timer to remove active task")
                            return@setOnClickListener
                        }
                        host.clearActivePlannerGoal()
                    }
                }
                taskSubtitleRow.addView(clearGoalBtn)
                controlsContainer.addView(taskSubtitleRow)
            }

            // 3. Center Clocks & Ring Wrapper
            val centerClocksWrapper = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled() && !isPomoWhite) {
                val glowAlpha = when {
                    themeCoordinator.activeBgMode == "OLED" -> 0.22f
                    themeCoordinator.activeBgMode == "LIGHT" -> 0.3f
                    else -> 0.38f
                }
                val timerGlow = ImageView(this).apply {
                    setImageDrawable(themeCoordinator.createGlowBlob(themeCoordinator.primaryColor))
                    alpha = glowAlpha
                    layoutParams = FrameLayout.LayoutParams(if (isLandscape) dp(420) else dp(360), if (isLandscape) dp(420) else dp(360), Gravity.CENTER)
                }
                centerClocksWrapper.addView(timerGlow)
            }

            val savedTimerMode = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
            timerRing = TimerRingView(this).apply {
                visibility = if (!isLandscape && (savedTimerMode == "COUNTDOWN" || savedTimerMode == "LECTURE")) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
            }

            statusBadge = TextView(this).apply {
                textSize = if (isLandscape) 12f else 13.5f
                setPadding(dp(16), dp(4), dp(16), dp(4))
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            statusBadgeContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                    setMargins(0, if (isLandscape) dp(4) else dp(6), 0, 0)
                }
                addView(statusBadge)

                if (savedTimerMode == "LECTURE") {
                    val timetableBtn = TextView(host).apply {
                        text = "📅 Class Timetable"
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
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

            studyTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = if (isLandscape) 104f else 54f
                typeface = Typeface.MONOSPACE
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 5)
                if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled() && !isPomoWhite) {
                    setShadowLayer(14f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }

            breakTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = if (isLandscape) 20f else 20f
                typeface = Typeface.MONOSPACE
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, if (isLandscape) 8 else 40)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }

            val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (isLandscape) {
                        toggleLandscapeControls()
                        return true
                    } else {
                        togglePortraitFullscreenMode()
                        return true
                    }
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
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

            // 4. Primary Action Controls
            controlActionContainer = LinearLayout(this).apply {
                orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (isLandscape) gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, if (isLandscape) dp(4) else dp(8), 0, if (isLandscape) dp(12) else dp(84))
                }
            }

            mainBtn = Button(this).apply {
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(20), dp(11), dp(20), dp(11))
                minimumHeight = dp(46)
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(140) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4))
                }
                setOnClickListener { handleStateToggle() }
                applyBubbleTouchAnimation(this)
            }

            pauseBtn = Button(this).apply {
                text = getString(R.string.btn_pause)
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(20), dp(11), dp(20), dp(11))
                minimumHeight = dp(46)
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(130) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4))
                }
                setOnClickListener { handlePause() }
                applyBubbleTouchAnimation(this)
            }

            stopBtn = HoldRingButton(this).apply {
                text = getString(R.string.btn_hold_to_end)
                ringColor = themeCoordinator.primaryColor
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                isSoundEffectsEnabled = false
                background = outlinedButtonBackground()
                setPadding(dp(20), dp(11), dp(20), dp(11))
                minimumHeight = dp(46)
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(140) else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(28), dp(4), dp(28), dp(4))
                }
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            holdStartTime = SystemClock.uptimeMillis()
                            isHoldingStop = true
                            v.isPressed = true
                            progress = 0f
                            handler.removeCallbacks(holdToEndRunnable)
                            handler.post(holdToEndRunnable)
                            performMicroHapticTick(v, 0f)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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

            controlActionContainer.addView(mainBtn)
            controlActionContainer.addView(pauseBtn)
            controlActionContainer.addView(stopBtn)

            // 2. Timer Mode Change Menu (At Top)
            val modeLabel = when (savedTimerMode) {
                "STOPWATCH" -> "⏱️ Stopwatch ▾"
                "SUBJECT" -> "📘 Subject Wise ▾"
                "COUNTDOWN" -> {
                    val pStudy = sharedPrefs.getInt("pomodoro_study_minutes", 25)
                    val pBreak = sharedPrefs.getInt("pomodoro_break_minutes", 5)
                    "🍅 $pStudy/$pBreak Pomodoro ▾"
                }
                "LECTURE" -> "🏛️ Lecture ▾"
                else -> "⏱️ Focus Mode ▾"
            }

            val modeOptionsPill = TextView(this).apply {
                text = modeLabel
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isPomoWhite) 0xFF475569.toInt() else tintedColor(themeCoordinator.textColor, 190))
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(6), dp(16), dp(6))
                background = if (isPomoWhite) {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 14f)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, dp(2), 0, dp(4))
                }
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
            host.modeOptionsPill = modeOptionsPill

            // Assemble into panelContainer in exact order:
            // 1. Universal Header (Top)
            // 2. Timer Mode Change Menu (Top)
            // 3. Center Clocks (Middle)
            // 4. Subject Menu & Active Task (Bottom, above start timer)
            // 5. Control Action Container (Start/Pause/Hold to end buttons)
            panelContainer.addView(navHeader)
            if (!isLandscape) {
                panelContainer.addView(modeOptionsPill)
            }
            panelContainer.addView(centerClocksWrapper)
            if (!isLandscape && showSubjectTagging) {
                panelContainer.addView(extraControlsContainer)
            }
            panelContainer.addView(controlActionContainer)

            updateVisualStyles()
            applyTrueFullscreenMode()
        }
    }
}

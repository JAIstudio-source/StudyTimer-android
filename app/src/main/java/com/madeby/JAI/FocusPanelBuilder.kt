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
            val isLandscapeEnabled = sharedPrefs.getBoolean(
                "is_landscape_mode_enabled",
                sharedPrefs.getBoolean("true_fullscreen_landscape", true)
            )
            val isLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled

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
     * Clean, horizontal desk-clock interface without cluttered headers,
     * removing extra buttons (e.g. no Break button) and providing a prominent,
     * perfectly proportioned Pomodoro ring.
     */
    private fun buildLandscapeLayout(sharedPrefs: SharedPreferences) {
        with(host) {
            val isPomoWhite = isPomodoroPureWhiteActive()

            // Safe stub for navHeader (never added to landscape panelContainer)
            navHeader = View(this).apply { visibility = View.GONE }
            extraControlsContainer = null
            modeOptionsPill = null
            gestureHintView = null

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

            val isWallpaperActive = WallpaperManager.getWallpaperType(this) != "NONE"
            val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(this)
            val isBrightSurface = isPomoWhite || (isWallpaperActive && isBrightUnderTimer) || !themeCoordinator.isDarkMode()

            val textColor = if (isBrightSurface) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
            val subTextColor = if (isBrightSurface) 0xFF64748B.toInt() else 0x99FFFFFF.toInt()
            val cardBg = if (isPomoWhite) {
                GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(0xFFF8FAFC.toInt())
                    setStroke(dp(1), 0xFFE2E8F0.toInt())
                }
            } else {
                themeCoordinator.createCardBackground(24f)
            }

            panelContainer.removeAllViews()
            panelContainer.orientation = LinearLayout.VERTICAL
            panelContainer.setPadding(0, 0, 0, 0)

            // 1. TOP FLOATING HUD BAR (44dp)
            val topHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply {
                    setMargins(dp(18), dp(4), dp(18), dp(2))
                }
            }
            landscapeHeaderView = topHeader

            // Top-Left: Exit to Portrait Button
            val exitPortraitBtn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                }
                setPadding(dp(10), dp(5), dp(12), dp(5))
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
                    textSize = 13f
                    setTextColor(textColor)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, dp(5), 0)
                }
                val label = TextView(host).apply {
                    text = "Portrait"
                    textSize = 11.5f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(textColor)
                }
                addView(icon)
                addView(label)
                applyBubbleTouchAnimation(this)
            }
            topHeader.addView(exitPortraitBtn)

            // Status Badge Container
            statusBadgeContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(10), 0, 0, 0)
                }
            }

            statusBadge = TextView(this).apply {
                text = "● READY"
                textSize = 11f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.primaryColor)
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(tintedColor(themeCoordinator.primaryColor, 35))
                    setStroke(dp(1), tintedColor(themeCoordinator.primaryColor, 100))
                }
                setPadding(dp(10), dp(4), dp(10), dp(4))
            }
            statusBadgeContainer.addView(statusBadge)
            topHeader.addView(statusBadgeContainer)

            // Spacer
            val headerSpacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            topHeader.addView(headerSpacer)

            // Top-Right: Ambient Sound
            val isAmbientEnabled = StudyAudioManager.isAmbientEnabled(this)
            if (isAmbientEnabled) {
                val soundBtn = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                        setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                    }
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        AmbientSoundBottomSheet.show(host) {
                            host.buildCurrentPanel()
                        }
                    }

                    val currentSoundName = sharedPrefs.getString("ambient_sound_name", "Focus Sound") ?: "Focus Sound"
                    val soundIcon = TextView(host).apply {
                        text = "♫"
                        textSize = 12f
                        setTextColor(themeCoordinator.primaryColor)
                        setPadding(0, 0, dp(4), 0)
                    }
                    val soundLabel = TextView(host).apply {
                        text = currentSoundName
                        textSize = 11f
                        typeface = themeCoordinator.getAppTypeface(isBold = true)
                        setTextColor(textColor)
                        maxWidth = dp(90)
                        setSingleLine(true)
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    addView(soundIcon)
                    addView(soundLabel)
                    applyBubbleTouchAnimation(this)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp(6)
                    }
                }
                topHeader.addView(soundBtn)
            }

            // Mode Selector Chip (Only visible when no timer is running)
            val isLandscapeTimerIdle = (currentTimerState == TimerState.IDLE)
            val modeBtn = LinearLayout(this).apply {
                tag = "mode_pill"
                visibility = if (isLandscapeTimerIdle) View.VISIBLE else View.GONE
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                }
                setPadding(dp(10), dp(5), dp(10), dp(5))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    TimerModeBottomSheet.show(host) {
                        host.buildCurrentPanel()
                    }
                }
                val modeTxt = TextView(host).apply {
                    text = "$modeLabel ▾"
                    textSize = 11f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(textColor)
                }
                addView(modeTxt)
                applyBubbleTouchAnimation(this)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(6)
                }
            }
            topHeader.addView(modeBtn)

            // Settings Gear Icon
            val settingsBtn = TextView(this).apply {
                text = "⚙️"
                textSize = 14f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x20000000.toInt() else 0x30FFFFFF.toInt())
                }
                setPadding(dp(8), dp(4), dp(8), dp(4))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    navigateToPanel(AppPanel.SETTINGS)
                }
                applyBubbleTouchAnimation(this)
            }
            topHeader.addView(settingsBtn)

            panelContainer.addView(topHeader)

            // 2. MAIN HORIZONTAL WORKSPACE
            val mainWorkspace = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    setMargins(dp(18), dp(2), dp(18), dp(10))
                }
            }

            // 2A. LEFT COLUMN: HERO FOCUS CLOCK (58%)
            val clockColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.25f)
            }

            // Subject Pill
            val subjectPill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, dp(2), 0, dp(4))
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isBrightSurface) 0x1A000000.toInt() else 0x25FFFFFF.toInt())
                    setStroke(dp(1), if (isBrightSurface) 0x25000000.toInt() else tintedColor(themeCoordinator.primaryColor, 120))
                }
                setPadding(dp(12), dp(4), dp(14), dp(4))
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
                            marginEnd = dp(6)
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
                    maxWidth = dp(170)
                }
                addView(subjName)
            }
            landscapeSubjectPillView = subjectPill
            clockColumn.addView(subjectPill)

            // Center Clocks Wrapper
            val centerClocksWrapper = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    setMargins(0, dp(2), 0, dp(2))
                }
            }

            timerRing = TimerRingView(this).apply {
                visibility = if (isPomodoro) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER)
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
                textSize = if (isPomodoro) 46f else 72f
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
                textSize = 17f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(6))
                visibility = View.GONE
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
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
            centerClocksWrapper.addView(studyTimerDisplay)
            centerClocksWrapper.addView(breakTimerDisplay)
            clockColumn.addView(centerClocksWrapper)

            // Quote
            val quoteView = TextView(this).apply {
                val quote = DailyQuotes.getTodayQuote()
                text = quote
                textSize = 10.5f
                typeface = themeCoordinator.getAppTypeface(isBold = false)
                setTextColor(subTextColor)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_HORIZONTAL
                maxWidth = dp(360)
            }
            clockColumn.addView(quoteView)

            mainWorkspace.addView(clockColumn)

            // 2B. DIVIDER
            val vDivider = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(if (isBrightSurface) 0x20000000.toInt() else 0x20FFFFFF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    setMargins(dp(12), dp(12), dp(12), dp(12))
                }
            }
            mainWorkspace.addView(vDivider)

            // 2C. RIGHT COLUMN: FOCUS STATION DOCK (42%)
            val rightDockCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                background = cardBg
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.95f)
            }
            landscapeDockView = rightDockCard

            // Header Row: Today's Focus
            val statsHeaderRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val statsTitle = TextView(this).apply {
                text = "TODAY'S FOCUS"
                textSize = 10f
                letterSpacing = 0.08f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(subTextColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            landscapeTodayFocusText = TextView(this).apply {
                text = "0m"
                textSize = 13.5f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.primaryColor)
            }
            statsHeaderRow.addView(statsTitle)
            statsHeaderRow.addView(landscapeTodayFocusText)
            rightDockCard.addView(statsHeaderRow)

            // Goal Progress Bar
            val goalBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = false
                max = 100
                progress = 0
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(6)
                ).apply {
                    setMargins(0, dp(6), 0, dp(4))
                }
            }
            landscapeGoalProgressBar = goalBar
            rightDockCard.addView(goalBar)

            // Goal Sub Row
            val goalSubRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }
            landscapeGoalPercentText = TextView(this).apply {
                text = "Goal Target"
                textSize = 10.5f
                typeface = themeCoordinator.getAppTypeface(isBold = false)
                setTextColor(subTextColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val streak = sharedPrefs.safeInt("current_streak", 1)
            val streakBadge = TextView(this).apply {
                text = "🔥 ${streak}d"
                textSize = 10.5f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(if (isBrightSurface) 0xFFD97706.toInt() else 0xFFFBBF24.toInt())
            }
            goalSubRow.addView(landscapeGoalPercentText)
            goalSubRow.addView(streakBadge)
            rightDockCard.addView(goalSubRow)

            // Divider
            val cardDivider = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(if (isBrightSurface) 0x15000000.toInt() else 0x15FFFFFF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }
            rightDockCard.addView(cardDivider)

            // Controls Title
            val actionTitle = TextView(this).apply {
                text = "CONTROLS"
                textSize = 9.5f
                letterSpacing = 0.08f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(subTextColor)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(6))
                }
            }
            rightDockCard.addView(actionTitle)

            // Controls Container
            controlActionContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val isIdle = (currentTimerState == TimerState.IDLE)
            val btnHeight = dp(44)

            pauseBtn = Button(this).apply {
                textSize = 13.5f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                minimumHeight = btnHeight
                isSoundEffectsEnabled = false
                layoutParams = if (isIdle) {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
                } else {
                    LinearLayout.LayoutParams(0, btnHeight, 1f).apply { setMargins(0, 0, dp(4), 0) }
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
                setPadding(dp(8), dp(4), dp(8), dp(4))
                minimumHeight = btnHeight
                isSoundEffectsEnabled = false
                visibility = if (isIdle) View.GONE else View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
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
                setPadding(dp(8), dp(4), dp(8), dp(4))
                minimumHeight = btnHeight
                visibility = if (isIdle) View.GONE else View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1.15f).apply {
                    setMargins(dp(4), 0, 0, 0)
                }
            }
            attachHoldToEndListener(stopBtn)

            controlActionContainer.addView(pauseBtn)
            controlActionContainer.addView(mainBtn)
            controlActionContainer.addView(stopBtn)
            rightDockCard.addView(controlActionContainer)

            // Zen Hint
            val zenHint = TextView(this).apply {
                text = "Tip: Tap timer for Zen StandBy mode"
                textSize = 9.5f
                typeface = themeCoordinator.getAppTypeface(isBold = false)
                setTextColor(subTextColor)
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(8), 0, 0)
                }
            }
            rightDockCard.addView(zenHint)

            mainWorkspace.addView(rightDockCard)
            panelContainer.addView(mainWorkspace)

            updateVisualStyles()
            applyTrueFullscreenMode()
            updateLandscapeDockStats()
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

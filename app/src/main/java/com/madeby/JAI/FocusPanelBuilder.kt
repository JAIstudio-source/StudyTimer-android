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
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            // 2. Active Subject & Task Header (Unified Session Capsule)
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

            // 1. Subject Pill Button: Colored Dot with outer ring + Bold Name + Dropdown Chevron
            if (showSubjectTagging) {
                val subjectPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Selected study subject: ${activeSubject.name}. Tap to change subject."
                    
                    val pillBg = if (isPomoWhite) {
                        GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(2), tintedColor(subjectColor, 180))
                        }
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(if (themeCoordinator.isDarkMode()) 0x331E293B.toInt() else 0x25FFFFFF.toInt())
                            setStroke(dp(2), tintedColor(subjectColor, 220))
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

                // Glowing color dot indicator with outer ring
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
                        if (isPomoWhite) 0xFF0F172A.toInt()
                        else if (isStudying) Color.parseColor("#10B981")
                        else themeCoordinator.textColor
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
                    setTextColor(subjectColor)
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
                        setColor(if (isPomoWhite) 0xFFF1F5F9.toInt() else 0x25FFFFFF.toInt())
                        setStroke(dp(1), tintedColor(themeCoordinator.accentColor, 180))
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
                    setTextColor(themeCoordinator.accentColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(85)
                }
                goalPill.addView(goalText)

                val clearGoalBtn = TextView(this).apply {
                    text = " ✕"
                    textSize = 11f
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
                        setColor(if (isPomoWhite) 0xFFF1F5F9.toInt() else 0x22FFFFFF.toInt())
                        setStroke(dp(1), if (hasSubTopic) tintedColor(themeCoordinator.primaryColor, 180) else tintedColor(themeCoordinator.textColor, 45))
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

                val topicIcon = TextView(this).apply {
                    text = "📖 "
                    textSize = 10.5f
                }
                topicPill.addView(topicIcon)

                val topicText = TextView(this).apply {
                    text = topicLabel
                    textSize = 12f
                    typeface = themeCoordinator.getAppTypeface(isBold = hasSubTopic)
                    setTextColor(if (isPomoWhite) 0xFF475569.toInt() else if (hasSubTopic) themeCoordinator.primaryColor else tintedColor(themeCoordinator.textColor, 190))
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(85)
                }
                topicPill.addView(topicText)

                val topicChevron = TextView(this).apply {
                    text = " ▾"
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (hasSubTopic) themeCoordinator.primaryColor else tintedColor(themeCoordinator.textColor, 130))
                }
                topicPill.addView(topicChevron)

                sessionPillsRow.addView(topicPill)
            }

            // 3. Mode Pill Button
            val modePill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                contentDescription = "Timer Mode: $modeLabel. Tap to switch mode."
                val pillBg = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isPomoWhite) 0xFFF1F5F9.toInt() else 0x22FFFFFF.toInt())
                    setStroke(dp(1), tintedColor(themeCoordinator.primaryColor, 130))
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
                setTextColor(if (isPomoWhite) 0xFF334155.toInt() else tintedColor(themeCoordinator.textColor, 220))
            }
            modePill.addView(modeText)
            sessionPillsRow.addView(modePill)

            sessionScrollWrapper.addView(sessionPillsRow)
            host.modeOptionsPill = sessionScrollWrapper
            controlsContainer.addView(sessionScrollWrapper)

            // 2.5 Connected Room HUD: Strictly and exclusively shown when the user is currently inside a study room
            val activeRoom = StudyRoomManager.getActiveRoom(this)
            if (activeRoom != null) {
                val roomHud = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    val hudBgColor = if (isPomoWhite) {
                        0xFFF8FAFC.toInt()
                    } else {
                        tintedColor(themeCoordinator.primaryColor, if (themeCoordinator.isDarkMode()) 35 else 25)
                    }
                    background = themeCoordinator.createGlassChip(hudBgColor, 16f)
                    setPadding(dp(12), dp(4), dp(12), dp(4))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, dp(4), 0, dp(2))
                    }
                }

                val liveDot = TextView(this).apply {
                    text = "● "
                    textSize = 9.5f
                    setTextColor(0xFF10B981.toInt()) // Emerald Green
                }
                roomHud.addView(liveDot)

                val roomText = TextView(this).apply {
                    text = "${activeRoom.name} • Connected"
                    textSize = 11.5f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else themeCoordinator.textColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    maxWidth = dp(220)
                }
                roomHud.addView(roomText)

                val roomActionIcon = TextView(this).apply {
                    text = " ↗"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(themeCoordinator.primaryColor)
                }
                roomHud.addView(roomActionIcon)

                roomHud.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    host.defaultCommunitySegment = CommunityPanelBuilder.CommunitySegment.ROOMS
                    host.navigateToPanel(AppPanel.COMMUNITY)
                }

                controlsContainer.addView(roomHud)

                // Asynchronously fetch members to display studying peer status
                CoroutineScope(Dispatchers.Main).launch {
                    val membersRes = StudyRoomManager.fetchRoomMembers(host, activeRoom)
                    val members = membersRes.getOrElse { emptyList() }
                    val peers = members.filterNot { it.isSelf }
                    val studyingPeers = peers.filter { it.isStudying }

                    val detailStr = when {
                        studyingPeers.isNotEmpty() -> {
                            val names = studyingPeers.take(2).joinToString(", ") { it.displayName }
                            val remaining = studyingPeers.size - 2
                            if (remaining > 0) "$names +$remaining studying" else "$names studying"
                        }
                        peers.isNotEmpty() -> "${peers.size} in room"
                        else -> "Co-study active"
                    }
                    roomText.text = "${activeRoom.name} • $detailStr"
                }
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
            val isPomodoro = (savedTimerMode == "COUNTDOWN")
            timerRing = TimerRingView(this).apply {
                visibility = if (!isLandscape && isPomodoro) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
            }

            statusBadge = TextView(this).apply {
                textSize = if (isLandscape) 12.5f else 14f
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
                    setMargins(0, if (isLandscape) dp(4) else dp(8), 0, 0)
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

            val selectedTypeface = FontStyleManager.getTypeface(this)

            studyTimerDisplay = TextView(this).apply {
                text = "00:00:00"
                isSingleLine = true
                maxLines = 1
                textSize = if (isLandscape) 110f else if (isPomodoro) 64f else 78f
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
                textSize = if (isLandscape) 26f else 22f
                typeface = selectedTypeface
                fontFeatureSettings = "tnum"
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, if (isLandscape) 10 else 44)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }

            val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    host.dismissGestureHint()
                    if (isLandscape) {
                        toggleLandscapeControls()
                        return true
                    } else {
                        togglePortraitFullscreenMode()
                        return true
                    }
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
                setTextColor(tintedColor(themeCoordinator.textColor, if (themeCoordinator.isDarkMode()) 95 else 115))
                gravity = Gravity.CENTER
                visibility = if (isLandscape || isGestureHintDismissed || currentTimerState != TimerState.IDLE) View.GONE else View.VISIBLE
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply {
                    bottomMargin = dp(76)
                }
            }
            host.gestureHintView = gestureHint
            centerClocksWrapper.addView(gestureHint)

            // 4. Primary Action Controls
            controlActionContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    if (isLandscape) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (isLandscape) gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, if (isLandscape) dp(6) else dp(10), 0, if (isLandscape) dp(12) else dp(88))
                }
            }

            mainBtn = Button(this).apply {
                textSize = 14.5f
                letterSpacing = 0.02f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                minimumHeight = dp(48)
                isSoundEffectsEnabled = false
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(160) else 0, dp(48), if (isLandscape) 0f else 1f).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(4), 0, dp(4), 0)
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
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(150) else 0, dp(48), if (isLandscape) 0f else 1f).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(4), 0, dp(4), 0)
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
                layoutParams = LinearLayout.LayoutParams(if (isLandscape) dp(160) else 0, dp(48), if (isLandscape) 0f else 1f).apply {
                    if (isLandscape) setMargins(dp(8), 0, dp(8), 0) else setMargins(dp(4), 0, dp(4), 0)
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
                        MotionEvent.ACTION_UP -> {
                            val elapsed = SystemClock.uptimeMillis() - holdStartTime
                            if (elapsed < 500L) {
                                AppToast.show(host, "Press and hold to finish session")
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

            controlActionContainer.addView(pauseBtn)
            controlActionContainer.addView(mainBtn)
            controlActionContainer.addView(stopBtn)

            // Assemble into panelContainer in unified, distraction-free order:
            // 1. Universal Header (Top)
            // 2. Unified Top Capsule: Subject Tag + Timer Mode + Active Task (Right under header)
            // 3. Center Clocks (Middle - ample breathing room)
            // 4. Control Action Container (Start / Pause / End buttons)
            panelContainer.addView(navHeader)
            if (!isLandscape) {
                panelContainer.addView(extraControlsContainer)
            }
            panelContainer.addView(centerClocksWrapper)
            panelContainer.addView(controlActionContainer)

            updateVisualStyles()
            applyTrueFullscreenMode()
        }
    }
}

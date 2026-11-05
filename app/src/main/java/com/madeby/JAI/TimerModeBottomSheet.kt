package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Clean, modern bottom sheet for switching Timer Modes and configuring Pomodoro presets.
 * Follows strict UI/UX guidelines: zero AI slop, uncluttered layout, contextual disclosure,
 * crisp vector iconography, and direct link to main Timer & Focus settings.
 */
object TimerModeBottomSheet {

    fun show(activity: MainActivity, onModeChanged: () -> Unit) {
        if (activity.currentTimerState == TimerState.STUDYING ||
            activity.currentTimerState == TimerState.BREAK ||
            activity.currentTimerState == TimerState.PAUSED) {
            AppToast.show(activity, "Stop or finish current session to switch timer mode")
            return
        }
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val theme = activity.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val textColor = theme.textColor
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        fun dp(v: Int): Int = activity.dp(v)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                val corner = dp(24).toFloat()
                cornerRadii = floatArrayOf(corner, corner, corner, corner, 0f, 0f, 0f, 0f)
                val bgCol = if (isDark) {
                    0xEE0D0F15.toInt()
                } else {
                    0xF5FFFFFF.toInt()
                }
                setColor(bgCol)
                setStroke(dp(1), if (isDark) 0x38FFFFFF.toInt() else 0x40CBD5E1.toInt())
            }
            setPadding(dp(20), dp(16), dp(20), dp(28))
            // Stable minimum height to avoid sheet feeling 'too low' and prevent abrupt height jumps
            minimumHeight = dp(390)
            layoutTransition = android.animation.LayoutTransition().apply {
                enableTransitionType(android.animation.LayoutTransition.CHANGING)
                setDuration(200L)
            }
        }

        // 1. Drag Handle
        val dragHandle = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(3).toFloat()
                setColor(if (isDark) 0xFF475569.toInt() else 0xFFCBD5E1.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14)
            }
        }
        root.addView(dragHandle)

        // 2. Header
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }
        val headerTitle = TextView(activity).apply {
            text = "TIMER MODE"
            textSize = 12f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = dp(44)
            minHeight = dp(44)
            contentDescription = "Close timer mode sheet"
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(headerTitle)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        var currentMode = sharedPrefs.getString("timer_mode", "COUNTDOWN") ?: "COUNTDOWN"
        if (currentMode == "SUBJECT") {
            currentMode = "COUNTDOWN"
            sharedPrefs.edit().putString("timer_mode", "COUNTDOWN").putBoolean("show_subject_pie_chart", true).apply()
        }

        // Section label
        val modeSectionLabel = TextView(activity).apply {
            text = "Select Mode"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(modeSectionLabel)

        // Mode definitions: key, title, iconRes
        val modeItems = listOf(
            Triple("COUNTDOWN", "Pomodoro", R.drawable.ic_clock),
            Triple("STOPWATCH", "Stopwatch", R.drawable.ic_target),
            Triple("LECTURE", "Class Schedule", R.drawable.ic_calendar)
        )

        val modesContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        // Presets Container: Keep always present in the layout hierarchy but elegantly dimmed/disabled
        // when Pomodoro is not active. This completely prevents the bottom sheet from jumping up and down!
        val presetsWrapper = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(14))
        }

        fun updatePresetsVisibility() {
            val isPomo = (currentMode == "COUNTDOWN")
            presetsWrapper.animate()
                .alpha(if (isPomo) 1f else 0.35f)
                .setDuration(180L)
                .start()
            for (i in 0 until presetsWrapper.childCount) {
                val child = presetsWrapper.getChildAt(i)
                child.isEnabled = isPomo
                if (child is LinearLayout) {
                    for (j in 0 until child.childCount) {
                        child.getChildAt(j).isEnabled = isPomo
                    }
                }
            }
        }

        fun selectMode(key: String) {
            currentMode = key
            activity.timerMode = key
            val editor = sharedPrefs.edit()
                .putString("timer_mode", key)
                .putString("timerMode", key)
                .putBoolean("lecture_mode_enabled", key == "LECTURE")

            if (key == "COUNTDOWN") {
                val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                val workSecs = pomoMins * 60L
                editor.putLong("focus_countdown_secs", workSecs)
                editor.putLong("focus_remaining_secs", workSecs)
                if (activity.currentTimerState == TimerState.IDLE) {
                    activity.focusCountdownSecs = workSecs
                    activity.focusRemainingSecs = workSecs
                }
            } else if (key == "STOPWATCH") {
                if (activity.currentTimerState == TimerState.IDLE) {
                    activity.focusRemainingSecs = 0L
                }
            }
            editor.apply()
            updatePresetsVisibility()
            onModeChanged()
        }

        fun buildModeRows() {
            modesContainer.removeAllViews()
            for ((key, title, iconRes) in modeItems) {
                val isSelected = currentMode == key
                val item = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setColor(activity.tintedColor(primary, if (isDark) 40 else 25))
                            setStroke(dp(2), primary)
                        } else {
                            setColor(if (isDark) 0xFF1A1D27.toInt() else 0xFFF8FAFC.toInt())
                            setStroke(dp(1), if (isDark) 0xFF262A38.toInt() else 0xFFE2E8F0.toInt())
                        }
                    }
                    minimumHeight = dp(52)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        selectMode(key)
                        buildModeRows()
                    }
                }

                // Mode Icon
                val iconView = ImageView(activity).apply {
                    setImageResource(iconRes)
                    setColorFilter(if (isSelected) primary else if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginEnd = dp(12)
                    }
                }
                item.addView(iconView)

                // Title
                val titleView = TextView(activity).apply {
                    text = title
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (isSelected) primary else textColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                item.addView(titleView)

                // Check indicator
                if (isSelected) {
                    val checkView = ImageView(activity).apply {
                        setImageResource(R.drawable.ic_check_circle)
                        setColorFilter(primary)
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                    }
                    item.addView(checkView)
                }

                modesContainer.addView(item)
            }
        }
        buildModeRows()
        root.addView(modesContainer)

        // Presets Section Header
        presetsWrapper.addView(TextView(activity).apply {
            text = "Interval Presets"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, dp(4), 0, dp(8))
        })

        val presetsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val presets = listOf(
            Triple("25 / 5", 25, 5),
            Triple("50 / 10", 50, 10),
            Triple("90 / 20", 90, 20)
        )

        fun buildPresetButtons() {
            presetsRow.removeAllViews()
            val curStudyM = sharedPrefs.safeLong("study_interval_minutes", 25L).toInt()
            val curBreakM = sharedPrefs.safeLong("break_interval_minutes", 5L).toInt()

            for (i in presets.indices) {
                val (label, workM, breakM) = presets[i]
                val isCurrentPreset = (curStudyM == workM && curBreakM == breakM)

                val presetBtn = TextView(activity).apply {
                    text = label
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    minHeight = dp(42)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    setTextColor(if (isCurrentPreset) primary else textColor)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        if (isCurrentPreset) {
                            setColor(activity.tintedColor(primary, if (isDark) 35 else 20))
                            setStroke(dp(2), primary)
                        } else {
                            setColor(if (isDark) 0xFF1A1D27.toInt() else 0xFFF8FAFC.toInt())
                            setStroke(dp(1), if (isDark) 0xFF262A38.toInt() else 0xFFE2E8F0.toInt())
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (i < presets.size - 1) rightMargin = dp(8)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val workSecs = workM * 60L
                        val breakSecs = breakM * 60L
                        sharedPrefs.edit()
                            .putString("timer_mode", "COUNTDOWN")
                            .putString("timerMode", "COUNTDOWN")
                            .putLong("study_interval_minutes", workM.toLong())
                            .putLong("break_interval_minutes", breakM.toLong())
                            .putLong("focus_countdown_secs", workSecs)
                            .putLong("break_countdown_secs", breakSecs)
                            .putLong("focus_remaining_secs", workSecs)
                            .putLong("break_remaining_secs", breakSecs)
                            .putInt("pomodoro_study_minutes", workM)
                            .putInt("pomodoro_break_minutes", breakM)
                            .apply()
                        activity.timerMode = "COUNTDOWN"
                        if (activity.currentTimerState == TimerState.IDLE) {
                            activity.focusCountdownSecs = workSecs
                            activity.focusRemainingSecs = workSecs
                        }
                        buildPresetButtons()
                        onModeChanged()
                        dialog.dismiss()
                    }
                }
                presetsRow.addView(presetBtn)
            }
        }
        buildPresetButtons()
        presetsWrapper.addView(presetsRow)
        updatePresetsVisibility()
        root.addView(presetsWrapper)

        // View More Options Button (Leads directly to main Timer & Focus settings tab)
        val moreOptionsCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0x14FFFFFF.toInt() else 0x0A0F172A.toInt())
                setStroke(dp(1), if (isDark) 0x24FFFFFF.toInt() else 0x1AE2E8F0.toInt())
            }
            minimumHeight = dp(48)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
                activity.currentSettingsTab = AppSettingsTab.TIMER
                activity.navigateToPanel(AppPanel.SETTINGS)
            }
        }

        val settingsIcon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                marginEnd = dp(10)
            }
        }
        moreOptionsCard.addView(settingsIcon)

        val moreText = TextView(activity).apply {
            text = "More Timer & Focus Settings"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        moreOptionsCard.addView(moreText)

        val chevron = TextView(activity).apply {
            text = "›"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
        }
        moreOptionsCard.addView(chevron)

        root.addView(moreOptionsCard)

        FontStyleManager.applyRecursively(root)

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setWindowAnimations(android.R.style.Animation_InputMethod)
        }
        dialog.show()
    }
}

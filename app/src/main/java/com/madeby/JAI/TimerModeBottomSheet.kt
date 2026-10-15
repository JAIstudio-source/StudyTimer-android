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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Slide-up bottom sheet housing secondary timer configurations:
 * Modes (Stopwatch, Countdown, Pomodoro, Lecture), Interval Presets (25/5, 50/10, 90/20),
 * and quick switches (Flip-to-Focus gesture, Auto-Break, Sound/Haptics).
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
                    if (theme.activeBgMode == "ECLIPSE") 0xFF161922.toInt() else 0xFF12141C.toInt()
                } else {
                    0xFFFFFFFF.toInt()
                }
                setColor(bgCol)
                setStroke(dp(1), if (isDark) 0xFF2A2E3D.toInt() else 0xFFE2E8F0.toInt())
            }
            setPadding(dp(20), dp(16), dp(20), dp(24))
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
            text = "TIMER MODE & OPTIONS"
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
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(headerTitle)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Section: Timer Modes
        content.addView(TextView(activity).apply {
            text = "Focus Mode"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, 0, 0, dp(8))
        })

        var currentMode = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
        val modes = listOf(
            Triple("STOPWATCH", "⏱️ Stopwatch", "Continuous count-up focus timer without limits"),
            Triple("SUBJECT", "📘 Subject Wise", "Track time tagged to specific subjects & topics"),
            Triple("COUNTDOWN", "🍅 Pomodoro", "Structured study & break interval cycles"),
            Triple("LECTURE", "🏛️ Lecture / Classes", "Sync with your custom class timetable")
        )

        val modePillsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        fun refreshModes() {
            modePillsContainer.removeAllViews()
            for ((key, title, subtitle) in modes) {
                val isSelected = currentMode == key
                val item = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
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
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(8)
                    }
                    isClickable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentMode = key
                        activity.timerMode = key
                        val editor = sharedPrefs.edit()
                            .putString("timer_mode", key)
                            .putString("timerMode", key)
                            .putBoolean("lecture_mode_enabled", key == "LECTURE")
                        if (key == "SUBJECT") {
                            editor.putBoolean("show_subject_pie_chart", true)
                        }
                        if (key == "COUNTDOWN") {
                            val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                            editor.putLong("focus_countdown_secs", pomoMins * 60L)
                        }
                        editor.apply()
                        refreshModes()
                        dialog.dismiss()
                        onModeChanged()
                    }
                }

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(TextView(activity).apply {
                    text = title
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) primary else textColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (isSelected) {
                    row.addView(TextView(activity).apply {
                        text = "✓"
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(primary)
                    })
                }
                item.addView(row)

                val subText = TextView(activity).apply {
                    text = subtitle
                    textSize = 11.5f
                    setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                    setPadding(0, dp(2), 0, 0)
                }
                item.addView(subText)

                modePillsContainer.addView(item)
            }
        }
        refreshModes()
        content.addView(modePillsContainer)

        // Section: Pomodoro Presets
        content.addView(TextView(activity).apply {
            text = "Interval Presets"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, dp(4), 0, dp(8))
        })

        val presetsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(18))
        }

        val presets = listOf(
            Pair("25 / 5", 25 to 5),
            Pair("50 / 10", 50 to 10),
            Pair("90 / 20", 90 to 20)
        )

        for ((label, times) in presets) {
            val (workM, breakM) = times
            val curStudyM = sharedPrefs.getInt("pomodoro_study_minutes", 25)
            val curBreakM = sharedPrefs.getInt("pomodoro_break_minutes", 5)
            val isCurrentPreset = (curStudyM == workM && curBreakM == breakM)

            val presetBtn = TextView(activity).apply {
                text = label
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
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
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    sharedPrefs.edit()
                        .putInt("pomodoro_study_minutes", workM)
                        .putInt("pomodoro_break_minutes", breakM)
                        .apply()
                    dialog.dismiss()
                    onModeChanged()
                }
            }
            presetsRow.addView(presetBtn)
        }
        content.addView(presetsRow)

        // Section: Smart Options & Toggles
        content.addView(TextView(activity).apply {
            text = "Smart Controls"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, dp(4), 0, dp(8))
        })

        // Toggle 1: Flip-to-Focus
        val flipRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0xFF1A1D27.toInt() else 0xFFF8FAFC.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
        }
        val flipCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        flipCol.addView(TextView(activity).apply {
            text = "📱 Flip-to-Focus Gesture"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        })
        flipCol.addView(TextView(activity).apply {
            text = "Place phone face down to start timer automatically"
            textSize = 11.5f
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
        })
        flipRow.addView(flipCol)

        val flipSwitch = SwitchMaterial(activity).apply {
            isChecked = FlipToFocusManager.isEnabled(activity)
            setOnCheckedChangeListener { _, isChecked ->
                FlipToFocusManager.setEnabled(activity, isChecked)
            }
        }
        flipRow.addView(flipSwitch)
        content.addView(flipRow)

        // Toggle 2: Auto-Break Flow
        val breakRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0xFF1A1D27.toInt() else 0xFFF8FAFC.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
        }
        val breakCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        breakCol.addView(TextView(activity).apply {
            text = "☕ Auto-Start Breaks"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        })
        breakCol.addView(TextView(activity).apply {
            text = "Transition seamlessly into break without tapping"
            textSize = 11.5f
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
        })
        breakRow.addView(breakCol)

        val breakSwitch = SwitchMaterial(activity).apply {
            isChecked = sharedPrefs.getBoolean("auto_break_enabled", false)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean("auto_break_enabled", isChecked).apply()
            }
        }
        breakRow.addView(breakSwitch)
        content.addView(breakRow)

        scroll.addView(content)
        root.addView(scroll)

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

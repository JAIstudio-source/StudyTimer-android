package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * First-Time User Experience (FTUX) 3-Step Setup Wizard.
 * Guides new users through setting their study focus, daily goal, and preferred timer style.
 * Strictly adheres to clean typography with zero extra emojis.
 */
object OnboardingDialogHelper {

    fun showOnboardingIfNeeded(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_completed_onboarding_v2", false)) {
            return
        }

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val theme = activity.themeCoordinator
        val primary = theme.primaryColor
        val textColor = theme.textColor

        fun dp(v: Int): Int = activity.dp(v)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                (activity.resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(420)),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        var currentStep = 1
        var selectedFocus = "Competitive Exams"
        var selectedDailyHours = 4
        var selectedTimerMode = "COUNTDOWN"

        val stepContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Indicator Row: Step pills [ ━ ─ ─ ]
        val indicatorRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }

        fun updateIndicators() {
            indicatorRow.removeAllViews()
            for (step in 1..3) {
                val dot = View(activity).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dp(3).toFloat()
                        setColor(if (step == currentStep) primary else Color.argb(45, 255, 255, 255))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        if (step == currentStep) dp(24) else dp(8),
                        dp(5)
                    ).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }
                }
                indicatorRow.addView(dot)
            }
        }

        // Action Buttons Row: [ Skip ] and [ Continue / Get Started ]
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, 0)
        }

        val skipBtn = Button(activity).apply {
            text = "Skip"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(tintedColor(textColor, 140))
            background = null
            setOnClickListener {
                prefs.edit().putBoolean("has_completed_onboarding_v2", true).apply()
                dialog.dismiss()
            }
        }

        val nextBtn = Button(activity).apply {
            text = "Continue"
            textSize = 14f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(theme.bgColor)
            background = activity.rippleBackground(primary)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                setMargins(dp(10), 0, 0, 0)
            }
        }

        buttonRow.addView(skipBtn)
        buttonRow.addView(nextBtn)

        fun renderStep() {
            updateIndicators()
            stepContainer.removeAllViews()

            when (currentStep) {
                1 -> {
                    // Step 1: Study Focus
                    val step1View = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    val title = TextView(activity).apply {
                        text = "Welcome to StudyTimer"
                        textSize = 19f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    val subtitle = TextView(activity).apply {
                        text = "What is your main focus area?"
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(4), 0, dp(14))
                    }
                    step1View.addView(title)
                    step1View.addView(subtitle)

                    val focusCategories = listOf(
                        "Competitive Exams",
                        "College / University",
                        "School & Academics",
                        "Programming & Tech",
                        "Self-Improvement"
                    )

                    val optionsContainer = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    for (category in focusCategories) {
                        val isSelected = category == selectedFocus
                        val optBtn = Button(activity).apply {
                            text = if (isSelected) "✓  $category" else category
                            textSize = 13.5f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) Color.WHITE else textColor)
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            setPadding(dp(16), dp(11), dp(16), dp(11))
                            background = if (isSelected) {
                                GradientDrawable().apply {
                                    cornerRadius = dp(14).toFloat()
                                    setColor(primary)
                                }
                            } else {
                                theme.createGlassChip(tintedColor(textColor, 22), 14f)
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, dp(3), 0, dp(3))
                            }
                            setOnClickListener {
                                selectedFocus = category
                                renderStep()
                            }
                        }
                        optionsContainer.addView(optBtn)
                    }

                    step1View.addView(optionsContainer)
                    stepContainer.addView(step1View)
                    nextBtn.text = "Continue"
                }
                2 -> {
                    // Step 2: Daily Target & Timer Mode
                    val step2View = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    val title = TextView(activity).apply {
                        text = "Target & Timer Mode"
                        textSize = 19f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Choose your daily goal and study method:"
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(4), 0, dp(14))
                    }
                    step2View.addView(title)
                    step2View.addView(subtitle)

                    val goalLabel = TextView(activity).apply {
                        text = "DAILY STUDY TARGET"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(8))
                    }
                    step2View.addView(goalLabel)

                    // Daily Target Options (2h, 4h, 6h, 8h)
                    val targetRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(16))
                        }
                    }

                    val targetOptions = listOf(2, 4, 6, 8)
                    for (hrs in targetOptions) {
                        val isSelected = (hrs == selectedDailyHours)
                        val tBtn = Button(activity).apply {
                            text = "${hrs}h"
                            textSize = 13.5f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) Color.WHITE else textColor)
                            background = if (isSelected) {
                                GradientDrawable().apply {
                                    cornerRadius = dp(12).toFloat()
                                    setColor(primary)
                                }
                            } else {
                                theme.createGlassChip(tintedColor(textColor, 22), 12f)
                            }
                            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                                setMargins(dp(3), 0, dp(3), 0)
                            }
                            setOnClickListener {
                                selectedDailyHours = hrs
                                renderStep()
                            }
                        }
                        targetRow.addView(tBtn)
                    }
                    step2View.addView(targetRow)

                    val timerModeLabel = TextView(activity).apply {
                        text = "PREFERRED TIMER STYLE"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(8))
                    }
                    step2View.addView(timerModeLabel)

                    val timerStyles = listOf(
                        "COUNTDOWN" to "Pomodoro (25m Focus / 5m Break)",
                        "SUBJECT" to "Stopwatch (Continuous Count Up)"
                    )

                    for ((modeKey, modeDesc) in timerStyles) {
                        val isSelected = (modeKey == selectedTimerMode)
                        val modeBtn = Button(activity).apply {
                            text = if (isSelected) "✓  $modeDesc" else modeDesc
                            textSize = 12.5f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) Color.WHITE else textColor)
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            setPadding(dp(14), dp(10), dp(14), dp(10))
                            background = if (isSelected) {
                                GradientDrawable().apply {
                                    cornerRadius = dp(12).toFloat()
                                    setColor(primary)
                                }
                            } else {
                                theme.createGlassChip(tintedColor(textColor, 22), 12f)
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, dp(3), 0, dp(3))
                            }
                            setOnClickListener {
                                selectedTimerMode = modeKey
                                renderStep()
                            }
                        }
                        step2View.addView(modeBtn)
                    }

                    stepContainer.addView(step2View)
                    nextBtn.text = "Continue"
                }
                3 -> {
                    // Step 3: All Set Summary
                    val step3View = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                    }

                    val title = TextView(activity).apply {
                        text = "You're Ready to Focus"
                        textSize = 19f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Your personalized workspace is configured:"
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(4), 0, dp(14))
                    }
                    step3View.addView(title)
                    step3View.addView(subtitle)

                    // Summary Bento Card
                    val summaryCard = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        background = theme.createGlassChip(tintedColor(textColor, 24), 16f)
                        setPadding(dp(18), dp(14), dp(18), dp(14))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    fun addSummaryRow(label: String, value: String) {
                        val row = LinearLayout(activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, dp(4), 0, dp(4))
                            }
                        }
                        val lbl = TextView(activity).apply {
                            text = label
                            textSize = 13f
                            setTextColor(tintedColor(textColor, 140))
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val v = TextView(activity).apply {
                            text = value
                            textSize = 13.5f
                            typeface = theme.getAppTypeface(isBold = true)
                            setTextColor(primary)
                        }
                        row.addView(lbl)
                        row.addView(v)
                        summaryCard.addView(row)
                    }

                    addSummaryRow("Focus Area", selectedFocus)
                    addSummaryRow("Daily Goal", "${selectedDailyHours} Hours")
                    addSummaryRow("Timer Style", if (selectedTimerMode == "COUNTDOWN") "Pomodoro (25/5)" else "Stopwatch")

                    step3View.addView(summaryCard)
                    stepContainer.addView(step3View)

                    nextBtn.text = "Get Started"
                }
            }
        }

        nextBtn.setOnClickListener {
            if (currentStep < 3) {
                currentStep++
                renderStep()
            } else {
                // Apply configurations and finish onboarding
                prefs.edit()
                    .putString("user_target_focus", selectedFocus)
                    .putString("target_exam", selectedFocus)
                    .putInt("daily_goal_minutes", selectedDailyHours * 60)
                    .putString("timer_mode", selectedTimerMode)
                    .putBoolean("has_completed_onboarding_v2", true)
                    .apply()

                dialog.dismiss()
                activity.buildCurrentPanel()
                AppToast.show(activity, "Welcome to StudyTimer! Tap Start to begin your first session.")
            }
        }

        root.addView(indicatorRow)
        root.addView(stepContainer)
        root.addView(buttonRow)

        dialog.setContentView(root)
        renderStep()
        dialog.show()
    }

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}

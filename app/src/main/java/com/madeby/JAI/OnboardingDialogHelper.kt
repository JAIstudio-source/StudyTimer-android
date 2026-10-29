package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Premium Full-Screen FTUX Onboarding Wizard.
 * 
 * Replaces intrusive floating modals with a continuous, beautifully crafted
 * 3-step setup screen that matches StudyTimer's brand aura, clean typography,
 * high-contrast selection tiles, and zero cheesy emoji slop.
 */
object OnboardingDialogHelper {

    fun showOnboardingIfNeeded(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_completed_onboarding_v2", false)) {
            return
        }

        val dialog = Dialog(activity, R.style.FullScreenOnboardingDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(R.drawable.bg_login_gradient)

        val theme = activity.themeCoordinator
        val primary = theme.primaryColor
        val textColor = theme.textColor

        fun dp(v: Int): Int = activity.dp(v)

        var currentStep = 1
        var selectedFocus = "Competitive Exams"
        var selectedDailyHours = 4
        var selectedTimerMode = "COUNTDOWN"

        // Root container matching LoginActivity full-bleed style
        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = null
        }

        // Top App Bar / Progress Navigation Bar
        val topBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Brand mark in top bar
        val brandRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val miniLogo = ImageView(activity).apply {
            setImageResource(R.drawable.ic_logo_transparent)
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                setMargins(0, 0, dp(10), 0)
            }
            contentDescription = "StudyTimer Logo"
        }
        val brandName = TextView(activity).apply {
            text = "StudyTimer"
            textSize = 15f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(Color.WHITE)
            letterSpacing = -0.01f
        }
        brandRow.addView(miniLogo)
        brandRow.addView(brandName)

        val stepCounter = TextView(activity).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(tintedColor(textColor, 140))
            letterSpacing = 0.05f
        }

        topBar.addView(brandRow)
        topBar.addView(stepCounter)

        // Progress line track
        val progressTrack = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(24), 0, dp(24), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val stepPills = mutableListOf<View>()
        for (i in 1..3) {
            val pill = View(activity).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(3).toFloat()
                    setColor(if (i == 1) primary else Color.argb(35, 255, 255, 255))
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply {
                    setMargins(if (i > 1) dp(6) else 0, 0, 0, 0)
                }
            }
            stepPills.add(pill)
            progressTrack.addView(pill)
        }

        fun updateProgress() {
            stepCounter.text = "STEP $currentStep OF 3"
            stepPills.forEachIndexed { index, pill ->
                val stepNum = index + 1
                (pill.background as? GradientDrawable)?.setColor(
                    if (stepNum <= currentStep) primary else Color.argb(35, 255, 255, 255)
                )
            }
        }

        // Scrollable content area
        val scrollView = ScrollView(activity).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(20))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(contentContainer)

        // Bottom Action Bar [ Back / Skip ] and [ Continue / Get Started ]
        val bottomBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        var renderCurrentStepFn: (() -> Unit)? = null

        val secondaryBtn = Button(activity).apply {
            text = "Skip"
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(tintedColor(textColor, 150))
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(52)
            ).apply {
                setMargins(0, 0, dp(14), 0)
            }
            setOnClickListener {
                if (currentStep > 1) {
                    currentStep--
                    renderCurrentStepFn?.invoke()
                } else {
                    prefs.edit().putBoolean("has_completed_onboarding_v2", true).apply()
                    dialog.dismiss()
                }
            }
        }

        val primaryBtn = Button(activity).apply {
            text = "Continue"
            isAllCaps = false
            textSize = 15f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
        }

        bottomBar.addView(secondaryBtn)
        bottomBar.addView(primaryBtn)

        fun createCardTile(
            titleText: String,
            captionText: String,
            isSelected: Boolean,
            onClick: () -> Unit
        ): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(dp(18), dp(16), dp(18), dp(16))
                
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    if (isSelected) {
                        setColor(Color.argb(40, Color.red(primary), Color.green(primary), Color.blue(primary)))
                        setStroke(dp(1.8f.toInt()), primary)
                    } else {
                        setColor(Color.argb(20, 255, 255, 255))
                        setStroke(dp(1), Color.argb(25, 255, 255, 255))
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(12))
                }

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val titleTv = TextView(activity).apply {
                    text = titleText
                    textSize = 15.5f
                    typeface = theme.getAppTypeface(isBold = isSelected)
                    setTextColor(if (isSelected) Color.WHITE else textColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val indicator = View(activity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        if (isSelected) {
                            setColor(primary)
                        } else {
                            setColor(Color.TRANSPARENT)
                            setStroke(dp(1), Color.argb(60, 255, 255, 255))
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                }

                row.addView(titleTv)
                row.addView(indicator)
                addView(row)

                if (captionText.isNotBlank()) {
                    val capTv = TextView(activity).apply {
                        text = captionText
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(if (isSelected) tintedColor(textColor, 200) else tintedColor(textColor, 130))
                        setPadding(0, dp(4), 0, 0)
                    }
                    addView(capTv)
                }

                setOnClickListener { onClick() }
            }
        }

        fun renderStep() {
            updateProgress()
            contentContainer.removeAllViews()

            secondaryBtn.text = if (currentStep > 1) "Back" else "Skip"

            when (currentStep) {
                1 -> {
                    // Header
                    val header = TextView(activity).apply {
                        text = "What is your main focus area?"
                        textSize = 24f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(Color.WHITE)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Choose your current learning priority. StudyTimer will tailor your initial sessions around this goal."
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(24))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    val focusCategories = listOf(
                        "Competitive Exams" to "UPSC, GATE, GRE, USMLE, Bar & entrance tests",
                        "College / University" to "Degree coursework, finals, thesis & research",
                        "School & Academics" to "High school classes, daily homework & revision",
                        "Programming & Tech" to "Coding practice, personal projects & software skills",
                        "Self-Improvement" to "Reading, languages, writing & deep personal habits"
                    )

                    for ((category, desc) in focusCategories) {
                        val isSelected = (category == selectedFocus)
                        val tile = createCardTile(category, desc, isSelected) {
                            selectedFocus = category
                            renderStep()
                        }
                        contentContainer.addView(tile)
                    }

                    primaryBtn.text = "Continue"
                }
                2 -> {
                    val header = TextView(activity).apply {
                        text = "Target & Timer Mode"
                        textSize = 24f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(Color.WHITE)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Set a realistic daily goal and choose how you like your focus intervals timed."
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(20))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    val goalSectionHeader = TextView(activity).apply {
                        text = "DAILY STUDY GOAL"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(10))
                    }
                    contentContainer.addView(goalSectionHeader)

                    // Target hours selector pills
                    val targetHoursRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(24))
                        }
                    }

                    val targets = listOf(
                        2 to "2 hrs",
                        4 to "4 hrs",
                        6 to "6 hrs",
                        8 to "8 hrs"
                    )

                    for ((hrs, label) in targets) {
                        val isSelected = (hrs == selectedDailyHours)
                        val chip = Button(activity).apply {
                            text = label
                            isAllCaps = false
                            textSize = 13.5f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) Color.WHITE else textColor)
                            background = GradientDrawable().apply {
                                cornerRadius = dp(14).toFloat()
                                if (isSelected) {
                                    setColor(primary)
                                } else {
                                    setColor(Color.argb(20, 255, 255, 255))
                                    setStroke(dp(1), Color.argb(25, 255, 255, 255))
                                }
                            }
                            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                                setMargins(dp(3), 0, dp(3), 0)
                            }
                            setOnClickListener {
                                selectedDailyHours = hrs
                                renderStep()
                            }
                        }
                        targetHoursRow.addView(chip)
                    }
                    contentContainer.addView(targetHoursRow)

                    val timerSectionHeader = TextView(activity).apply {
                        text = "TIMER METHOD"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(10))
                    }
                    contentContainer.addView(timerSectionHeader)

                    val timerModes = listOf(
                        Triple(
                            "COUNTDOWN",
                            "Pomodoro Technique",
                            "25 minutes deep focus with 5 minutes resting intervals"
                        ),
                        Triple(
                            "SUBJECT",
                            "Stopwatch (Count-Up)",
                            "Continuous uninterrupted flow timer for flexible deep work"
                        )
                    )

                    for ((modeKey, title, desc) in timerModes) {
                        val isSelected = (modeKey == selectedTimerMode)
                        val tile = createCardTile(title, desc, isSelected) {
                            selectedTimerMode = modeKey
                            renderStep()
                        }
                        contentContainer.addView(tile)
                    }

                    primaryBtn.text = "Continue"
                }
                3 -> {
                    val header = TextView(activity).apply {
                        text = "You're Ready to Focus"
                        textSize = 24f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(Color.WHITE)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Your personalized workspace is configured and ready. You can modify these settings anytime in Preferences."
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 170))
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(24))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    // Minimalist Overview Bento Card
                    val summaryBox = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        background = GradientDrawable().apply {
                            cornerRadius = dp(20).toFloat()
                            setColor(Color.argb(25, 255, 255, 255))
                            setStroke(dp(1), Color.argb(35, 255, 255, 255))
                        }
                        setPadding(dp(22), dp(20), dp(22), dp(20))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(20))
                        }
                    }

                    fun addSummaryRow(label: String, value: String, isLast: Boolean = false) {
                        val row = LinearLayout(activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(0, dp(6), 0, dp(6))
                        }
                        val lbl = TextView(activity).apply {
                            text = label
                            textSize = 14f
                            setTextColor(tintedColor(textColor, 140))
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val v = TextView(activity).apply {
                            text = value
                            textSize = 14.5f
                            typeface = theme.getAppTypeface(isBold = true)
                            setTextColor(Color.WHITE)
                        }
                        row.addView(lbl)
                        row.addView(v)
                        summaryBox.addView(row)

                        if (!isLast) {
                            val divider = View(activity).apply {
                                background = GradientDrawable().apply {
                                    setColor(Color.argb(18, 255, 255, 255))
                                }
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    dp(1)
                                ).apply {
                                    setMargins(0, dp(4), 0, dp(4))
                                }
                            }
                            summaryBox.addView(divider)
                        }
                    }

                    addSummaryRow("Focus Area", selectedFocus)
                    addSummaryRow("Daily Study Target", "$selectedDailyHours Hours")
                    val modeDisplay = if (selectedTimerMode == "COUNTDOWN") "Pomodoro (25/5)" else "Stopwatch (Continuous)"
                    addSummaryRow("Timer Style", modeDisplay, isLast = true)

                    contentContainer.addView(summaryBox)

                    // Value proposition tip card
                    val tipBox = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(Color.argb(30, Color.red(primary), Color.green(primary), Color.blue(primary)))
                            setStroke(dp(1), Color.argb(60, Color.red(primary), Color.green(primary), Color.blue(primary)))
                        }
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val tipText = TextView(activity).apply {
                        text = "Tip: Your stats, streaks, and progress are stored privately on your device. Tap Start on the timer to log your first session."
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(tintedColor(textColor, 210))
                        setLineSpacing(dp(2).toFloat(), 1.0f)
                    }
                    tipBox.addView(tipText)
                    contentContainer.addView(tipBox)

                    primaryBtn.text = "Get Started"
                }
            }
        }
        renderCurrentStepFn = { renderStep() }

        primaryBtn.setOnClickListener {
            if (currentStep < 3) {
                currentStep++
                renderStep()
            } else {
                val goalSecs = (selectedDailyHours * 3600L).coerceAtLeast(1800L)
                GoalHistoryManager.setDailyGoal(activity, goalSecs)
                prefs.edit()
                    .putString("user_target_focus", selectedFocus)
                    .putString("target_exam", selectedFocus)
                    .putString("timer_mode", selectedTimerMode)
                    .putBoolean("has_completed_onboarding_v2", true)
                    .apply()

                dialog.dismiss()
                activity.buildCurrentPanel()
                AppToast.show(activity, "Welcome to StudyTimer! Tap Start to begin your first session.")
            }
        }

        rootLayout.addView(topBar)
        rootLayout.addView(progressTrack)
        rootLayout.addView(scrollView)
        rootLayout.addView(bottomBar)

        // Window Inset handling for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            topBar.setPadding(dp(24), statusBars.top + dp(14), dp(24), dp(12))
            bottomBar.setPadding(dp(24), dp(12), dp(24), navBars.bottom + dp(18))
            insets
        }

        dialog.setContentView(rootLayout)
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

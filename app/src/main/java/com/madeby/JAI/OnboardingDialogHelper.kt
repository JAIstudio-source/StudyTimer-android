package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Premium Full-Screen FTUX Onboarding Wizard.
 * 
 * Clean white background and crisp obsidian/black typography,
 * minimal inputs (Name -> Focus Area -> Target & Timer Mode),
 * zero extra text and zero visual clutter (Anti-UI-Slop compliant).
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
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Ensure status bar & navigation bar icons are dark (light theme)
        if (window != null) {
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }

        val theme = activity.themeCoordinator
        val primary = theme.primaryColor

        fun dp(v: Int): Int = activity.dp(v)

        // Clean White Canvas & Stark Black/Slate Typography
        val textPrimary = 0xFF0F172A.toInt()      // Deep obsidian slate / black
        val textSecondary = 0xFF475569.toInt()    // Slate neutral
        val textMuted = 0xFF94A3B8.toInt()        // Clean subtle placeholder
        val tileBgNormal = 0xFFF8FAFC.toInt()     // Soft clean off-white card surface
        val tileBorderNormal = 0xFFE2E8F0.toInt() // Crisp 1dp border
        val tileBgSelected = Color.argb(22, Color.red(primary), Color.green(primary), Color.blue(primary))
        val tileBorderSelected = primary

        var currentStep = 1
        var enteredName = ""
        var selectedFocus = "Competitive Exams"
        var selectedDailyHours = 4
        var selectedTimerMode = "COUNTDOWN"

        // Root container with clean white background
        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Top App Bar / Progress Navigation Bar
        val topBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(10))
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
            setColorFilter(primary)
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                setMargins(0, 0, dp(10), 0)
            }
            contentDescription = "StudyTimer Logo"
        }
        val brandName = TextView(activity).apply {
            text = "StudyTimer"
            textSize = 15f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(textPrimary)
            letterSpacing = -0.01f
        }
        brandRow.addView(miniLogo)
        brandRow.addView(brandName)

        val stepCounter = TextView(activity).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textSecondary)
            letterSpacing = 0.08f
        }

        topBar.addView(brandRow)
        topBar.addView(stepCounter)

        // Progress line track (3 smooth pills)
        val progressTrack = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(24), dp(4), dp(24), dp(16))
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
                    setColor(if (i == 1) primary else 0xFFE2E8F0.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply {
                    setMargins(if (i > 1) dp(8) else 0, 0, 0, 0)
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
                    if (stepNum <= currentStep) primary else 0xFFE2E8F0.toInt()
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
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(textSecondary)
            background = RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x18000000.toInt()),
                null,
                null
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(52)
            ).apply {
                setMargins(0, 0, dp(14), 0)
            }
            setOnClickListener {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(windowToken, 0)

                if (currentStep > 1) {
                    currentStep--
                    renderCurrentStepFn?.invoke()
                } else {
                    // Skip to step 2 keeping default name
                    currentStep = 2
                    renderCurrentStepFn?.invoke()
                }
            }
        }

        val primaryBtn = Button(activity).apply {
            text = "Continue"
            isAllCaps = false
            textSize = 15f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
        }

        bottomBar.addView(secondaryBtn)
        bottomBar.addView(primaryBtn)

        fun createCardTile(
            emojiIcon: String,
            titleText: String,
            captionText: String,
            isSelected: Boolean,
            onClick: () -> Unit
        ): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(dp(16), dp(14), dp(16), dp(14))

                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (isSelected) tileBgSelected else tileBgNormal)
                    setStroke(if (isSelected) dp(2) else dp(1), if (isSelected) tileBorderSelected else tileBorderNormal)
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(10))
                }

                val iconTv = TextView(activity).apply {
                    text = emojiIcon
                    textSize = 20f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                        setMargins(0, 0, dp(12), 0)
                    }
                }

                val textCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val titleTv = TextView(activity).apply {
                    text = titleText
                    textSize = 15f
                    typeface = theme.getAppTypeface(isBold = isSelected)
                    setTextColor(textPrimary)
                }
                textCol.addView(titleTv)

                if (captionText.isNotBlank()) {
                    val capTv = TextView(activity).apply {
                        text = captionText
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(if (isSelected) textPrimary else textSecondary)
                        setPadding(0, dp(2), 0, 0)
                    }
                    textCol.addView(capTv)
                }

                val indicator = View(activity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        if (isSelected) {
                            setColor(primary)
                        } else {
                            setColor(Color.TRANSPARENT)
                            setStroke(dp(2), 0xFFCBD5E1.toInt())
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                        setMargins(dp(10), 0, 0, 0)
                    }
                }

                addView(iconTv)
                addView(textCol)
                addView(indicator)

                setOnClickListener { onClick() }
            }
        }

        fun renderStep() {
            updateProgress()
            contentContainer.removeAllViews()

            secondaryBtn.text = if (currentStep > 1) "Back" else "Skip"

            when (currentStep) {
                1 -> {
                    // STEP 1: WELCOME & NAME (Clean, premium, no extra text)
                    val header = TextView(activity).apply {
                        text = "Welcome to StudyTimer"
                        textSize = 28f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "What should we call you?"
                        textSize = 16f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setPadding(0, dp(6), 0, dp(32))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    val nameInputBox = EditText(activity).apply {
                        tag = "name_input"
                        setText(enteredName)
                        hint = "Your name"
                        setHintTextColor(textMuted)
                        setTextColor(textPrimary)
                        textSize = 17f
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                        typeface = theme.getAppTypeface(isBold = false)
                        setSingleLine(true)
                        maxLines = 1
                        imeOptions = EditorInfo.IME_ACTION_DONE
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(0xFFF8FAFC.toInt())
                            setStroke(dp(1), 0xFFE2E8F0.toInt())
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(56)
                        )
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                enteredName = s?.toString()?.trim() ?: ""
                            }
                        })
                        setOnFocusChangeListener { _, hasFocus ->
                            (background as? GradientDrawable)?.apply {
                                setStroke(if (hasFocus) dp(2) else dp(1), if (hasFocus) primary else 0xFFE2E8F0.toInt())
                                setColor(if (hasFocus) Color.WHITE else 0xFFF8FAFC.toInt())
                            }
                        }
                        setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                enteredName = text.toString().trim()
                                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(windowToken, 0)
                                clearFocus()
                                true
                            } else false
                        }
                    }
                    contentContainer.addView(nameInputBox)

                    primaryBtn.text = "Continue →"
                }
                2 -> {
                    // STEP 2: FOCUS AREA
                    val header = TextView(activity).apply {
                        text = "What is your main focus?"
                        textSize = 26f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Choose your primary study area."
                        textSize = 15f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setPadding(0, dp(6), 0, dp(24))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    val focusCategories = listOf(
                        Triple("🎯", "Competitive Exams", "UPSC, JEE, NEET, GATE, GRE, USMLE & entrance tests"),
                        Triple("🎓", "College & University", "Degree coursework, semester finals, lab & thesis"),
                        Triple("💻", "Programming & Tech", "Coding practice, personal projects & software skills"),
                        Triple("📖", "School & Academics", "High school classes, daily homework & revision"),
                        Triple("⚡", "Personal Mastery", "Deep reading, languages, writing & skill building")
                    )

                    for ((emoji, category, desc) in focusCategories) {
                        val isSelected = (category == selectedFocus)
                        val tile = createCardTile(emoji, category, desc, isSelected) {
                            selectedFocus = category
                            renderStep()
                        }
                        contentContainer.addView(tile)
                    }

                    primaryBtn.text = "Continue →"
                }
                3 -> {
                    // STEP 3: COMMITMENT & TIMER STYLE
                    val header = TextView(activity).apply {
                        text = "Target & Timer Style"
                        textSize = 26f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Set your daily target and default timer mode."
                        textSize = 15f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setPadding(0, dp(6), 0, dp(20))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    // Daily Target Selector
                    val goalSectionHeader = TextView(activity).apply {
                        text = "DAILY STUDY TARGET"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(10))
                    }
                    contentContainer.addView(goalSectionHeader)

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
                        1 to "1h",
                        2 to "2h",
                        4 to "4h",
                        6 to "6h",
                        8 to "8h"
                    )

                    for ((hrs, label) in targets) {
                        val isSelected = (hrs == selectedDailyHours)
                        val chip = Button(activity).apply {
                            text = label
                            isAllCaps = false
                            textSize = 14.5f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) Color.WHITE else 0xFF334155.toInt())
                            background = GradientDrawable().apply {
                                cornerRadius = dp(14).toFloat()
                                if (isSelected) {
                                    setColor(primary)
                                } else {
                                    setColor(0xFFF1F5F9.toInt())
                                    setStroke(dp(1), 0xFFE2E8F0.toInt())
                                }
                            }
                            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
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

                    // Timer Mode Selector
                    val timerSectionHeader = TextView(activity).apply {
                        text = "DEFAULT TIMER MODE"
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
                            "⏱️",
                            Pair("Pomodoro Technique (25/5)", "Structured focus intervals with automatic resting breaks")
                        ),
                        Triple(
                            "SUBJECT",
                            "⏳",
                            Pair("Stopwatch (Count-Up)", "Continuous flow timer for open-ended deep sessions")
                        )
                    )

                    for ((modeKey, emoji, pair) in timerModes) {
                        val (title, desc) = pair
                        val isSelected = (modeKey == selectedTimerMode)
                        val tile = createCardTile(emoji, title, desc, isSelected) {
                            selectedTimerMode = modeKey
                            renderStep()
                        }
                        contentContainer.addView(tile)
                    }

                    primaryBtn.text = "Get Started 🚀"
                }
            }
        }
        renderCurrentStepFn = { renderStep() }

        primaryBtn.setOnClickListener {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(rootLayout.windowToken, 0)

            if (currentStep == 1) {
                val typed = contentContainer.findViewWithTag<EditText>("name_input")?.text?.toString()?.trim()
                if (!typed.isNullOrBlank()) {
                    enteredName = typed
                }
                currentStep++
                renderStep()
            } else if (currentStep == 2) {
                currentStep++
                renderStep()
            } else {
                // Finalize Onboarding
                val goalSecs = (selectedDailyHours * 3600L).coerceAtLeast(1800L)
                GoalHistoryManager.setDailyGoal(activity, goalSecs)

                val validName = if (enteredName.isNotBlank()) enteredName.trim() else "Student"
                val currentProfile = ProfileManager.getProfile(activity)
                val newProfile = currentProfile.copy(
                    displayName = validName,
                    lastApprovedDisplayName = validName,
                    targetExam = selectedFocus,
                    dailyGoalMinutes = selectedDailyHours * 60,
                    updatedAt = System.currentTimeMillis()
                )
                ProfileManager.saveProfile(activity, newProfile)

                prefs.edit()
                    .putString("custom_display_name", validName)
                    .putString("user_target_focus", selectedFocus)
                    .putString("target_exam", selectedFocus)
                    .putString("timer_mode", selectedTimerMode)
                    .putBoolean("has_completed_onboarding_v2", true)
                    .apply()

                dialog.dismiss()
                activity.buildCurrentPanel()
                val welcomeMsg = if (enteredName.isNotBlank()) "Welcome, ${enteredName.trim()}! Tap Start to begin your first session." else "Welcome to StudyTimer! Tap Start to begin."
                AppToast.show(activity, welcomeMsg)
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
            topBar.setPadding(dp(24), statusBars.top + dp(14), dp(24), dp(10))
            bottomBar.setPadding(dp(24), dp(10), dp(24), navBars.bottom + dp(16))
            insets
        }

        dialog.setContentView(rootLayout)
        renderStep()
        dialog.show()
    }
}

package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
import androidx.core.view.WindowInsetsCompat

/**
 * Premium Full-Screen FTUX Onboarding Wizard.
 * 
 * Redesigned for pure contrast, minimal questions (Name -> Focus Area -> Daily Target & Timer),
 * guaranteed 100% text readability on dark glass canvas, and zero visual slop.
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
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val theme = activity.themeCoordinator
        val primary = theme.primaryColor

        fun dp(v: Int): Int = activity.dp(v)

        // Colors locked to high-contrast dark aesthetic
        val textPrimary = 0xFFFFFFFF.toInt()
        val textSecondary = 0xFF94A3B8.toInt()
        val textMuted = 0xFF64748B.toInt()
        val tileBgNormal = 0x14FFFFFF.toInt()
        val tileBorderNormal = 0x22FFFFFF.toInt()
        val tileBgSelected = Color.argb(45, Color.red(primary), Color.green(primary), Color.blue(primary))
        val tileBorderSelected = primary

        var currentStep = 1
        var enteredName = ""
        var selectedFocus = "Competitive Exams"
        var selectedDailyHours = 4
        var selectedTimerMode = "COUNTDOWN"

        // Root container matching login full-bleed style
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
                    setColor(if (i == 1) primary else 0x28FFFFFF.toInt())
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
                    if (stepNum <= currentStep) primary else 0x28FFFFFF.toInt()
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
                android.content.res.ColorStateList.valueOf(0x22FFFFFF.toInt()),
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
                        setTextColor(if (isSelected) 0xFFCBD5E1.toInt() else textSecondary)
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
                            setStroke(dp(1), 0x55FFFFFF.toInt())
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
                    // STEP 1: WELCOME & NAME
                    val header = TextView(activity).apply {
                        text = "Welcome to StudyTimer"
                        textSize = 26f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Your quiet space for deep work, habit consistency, and uninterrupted exam mastery."
                        textSize = 14f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(28))
                    }
                    contentContainer.addView(header)
                    contentContainer.addView(subtitle)

                    // Hero Badge card
                    val heroBadge = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(0x18FFFFFF.toInt())
                            setStroke(dp(1), 0x22FFFFFF.toInt())
                        }
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(26))
                        }
                    }
                    val sparkIcon = TextView(activity).apply {
                        text = "⚡"
                        textSize = 22f
                        setPadding(0, 0, dp(12), 0)
                    }
                    val heroText = TextView(activity).apply {
                        text = "100% Offline & Private. No annoying popups, tracking ads, or forced accounts."
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setTextColor(0xFFE2E8F0.toInt())
                        setLineSpacing(dp(2).toFloat(), 1.0f)
                    }
                    heroBadge.addView(sparkIcon)
                    heroBadge.addView(heroText)
                    contentContainer.addView(heroBadge)

                    // Name Input Section
                    val nameSectionHeader = TextView(activity).apply {
                        text = "WHAT SHOULD WE CALL YOU?"
                        textSize = 11.5f
                        letterSpacing = 0.08f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(primary)
                        setPadding(0, 0, 0, dp(10))
                    }
                    contentContainer.addView(nameSectionHeader)

                    val nameInputBox = EditText(activity).apply {
                        tag = "name_input"
                        setText(enteredName)
                        hint = "Enter your first name or nickname"
                        setHintTextColor(textMuted)
                        setTextColor(textPrimary)
                        textSize = 15.5f
                        typeface = theme.getAppTypeface(isBold = false)
                        setSingleLine(true)
                        maxLines = 1
                        imeOptions = EditorInfo.IME_ACTION_DONE
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(0x14FFFFFF.toInt())
                            setStroke(dp(1), 0x33FFFFFF.toInt())
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(52)
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
                                setStroke(if (hasFocus) dp(2) else dp(1), if (hasFocus) primary else 0x33FFFFFF.toInt())
                                setColor(if (hasFocus) 0x22FFFFFF.toInt() else 0x14FFFFFF.toInt())
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

                    val nameHint = TextView(activity).apply {
                        text = "Used to personalize your daily greetings, session summaries, and stats."
                        textSize = 12f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textMuted)
                        setPadding(dp(2), dp(8), 0, 0)
                    }
                    contentContainer.addView(nameHint)

                    primaryBtn.text = "Continue →"
                }
                2 -> {
                    // STEP 2: FOCUS AREA
                    val header = TextView(activity).apply {
                        text = "What is your main focus?"
                        textSize = 25f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Choose your current learning priority. You can adjust this anytime."
                        textSize = 14f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(20))
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
                        text = "Target & Focus Style"
                        textSize = 25f
                        typeface = theme.getAppTypeface(isBold = true)
                        setTextColor(textPrimary)
                        letterSpacing = -0.02f
                    }
                    val subtitle = TextView(activity).apply {
                        text = "Set your baseline daily target and choose your preferred study timer flow."
                        textSize = 14f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(textSecondary)
                        setLineSpacing(dp(3).toFloat(), 1.0f)
                        setPadding(0, dp(8), 0, dp(20))
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
                            textSize = 14f
                            typeface = theme.getAppTypeface(isBold = isSelected)
                            setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else textSecondary)
                            background = GradientDrawable().apply {
                                cornerRadius = dp(14).toFloat()
                                if (isSelected) {
                                    setColor(primary)
                                } else {
                                    setColor(0x14FFFFFF.toInt())
                                    setStroke(dp(1), 0x24FFFFFF.toInt())
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
                            Pair("Pomodoro Technique (25/5)", "Structured focus intervals with automatic short & long resting breaks")
                        ),
                        Triple(
                            "SUBJECT",
                            "⏳",
                            Pair("Stopwatch (Count-Up)", "Continuous flow timer for open-ended deep sessions without rigid alarms")
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

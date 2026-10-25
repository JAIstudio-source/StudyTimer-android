package com.madeby.JAI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Psychological full-screen shield shown when a blocked app is opened during study.
 * Designed with a single primary CTA ("Return to Study") and subtle, friction-based
 * text actions to discourage impulsive distraction while honoring emergency needs.
 *
 * Displays the official StudyTimer logo and dynamically updates blocked app info
 * without tearing down the view or showing stale app information during fast app switches.
 */
class BlockerOverlayActivity : Activity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var timerTextView: TextView? = null
    private var appIconView: ImageView? = null
    private var noticeTextView: TextView? = null
    private var blockedPackageName: String = ""

    private val tickerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""

        // Configure window for lock-screen overlay and OLED black
        configureWindow()

        // Build clean programmatic layout matching StudyTimer design language
        setContentView(buildView())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        BlockerOverlayWindowManager.hide(this)
        AppBlockerManager.muteBackgroundMedia(this)
        val newPkg = intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        if (newPkg.isNotEmpty()) {
            blockedPackageName = newPkg
            updateBlockedAppDetails()
        }
    }

    override fun onResume() {
        super.onResume()
        BlockerOverlayWindowManager.hide(this)
        AppBlockerManager.muteBackgroundMedia(this)
        updateBlockedAppDetails()
        updateTimerDisplay()
        handler.removeCallbacks(tickerRunnable)
        handler.post(tickerRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickerRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppBlockerManager.clearCurrentBlocked()
    }

    private fun configureWindow() {
        window.setWindowAnimations(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun updateBlockedAppDetails() {
        val pm = packageManager
        val appInfo = runCatching { pm.getApplicationInfo(blockedPackageName, 0) }.getOrNull()
        val appLabel = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: "Distracting App"
        val appIconDrawable = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }

        noticeTextView?.text = "$appLabel is locked during study"
        if (appIconDrawable != null) {
            appIconView?.setImageDrawable(appIconDrawable)
            appIconView?.visibility = View.VISIBLE
        } else {
            appIconView?.visibility = View.GONE
        }
    }

    private fun buildView(): View {
        val theme = ThemeCoordinator(this).apply { applyThemeCoordinates() }
        val activeSubject = runCatching { SubjectTagManager.getSelectedSubject(this) }.getOrNull()
        val subjectColor = try {
            Color.parseColor(activeSubject?.colorHex ?: "#A78BFA")
        } catch (_: Exception) {
            theme.primaryColor
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt()) // Pure OLED Black
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(36))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Flexible center content space
        val centerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // --- 1. StudyTimer Official Logo ---
        val appLogoView = ImageView(this).apply {
            setImageResource(R.drawable.mylogo)
            val logoSize = dp(100)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(logoSize, logoSize).apply {
                bottomMargin = dp(18)
            }
            contentDescription = "StudyTimer Logo"
        }
        centerContainer.addView(appLogoView)

        // --- 2. Subject & Live Timer Hero ---
        val subjectTitle = TextView(this).apply {
            text = activeSubject?.name ?: "Focus Session"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        centerContainer.addView(subjectTitle)

        // Live Timer Digits
        timerTextView = TextView(this).apply {
            text = "00:00"
            textSize = 50f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = 0.05f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(24))
        }
        centerContainer.addView(timerTextView)

        // Blocked App Container
        val blockedNoticeCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0x18FFFFFF.toInt())
                setStroke(dp(1), 0x22FFFFFF.toInt())
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                marginEnd = dp(12)
            }
        }
        appIconView = iconView
        blockedNoticeCard.addView(iconView)

        val noticeText = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(0xFFE2E8F0.toInt())
        }
        noticeTextView = noticeText
        blockedNoticeCard.addView(noticeText)
        centerContainer.addView(blockedNoticeCard)

        // Initial setup of blocked app icon & name
        updateBlockedAppDetails()

        // Thoughtful psychological encouragement
        val reminderText = TextView(this).apply {
            text = "Your attention is valuable. Protect your momentum."
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0x8AFFFFFF.toInt()) // 54% opacity
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(270), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        centerContainer.addView(reminderText)

        root.addView(centerContainer)

        // --- 3. Bottom Action Hierarchy ---
        val actionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // HERO CTA: The ONE and only button on screen
        val returnButton = TextView(this).apply {
            text = "Return to Study"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(subjectColor)
            }
            elevation = dp(4).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                bottomMargin = dp(18)
            }
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(this@BlockerOverlayActivity, it)
                AppBlockerManager.clearCurrentBlocked()
                val intent = Intent(this@BlockerOverlayActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        actionsLayout.addView(returnButton)

        // Secondary Text Action: Go to Home screen (plain text, no pill background)
        val homeLink = TextView(this).apply {
            text = "Go to Home screen"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(0x99FFFFFF.toInt()) // 60% opacity white
            setPadding(dp(16), dp(8), dp(16), dp(12))
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(this@BlockerOverlayActivity, it)
                AppBlockerManager.clearCurrentBlocked()
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }
        actionsLayout.addView(homeLink)

        // Bottom Discreet Link: Emergency bypass (subtle muted text, low contrast)
        val emergencyLink = TextView(this).apply {
            text = "its important, let me use the app"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0x55FFFFFF.toInt()) // 33% opacity muted text
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(this@BlockerOverlayActivity, it)
                AppBlockerManager.clearCurrentBlocked()
                AppBlockerManager.activateEmergencyBypass()
                finish()
            }
        }
        actionsLayout.addView(emergencyLink)

        root.addView(actionsLayout)
        return root
    }

    private fun updateTimerDisplay() {
        val sp = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val state = sp.getString("timerState", "IDLE")

        // If the study session ended or paused, dismiss the overlay automatically
        if (state != "STUDYING") {
            finish()
            return
        }

        val mode = sp.getString("timer_mode", "STOPWATCH")
        val accumulated = sp.getLong("accumulatedStudy", 0L)
        val focusRemaining = sp.getLong("focus_remaining_secs", 0L)

        val timeString = if (mode == "COUNTDOWN" && focusRemaining > 0L) {
            formatSeconds(focusRemaining)
        } else {
            formatSeconds(accumulated)
        }
        timerTextView?.text = timeString
    }

    private fun formatSeconds(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0L)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Intercept back button: Redirect to home so user cannot back-press into the blocked app
        AppBlockerManager.clearCurrentBlocked()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

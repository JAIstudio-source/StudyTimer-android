package com.madeby.JAI

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * High-performance System Overlay Manager (Solution A) using WindowManager.addView()
 * with TYPE_APPLICATION_OVERLAY.
 *
 * Eliminates the visual "flash" caused by Activity task-switching:
 * Renders directly onto the screen composer (< 16ms), covering the restricted app
 * without creating or switching Android tasks.
 */
object BlockerOverlayWindowManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null

    private var rootView: FullscreenOverlayLayout? = null
    private var timerTextView: TextView? = null
    private var appIconView: ImageView? = null
    private var noticeTextView: TextView? = null
    private var returnButton: TextView? = null

    @Volatile
    private var isOverlayShowing = false

    @Volatile
    private var currentBlockedPkg: String = ""

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (isOverlayShowing) {
                rootView?.context?.let { updateTimerDisplay(it) }
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    fun isShowing(): Boolean = isOverlayShowing

    fun getCurrentBlockedPackage(): String = currentBlockedPkg

    /**
     * Attaches or updates the overlay directly on the system window layer.
     * Safe to invoke from any thread (automatically dispatches to the Main thread).
     */
    fun show(context: Context, blockedPackage: String) {
        mainHandler.post {
            val appContext = context.applicationContext

            if (!Settings.canDrawOverlays(appContext)) {
                return@post
            }

            currentBlockedPkg = blockedPackage

            if (isOverlayShowing && rootView?.parent != null) {
                // Overlay already active on screen: update app info with 0ms transition delay
                updateBlockedAppDetails(appContext)
                updateTimerDisplay(appContext)
                return@post
            }

            try {
                if (windowManager == null) {
                    windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                }
                val wm = windowManager ?: return@post

                val root = buildOverlayView(appContext)
                rootView = root

                val params = buildLayoutParams()
                wm.addView(root, params)
                isOverlayShowing = true

                // Start 1-second live timer update
                mainHandler.removeCallbacks(tickerRunnable)
                mainHandler.post(tickerRunnable)
            } catch (e: Exception) {
                android.util.Log.e("BlockerOverlayWM", "Failed to add system overlay view: ${e.message}", e)
                isOverlayShowing = false
            }
        }
    }

    /**
     * Removes the overlay from the system window layer.
     */
    fun hide(context: Context) {
        mainHandler.post {
            mainHandler.removeCallbacks(tickerRunnable)
            AppBlockerManager.releaseAudioFocus()
            if (isOverlayShowing && rootView != null) {
                try {
                    val wm = windowManager ?: (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                    if (rootView?.parent != null) {
                        wm?.removeView(rootView)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BlockerOverlayWM", "Error removing system overlay view: ${e.message}", e)
                } finally {
                    isOverlayShowing = false
                    rootView = null
                    timerTextView = null
                    appIconView = null
                    noticeTextView = null
                    returnButton = null
                    currentBlockedPkg = ""
                    AppBlockerManager.clearCurrentBlocked()
                    AppBlockerManager.releaseAudioFocus()
                }
            }
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = layoutType
            // Intercept touches and hardware buttons like Back without allowing pass-through to blocked app
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.FILL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun dp(context: Context, v: Int): Int {
        return (v * context.resources.displayMetrics.density).toInt()
    }

    private fun updateBlockedAppDetails(context: Context) {
        val pm = context.packageManager
        val appInfo = runCatching { pm.getApplicationInfo(currentBlockedPkg, 0) }.getOrNull()
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

    private fun buildOverlayView(context: Context): FullscreenOverlayLayout {
        val theme = ThemeCoordinator(context).apply { applyThemeCoordinates() }
        val activeSubject = runCatching { SubjectTagManager.getSelectedSubject(context) }.getOrNull()
        val subjectColor = try {
            Color.parseColor(activeSubject?.colorHex ?: "#A78BFA")
        } catch (_: Exception) {
            theme.primaryColor
        }

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val padTop = if (isLandscape) dp(context, 16) else dp(context, 48)
        val padBottom = if (isLandscape) dp(context, 16) else dp(context, 36)
        val logoSize = if (isLandscape) dp(context, 54) else dp(context, 100)
        val logoBottomMargin = if (isLandscape) dp(context, 8) else dp(context, 18)
        val timerTextSize = if (isLandscape) 36f else 50f
        val timerPaddingBottom = if (isLandscape) dp(context, 6) else dp(context, 24)
        val buttonHeight = if (isLandscape) dp(context, 44) else dp(context, 52)
        val buttonBottomMargin = if (isLandscape) dp(context, 8) else dp(context, 18)

        val root = FullscreenOverlayLayout(context).apply {
            setBackgroundColor(0xFF000000.toInt()) // Pure OLED Black
            isFocusable = true
            isFocusableInTouchMode = true
            setOnBackClickListener {
                // Intercept back key: send user to Home screen and hide overlay
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                hide(context)
            }
        }

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(context, 24), padTop, dp(context, 24), padBottom)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Center container
        val centerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // 1. StudyTimer Official Logo
        val appLogoView = ImageView(context).apply {
            setImageResource(R.drawable.mylogo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(logoSize, logoSize).apply {
                bottomMargin = logoBottomMargin
            }
            contentDescription = "StudyTimer Logo"
        }
        centerContainer.addView(appLogoView)

        // 2. Subject Title
        val subjectTitle = TextView(context).apply {
            text = activeSubject?.name ?: "Focus Session"
            textSize = if (isLandscape) 15f else 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        centerContainer.addView(subjectTitle)

        // 3. Live Timer Digits
        timerTextView = TextView(context).apply {
            text = "00:00"
            textSize = timerTextSize
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = 0.05f
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, timerPaddingBottom)
        }
        centerContainer.addView(timerTextView)

        // 4. Blocked Notice Card
        val blockedNoticeCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 14).toFloat()
                setColor(0x18FFFFFF.toInt())
                setStroke(dp(context, 1), 0x22FFFFFF.toInt())
            }
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (isLandscape) dp(context, 8) else dp(context, 16)
            }
        }

        appIconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 30), dp(context, 30)).apply {
                marginEnd = dp(context, 12)
            }
        }
        blockedNoticeCard.addView(appIconView)

        noticeTextView = TextView(context).apply {
            textSize = if (isLandscape) 13f else 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(0xFFE2E8F0.toInt())
        }
        blockedNoticeCard.addView(noticeTextView)
        centerContainer.addView(blockedNoticeCard)

        // 5. Psychological encouragement
        val reminderText = TextView(context).apply {
            text = "Your attention is valuable. Protect your momentum."
            textSize = if (isLandscape) 12f else 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0x8AFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(context, 270), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        centerContainer.addView(reminderText)

        contentLayout.addView(centerContainer)

        // 6. Bottom Action Hierarchy
        val actionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Return to Study Button
        returnButton = TextView(context).apply {
            text = "Return to Study"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 14).toFloat()
                setColor(subjectColor)
            }
            elevation = dp(context, 4).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeight
            ).apply {
                bottomMargin = buttonBottomMargin
            }
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(context, it)
                hide(context)
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        }
        actionsLayout.addView(returnButton)

        // Home screen link
        val homeLink = TextView(context).apply {
            text = "Go to Home screen"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(0x99FFFFFF.toInt())
            setPadding(dp(context, 16), dp(context, 6), dp(context, 16), dp(context, 8))
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(context, it)
                hide(context)
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
            }
        }
        actionsLayout.addView(homeLink)

        // Emergency bypass link
        val emergencyLink = TextView(context).apply {
            text = "its important, let me use the app"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0x55FFFFFF.toInt())
            setPadding(dp(context, 16), dp(context, 6), dp(context, 16), dp(context, 6))
            setOnClickListener {
                HapticFeedbackHelper.performMicroHaptic(context, it)
                AppBlockerManager.activateEmergencyBypass()
                hide(context)
            }
        }
        actionsLayout.addView(emergencyLink)

        contentLayout.addView(actionsLayout)
        scrollView.addView(contentLayout)
        root.addView(scrollView)

        updateBlockedAppDetails(context)
        updateTimerDisplay(context)

        return root
    }

    private fun updateTimerDisplay(context: Context) {
        val sp = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val state = sp.getString("timerState", "IDLE")

        if (state != "STUDYING") {
            hide(context)
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

    /**
     * Custom FrameLayout wrapper to intercept hardware buttons like KEYCODE_BACK
     * and prevent them from passing through to the blocked app.
     */
    class FullscreenOverlayLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {

        private var onBackClickListener: (() -> Unit)? = null

        fun setOnBackClickListener(listener: () -> Unit) {
            this.onBackClickListener = listener
        }

        override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
            if (event?.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackClickListener?.invoke()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }
}

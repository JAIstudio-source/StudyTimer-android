package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Universal persistent header builder for StudyTimer.
 * Left: Profile Avatar + Name + Streak Pill (🔥 14d) -> opens Profile dialog.
 * Center: Screen-centered title for tabs (PLANNER, INSIGHTS, COMMUNITY).
 * Right: Optional Context Action + Settings Gear (⚙️) -> opens Settings.
 */
object UniversalHeaderHelper {

    fun createHeader(
        activity: MainActivity,
        extraRightView: View? = null,
        title: String? = null
    ): View {
        val theme = activity.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val textColor = theme.textColor
        val isPomoWhite = activity.isPomodoroPureWhiteActive()
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        fun dp(v: Int): Int = activity.dp(v)

        val headerRoot = FrameLayout(activity).apply {
            setPadding(0, dp(4), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Left Container: Profile & Streak
        val profilePill = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = if (isPomoWhite) {
                GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            } else if (!isDark) {
                GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(0xFFFFFFFF.toInt())
                    setStroke(dp(1), 0xFFE2E8F0.toInt())
                }
            } else {
                theme.createGlassChip(activity.tintedColor(primary, 40), 18f)
            }
            setPadding(dp(4), dp(4), dp(10), dp(4))
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                activity.currentSettingsTab = AppSettingsTab.PROFILE
                activity.navigateToPanel(AppPanel.SETTINGS)
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            )
        }

        val rawName = prefs.getString("user_display_name", null)
            ?: prefs.getString("custom_username", null)
            ?: "Student"
        val displayName = if (rawName.length > 14) rawName.take(12) + "…" else rawName
        val initialLetter = (rawName.firstOrNull() ?: 'S').uppercaseChar().toString()

        // Avatar circle
        val avatarCircle = TextView(activity).apply {
            text = initialLetter
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                rightMargin = dp(6)
            }
        }
        profilePill.addView(avatarCircle)

        // Name (Keeps full name visible, single-line, graceful tail truncation if exceptionally long)
        val nameText = TextView(activity).apply {
            text = displayName
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else textColor)
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (!title.isNullOrBlank()) {
                maxWidth = dp(85)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dp(6)
            }
        }
        profilePill.addView(nameText)

        // Streak Pill
        val streak = activity.statsEngine.computeStatsSnapshot().streak
        val streakText = TextView(activity).apply {
            text = "🔥 ${streak}d"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isDark) 0xFFF59E0B.toInt() else 0xFFD97706.toInt())
        }
        profilePill.addView(streakText)
        headerRoot.addView(profilePill)

        // Exact Geometric Center Title (Pinned to exact 50% screen center)
        if (!title.isNullOrBlank()) {
            val titleView = TextView(activity).apply {
                text = title
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                letterSpacing = 0.04f
                setTextColor(primary)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
            headerRoot.addView(titleView)
        }

        // Right side Container (Extra Action + Settings Gear)
        val rightContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        }

        if (extraRightView != null) {
            rightContainer.addView(extraRightView)
        }

        // Settings Gear Icon
        val settingsIcon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_settings)
            if (isPomoWhite) {
                setColorFilter(0xFF0F172A.toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFF1F5F9.toInt())
                }
            } else {
                setColorFilter(primary)
                background = if (theme.isGlassStyle() || theme.isBubbleStyle()) {
                    theme.createGlassIconBackground(activity.tintedColor(primary, 70))
                } else null
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = activity.getString(R.string.cd_open_settings)
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                activity.currentSettingsTab = when (activity.currentPanel) {
                    AppPanel.FOCUS -> AppSettingsTab.HUB
                    AppPanel.STATS -> AppSettingsTab.ANALYTICS
                    AppPanel.PLANNER -> AppSettingsTab.PLANNER
                    AppPanel.COMMUNITY -> AppSettingsTab.COMMUNITY
                    else -> AppSettingsTab.HUB
                }
                activity.navigateToPanel(AppPanel.SETTINGS)
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                leftMargin = dp(4)
            }
        }
        rightContainer.addView(settingsIcon)
        headerRoot.addView(rightContainer)

        return headerRoot
    }
}

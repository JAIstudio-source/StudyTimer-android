package com.madeby.JAI

import android.content.Context
import android.content.res.ColorStateList
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

import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

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

        val rawName = ProfileManager.getEffectiveDisplayName(activity).ifBlank {
            prefs.getString("user_display_name", null)
                ?: prefs.getString("custom_username", null)
                ?: "Student"
        }
        val displayName = if (rawName.length > 14) rawName.take(12) + "…" else rawName
        val initialLetter = (rawName.firstOrNull() ?: 'S').uppercaseChar().toString()

        // Avatar container (Profile picture with smooth circular crop, falling back to letter badge)
        val avatarContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                rightMargin = dp(6)
            }
        }

        val avatarBitmap = LocalAvatarManager.getCircularAvatarBitmap(activity, dp(26))
        if (avatarBitmap != null) {
            val avatarImageView = ImageView(activity).apply {
                setImageBitmap(avatarBitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            avatarContainer.addView(avatarImageView)
        } else {
            val avatarCircle = TextView(activity).apply {
                text = initialLetter
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(LocalAvatarManager.getGoogleAvatarColor(rawName))
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            avatarContainer.addView(avatarCircle)
        }
        profilePill.addView(avatarContainer)

        // Name (Keeps full name visible, single-line, graceful tail truncation if exceptionally long)
        val nameText = TextView(activity).apply {
            text = displayName
            textSize = 13f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(if (isPomoWhite) 0xFF0F172A.toInt() else textColor)
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxWidth = if (!title.isNullOrBlank()) dp(62) else dp(115)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        profilePill.addView(nameText)
        headerRoot.addView(profilePill)

        // Exact Geometric Center Title (Guaranteed zero-overlap with left profile & right action via margins)
        if (!title.isNullOrBlank()) {
            val screenWidthDp = activity.resources.configuration.screenWidthDp
            val titleView = TextView(activity).apply {
                text = title
                textSize = 14.5f
                typeface = theme.getAppTypeface(isBold = true)
                letterSpacing = 0.04f
                setTextColor(primary)
                gravity = Gravity.CENTER
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxWidth = dp((screenWidthDp * 0.42f).toInt().coerceIn(100, 200))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
            headerRoot.addView(titleView)
        }

        // Right side Container (Extra Action + Wallpaper Icon + Settings Gear)
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
        val settingsIcon = AppCompatImageView(activity).apply {
            setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_settings))
            val iconBg = if (isPomoWhite) {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFF1F5F9.toInt())
                }
            } else {
                theme.createGlassIconBackground(activity.tintedColor(primary, if (isDark) 65 else 90))
            }
            background = iconBg
            setColorFilter(if (isPomoWhite) 0xFF0F172A.toInt() else primary)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = activity.getString(R.string.cd_open_settings)
            isClickable = true
            isFocusable = true
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

        FontStyleManager.applyRecursively(headerRoot)
        return headerRoot
    }
}

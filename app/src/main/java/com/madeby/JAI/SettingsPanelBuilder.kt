package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Clean, Hierarchical Category Navigation (Hub & Spoke) Architecture for Settings.
 * AMOLED #000000 base, #121212 / #141720 card surfaces, 16dp rounded corners.
 *
 * Hub Sections:
 * 0. Top User Profile Card (Avatar/Initials, display name, email, Google Connected status)
 * 1. Timer & Focus (Interval pickers, auto-start breaks, strict mode, mode picker)
 * 2. Goals & Reminders (Daily goal target picker, custom reminder picker, streak settings)
 * 3. Theme & Appearance (AMOLED/Slate/Light, 3D Bubble/Glass/Classic, Accent colors)
 * 4. Cloud & Backups (Google account sync, backup JSON/CSV export/import)
 * 5. User Profile Management Sub-Screen
 * 6. Developer & Advanced (Strictly gated behind dev unlock / debug)
 */
class SettingsPanelBuilder(private val host: MainActivity) {

    companion object {
        var pendingSettingsScrollY: Int? = null
    }

    private fun dp(v: Int): Int = host.dp(v)
    private fun dpF(v: Float): Float = v * host.resources.displayMetrics.density

    fun build(target: android.view.ViewGroup = host.panelContainer, captureScrollRef: Boolean = true) {
        with(host) {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

            if (captureScrollRef) {
                tabPageCache.keys.removeIf { it.startsWith("ST:") }
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    CloudSyncManager.syncDataToCloud(this@with)
                }
            }

            val settingsRootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }

            val settingsBackFab = TextView(this).apply {
                text = "← Back"
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.bgColor)
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                elevation = dp(6).toFloat()
                setPadding(dp(24), 0, dp(24), 0)
                setOnClickListener {
                    handleSettingsBack()
                }
                visibility = if (currentSettingsTab == AppSettingsTab.HUB) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    dp(48),
                    Gravity.BOTTOM or Gravity.END
                ).apply {
                    setMargins(0, 0, dp(20), dp(20))
                }
            }

            // Top Header & Hub Breadcrumb
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(4), dp(6), dp(6))
            }

            val backArrowBtn = TextView(this).apply {
                text = "←"
                textSize = 24f
                setTextColor(themeCoordinator.primaryColor)
                setPadding(dp(4), 0, dp(14), 0)
                setOnClickListener {
                    handleSettingsBack()
                }
            }
            headerRow.addView(backArrowBtn)

            val headerText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> getString(R.string.settings_title)
                    AppSettingsTab.TIMER -> "Timer & Focus"
                    AppSettingsTab.PLANNER -> "Planner & Goals"
                    AppSettingsTab.ANALYTICS -> "Insights & Stats"
                    AppSettingsTab.THEME -> "Theme & Appearance"
                    AppSettingsTab.CLOUD -> "Cloud & Backups"
                    AppSettingsTab.PROFILE -> "Profile Settings"
                    AppSettingsTab.COMMUNITY -> "Community Settings"
                    AppSettingsTab.DEVELOPER -> "Developer Tools"
                    else -> getString(R.string.settings_title)
                }
                setTextColor(themeCoordinator.textColor)
                textSize = 22f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                letterSpacing = -0.01f
            }
            headerRow.addView(headerText)

            if (currentSettingsTab != AppSettingsTab.HUB) {
                val spacer = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                }
                headerRow.addView(spacer)

                val allSettingsBtn = TextView(this).apply {
                    text = "All Settings ▾"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 14f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    isClickable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentSettingsTab = AppSettingsTab.HUB
                        settingsOriginWasSubTab = false
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                headerRow.addView(allSettingsBtn)
            }

            settingsRootLayout.addView(headerRow)

            val subtitleText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> "Customize your timer, goals, charts & backups"
                    AppSettingsTab.TIMER -> "Focus durations, break times & timer mode"
                    AppSettingsTab.PLANNER -> "Daily targets, goal reminders & weekly history"
                    AppSettingsTab.ANALYTICS -> "Heatmap, subject breakdown & chart display"
                    AppSettingsTab.THEME -> "OLED black, light mode, styles & colors"
                    AppSettingsTab.CLOUD -> "Google account sync, auto-backup & restore"
                    AppSettingsTab.PROFILE -> "Personal student identity, avatar, display name & bio"
                    AppSettingsTab.COMMUNITY -> "Leaderboard visibility, study room presence & alerts"
                    AppSettingsTab.DEVELOPER -> "Diagnostic tools & debug settings"
                    else -> getString(R.string.settings_subtitle)
                }
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                textSize = 13f
                setPadding(dp(6), 0, dp(6), dp(14))
            }
            settingsRootLayout.addView(subtitleText)

            val existingScrollView = if (captureScrollRef) settingsScrollViewRef else null
            val settingsScrollView: ScrollView
            if (existingScrollView != null) {
                (existingScrollView.parent as? android.view.ViewGroup)?.removeView(existingScrollView)
                settingsScrollView = existingScrollView
            } else {
                settingsScrollView = ScrollView(this).apply {
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    isFillViewport = true
                    isNestedScrollingEnabled = true
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                if (captureScrollRef) settingsScrollViewRef = settingsScrollView
            }
            settingsScrollView.isVerticalScrollBarEnabled = false
            settingsScrollView.isHorizontalScrollBarEnabled = false

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), 0, dp(16), dp(96))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            settingsScrollView.removeAllViews()
            settingsScrollView.addView(layout)
            val targetScrollY = pendingSettingsScrollY ?: 0
            settingsScrollView.post {
                settingsScrollView.scrollTo(0, targetScrollY)
                pendingSettingsScrollY = null
            }

            fun createSectionLabel(title: String): TextView {
                return TextView(this).apply {
                    text = title
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    letterSpacing = 0.15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(6), dp(18), dp(6), dp(8))
                }
            }

            fun createDivider(): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    background = GradientDrawable().apply {
                        setColor(if (themeCoordinator.isDarkMode()) Color.parseColor("#22232B") else tintedColor(themeCoordinator.textColor, 25))
                    }
                }
            }

            fun createSettingsCard(): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground(20f)
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(10))
                    }
                }
            }

            fun createSettingsRow(icon: String, title: String, subtitle: String, trailingView: View? = null): LinearLayout {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
                }
                if (icon.isNotEmpty()) {
                    val iconView = TextView(this).apply {
                        text = icon
                        textSize = 20f
                        setPadding(0, 0, dp(14), 0)
                    }
                    row.addView(iconView)
                }
                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                textCol.addView(TextView(this).apply {
                    text = title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                textCol.addView(TextView(this).apply {
                    text = subtitle
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                row.addView(textCol)
                if (trailingView != null) {
                    row.addView(trailingView)
                }
                return row
            }

            fun createCategoryVectorIcon(tab: AppSettingsTab): View {
                return object : View(this) {
                    private val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.FILL
                    }
                    private val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dpF(1.2f)
                    }
                    private val iconPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dpF(2f)
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.FILL
                    }
                    private val bgRect = android.graphics.RectF()
                    private val path = android.graphics.Path()

                    override fun onDraw(canvas: android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val w = width.toFloat()
                        val h = height.toFloat()
                        val r = dpF(10f)
                        bgRect.set(dpF(1f), dpF(1f), w - dpF(1f), h - dpF(1f))

                        val (accentColor, bgColor) = when (tab) {
                            AppSettingsTab.TIMER -> 0xFF818CF8.toInt() to 0x226366F1.toInt()
                            AppSettingsTab.PLANNER -> 0xFF10B981.toInt() to 0x2210B981.toInt()
                            AppSettingsTab.ANALYTICS -> 0xFFF59E0B.toInt() to 0x22F59E0B.toInt()
                            AppSettingsTab.PROFILE -> 0xFFEC4899.toInt() to 0x22EC4899.toInt()
                            AppSettingsTab.COMMUNITY -> 0xFF06B6D4.toInt() to 0x2206B6D4.toInt()
                            AppSettingsTab.THEME -> 0xFFC084FC.toInt() to 0x22A855F7.toInt()
                            AppSettingsTab.CLOUD -> 0xFF38BDF8.toInt() to 0x220EA5E9.toInt()
                            AppSettingsTab.DEVELOPER -> 0xFF34D399.toInt() to 0x2210B981.toInt()
                            else -> 0xFF94A3B8.toInt() to 0x2264748B.toInt()
                        }

                        bgPaint.color = bgColor
                        borderPaint.color = tintedColor(accentColor, 75)
                        canvas.drawRoundRect(bgRect, r, r, bgPaint)
                        canvas.drawRoundRect(bgRect, r, r, borderPaint)

                        iconPaint.color = accentColor
                        fillPaint.color = accentColor

                        val cx = w / 2f
                        val cy = h / 2f

                        when (tab) {
                            AppSettingsTab.TIMER -> {
                                val dialR = dpF(7.5f)
                                val dialCy = cy + dpF(1.2f)
                                canvas.drawCircle(cx, dialCy, dialR, iconPaint)
                                canvas.drawLine(cx, dialCy - dialR - dpF(2.5f), cx, dialCy - dialR, iconPaint)
                                canvas.drawLine(cx, dialCy, cx + dpF(3.5f), dialCy - dpF(3.5f), iconPaint)
                            }
                            AppSettingsTab.PLANNER -> {
                                path.reset()
                                val pw = dpF(5.5f)
                                val ph = dpF(7.5f)
                                val pRect = android.graphics.RectF(cx - pw, cy - ph, cx + pw, cy + ph)
                                canvas.drawRoundRect(pRect, dpF(2f), dpF(2f), iconPaint)
                                canvas.drawLine(cx - dpF(2.5f), cy - ph, cx + dpF(2.5f), cy - ph, iconPaint)
                                path.reset()
                                path.moveTo(cx - dpF(3f), cy - dpF(1.5f))
                                path.lineTo(cx - dpF(1f), cy + dpF(0.5f))
                                path.lineTo(cx + dpF(3f), cy - dpF(3.5f))
                                canvas.drawPath(path, iconPaint)
                                canvas.drawLine(cx - dpF(2.5f), cy + dpF(3.5f), cx + dpF(2.5f), cy + dpF(3.5f), iconPaint)
                            }
                            AppSettingsTab.ANALYTICS -> {
                                val baseY = cy + dpF(5.5f)
                                val barW = dpF(2.5f)
                                val r1 = android.graphics.RectF(cx - dpF(5.5f), baseY - dpF(5f), cx - dpF(5.5f) + barW, baseY)
                                canvas.drawRoundRect(r1, dpF(1f), dpF(1f), fillPaint)
                                val r2 = android.graphics.RectF(cx - dpF(1.25f), baseY - dpF(11f), cx - dpF(1.25f) + barW, baseY)
                                canvas.drawRoundRect(r2, dpF(1f), dpF(1f), fillPaint)
                                val r3 = android.graphics.RectF(cx + dpF(3f), baseY - dpF(8f), cx + dpF(3f) + barW, baseY)
                                canvas.drawRoundRect(r3, dpF(1f), dpF(1f), fillPaint)
                            }
                            AppSettingsTab.THEME -> {
                                val pr = dpF(7.5f)
                                canvas.drawCircle(cx, cy, pr, iconPaint)
                                fillPaint.color = 0xFFEC4899.toInt()
                                canvas.drawCircle(cx - dpF(3f), cy - dpF(2.5f), dpF(1.4f), fillPaint)
                                fillPaint.color = 0xFFF59E0B.toInt()
                                canvas.drawCircle(cx + dpF(2.8f), cy - dpF(2.8f), dpF(1.4f), fillPaint)
                                fillPaint.color = 0xFF38BDF8.toInt()
                                canvas.drawCircle(cx + dpF(3.2f), cy + dpF(2.2f), dpF(1.4f), fillPaint)
                                fillPaint.color = 0xFF10B981.toInt()
                                canvas.drawCircle(cx - dpF(2.5f), cy + dpF(3f), dpF(1.4f), fillPaint)
                            }
                            AppSettingsTab.CLOUD -> {
                                path.reset()
                                val cw = dpF(7f)
                                val ch = dpF(4.5f)
                                val cloudCy = cy - dpF(1.5f)
                                val cloudRect = android.graphics.RectF(cx - cw, cloudCy - ch, cx + cw, cloudCy + ch)
                                canvas.drawRoundRect(cloudRect, ch, ch, iconPaint)
                                val arrowY = cy + dpF(4.5f)
                                canvas.drawLine(cx - dpF(4.5f), arrowY, cx + dpF(4.5f), arrowY, iconPaint)
                                canvas.drawLine(cx + dpF(2.5f), arrowY - dpF(2f), cx + dpF(4.5f), arrowY, iconPaint)
                            }
                            AppSettingsTab.DEVELOPER -> {
                                path.reset()
                                path.moveTo(cx - dpF(3f), cy - dpF(4.5f))
                                path.lineTo(cx - dpF(6.5f), cy)
                                path.lineTo(cx - dpF(3f), cy + dpF(4.5f))
                                canvas.drawPath(path, iconPaint)

                                canvas.drawLine(cx - dpF(1f), cy + dpF(5f), cx + dpF(1f), cy - dpF(5f), iconPaint)

                                path.reset()
                                path.moveTo(cx + dpF(3f), cy - dpF(4.5f))
                                path.lineTo(cx + dpF(6.5f), cy)
                                path.lineTo(cx + dpF(3f), cy + dpF(4.5f))
                                canvas.drawPath(path, iconPaint)
                            }
                            AppSettingsTab.PROFILE -> {
                                val tr = dpF(3.8f)
                                canvas.drawCircle(cx, cy - dpF(2.6f), tr, iconPaint)
                                path.reset()
                                path.moveTo(cx - dpF(6f), cy + dpF(7f))
                                path.quadTo(cx, cy + dpF(2f), cx + dpF(6f), cy + dpF(7f))
                                canvas.drawPath(path, iconPaint)
                            }
                            AppSettingsTab.COMMUNITY -> {
                                val tr1 = dpF(3f)
                                canvas.drawCircle(cx - dpF(3.5f), cy - dpF(2.6f), tr1, iconPaint)
                                path.reset()
                                path.moveTo(cx - dpF(7.5f), cy + dpF(6f))
                                path.quadTo(cx - dpF(3.5f), cy + dpF(2f), cx + dpF(0.5f), cy + dpF(6f))
                                canvas.drawPath(path, iconPaint)

                                val tr2 = dpF(2.8f)
                                canvas.drawCircle(cx + dpF(4f), cy - dpF(3.5f), tr2, iconPaint)
                                path.reset()
                                path.moveTo(cx + dpF(0.5f), cy + dpF(5.5f))
                                path.quadTo(cx + dpF(4f), cy + dpF(1.5f), cx + dpF(7.5f), cy + dpF(5.5f))
                                canvas.drawPath(path, iconPaint)
                            }
                            else -> {
                                val ir = dpF(7.5f)
                                canvas.drawCircle(cx, cy, ir, iconPaint)
                                canvas.drawCircle(cx, cy - dpF(3.2f), dpF(1.2f), fillPaint)
                                canvas.drawLine(cx, cy - dpF(0.8f), cx, cy + dpF(3.8f), iconPaint)
                            }
                        }
                    }
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                }
            }

            fun createCustomDonutIcon(): View {
                return object : View(this) {
                    private val trackPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dp(4).toFloat()
                    }
                    private val arcPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dp(4).toFloat()
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    private val arcRect = android.graphics.RectF()

                    override fun onDraw(canvas: android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val pad = dp(3).toFloat()
                        arcRect.set(pad, pad, width.toFloat() - pad, height.toFloat() - pad)
                        trackPaint.color = tintedColor(themeCoordinator.primaryColor, 50)
                        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)
                        arcPaint.color = themeCoordinator.primaryColor
                        canvas.drawArc(arcRect, -90f, 220f, false, arcPaint)
                    }
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                }
            }

            fun createSettingsRowWithView(customIconView: View, title: String, subtitle: String, trailingView: View? = null): LinearLayout {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
                }
                row.addView(customIconView)
                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                textCol.addView(TextView(this).apply {
                    text = title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                textCol.addView(TextView(this).apply {
                    text = subtitle
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                row.addView(textCol)
                if (trailingView != null) {
                    row.addView(trailingView)
                }
                return row
            }

            // ==========================================
            // 1. SETTINGS HUB DASHBOARD (Hub & Spoke)
            // ==========================================
            if (currentSettingsTab == AppSettingsTab.HUB) {

                // --- TOP USER PROFILE CARD ---
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = ProfileManager.getEffectiveDisplayName(this).ifBlank { AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner" }
                val userEmail = AuthManager.getUserEmail(this)
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                val profileTopCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground(24f)
                    val outValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    foreground = getDrawable(outValue.resourceId)
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        currentSettingsTab = AppSettingsTab.PROFILE
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }

                val profileRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Avatar bubble with local photo support and remote fallback
                val avatarFrame = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                }
                val avatarCircle = TextView(this).apply {
                    text = avatarInitials
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(LocalAvatarManager.getGoogleAvatarColor(userName))
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER)
                }
                avatarFrame.addView(avatarCircle)

                val avatarImg = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(2), themeCoordinator.primaryColor)
                    }
                    visibility = View.GONE
                }
                avatarFrame.addView(avatarImg)

                val customAvatarBitmap = LocalAvatarManager.getCircularAvatarBitmap(this, dp(48))
                if (customAvatarBitmap != null) {
                    avatarImg.setImageBitmap(customAvatarBitmap)
                    avatarImg.visibility = View.VISIBLE
                    avatarCircle.visibility = View.GONE
                } else {
                    val remoteUrl = ProfileManager.getEffectiveAvatarUrl(this)
                    if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val bitmap = LocalAvatarManager.getCircularBitmapFromUrl(this@SettingsPanelBuilder.host, remoteUrl, dp(48))
                            if (bitmap != null) {
                                LocalAvatarManager.downloadAndSaveRemoteAvatar(this@SettingsPanelBuilder.host, remoteUrl)
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    avatarImg.setImageBitmap(bitmap)
                                    avatarImg.visibility = View.VISIBLE
                                    avatarCircle.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                profileRow.addView(avatarFrame)

                val profileTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(14), 0, dp(10), 0)
                }
                profileTextCol.addView(TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val authStatusRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(2), 0, 0)
                }
                val statusDot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                        setMargins(0, 0, dp(6), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    }
                }
                authStatusRow.addView(statusDot)
                authStatusRow.addView(TextView(this).apply {
                    text = if (isGoogleAuth) "${userEmail ?: "Connected"} • Cloud Sync Active" else "Guest / Offline Mode — Tap to Sign In"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                    textSize = 12f
                })
                profileTextCol.addView(authStatusRow)
                profileRow.addView(profileTextCol)

                val editProfileBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "Manage ›" else "Sign In ›"
                    setTextColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#38BDF8"))
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) themeCoordinator.primaryColor else Color.parseColor("#38BDF8"), 40), 14f)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                }
                profileRow.addView(editProfileBadge)
                profileTopCard.addView(profileRow)
                layout.addView(profileTopCard)

                // --- CATEGORIZED NAVIGATION ROW BUILDER ---
                fun addHubRowToCard(card: LinearLayout, title: String, subtitle: String, targetTab: AppSettingsTab) {
                    val chevron = TextView(this).apply {
                        text = "›"
                        textSize = 22f
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.35f
                        setPadding(dp(8), 0, 0, 0)
                    }
                    val iconView = createCategoryVectorIcon(targetTab)
                    val row = createSettingsRowWithView(iconView, title, subtitle, chevron)
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    row.setBackgroundResource(outVal.resourceId)
                    row.isClickable = true
                    row.isFocusable = true
                    row.setOnClickListener {
                        currentSettingsTab = targetTab
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                    card.addView(row)
                }

                val focusMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                val breakMins = sharedPrefs.safeLong("break_interval_minutes", 5L)
                val timerSub = "Focus: ${focusMins}m  •  Break: ${breakMins}m  •  ${timerMode.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} Mode"

                val dailyGoalSecs = StatsEngine.getDailyGoalSecs(sharedPrefs)
                val plannerSub = "Target: ${formatGoalLabel(dailyGoalSecs)} • Goal Reminders & Habits"
                val analyticsSub = "Heatmap, Subject Breakdown & Charts"

                val themeSub = "${themeCoordinator.activeBgMode.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} Palette • ${if (themeCoordinator.isGlassStyle()) "Glass" else "Standard"} Style"

                val cloudSub = if (isGoogleAuth) "Connected: $userEmail • Auto Backup Active" else "Sign In, Backup & Cloud Restore"

                // 1. UNIFIED PREFERENCES CARD GROUP
                layout.addView(createSectionLabel("PREFERENCES"))
                val prefsCard = createSettingsCard()
                addHubRowToCard(prefsCard, "Timer & Focus", timerSub, AppSettingsTab.TIMER)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Planner & Goals", plannerSub, AppSettingsTab.PLANNER)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Insights & Stats", analyticsSub, AppSettingsTab.ANALYTICS)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Community & Leaderboard", "Public rankings, live presence & room alerts", AppSettingsTab.COMMUNITY)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Theme & Appearance", themeSub, AppSettingsTab.THEME)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Cloud, Sync & Backups", cloudSub, AppSettingsTab.CLOUD)

                if (BuildConfig.DEBUG && isDevModeUnlocked) {
                    prefsCard.addView(createDivider())
                    addHubRowToCard(prefsCard, "Developer Tools", "Diagnostic tools & debug settings", AppSettingsTab.DEVELOPER)
                }
                layout.addView(prefsCard)

                // 2. UNIFIED COMPACT "ABOUT & LEGAL" ACCORDION CARD
                layout.addView(createSectionLabel("ABOUT & LEGAL"))
                val aboutLegalCard = createSettingsCard()

                var isAboutExpanded = sharedPrefs.getBoolean("about_legal_expanded", false)

                val expandChevron = TextView(this).apply {
                    text = if (isAboutExpanded) "⌄" else "›"
                    textSize = 20f
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    setPadding(dp(8), 0, 0, 0)
                }

                val expandedContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (isAboutExpanded) View.VISIBLE else View.GONE
                }

                val aboutHeaderIcon = createCategoryVectorIcon(AppSettingsTab.PROFILE)
                val headerRowToggle = createSettingsRowWithView(
                    aboutHeaderIcon,
                    "About, Support & Policies",
                    if (isAboutExpanded) "Tap to collapse" else "StudyTimer v${currentVersionName()} • Guide, feedback & legal terms",
                    expandChevron
                )
                headerRowToggle.isClickable = true
                headerRowToggle.isFocusable = true
                headerRowToggle.setOnClickListener {
                    isAboutExpanded = !isAboutExpanded
                    sharedPrefs.edit().putBoolean("about_legal_expanded", isAboutExpanded).apply()
                    expandChevron.text = if (isAboutExpanded) "⌄" else "›"
                    expandedContent.visibility = if (isAboutExpanded) View.VISIBLE else View.GONE
                }
                aboutLegalCard.addView(headerRowToggle)

                // Items inside expandedContent
                expandedContent.addView(createDivider())

                val guideRow = createSettingsRow("", "How to Use & App Guide", "User manual, timer modes & feature walkthrough")
                guideRow.setOnClickListener {
                    showAppGuideDialog()
                }
                expandedContent.addView(guideRow)
                expandedContent.addView(createDivider())

                val feedbackRow = createSettingsRow("", "Feedback & Bug Reports", "Send feature suggestions, questions or issues")
                feedbackRow.setOnClickListener {
                    showFeedbackReportDialog()
                }
                expandedContent.addView(feedbackRow)
                expandedContent.addView(createDivider())

                val updateChip = if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
                    TextView(this).apply {
                        text = "Check"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 45), 12f)
                        setPadding(dp(10), dp(5), dp(10), dp(5))
                    }
                } else null

                val versionRow = createSettingsRow(
                    "",
                    "StudyTimer v${currentVersionName()}",
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) "Check for the latest release & changelog" else "StudyTimer for Android",
                    updateChip
                )
                versionRow.setOnClickListener {
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
                        checkForUpdates(manual = true)
                    } else {
                        AppToast.show(this, "Version v${currentVersionName()} is up to date")
                    }
                }
                expandedContent.addView(versionRow)
                expandedContent.addView(createDivider())

                val privacyRow = createSettingsRow("", "Privacy Policy", "Read our data collection, analytics & privacy practices")
                privacyRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/privacy.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening privacy policy...")
                    }
                }
                expandedContent.addView(privacyRow)
                expandedContent.addView(createDivider())

                val termsRow = createSettingsRow("", "Terms of Service", "Review our terms, usage guidelines & licensing")
                termsRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/terms.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening terms of service...")
                    }
                }
                expandedContent.addView(termsRow)
                expandedContent.addView(createDivider())

                val deleteWebRow = createSettingsRow("", "Account Deletion Web Portal", "Request permanent deletion of data online (Play Store policy)")
                deleteWebRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/delete-account.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening account deletion portal...")
                    }
                }
                expandedContent.addView(deleteWebRow)

                aboutLegalCard.addView(expandedContent)
                layout.addView(aboutLegalCard)

                // Developer Credits (Main Settings Hub Only - Natural End of Scroll)
                val creditsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp(24), 0, 0)
                    }
                }
                var devClickCount = 0
                val developedByText = TextView(this).apply {
                    text = getString(R.string.developed_by)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        if (BuildConfig.DEBUG) {
                            devClickCount++
                            if (devClickCount >= 5) {
                                devClickCount = 0
                                isDevModeUnlocked = true
                                AppToast.show(context, "Developer Mode Activated")
                                currentSettingsTab = AppSettingsTab.DEVELOPER
                                navigateToPanel(AppPanel.SETTINGS)
                            }
                        }
                    }
                }
                creditsContainer.addView(developedByText)
                layout.addView(creditsContainer)
            }

            // ==========================================
            // 2. USER PROFILE MANAGEMENT SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.PROFILE) {
                layout.addView(createSectionLabel("STUDENT IDENTITY & PROFILE"))
                val profileCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = ProfileManager.getEffectiveDisplayName(this).ifBlank { AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner" }
                val userEmail = AuthManager.getUserEmail(this) ?: "Offline / Not Signed In"
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                val profileContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(20), dp(24), dp(20), dp(20))
                }

                // Centered Avatar Frame with Camera Edit Badge
                val avatarWrapper = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, dp(14))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        host.pickProfileAvatar()
                    }
                }

                val avatarBigCircle = TextView(this).apply {
                    text = avatarInitials
                    textSize = 34f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(LocalAvatarManager.getGoogleAvatarColor(userName))
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER)
                }
                avatarWrapper.addView(avatarBigCircle)

                val avatarLargeImg = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(2), themeCoordinator.primaryColor)
                    }
                    visibility = View.GONE
                }
                avatarWrapper.addView(avatarLargeImg)

                val customAvatarLarge = LocalAvatarManager.getCircularAvatarBitmap(this, dp(88))
                if (customAvatarLarge != null) {
                    avatarLargeImg.setImageBitmap(customAvatarLarge)
                    avatarLargeImg.visibility = View.VISIBLE
                    avatarBigCircle.visibility = View.GONE
                } else {
                    val remoteUrl = ProfileManager.getEffectiveAvatarUrl(this)
                    if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val bitmap = LocalAvatarManager.getCircularBitmapFromUrl(this@SettingsPanelBuilder.host, remoteUrl, dp(88))
                            if (bitmap != null) {
                                LocalAvatarManager.downloadAndSaveRemoteAvatar(this@SettingsPanelBuilder.host, remoteUrl)
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    avatarLargeImg.setImageBitmap(bitmap)
                                    avatarLargeImg.visibility = View.VISIBLE
                                    avatarBigCircle.visibility = View.GONE
                                }
                            }
                        }
                    }
                }

                // Edit Camera Badge
                val cameraBadge = TextView(this).apply {
                    text = "📷"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (themeCoordinator.isDarkMode()) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
                        setStroke(dp(2), themeCoordinator.bgColor)
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END)
                }
                avatarWrapper.addView(cameraBadge)
                profileContent.addView(avatarWrapper)

                // User Display Name
                val nameText = TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 19f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                profileContent.addView(nameText)

                // User Email
                val emailText = TextView(this).apply {
                    text = userEmail
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(3), 0, dp(8))
                }
                profileContent.addView(emailText)

                // Account Badge Chip
                val accountBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "✓ Google Account" else "👤 Guest Session"
                    setTextColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"), 35), 14f)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                }
                profileContent.addView(accountBadge)

                if (!isGoogleAuth) {
                    val signInCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#4285F4"), 40), 16f)
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dp(4), 0, dp(10))
                        }
                    }
                    signInCard.addView(TextView(this).apply {
                        text = "Sign In with Google"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    signInCard.addView(TextView(this).apply {
                        text = "Sign in to save your study logs to cloud, preserve your streaks, and join the global student leaderboard."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.7f
                        textSize = 12.5f
                        setPadding(0, dp(4), 0, dp(14))
                    })
                    val googleBtn = Button(this).apply {
                        text = "Sign in with Google"
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(Color.parseColor("#4285F4"))
                        }
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                        setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            startActivity(Intent(this@with, LoginActivity::class.java))
                        }
                    }
                    signInCard.addView(googleBtn)
                    profileContent.addView(signInCard)
                } else {
                    val currentProfile = ProfileManager.getProfile(this)

                    // --- MODERATION STATUS BANNER ---
                    when (currentProfile.moderationStatus) {
                        ModerationStatus.PENDING_APPROVAL -> {
                            val pendingCard = LinearLayout(this).apply {
                                orientation = LinearLayout.VERTICAL
                                background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#F59E0B"), 45), 14f)
                                setPadding(dp(14), dp(12), dp(14), dp(12))
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 0, 0, dp(14))
                                }
                            }
                            val reqName = currentProfile.pendingDisplayName
                            val hasNameReview = !reqName.isNullOrBlank() && reqName != currentProfile.displayName
                            val bannerTitle = if (hasNameReview) "⏳ Display Name Pending Approval" else "⏳ Profile Edit Under Review"
                            val bannerMsg = if (hasNameReview) {
                                "Your requested name \"$reqName\" is being reviewed by moderators. Leaderboards will display \"${currentProfile.displayName}\" until approved."
                            } else {
                                "Your profile changes are being reviewed by moderators. Public leaderboards will show your currently approved details until reviewed."
                            }
                            pendingCard.addView(TextView(this).apply {
                                text = bannerTitle
                                setTextColor(Color.parseColor("#F59E0B"))
                                textSize = 13f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            })
                            pendingCard.addView(TextView(this).apply {
                                text = bannerMsg
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.75f
                                textSize = 11.5f
                                setPadding(0, dp(4), 0, 0)
                            })
                            profileContent.addView(pendingCard)
                        }
                        ModerationStatus.REJECTED -> {
                            val rejectedCard = LinearLayout(this).apply {
                                orientation = LinearLayout.VERTICAL
                                background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 45), 14f)
                                setPadding(dp(14), dp(12), dp(14), dp(12))
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 0, 0, dp(14))
                                }
                            }
                            rejectedCard.addView(TextView(this).apply {
                                text = "❌ Profile Edit Rejected by Moderators"
                                setTextColor(Color.parseColor("#EF4444"))
                                textSize = 13f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            })
                            rejectedCard.addView(TextView(this).apply {
                                text = "Your previous requested name or photo was rejected for not meeting community standards. Your profile has been reset to safe defaults. Please choose a respectful display name below."
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.85f
                                textSize = 11.5f
                                setPadding(0, dp(4), 0, 0)
                            })
                            profileContent.addView(rejectedCard)
                        }
                        else -> {
                            // Approved status
                        }
                    }

                    var selectedAvatarId = ""

                    val draftName = ProfileManager.getDraftName(this)
                    val draftBio = ProfileManager.getDraftBio(this)
                    val draftExam = ProfileManager.getDraftExam(this)

                    // --- DISPLAY NAME INPUT ---
                    val nameLabel = TextView(this).apply {
                        text = "DISPLAY NAME (PUBLIC)"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(4))
                    }
                    profileContent.addView(nameLabel)

                    val editNameField = EditText(this).apply {
                        hint = "e.g. Alex_Studies"
                        setText(draftName ?: (currentProfile.pendingDisplayName ?: currentProfile.displayName))
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 14f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                        setPadding(dp(14), dp(11), dp(14), dp(11))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(12))
                        }
                        setSingleLine(true)
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                ProfileManager.saveDraftForm(this@with, s?.toString(), null, null)
                            }
                        })
                    }
                    profileContent.addView(editNameField)

                    // Save draft on avatar click so any current focus text is saved
                    avatarWrapper.setOnClickListener {
                        ProfileManager.saveDraftForm(
                            this@with,
                            editNameField.text.toString(),
                            null,
                            null
                        )
                        host.pickProfileAvatar()
                    }

                    // --- TARGET EXAM / GOAL SELECTOR ---
                    val examLabel = TextView(this).apply {
                        text = "TARGET EXAM / FOCUS TRACK"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(6))
                    }
                    profileContent.addView(examLabel)

                    val examTracks = listOf("JEE / NEET", "UPSC / Govt", "Board Exams", "College / Uni", "Self-Study", "Programming")
                    var selectedExam = draftExam ?: currentProfile.targetExam.ifBlank { "Self-Study" }

                    val chipsScroll = android.widget.HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(12))
                        }
                    }
                    val chipsLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val chipViews = mutableListOf<TextView>()
                    for (track in examTracks) {
                        val isSelected = track.equals(selectedExam, ignoreCase = true)
                        val chip = TextView(this).apply {
                            text = track
                            textSize = 12f
                            typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                            setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                            background = if (isSelected) {
                                themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                            } else {
                                themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                            }
                            setPadding(dp(12), dp(6), dp(12), dp(6))
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 0, dp(8), 0)
                            }
                            setOnClickListener {
                                selectedExam = track
                                ProfileManager.saveDraftForm(this@with, null, null, track)
                                for (c in chipViews) {
                                    val sel = c.text == track
                                    c.typeface = themeCoordinator.getAppTypeface(isBold = sel)
                                    c.setTextColor(if (sel) Color.WHITE else themeCoordinator.textColor)
                                    c.background = if (sel) {
                                        themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                                    } else {
                                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                                    }
                                }
                            }
                        }
                        chipViews.add(chip)
                        chipsLayout.addView(chip)
                    }
                    chipsScroll.addView(chipsLayout)
                    profileContent.addView(chipsScroll)

                    // --- STUDENT BIO / MOTTO ---
                    val bioLabel = TextView(this).apply {
                        text = "STUDENT BIO & MOTTO"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(4))
                    }
                    profileContent.addView(bioLabel)

                    val editBioField = EditText(this).apply {
                        hint = "e.g. Focused on daily consistency • Consistency is key"
                        setText(draftBio ?: currentProfile.bio)
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 13f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(14))
                        }
                        maxLines = 2
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                ProfileManager.saveDraftForm(this@with, null, s?.toString(), null)
                            }
                        })
                    }
                    profileContent.addView(editBioField)

                    // --- SAVE PROFILE ACTION BUTTON ---
                    val saveProfileBtn = Button(this).apply {
                        text = "Save Profile Changes"
                        setTextColor(Color.WHITE)
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                            setMargins(0, dp(4), 0, 0)
                        }
                        setOnClickListener {
                            val inputName = editNameField.text.toString().trim()
                            val inputBio = editBioField.text.toString().trim()

                            if (inputName.isBlank()) {
                                AppToast.show(this@with, "Please enter a valid display name.")
                                return@setOnClickListener
                            }

                            // Run local profanity pre-check
                            val check = ProfanityFilter.checkName(inputName)
                            if (!check.isClean) {
                                DialogFactory.showInfoDialog(
                                    context = this@with,
                                    themeCoordinator = themeCoordinator,
                                    title = "Inappropriate Name",
                                    message = check.reason ?: "Display name contains prohibited words."
                                )
                                return@setOnClickListener
                            }

                            val parentSaveBtn = this@apply

                            // Themed Verification Confirmation Dialog
                            val confirmDialog = Dialog(this@with)
                            confirmDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

                            val dialogRoot = LinearLayout(this@with).apply {
                                orientation = LinearLayout.VERTICAL
                                background = themeCoordinator.createDialogBackground(26f)
                                setPadding(dp(22), dp(22), dp(22), dp(20))
                            }

                            // Title & Icon
                            val titleView = TextView(this@with).apply {
                                text = "🛡️ Profile Verification"
                                setTextColor(themeCoordinator.textColor)
                                textSize = 17.5f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                setPadding(0, 0, 0, dp(4))
                            }
                            dialogRoot.addView(titleView)

                            val subtitleView = TextView(this@with).apply {
                                text = "Submit details to moderators for live leaderboard"
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.65f
                                textSize = 12.5f
                                setPadding(0, 0, 0, dp(14))
                            }
                            dialogRoot.addView(subtitleView)

                            // Preview Card
                            val previewBox = LinearLayout(this@with).apply {
                                orientation = LinearLayout.VERTICAL
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 22), 14f)
                                setPadding(dp(14), dp(12), dp(14), dp(12))
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 0, 0, dp(14))
                                }
                            }

                            val rowName = TextView(this@with).apply {
                                text = "👤 Display Name: $inputName"
                                setTextColor(themeCoordinator.textColor)
                                textSize = 13f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                setPadding(0, 0, 0, dp(4))
                            }
                            previewBox.addView(rowName)

                            val rowExam = TextView(this@with).apply {
                                text = "Focus Track: $selectedExam"
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.85f
                                textSize = 12f
                                setPadding(0, 0, 0, dp(4))
                            }
                            previewBox.addView(rowExam)

                            if (inputBio.isNotBlank()) {
                                val rowBio = TextView(this@with).apply {
                                    text = "💬 Motto: \"$inputBio\""
                                    setTextColor(themeCoordinator.textColor)
                                    alpha = 0.85f
                                    textSize = 12f
                                }
                                previewBox.addView(rowBio)
                            }
                            dialogRoot.addView(previewBox)

                            val explanationText = TextView(this@with).apply {
                                text = "Your profile changes will be checked to maintain a safe, spam-free study community."
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.65f
                                textSize = 11.5f
                                setPadding(0, 0, 0, dp(18))
                            }
                            dialogRoot.addView(explanationText)

                            // Button Row
                            val btnRow = LinearLayout(this@with).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.END
                            }

                            val cancelBtn = Button(this@with).apply {
                                text = "Edit Details"
                                setTextColor(tintedColor(themeCoordinator.textColor, 180))
                                textSize = 12f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 12f)
                                setPadding(dp(14), dp(8), dp(14), dp(8))
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)).apply {
                                    setMargins(0, 0, dp(10), 0)
                                }
                                setOnClickListener {
                                    confirmDialog.dismiss()
                                }
                            }
                            btnRow.addView(cancelBtn)

                            val confirmSubmitBtn = Button(this@with).apply {
                                text = "✓ Submit Profile"
                                setTextColor(Color.WHITE)
                                textSize = 12.5f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                background = GradientDrawable().apply {
                                    cornerRadius = dp(12).toFloat()
                                    setColor(Color.parseColor("#10B981")) // Emerald Green CTA
                                }
                                setPadding(dp(18), dp(8), dp(18), dp(8))
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42))
                                setOnClickListener {
                                    confirmDialog.dismiss()
                                    parentSaveBtn.isEnabled = false
                                    parentSaveBtn.text = "Submitting..."

                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        val result = ProfileSyncService.submitProfile(
                                            context = this@with,
                                            displayName = inputName,
                                            bio = inputBio,
                                            targetExam = selectedExam,
                                            dailyGoalMinutes = currentProfile.dailyGoalMinutes,
                                            avatarPresetId = selectedAvatarId,
                                            avatarUrl = currentProfile.avatarUrl
                                        )

                                        parentSaveBtn.isEnabled = true
                                        parentSaveBtn.text = "Save Profile Changes"

                                        when (result) {
                                            is ProfileSyncService.SubmissionResult.Success -> {
                                                ProfileManager.clearDraftForm(this@with)
                                                AppToast.show(this@with, result.message, Toast.LENGTH_LONG)
                                                navigateToPanel(AppPanel.SETTINGS)
                                            }
                                            is ProfileSyncService.SubmissionResult.RejectedLocally -> {
                                                DialogFactory.showInfoDialog(
                                                    context = this@with,
                                                    themeCoordinator = themeCoordinator,
                                                    title = "Name Not Allowed",
                                                    message = result.reason
                                                )
                                            }
                                            is ProfileSyncService.SubmissionResult.NetworkError -> {
                                                AppToast.show(this@with, result.message, Toast.LENGTH_LONG)
                                                navigateToPanel(AppPanel.SETTINGS)
                                            }
                                        }
                                    }
                                }
                            }
                            btnRow.addView(confirmSubmitBtn)
                            dialogRoot.addView(btnRow)

                            confirmDialog.setContentView(dialogRoot)
                            confirmDialog.window?.apply {
                                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                                setGravity(Gravity.CENTER)
                                setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                            confirmDialog.show()
                        }
                    }
                    profileContent.addView(saveProfileBtn)
                }

                profileCard.addView(profileContent)
                layout.addView(profileCard)

                // --- ACCOUNT ACTIONS CARD ---
                layout.addView(createSectionLabel("ACCOUNT & DATA"))
                val accountActionsCard = createSettingsCard().apply {
                    val actionsLayout = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }

                    // Navigation to Cloud & Backup settings (single source of truth)
                    val cloudNavRow = LinearLayout(this@with).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(6), 0, dp(12))
                        isClickable = true
                        isFocusable = true
                        val outVal = android.util.TypedValue()
                        theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                        setBackgroundResource(outVal.resourceId)
                        setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            currentSettingsTab = AppSettingsTab.CLOUD
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                    }
                    val syncIcon = TextView(this@with).apply {
                        text = "☁"
                        textSize = 20f
                        setTextColor(themeCoordinator.primaryColor)
                        setPadding(0, 0, dp(12), 0)
                    }
                    cloudNavRow.addView(syncIcon)

                    val syncCol = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    syncCol.addView(TextView(this@with).apply {
                        text = "Cloud & Backup Settings"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 14.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })
                    syncCol.addView(TextView(this@with).apply {
                        text = if (isGoogleAuth) {
                            "Connected • Tap to manage backups, export & import"
                        } else {
                            "Offline • Tap to sign in and back up study sessions"
                        }
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 11.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                    cloudNavRow.addView(syncCol)

                    val chevron = TextView(this@with).apply {
                        text = "Manage ›"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 12f)
                        setPadding(dp(10), dp(5), dp(10), dp(5))
                    }
                    cloudNavRow.addView(chevron)
                    actionsLayout.addView(cloudNavRow)

                    if (isGoogleAuth) {
                        actionsLayout.addView(createDivider())

                        val signOutBtn = Button(this@with).apply {
                            text = "Sign Out"
                            setTextColor(Color.parseColor("#EF4444"))
                            textSize = 13.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 40), 12f)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                                setMargins(0, dp(10), 0, dp(8))
                            }
                            setOnClickListener {
                                host.showSignOutConfirmDialog()
                            }
                        }
                        actionsLayout.addView(signOutBtn)
                    }

                    actionsLayout.addView(createDivider())

                    // Delete Account Link
                    val deleteAccountLink = TextView(this@with).apply {
                        text = "Delete Account & Clear Cloud Data"
                        setTextColor(Color.parseColor("#EF4444"))
                        alpha = 0.85f
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(12), 0, dp(4))
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            showDeleteAccountDialog()
                        }
                    }
                    actionsLayout.addView(deleteAccountLink)

                    addView(actionsLayout)
                }
                layout.addView(accountActionsCard)
            }

            // ==========================================
            // 2b. COMMUNITY & LEADERBOARD SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.COMMUNITY) {
                // CARD 1: PUBLIC PROFILE & ROOMS
                layout.addView(createSectionLabel("PUBLIC PROFILE & ROOMS"))
                val publicRoomsCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = ProfileManager.getEffectiveDisplayName(this).ifBlank { AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner" }
                val currentProfile = ProfileManager.getProfile(this)
                val targetExam = currentProfile.targetExam.ifBlank { "Self-Study" }
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                val previewContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(18), dp(18), dp(18))
                }

                val previewRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Avatar Frame (Read-Only: No camera badge, No click-to-edit)
                val previewAvatarWrapper = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                }

                val previewAvatarCircle = TextView(this).apply {
                    text = avatarInitials
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(LocalAvatarManager.getGoogleAvatarColor(userName))
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER)
                }
                previewAvatarWrapper.addView(previewAvatarCircle)

                val previewAvatarImg = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(2), themeCoordinator.primaryColor)
                    }
                    visibility = View.GONE
                }
                previewAvatarWrapper.addView(previewAvatarImg)

                val customAvatar = LocalAvatarManager.getCircularAvatarBitmap(this, dp(64))
                if (customAvatar != null) {
                    previewAvatarImg.setImageBitmap(customAvatar)
                    previewAvatarImg.visibility = View.VISIBLE
                    previewAvatarCircle.visibility = View.GONE
                } else {
                    val remoteUrl = ProfileManager.getEffectiveAvatarUrl(this)
                    if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val bitmap = LocalAvatarManager.getCircularBitmapFromUrl(this@SettingsPanelBuilder.host, remoteUrl, dp(64))
                            if (bitmap != null) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    previewAvatarImg.setImageBitmap(bitmap)
                                    previewAvatarImg.visibility = View.VISIBLE
                                    previewAvatarCircle.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                previewRow.addView(previewAvatarWrapper)

                // Info Column: Name + Badges + Bio
                val infoCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                infoCol.addView(TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 17f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val badgesRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(4), 0, dp(4))
                }
                badgesRow.addView(TextView(this).apply {
                    text = if (isGoogleAuth) "✓ Google Account" else "👤 Guest"
                    setTextColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    textSize = 10.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"), 35), 10f)
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = dp(6)
                    }
                })
                badgesRow.addView(TextView(this).apply {
                    text = targetExam
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 10.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                })
                infoCol.addView(badgesRow)

                val bioStr = currentProfile.bio.trim()
                if (bioStr.isNotEmpty()) {
                    infoCol.addView(TextView(this).apply {
                        text = "“$bioStr”"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        setPadding(0, dp(2), 0, 0)
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                }

                previewRow.addView(infoCol)
                previewContent.addView(previewRow)

                previewContent.addView(createDivider().apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        setMargins(0, dp(14), 0, dp(10))
                    }
                })

                val previewFooter = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                previewFooter.addView(TextView(this).apply {
                    text = "This preview is visible to peers in rooms."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 11.5f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                val editInProfileBtn = TextView(this).apply {
                    text = "Edit Profile →"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentSettingsTab = AppSettingsTab.PROFILE
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                previewFooter.addView(editInProfileBtn)
                previewContent.addView(previewFooter)

                publicRoomsCard.addView(previewContent)
                publicRoomsCard.addView(createDivider())

                val isRoomSound = sharedPrefs.getBoolean("study_room_sound_enabled", true)
                val roomSoundSwitch = SwitchMaterial(this).apply {
                    isChecked = isRoomSound
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("study_room_sound_enabled", isChecked).apply()
                    }
                }
                publicRoomsCard.addView(createSettingsRow(
                    "",
                    "Room Audio Alerts",
                    "Play chimes when students join or complete milestone focus sessions",
                    roomSoundSwitch
                ))
                publicRoomsCard.addView(createDivider())

                val isRoomPresence = sharedPrefs.getBoolean("study_room_auto_presence", true)
                val roomPresenceSwitch = SwitchMaterial(this).apply {
                    isChecked = isRoomPresence
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("study_room_auto_presence", isChecked).apply()
                    }
                }
                publicRoomsCard.addView(createSettingsRow(
                    "",
                    "Study Room Presence",
                    "Show your avatar and study tag in active study rooms when connected",
                    roomPresenceSwitch
                ))
                publicRoomsCard.addView(createDivider())

                val privatePrivacyRow = createSettingsRow(
                    "",
                    "Private Room Isolation",
                    "Private room codes and participants are shielded from the public directory. Your hours safely sync to your daily total."
                )
                publicRoomsCard.addView(privatePrivacyRow)
                layout.addView(publicRoomsCard)

                // CARD 2: LEADERBOARD & RANKINGS
                layout.addView(createSectionLabel("LEADERBOARD & RANKINGS"))
                val leaderboardSettingsCard = createSettingsCard()

                val isParticipating = LeaderboardManager.isParticipating(this)
                val isShareLive = LeaderboardManager.isLiveStatusSharingEnabled(this)

                var participateSwitchRef: SwitchMaterial? = null
                val leaderboardSubOptions = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (isParticipating) View.VISIBLE else View.GONE
                }

                fun promptTurnOffLeaderboard() {
                    DeveloperToolsHelper.showThemedConfirmDialog(
                        activity = host,
                        themeCoordinator = themeCoordinator,
                        title = "Pause Leaderboard Participation?",
                        message = "Turning this off stops your focus sessions from syncing to global student rankings and removes your live study presence.\n\nYour personal statistics, study logs, and streaks remain completely safe on your device.\n\nAre you sure you want to stop participating?",
                        confirmText = "Pause Participation",
                        isDestructive = true,
                        onCancel = {
                            participateSwitchRef?.isChecked = true
                        }
                    ) {
                        LeaderboardManager.setParticipating(this@with, false)
                        participateSwitchRef?.isChecked = false
                        leaderboardSubOptions.visibility = View.GONE
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            LeaderboardManager.removeTodayLeaderboardEntry(this@with)
                        }
                        AppToast.show(this@with, "Leaderboard participation paused")
                    }
                }

                fun resumeParticipation() {
                    LeaderboardManager.setParticipating(this@with, true)
                    participateSwitchRef?.isChecked = true
                    leaderboardSubOptions.visibility = View.VISIBLE
                    AppToast.show(this@with, "Participating in Leaderboard")
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val todayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val realSecs = LeaderboardManager.getRealTimerFocusSecondsForDate(this@with, todayKeyFmt).toInt()
                        LeaderboardManager.overrideLeaderboardDailyTotal(this@with, realSecs)
                    }
                }

                val partSwitch = SwitchMaterial(this).apply {
                    isChecked = isParticipating
                    setOnClickListener {
                        if (!isChecked) {
                            isChecked = true
                            promptTurnOffLeaderboard()
                        } else {
                            resumeParticipation()
                        }
                    }
                }
                participateSwitchRef = partSwitch

                val partRow = createSettingsRow(
                    "🏆",
                    "Participate in Leaderboard",
                    "Sync focus hours and appear on global rankings (On by default)",
                    partSwitch
                )
                partRow.setOnClickListener {
                    val cur = LeaderboardManager.isParticipating(this)
                    if (cur) {
                        promptTurnOffLeaderboard()
                    } else {
                        resumeParticipation()
                    }
                }
                leaderboardSettingsCard.addView(partRow)

                // Sub-options inside leaderboardSubOptions (Conditional: only visible when participating)
                leaderboardSubOptions.addView(createDivider())

                val liveSwitch = SwitchMaterial(this).apply {
                    isChecked = isShareLive
                    setOnClickListener {
                        val newState = isChecked
                        LeaderboardManager.setLiveStatusSharingEnabled(this@with, newState)
                        if (!newState) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                LeaderboardManager.updateStudyPresence(this@with, false)
                            }
                            AppToast.show(this@with, "Live study status hidden")
                        } else {
                            AppToast.show(this@with, "Live study status visible")
                        }
                    }
                }

                val liveRow = createSettingsRow(
                    "🟢",
                    "Share Live Study Status",
                    "Show green live indicator & active subject to others in study rooms & leaderboard",
                    liveSwitch
                )
                liveRow.setOnClickListener {
                    val cur = LeaderboardManager.isLiveStatusSharingEnabled(this)
                    val next = !cur
                    LeaderboardManager.setLiveStatusSharingEnabled(this, next)
                    liveSwitch.isChecked = next
                    if (!next) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            LeaderboardManager.updateStudyPresence(this@with, false)
                        }
                        AppToast.show(this@with, "Live study status hidden")
                    } else {
                        AppToast.show(this@with, "Live study status visible")
                    }
                }
                leaderboardSubOptions.addView(liveRow)
                leaderboardSubOptions.addView(createDivider())

                val examInfoRow = createSettingsRow(
                    "🎯",
                    "Leaderboard Focus Track",
                    "Your global ranking is categorized under \"$targetExam\" (configured in Profile Settings)"
                )
                leaderboardSubOptions.addView(examInfoRow)

                leaderboardSettingsCard.addView(leaderboardSubOptions)
                layout.addView(leaderboardSettingsCard)
            }

            // ==========================================
            // 3. TIMER & FOCUS CONTROLS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.TIMER) {
                // CARD 1: TIMER MODE & DURATIONS
                layout.addView(createSectionLabel("TIMER MODE & DURATIONS"))
                val timerModeCard = createSettingsCard()
                val isLecture = timerMode == "LECTURE"
                val isStopwatch = timerMode == "STOPWATCH"
                val isCountdown = timerMode == "COUNTDOWN"
                val isSubject = timerMode == "SUBJECT"

                fun modeRadio(selected: Boolean): View {
                    return View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                            setStroke(dp(2), if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        }
                    }
                }

                fun checkTimerRunningBeforeModeChange(): Boolean {
                    if (currentTimerState != TimerState.IDLE || accumulatedStudy > 0L || currentBreakSeconds > 0L) {
                        DeveloperToolsHelper.showThemedConfirmDialog(
                            activity = this@with,
                            themeCoordinator = themeCoordinator,
                            title = "⏱️ Timer Is Running",
                            message = "Timer mode can only be changed when no timer is running.\n\nPlease pause or stop your active timer before switching timer modes.",
                            confirmText = "OK",
                            isDestructive = false,
                            onCancel = {}
                        ) {}
                        return true
                    }
                    return false
                }

                val stopwatchRow = createSettingsRow("", getString(R.string.mode_stopwatch), getString(R.string.mode_stopwatch_sub), modeRadio(isStopwatch))
                stopwatchRow.setOnClickListener {
                    if (checkTimerRunningBeforeModeChange()) return@setOnClickListener
                    sharedPrefs.edit().putString("timer_mode", "STOPWATCH").putBoolean("lecture_mode_enabled", false).apply()
                    timerMode = "STOPWATCH"
                    if (currentTimerState == TimerState.IDLE) {
                        focusRemainingSecs = 0L
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(stopwatchRow)
                timerModeCard.addView(createDivider())

                val countdownRow = createSettingsRow("", getString(R.string.mode_pomodoro), getString(R.string.mode_pomodoro_sub), modeRadio(isCountdown))
                countdownRow.setOnClickListener {
                    if (checkTimerRunningBeforeModeChange()) return@setOnClickListener
                    val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                    val pomoSecs = pomoMins * 60L
                    sharedPrefs.edit()
                        .putString("timer_mode", "COUNTDOWN")
                        .putBoolean("lecture_mode_enabled", false)
                        .putLong("focus_countdown_secs", pomoSecs)
                        .apply()
                    timerMode = "COUNTDOWN"
                    if (currentTimerState == TimerState.IDLE) {
                        focusCountdownSecs = pomoSecs
                        focusRemainingSecs = pomoSecs
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(countdownRow)
                timerModeCard.addView(createDivider())

                val subjectRow = createSettingsRow("", "Subject Focus", "Pick a subject and track your study time", modeRadio(isSubject))
                subjectRow.setOnClickListener {
                    if (checkTimerRunningBeforeModeChange()) return@setOnClickListener
                    val pomoMins = sharedPrefs.safeLong("study_interval_minutes", 25L)
                    val pomoSecs = pomoMins * 60L
                    sharedPrefs.edit()
                        .putString("timer_mode", "SUBJECT")
                        .putBoolean("lecture_mode_enabled", false)
                        .putBoolean("show_subject_pie_chart", true)
                        .putLong("focus_countdown_secs", pomoSecs)
                        .apply()
                    timerMode = "SUBJECT"
                    if (currentTimerState == TimerState.IDLE) {
                        focusCountdownSecs = pomoSecs
                        focusRemainingSecs = pomoSecs
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }
                timerModeCard.addView(subjectRow)
                timerModeCard.addView(createDivider())

                val lectureRow = createSettingsRow("", "Class Schedule", "Follow your custom class timetable", modeRadio(isLecture))
                lectureRow.setOnClickListener {
                    if (checkTimerRunningBeforeModeChange()) return@setOnClickListener
                    val isConfigured = !sharedPrefs.getString("lecture_schedules_json", "").isNullOrEmpty() && sharedPrefs.getString("lecture_schedules_json", "[]") != "[]"
                    sharedPrefs.edit().putString("timer_mode", "LECTURE").putBoolean("lecture_mode_enabled", false).apply()
                    timerMode = "LECTURE"
                    if (isConfigured) {
                        navigateToPanel(AppPanel.FOCUS)
                    } else {
                        showLectureScheduleManagerDialog()
                    }
                }
                timerModeCard.addView(lectureRow)

                // If Pomodoro / Countdown mode is active, merge interval & customization settings into this card
                if (timerMode == "COUNTDOWN") {
                    timerModeCard.addView(createDivider())

                    // Quick Interval Presets Row
                    val presetsRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(10))
                    }
                    val presetPresets = listOf(
                        Triple("25/5 Standard", 25L, 5L),
                        Triple("50/10 Deep", 50L, 10L),
                        Triple("90/20 Flow", 90L, 20L)
                    )
                    for ((pName, pFocus, pBreak) in presetPresets) {
                        val curFocus = sharedPrefs.safeLong("study_interval_minutes", 25L)
                        val isSelected = curFocus == pFocus
                        val chip = TextView(this).apply {
                            text = pName
                            textSize = 12f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                            background = if (isSelected) {
                                GradientDrawable().apply {
                                    cornerRadius = dp(14).toFloat()
                                    setColor(themeCoordinator.primaryColor)
                                }
                            } else {
                                themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
                            }
                            setPadding(dp(8), dp(8), dp(8), dp(8))
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                setMargins(dp(3), 0, dp(3), 0)
                            }
                            gravity = Gravity.CENTER
                            setOnClickListener {
                                sharedPrefs.edit()
                                    .putLong("study_interval_minutes", pFocus)
                                    .putLong("focus_countdown_secs", pFocus * 60L)
                                    .putLong("break_interval_minutes", pBreak)
                                    .putLong("break_countdown_secs", pBreak * 60L)
                                    .apply()
                                if (currentTimerState == TimerState.IDLE) {
                                    focusCountdownSecs = pFocus * 60L
                                    focusRemainingSecs = pFocus * 60L
                                }
                                try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                                tabPageCache.remove(settingsTabKey(AppSettingsTab.TIMER))
                                navigateToPanel(AppPanel.SETTINGS)
                            }
                        }
                        presetsRow.addView(chip)
                    }
                    timerModeCard.addView(presetsRow)
                    timerModeCard.addView(createDivider())

                    fun formatIntervalValue(valMinutes: Long, unit: String): String {
                        if (unit != "min") return "$valMinutes $unit"
                        val h = valMinutes / 60L
                        val m = valMinutes % 60L
                        return when {
                            h > 0L && m > 0L -> "${h}h ${m}m"
                            h > 0L -> "${h}h"
                            else -> "${m}m"
                        }
                    }

                    fun makeIntervalStepper(title: String, subtitle: String, key: String, defaultVal: Long, minVal: Long, maxVal: Long, stepVal: Long, unit: String): LinearLayout {
                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(18), dp(12), dp(18), dp(12))
                        }
                        val textCol = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        textCol.addView(TextView(this).apply {
                            text = title
                            setTextColor(themeCoordinator.textColor)
                            textSize = 14.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        })
                        textCol.addView(TextView(this).apply {
                            text = subtitle
                            setTextColor(themeCoordinator.textColor)
                            alpha = 0.5f
                            textSize = 12f
                            setPadding(0, 2, 0, 0)
                        })
                        row.addView(textCol)

                        val valText = TextView(this).apply {
                            val curVal = sharedPrefs.safeLong(key, defaultVal)
                            text = formatIntervalValue(curVal, unit)
                            textSize = 14.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setTextColor(themeCoordinator.primaryColor)
                            setPadding(dp(8), 0, dp(8), 0)
                        }

                        fun makeStepBtn(symbol: String, delta: Long): TextView {
                            val btn = TextView(this).apply {
                                text = symbol
                                textSize = 18f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(themeCoordinator.textColor)
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 8f)
                                setPadding(dp(12), dp(4), dp(12), dp(4))
                            }

                            val repeatHandler = Handler(Looper.getMainLooper())
                            var holdTicks = 0

                            fun performStep() {
                                val cur = sharedPrefs.safeLong(key, defaultVal)
                                val multiplier = when {
                                    holdTicks > 30 -> 12L
                                    holdTicks > 15 -> 6L
                                    holdTicks > 6 -> 2L
                                    else -> 1L
                                }
                                val effectiveDelta = delta * multiplier
                                val next = max(minVal, Math.min(maxVal, cur + effectiveDelta))
                                val editor = sharedPrefs.edit().putLong(key, next)
                                if (key == "study_interval_minutes") {
                                    val nextSecs = next * 60L
                                    editor.putLong("focus_countdown_secs", nextSecs)
                                    if (currentTimerState == TimerState.IDLE) {
                                        focusCountdownSecs = nextSecs
                                        if (timerMode == "COUNTDOWN") {
                                            focusRemainingSecs = nextSecs
                                        }
                                    }
                                } else if (key == "break_interval_minutes") {
                                    val nextBreakSecs = next * 60L
                                    editor.putLong("break_countdown_secs", nextBreakSecs)
                                }
                                editor.apply()
                                valText.text = formatIntervalValue(next, unit)
                                try { btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
                            }

                            val repeatRunnable: Runnable = object : Runnable {
                                override fun run() {
                                    holdTicks++
                                    performStep()
                                    val nextDelay = when {
                                        holdTicks > 30 -> 50L
                                        holdTicks > 15 -> 90L
                                        holdTicks > 6 -> 160L
                                        else -> 260L
                                    }
                                    repeatHandler.postDelayed(this, nextDelay)
                                }
                            }

                            btn.setOnTouchListener { v, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        holdTicks = 0
                                        performStep()
                                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).start()
                                        repeatHandler.postDelayed(repeatRunnable, 350)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        repeatHandler.removeCallbacks(repeatRunnable)
                                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                    }
                                }
                                true
                            }

                            return btn
                        }

                        val ctrlLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        ctrlLayout.addView(makeStepBtn("−", -stepVal))
                        ctrlLayout.addView(valText)
                        ctrlLayout.addView(makeStepBtn("+", stepVal))
                        row.addView(ctrlLayout)
                        return row
                    }

                    val isFreedomMode = sharedPrefs.getBoolean("pomodoro_freedom_mode", false)
                    val maxFocus = if (isFreedomMode) 1440L else 120L
                    val focusSubtitle = if (isFreedomMode) "Extended continuous focus (up to 24h / 1440m)" else "How long each study session lasts"
                    timerModeCard.addView(makeIntervalStepper("Study Duration", focusSubtitle, "study_interval_minutes", 25L, 5L, maxFocus, 5L, "min"))
                    timerModeCard.addView(createDivider())
                    timerModeCard.addView(makeIntervalStepper("Short Break", if (isFreedomMode) "Standard break (bypassed in Freedom Mode)" else "Quick break between study sessions", "break_interval_minutes", 5L, 1L, 30L, 1L, "min"))
                    timerModeCard.addView(createDivider())

                    // Advanced Break & Automation Options (Conditional: only shown when Freedom Mode is OFF)
                    val advancedIntervalsLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        visibility = if (isFreedomMode) View.GONE else View.VISIBLE
                    }

                    advancedIntervalsLayout.addView(makeIntervalStepper("Long Break", "Longer rest after completing multiple sessions", "long_break_minutes", 15L, 5L, 60L, 5L, "min"))
                    advancedIntervalsLayout.addView(createDivider())
                    advancedIntervalsLayout.addView(makeIntervalStepper("Sessions Until Long Break", "Number of study sessions before a longer rest", "long_break_interval", 4L, 2L, 10L, 1L, "sessions"))
                    advancedIntervalsLayout.addView(createDivider())

                    val isOvertime = sharedPrefs.getBoolean("pomodoro_overtime_enabled", true)
                    val overtimeSwitch = SwitchMaterial(this).apply {
                        isChecked = isOvertime
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_overtime_enabled", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Overtime Flow", "Keep timer running past zero so your flow state is never interrupted", overtimeSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    val isAutoBreak = sharedPrefs.getBoolean("pomodoro_auto_break", true)
                    val autoBreakSwitch = SwitchMaterial(this).apply {
                        isChecked = isAutoBreak
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_auto_break", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Auto-Start Breaks", "Automatically start rest countdown when focus session finishes", autoBreakSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    val isAutoFocus = sharedPrefs.getBoolean("pomo_auto_start_focus", true)
                    val autoFocusSwitch = SwitchMaterial(this).apply {
                        isChecked = isAutoFocus
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomo_auto_start_focus", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Auto-Start Next Focus", "Automatically start next study interval when break ends", autoFocusSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    val isPureWhitePomo = sharedPrefs.getBoolean("pomodoro_pure_white_theme", true)
                    val pureWhitePomoSwitch = SwitchMaterial(this).apply {
                        isChecked = isPureWhitePomo
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_pure_white_theme", isChecked).apply()
                            tabPageCache.clear()
                            if (currentPanel == AppPanel.FOCUS) {
                                updateVisualStyles()
                            }
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "White Theme for Pomodoro", "Minimal white theme for Pomodoro timer", pureWhitePomoSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    timerModeCard.addView(advancedIntervalsLayout)

                    val freedomSwitch = SwitchMaterial(this).apply {
                        isChecked = isFreedomMode
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_freedom_mode", isChecked).apply()
                            advancedIntervalsLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
                        }
                    }
                    timerModeCard.addView(createSettingsRow("", "Long Sessions", "Study for extended periods without automatic breaks or session limits", freedomSwitch))
                }
                layout.addView(timerModeCard)

                // CARD 2: DISPLAY & GESTURES
                layout.addView(createSectionLabel("DISPLAY & GESTURES"))
                val displayGestureCard = createSettingsCard()

                val isLandscapeStopwatchEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
                val landscapeSwitch = SwitchMaterial(this).apply {
                    isChecked = isLandscapeStopwatchEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit()
                            .putBoolean("is_landscape_mode_enabled", isChecked)
                            .putBoolean("true_fullscreen_landscape", isChecked)
                            .apply()
                        if (currentPanel == AppPanel.FOCUS) {
                            applyImmersiveModeForLandscape()
                            buildCurrentPanel()
                        }
                    }
                }
                displayGestureCard.addView(createSettingsRow("", "Landscape Fullscreen", "Sideways fullscreen distraction-free clock", landscapeSwitch))
                displayGestureCard.addView(createDivider())

                val isPauseButtonEnabled = sharedPrefs.getBoolean("show_pause_button", true)
                val pauseButtonSwitch = SwitchMaterial(this).apply {
                    isChecked = isPauseButtonEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_pause_button", isChecked).apply()
                        if (currentPanel == AppPanel.FOCUS) updateVisualStyles()
                    }
                }
                displayGestureCard.addView(createSettingsRow("", getString(R.string.pause_button), getString(R.string.pause_button_sub), pauseButtonSwitch))
                displayGestureCard.addView(createDivider())

                val isPureWhite = sharedPrefs.getBoolean("pureWhiteTimer", true)
                val pureWhiteSwitch = SwitchMaterial(this).apply {
                    isChecked = isPureWhite
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("pureWhiteTimer", isChecked).apply()
                        updateVisualStyles()
                        tabPageCache.clear()
                    }
                }
                displayGestureCard.addView(createSettingsRow("", "White Timer", "Clean white timer digits instead of accent color", pureWhiteSwitch))
                displayGestureCard.addView(createDivider())

                // Physical Gestures (Flip to Focus + Conditional Auto-Pause on Face Up)
                val isFlipEnabled = FlipToFocusManager.isEnabled(this)
                val pauseFaceUpContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (isFlipEnabled) View.VISIBLE else View.GONE
                }

                val isPauseFaceUp = FlipToFocusManager.isPauseOnFaceUp(this)
                val pauseFaceUpSwitch = SwitchMaterial(this).apply {
                    isChecked = isPauseFaceUp
                    setOnCheckedChangeListener { _, isChecked ->
                        FlipToFocusManager.setPauseOnFaceUp(this@with, isChecked)
                    }
                }
                pauseFaceUpContainer.addView(createDivider())
                pauseFaceUpContainer.addView(createSettingsRow("", "Auto-Pause on Face Up", "Pause the timer when picking up or turning the phone face up", pauseFaceUpSwitch))

                val flipSwitch = SwitchMaterial(this).apply {
                    isChecked = isFlipEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        FlipToFocusManager.setEnabled(this@with, isChecked)
                        this@with.syncFlipToFocusState()
                        pauseFaceUpContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
                displayGestureCard.addView(createSettingsRow("", "Flip to Focus", "Turn phone face down on your desk to automatically start focusing", flipSwitch))
                displayGestureCard.addView(pauseFaceUpContainer)
                displayGestureCard.addView(createDivider())

                // Quick Study Time Adjust
                val adjustRow = createSettingsRow("", "Adjust Today's Study Time", "Add missed study minutes or correct today's total")
                adjustRow.setOnClickListener {
                    DeveloperToolsHelper.showAdjustTodayTimeDialog(host, themeCoordinator, isDeveloperExtended = false)
                }
                displayGestureCard.addView(adjustRow)
                layout.addView(displayGestureCard)
            }

            // ==========================================
            // 4. INSIGHTS & STATS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.ANALYTICS) {
                layout.addView(createSectionLabel("STATS & CHARTS"))
                val chartsCard = createSettingsCard()
                val isHeatmapEnabled = sharedPrefs.getBoolean("show_focus_heatmap", true)
                val heatmapSwitch = SwitchMaterial(this).apply {
                    isChecked = isHeatmapEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_focus_heatmap", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("", getString(R.string.focus_heatmap_setting), getString(R.string.focus_heatmap_setting_sub), heatmapSwitch))
                chartsCard.addView(createDivider())

                val isPieChartEnabled = sharedPrefs.safeBoolean("show_subject_pie_chart", true)
                val pieChartSwitch = SwitchMaterial(this).apply {
                    isChecked = isPieChartEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_subject_pie_chart", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("", "Subject Breakdown Chart", "Show subject distribution chart in Stats", pieChartSwitch))
                chartsCard.addView(createDivider())

                val isDonutEnabled = sharedPrefs.safeBoolean("use_donut_chart", true)
                val donutChartSwitch = SwitchMaterial(this).apply {
                    isChecked = isDonutEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("use_donut_chart", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRowWithView(createCustomDonutIcon(), "Donut Chart Style", "Modern ring chart with quick stats in center", donutChartSwitch))
                chartsCard.addView(createDivider())

                val isPatternEnabled = sharedPrefs.getBoolean("show_focus_pattern", true)
                val patternSwitch = SwitchMaterial(this).apply {
                    isChecked = isPatternEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_focus_pattern", isChecked).apply()
                        statsDirty = true
                        tabPageCache.clear()
                    }
                }
                chartsCard.addView(createSettingsRow("", getString(R.string.focus_pattern_setting), getString(R.string.focus_pattern_setting_sub), patternSwitch))
                layout.addView(chartsCard)

            }

            // ==========================================
            // 5. PLANNER & GOALS SUB-SCREEN
            // ==========================================
            // ==========================================
            // 5. PLANNER & GOALS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.PLANNER) {
                // CARD 1: DAILY GOALS & REMINDERS
                layout.addView(createSectionLabel("DAILY GOALS & REMINDERS"))
                val goalReminderCard = createSettingsCard()
                val goalValueText = TextView(this).apply {
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(8), 0, dp(8), 0)
                }
                fun makeGoalStepBtn(symbol: String, stepSecs: Long): TextView {
                    return TextView(this).apply {
                        text = symbol
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(themeCoordinator.textColor)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                        setPadding(dp(14), dp(4), dp(14), dp(4))
                        setOnClickListener {
                            val current = StatsEngine.getDailyGoalSecs(sharedPrefs)
                            val next = max(900L, current + stepSecs)
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            sharedPrefs.edit()
                                .putLong("daily_goal_secs", next)
                                .putInt("daily_goal_minutes", (next / 60).toInt())
                                .putLong("${todayStr}_goal_secs", next)
                                .apply()
                            GoalHistoryManager.recordGoalChange(host, todayStr, next)
                            goalValueText.text = formatGoalLabel(next)
                        }
                    }
                }
                goalValueText.text = formatGoalLabel(StatsEngine.getDailyGoalSecs(sharedPrefs))
                val goalRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                goalRow.addView(ImageView(this).apply {
                    setImageResource(R.drawable.ic_target)
                    setColorFilter(themeCoordinator.primaryColor)
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                })
                val goalTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal); setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
                goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal_sub); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
                goalRow.addView(goalTextCol)
                val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                controls.addView(makeGoalStepBtn("−", -900L))
                controls.addView(goalValueText)
                controls.addView(makeGoalStepBtn("+", 900L))
                goalRow.addView(controls)
                goalReminderCard.addView(goalRow)
                goalReminderCard.addView(createDivider())

                val isStreakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
                val streakGoalSwitch = SwitchMaterial(this).apply {
                    isChecked = isStreakGoalBased
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("streak_uses_daily_goal", isChecked).apply()
                    }
                }
                goalReminderCard.addView(createSettingsRow("", getString(R.string.streak_uses_goal), getString(R.string.streak_uses_goal_sub), streakGoalSwitch))
                goalReminderCard.addView(createDivider())

                val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
                val reminderTimeContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (reminderEnabled) View.VISIBLE else View.GONE
                }

                val reminderSwitch = SwitchMaterial(this).apply {
                    isChecked = reminderEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()
                        if (isChecked) {
                            GoalReminderScheduler.schedule(context)
                        } else {
                            GoalReminderScheduler.cancel(context)
                        }
                        reminderTimeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
                goalReminderCard.addView(createSettingsRow("", "Daily Goal Reminder", "Evening notification if daily goal is not reached", reminderSwitch))

                val remHour = sharedPrefs.safeInt("reminder_hour", 20)
                val remMinute = sharedPrefs.safeInt("reminder_minute", 0)
                val timeLabel = TextView(this).apply {
                    text = TimeFormat.formatHourMinute(context, remHour, remMinute)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                }
                val timeRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    setOnClickListener {
                        val curH = sharedPrefs.safeInt("reminder_hour", 20)
                        val curM = sharedPrefs.safeInt("reminder_minute", 0)
                        val is24H = TimeFormat.is24Hour(context)
                        android.app.TimePickerDialog(context, { _, hourOfDay, minute ->
                            sharedPrefs.edit()
                                .putInt("reminder_hour", hourOfDay)
                                .putInt("reminder_minute", minute)
                                .apply()
                            timeLabel.text = TimeFormat.formatHourMinute(context, hourOfDay, minute)
                            GoalReminderScheduler.schedule(context)
                        }, curH, curM, is24H).show()
                    }
                }
                timeRow.addView(TextView(this).apply { text = "⏰"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
                val timeTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                timeTextCol.addView(TextView(this).apply {
                    text = "Reminder Time"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                timeTextCol.addView(TextView(this).apply {
                    text = "Time to receive your daily reminder"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                timeRow.addView(timeTextCol)
                timeRow.addView(timeLabel)

                reminderTimeContainer.addView(createDivider())
                reminderTimeContainer.addView(timeRow)
                goalReminderCard.addView(reminderTimeContainer)

                layout.addView(goalReminderCard)

                // CARD 2: HABITS & TOOLS
                layout.addView(createSectionLabel("HABITS & TOOLS"))
                val habitsToolsCard = createSettingsCard()
                val matrixRow = createSettingsRow("", "Weekly Goal Tracker", "See which goals you finished each day this week")
                matrixRow.setOnClickListener {
                    showPlannerMatrixDialog()
                }
                habitsToolsCard.addView(matrixRow)
                habitsToolsCard.addView(createDivider())

                val yesterdayRow = createSettingsRow("", "Yesterday's Goals", "Review or adjust previous day's completed goals")
                yesterdayRow.setOnClickListener {
                    showEditYesterdayGoalsDialog()
                }
                habitsToolsCard.addView(yesterdayRow)
                habitsToolsCard.addView(createDivider())

                val plannerThemeRow = createSettingsRow("", "Goal Colors", "Choose custom colors for your daily study goals")
                plannerThemeRow.setOnClickListener {
                    showPlannerThemePickerDialog()
                }
                habitsToolsCard.addView(plannerThemeRow)
                layout.addView(habitsToolsCard)
            }

            // ==========================================
            // 6. CLOUD, SYNC & BACKUPS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.CLOUD) {
                layout.addView(createSectionLabel("CLOUD BACKUP"))
                val cloudCard = createSettingsCard()
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userEmail = AuthManager.getUserEmail(this) ?: "Not Signed In"

                val authRow = createSettingsRow("", "Cloud Account", userEmail)
                authRow.setOnClickListener {
                    if (!isGoogleAuth) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }
                cloudCard.addView(authRow)
                cloudCard.addView(createDivider())

                val syncPushBtn = Button(this).apply {
                    text = "Back Up to Cloud Now"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        setMargins(dp(16), dp(12), dp(16), dp(6))
                    }
                    setOnClickListener {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val result = CloudSyncManager.syncDataToCloudDetailed(this@with, force = true)
                            runOnUiThread {
                                if (result.isSuccess) {
                                    AppToast.show(this@with, "Cloud backup completed successfully!")
                                } else if (result.isUnauthenticated) {
                                    AppToast.show(this@with, "Please sign in with Google first.", Toast.LENGTH_LONG)
                                } else {
                                    AppToast.show(this@with, "Backup failed: ${result.errorMessage ?: "Check your internet connection"}", Toast.LENGTH_LONG)
                                }
                            }
                        }
                    }
                }
                cloudCard.addView(syncPushBtn)

                val syncPullBtn = Button(this).apply {
                    text = "Restore from Cloud Backup"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        setMargins(dp(16), 0, dp(16), dp(14))
                    }
                    setOnClickListener {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val ok = CloudSyncManager.restoreDataFromCloud(this@with)
                            runOnUiThread {
                                if (ok) {
                                    AppToast.show(this@with, "Cloud backup restored successfully!")
                                    tabPageCache.clear()
                                    statsDirty = true
                                    navigateToPanel(currentPanel)
                                } else {
                                    AppToast.show(this@with, "No cloud backup found or restore failed")
                                }
                            }
                        }
                    }
                }
                cloudCard.addView(syncPullBtn)
                layout.addView(cloudCard)

                layout.addView(createSectionLabel("LOCAL DATA & BACKUPS"))
                val dataCard = createSettingsCard()
                val exportRow = createSettingsRow("", getString(R.string.export_logs), getString(R.string.export_logs_sub))
                exportRow.setOnClickListener {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        val fileDateFormat = SimpleDateFormat("dd_MMM", Locale.getDefault())
                        putExtra(Intent.EXTRA_TITLE, "backup_${fileDateFormat.format(Date())}.json")
                    }
                    exportLauncher.launch(intent)
                }
                dataCard.addView(exportRow)
                dataCard.addView(createDivider())

                val importRow = createSettingsRow("", getString(R.string.import_data), getString(R.string.import_data_sub))
                importRow.setOnClickListener {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/json"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    importLauncher.launch(Intent.createChooser(intent, getString(R.string.select_backup_file)))
                }
                dataCard.addView(importRow)
                layout.addView(dataCard)
            }

            // ==========================================
            // 7. THEME & APPEARANCE SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.THEME) {
                layout.addView(createSectionLabel("APP THEME"))
                val modeCard = createSettingsCard()
                val isEclipse = themeCoordinator.activeBgMode == "ECLIPSE"
                val isLight = themeCoordinator.activeBgMode == "LIGHT"

                fun modeRadio(selected: Boolean): View {
                    return View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                            setStroke(dp(2), if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        }
                    }
                }

                val oledRow = createSettingsRow("", getString(R.string.theme_amoled), "Pure pitch black background for OLED screens", modeRadio(!isEclipse && !isLight))
                oledRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("activeBgMode", "OLED").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(oledRow)
                modeCard.addView(createDivider())

                val eclipseRow = createSettingsRow("", getString(R.string.theme_slate), getString(R.string.theme_slate_sub), modeRadio(isEclipse))
                eclipseRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("activeBgMode", "ECLIPSE").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(eclipseRow)
                modeCard.addView(createDivider())

                val lightRow = createSettingsRow("", getString(R.string.theme_light), getString(R.string.theme_light_sub), modeRadio(isLight))
                lightRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("activeBgMode", "LIGHT").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                modeCard.addView(lightRow)
                layout.addView(modeCard)

                // ==========================================
                // 2. CARD STYLES
                // ==========================================
                layout.addView(createSectionLabel("CARD STYLES"))
                val styleCard = createSettingsCard()
                val isBubble = themeCoordinator.isBubbleStyle()
                val isGlass = themeCoordinator.isGlassStyle()

                val glassRow = createSettingsRow("", getString(R.string.style_glass), getString(R.string.style_glass_sub), modeRadio(isGlass))
                glassRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("ui_style", "GLASS").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(glassRow)
                styleCard.addView(createDivider())

                val bubbleRow = createSettingsRow("", "3D Soft Depth", "Soft elevated cards with smooth depth", modeRadio(isBubble))
                bubbleRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("ui_style", "BUBBLE").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(bubbleRow)
                styleCard.addView(createDivider())

                val classicRow = createSettingsRow("", getString(R.string.style_classic), getString(R.string.style_classic_sub), modeRadio(!isGlass && !isBubble))
                classicRow.setOnClickListener {
                    pendingSettingsScrollY = settingsScrollView.scrollY
                    sharedPrefs.edit().putString("ui_style", "CLASSIC").apply()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                }
                styleCard.addView(classicRow)
                layout.addView(styleCard)

                // ==========================================
                // 3. WALLPAPER & TYPOGRAPHY (CONSOLIDATED CARD)
                // ==========================================
                layout.addView(createSectionLabel("WALLPAPER & TYPOGRAPHY"))
                val mediaFontCard = createSettingsCard()

                // Row 1: Wallpaper
                val wallpaperType = WallpaperManager.getWallpaperType(this)
                val activePresetId = WallpaperManager.getActivePresetId(this)

                val currentTitle = when (wallpaperType) {
                    WallpaperManager.TYPE_PRESET -> WallpaperManager.getPresetById(this, activePresetId)?.title ?: "Preset Wallpaper"
                    WallpaperManager.TYPE_STATIC -> "Custom Photo"
                    WallpaperManager.TYPE_LIVE_AURORA -> "Aurora Wave (Live)"
                    WallpaperManager.TYPE_LIVE_COSMIC -> "Cosmic Stars (Live)"
                    WallpaperManager.TYPE_LIVE_LOFI_RAIN -> "Lofi Rain (Live)"
                    WallpaperManager.TYPE_LIVE_VIDEO -> "Custom Video Loop"
                    else -> "Solid Theme Background"
                }

                val wallpaperRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    val outVal = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
                    isClickable = true
                    isFocusable = true
                }

                val heroPreviewContainer = FrameLayout(this).apply {
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, dp(10).toFloat())
                        }
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(0xFF1E293B.toInt())
                        setStroke(dp(1), 0x22FFFFFF)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                        rightMargin = dp(14)
                    }
                }

                val heroPreviewImg = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }

                when (wallpaperType) {
                    WallpaperManager.TYPE_PRESET -> {
                        val preset = WallpaperManager.getPresetById(this, activePresetId)
                        val thumb = if (preset != null) WallpaperManager.loadPresetThumbnail(this, preset) else null
                        if (thumb != null) {
                            heroPreviewImg.setImageBitmap(thumb)
                        } else {
                            heroPreviewImg.setBackgroundColor(0xFF334155.toInt())
                        }
                    }
                    WallpaperManager.TYPE_STATIC -> {
                        val file = WallpaperManager.getCustomWallpaperFile(this)
                        if (file.exists()) {
                            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                            val bm = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                            if (bm != null) heroPreviewImg.setImageBitmap(bm)
                        }
                    }
                    WallpaperManager.TYPE_LIVE_AURORA -> heroPreviewImg.setBackgroundColor(0xFF065F46.toInt())
                    WallpaperManager.TYPE_LIVE_COSMIC -> heroPreviewImg.setBackgroundColor(0xFF1E1B4B.toInt())
                    WallpaperManager.TYPE_LIVE_LOFI_RAIN -> heroPreviewImg.setBackgroundColor(0xFF1E293B.toInt())
                    WallpaperManager.TYPE_LIVE_VIDEO -> heroPreviewImg.setBackgroundColor(0xFF1E1E2E.toInt())
                    else -> heroPreviewImg.setBackgroundColor(if (themeCoordinator.isDarkMode()) 0xFF000000.toInt() else 0xFFF1F5F9.toInt())
                }
                heroPreviewContainer.addView(heroPreviewImg)
                wallpaperRow.addView(heroPreviewContainer)

                val heroTextLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val heroTitle = TextView(this).apply {
                    text = "Select Wallpaper"
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                }
                heroTextLayout.addView(heroTitle)

                val heroSub = TextView(this).apply {
                    text = "Active: $currentTitle • Tap to change"
                    textSize = 12f
                    setTextColor(tintedColor(themeCoordinator.textColor, 140))
                    setPadding(0, dp(2), 0, 0)
                }
                heroTextLayout.addView(heroSub)
                wallpaperRow.addView(heroTextLayout)

                val browseArrow = TextView(this).apply {
                    text = "Choose ›"
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                }
                wallpaperRow.addView(browseArrow)

                wallpaperRow.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    showWallpaperGalleryDialog()
                }
                mediaFontCard.addView(wallpaperRow)
                mediaFontCard.addView(createDivider())

                // Row 2: Font & Typography
                val currentFontId = FontStyleManager.getSelectedFontId(this)
                val currentFontItem = FontStyleManager.FONT_OPTIONS.find { it.id == currentFontId } ?: FontStyleManager.FONT_OPTIONS[0]

                val fontBadge = TextView(this).apply {
                    text = currentFontItem.displayName
                    textSize = 13f
                    typeface = FontStyleManager.getTypeface(this@with, currentFontItem.id)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                }

                val fontRow = createSettingsRow(
                    "",
                    "Timer & App Font Style",
                    "${currentFontItem.category} • ${currentFontItem.displayName}",
                    fontBadge
                )
                fontRow.setOnClickListener {
                    showFontSelectionDialog()
                }
                mediaFontCard.addView(fontRow)
                layout.addView(mediaFontCard)

                // ==========================================
                // 4. ACCENT COLORS (WITH INLINE RANDOMIZE BUTTON)
                // ==========================================
                val accentHeader = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(6), dp(18), dp(6), dp(8))
                }
                accentHeader.addView(TextView(this).apply {
                    text = "ACCENT COLORS"
                    textSize = 12f
                    letterSpacing = 0.12f
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                val rollBtn = TextView(this).apply {
                    text = "🎲 Randomize"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    setOnClickListener { v ->
                        val focusList = ThemeCoordinator.SOFT_FOCUS_PALETTE
                        val breakList = ThemeCoordinator.SOFT_BREAK_PALETTE
                        val randomFocusHex = focusList.random()
                        var randomBreakHex = breakList.random()
                        while (randomBreakHex.equals(randomFocusHex, ignoreCase = true) && breakList.size > 1) {
                            randomBreakHex = breakList.random()
                        }

                        val newFocus = Color.parseColor(randomFocusHex)
                        val newBreak = Color.parseColor(randomBreakHex)

                        val hsvFocus = FloatArray(3)
                        val hsvBreak = FloatArray(3)
                        Color.colorToHSV(newFocus, hsvFocus)
                        Color.colorToHSV(newBreak, hsvBreak)

                        sharedPrefs.edit()
                            .putInt("customPrimary", newFocus)
                            .putInt("customSecondary", newBreak)
                            .putInt("customHue", hsvFocus[0].toInt())
                            .putInt("customSecondaryHue", hsvBreak[0].toInt())
                            .apply()

                        themeCoordinator.primaryColor = newFocus
                        themeCoordinator.secondaryColor = newBreak
                        (settingsBackFab.background as? GradientDrawable)?.setColor(newFocus)

                        updateVisualStyles()
                        tabPageCache.clear()

                        try {
                            performMicroHaptic(v)
                        } catch (_: Exception) {}
                        pendingSettingsScrollY = settingsScrollView.scrollY
                        AppToast.show(this@with, "New theme colors applied!")
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                accentHeader.addView(rollBtn)
                layout.addView(accentHeader)

                // ==========================================
                // DUAL FOCUS & BREAK ACCENT COLOR CONTROLS
                // ==========================================
                fun makeAccentColorSection(
                    title: String,
                    subtitle: String,
                    prefKey: String,
                    currentColor: Int,
                    palette: List<String>,
                    onColorChanged: (Int) -> Unit
                ): LinearLayout {
                    val card = createSettingsCard()
                    val cardContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(14), dp(18), dp(16))
                    }

                    val colorHeaderRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 0, 0, dp(12))
                    }

                    var activeColor = currentColor
                    val hexStr = String.format("#%06X", 0xFFFFFF and activeColor)

                    val previewCircle = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                            setMargins(0, 0, dp(12), 0)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(activeColor)
                            setStroke(dp(2), Color.argb(100, 255, 255, 255))
                        }
                    }
                    colorHeaderRow.addView(previewCircle)

                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            minimumHeight = dp(38)
                        }
                    }
                    textCol.addView(TextView(this).apply {
                        text = title
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setSingleLine(true)
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    val hexLabel = TextView(this).apply {
                        text = "$subtitle  •  $hexStr"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 12f
                        setSingleLine(true)
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, 2, 0, 0)
                    }
                    textCol.addView(hexLabel)
                    colorHeaderRow.addView(textCol)
                    cardContainer.addView(colorHeaderRow)

                    // Curated Aesthetic Soft Swatches Horizontal Scroll
                    val swatchesScroll = android.widget.HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false
                        setPadding(0, 0, 0, dp(12))
                    }
                    val swatchesLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val swatchViews = ArrayList<View>()

                    fun refreshSwatchBorders() {
                        for ((idx, sView) in swatchViews.withIndex()) {
                            val hex = palette[idx]
                            val isMatch = String.format("#%06X", 0xFFFFFF and activeColor).equals(hex, ignoreCase = true)
                            (sView.background as? GradientDrawable)?.apply {
                                setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                            }
                        }
                    }

                    val hsv = FloatArray(3)
                    Color.colorToHSV(activeColor, hsv)

                    val hueHeader = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(2), 0, dp(6))
                    }
                    val hueTitle = TextView(this).apply {
                        text = "Fine-Tune Hue Slider"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val hueValLabel = TextView(this).apply {
                        text = "${hsv[0].toInt()}°"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.6f
                        textSize = 12f
                    }
                    hueHeader.addView(hueTitle)
                    hueHeader.addView(hueValLabel)

                    val hueSeekBar = android.widget.SeekBar(this).apply {
                        max = 360
                        progress = hsv[0].toInt()
                        setPadding(dp(4), dp(4), dp(4), dp(4))
                    }

                    hueSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                            hueValLabel.text = "$prog°"
                            if (fromUser) {
                                val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.70f, 0.95f))
                                activeColor = colorInt
                                (previewCircle.background as? GradientDrawable)?.setColor(activeColor)
                                val newHex = String.format("#%06X", 0xFFFFFF and activeColor)
                                hexLabel.text = "$subtitle  •  $newHex"
                                sharedPrefs.edit().putInt(prefKey, activeColor).apply()
                                onColorChanged(activeColor)
                                refreshSwatchBorders()
                            }
                        }
                        override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                        override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                    })

                    for (hex in palette) {
                        val swatchColor = Color.parseColor(hex)
                        val sView = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                                setMargins(0, 0, dp(8), 0)
                            }
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(swatchColor)
                                val isMatch = String.format("#%06X", 0xFFFFFF and activeColor).equals(hex, ignoreCase = true)
                                setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                            }
                            setOnClickListener {
                                activeColor = swatchColor
                                (previewCircle.background as? GradientDrawable)?.setColor(activeColor)
                                val newHex = String.format("#%06X", 0xFFFFFF and activeColor)
                                hexLabel.text = "$subtitle  •  $newHex"
                                sharedPrefs.edit().putInt(prefKey, activeColor).apply()

                                val swatchHsv = FloatArray(3)
                                Color.colorToHSV(activeColor, swatchHsv)
                                hueSeekBar.progress = swatchHsv[0].toInt()
                                hueValLabel.text = "${swatchHsv[0].toInt()}°"

                                onColorChanged(activeColor)
                                refreshSwatchBorders()
                            }
                        }
                        swatchViews.add(sView)
                        swatchesLayout.addView(sView)
                    }
                    swatchesScroll.addView(swatchesLayout)
                    cardContainer.addView(swatchesScroll)
                    cardContainer.addView(hueHeader)
                    cardContainer.addView(hueSeekBar)

                    card.addView(cardContainer)
                    return card
                }

                layout.addView(createSectionLabel("STUDY ACCENT COLOR"))
                val focusColorCard = makeAccentColorSection(
                    title = "Study Color",
                    subtitle = "Colors your study timer ring & primary buttons",
                    prefKey = "customPrimary",
                    currentColor = themeCoordinator.primaryColor,
                    palette = ThemeCoordinator.SOFT_FOCUS_PALETTE
                ) { newColor ->
                    themeCoordinator.primaryColor = newColor
                    (settingsBackFab.background as? GradientDrawable)?.setColor(newColor)
                    updateVisualStyles()
                    tabPageCache.clear()
                }
                layout.addView(focusColorCard)

                layout.addView(createSectionLabel("BREAK ACCENT COLOR"))
                val breakColorCard = makeAccentColorSection(
                    title = "Break Color",
                    subtitle = "Colors your break timer ring & status badges",
                    prefKey = "customSecondary",
                    currentColor = themeCoordinator.secondaryColor,
                    palette = ThemeCoordinator.SOFT_BREAK_PALETTE
                ) { newColor ->
                    themeCoordinator.secondaryColor = newColor
                    updateVisualStyles()
                    tabPageCache.clear()
                }
                layout.addView(breakColorCard)

                // ==========================================
                // INTERACTION & HAPTICS (AT BOTTOM OF THEME)
                // ==========================================
                layout.addView(createSectionLabel("FEEDBACK & HAPTICS"))
                val hapticCard = createSettingsCard()
                val isHapticsEnabled = sharedPrefs.getBoolean("haptics_enabled", true)
                val hapticsSwitch = SwitchMaterial(this).apply {
                    isChecked = isHapticsEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("haptics_enabled", isChecked).apply()
                        if (isChecked) performMicroHaptic(this)
                    }
                }
                hapticCard.addView(createSettingsRow("", "Tactile Haptics", "Subtle micro-haptic feedback on buttons & hold actions", hapticsSwitch))
                layout.addView(hapticCard)
            }

            // ==========================================
            // 8. DEVELOPER & ADVANCED SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.DEVELOPER) {
                if (BuildConfig.DEBUG && isDevModeUnlocked) {
                    layout.addView(createSectionLabel("ADVANCED TOOLS"))
                    val devCard = DeveloperToolsHelper.buildDevCard(host, themeCoordinator)
                    layout.addView(devCard)
                } else {
                    currentSettingsTab = AppSettingsTab.HUB
                    navigateToPanel(AppPanel.SETTINGS)
                }
            }

            settingsRootLayout.addView(settingsScrollView)

            val settingsRoot = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }
            settingsRoot.addView(settingsRootLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            settingsRoot.addView(settingsBackFab)

            FontStyleManager.applyRecursively(settingsRoot)
            target.addView(settingsRoot)
        }
    }

    private fun showFontSelectionDialog() {
        with(host) {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(20), dp(18), dp(20), dp(24))
            }

            // Header Row
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(12))
            }
            val titleText = TextView(this).apply {
                text = "Choose Font Style (20+)"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val closeBtn = TextView(this).apply {
                text = "✕"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { dialog.dismiss() }
            }
            headerRow.addView(titleText)
            headerRow.addView(closeBtn)
            dialogLayout.addView(headerRow)

            val maxScrollHeight = (resources.displayMetrics.heightPixels * 0.55f).toInt()
            val scroll = androidx.core.widget.NestedScrollView(this).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxScrollHeight)
            }
            val listLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val currentSelectedId = FontStyleManager.getSelectedFontId(this)
            var currentCategory = ""

            for (item in FontStyleManager.FONT_OPTIONS) {
                if (item.category != currentCategory) {
                    currentCategory = item.category
                    val catLabel = TextView(this).apply {
                        text = currentCategory.uppercase()
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(themeCoordinator.primaryColor)
                        letterSpacing = 0.08f
                        setPadding(dp(4), dp(12), dp(4), dp(4))
                    }
                    listLayout.addView(catLabel)
                }

                val isSelected = item.id == currentSelectedId
                val itemRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 45), 14f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 12), 14f)
                    }
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(4), 0, dp(4))
                    }
                    isClickable = true
                    setOnClickListener {
                        pendingSettingsScrollY = host.settingsScrollViewRef?.scrollY
                        FontStyleManager.setSelectedFontId(host, item.id)
                        performMicroHaptic(this)
                        dialog.dismiss()
                        tabPageCache.clear()
                        navigateToPanel(AppPanel.SETTINGS)
                        AppToast.show(host, "${item.displayName} applied!")
                    }
                }

                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val nameLabel = TextView(this).apply {
                    text = item.displayName
                    textSize = 14f
                    typeface = FontStyleManager.getTypeface(host, item.id)
                    setTextColor(if (isSelected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                }
                val previewLabel = TextView(this).apply {
                    text = item.sampleText
                    textSize = 12f
                    typeface = FontStyleManager.getTypeface(host, item.id)
                    setTextColor(themeCoordinator.textColor)
                    alpha = if (isSelected) 0.85f else 0.5f
                    setPadding(0, dp(2), 0, 0)
                }
                textCol.addView(nameLabel)
                textCol.addView(previewLabel)
                itemRow.addView(textCol)

                if (isSelected) {
                    val checkmark = TextView(this).apply {
                        text = "✓"
                        textSize = 16f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(themeCoordinator.primaryColor)
                        setPadding(dp(8), 0, dp(4), 0)
                    }
                    itemRow.addView(checkmark)
                }

                listLayout.addView(itemRow)
            }

            scroll.addView(listLayout)
            dialogLayout.addView(scroll)

            dialog.setContentView(dialogLayout)
            dialog.show()
            dialog.window?.let { win ->
                win.setLayout(
                    (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(420)),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            }
        }
    }
}

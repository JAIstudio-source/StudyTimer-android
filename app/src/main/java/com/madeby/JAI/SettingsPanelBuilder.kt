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
                gravity = Gravity.CENTER
                minimumWidth = dp(48)
                minimumHeight = dp(48)
                setPadding(dp(8), dp(4), dp(16), dp(4))
                setOnClickListener {
                    handleSettingsBack()
                }
            }
            headerRow.addView(backArrowBtn)

            val headerText = TextView(this).apply {
                text = when (currentSettingsTab) {
                    AppSettingsTab.HUB -> getString(R.string.settings_title)
                    AppSettingsTab.TIMER -> "Timer & Focus"
                    AppSettingsTab.PLANNER -> "Goal and Reminder"
                    AppSettingsTab.ANALYTICS -> "Insights & Stats"
                    AppSettingsTab.THEME -> "Theme & Appearance"
                    AppSettingsTab.CLOUD -> "Cloud & Backups"
                    AppSettingsTab.PROFILE -> "Student Profile"
                    AppSettingsTab.COMMUNITY -> "Leaderboard"
                    AppSettingsTab.DEVELOPER -> "Developer Tools"
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
                    gravity = Gravity.CENTER
                    minHeight = dp(36)
                    minWidth = dp(48)
                    contentDescription = "Return to All Settings Hub"
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
                    AppSettingsTab.HUB -> ""
                    AppSettingsTab.TIMER -> "Focus durations, break times & timer mode"
                    AppSettingsTab.PLANNER -> "Daily targets & goal reminders"
                    AppSettingsTab.ANALYTICS -> "Heatmap, subject breakdown & chart display"
                    AppSettingsTab.THEME -> "Personalize your background, colors & look"
                    AppSettingsTab.CLOUD -> "Google account sync, auto-backup & restore"
                    AppSettingsTab.PROFILE -> ""
                    AppSettingsTab.COMMUNITY -> "Rankings & live study presence"
                    AppSettingsTab.DEVELOPER -> "Diagnostic tools & debug settings"
                }
                setTextColor(themeCoordinator.textColor)
                alpha = 0.70f
                textSize = 13f
                setPadding(dp(6), 0, dp(6), dp(14))
            }
            if (subtitleText.text.isNotBlank()) {
                settingsRootLayout.addView(subtitleText)
            }

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
                if (subtitle.isNotBlank()) {
                    textCol.addView(TextView(this).apply {
                        text = subtitle
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                row.addView(textCol)
                if (trailingView != null) {
                    if (trailingView is SwitchMaterial) {
                        trailingView.background = null
                        trailingView.isFocusable = false
                        trailingView.defaultFocusHighlightEnabled = false
                        if (trailingView.contentDescription.isNullOrEmpty()) {
                            trailingView.contentDescription = "$title toggle"
                        }
                        row.background = null
                        row.isFocusable = false
                        row.defaultFocusHighlightEnabled = false
                        row.setOnClickListener {
                            trailingView.toggle()
                        }
                    }
                    row.addView(trailingView)
                }
                return row
            }

            fun createActionChip(text: String): TextView {
                return TextView(this).apply {
                    this.text = text
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 38), 12f)
                    setPadding(dp(11), dp(5), dp(11), dp(5))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(dp(10), 0, 0, 0)
                    }
                }
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
                if (subtitle.isNotBlank()) {
                    textCol.addView(TextView(this).apply {
                        text = subtitle
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.65f
                        textSize = 12f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                row.addView(textCol)
                if (trailingView != null) {
                    if (trailingView is SwitchMaterial) {
                        trailingView.background = null
                        trailingView.isFocusable = false
                        trailingView.defaultFocusHighlightEnabled = false
                        if (trailingView.contentDescription.isNullOrEmpty()) {
                            trailingView.contentDescription = "$title toggle"
                        }
                        row.background = null
                        row.isFocusable = false
                        row.defaultFocusHighlightEnabled = false
                        row.setOnClickListener {
                            trailingView.toggle()
                        }
                    }
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
                    text = if (isGoogleAuth) (userEmail ?: "Connected · Cloud Sync") else "Guest Mode · Tap to Sign In"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
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
                        alpha = 0.65f
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

                val timerSub = "Timer mode & app blocker"
                val dailyGoalSecs = StatsEngine.getDailyGoalSecs(sharedPrefs)
                val plannerSub = "Daily goal: ${formatGoalLabel(dailyGoalSecs)}"
                val themeSub = "Wallpaper, colors & card look"
                val cloudSub = if (isGoogleAuth) "Google Cloud backup" else "Sign In & Cloud Restore"

                // 1. UNIFIED PREFERENCES CARD GROUP
                layout.addView(createSectionLabel("PREFERENCES"))
                val prefsCard = createSettingsCard()
                addHubRowToCard(prefsCard, "Timer & Focus", timerSub, AppSettingsTab.TIMER)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Goal and Reminder", plannerSub, AppSettingsTab.PLANNER)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Insights & Stats", "Heatmap & pie chart", AppSettingsTab.ANALYTICS)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Leaderboard", "Rankings & live presence", AppSettingsTab.COMMUNITY)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Theme & Appearance", themeSub, AppSettingsTab.THEME)
                prefsCard.addView(createDivider())
                addHubRowToCard(prefsCard, "Cloud, Sync & Backups", cloudSub, AppSettingsTab.CLOUD)

                if (BuildConfig.DEBUG && isDevModeUnlocked) {
                    prefsCard.addView(createDivider())
                    addHubRowToCard(prefsCard, "Developer Tools", "Diagnostic tools & debug settings", AppSettingsTab.DEVELOPER)
                }
                layout.addView(prefsCard)

                // 2. HELP & SUPPORT
                layout.addView(createSectionLabel("HELP & SUPPORT"))
                val helpCard = createSettingsCard()

                val guideRow = createSettingsRow("", "How to Use & App Guide", "")
                guideRow.setOnClickListener {
                    showAppGuideDialog()
                }
                helpCard.addView(guideRow)
                helpCard.addView(createDivider())

                val feedbackRow = createSettingsRow("", "Feedback & Bug Reports", "")
                feedbackRow.setOnClickListener {
                    showFeedbackReportDialog()
                }
                helpCard.addView(feedbackRow)
                helpCard.addView(createDivider())

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
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) "Check for latest release & updates" else "",
                    updateChip
                )
                versionRow.setOnClickListener {
                    if (AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
                        checkForUpdates(manual = true)
                    } else {
                        AppToast.show(this, "Version v${currentVersionName()} is up to date")
                    }
                }
                helpCard.addView(versionRow)
                layout.addView(helpCard)

                // 3. LEGAL & PRIVACY
                layout.addView(createSectionLabel("LEGAL & PRIVACY"))
                val legalCard = createSettingsCard()

                val privacyRow = createSettingsRow("", "Privacy Policy", "")
                privacyRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/privacy.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening privacy policy...")
                    }
                }
                legalCard.addView(privacyRow)
                legalCard.addView(createDivider())

                val termsRow = createSettingsRow("", "Terms of Service", "")
                termsRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/terms.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening terms of service...")
                    }
                }
                legalCard.addView(termsRow)
                legalCard.addView(createDivider())

                val deleteWebRow = createSettingsRow("", "Delete Account Portal", "")
                deleteWebRow.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/delete-account.html")))
                    } catch (_: Exception) {
                        AppToast.show(this, "Opening account deletion portal...")
                    }
                }
                legalCard.addView(deleteWebRow)
                layout.addView(legalCard)

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
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = ProfileManager.getEffectiveDisplayName(this).ifBlank { AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google Account User" else "Guest Learner" }
                val userEmail = AuthManager.getUserEmail(this) ?: "Offline / Not Signed In"
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }
                val currentProfile = ProfileManager.getProfile(this)

                val draftName = ProfileManager.getDraftName(this)
                val draftBio = ProfileManager.getDraftBio(this)
                val draftExam = ProfileManager.getDraftExam(this)
                var selectedExam = draftExam ?: currentProfile.targetExam.ifBlank { "Self-Study" }

                // Hold references to fields so avatar click can save draft without losing typed edits
                var editNameFieldRef: EditText? = null
                var editBioFieldRef: EditText? = null

                // ----------------------------------------------------
                // CARD 1: PUBLIC IDENTITY SUMMARY
                // ----------------------------------------------------
                layout.addView(createSectionLabel("PUBLIC IDENTITY"))
                val identityCard = createSettingsCard()
                val identityContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(20), dp(22), dp(20), dp(20))
                }

                // Centered Avatar Frame with Camera Edit Badge
                val avatarWrapper = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, dp(12))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        ProfileManager.saveDraftForm(
                            this@with,
                            editNameFieldRef?.text?.toString(),
                            editBioFieldRef?.text?.toString(),
                            selectedExam
                        )
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
                    text = "\uD83D\uDCF7"
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
                identityContent.addView(avatarWrapper)

                // User Display Name
                val nameText = TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 20f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                identityContent.addView(nameText)

                // User Email
                val emailText = TextView(this).apply {
                    text = userEmail
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.60f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(2), 0, dp(10))
                }
                identityContent.addView(emailText)

                // Account Badge Chip
                val accountBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "Google Account" else "Guest Session"
                    setTextColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"), 30), 12f)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, if (currentProfile.bio.isNotBlank()) dp(12) else 0)
                    }
                }
                identityContent.addView(accountBadge)

                // Bio / Motto Quote (if present)
                if (currentProfile.bio.isNotBlank()) {
                    val mottoView = TextView(this).apply {
                        text = "\u201C${currentProfile.bio}\u201D"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.ITALIC)
                        gravity = Gravity.CENTER
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 18), 10f)
                        setPadding(dp(14), dp(7), dp(14), dp(7))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(dp(12), 0, dp(12), 0)
                        }
                    }
                    identityContent.addView(mottoView)
                }

                identityCard.addView(identityContent)
                layout.addView(identityCard)

                // ----------------------------------------------------
                // NON-GOOGLE SIGN IN CALLOUT
                // ----------------------------------------------------
                if (!isGoogleAuth) {
                    val signInCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#4285F4"), 35), 16f)
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dp(12), 0, dp(4))
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
                        alpha = 0.70f
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
                    layout.addView(signInCard)
                } else {
                    // ----------------------------------------------------
                    // MODERATION STATUS BANNERS (DISMISSIBLE / TRANSIENT)
                    // ----------------------------------------------------
                    if (currentProfile.moderationStatus == ModerationStatus.REJECTED && !ProfileManager.isRejectionAcknowledged(this)) {
                        val rejectedCard = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 35), 14f)
                            setPadding(dp(14), dp(12), dp(14), dp(12))
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, dp(12), 0, 0)
                            }
                        }
                        val bannerHeader = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        val bannerTitle = TextView(this).apply {
                            text = "\uD83D\uDEE1\uFE0F Profile Edit Rejected"
                            setTextColor(Color.parseColor("#EF4444"))
                            textSize = 13.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        bannerHeader.addView(bannerTitle)

                        val dismissBtn = TextView(this).apply {
                            text = "Dismiss \u2715"
                            setTextColor(Color.parseColor("#EF4444"))
                            textSize = 11.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 45), 10f)
                            setPadding(dp(10), dp(4), dp(10), dp(4))
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                ProfileManager.acknowledgeRejection(this@with)
                                rejectedCard.visibility = View.GONE
                            }
                        }
                        bannerHeader.addView(dismissBtn)
                        rejectedCard.addView(bannerHeader)

                        val bannerMsg = currentProfile.rejectionReason ?: "Your previous requested name was not approved by moderators. Your profile has been reset to safe defaults. You can choose a new display name below."
                        rejectedCard.addView(TextView(this).apply {
                            text = bannerMsg
                            setTextColor(themeCoordinator.textColor)
                            alpha = 0.85f
                            textSize = 12f
                            setPadding(0, dp(4), 0, 0)
                        })
                        layout.addView(rejectedCard)
                    } else if (currentProfile.moderationStatus == ModerationStatus.PENDING_APPROVAL) {
                        val pendingCard = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#F59E0B"), 35), 14f)
                            setPadding(dp(14), dp(12), dp(14), dp(12))
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, dp(12), 0, 0)
                            }
                        }
                        val reqName = currentProfile.pendingDisplayName
                        val hasNameReview = !reqName.isNullOrBlank() && reqName != currentProfile.displayName
                        val bannerTitle = if (hasNameReview) "\u23F3 Display Name Pending Approval" else "\u23F3 Profile Edit Under Review"
                        val bannerMsg = if (hasNameReview) {
                            "Your requested name \"$reqName\" is being reviewed by moderators. Public leaderboards will display \"${currentProfile.displayName}\" until approved."
                        } else {
                            "Your profile changes are being reviewed by moderators. Public leaderboards will show your currently approved details until reviewed."
                        }
                        pendingCard.addView(TextView(this).apply {
                            text = bannerTitle
                            setTextColor(Color.parseColor("#F59E0B"))
                            textSize = 13.5f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        })
                        pendingCard.addView(TextView(this).apply {
                            text = bannerMsg
                            setTextColor(themeCoordinator.textColor)
                            alpha = 0.80f
                            textSize = 12f
                            setPadding(0, dp(4), 0, 0)
                        })
                        layout.addView(pendingCard)
                    }

                    // ----------------------------------------------------
                    // CARD 2: EDIT DETAILS
                    // ----------------------------------------------------
                    layout.addView(createSectionLabel("EDIT DETAILS"))
                    val editDetailsCard = createSettingsCard()
                    val editContent = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(18), dp(18), dp(18), dp(18))
                    }

                    // Field 1: Display Name
                    val nameLabel = TextView(this).apply {
                        text = "Display Name"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(6))
                    }
                    editContent.addView(nameLabel)

                    val editNameField = EditText(this).apply {
                        hint = "e.g. Alex_Studies"
                        setText(draftName ?: (currentProfile.pendingDisplayName ?: currentProfile.displayName))
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 14f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                        setPadding(dp(14), dp(11), dp(14), dp(11))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(14))
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
                    editNameFieldRef = editNameField
                    editContent.addView(editNameField)

                    // Field 2: Focus Track
                    val examLabel = TextView(this).apply {
                        text = "Focus Track"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(6))
                    }
                    editContent.addView(examLabel)

                    val examTracks = listOf("JEE / NEET", "UPSC / Govt", "Board Exams", "College / Uni", "Self-Study", "Programming")
                    val chipsScroll = HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(14))
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
                            typeface = themeCoordinator.getAppTypeface(isBold = isSelected)
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
                                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                    editContent.addView(chipsScroll)

                    // Field 3: Bio & Motto
                    val bioLabel = TextView(this).apply {
                        text = "Bio & Motto"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setPadding(dp(2), 0, 0, dp(6))
                    }
                    editContent.addView(bioLabel)

                    val editBioField = EditText(this).apply {
                        hint = "Add a brief bio or study goal..."
                        setText(draftBio ?: currentProfile.bio)
                        setTextColor(themeCoordinator.textColor)
                        setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                        textSize = 13.5f
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(16))
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
                    editBioFieldRef = editBioField
                    editContent.addView(editBioField)

                    // Save Changes Button
                    val saveProfileBtn = Button(this).apply {
                        text = "Save Changes"
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                        setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                                text = "\uD83D\uDEE1\uFE0F Profile Verification"
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
                                text = "Display Name: $inputName"
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
                                    text = "Motto: \"$inputBio\""
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
                                text = "\u2713 Submit Profile"
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
                                            avatarPresetId = "",
                                            avatarUrl = currentProfile.avatarUrl
                                        )

                                        parentSaveBtn.isEnabled = true
                                        parentSaveBtn.text = "Save Changes"

                                        when (result) {
                                            is ProfileSyncService.SubmissionResult.Success -> {
                                                ProfileManager.clearDraftForm(this@with)
                                                AppToast.show(this@with, result.message, Toast.LENGTH_LONG)
                                                host.tabPageCache.remove(host.settingsTabKey(AppSettingsTab.PROFILE))
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
                                                host.tabPageCache.remove(host.settingsTabKey(AppSettingsTab.PROFILE))
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
                    editContent.addView(saveProfileBtn)
                    editDetailsCard.addView(editContent)
                    layout.addView(editDetailsCard)
                }

                // ----------------------------------------------------
                // CARD 3: ACCOUNT & DATA ACTIONS
                // ----------------------------------------------------
                layout.addView(createSectionLabel("ACCOUNT & DATA"))
                val accountActionsCard = createSettingsCard().apply {
                    val actionsLayout = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }

                    // Navigation to Cloud & Backup settings
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
                        text = "\u2601"
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
                            "Connected \u00B7 Manage backups, export & import"
                        } else {
                            "Offline \u00B7 Sign in to back up study sessions"
                        }
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.60f
                        textSize = 11.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                    cloudNavRow.addView(syncCol)

                    val chevron = TextView(this@with).apply {
                        text = "Manage \u203A"
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
                            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 35), 12f)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                                setMargins(0, dp(10), 0, dp(8))
                            }
                            setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            showDeleteAccountDialog()
                        }
                    }
                    actionsLayout.addView(deleteAccountLink)

                    addView(actionsLayout)
                }
                layout.addView(accountActionsCard)
            }

            // ==========================================
            // 2b. LEADERBOARD SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.COMMUNITY) {
                val isGoogleAuth = AuthManager.isLoggedIn(this)
                val userName = ProfileManager.getEffectiveDisplayName(this).ifBlank {
                    AuthManager.getUserName(this) ?: if (isGoogleAuth) "Google User" else "Guest Learner"
                }
                val avatarInitials = userName.take(1).uppercase(Locale.ROOT).ifEmpty { "G" }

                // CARD 1: PUBLIC IDENTITY
                layout.addView(createSectionLabel("PUBLIC IDENTITY"))
                val identityCard = createSettingsCard()

                val profileRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }

                // Avatar Frame (44dp - sleek & compact)
                val avatarWrapper = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                        setMargins(0, 0, dp(14), 0)
                    }
                }

                val avatarCircle = TextView(this).apply {
                    text = avatarInitials
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(LocalAvatarManager.getGoogleAvatarColor(userName))
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
                }
                avatarWrapper.addView(avatarCircle)

                val avatarImg = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(1.5f), themeCoordinator.primaryColor)
                    }
                    visibility = View.GONE
                }
                avatarWrapper.addView(avatarImg)

                val customAvatar = LocalAvatarManager.getCircularAvatarBitmap(this, dp(44))
                if (customAvatar != null) {
                    avatarImg.setImageBitmap(customAvatar)
                    avatarImg.visibility = View.VISIBLE
                    avatarCircle.visibility = View.GONE
                } else {
                    val remoteUrl = ProfileManager.getEffectiveAvatarUrl(this)
                    if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val bitmap = LocalAvatarManager.getCircularBitmapFromUrl(this@SettingsPanelBuilder.host, remoteUrl, dp(44))
                            if (bitmap != null) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    avatarImg.setImageBitmap(bitmap)
                                    avatarImg.visibility = View.VISIBLE
                                    avatarCircle.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                profileRow.addView(avatarWrapper)

                // Info: Name + Auth Badge
                val infoCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                infoCol.addView(TextView(this).apply {
                    text = userName
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val authBadge = TextView(this).apply {
                    text = if (isGoogleAuth) "✓ Google Account" else "👤 Guest"
                    setTextColor(if (isGoogleAuth) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(0, dp(2), 0, 0)
                }
                infoCol.addView(authBadge)
                profileRow.addView(infoCol)

                // Edit Profile Button
                val editBtn = TextView(this).apply {
                    text = "Edit ›"
                    textSize = 12.5f
                    gravity = Gravity.CENTER
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        HapticFeedbackHelper.performMicroHaptic(host, it)
                        currentSettingsTab = AppSettingsTab.PROFILE
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                profileRow.addView(editBtn)
                identityCard.addView(profileRow)
                layout.addView(identityCard)

                // CARD 2: PREFERENCES
                layout.addView(createSectionLabel("LEADERBOARD"))
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
                        title = "Pause Leaderboard?",
                        message = "You will be hidden from rankings. Your personal stats and streaks remain safe.",
                        confirmText = "Pause",
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
                        AppToast.show(this@with, "Leaderboard paused")
                    }
                }

                fun resumeParticipation() {
                    LeaderboardManager.setParticipating(this@with, true)
                    participateSwitchRef?.isChecked = true
                    leaderboardSubOptions.visibility = View.VISIBLE
                    AppToast.show(this@with, "Leaderboard active")
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val todayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val realSecs = LeaderboardManager.getRealTimerFocusSecondsForDate(this@with, todayKeyFmt).toInt()
                        LeaderboardManager.overrideLeaderboardDailyTotal(this@with, realSecs)
                    }
                }

                val partSwitch = SwitchMaterial(this).apply {
                    isChecked = isParticipating
                    setOnClickListener {
                        HapticFeedbackHelper.performMicroHaptic(host, it)
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
                    "",
                    "Show on Leaderboard",
                    "Share your daily study hours on global rankings",
                    partSwitch
                )
                partRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
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
                        HapticFeedbackHelper.performMicroHaptic(host, it)
                        val newState = isChecked
                        LeaderboardManager.setLiveStatusSharingEnabled(this@with, newState)
                        if (!newState) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                LeaderboardManager.updateStudyPresence(this@with, false)
                            }
                        }
                    }
                }

                val liveRow = createSettingsRow(
                    "",
                    "Share Live Study Status",
                    "Show when you're actively studying",
                    liveSwitch
                )
                liveRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
                    val cur = LeaderboardManager.isLiveStatusSharingEnabled(this)
                    val next = !cur
                    LeaderboardManager.setLiveStatusSharingEnabled(this, next)
                    liveSwitch.isChecked = next
                    if (!next) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            LeaderboardManager.updateStudyPresence(this@with, false)
                        }
                    }
                }
                leaderboardSubOptions.addView(liveRow)

                leaderboardSettingsCard.addView(leaderboardSubOptions)
                layout.addView(leaderboardSettingsCard)

                // Subtle Privacy Note
                val privacyNote = TextView(this).apply {
                    text = "Only your display name and study hours are shared."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(16), dp(16), dp(8))
                }
                layout.addView(privacyNote)
            }

            // ==========================================
            // 3. TIMER & FOCUS CONTROLS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.TIMER) {
                // CARD 1: TIMER MODE & DURATIONS
                layout.addView(createSectionLabel("TIMER MODE"))
                val timerModeCard = createSettingsCard()
                val isLecture = timerMode == "LECTURE"
                val isStopwatch = timerMode == "STOPWATCH"
                val isCountdown = timerMode == "COUNTDOWN" || timerMode == "SUBJECT"

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

                val countdownRow = createSettingsRow("", getString(R.string.mode_pomodoro), "Cycles with focus & rest intervals", modeRadio(isCountdown))
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

                val stopwatchRow = createSettingsRow("", getString(R.string.mode_stopwatch), "Open-ended focus tracking", modeRadio(isStopwatch))
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

                val lectureRow = createSettingsRow("", "Class Schedule", "Follow custom timetable", modeRadio(isLecture))
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
                                gravity = Gravity.CENTER
                                minimumWidth = dp(44)
                                minimumHeight = dp(44)
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(themeCoordinator.textColor)
                                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 8f)
                                setPadding(dp(12), dp(8), dp(12), dp(8))
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
                    val focusSubtitle = if (isFreedomMode) "Extended continuous focus (up to 24h)" else "Focus session length"
                    timerModeCard.addView(makeIntervalStepper("Study Duration", focusSubtitle, "study_interval_minutes", 25L, 5L, maxFocus, 5L, "min"))
                    timerModeCard.addView(createDivider())
                    timerModeCard.addView(makeIntervalStepper("Short Break", if (isFreedomMode) "Standard break (bypassed in Freedom Mode)" else "Rest between sessions", "break_interval_minutes", 5L, 1L, 30L, 1L, "min"))
                    timerModeCard.addView(createDivider())

                    // Advanced Break & Automation Options (Conditional: only shown when Freedom Mode is OFF)
                    val advancedIntervalsLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        visibility = if (isFreedomMode) View.GONE else View.VISIBLE
                    }

                    advancedIntervalsLayout.addView(makeIntervalStepper("Long Break", "Rest after multiple sessions", "long_break_minutes", 15L, 5L, 60L, 5L, "min"))
                    advancedIntervalsLayout.addView(createDivider())
                    advancedIntervalsLayout.addView(makeIntervalStepper("Sessions Until Long Break", "Sessions before long rest", "long_break_interval", 4L, 2L, 10L, 1L, "sessions"))
                    advancedIntervalsLayout.addView(createDivider())

                    val isOvertime = sharedPrefs.getBoolean("pomodoro_overtime_enabled", true)
                    val overtimeSwitch = SwitchMaterial(this).apply {
                        isChecked = isOvertime
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_overtime_enabled", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Overtime Flow", "Keep counting past zero to preserve flow", overtimeSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    val isAutoBreak = sharedPrefs.getBoolean("pomodoro_auto_break", true)
                    val autoBreakSwitch = SwitchMaterial(this).apply {
                        isChecked = isAutoBreak
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_auto_break", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Auto-Start Breaks", "Start break when focus finishes", autoBreakSwitch))
                    advancedIntervalsLayout.addView(createDivider())

                    val isAutoFocus = sharedPrefs.getBoolean("pomo_auto_start_focus", true)
                    val autoFocusSwitch = SwitchMaterial(this).apply {
                        isChecked = isAutoFocus
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomo_auto_start_focus", isChecked).apply()
                        }
                    }
                    advancedIntervalsLayout.addView(createSettingsRow("", "Auto-Start Next Focus", "Start next focus when break ends", autoFocusSwitch))
                    timerModeCard.addView(advancedIntervalsLayout)

                    val freedomSwitch = SwitchMaterial(this).apply {
                        isChecked = isFreedomMode
                        setOnCheckedChangeListener { _, isChecked ->
                            sharedPrefs.edit().putBoolean("pomodoro_freedom_mode", isChecked).apply()
                            advancedIntervalsLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
                        }
                    }
                    timerModeCard.addView(createDivider())
                    timerModeCard.addView(createSettingsRow("", "Extended Study (Freedom Mode)", "Allow sessions up to 24 hours", freedomSwitch))
                }
                layout.addView(timerModeCard)

                // CARD 2: APP BLOCKER
                layout.addView(createSectionLabel("APP BLOCKER"))
                val shieldCard = createSettingsCard()
                val isBlockerActive = AppBlockerManager.isEnabled(this)
                val blockerDesc = if (isBlockerActive) {
                    val count = AppBlockerManager.getBlockedPackages(this).size
                    "$count apps blocked while studying"
                } else {
                    "Block distracting apps while studying"
                }
                val shieldStatusBadge = TextView(this).apply {
                    text = if (isBlockerActive) "Active" else "Enable ›"
                    setTextColor(if (isBlockerActive) Color.parseColor("#10B981") else themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(
                        tintedColor(if (isBlockerActive) Color.parseColor("#10B981") else themeCoordinator.primaryColor, 35),
                        12f
                    )
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                }
                val blockerRow = createSettingsRow("", "App Blocker", blockerDesc, shieldStatusBadge)
                blockerRow.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    AppBlockerSettingsSheet.show(host) {
                        this@with.buildCurrentPanel()
                    }
                }
                shieldCard.addView(blockerRow)
                layout.addView(shieldCard)

                // CARD 3: CONTROLS & GESTURES
                layout.addView(createSectionLabel("CONTROLS & GESTURES"))
                val controlsCard = createSettingsCard()

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
                controlsCard.addView(createSettingsRow("", "Landscape Fullscreen", "Horizontal distraction-free clock", landscapeSwitch))
                controlsCard.addView(createDivider())

                val isPauseButtonEnabled = sharedPrefs.getBoolean("show_pause_button", true)
                val pauseButtonSwitch = SwitchMaterial(this).apply {
                    isChecked = isPauseButtonEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("show_pause_button", isChecked).apply()
                        if (currentPanel == AppPanel.FOCUS) updateVisualStyles()
                    }
                }
                controlsCard.addView(createSettingsRow("", getString(R.string.pause_button), "Display pause control on active timer", pauseButtonSwitch))
                controlsCard.addView(createDivider())

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
                pauseFaceUpContainer.addView(createSettingsRow("", "Auto-Pause on Face Up", "Pause when picking up or turning phone face up", pauseFaceUpSwitch))

                val flipSwitch = SwitchMaterial(this).apply {
                    isChecked = isFlipEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        FlipToFocusManager.setEnabled(this@with, isChecked)
                        this@with.syncFlipToFocusState()
                        pauseFaceUpContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
                controlsCard.addView(createSettingsRow("", "Flip to Focus", "Turn phone face down to start timer", flipSwitch))
                controlsCard.addView(pauseFaceUpContainer)
                controlsCard.addView(createDivider())

                // Tactile Haptics (Moved here from Theme)
                val isHapticsEnabled = sharedPrefs.getBoolean("haptics_enabled", true)
                val hapticsSwitch = SwitchMaterial(this).apply {
                    isChecked = isHapticsEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("haptics_enabled", isChecked).apply()
                        if (isChecked) performMicroHaptic(this)
                    }
                }
                controlsCard.addView(createSettingsRow("", "Tactile Haptics", "Subtle vibration on button taps & steppers", hapticsSwitch))
                layout.addView(controlsCard)
            }

            // ==========================================
            // 4. INSIGHTS & STATS SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.ANALYTICS) {
                layout.addView(createSectionLabel("CHART DISPLAY"))
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
                chartsCard.addView(createSettingsRow("", getString(R.string.focus_heatmap_setting), "Annual consistency matrix in Stats", heatmapSwitch))
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
                chartsCard.addView(createSettingsRow("", "Subject Breakdown Chart", "Subject distribution in Stats", pieChartSwitch))
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
                chartsCard.addView(createSettingsRowWithView(createCustomDonutIcon(), "Donut Chart Style", "Modern ring chart with center stats", donutChartSwitch))
                layout.addView(chartsCard)

                layout.addView(createSectionLabel("DATA CORRECTIONS"))
                val dataCard = createSettingsCard()
                val adjustRow = createSettingsRow("", "Adjust Today's Study Time", "Add missed minutes or correct today's total", createActionChip("Adjust ›"))
                adjustRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
                    DeveloperToolsHelper.showAdjustTodayTimeDialog(host, themeCoordinator, isDeveloperExtended = false)
                }
                dataCard.addView(adjustRow)
                layout.addView(dataCard)
            }

            // ==========================================
            // 5. GOAL AND REMINDER SUB-SCREEN
            // ==========================================
            else if (currentSettingsTab == AppSettingsTab.PLANNER) {
                // CARD 1: DAILY GOALS
                layout.addView(createSectionLabel("DAILY GOALS"))
                val goalCard = createSettingsCard()
                var currentGoalSecs = StatsEngine.getDailyGoalSecs(sharedPrefs)
                val goalValueText = TextView(this).apply {
                    text = formatGoalLabel(currentGoalSecs)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                var updateStreakSubtitle: (() -> Unit)? = null

                val repeatHandler = Handler(Looper.getMainLooper())
                var holdTicks = 0

                fun commitGoal() {
                    GoalHistoryManager.setDailyGoal(host, currentGoalSecs)
                    host.invalidateStatsCache()
                    host.recalculateStreak()
                    StudyWidgetProvider.refresh(host)
                }

                fun makeGoalStepBtn(symbol: String, stepSecs: Long): TextView {
                    val btn = TextView(this).apply {
                        text = symbol
                        textSize = 20f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(themeCoordinator.textColor)
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                    }

                    fun performStep(): Boolean {
                        val current = currentGoalSecs
                        val multiplier = when {
                            holdTicks > 30 -> 4L
                            holdTicks > 15 -> 2L
                            else -> 1L
                        }
                        val effectiveDelta = stepSecs * multiplier
                        val next = (current + effectiveDelta).coerceIn(900L, 86400L)
                        if (next != current) {
                            currentGoalSecs = next
                            goalValueText.text = formatGoalLabel(next)
                            updateStreakSubtitle?.invoke()
                            HapticFeedbackHelper.performMicroHaptic(host, btn)
                            return true
                        }
                        return false
                    }

                    val repeatRunnable: Runnable = object : Runnable {
                        override fun run() {
                            holdTicks++
                            val changed = performStep()
                            if (changed) {
                                val nextDelay = when {
                                    holdTicks > 30 -> 60L
                                    holdTicks > 15 -> 100L
                                    holdTicks > 6 -> 180L
                                    else -> 260L
                                }
                                repeatHandler.postDelayed(this, nextDelay)
                            }
                        }
                    }

                    btn.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                holdTicks = 0
                                performStep()
                                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).start()
                                repeatHandler.postDelayed(repeatRunnable, 350L)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (event.x < 0 || event.x > v.width || event.y < 0 || event.y > v.height) {
                                    repeatHandler.removeCallbacks(repeatRunnable)
                                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                    commitGoal()
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                repeatHandler.removeCallbacks(repeatRunnable)
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                                commitGoal()
                            }
                        }
                        true
                    }

                    return btn
                }

                val goalRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                val goalTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                goalTextCol.addView(TextView(this).apply { text = getString(R.string.daily_goal); setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
                goalTextCol.addView(TextView(this).apply { text = "Target daily study time"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
                goalRow.addView(goalTextCol)
                val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                controls.addView(makeGoalStepBtn("−", -900L))
                controls.addView(goalValueText)
                controls.addView(makeGoalStepBtn("+", 900L))
                goalRow.addView(controls)
                goalCard.addView(goalRow)
                goalCard.addView(createDivider())

                val isStreakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", true)
                val streakGoalSwitch = SwitchMaterial(this).apply {
                    isChecked = isStreakGoalBased
                }
                fun getStreakSubtitle(isGoalBased: Boolean): String {
                    return if (isGoalBased) {
                        val goalLabel = formatGoalLabel(currentGoalSecs)
                        "Reach $goalLabel daily to maintain streak"
                    } else {
                        "Any focus session maintains streak"
                    }
                }
                val streakRow = createSettingsRow("", getString(R.string.streak_uses_goal), getStreakSubtitle(isStreakGoalBased), streakGoalSwitch)
                val streakSubtitleView = (streakRow.getChildAt(0) as? LinearLayout)?.let { col ->
                    col.getChildAt(1) as? TextView
                }
                updateStreakSubtitle = {
                    streakSubtitleView?.text = getStreakSubtitle(streakGoalSwitch.isChecked)
                }
                streakGoalSwitch.setOnCheckedChangeListener { _, isChecked ->
                    HapticFeedbackHelper.performMicroHaptic(host)
                    sharedPrefs.edit().putBoolean("streak_uses_daily_goal", isChecked).apply()
                    updateStreakSubtitle?.invoke()
                    host.invalidateStatsCache()
                    host.recalculateStreak()
                    StudyWidgetProvider.refresh(host)
                }
                goalCard.addView(streakRow)
                layout.addView(goalCard)

                // CARD 2: REMINDERS
                layout.addView(createSectionLabel("REMINDERS"))
                val reminderCard = createSettingsCard()

                val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
                val reminderTimeContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = if (reminderEnabled) View.VISIBLE else View.GONE
                }

                val reminderSwitch = SwitchMaterial(this).apply {
                    isChecked = reminderEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        HapticFeedbackHelper.performMicroHaptic(host)
                        sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()
                        if (isChecked) {
                            GoalReminderScheduler.schedule(context)
                        } else {
                            GoalReminderScheduler.cancel(context)
                        }
                        reminderTimeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
                reminderCard.addView(createSettingsRow("", "Daily Goal Reminder", "Evening notification if goal is not reached", reminderSwitch))

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
                        HapticFeedbackHelper.performMicroHaptic(host, it)
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
                    text = "Evening reminder alert time"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    setPadding(0, 3, 0, 0)
                })
                timeRow.addView(timeTextCol)
                timeRow.addView(timeLabel)

                reminderTimeContainer.addView(createDivider())
                reminderTimeContainer.addView(timeRow)
                reminderCard.addView(reminderTimeContainer)
                layout.addView(reminderCard)
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
                val exportRow = createSettingsRow("", getString(R.string.export_logs), getString(R.string.export_logs_sub), createActionChip("Export ›"))
                exportRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
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

                val importRow = createSettingsRow("", getString(R.string.import_data), getString(R.string.import_data_sub), createActionChip("Restore ›"))
                importRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
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
                var isStudySelected = true // true = Study ring, false = Break ring

                var updateHeroRingAndPills: (() -> Unit)? = null
                var updateColorStudioDisplays: (() -> Unit)? = null
                var updateRingSegmentPills: (() -> Unit)? = null

                // ==========================================
                // 1. LIVE INTERACTIVE THEME PREVIEW (Hero Card)
                // ==========================================
                val heroPreviewCard = createSettingsCard()
                val heroContent = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(16), dp(18), dp(16))
                }

                val liveRingView = object : View(this) {
                    private val trackPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dpF(5.5f)
                    }
                    private val progressPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = dpF(5.5f)
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    private val timeTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    private val tagTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = dpF(9f)
                        isFakeBoldText = true
                        letterSpacing = 0.10f
                    }
                    private val ringBounds = android.graphics.RectF()

                    override fun onDraw(canvas: android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val w = width.toFloat()
                        val h = height.toFloat()
                        val pad = dpF(6f)
                        ringBounds.set(pad, pad, w - pad, h - pad)

                        val ringColor = if (isStudySelected) themeCoordinator.primaryColor else themeCoordinator.secondaryColor

                        // Outer track
                        trackPaint.color = tintedColor(ringColor, 40)
                        canvas.drawOval(ringBounds, trackPaint)

                        // Progress arc (~72%)
                        progressPaint.color = ringColor
                        canvas.drawArc(ringBounds, -90f, 260f, false, progressPaint)

                        // Center countdown time
                        timeTextPaint.color = themeCoordinator.textColor
                        timeTextPaint.textSize = dpF(14.5f)
                        timeTextPaint.typeface = FontStyleManager.getTypeface(this@with, FontStyleManager.getSelectedFontId(this@with))
                        val textY = (h / 2f) + dpF(1.5f)
                        canvas.drawText(if (isStudySelected) "25:00" else "05:00", w / 2f, textY, timeTextPaint)

                        // Mode label
                        tagTextPaint.color = tintedColor(themeCoordinator.textColor, 150)
                        canvas.drawText(if (isStudySelected) "FOCUS" else "BREAK", w / 2f, textY + dpF(11.5f), tagTextPaint)
                    }
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(dp(84), dp(84)).apply {
                        rightMargin = dp(16)
                    }
                }
                heroContent.addView(liveRingView)

                val heroInfoCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val liveBadgeRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(5))
                }
                val liveDot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                        rightMargin = dp(6)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#10B981"))
                    }
                }
                val liveTitle = TextView(this).apply {
                    text = "LIVE PREVIEW"
                    textSize = 11.5f
                    letterSpacing = 0.08f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(tintedColor(themeCoordinator.textColor, 175))
                }
                liveBadgeRow.addView(liveDot)
                liveBadgeRow.addView(liveTitle)
                heroInfoCol.addView(liveBadgeRow)

                val heroThemeDesc = TextView(this).apply {
                    val modeStr = when (themeCoordinator.activeBgMode) {
                        "LIGHT" -> "Light"
                        "ECLIPSE" -> "Midnight"
                        else -> "OLED"
                    }
                    val styleStr = if (themeCoordinator.isGlassStyle()) "Glass" else "Flat"
                    text = "$modeStr \u2022 $styleStr"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                }
                heroInfoCol.addView(heroThemeDesc)

                val colorPillsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(7), 0, 0)
                }

                val studyPillDot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(5) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(themeCoordinator.primaryColor)
                    }
                }
                val studyPillTxt = TextView(this).apply {
                    text = "Study"
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                }
                val studyPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 60), 10f)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(8)
                    }
                    addView(studyPillDot)
                    addView(studyPillTxt)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (!isStudySelected) {
                            HapticFeedbackHelper.performMicroHaptic(host, it)
                            isStudySelected = true
                            updateHeroRingAndPills?.invoke()
                            updateRingSegmentPills?.invoke()
                            updateColorStudioDisplays?.invoke()
                        }
                    }
                }

                val breakPillDot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(5) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(themeCoordinator.secondaryColor)
                    }
                }
                val breakPillTxt = TextView(this).apply {
                    text = "Break"
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.secondaryColor)
                }
                val breakPill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.secondaryColor, 30), 10f)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    addView(breakPillDot)
                    addView(breakPillTxt)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (isStudySelected) {
                            HapticFeedbackHelper.performMicroHaptic(host, it)
                            isStudySelected = false
                            updateHeroRingAndPills?.invoke()
                            updateRingSegmentPills?.invoke()
                            updateColorStudioDisplays?.invoke()
                        }
                    }
                }
                colorPillsRow.addView(studyPill)
                colorPillsRow.addView(breakPill)
                heroInfoCol.addView(colorPillsRow)

                heroContent.addView(heroInfoCol)
                heroPreviewCard.addView(heroContent)
                layout.addView(heroPreviewCard)

                updateHeroRingAndPills = {
                    liveRingView.invalidate()
                    (studyPillDot.background as? GradientDrawable)?.setColor(themeCoordinator.primaryColor)
                    studyPillTxt.setTextColor(themeCoordinator.primaryColor)
                    studyPill.background = if (isStudySelected) {
                        themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 10f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
                    }

                    (breakPillDot.background as? GradientDrawable)?.setColor(themeCoordinator.secondaryColor)
                    breakPillTxt.setTextColor(themeCoordinator.secondaryColor)
                    breakPill.background = if (!isStudySelected) {
                        themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 10f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.secondaryColor, 35), 10f)
                    }
                }

                // ==========================================
                // 2. BACKGROUND & CARD STYLE (Merged Grouped Card)
                // ==========================================
                layout.addView(createSectionLabel("BACKGROUND & CARD STYLE"))
                val bgAndStyleCard = createSettingsCard()
                val bgAndStyleContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }

                val bgRowTitle = TextView(this).apply {
                    text = "Background"
                    textSize = 12f
                    letterSpacing = 0.04f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    setPadding(dp(2), 0, 0, dp(8))
                }
                bgAndStyleContainer.addView(bgRowTitle)

                val activeMode = themeCoordinator.activeBgMode
                data class BgOption(val key: String, val name: String, val surfaceColor: Int, val isSelected: Boolean)
                val bgOptions = listOf(
                    BgOption("OLED", "OLED", 0xFF000000.toInt(), activeMode != "ECLIPSE" && activeMode != "LIGHT"),
                    BgOption("ECLIPSE", "Midnight", 0xFF0F172A.toInt(), activeMode == "ECLIPSE"),
                    BgOption("LIGHT", "Light", 0xFFF8FAFC.toInt(), activeMode == "LIGHT")
                )
                val bgTilesRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                for ((i, opt) in bgOptions.withIndex()) {
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        setPadding(dp(8), dp(10), dp(8), dp(10))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            if (i > 0) leftMargin = dp(8)
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(opt.surfaceColor)
                            if (opt.isSelected) {
                                setStroke(dp(2), themeCoordinator.primaryColor)
                            } else {
                                setStroke(dp(1), if (opt.key == "LIGHT") 0xFFCBD5E1.toInt() else 0x2EFFFFFF)
                            }
                        }
                        isClickable = true
                        isFocusable = true
                    }

                    val disk = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                            rightMargin = dp(7)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            if (opt.key == "LIGHT") {
                                setColor(0xFFFFFFFF.toInt())
                                setStroke(dp(1.2f), 0xFF94A3B8.toInt())
                            } else {
                                setColor(if (opt.key == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF121212.toInt())
                                setStroke(dp(1.2f), 0x33FFFFFF)
                            }
                        }
                    }
                    tile.addView(disk)

                    val label = TextView(this).apply {
                        text = opt.name
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(if (opt.key == "LIGHT") 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt())
                    }
                    tile.addView(label)

                    tile.setOnClickListener {
                        HapticFeedbackHelper.performMicroHaptic(host, it)
                        pendingSettingsScrollY = settingsScrollView.scrollY
                        sharedPrefs.edit().putString("activeBgMode", opt.key).apply()
                        themeCoordinator.applyThemeCoordinates()
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                    bgTilesRow.addView(tile)
                }
                bgAndStyleContainer.addView(bgTilesRow)

                val dividerBg = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        setMargins(0, dp(14), 0, dp(12))
                    }
                    setBackgroundColor(tintedColor(themeCoordinator.textColor, 25))
                }
                bgAndStyleContainer.addView(dividerBg)

                val styleRowTitle = TextView(this).apply {
                    text = "Card Style"
                    textSize = 12f
                    letterSpacing = 0.04f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    setPadding(dp(2), 0, 0, dp(8))
                }
                bgAndStyleContainer.addView(styleRowTitle)

                val isGlass = themeCoordinator.isGlassStyle()
                val styleRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val styleOptions = listOf(
                    Pair("GLASS", "\u2726 Glass"),
                    Pair("CLASSIC", "\u25A0 Flat")
                )
                for ((idx, sOpt) in styleOptions.withIndex()) {
                    val isSelected = (sOpt.first == "GLASS" && isGlass) || (sOpt.first == "CLASSIC" && !isGlass)
                    val sTile = TextView(this).apply {
                        text = sOpt.second
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            if (idx > 0) leftMargin = dp(8)
                        }
                        if (isSelected) {
                            setTextColor(themeCoordinator.primaryColor)
                            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                        } else {
                            setTextColor(themeCoordinator.textColor)
                            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 12f)
                        }
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            HapticFeedbackHelper.performMicroHaptic(host, it)
                            pendingSettingsScrollY = settingsScrollView.scrollY
                            sharedPrefs.edit().putString("ui_style", sOpt.first).apply()
                            themeCoordinator.applyThemeCoordinates()
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                    }
                    styleRow.addView(sTile)
                }
                bgAndStyleContainer.addView(styleRow)
                bgAndStyleCard.addView(bgAndStyleContainer)
                layout.addView(bgAndStyleCard)

                // ==========================================
                // 3. THEME COLORS (Unified Studio)
                // ==========================================
                val colorHeader = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(6), dp(18), dp(6), dp(8))
                }
                colorHeader.addView(TextView(this).apply {
                    text = "THEME COLORS"
                    textSize = 12f
                    letterSpacing = 0.12f
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                val shuffleBtn = TextView(this).apply {
                    text = "\uD83C\uDFB2 Shuffle Colors"
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
                        updateVisualStyles()
                        tabPageCache.clear()

                        HapticFeedbackHelper.performMicroHaptic(host, v)
                        updateRingSegmentPills?.invoke()
                        updateColorStudioDisplays?.invoke()
                        updateHeroRingAndPills?.invoke()
                        AppToast.show(this@with, "Fresh theme colors applied!")
                    }
                }
                colorHeader.addView(shuffleBtn)
                layout.addView(colorHeader)

                // Unified Color Studio Card
                val colorStudioCard = createSettingsCard()
                val studioContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(16))
                }

                val ringSegmentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 0, 0, dp(14))
                }
                val studyTabPill = TextView(this)
                val breakTabPill = TextView(this)

                updateRingSegmentPills = {
                    studyTabPill.apply {
                        text = "\u23F1\uFE0F Study Ring"
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(if (isStudySelected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        background = if (isStudySelected) {
                            themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                        } else {
                            themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 12f)
                        }
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            rightMargin = dp(6)
                        }
                    }
                    breakTabPill.apply {
                        text = "\u2615 Break Ring"
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(if (!isStudySelected) themeCoordinator.secondaryColor else themeCoordinator.textColor)
                        background = if (!isStudySelected) {
                            themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 12f)
                        } else {
                            themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 12f)
                        }
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(6)
                        }
                    }
                }
                updateRingSegmentPills?.invoke()
                ringSegmentRow.addView(studyTabPill)
                ringSegmentRow.addView(breakTabPill)
                studioContainer.addView(ringSegmentRow)

                val colorDetailRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(14))
                }
                val colorPreviewCircle = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        rightMargin = dp(12)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(themeCoordinator.primaryColor)
                        setStroke(dp(2), Color.argb(120, 255, 255, 255))
                    }
                }
                colorDetailRow.addView(colorPreviewCircle)

                val colorTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val colorTitleView = TextView(this).apply {
                    text = "Study Focus Color"
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                }
                val colorSubView = TextView(this).apply {
                    val hex = String.format("#%06X", 0xFFFFFF and themeCoordinator.primaryColor)
                    text = "Highlights study timer ring & primary buttons \u2022 $hex"
                    textSize = 11.5f
                    alpha = 0.65f
                    setTextColor(themeCoordinator.textColor)
                    setPadding(0, dp(2), 0, 0)
                }
                colorTextCol.addView(colorTitleView)
                colorTextCol.addView(colorSubView)
                colorDetailRow.addView(colorTextCol)
                studioContainer.addView(colorDetailRow)

                val swatchesScroll = android.widget.HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    setPadding(0, 0, 0, dp(12))
                }
                val swatchesContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val swatchViewsList = ArrayList<View>()
                val hueSlider = android.widget.SeekBar(this)
                val hueReadout = TextView(this)

                updateColorStudioDisplays = {
                    val targetColor = if (isStudySelected) themeCoordinator.primaryColor else themeCoordinator.secondaryColor
                    val targetHex = String.format("#%06X", 0xFFFFFF and targetColor)
                    (colorPreviewCircle.background as? GradientDrawable)?.setColor(targetColor)

                    if (isStudySelected) {
                        colorTitleView.text = "Study Focus Color"
                        colorSubView.text = "Highlights study timer ring & primary buttons \u2022 $targetHex"
                    } else {
                        colorTitleView.text = "Break Rest Color"
                        colorSubView.text = "Highlights rest timers & break status badges \u2022 $targetHex"
                    }

                    val paletteNow = if (isStudySelected) ThemeCoordinator.SOFT_FOCUS_PALETTE else ThemeCoordinator.SOFT_BREAK_PALETTE
                    for ((idx, sv) in swatchViewsList.withIndex()) {
                        if (idx < paletteNow.size) {
                            val hex = paletteNow[idx]
                            val sColor = Color.parseColor(hex)
                            val isMatch = targetHex.equals(hex, ignoreCase = true)
                            sv.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(sColor)
                                setStroke(dp(if (isMatch) 3 else 1), if (isMatch) Color.WHITE else 0x33FFFFFF)
                            }
                        }
                    }

                    val targetHsv = FloatArray(3)
                    Color.colorToHSV(targetColor, targetHsv)
                    hueSlider.progress = targetHsv[0].toInt()
                    hueReadout.text = "${targetHsv[0].toInt()}\u00B0"
                }

                val currentPalette = ThemeCoordinator.SOFT_FOCUS_PALETTE
                for (hex in currentPalette) {
                    val sColor = Color.parseColor(hex)
                    val sv = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                            rightMargin = dp(9)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(sColor)
                            val isMatch = String.format("#%06X", 0xFFFFFF and themeCoordinator.primaryColor).equals(hex, ignoreCase = true)
                            setStroke(dp(if (isMatch) 3 else 1), if (isMatch) Color.WHITE else 0x33FFFFFF)
                        }
                        setOnClickListener {
                            HapticFeedbackHelper.performMicroHaptic(host, it)
                            val paletteNow = if (isStudySelected) ThemeCoordinator.SOFT_FOCUS_PALETTE else ThemeCoordinator.SOFT_BREAK_PALETTE
                            val clickedHex = paletteNow[swatchViewsList.indexOf(this)]
                            val pickedColor = Color.parseColor(clickedHex)
                            if (isStudySelected) {
                                themeCoordinator.primaryColor = pickedColor
                                sharedPrefs.edit().putInt("customPrimary", pickedColor).apply()
                            } else {
                                themeCoordinator.secondaryColor = pickedColor
                                sharedPrefs.edit().putInt("customSecondary", pickedColor).apply()
                            }
                            updateVisualStyles()
                            tabPageCache.clear()
                            updateColorStudioDisplays?.invoke()
                            updateHeroRingAndPills?.invoke()
                        }
                    }
                    swatchViewsList.add(sv)
                    swatchesContainer.addView(sv)
                }
                swatchesScroll.addView(swatchesContainer)
                studioContainer.addView(swatchesScroll)

                val hueHeaderRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(4), 0, dp(6))
                }
                val hueTitle = TextView(this).apply {
                    text = "Fine-Tune Color Shade"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                hueReadout.apply {
                    val hsv = FloatArray(3)
                    Color.colorToHSV(themeCoordinator.primaryColor, hsv)
                    text = "${hsv[0].toInt()}\u00B0"
                    textSize = 12f
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.65f
                }
                hueHeaderRow.addView(hueTitle)
                hueHeaderRow.addView(hueReadout)
                studioContainer.addView(hueHeaderRow)

                val rainbowColors = intArrayOf(
                    Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
                )
                val rainbowTrack = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, rainbowColors).apply {
                    cornerRadius = dp(3).toFloat()
                }

                val sliderContainer = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(32))
                }
                val trackBar = View(this).apply {
                    background = rainbowTrack
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(5)).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setMargins(dp(12), 0, dp(12), 0)
                    }
                }
                sliderContainer.addView(trackBar)

                hueSlider.apply {
                    max = 360
                    val hsv = FloatArray(3)
                    Color.colorToHSV(themeCoordinator.primaryColor, hsv)
                    progress = hsv[0].toInt()
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val thumbDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setSize(dp(18), dp(18))
                        setColor(themeCoordinator.primaryColor)
                        setStroke(dp(2), Color.WHITE)
                    }
                    thumb = thumbDrawable

                    setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: android.widget.SeekBar?, progressVal: Int, fromUser: Boolean) {
                            if (fromUser) {
                                hueReadout.text = "${progressVal}\u00B0"
                                val currentHsv = FloatArray(3)
                                val baseColor = if (isStudySelected) themeCoordinator.primaryColor else themeCoordinator.secondaryColor
                                Color.colorToHSV(baseColor, currentHsv)
                                currentHsv[0] = progressVal.toFloat()
                                if (currentHsv[1] < 0.35f) currentHsv[1] = 0.65f
                                if (currentHsv[2] < 0.70f) currentHsv[2] = 0.95f
                                val chosenColor = Color.HSVToColor(currentHsv)

                                thumbDrawable.setColor(chosenColor)
                                (colorPreviewCircle.background as? GradientDrawable)?.setColor(chosenColor)

                                if (isStudySelected) {
                                    themeCoordinator.primaryColor = chosenColor
                                    sharedPrefs.edit().putInt("customPrimary", chosenColor).apply()
                                } else {
                                    themeCoordinator.secondaryColor = chosenColor
                                    sharedPrefs.edit().putInt("customSecondary", chosenColor).apply()
                                }
                                updateVisualStyles()
                                tabPageCache.clear()
                                updateHeroRingAndPills?.invoke()
                            }
                        }
                        override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                        override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {
                            updateColorStudioDisplays?.invoke()
                            updateHeroRingAndPills?.invoke()
                        }
                    })
                }
                sliderContainer.addView(hueSlider)
                studioContainer.addView(sliderContainer)

                studyTabPill.setOnClickListener {
                    if (!isStudySelected) {
                        HapticFeedbackHelper.performMicroHaptic(host, it)
                        isStudySelected = true
                        updateRingSegmentPills?.invoke()
                        updateColorStudioDisplays?.invoke()
                        updateHeroRingAndPills?.invoke()
                    }
                }
                breakTabPill.setOnClickListener {
                    if (isStudySelected) {
                        HapticFeedbackHelper.performMicroHaptic(host, it)
                        isStudySelected = false
                        updateRingSegmentPills?.invoke()
                        updateColorStudioDisplays?.invoke()
                        updateHeroRingAndPills?.invoke()
                    }
                }

                colorStudioCard.addView(studioContainer)
                layout.addView(colorStudioCard)

                // ==========================================
                // 4. DISPLAY & WALLPAPER (Grouped Settings Card)
                // ==========================================
                layout.addView(createSectionLabel("DISPLAY & WALLPAPER"))
                val displayCard = createSettingsCard()

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
                val isLiveWp = wallpaperType.startsWith("LIVE_") || wallpaperType == WallpaperManager.TYPE_LIVE_AURORA

                val wallpaperRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    isClickable = true
                    isFocusable = true
                }

                val wpPreviewFrame = FrameLayout(this).apply {
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, dp(10).toFloat())
                        }
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(0xFF1E293B.toInt())
                        setStroke(dp(1), 0x33FFFFFF)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(42)).apply {
                        rightMargin = dp(14)
                    }
                }

                val wpImg = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                when (wallpaperType) {
                    WallpaperManager.TYPE_PRESET -> {
                        val preset = WallpaperManager.getPresetById(this, activePresetId)
                        val thumb = if (preset != null) WallpaperManager.loadPresetThumbnail(this, preset) else null
                        if (thumb != null) wpImg.setImageBitmap(thumb) else wpImg.setBackgroundColor(0xFF334155.toInt())
                    }
                    WallpaperManager.TYPE_STATIC -> {
                        val file = WallpaperManager.getCustomWallpaperFile(this)
                        if (file.exists()) {
                            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                            val bm = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                            if (bm != null) wpImg.setImageBitmap(bm)
                        }
                    }
                    WallpaperManager.TYPE_LIVE_AURORA,
                    WallpaperManager.TYPE_LIVE_COSMIC,
                    WallpaperManager.TYPE_LIVE_LOFI_RAIN,
                    WallpaperManager.TYPE_LIVE_VIDEO -> {
                        val liveThumb = WallpaperManager.getLiveWallpaperThumbnail(this, wallpaperType)
                        if (liveThumb != null) {
                            wpImg.setImageBitmap(liveThumb)
                        } else {
                            val fallbackColor = when (wallpaperType) {
                                WallpaperManager.TYPE_LIVE_AURORA -> 0xFF065F46.toInt()
                                WallpaperManager.TYPE_LIVE_COSMIC -> 0xFF1E1B4B.toInt()
                                WallpaperManager.TYPE_LIVE_VIDEO -> 0xFF1E1E2E.toInt()
                                else -> 0xFF1E293B.toInt()
                            }
                            wpImg.setBackgroundColor(fallbackColor)
                        }
                    }
                    else -> wpImg.setBackgroundColor(if (themeCoordinator.isDarkMode()) 0xFF121212.toInt() else 0xFFE2E8F0.toInt())
                }
                wpPreviewFrame.addView(wpImg)
                wallpaperRow.addView(wpPreviewFrame)

                val wpTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val wpTitle = TextView(this).apply {
                    text = "Study Wallpaper"
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                }
                wpTextCol.addView(wpTitle)

                val wpSub = TextView(this).apply {
                    text = if (isLiveWp) "\u2728 Live Motion \u2022 $currentTitle" else "Active: $currentTitle"
                    textSize = 12f
                    setTextColor(tintedColor(themeCoordinator.textColor, 150))
                    setPadding(0, dp(2), 0, 0)
                }
                wpTextCol.addView(wpSub)
                wallpaperRow.addView(wpTextCol)

                val changeWpChip = TextView(this).apply {
                    text = "Change \u203A"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                }
                wallpaperRow.addView(changeWpChip)

                wallpaperRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
                    showWallpaperGalleryDialog()
                }
                displayCard.addView(wallpaperRow)

                displayCard.addView(createDivider())

                // Typography Row
                val currentFontId = FontStyleManager.getSelectedFontId(this)
                val currentFontItem = FontStyleManager.FONT_OPTIONS.find { it.id == currentFontId } ?: FontStyleManager.FONT_OPTIONS[0]

                val fontBadge = TextView(this).apply {
                    text = "${currentFontItem.displayName} \u203A"
                    textSize = 12.5f
                    typeface = FontStyleManager.getTypeface(this@with, currentFontItem.id)
                    setTextColor(themeCoordinator.primaryColor)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
                    setPadding(dp(11), dp(5), dp(11), dp(5))
                }

                val fontRow = createSettingsRow(
                    "",
                    "Timer & App Font",
                    "Digits & headings: ${currentFontItem.displayName}",
                    fontBadge
                )
                fontRow.setOnClickListener {
                    HapticFeedbackHelper.performMicroHaptic(host, it)
                    showFontSelectionDialog()
                }
                displayCard.addView(fontRow)

                displayCard.addView(createDivider())

                // Minimal White Focus Clock Row
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
                displayCard.addView(
                    createSettingsRow(
                        "",
                        "Minimal White Focus Clock",
                        "Distraction-free pure white timer ring during focus sessions",
                        pureWhitePomoSwitch
                    )
                )

                layout.addView(displayCard)
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

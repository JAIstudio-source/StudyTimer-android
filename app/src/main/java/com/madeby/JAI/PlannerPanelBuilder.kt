package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class PlannerPanelBuilder(private val host: MainActivity) {

    enum class PlannerFilter { ALL, ACTIVE, DONE }
    private var currentPlannerFilter: PlannerFilter = PlannerFilter.ALL
    enum class PlannerSort { DEFAULT, TIME_ASC, TIME_DESC }
    private var currentPlannerSort: PlannerSort = runCatching {
        PlannerSort.valueOf(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("planner_sort_mode", "DEFAULT") ?: "DEFAULT")
    }.getOrDefault(PlannerSort.DEFAULT)
    private var plannerScrollView: ScrollView? = null

    private val themeCoordinator get() = host.themeCoordinator
    private val statsEngine get() = host.statsEngine
    private val dateKeyFmt get() = host.dateKeyFmt
    private val cachedTodayStr get() = host.cachedTodayStr
    private var statsSnapshotCache
        get() = host.statsSnapshotCache
        set(value) { host.statsSnapshotCache = value }

    private fun dp(v: Int): Int = host.dp(v)
    private fun dp(v: Float): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)
    private fun rippleBackground(color: Int) = host.rippleBackground(color)
    private fun computeStatsSnapshot() = host.computeStatsSnapshot()
    private fun resolvePlannerColors() = host.resolvePlannerColors()
    private fun resolveGoalFor(dateStr: String) = host.resolveGoalFor(dateStr)
    private fun loadSessionGoalsFromJson(jsonStr: String) = host.loadSessionGoalsFromJson(jsonStr)
    private fun saveSessionGoalsToJson(goals: List<SessionGoal>) = host.saveSessionGoalsToJson(goals)
    private fun getStatusBarHeight() = host.getStatusBarHeight()
    private fun showSubjectPickerDialog() = host.showSubjectPickerDialog()
    private fun showAddCustomSubjectDialog(onCreated: ((SubjectTag) -> Unit)? = null) = host.showAddCustomSubjectDialog(onCreated)
    private var isDevModeUnlocked get() = host.isDevModeUnlocked; set(v) { host.isDevModeUnlocked = v }
    private var timerMode get() = host.timerMode; set(v) { host.timerMode = v }
    private var statsDirty get() = host.statsDirty; set(v) { host.statsDirty = v }
    private val tabPageCache get() = host.tabPageCache
    private fun recalculateStreak() = host.recalculateStreak()
    private fun refreshStatsPanel() = host.refreshStatsPanel()
    private fun createSectionLabel(text: String): TextView = host.createSectionLabel(text)
    private fun createDivider(): View = host.createDivider()
    private fun showPlannerThemePickerDialog() = host.showPlannerThemePickerDialog()
    private fun showConfirmDialog(title: String, message: String, confirmText: String = "Confirm", isDestructive: Boolean = false, onConfirm: () -> Unit) =
        DeveloperToolsHelper.showThemedConfirmDialog(host, themeCoordinator, title, message, confirmText, isDestructive, onConfirm = onConfirm)
    private fun getSystemService(name: String): Any? = host.getSystemService(name)

    fun build(target: ViewGroup = host.panelContainer) {
        val root = FrameLayout(host).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val mainCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val (plannerPrimary, _) = resolvePlannerColors()
        val isPomoWhite = host.isPomodoroPureWhiteActive()
        val isDark = themeCoordinator.isDarkMode()

        val overflowBtn = ImageView(host).apply {
            setImageDrawable(ContextCompat.getDrawable(host, R.drawable.ic_more_horiz))
            val iconBg = if (isPomoWhite) {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFF1F5F9.toInt())
                }
            } else {
                themeCoordinator.createGlassIconBackground(tintedColor(plannerPrimary, if (isDark) 65 else 90))
            }
            background = iconBg
            setColorFilter(if (isPomoWhite) 0xFF0F172A.toInt() else plannerPrimary)
            setPadding(dp(9), dp(9), dp(9), dp(9))
            contentDescription = "Planner options"
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                setMargins(0, 0, dp(4), 0)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                showPlannerOptionsMenu(target, this)
            }
        }

        val header = UniversalHeaderHelper.createHeader(host, extraRightView = overflowBtn, title = "PLANNER")
        mainCol.addView(header)

        val scroll = object : ScrollView(host) {
            override fun requestChildFocus(child: View?, focused: View?) {
                // Prevent automatic scroll jumping when child views gain focus or check state changes
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            tag = "planner_scroll_view"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setOnScrollChangeListener { _, _, _, scrollY, _ ->
                    if (scrollY >= 0) {
                        host.pendingPlannerScrollY = scrollY
                    }
                }
            } else {
                viewTreeObserver.addOnScrollChangedListener {
                    if (this.scrollY >= 0) {
                        host.pendingPlannerScrollY = this.scrollY
                    }
                }
            }
        }
        plannerScrollView = scroll
        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(84))
        }
        val snap = statsSnapshotCache ?: computeStatsSnapshot().also { statsSnapshotCache = it }
        renderPlannerTabContent(content, snap)
        scroll.addView(content)
        mainCol.addView(scroll)
        root.addView(mainCol)

        val fab = TextView(host).apply {
            text = "+"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                val (plannerPrimary, _) = resolvePlannerColors()
                setColor(plannerPrimary)
            }
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, dp(20), dp(80))
            }
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                showAddSessionGoalDialog()
            }
        }
        root.addView(fab)

        val oldScrollY = host.pendingPlannerScrollY.takeIf { it > 0 }
            ?: (target.findViewWithTag<ScrollView>("planner_scroll_view")?.scrollY ?: 0)
        target.removeAllViews()
        target.addView(root)
        if (oldScrollY > 0) {
            scroll.post {
                scroll.scrollTo(0, oldScrollY)
            }
            scroll.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (scroll.viewTreeObserver.isAlive) {
                        scroll.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    scroll.scrollTo(0, oldScrollY)
                    return true
                }
            })
        }
        FontStyleManager.applyRecursively(root)
    }

    private fun showPlannerOptionsMenu(target: ViewGroup, anchorView: View) {
        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, _) = resolvePlannerColors()
        val textColor = themeCoordinator.textColor

        val cardWidth = dp(230)

        val card = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(18f)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            layoutParams = ViewGroup.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            elevation = dp(16).toFloat()
        }

        val popupWindow = PopupWindow(
            card,
            cardWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(16).toFloat()
        }

        var isDismissing = false
        fun dismissWithAnim(onEnd: (() -> Unit)? = null) {
            if (isDismissing) return
            isDismissing = true
            card.animate()
                .scaleX(0.72f)
                .scaleY(0.72f)
                .alpha(0f)
                .translationY(-dp(8).toFloat())
                .setDuration(160)
                .setInterpolator(AccelerateInterpolator(1.6f))
                .withEndAction {
                    try { popupWindow.dismiss() } catch (_: Exception) {}
                    onEnd?.invoke()
                }
                .start()
        }

        // Header label
        val titleView = TextView(host).apply {
            text = "PLANNER OPTIONS"
            textSize = 10.5f
            letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(plannerPrimary)
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        card.addView(titleView)

        fun createMenuItem(
            iconRes: Int,
            iconTint: Int,
            title: String,
            badgeText: String? = null,
            titleColor: Int = textColor,
            onClick: () -> Unit
        ): View {
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val rowMask = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.WHITE)
                }
                val rowContent = themeCoordinator.createGlassChip(Color.TRANSPARENT, 10f)
                background = RippleDrawable(
                    ColorStateList.valueOf(tintedColor(plannerPrimary, if (isDark) 50 else 35)),
                    rowContent,
                    rowMask
                )
                setPadding(dp(10), dp(9), dp(10), dp(9))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(1)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    dismissWithAnim { onClick() }
                }
            }

            val iconPill = FrameLayout(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(tintedColor(iconTint, if (isDark) 35 else 22))
                    setStroke(dp(1), tintedColor(iconTint, if (isDark) 60 else 40))
                }
            }
            val iconView = ImageView(host).apply {
                setImageResource(iconRes)
                setColorFilter(iconTint)
                layoutParams = FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
            }
            iconPill.addView(iconView)
            row.addView(iconPill)

            val titleView = TextView(host).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(titleColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleView)

            if (badgeText != null) {
                val badgeView = TextView(host).apply {
                    text = badgeText
                    textSize = 10f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(iconTint)
                    background = themeCoordinator.createGlassChip(tintedColor(iconTint, if (isDark) 40 else 25), 6f)
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                }
                row.addView(badgeView)
            }

            return row
        }

        // 1. Weekly Goal Tracker
        card.addView(createMenuItem(
            R.drawable.ic_calendar,
            plannerPrimary,
            "Weekly Goal Tracker"
        ) {
            showPlannerMatrixDialog()
        })

        // 2. Edit Yesterday Goals
        val skyBlue = if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        card.addView(createMenuItem(
            R.drawable.ic_clock,
            skyBlue,
            "Edit Yesterday Goals"
        ) {
            showEditYesterdayGoalsDialog()
        })

        // 3. Sort Goals
        val sortAccent = if (isDark) 0xFFA78BFA.toInt() else 0xFF8B5CF6.toInt()
        val sortBadge = when (currentPlannerSort) {
            PlannerSort.DEFAULT -> null
            PlannerSort.TIME_ASC -> "Time ↑"
            PlannerSort.TIME_DESC -> "Time ↓"
        }
        card.addView(createMenuItem(
            R.drawable.ic_sort,
            sortAccent,
            "Sort Goals",
            badgeText = sortBadge
        ) {
            showGoalSortDialog()
        })

        // Subtle Hairline Divider before destructive action
        val divider = View(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                setMargins(dp(8), dp(4), dp(8), dp(4))
            }
            setBackgroundColor(tintedColor(textColor, if (isDark) 24 else 18))
        }
        card.addView(divider)

        // 4. Clear Completed Goals (with safety confirmation dialog)
        val dangerRed = 0xFFEF4444.toInt()
        card.addView(createMenuItem(
            R.drawable.ic_trash,
            dangerRed,
            "Clear Completed Goals",
            titleColor = dangerRed
        ) {
            val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val goalsJson = prefs.getString("session_goals_json", "[]") ?: "[]"
            val currentList = loadSessionGoalsFromJson(goalsJson)
            val completedCount = currentList.count { it.completed }
            if (completedCount == 0) {
                AppToast.show(host, "No completed goals to clear")
            } else {
                showConfirmClearCompletedDialog(target, completedCount, currentList)
            }
        })

        FontStyleManager.applyRecursively(card)

        // Pivot precisely from the 3-dot button right above the card
        card.pivotX = (cardWidth - anchorView.width / 2).toFloat()
        card.pivotY = 0f
        card.scaleX = 0.65f
        card.scaleY = 0.65f
        card.alpha = 0f
        card.translationY = -dp(8).toFloat()

        val xOffset = -(cardWidth - anchorView.width)
        val yOffset = dp(6)
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset)

        try {
            val container = card.rootView
            val wm = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val p = container.layoutParams as WindowManager.LayoutParams
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            p.dimAmount = 0.28f
            wm.updateViewLayout(container, p)
        } catch (_: Exception) {}

        card.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .setInterpolator(PathInterpolator(0.16f, 1f, 0.3f, 1f))
            .start()
    }

    internal fun showGoalSortDialog(onSorted: (() -> Unit)? = null) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val textColor = themeCoordinator.textColor

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(26f)
            setPadding(dp(22), dp(20), dp(22), dp(18))
        }

        content.addView(TextView(host).apply {
            text = "SORT GOALS"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Arrange Daily Goals"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        content.addView(TextView(host).apply {
            text = "Arrange goals by target study time:"
            setTextColor(tintedColor(textColor, if (isDark) 160 else 170))
            textSize = 12.5f
            setPadding(0, 0, 0, dp(14))
        })

        fun createSortOption(
            sortMode: PlannerSort,
            title: String,
            subtitle: String
        ): View {
            val isSelected = currentPlannerSort == sortMode
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val rowMask = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.WHITE)
                }
                val rowContent = if (isSelected) {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(tintedColor(plannerPrimary, if (isDark) 40 else 25))
                        setStroke(dp(1.5f), plannerPrimary)
                    }
                } else {
                    if (isDark) {
                        themeCoordinator.createGlassChip(Color.argb(25, 255, 255, 255), 14f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(0xFFF8FAFC.toInt())
                            setStroke(dp(1), 0xFFE2E8F0.toInt())
                        }
                    }
                }
                background = RippleDrawable(
                    ColorStateList.valueOf(tintedColor(plannerPrimary, 40)),
                    rowContent,
                    rowMask
                )
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    currentPlannerSort = sortMode
                    host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                        .putString("planner_sort_mode", sortMode.name).apply()
                    dialog.dismiss()
                    onSorted?.invoke() ?: host.buildPlannerPanel()
                }
            }

            val radioIndicator = FrameLayout(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    rightMargin = dp(12)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (isSelected) {
                        setColor(plannerPrimary)
                    } else {
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(1.5f), tintedColor(textColor, if (isDark) 100 else 120))
                    }
                }
            }
            if (isSelected) {
                val innerDot = View(host).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(8), dp(8), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.WHITE)
                    }
                }
                radioIndicator.addView(innerDot)
            }
            row.addView(radioIndicator)

            val textCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(host).apply {
                text = title
                setTextColor(if (isSelected) plannerPrimary else textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            })
            textCol.addView(TextView(host).apply {
                text = subtitle
                setTextColor(tintedColor(textColor, if (isDark) 140 else 150))
                textSize = 11.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            })
            row.addView(textCol)

            return row
        }

        content.addView(createSortOption(
            PlannerSort.DEFAULT,
            "Default Order",
            "Manual drag-and-drop order"
        ))

        content.addView(createSortOption(
            PlannerSort.TIME_ASC,
            "Shortest Time First",
            "Ascending (e.g. 15m → 60m → 120m)"
        ))

        content.addView(createSortOption(
            PlannerSort.TIME_DESC,
            "Longest Time First",
            "Descending (e.g. 120m → 60m → 15m)"
        ))

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            setTextColor(tintedColor(textColor, if (isDark) 170 else 150))
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                topMargin = dp(4)
            }
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(cancelBtn)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun showConfirmClearCompletedDialog(target: ViewGroup, completedCount: Int, currentList: List<SessionGoal>) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val textColor = themeCoordinator.textColor

        val card = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(22f)
            setPadding(dp(22), dp(20), dp(22), dp(18))
            layoutParams = FrameLayout.LayoutParams(dp(300), FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        val titleTv = TextView(host).apply {
            text = "Clear Completed Goals?"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, 0, 0, dp(8))
        }
        card.addView(titleTv)

        val msgTv = TextView(host).apply {
            text = "Archive $completedCount finished goal${if (completedCount > 1) "s" else ""}? Your active goals will remain untouched."
            textSize = 13.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(tintedColor(textColor, 180))
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, 0, 0, dp(18))
        }
        card.addView(msgTv)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(tintedColor(textColor, 170))
            background = themeCoordinator.createGlassChip(Color.TRANSPARENT, 10f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
            }
        }
        btnRow.addView(cancelBtn)

        val clearBtn = TextView(host).apply {
            text = "Clear ($completedCount)"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(0xFFEF4444.toInt())
            }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(8)
            }
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                dialog.dismiss()
                val remaining = currentList.filter { !it.completed }
                saveSessionGoalsToJson(remaining)
                PlannerHistoryManager.snapshotToday(host, remaining)
                build(target)
                AppToast.show(host, "Cleared $completedCount completed goal${if (completedCount > 1) "s" else ""}")
            }
        }
        btnRow.addView(clearBtn)
        card.addView(btnRow)

        val wrapper = FrameLayout(host).apply {
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(card)
        }
        dialog.setContentView(wrapper)
        FontStyleManager.applyRecursively(card)
        dialog.show()
    }

    internal fun renderPlannerTabContent(parent: LinearLayout, snap: StatsSnapshot) {
        parent.clipChildren = false
        parent.clipToPadding = false
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val goalsJson = prefs.getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }
        val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(host, todayStr)
        val dailyGoalDurations = PlannerHistoryManager.getDailyGoalDurations(host, todayStr)
        val dailySubTopicDurations = mutableMapOf<String, Map<String, Long>>()
        for (g in goalsList) {
            val subId = g.subjectId
            if (!subId.isNullOrBlank() && subId != "all" && !dailySubTopicDurations.containsKey(subId)) {
                dailySubTopicDurations[subId] = SubjectTagManager.getDailySubTopicDurations(host, subId, todayStr)
            }
        }
        val todayFocusSecs = snap.todayFocus
        val todayCal = Calendar.getInstance()
        fun isGoalScheduledToday(goal: PlannerGoal): Boolean {
            return PlannerHistoryManager.isGoalScheduledOnDay(goal.scheduledDays, todayCal)
        }

        // Only goals scheduled for today participate in active study duration allocation
        val scheduledGoals = goalsList.filter { isGoalScheduledToday(it) }
        val progressMap = PlannerHistoryManager.calculateGoalProgress(
            scheduledGoals, todayFocusSecs, dailySubjectDurations, dailyGoalDurations, dailySubTopicDurations
        )

        fun reloadPlanner() {
            val scroll = plannerScrollView ?: (parent.parent as? ScrollView) ?: host.findPlannerScrollView()
            val savedY = scroll?.scrollY?.takeIf { it > 0 } ?: host.pendingPlannerScrollY
            if (savedY > 0) {
                host.pendingPlannerScrollY = savedY
            }
            val oldHeight = parent.height
            if (oldHeight > 0) {
                parent.minimumHeight = oldHeight
            }
            val curSnap = statsSnapshotCache ?: computeStatsSnapshot().also { statsSnapshotCache = it }
            parent.removeAllViews()
            renderPlannerTabContent(parent, curSnap)
            if (scroll != null && savedY > 0) {
                scroll.post {
                    parent.minimumHeight = 0
                    scroll.scrollTo(0, savedY)
                }
                scroll.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (scroll.viewTreeObserver.isAlive) {
                            scroll.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        parent.minimumHeight = 0
                        scroll.scrollTo(0, savedY)
                        return true
                    }
                })
            } else {
                parent.minimumHeight = 0
            }
        }

        // Auto-complete goals (subject-tagged or untagged) whose target time has reached (only on actively scheduled days)
        var goalsUpdated = false
        val activeGoalsList = goalsList.map { goal ->
            val prog = progressMap[goal.id]
            if (isGoalScheduledToday(goal) && goal.targetMinutes > 0 && prog != null && prog.isAchieved && !goal.completed) {
                goalsUpdated = true
                goal.copy(completed = true, checkedAt = System.currentTimeMillis())
            } else {
                goal
            }
        }.toMutableList()
        if (goalsUpdated) {
            saveSessionGoalsToJson(activeGoalsList)
            PlannerHistoryManager.snapshotToday(host, activeGoalsList)
        }

        fun isGoalDone(goal: SessionGoal): Boolean {
            if (!isGoalScheduledToday(goal)) return false
            val prog = progressMap[goal.id]
            val actualMins = prog?.actualMinutes ?: 0
            return goal.completed || (goal.targetMinutes > 0 && actualMins >= goal.targetMinutes)
        }

        val trulyAchievedIds = mutableSetOf<String>()
        for (goal in activeGoalsList) {
            if (isGoalDone(goal)) {
                trulyAchievedIds.add(goal.id)
            }
        }

        // Daily Target summary strictly reflects goals scheduled for today!
        val scheduledTodayGoals = activeGoalsList.filter { isGoalScheduledToday(it) }
        val completedCount = scheduledTodayGoals.count { trulyAchievedIds.contains(it.id) }
        val totalCount = scheduledTodayGoals.size
        val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        // Upcoming Exam Countdown Capsule (Pinned at Top of Planner - Clean Typography, No Emojis/Pills)
        val upcomingExams = try {
            ExamCountdownManager.getExams(host).filter {
                it.targetTimestampMs >= System.currentTimeMillis() - 86400000L
            }.sortedBy { it.targetTimestampMs }
        } catch (_: Exception) { emptyList() }

        if (upcomingExams.isNotEmpty()) {
            val exam = upcomingExams.first()
            val breakdown = ExamCountdownManager.getCountdownBreakdown(exam.targetTimestampMs)
            val examCapsule = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(8))
                }
                setOnClickListener {
                    CalendarTimeline(host).showAddEditExamModal(exam)
                }
            }
            val examTextCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            examTextCol.addView(TextView(host).apply {
                text = "UPCOMING EXAM"
                textSize = 10f
                letterSpacing = 0.12f
                typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(plannerPrimary)
            })
            examTextCol.addView(TextView(host).apply {
                text = exam.title
                textSize = 13.5f
                typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.textColor)
            })
            examTextCol.addView(TextView(host).apply {
                text = breakdown.readableSubtitle.ifBlank { breakdown.mainHeadline }
                textSize = 11f
                setTextColor(tintedColor(themeCoordinator.textColor, 170))
            })
            examCapsule.addView(examTextCol)

            val daysBadge = TextView(host).apply {
                text = breakdown.mainHeadline
                textSize = 12.5f
                typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(plannerPrimary)
                setPadding(dp(8), dp(4), dp(8), dp(4))
            }
            examCapsule.addView(daysBadge)
            parent.addView(examCapsule)
        }

        // 1. Daily Target Summary Card (Clean, Refined, No Emoji)
        val summaryCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val topRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(host).apply {
            text = "DAILY TARGET"
            setTextColor(plannerPrimary)
            textSize = 11.5f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
            letterSpacing = 0.12f
        })
        topRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })

        val gridBtn = TextView(host).apply {
            text = "History ›"
            setTextColor(plannerPrimary)
            textSize = 12f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
            setPadding(dp(8), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            contentDescription = "Weekly Study History"
            setOnClickListener { showPlannerMatrixDialog() }
        }
        topRow.addView(gridBtn)
        summaryCard.addView(topRow)

        val isGoalReached = progressPct >= 100 && totalCount > 0
        val lineProgressColor = if (isGoalReached) 0xFF43D36E.toInt() else plannerPrimary

        val progressRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(4)) }
        progressRow.addView(TextView(host).apply {
            tag = "planner_completed_text"
            text = "$completedCount of $totalCount completed"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
        })
        progressRow.addView(LinearLayout(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        progressRow.addView(TextView(host).apply {
            tag = "planner_pct_text"
            text = "$progressPct%"
            setTextColor(lineProgressColor)
            textSize = 14f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
        })
        summaryCard.addView(progressRow)

        val progressBar = ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal).apply {
            tag = "planner_progress_bar"
            max = 100
            progress = progressPct
            progressTintList = android.content.res.ColorStateList.valueOf(lineProgressColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(if (themeCoordinator.isDarkMode()) 0xFF222430.toInt() else 0xFFE2E8F0.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply { setMargins(0, dp(2), 0, dp(2)) }
        }
        summaryCard.addView(progressBar)
        parent.addView(summaryCard)

        // 2. Filter Tabs (Dynamic high-contrast glass capsules adapting to light and dark wallpapers)
        val activeCount = (totalCount - completedCount).coerceAtLeast(0)
        val isPomoWhite = host.isPomodoroPureWhiteActive()
        val isWallpaperActive = WallpaperManager.getWallpaperType(host) != "NONE"
        val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(host)
        val isBrightSurface = if (isWallpaperActive) isBrightUnderTimer else (isPomoWhite || !themeCoordinator.isDarkMode())

        val filterRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(2), 0, dp(8))
            }
        }

        fun createFilterTab(filter: PlannerFilter, label: String, count: Int): TextView {
            val isSelected = currentPlannerFilter == filter
            return TextView(host).apply {
                tag = "filter_tab_$filter"
                text = "$label ($count)"
                textSize = 12f
                typeface = host.themeCoordinator.getAppTypeface(isBold = isSelected)
                setPadding(dp(14), dp(7), dp(14), dp(7))

                if (isBrightSurface) {
                    // Bright wallpaper: Solid dark slate capsule matching the cards for maximum contrast
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setColor(plannerPrimary)
                        } else {
                            setColor(0xFF1E293B.toInt())
                            setStroke(dp(1), 0xFF334155.toInt())
                        }
                    }
                    setTextColor(if (isSelected) ThemeCoordinator.getContrastColor(plannerPrimary) else Color.WHITE)
                } else {
                    // Dark wallpaper: Translucent glass capsule with glowing selected state
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setColor(plannerPrimary)
                        } else {
                            setColor(0x22FFFFFF.toInt())
                            setStroke(dp(1), 0x33FFFFFF.toInt())
                        }
                    }
                    setTextColor(if (isSelected) ThemeCoordinator.getContrastColor(plannerPrimary) else tintedColor(themeCoordinator.textColor, 220))
                }

                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (currentPlannerFilter != filter) {
                        currentPlannerFilter = filter
                        reloadPlanner()
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
        }

        filterRow.addView(createFilterTab(PlannerFilter.ALL, "All", totalCount))
        filterRow.addView(createFilterTab(PlannerFilter.ACTIVE, "Remaining", activeCount))
        filterRow.addView(createFilterTab(PlannerFilter.DONE, "Done", completedCount))
        parent.addView(filterRow)

        val filteredGoals = when (currentPlannerFilter) {
            PlannerFilter.ALL -> activeGoalsList.filter { isGoalScheduledToday(it) }
            PlannerFilter.ACTIVE -> activeGoalsList.filter { isGoalScheduledToday(it) && !isGoalDone(it) }
            PlannerFilter.DONE -> activeGoalsList.filter { isGoalScheduledToday(it) && isGoalDone(it) }
        }

        val otherDaysGoalsToRender = if (currentPlannerFilter == PlannerFilter.ALL) {
            activeGoalsList.filterNot { isGoalScheduledToday(it) }
        } else {
            emptyList()
        }

        val displayedGoals = when (currentPlannerSort) {
            PlannerSort.DEFAULT -> filteredGoals
            PlannerSort.TIME_ASC -> filteredGoals.sortedWith(
                compareBy<PlannerGoal> { if (it.targetMinutes <= 0) Int.MAX_VALUE else it.targetMinutes }
                    .thenBy { it.title }
            )
            PlannerSort.TIME_DESC -> filteredGoals.sortedWith(
                compareByDescending<PlannerGoal> { it.targetMinutes }
                    .thenBy { it.title }
            )
        }

        if (displayedGoals.isEmpty() && otherDaysGoalsToRender.isEmpty()) {
            val emptyFilterView = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = themeCoordinator.createCardBackground()
                setPadding(dp(20), dp(28), dp(20), dp(28))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(12))
                }
            }
            val emptyMsg = when {
                activeGoalsList.isEmpty() -> "No Goals Set for Today"
                currentPlannerFilter == PlannerFilter.ACTIVE -> "All daily goals completed!"
                currentPlannerFilter == PlannerFilter.DONE -> "No completed goals yet."
                else -> "No goals found."
            }
            emptyFilterView.addView(TextView(host).apply {
                text = emptyMsg
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                gravity = Gravity.CENTER
            })
            if (activeGoalsList.isEmpty()) {
                val addFirstBtn = TextView(host).apply {
                    text = "+ Add Daily Goal"
                    setTextColor(Color.WHITE)
                    textSize = 13.5f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(plannerPrimary)
                    }
                    setPadding(dp(18), dp(9), dp(18), dp(9))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(14)
                    }
                    setOnClickListener { showAddSessionGoalDialog() }
                }
                emptyFilterView.addView(addFirstBtn)
            }
            parent.addView(emptyFilterView)
        } else {
            val goalsContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                clipChildren = false
                clipToPadding = false
            }

            if (displayedGoals.isEmpty()) {
                val emptyFilterView = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(24), dp(20), dp(24))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(10))
                    }
                }
                val emptyMsg = when {
                    scheduledGoals.isEmpty() -> "No Goals Scheduled for Today"
                    currentPlannerFilter == PlannerFilter.ACTIVE -> "All daily goals completed!"
                    currentPlannerFilter == PlannerFilter.DONE -> "No completed goals yet."
                    else -> "No active goals found."
                }
                emptyFilterView.addView(TextView(host).apply {
                    text = emptyMsg
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    gravity = Gravity.CENTER
                })
                goalsContainer.addView(emptyFilterView)
            }

            class GoalDisplayInfo(
                val isChecked: Boolean,
                val checkSymbol: String,
                val progressText: String,
                val chipColor: Int,
                val isDeficit: Boolean,
                val isGreyMark: Boolean
            )

            val infoMap = mutableMapOf<String, GoalDisplayInfo>()
            val greenColor = 0xFF22C55E.toInt()
            val greyColor = if (themeCoordinator.isDarkMode()) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
            val greyBgColor = if (themeCoordinator.isDarkMode()) 0xFF475569.toInt() else 0xFF64748B.toInt()

            fun computeGoalDisplayInfo(goal: SessionGoal): GoalDisplayInfo {
                val targetMins = goal.targetMinutes
                val prog = progressMap[goal.id]
                val actualMins = prog?.actualMinutes ?: 0
                val isScheduledToday = isGoalScheduledToday(goal)

                if (!isScheduledToday) {
                    return GoalDisplayInfo(
                        isChecked = false,
                        checkSymbol = "—",
                        progressText = if (targetMins > 0) "${targetMins}m" else "",
                        chipColor = tintedColor(themeCoordinator.textColor, 70),
                        isDeficit = false,
                        isGreyMark = false
                    )
                }

                if (targetMins > 0) {
                    val isDone = actualMins >= targetMins
                    val remainingMins = (targetMins - actualMins).coerceAtLeast(0)
                    val remText = if (actualMins > 0) "${actualMins}/${targetMins}m · ${remainingMins}m left" else "${remainingMins}m left"

                    if (isDone) {
                        return GoalDisplayInfo(
                            isChecked = true,
                            checkSymbol = "✓",
                            progressText = "${actualMins}/${targetMins}m",
                            chipColor = greenColor,
                            isDeficit = false,
                            isGreyMark = false
                        )
                    } else if (goal.completed) {
                        return GoalDisplayInfo(
                            isChecked = true,
                            checkSymbol = "✓",
                            progressText = remText,
                            chipColor = greyColor,
                            isDeficit = false,
                            isGreyMark = true
                        )
                    } else {
                        return GoalDisplayInfo(
                            isChecked = false,
                            checkSymbol = "",
                            progressText = remText,
                            chipColor = plannerPrimary,
                            isDeficit = false,
                            isGreyMark = false
                        )
                    }
                } else {
                    if (goal.completed) {
                        return GoalDisplayInfo(
                            isChecked = true,
                            checkSymbol = "✓",
                            progressText = if (actualMins > 0) "${actualMins}m" else "Done",
                            chipColor = greenColor,
                            isDeficit = false,
                            isGreyMark = false
                        )
                    } else {
                        return GoalDisplayInfo(
                            isChecked = false,
                            checkSymbol = "",
                            progressText = if (actualMins > 0) "${actualMins}m" else "",
                            chipColor = plannerPrimary,
                            isDeficit = false,
                            isGreyMark = false
                        )
                    }
                }
            }

            for (goal in activeGoalsList) {
                infoMap[goal.id] = computeGoalDisplayInfo(goal)
            }

            fun updateFilterCounts() {
                val curScheduledToday = activeGoalsList.filter { isGoalScheduledToday(it) }
                val curTotal = curScheduledToday.size
                val curDone = curScheduledToday.count { isGoalDone(it) }
                val curActive = (curTotal - curDone).coerceAtLeast(0)
                parent.findViewWithTag<TextView>("filter_tab_ALL")?.text = "All ($curTotal)"
                parent.findViewWithTag<TextView>("filter_tab_ACTIVE")?.text = "Remaining ($curActive)"
                parent.findViewWithTag<TextView>("filter_tab_DONE")?.text = "Done ($curDone)"
                val curPct = if (curTotal > 0) (curDone * 100) / curTotal else 0
                parent.findViewWithTag<TextView>("planner_completed_text")?.text = "$curDone of $curTotal completed"
                parent.findViewWithTag<TextView>("planner_pct_text")?.text = "$curPct%"
                parent.findViewWithTag<ProgressBar>("planner_progress_bar")?.progress = curPct
            }

            val toggleGoalAction: (SessionGoal) -> Unit = { goalToToggle ->
                val now = System.currentTimeMillis()
                val targetIndex = activeGoalsList.indexOfFirst { it.id == goalToToggle.id }
                if (targetIndex != -1) {
                    val currentGoal = activeGoalsList[targetIndex]
                    val newCompleted = !currentGoal.completed
                    val updatedGoal = currentGoal.copy(completed = newCompleted, checkedAt = if (newCompleted) now else 0L)
                    activeGoalsList[targetIndex] = updatedGoal

                    host.isLocalPlannerUpdateInProgress = true
                    try {
                        saveSessionGoalsToJson(activeGoalsList)
                    } finally {
                        host.isLocalPlannerUpdateInProgress = false
                    }

                    if (updatedGoal.completed) {
                        GoalReminderScheduler.cancelGoalReminder(host, updatedGoal.id)
                    } else if (updatedGoal.reminderEnabled) {
                        GoalReminderScheduler.scheduleGoalReminder(host, updatedGoal)
                    }
                    PlannerHistoryManager.snapshotToday(host, activeGoalsList)
                    host.updateStatsLiveViews()

                    val newInfo = computeGoalDisplayInfo(updatedGoal)
                    infoMap[updatedGoal.id] = newInfo

                    // Immediately update checkbox state without lag
                    parent.findViewWithTag<TextView>("goal_check_${updatedGoal.id}")?.apply {
                        text = if (isGoalScheduledToday(updatedGoal)) newInfo.checkSymbol else "—"
                        val checkColor = when {
                            !newInfo.isChecked -> Color.TRANSPARENT
                            newInfo.isGreyMark -> greyBgColor
                            else -> greenColor
                        }
                        val strokeColor = when {
                            !newInfo.isChecked -> plannerPrimary
                            newInfo.isGreyMark -> greyBgColor
                            else -> greenColor
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(checkColor)
                            setStroke(dp(1.8f), strokeColor)
                        }
                    }

                    // Immediately update title strikethrough & styling
                    parent.findViewWithTag<TextView>("goal_title_${updatedGoal.id}")?.apply {
                        setTextColor(if (newInfo.isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                        paintFlags = if (newInfo.isChecked) {
                            paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                    }

                    // Immediately update note alpha
                    parent.findViewWithTag<TextView>("goal_note_${updatedGoal.id}")?.alpha = if (newInfo.isChecked) 0.35f else 0.65f

                    // Immediately update progress & remaining time chip
                    parent.findViewWithTag<TextView>("goal_chip_${updatedGoal.id}")?.apply {
                        text = newInfo.progressText
                        setTextColor(newInfo.chipColor)
                        val shouldShow = newInfo.isChecked || (updatedGoal.targetMinutes > 0 || (progressMap[updatedGoal.id]?.actualMinutes ?: 0) > 0)
                        visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }

                    // Immediately update goal mini progress bar
                    parent.findViewWithTag<ProgressBar>("goal_minibar_${updatedGoal.id}")?.apply {
                        val barColor = when {
                            newInfo.isChecked && !newInfo.isGreyMark -> greenColor
                            newInfo.isChecked && newInfo.isGreyMark -> greyColor
                            else -> plannerPrimary
                        }
                        progressTintList = ColorStateList.valueOf(barColor)
                    }

                    // Immediately update play button visibility
                    parent.findViewWithTag<View>("goal_action_${updatedGoal.id}")?.visibility = if (!newInfo.isChecked) View.VISIBLE else View.GONE

                    updateFilterCounts()

                    if (currentPlannerFilter != PlannerFilter.ALL) {
                        reloadPlanner()
                    }
                }
            }

            val animateToggleGoalAction = { targetView: View, goalToToggle: SessionGoal ->
                targetView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                targetView.animate()
                    .scaleX(1.28f)
                    .scaleY(1.28f)
                    .setDuration(110)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.8f))
                    .withEndAction {
                        targetView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(80)
                            .withEndAction {
                                toggleGoalAction(goalToToggle)
                            }
                            .start()
                    }
                    .start()
            }

            val todayGoalsToRender = displayedGoals

            fun renderGoalItem(goal: SessionGoal, isDayOff: Boolean, targetContainer: LinearLayout = goalsContainer) {
                val info = infoMap[goal.id] ?: return
                val isChecked = info.isChecked
                val targetMins = goal.targetMinutes
                val prog = progressMap[goal.id]
                val actualMins = prog?.actualMinutes ?: 0

                val goalCard = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    clipChildren = false
                    clipToPadding = false
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    if (isDayOff) {
                        alpha = 0.52f
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                }

                // 1. Left: Checkbox (generous 44dp x 44dp touch target to prevent mis-tapping the card)
                val checkWrapper = FrameLayout(host).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                        setMargins(dp(-4), dp(-4), dp(4), dp(-4))
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    isClickable = true
                    isFocusable = true
                }
                val checkBtn = TextView(host).apply {
                    tag = "goal_check_${goal.id}"
                    setTag(R.id.tag_goal_check_state, "${isChecked}_${info.isGreyMark}_${info.chipColor}")
                    text = if (isDayOff) "—" else info.checkSymbol
                    textSize = 12.5f
                    gravity = Gravity.CENTER
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(if (isDayOff) tintedColor(themeCoordinator.textColor, 80) else Color.WHITE)
                    val checkColor = when {
                        isDayOff -> Color.TRANSPARENT
                        !isChecked -> Color.TRANSPARENT
                        info.isGreyMark -> greyBgColor
                        else -> greenColor
                    }
                    val strokeColor = when {
                        isDayOff -> tintedColor(themeCoordinator.textColor, 70)
                        !isChecked -> plannerPrimary
                        info.isGreyMark -> greyBgColor
                        else -> greenColor
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(checkColor)
                        setStroke(dp(1.8f), strokeColor)
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER)
                }
                checkWrapper.addView(checkBtn)
                if (isDayOff) {
                    checkWrapper.setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        showGoalActionMenu(goal) { animateToggleGoalAction(checkBtn, goal) }
                    }
                } else {
                    checkWrapper.setOnClickListener { animateToggleGoalAction(checkBtn, goal) }
                }
                goalCard.addView(checkWrapper)

                // 2. Middle: Content Column (Title, Note, Progression Bar, Scope, Timing)
                val mainContentCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, dp(6), 0)
                    }
                }

                val titleTv = TextView(host).apply {
                    tag = "goal_title_${goal.id}"
                    text = goal.title
                    setTextColor(if (isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                    textSize = 14.5f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    if (isChecked) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                }
                mainContentCol.addView(titleTv)

                if (goal.note.isNotBlank()) {
                    mainContentCol.addView(TextView(host).apply {
                        tag = "goal_note_${goal.id}"
                        text = goal.note
                        setTextColor(themeCoordinator.textColor)
                        alpha = if (isChecked) 0.35f else 0.65f
                        textSize = 11.5f
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setPadding(0, dp(1), 0, 0)
                    })
                }

                if (targetMins > 0) {
                    val pct = ((actualMins * 100) / targetMins).coerceIn(0, 100)
                    val barColor = when {
                        info.isChecked && !info.isGreyMark -> greenColor
                        info.isChecked && info.isGreyMark -> greyColor
                        else -> plannerPrimary
                    }
                    val miniBar = ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal).apply {
                        tag = "goal_minibar_${goal.id}"
                        max = 100
                        progress = pct
                        progressTintList = ColorStateList.valueOf(barColor)
                        progressBackgroundTintList = ColorStateList.valueOf(if (themeCoordinator.isDarkMode()) 0x22FFFFFF else 0x1A000000)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2.5f)).apply {
                            setMargins(0, dp(4), 0, dp(3))
                        }
                    }
                    mainContentCol.addView(miniBar)
                }

                // Metadata Rows (Scope + Timing)
                val hasSubject = !goal.subjectId.isNullOrBlank() && goal.subjectId != "all"
                val hasSubTopic = !goal.subTopic.isNullOrBlank()
                val hasScheduledDays = goal.scheduledDays.isNotEmpty()
                val hasReminder = goal.reminderEnabled

                val hasScope = hasSubject || hasSubTopic
                val hasTiming = hasScheduledDays || hasReminder

                // Line 1: Academic Scope (Subject Badge + Chapter / Sub-Topic)
                if (hasScope) {
                    val scopeRow = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, dp(3), 0, 0)
                        }
                    }

                    val sub = if (hasSubject) SubjectTagManager.resolveSubject(host, goal.subjectId) else null
                    val subColor = sub?.let { try { Color.parseColor(it.colorHex) } catch (_: Exception) { plannerPrimary } } ?: plannerPrimary

                    if (hasSubject && sub != null) {
                        val subBadge = TextView(host).apply {
                            text = sub.name
                            setTextColor(subColor)
                            textSize = 11f
                            isSingleLine = true
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            maxWidth = dp(130)
                            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                            background = themeCoordinator.createGlassChip(tintedColor(subColor, 55), 7f)
                            setPadding(dp(7), dp(2), dp(7), dp(2))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, dp(6), 0)
                            }
                        }
                        scopeRow.addView(subBadge)
                    }

                    if (hasSubTopic) {
                        val topicItem = LinearLayout(host).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        topicItem.addView(ImageView(host).apply {
                            setImageResource(R.drawable.ic_book)
                            imageTintList = ColorStateList.valueOf(subColor)
                            layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                                setMargins(0, 0, dp(3), 0)
                            }
                        })
                        topicItem.addView(TextView(host).apply {
                            text = goal.subTopic
                            setTextColor(subColor)
                            textSize = 11f
                            isSingleLine = true
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            maxWidth = dp(150)
                            typeface = host.themeCoordinator.getAppTypeface(isBold = false)
                        })
                        scopeRow.addView(topicItem)
                    }

                    mainContentCol.addView(scopeRow)
                }

                // Line 2: Timing Context (Scheduled Days + Reminder Alert)
                if (hasTiming) {
                    val timingRow = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, dp(2), 0, 0)
                        }
                    }

                    if (hasScheduledDays) {
                        val daysMap = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                        val daysText = when {
                            goal.scheduledDays.size == 7 -> "Every Day"
                            goal.scheduledDays.size == 5 && goal.scheduledDays.sorted() == listOf(1, 2, 3, 4, 5) -> "Weekdays"
                            goal.scheduledDays.size == 2 && goal.scheduledDays.sorted() == listOf(6, 7) -> "Weekends"
                            else -> goal.scheduledDays.sorted().mapNotNull { daysMap[it] }.joinToString(", ")
                        }
                        val schedItem = LinearLayout(host).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        schedItem.addView(ImageView(host).apply {
                            setImageResource(R.drawable.ic_calendar)
                            imageTintList = ColorStateList.valueOf(tintedColor(themeCoordinator.textColor, 170))
                            layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                                setMargins(0, 0, dp(3), 0)
                            }
                        })
                        schedItem.addView(TextView(host).apply {
                            text = daysText
                            setTextColor(tintedColor(themeCoordinator.textColor, 170))
                            textSize = 10.5f
                            isSingleLine = true
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            maxWidth = dp(160)
                            typeface = host.themeCoordinator.getAppTypeface(isBold = false)
                        })
                        timingRow.addView(schedItem)
                    }

                    if (hasScheduledDays && hasReminder) {
                        timingRow.addView(TextView(host).apply {
                            text = "\u00B7"
                            textSize = 11f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(tintedColor(themeCoordinator.textColor, 70))
                            setPadding(dp(4), 0, dp(4), 0)
                        })
                    }

                    if (hasReminder) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, goal.reminderHour)
                            set(Calendar.MINUTE, goal.reminderMinute)
                        }
                        val reminderTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                        val reminderColor = tintedColor(themeCoordinator.textColor, 170)
                        val reminderItem = LinearLayout(host).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        reminderItem.addView(ImageView(host).apply {
                            setImageResource(R.drawable.ic_clock)
                            imageTintList = ColorStateList.valueOf(reminderColor)
                            layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                                setMargins(0, 0, dp(3), 0)
                            }
                        })
                        reminderItem.addView(TextView(host).apply {
                            text = reminderTimeStr
                            setTextColor(reminderColor)
                            textSize = 10.5f
                            isSingleLine = true
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            typeface = host.themeCoordinator.getAppTypeface(isBold = false)
                        })
                        timingRow.addView(reminderItem)
                    }

                    mainContentCol.addView(timingRow)
                }

                goalCard.addView(mainContentCol)

                // 3. Right: Action Column (Time 0/45m, Start Button, Grip) - Centered Vertically in Card
                val rightCol = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }

                val shouldShowTime = isChecked || (targetMins > 0 || actualMins > 0)
                val chipTv = TextView(host).apply {
                    tag = "goal_chip_${goal.id}"
                    text = info.progressText
                    textSize = 11.5f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(info.chipColor)
                    setPadding(dp(4), 0, dp(4), 0)
                    visibility = if (shouldShowTime) View.VISIBLE else View.GONE
                }
                rightCol.addView(chipTv)

                if (!isDayOff) {
                    val playIcon = FrameLayout(host).apply {
                        tag = "goal_action_${goal.id}"
                        visibility = if (!isChecked) View.VISIBLE else View.GONE
                        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                            setMargins(dp(2), 0, dp(2), 0)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(tintedColor(plannerPrimary, if (themeCoordinator.isDarkMode()) 45 else 25))
                        }
                        isClickable = true
                        isFocusable = true
                        contentDescription = "Start ${goal.title}"
                        val iv = ImageView(host).apply {
                            setImageResource(R.drawable.ic_play_arrow)
                            imageTintList = ColorStateList.valueOf(plannerPrimary)
                            layoutParams = FrameLayout.LayoutParams(dp(13), dp(13), Gravity.CENTER)
                        }
                        addView(iv)
                        setOnClickListener { v ->
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            host.startFocusForGoal(goal)
                        }
                    }
                    rightCol.addView(playIcon)
                }

                if (!isDayOff && activeGoalsList.size > 1) {
                    val gripBtn = TextView(host).apply {
                        text = "\u283F"
                        textSize = 15f
                        setTextColor(tintedColor(themeCoordinator.textColor, 80))
                        setPadding(dp(4), dp(4), dp(2), dp(4))
                    }
                    rightCol.addView(gripBtn)

                    gripBtn.setOnLongClickListener { v ->
                        val clipData = android.content.ClipData.newPlainText("goal_id", goal.id)
                        val shadow = View.DragShadowBuilder(goalCard)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            v.startDragAndDrop(clipData, shadow, goal.id, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            v.startDrag(clipData, shadow, goal.id, 0)
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        true
                    }
                }

                goalCard.addView(rightCol)

                if (isDayOff) {
                    goalCard.setOnClickListener {
                        showGoalActionMenu(goal)
                    }
                    goalCard.setOnLongClickListener { v ->
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        showGoalActionMenu(goal)
                        true
                    }
                } else {
                    goalCard.setOnClickListener {
                        showGoalActionMenu(goal) { animateToggleGoalAction(checkBtn, goal) }
                    }
                    goalCard.setOnLongClickListener { v ->
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        showGoalActionMenu(goal) { animateToggleGoalAction(checkBtn, goal) }
                        true
                    }
                }

                if (!isDayOff && activeGoalsList.size > 1) {
                    goalCard.setOnDragListener { v, event ->
                        when (event.action) {
                            android.view.DragEvent.ACTION_DRAG_STARTED -> true
                            android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                                v.alpha = 0.55f
                                true
                            }
                            android.view.DragEvent.ACTION_DRAG_EXITED -> {
                                v.alpha = 1.0f
                                true
                            }
                            android.view.DragEvent.ACTION_DROP -> {
                                v.alpha = 1.0f
                                val draggedGoalId = event.localState as? String
                                if (!draggedGoalId.isNullOrEmpty() && draggedGoalId != goal.id) {
                                    val fromIdx = activeGoalsList.indexOfFirst { it.id == draggedGoalId }
                                    val toIdx = activeGoalsList.indexOfFirst { it.id == goal.id }
                                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                        val mutable = activeGoalsList.toMutableList()
                                        val moved = mutable.removeAt(fromIdx)
                                        mutable.add(toIdx, moved)
                                        saveSessionGoalsToJson(mutable)
                                        currentPlannerSort = PlannerSort.DEFAULT
                                        host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                                            .putString("planner_sort_mode", "DEFAULT").apply()
                                        PlannerHistoryManager.snapshotToday(host, mutable)
                                        reloadPlanner()
                                    }
                                }
                                true
                            }
                            android.view.DragEvent.ACTION_DRAG_ENDED -> {
                                v.alpha = 1.0f
                                true
                            }
                            else -> true
                        }
                    }
                }

                targetContainer.addView(goalCard)
            }

            todayGoalsToRender.forEach { renderGoalItem(it, isDayOff = false, targetContainer = goalsContainer) }

            if (otherDaysGoalsToRender.isNotEmpty()) {
                val otherDaysContainer = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                }
                otherDaysGoalsToRender.forEach { renderGoalItem(it, isDayOff = true, targetContainer = otherDaysContainer) }

                var isExpanded = false
                val otherDaysHeader = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(10f)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp(10), 0, dp(6))
                    }
                    isClickable = true
                    isFocusable = true
                }
                val otherDaysTitle = TextView(host).apply {
                    text = "OTHER DAYS / DAY OFF (${otherDaysGoalsToRender.size})"
                    textSize = 11f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    letterSpacing = 0.08f
                    setTextColor(tintedColor(themeCoordinator.textColor, 140))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val expandArrow = TextView(host).apply {
                    text = "▾"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(tintedColor(themeCoordinator.textColor, 140))
                }
                otherDaysHeader.addView(otherDaysTitle)
                otherDaysHeader.addView(expandArrow)

                otherDaysHeader.setOnClickListener {
                    isExpanded = !isExpanded
                    otherDaysContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    expandArrow.text = if (isExpanded) "▴" else "▾"
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }

                goalsContainer.addView(otherDaysHeader)
                goalsContainer.addView(otherDaysContainer)
            }

            parent.addView(goalsContainer)
        }

        // Daily Motivational Quote (Placed below goals section)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayQuote = DailyQuotes.getTodayQuote(dayOfYear)

        val quoteCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(8), 0, dp(14))
            }
        }

        val quoteHeader = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        quoteHeader.addView(TextView(host).apply {
            text = "DAILY INSPIRATION"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val quoteText = TextView(host).apply {
            text = todayQuote
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            setLineSpacing(dp(3).toFloat(), 1.15f)
        }

        quoteCard.addView(quoteHeader)
        quoteCard.addView(quoteText)
        parent.addView(quoteCard)

        FontStyleManager.applyRecursively(parent)
    }

    private fun showGoalActionMenu(goal: PlannerGoal, onToggle: (() -> Unit)? = null) {
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val freshGoals = loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
        val currentGoal = freshGoals.find { it.id == goal.id } ?: goal
        val isCompleted = currentGoal.completed

        val (dialog, content) = DialogFactory.createBaseDialog(host, themeCoordinator)
        val (plannerPrimary, _) = resolvePlannerColors()

        content.addView(TextView(host).apply {
            text = currentGoal.title
            setTextColor(plannerPrimary)
            textSize = 16.5f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
            setPadding(0, 0, 0, dp(4))
        })

        if (!currentGoal.note.isNullOrBlank()) {
            content.addView(TextView(host).apply {
                text = currentGoal.note
                textSize = 12f
                setTextColor(tintedColor(themeCoordinator.textColor, 170))
                setPadding(0, 0, 0, dp(10))
            })
        }

        // Action 1: Toggle Completion
        if (onToggle != null) {
            val toggleAction = Button(host).apply {
                text = if (isCompleted) "Mark Incomplete ◯" else "Mark Complete ✓"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(8))
                }
                setOnClickListener {
                    dialog.dismiss()
                    onToggle.invoke()
                }
            }
            content.addView(toggleAction)
        }

        // Action 2: Start Focus
        val focusAction = Button(host).apply {
            text = "▶ Start Focus Timer"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(ThemeCoordinator.getContrastColor(plannerPrimary))
            background = rippleBackground(plannerPrimary)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                host.startFocusForGoal(currentGoal)
            }
        }
        content.addView(focusAction)

        // Action 3: Edit Goal
        val editAction = Button(host).apply {
            text = "✏️ Edit Goal"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createCardBackground(14f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                showEditSessionGoalDialog(currentGoal)
            }
        }
        content.addView(editAction)

        // Action 4: View History
        val historyAction = Button(host).apply {
            text = "📊 View Streaks & History"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createCardBackground(14f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                showGoalHistoryDialog(currentGoal)
            }
        }
        content.addView(historyAction)

        // Action 5: Delete Goal
        val deleteAction = Button(host).apply {
            text = "🗑️ Delete Goal"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.parseColor("#EF4444"))
            background = themeCoordinator.createCardBackground(14f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                showConfirmDialog(
                    title = "Delete Goal?",
                    message = "Are you sure you want to remove '${currentGoal.title}' from your planner?"
                ) {
                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val updated = currentGoals.filter { it.id != currentGoal.id }
                    saveSessionGoalsToJson(updated)
                    GoalReminderScheduler.cancelGoalReminder(host, currentGoal.id)
                    PlannerHistoryManager.snapshotToday(host, updated)
                    refreshStatsPanel()
                }
            }
        }
        content.addView(deleteAction)

        val cancelBtn = Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(cancelBtn)

        dialog.setContentView(content)
        dialog.show()
    }

    internal fun showGoalHistoryDialog(goal: PlannerGoal, displayMonthOffset: Int = 0) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val historyDetailedMap = PlannerHistoryManager.loadGoalHistoryDetailed(host, goal.id, goal.scheduledDays, goal.createdAt)
        val snapshotsMap = PlannerHistoryManager.loadGoalSnapshotsMap(host, goal.id)
        val insights = PlannerHistoryManager.computeGoalInsights(host, goal.id, goal.scheduledDays, goal.createdAt)
        var currentOffset = displayMonthOffset

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // 1. Scrollable Header Box for Goal Title and Optional Note ONLY
        val maxHeaderHeight = dp(110)
        val headerScroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(4))
            }
            isVerticalScrollBarEnabled = true
        }

        val headerBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        headerBox.addView(TextView(host).apply {
            text = goal.title
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        if (goal.note.isNotBlank()) {
            headerBox.addView(TextView(host).apply {
                text = goal.note
                setTextColor(themeCoordinator.textColor)
                alpha = 0.8f
                textSize = 13f
                setLineSpacing(dp(2).toFloat(), 1.1f)
                setPadding(0, dp(4), 0, 0)
            })
        }

        headerScroll.addView(headerBox)

        // Restrict headerScroll height to maxHeaderHeight if content exceeds it
        headerScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                headerScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (headerScroll.height > maxHeaderHeight) {
                    headerScroll.layoutParams = (headerScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxHeaderHeight
                    }
                }
            }
        })
        content.addView(headerScroll)

        // 2. FIXED Stats Summary Chips Row
        val statsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(10))
        }
        fun makeChip(icon: String, textVal: String) {
            val chip = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isDark) {
                    themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 100), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
            }
            if (icon.isNotBlank()) {
                chip.addView(TextView(host).apply { text = icon; textSize = 13f; setPadding(0, 0, dp(4), 0) })
            }
            chip.addView(TextView(host).apply {
                text = textVal
                setTextColor(if (isDark) themeCoordinator.textColor else 0xFF0F172A.toInt())
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            statsRow.addView(chip)
        }
        makeChip("", "${insights.currentStreak}d streak")
        makeChip("", "${(insights.completionRate * 100).toInt()}% complete")
        makeChip("", "${insights.completedDays}/${insights.totalDaysTracked} days")
        content.addView(statsRow)

        content.addView(createDivider())

        // 3. FIXED Calendar Grid View Container
        val calendarContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateKeys = historyDetailedMap.keys.mapNotNull { runCatching { sdf.parse(it) }.getOrNull() }
        val minCal = Calendar.getInstance()
        if (dateKeys.isNotEmpty()) {
            minCal.time = dateKeys.minOrNull()!!
        } else {
            minCal.timeInMillis = goal.createdAt
        }
        val todayCal = Calendar.getInstance()
        val minOffset = (minCal.get(Calendar.YEAR) - todayCal.get(Calendar.YEAR)) * 12 + (minCal.get(Calendar.MONTH) - todayCal.get(Calendar.MONTH))
        val maxOffset = 0

        val selectedDayNoteCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = if (isDark) {
                themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 40), 16f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(0xFFF8FAFC.toInt())
                    setStroke(dp(1), 0xFFE2E8F0.toInt())
                }
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(6), 0, dp(4))
            }
        }
        val noteCardHeader = TextView(host).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(plannerPrimary)
        }
        val noteCardContent = TextView(host).apply {
            textSize = 12f
            setTextColor(themeCoordinator.textColor)
            setPadding(0, dp(3), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.1f)
        }
        selectedDayNoteCard.addView(noteCardHeader)
        selectedDayNoteCard.addView(noteCardContent)

        var lastSelectedDateKey: String? = null
        selectedDayNoteCard.isClickable = true
        selectedDayNoteCard.setOnClickListener {
            val dKey = lastSelectedDateKey ?: return@setOnClickListener
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            showHistoricalGoalDetailDialog(
                goalId = goal.id,
                goalTitle = goal.title,
                fallbackNote = goal.note,
                subjectId = goal.subjectId,
                dateStr = dKey,
                targetMinutes = goal.targetMinutes
            )
        }

        fun updateCalendarView(offset: Int) {
            calendarContainer.removeAllViews()
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, offset)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthLabelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            // Calendar header with navigation
            val calNavRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(6))
            }
            calNavRow.addView(TextView(host).apply {
                text = "‹"
                textSize = 24f
                setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                alpha = if (currentOffset <= minOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset > minOffset) {
                        currentOffset--
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calNavRow.addView(TextView(host).apply {
                text = monthLabelSdf.format(cal.time)
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            calNavRow.addView(TextView(host).apply {
                text = "›"
                textSize = 24f
                setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                alpha = if (currentOffset >= maxOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset < maxOffset) {
                        currentOffset++
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calendarContainer.addView(calNavRow)

            // Day of week labels
            val weekdaysRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(6)) }
            val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
            for (day in daysOfWeek) {
                weekdaysRow.addView(TextView(host).apply {
                    text = day
                    setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            calendarContainer.addView(weekdaysRow)

            // Days Grid
            val gridCal = cal.clone() as Calendar
            gridCal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) - 1
            val maxDaysInMonth = gridCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val todayStr = sdf.format(Date())

            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > maxDaysInMonth) break
                val weekRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(3), 0, dp(3)) }
                for (col in 0..6) {
                    if ((week == 0 && col < firstDayOfWeek) || dayCounter > maxDaysInMonth) {
                        weekRow.addView(View(host).apply { layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f) })
                    } else {
                        val currCal = cal.clone() as Calendar
                        currCal.set(Calendar.DAY_OF_MONTH, dayCounter)
                        val dateKey = sdf.format(currCal.time)
                        val status = historyDetailedMap[dateKey]
                        val isToday = dateKey == todayStr

                        val dayCell = FrameLayout(host).apply {
                            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
                            val circle = View(host).apply {
                                layoutParams = FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER)
                                background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                    when (status) {
                                        GoalHistoryStatus.ACHIEVED -> {
                                            val snap = snapshotsMap[dateKey]
                                            val isGreyAchieved = snap != null && snap.targetMinutes > 0 && snap.actualMinutes < snap.targetMinutes
                                            if (isGreyAchieved) {
                                                setColor(if (isDark) 0xFF475569.toInt() else 0xFF64748B.toInt())
                                            } else {
                                                setColor(if (isDark) 0xFF22C55E.toInt() else 0xFF10B981.toInt())
                                            }
                                        }
                                        GoalHistoryStatus.DEFICIT -> {
                                            setColor(if (isDark) 0xFF475569.toInt() else 0xFF64748B.toInt())
                                        }
                                        GoalHistoryStatus.NOT_COMPLETED -> {
                                            setColor(if (isDark) 0x33EF4444.toInt() else 0x22EF4444.toInt())
                                            setStroke(dp(1), if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt())
                                        }
                                        GoalHistoryStatus.NOT_SCHEDULED -> {
                                            setColor(Color.TRANSPARENT)
                                        }
                                        null -> {
                                            if (isToday) {
                                                setColor(if (isDark) tintedColor(plannerSecondary, 40) else 0x1A0284C7.toInt())
                                                setStroke(dp(2), if (isDark) plannerSecondary else 0xFF0284C7.toInt())
                                            } else {
                                                setColor(Color.TRANSPARENT)
                                            }
                                        }
                                    }
                                }
                            }
                            addView(circle)
                            addView(TextView(host).apply {
                                text = when (status) {
                                    GoalHistoryStatus.ACHIEVED, GoalHistoryStatus.DEFICIT -> "\u2713"
                                    GoalHistoryStatus.NOT_COMPLETED -> "\u2715"
                                    GoalHistoryStatus.NOT_SCHEDULED -> "—"
                                    else -> dayCounter.toString()
                                }
                                textSize = 12f
                                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                                gravity = Gravity.CENTER
                                setTextColor(
                                    when (status) {
                                        GoalHistoryStatus.ACHIEVED, GoalHistoryStatus.DEFICIT -> 0xFFFFFFFF.toInt()
                                        GoalHistoryStatus.NOT_COMPLETED -> if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()
                                        GoalHistoryStatus.NOT_SCHEDULED -> if (isDark) 0x44FFFFFF.toInt() else 0xFF94A3B8.toInt()
                                        null -> if (isToday) (if (isDark) plannerSecondary else 0xFF0284C7.toInt()) else (if (isDark) 0xCCFFFFFF.toInt() else 0xFF334155.toInt())
                                    }
                                )
                                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            })

                            isClickable = true
                            setOnClickListener { view ->
                                lastSelectedDateKey = dateKey
                                val snapshot = snapshotsMap[dateKey]
                                val statusLabel = when (status) {
                                    GoalHistoryStatus.ACHIEVED -> {
                                        val isGreyAchieved = snapshot != null && snapshot.targetMinutes > 0 && snapshot.actualMinutes < snapshot.targetMinutes
                                        if (isGreyAchieved) {
                                            val rem = (snapshot?.targetMinutes ?: goal.targetMinutes) - (snapshot?.actualMinutes ?: 0)
                                            "Marked Complete (${snapshot?.actualMinutes ?: 0}/${snapshot?.targetMinutes ?: goal.targetMinutes}m · ${rem}m left)"
                                        } else {
                                            "Goal Achieved (${snapshot?.targetMinutes ?: goal.targetMinutes}m target)"
                                        }
                                    }
                                    GoalHistoryStatus.DEFICIT -> "Marked Complete (${snapshot?.actualMinutes ?: 0}/${snapshot?.targetMinutes ?: goal.targetMinutes}m)"
                                    GoalHistoryStatus.NOT_COMPLETED -> "Missed Goal (0/${snapshot?.targetMinutes ?: goal.targetMinutes}m completed)"
                                    GoalHistoryStatus.NOT_SCHEDULED -> "Not Scheduled for this day"
                                    null -> if (isToday) "Today (In Progress)" else "Outside Active Tracking"
                                }
                                val displayDateSdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                                val dateFormatted = runCatching { displayDateSdf.format(sdf.parse(dateKey)!!) }.getOrDefault(dateKey)
                                noteCardHeader.text = "$dateFormatted • $statusLabel"
                                val dayNote = snapshot?.note?.takeIf { it.isNotBlank() } ?: goal.note.takeIf { it.isNotBlank() } ?: "No specific notes recorded for this date."
                                noteCardContent.text = dayNote
                                selectedDayNoteCard.visibility = View.VISIBLE
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)

                                showHistoricalGoalDetailDialog(
                                    goalId = goal.id,
                                    goalTitle = goal.title,
                                    fallbackNote = goal.note,
                                    subjectId = goal.subjectId,
                                    dateStr = dateKey,
                                    targetMinutes = goal.targetMinutes
                                )
                            }
                        }
                        weekRow.addView(dayCell)
                        dayCounter++
                    }
                }
                calendarContainer.addView(weekRow)
            }
        }

        updateCalendarView(currentOffset)
        content.addView(calendarContainer)
        content.addView(selectedDayNoteCard)

        // 4. FIXED Legend Row
        val legendRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(6))
        }
        legendRow.addView(TextView(host).apply {
            text = "✓ Achieved   ✕ Incomplete / Missed   — Not Scheduled\n(Tap marked date to view session notes)"
            gravity = Gravity.CENTER
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            textSize = 11f
        })
        content.addView(legendRow)

        // 5. FIXED Action Buttons Row at Bottom (Edit Goal + Close)
        val buttonRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }

        val editBtn = TextView(host).apply {
            text = "Edit Goal"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(plannerPrimary)
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener {
                dialog.dismiss()
                showEditSessionGoalDialog(goal)
            }
        }
        buttonRow.addView(editBtn)

        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonRow.addView(closeBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showEditYesterdayGoalsDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val yesterdayDisplayStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(cal.time)

        val (plannerPrimary, _) = resolvePlannerColors()
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(host, yesterdayStr)
        val dayFocusSecs = prefs.getLong("${yesterdayStr}_focus_total", 0L)
        val yesterdayFocusMins = (dayFocusSecs / 60).toInt()

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        // Header Title
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        headerRow.addView(TextView(host).apply {
            text = "Yesterday's Goals"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val closeBtn = TextView(host).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        root.addView(TextView(host).apply {
            text = "$yesterdayDisplayStr • ${yesterdayFocusMins / 60}h ${yesterdayFocusMins % 60}m studied"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setPadding(0, 0, 0, dp(12))
        })

        data class YesterdayGoalItem(
            var id: String,
            var title: String,
            var targetMinutes: Int,
            var completed: Boolean,
            var isAchieved: Boolean,
            var subjectId: String?
        )

        val itemsList = mutableListOf<YesterdayGoalItem>()
        val existingSnapshots = PlannerHistoryManager.loadDaySnapshot(host, yesterdayStr)
        if (existingSnapshots.isNotEmpty()) {
            for (s in existingSnapshots) {
                itemsList.add(YesterdayGoalItem(s.goalId, s.title, s.targetMinutes, s.completed, s.isAchieved, s.subjectId))
            }
            val currentGoals = loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
            val yesterdayDailyGoalDurations = PlannerHistoryManager.getDailyGoalDurations(host, yesterdayStr)
            val yesterdaySubTopicDurations = mutableMapOf<String, Map<String, Long>>()
            for (g in currentGoals) {
                val subId = g.subjectId
                if (!subId.isNullOrBlank() && subId != "all" && !yesterdaySubTopicDurations.containsKey(subId)) {
                    yesterdaySubTopicDurations[subId] = SubjectTagManager.getDailySubTopicDurations(host, subId, yesterdayStr)
                }
            }
            val progressMap = PlannerHistoryManager.calculateGoalProgress(
                currentGoals, yesterdayFocusMins * 60L, dailySubjectDurations, yesterdayDailyGoalDurations, yesterdaySubTopicDurations
            )
            for (g in currentGoals) {
                val prog = progressMap[g.id]
                val autoDone = if (g.targetMinutes > 0) (prog?.isAchieved ?: false) else g.completed
                itemsList.add(YesterdayGoalItem(g.id, g.title, g.targetMinutes, autoDone, autoDone, g.subjectId))
            }
        }

        val scroll = ScrollView(host).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (host.resources.displayMetrics.heightPixels * 0.40f).toInt()
            )
        }

        val goalsContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(goalsContainer)

        fun renderGoals() {
            goalsContainer.removeAllViews()
            if (itemsList.isEmpty()) {
                goalsContainer.addView(TextView(host).apply {
                    text = "No goals recorded for yesterday. Tap below to add any goals you completed."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(24), dp(16), dp(24))
                })
            }
            for ((idx, item) in itemsList.withIndex()) {
                val card = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                }

                val toggleBadge = TextView(host).apply {
                    text = if (item.completed) "Done" else "Not Done"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (item.completed) Color.parseColor("#10B981") else Color.parseColor("#475569"))
                    }
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener {
                        item.completed = !item.completed
                        item.isAchieved = item.completed
                        renderGoals()
                    }
                }
                card.addView(toggleBadge)

                val textCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(10), 0, dp(8), 0)
                    }
                }
                textCol.addView(TextView(host).apply {
                    text = item.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })

                val target = item.targetMinutes
                val actual = if (!item.subjectId.isNullOrBlank() && item.subjectId != "all") {
                    ((dailySubjectDurations[item.subjectId] ?: 0L) / 60).toInt()
                } else {
                    yesterdayFocusMins
                }
                val subText = if (target > 0) "Target: ${target}m • Studied: ${actual}m" else "Daily Goal"
                textCol.addView(TextView(host).apply {
                    text = subText
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })
                card.addView(textCol)

                val removeBtn = TextView(host).apply {
                    text = "✕"
                    textSize = 14f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        itemsList.removeAt(idx)
                        renderGoals()
                    }
                }
                card.addView(removeBtn)
                goalsContainer.addView(card)
            }
        }
        renderGoals()

        root.addView(scroll)

        val addRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(12))
        }

        val addHabitBtn = TextView(host).apply {
            text = "+ Add Yesterday's Goal"
            setTextColor(plannerPrimary)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 35), 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val inputDialog = Dialog(host)
                inputDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val inputRoot = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(24f)
                    setPadding(dp(20), dp(18), dp(20), dp(18))
                }
                val titleView = TextView(host).apply {
                    text = "Add Yesterday's Goal"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, 0, 0, dp(12))
                }
                val inputEdit = EditText(host).apply {
                    hint = "Goal name (e.g. Math homework)"
                    setTextColor(themeCoordinator.textColor)
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                }
                val btnRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }
                val cancelBtn = Button(host).apply {
                    text = "Cancel"
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    textSize = 12f
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                    setOnClickListener { inputDialog.dismiss() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                val addBtn = Button(host).apply {
                    text = "Add Goal"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(themeCoordinator.primaryColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38))
                    setOnClickListener {
                        val t = inputEdit.text.toString().trim()
                        if (t.isNotEmpty()) {
                            itemsList.add(YesterdayGoalItem("custom_${System.currentTimeMillis()}", t, 30, true, true, null))
                            renderGoals()
                        }
                        inputDialog.dismiss()
                    }
                }
                btnRow.addView(cancelBtn)
                btnRow.addView(addBtn)
                inputRoot.addView(titleView)
                inputRoot.addView(inputEdit)
                inputRoot.addView(btnRow)
                inputDialog.setContentView(inputRoot)
                inputDialog.window?.apply {
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                inputDialog.show()
            }
        }
        addRow.addView(addHabitBtn)
        root.addView(addRow)

        val saveBtn = Button(host).apply {
            text = "SAVE YESTERDAY'S GOALS"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(plannerPrimary)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val array = org.json.JSONArray()
                for (item in itemsList) {
                    val target = item.targetMinutes
                    val actual = if (!item.subjectId.isNullOrBlank() && item.subjectId != "all") {
                        ((dailySubjectDurations[item.subjectId] ?: 0L) / 60).toInt()
                    } else {
                        yesterdayFocusMins
                    }
                    val isAchieved = if (item.completed) (target == 0 || actual >= target) else false
                    array.put(org.json.JSONObject().apply {
                        put("goalId", item.id)
                        put("title", item.title)
                        put("targetMinutes", item.targetMinutes)
                        put("completed", item.completed)
                        put("checkedAt", if (item.completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L)
                        put("isAchieved", isAchieved)
                        if (item.subjectId != null) put("subjectId", item.subjectId)
                    })
                }

                prefs.edit().putString("${yesterdayStr}_planner_snapshot", array.toString()).apply()
                statsDirty = true
                tabPageCache.clear()
                recalculateStreak()
                refreshStatsPanel()

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    CloudSyncManager.syncDataToCloud(host)
                }

                AppToast.show(host, "Yesterday's goals updated!")
                dialog.dismiss()
            }
        }
        root.addView(saveBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun migrateHistoricalDailyGoals(context: Context) {
        GoalHistoryManager.reconcileAllHistoricalGoals(context)
    }

    internal fun showHistoricalGoalDetailDialog(
        goalId: String,
        goalTitle: String,
        fallbackNote: String,
        subjectId: String?,
        dateStr: String,
        targetMinutes: Int = 0
    ) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, _) = resolvePlannerColors()
        val daySnapshots = PlannerHistoryManager.loadDaySnapshot(host, dateStr)
        val snapshot = daySnapshots.find { it.goalId == goalId }
            ?: daySnapshots.find { it.title.equals(goalTitle, ignoreCase = true) }
        val goalDurations = PlannerHistoryManager.getDailyGoalDurations(host, dateStr)
        val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(host, dateStr)
        val dayFocusSecs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getLong("${dateStr}_focus_total", 0L)

        val goalSecs = goalDurations[goalId] ?: (if (snapshot != null) goalDurations[snapshot.goalId] ?: 0L else 0L)
        val subId = snapshot?.subjectId ?: subjectId
        val targetMins = snapshot?.targetMinutes?.takeIf { it > 0 } ?: targetMinutes

        val actualMins = if (goalSecs > 0L) {
            (goalSecs / 60).toInt()
        } else if (!subId.isNullOrBlank() && subId != "all") {
            ((dailySubjectDurations[subId] ?: 0L) / 60).toInt()
        } else {
            (dayFocusSecs / 60).toInt()
        }

        val isCompleted = snapshot?.completed ?: (targetMins > 0 && actualMins >= targetMins)
        val isAchieved = snapshot?.isAchieved ?: (targetMins > 0 && actualMins >= targetMins)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val prettyDateSdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val dateFormatted = runCatching { prettyDateSdf.format(sdf.parse(dateStr)!!) }.getOrDefault(dateStr)

        val resolvedSub = if (!subId.isNullOrBlank() && subId != "all") {
            SubjectTagManager.resolveSubject(host, subId)
        } else null

        val noteText = snapshot?.note?.takeIf { it.isNotBlank() }
            ?: fallbackNote.takeIf { it.isNotBlank() }
            ?: "No note was recorded for this day."

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // Header Row
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }

        val subBadge = TextView(host).apply {
            if (resolvedSub != null) {
                text = resolvedSub.name
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(runCatching { Color.parseColor(resolvedSub.colorHex) }.getOrDefault(plannerPrimary))
                }
            } else {
                text = "📚 All Subjects"
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(tintedColor(plannerPrimary, 70))
                }
            }
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(3), dp(10), dp(3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }
        headerRow.addView(subBadge)

        headerRow.addView(TextView(host).apply {
            text = dateFormatted
            textSize = 12.5f
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        content.addView(headerRow)

        // Goal Title
        content.addView(TextView(host).apply {
            text = goalTitle
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(12))
        })

        // Status & Metrics Row
        val statsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        fun makeDetailChip(label: String, value: String, valueColor: Int? = null) {
            val chip = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = if (isDark) {
                    themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 70), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            }
            chip.addView(TextView(host).apply {
                text = label.uppercase(Locale.getDefault())
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF94A3B8.toInt())
            })
            chip.addView(TextView(host).apply {
                text = value
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(valueColor ?: if (isDark) Color.WHITE else 0xFF0F172A.toInt())
                setPadding(0, dp(2), 0, 0)
            })
            statsRow.addView(chip)
        }

        val liveGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]")
        val scheduledDays = snapshot?.scheduledDays?.takeIf { it.isNotEmpty() }
            ?: liveGoals.find { it.id == goalId }?.scheduledDays
            ?: emptyList()
        val cal = Calendar.getInstance().apply {
            runCatching { time = sdf.parse(dateStr)!! }
        }
        val isScheduled = PlannerHistoryManager.isGoalScheduledOnDay(scheduledDays, cal)

        val isTargetMet = (targetMins > 0 && actualMins >= targetMins) || (targetMins == 0 && isCompleted)
        val isEarlyStopped = isCompleted && targetMins > 0 && actualMins < targetMins
        val statusText = when {
            !isScheduled && !isAchieved && actualMins == 0 -> "— Not Scheduled"
            isAchieved || isTargetMet -> "✓ Completed"
            isEarlyStopped -> "✕ Stopped Early"
            actualMins > 0 -> "○ In Progress"
            else -> "○ Incomplete"
        }
        val statusColor = when {
            !isScheduled && !isAchieved && actualMins == 0 -> if (isDark) 0x88FFFFFF.toInt() else 0xFF94A3B8.toInt()
            isAchieved || isTargetMet -> if (isDark) 0xFF22C55E.toInt() else 0xFF10B981.toInt()
            isEarlyStopped || (!isCompleted && actualMins == 0) -> if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()
            else -> plannerPrimary
        }

        makeDetailChip("Focus Time", if (goalSecs > 0) "${goalSecs / 60}m ${goalSecs % 60}s" else "${actualMins}m")
        makeDetailChip("Target", if (targetMins > 0) "${targetMins}m" else "Flexible")
        makeDetailChip("Status", statusText, statusColor)
        content.addView(statsRow)

        // Session Note Box
        val noteBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = if (isDark) {
                themeCoordinator.createGlassChip(0x33000000.toInt(), 16f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(0xFFF8FAFC.toInt())
                    setStroke(dp(1), 0xFFE2E8F0.toInt())
                }
            }
            setPadding(dp(16), dp(12), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
        noteBox.addView(TextView(host).apply {
            text = "📝 SESSION NOTE"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(plannerPrimary)
            setPadding(0, 0, 0, dp(4))
        })
        noteBox.addView(TextView(host).apply {
            text = noteText
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            setLineSpacing(dp(2).toFloat(), 1.15f)
        })
        content.addView(noteBox)

        // Close Button
        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(plannerPrimary)
            }
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.let { win ->
            win.setLayout(
                (host.resources.displayMetrics.widthPixels * 0.90f).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    internal data class MatrixGoalItem(
        val id: String,
        val title: String,
        val note: String = "",
        val targetMinutes: Int = 0,
        val isDeleted: Boolean = false,
        val subjectId: String? = null,
        val subTopic: String? = null,
        val scheduledDays: List<Int> = emptyList(),
        val createdAt: Long = 0L
    )

    internal fun showPlannerMatrixDialog(startFullscreen: Boolean = false) {
        val isFullscreen = startFullscreen
        val dialog = Dialog(host,
            if (isFullscreen) android.R.style.Theme_Black_NoTitleBar_Fullscreen else android.R.style.Theme_Dialog
        )
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val goalsJson = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        var selectedRangeDays = 14

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = if (isFullscreen) {
                null
            } else {
                themeCoordinator.createDialogBackground(28f)
            }
            setPadding(
                dp(16),
                if (isFullscreen) getStatusBarHeight() + dp(12) else dp(18),
                dp(16),
                if (isFullscreen) dp(16) else dp(18)
            )
            if (isFullscreen) {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        val topHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topHeaderRow.addView(TextView(host).apply {
            text = "WEEKLY GOAL TRACKER"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val fsBtn = TextView(host).apply {
            text = if (isFullscreen) "Exit Fullscreen" else "Fullscreen"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(plannerPrimary, 12f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener {
                dialog.dismiss()
                showPlannerMatrixDialog(startFullscreen = !isFullscreen)
            }
        }
        topHeaderRow.addView(fsBtn)
        content.addView(topHeaderRow)

        content.addView(TextView(host).apply {
            text = "See which goals you completed each day this week:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(10))
        })

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateHeaderFmt = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Collect all goals (both active and historically deleted)
        val allGoalItemsMap = mutableMapOf<String, MatrixGoalItem>()

        // Add current active goals first
        for (g in goalsList) {
            allGoalItemsMap[g.id] = MatrixGoalItem(
                id = g.id,
                title = g.title,
                note = g.note,
                targetMinutes = g.targetMinutes,
                isDeleted = false,
                subjectId = g.subjectId,
                subTopic = g.subTopic,
                scheduledDays = g.scheduledDays,
                createdAt = g.createdAt
            )
        }

        // Add past deleted goals from daily snapshots
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        for (key in prefs.all.keys) {
            if (key.endsWith("_planner_snapshot")) {
                if (!prefs.contains(key)) continue
                val snapshots = PlannerHistoryManager.loadDaySnapshot(host, key.removeSuffix("_planner_snapshot"))
                for (s in snapshots) {
                    if (s.goalId.isNotBlank() && !allGoalItemsMap.containsKey(s.goalId)) {
                        allGoalItemsMap[s.goalId] = MatrixGoalItem(
                            id = s.goalId,
                            title = if (s.title.isNotBlank()) s.title else "Deleted Goal",
                            note = s.note,
                            targetMinutes = s.targetMinutes,
                            isDeleted = true,
                            subjectId = s.subjectId,
                            subTopic = s.subTopic,
                            scheduledDays = s.scheduledDays,
                            createdAt = 0L
                        )
                    }
                }
            }
        }

        val allGoalItems = allGoalItemsMap.values.toList()
        val allDates = mutableSetOf<String>()

        val goalHistories = allGoalItems.associate { item ->
            val h = PlannerHistoryManager.loadGoalHistoryDetailed(
                context = host,
                goalId = item.id,
                scheduledDays = item.scheduledDays,
                createdAt = item.createdAt
            )
            allDates.addAll(h.keys)
            item.id to h
        }

        val allCandidateDates = mutableListOf<Date>()
        for (dStr in allDates) {
            runCatching { sdf.parse(dStr) }.getOrNull()?.let { allCandidateDates.add(it) }
        }
        for (key in prefs.all.keys) {
            if (key.endsWith("_planner_snapshot")) {
                runCatching { sdf.parse(key.removeSuffix("_planner_snapshot")) }.getOrNull()?.let { allCandidateDates.add(it) }
            }
            if (key.endsWith("_focus_total")) {
                val total = (prefs.all[key] as? Number)?.toLong() ?: 0L
                if (total > 0) {
                    runCatching { sdf.parse(key.removeSuffix("_focus_total")) }.getOrNull()?.let { allCandidateDates.add(it) }
                }
            }
        }
        for (g in goalsList) {
            if (g.createdAt > 0) {
                allCandidateDates.add(Date(g.createdAt))
            }
        }
        runCatching {
            TimelineLogger.load(host).forEach { entry ->
                if (entry.timestamp > 0) allCandidateDates.add(Date(entry.timestamp))
            }
        }

        val earliestDate = allCandidateDates.minOrNull() ?: Date()
        val earliestCal = Calendar.getInstance().apply {
            time = earliestDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var currentAllTimeDays = 30

        val matrixContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun renderMatrix(rangeDays: Int) {
            matrixContainer.removeAllViews()
            val calendarList = mutableListOf<Date>()
            val futureOffset = if (rangeDays == 0) 0 else 3
            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, futureOffset) }
            val count = if (rangeDays > 0) rangeDays else currentAllTimeDays
            var reachedEarliest = false

            for (i in 0 until count) {
                val c = startCal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, -i)
                val dayCal = Calendar.getInstance().apply {
                    time = c.time
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (rangeDays == 0 && dayCal.before(earliestCal)) {
                    reachedEarliest = true
                    break
                }
                calendarList.add(c.time)
            }
            val filteredDates = calendarList

            if (filteredDates.isEmpty()) {
                matrixContainer.addView(TextView(host).apply {
                    text = "No goal history yet. Completed goals will appear here."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(20), 0, dp(20))
                })
                return
            }

            val splitLayout = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val leftColumnWidth = dp(136)
            val rowHeight = dp(42)

            // Left Fixed Column for Goal Titles
            val leftColumn = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(leftColumnWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            leftColumn.addView(TextView(host).apply {
                text = "Goal"
                setTextColor(plannerPrimary)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(leftColumnWidth, rowHeight)
            })
            leftColumn.addView(createDivider())

            for (item in allGoalItems) {
                val sub = if (!item.subjectId.isNullOrBlank() && item.subjectId != "all") {
                    SubjectTagManager.resolveSubject(host, item.subjectId)
                } else null
                val prefix = if (sub != null && sub.name.isNotBlank()) "${sub.name} · " else ""
                val itemRow = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(leftColumnWidth, rowHeight)
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    isClickable = true
                    isFocusable = true
                    val typedValue = android.util.TypedValue()
                    if (host.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                        foreground = androidx.core.content.ContextCompat.getDrawable(host, typedValue.resourceId)
                    }
                    setOnClickListener {
                        showGoalDetailFloatingCard(item) {
                            dialog.dismiss()
                            showPlannerMatrixDialog(startFullscreen = isFullscreen)
                        }
                    }
                    setOnLongClickListener {
                        if (item.isDeleted) {
                            showArchivedGoalActionMenu(item) {
                                dialog.dismiss()
                                showPlannerMatrixDialog(startFullscreen = isFullscreen)
                            }
                        } else {
                            showDeletePlannerGoalMatrixDialog(item.id, item.title) {
                                dialog.dismiss()
                                showPlannerMatrixDialog(startFullscreen = isFullscreen)
                            }
                        }
                        true
                    }
                }

                val titleTv = TextView(host).apply {
                    text = if (prefix.isNotBlank()) "$prefix${item.title}" else item.title
                    setTextColor(
                        if (item.isDeleted) {
                            if (isDark) tintedColor(themeCoordinator.textColor, 140) else 0xFF64748B.toInt()
                        } else {
                            if (isDark) themeCoordinator.textColor else 0xFF0F172A.toInt()
                        }
                    )
                    textSize = 12.5f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                itemRow.addView(titleTv)

                if (item.isDeleted) {
                    val archBadge = TextView(host).apply {
                        text = "Archived • Tap to merge"
                        setTextColor(if (isDark) 0xFFF59E0B.toInt() else 0xFFD97706.toInt())
                        textSize = 9f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    itemRow.addView(archBadge)
                } else if (item.targetMinutes > 0) {
                    val subTv = TextView(host).apply {
                        text = "${item.targetMinutes}m target"
                        setTextColor(tintedColor(themeCoordinator.textColor, if (isDark) 120 else 140))
                        textSize = 9.5f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        maxLines = 1
                    }
                    itemRow.addView(subTv)
                }

                leftColumn.addView(itemRow)
                leftColumn.addView(createDivider())
            }
            splitLayout.addView(leftColumn)

            // Right Scrollable Dates Container
            val rightScroll = HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val rightTable = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
            }

            val todayStr = sdf.format(Date())

            val allGoalsTodayCompleted = allGoalItems.isNotEmpty() && allGoalItems.all { item ->
                val history = goalHistories[item.id] ?: emptyMap()
                history[todayStr] == GoalHistoryStatus.ACHIEVED
            }

            val todayAccent = if (allGoalsTodayCompleted) (if (isDark) 0xFF22C55E.toInt() else 0xFF10B981.toInt()) else (if (isDark) plannerSecondary else 0xFF0284C7.toInt())
            val todayBgColor = if (isDark) {
                if (allGoalsTodayCompleted) Color.argb(35, 34, 197, 94) else Color.argb(20, Color.red(plannerPrimary), Color.green(plannerPrimary), Color.blue(plannerPrimary))
            } else {
                if (allGoalsTodayCompleted) 0x2210B981.toInt() else 0x140284C7.toInt()
            }

            val dayOfWeekFmt = SimpleDateFormat("EEE", Locale.getDefault())

            val headerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, rowHeight)
            }
            for (d in filteredDates) {
                val dateKey = sdf.format(d)
                val isToday = (dateKey == todayStr)
                val cal = Calendar.getInstance().apply { time = d }
                val isWeekend = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                val weekdayStr = if (isToday) "TODAY" else dayOfWeekFmt.format(d).uppercase()
                val dateShortStr = dateHeaderFmt.format(d)

                headerRow.addView(TextView(host).apply {
                    text = "$weekdayStr\n$dateShortStr"
                    setTextColor(
                        if (isToday) {
                            if (isDark) Color.WHITE else 0xFF0369A1.toInt()
                        } else if (isWeekend) {
                            if (isDark) 0xFFF59E0B.toInt() else 0xFFD97706.toInt()
                        } else {
                            if (isDark) plannerPrimary else 0xFF475569.toInt()
                        }
                    )
                    textSize = if (isToday) 9.5f else 10f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    if (isToday) {
                        background = if (isDark) {
                            themeCoordinator.createGlassChip(plannerPrimary, 10f)
                        } else {
                            GradientDrawable().apply {
                                cornerRadius = dp(10).toFloat()
                                setColor(0xFFE0F2FE.toInt())
                                setStroke(dp(1), 0xFF0284C7.toInt())
                            }
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
            rightTable.addView(headerRow)
            rightTable.addView(createDivider())

            val greenColor = if (isDark) 0xFF22C55E.toInt() else 0xFF047857.toInt()
            val redColor = if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()

            for (item in allGoalItems) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, rowHeight)
                }
                val history = goalHistories[item.id] ?: emptyMap()
                for (d in filteredDates) {
                    val dateKey = sdf.format(d)
                    val status = history[dateKey]
                    val isToday = (dateKey == todayStr)
                    row.addView(TextView(host).apply {
                        val cal = Calendar.getInstance().apply { time = d }
                        val isScheduled = PlannerHistoryManager.isGoalScheduledOnDay(item.scheduledDays, cal)
                        val isMissed = (status == GoalHistoryStatus.DEFICIT || status == GoalHistoryStatus.NOT_COMPLETED || (!isToday && status == null && !d.after(Date()) && isScheduled))
                        val isAchieved = (status == GoalHistoryStatus.ACHIEVED)
                        val isUnscheduled = (status == GoalHistoryStatus.NOT_SCHEDULED || (!isScheduled && !isAchieved))

                        text = when {
                            isUnscheduled -> "—"
                            isAchieved -> "\u2713"
                            isMissed -> "\u2715"
                            isToday -> "\u25CB"
                            d.after(Date()) -> ""
                            else -> "—"
                        }
                        setTextColor(when {
                            isUnscheduled -> if (isDark) tintedColor(themeCoordinator.textColor, 70) else 0xFF94A3B8.toInt()
                            isAchieved -> greenColor
                            isToday -> plannerPrimary
                            isMissed -> redColor
                            else -> if (isDark) tintedColor(themeCoordinator.textColor, 80) else 0xFF94A3B8.toInt()
                        })
                        textSize = if (isToday && !isUnscheduled) 16f else 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        if (isToday && !isUnscheduled) {
                            background = GradientDrawable().apply {
                                setColor(todayBgColor)
                                cornerRadius = dp(6).toFloat()
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                        isClickable = true
                        isFocusable = true
                        val typedValue = android.util.TypedValue()
                        if (host.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                            foreground = androidx.core.content.ContextCompat.getDrawable(host, typedValue.resourceId)
                        }
                        setOnClickListener {
                            it.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            showHistoricalGoalDetailDialog(
                                goalId = item.id,
                                goalTitle = item.title,
                                fallbackNote = item.note,
                                subjectId = item.subjectId,
                                dateStr = dateKey,
                                targetMinutes = item.targetMinutes
                            )
                        }
                    })
                }
                rightTable.addView(row)
                rightTable.addView(createDivider())
            }
            rightScroll.addView(rightTable)

            val todayIndex = filteredDates.indexOfFirst { sdf.format(it) == todayStr }
            if (todayIndex >= 0) {
                val targetScrollX = todayIndex * dp(54)
                rightScroll.visibility = View.INVISIBLE
                rightScroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        rightScroll.viewTreeObserver.removeOnPreDrawListener(this)
                        rightScroll.scrollTo(targetScrollX, 0)
                        rightScroll.visibility = View.VISIBLE
                        return true
                    }
                })
            }

            splitLayout.addView(rightScroll)

            matrixContainer.addView(splitLayout)

            if (rangeDays == 0 && !reachedEarliest) {
                val loadMoreBtn = TextView(host).apply {
                    text = "Load More (+30 Days)"
                    setTextColor(if (isDark) plannerPrimary else 0xFF0F172A.toInt())
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = if (isDark) {
                        themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 70), 12f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(10), 0, 0)
                    }
                    setOnClickListener {
                        currentAllTimeDays += 30
                        renderMatrix(0)
                    }
                }
                matrixContainer.addView(loadMoreBtn)
            }
        }

        // Filter Bar (14 Days | 30 Days | All Time)
        val filterRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }

        fun makeFilterBtn(label: String, daysVal: Int): TextView {
            val isSel = selectedRangeDays == daysVal
            return TextView(host).apply {
                text = label
                textSize = 12f
                typeface = host.themeCoordinator.getAppTypeface(isBold = isSel)
                setTextColor(
                    if (isSel) {
                        Color.WHITE
                    } else {
                        if (isDark) 0xAAFFFFFF.toInt() else 0xFF475569.toInt()
                    }
                )
                background = if (isSel) {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(plannerPrimary)
                    }
                } else {
                    if (isDark) {
                        themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(20).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                }
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(6), 0) }
                setOnClickListener {
                    selectedRangeDays = daysVal
                    for (i in 0 until filterRow.childCount) {
                        val child = filterRow.getChildAt(i) as? TextView ?: continue
                        val sel = (child.tag as? Int) == selectedRangeDays
                        child.typeface = host.themeCoordinator.getAppTypeface(isBold = sel)
                        child.setTextColor(
                            if (sel) {
                                Color.WHITE
                            } else {
                                if (isDark) 0xAAFFFFFF.toInt() else 0xFF475569.toInt()
                            }
                        )
                        child.background = if (sel) {
                            GradientDrawable().apply {
                                cornerRadius = dp(20).toFloat()
                                setColor(plannerPrimary)
                            }
                        } else {
                            if (isDark) {
                                themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                            } else {
                                GradientDrawable().apply {
                                    cornerRadius = dp(20).toFloat()
                                    setColor(0xFFF1F5F9.toInt())
                                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                                }
                            }
                        }
                    }
                    renderMatrix(selectedRangeDays)
                }
                tag = daysVal
            }
        }
        filterRow.addView(makeFilterBtn("14 Days", 14))
        filterRow.addView(makeFilterBtn("30 Days", 30))
        filterRow.addView(makeFilterBtn("All Time", 0))

        content.addView(filterRow)

        val legendScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val legendRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun addLegendChip(symbol: String, label: String, symColor: Int) {
            val chip = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isDark) {
                    themeCoordinator.createGlassChip(tintedColor(symColor, 35), 8f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(tintedColor(symColor, 20))
                        setStroke(dp(1), tintedColor(symColor, 60))
                    }
                }
                setPadding(dp(8), dp(4), dp(8), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
            chip.addView(TextView(host).apply {
                text = symbol
                setTextColor(symColor)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, dp(4), 0)
            })
            chip.addView(TextView(host).apply {
                text = label
                setTextColor(if (isDark) tintedColor(themeCoordinator.textColor, 200) else 0xFF334155.toInt())
                textSize = 10.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            })
            legendRow.addView(chip)
        }

        val legendGreen = if (isDark) 0xFF22C55E.toInt() else 0xFF047857.toInt()
        val legendRed = if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()
        addLegendChip("\u2713", "Completed", legendGreen)
        addLegendChip("\u2715", "Missed", legendRed)
        addLegendChip("\u25CB", "In Progress", plannerPrimary)
        addLegendChip("—", "Not Scheduled", if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
        legendScroll.addView(legendRow)
        content.addView(legendScroll)

        renderMatrix(selectedRangeDays)
        content.addView(matrixContainer)

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = if (isDark) {
                themeCoordinator.createGlassChip(plannerPrimary, 24f)
            } else {
                GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(0xFFF1F5F9.toInt())
                    setStroke(dp(1), 0xFFCBD5E1.toInt())
                }
            }
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { setMargins(0, dp(14), 0, 0) }
        }
        content.addView(closeBtn)

        if (isFullscreen) {
            val rootContainer = WallpaperManager.buildWallpaperContainer(host, themeCoordinator)
            val scrollWrapper = ScrollView(host).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                addView(content)
            }
            rootContainer.addView(scrollWrapper)
            dialog.setContentView(rootContainer)

            dialog.window?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setDecorFitsSystemWindows(false)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                statusBarColor = Color.TRANSPARENT
                navigationBarColor = Color.TRANSPARENT
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                decorView.setPadding(0, 0, 0, 0)
                try {
                    val isLightMode = themeCoordinator.activeBgMode == "LIGHT"
                    val decor = peekDecorView() ?: decorView
                    androidx.core.view.WindowCompat.getInsetsController(this, decor).let { controller ->
                        controller.isAppearanceLightStatusBars = isLightMode
                        controller.isAppearanceLightNavigationBars = isLightMode
                    }
                } catch (_: Exception) {}
            }
            dialog.setOnShowListener {
                dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                dialog.window?.decorView?.setPadding(0, 0, 0, 0)
            }
            dialog.setOnDismissListener {
                for (i in 0 until rootContainer.childCount) {
                    (rootContainer.getChildAt(i) as? LiveWallpaperView)?.release()
                }
            }
        } else {
            dialog.setContentView(content)
            dialog.window?.apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setLayout((host.resources.displayMetrics.widthPixels * 0.95f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        dialog.show()
    }

    internal fun showFeedbackReportDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // Header Title
        val titleText = TextView(host).apply {
            text = "Report a Problem & Feedback"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        content.addView(titleText)

        val subtitleText = TextView(host).apply {
            text = "Help us improve StudyTimer! Report bugs, suggest features, or share thoughts."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(subtitleText)

        // Category Selector Chips
        val categories = listOf("Bug Report", "Feature Request", "General Feedback")
        var selectedCategory = "Bug Report"

        val chipContainer = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }

        val chipViews = ArrayList<TextView>()
        fun updateChipStyles() {
            chipViews.forEachIndexed { index, chip ->
                val isSelected = categories[index] == selectedCategory
                chip.setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                chip.background = if (isSelected) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 18f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 18f)
                }
            }
        }

        categories.forEach { cat ->
            val chip = TextView(host).apply {
                text = when (cat) {
                    "Bug Report" -> "Bug"
                    "Feature Request" -> "Feature"
                    else -> "Feedback"
                }
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener {
                    selectedCategory = cat
                    updateChipStyles()
                }
            }
            chipViews.add(chip)
            chipContainer.addView(chip)
        }
        updateChipStyles()
        content.addView(chipContainer)

        // Description Text Field (Max 2,000 characters)
        val inputField = android.widget.EditText(host).apply {
            hint = "Describe what happened or what you'd like to see (max 2,000 chars)..."
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            filters = arrayOf(android.text.InputFilter.LengthFilter(2000))
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 7
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        content.addView(inputField)

        // Contact Email Input Field (Optional, Max 100 characters)
        val contactField = android.widget.EditText(host).apply {
            hint = "Your email for reply (optional)..."
            val userEmail = AuthManager.getUserEmail(host)
            if (!userEmail.isNullOrBlank()) setText(userEmail)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        content.addView(contactField)

        // Anonymous Diagnostic Info Toggle
        var includeDiagnostics = true
        val toggleRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(12))
            isClickable = true
            isFocusable = true
        }

        val checkIcon = TextView(host).apply {
            text = "✓"
            textSize = 15f
            setTextColor(themeCoordinator.primaryColor)
            setPadding(0, 0, dp(8), 0)
        }
        toggleRow.addView(checkIcon)

        val toggleLabel = TextView(host).apply {
            text = "Include anonymous diagnostic info\n(App version, OS, Device model, Sync state)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 11.5f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleRow.addView(toggleLabel)

        toggleRow.setOnClickListener {
            includeDiagnostics = !includeDiagnostics
            checkIcon.text = if (includeDiagnostics) "✓" else "○"
        }
        content.addView(toggleRow)

        val feedbackPrefs = host.getSharedPreferences("studytimer_feedback_prefs", Context.MODE_PRIVATE)
        val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes cooldown
        val lastSubmissionEpoch = feedbackPrefs.getLong("last_feedback_submission_epoch", 0L)
        val isDevBypass = BuildConfig.DEBUG || isDevModeUnlocked

        // Cooldown Warning & Live Countdown View
        val cooldownWarningText = TextView(host).apply {
            setTextColor(0xFFFF7043.toInt()) // Soft coral warning
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(6))
            visibility = View.GONE
        }

        // Urgent Help Fallback Action
        val urgentEmailBtn = TextView(host).apply {
            text = "Need urgent help? [Email studytimer737@gmail.com]"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:studytimer737@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "[StudyTimer Urgent Help]")
                }
                try {
                    host.startActivity(Intent.createChooser(emailIntent, "Email Support..."))
                } catch (_: Exception) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Support Email", "studytimer737@gmail.com"))
                    AppToast.show(host, "Email copied to clipboard: studytimer737@gmail.com", Toast.LENGTH_LONG)
                }
            }
        }

        // Submit Feedback Action Button
        val sendBtn = Button(host).apply {
            text = "Submit Report"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, dp(4), 0, dp(6))
            }
        }

        // Countdown Timer Handler
        val countdownHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSubmissionEpoch
                val remaining = COOLDOWN_MS - elapsed

                if (!isDevBypass && remaining > 0) {
                    val mins = (remaining / 1000) / 60
                    val secs = (remaining / 1000) % 60
                    sendBtn.isEnabled = false
                    sendBtn.alpha = 0.5f
                    cooldownWarningText.visibility = View.VISIBLE
                    cooldownWarningText.text = "⏳ You recently sent a report. Please wait ${mins}m ${secs}s before submitting another."
                    urgentEmailBtn.visibility = View.VISIBLE
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    sendBtn.isEnabled = true
                    sendBtn.alpha = 1.0f
                    cooldownWarningText.visibility = View.GONE
                    urgentEmailBtn.visibility = View.GONE
                }
            }
        }

        countdownHandler.post(countdownRunnable)
        dialog.setOnDismissListener {
            countdownHandler.removeCallbacks(countdownRunnable)
        }

        sendBtn.setOnClickListener {
            val now = System.currentTimeMillis()
            val elapsed = now - lastSubmissionEpoch
            val remaining = COOLDOWN_MS - elapsed

            if (!isDevBypass && remaining > 0) {
                val mins = (remaining / 1000) / 60
                val secs = (remaining / 1000) % 60
                AppToast.show(host, "Please wait ${mins}m ${secs}s before submitting another report.")
                return@setOnClickListener
            }

            // Daily rate limit check (max 10 submissions / 24 hours)
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dailyCount = feedbackPrefs.safeInt("submissions_count_$todayDateKey", 0)
            if (!isDevBypass && dailyCount >= 10) {
                AppToast.show(host, "Daily submission limit reached (10/day). Please try again tomorrow.", Toast.LENGTH_LONG)
                return@setOnClickListener
            }

            val userFeedbackText = inputField.text.toString().trim().take(2000)
            if (userFeedbackText.length < 5) {
                AppToast.show(host, "Please enter at least 5 characters.")
                return@setOnClickListener
            }

            val userContactText = contactField.text.toString().trim().take(100)
            sendBtn.isEnabled = false
            sendBtn.text = "⏳ Submitting..."

            val isBatteryOptIgnored = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    pm?.isIgnoringBatteryOptimizations(host.packageName) ?: false
                } else true
            } catch (_: Exception) { false }

            val notifsEnabled = try {
                androidx.core.app.NotificationManagerCompat.from(host).areNotificationsEnabled()
            } catch (_: Exception) { true }

            val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val currentTimerState = prefs.safeString("timerState", "IDLE") ?: "IDLE"
            val selectedSub = try { SubjectTagManager.getSelectedSubject(host).name } catch (_: Exception) { "General" }
            val dm = host.resources.displayMetrics
            val freeRamMb = Runtime.getRuntime().freeMemory() / (1024 * 1024)
            val maxRamMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val todayFocusSecs = prefs.safeLong("${todayStr}_focus_total", 0L)

            // Build Comprehensive Privacy-Safe Diagnostic Object
            val diagJson = org.json.JSONObject().apply {
                put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                put("android_os", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("brand", Build.BRAND)
                put("timer_mode", timerMode)
                put("timer_state", currentTimerState)
                put("active_subject", selectedSub)
                put("sync_status", if (AuthManager.isLoggedIn(host)) "ONLINE_SYNCED" else "GUEST_OFFLINE")
                put("notifications_enabled", notifsEnabled)
                put("battery_opt_ignored", isBatteryOptIgnored)
                put("leaderboard_participating", LeaderboardManager.isParticipating(host))
                put("live_status_sharing", LeaderboardManager.isLiveStatusSharingEnabled(host))
                put("today_focus_secs", todayFocusSecs)
                put("screen_resolution", "${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi}dpi)")
                put("memory_mb", "$freeRamMb / $maxRamMb MB")
                put("timestamp_epoch", System.currentTimeMillis() / 1000)
            }

            val typeEnum = when (selectedCategory) {
                "Bug Report" -> "BUG_REPORT"
                "Feature Request" -> "FEATURE_REQUEST"
                else -> "GENERAL_FEEDBACK"
            }

            // Execute Real HTTP POST in background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                var isSuccess = false
                var errorMessage = ""
                var statusCode: Int

                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

                val targetEndpoint = if (supabaseUrl.isNotBlank()) {
                    "$supabaseUrl/rest/v1/feedback_reports"
                } else ""

                val payload = org.json.JSONObject().apply {
                    put("type", typeEnum)
                    put("status", "NEW")
                    if (userContactText.isNotBlank()) put("user_contact", userContactText)
                    put("message", userFeedbackText)
                    put("diagnostics", if (includeDiagnostics) diagJson else org.json.JSONObject())
                }

                android.util.Log.d("FeedbackSubmission", "========================================")
                android.util.Log.d("FeedbackSubmission", "Sending to: $targetEndpoint")
                android.util.Log.d("FeedbackSubmission", "Payload: ${payload.toString()}")
                android.util.Log.d("FeedbackSubmission", "========================================")

                if (targetEndpoint.isNotBlank()) {
                    try {
                        val url = java.net.URL(targetEndpoint)
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", supabaseAnonKey)
                        conn.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Prefer", "return=representation")
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doOutput = true

                        conn.outputStream.use { os ->
                            os.write(payload.toString().toByteArray(Charsets.UTF_8))
                        }

                        statusCode = conn.responseCode
                        val responseBody = try {
                            if (statusCode in 200..299) {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                            }
                        } catch (_: Exception) { "Could not read response" }

                        android.util.Log.d("FeedbackSubmission", "Server Response HTTP $statusCode: $responseBody")

                        if (statusCode in 200..299) {
                            isSuccess = true
                        } else {
                            errorMessage = "Server HTTP $statusCode: $responseBody"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FeedbackSubmission", "Network error during feedback post", e)
                        errorMessage = e.localizedMessage ?: "Network connection error"
                    }
                } else {
                    errorMessage = "No backend endpoint configured."
                }

                // Handle on Main Thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (isSuccess) {
                        feedbackPrefs.edit()
                            .putLong("last_feedback_submission_epoch", System.currentTimeMillis())
                            .putInt("submissions_count_$todayDateKey", dailyCount + 1)
                            .apply()
                        AppToast.show(host, "Report submitted successfully! Thank you for your feedback.", Toast.LENGTH_LONG)
                        dialog.dismiss()
                    } else {
                        sendBtn.isEnabled = true
                        sendBtn.text = "Submit Report"
                        android.util.Log.w("FeedbackSubmission", "HTTP Failed: $errorMessage. Triggering email fallback.")

                        val devEmail = "studytimer737@gmail.com"
                        val emailSubject = "[StudyTimer Feedback - $selectedCategory] v${BuildConfig.VERSION_NAME}"
                        val diagnosticBlock = if (includeDiagnostics) {
                            """
                            
                            -------------------------------------
                            Diagnostic Info (Helps us fix bugs faster):
                            • App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                            • Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            • Device: ${Build.MANUFACTURER} ${Build.MODEL}
                            • Timer Mode Active: $timerMode
                            • Sync Status: ${if (AuthManager.isLoggedIn(host)) "Online" else "Guest"}
                            -------------------------------------
                            """.trimIndent()
                        } else ""

                        val emailBody = "$userFeedbackText\n$diagnosticBlock"

                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(devEmail))
                            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                            putExtra(Intent.EXTRA_TEXT, emailBody)
                        }

                        try {
                            AppToast.show(host, "Opening email client to send your feedback...", Toast.LENGTH_LONG)
                            host.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email..."))
                            dialog.dismiss()
                        } catch (_: Exception) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("StudyTimer Feedback", "To: $devEmail\nSubject: $emailSubject\n\n$emailBody"))
                            AppToast.show(host, "Feedback copied to clipboard! Please email to studytimer737@gmail.com", Toast.LENGTH_LONG)
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        content.addView(sendBtn)
        content.addView(cooldownWarningText)
        content.addView(urgentEmailBtn)

        val dismissRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(6), 0, 0)
        }
        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(tintedColor(themeCoordinator.textColor, 170))
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 14f)
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        dismissRow.addView(closeBtn)
        content.addView(dismissRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showAppGuideDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val titleText = TextView(host).apply {
            text = "StudyTimer — How to Use Summary"
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(titleText)

        val scrollView = ScrollView(host).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val list = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun createGuideCard(sectionNumber: String, sectionTitle: String, bullets: List<Pair<String, String>>) {
            val card = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (themeCoordinator.isDarkMode()) 0x18FFFFFF.toInt() else tintedColor(themeCoordinator.primaryColor, 15))
                    setStroke(dp(1), if (themeCoordinator.isDarkMode()) 0x22FFFFFF.toInt() else tintedColor(themeCoordinator.primaryColor, 40))
                }
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(14))
                }
            }

            val headerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }
            val titleView = TextView(host).apply {
                text = "$sectionNumber. $sectionTitle"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            headerRow.addView(titleView)
            card.addView(headerRow)

            for ((itemTitle, itemDesc) in bullets) {
                val itemRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(3), 0, dp(3))
                }
                val bulletDot = TextView(host).apply {
                    text = "• "
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, dp(4), 0)
                }
                val itemText = TextView(host).apply {
                    val fullSpannable = android.text.SpannableStringBuilder()
                    val boldSpan = android.text.style.StyleSpan(Typeface.BOLD)
                    val titleFormatted = "$itemTitle: "
                    fullSpannable.append(titleFormatted)
                    fullSpannable.setSpan(boldSpan, 0, titleFormatted.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    fullSpannable.append(itemDesc)
                    text = fullSpannable
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12.5f
                    setLineSpacing(0f, 1.15f)
                    alpha = 0.9f
                }
                itemRow.addView(bulletDot)
                itemRow.addView(itemText)
                card.addView(itemRow)
            }

            list.addView(card)
        }

        // 1. All 4 Timer Modes
        createGuideCard(
            "1",
            "Timer Modes & Focus Flow",
            listOf(
                "Subject-Wise Timer" to "Tag each session with subjects, custom color badges & icons to track detailed subject breakdowns.",
                "Pomodoro & White Mode" to "Focus intervals with automatic short/long breaks. Includes an optional Minimal White Background & Black Timer theme.",
                "Custom Countdown" to "Set targeted hours and minutes for timed study sprints, practice tests, and revision blocks.",
                "Stopwatch Mode" to "Open-ended count-up timer with precision lap recordings for flexible study.",
                "Finish Session" to "Long-press safety control: hold to complete and save your session."
            )
        )

        // 2. Planner & Habit Tracking
        createGuideCard(
            "2",
            "Planner, Goals & Study History",
            listOf(
                "Daily Habit Checklist" to "Create daily study goals, link subjects, set target minutes, and reorder via hold-and-drag.",
                "Weekly Goal History & Calendar" to "Tap any goal to inspect its monthly study calendar or view your full weekly completion history.",
                "Yesterday's Goals Logger" to "Easily retroactively log or adjust completed tasks and habits from the previous day anytime."
            )
        )

        // 3. Insights & Consistency Analytics
        createGuideCard(
            "3",
            "Insights, Heatmap & Calendar",
            listOf(
                "Daily & Weekly Analytics" to "Comprehensive overview of total focus hours, subject distribution pie chart, and daily study rhythms.",
                "Activity Heatmap" to "6-month visual consistency grid highlighting your daily study intensity and habit trends.",
                "Interactive Calendar" to "Explore day-by-day session timelines, individual logs, and goal achievement checkmarks."
            )
        )

        // 4. Themes, Display & Widgets
        createGuideCard(
            "4",
            "Themes, Display & Widgets",
            listOf(
                "Curated Theme Palettes" to "Sleek Dark, OLED True Black, and High-Contrast Light theme with custom vibrant accents.",
                "Fullscreen Digital Clock" to "Distraction-free full-screen clock with immersive status and navigation bar integration.",
                "Home Screen Widgets" to "Track your daily study progress and launch focus sessions directly from your home screen."
            )
        )

        // 5. Offline Storage & Cloud Sync
        createGuideCard(
            "5",
            "Offline Privacy & Cloud Backup",
            listOf(
                "100% Offline-First" to "Fast local database ensures all your study logs, streak records, and habits remain strictly private on device.",
                "Google Cloud Sync" to "Optional cloud backup to safely preserve, sync, and restore records across your devices seamlessly.",
                "100% Free & Open" to "Zero advertisements, no subscription paywalls, and fully unlocked features."
            )
        )

        scrollView.addView(list)
        content.addView(scrollView)

        val closeBtn = Button(host).apply {
            text = "GOT IT! LET'S STUDY"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
                dialog.dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, dp(14), 0, 0)
            }
        }
        content.addView(closeBtn)

        dialog.setOnDismissListener {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
        }
        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), (host.resources.displayMetrics.heightPixels * 0.80f).toInt())
        }
        dialog.show()
    }

    private fun showGoalDetailFloatingCard(item: Any, onRefresh: (() -> Unit)? = null) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val itemId: String
        val itemTitle: String
        val itemNote: String
        val itemTargetMins: Int
        val itemIsDeleted: Boolean
        val itemSubjectId: String?
        val itemSubTopic: String?
        val itemScheduledDays: List<Int>
        val itemCreatedAt: Long

        when (item) {
            is MatrixGoalItem -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = item.isDeleted
                itemSubjectId = item.subjectId
                itemSubTopic = item.subTopic
                itemScheduledDays = item.scheduledDays
                itemCreatedAt = item.createdAt
            }
            is PlannerGoal -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = false
                itemSubjectId = item.subjectId
                itemSubTopic = item.subTopic
                itemScheduledDays = item.scheduledDays
                itemCreatedAt = item.createdAt
            }
            else -> {
                itemId = runCatching { item.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getId").invoke(item) as String }.getOrNull() ?: ""
                itemTitle = runCatching { item.javaClass.getDeclaredField("title").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTitle").invoke(item) as String }.getOrNull() ?: ""
                itemNote = runCatching { item.javaClass.getDeclaredField("note").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getNote").invoke(item) as String }.getOrNull() ?: ""
                itemTargetMins = runCatching { item.javaClass.getDeclaredField("targetMinutes").apply { isAccessible = true }.get(item) as Int }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTargetMinutes").invoke(item) as Int }.getOrNull() ?: 0
                itemIsDeleted = runCatching { item.javaClass.getDeclaredField("isDeleted").apply { isAccessible = true }.get(item) as Boolean }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("isDeleted").invoke(item) as Boolean }.getOrNull() ?: false
                itemSubjectId = null
                itemSubTopic = null
                itemScheduledDays = emptyList()
                itemCreatedAt = 0L
            }
        }

        val goalsJson = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val activeGoal = goalsList.find { it.id == itemId }
        val noteText = if (activeGoal != null && activeGoal.note.isNotBlank()) activeGoal.note else itemNote
        val targetMins = if (activeGoal != null && activeGoal.targetMinutes > 0) activeGoal.targetMinutes else itemTargetMins

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        // 1. FIXED Header Tag
        val headerTag = TextView(host).apply {
            text = if (itemIsDeleted) "ARCHIVED GOAL" else "GOAL DETAILS"
            setTextColor(if (itemIsDeleted) 0xFFF59E0B.toInt() else plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        content.addView(headerTag)

        // 2. Scrollable Body Box (Title + Target + Notes)
        val maxCardBodyHeight = dp(240)
        val bodyScroll = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val bodyBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        bodyBox.addView(TextView(host).apply {
            text = if (itemTitle.isNotBlank()) itemTitle else "Untitled Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        })

        if (itemIsDeleted) {
            bodyBox.addView(TextView(host).apply {
                text = "Archived from planner • Past history & completions preserved"
                setTextColor(if (themeCoordinator.isDarkMode()) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                textSize = 12f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setPadding(0, 0, 0, dp(6))
            })
        }

        if (targetMins > 0) {
            bodyBox.addView(TextView(host).apply {
                text = "Target: $targetMins min / day"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 13f
                setPadding(0, 0, 0, dp(6))
            })
        }

        if (noteText.isNotBlank()) {
            bodyBox.addView(createDivider())
            bodyBox.addView(TextView(host).apply {
                text = "NOTES"
                setTextColor(tintedColor(themeCoordinator.textColor, 140))
                textSize = 10f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(4))
            })

            val noteBox = TextView(host).apply {
                text = noteText
                setTextColor(if (themeCoordinator.isDarkMode()) themeCoordinator.textColor else 0xFF0F172A.toInt())
                textSize = 14f
                setLineSpacing(dp(3).toFloat(), 1.1f)
                background = if (themeCoordinator.isDarkMode()) {
                    themeCoordinator.createGlassChip(Color.argb(30, 255, 255, 255), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF8FAFC.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }
            bodyBox.addView(noteBox)
        }

        bodyScroll.addView(bodyBox)

        bodyScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bodyScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (bodyScroll.height > maxCardBodyHeight) {
                    bodyScroll.layoutParams = (bodyScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxCardBodyHeight
                    }
                }
            }
        })
        content.addView(bodyScroll)

        // 3. Action Buttons Row at Bottom
        val buttonRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        val isDark = themeCoordinator.isDarkMode()
        if (itemIsDeleted) {
            val mergeBtn = TextView(host).apply {
                text = "Merge Progress"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(plannerPrimary)
                }
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1.25f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showMergeGoalDialog(itemId, itemTitle) {
                        onRefresh?.invoke()
                    }
                }
            }
            buttonRow.addView(mergeBtn)

            val restoreBtn = TextView(host).apply {
                text = "Restore"
                setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = if (isDark) {
                    themeCoordinator.createGlassChip(Color.argb(45, 56, 189, 248), 20f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFE0F2FE.toInt())
                        setStroke(dp(1), 0xFF38BDF8.toInt())
                    }
                }
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 0.95f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    dialog.dismiss()
                    val restored = PlannerHistoryManager.restoreArchivedGoal(
                        context = host,
                        archivedGoalId = itemId,
                        title = itemTitle,
                        note = itemNote,
                        targetMinutes = itemTargetMins,
                        subjectId = itemSubjectId,
                        subTopic = itemSubTopic,
                        scheduledDays = itemScheduledDays,
                        createdAt = itemCreatedAt
                    )
                    if (restored) {
                        AppToast.show(host, "Restored '$itemTitle' to active planner")
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            CloudSyncManager.syncDataToCloud(host)
                        }
                        onRefresh?.invoke()
                    } else {
                        AppToast.show(host, "Goal is already active in planner")
                    }
                }
            }
            buttonRow.addView(restoreBtn)

            val closeBtn = TextView(host).apply {
                text = "Close"
                setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = if (isDark) {
                    themeCoordinator.createGlassChip(Color.argb(35, 255, 255, 255), 20f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 0.8f)
                setOnClickListener { dialog.dismiss() }
            }
            buttonRow.addView(closeBtn)
            content.addView(buttonRow)

            val permDeleteBtn = TextView(host).apply {
                text = "Permanently Delete History"
                setTextColor(0xFFEF4444.toInt())
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setPadding(0, dp(14), 0, dp(2))
                setOnClickListener {
                    dialog.dismiss()
                    showDeletePlannerGoalMatrixDialog(itemId, itemTitle) {
                        onRefresh?.invoke()
                    }
                }
            }
            content.addView(permDeleteBtn)
        } else {
            if (activeGoal != null) {
                val historyBtn = TextView(host).apply {
                    text = "Goal History"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(plannerPrimary)
                    }
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
                    setOnClickListener {
                        dialog.dismiss()
                        showGoalHistoryDialog(activeGoal)
                    }
                }
                buttonRow.addView(historyBtn)
            }

            val closeBtn = TextView(host).apply {
                text = "Close"
                setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = if (isDark) {
                    themeCoordinator.createGlassChip(if (activeGoal != null) Color.argb(40, 255, 255, 255) else plannerPrimary, 20f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dp(1), 0xFFCBD5E1.toInt())
                    }
                }
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(if (activeGoal != null) 0 else LinearLayout.LayoutParams.MATCH_PARENT, dp(44), 1f)
                setOnClickListener { dialog.dismiss() }
            }
            buttonRow.addView(closeBtn)
            content.addView(buttonRow)
        }

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showMergeGoalDialog(
        fromGoalId: String,
        fromGoalTitle: String,
        onSuccess: () -> Unit
    ) {
        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val textColor = themeCoordinator.textColor

        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val goalsJson = prefs.getString("session_goals_json", "[]") ?: "[]"
        val activeGoals = loadSessionGoalsFromJson(goalsJson).filterNot { it.id == fromGoalId }

        if (activeGoals.isEmpty()) {
            DialogFactory.showInfoDialog(
                context = host,
                themeCoordinator = themeCoordinator,
                title = "No Active Goals Found",
                message = "There are no active goals currently in your planner. Please create an active goal first before merging past history."
            )
            return
        }

        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(18))
        }

        content.addView(TextView(host).apply {
            text = "MERGE GOAL HISTORY"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Merge '$fromGoalTitle' into..."
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        })

        content.addView(TextView(host).apply {
            text = "Select an active goal below. All past completed days, checkmarks, and tracked study hours will combine seamlessly into it."
            setTextColor(tintedColor(textColor, if (isDark) 180 else 160))
            textSize = 12.5f
            setLineSpacing(dp(2).toFloat(), 1.05f)
            setPadding(0, 0, 0, dp(14))
        })

        val listScroll = ScrollView(host).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            )
        }

        val listContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (active in activeGoals) {
            val sub = if (!active.subjectId.isNullOrBlank() && active.subjectId != "all") {
                SubjectTagManager.resolveSubject(host, active.subjectId)
            } else null

            val subColor = sub?.let { runCatching { Color.parseColor(it.colorHex) }.getOrNull() } ?: plannerPrimary

            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val rowMask = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.WHITE)
                }
                val rowContent = if (isDark) {
                    themeCoordinator.createGlassChip(Color.argb(25, 255, 255, 255), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(0xFFF8FAFC.toInt())
                        setStroke(dp(1), 0xFFE2E8F0.toInt())
                    }
                }
                background = RippleDrawable(
                    ColorStateList.valueOf(tintedColor(plannerPrimary, 40)),
                    rowContent,
                    rowMask
                )
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    DialogFactory.showConfirmationDialog(
                        context = host,
                        themeCoordinator = themeCoordinator,
                        title = "Confirm Merge?",
                        message = "Merge all history from '$fromGoalTitle' into '${active.title}'?\n\nThis will combine all past checkmarks, study records, and streaks into '${active.title}'. '$fromGoalTitle' will be removed from your archived list.",
                        positiveText = "Merge History",
                        negativeText = "Cancel",
                        onConfirm = {
                            PlannerHistoryManager.mergeGoalHistory(
                                context = host,
                                fromGoalId = fromGoalId,
                                toGoalId = active.id,
                                toGoalTitle = active.title
                            )
                            AppToast.show(host, "Merged '$fromGoalTitle' into '${active.title}'")
                            dialog.dismiss()
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                CloudSyncManager.syncDataToCloud(host)
                            }
                            onSuccess()
                        }
                    )
                }
            }

            val iconPill = FrameLayout(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    rightMargin = dp(10)
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(tintedColor(subColor, if (isDark) 40 else 25))
                }
            }
            iconPill.addView(ImageView(host).apply {
                setImageResource(R.drawable.ic_merge)
                imageTintList = ColorStateList.valueOf(subColor)
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER)
            })
            row.addView(iconPill)

            val textCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(host).apply {
                text = active.title
                setTextColor(if (isDark) themeCoordinator.textColor else 0xFF0F172A.toInt())
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            val detailDesc = StringBuilder()
            if (active.targetMinutes > 0) detailDesc.append("${active.targetMinutes}m daily target")
            if (sub != null) {
                if (detailDesc.isNotEmpty()) detailDesc.append(" • ")
                detailDesc.append(sub.name)
            }
            if (detailDesc.isNotEmpty()) {
                textCol.addView(TextView(host).apply {
                    text = detailDesc.toString()
                    setTextColor(tintedColor(textColor, if (isDark) 140 else 150))
                    textSize = 11f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                })
            }
            row.addView(textCol)
            listContainer.addView(row)
        }
        listScroll.addView(listContainer)
        content.addView(listScroll)

        val closeBtn = TextView(host).apply {
            text = "Cancel"
            setTextColor(tintedColor(textColor, if (isDark) 180 else 160))
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                topMargin = dp(8)
            }
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun showArchivedGoalActionMenu(item: MatrixGoalItem, onRefresh: () -> Unit) {
        val isDark = themeCoordinator.isDarkMode()
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val textColor = themeCoordinator.textColor

        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(26f)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }

        content.addView(TextView(host).apply {
            text = "ARCHIVED GOAL OPTIONS"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = item.title
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(14))
        })

        fun createOptionRow(iconRes: Int, iconTint: Int, title: String, subtitle: String, onClick: () -> Unit): View {
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val rowMask = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.WHITE)
                }
                val rowContent = if (isDark) {
                    themeCoordinator.createGlassChip(Color.argb(25, 255, 255, 255), 12f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(0xFFF8FAFC.toInt())
                        setStroke(dp(1), 0xFFE2E8F0.toInt())
                    }
                }
                background = RippleDrawable(
                    ColorStateList.valueOf(tintedColor(iconTint, 40)),
                    rowContent,
                    rowMask
                )
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    dialog.dismiss()
                    onClick()
                }
            }

            val iconPill = FrameLayout(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    rightMargin = dp(10)
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(tintedColor(iconTint, if (isDark) 40 else 25))
                }
            }
            iconPill.addView(ImageView(host).apply {
                setImageResource(iconRes)
                imageTintList = ColorStateList.valueOf(iconTint)
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER)
            })
            row.addView(iconPill)

            val textCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(host).apply {
                text = title
                setTextColor(textColor)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            textCol.addView(TextView(host).apply {
                text = subtitle
                setTextColor(tintedColor(textColor, if (isDark) 140 else 160))
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            })
            row.addView(textCol)

            return row
        }

        // 1. Merge into Active Goal
        content.addView(createOptionRow(
            R.drawable.ic_merge,
            plannerPrimary,
            "Merge into Active Goal",
            "Combine this goal's past history with another active goal"
        ) {
            showMergeGoalDialog(item.id, item.title, onRefresh)
        })

        // 2. Restore to Planner
        val restoreColor = if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        content.addView(createOptionRow(
            R.drawable.ic_restore,
            restoreColor,
            "Restore to Planner",
            "Re-activate this goal into your daily planner"
        ) {
            DialogFactory.showConfirmationDialog(
                context = host,
                themeCoordinator = themeCoordinator,
                title = "Restore Goal to Planner?",
                message = "Restore '${item.title}' back to your active daily planner with all of its existing progress intact?",
                positiveText = "Restore",
                negativeText = "Cancel",
                onConfirm = {
                    val restored = PlannerHistoryManager.restoreArchivedGoal(
                        context = host,
                        archivedGoalId = item.id,
                        title = item.title,
                        note = item.note,
                        targetMinutes = item.targetMinutes,
                        subjectId = item.subjectId,
                        subTopic = item.subTopic,
                        scheduledDays = item.scheduledDays,
                        createdAt = item.createdAt
                    )
                    if (restored) {
                        AppToast.show(host, "Restored '${item.title}' to active planner")
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            CloudSyncManager.syncDataToCloud(host)
                        }
                        onRefresh()
                    } else {
                        AppToast.show(host, "Goal is already active in planner")
                    }
                }
            )
        })

        // 3. Delete from History
        val dangerRed = 0xFFEF4444.toInt()
        content.addView(createOptionRow(
            R.drawable.ic_trash,
            dangerRed,
            "Delete from History",
            "Permanently erase all past records of this goal"
        ) {
            showDeletePlannerGoalMatrixDialog(item.id, item.title, onRefresh)
        })

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            setTextColor(tintedColor(textColor, if (isDark) 180 else 160))
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                topMargin = dp(4)
            }
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(cancelBtn)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showDeletePlannerGoalMatrixDialog(goalId: String, goalTitle: String, onDeleted: () -> Unit) {
        DialogFactory.showConfirmationDialog(
            context = host,
            themeCoordinator = themeCoordinator,
            title = "Delete Goal from History?",
            message = "Goal: $goalTitle\n\nWould you like to keep or delete this goal from your weekly history tracker?",
            positiveText = "Delete",
            negativeText = "Keep",
            isDestructive = true,
            onConfirm = {
                val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val activeJson = prefs.getString("session_goals_json", "[]") ?: "[]"
                val activeList = loadSessionGoalsFromJson(activeJson).filterNot { it.id == goalId }
                saveSessionGoalsToJson(activeList)

                PlannerHistoryManager.deleteGoalHistory(host, goalId)

                AppToast.show(host, "Deleted '$goalTitle' from history tracker")
                onDeleted()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    CloudSyncManager.syncDataToCloud(host)
                }
            }
        )
    }

    private fun promptAddSubTopicDialog(subjectId: String, onAdded: (String) -> Unit) {
        val input = android.widget.EditText(host).apply {
            hint = "Chapter / Sub-Topic name"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, dp(14))
            }
        }
        val dialog = Dialog(host).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        }
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(22f)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        container.addView(TextView(host).apply {
            text = "Add Chapter"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
        })
        container.addView(input)
        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            background = null
            setTextColor(themeCoordinator.textColor)
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Add"
            background = rippleBackground(themeCoordinator.primaryColor)
            setTextColor(ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor))
            setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (SubjectTagManager.addSubTopic(host, subjectId, name)) {
                        onAdded(name)
                        dialog.dismiss()
                    } else {
                        AppToast.show(host, "Chapter already exists")
                    }
                }
            }
        })
        container.addView(btnRow)
        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun showAddSessionGoalDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "New Study Goal"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box to ensure buttons never go offscreen
        val scrollContainer = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val inputsBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(host).apply {
            hint = "Goal name (e.g. Math practice, Physics)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(host).apply {
            hint = "Notes (optional)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(host).apply {
            hint = "Target time in minutes (or select below)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) }
        }

        // 1-Tap Duration Presets Row
        val durationPresets = listOf(15, 25, 45, 60, 90, 120)
        val presetScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val presetRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        for (m in durationPresets) {
            val chip = TextView(host).apply {
                text = "${m}m"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    durationInput.setText(m.toString())
                    durationInput.setSelection(durationInput.text.length)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)

        inputsBox.addView(durationInput)
        inputsBox.addView(presetScroll)

        // Progressive Disclosure: Collapsible container for secondary configuration
        val moreOptionsHeader = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(10))
            isClickable = true
        }
        val moreOptionsToggleText = TextView(host).apply {
            text = "More Options (Subjects, Days, Reminders) ▾"
            textSize = 12.5f
            typeface = host.themeCoordinator.getAppTypeface(isBold = true)
            setTextColor(themeCoordinator.primaryColor)
        }
        moreOptionsHeader.addView(moreOptionsToggleText)
        inputsBox.addView(moreOptionsHeader)

        val moreOptionsContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        inputsBox.addView(moreOptionsContainer)

        var isMoreOptionsExpanded = false
        moreOptionsHeader.setOnClickListener {
            isMoreOptionsExpanded = !isMoreOptionsExpanded
            moreOptionsContainer.visibility = if (isMoreOptionsExpanded) View.VISIBLE else View.GONE
            moreOptionsToggleText.text = if (isMoreOptionsExpanded) "Fewer Options ▴" else "More Options (Subjects, Days, Reminders) ▾"
        }

        // Subject Tag Toggle Card (Off by default)
        var subjectEnabled = false
        var selectedSubjectId: String? = null
        var selectedSubTopic: String? = null
        val allSubjects = SubjectTagManager.getAllSubjects(host)

        val subjectCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val subjectHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val subjectTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        subjectTextCol.addView(TextView(host).apply {
            text = "Subject & Topic"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        subjectTextCol.addView(TextView(host).apply {
            text = "Tag with a specific subject or chapter"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        subjectHeaderRow.addView(subjectTextCol)

        val subjectSwitch = SwitchMaterial(host).apply {
            isChecked = false
        }
        subjectHeaderRow.addView(subjectSwitch)
        subjectCard.addView(subjectHeaderRow)

        val subjectContentContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        val subjectChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val subjectChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Sub-topic selection container
        val subTopicSection = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val subTopicLabel = TextView(host).apply {
            text = "Chapter / Sub-Topic (optional)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        }
        subTopicSection.addView(subTopicLabel)

        val subTopicChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
        }
        val subTopicChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        subTopicChipScroll.addView(subTopicChipRow)
        subTopicSection.addView(subTopicChipScroll)

        fun refreshSubTopicChips() {
            val subId = selectedSubjectId
            if (subId == null || subId == "all") {
                subTopicSection.visibility = View.GONE
                selectedSubTopic = null
                return
            }
            subTopicSection.visibility = View.VISIBLE
            subTopicChipRow.removeAllViews()

            val isAllChapters = selectedSubTopic.isNullOrBlank()
            val allSubChip = TextView(host).apply {
                text = "All Chapters"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllChapters) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllChapters) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubTopic = null
                    refreshSubTopicChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subTopicChipRow.addView(allSubChip)

            val subTopics = SubjectTagManager.getSubTopics(host, subId)
            for (st in subTopics) {
                val isSelected = selectedSubTopic == st
                val chip = TextView(host).apply {
                    text = st
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubTopic = st
                        refreshSubTopicChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subTopicChipRow.addView(chip)
            }

            // + Add Chapter chip
            val addChip = TextView(host).apply {
                text = "+ Add Chapter"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 12f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    promptAddSubTopicDialog(subId) { newName ->
                        selectedSubTopic = newName
                        refreshSubTopicChips()
                    }
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subTopicChipRow.addView(addChip)
        }

        fun refreshSubjectChips() {
            subjectChipRow.removeAllViews()

            // "All / General" Chip
            val isAllSelected = selectedSubjectId == null || selectedSubjectId == "all"
            val allChip = TextView(host).apply {
                text = "All Subjects"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllSelected) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllSelected) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubjectId = null
                    selectedSubTopic = null
                    refreshSubjectChips()
                    refreshSubTopicChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subjectChipRow.addView(allChip)

            for (sub in allSubjects) {
                val isSelected = selectedSubjectId == sub.id
                val subColor = try { Color.parseColor(sub.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                val chip = TextView(host).apply {
                    text = sub.name
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(subColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubjectId = sub.id
                        selectedSubTopic = null
                        refreshSubjectChips()
                        refreshSubTopicChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subjectChipRow.addView(chip)
            }
        }
        refreshSubjectChips()
        subjectChipScroll.addView(subjectChipRow)
        subjectContentContainer.addView(subjectChipScroll)
        subjectContentContainer.addView(subTopicSection)
        subjectCard.addView(subjectContentContainer)
        moreOptionsContainer.addView(subjectCard)

        subjectSwitch.setOnCheckedChangeListener { _, isChecked ->
            subjectEnabled = isChecked
            if (isChecked && (selectedSubjectId == null || selectedSubjectId == "all")) {
                selectedSubjectId = SubjectTagManager.getSelectedSubject(host).id
                refreshSubjectChips()
                refreshSubTopicChips()
            }
            subjectContentContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Scheduled Days Toggle Card (Off by default)
        var scheduleEnabled = false
        val selectedDays = mutableSetOf<Int>() // empty = Every Day
        val scheduleDaysList = listOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )

        val scheduleCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val scheduleHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val scheduleTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        scheduleTextCol.addView(TextView(host).apply {
            text = "Scheduled Days"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        scheduleTextCol.addView(TextView(host).apply {
            text = "Choose active study days (otherwise every day)"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        scheduleHeaderRow.addView(scheduleTextCol)

        val scheduleSwitch = SwitchMaterial(host).apply {
            isChecked = false
        }
        scheduleHeaderRow.addView(scheduleSwitch)
        scheduleCard.addView(scheduleHeaderRow)

        val scheduleContentContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        val scheduleChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(4))
            }
        }
        val scheduleChipRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        scheduleChipScroll.addView(scheduleChipRow)

        fun refreshScheduleChips() {
            scheduleChipRow.removeAllViews()

            val isEveryDay = selectedDays.isEmpty() || selectedDays.size == 7
            val everyDayChip = TextView(host).apply {
                text = "Every Day"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isEveryDay) Color.WHITE else themeCoordinator.textColor)
                background = if (isEveryDay) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedDays.clear()
                    refreshScheduleChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            scheduleChipRow.addView(everyDayChip)

            for ((dayInt, dayLabel) in scheduleDaysList) {
                val isSelected = !isEveryDay && selectedDays.contains(dayInt)
                val dayChip = TextView(host).apply {
                    text = dayLabel
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        if (isEveryDay) {
                            selectedDays.clear()
                            selectedDays.add(dayInt)
                        } else {
                            if (selectedDays.contains(dayInt)) {
                                selectedDays.remove(dayInt)
                            } else {
                                selectedDays.add(dayInt)
                            }
                        }
                        refreshScheduleChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                scheduleChipRow.addView(dayChip)
            }
        }
        refreshScheduleChips()
        scheduleContentContainer.addView(scheduleChipScroll)
        scheduleCard.addView(scheduleContentContainer)
        moreOptionsContainer.addView(scheduleCard)

        scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            scheduleEnabled = isChecked
            scheduleContentContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // 5. Reminder Section (Off by default, reveals custom time picker on toggle)
        var reminderEnabled = false
        var reminderHour = 20
        var reminderMinute = 0

        val reminderCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }

        val reminderHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val reminderTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        reminderTextCol.addView(TextView(host).apply {
            text = "Reminder"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        reminderTextCol.addView(TextView(host).apply {
            text = "Get notified when it's time to study"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        reminderHeaderRow.addView(reminderTextCol)

        val reminderSwitch = SwitchMaterial(host).apply {
            isChecked = false
        }
        reminderHeaderRow.addView(reminderSwitch)
        reminderCard.addView(reminderHeaderRow)

        fun formatReminderTime(h: Int, m: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
            }
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        }

        val timePickerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(2))
        }

        val timePill = TextView(host).apply {
            text = formatReminderTime(reminderHour, reminderMinute)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
            val clockIcon = androidx.core.content.ContextCompat.getDrawable(host, R.drawable.ic_clock)?.mutate()?.apply {
                setTint(themeCoordinator.primaryColor)
                setBounds(0, 0, dp(15), dp(15))
            }
            setCompoundDrawables(clockIcon, null, null, null)
            compoundDrawablePadding = dp(6)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            setOnClickListener {
                android.app.TimePickerDialog(
                    host,
                    { _, hourOfDay, minute ->
                        reminderHour = hourOfDay
                        reminderMinute = minute
                        text = formatReminderTime(reminderHour, reminderMinute)
                    },
                    reminderHour,
                    reminderMinute,
                    false
                ).show()
            }
        }
        timePickerRow.addView(TextView(host).apply {
            text = "Alert at: "
            textSize = 12f
            setTextColor(tintedColor(themeCoordinator.textColor, 180))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        timePickerRow.addView(timePill)
        reminderCard.addView(timePickerRow)

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderEnabled = isChecked
            timePickerRow.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        moreOptionsContainer.addView(reminderCard)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save Goal"
            setTextColor(ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor))
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val titleText = titleInput.text.toString().trim()
                if (titleText.isNotBlank()) {
                    val noteText = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val targetMins = rawMins.coerceAtMost(1440)
                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val scheduledList = if (scheduleEnabled && selectedDays.isNotEmpty() && selectedDays.size < 7) selectedDays.sorted() else emptyList()
                    val newGoal = PlannerGoal(
                        title = titleText,
                        note = noteText,
                        targetMinutes = targetMins,
                        subjectId = if (subjectEnabled) selectedSubjectId else null,
                        subTopic = if (subjectEnabled) selectedSubTopic else null,
                        scheduledDays = scheduledList,
                        reminderEnabled = reminderEnabled,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute
                    )
                    currentGoals.add(newGoal)
                    saveSessionGoalsToJson(currentGoals)
                    if (reminderEnabled) {
                        GoalReminderScheduler.scheduleGoalReminder(host, newGoal)
                    }
                    refreshStatsPanel()
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showEditSessionGoalDialog(goal: PlannerGoal) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, _) = resolvePlannerColors()

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Edit Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box
        val scrollContainer = ScrollView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val inputsBox = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(host).apply {
            hint = "Goal name"
            setText(goal.title)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(host).apply {
            hint = "Notes (optional)"
            setText(goal.note)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(host).apply {
            hint = "Target time in minutes (or select below)"
            setText(if (goal.targetMinutes > 0) goal.targetMinutes.toString() else "")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) }
        }
        inputsBox.addView(durationInput)

        // 1-Tap Duration Presets Row
        val durationPresets = listOf(15, 25, 45, 60, 90, 120)
        val presetScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val presetRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        for (m in durationPresets) {
            val chip = TextView(host).apply {
                text = "${m}m"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 40), 10f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    durationInput.setText(m.toString())
                    durationInput.setSelection(durationInput.text.length)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(6), 0)
                }
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        inputsBox.addView(presetScroll)

        // Subject Tag Toggle Card
        val hasInitialSubject = !goal.subjectId.isNullOrBlank() && goal.subjectId != "all"
        var subjectEnabled = hasInitialSubject
        var selectedSubjectId: String? = goal.subjectId
        var selectedSubTopic: String? = goal.subTopic
        val allSubjects = SubjectTagManager.getAllSubjects(host)

        val subjectCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val subjectHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val subjectTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        subjectTextCol.addView(TextView(host).apply {
            text = "Subject & Topic"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        subjectTextCol.addView(TextView(host).apply {
            text = "Tag with a specific subject or chapter"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        subjectHeaderRow.addView(subjectTextCol)

        val subjectSwitch = SwitchMaterial(host).apply {
            isChecked = hasInitialSubject
        }
        subjectHeaderRow.addView(subjectSwitch)
        subjectCard.addView(subjectHeaderRow)

        val subjectContentContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (hasInitialSubject) View.VISIBLE else View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        val subjectChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val subjectChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Sub-topic selection container
        val subTopicSection = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (selectedSubjectId != null && selectedSubjectId != "all") View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val subTopicLabel = TextView(host).apply {
            text = "Chapter / Sub-Topic (optional)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        }
        subTopicSection.addView(subTopicLabel)

        val subTopicChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
        }
        val subTopicChipRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        subTopicChipScroll.addView(subTopicChipRow)
        subTopicSection.addView(subTopicChipScroll)

        fun refreshSubTopicChips() {
            val subId = selectedSubjectId
            if (subId == null || subId == "all") {
                subTopicSection.visibility = View.GONE
                selectedSubTopic = null
                return
            }
            subTopicSection.visibility = View.VISIBLE
            subTopicChipRow.removeAllViews()

            val isAllChapters = selectedSubTopic.isNullOrBlank()
            val allSubChip = TextView(host).apply {
                text = "All Chapters"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllChapters) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllChapters) {
                    themeCoordinator.createGlassChip(plannerPrimary, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubTopic = null
                    refreshSubTopicChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subTopicChipRow.addView(allSubChip)

            val subTopics = SubjectTagManager.getSubTopics(host, subId)
            for (st in subTopics) {
                val isSelected = selectedSubTopic == st
                val chip = TextView(host).apply {
                    text = st
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(plannerPrimary, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubTopic = st
                        refreshSubTopicChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subTopicChipRow.addView(chip)
            }

            // + Add Chapter chip
            val addChip = TextView(host).apply {
                text = "+ Add Chapter"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(plannerPrimary)
                background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 35), 12f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    promptAddSubTopicDialog(subId) { newName ->
                        selectedSubTopic = newName
                        refreshSubTopicChips()
                    }
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subTopicChipRow.addView(addChip)
        }

        fun refreshSubjectChips() {
            subjectChipRow.removeAllViews()

            // "All / General" Chip
            val isAllSelected = selectedSubjectId == null || selectedSubjectId == "all"
            val allChip = TextView(host).apply {
                text = "All Subjects"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isAllSelected) Color.WHITE else themeCoordinator.textColor)
                background = if (isAllSelected) {
                    themeCoordinator.createGlassChip(plannerPrimary, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedSubjectId = null
                    selectedSubTopic = null
                    refreshSubjectChips()
                    refreshSubTopicChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            subjectChipRow.addView(allChip)

            for (sub in allSubjects) {
                val isSelected = selectedSubjectId == sub.id
                val subColor = try { Color.parseColor(sub.colorHex) } catch (_: Exception) { plannerPrimary }
                val chip = TextView(host).apply {
                    text = sub.name
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(subColor, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        selectedSubjectId = sub.id
                        selectedSubTopic = null
                        refreshSubjectChips()
                        refreshSubTopicChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                subjectChipRow.addView(chip)
            }
        }
        refreshSubjectChips()
        refreshSubTopicChips()
        subjectChipScroll.addView(subjectChipRow)
        subjectContentContainer.addView(subjectChipScroll)
        subjectContentContainer.addView(subTopicSection)
        subjectCard.addView(subjectContentContainer)
        inputsBox.addView(subjectCard)

        subjectSwitch.setOnCheckedChangeListener { _, isChecked ->
            subjectEnabled = isChecked
            if (isChecked && (selectedSubjectId == null || selectedSubjectId == "all")) {
                selectedSubjectId = SubjectTagManager.getSelectedSubject(host).id
                refreshSubjectChips()
                refreshSubTopicChips()
            }
            subjectContentContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Scheduled Days Toggle Card
        val hasInitialSchedule = goal.scheduledDays.isNotEmpty() && goal.scheduledDays.size < 7
        var scheduleEnabled = hasInitialSchedule
        val selectedDays = goal.scheduledDays.toMutableSet()
        val scheduleDaysList = listOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )

        val scheduleCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val scheduleHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val scheduleTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        scheduleTextCol.addView(TextView(host).apply {
            text = "Scheduled Days"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        scheduleTextCol.addView(TextView(host).apply {
            text = "Choose active study days (otherwise every day)"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        scheduleHeaderRow.addView(scheduleTextCol)

        val scheduleSwitch = SwitchMaterial(host).apply {
            isChecked = hasInitialSchedule
        }
        scheduleHeaderRow.addView(scheduleSwitch)
        scheduleCard.addView(scheduleHeaderRow)

        val scheduleContentContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (hasInitialSchedule) View.VISIBLE else View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        val scheduleChipScroll = HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(4))
            }
        }
        val scheduleChipRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        scheduleChipScroll.addView(scheduleChipRow)

        fun refreshScheduleChips() {
            scheduleChipRow.removeAllViews()

            val isEveryDay = selectedDays.isEmpty() || selectedDays.size == 7
            val everyDayChip = TextView(host).apply {
                text = "Every Day"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isEveryDay) Color.WHITE else themeCoordinator.textColor)
                background = if (isEveryDay) {
                    themeCoordinator.createGlassChip(plannerPrimary, 12f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    selectedDays.clear()
                    refreshScheduleChips()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            scheduleChipRow.addView(everyDayChip)

            for ((dayInt, dayLabel) in scheduleDaysList) {
                val isSelected = !isEveryDay && selectedDays.contains(dayInt)
                val dayChip = TextView(host).apply {
                    text = dayLabel
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        themeCoordinator.createGlassChip(plannerPrimary, 12f)
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        if (isEveryDay) {
                            selectedDays.clear()
                            selectedDays.add(dayInt)
                        } else {
                            if (selectedDays.contains(dayInt)) {
                                selectedDays.remove(dayInt)
                            } else {
                                selectedDays.add(dayInt)
                            }
                        }
                        refreshScheduleChips()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                scheduleChipRow.addView(dayChip)
            }
        }
        refreshScheduleChips()
        scheduleContentContainer.addView(scheduleChipScroll)
        scheduleCard.addView(scheduleContentContainer)
        inputsBox.addView(scheduleCard)

        scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            scheduleEnabled = isChecked
            scheduleContentContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // 5. Reminder Section (Pre-filled with goal settings)
        var reminderEnabled = goal.reminderEnabled
        var reminderHour = goal.reminderHour
        var reminderMinute = goal.reminderMinute

        val reminderCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }

        val reminderHeaderRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val reminderTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        reminderTextCol.addView(TextView(host).apply {
            text = "Reminder"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        })
        reminderTextCol.addView(TextView(host).apply {
            text = "Get notified when it's time to study"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        })
        reminderHeaderRow.addView(reminderTextCol)

        val reminderSwitch = SwitchMaterial(host).apply {
            isChecked = reminderEnabled
        }
        reminderHeaderRow.addView(reminderSwitch)
        reminderCard.addView(reminderHeaderRow)

        fun formatReminderTime(h: Int, m: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
            }
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        }

        val timePickerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = if (reminderEnabled) View.VISIBLE else View.GONE
            setPadding(0, dp(8), 0, dp(2))
        }

        val timePill = TextView(host).apply {
            text = formatReminderTime(reminderHour, reminderMinute)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(plannerPrimary)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 40), 12f)
            val clockIcon = androidx.core.content.ContextCompat.getDrawable(host, R.drawable.ic_clock)?.mutate()?.apply {
                setTint(plannerPrimary)
                setBounds(0, 0, dp(15), dp(15))
            }
            setCompoundDrawables(clockIcon, null, null, null)
            compoundDrawablePadding = dp(6)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            setOnClickListener {
                android.app.TimePickerDialog(
                    host,
                    { _, hourOfDay, minute ->
                        reminderHour = hourOfDay
                        reminderMinute = minute
                        text = formatReminderTime(reminderHour, reminderMinute)
                    },
                    reminderHour,
                    reminderMinute,
                    false
                ).show()
            }
        }
        timePickerRow.addView(TextView(host).apply {
            text = "Alert at: "
            textSize = 12f
            setTextColor(tintedColor(themeCoordinator.textColor, 180))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        timePickerRow.addView(timePill)
        reminderCard.addView(timePickerRow)

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderEnabled = isChecked
            timePickerRow.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        inputsBox.addView(reminderCard)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val deleteGoalBtn = Button(host).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#EF4444"))
            background = null
            setOnClickListener {
                dialog.dismiss()
                showConfirmDialog(
                    title = "Delete Goal?",
                    message = "Are you sure you want to remove '${goal.title}' from your planner?"
                ) {
                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val updated = currentGoals.filter { it.id != goal.id }
                    saveSessionGoalsToJson(updated)
                    GoalReminderScheduler.cancelGoalReminder(host, goal.id)
                    PlannerHistoryManager.snapshotToday(host, updated)
                    refreshStatsPanel()
                }
            }
        }
        btnRow.addView(deleteGoalBtn)

        val spacer = View(host).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }
        btnRow.addView(spacer)

        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save Changes"
            setTextColor(ThemeCoordinator.getContrastColor(plannerPrimary))
            background = rippleBackground(plannerPrimary)
            setOnClickListener {
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    val newNote = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val newTargetMins = rawMins.coerceAtMost(1440)

                    val currentGoals = loadSessionGoalsFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val idx = currentGoals.indexOfFirst { it.id == goal.id }
                    if (idx >= 0) {
                        val oldG = currentGoals[idx]
                        val scheduledList = if (scheduleEnabled && selectedDays.isNotEmpty() && selectedDays.size < 7) selectedDays.sorted() else emptyList()
                        val updatedGoal = PlannerGoal(
                            id = oldG.id,
                            title = newTitle,
                            note = newNote,
                            targetMinutes = newTargetMins,
                            completed = oldG.completed,
                            checkedAt = oldG.checkedAt,
                            createdAt = oldG.createdAt,
                            subjectId = if (subjectEnabled) selectedSubjectId else null,
                            subTopic = if (subjectEnabled) selectedSubTopic else null,
                            scheduledDays = scheduledList,
                            reminderEnabled = reminderEnabled,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute
                        )
                        currentGoals[idx] = updatedGoal
                        saveSessionGoalsToJson(currentGoals)
                        if (reminderEnabled && !oldG.completed) {
                            GoalReminderScheduler.scheduleGoalReminder(host, updatedGoal)
                        } else {
                            GoalReminderScheduler.cancelGoalReminder(host, oldG.id)
                        }
                        refreshStatsPanel()
                    }
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<LectureScheduleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(LectureScheduleItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Lecture"),
                    startTime = obj.optString("startTime", "09:00"),
                    endTime = obj.optString("endTime", "10:00"),
                    enabled = obj.optBoolean("enabled", true),
                    subjectId = obj.optString("subjectId", "general")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveLectureSchedulesToJson(items: List<LectureScheduleItem>) {
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("enabled", item.enabled)
                put("subjectId", item.subjectId)
            }
            array.put(obj)
        }
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val hasEnabled = items.any { it.enabled }
        prefs.edit()
            .putString("lecture_schedules_json", array.toString())
            .apply()

        if (hasEnabled) {
            LectureScheduleScheduler.scheduleAll(host)
        } else {
            LectureScheduleScheduler.cancelAll(host)
        }
    }

    internal fun showLectureScheduleManagerDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val contentWidth = (host.resources.displayMetrics.widthPixels * 0.82).toInt()
        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        val topRow = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(host).apply {
            text = "\uD83C\uDF93 Class Timetable"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val addBtn = TextView(host).apply {
            text = "+ Add Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 12f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                dialog.dismiss()
                showAddLectureScheduleDialog()
            }
        }
        topRow.addView(addBtn)
        content.addView(topRow)

        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val items = loadLectureSchedulesFromJson(prefs.getString("lecture_schedules_json", "[]") ?: "[]")

        fun formatScheduleTime(timeStr: String): String {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return TimeFormat.formatHourMinute(host, h, m)
            }
            return timeStr
        }

        if (items.isEmpty()) {
            content.addView(TextView(host).apply {
                text = "No scheduled classes yet.\nTap '+ Add Class' to set start and end times."
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
            })
        } else {
            val listContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }
            for (item in items) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val textCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val itemSub = SubjectTagManager.resolveSubject(host, item.subjectId)
                textCol.addView(TextView(host).apply {
                    text = item.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                textCol.addView(TextView(host).apply {
                    text = "${formatScheduleTime(item.startTime)} - ${formatScheduleTime(item.endTime)}"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                })
                row.addView(textCol)

                val toggleSwitch = SwitchMaterial(host).apply {
                    isChecked = item.enabled
                    scaleX = 0.75f
                    scaleY = 0.75f
                    setOnCheckedChangeListener { _, isChecked ->
                        val updated = items.map { if (it.id == item.id) it.copy(enabled = isChecked) else it }
                        saveLectureSchedulesToJson(updated)
                    }
                }

                row.addView(toggleSwitch)

                val editBtn = TextView(host).apply {
                    text = "\u270F\uFE0F"
                    textSize = 14f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        dialog.dismiss()
                        showAddLectureScheduleDialog(editItem = item)
                    }
                }
                row.addView(editBtn)

                val delBtn = TextView(host).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 16f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        showConfirmDialog("Delete Class?", "Are you sure you want to remove '${item.title}' from your timetable?") {
                            val updated = items.filter { it.id != item.id }
                            saveLectureSchedulesToJson(updated)
                            dialog.dismiss()
                            showLectureScheduleManagerDialog()
                        }
                    }
                }
                row.addView(delBtn)
                listContainer.addView(row)
            }
            content.addView(listContainer)
        }

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    internal fun showAddLectureScheduleDialog(editItem: LectureScheduleItem? = null) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val contentWidth = (host.resources.displayMetrics.widthPixels * 0.75).toInt()
            val content = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(18), dp(20), dp(18))
            }

            content.addView(TextView(host).apply {
                text = if (editItem != null) "Edit Class Schedule" else "Add Class Schedule"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            })

            val titleInput = android.widget.EditText(host).apply {
                hint = "Class Title (e.g. Physics 101)"
                if (editItem != null) setText(editItem.title)
                filters = arrayOf(android.text.InputFilter.LengthFilter(100))
                setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(11), dp(14), dp(11))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }
            content.addView(titleInput)

            fun parseIsPm(time24: String?): Boolean {
                if (time24.isNullOrBlank()) return false
                val parts = time24.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 0
                    return h >= 12
                }
                return false
            }

            var startIsPm = parseIsPm(editItem?.startTime)
            var endIsPm = parseIsPm(editItem?.endTime)

            fun createTimeInputGroup(initTime24: String?): Triple<LinearLayout, android.widget.EditText, android.widget.EditText> {
                val group = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                var hInit = ""
                var mInit = ""
                if (!initTime24.isNullOrBlank()) {
                    val parts = initTime24.split(":")
                    if (parts.size == 2) {
                        val h24 = parts[0].toIntOrNull() ?: 0
                        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
                        hInit = String.format(Locale.US, "%02d", h12)
                        mInit = parts[1]
                    }
                }

                val hourEdit = android.widget.EditText(host).apply {
                    hint = "10"
                    if (hInit.isNotBlank()) setText(hInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val colonLabel = TextView(host).apply {
                    text = ":"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(4), 0, dp(4), 0)
                }

                val minEdit = android.widget.EditText(host).apply {
                    hint = "00"
                    if (mInit.isNotBlank()) setText(mInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                hourEdit.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (s?.length == 2) {
                            minEdit.requestFocus()
                        }
                    }
                })

                group.addView(hourEdit)
                group.addView(colonLabel)
                group.addView(minEdit)
                return Triple(group, hourEdit, minEdit)
            }

            content.addView(TextView(host).apply {
                text = "Start Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val startRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (startGroup, startHEdit, startMEdit) = createTimeInputGroup(editItem?.startTime)
            startRow.addView(startGroup)

            val startAmBtn = TextView(host).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val startPmBtn = TextView(host).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val activeToggleTextColor: Int = ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor)
            startAmBtn.setOnClickListener { startIsPm = false; startAmBtn.setTextColor(activeToggleTextColor); startAmBtn.background = rippleBackground(themeCoordinator.primaryColor); startPmBtn.setTextColor(themeCoordinator.textColor); startPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            startPmBtn.setOnClickListener { startIsPm = true; startPmBtn.setTextColor(activeToggleTextColor); startPmBtn.background = rippleBackground(themeCoordinator.primaryColor); startAmBtn.setTextColor(themeCoordinator.textColor); startAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            startAmBtn.setTextColor(if (!startIsPm) activeToggleTextColor else themeCoordinator.textColor)
            startAmBtn.background = if (!startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            startPmBtn.setTextColor(if (startIsPm) activeToggleTextColor else themeCoordinator.textColor)
            startPmBtn.background = if (startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val startToggleRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            startToggleRow.addView(startAmBtn)
            startToggleRow.addView(startPmBtn)
            startRow.addView(startToggleRow)
            content.addView(startRow)

            content.addView(TextView(host).apply {
                text = "End Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val endRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (endGroup, endHEdit, endMEdit) = createTimeInputGroup(editItem?.endTime)
            endRow.addView(endGroup)

            val endAmBtn = TextView(host).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val endPmBtn = TextView(host).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            endAmBtn.setOnClickListener { endIsPm = false; endAmBtn.setTextColor(activeToggleTextColor); endAmBtn.background = rippleBackground(themeCoordinator.primaryColor); endPmBtn.setTextColor(themeCoordinator.textColor); endPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            endPmBtn.setOnClickListener { endIsPm = true; endPmBtn.setTextColor(activeToggleTextColor); endPmBtn.background = rippleBackground(themeCoordinator.primaryColor); endAmBtn.setTextColor(themeCoordinator.textColor); endAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            endAmBtn.setTextColor(if (!endIsPm) activeToggleTextColor else themeCoordinator.textColor)
            endAmBtn.background = if (!endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            endPmBtn.setTextColor(if (endIsPm) activeToggleTextColor else themeCoordinator.textColor)
            endPmBtn.background = if (endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val endToggleRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            endToggleRow.addView(endAmBtn)
            endToggleRow.addView(endPmBtn)
            endRow.addView(endToggleRow)
            content.addView(endRow)

            content.addView(TextView(host).apply {
                text = "Subject Tag"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })

            val availableSubjects = SubjectTagManager.getAllSubjects(host).toMutableList()
            val initSubId = editItem?.subjectId ?: SubjectTagManager.getSelectedSubject(host).id
            var chosenSubId = initSubId

            val subjectRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }

            val subjectSelectBtn = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val curInit = availableSubjects.find { it.id == chosenSubId } ?: availableSubjects.firstOrNull() ?: SubjectTagManager.DEFAULT_SUBJECTS[0]
            val subjectLabelTv = TextView(host).apply {
                text = curInit.name
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val subjectArrowTv = TextView(host).apply {
                text = " ▾"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            subjectSelectBtn.addView(subjectLabelTv)
            subjectSelectBtn.addView(subjectArrowTv)

            val addQuickSubBtn = TextView(host).apply {
                text = "+ Add"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
            }

            fun updateSubjectLabel() {
                val cur = availableSubjects.find { it.id == chosenSubId } ?: availableSubjects.firstOrNull() ?: SubjectTagManager.DEFAULT_SUBJECTS[0]
                subjectLabelTv.text = cur.name
            }

            fun openSubjectPickerMenu() {
                val dialog = Dialog(host)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                val dialogLayout = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(20f)
                    setPadding(dp(18), dp(18), dp(18), dp(16))
                }
                dialogLayout.addView(TextView(host).apply {
                    text = "Select Subject"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 16f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                    setPadding(0, 0, 0, dp(12))
                })

                val scroll = ScrollView(host).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                    isVerticalScrollBarEnabled = false
                }
                val itemsList = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                }

                for (s in availableSubjects) {
                    val isCur = s.id == chosenSubId
                    val item = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = if (isCur) {
                            themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                        } else {
                            rippleBackground(tintedColor(themeCoordinator.textColor, 15))
                        }
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(6))
                        }
                        isClickable = true
                        setOnClickListener {
                            chosenSubId = s.id
                            updateSubjectLabel()
                            dialog.dismiss()
                        }
                    }
                    val itemSubColor = try { Color.parseColor(s.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                    item.addView(View(host).apply {
                        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(itemSubColor) }
                        layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { setMargins(0, 0, dp(10), 0) }
                    })
                    item.addView(TextView(host).apply {
                        text = s.name
                        setTextColor(if (isCur) Color.WHITE else themeCoordinator.textColor)
                        textSize = 14f
                        typeface = host.themeCoordinator.getAppTypeface(isBold = isCur)
                    })
                    itemsList.addView(item)
                }

                val addRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 12f)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(4), 0, dp(6))
                    }
                    isClickable = true
                    setOnClickListener {
                        dialog.dismiss()
                        showAddCustomSubjectDialog { newSub ->
                            availableSubjects.clear()
                            availableSubjects.addAll(SubjectTagManager.getAllSubjects(host))
                            chosenSubId = newSub.id
                            updateSubjectLabel()
                        }
                    }
                }
                addRow.addView(TextView(host).apply {
                    text = "＋ Add New Subject..."
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 14f
                    typeface = host.themeCoordinator.getAppTypeface(isBold = true)
                })
                itemsList.addView(addRow)
                scroll.addView(itemsList)
                dialogLayout.addView(scroll)

                dialog.setContentView(dialogLayout)
                dialog.window?.apply {
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    setLayout((host.resources.displayMetrics.widthPixels * 0.85f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                dialog.show()
            }

            subjectSelectBtn.setOnClickListener { openSubjectPickerMenu() }
            addQuickSubBtn.setOnClickListener {
                showAddCustomSubjectDialog { newSub ->
                    availableSubjects.clear()
                    availableSubjects.addAll(SubjectTagManager.getAllSubjects(host))
                    chosenSubId = newSub.id
                    updateSubjectLabel()
                }
            }

            subjectRow.addView(subjectSelectBtn)
            subjectRow.addView(addQuickSubBtn)
            content.addView(subjectRow)

            fun convertTo24h(hEdit: android.widget.EditText, mEdit: android.widget.EditText, isPm: Boolean): String {
                val rawH = hEdit.text.toString().trim().toIntOrNull() ?: return ""
                val rawM = mEdit.text.toString().trim().toIntOrNull() ?: 0
                val h12 = rawH.coerceIn(1, 12)
                val m = rawM.coerceIn(0, 59)
                val h24 = when {
                    isPm && h12 < 12 -> h12 + 12
                    !isPm && h12 == 12 -> 0
                    else -> h12
                }
                return String.format(Locale.US, "%02d:%02d", h24, m)
            }

            val btnRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(10), 0, 0)
            }
            btnRow.addView(Button(host).apply {
                text = "Cancel"
                setTextColor(themeCoordinator.textColor)
                background = null
                setOnClickListener {
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            btnRow.addView(Button(host).apply {
                text = if (editItem != null) "Update Class" else "Save Class"
                setTextColor(ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor))
                background = rippleBackground(themeCoordinator.primaryColor)
                setOnClickListener {
                    val titleText = titleInput.text.toString().trim().take(100)
                    val startText = convertTo24h(startHEdit, startMEdit, startIsPm)
                    val endText = convertTo24h(endHEdit, endMEdit, endIsPm)
                    val selectedSubjectId = chosenSubId

                    if (titleText.isBlank() || startText.isBlank() || endText.isBlank()) {
                        AppToast.show(host, "Please fill all fields")
                        return@setOnClickListener
                    }
                    val sM = parseTimeToMinutes(startText)
                    val eM = parseTimeToMinutes(endText)
                    if (sM == null || eM == null || eM <= sM) {
                        AppToast.show(host, "End time must be after start time")
                        return@setOnClickListener
                    }

                    val currentSchedules = loadLectureSchedulesFromJson(host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("lecture_schedules_json", "[]") ?: "[]").toMutableList()
                    if (editItem != null) {
                        val idx = currentSchedules.indexOfFirst { it.id == editItem.id }
                        if (idx != -1) {
                            currentSchedules[idx] = editItem.copy(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId)
                        }
                    } else {
                        currentSchedules.add(LectureScheduleItem(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId))
                    }
                    saveLectureSchedulesToJson(currentSchedules)
                    LectureScheduleScheduler.scheduleAll(host)
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            content.addView(btnRow)

            dialog.setContentView(content)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
            dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        } catch (e: Exception) {
            android.util.Log.e("StudyTimer", "Error in showAddLectureScheduleDialog", e)
        }
    }
}

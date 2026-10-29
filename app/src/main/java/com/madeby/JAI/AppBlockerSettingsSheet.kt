package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, student-friendly configuration sheet for App Blocker.
 * Proportioned strictly to lower screen, with live permission detection,
 * simple language, segment tabs, and zero visual clutter.
 */
object AppBlockerSettingsSheet {

    private enum class FilterTab {
        ALL, BLOCKED, ALLOWED
    }

    fun show(activity: MainActivity, onDismissed: (() -> Unit)? = null) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val theme = activity.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val textColor = theme.textColor

        fun dp(v: Int): Int = activity.dp(v)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                val corner = dp(24).toFloat()
                cornerRadii = floatArrayOf(corner, corner, corner, corner, 0f, 0f, 0f, 0f)
                val bgCol = if (isDark) {
                    if (theme.activeBgMode == "ECLIPSE") 0xF0101726.toInt() else 0xEE0D0F15.toInt()
                } else {
                    0xF8FFFFFF.toInt()
                }
                setColor(bgCol)
                setStroke(dp(1), if (isDark) 0x33FFFFFF.toInt() else 0x22000000.toInt())
            }
            setPadding(dp(20), dp(12), dp(20), dp(18))
        }

        // 1. Drag Handle
        val dragHandle = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(3).toFloat()
                setColor(if (isDark) 0x55FFFFFF.toInt() else 0x33000000.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
        }
        root.addView(dragHandle)

        // 2. Header Row
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val headerTextLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val headerTitle = TextView(activity).apply {
            text = "App Blocker"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val headerSubtitle = TextView(activity).apply {
            text = "Block distracting apps while studying"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(1), 0, 0)
        }
        headerTextLayout.addView(headerTitle)
        headerTextLayout.addView(headerSubtitle)
        headerRow.addView(headerTextLayout)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(10), dp(6), dp(10), dp(6))
            isClickable = true
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // 3. Master Toggle Card
        val isCurrentlyEnabled = AppBlockerManager.isEnabled(activity)

        val masterCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val masterLabelLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val masterTopRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val masterLabel = TextView(activity).apply {
            text = "Block Apps"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        masterTopRow.addView(masterLabel)

        val masterBadge = TextView(activity).apply {
            textSize = 10.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(7), dp(2), dp(7), dp(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(8)
            }
        }
        masterTopRow.addView(masterBadge)
        masterLabelLayout.addView(masterTopRow)

        val masterDesc = TextView(activity).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(2), 0, 0)
        }
        masterLabelLayout.addView(masterDesc)
        masterCard.addView(masterLabelLayout)

        val masterSwitch = SwitchMaterial(activity).apply {
            isChecked = isCurrentlyEnabled
        }
        masterCard.addView(masterSwitch)
        root.addView(masterCard)

        fun updateMasterCardVisuals(enabled: Boolean) {
            masterCard.background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                if (enabled) {
                    setColor(if (isDark) 0x1A22C55E.toInt() else 0x1210B981.toInt())
                    setStroke(dp(1), if (isDark) 0x5522C55E.toInt() else 0x4410B981.toInt())
                } else {
                    setColor(if (isDark) 0x14FFFFFF.toInt() else 0xFFF1F5F9.toInt())
                    setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0xFFE2E8F0.toInt())
                }
            }
            if (enabled) {
                masterBadge.text = "Active"
                masterBadge.setTextColor(0xFF10B981.toInt())
                masterBadge.background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(0x2410B981.toInt())
                }
                masterDesc.text = "Active only while study timer is running"
            } else {
                masterBadge.text = "Off"
                masterBadge.setTextColor(if (isDark) 0x77FFFFFF.toInt() else 0xFF94A3B8.toInt())
                masterBadge.background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(if (isDark) 0x18FFFFFF.toInt() else 0x12000000.toInt())
                }
                masterDesc.text = "Turn on to block selected apps during study"
            }
        }
        updateMasterCardVisuals(isCurrentlyEnabled)

        // 4. Actions & Search Container
        val actionsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isCurrentlyEnabled) View.VISIBLE else View.GONE
        }

        // 5. Search Box
        val searchBox = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0x14FFFFFF.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), if (isDark) 0x1FFFFFFF.toInt() else 0xFFE2E8F0.toInt())
            }
            setPadding(dp(12), dp(2), dp(10), dp(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val searchPrompt = TextView(activity).apply {
            text = "⌕"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDark) 0x77FFFFFF.toInt() else 0xFF94A3B8.toInt())
            setPadding(0, 0, dp(8), 0)
        }
        searchBox.addView(searchPrompt)

        val searchInput = EditText(activity).apply {
            hint = "Search apps..."
            textSize = 13.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(textColor)
            setHintTextColor(if (isDark) 0x55FFFFFF.toInt() else 0xFF94A3B8.toInt())
            background = null
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchBox.addView(searchInput)

        val clearSearchBtn = TextView(activity).apply {
            text = "✕"
            textSize = 12f
            setTextColor(if (isDark) 0x77FFFFFF.toInt() else 0xFF94A3B8.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            visibility = View.GONE
            isClickable = true
            setOnClickListener { searchInput.setText("") }
        }
        searchBox.addView(clearSearchBtn)
        actionsContainer.addView(searchBox)

        // 6. Filter Segment Tabs (All / Blocked / Allowed)
        var currentFilter = FilterTab.ALL

        val filterTabsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (isDark) 0x10FFFFFF.toInt() else 0xFFF1F5F9.toInt())
            }
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        fun createSegmentTab(label: String): TextView {
            return TextView(activity).apply {
                text = label
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(6), dp(8), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        }

        val tabAll = createSegmentTab("All")
        val tabBlocked = createSegmentTab("Blocked")
        val tabAllowed = createSegmentTab("Allowed")

        filterTabsRow.addView(tabAll)
        filterTabsRow.addView(tabBlocked)
        filterTabsRow.addView(tabAllowed)
        actionsContainer.addView(filterTabsRow)

        // 7. Counter & Quick Actions Row
        val statsAndActionsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(6))
        }

        val statsCounterText = TextView(activity).apply {
            text = "Loading apps..."
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statsAndActionsRow.addView(statsCounterText)

        val blockAllLink = TextView(activity).apply {
            text = "Block all"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            isClickable = true
        }
        val actionDivider = TextView(activity).apply {
            text = "•"
            textSize = 11f
            setTextColor(if (isDark) 0x44FFFFFF.toInt() else 0xFFCBD5E1.toInt())
            setPadding(dp(2), 0, dp(2), 0)
        }
        val allowAllLink = TextView(activity).apply {
            text = "Unblock all"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(dp(6), dp(2), dp(6), dp(2))
            isClickable = true
        }
        statsAndActionsRow.addView(blockAllLink)
        statsAndActionsRow.addView(actionDivider)
        statsAndActionsRow.addView(allowAllLink)
        actionsContainer.addView(statsAndActionsRow)

        // 8. App List Scroll Container (Proportioned so sheet NEVER goes too up)
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val maxScrollHeight = (screenHeight * 0.40f).toInt().coerceIn(dp(200), dp(320))

        val loadingBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(22), 0, dp(22))
            }
        }
        actionsContainer.addView(loadingBar)

        val emptyStateText = TextView(activity).apply {
            text = "No apps found"
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(if (isDark) 0x66FFFFFF.toInt() else 0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(24))
            visibility = View.GONE
        }
        actionsContainer.addView(emptyStateText)

        val scrollView = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
            visibility = View.GONE
        }

        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listContainer)
        actionsContainer.addView(scrollView)

        root.addView(actionsContainer)

        // 9. Bottom Done Button
        val doneBtn = TextView(activity).apply {
            text = "Done"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                topMargin = dp(12)
            }
            isClickable = true
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(doneBtn)

        // State holder
        var allApps = listOf<AppBlockerManager.AppItem>()
        data class RowHolder(
            val app: AppBlockerManager.AppItem,
            val rowView: View,
            val switchView: SwitchMaterial
        )
        val rowHolders = mutableListOf<RowHolder>()

        fun updateTabsAndCounters() {
            val blockedSet = AppBlockerManager.getBlockedPackages(activity)
            val blockedCount = allApps.count { blockedSet.contains(it.packageName) }
            val allowedCount = allApps.size - blockedCount

            tabAll.text = "All (${allApps.size})"
            tabBlocked.text = "Blocked ($blockedCount)"
            tabAllowed.text = "Allowed ($allowedCount)"

            statsCounterText.text = "$blockedCount blocked · $allowedCount allowed"

            val activeBg = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(primary)
            }
            val inactiveBg = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0)
            }

            tabAll.background = if (currentFilter == FilterTab.ALL) activeBg else inactiveBg
            tabAll.setTextColor(if (currentFilter == FilterTab.ALL) Color.WHITE else (if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt()))

            tabBlocked.background = if (currentFilter == FilterTab.BLOCKED) activeBg else inactiveBg
            tabBlocked.setTextColor(if (currentFilter == FilterTab.BLOCKED) Color.WHITE else (if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt()))

            tabAllowed.background = if (currentFilter == FilterTab.ALLOWED) activeBg else inactiveBg
            tabAllowed.setTextColor(if (currentFilter == FilterTab.ALLOWED) Color.WHITE else (if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt()))
        }

        fun filterApps() {
            val q = searchInput.text?.toString()?.trim()?.lowercase() ?: ""
            val blockedSet = AppBlockerManager.getBlockedPackages(activity)
            var visibleCount = 0

            for (holder in rowHolders) {
                val matchesQuery = q.isEmpty() ||
                        holder.app.label.lowercase().contains(q) ||
                        holder.app.packageName.lowercase().contains(q)

                val isBlocked = blockedSet.contains(holder.app.packageName)
                val matchesTab = when (currentFilter) {
                    FilterTab.ALL -> true
                    FilterTab.BLOCKED -> isBlocked
                    FilterTab.ALLOWED -> !isBlocked
                }

                val visible = matchesQuery && matchesTab
                holder.rowView.visibility = if (visible) View.VISIBLE else View.GONE
                if (visible) visibleCount++
            }

            emptyStateText.visibility = if (visibleCount == 0 && allApps.isNotEmpty()) View.VISIBLE else View.GONE
            scrollView.visibility = if (visibleCount > 0) View.VISIBLE else View.GONE
        }

        tabAll.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            currentFilter = FilterTab.ALL
            updateTabsAndCounters()
            filterApps()
        }
        tabBlocked.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            currentFilter = FilterTab.BLOCKED
            updateTabsAndCounters()
            filterApps()
        }
        tabAllowed.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            currentFilter = FilterTab.ALLOWED
            updateTabsAndCounters()
            filterApps()
        }

        // Asynchronous app list loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loaded = AppBlockerManager.loadInstalledApps(activity)
                withContext(Dispatchers.Main) {
                    if (activity.isFinishing || activity.isDestroyed) return@withContext
                    allApps = loaded
                    loadingBar.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE

                    val currentBlocked = AppBlockerManager.getBlockedPackages(activity)

                    listContainer.removeAllViews()
                    rowHolders.clear()

                    for (app in allApps) {
                        val isBlocked = currentBlocked.contains(app.packageName)

                        val row = LinearLayout(activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                RippleDrawable(
                                    ColorStateList.valueOf(if (isDark) 0x1AFFFFFF.toInt() else 0x14000000.toInt()),
                                    GradientDrawable().apply {
                                        cornerRadius = dp(10).toFloat()
                                        setColor(0)
                                    },
                                    null
                                )
                            } else {
                                GradientDrawable().apply {
                                    cornerRadius = dp(10).toFloat()
                                    setColor(0)
                                }
                            }
                            setPadding(dp(6), dp(7), dp(6), dp(7))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }

                        // App Icon
                        val iconView = ImageView(activity).apply {
                            setImageDrawable(app.icon)
                            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                                marginEnd = dp(12)
                            }
                        }
                        row.addView(iconView)

                        // App Title (Clean, no technical package clutter)
                        val titleView = TextView(activity).apply {
                            text = app.label
                            textSize = 14f
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            setTextColor(textColor)
                            setSingleLine(true)
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        row.addView(titleView)

                        // Switch
                        val appSwitch = SwitchMaterial(activity).apply {
                            isChecked = isBlocked
                            isClickable = false
                        }
                        row.addView(appSwitch)

                        val holder = RowHolder(app, row, appSwitch)
                        rowHolders.add(holder)

                        row.setOnClickListener {
                            HapticFeedbackHelper.performMicroHaptic(activity, it)
                            val nextState = !appSwitch.isChecked
                            appSwitch.isChecked = nextState
                            AppBlockerManager.setPackageBlocked(activity, app.packageName, nextState)
                            updateTabsAndCounters()
                        }

                        listContainer.addView(row)
                    }

                    updateTabsAndCounters()
                    filterApps()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        loadingBar.visibility = View.GONE
                        AppToast.show(activity, "Could not load apps: ${e.message}")
                    }
                }
            }
        }

        // Quick Actions Listeners
        blockAllLink.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            val allPackages = allApps.map { it.packageName }.toSet()
            AppBlockerManager.setBlockedPackages(activity, allPackages)
            for (h in rowHolders) {
                h.switchView.isChecked = true
            }
            updateTabsAndCounters()
            filterApps()
        }

        allowAllLink.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            AppBlockerManager.setBlockedPackages(activity, emptySet())
            for (h in rowHolders) {
                h.switchView.isChecked = false
            }
            updateTabsAndCounters()
            filterApps()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                clearSearchBtn.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                filterApps()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Master Switch Listener with Live Permission Checking
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            HapticFeedbackHelper.performMicroHaptic(activity)
            if (isChecked) {
                if (AppBlockerManager.hasAllPermissions(activity)) {
                    AppBlockerManager.setEnabled(activity, true)
                    updateMasterCardVisuals(true)
                    actionsContainer.visibility = View.VISIBLE
                } else {
                    showPermissionsDialog(activity) { granted ->
                        if (granted) {
                            masterSwitch.isChecked = true
                            AppBlockerManager.setEnabled(activity, true)
                            updateMasterCardVisuals(true)
                            actionsContainer.visibility = View.VISIBLE
                        } else {
                            masterSwitch.isChecked = false
                            AppBlockerManager.setEnabled(activity, false)
                            updateMasterCardVisuals(false)
                            actionsContainer.visibility = View.GONE
                        }
                    }
                }
            } else {
                AppBlockerManager.setEnabled(activity, false)
                updateMasterCardVisuals(false)
                actionsContainer.visibility = View.GONE
            }
        }

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setWindowAnimations(android.R.style.Animation_InputMethod)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        dialog.setOnDismissListener {
            onDismissed?.invoke()
        }

        dialog.show()
    }

    /**
     * Student-friendly permission dialog with live auto-sync.
     * Simple language, no jargon, and auto-dismisses once both permissions are granted.
     */
    private fun showPermissionsDialog(activity: MainActivity, onResult: (Boolean) -> Unit) {
        val theme = activity.themeCoordinator
        val (dialog, root) = DialogFactory.createBaseDialog(activity, theme)
        val isDark = theme.isDarkMode()
        val textColor = theme.textColor
        val primary = theme.primaryColor

        fun dp(v: Int): Int = activity.dp(v)

        var hasCompleted = false

        // Title
        root.addView(TextView(activity).apply {
            text = "App Permissions"
            setTextColor(textColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Friendly subtitle
        root.addView(TextView(activity).apply {
            text = "StudyTimer needs two permissions to block distracting apps while your timer is running."
            setTextColor(textColor)
            alpha = 0.75f
            textSize = 12.5f
            setPadding(0, dp(6), 0, dp(14))
        })

        // Permission 1: Usage Access Card
        val usageCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }
        val usageTextLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val usageTitle = TextView(activity).apply {
            text = "1. Usage access"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val usageSubtitle = TextView(activity).apply {
            text = "To know when a blocked app opens"
            textSize = 11.5f
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(1), 0, 0)
        }
        usageTextLayout.addView(usageTitle)
        usageTextLayout.addView(usageSubtitle)
        usageCard.addView(usageTextLayout)

        val usageBtn = TextView(activity).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        usageCard.addView(usageBtn)
        root.addView(usageCard)

        // Permission 2: Display Over Other Apps Card
        val overlayCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }
        val overlayTextLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val overlayTitle = TextView(activity).apply {
            text = "2. Display over apps"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val overlaySubtitle = TextView(activity).apply {
            text = "To show the reminder over blocked apps"
            textSize = 11.5f
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(1), 0, 0)
        }
        overlayTextLayout.addView(overlayTitle)
        overlayTextLayout.addView(overlaySubtitle)
        overlayCard.addView(overlayTextLayout)

        val overlayBtn = TextView(activity).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        overlayCard.addView(overlayBtn)
        root.addView(overlayCard)

        // Bottom Action Row
        val btnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val cancelBtn = TextView(activity).apply {
            text = "Cancel"
            setTextColor(textColor)
            alpha = 0.7f
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isClickable = true
            setOnClickListener {
                hasCompleted = true
                dialog.dismiss()
                onResult(false)
            }
        }
        btnRow.addView(cancelBtn)

        val spacer = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        btnRow.addView(spacer)

        val proceedBtn = TextView(activity).apply {
            text = "Done"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(primary)
            }
            setPadding(dp(18), dp(8), dp(18), dp(8))
            isClickable = true
            setOnClickListener {
                val allGranted = AppBlockerManager.hasAllPermissions(activity)
                if (allGranted) {
                    hasCompleted = true
                    dialog.dismiss()
                    onResult(true)
                } else {
                    AppToast.show(activity, "Please grant both permissions to proceed")
                }
            }
        }
        btnRow.addView(proceedBtn)
        root.addView(btnRow)

        // Live updater function
        fun updatePermissionsStatus() {
            if (hasCompleted) return
            val hasUsage = AppBlockerManager.hasUsageStatsPermission(activity)
            val hasOverlay = AppBlockerManager.hasOverlayPermission(activity)

            // 1. Usage UI
            if (hasUsage) {
                usageBtn.text = "✓ Granted"
                usageBtn.setTextColor(0xFF10B981.toInt())
                usageBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(0x2410B981.toInt())
                }
                usageBtn.setOnClickListener(null)
                usageCard.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(if (isDark) 0x1410B981.toInt() else 0x0D10B981.toInt())
                    setStroke(dp(1), 0x3310B981.toInt())
                }
            } else {
                usageBtn.text = "Grant"
                usageBtn.setTextColor(primary)
                usageBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (isDark) 0x1FFFFFFF.toInt() else 0x14000000.toInt())
                }
                usageBtn.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                        activity.startActivity(intent)
                    } catch (_: Exception) {
                        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }
                usageCard.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(if (isDark) 0x14FFFFFF.toInt() else 0xFFF1F5F9.toInt())
                    setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0xFFE2E8F0.toInt())
                }
            }

            // 2. Overlay UI
            if (hasOverlay) {
                overlayBtn.text = "✓ Granted"
                overlayBtn.setTextColor(0xFF10B981.toInt())
                overlayBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(0x2410B981.toInt())
                }
                overlayBtn.setOnClickListener(null)
                overlayCard.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(if (isDark) 0x1410B981.toInt() else 0x0D10B981.toInt())
                    setStroke(dp(1), 0x3310B981.toInt())
                }
            } else {
                overlayBtn.text = "Grant"
                overlayBtn.setTextColor(primary)
                overlayBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (isDark) 0x1FFFFFFF.toInt() else 0x14000000.toInt())
                }
                overlayBtn.setOnClickListener {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    } catch (_: Exception) {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    }
                }
                overlayCard.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(if (isDark) 0x14FFFFFF.toInt() else 0xFFF1F5F9.toInt())
                    setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0xFFE2E8F0.toInt())
                }
            }

            // 3. Auto-Proceed when both are granted
            if (hasUsage && hasOverlay) {
                hasCompleted = true
                HapticFeedbackHelper.performMicroHaptic(activity)
                proceedBtn.text = "Granted ✓"
                proceedBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(0xFF10B981.toInt())
                }
                root.postDelayed({
                    if (dialog.isShowing) {
                        dialog.dismiss()
                        onResult(true)
                    }
                }, 350L)
            }
        }

        // Initial setup
        updatePermissionsStatus()

        // 1. Lifecycle Observer for ON_RESUME
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionsStatus()
            }
        }
        activity.lifecycle.addObserver(lifecycleObserver)

        // 2. Window Focus Listener
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                updatePermissionsStatus()
            }
        }
        dialog.window?.decorView?.viewTreeObserver?.addOnWindowFocusChangeListener(focusListener)

        // 3. Safe 350ms Polling fallback while dialog is visible
        val pollHandler = Handler(Looper.getMainLooper())
        val pollRunnable = object : Runnable {
            override fun run() {
                if (dialog.isShowing && !hasCompleted) {
                    updatePermissionsStatus()
                    pollHandler.postDelayed(this, 350L)
                }
            }
        }
        pollHandler.postDelayed(pollRunnable, 350L)

        dialog.setOnDismissListener {
            pollHandler.removeCallbacks(pollRunnable)
            activity.lifecycle.removeObserver(lifecycleObserver)
            try {
                dialog.window?.decorView?.viewTreeObserver?.removeOnWindowFocusChangeListener(focusListener)
            } catch (_: Exception) {}
        }

        dialog.show()
    }
}

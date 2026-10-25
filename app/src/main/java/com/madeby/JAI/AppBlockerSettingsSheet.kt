package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, modern configuration sheet for App Blocker.
 * Provides instant search, clean "Select all" / "Deselect all" actions,
 * and transparent on-demand permission onboarding.
 */
object AppBlockerSettingsSheet {

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
                    0xF5FFFFFF.toInt()
                }
                setColor(bgCol)
                setStroke(dp(1), if (isDark) 0x38FFFFFF.toInt() else 0x40CBD5E1.toInt())
            }
            setPadding(dp(20), dp(14), dp(20), dp(20))
        }

        // 1. Drag Handle
        val dragHandle = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(3).toFloat()
                setColor(if (isDark) 0xFF475569.toInt() else 0xFFCBD5E1.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14)
            }
        }
        root.addView(dragHandle)

        // 2. Header Row
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
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
            text = "Block distracting apps during study sessions"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(2), 0, 0)
        }
        headerTextLayout.addView(headerTitle)
        headerTextLayout.addView(headerSubtitle)
        headerRow.addView(headerTextLayout)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // 3. Master Toggle Row
        val masterCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0x1AFFFFFF.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0xFFE2E8F0.toInt())
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val masterLabelLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val masterLabel = TextView(activity).apply {
            text = "Enable Focus Shield"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val masterDesc = TextView(activity).apply {
            text = "Active only while study timer is running"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(0, dp(2), 0, 0)
        }
        masterLabelLayout.addView(masterLabel)
        masterLabelLayout.addView(masterDesc)
        masterCard.addView(masterLabelLayout)

        val isCurrentlyEnabled = AppBlockerManager.isEnabled(activity)
        val masterSwitch = SwitchMaterial(activity).apply {
            isChecked = isCurrentlyEnabled
        }
        masterCard.addView(masterSwitch)
        root.addView(masterCard)

        // 4. Quick Actions & Status Bar (Clean Text, No Bulky Pills)
        val actionsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isCurrentlyEnabled) View.VISIBLE else View.GONE
        }

        val statsAndActionsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val statsCounterText = TextView(activity).apply {
            text = "Loading apps..."
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statsAndActionsRow.addView(statsCounterText)

        val selectAllLink = TextView(activity).apply {
            text = "Select all"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val actionDivider = TextView(activity).apply {
            text = "•"
            textSize = 11f
            setTextColor(if (isDark) 0x55FFFFFF.toInt() else 0xFFCBD5E1.toInt())
        }
        val deselectAllLink = TextView(activity).apply {
            text = "Deselect all"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0xFF64748B.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        statsAndActionsRow.addView(selectAllLink)
        statsAndActionsRow.addView(actionDivider)
        statsAndActionsRow.addView(deselectAllLink)
        actionsContainer.addView(statsAndActionsRow)

        // 5. Search Field
        val searchBox = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0x14FFFFFF.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), if (isDark) 0x1FFFFFFF.toInt() else 0xFFCBD5E1.toInt())
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val searchIcon = TextView(activity).apply {
            text = "🔍"
            textSize = 12f
            setPadding(0, 0, dp(8), 0)
        }
        searchBox.addView(searchIcon)

        val searchInput = EditText(activity).apply {
            hint = "Search installed apps..."
            textSize = 14f
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
            textSize = 13f
            setTextColor(if (isDark) 0x77FFFFFF.toInt() else 0xFF94A3B8.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            visibility = View.GONE
            setOnClickListener { searchInput.setText("") }
        }
        searchBox.addView(clearSearchBtn)
        actionsContainer.addView(searchBox)

        // 6. App List Scroll Container
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val maxScrollHeight = (screenHeight * 0.48f).toInt().coerceIn(dp(220), dp(400))

        val loadingBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(32), 0, dp(32))
            }
        }
        actionsContainer.addView(loadingBar)

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

        // 7. Bottom Done Button
        val doneBtn = TextView(activity).apply {
            text = "Done"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(14)
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(doneBtn)

        // State holder for installed apps
        var allApps = listOf<AppBlockerManager.AppItem>()
        val rowViews = mutableListOf<Triple<AppBlockerManager.AppItem, View, SwitchMaterial>>()

        fun updateCounter() {
            val blockedSet = AppBlockerManager.getBlockedPackages(activity)
            val blockedCount = allApps.count { blockedSet.contains(it.packageName) }
            val allowedCount = allApps.size - blockedCount
            statsCounterText.text = "$blockedCount blocked • $allowedCount allowed"
        }

        fun filterApps(query: String) {
            val q = query.trim().lowercase()
            for ((item, row, _) in rowViews) {
                val matches = q.isEmpty() || item.label.lowercase().contains(q) || item.packageName.lowercase().contains(q)
                row.visibility = if (matches) View.VISIBLE else View.GONE
            }
        }

        // Asynchronous app list loading
        CoroutineScope(Dispatchers.IO).launch {
            val loaded = AppBlockerManager.loadInstalledApps(activity)
            withContext(Dispatchers.Main) {
                if (activity.isFinishing || activity.isDestroyed) return@withContext
                allApps = loaded
                loadingBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE

                val currentBlocked = AppBlockerManager.getBlockedPackages(activity)

                listContainer.removeAllViews()
                rowViews.clear()

                for (app in allApps) {
                    val row = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            cornerRadius = dp(10).toFloat()
                            setColor(0)
                        }
                        setPadding(dp(4), dp(8), dp(4), dp(8))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // App Icon
                    val iconView = ImageView(activity).apply {
                        setImageDrawable(app.icon)
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                            marginEnd = dp(12)
                        }
                    }
                    row.addView(iconView)

                    // App Title & Subtitle
                    val titleLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val titleView = TextView(activity).apply {
                        text = app.label
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setTextColor(textColor)
                        setSingleLine(true)
                    }
                    val pkgView = TextView(activity).apply {
                        text = app.packageName
                        textSize = 11f
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        setTextColor(if (isDark) 0x66FFFFFF.toInt() else 0xFF94A3B8.toInt())
                        setSingleLine(true)
                    }
                    titleLayout.addView(titleView)
                    titleLayout.addView(pkgView)
                    row.addView(titleLayout)

                    // Switch
                    val appSwitch = SwitchMaterial(activity).apply {
                        isChecked = currentBlocked.contains(app.packageName)
                        isClickable = false // Row handles clicks for better touch area
                    }
                    row.addView(appSwitch)

                    row.setOnClickListener {
                        HapticFeedbackHelper.performMicroHaptic(activity, it)
                        val nextState = !appSwitch.isChecked
                        appSwitch.isChecked = nextState
                        AppBlockerManager.setPackageBlocked(activity, app.packageName, nextState)
                        updateCounter()
                    }

                    rowViews.add(Triple(app, row, appSwitch))
                    listContainer.addView(row)
                }

                updateCounter()
            }
        }

        // Quick Actions Listeners
        selectAllLink.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            val allPackages = allApps.map { it.packageName }.toSet()
            AppBlockerManager.setBlockedPackages(activity, allPackages)
            for ((_, _, sw) in rowViews) {
                sw.isChecked = true
            }
            updateCounter()
        }

        deselectAllLink.setOnClickListener {
            HapticFeedbackHelper.performMicroHaptic(activity, it)
            AppBlockerManager.setBlockedPackages(activity, emptySet())
            for ((_, _, sw) in rowViews) {
                sw.isChecked = false
            }
            updateCounter()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                clearSearchBtn.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                filterApps(q)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Master Switch Listener with Permission Checking
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            HapticFeedbackHelper.performMicroHaptic(activity)
            if (isChecked) {
                if (AppBlockerManager.hasAllPermissions(activity)) {
                    AppBlockerManager.setEnabled(activity, true)
                    actionsContainer.visibility = View.VISIBLE
                } else {
                    // Show clean permissions onboarding dialog
                    showPermissionsDialog(activity) { granted ->
                        if (granted) {
                            masterSwitch.isChecked = true
                            AppBlockerManager.setEnabled(activity, true)
                            actionsContainer.visibility = View.VISIBLE
                        } else {
                            masterSwitch.isChecked = false
                            AppBlockerManager.setEnabled(activity, false)
                            actionsContainer.visibility = View.GONE
                        }
                    }
                }
            } else {
                AppBlockerManager.setEnabled(activity, false)
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
     * Clean, transparent permission explanation dialog.
     * Guides the user directly to the two special permission settings.
     */
    private fun showPermissionsDialog(activity: MainActivity, onResult: (Boolean) -> Unit) {
        val theme = activity.themeCoordinator
        val (dialog, root) = DialogFactory.createBaseDialog(activity, theme)
        val isDark = theme.isDarkMode()
        val textColor = theme.textColor
        val primary = theme.primaryColor

        fun dp(v: Int): Int = activity.dp(v)

        // Title
        root.addView(TextView(activity).apply {
            text = "Permissions for Focus Shield"
            setTextColor(textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Explanation Message
        root.addView(TextView(activity).apply {
            text = "To keep you focused during study sessions, Android requires two special permissions. No personal data is tracked or shared."
            setTextColor(textColor)
            alpha = 0.8f
            textSize = 13f
            setPadding(0, dp(8), 0, dp(16))
        })

        // Permission 1: Usage Access
        val usageCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0x1AFFFFFF.toInt() else 0xFFF1F5F9.toInt())
            }
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
            text = "1. Usage Access"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val usageSubtitle = TextView(activity).apply {
            text = "Detects when a blocked app is opened"
            textSize = 12f
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
        }
        usageTextLayout.addView(usageTitle)
        usageTextLayout.addView(usageSubtitle)
        usageCard.addView(usageTextLayout)

        val hasUsage = AppBlockerManager.hasUsageStatsPermission(activity)
        val usageBtn = TextView(activity).apply {
            text = if (hasUsage) "✓ Granted" else "Grant"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (hasUsage) 0xFF10B981.toInt() else primary)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            if (!hasUsage) {
                setOnClickListener {
                    activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        }
        usageCard.addView(usageBtn)
        root.addView(usageCard)

        // Permission 2: Display Over Other Apps
        val overlayCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0x1AFFFFFF.toInt() else 0xFFF1F5F9.toInt())
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        }
        val overlayTextLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val overlayTitle = TextView(activity).apply {
            text = "2. Display Over Other Apps"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        }
        val overlaySubtitle = TextView(activity).apply {
            text = "Displays the focus reminder over locked apps"
            textSize = 12f
            setTextColor(if (isDark) 0x8AFFFFFF.toInt() else 0xFF64748B.toInt())
        }
        overlayTextLayout.addView(overlayTitle)
        overlayTextLayout.addView(overlaySubtitle)
        overlayCard.addView(overlayTextLayout)

        val hasOverlay = AppBlockerManager.hasOverlayPermission(activity)
        val overlayBtn = TextView(activity).apply {
            text = if (hasOverlay) "✓ Granted" else "Grant"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (hasOverlay) 0xFF10B981.toInt() else primary)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            if (!hasOverlay) {
                setOnClickListener {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                }
            }
        }
        overlayCard.addView(overlayBtn)
        root.addView(overlayCard)

        // Action Buttons Row
        val btnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = TextView(activity).apply {
            text = "Cancel"
            setTextColor(textColor)
            alpha = 0.7f
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                dialog.dismiss()
                onResult(false)
            }
        }
        btnRow.addView(cancelBtn)

        val checkAndProceedBtn = TextView(activity).apply {
            text = "I've Granted Permissions"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(primary)
            }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                val allGranted = AppBlockerManager.hasAllPermissions(activity)
                if (allGranted) {
                    dialog.dismiss()
                    onResult(true)
                } else {
                    AppToast.show(activity, "Please grant both permissions to enable App Blocker")
                }
            }
        }
        btnRow.addView(checkAndProceedBtn)
        root.addView(btnRow)

        dialog.show()
    }
}

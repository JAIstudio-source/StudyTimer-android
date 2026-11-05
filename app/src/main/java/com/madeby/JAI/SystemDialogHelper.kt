package com.madeby.JAI

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.PackageInfoCompat

/**
 * Handles system-level dialogs: custom themed prompts, update notifications,
 * and battery optimization rationale.
 */
class SystemDialogHelper(private val activity: MainActivity) {

    private val themeCoordinator get() = activity.themeCoordinator
    private val dp = { v: Int -> activity.dp(v) }
    private fun tintedColor(color: Int, alpha: Int): Int = activity.tintedColor(color, alpha)
    private var updateDialogRef: Dialog? = null
    private var broadcastDialogRef: Dialog? = null
    private var batteryOptDialogRef: Dialog? = null

    fun showCustomDialog(
        badge: String? = null,
        title: String,
        message: String,
        positiveText: String,
        onPositive: () -> Unit,
        negativeText: String? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        if (!badge.isNullOrEmpty()) {
            content.addView(TextView(activity).apply {
                text = badge.uppercase()
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
        }
        content.addView(TextView(activity).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, if (badge.isNullOrEmpty()) 0 else dp(6), 0, 0)
        })
        content.addView(TextView(activity).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(20), 0, 0)
        }
        if (!negativeText.isNullOrEmpty()) {
            buttonRow.addView(TextView(activity).apply {
                text = negativeText
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener {
                    dialog.dismiss()
                    onNegative?.invoke()
                }
            })
        }
        buttonRow.addView(TextView(activity).apply {
            text = positiveText
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                dialog.dismiss()
                onPositive.invoke()
            }
        })
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    fun maybePromptBatteryOptimization() {
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(activity.packageName)) return
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val opens = prefs.safeInt("battery_opt_opens", 0) + 1
        prefs.edit().putInt("battery_opt_opens", opens).apply()
        if (opens % 3 != 0) return
        showBatteryOptimizationDialog()
    }

    fun showBatteryOptimizationDialog() {
        if (batteryOptDialogRef?.isShowing == true) return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.btn_run_in_background)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.battery_title)
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.battery_message)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.55f
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })

        val detectedBrand = OemOptimizationHelper.detectBrand()
        if (detectedBrand != OemOptimizationHelper.OemBrand.OTHER) {
            val guideLink = TextView(activity).apply {
                text = "View setup guide for ${detectedBrand.brandName} ›"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(12), 0, 0)
                setOnClickListener {
                    dialog.dismiss()
                    OemGuideBottomSheet.show(activity, detectedBrand)
                }
            }
            content.addView(guideLink)
        }
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }
        val notNowBtn = TextView(activity).apply {
            text = activity.getString(R.string.btn_not_now)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
            setOnClickListener { dialog.dismiss() }
        }
        val fixBtn = TextView(activity).apply {
            text = activity.getString(R.string.btn_allow)
            gravity = Gravity.CENTER
            setTextColor(ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor))
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(themeCoordinator.primaryColor)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {
                        AppToast.show(activity, "Battery settings unavailable")
                    }
                }
            }
        }
        buttonRow.addView(notNowBtn)
        buttonRow.addView(fixBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        batteryOptDialogRef = dialog
        dialog.setOnDismissListener { batteryOptDialogRef = null }
        dialog.show()
    }

    fun openUpdateUrl(url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            AppToast.show(activity, activity.getString(R.string.toast_update_page_failed))
        }
    }

    fun currentVersionCodeLong(): Long =
        runCatching { PackageInfoCompat.getLongVersionCode(activity.packageManager.getPackageInfo(activity.packageName, 0)) }.getOrDefault(0L)

    fun currentVersionName(): String =
        runCatching { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName }.getOrNull() ?: "1.0.0"

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    fun checkForUpdates(manual: Boolean) {
        if (!AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
            if (manual) {
                AppToast.show(activity, activity.getString(R.string.toast_up_to_date, currentVersionName()))
            }
            return
        }
        if (!isNetworkAvailable()) {
            if (manual) {
                AppToast.show(activity, activity.getString(R.string.toast_update_check_failed))
            }
            return
        }
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (manual) {
            AppToast.show(activity, activity.getString(R.string.toast_checking_updates))
        }
        UpdateChecker.check(activity) { info ->
            if (activity.isDestroyed || activity.isFinishing) return@check
            if (info == null) {
                if (manual) {
                    AppToast.show(activity, activity.getString(R.string.toast_update_check_failed))
                }
                return@check
            }

            val currentCode = currentVersionCodeLong()

            // 1. Check & display broadcast notices targeted at current version
            val broadcast = info.broadcast
            if (broadcast != null && currentCode >= broadcast.minVersionCode && currentCode <= broadcast.maxVersionCode) {
                val lastSeenId = prefs.getString("last_seen_broadcast_id", null)
                val isNewBroadcast = lastSeenId != broadcast.id
                if (isNewBroadcast || !broadcast.canDismiss) {
                    showBroadcastDialog(broadcast)
                }
            }

            // 2. Check for hard-blocked or regular updates
            val isHardBlocked = info.isHardBlocked(currentCode)
            val isNewer = info.versionCode > currentCode

            if (isHardBlocked) {
                showUpdateAvailableDialog(info, isForce = true)
                return@check
            }

            if (isNewer) {
                if (!manual && prefs.safeInt("update_dismissed_version", -1) == info.versionCode) {
                    return@check
                }
                showUpdateAvailableDialog(info, isForce = false)
                return@check
            }

            if (manual) {
                AppToast.show(activity, activity.getString(R.string.toast_up_to_date, currentVersionName()))
            }
        }
    }

    fun openBatterySettings() {
        try {
            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            try {
                activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                AppToast.show(activity, "Battery settings unavailable")
            }
        }
    }

    fun openNotificationSettings() {
        try {
            activity.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            )
        } catch (_: Exception) {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${activity.packageName}")))
        }
    }

    fun dismissDialogs() {
        try {
            if (updateDialogRef?.isShowing == true) updateDialogRef?.dismiss()
            if (broadcastDialogRef?.isShowing == true) broadcastDialogRef?.dismiss()
            if (batteryOptDialogRef?.isShowing == true) batteryOptDialogRef?.dismiss()
        } catch (_: Exception) {}
        updateDialogRef = null
        broadcastDialogRef = null
        batteryOptDialogRef = null
    }

    fun showBroadcastDialog(broadcast: BroadcastMessage) {
        if (activity.isDestroyed || activity.isFinishing) return
        if (broadcastDialogRef?.isShowing == true) return
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val accentColor = when (broadcast.type.lowercase()) {
            "error", "outage" -> 0xFFEF4444.toInt()
            "warning" -> 0xFFF59E0B.toInt()
            "maintenance" -> 0xFF8B5CF6.toInt()
            else -> themeCoordinator.primaryColor
        }

        val badgeText = when (broadcast.type.lowercase()) {
            "error", "outage" -> "⚠️ SYSTEM ALERT"
            "warning" -> "⚡ IMPORTANT NOTICE"
            "maintenance" -> "🛠️ SCHEDULED MAINTENANCE"
            else -> "📢 ANNOUNCEMENT"
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(activity).apply {
            text = badgeText
            setTextColor(accentColor)
            textSize = 11.5f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(activity).apply {
            text = broadcast.title
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        val msgScroll = androidx.core.widget.NestedScrollView(activity).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val msgText = TextView(activity).apply {
            text = broadcast.message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13.5f
            setLineSpacing(0f, 1.25f)
            setPadding(0, dp(12), 0, dp(6))
        }
        msgScroll.addView(msgText)
        content.addView(msgScroll)

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }

        if (!broadcast.actionUrl.isNullOrBlank()) {
            val actionBtn = TextView(activity).apply {
                text = broadcast.actionText ?: activity.getString(R.string.btn_learn_more)
                gravity = Gravity.CENTER
                setTextColor(ThemeCoordinator.getContrastColor(accentColor))
                textSize = 13f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = activity.rippleBackground(accentColor)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                setOnClickListener {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(broadcast.actionUrl)))
                    } catch (_: Exception) {
                        AppToast.show(activity, "Could not open link.")
                    }
                }
            }
            buttonRow.addView(actionBtn)
        }

        if (broadcast.canDismiss) {
            val dismissBtn = TextView(activity).apply {
                text = activity.getString(R.string.btn_got_it)
                gravity = Gravity.CENTER
                setTextColor(if (broadcast.actionUrl.isNullOrBlank()) ThemeCoordinator.getContrastColor(accentColor) else themeCoordinator.textColor)
                textSize = 13f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = if (broadcast.actionUrl.isNullOrBlank()) {
                    activity.rippleBackground(accentColor)
                } else {
                    GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
                }
                setPadding(dp(18), dp(12), dp(18), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    if (broadcast.actionUrl.isNullOrBlank()) LinearLayout.LayoutParams.MATCH_PARENT else 0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    prefs.edit().putString("last_seen_broadcast_id", broadcast.id).apply()
                    dialog.dismiss()
                }
            }
            buttonRow.addView(dismissBtn)
        }

        content.addView(buttonRow)
        dialog.setContentView(content)

        if (!broadcast.canDismiss) {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
        } else {
            dialog.setOnCancelListener {
                prefs.edit().putString("last_seen_broadcast_id", broadcast.id).apply()
            }
        }

        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        broadcastDialogRef = dialog
        dialog.setOnDismissListener { broadcastDialogRef = null }
        dialog.show()
    }

    fun showUpdateAvailableDialog(info: UpdateInfo, isForce: Boolean = false) {
        if (updateDialogRef?.isShowing == true) return
        val currentCode = currentVersionCodeLong()
        val hardBlocked = isForce || info.isHardBlocked(currentCode)

        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        val badgeColor = if (hardBlocked) 0xFFEF4444.toInt() else themeCoordinator.primaryColor
        val badgeTitle = if (hardBlocked) activity.getString(R.string.update_critical_required) else activity.getString(R.string.update_available)

        content.addView(TextView(activity).apply {
            text = badgeTitle
            setTextColor(badgeColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.update_version_ready, info.versionName)
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        if (info.hasReleaseNotes) {
            content.addView(TextView(activity).apply {
                text = info.releaseNotes
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
        }
        content.addView(TextView(activity).apply {
            text = if (hardBlocked) activity.getString(R.string.update_critical_message) else activity.getString(R.string.update_message)
            setTextColor(themeCoordinator.textColor)
            alpha = if (hardBlocked) 0.85f else 0.45f
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }

        if (!hardBlocked) {
            val laterBtn = TextView(activity).apply {
                text = activity.getString(R.string.btn_later)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
                setPadding(dp(18), dp(12), dp(18), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
                setOnClickListener {
                    prefs.edit().putInt("update_dismissed_version", info.versionCode).apply()
                    dialog.dismiss()
                }
            }
            buttonRow.addView(laterBtn)
        }

        val updateBtn = TextView(activity).apply {
            text = activity.getString(R.string.btn_update)
            gravity = Gravity.CENTER
            val primaryBtnColor = if (hardBlocked) 0xFFEF4444.toInt() else themeCoordinator.primaryColor
            setTextColor(ThemeCoordinator.getContrastColor(primaryBtnColor))
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(primaryBtnColor)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = if (hardBlocked) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            } else {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            setOnClickListener {
                prefs.edit().remove("update_dismissed_version").apply()
                if (!hardBlocked) dialog.dismiss()
                openUpdateUrl(info.apkUrl?.takeIf { it.isNotBlank() } ?: info.url)
            }
        }
        buttonRow.addView(updateBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)

        if (hardBlocked) {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    activity.finishAffinity()
                    true
                } else false
            }
        } else {
            dialog.setOnCancelListener { prefs.edit().putInt("update_dismissed_version", info.versionCode).apply() }
        }

        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        updateDialogRef = dialog
        dialog.setOnDismissListener { updateDialogRef = null }
        dialog.show()
    }
}

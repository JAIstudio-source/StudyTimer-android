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
            setTextColor(themeCoordinator.bgColor)
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

    fun checkForUpdates(manual: Boolean) {
        if (!AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
            if (manual) {
                AppToast.show(activity, activity.getString(R.string.toast_up_to_date, currentVersionName()))
            }
            return
        }
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!manual) {
            val launches = prefs.safeInt("update_launch_count", 0) + 1
            prefs.edit().putInt("update_launch_count", launches).apply()
            if (launches % 5 != 0) return
        }
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
            if (info.versionCode <= currentVersionCodeLong()) {
                if (manual) {
                    AppToast.show(activity, activity.getString(R.string.toast_up_to_date, currentVersionName()))
                }
                return@check
            }
            if (!manual && prefs.safeInt("update_dismissed_version", -1) == info.versionCode) {
                return@check
            }
            showUpdateAvailableDialog(info)
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
            if (batteryOptDialogRef?.isShowing == true) batteryOptDialogRef?.dismiss()
        } catch (_: Exception) {}
        updateDialogRef = null
        batteryOptDialogRef = null
    }

    fun showUpdateAvailableDialog(info: UpdateInfo) {
        if (updateDialogRef?.isShowing == true) return
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.update_available)
            setTextColor(themeCoordinator.primaryColor)
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
                alpha = 0.6f
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
        }
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.update_message)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
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
        val updateBtn = TextView(activity).apply {
            text = activity.getString(R.string.btn_update)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(themeCoordinator.primaryColor)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                prefs.edit().remove("update_dismissed_version").apply()
                dialog.dismiss()
                openUpdateUrl(info.apkUrl?.takeIf { it.isNotBlank() } ?: info.url)
            }
        }
        buttonRow.addView(laterBtn)
        buttonRow.addView(updateBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        updateDialogRef = dialog
        dialog.setOnDismissListener { updateDialogRef = null }
        dialog.setOnCancelListener { prefs.edit().putInt("update_dismissed_version", info.versionCode).apply() }
        dialog.show()
    }
}

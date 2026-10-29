package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakRescueDialog {

    fun maybeShow(activity: MainActivity) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayStr = sdf.format(cal.time)

            val promptKey = "streak_rescue_prompt_$yesterdayStr"
            if (prefs.getBoolean(promptKey, false)) return

            val availableFreezes = StreakCalculator.getAvailableFreezesThisMonth(activity)
            if (availableFreezes <= 0) return

            val frozenDates = StreakCalculator.getFrozenDates(activity)
            if (frozenDates.contains(yesterdayStr)) return

            val yesterdayFocus = prefs.getLong("${yesterdayStr}_focus_total", 0L)
            val yesterdayGoal = activity.resolveGoalFor(yesterdayStr)
            if (yesterdayFocus >= yesterdayGoal) return // Goal was met yesterday

            // Check if user had an active streak prior to yesterday (day - 2 was qualified or frozen)
            val dayBeforeCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }
            val dayBeforeStr = sdf.format(dayBeforeCal.time)
            val dayBeforeFocus = prefs.getLong("${dayBeforeStr}_focus_total", 0L)
            val dayBeforeGoal = activity.resolveGoalFor(dayBeforeStr)
            val dayBeforeFrozen = frozenDates.contains(dayBeforeStr)

            val hadStreak = (dayBeforeFocus >= dayBeforeGoal || dayBeforeFrozen)
            if (!hadStreak) return

            // Record that we prompted so we don't nag repeatedly for the same day
            prefs.edit().putBoolean(promptKey, true).apply()

            val displayDateSdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            val formattedDate = try { displayDateSdf.format(cal.time) } catch (_: Exception) { yesterdayStr }

            show(activity, yesterdayStr, formattedDate, availableFreezes)
        } catch (_: Exception) {}
    }

    fun show(activity: MainActivity, yesterdayStr: String, formattedDate: String, availableFreezes: Int) {
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val themeCoordinator = activity.themeCoordinator
        val isDark = themeCoordinator.isDarkMode()
        val density = activity.resources.displayMetrics.density
        val dp = { v: Int -> (v * density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(24), dp(22), dp(24), dp(22))
        }

        // Top Header Tag
        val tagRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        val iconBox = FrameLayout(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(if (isDark) 0x3338BDF8.toInt() else 0x220284C7.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
        }
        iconBox.addView(TextView(activity).apply {
            text = "❄️"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        tagRow.addView(iconBox)

        tagRow.addView(TextView(activity).apply {
            text = "STREAK RESCUE"
            setTextColor(if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt())
            textSize = 11.5f
            letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        // Freeze Counter Pill
        tagRow.addView(TextView(activity).apply {
            text = "$availableFreezes/2 Freezes Left"
            setTextColor(if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt())
            textSize = 10.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0x2238BDF8.toInt() else 0x1A0284C7.toInt())
                setStroke(dp(1), if (isDark) 0x6638BDF8.toInt() else 0x4D0284C7.toInt())
            }
            setPadding(dp(8), dp(3), dp(8), dp(3))
        })
        root.addView(tagRow)

        // Title
        root.addView(TextView(activity).apply {
            text = "Streak at Risk!"
            setTextColor(themeCoordinator.textColor)
            textSize = 21f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        // Description
        root.addView(TextView(activity).apply {
            text = "You missed your study goal on $formattedDate. Don't let your hard work reset — use a streak freeze to protect yesterday and keep your streak alive!"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 13.5f
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, 0, 0, dp(18))
        })

        // Primary Action: Rescue Streak
        val rescueBtn = TextView(activity).apply {
            text = "❄️ Protect Yesterday & Save Streak"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (isDark) 0xFF0284C7.toInt() else 0xFF0369A1.toInt())
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                StreakCalculator.applyStreakFreeze(activity, yesterdayStr)
                activity.recalculateStreak()
                AppToast.show(activity, "❄️ Streak rescued! Yesterday is protected.")
                dialog.dismiss()
                activity.refreshStatsPanel()
            }
        }
        root.addView(rescueBtn)

        // Dismiss Action
        val dismissBtn = TextView(activity).apply {
            text = "Let Streak Reset"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isClickable = true
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(dismissBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}


package com.madeby.JAI

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Manages planner color schemes and theme presets selection.
 */
object PlannerThemeHelper {

    private fun tintedColor(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    fun resolvePlannerColors(context: Context, themeCoordinator: ThemeCoordinator): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return when (prefs.getString("planner_theme_preset", "DEFAULT")) {
            "EMERALD" -> Pair(0xFF10B981.toInt(), 0xFFF59E0B.toInt())
            "VIOLET" -> Pair(0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt())
            "OCEAN" -> Pair(0xFF06B6D4.toInt(), 0xFFFF6B6B.toInt())
            "SUNSET" -> Pair(0xFFF97316.toInt(), 0xFF10B981.toInt())
            "MIDNIGHT" -> Pair(0xFFEAB308.toInt(), 0xFFEC4899.toInt())
            else -> Pair(themeCoordinator.accentColor, themeCoordinator.secondaryColor)
        }
    }

    fun resolvePlannerAccentColor(context: Context, themeCoordinator: ThemeCoordinator): Int =
        resolvePlannerColors(context, themeCoordinator).first

    fun showPlannerThemePickerDialog(
        activity: Activity,
        themeCoordinator: ThemeCoordinator,
        onThemeChanged: () -> Unit
    ) {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(activity).apply {
            text = "Goal Colors"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, 0, 0, dp(14))
        })

        val presets = listOf(
            "DEFAULT" to "Classic Theme",
            "EMERALD" to "Emerald & Amber",
            "VIOLET" to "Violet & Cyan",
            "OCEAN" to "Ocean & Coral",
            "SUNSET" to "Sunset & Mint",
            "MIDNIGHT" to "Obsidian & Pink"
        )

        val currentPreset = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .getString("planner_theme_preset", "DEFAULT")

        for ((key, name) in presets) {
            val isSel = (key == currentPreset)
            val btn = TextView(activity).apply {
                text = if (isSel) "✓ $name" else name
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", if (isSel) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (isSel) Color.WHITE else themeCoordinator.textColor)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    if (isSel) {
                        setColor(themeCoordinator.primaryColor)
                    } else {
                        setColor(tintedColor(themeCoordinator.textColor, 15))
                    }
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(8)) }
                setOnClickListener {
                    activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                        .putString("planner_theme_preset", key).apply()
                    dialog.dismiss()
                    onThemeChanged()
                }
            }
            content.addView(btn)
        }

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}

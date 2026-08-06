package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build

class ThemeCoordinator(private val context: Context) {

    var activeBgMode = "OLED"
    var bgColor = 0xFF000000.toInt()
    var boxColor = 0xFF121212.toInt()
    var textColor = 0xFFFFFFFF.toInt()
    var primaryColor = Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f))
    var secondaryColor = Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f))

    fun applyThemeCoordinates() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putInt("customHue", 190)
                .putInt("customPrimary", Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f)))
                .putInt("customSecondaryHue", 120)
                .putInt("customSecondary", Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f)))
                .apply()
        }

        activeBgMode = sharedPrefs.getString("activeBgMode", "OLED") ?: "OLED"

        when (activeBgMode) {
            "LIGHT" -> {
                bgColor = 0xFFFFFFFF.toInt()
                boxColor = 0xFFEDF0F5.toInt()
                textColor = 0xFF0F172A.toInt()
            }
            "ECLIPSE" -> {
                bgColor = 0xFF0F172A.toInt()
                boxColor = 0xFF1E293B.toInt()
                textColor = 0xFFF8FAFC.toInt()
            }
            else -> {
                bgColor = 0xFF000000.toInt()
                boxColor = 0xFF121212.toInt()
                textColor = 0xFFFFFFFF.toInt()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sharedPrefs.getBoolean("dynamic_color", false)) {
            primaryColor = context.getColor(android.R.color.system_accent1_500)
            secondaryColor = context.getColor(android.R.color.system_accent2_500)
        } else {
            primaryColor = sharedPrefs.safeInt("customPrimary", Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f)))
            secondaryColor = sharedPrefs.safeInt("customSecondary", Color.HSVToColor(floatArrayOf(120f, 0.65f, 0.95f)))
        }
    }

    fun createCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 35f
            setColor(boxColor)
            if (activeBgMode == "LIGHT") {
                val density = context.resources.displayMetrics.density
                setStroke((1 * density).toInt(), Color.argb(30, 15, 23, 42))
            }
        }
    }

    fun createButtonBackground(colorHex: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 80f
            setColor(colorHex)
        }
    }
}

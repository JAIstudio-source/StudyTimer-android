package com.madeby.JAI

import android.content.Context
import android.graphics.Typeface
import java.util.concurrent.ConcurrentHashMap

data class AppFontItem(
    val id: String,
    val displayName: String,
    val category: String,
    val sampleText: String,
    val systemFamily: String,
    val style: Int = Typeface.NORMAL
)

object FontStyleManager {
    private const val PREF_KEY_FONT = "app_custom_font_id"
    private const val PREFS_NAME = "StudyTimerPrefs"

    val FONT_OPTIONS = listOf(
        // Modern Sans
        AppFontItem("sans_default", "System Default", "Modern Sans", "25:00 Focus Sharp", "sans-serif"),
        AppFontItem("sans_medium", "Sans Medium", "Modern Sans", "25:00 Focus Sharp", "sans-serif-medium"),
        AppFontItem("sans_condensed", "Sans Condensed", "Modern Sans", "25:00 Focus Sharp", "sans-serif-condensed"),
        AppFontItem("sans_black", "Sans Heavy", "Modern Sans", "25:00 Focus Sharp", "sans-serif-black"),
        AppFontItem("sans_light", "Sans Minimal", "Modern Sans", "25:00 Focus Sharp", "sans-serif-light"),
        AppFontItem("sans_thin", "Ultra Thin", "Modern Sans", "25:00 Focus Sharp", "sans-serif-thin"),

        // Monospace & Digital Clocks (Timer Favorites)
        AppFontItem("mono_default", "Clean Monospace", "Monospace", "00:25:00", "monospace"),
        AppFontItem("mono_bold", "Bold Console Mono", "Monospace", "00:25:00", "monospace", Typeface.BOLD),
        AppFontItem("mono_sans", "Sans Mono", "Monospace", "00:25:00", "sans-serif-monospace"),
        AppFontItem("mono_code", "Matrix Terminal", "Monospace", "00:25:00", "monospace", Typeface.ITALIC),

        // Editorial & Classy Serifs
        AppFontItem("serif_default", "Classic Editorial", "Refined Serif", "Deep Work Habit", "serif"),
        AppFontItem("serif_bold", "Bold Academic Serif", "Refined Serif", "Deep Work Habit", "serif", Typeface.BOLD),
        AppFontItem("serif_italic", "Literary Italic", "Refined Serif", "Deep Work Habit", "serif", Typeface.ITALIC),
        AppFontItem("serif_condensed", "Compact Chronicle", "Refined Serif", "Deep Work Habit", "serif-condensed"),

        // Casual, Creative & Expressive
        AppFontItem("casual", "Casual Chill", "Creative & Chill", "Study flow & focus", "casual"),
        AppFontItem("casual_bold", "Punchy Casual", "Creative & Chill", "Study flow & focus", "casual", Typeface.BOLD),
        AppFontItem("cursive", "Aesthetic Script", "Creative & Chill", "Study flow & focus", "cursive"),
        AppFontItem("cursive_bold", "Warm Handwriting", "Creative & Chill", "Study flow & focus", "cursive", Typeface.BOLD),

        // High-Contrast Display & Geometry
        AppFontItem("condensed_light", "Nordic Condensed", "Geometric", "25 MIN FOCUS", "sans-serif-condensed-light"),
        AppFontItem("condensed_medium", "Tech Headline", "Geometric", "25 MIN FOCUS", "sans-serif-condensed", Typeface.BOLD),
        AppFontItem("smallcaps", "Small Caps Hybrid", "Geometric", "Deep Focus Room", "sans-serif-smallcaps")
    )

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    fun getSelectedFontId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_FONT, "sans_medium") ?: "sans_medium"
    }

    fun setSelectedFontId(context: Context, fontId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_FONT, fontId)
            .apply()
    }

    fun getTypeface(context: Context, fontId: String? = null, overrideStyle: Int? = null): Typeface {
        val id = fontId ?: getSelectedFontId(context)
        val cacheKey = "$id:${overrideStyle ?: -1}"
        return typefaceCache.getOrPut(cacheKey) {
            val item = FONT_OPTIONS.find { it.id == id } ?: FONT_OPTIONS[0]
            val finalStyle = overrideStyle ?: item.style
            try {
                Typeface.create(item.systemFamily, finalStyle)
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    fun getBoldTypeface(context: Context): Typeface = getTypeface(context, overrideStyle = Typeface.BOLD)

    fun getNormalTypeface(context: Context): Typeface = getTypeface(context, overrideStyle = Typeface.NORMAL)

    fun applyTo(textView: android.widget.TextView, isBold: Boolean = false) {
        textView.typeface = if (isBold) getBoldTypeface(textView.context) else getNormalTypeface(textView.context)
    }

    /**
     * Recursively traverses any View hierarchy (panels, cards, dialogs, nav bars)
     * and applies the user's selected global font, honoring whether each element is bold or regular.
     */
    fun applyRecursively(root: android.view.View, headingsBold: Boolean = true) {
        if (root is android.widget.TextView) {
            val tf = root.typeface
            val isCurrentBold = tf?.isBold == true || ((tf?.style ?: 0) and Typeface.BOLD != 0) || root.paint.isFakeBoldText
            root.typeface = if (isCurrentBold && headingsBold) {
                getBoldTypeface(root.context)
            } else {
                getNormalTypeface(root.context)
            }
        }
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                applyRecursively(root.getChildAt(i), headingsBold)
            }
        }
    }
}

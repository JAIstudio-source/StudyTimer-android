package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build

class ThemeCoordinator(private val context: Context) {

    var activeBgMode = "LIGHT"
    var uiStyle = "GLASS"
    var bgColor = 0xFFF8FAFC.toInt()
    var boxColor = 0xFFFFFFFF.toInt()
    var textColor = 0xFF0F172A.toInt()
    var primaryColor = Color.parseColor("#4F46E5")
    var secondaryColor = Color.parseColor("#0284C7")
    var accentColor = Color.parseColor("#4F46E5")

    companion object {
        fun getContrastColor(bgColor: Int): Int {
            val r = Color.red(bgColor) / 255.0
            val g = Color.green(bgColor) / 255.0
            val b = Color.blue(bgColor) / 255.0
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            return if (luminance > 0.55) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
        }

        val SOFT_FOCUS_PALETTE = listOf(
            "#818CF8", // Soft Lavender / Indigo
            "#60A5FA", // Soft Sky Blue
            "#38BDF8", // Pastel Cyan
            "#A78BFA", // Light Purple
            "#F472B6", // Pastel Rose
            "#FB7185", // Soft Coral
            "#FB923C", // Pastel Peach
            "#FBBF24", // Warm Amber
            "#34D399", // Soft Mint
            "#2DD4BF"  // Aqua Teal
        )

        val SOFT_BREAK_PALETTE = listOf(
            "#34D399", // Mint Green
            "#2DD4BF", // Soothing Teal
            "#38BDF8", // Calm Sky Blue
            "#A7F3D0", // Soft Seafoam
            "#6EE7B7", // Pastel Emerald
            "#818CF8", // Relaxing Lavender
            "#F472B6", // Rose Quartz
            "#FDE047", // Warm Lemon
            "#FDBA74", // Light Apricot
            "#94A3B8"  // Zen Slate
        )

        // Decoupled dedicated analytics accent for Insights floating nav capsule
        val INSIGHTS_NAV_ACCENT = Color.parseColor("#6366F1")
    }

    fun applyThemeCoordinates() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "LIGHT")
                .putString("ui_style", "GLASS")
                .putInt("customHue", 243)
                .putInt("customPrimary", Color.parseColor("#4F46E5"))
                .putInt("customSecondaryHue", 199)
                .putInt("customSecondary", Color.parseColor("#0284C7"))
                .apply()
        }

        var storedMode = sharedPrefs.getString("activeBgMode", "LIGHT") ?: "LIGHT"
        if (storedMode == "ECLIPSE") {
            storedMode = "LIGHT"
            sharedPrefs.edit().putString("activeBgMode", "LIGHT").apply()
        } else if (storedMode != "LIGHT" && storedMode != "OLED") {
            storedMode = "LIGHT"
        }
        activeBgMode = storedMode

        uiStyle = sharedPrefs.getString("ui_style", "GLASS")?.takeIf { it != "BUBBLE" } ?: "GLASS"

        when (activeBgMode) {
            "LIGHT" -> {
                bgColor = 0xFFF8FAFC.toInt()
                boxColor = 0xFFFFFFFF.toInt()
                textColor = 0xFF0F172A.toInt()
                accentColor = Color.parseColor("#4F46E5")
            }
            else -> {
                bgColor = 0xFF000000.toInt()
                boxColor = 0xFF12131A.toInt() // Elevated OLED Dark surface
                textColor = 0xFFFFFFFF.toInt() // Dynamic high-contrast stark white
                accentColor = Color.HSVToColor(floatArrayOf(36f, 0.85f, 0.95f))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sharedPrefs.getBoolean("dynamic_color", false)) {
            primaryColor = context.getColor(android.R.color.system_accent1_500)
            secondaryColor = context.getColor(android.R.color.system_accent2_500)
            accentColor = context.getColor(android.R.color.system_accent1_300)
        } else {
            val defaultPrimary = if (activeBgMode == "LIGHT") Color.parseColor("#4F46E5") else Color.parseColor("#A78BFA")
            val defaultSecondary = if (activeBgMode == "LIGHT") Color.parseColor("#0284C7") else Color.parseColor("#38BDF8")
            primaryColor = sharedPrefs.safeInt("customPrimary", defaultPrimary)
            secondaryColor = sharedPrefs.safeInt("customSecondary", defaultSecondary)
        }
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun isDarkMode(): Boolean {
        return activeBgMode != "LIGHT"
    }
    fun isGlassStyle(): Boolean = uiStyle == "GLASS"
    fun isBubbleStyle(): Boolean = false

    fun createBackgroundDrawable(): GradientDrawable {
        if (!isGlassStyle() && !isBubbleStyle()) {
            val flatBg = if (activeBgMode == "LIGHT") 0xFFF8FAFC.toInt() else 0xFF000000.toInt()
            return GradientDrawable().apply { setColor(flatBg) }
        }
        val top: Int
        val bottom: Int
        if (activeBgMode == "LIGHT") {
            top = 0xFFF8FAFC.toInt()
            bottom = 0xFFF1F5F9.toInt()
        } else {
            // Luxury Obsidian & Frosted Glass Canvas
            top = 0xFF0D0F17.toInt()
            bottom = 0xFF05060A.toInt()
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, bottom))
    }

    fun createCardBackground(cornerRadius: Float = 24f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        val isLightSurface = activeBgMode == "LIGHT"

        if (!isGlassStyle()) {
            // Flat Mode: 100% Solid Opaque Cards (no translucency, ignores wallpaper)
            return if (isLightSurface) {
                GradientDrawable().apply {
                    this.cornerRadius = cornerRadius * density
                    setColor(0xFFFFFFFF.toInt()) // Crisp pure white
                    setStroke((1f * density).toInt(), 0xFFE2E8F0.toInt())
                }
            } else {
                GradientDrawable().apply {
                    this.cornerRadius = cornerRadius * density
                    setColor(0xFF12131A.toInt()) // Deep solid elevated dark surface
                    setStroke((1f * density).toInt(), Color.argb(32, 255, 255, 255))
                }
            }
        }

        // Glass Mode: Frosted translucent glass card that blends with custom wallpapers and canvas
        val fillCol = if (isLightSurface) Color.argb(205, 255, 255, 255) else Color.argb(170, 14, 16, 24)
        val strokeCol = if (isLightSurface) Color.argb(80, 203, 213, 225) else Color.argb(45, 255, 255, 255)
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fillCol)
            setStroke((1f * density).toInt(), strokeCol)
        }
    }

    fun createDialogBackground(cornerRadius: Float = 28f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (activeBgMode == "LIGHT") {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(0xFFFFFFFF.toInt())
                setStroke((1f * density).toInt(), 0xFFE2E8F0.toInt())
            }
        }
        if (isBubbleStyle()) {
            val dialogTop = 0xFF161922.toInt()
            val dialogBottom = 0xFF0B0D12.toInt()
            return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(dialogTop, dialogBottom)).apply {
                this.cornerRadius = cornerRadius * density
                setStroke((1.5f * density).toInt(), 0x3EFFFFFF.toInt())
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(0xFF141622.toInt())
                setStroke((1 * density).toInt(), Color.argb(40, 255, 255, 255))
            }
        }
        val fill = 0xEE0D0F15.toInt()
        val stroke = 0x38FFFFFF.toInt()
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1 * density).toInt(), stroke)
        }
    }

    fun createGlowGradient(colorHex: Int, cornerRadius: Float = 80f): GradientDrawable {
        if (!isGlassStyle() && !isBubbleStyle()) {
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius
                setColor(colorHex)
            }
        }
        val light = blend(colorHex, -1, 0.30f)
        val dark = blend(colorHex, 0xFF000000.toInt(), 0.20f)
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(light, colorHex, dark)).apply {
            this.cornerRadius = cornerRadius
        }
    }

    fun createGlowBlob(color: Int, radius: Float = 240f, alpha: Int = 55): GradientDrawable {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setGradientType(GradientDrawable.RADIAL_GRADIENT)
            setGradientRadius(radius * context.resources.displayMetrics.density)
            setColors(intArrayOf(Color.argb(alpha, r, g, b), Color.argb(0, r, g, b)))
        }
    }

    fun createGlassChip(borderColor: Int, cornerRadius: Float = 30f): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            if (borderColor != 0 && Color.alpha(borderColor) > 150) {
                return Soft3DBubbleDrawable(borderColor, cornerRadius * density, isDarkMode(), elevationPx = 6f)
            }
            val fill = if (isDarkMode()) 0x22FFFFFF.toInt() else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) 0x33FFFFFF.toInt() else (if (borderColor != 0) borderColor else 0x330F172A.toInt())
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fill)
                setStroke((1 * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            val fill = if (isDarkMode()) 0xFF1E2230.toInt() else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) (if (borderColor != 0) borderColor else Color.argb(40, 255, 255, 255)) else (if (borderColor != 0) borderColor else 0xFFE2E8F0.toInt())
            return GradientDrawable().apply {
                this.cornerRadius = cornerRadius * density
                setColor(fill)
                setStroke((1 * density).toInt(), stroke)
            }
        }
        val fill = if (isDarkMode()) 0x22FFFFFF.toInt() else 0xFFF1F5F9.toInt()
        val stroke = if (isDarkMode()) borderColor else (if (borderColor != 0) borderColor else Color.argb(50, 15, 23, 42))
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius * density
            setColor(fill)
            setStroke((1.5f * density).toInt(), stroke)
        }
    }

    fun createGlassIconBackground(borderColor: Int): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            val fill = if (isDarkMode()) 0x1EFFFFFF.toInt() else 0xFFF1F5F9.toInt()
            val stroke = if (isDarkMode()) 0x28FFFFFF.toInt() else (if (borderColor != 0) borderColor else 0x330F172A.toInt())
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
        }
        if (!isGlassStyle()) {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDarkMode()) 0xFF1A1C26.toInt() else 0xFFF1F5F9.toInt())
                setStroke((1 * density).toInt(), if (activeBgMode == "LIGHT") 0xFFE2E8F0.toInt() else Color.argb(35, 255, 255, 255))
            }
        }
        val fill = if (isDarkMode()) 0x1CFFFFFF.toInt() else 0xFFF1F5F9.toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((2 * density).toInt(), if (isDarkMode()) borderColor else (if (borderColor != 0) borderColor else Color.argb(40, 15, 23, 42)))
        }
    }

    fun createButtonBackground(colorHex: Int): android.graphics.drawable.Drawable {
        val density = context.resources.displayMetrics.density
        if (isBubbleStyle()) {
            return Soft3DBubbleDrawable(colorHex, 80f * density, isDarkMode(), elevationPx = 14f)
        }
        return GradientDrawable().apply {
            cornerRadius = 80f * density
            setColor(colorHex)
        }
    }

    fun getAppTypeface(isBold: Boolean = false): android.graphics.Typeface {
        return if (isBold) FontStyleManager.getBoldTypeface(context) else FontStyleManager.getNormalTypeface(context)
    }

    fun getContrastTextColorFor(backgroundColor: Int): Int = getContrastColor(backgroundColor)

    fun getDynamicWallpaperContrastTextColor(): Int {
        return if (WallpaperManager.isBackgroundUnderTimerBright(context)) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
    }
}

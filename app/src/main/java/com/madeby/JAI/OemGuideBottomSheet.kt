package com.madeby.JAI

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Clean, minimal bottom sheet that presents step-by-step background reliability
 * guidance tailored to the user's specific phone manufacturer, with screenshot walkthroughs.
 */
object OemGuideBottomSheet {

    fun show(activity: MainActivity, initialBrand: OemOptimizationHelper.OemBrand? = null) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val theme = activity.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val textColor = theme.textColor

        fun dp(v: Int): Int = activity.dp(v)
        fun tintedColor(c: Int, alpha: Int): Int = activity.tintedColor(c, alpha)

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
                setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0x1A000000.toInt())
            }
            setPadding(dp(20), dp(12), dp(20), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Drag handle
        val handle = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(if (isDark) 0x44FFFFFF.toInt() else 0x33000000.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14)
            }
        }
        root.addView(handle)

        // Tag badge
        val tagView = TextView(activity).apply {
            text = "BACKGROUND RELIABILITY"
            setTextColor(primary)
            textSize = 11f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(tagView)

        // Title
        val titleView = TextView(activity).apply {
            text = "Keep Timer Running in Background"
            setTextColor(textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        }
        root.addView(titleView)

        // Subtitle
        val subtitleView = TextView(activity).apply {
            text = "Certain phones aggressively close background apps. Follow these simple steps so your study sessions and alarms are never interrupted."
            setTextColor(textColor)
            alpha = 0.65f
            textSize = 13f
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(subtitleView)

        // Scrollable content container for brand steps & visuals
        val scrollView = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(340)
            )
        }
        val contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(contentContainer)
        root.addView(scrollView)

        var selectedBrand = initialBrand ?: OemOptimizationHelper.detectBrand()

        // Brand pill selector bar
        val brandScroll = android.widget.HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dp(14))
        }
        val brandRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        brandScroll.addView(brandRow)
        root.addView(brandScroll, 4) // Insert right below subtitle

        fun renderBrandDetails() {
            contentContainer.removeAllViews()

            // Steps section
            selectedBrand.steps.forEachIndexed { index, stepText ->
                val stepRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(6), 0, dp(6))
                }

                // Number badge
                val numberBadge = TextView(activity).apply {
                    text = "${index + 1}"
                    gravity = Gravity.CENTER
                    setTextColor(primary)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(tintedColor(primary, 24))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                        rightMargin = dp(12)
                    }
                }
                stepRow.addView(numberBadge)

                val stepLabel = TextView(activity).apply {
                    text = stepText
                    setTextColor(textColor)
                    textSize = 13f
                    alpha = 0.85f
                    setLineSpacing(dp(2).toFloat(), 1f)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                stepRow.addView(stepLabel)
                contentContainer.addView(stepRow)
            }

            // Visual guide images if available
            if (selectedBrand.drawableResIds.isNotEmpty()) {
                val visualHeader = TextView(activity).apply {
                    text = "VISUAL GUIDE"
                    setTextColor(primary)
                    textSize = 11f
                    letterSpacing = 0.1f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(14), 0, dp(8))
                }
                contentContainer.addView(visualHeader)

                selectedBrand.drawableResIds.forEach { resId ->
                    val imgCard = ImageView(activity).apply {
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageResource(resId)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(if (isDark) 0x1AFFFFFF.toInt() else 0x0A000000.toInt())
                            setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0x14000000.toInt())
                        }
                        clipToOutline = true
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(10)
                        }
                    }
                    contentContainer.addView(imgCard)
                }
            }
        }

        fun updateBrandPills() {
            brandRow.removeAllViews()
            val allBrands = listOf(
                OemOptimizationHelper.OemBrand.XIAOMI,
                OemOptimizationHelper.OemBrand.SAMSUNG,
                OemOptimizationHelper.OemBrand.VIVO,
                OemOptimizationHelper.OemBrand.HUAWEI,
                OemOptimizationHelper.OemBrand.ASUS,
                OemOptimizationHelper.OemBrand.OTHER
            )

            allBrands.forEach { brand ->
                val isSelected = (brand == selectedBrand)
                val pill = TextView(activity).apply {
                    text = brand.brandName
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) theme.bgColor else textColor)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        if (isSelected) {
                            setColor(primary)
                        } else {
                            setColor(if (isDark) 0x1CFFFFFF.toInt() else 0x0F000000.toInt())
                            setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0x14000000.toInt())
                        }
                    }
                    setPadding(dp(14), dp(7), dp(14), dp(7))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(8)
                    }
                    setOnClickListener {
                        selectedBrand = brand
                        updateBrandPills()
                        renderBrandDetails()
                    }
                }
                brandRow.addView(pill)
            }
        }

        updateBrandPills()
        renderBrandDetails()

        // Button row
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }

        val dismissBtn = TextView(activity).apply {
            text = "Done"
            gravity = Gravity.CENTER
            setTextColor(textColor)
            alpha = 0.7f
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0x1CFFFFFF.toInt() else 0x0F000000.toInt())
            }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                rightMargin = dp(10)
            }
            setOnClickListener { dialog.dismiss() }
        }

        val openSettingsBtn = TextView(activity).apply {
            text = "Open Settings"
            gravity = Gravity.CENTER
            setTextColor(theme.bgColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(primary)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )
            setOnClickListener {
                selectedBrand.openSettings(activity)
            }
        }

        buttonRow.addView(dismissBtn)
        buttonRow.addView(openSettingsBtn)
        root.addView(buttonRow)

        dialog.setContentView(root)
        dialog.window?.let { win ->
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            win.setGravity(Gravity.BOTTOM)
            win.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            win.setWindowAnimations(android.R.style.Animation_InputMethod)
        }
        dialog.show()
    }
}


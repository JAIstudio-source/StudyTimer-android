package com.madeby.JAI

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object WallpaperGalleryDialog {

    fun show(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        onWallpaperChanged: () -> Unit
    ) {
        val context = activity
        val dialog = BottomSheetDialog(context, R.style.TransparentBottomSheetDialogTheme)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(activity.dp(18), activity.dp(16), activity.dp(18), activity.dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- Drag handle ---
        val dragHandle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(0x33888888)
                cornerRadius = activity.dp(3).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(activity.dp(36), activity.dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = activity.dp(12)
            }
        }
        root.addView(dragHandle)

        // --- Header Row ---
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(4), 0, activity.dp(4), activity.dp(10))
        }

        val headerTextLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            text = "Wallpaper & Ambience"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        }
        headerTextLayout.addView(titleView)

        val subView = TextView(context).apply {
            text = "Visual presets, live shaders & ambience tuning"
            textSize = 12f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        }
        headerTextLayout.addView(subView)
        headerRow.addView(headerTextLayout)

        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(activity.dp(10), activity.dp(6), activity.dp(10), activity.dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // --- Nested Scrollable Container (Fixes Bottom-Scroll Lock & Sheet Conflicts) ---
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val maxScrollHeight = (screenHeight * 0.72f).toInt().coerceIn(activity.dp(380), activity.dp(620))

        val scrollView = NestedScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Tracking active selection views for instant UI refresh
        var activeBorderCard: FrameLayout? = null
        var activeCheckBadge: TextView? = null

        fun createSectionTitle(title: String, subtitle: String? = null): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dp(4), activity.dp(14), activity.dp(4), activity.dp(8))
                addView(TextView(context).apply {
                    text = title
                    textSize = 12f
                    letterSpacing = 0.08f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                })
                if (subtitle != null) {
                    addView(TextView(context).apply {
                        text = subtitle
                        textSize = 11f
                        setTextColor(tintedColor(themeCoordinator.textColor, 120))
                    })
                }
            }
        }

        // ==========================================
        // SECTION 1: LIVE AMBIENT SHADERS (AURORA WAVE IS DEFAULT)
        // ==========================================
        contentLayout.addView(createSectionTitle("LIVE AMBIENT", "Animated background motion"))

        val activeType = WallpaperManager.getWallpaperType(context)
        val activePresetId = WallpaperManager.getActivePresetId(context)
        val cardCornerRadius = activity.dp(14).toFloat()

        // Excludes Amber Video / Amber Hearth completely per requirement
        val liveShaders = listOf(
            Triple(WallpaperManager.TYPE_LIVE_AURORA, "Aurora Wave", "Emerald & violet ambient ribbons"),
            Triple(WallpaperManager.TYPE_LIVE_COSMIC, "Cosmic Stars", "Deep space nebula & twinkling stars"),
            Triple(WallpaperManager.TYPE_LIVE_LOFI_RAIN, "Lofi Rain", "Soothing falling rain & mist")
        )

        val liveRowScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val liveRowContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(activity.dp(2), activity.dp(2), activity.dp(2), activity.dp(2))
        }

        liveShaders.forEach { (shaderType, title, _) ->
            val isSelected = (activeType == shaderType)

            val shaderCard = FrameLayout(context).apply {
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, cardCornerRadius)
                    }
                }
                val bgColors = when (shaderType) {
                    WallpaperManager.TYPE_LIVE_AURORA -> intArrayOf(0xFF0F172A.toInt(), 0xFF065F46.toInt(), 0xFF4C1D95.toInt())
                    WallpaperManager.TYPE_LIVE_COSMIC -> intArrayOf(0xFF090D16.toInt(), 0xFF1E1B4B.toInt(), 0xFF312E81.toInt())
                    WallpaperManager.TYPE_LIVE_LOFI_RAIN -> intArrayOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF334155.toInt())
                    else -> intArrayOf(0xFF0F172A.toInt(), 0xFF065F46.toInt(), 0xFF4C1D95.toInt())
                }
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, bgColors).apply {
                    cornerRadius = cardCornerRadius
                    if (isSelected) {
                        setStroke(activity.dp(2), themeCoordinator.primaryColor)
                    } else {
                        setStroke(activity.dp(1), 0x22FFFFFF)
                    }
                }
                layoutParams = LinearLayout.LayoutParams(activity.dp(135), activity.dp(92)).apply {
                    setMargins(activity.dp(4), 0, activity.dp(6), 0)
                }
            }

            val shaderTextLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                setPadding(activity.dp(10), activity.dp(8), activity.dp(10), activity.dp(8))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                val badge = TextView(context).apply {
                    text = "LIVE"
                    textSize = 8f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = activity.dp(4).toFloat()
                        setColor(0x55000000)
                    }
                    setPadding(activity.dp(4), activity.dp(2), activity.dp(4), activity.dp(2))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = activity.dp(4)
                    }
                }
                addView(badge)

                addView(TextView(context).apply {
                    text = title
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
            }
            shaderCard.addView(shaderTextLayout)

            val shaderBadge = TextView(context).apply {
                text = "✓"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                val badgeSize = activity.dp(20)
                layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(0, activity.dp(6), activity.dp(6), 0)
                }
                visibility = if (isSelected) View.VISIBLE else View.GONE
            }
            shaderCard.addView(shaderBadge)

            if (isSelected) {
                activeBorderCard = shaderCard
                activeCheckBadge = shaderBadge
            }

            shaderCard.setOnClickListener {
                shaderCard.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                activeBorderCard?.background = GradientDrawable().apply {
                    cornerRadius = cardCornerRadius
                    setColor(0xFF1E293B.toInt())
                    setStroke(activity.dp(1), 0x22FFFFFF)
                }
                activeCheckBadge?.visibility = View.GONE

                val bgColors = when (shaderType) {
                    WallpaperManager.TYPE_LIVE_AURORA -> intArrayOf(0xFF0F172A.toInt(), 0xFF065F46.toInt(), 0xFF4C1D95.toInt())
                    WallpaperManager.TYPE_LIVE_COSMIC -> intArrayOf(0xFF090D16.toInt(), 0xFF1E1B4B.toInt(), 0xFF312E81.toInt())
                    WallpaperManager.TYPE_LIVE_LOFI_RAIN -> intArrayOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF334155.toInt())
                    else -> intArrayOf(0xFF0F172A.toInt(), 0xFF065F46.toInt(), 0xFF4C1D95.toInt())
                }
                shaderCard.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, bgColors).apply {
                    cornerRadius = cardCornerRadius
                    setStroke(activity.dp(2), themeCoordinator.primaryColor)
                }
                shaderBadge.visibility = View.VISIBLE
                activeBorderCard = shaderCard
                activeCheckBadge = shaderBadge

                activity.setLiveWallpaper(shaderType)
                onWallpaperChanged()
            }

            liveRowContainer.addView(shaderCard)
        }
        liveRowScroll.addView(liveRowContainer)
        contentLayout.addView(liveRowScroll)

        // ==========================================
        // SECTION 2: PRESET PHOTO WALLPAPERS (Robust Weighted Rows - 100% Guaranteed Visibility)
        // ==========================================
        val presets = WallpaperManager.getPresetCatalog(context)
        contentLayout.addView(createSectionTitle("WALLPAPERS (${presets.size})", "Tap to preview and apply"))

        // Chunk into rows of 3 to avoid GridLayout collapsing bugs across OEMs
        val chunkedPresets = presets.chunked(3)
        chunkedPresets.forEach { rowItems ->
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            for (preset in rowItems) {
                val isSelected = (activeType == WallpaperManager.TYPE_PRESET && activePresetId == preset.id)

                val cardContainer = FrameLayout(context).apply {
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, cardCornerRadius)
                        }
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = cardCornerRadius
                        setColor(0xFF1E293B.toInt())
                        if (isSelected) {
                            setStroke(activity.dp(2), themeCoordinator.primaryColor)
                        } else {
                            setStroke(activity.dp(1), 0x22FFFFFF)
                        }
                    }
                }

                // Image Preview
                val previewImage = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    val thumb = WallpaperManager.loadPresetThumbnail(context, preset)
                    if (thumb != null) {
                        setImageBitmap(thumb)
                    } else {
                        setBackgroundColor(0xFF334155.toInt())
                    }
                }
                cardContainer.addView(previewImage)

                // Gradient scrim at bottom for text readability
                val scrimOverlay = View(context).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.TRANSPARENT, 0x99000000.toInt(), 0xDD000000.toInt())
                    )
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        activity.dp(56)
                    ).apply {
                        gravity = Gravity.BOTTOM
                    }
                }
                cardContainer.addView(scrimOverlay)

                // Text label overlay
                val labelLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.BOTTOM
                    setPadding(activity.dp(8), activity.dp(4), activity.dp(8), activity.dp(8))
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.BOTTOM
                    }

                    addView(TextView(context).apply {
                        text = preset.title
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    addView(TextView(context).apply {
                        text = preset.category
                        textSize = 9f
                        setTextColor(0xAAFFFFFF.toInt())
                        maxLines = 1
                    })
                }
                cardContainer.addView(labelLayout)

                // Selection Checkmark Chip (Top-Right)
                val checkBadge = TextView(context).apply {
                    text = "✓"
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(themeCoordinator.primaryColor)
                    }
                    val badgeSize = activity.dp(20)
                    layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                        gravity = Gravity.TOP or Gravity.END
                        setMargins(0, activity.dp(6), activity.dp(6), 0)
                    }
                    visibility = if (isSelected) View.VISIBLE else View.GONE
                }
                cardContainer.addView(checkBadge)

                if (isSelected) {
                    activeBorderCard = cardContainer
                    activeCheckBadge = checkBadge
                }

                // Row weighted layout params
                cardContainer.layoutParams = LinearLayout.LayoutParams(
                    0,
                    activity.dp(136),
                    1f
                ).apply {
                    setMargins(activity.dp(4), activity.dp(4), activity.dp(4), activity.dp(4))
                }

                // Click listener to select preset
                cardContainer.setOnClickListener {
                    cardContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    // Update previous active selection UI
                    activeBorderCard?.background = GradientDrawable().apply {
                        cornerRadius = cardCornerRadius
                        setColor(0xFF1E293B.toInt())
                        setStroke(activity.dp(1), 0x22FFFFFF)
                    }
                    activeCheckBadge?.visibility = View.GONE

                    // Set new active selection UI
                    cardContainer.background = GradientDrawable().apply {
                        cornerRadius = cardCornerRadius
                        setColor(0xFF1E293B.toInt())
                        setStroke(activity.dp(2), themeCoordinator.primaryColor)
                    }
                    checkBadge.visibility = View.VISIBLE
                    activeBorderCard = cardContainer
                    activeCheckBadge = checkBadge

                    // Apply preset
                    WallpaperManager.setActivePreset(context, preset.id)
                    if (!WallpaperManager.hasUserCustomDimAlpha(context)) {
                        WallpaperManager.setWallpaperDimAlpha(context, preset.recommendedDim, isUserExplicit = false)
                    }
                    activity.updateWallpaperLayer()
                    onWallpaperChanged()
                    AppToast.show(context, "${preset.title} applied")
                }

                rowLayout.addView(cardContainer)
            }

            // Fill remaining columns if last row has fewer than 3 items
            if (rowItems.size < 3) {
                val missing = 3 - rowItems.size
                for (i in 0 until missing) {
                    val spacer = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply {
                            setMargins(activity.dp(4), activity.dp(4), activity.dp(4), activity.dp(4))
                        }
                    }
                    rowLayout.addView(spacer)
                }
            }

            contentLayout.addView(rowLayout)
        }

        // ==========================================
        // SECTION 3: CUSTOM & SOLID OPTIONS
        // ==========================================
        contentLayout.addView(createSectionTitle("CUSTOM & SOLID"))

        val customRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun createActionTile(@androidx.annotation.DrawableRes iconRes: Int, title: String, isSolidReset: Boolean = false, onClick: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(14).toFloat()
                    setColor(if (isSolidReset) 0x1AEE4444.toInt() else 0x1AFFFFFF.toInt())
                    setStroke(activity.dp(1), if (isSolidReset) 0x33EE4444.toInt() else 0x25FFFFFF)
                }
                setPadding(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(activity.dp(3), activity.dp(2), activity.dp(3), activity.dp(2))
                }
                addView(ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(if (isSolidReset) 0xFFEF4444.toInt() else themeCoordinator.primaryColor)
                    layoutParams = LinearLayout.LayoutParams(activity.dp(22), activity.dp(22)).apply {
                        gravity = Gravity.CENTER
                    }
                })
                addView(TextView(context).apply {
                    text = title
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSolidReset) 0xFFEF4444.toInt() else themeCoordinator.textColor)
                    gravity = Gravity.CENTER
                    setPadding(0, activity.dp(6), 0, 0)
                })
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            }
        }

        // Custom Photo Tile
        customRow.addView(createActionTile(R.drawable.ic_image, "My Photo") {
            dialog.dismiss()
            activity.pickCustomWallpaper()
        })

        // Custom Video Loop Tile
        customRow.addView(createActionTile(R.drawable.ic_video, "My Video") {
            dialog.dismiss()
            activity.pickLiveVideoWallpaper()
        })

        // Reset to Default Solid Background
        customRow.addView(createActionTile(R.drawable.ic_solid_canvas, "Default Solid", isSolidReset = true) {
            WallpaperManager.clearWallpaper(context)
            activity.updateWallpaperLayer()
            onWallpaperChanged()
            AppToast.show(context, "Solid background restored")
            dialog.dismiss()
        })

        contentLayout.addView(customRow)

        // ==========================================
        // SECTION 4: AMBIENCE & CONTROLS (Dim Scrim Slider)
        // ==========================================
        contentLayout.addView(createSectionTitle("BACKGROUND DIM", "Adjust dimness to keep timer readable"))

        val dimCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(16f)
            setPadding(activity.dp(16), activity.dp(14), activity.dp(16), activity.dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, activity.dp(4), 0, activity.dp(12))
            }
        }

        val currentDimAlpha = WallpaperManager.getWallpaperDimAlpha(context)
        val initialPercent = (currentDimAlpha * 100).toInt()

        val sliderHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dimLabel = TextView(context).apply {
            text = "Dim Intensity"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        sliderHeader.addView(dimLabel)

        val dimValueText = TextView(context).apply {
            text = "$initialPercent%"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
        }
        sliderHeader.addView(dimValueText)
        dimCard.addView(sliderHeader)

        val dimSeekBar = SeekBar(context).apply {
            max = 100
            progress = initialPercent.coerceIn(0, 100)
            splitTrack = false
            setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val percent = progress.coerceIn(0, 100)
                    dimValueText.text = "$percent%"
                    if (fromUser) {
                        val alpha = percent / 100f
                        WallpaperManager.setWallpaperDimAlpha(context, alpha, isUserExplicit = true)
                        activity.wallpaperDimScrimView?.setBackgroundColor(
                            Color.argb((alpha * 255).toInt(), 0, 0, 0)
                        )
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    activity.updateWallpaperLayer()
                }
            })
        }
        dimCard.addView(dimSeekBar)

        val dimHint = TextView(context).apply {
            text = "Tip: 40%–50% works best for light backgrounds"
            textSize = 11f
            setTextColor(tintedColor(themeCoordinator.textColor, 120))
            setPadding(0, activity.dp(2), 0, 0)
        }
        dimCard.addView(dimHint)
        contentLayout.addView(dimCard)

        scrollView.addView(contentLayout)
        root.addView(scrollView)

        dialog.setContentView(root)
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setOnShowListener {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}

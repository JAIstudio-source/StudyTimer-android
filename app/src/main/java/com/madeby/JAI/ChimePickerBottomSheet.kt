package com.madeby.JAI

import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Bottom sheet for picking the timer completion bell sound.
 * Includes interactive play previews for each sound.
 */
object ChimePickerBottomSheet {

    fun show(activity: MainActivity, onSelected: (String) -> Unit = {}) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val theme = activity.themeCoordinator
        val isDark = theme.isDarkMode()
        val primary = theme.primaryColor
        val textColor = theme.textColor

        fun dp(v: Int): Int = activity.dp(v)
        fun dpF(v: Float): Int = (v * activity.resources.displayMetrics.density).toInt()
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

        // Header: Title + Close Button
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(activity).apply {
            text = "Timer Completion Bell"
            textSize = 18f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(textColor)
        }
        val subTv = TextView(activity).apply {
            text = "Choose the sound you hear when study or break finishes"
            textSize = 12.5f
            typeface = theme.getAppTypeface(isBold = false)
            setTextColor(tintedColor(textColor, 170))
        }
        titleCol.addView(titleTv)
        titleCol.addView(subTv)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(tintedColor(textColor, 160))
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(titleCol)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        val chimeListContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun refreshList() {
            chimeListContainer.removeAllViews()
            val currentSelected = StudyAudioManager.getSelectedChime(activity)

            for (chime in StudyAudioManager.CHIME_OPTIONS) {
                val isSelected = chime.id == currentSelected

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        if (isSelected) {
                            setColor(tintedColor(primary, 35))
                            setStroke(dpF(1.5f).coerceAtLeast(1), primary)
                        } else {
                            setColor(if (isDark) 0x12FFFFFF.toInt() else 0x08000000.toInt())
                            setStroke(dp(1), if (isDark) 0x18FFFFFF.toInt() else 0x0F000000.toInt())
                        }
                    }
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        StudyAudioManager.setSelectedChime(activity, chime.id)
                        StudyAudioManager.playChimePreview(activity, chime.id)
                        refreshList()
                        onSelected(chime.id)
                    }
                }

                // Play Preview Button
                val previewBtn = TextView(activity).apply {
                    text = "▶"
                    textSize = 13f
                    setTextColor(primary)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(tintedColor(primary, 40))
                    }
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        marginEnd = dp(12)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        StudyAudioManager.playChimePreview(activity, chime.id)
                    }
                }
                row.addView(previewBtn)

                val infoCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val nameTv = TextView(activity).apply {
                    text = chime.displayName
                    textSize = 14f
                    typeface = theme.getAppTypeface(isBold = isSelected)
                    setTextColor(if (isSelected) primary else textColor)
                }

                val descTv = TextView(activity).apply {
                    text = chime.description
                    textSize = 11.5f
                    typeface = theme.getAppTypeface(isBold = false)
                    setTextColor(tintedColor(textColor, 150))
                }
                infoCol.addView(nameTv)
                infoCol.addView(descTv)
                row.addView(infoCol)

                val radioIcon = TextView(activity).apply {
                    text = if (isSelected) "●" else "○"
                    textSize = 14f
                    setTextColor(if (isSelected) primary else tintedColor(textColor, 120))
                }
                row.addView(radioIcon)

                chimeListContainer.addView(row)
            }
        }

        refreshList()

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
            )
            isFillViewport = true
        }
        scroll.addView(chimeListContainer)
        root.addView(scroll)

        dialog.setContentView(root)
        dialog.window?.let { win ->
            win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
            win.setWindowAnimations(android.R.style.Animation_InputMethod)
        }
        dialog.show()
    }
}

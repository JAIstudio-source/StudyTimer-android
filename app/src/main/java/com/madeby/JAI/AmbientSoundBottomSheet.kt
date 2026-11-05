package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Bottom sheet for choosing and controlling ambient focus sounds.
 * Plain, simple language with zero technical jargon.
 */
object AmbientSoundBottomSheet {

    fun show(activity: MainActivity, onUpdated: () -> Unit = {}) {
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

        // Header Row: Title + Close Button
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(activity).apply {
            text = "Background Focus Sounds"
            textSize = 18f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(textColor)
        }
        val subTv = TextView(activity).apply {
            text = "Calming sounds to help you stay focused"
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

        // Master Switch Card
        val masterCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0x1EFFFFFF.toInt() else 0x0C000000.toInt())
                setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0x14000000.toInt())
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val switchLabelCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val switchTitle = TextView(activity).apply {
            text = "Play Sounds While Studying"
            textSize = 14.5f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(textColor)
        }
        val switchSub = TextView(activity).apply {
            text = "Plays automatically during active focus"
            textSize = 12f
            typeface = theme.getAppTypeface(isBold = false)
            setTextColor(tintedColor(textColor, 160))
        }
        switchLabelCol.addView(switchTitle)
        switchLabelCol.addView(switchSub)

        val soundListContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        val volumeContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isDark) 0x1EFFFFFF.toInt() else 0x0C000000.toInt())
                setStroke(dp(1), if (isDark) 0x22FFFFFF.toInt() else 0x14000000.toInt())
            }
            setPadding(dp(14), dp(10), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val isCurrentlyEnabled = StudyAudioManager.isAmbientEnabled(activity)

        val masterSwitch = SwitchMaterial(activity).apply {
            isChecked = isCurrentlyEnabled
            setOnCheckedChangeListener { _, isChecked ->
                StudyAudioManager.setAmbientEnabled(activity, isChecked)
                volumeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                soundListContainer.alpha = if (isChecked) 1.0f else 0.5f

                if (isChecked) {
                    if (activity.currentTimerState == TimerState.STUDYING) {
                        StudyAudioManager.startAmbient(activity)
                    } else {
                        // Play preview briefly so user knows it works
                        StudyAudioManager.startAmbient(activity)
                    }
                } else {
                    StudyAudioManager.stopAmbient()
                }
                onUpdated()
            }
        }
        masterCard.addView(switchLabelCol)
        masterCard.addView(masterSwitch)
        root.addView(masterCard)

        // Volume Slider
        val currentVol = StudyAudioManager.getAmbientVolume(activity)
        val volPct = (currentVol * 100).toInt()
        val volumeLabel = TextView(activity).apply {
            text = "Sound Volume: $volPct%"
            textSize = 13f
            typeface = theme.getAppTypeface(isBold = true)
            setTextColor(tintedColor(textColor, 220))
            setPadding(0, 0, 0, dp(4))
        }
        val volumeSeek = SeekBar(activity).apply {
            max = 100
            progress = volPct
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val v = (progress.coerceAtLeast(5)) / 100f
                        StudyAudioManager.setAmbientVolume(activity, v)
                        volumeLabel.text = "Sound Volume: $progress%"
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        volumeContainer.addView(volumeLabel)
        volumeContainer.addView(volumeSeek)
        volumeContainer.visibility = if (isCurrentlyEnabled) View.VISIBLE else View.GONE
        root.addView(volumeContainer)

        // Sound Selection List
        val listScroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(280)
            )
            isFillViewport = true
        }

        fun refreshSoundList() {
            soundListContainer.removeAllViews()
            val selectedId = StudyAudioManager.getSelectedAmbient(activity)

            for (sound in StudyAudioManager.AMBIENT_OPTIONS) {
                val isSelected = sound.id == selectedId

                val itemRow = LinearLayout(activity).apply {
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
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        StudyAudioManager.changeAmbientSound(activity, sound.id)
                        if (!StudyAudioManager.isAmbientEnabled(activity)) {
                            // Automatically enable when user taps a specific sound
                            StudyAudioManager.setAmbientEnabled(activity, true)
                            masterSwitch.isChecked = true
                            volumeContainer.visibility = View.VISIBLE
                        }
                        refreshSoundList()
                        onUpdated()
                    }
                }

                val emojiTv = TextView(activity).apply {
                    text = sound.iconEmoji
                    textSize = 20f
                    setPadding(0, 0, dp(12), 0)
                }

                val textCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val nameTv = TextView(activity).apply {
                    text = sound.displayName
                    textSize = 14f
                    typeface = theme.getAppTypeface(isBold = isSelected)
                    setTextColor(if (isSelected) primary else textColor)
                }

                val descTv = TextView(activity).apply {
                    text = sound.description
                    textSize = 11.5f
                    typeface = theme.getAppTypeface(isBold = false)
                    setTextColor(tintedColor(textColor, 150))
                }
                textCol.addView(nameTv)
                textCol.addView(descTv)

                val indicator = TextView(activity).apply {
                    text = if (isSelected) "●" else "○"
                    textSize = 14f
                    setTextColor(if (isSelected) primary else tintedColor(textColor, 120))
                }

                itemRow.addView(emojiTv)
                itemRow.addView(textCol)
                itemRow.addView(indicator)
                soundListContainer.addView(itemRow)
            }
        }

        refreshSoundList()
        soundListContainer.alpha = if (isCurrentlyEnabled) 1.0f else 0.5f
        listScroll.addView(soundListContainer)
        root.addView(listScroll)

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

package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeeklySummaryShareHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private val handler get() = host.handler

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)
    private fun rippleBackground(color: Int) = host.rippleBackground(color)
    private fun getString(resId: Int) = host.getString(resId)



    fun showCustomizeHighlightsDialog() {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Customize Highlights"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Pin your favorite metrics (minimum 2). Unpinned cards will be hidden to save space."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 12f
            setPadding(0, dp(4), 0, dp(14))
        })

        data class HighlightOption(val key: String, val title: String, val subtitle: String)

        val options = listOf(
            HighlightOption("WEEKLY_TREND", "Weekly Trend", "Compare this week vs last week"),
            HighlightOption("ACTIVE_DAYS", "Active Days", "Track active days and streak count"),
            HighlightOption("BEST_DAY", "Best Day", "Your highest study day of all time"),
            HighlightOption("RECORD_WEEK", "Best Week", "All-time highest study week"),
            HighlightOption("GOAL_SUCCESS", "Daily Goal Hits", "Days where daily target was met"),
            HighlightOption("AVG_SESSION", "Average Session", "Average duration per study session")
        )

        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val pinnedPrefsJson = sharedPrefs.getString("pinned_highlights_order", null)
        val currentPinned = mutableSetOf<String>()
        if (!pinnedPrefsJson.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(pinnedPrefsJson)
                for (i in 0 until arr.length()) currentPinned.add(arr.getString(i))
            } catch (_: Exception) {
                currentPinned.addAll(listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK"))
            }
        } else {
            currentPinned.addAll(listOf("WEEKLY_TREND", "ACTIVE_DAYS", "BEST_DAY", "RECORD_WEEK"))
        }

        val scroll = ScrollView(host).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (host.resources.displayMetrics.heightPixels * 0.40f).toInt())
        }
        val optionsBox = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(optionsBox)

        fun renderOptions() {
            optionsBox.removeAllViews()
            for (opt in options) {
                val isChecked = currentPinned.contains(opt.key)
                val card = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                    setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        if (isChecked) {
                            if (currentPinned.size <= 2) {
                                AppToast.show(host, "Keep at least 2 highlights pinned")
                            } else {
                                currentPinned.remove(opt.key)
                                renderOptions()
                            }
                        } else {
                            currentPinned.add(opt.key)
                            renderOptions()
                        }
                    }
                }

                val checkBadge = TextView(host).apply {
                    text = if (isChecked) "Pinned" else "Hidden"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(if (isChecked) themeCoordinator.primaryColor else 0xFF475569.toInt())
                    }
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                }
                card.addView(checkBadge)

                val col = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(10), 0, dp(4), 0) }
                }
                col.addView(TextView(host).apply {
                    text = opt.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                col.addView(TextView(host).apply {
                    text = opt.subtitle
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })
                card.addView(col)
                optionsBox.addView(card)
            }
        }
        renderOptions()
        content.addView(scroll)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }
        btnRow.addView(Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(host).apply {
            text = "Save"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                if (currentPinned.size < 2) {
                    AppToast.show(host, "Select at least 2 highlights")
                    return@setOnClickListener
                }
                val arr = org.json.JSONArray()
                for (opt in options) {
                    if (currentPinned.contains(opt.key)) arr.put(opt.key)
                }
                sharedPrefs.edit().putString("pinned_highlights_order", arr.toString()).apply()
                host.refreshStatsPanel()
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showWeeklyTrendDetailDialog(thisWeek: Long, prevWeek: Long, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Weekly Trend Comparison"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val trendDiff = if (prevWeek > 0L) ((thisWeek - prevWeek).toFloat() / prevWeek.toFloat() * 100f).toInt() else if (thisWeek > 0L) 100 else 0
        val trendText = when {
            trendDiff > 0 -> "+${trendDiff}% increase compared to last week"
            trendDiff < 0 -> "${trendDiff}% compared to last week"
            else -> "Identical study time to last week"
        }

        content.addView(TextView(host).apply {
            text = "This Week: ${thisWeek / 3600}h ${(thisWeek % 3600) / 60}m · Last Week: ${prevWeek / 3600}h ${(prevWeek % 3600) / 60}m\n$trendText"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val cal = Calendar.getInstance()
        val mondayOffset = WeekHelper.mondayOffset(cal)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDateStr = sdf.format(Date())

        val listContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        for (i in 0..6) {
            val cThis = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset + i) }
            val cPrev = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset - 7 + i) }
            val dateThisStr = sdf.format(cThis.time)
            val isToday = dateThisStr == todayDateStr
            val fThis = snap.dayFocus[dateThisStr] ?: 0L
            val fPrev = snap.dayFocus[sdf.format(cPrev.time)] ?: 0L

            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isToday) {
                    GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        val cardBg = if (themeCoordinator.isDarkMode()) {
                            if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF181A24.toInt()
                        } else {
                            0xFFEDF0F5.toInt()
                        }
                        setColor(cardBg)
                        setStroke(dp(1), themeCoordinator.primaryColor)
                    }
                } else {
                    themeCoordinator.createCardBackground(10f)
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(4))
                }
            }
            val dayNameText = if (isToday) "${weekdays[i]} (Today)" else weekdays[i]
            row.addView(TextView(host).apply {
                text = dayNameText
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(host).apply {
                val thisStr = "${fThis / 3600}h ${(fThis % 3600) / 60}m"
                val prevStr = "${fPrev / 3600}h ${(fPrev % 3600) / 60}m"
                text = "$thisStr (prev: $prevStr)"
                setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                alpha = if (isToday) 1f else 0.65f
                textSize = 12f
                typeface = Typeface.MONOSPACE
            })
            listContainer.addView(row)
        }
        content.addView(listContainer)

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(14), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showActiveDaysDetailDialog(activeDays: Int, longestStreak: Int, currentStreak: Int, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Study Consistency"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Building a daily study habit is the single most effective way to retain knowledge."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(14))
        })

        fun makeStatRow(label: String, value: String): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground(12f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(6))
                }
                addView(TextView(host).apply {
                    text = label
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(host).apply {
                    text = value
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
            }
        }

        content.addView(makeStatRow("Current Active Streak", "$currentStreak days"))
        content.addView(makeStatRow("Longest Recorded Streak", "$longestStreak days"))
        content.addView(makeStatRow("Total Active Study Days", "$activeDays days"))

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(12), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showBestDayDetailDialog(bestDayLabel: String, bestDaySecs: Long, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "All-Time Best Day"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            val sub = if (bestDaySecs > 0L) "Your single highest study day of all time was on $bestDayLabel." else "Keep studying to set your all-time best study day record."
            text = sub
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(10))
        })

        if (bestDaySecs > 0L) {
            val recordCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(12))
                }
                addView(TextView(host).apply {
                    text = "RECORD FOCUS TIME"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                addView(TextView(host).apply {
                    text = "${bestDaySecs / 3600}h ${(bestDaySecs % 3600) / 60}m studied"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 20f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(6), 0, dp(3))
                })
                addView(TextView(host).apply {
                    text = "Achieved on $bestDayLabel"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
            }
            content.addView(recordCard)
        }

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(6), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showRecordWeekDetailDialog(bestWeekLabel: String, bestWeekSecs: Long, longestStreak: Int, snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Best Study Week"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(host).apply {
            text = "Your all-time highest weekly study record in StudyTimer:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12.5f
            setPadding(0, dp(4), 0, dp(14))
        })

        fun makeRecordCard(title: String, mainVal: String, subVal: String): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(14f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(8))
                }
                addView(TextView(host).apply {
                    text = title
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                addView(TextView(host).apply {
                    text = mainVal
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
                if (subVal.isNotBlank()) {
                    addView(TextView(host).apply {
                        text = subVal
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 11.5f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
            }
        }

        val weekRecordStr = if (bestWeekSecs > 0L) "${bestWeekSecs / 3600}h ${(bestWeekSecs % 3600) / 60}m studied" else "No best week yet"
        val weekLabelStr = if (bestWeekSecs > 0L) "Week of $bestWeekLabel" else "Study consistently to set your weekly record"
        content.addView(makeRecordCard("BEST STUDY WEEK", weekRecordStr, weekLabelStr))

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showGoalSuccessDetailDialog(snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Daily Goal Success"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val goalHits = snap.goalHits
        content.addView(TextView(host).apply {
            text = "You have met your daily study target on $goalHits days. Keep setting realistic daily goals in the Planner to build long-term focus habits."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showAvgSessionDetailDialog(snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Session Breakdown"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        var totalSessionCount = 0
        var totalDuration = 0L
        var longestSession = 0L
        val entries = TimelineLogger.load(host)
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val allDates = entries.mapNotNull { if (it.timestamp > 0L) dateSdf.format(Date(it.timestamp)) else null }.distinct()
        for (dStr in allDates) {
            val (sessions, _) = host.dayBlocks(dStr)
            for (s in sessions) {
                if (s.secs >= 60L) {
                    totalSessionCount++
                    totalDuration += s.secs
                    if (s.secs > longestSession) longestSession = s.secs
                }
            }
        }
        val avgMins = if (totalSessionCount > 0) ((totalDuration / totalSessionCount) / 60).toInt() else 0

        content.addView(TextView(host).apply {
            text = "Average Session Duration: ${avgMins}m\nTotal Completed Sessions: $totalSessionCount\nLongest Single Session: ${longestSession / 3600}h ${(longestSession % 3600) / 60}m"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 12.5f
            setLineSpacing(dp(3).toFloat(), 1.15f)
            setPadding(0, dp(4), 0, dp(14))
        })

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 20f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(0, dp(8), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}

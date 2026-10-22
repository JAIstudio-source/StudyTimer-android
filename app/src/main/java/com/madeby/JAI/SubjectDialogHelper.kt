package com.madeby.JAI

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubjectDialogHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator

    private fun dp(v: Int): Int = host.dp(v)
    private fun tintedColor(color: Int, alpha: Int): Int = host.tintedColor(color, alpha)

    fun showSubjectPickerDialog(allowActiveSwitch: Boolean = false) {
        val isBreak = host.currentTimerState == TimerState.BREAK
        if (!allowActiveSwitch && !isBreak && host.currentTimerState != TimerState.IDLE) {
            DeveloperToolsHelper.showThemedConfirmDialog(
                activity = host,
                themeCoordinator = themeCoordinator,
                title = "⏱️ Active Session Running",
                message = "Subject cannot be changed during an active study session.\n\nYou can change subjects during breaks, between Pomodoro rounds, or when the timer is idle.",
                confirmText = "OK",
                isDestructive = false,
                onCancel = {}
            ) {}
            return
        }
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            var selectionMade = false
            dialog.setOnDismissListener {
                if (!selectionMade && host.appPrefs.getBoolean("pomo_grace_realloc_pending", false)) {
                    val origSubId = host.appPrefs.getString("pomo_grace_orig_subject_id", null) ?: SubjectTagManager.getSelectedSubject(host).id
                    SubjectTagManager.finalizePomoGraceReallocation(host, origSubId)
                    host.invalidateStatsCache()
                }
            }

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(22), dp(22), dp(24))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(420)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "Select Study Subject"
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(title)

            val subtitle = TextView(host).apply {
                text = "Tap to select • Tap 📖 Chapters to manage topics"
                textSize = 12f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(subtitle)

            val maxScrollHeight = (host.resources.displayMetrics.heightPixels * 0.42f).toInt()
            val scrollContainer = object : ScrollView(host) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val customHeightSpec = View.MeasureSpec.makeMeasureSpec(maxScrollHeight, View.MeasureSpec.AT_MOST)
                    super.onMeasure(widthMeasureSpec, customHeightSpec)
                }
            }.apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val subjectsListContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val currentSelectedSubj = SubjectTagManager.getSelectedSubject(host)
            val subjectsList = SubjectTagManager.getAllSubjects(host)

            fun isLightColor(color: Int): Boolean {
                val r = Color.red(color) / 255.0
                val g = Color.green(color) / 255.0
                val b = Color.blue(color) / 255.0
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                return lum > 0.65
            }

            fun createSubjectCard(subj: SubjectTag, isSelected: Boolean): View {
                val baseColor = try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                val contentTextColor = if (isLightColor(baseColor)) 0xFF0F172A.toInt() else Color.WHITE
                return Button(host).apply {
                    text = if (isSelected) "✓ ${subj.name}" else subj.name
                    textSize = 12.5f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    isAllCaps = false
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(contentTextColor)
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    background = GradientDrawable().apply {
                        setColor(baseColor)
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setStroke(dp(2), if (isLightColor(baseColor)) 0xFF0F172A.toInt() else Color.WHITE)
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                    setOnClickListener {
                        selectionMade = true
                        val wasGracePending = host.appPrefs.getBoolean("pomo_grace_realloc_pending", false)
                        if (wasGracePending) {
                            SubjectTagManager.finalizePomoGraceReallocation(host, subj.id)
                        }
                        SubjectTagManager.setSelectedSubject(host, subj.id)
                        if (host.currentTimerState != TimerState.IDLE) {
                            TimelineLogger.record(host, host.currentTimerState, subId = subj.id, subName = subj.name, subColor = subj.colorHex)
                            StudyWidgetProvider.refresh(host)
                            host.invalidateStatsCache()
                        }
                        val subTopics = SubjectTagManager.getSubTopics(host, subj.id)
                        dialog.dismiss()
                        if (subTopics.isNotEmpty()) {
                            showSubTopicPickerDialog(subj) {
                                host.navigateToPanel(AppPanel.FOCUS)
                            }
                        } else {
                            SubjectTagManager.setSelectedSubTopic(host, subj.id, null)
                            host.navigateToPanel(AppPanel.FOCUS)
                            AppToast.show(host, "Subject selected: ${subj.name}")
                        }
                    }
                    setOnLongClickListener {
                        dialog.dismiss()
                        showSubjectOptionsModal(subj)
                        true
                    }
                }
            }

            // Render subjects in pairs (2 blocks per row)
            for (i in subjectsList.indices step 2) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                }

                val subj1 = subjectsList[i]
                val card1 = createSubjectCard(subj1, subj1.id == currentSelectedSubj.id)
                (card1.layoutParams as LinearLayout.LayoutParams).apply {
                    if (i + 1 < subjectsList.size) setMargins(0, 0, dp(4), 0)
                }
                row.addView(card1)

                if (i + 1 < subjectsList.size) {
                    val subj2 = subjectsList[i + 1]
                    val card2 = createSubjectCard(subj2, subj2.id == currentSelectedSubj.id)
                    (card2.layoutParams as LinearLayout.LayoutParams).apply {
                        setMargins(dp(4), 0, 0, 0)
                    }
                    row.addView(card2)
                } else {
                    // Empty space holder to keep 2-column balanced
                    row.addView(View(host).apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                            setMargins(dp(4), 0, 0, 0)
                        }
                    })
                }

                subjectsListContainer.addView(row)
            }

            scrollContainer.addView(subjectsListContainer)
            container.addView(scrollContainer)

            val bottomActionRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(14), 0, 0)
                }
            }

            val addBtn = TextView(host).apply {
                text = "+ Add"
                textSize = 12f
                gravity = Gravity.CENTER
                isSingleLine = true
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 14f)
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    setMargins(0, 0, dp(3), 0)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    showAddCustomSubjectDialog()
                }
            }
            bottomActionRow.addView(addBtn)

            val chaptersBtn = TextView(host).apply {
                text = "📖 Chapters"
                textSize = 12f
                gravity = Gravity.CENTER
                isSingleLine = true
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.accentColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, 35), 14f)
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1.15f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    showSubTopicPickerDialog(currentSelectedSubj) {
                        host.navigateToPanel(AppPanel.FOCUS)
                    }
                }
            }
            bottomActionRow.addView(chaptersBtn)

            val manageBtn = TextView(host).apply {
                text = "Manage"
                textSize = 12f
                gravity = Gravity.CENTER
                isSingleLine = true
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 0.95f).apply {
                    setMargins(dp(3), 0, 0, 0)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    showManageSubjectsDialog()
                }
            }
            bottomActionRow.addView(manageBtn)

            container.addView(bottomActionRow)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showManageSubjectsDialog() {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(26f)
                setPadding(dp(20), dp(20), dp(20), dp(18))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(400)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "Manage Subjects"
                textSize = 17f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(title)

            val subtitle = TextView(host).apply {
                text = "Edit name, color, chapters, or remove subjects"
                textSize = 12f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.55f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            }
            container.addView(subtitle)

            val scrollContainer = ScrollView(host).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }

            val listContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
            }

            val subjectsList = SubjectTagManager.getAllSubjects(host)
            for (subj in subjectsList) {
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 18), 12f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                }

                val baseColor = try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                val colorDot = View(host).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(baseColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                        rightMargin = dp(10)
                    }
                }
                row.addView(colorDot)

                val nameView = TextView(host).apply {
                    text = subj.name
                    textSize = 13.5f
                    typeface = themeCoordinator.getAppTypeface(isBold = true)
                    setTextColor(themeCoordinator.textColor)
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(nameView)

                val editBtn = TextView(host).apply {
                    text = "Edit"
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        dialog.dismiss()
                        showEditSubjectDialog(subj) {
                            showManageSubjectsDialog()
                        }
                    }
                }
                row.addView(editBtn)

                val deleteBtn = TextView(host).apply {
                    text = "Delete"
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.parseColor("#EF4444"))
                    setPadding(dp(8), dp(4), dp(4), dp(4))
                    setOnClickListener {
                        dialog.dismiss()
                        showDeleteSubjectConfirmDialog(subj)
                    }
                }
                row.addView(deleteBtn)

                listContainer.addView(row)
            }

            scrollContainer.addView(listContainer)
            container.addView(scrollContainer)

            val closeBtn = Button(host).apply {
                text = "Done"
                textSize = 13.5f
                typeface = themeCoordinator.getAppTypeface(isBold = true)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                    setMargins(0, dp(6), 0, 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showSubjectPickerDialog()
                }
            }
            container.addView(closeBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showSubjectOptionsModal(subj: SubjectTag) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(20), dp(20), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(dp(360)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "${subj.iconEmoji} ${subj.name}"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val subTopicsBtn = Button(host).apply {
                text = "Chapters & Sub-Topics"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(10))
                }
                setOnClickListener {
                    dialog.dismiss()
                    showSubTopicPickerDialog(subj)
                }
            }
            container.addView(subTopicsBtn)

            val editBtn = Button(host).apply {
                text = "Edit Subject"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor })
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(10))
                }
                setOnClickListener {
                    dialog.dismiss()
                    showEditSubjectDialog(subj) {
                        showSubjectPickerDialog()
                    }
                }
            }
            container.addView(editBtn)

            if (subj.id != "general") {
                val deleteBtn = Button(host).apply {
                    text = "Delete Subject"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(0xFFEF4444.toInt())
                    background = themeCoordinator.createGlassChip(tintedColor(0xFFEF4444.toInt(), 30), 12f)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        dialog.dismiss()
                        showDeleteSubjectConfirmDialog(subj)
                    }
                }
                container.addView(deleteBtn)
            }

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showSubTopicPickerDialog(subj: SubjectTag, onSelected: ((String?) -> Unit)? = null) {
        val isBreak = host.currentTimerState == TimerState.BREAK
        if (!isBreak && host.currentTimerState != TimerState.IDLE) {
            DeveloperToolsHelper.showThemedConfirmDialog(
                activity = host,
                themeCoordinator = themeCoordinator,
                title = "⏱️ Active Session Running",
                message = "Chapter / subtopic cannot be changed during an active study session.\n\nYou can change chapters or topics during breaks, between Pomodoro rounds, or when the timer is idle.",
                confirmText = "OK",
                isDestructive = false,
                onCancel = {}
            ) {}
            return
        }
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(26f)
                setPadding(dp(22), dp(22), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(400)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "${subj.iconEmoji} ${subj.name}"
                textSize = 17.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
            }
            container.addView(title)

            val subtitle = TextView(host).apply {
                text = "Select Chapter / Sub-Topic"
                textSize = 12.5f
                setTextColor(tintedColor(themeCoordinator.textColor, 140))
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, dp(14))
            }
            container.addView(subtitle)

            val listScroll = ScrollView(host).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(260).coerceAtMost((host.resources.displayMetrics.heightPixels * 0.40f).toInt())
                )
            }
            val topicsContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val currentSubTopic = SubjectTagManager.getSelectedSubTopic(host, subj.id)
            val allTopics = SubjectTagManager.getSubTopics(host, subj.id).toMutableList()
            val studyTotals = SubjectTagManager.getSubTopicStudyTotals(host, subj.id)
            var selectionMade = false

            fun formatTopicDuration(secs: Long): String {
                if (secs <= 0L) return ""
                val h = secs / 3600
                val m = (secs % 3600) / 60
                val s = secs % 60
                return when {
                    h > 0 -> "${h}h ${m}m"
                    m > 0 -> "${m}m"
                    else -> "${s}s"
                }
            }

            fun populateTopicRows() {
                topicsContainer.removeAllViews()

                // Row for All / General (Unassigned)
                val allRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val isSelected = currentSubTopic.isNullOrBlank()
                    background = if (isSelected) {
                        GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor })
                        }
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 12f)
                    }
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                    setOnClickListener {
                        selectionMade = true
                        SubjectTagManager.setSelectedSubTopic(host, subj.id, null)
                        dialog.dismiss()
                        onSelected?.invoke(null)
                        host.buildCurrentPanel()
                        AppToast.show(host, "Studying: ${subj.name} (All Topics)")
                    }
                }
                allRow.addView(TextView(host).apply {
                    text = if (currentSubTopic.isNullOrBlank()) "✓ All Chapters / Topics" else "All Chapters / Topics"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (currentSubTopic.isNullOrBlank()) Color.WHITE else themeCoordinator.textColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                topicsContainer.addView(allRow)

                for (topic in allTopics) {
                    val isSelected = topic.equals(currentSubTopic, ignoreCase = true)
                    val durSecs = studyTotals[topic] ?: 0L
                    val durText = formatTopicDuration(durSecs)

                    val row = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = if (isSelected) {
                            GradientDrawable().apply {
                                cornerRadius = dp(12).toFloat()
                                setColor(try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor })
                            }
                        } else {
                            themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 12f)
                        }
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(6))
                        }
                        setOnClickListener {
                            selectionMade = true
                            SubjectTagManager.setSelectedSubTopic(host, subj.id, topic)
                            dialog.dismiss()
                            onSelected?.invoke(topic)
                            host.buildCurrentPanel()
                            AppToast.show(host, "Studying: ${subj.name} • $topic")
                        }
                    }

                    row.addView(TextView(host).apply {
                        text = if (isSelected) "✓ $topic" else topic
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    if (durText.isNotEmpty()) {
                        row.addView(TextView(host).apply {
                            text = durText
                            textSize = 12f
                            setTextColor(if (isSelected) Color.WHITE else tintedColor(themeCoordinator.textColor, 160))
                            setPadding(dp(6), 0, dp(6), 0)
                        })
                    }

                    val delBtn = TextView(host).apply {
                        text = "✕"
                        textSize = 12f
                        setTextColor(if (isSelected) Color.WHITE else 0xFFEF4444.toInt())
                        setPadding(dp(8), dp(4), dp(4), dp(4))
                        setOnClickListener {
                            SubjectTagManager.removeSubTopic(host, subj.id, topic)
                            allTopics.remove(topic)
                            if (topic.equals(currentSubTopic, ignoreCase = true)) {
                                SubjectTagManager.setSelectedSubTopic(host, subj.id, null)
                            }
                            populateTopicRows()
                        }
                    }
                    row.addView(delBtn)

                    topicsContainer.addView(row)
                }
            }

            populateTopicRows()
            listScroll.addView(topicsContainer)
            container.addView(listScroll)

            // Add new Sub-Topic Row
            val addRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, 0)
            }
            val input = EditText(host).apply {
                hint = "+ Add chapter / topic"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            val addBtn = Button(host).apply {
                text = "Add"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        if (SubjectTagManager.addSubTopic(host, subj.id, name)) {
                            allTopics.add(name)
                            input.setText("")
                            populateTopicRows()
                        } else {
                            AppToast.show(host, "Topic already exists")
                        }
                    }
                }
            }
            addRow.addView(input)
            addRow.addView(addBtn)
            container.addView(addRow)

            dialog.setOnDismissListener {
                if (!selectionMade) {
                    selectionMade = true
                    SubjectTagManager.setSelectedSubTopic(host, subj.id, null)
                    onSelected?.invoke(null)
                    host.buildCurrentPanel()
                    AppToast.show(host, "Studying: ${subj.name} (All Topics)")
                }
            }

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showEditSubjectDialog(subj: SubjectTag, onUpdated: (() -> Unit)? = null) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(20), dp(20), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(380)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "Edit Subject"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val input = android.widget.EditText(host).apply {
                setText(subj.name)
                hint = "Subject Name (max 25 chars)"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                filters = arrayOf(android.text.InputFilter.LengthFilter(25))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            container.addView(input)

            var selectedEmoji = subj.iconEmoji
            var selectedColorHex = subj.colorHex

            val emojiHeader = TextView(host).apply {
                text = "Select Icon Emoji"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(4))
            }
            container.addView(emojiHeader)

            val presetEmojis = mutableListOf("📚", "💻", "📐", "🧪", "📜", "⚛️", "🎨", "✍️", "🎧", "🎯", "💡", "🧠", "📖", "🔬", "📊", "🏆")
            if (selectedEmoji.isNotBlank() && !presetEmojis.contains(selectedEmoji)) {
                presetEmojis.add(0, selectedEmoji)
            }
            val emojiScroll = android.widget.HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(6))
            }
            val emojiLayout = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }

            fun refreshEmojiViews() {
                emojiLayout.removeAllViews()
                for (em in presetEmojis) {
                    val isMatch = em == selectedEmoji
                    val eView = TextView(host).apply {
                        text = em
                        textSize = 18f
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            setColor(if (isMatch) tintedColor(themeCoordinator.primaryColor, 60) else tintedColor(themeCoordinator.textColor, 15))
                            cornerRadius = dp(10).toFloat()
                            setStroke(dp(2), if (isMatch) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(6), 0) }
                        setOnClickListener {
                            selectedEmoji = em
                            refreshEmojiViews()
                        }
                    }
                    emojiLayout.addView(eView)
                }

                // Custom Emoji Add Button at the end of the line
                val customBtn = TextView(host).apply {
                    text = "➕"
                    textSize = 15f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(tintedColor(themeCoordinator.primaryColor, 30))
                        cornerRadius = dp(10).toFloat()
                        setStroke(dp(2), tintedColor(themeCoordinator.primaryColor, 100))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(6), 0) }
                    setOnClickListener {
                        showCustomEmojiInputDialog(selectedEmoji) { newEmoji ->
                            selectedEmoji = newEmoji
                            if (!presetEmojis.contains(newEmoji)) {
                                presetEmojis.add(newEmoji)
                            }
                            refreshEmojiViews()
                        }
                    }
                }
                emojiLayout.addView(customBtn)
            }

            refreshEmojiViews()
            emojiScroll.addView(emojiLayout)
            container.addView(emojiScroll)

            val previewRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(14), 0, dp(8))
            }
            val colorPreviewCircle = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { setMargins(0, 0, dp(10), 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(try { Color.parseColor(selectedColorHex) } catch (_: Exception) { themeCoordinator.primaryColor })
                }
            }
            previewRow.addView(colorPreviewCircle)

            val colorLabel = TextView(host).apply {
                text = "Subject Color: $selectedColorHex"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            previewRow.addView(colorLabel)
            container.addView(previewRow)

            val swatchesScroll = android.widget.HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(10))
            }
            val swatchesLayout = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
            val swatchViews = ArrayList<View>()
            val presetColors = listOf("#6366F1", "#EC4899", "#F97316", "#06B6D4", "#10B981", "#EAB308", "#8B5CF6", "#EF4444", "#3B82F6", "#14B8A6")

            fun refreshSubjectSwatchBorders() {
                for ((idx, sView) in swatchViews.withIndex()) {
                    val hex = presetColors[idx]
                    val isMatch = hex.equals(selectedColorHex, true)
                    (sView.background as? GradientDrawable)?.setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                }
            }

            val curColorInt = try { Color.parseColor(selectedColorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
            val initHsv = FloatArray(3)
            Color.colorToHSV(curColorInt, initHsv)

            val hueHeader = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(4))
            }
            val hueTitle = TextView(host).apply {
                text = "Fine-Tune Hue Slider"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val hueValLabel = TextView(host).apply {
                text = "${initHsv[0].toInt()}°"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
            }
            hueHeader.addView(hueTitle)
            hueHeader.addView(hueValLabel)

            val hueSeekBar = android.widget.SeekBar(host).apply {
                max = 360
                progress = initHsv[0].toInt()
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                        hueValLabel.text = "$prog°"
                        if (fromUser) {
                            val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.70f, 0.95f))
                            selectedColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                            (colorPreviewCircle.background as? GradientDrawable)?.setColor(colorInt)
                            colorLabel.text = "Subject Color: $selectedColorHex"
                            refreshSubjectSwatchBorders()
                        }
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }

            for (hex in presetColors) {
                val swatch = View(host).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(8), 0) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        setStroke(dp(2), if (hex.equals(selectedColorHex, true)) Color.WHITE else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        selectedColorHex = hex
                        val cInt = Color.parseColor(hex)
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(cInt)
                        colorLabel.text = "Subject Color: $selectedColorHex"
                        val swatchHsv = FloatArray(3)
                        Color.colorToHSV(cInt, swatchHsv)
                        hueSeekBar.progress = swatchHsv[0].toInt()
                        hueValLabel.text = "${swatchHsv[0].toInt()}°"
                        refreshSubjectSwatchBorders()
                    }
                }
                swatchViews.add(swatch)
                swatchesLayout.addView(swatch)
            }
            swatchesScroll.addView(swatchesLayout)
            container.addView(swatchesScroll)
            container.addView(hueHeader)
            container.addView(hueSeekBar)

            val weeklyHeader = TextView(host).apply {
                text = "Weekly Goal Target (Hours / Week)"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(4))
            }
            container.addView(weeklyHeader)

            val currentWeeklyTarget = SubjectTagManager.getWeeklyTargetHours(host, subj.id)
            val weeklyInput = android.widget.EditText(host).apply {
                setText(if (currentWeeklyTarget > 0f) "${currentWeeklyTarget.toInt()}" else "")
                hint = "e.g. 10 (Optional target in hours)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }
            container.addView(weeklyInput)

            val topicsHeader = TextView(host).apply {
                text = "Chapters & Sub-Topics"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(4))
            }
            container.addView(topicsHeader)

            val topicsChipsLayout = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(2), 0, dp(6))
            }

            val currentSubTopics = SubjectTagManager.getSubTopics(host, subj.id).toMutableList()
            fun renderTopicChips() {
                topicsChipsLayout.removeAllViews()
                for (t in currentSubTopics) {
                    val chip = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                        setPadding(dp(10), dp(4), dp(8), dp(4))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, dp(6), dp(4))
                        }
                    }
                    chip.addView(TextView(host).apply {
                        text = t
                        textSize = 12f
                        setTextColor(themeCoordinator.textColor)
                    })
                    chip.addView(TextView(host).apply {
                        text = " ✕"
                        textSize = 11f
                        setTextColor(if (themeCoordinator.isDarkMode()) 0xFFEF4444.toInt() else 0xFFDC2626.toInt())
                        setOnClickListener {
                            currentSubTopics.remove(t)
                            SubjectTagManager.removeSubTopic(host, subj.id, t)
                            renderTopicChips()
                        }
                    })
                    topicsChipsLayout.addView(chip)
                }
            }
            renderTopicChips()
            container.addView(topicsChipsLayout)

            val addTopicRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(8))
            }
            val newTopicInput = android.widget.EditText(host).apply {
                hint = "+ Add chapter or topic"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }
            val addTopicBtn = TextView(host).apply {
                text = "Add"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener {
                    val tName = newTopicInput.text.toString().trim()
                    if (tName.isNotEmpty()) {
                        if (SubjectTagManager.addSubTopic(host, subj.id, tName)) {
                            currentSubTopics.add(tName)
                            newTopicInput.setText("")
                            renderTopicChips()
                        }
                    }
                }
            }
            addTopicRow.addView(newTopicInput)
            addTopicRow.addView(addTopicBtn)
            container.addView(addTopicRow)

            val saveBtn = Button(host).apply {
                text = "Save Changes"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(themeCoordinator.primaryColor)
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(16), 0, 0)
                }
                setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        AppToast.show(host, "Please enter a subject name")
                        return@setOnClickListener
                    }
                    val check = ProfanityFilter.checkSubjectName(name)
                    if (!check.isClean) {
                        AppToast.show(host, check.reason ?: "Subject name contains inappropriate words 🛡️")
                        return@setOnClickListener
                    }
                    val weeklyHours = weeklyInput.text.toString().trim().toFloatOrNull() ?: 0f
                    SubjectTagManager.setWeeklyTargetHours(host, subj.id, weeklyHours)

                    val updatedSubj = subj.copy(
                        name = check.sanitizedText,
                        iconEmoji = selectedEmoji,
                        colorHex = selectedColorHex,
                        subTopics = currentSubTopics,
                        weeklyTargetHours = weeklyHours
                    )
                    SubjectTagManager.updateSubject(host, updatedSubj)
                    dialog.dismiss()
                    AppToast.show(host, "Subject updated across history")
                    host.navigateToPanel(AppPanel.FOCUS)
                    onUpdated?.invoke()
                }
            }
            container.addView(saveBtn)

            val dialogScroll = android.widget.ScrollView(host).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                addView(container)
            }
            dialog.setContentView(dialogScroll)
            dialog.window?.setLayout(
                (host.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(380)),
                (host.resources.displayMetrics.heightPixels * 0.85f).toInt()
            )
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showDeleteSubjectConfirmDialog(subj: SubjectTag) {
        try {
            DialogFactory.showConfirmationDialog(
                context = host,
                themeCoordinator = themeCoordinator,
                title = "Delete '${subj.name}' Subject?",
                message = "Are you sure you want to remove ${subj.iconEmoji} ${subj.name}? Existing recorded stats for this subject will remain saved.",
                positiveText = "Delete",
                negativeText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    SubjectTagManager.removeSubject(host, subj.id)
                    host.navigateToPanel(AppPanel.FOCUS)
                    showSubjectPickerDialog()
                }
            )
        } catch (_: Exception) {}
    }

    fun showAddCustomSubjectDialog(onSubjectCreated: ((SubjectTag) -> Unit)? = null) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(20), dp(20), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(380)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "Add New Subject"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val input = android.widget.EditText(host).apply {
                hint = "Subject Name (max 25 chars)"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                filters = arrayOf(android.text.InputFilter.LengthFilter(25))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            container.addView(input)

            var selectedEmoji = "📚"

            val emojiHeader = TextView(host).apply {
                text = "Select Icon Emoji"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(4))
            }
            container.addView(emojiHeader)

            val presetEmojis = mutableListOf("📚", "💻", "📐", "🧪", "📜", "⚛️", "🎨", "✍️", "🎧", "🎯", "💡", "🧠", "📖", "🔬", "📊", "🏆")
            val emojiScroll = android.widget.HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(6))
            }
            val emojiLayout = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }

            fun refreshEmojiViews() {
                emojiLayout.removeAllViews()
                for (em in presetEmojis) {
                    val isMatch = em == selectedEmoji
                    val eView = TextView(host).apply {
                        text = em
                        textSize = 18f
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            setColor(if (isMatch) tintedColor(themeCoordinator.primaryColor, 60) else tintedColor(themeCoordinator.textColor, 15))
                            cornerRadius = dp(10).toFloat()
                            setStroke(dp(2), if (isMatch) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(6), 0) }
                        setOnClickListener {
                            selectedEmoji = em
                            refreshEmojiViews()
                        }
                    }
                    emojiLayout.addView(eView)
                }

                // Custom Emoji Add Button at the end of the line
                val customBtn = TextView(host).apply {
                    text = "➕"
                    textSize = 15f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(tintedColor(themeCoordinator.primaryColor, 30))
                        cornerRadius = dp(10).toFloat()
                        setStroke(dp(2), tintedColor(themeCoordinator.primaryColor, 100))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(6), 0) }
                    setOnClickListener {
                        showCustomEmojiInputDialog(selectedEmoji) { newEmoji ->
                            selectedEmoji = newEmoji
                            if (!presetEmojis.contains(newEmoji)) {
                                presetEmojis.add(newEmoji)
                            }
                            refreshEmojiViews()
                        }
                    }
                }
                emojiLayout.addView(customBtn)
            }

            refreshEmojiViews()
            emojiScroll.addView(emojiLayout)
            container.addView(emojiScroll)

            var selectedColorHex = SubjectTagManager.generateUniqueColor(host)

            val previewRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(14), 0, dp(8))
            }
            val colorPreviewCircle = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(selectedColorHex))
                }
            }
            previewRow.addView(colorPreviewCircle)

            val colorLabel = TextView(host).apply {
                text = "Subject Color: $selectedColorHex"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            previewRow.addView(colorLabel)
            container.addView(previewRow)

            // Preset Swatches Horizontal Row
            val swatchesScroll = android.widget.HorizontalScrollView(host).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(10))
            }
            val swatchesLayout = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val swatchViews = ArrayList<View>()
            val presetColors = listOf("#6366F1", "#EC4899", "#F97316", "#06B6D4", "#10B981", "#EAB308", "#8B5CF6", "#EF4444", "#3B82F6", "#14B8A6")

            fun refreshSubjectSwatchBorders() {
                for ((idx, sView) in swatchViews.withIndex()) {
                    val hex = presetColors[idx]
                    val isMatch = hex.equals(selectedColorHex, true)
                    (sView.background as? GradientDrawable)?.setStroke(dp(2), if (isMatch) Color.WHITE else Color.TRANSPARENT)
                }
            }

            val curColorInt = try { Color.parseColor(selectedColorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
            val initHsv = FloatArray(3)
            Color.colorToHSV(curColorInt, initHsv)

            val hueHeader = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(4))
            }
            val hueTitle = TextView(host).apply {
                text = "Fine-Tune Hue Slider"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val hueValLabel = TextView(host).apply {
                text = "${initHsv[0].toInt()}°"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
            }
            hueHeader.addView(hueTitle)
            hueHeader.addView(hueValLabel)

            val hueSeekBar = android.widget.SeekBar(host).apply {
                max = 360
                progress = initHsv[0].toInt()
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                        hueValLabel.text = "$prog°"
                        if (fromUser) {
                            val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.70f, 0.95f))
                            selectedColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                            (colorPreviewCircle.background as? GradientDrawable)?.setColor(colorInt)
                            colorLabel.text = "Subject Color: $selectedColorHex"
                            refreshSubjectSwatchBorders()
                        }
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }

            for (hex in presetColors) {
                val swatch = View(host).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        setStroke(dp(2), if (hex.equals(selectedColorHex, true)) Color.WHITE else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        selectedColorHex = hex
                        val cInt = Color.parseColor(hex)
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(cInt)
                        colorLabel.text = "Subject Color: $selectedColorHex"

                        val swatchHsv = FloatArray(3)
                        Color.colorToHSV(cInt, swatchHsv)
                        hueSeekBar.progress = swatchHsv[0].toInt()
                        hueValLabel.text = "${swatchHsv[0].toInt()}°"

                        refreshSubjectSwatchBorders()
                    }
                }
                swatchViews.add(swatch)
                swatchesLayout.addView(swatch)
            }
            swatchesScroll.addView(swatchesLayout)
            container.addView(swatchesScroll)
            container.addView(hueHeader)
            container.addView(hueSeekBar)

            val saveBtn = Button(host).apply {
                text = "Save Subject"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(themeCoordinator.primaryColor)
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(16), 0, 0)
                }
                setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        AppToast.show(host, "Please enter a subject name")
                        return@setOnClickListener
                    }
                    val check = ProfanityFilter.checkSubjectName(name)
                    if (!check.isClean) {
                        AppToast.show(host, check.reason ?: "Subject name contains inappropriate words 🛡️")
                        return@setOnClickListener
                    }
                    val created = SubjectTagManager.addCustomSubject(host, check.sanitizedText, selectedEmoji, selectedColorHex)
                    dialog.dismiss()
                    host.navigateToPanel(AppPanel.FOCUS)
                    onSubjectCreated?.invoke(created)
                }
            }
            container.addView(saveBtn)

            dialog.setContentView(container)
            dialog.window?.setLayout(
                (host.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(380)),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showPieChartDetailsModal(
        initialDateKey: String = SubjectTagManager.getTodayKey(),
        initialPeriod: SubjectTagManager.SubjectStatsPeriod = SubjectTagManager.SubjectStatsPeriod.TODAY
    ) {
        val dialog = Dialog(host, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            decorView.setPadding(0, 0, 0, 0)
            try {
                val isLight = themeCoordinator.activeBgMode == "LIGHT"
                val decor = peekDecorView() ?: decorView
                androidx.core.view.WindowCompat.getInsetsController(this, decor).let { controller ->
                    controller.isAppearanceLightStatusBars = isLight
                    controller.isAppearanceLightNavigationBars = isLight
                }
            } catch (_: Exception) {}
        }

        var currentPeriod = initialPeriod
        var currentDateKey = initialDateKey
        var currentWeekMonday = WeekHelper.mondayOf(Calendar.getInstance())
        var currentMonthCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDisplay = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val sdfWeekFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        val sdfMonthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val todayKey = SubjectTagManager.getTodayKey()

        val rootContainer = WallpaperManager.buildWallpaperContainer(host, themeCoordinator)

        val rootLayout = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(16), host.getStatusBarHeight() + dp(4), dp(16), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        rootContainer.addView(rootLayout)

        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        headerRow.addView(TextView(host).apply {
            text = "Subject Breakdown"
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val closeBtn = TextView(host).apply {
            text = "Close"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f)
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        rootLayout.addView(headerRow)

        // Segmented Period Selector [ Day | Week | Month | All Time ]
        val periodTabsScroll = HorizontalScrollView(host).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(12)) }
        }
        val periodTabsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        periodTabsScroll.addView(periodTabsRow)
        rootLayout.addView(periodTabsScroll)

        // Navigation Row (< 📅 16 Aug 2026 >)
        val dateNavRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        val prevBtn = TextView(host).apply {
            text = "◄"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        val dateTitleBtn = TextView(host).apply {
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, dp(8), 0) }
        }

        val nextBtn = TextView(host).apply {
            text = "►"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        dateNavRow.addView(prevBtn)
        dateNavRow.addView(dateTitleBtn)
        dateNavRow.addView(nextBtn)
        rootLayout.addView(dateNavRow)

        val scrollView = ScrollView(host).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val scrollContent = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        scrollView.addView(scrollContent)
        rootLayout.addView(scrollView)

        fun updateDateView() {
            // Update Title & Nav Buttons according to active period
            when (currentPeriod) {
                SubjectTagManager.SubjectStatsPeriod.TODAY -> {
                    val parsedDate = try { sdfKey.parse(currentDateKey) } catch (_: Exception) { Date() } ?: Date()
                    val formattedTitle = if (currentDateKey == todayKey) "Today (${sdfDisplay.format(parsedDate)})" else sdfDisplay.format(parsedDate)
                    dateTitleBtn.text = formattedTitle
                    val isToday = (currentDateKey == todayKey)
                    prevBtn.visibility = View.VISIBLE
                    nextBtn.visibility = View.VISIBLE
                    nextBtn.alpha = if (isToday) 0.3f else 1.0f
                    nextBtn.isEnabled = !isToday
                }
                SubjectTagManager.SubjectStatsPeriod.WEEK -> {
                    val wCal = currentWeekMonday.clone() as Calendar
                    val startStr = sdfWeekFmt.format(wCal.time)
                    wCal.add(Calendar.DAY_OF_YEAR, 6)
                    val endStr = "${sdfWeekFmt.format(wCal.time)} ${wCal.get(Calendar.YEAR)}"
                    dateTitleBtn.text = "Week of $startStr – $endStr"

                    val isCurrentWeek = WeekHelper.mondayOf(Calendar.getInstance()).timeInMillis <= currentWeekMonday.timeInMillis
                    prevBtn.visibility = View.VISIBLE
                    nextBtn.visibility = View.VISIBLE
                    nextBtn.alpha = if (isCurrentWeek) 0.3f else 1.0f
                    nextBtn.isEnabled = !isCurrentWeek
                }
                SubjectTagManager.SubjectStatsPeriod.MONTH -> {
                    dateTitleBtn.text = sdfMonthFmt.format(currentMonthCal.time)
                    val nowCal = Calendar.getInstance()
                    val isCurrentMonth = (nowCal.get(Calendar.YEAR) == currentMonthCal.get(Calendar.YEAR) && nowCal.get(Calendar.MONTH) <= currentMonthCal.get(Calendar.MONTH))
                    prevBtn.visibility = View.VISIBLE
                    nextBtn.visibility = View.VISIBLE
                    nextBtn.alpha = if (isCurrentMonth) 0.3f else 1.0f
                    nextBtn.isEnabled = !isCurrentMonth
                }
                SubjectTagManager.SubjectStatsPeriod.ALL_TIME -> {
                    dateTitleBtn.text = "All-Time Study History"
                    prevBtn.visibility = View.GONE
                    nextBtn.visibility = View.GONE
                }
            }

            scrollContent.removeAllViews()

            val topChartCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(16)) }
            }

            // Gather Data
            val statsEngine = StatsEngine(host)
            val subjectMap: Map<String, Pair<SubjectTag, Long>> = when (currentPeriod) {
                SubjectTagManager.SubjectStatsPeriod.TODAY -> {
                    val (daySessions, _) = statsEngine.dayBlocks(currentDateKey)
                    val map = LinkedHashMap<String, Pair<SubjectTag, Long>>()
                    for (s in daySessions) {
                        val subId = s.subjectId ?: "general"
                        val subj = SubjectTagManager.resolveSubject(host, subId, s.subjectName ?: "General", s.subjectColor)
                        val currentSecs = map[subj.id]?.second ?: 0L
                        map[subj.id] = subj to (currentSecs + s.secs)
                    }
                    if (map.isEmpty()) {
                        val legacy = SubjectTagManager.getSubjectDurationsForDate(host, currentDateKey)
                        for ((subId, secs) in legacy) {
                            val subj = SubjectTagManager.resolveSubject(host, subId)
                            val currentSecs = map[subj.id]?.second ?: 0L
                            map[subj.id] = subj to (currentSecs + secs)
                        }
                    }
                    map
                }
                SubjectTagManager.SubjectStatsPeriod.WEEK -> {
                    statsEngine.getSubjectBreakdownForDates(SubjectTagManager.getDatesForWeek(currentWeekMonday))
                }
                SubjectTagManager.SubjectStatsPeriod.MONTH -> {
                    statsEngine.getSubjectBreakdownForDates(SubjectTagManager.getDatesForMonth(currentMonthCal.get(Calendar.YEAR), currentMonthCal.get(Calendar.MONTH)))
                }
                SubjectTagManager.SubjectStatsPeriod.ALL_TIME -> {
                    statsEngine.getAllTimeSubjectBreakdown()
                }
            }

            val totalSecsAllModal = subjectMap.values.sumOf { it.second }
            val activeSubjectsSorted = subjectMap.values
                .filter { it.second >= 60L }
                .sortedByDescending { it.second }

            // Period Summary Banner
            val summaryRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(9), dp(12), dp(9))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 30), 12f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(14))
                }
            }
            summaryRow.addView(TextView(host).apply {
                text = formatDuration(totalSecsAllModal)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            val dailyAvg = when (currentPeriod) {
                SubjectTagManager.SubjectStatsPeriod.TODAY -> totalSecsAllModal
                SubjectTagManager.SubjectStatsPeriod.WEEK -> (totalSecsAllModal / 7L).coerceAtLeast(0L)
                SubjectTagManager.SubjectStatsPeriod.MONTH -> {
                    val dom = if (currentMonthCal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    } else {
                        currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }
                    (totalSecsAllModal / dom.toLong().coerceAtLeast(1L)).coerceAtLeast(0L)
                }
                SubjectTagManager.SubjectStatsPeriod.ALL_TIME -> totalSecsAllModal
            }
            if (currentPeriod != SubjectTagManager.SubjectStatsPeriod.TODAY && totalSecsAllModal > 0L) {
                summaryRow.addView(TextView(host).apply {
                    text = "  •  ~${formatDuration(dailyAvg)}/day"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.8f
                    textSize = 12f
                })
            }
            summaryRow.addView(TextView(host).apply {
                text = "  •  ${activeSubjectsSorted.size} subjects"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.65f
                textSize = 12f
            })
            topChartCard.addView(summaryRow)

            val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val isDonut = sharedPrefs.safeBoolean("use_donut_chart", true)
            val largePieView = SubjectPieChartView(host).apply {
                primaryColor = themeCoordinator.primaryColor
                textColor = themeCoordinator.textColor
                isDonutMode = isDonut
                boxColor = if (themeCoordinator.isDarkMode()) (if (themeCoordinator.activeBgMode == "ECLIPSE") 0xFF1E293B.toInt() else 0xFF111625.toInt()) else 0xFFFFFFFF.toInt()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340))
                onSliceSelectedListener = { slice, _ ->
                    if (slice != null && (slice.label == "Others" || slice.bundledSlices.isNotEmpty())) {
                        showOthersBreakdownDialog(slice, totalSecsAllModal.toDouble())
                    }
                }
            }

            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()
            for ((subj, secs) in activeSubjectsSorted) {
                slicesList.add(SubjectPieChartView.PieSlice(subj.name, subj.iconEmoji, secs.toDouble(), subj.colorHex))
            }

            if (slicesList.isNotEmpty()) {
                largePieView.setData(slicesList)
                topChartCard.addView(largePieView)
            } else {
                topChartCard.addView(TextView(host).apply {
                    text = "No subject activity recorded for this period"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(30), 0, dp(30))
                })
            }

            scrollContent.addView(topChartCard)

            val (daySessions, _) = if (currentPeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) statsEngine.dayBlocks(currentDateKey) else emptyList<BlockInfo>() to emptyList<BlockInfo>()
            val subjectBreakDurations = if (currentPeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) SubjectTagManager.getSubjectBreakDurationsForDate(host, currentDateKey) else emptyMap()

            if (slicesList.isEmpty()) {
                val emptyCard = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(24), dp(20), dp(24))
                    gravity = Gravity.CENTER
                }
                emptyCard.addView(TextView(host).apply {
                    text = "No Subject Sessions"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                emptyCard.addView(TextView(host).apply {
                    text = "No study time with subject tags recorded for this period."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                })
                scrollContent.addView(emptyCard)
            } else {
                for (subjPair in activeSubjectsSorted) {
                    val (subj, secs) = subjPair
                    val subjColor = try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }

                    val subjCard = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createCardBackground()
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
                    }

                    val headerRowSubj = LinearLayout(host).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val colorCircle = FrameLayout(host).apply {
                        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(subjColor) }
                        layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(10), 0) }
                    }

                    headerRowSubj.addView(colorCircle)
                    headerRowSubj.addView(TextView(host).apply {
                        text = "${subj.iconEmoji} ${subj.name}"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 17f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    val pct = if (totalSecsAllModal > 0) ((secs.toDouble() / totalSecsAllModal.toDouble()) * 100).toInt() else 0
                    headerRowSubj.addView(TextView(host).apply {
                        text = "$pct% Share"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })

                    subjCard.addView(headerRowSubj)

                    // Weekly Target Quota (if set)
                    if (subj.weeklyTargetHours > 0f) {
                        val targetSecs = (subj.weeklyTargetHours * 3600f).toLong()
                        val quotaPct = ((secs.toDouble() / targetSecs.toDouble()) * 100.0).toInt()
                        val isReached = quotaPct >= 100

                        val quotaTag = TextView(host).apply {
                            text = if (isReached) {
                                "Target Reached: ${String.format(Locale.US, "%.1f", secs / 3600f)}h / ${String.format(Locale.US, "%.1f", subj.weeklyTargetHours)}h ($quotaPct%)"
                            } else {
                                "Weekly Goal: ${String.format(Locale.US, "%.1f", secs / 3600f)}h / ${String.format(Locale.US, "%.1f", subj.weeklyTargetHours)}h ($quotaPct%)"
                            }
                            setTextColor(if (isReached) Color.parseColor("#10B981") else themeCoordinator.primaryColor)
                            textSize = 12f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setPadding(0, dp(6), 0, 0)
                        }
                        subjCard.addView(quotaTag)
                    }

                    // Sub-Topics list chips (if any)
                    if (subj.subTopics.isNotEmpty()) {
                        val studyTotals = SubjectTagManager.getSubTopicStudyTotals(host, subj.id)
                        val chaptersWithTime = subj.subTopics.map { topic ->
                            val tSecs = studyTotals[topic] ?: 0L
                            if (tSecs > 0L) {
                                val tm = tSecs / 60L
                                val ts = tSecs % 60L
                                val tStr = if (tm > 0) "${tm}m" else "${ts}s"
                                "$topic ($tStr)"
                            } else {
                                topic
                            }
                        }
                        val subTopicsLabel = TextView(host).apply {
                            text = "Chapters / Topics: " + chaptersWithTime.joinToString(", ")
                            setTextColor(themeCoordinator.textColor)
                            alpha = 0.7f
                            textSize = 12f
                            setPadding(0, dp(4), 0, 0)
                        }
                        subjCard.addView(subTopicsLabel)
                    }

                    val hrs = secs / 3600L
                    val mins = (secs % 3600L) / 60L
                    val focusTimeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                    val bSecs = subjectBreakDurations[subj.id] ?: 0L
                    val bHrs = bSecs / 3600L
                    val bMins = (bSecs % 3600L) / 60L
                    val breakTimeStr = if (bHrs > 0) "${bHrs}h ${bMins}m" else "${bMins}m"

                    val timingStr = if (currentPeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) {
                        val matchedSessions = daySessions.filter {
                            (it.subjectId != null && it.subjectId == subj.id) ||
                            (it.subjectId == null && subj.id == "general")
                        }
                        val validMatchedSessions = matchedSessions.filter { it.secs >= 60L }
                        if (validMatchedSessions.isNotEmpty()) {
                            val firstMs = validMatchedSessions.minOf { it.startMs }
                            val lastMs = validMatchedSessions.maxOf { it.endMs }
                            "${TimeFormat.formatWallClock(host, firstMs)} \u2013 ${TimeFormat.formatWallClock(host, lastMs)}"
                        } else "N/A"
                    } else {
                        "Period Total"
                    }

                    val detailsText = TextView(host).apply {
                        text = if (currentPeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) {
                            "Study Duration: $focusTimeStr\nBreak Duration: $breakTimeStr\nTime Window: $timingStr"
                        } else {
                            "Total Study Time: $focusTimeStr"
                        }
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 13.5f
                        setLineSpacing(dp(4).toFloat(), 1.1f)
                        setPadding(0, dp(10), 0, 0)
                    }
                    subjCard.addView(detailsText)

                    scrollContent.addView(subjCard)
                }
            }
        }

        fun renderPeriodTabs() {
            periodTabsRow.removeAllViews()
            val periods = SubjectTagManager.SubjectStatsPeriod.values()
            for (p in periods) {
                val isSelected = (p == currentPeriod)
                val tabBtn = TextView(host).apply {
                    text = p.displayName
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    setPadding(dp(14), dp(7), dp(14), dp(7))
                    background = if (isSelected) {
                        GradientDrawable().apply {
                            cornerRadius = dp(16).toFloat()
                            setColor(themeCoordinator.primaryColor)
                        }
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 16f)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, dp(8), 0) }
                    setOnClickListener {
                        if (currentPeriod != p) {
                            HapticFeedbackHelper.performMicroHapticTick(host, this)
                            currentPeriod = p
                            renderPeriodTabs()
                            updateDateView()
                        }
                    }
                }
                periodTabsRow.addView(tabBtn)
            }
        }
        renderPeriodTabs()

        prevBtn.setOnClickListener {
            when (currentPeriod) {
                SubjectTagManager.SubjectStatsPeriod.TODAY -> {
                    val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    currentDateKey = sdfKey.format(cal.time)
                }
                SubjectTagManager.SubjectStatsPeriod.WEEK -> {
                    currentWeekMonday.add(Calendar.WEEK_OF_YEAR, -1)
                }
                SubjectTagManager.SubjectStatsPeriod.MONTH -> {
                    currentMonthCal.add(Calendar.MONTH, -1)
                }
                SubjectTagManager.SubjectStatsPeriod.ALL_TIME -> {}
            }
            updateDateView()
        }

        nextBtn.setOnClickListener {
            when (currentPeriod) {
                SubjectTagManager.SubjectStatsPeriod.TODAY -> {
                    val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val newKey = sdfKey.format(cal.time)
                    if (newKey <= todayKey) {
                        currentDateKey = newKey
                    }
                }
                SubjectTagManager.SubjectStatsPeriod.WEEK -> {
                    val nextW = currentWeekMonday.clone() as Calendar
                    nextW.add(Calendar.WEEK_OF_YEAR, 1)
                    if (nextW.timeInMillis <= WeekHelper.mondayOf(Calendar.getInstance()).timeInMillis) {
                        currentWeekMonday = nextW
                    }
                }
                SubjectTagManager.SubjectStatsPeriod.MONTH -> {
                    val nowCal = Calendar.getInstance()
                    val nextM = currentMonthCal.clone() as Calendar
                    nextM.add(Calendar.MONTH, 1)
                    if (nextM.get(Calendar.YEAR) < nowCal.get(Calendar.YEAR) || (nextM.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && nextM.get(Calendar.MONTH) <= nowCal.get(Calendar.MONTH))) {
                        currentMonthCal = nextM
                    }
                }
                SubjectTagManager.SubjectStatsPeriod.ALL_TIME -> {}
            }
            updateDateView()
        }

        dateTitleBtn.setOnClickListener {
            if (currentPeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) {
                val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
                DatePickerDialog(host, { _, y: Int, m: Int, d: Int ->
                    val selCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, d)
                    }
                    val selKey = sdfKey.format(selCal.time)
                    if (selKey <= todayKey) {
                        currentDateKey = selKey
                        updateDateView()
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        updateDateView()
        dialog.setContentView(rootContainer)
        dialog.setOnShowListener {
            dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            dialog.window?.decorView?.setPadding(0, 0, 0, 0)
        }
        dialog.setOnDismissListener {
            for (i in 0 until rootContainer.childCount) {
                (rootContainer.getChildAt(i) as? LiveWallpaperView)?.release()
            }
        }
        dialog.show()
    }

    fun showCustomEmojiInputDialog(initialEmoji: String, onEmojiSelected: (String) -> Unit) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(22), dp(20), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(dp(360)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(host).apply {
                text = "Add Custom Emoji"
                setTextColor(themeCoordinator.textColor)
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(6))
            }
            container.addView(title)

            val subtitle = TextView(host).apply {
                text = "Type or paste an emoji to use for this subject:"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                textSize = 12.5f
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(subtitle)

            val input = android.widget.EditText(host).apply {
                setText(if (initialEmoji.isNotBlank() && initialEmoji.length <= 4) initialEmoji else "")
                hint = "e.g. 🚀 🎮 🧬 ⚡"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 80))
                setTextColor(themeCoordinator.textColor)
                textSize = 24f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(tintedColor(themeCoordinator.textColor, 18))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(2), tintedColor(themeCoordinator.primaryColor, 90))
                }
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            container.addView(input)

            val btnRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(18), 0, 0)
            }

            val cancelBtn = TextView(host).apply {
                text = "Cancel"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                textSize = 14f
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener { dialog.dismiss() }
            }
            val setBtn = TextView(host).apply {
                text = "Set Emoji"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 12f)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener {
                    val emojiStr = input.text.toString().trim()
                    if (emojiStr.isNotEmpty()) {
                        onEmojiSelected(emojiStr)
                        dialog.dismiss()
                    } else {
                        AppToast.show(host, "Please enter an emoji")
                    }
                }
            }

            btnRow.addView(cancelBtn)
            btnRow.addView(setBtn)
            container.addView(btnRow)

            dialog.setContentView(container)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showOthersBreakdownDialog(othersSlice: SubjectPieChartView.PieSlice, totalVal: Double) {
        try {
            val dialog = Dialog(host)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(20), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(440)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Header Row
            val headerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }
            headerRow.addView(TextView(host).apply {
                text = "📂 Others Breakdown"
                setTextColor(themeCoordinator.textColor)
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val closeBtn = TextView(host).apply {
                text = "✕"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 18f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { dialog.dismiss() }
            }
            headerRow.addView(closeBtn)
            container.addView(headerRow)

            val bundled = othersSlice.bundledSlices.sortedByDescending { it.value }
            val othersTotalSecs = othersSlice.value.toLong()
            val overallPct = if (totalVal > 0) Math.round((othersSlice.value / totalVal) * 100).toInt() else 0

            fun formatDur(secs: Long): String {
                val h = secs / 3600
                val m = (secs % 3600) / 60
                return when {
                    h > 0 -> "${h}h ${m}m"
                    m > 0 -> "${m}m"
                    else -> "<1m"
                }
            }

            val totalFormatted = formatDur(othersTotalSecs)

            // Subtitle summary card
            val summaryCard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 30), 14f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }
            summaryCard.addView(TextView(host).apply {
                text = "${bundled.size} bundled subjects • $totalFormatted ($overallPct% of total study time)"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            container.addView(summaryCard)

            // ScrollView with subjects list
            val scrollView = ScrollView(host).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(260))
            }
            val listContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

            for (item in bundled) {
                val itemSecs = item.value.toLong()
                val itemDurStr = formatDur(itemSecs)
                val pctOfTotal = if (totalVal > 0) ((item.value / totalVal) * 100).toInt() else 0
                val itemColor = try { Color.parseColor(item.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }

                val rowCard = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply {
                        setColor(tintedColor(themeCoordinator.textColor, 15))
                        cornerRadius = dp(12).toFloat()
                    }
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }

                val rowTop = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val colorCircle = View(host).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(itemColor) }
                    layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { setMargins(0, 0, dp(8), 0) }
                }
                rowTop.addView(colorCircle)

                rowTop.addView(TextView(host).apply {
                    text = if (item.emoji.isNotBlank()) "${item.emoji} ${item.label}" else item.label
                    setTextColor(themeCoordinator.textColor)
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                rowTop.addView(TextView(host).apply {
                    text = "$itemDurStr ($pctOfTotal%)"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13.5f
                    typeface = Typeface.MONOSPACE
                })
                rowCard.addView(rowTop)

                // Visual bar representation
                val barTrack = View(host).apply {
                    val barWidthPct = if (othersSlice.value > 0) (item.value / othersSlice.value).toFloat() else 0f
                    background = GradientDrawable().apply {
                        setColor(tintedColor(itemColor, 80))
                        cornerRadius = dp(4).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams((host.resources.displayMetrics.widthPixels * 0.70f * barWidthPct).toInt().coerceAtLeast(dp(12)), dp(5)).apply { setMargins(0, dp(8), 0, 0) }
                }
                rowCard.addView(barTrack)

                listContainer.addView(rowCard)
            }

            scrollView.addView(listContainer)
            container.addView(scrollView)

            dialog.setContentView(container)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var pomoSubjectPromptShowing = false

    fun checkPomoSubjectPrompt() {
        if (host.isFinishing || host.isDestroyed || pomoSubjectPromptShowing) return
        val prefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val hasFlag = prefs.getBoolean("pomo_prompt_subject_change", false)
        val hasIntentExtra = host.intent?.getBooleanExtra("EXTRA_PROMPT_CHANGE_SUBJECT", false) == true
        if (!hasFlag && !hasIntentExtra) return

        prefs.edit().putBoolean("pomo_prompt_subject_change", false).apply()
        host.intent?.removeExtra("EXTRA_PROMPT_CHANGE_SUBJECT")

        val origSubId = prefs.getString("pomo_grace_orig_subject_id", null) ?: SubjectTagManager.getSelectedSubject(host).id
        val curSub = SubjectTagManager.resolveSubject(host, origSubId)
        val pomoCount = prefs.safeInt("pomo_completed_count", 0) + 1

        val startTime = prefs.getLong("pomo_prompt_start_time", System.currentTimeMillis() / 1000)
        val now = System.currentTimeMillis() / 1000
        val elapsed = (now - startTime).coerceAtLeast(0L)
        val totalTimeout = 60L
        val remainingSecs = (totalTimeout - elapsed).coerceAtLeast(0L)

        if (remainingSecs <= 0L) {
            SubjectTagManager.finalizePomoGraceReallocation(host, origSubId)
            host.invalidateStatsCache()
            return
        }

        pomoSubjectPromptShowing = true
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var dismissedByAction = false
        var countDownTimer: android.os.CountDownTimer? = null

        dialog.setOnDismissListener {
            countDownTimer?.cancel()
            pomoSubjectPromptShowing = false
            if (!dismissedByAction) {
                if (prefs.getBoolean("pomo_grace_realloc_pending", false)) {
                    SubjectTagManager.finalizePomoGraceReallocation(host, origSubId)
                    host.invalidateStatsCache()
                }
            }
        }

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(28f)
        }

        root.addView(TextView(host).apply {
            text = "🍅 Break Finished"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        root.addView(TextView(host).apply {
            text = "Next Pomodoro session (#$pomoCount) has started.\n\nCurrent subject: ${curSub.iconEmoji} ${curSub.name}\n\nWould you like to keep this subject or switch to another?"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13.5f
            setPadding(0, dp(10), 0, dp(18))
        })

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val keepBtn = Button(host).apply {
            text = "Keep (${curSub.name}) (${remainingSecs}s)"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener {
                dismissedByAction = true
                dialog.dismiss()
                SubjectTagManager.finalizePomoGraceReallocation(host, origSubId)
                host.invalidateStatsCache()
            }
        }
        btnRow.addView(keepBtn)

        val switchBtn = Button(host).apply {
            text = "Change Subject"
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                dismissedByAction = true
                dialog.dismiss()
                showSubjectPickerDialog(allowActiveSwitch = true)
            }
        }
        btnRow.addView(switchBtn)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()
        val width = (host.resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(380))
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        countDownTimer = object : android.os.CountDownTimer(remainingSecs * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = (millisUntilFinished / 1000L).coerceAtLeast(1L)
                keepBtn.text = "Keep (${curSub.name}) (${secLeft}s)"
            }

            override fun onFinish() {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }.start()
    }
}

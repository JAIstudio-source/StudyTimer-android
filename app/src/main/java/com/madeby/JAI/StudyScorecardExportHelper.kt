package com.madeby.JAI

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyScorecardExportHelper(private val host: MainActivity) {

    private val themeCoordinator get() = host.themeCoordinator
    private fun dp(v: Int): Int = host.dp(v)
    private fun dpF(v: Float): Float = v * host.resources.displayMetrics.density

    enum class TimeRange {
        TODAY,
        THIS_WEEK,
        THIS_MONTH
    }

    enum class ExportFormat {
        IMAGE_PNG,
        PDF_DOC
    }

    fun showExportDialog(snap: StatsSnapshot) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        var selectedRange = TimeRange.THIS_WEEK
        var selectedFormat = ExportFormat.IMAGE_PNG
        var userNote = ""

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(26f)
            setPadding(dp(18), dp(18), dp(18), dp(16))
        }

        // Header: Title & Close
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val titleCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val dialogTitle = TextView(host).apply {
            text = "Weekly Report Card"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        titleCol.addView(dialogTitle)
        titleCol.addView(TextView(host).apply {
            text = "Clean black & white summary to save on device and share"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 11.5f
            setPadding(0, dp(2), 0, 0)
        })
        headerRow.addView(titleCol)

        val closeBtn = TextView(host).apply {
            text = "✕"
            textSize = 15f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.55f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // Range selector tabs (Today, This Week, This Month)
        val rangeRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val rangeChips = mutableListOf<TextView>()
        val ranges = listOf(
            Pair(TimeRange.TODAY, "Today"),
            Pair(TimeRange.THIS_WEEK, "This Week"),
            Pair(TimeRange.THIS_MONTH, "This Month")
        )

        // Format selector tabs (Image, PDF)
        val formatRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val formatChips = mutableListOf<TextView>()
        val formats = listOf(
            Pair(ExportFormat.IMAGE_PNG, "📸 Save Image (PNG)"),
            Pair(ExportFormat.PDF_DOC, "📄 Save PDF Document")
        )

        // Note Input
        val noteInput = EditText(host).apply {
            hint = "Note to self (optional, e.g. Finished Chapter 4!)"
            setHintTextColor(0xFF94A3B8.toInt())
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                val bg = if (themeCoordinator.isDarkMode()) 0xFF1E212D.toInt() else 0xFFF1F5F9.toInt()
                setColor(bg)
                setStroke(dp(1), if (themeCoordinator.isDarkMode()) 0x33FFFFFF.toInt() else 0xFFE2E8F0.toInt())
            }
            setPadding(dp(12), dp(9), dp(12), dp(9))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            maxLines = 2
        }

        // Live Preview Container (Scrollable)
        val previewScroll = ScrollView(host).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (host.resources.displayMetrics.heightPixels * 0.44f).toInt()
            ).apply {
                setMargins(0, 0, 0, dp(14))
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF0B0D14.toInt() else 0xFFE5E7EB.toInt())
                setStroke(dp(1), if (themeCoordinator.isDarkMode()) 0x33FFFFFF.toInt() else 0xFFCBD5E1.toInt())
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val cardHostContainer = FrameLayout(host).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        previewScroll.addView(cardHostContainer)

        var currentCardView: View? = null

        fun updateCardPreview() {
            cardHostContainer.removeAllViews()
            val card = buildBlackWhiteCard(snap, selectedRange, userNote)
            currentCardView = card
            cardHostContainer.addView(card)
        }

        fun refreshRangeUI() {
            val currentRangeName = when (selectedRange) {
                TimeRange.TODAY -> "Daily Report"
                TimeRange.THIS_WEEK -> "Weekly Report"
                TimeRange.THIS_MONTH -> "Monthly Report"
            }
            dialogTitle.text = "$currentRangeName Card"

            for ((idx, rPair) in ranges.withIndex()) {
                val chip = rangeChips.getOrNull(idx) ?: continue
                val isSel = rPair.first == selectedRange
                chip.setTextColor(if (isSel) Color.WHITE else themeCoordinator.textColor)
                chip.background = if (isSel) {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(themeCoordinator.primaryColor)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(if (themeCoordinator.isDarkMode()) 0xFF1E212D.toInt() else 0xFFF1F5F9.toInt())
                    }
                }
            }
        }

        for (rPair in ranges) {
            val chip = TextView(host).apply {
                text = rPair.second
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(7), dp(14), dp(7))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectedRange = rPair.first
                    refreshRangeUI()
                    updateCardPreview()
                }
            }
            rangeChips.add(chip)
            rangeRow.addView(chip)
        }
        root.addView(rangeRow)

        fun refreshFormatUI() {
            for ((idx, fPair) in formats.withIndex()) {
                val chip = formatChips.getOrNull(idx) ?: continue
                val isSel = fPair.first == selectedFormat
                chip.setTextColor(if (isSel) Color.WHITE else themeCoordinator.textColor)
                chip.background = if (isSel) {
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(0xFF0F172A.toInt())
                        setStroke(dp(1), themeCoordinator.primaryColor)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(if (themeCoordinator.isDarkMode()) 0xFF1E212D.toInt() else 0xFFF1F5F9.toInt())
                    }
                }
            }
        }

        for (fPair in formats) {
            val chip = TextView(host).apply {
                text = fPair.second
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectedFormat = fPair.first
                    refreshFormatUI()
                }
            }
            formatChips.add(chip)
            formatRow.addView(chip)
        }
        root.addView(formatRow)

        noteInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userNote = s?.toString()?.trim() ?: ""
                updateCardPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        root.addView(noteInput)
        root.addView(previewScroll)

        // Action Buttons
        val actionRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val cancelBtn = Button(host).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        actionRow.addView(cancelBtn)

        val saveAndShareBtn = Button(host).apply {
            text = "💾 Save to Device & Share"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(8), 0, 0, 0)
            }
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val cardView = currentCardView ?: return@setOnClickListener
                saveToDeviceAndShare(cardView, selectedFormat, selectedRange, snap, userNote)
                dialog.dismiss()
            }
        }
        actionRow.addView(saveAndShareBtn)
        root.addView(actionRow)

        refreshRangeUI()
        refreshFormatUI()
        updateCardPreview()

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (host.resources.displayMetrics.widthPixels * 0.94f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    /**
     * Builds the clean black-and-white editorial scorecard view.
     * Features:
     * - Background: Pure white `#FFFFFF`
     * - Watermark: `ic_watermark_logo.png` (transparent background, light cyan outline)
     *   placed ON TOP of the content with subtle alpha so it is completely visible across the tables.
     * - Borders: Crisp 1.5dp black borders and internal grid lines
     * - Typography: Clean bold sans-serif headers + monospace numbers/durations
     */
    private fun buildBlackWhiteCard(
        snap: StatsSnapshot,
        range: TimeRange,
        note: String,
        isPdfPage1: Boolean = false
    ): View {
        val cardFrame = FrameLayout(host).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(2), 0xFF111827.toInt())
            }
        }

        // 1. Content Layer (Underneath the watermark)
        val cardContent = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // --- TOP BRANDING (Real Round Corner Logo Image + Titles) ---
        val brandRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }

        val logoView = ImageView(host).apply {
            setImageResource(R.drawable.mylogo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                setMargins(0, 0, dp(12), 0)
            }
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(10).toFloat())
                }
            }
            clipToOutline = true
        }
        brandRow.addView(logoView)

        val brandTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val topTitle = TextView(host).apply {
            text = "STUDY TIMER APP"
            setTextColor(0xFF111827.toInt())
            textSize = 17f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val topSubtitle = TextView(host).apply {
            text = "Made By Team JAI"
            setTextColor(0xFF4B5563.toInt())
            textSize = 11f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, dp(1), 0, 0)
        }
        brandTextCol.addView(topTitle)
        brandTextCol.addView(topSubtitle)
        brandRow.addView(brandTextCol)

        cardContent.addView(brandRow)

        // Solid Horizontal Divider
        cardContent.addView(View(host).apply {
            setBackgroundColor(0xFF111827.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)).apply {
                setMargins(0, 0, 0, dp(10))
            }
        })

        // --- REPORT TITLE ---
        val reportTitleText = when (range) {
            TimeRange.TODAY -> "SCORECARD SUMMARY • DAILY REPORT"
            TimeRange.THIS_WEEK -> "SCORECARD SUMMARY • WEEKLY REPORT"
            TimeRange.THIS_MONTH -> "SCORECARD SUMMARY • MONTHLY REPORT"
        }
        val reportTitle = TextView(host).apply {
            text = reportTitleText
            setTextColor(0xFF111827.toInt())
            textSize = 13.5f
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setPadding(0, 0, 0, dp(10))
        }
        cardContent.addView(reportTitle)

        // --- STUDENT INFO & STREAK ROW ---
        val studentName = ProfileManager.getProfile(host).displayName.ifBlank { "Student" }
        val streakCount = snap.streak

        val infoBox = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeBorderedBox(bgColor = 0xFFF8FAFC.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        val studentCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        studentCol.addView(TextView(host).apply {
            text = "STUDENT: $studentName"
            setTextColor(0xFF111827.toInt())
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        studentCol.addView(TextView(host).apply {
            text = getDateLabelForRange(range)
            setTextColor(0xFF64748B.toInt())
            textSize = 10.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, dp(1), 0, 0)
        })
        infoBox.addView(studentCol)

        val streakBadge = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFFF1F5F9.toInt())
                setStroke(dp(1), 0xFF111827.toInt())
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        streakBadge.addView(ImageView(host).apply {
            setImageResource(R.drawable.ic_flame)
            setColorFilter(0xFF111827.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(13), dp(13)).apply {
                marginEnd = dp(4)
            }
        })
        streakBadge.addView(TextView(host).apply {
            text = "${streakCount}d Streak"
            setTextColor(0xFF111827.toInt())
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        infoBox.addView(streakBadge)
        cardContent.addView(infoBox)

        // --- 2x2 SCORECARD TABLE GRID ---
        val statsGrid = buildScorecardGrid(snap, range)
        cardContent.addView(statsGrid)

        // --- GRAPH / CALENDAR / TIMELINE SECTION ---
        when (range) {
            TimeRange.THIS_WEEK -> {
                val weekGraphCard = buildWeeklyBarGraphView(snap)
                cardContent.addView(weekGraphCard)
                // Subject breakdown
                val subjectsCard = buildSubjectBreakdownView(range)
                cardContent.addView(subjectsCard)
            }
            TimeRange.TODAY -> {
                val dailyTimeline = buildDailyTimelineView()
                cardContent.addView(dailyTimeline)
                // Subject breakdown
                val subjectsCard = buildSubjectBreakdownView(range)
                cardContent.addView(subjectsCard)
            }
            TimeRange.THIS_MONTH -> {
                // Monthly Goal Status Calendar (as requested by user!)
                val monthCalendarView = buildMonthlyGoalCalendarView(snap)
                cardContent.addView(monthCalendarView)

                // For single image or Page 1 of PDF, add compact subjects preview
                if (!isPdfPage1) {
                    val subjectsCard = buildSubjectBreakdownView(range, maxItems = 4)
                    cardContent.addView(subjectsCard)
                }
            }
        }

        // --- OPTIONAL NOTE TO SELF ---
        if (note.isNotBlank()) {
            val noteCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = makeBorderedBox(bgColor = 0xFFF9FAFB.toInt())
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(10), 0, 0)
                }
            }
            noteCard.addView(TextView(host).apply {
                text = "NOTE TO SELF"
                setTextColor(0xFF111827.toInt())
                textSize = 9.5f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            noteCard.addView(TextView(host).apply {
                text = "\"$note\""
                setTextColor(0xFF1F2937.toInt())
                textSize = 12f
                typeface = Typeface.create("serif", Typeface.ITALIC)
                setPadding(0, dp(3), 0, 0)
            })
            cardContent.addView(noteCard)
        }

        // --- FOOTER ---
        cardContent.addView(View(host).apply {
            setBackgroundColor(0xFFE5E7EB.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(12), 0, dp(8))
            }
        })

        val footerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val footerPageText = if (isPdfPage1) "StudyTimer App • Page 1 of 2" else "StudyTimer App • Keep Going!"
        footerRow.addView(TextView(host).apply {
            text = footerPageText
            setTextColor(0xFF4B5563.toInt())
            textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val genTimeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        footerRow.addView(TextView(host).apply {
            text = "Generated $genTimeStr"
            setTextColor(0xFF9CA3AF.toInt())
            textSize = 9.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        })
        cardContent.addView(footerRow)
        cardFrame.addView(cardContent)
        return cardFrame
    }

    /**
     * Builds Page 2 of the Monthly PDF Document (Detailed Breakdown).
     */
    private fun buildMonthlyPdfPage2(snap: StatsSnapshot): View {
        val cardFrame = FrameLayout(host).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(2), 0xFF111827.toInt())
            }
        }

        val cardContent = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // Top Branding (Real Round Corner Logo Image + Titles)
        val brandRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }

        val logoView = ImageView(host).apply {
            setImageResource(R.drawable.mylogo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                setMargins(0, 0, dp(12), 0)
            }
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(10).toFloat())
                }
            }
            clipToOutline = true
        }
        brandRow.addView(logoView)

        val brandTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandTextCol.addView(TextView(host).apply {
            text = "STUDY TIMER APP"
            setTextColor(0xFF111827.toInt())
            textSize = 17f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        brandTextCol.addView(TextView(host).apply {
            text = "Made By Team JAI"
            setTextColor(0xFF4B5563.toInt())
            textSize = 11f
            letterSpacing = 0.08f
            setPadding(0, dp(1), 0, 0)
        })
        brandRow.addView(brandTextCol)
        cardContent.addView(brandRow)
        cardContent.addView(View(host).apply {
            setBackgroundColor(0xFF111827.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)).apply {
                setMargins(0, 0, 0, dp(10))
            }
        })

        // Subtitle
        cardContent.addView(TextView(host).apply {
            text = "MONTHLY STUDY REPORT • DETAILED AUDIT (PAGE 2)"
            setTextColor(0xFF111827.toInt())
            textSize = 13.5f
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setPadding(0, 0, 0, dp(12))
        })

        // 1. Week-by-Week Progress Bars
        val weekProgressCard = buildMonthlyProgressView(snap)
        cardContent.addView(weekProgressCard)

        // 2. Full Subject Breakdown Table
        val subjectsCard = buildSubjectBreakdownView(TimeRange.THIS_MONTH, maxItems = 12)
        cardContent.addView(subjectsCard)

        // 3. Consistency & Habit Records
        val habitCard = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, 0)
            }
        }
        habitCard.addView(TextView(host).apply {
            text = "CONSISTENCY & HABIT METRICS"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        fun addHabitRow(label: String, value: String) {
            val r = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(2), 0, dp(2))
            }
            r.addView(TextView(host).apply {
                text = label
                setTextColor(0xFF374151.toInt())
                textSize = 11.5f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            r.addView(TextView(host).apply {
                text = value
                setTextColor(0xFF111827.toInt())
                textSize = 11.5f
                typeface = Typeface.MONOSPACE
            })
            habitCard.addView(r)
        }

        addHabitRow("Current Active Streak", "${snap.streak} Days")
        addHabitRow("All-Time Longest Streak", "${snap.longestStreak} Days")
        addHabitRow("Total Active Study Days", "${snap.activeDays} Days")
        addHabitRow("Daily Goals Met (All-Time)", "${snap.goalHits} Goals Won")
        cardContent.addView(habitCard)

        // Footer
        cardContent.addView(View(host).apply {
            setBackgroundColor(0xFFE5E7EB.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(14), 0, dp(8))
            }
        })

        val footerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        footerRow.addView(TextView(host).apply {
            text = "StudyTimer App • Page 2 of 2"
            setTextColor(0xFF4B5563.toInt())
            textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val genTimeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        footerRow.addView(TextView(host).apply {
            text = "Generated $genTimeStr"
            setTextColor(0xFF9CA3AF.toInt())
            textSize = 9.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        })
        cardContent.addView(footerRow)
        cardFrame.addView(cardContent)
        return cardFrame
    }

    /**
     * Creates a 2x2 grid of boxes with solid black borders matching the reference image.
     */
    private fun buildScorecardGrid(snap: StatsSnapshot, range: TimeRange): View {
        val grid = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        fun makeBox(title: String, value: String): LinearLayout {
            return LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = makeBorderedBox()
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(host).apply {
                    text = title
                    setTextColor(0xFF4B5563.toInt())
                    textSize = 9.5f
                    letterSpacing = 0.08f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                addView(TextView(host).apply {
                    text = value
                    setTextColor(0xFF111827.toInt())
                    textSize = 14.5f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(3), 0, 0)
                })
            }
        }

        when (range) {
            TimeRange.TODAY -> {
                val todayFocus = snap.todayFocus
                val goalSecs = snap.heroGoalSecs.coerceAtLeast(host.dailyGoalSecs())
                val goalStatus = if (goalSecs > 0 && todayFocus >= goalSecs) {
                    "Achieved ✓"
                } else if (goalSecs > 0) {
                    "${(todayFocus.toFloat() / goalSecs.toFloat() * 100f).toInt()}% Done"
                } else {
                    "In Progress"
                }

                val (sessions, _) = host.dayBlocks(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                val longestStretch = sessions.maxOfOrNull { it.secs } ?: 0L
                val longestStr = if (longestStretch > 0) "${longestStretch / 3600}h ${(longestStretch % 3600) / 60}m" else "0m"

                val row1 = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
                row1.addView(makeBox("TODAY'S GOAL STATUS", goalStatus).apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row1.addView(makeBox("TIME STUDIED TODAY", formatHoursMins(todayFocus)))
                grid.addView(row1)

                val row2 = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(6)
                    }
                }
                row2.addView(makeBox("STUDY SESSIONS", "${sessions.size} Completed").apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row2.addView(makeBox("LONGEST FOCUS", longestStr))
                grid.addView(row2)
            }
            TimeRange.THIS_WEEK -> {
                val thisWeekFocus = snap.thisWeek
                val avgDaily = thisWeekFocus / 7L
                val topSubjectName = getTopSubjectForWeek()

                // Calculate how many days goal was achieved this week
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val mon = WeekHelper.mondayOf(Calendar.getInstance())
                var daysHit = 0
                for (i in 0..6) {
                    val c = mon.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, i)
                    val dKey = sdf.format(c.time)
                    val f = snap.dayFocus[dKey] ?: 0L
                    val g = host.resolveGoalFor(dKey)
                    if (g > 0 && f >= g) daysHit++
                }
                val goalStatusStr = if (daysHit > 0) "Achieved ($daysHit/7d)" else "In Progress"

                val row1 = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
                row1.addView(makeBox("WEEKLY GOAL STATUS", goalStatusStr).apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row1.addView(makeBox("TIME STUDIED THIS WEEK", formatHoursMins(thisWeekFocus)))
                grid.addView(row1)

                val row2 = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(6)
                    }
                }
                row2.addView(makeBox("DAILY AVERAGE", "${formatHoursMins(avgDaily)} / day").apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row2.addView(makeBox("TOP SUBJECT", topSubjectName))
                grid.addView(row2)
            }
            TimeRange.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val monthDates = SubjectTagManager.getDatesForMonth(year, month)
                var monthTotalFocus = 0L
                var activeDaysCount = 0
                var goalsMetMonth = 0
                for (dKey in monthDates) {
                    val f = snap.dayFocus[dKey] ?: 0L
                    val g = host.resolveGoalFor(dKey)
                    if (f > 0) {
                        monthTotalFocus += f
                        activeDaysCount++
                    }
                    if (g > 0 && f >= g) goalsMetMonth++
                }
                val avgDailyMonth = if (monthDates.isNotEmpty()) monthTotalFocus / monthDates.size else 0L

                val row1 = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
                row1.addView(makeBox("MONTHLY GOAL HITS", "$goalsMetMonth of ${monthDates.size} Days").apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row1.addView(makeBox("TIME STUDIED THIS MONTH", formatHoursMins(monthTotalFocus)))
                grid.addView(row1)

                val row2 = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(6)
                    }
                }
                row2.addView(makeBox("DAILY AVERAGE", "${formatHoursMins(avgDailyMonth)} / day").apply { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { rightMargin = dp(6) } })
                row2.addView(makeBox("TOP SUBJECT", getTopSubjectForMonth()))
                grid.addView(row2)
            }
        }

        return grid
    }

    /**
     * Builds the exact MONTHLY GOAL STATUS CALENDAR from the user's reference image!
     * Features:
     * - Black top header with white text: Mon Tue Wed Thu Fri Sat Sun
     * - Clean 7-column grid
     * - Each day has day number in a circle, and below it: ✓ (goal met) or ✕ (missed) or – (future)
     * - Compact layout so the single photo card remains balanced and not overly lengthy!
     */
    private fun buildMonthlyGoalCalendarView(snap: StatsSnapshot): View {
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        // Section Title
        container.addView(TextView(host).apply {
            text = "MONTHLY GOAL STATUS CALENDAR"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(8), dp(10), dp(6))
        })

        // Calendar Header Banner (Solid Black with Mon-Sun)
        val headerRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(0, dp(4), 0, dp(4))
        }
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (dayName in daysOfWeek) {
            headerRow.addView(TextView(host).apply {
                text = dayName
                setTextColor(Color.WHITE)
                textSize = 10.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        container.addView(headerRow)

        // Date calculations
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) // 0-based
        val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Determine starting day of week (Monday = 0, Sunday = 6)
        val firstDow = monthCal.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = if (firstDow == Calendar.SUNDAY) 6 else firstDow - Calendar.MONDAY
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalSlots = ((leadingEmpty + daysInMonth + 6) / 7) * 7

        val gridContainer = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

        var slotIndex = 0
        while (slotIndex < totalSlots) {
            val weekRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            for (col in 0..6) {
                val dayNum = slotIndex - leadingEmpty + 1
                val isValidDay = dayNum in 1..daysInMonth

                val cell = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(0xFFFFFFFF.toInt())
                        setStroke(dp(1), 0xFFE2E8F0.toInt())
                    }
                    setPadding(0, dp(4), 0, dp(4))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                if (isValidDay) {
                    val dateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth + 1, dayNum)
                    val f = snap.dayFocus[dateKey] ?: 0L
                    val g = host.resolveGoalFor(dateKey)
                    val isToday = dayNum == todayDayOfMonth
                    val isFuture = dayNum > todayDayOfMonth
                    val isGoalMet = g > 0 && f >= g

                    // Circle with Day Number
                    val dayCircle = TextView(host).apply {
                        text = "$dayNum"
                        textSize = 10f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        gravity = Gravity.CENTER
                        setTextColor(if (isGoalMet) Color.WHITE else 0xFF111827.toInt())
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            if (isGoalMet) {
                                setColor(0xFF111827.toInt())
                            } else {
                                setColor(Color.TRANSPARENT)
                                setStroke(dp(1), if (isToday) 0xFF111827.toInt() else 0xFF64748B.toInt())
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                    }
                    cell.addView(dayCircle)

                    // Indicator: ✓ / ✕ / –
                    val statusText = when {
                        isGoalMet -> "✓"
                        isFuture -> "–"
                        f > 0L -> "✓"
                        else -> "✕"
                    }
                    val statusColor = when (statusText) {
                        "✓" -> 0xFF16A34A.toInt()
                        "✕" -> 0xFFDC2626.toInt()
                        else -> 0xFF9CA3AF.toInt()
                    }
                    cell.addView(TextView(host).apply {
                        text = statusText
                        textSize = 11f
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        setTextColor(statusColor)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(2), 0, 0)
                    })
                } else {
                    // Empty Cell
                    cell.addView(TextView(host).apply {
                        text = "–"
                        textSize = 10f
                        setTextColor(0xFFCBD5E1.toInt())
                        gravity = Gravity.CENTER
                        setPadding(0, dp(12), 0, dp(12))
                    })
                }

                weekRow.addView(cell)
                slotIndex++
            }
            gridContainer.addView(weekRow)
        }

        container.addView(gridContainer)
        return container
    }

    /**
     * Builds the exact 7-Day Bar Graph matching the app's BarTrackView, but in high-contrast black & white.
     */
    private fun buildWeeklyBarGraphView(snap: StatsSnapshot): View {
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        container.addView(TextView(host).apply {
            text = "THIS WEEK'S STUDY GRAPH"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        val mondayOffset = WeekHelper.mondayOffset(cal)
        val todayStr = sdf.format(Date())

        var maxFocus = 1L
        val weekDaysList = ArrayList<Triple<String, String, Long>>()
        for (i in 0..6) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset + i) }
            val dStr = sdf.format(c.time)
            val f = snap.dayFocus[dStr] ?: 0L
            if (f > maxFocus) maxFocus = f
            val dayLabel = "${weekdayNames[i]} ${SimpleDateFormat("d MMM", Locale.getDefault()).format(c.time)}"
            weekDaysList.add(Triple(dStr, dayLabel, f))
        }

        val scale = if (maxFocus <= 0L) 3600L else Math.ceil(maxFocus.toDouble() / 3600.0).toLong() * 3600L

        for ((dStr, label, fSecs) in weekDaysList) {
            val isToday = dStr == todayStr
            val isPeak = fSecs == maxFocus && maxFocus > 0L
            val dayGoalSecs = host.resolveGoalFor(dStr)
            val dayGoalRatio = (dayGoalSecs.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
            val barRatio = if (scale > 0L) fSecs.toFloat() / scale.toFloat() else 0f

            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
            }

            row.addView(TextView(host).apply {
                text = if (isToday) "$label • Today" else label
                setTextColor(if (isToday) 0xFF000000.toInt() else 0xFF374151.toInt())
                textSize = 11f
                typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(dp(95), LinearLayout.LayoutParams.WRAP_CONTENT)
            })

            // Black & White Bar Track View
            val barView = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(12), 1f).apply {
                    setMargins(dp(6), 0, dp(8), 0)
                }
                background = object : android.graphics.drawable.Drawable() {
                    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFFE5E7EB.toInt()
                        style = Paint.Style.FILL
                    }
                    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isToday) 0xFF000000.toInt() else 0xFF1F2937.toInt()
                        style = Paint.Style.FILL
                    }
                    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF6B7280.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = dpF(1.5f)
                    }

                    override fun draw(canvas: Canvas) {
                        val b = bounds
                        val radius = b.height() / 2f
                        canvas.drawRoundRect(RectF(b), radius, radius, trackPaint)
                        val fillW = (b.width() * barRatio.coerceIn(0f, 1f))
                        if (fillW > 0) {
                            canvas.drawRoundRect(RectF(b.left.toFloat(), b.top.toFloat(), b.left + fillW, b.bottom.toFloat()), radius, radius, fillPaint)
                        }
                        if (dayGoalRatio > 0.05f && dayGoalRatio < 0.98f) {
                            val tickX = b.left + (b.width() * dayGoalRatio)
                            canvas.drawLine(tickX, b.top.toFloat(), tickX, b.bottom.toFloat(), tickPaint)
                        }
                    }

                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                    @Deprecated("Deprecated in Java")
                    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                }
            }
            row.addView(barView)

            val timeLabel = formatHoursMins(fSecs)
            val peakTag = if (isPeak) " ★" else ""
            row.addView(TextView(host).apply {
                text = "$timeLabel$peakTag"
                setTextColor(if (isPeak) 0xFF000000.toInt() else 0xFF374151.toInt())
                textSize = 11.5f
                typeface = Typeface.MONOSPACE
            })

            container.addView(row)
        }

        return container
    }

    /**
     * Builds Today's study session list for the Daily card.
     */
    private fun buildDailyTimelineView(): View {
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        container.addView(TextView(host).apply {
            text = "TODAY'S STUDY SESSIONS"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val (sessions, _) = host.dayBlocks(todayStr)
        val validSessions = sessions.filter { it.secs >= 60L }

        if (validSessions.isEmpty()) {
            container.addView(TextView(host).apply {
                text = "No completed focus sessions logged yet today. Start the timer to fill this card!"
                setTextColor(0xFF6B7280.toInt())
                textSize = 11.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setPadding(0, dp(4), 0, dp(4))
            })
        } else {
            val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
            for ((idx, s) in validSessions.withIndex()) {
                val subName = s.subjectName ?: "General Study"
                val startStr = if (s.startMs > 0) timeFmt.format(Date(s.startMs)) else "Session"
                val endStr = if (s.endMs > 0) timeFmt.format(Date(s.endMs)) else ""
                val intervalStr = if (endStr.isNotBlank()) "$startStr – $endStr" else startStr

                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(2), 0, dp(2))
                }
                row.addView(TextView(host).apply {
                    text = "${idx + 1}. $subName ($intervalStr)"
                    setTextColor(0xFF1F2937.toInt())
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(host).apply {
                    text = formatHoursMins(s.secs)
                    setTextColor(0xFF111827.toInt())
                    textSize = 11.5f
                    typeface = Typeface.MONOSPACE
                })
                container.addView(row)
            }
        }

        return container
    }

    /**
     * Builds Monthly 4-week summary bars.
     */
    private fun buildMonthlyProgressView(snap: StatsSnapshot): View {
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        container.addView(TextView(host).apply {
            text = "MONTHLY WEEK-BY-WEEK PROGRESS"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val monthDates = SubjectTagManager.getDatesForMonth(year, month)

        val weekBuckets = mutableListOf<Pair<String, Long>>()
        var w1 = 0L; var w2 = 0L; var w3 = 0L; var w4 = 0L
        for ((idx, dKey) in monthDates.withIndex()) {
            val f = snap.dayFocus[dKey] ?: 0L
            when (idx) {
                in 0..6 -> w1 += f
                in 7..13 -> w2 += f
                in 14..20 -> w3 += f
                else -> w4 += f
            }
        }
        weekBuckets.add(Pair("Week 1 (Days 1–7)", w1))
        weekBuckets.add(Pair("Week 2 (Days 8–14)", w2))
        weekBuckets.add(Pair("Week 3 (Days 15–21)", w3))
        weekBuckets.add(Pair("Week 4 (Days 22+)", w4))

        val maxW = (weekBuckets.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(3600L)

        for ((lbl, wSecs) in weekBuckets) {
            val ratio = (wSecs.toFloat() / maxW.toFloat()).coerceIn(0f, 1f)
            val row = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
            }
            row.addView(TextView(host).apply {
                text = lbl
                setTextColor(0xFF374151.toInt())
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val barView = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(12), 1f).apply { setMargins(dp(6), 0, dp(8), 0) }
                background = object : android.graphics.drawable.Drawable() {
                    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5E7EB.toInt(); style = Paint.Style.FILL }
                    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111827.toInt(); style = Paint.Style.FILL }
                    override fun draw(canvas: Canvas) {
                        val b = bounds
                        val radius = b.height() / 2f
                        canvas.drawRoundRect(RectF(b), radius, radius, trackPaint)
                        val fillW = b.width() * ratio
                        if (fillW > 0) canvas.drawRoundRect(RectF(b.left.toFloat(), b.top.toFloat(), b.left + fillW, b.bottom.toFloat()), radius, radius, fillPaint)
                    }
                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                    @Deprecated("Deprecated in Java")
                    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                }
            }
            row.addView(barView)
            row.addView(TextView(host).apply {
                text = formatHoursMins(wSecs)
                setTextColor(0xFF111827.toInt())
                textSize = 11.5f
                typeface = Typeface.MONOSPACE
            })
            container.addView(row)
        }

        return container
    }

    /**
     * Builds Subject-wise study time list with duration and percentages.
     */
    private fun buildSubjectBreakdownView(range: TimeRange, maxItems: Int = 10): View {
        val container = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = makeBorderedBox()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        container.addView(TextView(host).apply {
            text = "SUBJECT WISE STUDY TIME"
            setTextColor(0xFF111827.toInt())
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        })

        val subjectMap = getSubjectsForRange(range)
        val allSubjects = SubjectTagManager.getAllSubjects(host)
        val subjectNameMap = allSubjects.associate { it.id to it.name }
        val totalSecs = subjectMap.values.sum().coerceAtLeast(1L)

        val sortedList = subjectMap.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .take(maxItems)

        if (sortedList.isEmpty()) {
            container.addView(TextView(host).apply {
                text = "• No subject data logged for this period"
                setTextColor(0xFF6B7280.toInt())
                textSize = 11.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setPadding(0, dp(2), 0, dp(2))
            })
        } else {
            for (entry in sortedList) {
                val name = subjectNameMap[entry.key] ?: entry.key.replaceFirstChar { it.uppercase() }
                val pct = ((entry.value.toFloat() / totalSecs.toFloat()) * 100f).toInt()
                val row = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(2), 0, dp(2))
                }
                row.addView(TextView(host).apply {
                    text = "• $name"
                    setTextColor(0xFF1F2937.toInt())
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(host).apply {
                    text = "${formatHoursMins(entry.value)} ($pct%)"
                    setTextColor(0xFF111827.toInt())
                    textSize = 11.5f
                    typeface = Typeface.MONOSPACE
                })
                container.addView(row)
            }
        }

        return container
    }

    private fun getSubjectsForRange(range: TimeRange): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        when (range) {
            TimeRange.TODAY -> {
                val todayStr = sdf.format(Date())
                result.putAll(SubjectTagManager.getSubjectDurationsForDate(host, todayStr))
            }
            TimeRange.THIS_WEEK -> {
                val mon = WeekHelper.mondayOf(Calendar.getInstance())
                for (i in 0..6) {
                    val c = mon.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, i)
                    val map = SubjectTagManager.getSubjectDurationsForDate(host, sdf.format(c.time))
                    for ((k, v) in map) result[k] = (result[k] ?: 0L) + v
                }
            }
            TimeRange.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val dates = SubjectTagManager.getDatesForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                for (dStr in dates) {
                    val map = SubjectTagManager.getSubjectDurationsForDate(host, dStr)
                    for ((k, v) in map) result[k] = (result[k] ?: 0L) + v
                }
            }
        }
        return result
    }

    private fun getTopSubjectForWeek(): String {
        val map = getSubjectsForRange(TimeRange.THIS_WEEK)
        val top = map.maxByOrNull { it.value } ?: return "None"
        val all = SubjectTagManager.getAllSubjects(host)
        val name = all.find { it.id == top.key }?.name ?: top.key
        return "$name (${formatHoursMins(top.value)})"
    }

    private fun getTopSubjectForMonth(): String {
        val map = getSubjectsForRange(TimeRange.THIS_MONTH)
        val top = map.maxByOrNull { it.value } ?: return "None"
        val all = SubjectTagManager.getAllSubjects(host)
        val name = all.find { it.id == top.key }?.name ?: top.key
        return "$name (${formatHoursMins(top.value)})"
    }

    private fun getDateLabelForRange(range: TimeRange): String {
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        return when (range) {
            TimeRange.TODAY -> "Date: ${sdfDate.format(Date())}"
            TimeRange.THIS_WEEK -> {
                val mon = WeekHelper.mondayOf(cal)
                val sun = mon.clone() as Calendar
                sun.add(Calendar.DAY_OF_YEAR, 6)
                "Week: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(mon.time)} – ${sdfDate.format(sun.time)}"
            }
            TimeRange.THIS_MONTH -> "Month: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}"
        }
    }

    private fun formatHoursMins(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun makeBorderedBox(bgColor: Int = 0xFFFFFFFF.toInt()): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), 0xFF111827.toInt())
        }
    }

    private fun layoutViewForExport(view: View, targetW: Int): Int {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(targetW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetH = view.measuredHeight.coerceAtLeast(100)
        view.layout(0, 0, targetW, targetH)
        return targetH
    }

    /**
     * Saves the generated report (Image or PDF) permanently to device storage (Pictures / Downloads),
     * and then launches the Android Share Sheet.
     */
    private fun saveToDeviceAndShare(
        cardView: View,
        format: ExportFormat,
        range: TimeRange,
        snap: StatsSnapshot,
        userNote: String
    ) {
        try {
            val targetW = cardView.width.takeIf { it > 100 } ?: (host.resources.displayMetrics.widthPixels * 0.92f).toInt()
            val rangeSuffix = when (range) {
                TimeRange.TODAY -> "Daily"
                TimeRange.THIS_WEEK -> "Weekly"
                TimeRange.THIS_MONTH -> "Monthly"
            }
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val reportName = when (range) {
                TimeRange.TODAY -> "Daily Report"
                TimeRange.THIS_WEEK -> "Weekly Report"
                TimeRange.THIS_MONTH -> "Monthly Report"
            }

            if (format == ExportFormat.IMAGE_PNG) {
                val measuredH = layoutViewForExport(cardView, targetW)
                val bitmap = Bitmap.createBitmap(targetW, measuredH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                cardView.draw(canvas)

                val fileName = "StudyReport_${rangeSuffix}_$timeTag.png"
                val savedUri = saveImageToDeviceStorage(bitmap, fileName)

                AppToast.show(host, "Saved to Pictures/StudyTimer! 📸")

                // Open Share Sheet
                val shareUri = savedUri ?: run {
                    val sharedDir = File(host.cacheDir, "shared").apply { mkdirs() }
                    val cacheFile = File(sharedDir, fileName)
                    FileOutputStream(cacheFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    FileProvider.getUriForFile(host, "${host.packageName}.fileprovider", cacheFile)
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "StudyTimer — $reportName")
                    putExtra(Intent.EXTRA_TEXT, "Here is my $reportName from StudyTimer! 📚")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                host.startActivity(Intent.createChooser(shareIntent, "Share $reportName"))

            } else {
                // PDF Document (Single page for Daily/Weekly; 2 Pages for Monthly!)
                val pdfDocument = PdfDocument()
                val isMultiPageMonth = (range == TimeRange.THIS_MONTH)

                if (isMultiPageMonth) {
                    // Page 1: Overview + Monthly Goal Calendar
                    val page1View = buildBlackWhiteCard(snap, TimeRange.THIS_MONTH, userNote, isPdfPage1 = true)
                    val p1Height = layoutViewForExport(page1View, targetW)
                    val pageInfo1 = PdfDocument.PageInfo.Builder(targetW, p1Height, 1).create()
                    val page1 = pdfDocument.startPage(pageInfo1)
                    page1View.draw(page1.canvas)
                    pdfDocument.finishPage(page1)

                    // Page 2: Detailed Breakdown (Weekly Trend + Full Subjects + Consistency)
                    val page2View = buildMonthlyPdfPage2(snap)
                    val p2Height = layoutViewForExport(page2View, targetW)
                    val pageInfo2 = PdfDocument.PageInfo.Builder(targetW, p2Height, 2).create()
                    val page2 = pdfDocument.startPage(pageInfo2)
                    page2View.draw(page2.canvas)
                    pdfDocument.finishPage(page2)
                } else {
                    val measuredH = layoutViewForExport(cardView, targetW)
                    val pageInfo = PdfDocument.PageInfo.Builder(targetW, measuredH, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    cardView.draw(page.canvas)
                    pdfDocument.finishPage(page)
                }

                val fileName = "StudyReport_${rangeSuffix}_$timeTag.pdf"
                val savedUri = savePdfToDeviceStorage(pdfDocument, fileName)

                AppToast.show(host, "Saved to Downloads/StudyTimer! 📄")

                val shareUri = savedUri ?: run {
                    val sharedDir = File(host.cacheDir, "shared").apply { mkdirs() }
                    val cacheFile = File(sharedDir, fileName)
                    FileOutputStream(cacheFile).use { pdfDocument.writeTo(it) }
                    FileProvider.getUriForFile(host, "${host.packageName}.fileprovider", cacheFile)
                }
                pdfDocument.close()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "StudyTimer — $reportName (PDF)")
                    putExtra(Intent.EXTRA_TEXT, "Here is my official $reportName PDF from StudyTimer! 📄")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                host.startActivity(Intent.createChooser(shareIntent, "Share $reportName (PDF)"))
            }
        } catch (e: Exception) {
            AppToast.show(host, "Save failed: ${e.localizedMessage}")
        }
    }

    private fun saveImageToDeviceStorage(bitmap: Bitmap, filename: String): Uri? {
        val resolver = host.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StudyTimer")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "StudyTimer").apply { mkdirs() }
            val file = File(appDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.media.MediaScannerConnection.scanFile(host, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            return FileProvider.getUriForFile(host, "${host.packageName}.fileprovider", file)
        }
        return null
    }

    private fun savePdfToDeviceStorage(pdfDoc: PdfDocument, filename: String): Uri? {
        val resolver = host.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyTimer")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    pdfDoc.writeTo(out)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "StudyTimer").apply { mkdirs() }
            val file = File(appDir, filename)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            return FileProvider.getUriForFile(host, "${host.packageName}.fileprovider", file)
        }
        return null
    }
}

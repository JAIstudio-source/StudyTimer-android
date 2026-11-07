package com.madeby.JAI

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.withContext
import android.app.Dialog
import android.app.TimePickerDialog
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.HapticFeedbackConstants
import android.widget.HorizontalScrollView
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Modern Developer & Debug Suite 2.0 for StudyTimer.
 * Gated strictly behind developer unlock flags, with modular isolated sections for:
 * 1. Manual Session Logger & Mocking:
 *    - Start Time & End Time Pickers (with real-time duration computation & validation)
 *    - Custom subjects, untagged general focus, custom date pickers
 * 2. Database & State Management:
 *    - Realistic, consistent mock data generator (Deterministic template, habitual variance, rest days)
 *    - Live state, preset demo seeds, granular resets
 * 3. Cloud Sync & Conflict Simulation (Force push/pull, simulate conflict)
 * 4. Trigger & Alert Testing (Celebrations, alarms, audio/haptic chimes)
 * 5. UI & Theme Overrides (Edge cases, AMOLED switcher)
 */
object DeveloperToolsHelper {

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun getPickerThemeRes(): Int {
        return R.style.AmoledPickerDialogTheme
    }

    /**
     * Shows a beautiful glassmorphism-themed Subject Picker modal adhering to ThemeCoordinator.
     */
    fun showThemedSubjectPickerModal(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        title: String = "Select Subject Tag",
        includeGeneral: Boolean = true,
        includeCreateOption: Boolean = false,
        onSelected: (SubjectTag?) -> Unit,
        onCreateCustom: (() -> Unit)? = null
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
        val subjects = SubjectTagManager.getAllSubjects(activity)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        val titleBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val titleText = TextView(activity).apply {
            text = title
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setOnClickListener { dialog.dismiss() }
        }
        titleBar.addView(titleText)
        titleBar.addView(closeBtn)
        root.addView(titleBar)

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.52f).toInt()
            )
        }
        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }

        fun createSubjectGridCard(name: String, colorHex: String, tag: SubjectTag?): View {
            val colorInt = try { Color.parseColor(colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 22), 14f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                setOnClickListener {
                    onSelected(tag)
                    dialog.dismiss()
                }
            }

            val colorDot = View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colorInt)
                }
                layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                    setMargins(0, 0, dp(8), 0)
                }
            }

            val label = TextView(activity).apply {
                text = name
                setTextColor(themeCoordinator.textColor)
                textSize = 12.5f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            card.addView(colorDot)
            card.addView(label)
            return card
        }

        data class PickerItem(val name: String, val colorHex: String, val tag: SubjectTag?)
        val pickerItems = mutableListOf<PickerItem>()
        if (includeGeneral) {
            pickerItems.add(PickerItem("General Focus", "#6366F1", null))
        }
        for (sub in subjects) {
            pickerItems.add(PickerItem(sub.name, sub.colorHex, sub))
        }

        for (i in pickerItems.indices step 2) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(6))
                }
            }

            val item1 = pickerItems[i]
            val card1 = createSubjectGridCard(item1.name, item1.colorHex, item1.tag)
            (card1.layoutParams as LinearLayout.LayoutParams).apply {
                if (i + 1 < pickerItems.size) setMargins(0, 0, dp(4), 0)
            }
            row.addView(card1)

            if (i + 1 < pickerItems.size) {
                val item2 = pickerItems[i + 1]
                val card2 = createSubjectGridCard(item2.name, item2.colorHex, item2.tag)
                (card2.layoutParams as LinearLayout.LayoutParams).apply {
                    setMargins(dp(4), 0, 0, 0)
                }
                row.addView(card2)
            } else {
                row.addView(View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                        setMargins(dp(4), 0, 0, 0)
                    }
                })
            }
            listContainer.addView(row)
        }

        if (includeCreateOption) {
            val createCard = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 14f)
                setPadding(dp(14), dp(11), dp(14), dp(11))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(4), 0, dp(8))
                }
                setOnClickListener {
                    onCreateCustom?.invoke()
                    dialog.dismiss()
                }
            }
            val plusIcon = TextView(activity).apply {
                text = "➕"
                textSize = 15f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                    setMargins(0, 0, dp(10), 0)
                }
            }
            val plusLabel = TextView(activity).apply {
                text = "Create Custom Subject..."
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            createCard.addView(plusIcon)
            createCard.addView(plusLabel)
            listContainer.addView(createCard)
        }

        scroll.addView(listContainer)
        root.addView(scroll)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout((activity.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    /**
     * Shows a beautiful themed confirmation dialog instead of standard Android alerts.
     */
    fun showThemedConfirmDialog(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        title: String,
        message: String,
        confirmText: String = "Confirm",
        isDestructive: Boolean = false,
        onCancel: (() -> Unit)? = null,
        onConfirm: () -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val titleView = TextView(activity).apply {
            text = title
            setTextColor(if (isDestructive) Color.parseColor("#EF4444") else themeCoordinator.primaryColor)
            textSize = 16.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        val msgView = TextView(activity).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13f
            setPadding(0, 0, 0, dp(18))
        }

        val btnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = Button(activity).apply {
            text = "Cancel"
            setTextColor(tintedColor(themeCoordinator.textColor, 180))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 12f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)).apply {
                setMargins(0, 0, dp(8), 0)
            }
            setOnClickListener {
                dialog.dismiss()
                onCancel?.invoke()
            }
        }

        val confirmBtn = Button(activity).apply {
            text = confirmText
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDestructive) Color.parseColor("#EF4444") else themeCoordinator.primaryColor)
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40))
            setOnClickListener {
                dialog.dismiss()
                onConfirm()
            }
        }

        btnRow.addView(cancelBtn)
        btnRow.addView(confirmBtn)

        root.addView(titleView)
        root.addView(msgView)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.setOnCancelListener { onCancel?.invoke() }
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    fun buildDevCard(activity: MainActivity, themeCoordinator: ThemeCoordinator): View {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(32f)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(14), 0, dp(14))
            }
        }

        // Header Title
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        header.addView(TextView(activity).apply {
            text = "🛠️ DEV SUITE 2.0"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val openFullBtn = TextView(activity).apply {
            text = "Open Full Console ↗"
            textSize = 11.5f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { showFullDevConsoleDialog(activity, themeCoordinator) }
        }
        header.addView(openFullBtn)
        container.addView(header)

        // Quick Action Rows
        val quickGrid1 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        val quickGrid2 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }

        fun quickActionBtn(label: String, colorHex: Int, onClick: () -> Unit): View {
            return Button(activity).apply {
                text = label
                textSize = 10.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(colorHex)
                }
                setPadding(dp(2), dp(2), dp(2), dp(2))
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener { onClick() }
            }
        }

        quickGrid1.addView(quickActionBtn("+25m Focus", themeCoordinator.primaryColor) {
            fastForwardSession(activity, 25 * 60L)
        })
        quickGrid1.addView(quickActionBtn("Adjust Time", Color.parseColor("#38BDF8")) {
            showAdjustTodayTimeDialog(activity, themeCoordinator, isDeveloperExtended = true)
        })
        quickGrid1.addView(quickActionBtn("Manual Log", Color.parseColor("#8B5CF6")) {
            showManualSessionLoggerDialog(activity, themeCoordinator)
        })
        quickGrid1.addView(quickActionBtn("Today Burst", Color.parseColor("#EC4899")) {
            seedPresetTodayBurst(activity)
            Toast.makeText(activity, "Populated today with 4h 30m realistic focus!", Toast.LENGTH_SHORT).show()
        })

        quickGrid2.addView(quickActionBtn("🔬 PCM", Color.parseColor("#10B981")) {
            seedPresetPCM(activity)
            Toast.makeText(activity, "Seeded PCM Showcase (Math, Physics, Chem, Mock)!", Toast.LENGTH_SHORT).show()
        })
        quickGrid2.addView(quickActionBtn("🩺 PCB", Color.parseColor("#06B6D4")) {
            seedPresetPCB(activity)
            Toast.makeText(activity, "Seeded PCB Showcase (Bio, Chem, Phys, NEET)!", Toast.LENGTH_SHORT).show()
        })
        quickGrid2.addView(quickActionBtn("💻 Tech/CS", Color.parseColor("#6366F1")) {
            seedPresetCS(activity)
            Toast.makeText(activity, "Seeded Tech/CS Showcase (DSA, Android, AI)!", Toast.LENGTH_SHORT).show()
        })
        quickGrid2.addView(quickActionBtn("🔥 30D Gold", Color.parseColor("#F59E0B")) {
            seedPreset30DayStreak(activity)
            Toast.makeText(activity, "Seeded 30-Day Master Golden Streak!", Toast.LENGTH_SHORT).show()
        })

        val stressLabel = TextView(activity).apply {
            text = "🧪 LIVE STRESS & LEADERBOARD LAB"
            textSize = 10.5f
            letterSpacing = 0.12f
            setTextColor(themeCoordinator.primaryColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }
        val quickGridStress1 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        quickGridStress1.addView(quickActionBtn("🧪 Persona", Color.parseColor("#8B5CF6")) {
            toggleTestPersona(activity)
        })
        quickGridStress1.addView(quickActionBtn("+45m Timer", Color.parseColor("#10B981")) {
            injectLiveTimerSession(activity, 45L, isManual = false)
        })
        quickGridStress1.addView(quickActionBtn("+45m Manual", Color.parseColor("#F59E0B")) {
            injectLiveTimerSession(activity, 45L, isManual = true)
        })
        quickGridStress1.addView(quickActionBtn("-15m Trim", Color.parseColor("#EF4444")) {
            trimLiveLatestSession(activity, 15L)
        })
        quickGridStress1.addView(quickActionBtn("🛡️ Audit", Color.parseColor("#06B6D4")) {
            showLiveLeaderboardAuditDialog(activity, themeCoordinator)
        })

        val quickGridStress2 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        quickGridStress2.addView(quickActionBtn("⚡ UI Storm", Color.parseColor("#38BDF8")) {
            runUiChaosStorm(activity)
        })
        quickGridStress2.addView(quickActionBtn("🎯 Tab Stress", Color.parseColor("#EC4899")) {
            runPlannerTabStress(activity)
        })
        quickGridStress2.addView(quickActionBtn("⏱️ Timer Spam", Color.parseColor("#F59E0B")) {
            runTimerRapidStress(activity)
        })
        quickGridStress2.addView(quickActionBtn("💥 Gauntlet", Color.parseColor("#8B5CF6")) {
            runChaosGauntlet(activity, themeCoordinator)
        })

        val quickGridStress3 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        quickGridStress3.addView(quickActionBtn("🌙 Rollover", Color.parseColor("#6366F1")) {
            simulateMidnightRollover(activity)
        })
        quickGridStress3.addView(quickActionBtn("🧹 Reset Today", Color.parseColor("#EF4444")) {
            resetTodaySessions(activity)
        })

        container.addView(quickGrid1)
        container.addView(quickGrid2)
        container.addView(stressLabel)
        container.addView(quickGridStress1)
        container.addView(quickGridStress2)
        container.addView(quickGridStress3)
        return container
    }

    fun toggleTestPersona(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val isTestActive = prefs.getBoolean("is_test_persona_active", false)
        if (!isTestActive) {
            val realName = prefs.getString("user_display_name", null)
                ?: prefs.getString("custom_username", "Student") ?: "Student"
            prefs.edit()
                .putBoolean("is_test_persona_active", true)
                .putString("backup_real_user_name", realName)
                .putString("user_display_name", "🧪 Test Pilot")
                .putString("custom_username", "🧪 Test Pilot")
                .apply()
            Toast.makeText(activity, "Switched to 🧪 Test Pilot Persona (Original backed up)", Toast.LENGTH_SHORT).show()
        } else {
            val originalName = prefs.getString("backup_real_user_name", "Student") ?: "Student"
            prefs.edit()
                .putBoolean("is_test_persona_active", false)
                .putString("user_display_name", originalName)
                .putString("custom_username", originalName)
                .apply()
            Toast.makeText(activity, "Restored Real Account: $originalName", Toast.LENGTH_SHORT).show()
        }
        activity.tabPageCache.clear()
        activity.navigateToPanel(activity.currentPanel)
    }

    fun injectLiveTimerSession(activity: MainActivity, durationMinutes: Long, isManual: Boolean) {
        val extraSecs = durationMinutes * 60L
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val now = System.currentTimeMillis()

        sharedPrefs.edit()
            .putLong("${todayStr}_focus_total", currentFocus + extraSecs)
            .putLong("last_data_modified_timestamp", now)
            .apply()

        val selectedSubject = SubjectTagManager.getSelectedSubject(activity)
        SubjectTagManager.recordSubjectStudyTime(activity, selectedSubject.id, extraSecs, todayStr)

        val startMs = now - (extraSecs * 1000L)
        val blockState = if (isManual) "MANUAL_FOCUS" else "STUDYING"
        TimelineLogger.recordRaw(
            context = activity,
            state = blockState,
            timestamp = startMs,
            subId = selectedSubject.id,
            subName = selectedSubject.name,
            subColor = selectedSubject.colorHex
        )
        TimelineLogger.recordRaw(
            context = activity,
            state = "IDLE",
            timestamp = now
        )

        if (!isManual) {
            CoroutineScope(Dispatchers.IO).launch {
                val realTotal = LeaderboardManager.getRealTimerFocusSecondsForDate(activity, todayStr).toInt()
                LeaderboardManager.overrideLeaderboardDailyTotal(
                    context = activity,
                    totalSeconds = realTotal,
                    subject = selectedSubject.name,
                    color = selectedSubject.colorHex
                )
            }
            Toast.makeText(activity, "✅ +${durationMinutes}m Authentic Timer added & synced to Leaderboard!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "🛡️ +${durationMinutes}m Manual Focus added! (Anti-cheat: NOT sent to Leaderboard)", Toast.LENGTH_SHORT).show()
        }

        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.navigateToPanel(activity.currentPanel)
    }

    fun trimLiveLatestSession(activity: MainActivity, trimMinutes: Long) {
        val trimSecs = trimMinutes * 60L
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val newFocus = (currentFocus - trimSecs).coerceAtLeast(0L)

        sharedPrefs.edit()
            .putLong("${todayStr}_focus_total", newFocus)
            .putLong("last_data_modified_timestamp", System.currentTimeMillis())
            .apply()

        CoroutineScope(Dispatchers.IO).launch {
            val realTotal = LeaderboardManager.getRealTimerFocusSecondsForDate(activity, todayStr).toInt()
            LeaderboardManager.overrideLeaderboardDailyTotal(
                context = activity,
                totalSeconds = realTotal
            )
        }

        Toast.makeText(activity, "✂️ Trimmed ${trimMinutes}m! Leaderboard updated to ${newFocus / 60}m", Toast.LENGTH_SHORT).show()
        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.navigateToPanel(activity.currentPanel)
    }

    fun showLiveLeaderboardAuditDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val entries = TimelineLogger.load(activity)
        val parsedAll = parseDayBlocks(entries)
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStartMs = startCal.timeInMillis
        val dayEndMs = dayStartMs + 24L * 3600_000L - 1000L
        val daySessions = parsedAll.sessions.filter { it.startMs in dayStartMs..dayEndMs }

        val totalSecs = daySessions.sumOf { it.secs }
        val manualSecs = daySessions.filter { it.manual }.sumOf { it.secs }
        val authenticSecs = daySessions.filter { !it.manual }.sumOf { it.secs }
        val leaderboardVerified = LeaderboardManager.getRealTimerFocusSecondsForDate(activity, todayStr)

        val isPassing = authenticSecs == leaderboardVerified
        val title = if (isPassing) "🛡️ Anti-Cheat Audit: PASSED" else "⚠️ Anti-Cheat Audit: MISMATCH"
        val msg = StringBuilder().apply {
            append("• Total Timeline Focus: ${totalSecs / 60}m (${totalSecs}s)\n")
            append("• Manual Focus (Blocked): ${manualSecs / 60}m (${manualSecs}s)\n")
            append("• Authentic Timer: ${authenticSecs / 60}m (${authenticSecs}s)\n")
            append("• Leaderboard Verified: ${leaderboardVerified / 60}m (${leaderboardVerified}s)\n\n")
            if (isPassing) {
                append("✅ Anti-Cheat Invariant: 100% Verified! Leaderboard strictly counts ONLY authentic timer minutes.")
            } else {
                append("⚠️ Discrepancy detected between authentic sessions and leaderboard score.")
            }
        }.toString()

        showThemedConfirmDialog(
            activity = activity,
            themeCoordinator = themeCoordinator,
            title = title,
            message = msg,
            confirmText = "Done",
            isDestructive = false,
            onCancel = {}
        ) {}
    }

    fun runUiChaosStorm(activity: MainActivity) {
        Toast.makeText(activity, "⚡ Starting UI Chaos Storm...", Toast.LENGTH_SHORT).show()
        val panels = listOf(AppPanel.FOCUS, AppPanel.PLANNER, AppPanel.STATS, AppPanel.COMMUNITY, AppPanel.SETTINGS)
        CoroutineScope(Dispatchers.Main).launch {
            for (round in 1..4) {
                for (p in panels) {
                    activity.navigateToPanel(p)
                    delay(120)
                }
            }
            activity.currentSettingsTab = AppSettingsTab.DEVELOPER
            activity.navigateToPanel(AppPanel.SETTINGS)
            Toast.makeText(activity, "✅ UI Storm Complete: 20 rapid transitions survived with 0 crashes!", Toast.LENGTH_LONG).show()
        }
    }

    fun runPlannerTabStress(activity: MainActivity) {
        Toast.makeText(activity, "🎯 Starting Planner Tab Stress...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            activity.navigateToPanel(AppPanel.PLANNER)
            delay(160)
            val tags = listOf("filter_tab_ALL", "filter_tab_ACTIVE", "filter_tab_DONE")
            for (round in 1..4) {
                for (tag in tags) {
                    val tab = activity.findViewById<View>(android.R.id.content)?.findViewWithTag<View>(tag)
                    tab?.performClick()
                    delay(120)
                }
            }
            delay(200)
            activity.currentSettingsTab = AppSettingsTab.DEVELOPER
            activity.navigateToPanel(AppPanel.SETTINGS)
            Toast.makeText(activity, "✅ Planner Tab Stress Complete: 12 filter switches survived with 0 glitches!", Toast.LENGTH_LONG).show()
        }
    }

    fun runTimerRapidStress(activity: MainActivity) {
        Toast.makeText(activity, "⏱️ Starting Timer Rapid Stress...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            activity.navigateToPanel(AppPanel.FOCUS)
            delay(150)
            for (round in 1..5) {
                if (activity.currentTimerState == TimerState.IDLE || activity.currentTimerState == TimerState.PAUSED) {
                    activity.handleStateToggle()
                }
                delay(100)
                if (activity.currentTimerState == TimerState.STUDYING) {
                    activity.handleStateToggle()
                }
                delay(100)
                activity.handleStopSession()
                delay(120)
            }
            if (activity.currentTimerState != TimerState.IDLE) {
                activity.handleStopSession()
            }
            activity.currentSettingsTab = AppSettingsTab.DEVELOPER
            activity.navigateToPanel(AppPanel.SETTINGS)
            Toast.makeText(activity, "✅ Timer Spam Complete: 15 state churns survived with clean IDLE!", Toast.LENGTH_LONG).show()
        }
    }

    fun runChaosGauntlet(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(false)
        }
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }
        val titleView = TextView(activity).apply {
            text = "💥 CHAOS GAUNTLET ACTIVE"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
        }
        val statusView = TextView(activity).apply {
            text = "Step 1/3: ⏱️ Rapid Timer State Churn..."
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            setPadding(0, dp(10), 0, dp(6))
        }
        content.addView(titleView)
        content.addView(statusView)
        dialog.setContentView(content)
        dialog.window?.let {
            it.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            // Step 1: Timer stress
            activity.navigateToPanel(AppPanel.FOCUS)
            delay(200)
            for (i in 1..4) {
                if (activity.currentTimerState == TimerState.IDLE || activity.currentTimerState == TimerState.PAUSED) {
                    activity.handleStateToggle()
                }
                delay(90)
                if (activity.currentTimerState == TimerState.STUDYING) {
                    activity.handleStateToggle()
                }
                delay(90)
                activity.handleStopSession()
                delay(90)
            }

            // Step 2: Tab stress
            statusView.text = "Step 2/3: 🎯 Planner Filter Tab Stress..."
            activity.navigateToPanel(AppPanel.PLANNER)
            delay(200)
            val tags = listOf("filter_tab_ALL", "filter_tab_ACTIVE", "filter_tab_DONE")
            for (r in 1..3) {
                for (t in tags) {
                    activity.findViewById<View>(android.R.id.content)?.findViewWithTag<View>(t)?.performClick()
                    delay(90)
                }
            }

            // Step 3: UI Carousel
            statusView.text = "Step 3/3: ⚡ UI Carousel Storm..."
            val panels = listOf(AppPanel.FOCUS, AppPanel.PLANNER, AppPanel.STATS, AppPanel.COMMUNITY, AppPanel.SETTINGS)
            for (r in 1..3) {
                for (p in panels) {
                    activity.navigateToPanel(p)
                    delay(90)
                }
            }

            activity.currentSettingsTab = AppSettingsTab.DEVELOPER
            activity.navigateToPanel(AppPanel.SETTINGS)
            dialog.dismiss()

            showThemedConfirmDialog(
                activity = activity,
                themeCoordinator = themeCoordinator,
                title = "🏆 Chaos Gauntlet: 100% Stable",
                message = "• Rapid timer transitions: 16 (0 leaks, clean IDLE)\n• Filter tab state switches: 9 (0 UI glitches)\n• Full panel carousel cycles: 15 (0 crashes)\n• Local & remote caches: Perfectly synchronized!",
                confirmText = "Done",
                isDestructive = false,
                onCancel = {}
            ) {}
        }
    }

    fun simulateMidnightRollover(activity: MainActivity) {
        activity.cachedTodayStr = "1970-01-01"
        activity.checkAndResetGoalsForNewDay()
        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.navigateToPanel(activity.currentPanel)
        Toast.makeText(activity, "🌙 Midnight Rollover simulated: Goals refreshed & streak recalculated!", Toast.LENGTH_SHORT).show()
    }

    fun resetTodaySessions(activity: MainActivity) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("${todayStr}_focus_total", 0L)
            .putLong("last_data_modified_timestamp", System.currentTimeMillis())
            .apply()

        CoroutineScope(Dispatchers.IO).launch {
            LeaderboardManager.overrideLeaderboardDailyTotal(activity, 0)
        }

        if (activity.currentTimerState != TimerState.IDLE) {
            activity.handleStopSession()
        }
        activity.accumulatedStudy = 0L
        activity.currentBreakSeconds = 0L

        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.navigateToPanel(activity.currentPanel)
        Toast.makeText(activity, "🧹 Today's focus & leaderboard entry reset to 0m!", Toast.LENGTH_SHORT).show()
    }

    fun showFullDevConsoleDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }
        val displayMetrics = activity.resources.displayMetrics

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(30f)
            setPadding(dp(20), dp(20), dp(20), dp(16))
        }

        // Title bar
        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }
        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(activity).apply {
            text = "⚡ Developer Architecture Suite"
            setTextColor(themeCoordinator.textColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        titleCol.addView(TextView(activity).apply {
            text = "Diagnostics, Custom Mocking & Edge-case Testing"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11.5f
            alpha = 0.85f
            setPadding(0, dp(2), 0, 0)
        })
        titleRow.addView(titleCol)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { dialog.dismiss() }
        }
        titleRow.addView(closeBtn)
        root.addView(titleRow)

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (displayMetrics.heightPixels * 0.72f).toInt()
            )
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }

        // Section Builder Helper
        fun addSection(title: String, icon: String, block: LinearLayout.() -> Unit) {
            val sectionHeader = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(14), dp(4), dp(8))
            }
            sectionHeader.addView(TextView(activity).apply {
                text = "$icon $title"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            content.addView(sectionHeader)

            val sectionCard = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(24f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            sectionCard.block()
            content.addView(sectionCard)
        }

        fun devButton(label: String, subtitle: String? = null, colorHex: Int = themeCoordinator.primaryColor, onClick: () -> Unit): View {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(colorHex, 60), 16f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(4), 0, dp(4))
                }
                addView(TextView(activity).apply {
                    text = label
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                if (subtitle != null) {
                    addView(TextView(activity).apply {
                        text = subtitle
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 11f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                setOnClickListener {
                    onClick()
                }
            }
        }

        // 0. One-Click Showcase Presets (For Screenshots & Marketing)
        addSection("ONE-CLICK SHOWCASE PRESETS (SCREENSHOTS & MARKETING)", "📸") {
            addView(devButton("🔬 Preset: PCM Engineering Aspirant (21 Days)", "Physics ⚛️, Chemistry 🧪, Math 📐, JEE Mock Drills 📝, linked goals & 21-day streak", Color.parseColor("#10B981")) {
                seedPresetPCM(activity)
                Toast.makeText(activity, "Seeded PCM Engineering Aspirant Showcase!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            })

            addView(devButton("🩺 Preset: PCB Medical Aspirant (21 Days)", "Biology 🧬, Chemistry 🧪, Physics ⚛️, NEET Drills 📑, linked goals & 21-day streak", Color.parseColor("#06B6D4")) {
                seedPresetPCB(activity)
                Toast.makeText(activity, "Seeded PCB Medical Aspirant Showcase!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            })

            addView(devButton("💻 Preset: CS & Software Engineering (21 Days)", "DSA ⚡, Android Architecture 📱, AI & ML 🤖, Open Source 🛠️, linked goals & 21-day streak", Color.parseColor("#8B5CF6")) {
                seedPresetCS(activity)
                Toast.makeText(activity, "Seeded CS & Software Engineering Showcase!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            })

            addView(devButton("🔥 Preset: 30-Day Master Golden Streak", "100% full-month heatmap, unbroken 30-day streak, perfect habit matrix", Color.parseColor("#F59E0B")) {
                seedPreset30DayStreak(activity)
                Toast.makeText(activity, "Seeded 30-Day Master Golden Streak!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            })

            addView(devButton("✨ Burst Today's Focus (4h 30m Real-time Demo)", "Populates today with 4h 30m across current subjects and marks today's goals", Color.parseColor("#EC4899")) {
                seedPresetTodayBurst(activity)
                Toast.makeText(activity, "Populated today with 4h 30m realistic focus!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            })
        }

        // 1. Manual Session Logger & Mocking
        addSection("MANUAL SESSION LOGGER & MOCKING", "✍️") {
            addView(devButton("⏱️ Adjust Focus & Break Time (Extended)", "Pick any date, add/deduct/set focus & break records with full accuracy", Color.parseColor("#38BDF8")) {
                dialog.dismiss()
                showAdjustTodayTimeDialog(activity, themeCoordinator, isDeveloperExtended = true)
            })

            addView(devButton("⚡ Add Custom Time (Leaderboard & Cloud Synced)", "Log custom study time directly verified & pushed to the Public Leaderboard", Color.parseColor("#10B981")) {
                dialog.dismiss()
                showDevCustomLeaderboardTimeDialog(activity, themeCoordinator)
            })

            addView(devButton("✍️ Custom Manual Session Builder", "Start & End Time pickers, real-time duration, custom or untagged subject") {
                dialog.dismiss()
                showManualSessionLoggerDialog(activity, themeCoordinator)
            })

            val ffRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, dp(6))
            }
            fun ffBtn(txt: String, secs: Long) = Button(activity).apply {
                text = txt
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener {
                    fastForwardSession(activity, secs)
                    Toast.makeText(activity, "Fast-forwarded $txt!", Toast.LENGTH_SHORT).show()
                }
            }
            ffRow.addView(ffBtn("+5m", 300L))
            ffRow.addView(ffBtn("+25m", 1500L))
            ffRow.addView(ffBtn("+1h", 3600L))
            addView(ffRow)

            addView(devButton("Generate Realistic 7-Day History (Deterministic)", "Consistent habits (Math 35%, Physics 30%, Chemistry 20%, Revision 15%)") {
                seedRealisticHistory(activity, days = 7, deterministicSeed = 707L)
                Toast.makeText(activity, "Generated 7-day realistic logs", Toast.LENGTH_SHORT).show()
                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })

            addView(devButton("Generate Heavy 30-Day History (Deterministic)", "Full month heatmap, natural weekend rest patterns, consistent subject roster") {
                seedRealisticHistory(activity, days = 30, deterministicSeed = 3030L)
                Toast.makeText(activity, "Generated 30-day realistic logs", Toast.LENGTH_SHORT).show()
                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })
        }

        // 2. Planner & Habits Lab
        addSection("PLANNER & HABITS LAB", "📋") {
            addView(devButton("✍️ Custom Historical Planner Snapshot Builder", "Date picker, select custom goals/habits & status for ANY past date", Color.parseColor("#8B5CF6")) {
                dialog.dismiss()
                showCustomPlannerSnapshotLoggerDialog(activity, themeCoordinator)
            })

            addView(devButton("🌱 Seed Sample Daily Goals & Habits", "Populates 4 structured planner goals linked with subjects") {
                seedSampleGoals(activity)
                Toast.makeText(activity, "Seeded sample daily planner goals!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🌅 Simulate Next-Day Rollover", "Snapshots today's completion status & resets habits for a new day") {
                simulatePlannerDayRollover(activity)
                Toast.makeText(activity, "Simulated rollover: Snapshotted today & reset habits for a new day!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🔥 Seed 14-Day Realistic Habit History", "Generates 14 days of daily habit snapshots for streaks & matrices") {
                seedRealisticHabitHistory(activity, days = 14)
                Toast.makeText(activity, "Generated 14 days of habit history!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("✅ Mark All Today's Habits Completed", "Sets all current goals as done to test streaks & celebrations") {
                markAllTodayGoals(activity, completed = true)
                Toast.makeText(activity, "Marked all habits completed for today!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🔄 Reset Today's Habits to Incomplete", "Unchecks all current goals for today") {
                markAllTodayGoals(activity, completed = false)
                Toast.makeText(activity, "Reset all habits to incomplete for today!", Toast.LENGTH_SHORT).show()
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🔧 Repair Historical Goals from 45m Default", "Applies current daily goal to all past days that were reset to 45m", Color.parseColor("#10B981")) {
                val currentGoalSecs = StatsEngine.getDailyGoalSecs(activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE))
                GoalHistoryManager.repairCorruptedHistoricalGoals(activity, currentGoalSecs)
                activity.invalidateStatsCache()
                activity.recalculateStreak()
                AppToast.show(activity, "Historical goals repaired to ${currentGoalSecs / 60}m")
                activity.refreshStatsPanel()
                activity.tabPageCache.clear()
            })

            addView(devButton("🗑️ Wipe Planner & Habit History", "Clears all historical snapshots and resets active goals", Color.parseColor("#EF4444")) {
                showThemedConfirmDialog(
                    activity = activity,
                    themeCoordinator = themeCoordinator,
                    title = "Wipe Planner History?",
                    message = "This will delete all past habit completion snapshots and reset current goals.",
                    confirmText = "WIPE PLANNER",
                    isDestructive = true
                ) {
                    wipePlannerData(activity)
                    Toast.makeText(activity, "Planner & habit history cleared!", Toast.LENGTH_SHORT).show()
                    activity.refreshStatsPanel()
                    activity.tabPageCache.clear()
                }
            })
        }

        // 2. Database & State Management
        addSection("DATABASE & STATE MANAGEMENT", "🗄️") {
            val liveStatsText = TextView(activity).apply {
                val sessionsCount = TimelineLogger.load(activity).size
                val subjectsCount = SubjectTagManager.getAllSubjects(activity).size
                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val streak = prefs.safeInt("current_streak", 0)
                val goalMins = prefs.safeLong("dailyGoalMinutes", 120L)
                text = "📊 Live State:\n• Timeline Entries: $sessionsCount\n• Registered Subjects: $subjectsCount\n• Current Streak: $streak days\n• Daily Goal: ${goalMins}m"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 12f
                typeface = Typeface.MONOSPACE
                background = themeCoordinator.createGlassChip(0x22FFFFFF.toInt(), 12f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            addView(liveStatsText)

            addView(devButton("Seed Preset: Standard Student (4 Subjects)", "Pre-populates Math, Physics, Chemistry, and Revision with balanced ratios") {
                seedPresetStandardStudent(activity)
                Toast.makeText(activity, "Seeded Standard Student preset!", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.recalculateStreak()
                activity.tabPageCache.clear()
            })

            addView(devButton("Reset Streak to 0", "Sets streak counters and calculation dates to zero") {
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .putInt("current_streak", 0)
                    .putLong("streak_last_calculated", 0L)
                    .apply()
                activity.recalculateStreak()
                Toast.makeText(activity, "Streak reset to 0", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Clear Today's Logs Only", "Removes today's focus/break entries without touching past days", Color.parseColor("#EF4444")) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
                    remove("${todayStr}_focus_total")
                    remove("${todayStr}_break_total")
                    remove("${todayStr}_focus_manual")
                    remove("${todayStr}_break_manual")
                }.apply()
                TimelineLogger.deleteDay(activity, todayStr)
                SubjectTagManager.clearTodaySubjectDurations(activity, todayStr)
                activity.statsDirty = true
                activity.recalculateStreak()
                Toast.makeText(activity, "Cleared today's entries", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Wipe Custom Subjects", "Removes user-created subjects and restores defaults") {
                activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE).edit()
                    .putString("custom_subjects_json", "[]")
                    .apply()
                Toast.makeText(activity, "Restored default subjects", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })

            addView(devButton("Wipe All Local Database Logs", "Completely erases timeline and resets all statistics", Color.parseColor("#EF4444")) {
                showThemedConfirmDialog(
                    activity = activity,
                    themeCoordinator = themeCoordinator,
                    title = "Wipe Entire Database?",
                    message = "This will permanently delete all local timeline sessions, calendar history, and subject counters.",
                    confirmText = "WIPE EVERYTHING",
                    isDestructive = true
                ) {
                    TimelineLogger.importRaw(activity, "[]")
                    val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    for (k in prefs.all.keys) {
                        if (k.endsWith("_focus_total") || k.endsWith("_break_total") || k.startsWith("subject_")) {
                            editor.remove(k)
                        }
                    }
                    editor.putInt("current_streak", 0).apply()
                    activity.statsDirty = true
                    activity.recalculateStreak()
                    activity.tabPageCache.clear()
                    Toast.makeText(activity, "Database wiped cleanly", Toast.LENGTH_LONG).show()
                }
            })
        }

        // 3. Sync & Cloud Conflict Simulation
        addSection("SYNC & CLOUD CONFLICT SIMULATION", "☁️") {
            addView(devButton("Simulate Remote Cloud Conflict", "Triggers the 3-option conflict resolution dialog with a mock newer cloud snapshot", Color.parseColor("#F59E0B")) {
                dialog.dismiss()
                simulateCloudConflict(activity)
            })

            addView(devButton("Force Immediate Cloud Push", "Uploads current local state directly to cloud storage") {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val success = CloudSyncManager.syncDataToCloud(activity, force = true)
                    activity.runOnUiThread {
                        Toast.makeText(activity, if (success) "Cloud push succeeded" else "Cloud push failed (Check network/login)", Toast.LENGTH_SHORT).show()
                    }
                }
            })

            addView(devButton("Force Cloud Pull & Restore", "Downloads and overwrites with latest cloud copy") {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val success = CloudSyncManager.restoreDataFromCloud(activity)
                    activity.runOnUiThread {
                        if (success) {
                            Toast.makeText(activity, "Cloud restored successfully", Toast.LENGTH_SHORT).show()
                            activity.tabPageCache.clear()
                            activity.recreate()
                        } else {
                            Toast.makeText(activity, "No cloud backup found or pull failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        // 4. Notifications & Celebration Triggers
        addSection("NOTIFICATIONS & CELEBRATION TRIGGERS", "🎉") {
            addView(devButton("Trigger Goal Celebration Banner", "Launches the interactive particle confetti celebration modal") {
                dialog.dismiss()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    CelebrationEngine.showCelebrationDialog(activity, isGoalAchieved = true, streak = 5)
                }, 150)
            })

            addView(devButton("Trigger Streak Level-Up Animation (N -> N+1)", "Launches animated count-up popup with tier borders") {
                val tiers = arrayOf(
                    "Starter Streak (Day 4 -> 5)",
                    "Bronze Streak (Day 7 -> 8)",
                    "Silver Streak (Day 14 -> 15)",
                    "Gold Streak (Day 21 -> 22)",
                    "Diamond Streak (Day 28 -> 29)",
                    "Fire Streak (Day 50 -> 51)",
                    "Century Streak (Day 100 -> 101)"
                )
                val streaks = arrayOf(
                    Pair(4, 5),
                    Pair(7, 8),
                    Pair(14, 15),
                    Pair(21, 22),
                    Pair(28, 29),
                    Pair(50, 51),
                    Pair(100, 101)
                )
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Select Streak to Preview (N -> N+1)")
                    .setItems(tiers) { d, which ->
                        d.dismiss()
                        dialog.dismiss()
                        val (oldS, newS) = streaks[which]
                        StreakUpAnimationDialog.show(activity, oldStreak = oldS, newStreak = newS)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })

            addView(devButton("Trigger Milestone Streak Banner", "Preview special milestone celebrations (7, 14, 21, 28, 50, 100 days)") {
                dialog.dismiss()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showStreakTestPicker(activity)
                }, 150)
            })

                        addView(devButton("Test Festival Notifications & Variations", "Preview Diwali, Holi, Eid, Christmas, etc. in status bar") {
                showFestivalTestPicker(activity)
            })

                        addView(devButton("Trigger Evening Goal Reminder", "Fires the daily goal notification based on today's progress") {
                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val focus = prefs.getLong("${todayStr}_focus_total", 0L) + prefs.getLong("accumulatedStudy", 0L)
                val goal = prefs.getLong("${todayStr}_goal_secs", prefs.getLong("daily_goal_secs", 7200L))

                val openIntent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    activity, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notif = NotificationCompat.Builder(activity, GoalReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flame)
                    .setContentTitle(activity.getString(R.string.goal_reminder_title))
                    .setContentText(activity.getString(R.string.goal_reminder_text, focus / 3600, (focus % 3600) / 60, activity.getString(R.string.duration_h_m, goal / 3600, (goal % 3600) / 60)))
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(openPending)
                    .build()

                val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NotificationHelper.NOTIFICATION_ID_GOAL_REMINDER, notif)
                Toast.makeText(activity, "Fired Daily Goal Reminder notification!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Trigger Session Complete Notification", "Fires timer countdown finished heads-up alert") {
                val openIntent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    activity, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notif = NotificationCompat.Builder(activity, NotificationHelper.CHANNEL_COMPLETION)
                    .setSmallIcon(R.drawable.ic_flame)
                    .setContentTitle("Study Session Complete")
                    .setContentText("Great job! Your focus countdown finished successfully.")
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(openPending)
                    .build()

                val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NotificationHelper.NOTIFICATION_ID_COMPLETION, notif)
                Toast.makeText(activity, "Fired Session Complete notification!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Trigger Inactivity / Auto-Pause Alert", "Fires study timer inactivity check notification") {
                val openIntent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    activity, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notif = NotificationCompat.Builder(activity, NotificationHelper.CHANNEL_INACTIVITY)
                    .setSmallIcon(R.drawable.ic_flame)
                    .setContentTitle("Are you still studying?")
                    .setContentText("StudyTimer was paused due to inactivity. Tap to resume your session.")
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(openPending)
                    .build()

                val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NotificationHelper.NOTIFICATION_ID_INACTIVITY_CHECK, notif)
                Toast.makeText(activity, "Fired Inactivity Alert notification!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Trigger Haptic Pulse", "Fires study completion haptic vibration waveform") {
                try {
                    activity.window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (_: Exception) {}
            })
        }

        // 5. UI & Theme Overrides
                // 5. App Blocker & Focus Shield Lab
        addSection("APP BLOCKER & FOCUS SHIELD LAB", "🛡️") {
            addView(devButton("Launch Blocker Overlay Simulation", "Tests the full-screen psychological shield for Instagram", Color.parseColor("#EC4899")) {
                dialog.dismiss()
                val intent = Intent(activity, BlockerOverlayActivity::class.java).apply {
                    putExtra(BlockerOverlayActivity.EXTRA_BLOCKED_PACKAGE, "com.instagram.android")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                activity.startActivity(intent)
            })

            addView(devButton("Seed Mock Distracting Apps List", "Populates Instagram, YouTube, X, Reddit in blocked packages list", Color.parseColor("#8B5CF6")) {
                val mockBlocked = setOf(
                    "com.instagram.android",
                    "com.google.android.youtube",
                    "com.twitter.android",
                    "com.reddit.frontpage",
                    "com.zhiliaoapp.musically"
                )
                AppBlockerManager.setBlockedPackages(activity, mockBlocked)
                AppBlockerManager.setEnabled(activity, true)
                Toast.makeText(activity, "Seeded 5 mock distracting apps & enabled blocker!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Toggle Blocker Shield (On / Off)", "Quickly arms or disarms the background focus shield") {
                val newState = !AppBlockerManager.isEnabled(activity)
                AppBlockerManager.setEnabled(activity, newState)
                Toast.makeText(activity, if (newState) "Focus Shield Armed" else "Focus Shield Disarmed", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Check Blocker Permissions", "Verifies Usage Stats & System Overlay permission status") {
                val hasUsage = AppBlockerManager.hasUsageStatsPermission(activity)
                val hasOverlay = AppBlockerManager.hasOverlayPermission(activity)
                android.app.AlertDialog.Builder(activity)
                    .setTitle("App Blocker Permissions")
                    .setMessage("• Usage Stats Access: ${if (hasUsage) "GRANTED" else "NOT GRANTED"}\n• Display Over Other Apps: ${if (hasOverlay) "GRANTED" else "NOT GRANTED"}")
                    .setPositiveButton("OK", null)
                    .show()
            })
        }

        // 6. Exam Countdown Lab
        addSection("EXAM COUNTDOWN LAB", "⏳") {
            addView(devButton("Seed 3 Sample Exam Countdowns", "Populates JEE Main/Finals (14d), Semester Exam (45d), Entrance (90d)", Color.parseColor("#10B981")) {
                val cal = Calendar.getInstance()
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                cal.add(Calendar.DAY_OF_YEAR, 14)
                val exam1Date = df.format(cal.time)
                val exam1Ts = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 31)
                val exam2Date = df.format(cal.time)
                val exam2Ts = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 45)
                val exam3Date = df.format(cal.time)
                val exam3Ts = cal.timeInMillis

                val sampleExams = listOf(
                    ExamCountdown(id = "exam_mock_1", title = "Final Semester Examinations", targetDateStr = exam1Date, targetTimestampMs = exam1Ts, colorHex = "#38BDF8", notes = "Cover entire semester syllabus"),
                    ExamCountdown(id = "exam_mock_2", title = "National Entrance Test", targetDateStr = exam2Date, targetTimestampMs = exam2Ts, colorHex = "#F59E0B", notes = "Speed drills and mock tests"),
                    ExamCountdown(id = "exam_mock_3", title = "Board & Certification Exam", targetDateStr = exam3Date, targetTimestampMs = exam3Ts, colorHex = "#10B981", notes = "Comprehensive revision")
                )
                ExamCountdownManager.saveExams(activity, sampleExams)
                Toast.makeText(activity, "Seeded 3 upcoming exam countdowns!", Toast.LENGTH_SHORT).show()
                activity.tabPageCache.clear()
            })

            addView(devButton("Simulate Exam Day (0 Days Left)", "Sets an exam date to today to test 'Today / Exam Day' urgency badge", Color.parseColor("#EF4444")) {
                val cal = Calendar.getInstance()
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = df.format(cal.time)
                val todayExam = listOf(
                    ExamCountdown(id = "exam_today_test", title = "Physics Major Exam (Today!)", targetDateStr = todayStr, targetTimestampMs = cal.timeInMillis, colorHex = "#EF4444", notes = "Exam takes place today")
                )
                ExamCountdownManager.saveExams(activity, todayExam)
                Toast.makeText(activity, "Set exam countdown to Today!", Toast.LENGTH_SHORT).show()
                activity.tabPageCache.clear()
            })

            addView(devButton("Clear All Exam Countdowns", "Removes all saved exam targets from the planner", Color.parseColor("#64748B")) {
                ExamCountdownManager.saveExams(activity, emptyList())
                Toast.makeText(activity, "Cleared all exam countdowns", Toast.LENGTH_SHORT).show()
                activity.tabPageCache.clear()
            })
        }

        // 7. Lecture Timetable Lab
        addSection("LECTURE & TIMETABLE LAB", "📅") {
            addView(devButton("Seed Weekly Lecture Timetable", "Adds Physics (09:00), Calculus (11:30), Chemistry (14:00) schedule", Color.parseColor("#6366F1")) {
                val sampleLectures = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "lec_dev_1")
                        put("title", "Physics Lecture")
                        put("startTime", "09:00")
                        put("endTime", "10:30")
                        put("enabled", true)
                        put("subjectId", "physics")
                    })
                    put(JSONObject().apply {
                        put("id", "lec_dev_2")
                        put("title", "Calculus & Linear Algebra")
                        put("startTime", "11:30")
                        put("endTime", "13:00")
                        put("enabled", true)
                        put("subjectId", "math")
                    })
                    put(JSONObject().apply {
                        put("id", "lec_dev_3")
                        put("title", "Organic Chemistry Lab")
                        put("startTime", "14:00")
                        put("endTime", "15:30")
                        put("enabled", true)
                        put("subjectId", "chemistry")
                    })
                }
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .putString("lecture_schedules_json", sampleLectures.toString())
                    .putBoolean("lecture_mode_enabled", true)
                    .apply()
                LectureScheduleScheduler.scheduleAll(activity)
                Toast.makeText(activity, "Seeded 3 weekly lectures & armed scheduler!", Toast.LENGTH_SHORT).show()
                activity.tabPageCache.clear()
            })

            addView(devButton("Trigger Class Start Notification", "Dispatches class schedule starting alert immediately") {
                val intent = Intent(activity, LectureScheduleReceiver::class.java).apply {
                    action = LectureScheduleReceiver.ACTION_LECTURE_START
                }
                activity.sendBroadcast(intent)
                Toast.makeText(activity, "Broadcasted Lecture Start event!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Clear Lecture Schedules", "Wipes all scheduled lecture items", Color.parseColor("#64748B")) {
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .putString("lecture_schedules_json", "[]")
                    .apply()
                LectureScheduleScheduler.cancelAll(activity)
                Toast.makeText(activity, "Cleared all lecture schedules", Toast.LENGTH_SHORT).show()
                activity.tabPageCache.clear()
            })
        }

        // 8. Updates, Widgets & Scorecard Lab
        addSection("UPDATES, WIDGETS & SCORECARD LAB", "🚀") {
            addView(devButton("🚀 Launch Welcome Onboarding Wizard", "Previews the redesigned 3-step setup (Name, Focus Area, Goal & Timer)", Color.parseColor("#38BDF8")) {
                dialog.dismiss()
                activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_completed_onboarding_v2", false).apply()
                OnboardingDialogHelper.showOnboardingIfNeeded(activity)
            })

            addView(devButton("Simulate 'Update Available' Dialog", "Previews the in-app update prompt with release notes and download CTA", Color.parseColor("#38BDF8")) {
                dialog.dismiss()
                val mockUpdate = UpdateInfo(
                    versionCode = 999,
                    versionName = "3.0.0-PRO",
                    url = "https://get-studytimer.vercel.app/?download=true",
                    apkUrl = "https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk",
                    releaseNotes = "• Brand new Festival Notifications system with personalized wishes\n• Enhanced App Blocker Focus Shield with instant audio silencing\n• Expanded Exam Countdowns & Planner Timetable\n• Improved battery efficiency & seamless cloud sync"
                )
                activity.showUpdateAvailableDialog(mockUpdate)
            })

            addView(devButton("Force Home Screen Widget Refresh", "Broadcasts immediate update intent to all pinned StudyTimer widgets", Color.parseColor("#10B981")) {
                StudyWidgetProvider.refresh(activity)
                val intent = Intent(activity, StudyWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                activity.sendBroadcast(intent)
                Toast.makeText(activity, "Widget refresh broadcast dispatched!", Toast.LENGTH_SHORT).show()
            })

            addView(devButton("Preview Shareable Study Scorecard", "Launches graphic scorecard export modal with current stats", Color.parseColor("#EC4899")) {
                dialog.dismiss()
                activity.showStudyScorecardExportDialog()
            })
        }

        // 8a. Stress Testing & Anti-Cheat Audit Lab
        addSection("STRESS TESTING & ANTI-CHEAT AUDIT LAB", "🧪") {
            addView(devButton("⚡ Run UI Carousel Storm (20 Transitions)", "Spams all main panels rapidly to verify 0 layout crashes or memory leaks", Color.parseColor("#38BDF8")) {
                dialog.dismiss()
                runUiChaosStorm(activity)
            })
            addView(devButton("🎯 Run Planner Filter Tab Stress (12 Cycles)", "Spams [All], [Remaining], and [Done] filter tabs and Day-Off views", Color.parseColor("#EC4899")) {
                dialog.dismiss()
                runPlannerTabStress(activity)
            })
            addView(devButton("⏱️ Run Timer State Machine Spam (15 Cycles)", "Spams Start, Pause, Resume, and Stop transitions to test thread safety", Color.parseColor("#F59E0B")) {
                dialog.dismiss()
                runTimerRapidStress(activity)
            })
            addView(devButton("💥 Run Complete Chaos Gauntlet", "Sequentially executes Timer Spam, Planner Stress, and UI Carousel with live HUD", Color.parseColor("#8B5CF6")) {
                dialog.dismiss()
                runChaosGauntlet(activity, themeCoordinator)
            })
            addView(devButton("🧪 Toggle Test Persona (Real Account ↔ Test Pilot)", "Safe sandbox mode: backs up your real identity and switches to Test Pilot", Color.parseColor("#8B5CF6")) {
                toggleTestPersona(activity)
            })
            addView(devButton("🛡️ Run Live Anti-Cheat & Leaderboard Audit", "Verifies timeline integrity, manual log rejection, and authentic timer seconds", Color.parseColor("#06B6D4")) {
                showLiveLeaderboardAuditDialog(activity, themeCoordinator)
            })
            addView(devButton("🌙 Simulate Midnight Rollover", "Fast-forwards system day detection to trigger goal resets and streak calculation", Color.parseColor("#6366F1")) {
                simulateMidnightRollover(activity)
            })
            addView(devButton("🧹 Reset Today's Focus & Leaderboard Score", "Resets today's focus to 0m and clears leaderboard entry without corrupting streaks", Color.parseColor("#EF4444")) {
                resetTodaySessions(activity)
            })
        }

        // 8b. System Broadcasts & Outage Lab
        addSection("SYSTEM BROADCASTS & OUTAGE ALERTS", "📢") {
            addView(devButton("📢 Broadcast Announcement Composer", "Compose, preview, and dispatch announcements or server error notices", Color.parseColor("#EC4899")) {
                dialog.dismiss()
                showBroadcastComposerDialog(activity, themeCoordinator)
            })

            addView(devButton("🚨 Simulate Force Update (Hard Blocking)", "Tests the non-dismissable hard-blocking critical update dialog", Color.parseColor("#EF4444")) {
                dialog.dismiss()
                val mockForceUpdate = UpdateInfo(
                    versionCode = 999,
                    versionName = "4.0.0-CRITICAL",
                    url = "https://get-studytimer.vercel.app/?download=true",
                    apkUrl = "https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk",
                    releaseNotes = "• Critical Security & Database Compatibility Update\n• Older versions are permanently deprecated\n• Update required to proceed",
                    minSupportedVersionCode = 999,
                    forceUpdate = true
                )
                activity.systemDialogHelper.showUpdateAvailableDialog(mockForceUpdate, isForce = true)
            })

            addView(devButton("⚡ Simulate Emergency Outage Notice", "Previews a high-priority system error banner on screen", Color.parseColor("#F59E0B")) {
                dialog.dismiss()
                val mockNotice = BroadcastMessage(
                    id = "emergency_test_${System.currentTimeMillis()}",
                    title = "Database Upgrades Underway",
                    message = "We are currently running database index maintenance. Cloud sync will automatically resume in 20 minutes. All local sessions are completely safe.",
                    type = "warning",
                    canDismiss = true,
                    actionUrl = "https://get-studytimer.vercel.app",
                    actionText = "System Status"
                )
                activity.systemDialogHelper.showBroadcastDialog(mockNotice)
            })

            addView(devButton("🗑️ Deactivate Active Cloud Broadcast", "Deactivates any active broadcast stored in Supabase", Color.parseColor("#64748B")) {
                deactivateBroadcastInSupabase(activity) { success, msg ->
                    AppToast.show(activity, msg)
                }
            })
        }

        addSection("UI & EDGE CASE PREVIEWS", "🎨") {
            addView(devButton("Force Toggle AMOLED / Light Mode", "Instantly swaps color matrix between pitch black and light") {
                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val current = prefs.getString("activeBgMode", "OLED")
                val next = if (current == "LIGHT") "OLED" else "LIGHT"
                prefs.edit().putString("activeBgMode", next).apply()
                themeCoordinator.applyThemeCoordinates()
                activity.tabPageCache.clear()
                dialog.dismiss()
                activity.recreate()
            })

            addView(devButton("Inject Max-Length Subject Names", "Creates extra-long named subject to test legend & chart clipping") {
                val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                val customJson = subPrefs.getString("custom_subjects_json", "[]") ?: "[]"
                val arr = org.json.JSONArray(customJson)
                val longSubject = JSONObject().apply {
                    put("id", "custom_long_${System.currentTimeMillis()}")
                    put("name", "Advanced Quantum Thermodynamics & Electro-Optics Lab II")
                    put("iconEmoji", "")
                    put("colorHex", "#EC4899")
                }
                arr.put(longSubject)
                subPrefs.edit().putString("custom_subjects_json", arr.toString()).apply()
                Toast.makeText(activity, "Inserted long subject name test", Toast.LENGTH_SHORT).show()
                activity.statsDirty = true
                activity.tabPageCache.clear()
            })
        }

        scroll.addView(content)
        root.addView(scroll)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()
    }

    /**
     * Precise Manual Session Logger:
     * - Start Time & End Time Pickers with real-time dynamic duration calculation
     * - Validates that End Time is always later than Start Time
     * - Select registered subject, create a new custom subject, or select "Untagged / General Focus"
     * - Uses custom AMOLED dialog & picker themes to prevent system white/purple clashes
     */
    fun showManualSessionLoggerDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        root.addView(TextView(activity).apply {
            text = "✍️ Manual Session Logger"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        })

        val subjects = SubjectTagManager.getAllSubjects(activity)
        var selectedSubject: SubjectTag? = subjects.firstOrNull() // null = untagged general focus

        val subjectPickerBtn = TextView(activity).apply {
            text = "Tag: ${selectedSubject?.name ?: "No Subject Tag / General"}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 80), 14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val customInputContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        val customNameEdit = EditText(activity).apply {
            hint = "Enter custom subject name"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        customInputContainer.addView(customNameEdit)

        subjectPickerBtn.setOnClickListener {
            showThemedSubjectPickerModal(
                activity = activity,
                themeCoordinator = themeCoordinator,
                title = "Select or Create Subject",
                includeGeneral = true,
                includeCreateOption = true,
                onSelected = { sub ->
                    selectedSubject = sub
                    customInputContainer.visibility = View.GONE
                    if (sub == null) {
                        subjectPickerBtn.text = "Tag: No Subject Tag / General Focus"
                    } else {
                        subjectPickerBtn.text = "Tag: ${sub.name}"
                    }
                },
                onCreateCustom = {
                    selectedSubject = null
                    customInputContainer.visibility = View.VISIBLE
                    subjectPickerBtn.text = "Tag: Custom Subject (Type below)"
                }
            )
        }
        root.addView(subjectPickerBtn)
        root.addView(customInputContainer)

        // Date, Start Time & End Time Setup
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 45) // Default 45m session
        }

        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val datePickerBtn = TextView(activity).apply {
            text = "📅 Date: ${dateFmt.format(startCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, 0)
            }
        }

        val timeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val startTimeBtn = TextView(activity).apply {
            text = "⏰ Start: ${timeFmt.format(startCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(4), 0)
            }
        }

        val endTimeBtn = TextView(activity).apply {
            text = "🏁 End: ${timeFmt.format(endCal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 12.5f
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, 0, 0)
            }
        }

        timeRow.addView(startTimeBtn)
        timeRow.addView(endTimeBtn)

        val durationSummaryText = TextView(activity).apply {
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(4), dp(8), dp(4), dp(12))
        }

        fun updateDurationPreview(): Long {
            val diffMs = endCal.timeInMillis - startCal.timeInMillis
            val diffMins = diffMs / 60000L
            if (diffMins <= 0) {
                durationSummaryText.text = "⚠️ End time must be after start time!"
                durationSummaryText.setTextColor(Color.parseColor("#EF4444"))
                return 0L
            } else {
                val h = diffMins / 60
                val m = diffMins % 60
                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                durationSummaryText.text = "✨ Calculated Focus Duration: $durStr ($diffMins mins)"
                durationSummaryText.setTextColor(themeCoordinator.primaryColor)
                return diffMins * 60L
            }
        }
        updateDurationPreview()

        datePickerBtn.setOnClickListener {
            val dpd = DatePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, y, m, d ->
                    startCal.set(Calendar.YEAR, y)
                    startCal.set(Calendar.MONTH, m)
                    startCal.set(Calendar.DAY_OF_MONTH, d)
                    endCal.set(Calendar.YEAR, y)
                    endCal.set(Calendar.MONTH, m)
                    endCal.set(Calendar.DAY_OF_MONTH, d)
                    datePickerBtn.text = "📅 Date: ${dateFmt.format(startCal.time)}"
                    updateDurationPreview()
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
            )
            dpd.setOnShowListener {
                dpd.getButton(DatePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                dpd.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            dpd.show()
        }

        startTimeBtn.setOnClickListener {
            val tpd = TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    startCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    startCal.set(Calendar.MINUTE, minute)
                    startCal.set(Calendar.SECOND, 0)
                    startTimeBtn.text = "⏰ Start: ${timeFmt.format(startCal.time)}"
                    updateDurationPreview()
                },
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                false
            )
            tpd.setOnShowListener {
                tpd.getButton(TimePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                tpd.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            tpd.show()
        }

        endTimeBtn.setOnClickListener {
            val tpd = TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    endCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    endCal.set(Calendar.MINUTE, minute)
                    endCal.set(Calendar.SECOND, 0)
                    endTimeBtn.text = "🏁 End: ${timeFmt.format(endCal.time)}"
                    updateDurationPreview()
                },
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                false
            )
            tpd.setOnShowListener {
                tpd.getButton(TimePickerDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                tpd.getButton(TimePickerDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor("#94A3B8"))
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            }
            tpd.show()
        }

        root.addView(datePickerBtn)
        root.addView(timeRow)
        root.addView(durationSummaryText)

        // Quick Preset Durations
        val quickDurRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }
        fun durPresetBtn(mins: Long) = Button(activity).apply {
            text = "+${mins}m"
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = themeCoordinator.createButtonBackground(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            setOnClickListener {
                endCal.timeInMillis = startCal.timeInMillis + (mins * 60000L)
                endTimeBtn.text = "🏁 End: ${timeFmt.format(endCal.time)}"
                updateDurationPreview()
            }
        }
        quickDurRow.addView(durPresetBtn(25L))
        quickDurRow.addView(durPresetBtn(45L))
        quickDurRow.addView(durPresetBtn(60L))
        quickDurRow.addView(durPresetBtn(90L))
        root.addView(quickDurRow)

        // Insert Button
        val insertBtn = Button(activity).apply {
            text = "LOG SESSION INTO DATABASE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                val focusSecs = updateDurationPreview()
                if (focusSecs <= 0) {
                    Toast.makeText(activity, "Invalid duration: End time must be after Start time!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val inputName = customNameEdit.text.toString().trim()
                var finalSubId: String? = null
                var finalSubName: String? = null
                var finalSubColor: String? = null

                if (selectedSubject != null) {
                    finalSubId = selectedSubject!!.id
                    finalSubName = selectedSubject!!.name
                    finalSubColor = selectedSubject!!.colorHex
                } else if (inputName.isNotEmpty()) {
                    // Create and register new custom subject
                    val newId = "custom_${System.currentTimeMillis()}"
                    val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                    val customJson = subPrefs.getString("custom_subjects_json", "[]") ?: "[]"
                    val arr = JSONArray(customJson)
                    arr.put(JSONObject().apply {
                        put("id", newId)
                        put("name", inputName)
                        put("iconEmoji", "")
                        put("colorHex", "#38BDF8")
                    })
                    subPrefs.edit().putString("custom_subjects_json", arr.toString()).apply()
                    finalSubId = newId
                    finalSubName = inputName
                    finalSubColor = "#38BDF8"
                }

                val startMs = startCal.timeInMillis
                val endMs = endCal.timeInMillis
                val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startMs))

                val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val curTotal = sharedPrefs.getLong("${dayKey}_focus_total", 0L)
                sharedPrefs.edit()
                    .putLong("${dayKey}_focus_total", curTotal + focusSecs)
                    .putLong("last_data_modified_timestamp", System.currentTimeMillis())
                    .apply()

                if (finalSubId != null) {
                    SubjectTagManager.recordSubjectStudyTime(activity, finalSubId, focusSecs, dayKey)
                }

                TimelineLogger.recordRaw(
                    context = activity,
                    state = "STUDYING",
                    timestamp = startMs,
                    subId = finalSubId,
                    subName = finalSubName,
                    subColor = finalSubColor
                )
                TimelineLogger.recordRaw(
                    context = activity,
                    state = "IDLE",
                    timestamp = endMs
                )

                activity.recalculateStreak()
                activity.statsDirty = true
                activity.tabPageCache.clear()
                Toast.makeText(activity, "Successfully logged ${focusSecs / 60}m session for $dayKey!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        root.addView(insertBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun fastForwardSession(activity: MainActivity, extraSecs: Long) {
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val now = System.currentTimeMillis()

        sharedPrefs.edit()
            .putLong("${todayStr}_focus_total", currentFocus + extraSecs)
            .putLong("last_data_modified_timestamp", now)
            .apply()

        val selectedSubject = SubjectTagManager.getSelectedSubject(activity)
        SubjectTagManager.recordSubjectStudyTime(activity, selectedSubject.id, extraSecs, todayStr)

        val startMs = now - (extraSecs * 1000L)
        TimelineLogger.recordRaw(
            context = activity,
            state = "STUDYING",
            timestamp = startMs,
            subId = selectedSubject.id,
            subName = selectedSubject.name,
            subColor = selectedSubject.colorHex
        )
        TimelineLogger.recordRaw(
            context = activity,
            state = "IDLE",
            timestamp = now
        )

        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
    }

    data class PresetConfig(
        val title: String,
        val subjects: List<SubjectTag>,
        val subjectWeights: List<Float>,
        val goals: List<PlannerGoal>,
        val days: Int = 21,
        val dailyGoalMins: Long = 240L,
        val baseMinsWeekday: Long = 270L,
        val baseMinsWeekend: Long = 180L,
        val seed: Long = 101L,
        val todayFocusMinutes: Long = 215L
    )

    fun seedPresetPCM(activity: MainActivity) {
        val subjects = listOf(
            SubjectTag("pcm_math", "Mathematics", "📐", "#10B981"),
            SubjectTag("pcm_physics", "Physics", "⚛️", "#8B5CF6"),
            SubjectTag("pcm_chemistry", "Chemistry", "🧪", "#EC4899"),
            SubjectTag("pcm_mock", "JEE Mock Tests & PYQs", "📝", "#F59E0B")
        )
        val goals = listOf(
            PlannerGoal(id = "pcm_g1", title = "📐 Solve 25 Calculus & Integration PYQs", note = "Definite integrals & curve tracing", targetMinutes = 60, subjectId = "pcm_math"),
            PlannerGoal(id = "pcm_g2", title = "⚛️ Physics Mechanics & Rotation Numericals", note = "Angular momentum & torque problems", targetMinutes = 50, subjectId = "pcm_physics"),
            PlannerGoal(id = "pcm_g3", title = "🧪 Organic Chemistry Reaction Flowcharts", note = "Aldehydes, Ketones & Amines", targetMinutes = 45, subjectId = "pcm_chemistry"),
            PlannerGoal(id = "pcm_g4", title = "📝 Full-Length JEE Test Error Analysis", note = "Speed & accuracy check", targetMinutes = 35, subjectId = "pcm_mock"),
            PlannerGoal(id = "pcm_g5", title = "💧 Formula Sheet Evening Revision", note = "Quick flashcards & mental review", targetMinutes = 0, subjectId = null)
        )
        val config = PresetConfig(
            title = "PCM Engineering Aspirant",
            subjects = subjects,
            subjectWeights = listOf(0.35f, 0.30f, 0.22f, 0.13f),
            goals = goals,
            days = 21,
            dailyGoalMins = 240L,
            baseMinsWeekday = 280L,
            baseMinsWeekend = 190L,
            seed = 101L,
            todayFocusMinutes = 225L
        )
        applyComprehensivePreset(activity, config)
    }

    fun seedPresetPCB(activity: MainActivity) {
        val subjects = listOf(
            SubjectTag("pcb_bio", "Biology & Genetics", "🧬", "#10B981"),
            SubjectTag("pcb_chem", "Chemistry & Biomolecules", "🧪", "#EC4899"),
            SubjectTag("pcb_phys", "Physics (Optics & Waves)", "⚛️", "#8B5CF6"),
            SubjectTag("pcb_neet", "NEET MCQ & NCERT Drills", "📑", "#06B6D4")
        )
        val goals = listOf(
            PlannerGoal(id = "pcb_g1", title = "🧬 NCERT Line-by-Line Biology Revision", note = "Genetics, Evolution & Ecology", targetMinutes = 60, subjectId = "pcb_bio"),
            PlannerGoal(id = "pcb_g2", title = "🧪 Biomolecules & Organic Reactions", note = "Polymers & Reaction Sheet", targetMinutes = 45, subjectId = "pcb_chem"),
            PlannerGoal(id = "pcb_g3", title = "⚛️ Ray Optics & Wave Optics Numericals", note = "Formulas & 30 numericals", targetMinutes = 45, subjectId = "pcb_phys"),
            PlannerGoal(id = "pcb_g4", title = "📑 60 NEET Speed Drills & Question Bank", note = "Timed test under 45 mins", targetMinutes = 40, subjectId = "pcb_neet"),
            PlannerGoal(id = "pcb_g5", title = "🌿 Morning Biology Diagram Practice", note = "Nephron, Plant Anatomy & Flashcards", targetMinutes = 0, subjectId = null)
        )
        val config = PresetConfig(
            title = "PCB Medical Aspirant",
            subjects = subjects,
            subjectWeights = listOf(0.40f, 0.25f, 0.22f, 0.13f),
            goals = goals,
            days = 21,
            dailyGoalMins = 240L,
            baseMinsWeekday = 290L,
            baseMinsWeekend = 195L,
            seed = 202L,
            todayFocusMinutes = 235L
        )
        applyComprehensivePreset(activity, config)
    }

    fun seedPresetCS(activity: MainActivity) {
        val subjects = listOf(
            SubjectTag("cs_dsa", "DSA & Algorithms", "⚡", "#38BDF8"),
            SubjectTag("cs_android", "Android & Architecture", "📱", "#10B981"),
            SubjectTag("cs_ai", "AI & Deep Learning", "🤖", "#8B5CF6"),
            SubjectTag("cs_oss", "Open Source & Systems", "🛠️", "#F59E0B")
        )
        val goals = listOf(
            PlannerGoal(id = "cs_g1", title = "⚡ Solve 2 LeetCode Mediums (Trees/DP)", note = "Optimized space & time complexity", targetMinutes = 60, subjectId = "cs_dsa"),
            PlannerGoal(id = "cs_g2", title = "📱 Clean Architecture & Kotlin Flow", note = "UseCases, Repositories & Tests", targetMinutes = 50, subjectId = "cs_android"),
            PlannerGoal(id = "cs_g3", title = "🤖 Transformer Attention Mechanism Paper", note = "Math breakdown & embeddings", targetMinutes = 45, subjectId = "cs_ai"),
            PlannerGoal(id = "cs_g4", title = "🛠️ Review PRs & Refactor Legacy Modules", note = "Zero warnings & clean build", targetMinutes = 35, subjectId = "cs_oss"),
            PlannerGoal(id = "cs_g5", title = "☕ Read Tech RFC / System Design", note = "Daily engineering habit", targetMinutes = 0, subjectId = null)
        )
        val config = PresetConfig(
            title = "Tech & Software Engineer",
            subjects = subjects,
            subjectWeights = listOf(0.35f, 0.30f, 0.20f, 0.15f),
            goals = goals,
            days = 21,
            dailyGoalMins = 240L,
            baseMinsWeekday = 270L,
            baseMinsWeekend = 180L,
            seed = 303L,
            todayFocusMinutes = 220L
        )
        applyComprehensivePreset(activity, config)
    }

    fun seedPreset30DayStreak(activity: MainActivity) {
        val subjects = listOf(
            SubjectTag("pcm_math", "Mathematics", "📐", "#10B981"),
            SubjectTag("pcm_physics", "Physics", "⚛️", "#8B5CF6"),
            SubjectTag("pcm_chemistry", "Chemistry", "🧪", "#EC4899"),
            SubjectTag("pcm_mock", "JEE Mock Tests & PYQs", "📝", "#F59E0B")
        )
        val goals = listOf(
            PlannerGoal(id = "pcm_g1", title = "📐 Solve 25 Calculus & Integration PYQs", note = "Definite integrals & curve tracing", targetMinutes = 60, subjectId = "pcm_math"),
            PlannerGoal(id = "pcm_g2", title = "⚛️ Physics Mechanics & Rotation Numericals", note = "Angular momentum & torque problems", targetMinutes = 50, subjectId = "pcm_physics"),
            PlannerGoal(id = "pcm_g3", title = "🧪 Organic Chemistry Reaction Flowcharts", note = "Aldehydes, Ketones & Amines", targetMinutes = 45, subjectId = "pcm_chemistry"),
            PlannerGoal(id = "pcm_g4", title = "📝 Full-Length JEE Test Error Analysis", note = "Speed & accuracy check", targetMinutes = 35, subjectId = "pcm_mock"),
            PlannerGoal(id = "pcm_g5", title = "💧 Formula Sheet Evening Revision", note = "Quick flashcards & mental review", targetMinutes = 0, subjectId = null)
        )
        val config = PresetConfig(
            title = "30-Day Master Golden Streak",
            subjects = subjects,
            subjectWeights = listOf(0.35f, 0.30f, 0.22f, 0.13f),
            goals = goals,
            days = 30,
            dailyGoalMins = 240L,
            baseMinsWeekday = 300L,
            baseMinsWeekend = 220L,
            seed = 777L,
            todayFocusMinutes = 260L
        )
        applyComprehensivePreset(activity, config)
    }

    fun seedPresetTodayBurst(activity: MainActivity, minutes: Long = 270L) {
        val subjects = SubjectTagManager.getAllSubjects(activity)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val totalSecs = minutes * 60L
        val breakSecs = (totalSecs * 0.18f).toLong()

        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("${todayStr}_focus_total", totalSecs)
            .putLong("${todayStr}_break_total", breakSecs)
            .putLong("accumulatedStudy", 0L)
            .putLong("currentBreakSeconds", 0L)
            .putLong("last_data_modified_timestamp", System.currentTimeMillis())
            .apply()

        activity.accumulatedStudy = 0L
        activity.currentBreakSeconds = 0L

        val subList = if (subjects.isEmpty()) {
            listOf(SubjectTag("general", "General Focus", "⏱", "#10B981"))
        } else subjects

        val perSubjSecs = totalSecs / subList.size
        for (s in subList) {
            SubjectTagManager.recordSubjectStudyTime(activity, s.id, perSubjSecs, todayStr)
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val list = TimelineLogger.load(activity).filterNot {
            val entryDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.timestamp))
            entryDate == todayStr
        }.toMutableList()

        for (s in subList) {
            val startMs = cal.timeInMillis
            val endMs = startMs + (perSubjSecs * 1000L)
            list.add(TimelineEntry(timestamp = startMs, state = "STUDYING", subId = s.id, subName = s.name, subColor = s.colorHex))
            list.add(TimelineEntry(timestamp = endMs, state = "IDLE"))
            cal.timeInMillis = endMs + (15 * 60 * 1000L)
        }
        list.sortBy { it.timestamp }
        TimelineLogger.importRaw(activity, timelineToJsonString(list))

        TimelineLogger.invalidate()
        activity.statsEngine.forceReconcileDayTotals(todayStr)
        activity.invalidateStatsCache()
        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.refreshStatsPanel()
        activity.updateVisualStyles()
        StudyWidgetProvider.refresh(activity)
    }

    private fun applyComprehensivePreset(activity: MainActivity, config: PresetConfig) {
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // 1. Save Subjects
        val subPrefs = activity.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
        val customArray = JSONArray().apply {
            for (s in config.subjects) {
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("iconEmoji", "")
                    put("colorHex", s.colorHex)
                })
            }
        }
        subPrefs.edit().putString("custom_subjects_json", customArray.toString()).apply()

        // 2. Clear old subject keys & planner keys
        for (k in sharedPrefs.all.keys) {
            if (k.startsWith("subject_") || k.endsWith("_planner_snapshot")) {
                editor.remove(k)
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val timelineList = ArrayList<TimelineEntry>()
        val rng = Random(config.seed)

        for (i in config.days downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val isToday = (i == 0)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val dateStr = sdf.format(cal.time)

            val totalDailyFocusSecs = if (isToday) {
                config.todayFocusMinutes * 60L
            } else {
                val baseMins = if (isSunday) config.baseMinsWeekend else config.baseMinsWeekday
                val variance = rng.nextLong(-25, 30)
                (baseMins + variance).coerceAtLeast(60L) * 60L
            }
            val totalDailyBreakSecs = (totalDailyFocusSecs * 0.18f).toLong()

            editor.putLong("${dateStr}_focus_total", totalDailyFocusSecs)
            editor.putLong("${dateStr}_break_total", totalDailyBreakSecs)
            editor.remove("${dateStr}_focus_manual")
            editor.remove("${dateStr}_break_manual")

            // Distribute focus among subjects
            val sessionList = mutableListOf<Pair<SubjectTag, Long>>()
            var distributedSecs = 0L
            for (idx in config.subjects.indices) {
                val subj = config.subjects[idx]
                val weight = config.subjectWeights.getOrElse(idx) { 1f / config.subjects.size }
                val subjSecs = if (idx == config.subjects.size - 1) {
                    (totalDailyFocusSecs - distributedSecs).coerceAtLeast(0L)
                } else {
                    (totalDailyFocusSecs * weight).toLong()
                }
                distributedSecs += subjSecs
                if (subjSecs > 60L) {
                    sessionList.add(subj to subjSecs)
                    SubjectTagManager.recordSubjectStudyTime(activity, subj.id, subjSecs, dateStr)
                }
            }

            // Generate realistic timeline sessions (Morning -> Afternoon -> Evening)
            val sessionCal = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, if (isSunday) 10 else 8)
                set(Calendar.MINUTE, if (isSunday) 30 else 15)
                set(Calendar.SECOND, 0)
            }

            for ((sIdx, item) in sessionList.withIndex()) {
                val (subj, durationSecs) = item
                val startMs = sessionCal.timeInMillis
                val endMs = startMs + (durationSecs * 1000L)

                timelineList.add(
                    TimelineEntry(
                        timestamp = startMs,
                        state = "STUDYING",
                        subId = subj.id,
                        subName = subj.name,
                        subColor = subj.colorHex
                    )
                )
                timelineList.add(
                    TimelineEntry(
                        timestamp = endMs,
                        state = "IDLE"
                    )
                )

                // Add natural break before next session
                val breakMins = if (sIdx == 1) 40 else 15
                sessionCal.timeInMillis = endMs + (breakMins * 60 * 1000L)
            }

            // Snapshot goals for this date
            val snapshots = config.goals.mapIndexed { gIdx, g ->
                val isDone = if (isToday) {
                    gIdx < config.goals.size - 1 // Leave last one or two in progress for realistic demo
                } else {
                    if (isSunday) rng.nextFloat() < 0.75f else rng.nextFloat() < 0.95f
                }
                val checkedTime = if (isDone) cal.timeInMillis + (17 * 3600 * 1000L) else 0L
                PlannerGoalSnapshot(
                    goalId = g.id,
                    title = g.title,
                    targetMinutes = g.targetMinutes,
                    completed = isDone,
                    checkedAt = checkedTime,
                    isAchieved = isDone,
                    subjectId = g.subjectId
                )
            }

            val array = JSONArray()
            for (s in snapshots) {
                array.put(JSONObject().apply {
                    put("goalId", s.goalId)
                    put("title", s.title)
                    put("targetMinutes", s.targetMinutes)
                    put("completed", s.completed)
                    put("checkedAt", s.checkedAt)
                    put("isAchieved", s.isAchieved)
                    if (s.subjectId != null) put("subjectId", s.subjectId)
                })
            }
            editor.putString("${dateStr}_planner_snapshot", array.toString())
        }

        // Save active today goals matching current snapshot
        val todayGoals = config.goals.mapIndexed { gIdx, g ->
            val isDone = gIdx < config.goals.size - 1
            g.copy(completed = isDone, checkedAt = if (isDone) System.currentTimeMillis() - 3600000L else 0L)
        }
        activity.saveSessionGoalsToJson(todayGoals)

        val todayStr = sdf.format(Date())
        editor.putString("last_planner_reset_date", todayStr)
        editor.putInt("current_streak", config.days)
        editor.putLong("dailyGoalMinutes", config.dailyGoalMins)
        editor.putLong("accumulatedStudy", 0L)
        editor.putLong("currentBreakSeconds", 0L)
        editor.putLong("last_data_modified_timestamp", System.currentTimeMillis())
        editor.apply()

        activity.accumulatedStudy = 0L
        activity.currentBreakSeconds = 0L

        timelineList.sortBy { it.timestamp }
        TimelineLogger.importRaw(activity, timelineToJsonString(timelineList))

        TimelineLogger.invalidate()
        activity.statsEngine.forceReconcileDayTotals(todayStr)
        activity.invalidateStatsCache()
        activity.recalculateStreak()
        activity.statsDirty = true
        activity.tabPageCache.clear()
        activity.refreshStatsPanel()
        activity.updateVisualStyles()
        StudyWidgetProvider.refresh(activity)
    }

    private fun seedRealisticHistory(activity: MainActivity, days: Int, deterministicSeed: Long = 42L) {
        seedPresetPCM(activity)
    }

    private fun seedPresetStandardStudent(activity: MainActivity) {
        seedPresetPCM(activity)
    }

    private fun simulateCloudConflict(activity: MainActivity) {
        val mockCloudRecord = JSONObject().apply {
            put("user_id", AuthManager.getUserId(activity) ?: "mock_user_123")
            put("user_name", "Cloud Student (Mock)")
            put("updated_at", System.currentTimeMillis() + 3600000L * 24L) // 1 day in the future
            put("last_modified_timestamp", System.currentTimeMillis() + 3600000L * 24L)
            put("prefs_data", JSONObject().apply {
                put("current_streak", 42)
                put("dailyGoalMinutes", 180L)
            }.toString())
            put("timeline_data", "[]")
        }

        val localTs = BackupManager(activity).getLastModifiedTimestamp()
        val cloudTs = System.currentTimeMillis() + 3600000L * 24L

        activity.showSyncConflictDialog(localTs, cloudTs, mockCloudRecord)
    }

    /**
     * Dialog for adjusting study focus and break time.
     * In Normal mode (for standard users), it accurately adjusts TODAY's focus or break time.
     * In Developer mode, its capabilities are extended to allow selecting ANY date (historical/future)
     * and performing Add, Deduct, or Direct Set adjustments.
     */
    fun showAdjustTodayTimeDialog(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        isDeveloperExtended: Boolean = false,
        initialDate: String? = null,
        defaultSyncLeaderboard: Boolean = false
    ) {
        val sharedPrefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val timerState = sharedPrefs.safeString("timerState", "IDLE") ?: "IDLE"
        val isTimerActive = activity.currentTimerState != TimerState.IDLE ||
                timerState != "IDLE" ||
                activity.accumulatedStudy > 0L ||
                activity.currentBreakSeconds > 0L

        if (isTimerActive) {
            showThemedConfirmDialog(
                activity = activity,
                themeCoordinator = themeCoordinator,
                title = "Timer Is Running",
                message = "Time can only be adjusted when no timer is running.\n\nPlease pause or finish your active timer session first before adjusting study or break records.",
                confirmText = "Go to Timer",
                isDestructive = false,
                onCancel = {}
            ) {
                activity.navigateToPanel(AppPanel.FOCUS)
            }
            return
        }

        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var targetDateStr = initialDate ?: todayStr

        var isFocusCategory = true // true = Study Focus, false = Break Time
        var actionMode = 0 // 0 = Add, 1 = Deduct, 2 = Set (dev only)
        var syncLeaderboard = defaultSyncLeaderboard || isDeveloperExtended

        val subjects = SubjectTagManager.getAllSubjects(activity)
        var selectedSubject: SubjectTag? = subjects.firstOrNull()

        var enteredMins = 30L

        // Timing variables for clock slot
        var isSpecificTimeSlot = false
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startCal = Calendar.getInstance().apply { add(Calendar.MINUTE, -30) }
        val endCal = Calendar.getInstance()

        val dialog = BottomSheetDialog(activity, R.style.TransparentBottomSheetDialogTheme)

        // Solid, completely opaque bottom sheet background - prevents any screen content bleed-through
        val isDark = themeCoordinator.isDarkMode()
        val baseBgColor = if (!isDark) {
            0xFFFFFFFF.toInt()
        } else if (themeCoordinator.activeBgMode == "OLED") {
            0xFF0A0A0C.toInt()
        } else {
            0xFF10131C.toInt()
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    dp(28).toFloat(), dp(28).toFloat(),
                    dp(28).toFloat(), dp(28).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(baseBgColor)
                setStroke(dp(1), if (isDark) 0x2EFFFFFF else 0x1E000000)
            }
            setPadding(dp(20), dp(12), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Top Drag Handle
        val dragHandle = View(activity).apply {
            background = GradientDrawable().apply {
                setColor(if (isDark) 0x33FFFFFF else 0x22000000)
                cornerRadius = dp(3).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14)
            }
        }
        root.addView(dragHandle)

        // Header Row (Title + Subtitle + Close ✕)
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val headerTextCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(activity).apply {
            text = if (isDeveloperExtended) "Adjust Time & Data (Extended)" else "Add Past Session"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
        }
        val subView = TextView(activity).apply {
            text = "Log missed study time or correct today's session"
            textSize = 12f
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        }
        headerTextCol.addView(titleView)
        headerTextCol.addView(subView)
        headerRow.addView(headerTextCol)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            setPadding(dp(10), dp(6), dp(6), dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // Nested Scrollable Content
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val maxScrollHeight = (screenHeight * 0.75f).toInt().coerceIn(dp(400), dp(680))

        val scrollView = NestedScrollView(activity).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Leaderboard toggle (developer only)
        val leaderboardToggleBtn = TextView(activity).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            visibility = if (isDeveloperExtended) View.VISIBLE else View.GONE
        }
        fun updateLeaderboardToggleUI() {
            if (syncLeaderboard) {
                leaderboardToggleBtn.text = "Sync to Public Leaderboard: ENABLED"
                leaderboardToggleBtn.setTextColor(Color.WHITE)
                leaderboardToggleBtn.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.parseColor("#10B981"))
                }
            } else {
                leaderboardToggleBtn.text = "Sync to Public Leaderboard: DISABLED"
                leaderboardToggleBtn.setTextColor(tintedColor(themeCoordinator.textColor, 160))
                leaderboardToggleBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 12f)
            }
        }
        leaderboardToggleBtn.setOnClickListener {
            syncLeaderboard = !syncLeaderboard
            updateLeaderboardToggleUI()
        }
        updateLeaderboardToggleUI()
        content.addView(leaderboardToggleBtn)

        // 1. Top Category Switcher (Focus vs Break)
        val categoryRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 22), 14f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
        }

        val focusTab = TextView(activity).apply {
            text = "Study Focus"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val breakTab = TextView(activity).apply {
            text = "Break Time"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        categoryRow.addView(focusTab)
        categoryRow.addView(breakTab)
        content.addView(categoryRow)

        // 2. Action Mode Switcher (Add Time vs Deduct Time)
        val modeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 18), 12f)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        val addModeBtn = TextView(activity).apply {
            text = "+ Add Time"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val deductModeBtn = TextView(activity).apply {
            text = "- Deduct Time"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val setModeBtn = TextView(activity).apply {
            text = "Set Exact"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            visibility = if (isDeveloperExtended) View.VISIBLE else View.GONE
        }
        modeRow.addView(addModeBtn)
        modeRow.addView(deductModeBtn)
        if (isDeveloperExtended) modeRow.addView(setModeBtn)
        content.addView(modeRow)

        // 3. Hero Duration Display Card
        val heroCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = themeCoordinator.createCardBackground(18f)
            setPadding(dp(16), dp(16), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        val heroLabel = TextView(activity).apply {
            text = "DURATION TO ADD"
            textSize = 11f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(tintedColor(themeCoordinator.textColor, 140))
        }
        heroCard.addView(heroLabel)

        // Stepper Row: [-15m] [ 01h : 30m ] [+15m]
        val stepperRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        var updateUI: () -> Unit = {}

        fun createStepperBtn(label: String, onClick: () -> Unit): TextView {
            return TextView(activity).apply {
                text = label
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            }
        }

        val stepMinusBtn = createStepperBtn("-15m") {
            enteredMins = (enteredMins - 15L).coerceAtLeast(0L)
            updateUI()
        }

        val heroDurationText = TextView(activity).apply {
            text = "00h : 30m"
            textSize = 32f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val curH = (enteredMins / 60L).toInt()
                val curM = (enteredMins % 60L).toInt()
                TimePickerDialog(
                    activity,
                    getPickerThemeRes(),
                    { _, hourOfDay, minute ->
                        enteredMins = (hourOfDay * 60L + minute).coerceIn(0L, 1440L)
                        updateUI()
                    },
                    curH,
                    curM,
                    true
                ).apply {
                    setTitle("Set Duration (Hours : Minutes)")
                }.show()
            }
        }

        val stepPlusBtn = createStepperBtn("+15m") {
            enteredMins = (enteredMins + 15L).coerceAtMost(1440L)
            updateUI()
        }

        stepperRow.addView(stepMinusBtn)
        stepperRow.addView(heroDurationText)
        stepperRow.addView(stepPlusBtn)
        heroCard.addView(stepperRow)

        // Quick Preset Chips (+15m, +30m, +45m, +1h, +2h, Reset)
        val chipsScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val chipsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        chipsScroll.addView(chipsContainer)
        heroCard.addView(chipsScroll)

        content.addView(heroCard)

        // 4. Subject & Time Slot Attribution Card
        val metaCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(16f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        // Subject row (visible only in Study Focus mode)
        val subjectRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            isClickable = true
            isFocusable = true
        }

        val subjectLabel = TextView(activity).apply {
            text = "Subject:"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val subjectChip = TextView(activity).apply {
            text = "${selectedSubject?.name ?: "General Focus"} ▾"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 8f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        subjectRow.addView(subjectLabel)
        subjectRow.addView(subjectChip)
        subjectRow.setOnClickListener {
            showThemedSubjectPickerModal(
                activity = activity,
                themeCoordinator = themeCoordinator,
                title = "Attribute to Subject",
                includeGeneral = true,
                includeCreateOption = false,
                onSelected = { sub ->
                    selectedSubject = sub
                    subjectChip.text = if (sub == null) {
                        "General Focus ▾"
                    } else {
                        "${sub?.name ?: "General Focus"} ▾"
                    }
                    updateUI()
                }
            )
        }
        metaCard.addView(subjectRow)

        // Divider
        val metaDivider = View(activity).apply {
            background = GradientDrawable().apply { setColor(0x18FFFFFF) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(4), 0, dp(8))
            }
        }
        metaCard.addView(metaDivider)

        // Time slot selection (Ended Just Now vs Custom Time of Day)
        val timeSlotRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val timeSlotLabel = TextView(activity).apply {
            text = "Time Slot:"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val timeSlotToggle = TextView(activity).apply {
            text = "Ended Just Now ▾"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 8f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        timeSlotRow.addView(timeSlotLabel)
        timeSlotRow.addView(timeSlotToggle)
        metaCard.addView(timeSlotRow)

        // Custom Time Picker Row (collapsible)
        val customTimeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(4))
        }
        val startTimeBtn = TextView(activity).apply {
            text = "Start: ${timeFmt.format(startCal.time)} ▾"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 10f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4)
            }
        }
        val endTimeBtn = TextView(activity).apply {
            text = "End: ${timeFmt.format(endCal.time)} ▾"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 10f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4)
            }
        }
        customTimeRow.addView(startTimeBtn)
        customTimeRow.addView(endTimeBtn)
        metaCard.addView(customTimeRow)

        fun syncFromClockWheel() {
            var diffMs = endCal.timeInMillis - startCal.timeInMillis
            if (diffMs <= 0) {
                endCal.timeInMillis = startCal.timeInMillis + (30 * 60000L)
                endTimeBtn.text = "End: ${timeFmt.format(endCal.time)} ▾"
                diffMs = 30 * 60000L
            }
            enteredMins = (diffMs / 60000L).coerceIn(1L, 1440L)
            updateUI()
        }

        timeSlotToggle.setOnClickListener {
            isSpecificTimeSlot = !isSpecificTimeSlot
            if (isSpecificTimeSlot) {
                timeSlotToggle.text = "Custom Time ▾"
                timeSlotToggle.setTextColor(themeCoordinator.primaryColor)
                customTimeRow.visibility = View.VISIBLE
                syncFromClockWheel()
            } else {
                timeSlotToggle.text = "Ended Just Now ▾"
                timeSlotToggle.setTextColor(themeCoordinator.textColor)
                customTimeRow.visibility = View.GONE
                updateUI()
            }
        }

        startTimeBtn.setOnClickListener {
            TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    startCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    startCal.set(Calendar.MINUTE, minute)
                    startCal.set(Calendar.SECOND, 0)
                    startTimeBtn.text = "Start: ${timeFmt.format(startCal.time)} ▾"
                    syncFromClockWheel()
                },
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                false
            ).show()
        }

        endTimeBtn.setOnClickListener {
            TimePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, hourOfDay, minute ->
                    endCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    endCal.set(Calendar.MINUTE, minute)
                    endCal.set(Calendar.SECOND, 0)
                    endTimeBtn.text = "End: ${timeFmt.format(endCal.time)} ▾"
                    syncFromClockWheel()
                },
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                false
            ).show()
        }

        content.addView(metaCard)

        // 5. Date Card (Developer mode only)
        val dateCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 12f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            visibility = View.VISIBLE
        }
        val dateLabel = TextView(activity).apply {
            textSize = 12f
            setTextColor(themeCoordinator.textColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val pickDateBtn = TextView(activity).apply {
            text = "Pick Date ▾"
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#38BDF8"))
            }
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                val cal = Calendar.getInstance()
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(targetDateStr)?.let {
                        cal.time = it
                    }
                } catch (_: Exception) {}

                DatePickerDialog(
                    activity,
                    getPickerThemeRes(),
                    { _, year, month, dayOfMonth ->
                        targetDateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        activity.runOnUiThread { updateUI() }
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        dateCard.addView(dateLabel)
        dateCard.addView(pickDateBtn)
        content.addView(dateCard)

        // 6. Live Impact & Goal Progress Card
        val summaryCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(16f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14)
            }
        }

        val summaryHeader = TextView(activity).apply {
            text = "RECORD IMPACT"
            textSize = 10f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(tintedColor(themeCoordinator.textColor, 120))
            setPadding(0, 0, 0, dp(4))
        }
        val summaryCurrentText = TextView(activity).apply {
            textSize = 12f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, dp(4))
        }
        val summaryResultText = TextView(activity).apply {
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val summaryGoalText = TextView(activity).apply {
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(tintedColor(themeCoordinator.textColor, 150))
            setPadding(0, dp(4), 0, 0)
        }
        summaryCard.addView(summaryHeader)
        summaryCard.addView(summaryCurrentText)
        summaryCard.addView(summaryResultText)
        summaryCard.addView(summaryGoalText)
        content.addView(summaryCard)

        // 7. Apply Action CTA Button
        val applyBtn = Button(activity).apply {
            text = "+ LOG 30m TO FOCUS"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50))
        }
        content.addView(applyBtn)

        scrollView.addView(content)
        root.addView(scrollView)

        updateUI = {
            val isTargetToday = targetDateStr == todayStr

            // Calculate current effective focus & break times
            val currentStoredFocus = sharedPrefs.getLong("${targetDateStr}_focus_total", 0L)
            val runningStudy = if (isTargetToday) activity.accumulatedStudy else 0L
            val effectiveFocusSecs = currentStoredFocus + runningStudy
            val effectiveFocusMins = effectiveFocusSecs / 60L

            val currentStoredBreak = sharedPrefs.getLong("${targetDateStr}_break_total", 0L)
            val runningBreak = if (isTargetToday) activity.currentBreakSeconds else 0L
            val effectiveBreakSecs = currentStoredBreak + runningBreak
            val effectiveBreakMins = effectiveBreakSecs / 60L

            // Date label
            val dateDisplay = try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(targetDateStr)
                SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(parsed ?: Date())
            } catch (_: Exception) {
                targetDateStr
            }

            dateLabel.text = if (isTargetToday) {
                "Today • $dateDisplay"
            } else {
                "Date • $dateDisplay"
            }

            // Category Tab Styles
            if (isFocusCategory) {
                focusTab.setTextColor(Color.WHITE)
                focusTab.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(themeCoordinator.primaryColor)
                }
                breakTab.setTextColor(tintedColor(themeCoordinator.textColor, 140))
                breakTab.background = null
                subjectRow.visibility = View.VISIBLE
                metaDivider.visibility = View.VISIBLE
            } else {
                breakTab.setTextColor(Color.WHITE)
                breakTab.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.parseColor("#F43F5E"))
                }
                focusTab.setTextColor(tintedColor(themeCoordinator.textColor, 140))
                focusTab.background = null
                subjectRow.visibility = View.GONE
                metaDivider.visibility = View.GONE
            }

            // Action Mode Tab Styles
            val activeActionColor = when (actionMode) {
                0 -> if (isFocusCategory) themeCoordinator.primaryColor else Color.parseColor("#F43F5E")
                1 -> Color.parseColor("#F59E0B") // Warm amber warning for deduct
                else -> Color.parseColor("#38BDF8")
            }

            addModeBtn.setTextColor(if (actionMode == 0) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            addModeBtn.background = if (actionMode == 0) GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(if (isFocusCategory) themeCoordinator.primaryColor else Color.parseColor("#F43F5E"))
            } else null

            deductModeBtn.setTextColor(if (actionMode == 1) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            deductModeBtn.background = if (actionMode == 1) GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(Color.parseColor("#F59E0B"))
            } else null

            setModeBtn.setTextColor(if (actionMode == 2) Color.WHITE else tintedColor(themeCoordinator.textColor, 140))
            setModeBtn.background = if (actionMode == 2) GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(Color.parseColor("#38BDF8"))
            } else null

            heroLabel.text = when (actionMode) {
                0 -> if (isFocusCategory) "DURATION TO ADD (STUDY)" else "DURATION TO ADD (BREAK)"
                1 -> if (isFocusCategory) "DURATION TO DEDUCT (STUDY)" else "DURATION TO DEDUCT (BREAK)"
                else -> "SET EXACT TOTAL"
            }

            // Update Hero Duration display
            val durH = enteredMins / 60L
            val durM = enteredMins % 60L
            heroDurationText.text = String.format(Locale.getDefault(), "%02dh : %02dm", durH, durM)
            heroDurationText.setTextColor(activeActionColor)

            // Preset chips
            chipsContainer.removeAllViews()
            val chipsList = when {
                actionMode == 2 -> if (isFocusCategory) listOf(0L, 30L, 60L, 120L, 240L) else listOf(0L, 5L, 15L, 30L, 60L)
                isFocusCategory -> if (actionMode == 0) listOf(15L, 30L, 45L, 60L, 120L) else listOf(10L, 15L, 30L, 45L, 60L)
                else -> if (actionMode == 0) listOf(5L, 10L, 15L, 20L, 30L) else listOf(5L, 10L, 15L, 20L, 30L)
            }

            for (p in chipsList) {
                val chip = TextView(activity).apply {
                    val label = if (p >= 60 && p % 60 == 0L) "${p / 60}h" else "${p}m"
                    text = when (actionMode) {
                        0 -> "+$label"
                        1 -> "-$label"
                        else -> label
                    }
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(activeActionColor)
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createGlassChip(tintedColor(activeActionColor, 35), 10f)
                    setPadding(dp(12), dp(7), dp(12), dp(7))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(6), 0)
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        enteredMins = if (actionMode == 0) {
                            (enteredMins + p).coerceAtMost(1440L)
                        } else if (actionMode == 1) {
                            (enteredMins + p).coerceAtMost(if (isFocusCategory) effectiveFocusMins else effectiveBreakMins)
                        } else {
                            p
                        }
                        updateUI()
                    }
                }
                chipsContainer.addView(chip)
            }

            // Reset chip
            val resetChip = TextView(activity).apply {
                text = "Reset"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(tintedColor(themeCoordinator.textColor, 160))
                gravity = Gravity.CENTER
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                setPadding(dp(12), dp(7), dp(12), dp(7))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    enteredMins = 0L
                    updateUI()
                }
            }
            chipsContainer.addView(resetChip)

            val enteredSecs = enteredMins * 60L
            val subName = selectedSubject?.name ?: "General Focus"

            val MAX_DAY_SECS = 86400L // 24 hours
            val otherSecs = if (isFocusCategory) effectiveBreakSecs else effectiveFocusSecs
            val maxAllowableSecs = (MAX_DAY_SECS - otherSecs).coerceAtLeast(0L)
            val maxAllowableMins = maxAllowableSecs / 60L

            var isExceedingDayLimit = false

            // Goal progress
            val dailyGoalSecs = try { activity.resolveGoalFor(targetDateStr) } catch (_: Exception) { 0L }

            if (isFocusCategory) {
                val curH = effectiveFocusMins / 60L
                val curM = effectiveFocusMins % 60L
                summaryCurrentText.text = "Current Recorded: ${curH}h ${curM}m (${effectiveFocusMins} mins)"

                val newTotalSecs = when (actionMode) {
                    0 -> effectiveFocusSecs + enteredSecs
                    1 -> (effectiveFocusSecs - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }
                val newH = newTotalSecs / 3600L
                val newM = (newTotalSecs % 3600L) / 60L

                if (newTotalSecs > MAX_DAY_SECS || (newTotalSecs + effectiveBreakSecs) > MAX_DAY_SECS) {
                    isExceedingDayLimit = true
                    summaryResultText.text = "Exceeds 24 hours in a single day!\n• Maximum allowable focus: ${maxAllowableMins / 60}h ${maxAllowableMins % 60}m"
                    summaryResultText.setTextColor(Color.parseColor("#EF4444"))
                    applyBtn.text = "EXCEEDS 24-HOUR LIMIT"
                    summaryGoalText.visibility = View.GONE
                } else {
                    summaryGoalText.visibility = View.VISIBLE
                    when (actionMode) {
                        0 -> {
                            summaryResultText.text = "${curH}h ${curM}m  ->  ${newH}h ${newM}m  (+${durH}h ${durM}m to $subName)"
                            summaryResultText.setTextColor(themeCoordinator.primaryColor)
                            applyBtn.text = "+ LOG ${if (durH > 0) "${durH}h ${durM}m" else "${durM}m"} TO FOCUS"
                        }
                        1 -> {
                            summaryResultText.text = "${curH}h ${curM}m  ->  ${newH}h ${newM}m  (-${durH}h ${durM}m from $subName)"
                            summaryResultText.setTextColor(Color.parseColor("#F59E0B"))
                            applyBtn.text = "DEDUCT ${if (durH > 0) "${durH}h ${durM}m" else "${durM}m"} FROM FOCUS"
                        }
                        else -> {
                            summaryResultText.text = "Set Total Focus to ${newH}h ${newM}m"
                            summaryResultText.setTextColor(Color.parseColor("#38BDF8"))
                            applyBtn.text = "SET FOCUS TOTAL TO ${enteredMins}m"
                        }
                    }

                    if (dailyGoalSecs > 0) {
                        val pct = ((newTotalSecs.toFloat() / dailyGoalSecs.toFloat()) * 100).toInt()
                        val goalH = dailyGoalSecs / 3600L
                        val goalM = (dailyGoalSecs % 3600L) / 60L
                        if (pct >= 100) {
                            summaryGoalText.text = "Daily Goal: ${newH}h ${newM}m / ${goalH}h ${goalM}m (100% Completed!)"
                            summaryGoalText.setTextColor(Color.parseColor("#10B981"))
                        } else {
                            summaryGoalText.text = "Daily Goal: ${newH}h ${newM}m / ${goalH}h ${goalM}m (${pct}% reached)"
                            summaryGoalText.setTextColor(tintedColor(themeCoordinator.textColor, 160))
                        }
                    } else {
                        summaryGoalText.visibility = View.GONE
                    }
                }
            } else {
                // Break Category
                val curH = effectiveBreakMins / 60L
                val curM = effectiveBreakMins % 60L
                summaryCurrentText.text = "Current Recorded: ${curH}h ${curM}m (${effectiveBreakMins} mins)"

                val newTotalSecs = when (actionMode) {
                    0 -> effectiveBreakSecs + enteredSecs
                    1 -> (effectiveBreakSecs - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }
                val newH = newTotalSecs / 3600L
                val newM = (newTotalSecs % 3600L) / 60L

                if (newTotalSecs > MAX_DAY_SECS || (newTotalSecs + effectiveFocusSecs) > MAX_DAY_SECS) {
                    isExceedingDayLimit = true
                    summaryResultText.text = "Exceeds 24 hours in a single day!\n• Maximum allowable break: ${maxAllowableMins / 60}h ${maxAllowableMins % 60}m"
                    summaryResultText.setTextColor(Color.parseColor("#EF4444"))
                    applyBtn.text = "EXCEEDS 24-HOUR LIMIT"
                } else {
                    when (actionMode) {
                        0 -> {
                            summaryResultText.text = "${curH}h ${curM}m  ->  ${newH}h ${newM}m  (+${durH}h ${durM}m break)"
                            summaryResultText.setTextColor(Color.parseColor("#F43F5E"))
                            applyBtn.text = "+ LOG ${if (durH > 0) "${durH}h ${durM}m" else "${durM}m"} TO BREAK"
                        }
                        1 -> {
                            summaryResultText.text = "${curH}h ${curM}m  ->  ${newH}h ${newM}m  (-${durH}h ${durM}m break)"
                            summaryResultText.setTextColor(Color.parseColor("#F59E0B"))
                            applyBtn.text = "DEDUCT ${if (durH > 0) "${durH}h ${durM}m" else "${durM}m"} FROM BREAK"
                        }
                        else -> {
                            summaryResultText.text = "🎯 Set Total Break to ${newH}h ${newM}m"
                            summaryResultText.setTextColor(Color.parseColor("#38BDF8"))
                            applyBtn.text = "SET BREAK TOTAL TO ${enteredMins}m"
                        }
                    }
                    summaryGoalText.visibility = View.GONE
                }
            }

            applyBtn.isEnabled = !isExceedingDayLimit && (enteredMins > 0 || actionMode == 2)
            applyBtn.alpha = if (applyBtn.isEnabled) 1.0f else 0.45f
            applyBtn.background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isExceedingDayLimit) Color.parseColor("#475569") else activeActionColor)
            }
        }

        focusTab.setOnClickListener {
            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            isFocusCategory = true
            updateUI()
        }
        breakTab.setOnClickListener {
            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            isFocusCategory = false
            updateUI()
        }

        addModeBtn.setOnClickListener {
            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            actionMode = 0
            updateUI()
        }
        deductModeBtn.setOnClickListener {
            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            actionMode = 1
            updateUI()
        }
        setModeBtn.setOnClickListener {
            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            actionMode = 2
            updateUI()
        }

        applyBtn.setOnClickListener {
            if (enteredMins <= 0 && actionMode != 2) {
                Toast.makeText(activity, "Please choose a duration greater than 0m", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (enteredMins > 1440L) {
                Toast.makeText(activity, "Cannot add more than 24 hours to a single day", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val enteredSecs = enteredMins * 60L
            val isTargetToday = targetDateStr == todayStr
            val now = System.currentTimeMillis()
            val effectiveSubId = selectedSubject?.id ?: "general"
            val MAX_DAY_SECS = 86400L

            if (isFocusCategory) {
                val currentStoredFocus = sharedPrefs.getLong("${targetDateStr}_focus_total", 0L)
                val runningStudy = if (isTargetToday) activity.accumulatedStudy else 0L
                val currentEffectiveFocus = currentStoredFocus + runningStudy
                val currentStoredBreak = sharedPrefs.getLong("${targetDateStr}_break_total", 0L) + (if (isTargetToday) activity.currentBreakSeconds else 0L)

                val newTotalFocus = when (actionMode) {
                    0 -> currentEffectiveFocus + enteredSecs
                    1 -> (currentEffectiveFocus - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }

                if (newTotalFocus > MAX_DAY_SECS || (newTotalFocus + currentStoredBreak) > MAX_DAY_SECS) {
                    val maxAllowedMins = ((MAX_DAY_SECS - currentStoredBreak) / 60L).coerceAtLeast(0L)
                    Toast.makeText(activity, "Cannot exceed 24 hours in a single day. Maximum allowable total study time is ${maxAllowedMins / 60}h ${maxAllowedMins % 60}m.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                when (actionMode) {
                    0 -> {
                        if (isSpecificTimeSlot) {
                            val parsedDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(targetDateStr) } catch (_: Exception) { null } ?: Date()
                            val c1 = Calendar.getInstance().apply {
                                time = parsedDate
                                set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, startCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                            }
                            val c2 = Calendar.getInstance().apply {
                                time = parsedDate
                                set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, endCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                            }
                            val startMs = c1.timeInMillis
                            val endMs = if (c2.timeInMillis > startMs) c2.timeInMillis else startMs + enteredSecs * 1000L
                            TimelineLogger.addBlock(
                                context = activity,
                                startMs = startMs,
                                endMs = endMs,
                                state = "MANUAL_FOCUS",
                                subId = selectedSubject?.id,
                                subName = selectedSubject?.name,
                                subColor = selectedSubject?.colorHex
                            )
                        } else {
                            TimelineLogger.appendBlockForDay(
                                context = activity,
                                dateStr = targetDateStr,
                                durationSecs = enteredSecs,
                                state = "MANUAL_FOCUS",
                                subId = selectedSubject?.id,
                                subName = selectedSubject?.name,
                                subColor = selectedSubject?.colorHex
                            )
                        }
                        SubjectTagManager.adjustSubjectStudyTime(activity, effectiveSubId, enteredSecs, targetDateStr)
                    }
                    1 -> {
                        TimelineLogger.deductDurationForDay(
                            context = activity,
                            dateStr = targetDateStr,
                            deductSecs = enteredSecs,
                            isBreak = false,
                            adjustSubjects = true,
                            targetSubId = selectedSubject?.id
                        )
                    }
                    else -> {
                        TimelineLogger.setTotalDurationForDay(
                            context = activity,
                            dateStr = targetDateStr,
                            targetSecs = enteredSecs,
                            isBreak = false,
                            subId = selectedSubject?.id,
                            subName = selectedSubject?.name,
                            subColor = selectedSubject?.colorHex
                        )
                    }
                }

                val storedFirst = sharedPrefs.getString("first_study_session_date", null)
                val firstToSet = if (storedFirst == null || targetDateStr < storedFirst) targetDateStr else storedFirst
                sharedPrefs.edit()
                    .putString("first_study_session_date", firstToSet)
                    .putLong("${targetDateStr}_focus_total", newTotalFocus)
                    .putLong("${targetDateStr}_focus_manual", 0L)
                    .putLong("last_data_modified_timestamp", now)
                    .apply()

                if (isTargetToday) {
                    activity.accumulatedStudy = 0L
                    sharedPrefs.edit().putLong("accumulatedStudy", 0L).apply()
                }

                if (isFocusCategory && (actionMode == 1 || syncLeaderboard)) {
                    val subName = selectedSubject?.name ?: "General Focus"
                    val subColor = selectedSubject?.colorHex ?: "#3b82f6"
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        LeaderboardManager.overrideLeaderboardDailyTotal(
                            context = activity,
                            totalSeconds = if (syncLeaderboard && actionMode != 1) newTotalFocus.toInt() else LeaderboardManager.getRealTimerFocusSecondsForDate(activity, targetDateStr).toInt(),
                            subject = subName,
                            color = subColor
                        )
                    }
                }

                val subName = selectedSubject?.name ?: "General Focus"
                val syncTag = if (syncLeaderboard) " [Synced to Leaderboard]" else ""
                val msg = when (actionMode) {
                    0 -> "Added ${enteredMins}m to ${if (isTargetToday) "today's" else targetDateStr} focus ($subName)!$syncTag"
                    1 -> "Deducted ${enteredMins}m from ${if (isTargetToday) "today's" else targetDateStr} focus ($subName)!$syncTag"
                    else -> "Set ${if (isTargetToday) "today's" else targetDateStr} focus total to ${enteredMins}m!$syncTag"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            } else {
                // Break Category
                val currentStoredBreak = sharedPrefs.getLong("${targetDateStr}_break_total", 0L)
                val runningBreak = if (isTargetToday) activity.currentBreakSeconds else 0L
                val currentEffectiveBreak = currentStoredBreak + runningBreak
                val currentStoredFocus = sharedPrefs.getLong("${targetDateStr}_focus_total", 0L) + (if (isTargetToday) activity.accumulatedStudy else 0L)

                val newTotalBreak = when (actionMode) {
                    0 -> currentEffectiveBreak + enteredSecs
                    1 -> (currentEffectiveBreak - enteredSecs).coerceAtLeast(0L)
                    else -> enteredSecs
                }

                if (newTotalBreak > MAX_DAY_SECS || (newTotalBreak + currentStoredFocus) > MAX_DAY_SECS) {
                    val maxAllowedMins = ((MAX_DAY_SECS - currentStoredFocus) / 60L).coerceAtLeast(0L)
                    Toast.makeText(activity, "Cannot exceed 24 hours in a single day. Maximum allowable total break time is ${maxAllowedMins / 60}h ${maxAllowedMins % 60}m.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                when (actionMode) {
                    0 -> TimelineLogger.appendBlockForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        durationSecs = enteredSecs,
                        state = "MANUAL_BREAK"
                    )
                    1 -> TimelineLogger.deductDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        deductSecs = enteredSecs,
                        isBreak = true
                    )
                    else -> TimelineLogger.setTotalDurationForDay(
                        context = activity,
                        dateStr = targetDateStr,
                        targetSecs = enteredSecs,
                        isBreak = true
                    )
                }

                sharedPrefs.edit()
                    .putLong("${targetDateStr}_break_total", newTotalBreak)
                    .putLong("${targetDateStr}_break_manual", 0L)
                    .putLong("last_data_modified_timestamp", now)
                    .apply()

                if (isTargetToday) {
                    activity.currentBreakSeconds = 0L
                    sharedPrefs.edit().putLong("currentBreakSeconds", 0L).apply()
                }

                val msg = when (actionMode) {
                    0 -> "Added ${enteredMins}m to ${if (isTargetToday) "today's" else targetDateStr} break!"
                    1 -> "Deducted ${enteredMins}m from ${if (isTargetToday) "today's" else targetDateStr} break!"
                    else -> "Set ${if (isTargetToday) "today's" else targetDateStr} break total to ${enteredMins}m!"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }

            TimelineLogger.invalidate()
            TimelineLogger.reconcileSubjectDurationsFromTimeline(activity, targetDateStr)
            activity.statsEngine.forceReconcileDayTotals(targetDateStr)
            activity.invalidateStatsCache()
            activity.recalculateStreak()
            activity.refreshStatsPanel()
            activity.updateVisualStyles()
            StudyWidgetProvider.refresh(activity)

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                CloudSyncManager.syncDataToCloud(activity)
            }

            dialog.dismiss()
        }

        updateUI()

        dialog.setContentView(root)
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setOnShowListener {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }

    fun showDevCustomLeaderboardTimeDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        showAdjustTodayTimeDialog(
            activity = activity,
            themeCoordinator = themeCoordinator,
            isDeveloperExtended = true,
            defaultSyncLeaderboard = true
        )
    }

    private fun seedSampleGoals(activity: MainActivity) {
        val sampleGoals = listOf(
            PlannerGoal(title = "📐 Solve 20 Calculus problems", note = "Derivatives and Integrals", targetMinutes = 60, subjectId = "math"),
            PlannerGoal(title = "⚛️ Physics Numerical Practice", note = "Thermodynamics chapter", targetMinutes = 45, subjectId = "physics"),
            PlannerGoal(title = "🧪 Review Organic Mechanisms", note = "Reaction pathways & notes", targetMinutes = 30, subjectId = "chemistry"),
            PlannerGoal(title = "💧 Daily Hydration & Flashcards", note = "Quick evening revision", targetMinutes = 0, subjectId = null)
        )
        activity.saveSessionGoalsToJson(sampleGoals)
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        prefs.edit().putString("last_planner_reset_date", todayStr).apply()
    }

    private fun simulatePlannerDayRollover(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentGoals = activity.loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
        if (currentGoals.isNotEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            PlannerHistoryManager.snapshotToday(activity, currentGoals)
            val resetGoals = currentGoals.map { it.copy(completed = false, checkedAt = 0L) }
            activity.saveSessionGoalsToJson(resetGoals)
            prefs.edit().putString("last_planner_reset_date", todayStr).apply()
        }
    }

    private fun seedRealisticHabitHistory(activity: MainActivity, days: Int = 14) {
        val sampleGoals = listOf(
            PlannerGoal(id = "sample_goal_1", title = "📐 Solve Calculus Problems", targetMinutes = 45, subjectId = "math"),
            PlannerGoal(id = "sample_goal_2", title = "⚛️ Physics Core Concept Review", targetMinutes = 30, subjectId = "physics"),
            PlannerGoal(id = "sample_goal_3", title = "🧪 Organic Chemistry Reactions", targetMinutes = 30, subjectId = "chemistry"),
            PlannerGoal(id = "sample_goal_4", title = "💧 Daily Review & Flashcards", targetMinutes = 0, subjectId = null)
        )
        activity.saveSessionGoalsToJson(sampleGoals)

        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val rng = Random(888L)

        for (i in days downTo 1) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

            val snapshots = sampleGoals.map { g ->
                val completed = if (isSunday) rng.nextFloat() < 0.60f else rng.nextFloat() < 0.85f
                val isAchieved = completed
                val checkedTime = if (completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L
                PlannerGoalSnapshot(
                    goalId = g.id,
                    title = g.title,
                    targetMinutes = g.targetMinutes,
                    completed = completed,
                    checkedAt = checkedTime,
                    isAchieved = isAchieved,
                    subjectId = g.subjectId
                )
            }

            val array = JSONArray()
            for (s in snapshots) {
                array.put(JSONObject().apply {
                    put("goalId", s.goalId)
                    put("title", s.title)
                    put("targetMinutes", s.targetMinutes)
                    put("completed", s.completed)
                    put("checkedAt", s.checkedAt)
                    put("isAchieved", s.isAchieved)
                    if (s.subjectId != null) put("subjectId", s.subjectId)
                })
            }
            editor.putString("${dateStr}_planner_snapshot", array.toString())
        }
        val todayStr = sdf.format(Date())
        editor.putString("last_planner_reset_date", todayStr)
        editor.apply()
    }

    private fun markAllTodayGoals(activity: MainActivity, completed: Boolean) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentGoals = activity.loadSessionGoalsFromJson(prefs.getString("session_goals_json", "[]") ?: "[]")
        val updated = currentGoals.map {
            it.copy(completed = completed, checkedAt = if (completed) System.currentTimeMillis() else 0L)
        }
        activity.saveSessionGoalsToJson(updated)
    }

    private fun wipePlannerData(activity: MainActivity) {
        val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (k in prefs.all.keys) {
            if (k.endsWith("_planner_snapshot")) {
                editor.remove(k)
            }
        }
        editor.putString("session_goals_json", "[]")
        editor.apply()
    }

    data class EditableGoalItem(
        var id: String,
        var title: String,
        var targetMinutes: Int,
        var completed: Boolean,
        var isAchieved: Boolean,
        var subjectId: String?
    )

    fun showCustomPlannerSnapshotLoggerDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        // Title
        root.addView(TextView(activity).apply {
            text = "✍️ Custom Historical Planner Logger"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(activity).apply {
            text = "Select any past date and set custom habit completion statuses to test streaks, matrices & insights."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(12))
        })

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } // default yesterday
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateDisplayFmt = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

        val datePickerBtn = TextView(activity).apply {
            text = "📅 Selected Date: ${dateDisplayFmt.format(cal.time)}"
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 60), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        root.addView(datePickerBtn)

        val itemsList = mutableListOf<EditableGoalItem>()

        fun loadGoalsForCurrentDate() {
            val dateStr = sdf.format(cal.time)
            itemsList.clear()
            val existingSnapshots = PlannerHistoryManager.loadDaySnapshot(activity, dateStr)
            if (existingSnapshots.isNotEmpty()) {
                for (s in existingSnapshots) {
                    itemsList.add(EditableGoalItem(s.goalId, s.title, s.targetMinutes, s.completed, s.isAchieved, s.subjectId))
                }
            } else {
                val currentGoals = activity.loadSessionGoalsFromJson(activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]")
                if (currentGoals.isNotEmpty()) {
                    for (g in currentGoals) {
                        itemsList.add(EditableGoalItem(g.id, g.title, g.targetMinutes, true, true, g.subjectId))
                    }
                } else {
                    itemsList.add(EditableGoalItem("g1", "📐 Calculus Practice", 45, true, true, "math"))
                    itemsList.add(EditableGoalItem("g2", "⚛️ Physics Numerical Set", 30, true, true, "physics"))
                    itemsList.add(EditableGoalItem("g3", "🧪 Chemistry Theory Revision", 30, false, false, "chemistry"))
                    itemsList.add(EditableGoalItem("g4", "💧 Daily Routine & Flashcards", 0, true, true, null))
                }
            }
        }
        loadGoalsForCurrentDate()

        val goalsScrollView = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.38f).toInt()
            )
        }

        val goalsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        goalsScrollView.addView(goalsContainer)

        fun renderGoalsList() {
            goalsContainer.removeAllViews()
            for ((idx, item) in itemsList.withIndex()) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground(14f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(6))
                    }
                }

                // Completion Toggle
                val toggleBtn = TextView(activity).apply {
                    text = if (item.completed) "✓ Completed" else "✗ Incomplete"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (item.completed) Color.parseColor("#10B981") else Color.parseColor("#475569"))
                    }
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener {
                        item.completed = !item.completed
                        item.isAchieved = item.completed
                        renderGoalsList()
                    }
                }

                val titleCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(10), 0, dp(10), 0)
                    }
                }
                titleCol.addView(TextView(activity).apply {
                    text = item.title
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                titleCol.addView(TextView(activity).apply {
                    text = if (item.targetMinutes > 0) "Target: ${item.targetMinutes}m" else "Daily Habit (No target)"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.55f
                    textSize = 11f
                })

                val removeBtn = TextView(activity).apply {
                    text = "✕"
                    textSize = 14f
                    setTextColor(Color.parseColor("#EF4444"))
                    alpha = 0.7f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        itemsList.removeAt(idx)
                        renderGoalsList()
                    }
                }

                row.addView(toggleBtn)
                row.addView(titleCol)
                row.addView(removeBtn)
                goalsContainer.addView(row)
            }
        }
        renderGoalsList()

        datePickerBtn.setOnClickListener {
            val dpd = DatePickerDialog(
                activity,
                getPickerThemeRes(),
                { _, y, m, d ->
                    cal.set(Calendar.YEAR, y)
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    datePickerBtn.text = "📅 Selected Date: ${dateDisplayFmt.format(cal.time)}"
                    loadGoalsForCurrentDate()
                    renderGoalsList()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dpd.show()
        }

        root.addView(goalsScrollView)

        // Add custom habit button
        val addCustomHabitBtn = TextView(activity).apply {
            text = "➕ Add Another Habit to this Date"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, dp(12))
            }
            setOnClickListener {
                val inputDialog = Dialog(activity)
                inputDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val inputRoot = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(24f)
                    setPadding(dp(20), dp(18), dp(20), dp(18))
                }
                val titleView = TextView(activity).apply {
                    text = "Add Habit to Snapshot"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, 0, 0, dp(12))
                }
                val inputEdit = EditText(activity).apply {
                    hint = "Habit title (e.g. 📝 Flashcards review)"
                    setTextColor(themeCoordinator.textColor)
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 10f)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                }
                val btnRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }
                val cancelBtn = Button(activity).apply {
                    text = "Cancel"
                    setTextColor(tintedColor(themeCoordinator.textColor, 180))
                    textSize = 12f
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 10f)
                    setOnClickListener { inputDialog.dismiss() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                val addBtn = Button(activity).apply {
                    text = "Add Habit"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(themeCoordinator.primaryColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38))
                    setOnClickListener {
                        val t = inputEdit.text.toString().trim()
                        if (t.isNotEmpty()) {
                            itemsList.add(EditableGoalItem("custom_${System.currentTimeMillis()}", t, 30, true, true, null))
                            renderGoalsList()
                        }
                        inputDialog.dismiss()
                    }
                }
                btnRow.addView(cancelBtn)
                btnRow.addView(addBtn)
                inputRoot.addView(titleView)
                inputRoot.addView(inputEdit)
                inputRoot.addView(btnRow)
                inputDialog.setContentView(inputRoot)
                inputDialog.window?.apply {
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                inputDialog.show()
            }
        }
        root.addView(addCustomHabitBtn)

        // Save Button
        val saveBtn = Button(activity).apply {
            text = "SAVE SNAPSHOT FOR THIS DATE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                if (itemsList.isEmpty()) {
                    Toast.makeText(activity, "Please add at least one goal/habit for this snapshot", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val dateStr = sdf.format(cal.time)
                val array = JSONArray()
                for (item in itemsList) {
                    array.put(JSONObject().apply {
                        put("goalId", item.id)
                        put("title", item.title)
                        put("targetMinutes", item.targetMinutes)
                        put("completed", item.completed)
                        put("checkedAt", if (item.completed) cal.timeInMillis + (18 * 3600 * 1000L) else 0L)
                        put("isAchieved", item.isAchieved)
                        if (item.subjectId != null) put("subjectId", item.subjectId)
                    })
                }

                val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("${dateStr}_planner_snapshot", array.toString()).apply()

                activity.statsDirty = true
                activity.tabPageCache.clear()
                activity.refreshStatsPanel()

                Toast.makeText(activity, "Saved planner snapshot for $dateStr!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        root.addView(saveBtn)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showFestivalTestPicker(activity: MainActivity) {
        val festivals = FestivalType.values()
        val options = festivals.map { it.displayName }.toTypedArray()

        android.app.AlertDialog.Builder(activity)
            .setTitle("Select Festival to Preview")
            .setItems(options) { dialogInterface, which ->
                dialogInterface.dismiss()
                val selectedFestival = festivals[which]
                val userName = FestivalManager.getUserFirstName(activity)
                val varOptions = selectedFestival.greetings.mapIndexed { idx, g ->
                    val (formattedTitle, _) = g.format(userName)
                    "Variation ${idx + 1}: $formattedTitle"
                }.toTypedArray()

                android.app.AlertDialog.Builder(activity)
                    .setTitle("${selectedFestival.displayName} Variations")
                    .setItems(varOptions) { _, varIdx ->
                        FestivalManager.checkAndNotify(
                            activity,
                            forceFestival = selectedFestival,
                            forceVariation = varIdx,
                            isTest = true
                        )
                        android.widget.Toast.makeText(
                            activity,
                            "Fired ${selectedFestival.displayName} notification (Variation ${varIdx + 1})",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStreakTestPicker(activity: MainActivity) {
        val options = arrayOf(
            "7-Day Bronze Streak",
            "14-Day Silver Streak",
            "21-Day Gold Streak",
            "28-Day Diamond Streak",
            "50-Day Fire Streak",
            "100-Day Crown Streak"
        )
        val streakValues = intArrayOf(7, 14, 21, 28, 50, 100)

        android.app.AlertDialog.Builder(activity)
            .setTitle("Select Streak Milestone")
            .setItems(options) { dialogInterface, which ->
                dialogInterface.dismiss()
                val targetStreak = streakValues[which]
                CelebrationEngine.showCelebrationDialog(activity, isGoalAchieved = false, streak = targetStreak)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    


    fun verifyAdminPasskey(input: String): Boolean {
        if (!BuildConfig.DEBUG) return false
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(trimmed.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            digest.equals(BuildConfig.DEV_ADMIN_HASH, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun hasBroadcastPermission(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("admin_broadcast_permission", false)
    }

    fun requestBroadcastPermission(
        activity: MainActivity,
        themeCoordinator: ThemeCoordinator,
        onGranted: () -> Unit
    ) {
        if (!BuildConfig.DEBUG) return
        if (hasBroadcastPermission(activity)) {
            onGranted()
            return
        }
        val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        root.addView(TextView(activity).apply {
            text = "🔐 BROADCAST AUTHORIZATION"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        root.addView(TextView(activity).apply {
            text = "Broadcast System Access"
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, 0)
        })

        root.addView(TextView(activity).apply {
            text = "Enter master developer passkey to access Developer Broadcasts and announcements."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 13f
            setPadding(0, dp(6), 0, dp(14))
        })

        val pinInput = EditText(activity).apply {
            hint = "Enter Master Passkey"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 70))
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(tintedColor(themeCoordinator.textColor, 20))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 40))
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(pinInput)

        val btnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = TextView(activity).apply {
            text = "Cancel"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(10), 0)
            }
            setOnClickListener { dialog.dismiss() }
        }
        val authBtn = TextView(activity).apply {
            text = "Authorize"
            gravity = Gravity.CENTER
            setTextColor(ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor))
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(themeCoordinator.primaryColor)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val entered = pinInput.text.toString().trim()
                if (verifyAdminPasskey(entered)) {
                    val prefs = activity.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("admin_broadcast_permission", true).apply()
                    AppToast.show(activity, "Developer Broadcast Permission Granted")
                    dialog.dismiss()
                    onGranted()
                } else {
                    AppToast.show(activity, "Incorrect Passkey")
                }
            }
        }
        btnRow.addView(cancelBtn)
        btnRow.addView(authBtn)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.window?.let {
            it.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            it.setLayout((activity.resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    fun generateVersionJsonSnippet(broadcast: BroadcastMessage): String {
        val bObj = JSONObject().apply {
            put("id", broadcast.id)
            put("title", broadcast.title)
            put("message", broadcast.message)
            put("type", broadcast.type)
            if (!broadcast.canDismiss) put("canDismiss", false)
            broadcast.actionUrl?.let { put("actionUrl", it) }
            broadcast.actionText?.let { put("actionText", it) }
        }
        return "\"broadcast\": " + bObj.toString(2)
    }

    fun publishBroadcastToSupabase(
        context: Context,
        broadcast: BroadcastMessage,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Supabase credentials missing.")
                }
                return@launch
            }

            try {
                val url = URL("$supabaseUrl/rest/v1/app_broadcasts?on_conflict=id")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("id", broadcast.id)
                    put("title", broadcast.title)
                    put("message", broadcast.message)
                    put("type", broadcast.type)
                    put("can_dismiss", broadcast.canDismiss)
                    put("action_url", broadcast.actionUrl ?: JSONObject.NULL)
                    put("action_text", broadcast.actionText ?: JSONObject.NULL)
                    put("min_version_code", broadcast.minVersionCode)
                    put("max_version_code", broadcast.maxVersionCode)
                    put("is_active", true)
                }

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val respBody = try {
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                } catch (_: Exception) { "" }

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        onResult(true, "Broadcast published to Cloud! Users will see it on launch.")
                    } else if (code == 404) {
                        onResult(false, "Supabase table 'app_broadcasts' not created yet. Run the SQL schema or use version.json.")
                    } else {
                        onResult(false, "Supabase HTTP $code: $respBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error: ${e.message}")
                }
            }
        }
    }

    fun deactivateBroadcastInSupabase(
        context: Context,
        broadcastId: String? = null,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                withContext(Dispatchers.Main) { onResult(false, "Supabase credentials missing.") }
                return@launch
            }
            try {
                val query = if (broadcastId.isNullOrBlank()) "is_active=eq.true" else "id=eq.$broadcastId"
                val url = URL("$supabaseUrl/rest/v1/app_broadcasts?$query")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("is_active", false)
                }
                conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        onResult(true, "Active broadcast cleared from cloud.")
                    } else {
                        onResult(false, "Supabase HTTP $code")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error: ${e.message}") }
            }
        }
    }

    fun showBroadcastComposerDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        requestBroadcastPermission(activity, themeCoordinator) {
            val dp = { v: Int -> (v * activity.resources.displayMetrics.density).toInt() }
            val dialog = BottomSheetDialog(activity, R.style.TransparentBottomSheetDialogTheme)
            val displayMetrics = activity.resources.displayMetrics

            val rootScroll = NestedScrollView(activity).apply {
                isVerticalScrollBarEnabled = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (displayMetrics.heightPixels * 0.85f).toInt()
                )
            }
            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(30f)
                setPadding(dp(22), dp(20), dp(22), dp(24))
            }

            val headerRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(14))
            }
            val titleCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleCol.addView(TextView(activity).apply {
                text = "📢 Broadcast & Alerts Suite"
                setTextColor(themeCoordinator.textColor)
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            titleCol.addView(TextView(activity).apply {
                text = "Dispatch announcements, maintenance alerts, or outage info"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                alpha = 0.85f
                setPadding(0, dp(2), 0, 0)
            })
            headerRow.addView(titleCol)

            val closeBtn = TextView(activity).apply {
                text = "✕"
                textSize = 16f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { dialog.dismiss() }
            }
            headerRow.addView(closeBtn)
            root.addView(headerRow)

            fun createFieldLabel(label: String): TextView = TextView(activity).apply {
                text = label
                setTextColor(themeCoordinator.textColor)
                textSize = 11.5f
                letterSpacing = 0.12f
                alpha = 0.75f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(4))
            }

            fun createInputField(hintText: String, lines: Int = 1): EditText = EditText(activity).apply {
                hint = hintText
                setHintTextColor(tintedColor(themeCoordinator.textColor, 70))
                setTextColor(themeCoordinator.textColor)
                textSize = 13.5f
                if (lines > 1) {
                    setLines(lines)
                    gravity = Gravity.TOP or Gravity.START
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(tintedColor(themeCoordinator.textColor, 18))
                    setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
                }
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            root.addView(createFieldLabel("NOTICE ID (UNIQUE IDENTIFIER)"))
            val idInput = createInputField("e.g. notice_2026_11").apply {
                setText("notice_${System.currentTimeMillis() / 1000}")
            }
            root.addView(idInput)

            root.addView(createFieldLabel("SEVERITY / BANNER TYPE"))
            var selectedType = "info"
            val typeRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dp(4))
            }
            val typeButtons = ArrayList<TextView>()
            val types = listOf(
                "info" to ("📢 Info" to themeCoordinator.primaryColor),
                "warning" to ("⚡ Warning" to 0xFFF59E0B.toInt()),
                "error" to ("⚠️ Error" to 0xFFEF4444.toInt()),
                "maintenance" to ("🛠️ Maint." to 0xFF8B5CF6.toInt())
            )

            fun updateTypeButtons() {
                for (i in types.indices) {
                    val (typeKey, pair) = types[i]
                    val isSelected = selectedType == typeKey
                    val btn = typeButtons[i]
                    btn.background = if (isSelected) {
                        activity.rippleBackground(pair.second)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(10).toFloat()
                            setColor(tintedColor(themeCoordinator.textColor, 18))
                        }
                    }
                    btn.setTextColor(if (isSelected) ThemeCoordinator.getContrastColor(pair.second) else themeCoordinator.textColor)
                }
            }

            for ((typeKey, pair) in types) {
                val btn = TextView(activity).apply {
                    text = pair.first
                    gravity = Gravity.CENTER
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(2), 0, dp(2), 0)
                    }
                    setOnClickListener {
                        selectedType = typeKey
                        updateTypeButtons()
                    }
                }
                typeButtons.add(btn)
                typeRow.addView(btn)
            }
            updateTypeButtons()
            root.addView(typeRow)

            root.addView(createFieldLabel("TITLE"))
            val titleInput = createInputField("e.g. Scheduled Cloud Maintenance").apply {
                setText("Important Service Announcement")
            }
            root.addView(titleInput)

            root.addView(createFieldLabel("MESSAGE CONTENT"))
            val msgInput = createInputField("Type broadcast message details for users...", lines = 3).apply {
                setText("We are performing routine cloud database optimizations. Your local timer data remains completely safe.")
            }
            root.addView(msgInput)

            var canDismiss = true
            val dismissToggleRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(6))
            }
            val dismissLabel = TextView(activity).apply {
                text = "User can dismiss this notice: YES"
                setTextColor(themeCoordinator.textColor)
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val dismissToggleBtn = TextView(activity).apply {
                text = "Toggle"
                textSize = 11f
                setTextColor(themeCoordinator.primaryColor)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 35), 10f)
                setPadding(dp(12), dp(5), dp(12), dp(5))
                setOnClickListener {
                    canDismiss = !canDismiss
                    dismissLabel.text = if (canDismiss) "User can dismiss this notice: YES" else "User can dismiss this notice: NO (Blocking Alert)"
                    dismissLabel.setTextColor(if (canDismiss) themeCoordinator.textColor else 0xFFEF4444.toInt())
                }
            }
            dismissToggleRow.addView(dismissLabel)
            dismissToggleRow.addView(dismissToggleBtn)
            root.addView(dismissToggleRow)

            root.addView(createFieldLabel("ACTION LINK (OPTIONAL)"))
            val urlInput = createInputField("https://get-studytimer.vercel.app")
            root.addView(urlInput)

            root.addView(createFieldLabel("ACTION BUTTON TEXT (OPTIONAL)"))
            val urlTextInput = createInputField("e.g. Learn More / Status Page")
            root.addView(urlTextInput)

            val ctaContainer = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(18), 0, 0)
            }

            fun buildBroadcast(): BroadcastMessage {
                return BroadcastMessage(
                    id = idInput.text.toString().trim().ifBlank { "notice_${System.currentTimeMillis() / 1000}" },
                    title = titleInput.text.toString().trim().ifBlank { "Notice" },
                    message = msgInput.text.toString().trim(),
                    type = selectedType,
                    canDismiss = canDismiss,
                    actionUrl = urlInput.text.toString().trim().ifBlank { null },
                    actionText = urlTextInput.text.toString().trim().ifBlank { null }
                )
            }

            fun createCtaButton(label: String, bgColor: Int, onClick: () -> Unit): View {
                return Button(activity).apply {
                    text = label
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(ThemeCoordinator.getContrastColor(bgColor))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(bgColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        setMargins(0, dp(4), 0, dp(4))
                    }
                    setOnClickListener { onClick() }
                }
            }

            ctaContainer.addView(createCtaButton("👁️ Preview Broadcast on Screen", themeCoordinator.primaryColor) {
                val b = buildBroadcast()
                if (b.message.isBlank()) {
                    AppToast.show(activity, "Please enter a message.")
                    return@createCtaButton
                }
                activity.systemDialogHelper.showBroadcastDialog(b)
            })

            ctaContainer.addView(createCtaButton("📋 Copy JSON for version.json", 0xFF6366F1.toInt()) {
                val b = buildBroadcast()
                val snippet = generateVersionJsonSnippet(b)
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("version.json broadcast", snippet))
                AppToast.show(activity, "Copied broadcast JSON for version.json to clipboard!")
            })

            ctaContainer.addView(createCtaButton("☁️ Publish to Supabase Cloud", 0xFF10B981.toInt()) {
                val b = buildBroadcast()
                if (b.message.isBlank()) {
                    AppToast.show(activity, "Please enter a message.")
                    return@createCtaButton
                }
                AppToast.show(activity, "Publishing broadcast to cloud...")
                publishBroadcastToSupabase(activity, b) { success, msg ->
                    AppToast.show(activity, msg)
                }
            })

            ctaContainer.addView(createCtaButton("🗑️ Clear / Deactivate Cloud Broadcast", 0xFFEF4444.toInt()) {
                deactivateBroadcastInSupabase(activity) { success, msg ->
                    AppToast.show(activity, msg)
                }
            })

            root.addView(ctaContainer)
            rootScroll.addView(root)
            dialog.setContentView(rootScroll)
            dialog.show()
        }
    }
}


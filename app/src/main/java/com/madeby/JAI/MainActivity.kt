package com.madeby.JAI

import android.app.Dialog
import android.app.NotificationChannel
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

import android.app.NotificationManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.HapticFeedbackConstants
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.MotionEvent
import android.widget.Toast
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

private val dailyQuotes = listOf(
    "\"The secret of getting ahead is getting started.\" \u2014 Mark Twain",
    "\"Success is the sum of small efforts, repeated day in and day out.\" \u2014 Robert Collier",
    "\"The future depends on what you do today.\" \u2014 Mahatma Gandhi",
    "\"It always seems impossible until it's done.\" \u2014 Nelson Mandela",
    "\"Education is the most powerful weapon which you can use to change the world.\" \u2014 Nelson Mandela",
    "\"The expert in anything was once a beginner.\" \u2014 Helen Hayes",
    "\"Well done is better than well said.\" \u2014 Benjamin Franklin",
    "\"Quality is not an act, it is a habit.\" \u2014 Aristotle",
    "\"Our greatest glory is not in never falling, but in rising every time we fall.\" \u2014 Confucius",
    "\"He who has a why to live can bear almost any how.\" \u2014 Friedrich Nietzsche",
    "\"Do what you can, with what you have, where you are.\" \u2014 Theodore Roosevelt",
    "\"The beautiful thing about learning is that no one can take it away from you.\" \u2014 B. B. King",
    "\"An investment in knowledge pays the best interest.\" \u2014 Benjamin Franklin",
    "\"The roots of education are bitter, but the fruit is sweet.\" \u2014 Aristotle",
    "\"Act as if what you do makes a difference. It does.\" \u2014 William James",
    "\"Do not wait to strike till the iron is hot, but make it hot by striking.\" \u2014 William Butler Yeats",
    "\"Perseverance is not a long race; it is many short races one after another.\" \u2014 Walter Elliot",
    "\"Knowing is not enough; we must apply. Willing is not enough; we must do.\" \u2014 Johann Wolfgang von Goethe",
    "\"The best way to predict the future is to create it.\" \u2014 Peter Drucker",
    "\"Don't watch the clock; do what it does. Keep going.\" \u2014 Sam Levenson",
    "\"Knowing yourself is the beginning of wisdom.\" \u2014 Aristotle",
    "\"We become what we repeatedly do.\" \u2014 Aristotle",
    "\"To improve is to change; to be perfect is to change often.\" \u2014 Winston Churchill",
    "\"The greatest victory is over yourself.\" \u2014 Plato",
    "\"No man ever steps in the same river twice.\" \u2014 Heraclitus",
    "\"Freedom is the power to choose our own chains.\" \u2014 Jean-Jacques Rousseau",
    "\"The unexamined life is not worth living.\" \u2014 Socrates",
    "\"The mind is not a vessel to be filled but a fire to be kindled.\" \u2014 Plutarch",
    "\"Patience is power.\" \u2014 Rumi",
    "\"Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.\" \u2014 Rumi",
    "\"Respond intelligently even to unintelligent treatment.\" \u2014 Lao Tzu",
    "\"Nature does not hurry, yet everything is accomplished.\" \u2014 Lao Tzu",
    "\"Be the change that you wish to see in the world.\" \u2014 Mahatma Gandhi",
    "\"Strength does not come from physical capacity. It comes from an indomitable will.\" \u2014 Mahatma Gandhi",
    "\"Live as if you were to die tomorrow. Learn as if you were to live forever.\" \u2014 Mahatma Gandhi",
    "\"Arise, awake, and stop not till the goal is reached.\" \u2014 Swami Vivekananda",
    "\"Take risks in your life. If you win, you can lead; if you lose, you can guide.\" \u2014 Swami Vivekananda",
    "\"All power is within you; you can do anything.\" \u2014 Swami Vivekananda",
    "\"Dream is not that which you see while sleeping; it is something that does not let you sleep.\" \u2014 A. P. J. Abdul Kalam",
    "\"Excellence happens not by accident. It is a process.\" \u2014 A. P. J. Abdul Kalam",
    "\"If you want to shine like a sun, first burn like a sun.\" \u2014 A. P. J. Abdul Kalam",
    "\"Failure will never overtake me if my determination to succeed is strong enough.\" \u2014 Og Mandino",
    "\"The best revenge is to be unlike him who performed the injury.\" \u2014 Marcus Aurelius",
    "\"Waste no time on what you cannot control.\" \u2014 Epictetus",
    "\"Difficulties are things that show what people are.\" \u2014 Epictetus",
    "\"The willing are led by fate; the unwilling are dragged.\" \u2014 Cleanthes",
    "\"To be everywhere is to be nowhere.\" \u2014 Seneca",
    "\"Every new beginning comes from some other beginning's end.\" \u2014 Seneca",
    "\"Without continual growth and progress, such words as improvement and success have no meaning.\" \u2014 Benjamin Franklin",
    "\"The journey itself is my home.\" \u2014 Matsuo Bash\u014D",
    "\"The only source of knowledge is experience.\" \u2014 Albert Einstein",
    "\"Logic will get you from A to B. Imagination will take you everywhere.\" \u2014 Albert Einstein",
    "\"Anyone who has never made a mistake has never tried anything new.\" \u2014 Albert Einstein",
    "\"Do not pray for an easy life; pray for the strength to endure a difficult one.\" \u2014 Bruce Lee",
    "\"Absorb what is useful, discard what is not.\" \u2014 Bruce Lee",
    "\"The successful warrior is the average man, with laser-like focus.\" \u2014 Bruce Lee",
    "\"The only true failure is the failure to learn.\" \u2014 John C. Maxwell",
    "\"Success is achieved and maintained by those who try and keep trying.\" \u2014 W. Clement Stone",
    "\"There is no substitute for hard work.\" \u2014 Thomas A. Edison",
    "\"The only limit to our realization of tomorrow is our doubts of today.\" \u2014 Franklin D. Roosevelt",
    "\"It does not matter how slowly you go as long as you do not stop.\" \u2014 Confucius",
    "\"Happiness depends upon ourselves.\" \u2014 Aristotle",
    "\"The mind is everything. What you think you become.\" \u2014 Gautama Buddha",
    "\"The best preparation for tomorrow is doing your best today.\" \u2014 H. Jackson Brown Jr.",
    "\"Energy and persistence conquer all things.\" \u2014 Benjamin Franklin",
    "\"Life is really simple, but we insist on making it complicated.\" \u2014 Confucius",
    "\"The greatest wealth is to live content with little.\" \u2014 Plato",
    "\"Do not let what you cannot do interfere with what you can do.\" \u2014 John Wooden",
    "\"If you can dream it, you can do it.\" \u2014 Walt Disney",
    "\"The way to get started is to quit talking and begin doing.\" \u2014 Walt Disney",
    "\"Success usually comes to those who are too busy to be looking for it.\" \u2014 Henry David Thoreau",
    "\"What lies behind us and what lies before us are tiny matters compared to what lies within us.\" \u2014 Ralph Waldo Emerson",
    "\"Fortune favors the bold.\" \u2014 Virgil",
    "\"The harder you work, the luckier you get.\" \u2014 Gary Player",
    "\"A journey is best measured in friends, rather than miles.\" \u2014 Tim Cahill",
    "\"What you do speaks so loudly that I cannot hear what you say.\" \u2014 Ralph Waldo Emerson",
    "\"Success is where preparation and opportunity meet.\" \u2014 Bobby Unser",
    "\"No act of kindness, no matter how small, is ever wasted.\" \u2014 Aesop",
    "\"The future belongs to those who prepare for it today.\" \u2014 Malcolm X",
    "\"The purpose of our lives is to be happy.\" \u2014 Dalai Lama",
    "\"Difficulties strengthen the mind, as labor does the body.\" \u2014 Seneca",
    "\"Waste no more time arguing what a good person should be. Be one.\" \u2014 Marcus Aurelius",
    "\"The soul becomes dyed with the color of its thoughts.\" \u2014 Marcus Aurelius",
    "\"He who learns but does not think is lost.\" \u2014 Confucius",
    "\"Knowledge speaks, but wisdom listens.\" \u2014 Jimi Hendrix",
    "\"The only impossible journey is the one you never begin.\" \u2014 Tony Robbins",
)

class MainActivity : AppCompatActivity() {

    private var currentPanel = AppPanel.FOCUS
    private var currentTimerState = TimerState.IDLE
    private var currentStatsTab = AppStatsTab.OVERVIEW
    private var currentSettingsTab = AppSettingsTab.SIMPLE
    private var tabDragSlop = 12
    private var tabDragArmed = false
    private var tabDragActive = false
    private var tabDragSide = 1
    private var tabDragOverlay: FrameLayout? = null
    private var tabDragDownX = 0f
    private var tabDragDownY = 0f
    private var tabDragLastX = 0f
    private var tabDragLastT = 0L
    private var tabDragVelocityX = 0f
    private var tabDragSettling = false
    private var tabDragSettleIsCommit = false
    private var tabDragSettleToken = 0
    private var tabDragCommitStatsTab = AppStatsTab.OVERVIEW
    private var tabDragCommitSettingsTab = AppSettingsTab.SIMPLE
    private class CachedTabPage(val view: View, val statsGen: Int, val themeSig: String)
    private val tabPageCache = HashMap<String, CachedTabPage>()
    private var selectedDaysFilter = 7

    private var accumulatedStudy: Long = 0
    private var currentBreakSeconds: Long = 0

    private var timerMode: String = "STOPWATCH"
    private var focusCountdownSecs: Long = 1500L
    private var focusRemainingSecs: Long = 0L
    private var prePauseState: TimerState = TimerState.STUDYING

    private var lastKeepScreenOn = -1

    private var pauseBlinkAnimator: ValueAnimator? = null

    private var frontFlipAnim: ValueAnimator? = null
    private var backFlipAnim: ValueAnimator? = null

    private var isDevModeUnlocked = false
    private var isAdjustingFocusMode = true
    private var showFocusHueBar = false
    private var showBreakHueBar = false
    private var updateDialogRef: android.app.Dialog? = null
    private var batteryOptDialogRef: android.app.Dialog? = null
    private var settingsScrollViewRef: ScrollView? = null
    private var pendingSettingsScrollY = 0

    private var statsSnapshotCache: StatsSnapshot? = null
    private var statsSnapshotGen = 0
    private var statsDirty = true
    private var statsInternalRefresh = false
    private var lastStyleKey = ""
    private var lastTickTimerState: TimerState? = null
    private var lastDayBucket: Long = -1L
    private var cachedTodayStr = ""

    private lateinit var themeCoordinator: ThemeCoordinator
    private lateinit var backupManager: BackupManager

    private lateinit var rootLayout: LinearLayout
    private lateinit var panelContainer: LinearLayout

    private lateinit var statusBadge: TextView
    private lateinit var studyTimerDisplay: TextView
    private lateinit var breakTimerDisplay: TextView
    private lateinit var timerRing: TimerRingView
    private lateinit var mainBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var stopBtn: HoldRingButton
    private lateinit var controlActionContainer: LinearLayout
    private lateinit var panelHost: FrameLayout
    private lateinit var navHeader: LinearLayout
    private lateinit var statusBadgeContainer: LinearLayout
    private var isZenModeActive = false
    private var isNavigatingBack = false
    private var pendingAnchorDate: String? = null

    private var currentTimelineLimit = 15
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> showRestoreConfirmDialog(uri) }
        }
    }

    private fun showRestoreConfirmDialog(uri: Uri) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = "\u267B\uFE0F RESTORE BACKUP"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Restore backup?"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = "Importing replaces ALL current study data with the backup file. This cannot be undone."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = Button(this).apply {
            text = "CANCEL"
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val restoreBtn = Button(this).apply {
            text = "RESTORE"
            setTextColor(themeCoordinator.bgColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                dialog.dismiss()
                val success = backupManager.importDataFromJSON(uri)
                if (success) {
                    Toast.makeText(this@MainActivity, "Logs restored successfully!", Toast.LENGTH_SHORT).show()
                    themeCoordinator.applyThemeCoordinates()
                    navigateToPanel(AppPanel.SETTINGS)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse backup file structure.", Toast.LENGTH_SHORT).show()
                }
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(restoreBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = backupManager.exportDataToJSON(uri)
                if (success) {
                    Toast.makeText(this, "Logs exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to export logs.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val csvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = exportCsv(uri)
                if (success) {
                    Toast.makeText(this, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to export CSV.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private val HOLD_TO_END_DURATION_MS = 1200L
    private var holdStartTime = 0L
    private var isHoldingStop = false

    private fun resetHoldToEnd() {
        isHoldingStop = false
        if (::stopBtn.isInitialized) {
            stopBtn.isPressed = false
            stopBtn.progress = 0f
        }
        handler.removeCallbacks(holdToEndRunnable)
    }

    private val holdToEndRunnable = object : Runnable {
        override fun run() {
            if (isDestroyed || !isHoldingStop || !::stopBtn.isInitialized || !stopBtn.isPressed) {
                resetHoldToEnd()
                return
            }
            val elapsed = SystemClock.uptimeMillis() - holdStartTime
            if (elapsed < 0L || elapsed > HOLD_TO_END_DURATION_MS + 1500L) {
                resetHoldToEnd()
                return
            }
            val progress = (elapsed.toFloat() / HOLD_TO_END_DURATION_MS).coerceIn(0f, 1f)
            stopBtn.progress = progress
            if (progress >= 1f) {
                resetHoldToEnd()
                handleStopSession()
                Toast.makeText(this@MainActivity, "Session saved successfully.", Toast.LENGTH_SHORT).show()
            } else {
                handler.postDelayed(this, 16L)
            }
        }
    }

    private val CHANNEL_ID = "study_timer_channels"

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().remove("notification_perm_prompt_count").apply()
            return
        }
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val promptCount = prefs.safeInt("notification_perm_prompt_count", 0)
        if (promptCount == 0) {
            prefs.edit().putInt("notification_perm_prompt_count", 1).apply()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            return
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Enable notifications?")
                .setMessage("Notifications keep your study session visible and remind you to reach your daily goal. You can change this anytime in Settings.")
                .setPositiveButton("Allow") { _, _ ->
                    prefs.edit().putInt("notification_perm_prompt_count", promptCount + 1).apply()
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Not now", null)
                .create()
                .show()
        } else {
            android.app.AlertDialog.Builder(this)
                .setTitle("Notifications are off")
                .setMessage("Notifications are blocked. You can still use the app, but you won't get session updates or daily goal reminders. Enable them anytime in system Settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    openNotificationSettings()
                }
                .setNegativeButton("Not now", null)
                .create()
                .show()
        }
    }

    private fun maybePromptBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val opens = prefs.safeInt("battery_opt_opens", 0) + 1
        prefs.edit().putInt("battery_opt_opens", opens).apply()
        if (opens % 3 != 0) return
        showBatteryOptimizationDialog()
    }

    private fun showBatteryOptimizationDialog() {
        if (batteryOptDialogRef?.isShowing == true) return
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = "\u26A1 RUN IN BACKGROUND"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Keep your timer running"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = "For long study sessions, this permission keeps the app running in the background so the system doesn't pause your timer and you don't lose your progress and data."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val laterBtn = TextView(this).apply {
            text = "LATER"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
            setOnClickListener { dialog.dismiss() }
        }
        val allowBtn = TextView(this).apply {
            text = "ALLOW"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(themeCoordinator.primaryColor) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                } catch (_: Exception) {
                    openBatterySettings()
                }
            }
        }
        buttonRow.addView(laterBtn)
        buttonRow.addView(allowBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        batteryOptDialogRef = dialog
        dialog.show()
    }

    private fun openBatterySettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")))
        }
    }

    @Suppress("DEPRECATION")
    private fun currentVersionCodeLong(): Long =
        runCatching { PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0)) }.getOrDefault(0L)

    @Suppress("DEPRECATION")
    private fun currentVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0.0"

    private fun openUpdateUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open the update page.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates(manual: Boolean) {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!manual) {
            val launches = prefs.safeInt("update_launch_count", 0) + 1
            prefs.edit().putInt("update_launch_count", launches).apply()
            if (launches % 5 != 0) return
        }
        if (manual) {
            Toast.makeText(this, "Checking for updates\u2026", Toast.LENGTH_SHORT).show()
        }
        UpdateChecker.check(this) { info ->
            if (isDestroyed || isFinishing) return@check
            if (info == null) {
                if (manual) {
                    Toast.makeText(this, "Couldn't check for updates. Check your connection.", Toast.LENGTH_SHORT).show()
                }
                return@check
            }
            if (info.versionCode <= currentVersionCodeLong()) {
                if (manual) {
                    Toast.makeText(this, "You're up to date (v${currentVersionName()})", Toast.LENGTH_SHORT).show()
                }
                return@check
            }
            if (!manual && prefs.safeInt("update_dismissed_version", -1) == info.versionCode) {
                return@check
            }
            showUpdateAvailableDialog(info)
        }
    }

    private fun showUpdateAvailableDialog(info: UpdateInfo) {
        if (updateDialogRef?.isShowing == true) return
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = "\uD83D\uDD04 UPDATE AVAILABLE"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "v${info.versionName} is ready"
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        if (info.hasReleaseNotes) {
            content.addView(TextView(this).apply {
                text = info.releaseNotes
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
        }
        content.addView(TextView(this).apply {
            text = "A newer version is available. Your download will start now."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val laterBtn = TextView(this).apply {
            text = "LATER"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
            setOnClickListener {
                prefs.edit().putInt("update_dismissed_version", info.versionCode).apply()
                dialog.dismiss()
            }
        }
        val updateBtn = TextView(this).apply {
            text = "UPDATE"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(themeCoordinator.primaryColor) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                prefs.edit().remove("update_dismissed_version").apply()
                dialog.dismiss()
                openUpdateUrl(info.apkUrl ?: info.url)
            }
        }
        buttonRow.addView(laterBtn)
        buttonRow.addView(updateBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setOnCancelListener { prefs.edit().putInt("update_dismissed_version", info.versionCode).apply() }
        updateDialogRef = dialog
        dialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().remove("notification_perm_prompt_count").apply()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun lightenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * amount).coerceIn(0f, 1f)
        hsv[1] = (hsv[1] * (1f - amount * 0.45f)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun darkenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun rippleBackground(color: Int): android.graphics.drawable.RippleDrawable {
        val shape = themeCoordinator.createGlowGradient(color, 80f)
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(90, 255, 255, 255)),
            shape,
            shape
        )
    }

    private fun outlinedButtonBackground(): android.graphics.drawable.RippleDrawable {
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
            GradientDrawable().apply { cornerRadius = 80f; setColor(0x00000000); setStroke(3, if (themeCoordinator.isGlassStyle()) tintedColor(themeCoordinator.primaryColor, 120) else themeCoordinator.boxColor) },
            null
        )
    }

    private fun pauseButtonVisibility(): Int {
        val showPause = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getBoolean("show_pause_button", true)
        return if (showPause) View.VISIBLE else View.GONE
    }

    private fun metricRowBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(tintedColor(color, 64))
        }
    }

    private fun formatGoalLabel(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun dailyGoalSecs(): Long {
        return getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getLong("daily_goal_secs", 2700L)
    }

    private fun resolveGoalFor(dateStr: String): Long {
        return getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .getLong("${dateStr}_goal_secs", dailyGoalSecs())
    }

    private fun applyHoldToRepeat(view: View, initialDelayMs: Long = 300L, step: () -> Unit) {
        val repeatHandler = Handler(Looper.getMainLooper())
        var repeatDelay = initialDelayMs
        val repeatRunnable = object : Runnable {
            override fun run() {
                step()
                repeatDelay = max(50L, repeatDelay - 25L)
                repeatHandler.postDelayed(this, repeatDelay)
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    step()
                    repeatDelay = initialDelayMs
                    repeatHandler.removeCallbacks(repeatRunnable)
                    repeatHandler.postDelayed(repeatRunnable, initialDelayMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    repeatHandler.removeCallbacks(repeatRunnable)
                    true
                }
                else -> true
            }
        }
    }

    private fun pureWhiteTimerEnabled(): Boolean {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pureWhiteTimer", false) && themeCoordinator.activeBgMode != "LIGHT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        themeCoordinator = ThemeCoordinator(this)
        backupManager = BackupManager(this)
        
        backupManager.triggerAutoRestoreIfPresent()
        themeCoordinator.applyThemeCoordinates()

        createNotificationChannel()
        GoalReminderScheduler.schedule(this)

        requestNotificationPermissionIfNeeded()

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createBackgroundDrawable()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        panelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        panelHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        panelHost.addView(panelContainer)
        rootLayout.addView(panelHost)
        setContentView(rootLayout)

        tabDragSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        currentTimerState = TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE")
        accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
        currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
        selectedDaysFilter = sharedPrefs.safeInt("selected_days_filter", 7)

        if (currentTimerState != TimerState.IDLE) {
            val resumeIntent = Intent(this, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(resumeIntent)
            } else {
                startService(resumeIntent)
            }
        }

        navigateToPanel(AppPanel.FOCUS)
        setupTimerLoop()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var lastBackTime = 0L
            override fun handleOnBackPressed() {
                if (currentPanel == AppPanel.HEATMAP) {
                    navigateToPanel(AppPanel.STATS)
                } else if (currentPanel != AppPanel.FOCUS) {
                    navigateToPanel(AppPanel.FOCUS)
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < 2000) {
                        finish()
                    } else {
                        lastBackTime = now
                        Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        if (intent?.getBooleanExtra("NOTIFICATION_TOGGLE_TRIGGER", false) == true) {
            handleStateToggle()
        }

        applyImmersiveModeForLandscape()

        maybeShowOnboarding()

    }

    private fun csvCell(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    private fun exportCsv(uri: Uri): Boolean {
        return try {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val sb = StringBuilder()

            sb.append(csvCell("StudyTimer Export")).append(',').append(csvCell(sdf.format(Date()))).append('\n')
            sb.append('\n')

            val keys = sharedPrefs.all.keys.filter { it.endsWith("_focus_total") }.sorted()
            var totalFocus = 0L
            var totalBreak = 0L
            var activeDays = 0
            var maxDayFocus = 0L
            val sessionLines = StringBuilder()

            sb.append("== Daily Summary ==\n")
            sb.append("date,weekday,focus_seconds,break_seconds,focus_formatted,break_formatted,goal_met,longest_focus_minutes\n")
            for (k in keys) {
                val d = k.removeSuffix("_focus_total")
                val f = sharedPrefs.getLong(k, 0L) + (if (d == todayStr) sharedPrefs.getLong("accumulatedStudy", 0L) else 0L)
                val b = sharedPrefs.getLong("${d}_break_total", 0L) + (if (d == todayStr) currentBreakSeconds else 0L)
                if (f <= 0L && b <= 0L) continue
                val parsed = try { sdf.parse(d) } catch (_: Exception) { null }
                val weekday = if (parsed != null) dayNameSdf.format(parsed) else ""
                val (sessions, breaks) = dayBlocks(d)
                val longest = sessions.maxOfOrNull { it.secs } ?: 0L
                sb.append(csvCell(d)).append(',')
                    .append(csvCell(weekday)).append(',')
                    .append(f).append(',')
                    .append(b).append(',')
                    .append(csvCell(formatDuration(f))).append(',')
                    .append(csvCell(formatDuration(b))).append(',')
                    .append(if (f >= resolveGoalFor(d)) "Yes" else "No").append(',')
                    .append(longest / 60).append('\n')

                val rows = ArrayList<Pair<BlockInfo, String>>()
                for (s in sessions) rows.add(Pair(s, "Focus"))
                for (bk in breaks) rows.add(Pair(bk, "Break"))
                rows.sortBy { it.first.startMs }
                for ((blk, type) in rows) {
                    sessionLines.append(csvCell(d)).append(',')
                        .append(csvCell(TimeFormat.formatWallClock(this, blk.startMs))).append(',')
                        .append(csvCell(TimeFormat.formatWallClock(this, blk.endMs))).append(',')
                        .append(type).append(',')
                        .append(blk.secs).append(',')
                        .append(csvCell(formatDuration(blk.secs))).append('\n')
                }

                totalFocus += f
                totalBreak += b
                activeDays++
                if (f > maxDayFocus) maxDayFocus = f
            }
            sb.append('\n')

            sb.append("== Sessions ==\n")
            sb.append("date,start,end,type,seconds,duration\n")
            sb.append(sessionLines)
            sb.append('\n')

            sb.append("== Totals ==\n")
            sb.append("metric,value\n")
            sb.append(csvCell("Total focus")).append(',').append(csvCell(formatDuration(totalFocus))).append('\n')
            sb.append(csvCell("Total break")).append(',').append(csvCell(formatDuration(totalBreak))).append('\n')
            sb.append(csvCell("Active days")).append(',').append(activeDays).append('\n')
            sb.append(csvCell("Average focus per active day")).append(',').append(csvCell(if (activeDays > 0) formatDuration(totalFocus / activeDays) else "0m")).append('\n')
            sb.append(csvCell("Longest day focus")).append(',').append(csvCell(formatDuration(maxDayFocus))).append('\n')
            sb.append(csvCell("Daily goal")).append(',').append(csvCell(formatDuration(dailyGoalSecs()))).append('\n')

            contentResolver.openOutputStream(uri, "w")?.use { stream ->
                stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navigateToPanel(currentPanel)
        applyImmersiveModeForLandscape()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && handleTabDragTouch(ev)) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("NOTIFICATION_TOGGLE_TRIGGER", false) == true) {
            handleStateToggle()
        }
    }


    private fun handleTabDragTouch(ev: MotionEvent): Boolean {
        if (currentPanel != AppPanel.STATS && currentPanel != AppPanel.SETTINGS) {
            tabDragArmed = false
            tabDragActive = false
            return false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (tabDragSettling) finalizeSettle(tabDragSettleIsCommit)
                if (panelHost.childCount > 1) return false
                if (swipeStartedOnHorizontalScroll(ev.rawX, ev.rawY)) return false
                tabDragArmed = true
                tabDragActive = false
                tabDragDownX = ev.x
                tabDragDownY = ev.y
                tabDragLastX = ev.x
                tabDragLastT = SystemClock.uptimeMillis()
                tabDragVelocityX = 0f
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (tabDragSettling) return true
                if (!tabDragArmed) return false
                val now = SystemClock.uptimeMillis()
                if (now - tabDragLastT > 0) {
                    tabDragVelocityX = (ev.x - tabDragLastX) / (now - tabDragLastT) * 1000f
                }
                tabDragLastX = ev.x
                tabDragLastT = now
                if (!tabDragActive) {
                    val dx = ev.x - tabDragDownX
                    val dy = ev.y - tabDragDownY
                    if (Math.abs(dy) > tabDragSlop && Math.abs(dy) >= Math.abs(dx) * 1.1f) {
                        tabDragArmed = false
                        return false
                    }
                    if (Math.abs(dx) > tabDragSlop && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                        val side = if (dx < 0) 1 else -1
                        if (tabDragNeighborExists(side) && beginTabDrag(side)) {
                            tabDragActive = true
                            panelContainer.cancelPendingInputEvents()
                        } else {
                            tabDragArmed = false
                            return false
                        }
                    } else {
                        return false
                    }
                }
                val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
                val delta = Math.max(-width.toFloat(), Math.min(width.toFloat(), ev.x - tabDragDownX))
                panelContainer.translationX = delta
                tabDragOverlay?.translationX = (tabDragSide * width) + delta
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (tabDragSettling) return true
                if (!tabDragActive) {
                    tabDragArmed = false
                    return false
                }
                val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
                val delta = panelContainer.translationX
                val commit = when {
                    tabDragSide == 1 -> delta < -width * 0.22f || tabDragVelocityX < -550f
                    else -> delta > width * 0.22f || tabDragVelocityX > 550f
                }
                tabDragArmed = false
                tabDragActive = false
                if (commit) finishTabDrag() else cancelTabDrag()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (tabDragSettling) return true
                if (!tabDragActive) {
                    tabDragArmed = false
                    return false
                }
                tabDragArmed = false
                tabDragActive = false
                cancelTabDrag()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                if (tabDragActive || tabDragSettling) return true
                return false
            }
        }
        return false
    }

    private fun tabDragNeighborExists(side: Int): Boolean {
        return when (currentPanel) {
            AppPanel.STATS -> if (side == 1) currentStatsTab != AppStatsTab.TIMELINE else currentStatsTab != AppStatsTab.OVERVIEW
            AppPanel.SETTINGS -> if (side == 1) currentSettingsTab != AppSettingsTab.THEME else currentSettingsTab != AppSettingsTab.SIMPLE
            else -> false
        }
    }

    private fun applyCardStyle(v: View) {
        val r = dp(22)
        val bg = android.graphics.drawable.GradientDrawable().apply {
            setColor(themeCoordinator.bgColor)
            cornerRadius = r.toFloat()
        }
        v.background = bg
        v.clipToOutline = true
        v.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        v.elevation = dp(14).toFloat()
    }

    private fun clearCardStyle(v: View) {
        v.background = null
        v.clipToOutline = false
        v.elevation = 0f
    }

    private fun beginTabDrag(side: Int): Boolean {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val height = panelContainer.height.takeIf { it > 0 } ?: dp(240)
        tabDragSide = side
        val key = when (currentPanel) {
            AppPanel.STATS -> {
                tabDragCommitStatsTab = if (side == 1) AppStatsTab.TIMELINE else AppStatsTab.OVERVIEW
                statsTabKey(tabDragCommitStatsTab)
            }
            AppPanel.SETTINGS -> {
                tabDragCommitSettingsTab = if (side == 1) AppSettingsTab.THEME else AppSettingsTab.SIMPLE
                settingsTabKey(tabDragCommitSettingsTab)
            }
            else -> return false
        }
        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(width, height)
        }
        applyCardStyle(panelContainer)
        overlay.addView(getOrBuildTabPage(key))
        applyCardStyle(overlay)
        overlay.translationX = (side * width).toFloat()
        panelHost.addView(overlay)
        tabDragOverlay = overlay
        return true
    }

    private fun settleAnimDuration(remaining: Float): Long {
        val dist = Math.abs(remaining)
        val vel = Math.abs(tabDragVelocityX)
        val base = if (vel > 120f) (dist / vel * 1000f).toLong() else 340L
        return Math.max(150L, Math.min(340L, base))
    }

    private fun settleInterpolator() = android.view.animation.PathInterpolator(0.25f, 0.9f, 0.25f, 1f)

    private fun finishTabDrag() {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val overlay = tabDragOverlay
        val token = ++tabDragSettleToken
        tabDragSettling = true
        tabDragSettleIsCommit = true
        val targetX = (-tabDragSide * width).toFloat()
        val remaining = targetX - panelContainer.translationX
        val dur = settleAnimDuration(remaining)
        val interp = settleInterpolator()
        panelContainer.animate().translationX(targetX)
            .setDuration(dur)
            .setInterpolator(interp)
            .withLayer().start()
        overlay?.animate()?.translationX(0f)
            ?.setDuration(dur)
            ?.setInterpolator(interp)
            ?.withLayer()?.start()
        handler.postDelayed({ if (token == tabDragSettleToken) finalizeSettle(true) }, dur + 80L)
    }

    private fun cancelTabDrag() {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val overlay = tabDragOverlay
        val token = ++tabDragSettleToken
        tabDragSettling = true
        tabDragSettleIsCommit = false
        val remaining = 0f - panelContainer.translationX
        val dur = settleAnimDuration(remaining)
        val interp = settleInterpolator()
        panelContainer.animate().translationX(0f)
            .setDuration(dur)
            .setInterpolator(interp)
            .withLayer().start()
        overlay?.animate()?.translationX((tabDragSide * width).toFloat())
            ?.setDuration(dur)
            ?.setInterpolator(interp)
            ?.withLayer()?.start()
        handler.postDelayed({ if (token == tabDragSettleToken) finalizeSettle(false) }, dur + 80L)
    }

    private fun finalizeSettle(commit: Boolean) {
        if (!tabDragSettling) return
        tabDragSettling = false
        tabDragSettleToken++
        val overlay = tabDragOverlay
        panelContainer.animate().cancel()
        panelContainer.translationX = 0f
        overlay?.animate()?.cancel()
        clearCardStyle(panelContainer)
        var incomingPage: View? = null
        if (overlay != null) {
            if (overlay.parent != null) panelHost.removeView(overlay)
            if (overlay.childCount > 0) {
                incomingPage = overlay.getChildAt(0)
                overlay.removeView(incomingPage)
            }
        }
        tabDragOverlay = null
        if (commit) {
            when (currentPanel) {
                AppPanel.STATS -> {
                    currentStatsTab = tabDragCommitStatsTab
                    if (currentStatsTab == AppStatsTab.TIMELINE) currentTimelineLimit = 15
                }
                AppPanel.SETTINGS -> currentSettingsTab = tabDragCommitSettingsTab
                else -> {}
            }
            if (incomingPage != null) {
                swapPanelContent(incomingPage)
            } else {
                navigateToPanel(currentPanel)
            }
        }
        prewarmTabPages()
    }

    private fun swapPanelContent(page: View) {
        panelContainer.removeAllViews()
        panelContainer.addView(page)
        if (currentPanel == AppPanel.SETTINGS) {
            settingsScrollViewRef = findScrollViewDescendant(page)
        }
    }

    private fun findScrollViewDescendant(root: View): ScrollView? {
        if (root is ScrollView) return root
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                findScrollViewDescendant(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun statsTabKey(t: AppStatsTab): String = "S:${t.ordinal}"
    private fun settingsTabKey(t: AppSettingsTab): String = "ST:${t.ordinal}"

    private fun tabThemeSig(): String =
        "${themeCoordinator.primaryColor}|${themeCoordinator.secondaryColor}|${themeCoordinator.bgColor}|${themeCoordinator.uiStyle}|${themeCoordinator.activeBgMode}"

    private fun getOrBuildTabPage(key: String): View {
        val cached = tabPageCache[key]
        if (cached != null) {
            val valid = when {
                key.startsWith("ST:") -> cached.themeSig == tabThemeSig()
                else -> cached.statsGen == statsSnapshotGen
            }
            if (valid && cached.view.parent == null) return cached.view
        }
        val scratch = FrameLayout(this)
        val page = when {
            key.startsWith("ST:") -> {
                val prev = currentSettingsTab
                currentSettingsTab = if (key == settingsTabKey(AppSettingsTab.THEME)) AppSettingsTab.THEME else AppSettingsTab.SIMPLE
                buildSettingsPanel(scratch, captureScrollRef = false)
                currentSettingsTab = prev
                scratch.getChildAt(0)
            }
            else -> {
                val prev = currentStatsTab
                currentStatsTab = if (key == statsTabKey(AppStatsTab.TIMELINE)) AppStatsTab.TIMELINE else AppStatsTab.OVERVIEW
                if (currentStatsTab == AppStatsTab.TIMELINE) currentTimelineLimit = 15
                buildStatsPanel(scratch)
                currentStatsTab = prev
                scratch.getChildAt(0)
            }
        }
        scratch.removeView(page)
        tabPageCache[key] = if (key.startsWith("ST:")) CachedTabPage(page, 0, tabThemeSig()) else CachedTabPage(page, statsSnapshotGen, "")
        return page
    }

    private fun prewarmTabPages() {
        if (panelHost.childCount > 1 || tabDragSettling) return
        when (currentPanel) {
            AppPanel.STATS -> {
                if (statsSnapshotCache == null) return
                getOrBuildTabPage(statsTabKey(AppStatsTab.OVERVIEW))
                getOrBuildTabPage(statsTabKey(AppStatsTab.TIMELINE))
            }
            AppPanel.SETTINGS -> {
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.SIMPLE))
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.THEME))
            }
            else -> {}
        }
    }

    private fun swipeStartedOnHorizontalScroll(rawX: Float, rawY: Float): Boolean {
        val loc = IntArray(2)
        panelContainer.getLocationOnScreen(loc)
        val x = rawX - loc[0]
        val y = rawY - loc[1]
        if (x < 0 || y < 0 || x > panelContainer.width || y > panelContainer.height) return false
        val hit = findViewAt(panelContainer, x, y) ?: return false
        var v: View? = hit
        while (v != null) {
            if (v is HorizontalScrollView) return true
            v = v.parent as? View
        }
        return false
    }

    private fun findViewAt(parent: View, x: Float, y: Float): View? {
        if (parent is android.view.ViewGroup) {
            for (i in parent.childCount - 1 downTo 0) {
                val child = parent.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                    return findViewAt(child, x - child.left, y - child.top) ?: child
                }
            }
        }
        return parent
    }

    private fun buildCurrentPanel() {
        panelContainer.removeAllViews()
        rootLayout.background = themeCoordinator.createBackgroundDrawable()
        when (currentPanel) {
            AppPanel.FOCUS -> buildFocusPanel()
            AppPanel.STATS -> buildStatsPanel()
            AppPanel.SETTINGS -> buildSettingsPanel()
            AppPanel.HEATMAP -> buildHeatmapFullscreenPanel()
        }
        prewarmTabPages()
    }

    private fun navigateToPanel(targetPanel: AppPanel) {
        isNavigatingBack = false

        if (targetPanel == AppPanel.STATS && currentPanel != AppPanel.STATS) {
            currentStatsTab = AppStatsTab.OVERVIEW
        }
        statsInternalRefresh = (targetPanel == AppPanel.STATS && currentPanel == AppPanel.STATS)

        if (currentPanel == AppPanel.STATS && targetPanel != AppPanel.STATS && targetPanel != AppPanel.HEATMAP) {
            statsDirty = true
        }

        if (currentPanel == AppPanel.SETTINGS && targetPanel != AppPanel.SETTINGS) {
            Thread { backupManager.runSilentAutoBackup() }.start()
        }

        if (targetPanel == AppPanel.HEATMAP && currentPanel != AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (targetPanel != AppPanel.HEATMAP && currentPanel == AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (targetPanel != AppPanel.SETTINGS) {
            isDevModeUnlocked = false
        }

        val isSamePanel = targetPanel == currentPanel
        val prevPanel = currentPanel
        currentPanel = targetPanel

        if (isSamePanel || panelContainer.childCount == 0) {
            buildCurrentPanel()
            return
        }

        val slideRight = when {
            prevPanel == AppPanel.FOCUS && targetPanel == AppPanel.SETTINGS -> true
            prevPanel == AppPanel.SETTINGS && targetPanel == AppPanel.FOCUS -> false
            prevPanel == AppPanel.FOCUS && targetPanel == AppPanel.STATS -> false
            prevPanel == AppPanel.STATS && targetPanel == AppPanel.FOCUS -> true
            else -> prevPanel.ordinal < targetPanel.ordinal
        }

        performSlidingTransition(if (slideRight) 1 else -1) {
            buildCurrentPanel()
        }
    }

    private fun performSlidingTransition(exitDir: Int, rebuild: () -> Unit) {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val height = panelContainer.height.takeIf { it > 0 } ?: dp(240)
        val snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        panelContainer.draw(Canvas(snapshot))

        rebuild()

        for (j in 0 until panelContainer.childCount) {
            panelContainer.getChildAt(j).translationX = (-exitDir * width).toFloat()
        }

        val overlay = ImageView(this).apply {
            setImageBitmap(snapshot)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(width, height)
        }
        panelHost.addView(overlay)

        overlay.animate().translationX((exitDir * width).toFloat())
            .setDuration(280L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withLayer().start()
        for (j in 0 until panelContainer.childCount) {
            panelContainer.getChildAt(j).animate().translationX(0f)
                .setDuration(280L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withLayer().start()
        }

        handler.postDelayed({
            if (overlay.parent != null) panelHost.removeView(overlay)
        }, 340L)
    }

    private fun buildFocusPanel() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        navHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, if (isLandscape) 10 else 50, 0, 20) 
            }
        }

        val settingsIconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_settings) 
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(16, 16, 16, 16)
            background = if (themeCoordinator.isGlassStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = "Open settings"
            setOnClickListener { navigateToPanel(AppPanel.SETTINGS) }
        }
        navHeader.addView(settingsIconView)

        val headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        navHeader.addView(headerSpacer)

        val centerClocksWrapper = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val savedTimerMode = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
        if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled()) {
            val glowAlpha = when {
                themeCoordinator.activeBgMode == "OLED" -> 0.22f
                themeCoordinator.activeBgMode == "LIGHT" -> 0.3f
                else -> 0.38f
            }
            val timerGlow = ImageView(this).apply {
                setImageDrawable(themeCoordinator.createGlowBlob(themeCoordinator.primaryColor))
                alpha = glowAlpha
                layoutParams = FrameLayout.LayoutParams(dp(360), dp(360), Gravity.CENTER)
            }
            centerClocksWrapper.addView(timerGlow)
        }

        timerRing = TimerRingView(this).apply {
            visibility = if (!isLandscape && savedTimerMode == "COUNTDOWN") View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
        }

        statusBadge = TextView(this).apply {
            textSize = if (isLandscape) 13f else 14f
            setPadding(35, 12, 35, 12)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110))
        }

        statusBadgeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, 0, 0, if (isLandscape) 4 else 10)
            }
            addView(statusBadge)
        }

        studyTimerDisplay = TextView(this).apply {
            text = "00:00:00"
            textSize = if (isLandscape) 96f else 54f 
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 5)
            if (themeCoordinator.isGlassStyle() && !pureWhiteTimerEnabled()) setShadowLayer(14f, 0f, 0f, tintedColor(themeCoordinator.primaryColor, 90))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

        breakTimerDisplay = TextView(this).apply {
            text = "Break: 00:00:00"
            textSize = if (isLandscape) 24f else 20f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, if (isLandscape) 20 else 40)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        }

        centerClocksWrapper.addView(timerRing)
        centerClocksWrapper.addView(statusBadgeContainer)
        centerClocksWrapper.addView(studyTimerDisplay)
        centerClocksWrapper.addView(breakTimerDisplay)

        controlActionContainer = LinearLayout(this).apply {
            orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, if (isLandscape) 10 else 30) }
        }

        mainBtn = Button(this).apply {
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(40, 32, 40, 32)
            isSoundEffectsEnabled = false
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply { if (isLandscape) setMargins(10, 0, 10, 0) }
            setOnClickListener { handleStateToggle() }
        }

        pauseBtn = Button(this).apply {
            text = "\u275A\u275A PAUSE"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(40, 32, 40, 32)
            isSoundEffectsEnabled = false
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply {
                if (isLandscape) setMargins(10, 0, 10, 0) else setMargins(0, 24, 0, 0)
            }
            setOnClickListener { handlePause() }
        }

        stopBtn = HoldRingButton(this).apply {
            text = "HOLD TO END SESSION"
            ringColor = themeCoordinator.primaryColor
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isSoundEffectsEnabled = false
            background = outlinedButtonBackground()
            setPadding(40, 28, 40, 28)
            layoutParams = LinearLayout.LayoutParams(if (isLandscape) 0 else LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, if (isLandscape) 1f else 0f).apply { if (isLandscape) setMargins(10, 0, 10, 0) else setMargins(0, 24, 0, 0) }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        holdStartTime = SystemClock.uptimeMillis()
                        isHoldingStop = true
                        v.isPressed = true
                        progress = 0f
                        handler.removeCallbacks(holdToEndRunnable)
                        handler.post(holdToEndRunnable)
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resetHoldToEnd()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.x < 0f || event.y < 0f || event.x > v.width || event.y > v.height) {
                            resetHoldToEnd()
                        }
                        true
                    }
                    else -> true
                }
            }
        }

        controlActionContainer.addView(mainBtn)
        controlActionContainer.addView(pauseBtn)
        controlActionContainer.addView(stopBtn)

        val statsFloatingIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_insights)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(themeCoordinator.primaryColor)
            setPadding(14, 14, 14, 14)
            background = if (themeCoordinator.isGlassStyle()) themeCoordinator.createGlassIconBackground(tintedColor(themeCoordinator.primaryColor, 70)) else null
            contentDescription = "Open insights"
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.END
                setMargins(0, 0, 10, if (isLandscape) 5 else 20)
            }
        }

        panelContainer.addView(navHeader)
        panelContainer.addView(centerClocksWrapper)
        panelContainer.addView(controlActionContainer)
        
        if (!isLandscape) {
            panelContainer.addView(statsFloatingIcon)
        } else {
            navHeader.addView(statsFloatingIcon) 
        }

        updateVisualStyles()
        applyTrueFullscreenMode()
    }

    private fun recalculateStreak(todayExtra: Long = 0L) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)

        val streak = StreakCalculator.currentStreak(
            todayExtra = todayExtra,
            focusOn = { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                sharedPrefs.getLong("${sdf.format(cal.time)}_focus_total", 0L)
            },
            goalFor = if (streakGoalBased) ({ daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                resolveGoalFor(sdf.format(cal.time))
            }) else null
        )

        sharedPrefs.edit()
            .putInt("current_streak", streak)
            .putString("streak_last_calculated", todayStr)
            .apply()
    }

    private data class MonthBucket(val label: String, val focus: Long, val breakSecs: Long)

    private data class StatsSnapshot(
        val todayFocus: Long,
        val todayBreak: Long,
        val streak: Int,
        val avg7: Long,
        val yesterdaySecs: Long,
        val heroGoalSecs: Long,
        val totalLifeFocus: Long,
        val totalLifeBreak: Long,
        val totalLife: Long,
        val longestStreak: Int,
        val activeDays: Int,
        val bestWeekdayName: String,
        val bestWeekdaySecs: Long,
        val bestWeekLabel: String,
        val bestWeekSecs: Long,
        val thisWeek: Long,
        val prevWeek: Long,
        val goalHits: Int,
        val hasAnySessions: Boolean,
        val showHeatmap: Boolean,
        val showPattern: Boolean,
        val heatmapData: Map<String, Long>,
        val blockSecs7: LongArray,
        val maxBlock7: Long,
        val blockSecs30: LongArray,
        val maxBlock30: Long,
        val patternTotal7: Long,
        val patternTotal30: Long,
        val dayFocus: Map<String, Long>,
        val monthBuckets: List<MonthBucket>,
        val allHistKeys: List<String>,
        val entriesByDay: Map<String, List<TimelineEntry>>
    )

    private fun computeStatsSnapshot(): StatsSnapshot {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        reconcileDayTotals(todayStr)
        val currentSessionSecs = sharedPrefs.getLong("accumulatedStudy", 0L)
        val currentBreakSecs = currentBreakSeconds

        recalculateStreak(todayExtra = currentSessionSecs)
        val streak = sharedPrefs.safeInt("current_streak", 0)

        val todayFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L) + currentSessionSecs
        val todayBreak = sharedPrefs.getLong("${todayStr}_break_total", 0L) + currentBreakSecs

        var total7 = 0L
        for (i in 0 until 7) {
            val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i)
            val d = sdf.format(cal.time)
            total7 += sharedPrefs.getLong("${d}_focus_total", 0L)
            if (d == todayStr) total7 += currentSessionSecs
        }
        val avg7 = total7 / 7

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdaySecs = sharedPrefs.getLong("${sdf.format(yesterdayCal.time)}_focus_total", 0L)

        val heroGoalSecs = resolveGoalFor(todayStr)

        val allKeys = sharedPrefs.all.keys
        val allFocusKeys = allKeys.filter { it.endsWith("_focus_total") }

        val dayFocus = HashMap<String, Long>()
        val dayBreak = HashMap<String, Long>()
        var totalLifeFocus = 0L
        var totalLifeBreak = 0L
        for (key in allFocusKeys) {
            val dStr = key.removeSuffix("_focus_total")
            val f = sharedPrefs.getLong(key, 0L)
            val b = sharedPrefs.getLong("${dStr}_break_total", 0L)
            dayFocus[dStr] = f
            dayBreak[dStr] = b
            totalLifeFocus += f
            totalLifeBreak += b
        }
        dayFocus[todayStr] = (dayFocus[todayStr] ?: 0L) + currentSessionSecs
        dayBreak[todayStr] = (dayBreak[todayStr] ?: 0L) + currentBreakSecs
        totalLifeFocus += currentSessionSecs
        totalLifeBreak += currentBreakSecs
        val totalLife = totalLifeFocus + totalLifeBreak

        val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)

        var longestStreak = StreakCalculator.longestStreak(
            allFocusKeys.mapNotNull { key ->
                val dStr = key.removeSuffix("_focus_total")
                val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: return@mapNotNull null
                Pair(parsed, dayFocus[dStr] ?: 0L)
            },
            goalFor = if (streakGoalBased) ({ date -> resolveGoalFor(sdf.format(date)) }) else null
        )

        val activeDays = allFocusKeys.count { k ->
            val d = k.removeSuffix("_focus_total")
            val f = dayFocus[d] ?: 0L
            val b = dayBreak[d] ?: 0L
            if (f > 0L || b > 0L) true
            else d == todayStr && (currentSessionSecs > 0L || currentBreakSecs > 0L)
        }

        val weekdayTotals = LongArray(7)
        val weekdayCounts = IntArray(7)
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            if (dStr == todayStr) continue
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val f = dayFocus[dStr] ?: 0L
            if (f > 0L) {
                val cal = Calendar.getInstance().apply { time = parsed }
                val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
                weekdayTotals[idx] += f
                weekdayCounts[idx]++
            }
        }
        val todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        if (todayFocus > 0L) {
            weekdayTotals[todayIdx] += todayFocus
            weekdayCounts[todayIdx]++
        }
        val bestWeekdayIdx = (0 until 7).maxByOrNull { idx ->
            if (weekdayCounts[idx] > 0) weekdayTotals[idx] / weekdayCounts[idx] else 0L
        } ?: 0
        val bestWeekdaySecs = if (weekdayCounts[bestWeekdayIdx] > 0) weekdayTotals[bestWeekdayIdx] / weekdayCounts[bestWeekdayIdx] else 0L
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestWeekdayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestWeekdayIdx + 1) }.time)

        val weekTotals = HashMap<String, Long>()
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val f = dayFocus[dStr] ?: 0L
            if (f <= 0L) continue
            val cal = Calendar.getInstance().apply { time = parsed }
            val weekKey = sdf.format(WeekHelper.mondayOf(cal).time)
            weekTotals[weekKey] = (weekTotals[weekKey] ?: 0L) + f
        }
        var bestWeekSecs = 0L
        var bestWeekLabel = ""
        val shortDateSdf = SimpleDateFormat("d MMM", Locale.getDefault())
        weekTotals.maxByOrNull { it.value }?.let { best ->
            if (best.value > 0L) {
                val weekStart = try { sdf.parse(best.key) } catch (_: Exception) { null } ?: Date()
                val weekEnd = Calendar.getInstance().apply { time = weekStart; add(Calendar.DAY_OF_YEAR, 6) }.time
                bestWeekSecs = best.value
                bestWeekLabel = "${shortDateSdf.format(weekStart)} \u2013 ${shortDateSdf.format(weekEnd)}"
            }
        }

        fun mondayOffset(): Int = WeekHelper.mondayOffset(Calendar.getInstance())
        fun weekFocus(weekOffset: Int): Long {
            var sum = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset() + i + weekOffset * 7) }
                sum += dayFocus[sdf.format(c.time)] ?: 0L
            }
            return sum
        }
        val thisWeek = weekFocus(0)
        val prevWeek = weekFocus(-1)

        var goalHits = 0
        for (i in 0 until 14) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(c.time)
            if ((dayFocus[dStr] ?: 0L) >= resolveGoalFor(dStr)) goalHits++
        }

        val heatmapData = HashMap<String, Long>()
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            heatmapData[dStr] = sharedPrefs.getLong(k, 0L)
        }
        if (currentSessionSecs > 0L) {
            heatmapData[todayStr] = (heatmapData[todayStr] ?: 0L) + currentSessionSecs
        }

        val showHeatmap = sharedPrefs.getBoolean("show_focus_heatmap", true)
        val showPattern = sharedPrefs.getBoolean("show_focus_pattern", true)

        val timeline = TimelineLogger.load(this)
        val nowMs = System.currentTimeMillis()
        fun buildBlockArray(windowDays: Int): LongArray {
            val arr = LongArray(12)
            val windowStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -windowDays)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            fun addFocusInterval(startMs: Long, endMs: Long) {
                var s = max(startMs, windowStart)
                val e = min(endMs, nowMs)
                while (s < e) {
                    val cal = Calendar.getInstance().apply { timeInMillis = s }
                    val block = cal.get(Calendar.HOUR_OF_DAY) / 2
                    val nextHour = Calendar.getInstance().apply {
                        timeInMillis = s
                        add(Calendar.HOUR_OF_DAY, 1)
                        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val segEnd = min(e, nextHour)
                    arr[block] += (segEnd - s) / 1000L
                    s = segEnd
                }
            }
            for (i in 1 until timeline.size) {
                if (timeline[i - 1].state != "STUDYING" && timeline[i - 1].state != "MANUAL_FOCUS") continue
                addFocusInterval(timeline[i - 1].timestamp, timeline[i].timestamp)
            }
            timeline.lastOrNull()?.let { last ->
                if (last.state == "STUDYING" || last.state == "MANUAL_FOCUS") addFocusInterval(last.timestamp, nowMs)
            }
            val manualFocusToday = sharedPrefs.getLong("${todayStr}_focus_manual", 0L)
            if (manualFocusToday != 0L) {
                if (manualFocusToday > 0L) {
                    addFocusInterval(nowMs - manualFocusToday * 1000L, nowMs)
                } else {
                    val calNow = Calendar.getInstance()
                    val block = calNow.get(Calendar.HOUR_OF_DAY) / 2
                    arr[block] = max(0L, arr[block] + manualFocusToday)
                }
            }
            return arr
        }
        val blockSecs7 = buildBlockArray(7)
        val blockSecs30 = buildBlockArray(30)
        val maxBlock7 = blockSecs7.maxOrNull() ?: 0L
        val maxBlock30 = blockSecs30.maxOrNull() ?: 0L
        val patternTotal7 = blockSecs7.sum()
        val patternTotal30 = blockSecs30.sum()

        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthAgg = LinkedHashMap<String, MonthBucket>()
        for ((dStr, f) in dayFocus) {
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val b = dayBreak[dStr] ?: 0L
            if (f <= 0L && b <= 0L) continue
            val mKey = monthSdf.format(parsed)
            val existing = monthAgg[mKey]
            monthAgg[mKey] = MonthBucket(mKey, (existing?.focus ?: 0L) + f, (existing?.breakSecs ?: 0L) + b)
        }
        val monthBuckets = monthAgg.values.toList().sortedBy { runCatching { monthSdf.parse(it.label)?.time }.getOrNull() ?: 0L }

        val allHistKeys = allFocusKeys.sortedDescending()
        val entriesByDay = timeline.groupBy { sdf.format(Date(it.timestamp)) }

        val hasAnySessions = totalLifeFocus > 0L || totalLifeBreak > 0L || currentSessionSecs > 0L || currentBreakSecs > 0L

        return StatsSnapshot(
            todayFocus, todayBreak, streak, avg7, yesterdaySecs, heroGoalSecs,
            totalLifeFocus, totalLifeBreak, totalLife, longestStreak, activeDays,
            bestWeekdayName, bestWeekdaySecs, bestWeekLabel, bestWeekSecs,
            thisWeek, prevWeek, goalHits, hasAnySessions,
            showHeatmap, showPattern, heatmapData, blockSecs7, maxBlock7, blockSecs30, maxBlock30,
            patternTotal7, patternTotal30,
            dayFocus, monthBuckets, allHistKeys, entriesByDay
        )
    }

    private fun buildStatsPanel(target: android.view.ViewGroup = panelContainer) {
        val renderTab = currentStatsTab
        val statsRoot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val cached = statsSnapshotCache
        if (cached != null && (statsInternalRefresh || !statsDirty)) {
            renderStatsContent(statsRoot, cached, renderTab)
            target.addView(statsRoot)
            return
        }

        val loading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        loading.addView(ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply { isIndeterminate = true })
        loading.addView(TextView(this).apply {
            text = "Crunching numbers\u2026"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(14), 0, 0)
        })
        statsRoot.addView(loading, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        target.addView(statsRoot)

        Thread {
            val snap = computeStatsSnapshot()
            handler.post {
                if (isDestroyed || isFinishing) return@post
                statsSnapshotCache = snap
                statsSnapshotGen++
                statsDirty = false
                statsRoot.removeView(loading)
                renderStatsContent(statsRoot, snap, renderTab)
            }
        }.start()
    }

    private fun buildHeatmapData(): Map<String, Long> {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val data = HashMap<String, Long>()
        for (k in sharedPrefs.all.keys) {
            if (k.endsWith("_focus_total")) {
                data[k.removeSuffix("_focus_total")] = sharedPrefs.getLong(k, 0L)
            }
        }
        val currentSessionSecs = sharedPrefs.getLong("accumulatedStudy", 0L)
        if (currentSessionSecs > 0L) {
            data[todayStr] = (data[todayStr] ?: 0L) + currentSessionSecs
        }
        return data
    }

    private fun buildHeatmapFullscreenPanel() {
        val heatmapData = buildHeatmapData()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, dp(10)) }
        }
        val headerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerCol.addView(TextView(this).apply {
            text = "FOCUS HEATMAP \u00B7 FULL SCREEN"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerCol.addView(TextView(this).apply {
            text = "Last 6 months \u00B7 swipe to scroll \u00B7 tap a day to inspect"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(0, dp(3), 0, 0)
        })
        headerRow.addView(headerCol)
        headerRow.addView(TextView(this).apply {
            text = "\u2715 Done"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
        })
        root.addView(headerRow)

        val heatmapScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        heatmapScroll.addView(HeatmapView(this).apply {
            forcedCellSize = dp(32).toFloat()
            setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
            onDayTap = { dateStr ->
                val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                this@MainActivity.showDayDialog(dateStr, lbl)
            }
        })
        heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
        root.addView(heatmapScroll)

        panelContainer.addView(root)
    }

    private fun renderStatsContent(statsRoot: FrameLayout, snap: StatsSnapshot, tab: AppStatsTab = currentStatsTab) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f); isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(96)) }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = "Insights"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 22f
            setPadding(dp(6), 0, dp(6), 0)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayQuote = dailyQuotes[dayOfYear % dailyQuotes.size]
        val quoteCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
        }
        quoteCard.addView(TextView(this).apply {
            text = "Thought of the Day"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.5f
            textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.15f
        })
        quoteCard.addView(TextView(this).apply {
            text = todayQuote
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(quoteCard)

        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }

        fun tabBtn(title: String, tab: AppStatsTab): TextView {
            return TextView(this).apply {
                text = title
                textSize = 14f
                setPadding(dp(18), dp(10), dp(18), dp(10))
                typeface = Typeface.create("sans-serif-medium", if (currentStatsTab == tab) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (currentStatsTab == tab) themeCoordinator.primaryColor else themeCoordinator.textColor)
                background = if (currentStatsTab == tab) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f) else null
                setOnClickListener {
                    currentStatsTab = tab
                    if (tab == AppStatsTab.TIMELINE) currentTimelineLimit = 15
                    navigateToPanel(AppPanel.STATS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4), 0, dp(4), 0) }
            }
        }
        tabContainer.addView(tabBtn("Overview", AppStatsTab.OVERVIEW))
        tabContainer.addView(tabBtn("History", AppStatsTab.TIMELINE))
        content.addView(tabContainer)

        val todayFocus = snap.todayFocus
        val todayBreak = snap.todayBreak
        val todayH = todayFocus / 3600
        val todayM = (todayFocus % 3600) / 60
        val todayBH = todayBreak / 3600
        val todayBM = (todayBreak % 3600) / 60
        val streak = snap.streak

        val avg7 = snap.avg7
        val avgH = avg7 / 3600
        val avgM = (avg7 % 3600) / 60

        val chartCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val heroCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, dp(8)) }
        }

        val heroTopRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heroTopRow.addView(TextView(this@MainActivity).apply {
            text = "TODAY"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.2f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        heroTopRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        val streakChip = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { cornerRadius = 14f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
        streakChip.addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_flame)
            setColorFilter(themeCoordinator.primaryColor)
            contentDescription = "Day streak"
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
        })
        streakChip.addView(TextView(this@MainActivity).apply {
            text = "${streak}d streak"
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(5), 0, 0, 0)
        })
        heroTopRow.addView(streakChip)
        heroCard.addView(heroTopRow)

        val heroMainRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, 0) }
        heroMainRow.addView(TextView(this@MainActivity).apply {
            text = "${todayH}h ${todayM}m"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 40f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        heroMainRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 0) })
        val trendChip = TextView(this@MainActivity).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(9), dp(5), dp(9), dp(5))
        }
        val yesterdaySecs = snap.yesterdaySecs
        val trendPct = if (yesterdaySecs > 0L) {
            (((todayFocus - yesterdaySecs).toFloat() / yesterdaySecs.toFloat()) * 100f).toInt()
        } else if (todayFocus > 0L) {
            100
        } else {
            0
        }
        when {
            yesterdaySecs == 0L && todayFocus == 0L -> {
                trendChip.text = "no sessions yet"
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.5f
            }
            trendPct > 0 -> {
                trendChip.text = "\u2191 $trendPct% vs yesterday"
                trendChip.setTextColor(themeCoordinator.primaryColor)
                trendChip.background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            }
            trendPct < 0 -> {
                trendChip.text = "${-trendPct}% \u2193 vs yesterday"
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.6f
            }
            else -> {
                trendChip.text = "same as yesterday"
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.6f
            }
        }
        heroMainRow.addView(trendChip)
        heroMainRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        heroCard.addView(heroMainRow)

        val heroGoalSecs = snap.heroGoalSecs
        val heroGoalPctRaw = todayFocus.toFloat() / heroGoalSecs.toFloat() * 100f
        val heroGoalPct = heroGoalPctRaw.coerceIn(0f, 100f)
        val goalReached = todayFocus >= heroGoalSecs
        val goalRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(12), 0, 0) }
        val goalTextCol = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        goalTextCol.addView(TextView(this@MainActivity).apply {
            text = "${formatGoalLabel(heroGoalSecs)} daily goal"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        goalTextCol.addView(TextView(this@MainActivity).apply {
            text = if (goalReached) "\u2713 Goal reached!" else {
                val remaining = heroGoalSecs - todayFocus
                "${formatGoalLabel(remaining)} to go"
            }
            setTextColor(if (goalReached) 0xFF4CAF50.toInt() else themeCoordinator.textColor)
            alpha = if (goalReached) 1f else 0.5f
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        })
        goalRow.addView(goalTextCol)

        val goalRingColor = if (goalReached) 0xFF4CAF50.toInt() else themeCoordinator.primaryColor
        val goalRingWrap = FrameLayout(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
        }
        goalRingWrap.addView(SegmentRing(
            listOf(heroGoalPct / 100f to goalRingColor),
            themeCoordinator.bgColor,
            dp(6),
            Pair(goalRingColor, lightenColor(goalRingColor, 0.35f))
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        goalRingWrap.addView(TextView(this@MainActivity).apply {
            text = if (goalReached) "\u2713" else "${heroGoalPctRaw.toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(goalRingColor)
            textSize = if (goalReached) 20f else 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        goalRow.addView(goalRingWrap)
        heroCard.addView(goalRow)

        val breakRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        breakRow.addView(TextView(this@MainActivity).apply {
            text = "Break ${todayBH}h ${todayBM}m"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(tintedColor(themeCoordinator.secondaryColor, 22)) }
            setPadding(dp(12), dp(7), dp(12), dp(7))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        breakRow.addView(TextView(this@MainActivity).apply {
            text = "7d avg ${avgH}h ${avgM}m"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.55f
            textSize = 12f
            setPadding(dp(10), 0, 0, 0)
        })
        heroCard.addView(breakRow)

        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val filterLabels = listOf("7 Days", "30 Days", "All")
        val filterValues = listOf(7, 30, -1)
        for (idx in filterLabels.indices) {
            val isActive = selectedDaysFilter == filterValues[idx]
            chipRow.addView(TextView(this).apply {
                text = filterLabels[idx]
                textSize = 13f
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setTextColor(if (isActive) themeCoordinator.primaryColor else themeCoordinator.textColor)
                alpha = if (isActive) 1f else 0.5f
                typeface = Typeface.create("sans-serif-medium", if (isActive) Typeface.BOLD else Typeface.NORMAL)
                background = if (isActive) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f) else null
                setOnClickListener {
                    selectedDaysFilter = filterValues[idx]
                    sharedPrefs.edit().putInt("selected_days_filter", selectedDaysFilter).apply()
                    navigateToPanel(AppPanel.STATS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(8), 0) }
            })
        }
        chartCard.addView(chipRow)

        chartCard.addView(TextView(this).apply {
            text = "| ${formatGoalLabel(dailyGoalSecs())} goal mark    \u25CB today"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(dp(4), 0, dp(4), dp(6))
        })

        val chartContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        chartCard.addView(chartContainer)

        fun buildChart(daysLimit: Int) {
            chartContainer.removeAllViews()
            var maxDayFocusFound = 1L

            if (daysLimit == 7) {
                val displaySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val cal = Calendar.getInstance()
                val mondayOffset = WeekHelper.mondayOffset(cal)
                val weekData = ArrayList<Pair<String, String>>()

                for (i in 0..6) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -mondayOffset + i)
                    val dStr = sdf.format(c.time)
                    weekData.add(Pair(dStr, "${weekdayNames[i]} ${displaySdf.format(c.time)}"))
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L
                val goalRatio = (dailyGoalSecs().toFloat() / scale.toFloat()).coerceIn(0f, 1f)

                for ((dStr, label) in weekData) {
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(this).apply {
                        text = label
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = goalRatio,
                        trackColor = themeCoordinator.bgColor,
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = lightenColor(themeCoordinator.primaryColor, 0.3f),
                        isToday = isToday,
                        barHeight = dp(12)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(12), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(this).apply {
                        text = "${f / 3600}h ${(f % 3600) / 60}m"
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, label) }
                    chartContainer.addView(row)
                }
            } else if (daysLimit == 30) {
                val labelSdf = SimpleDateFormat("d MMM", Locale.getDefault())
                for (i in 0 until 30) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L
                val goalRatio = (dailyGoalSecs().toFloat() / scale.toFloat()).coerceIn(0f, 1f)

                for (i in 29 downTo 0) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(2)) }
                    row.addView(TextView(this).apply {
                        text = labelSdf.format(c.time)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(55), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = goalRatio,
                        trackColor = themeCoordinator.bgColor,
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = lightenColor(themeCoordinator.primaryColor, 0.3f),
                        isToday = isToday,
                        barHeight = dp(9)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(9), 1f).apply { setMargins(dp(4), 0, dp(6), 0) } })
                    row.addView(TextView(this).apply {
                        text = "${f / 3600}h ${(f % 3600) / 60}m"
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, labelSdf.format(c.time)) }
                    chartContainer.addView(row)
                }
            } else {
                var maxM = 1L
                for (mb in snap.monthBuckets) if (mb.focus > maxM) maxM = mb.focus
                val scale = if (maxM <= 0L) 3600L else Math.ceil(maxM.toDouble() / 3600.0).toLong() * 3600L

                for (mb in snap.monthBuckets) {
                    val mSecs = mb.focus
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(this).apply { text = mb.label; setTextColor(themeCoordinator.textColor); textSize = 13f; layoutParams = LinearLayout.LayoutParams(dp(75), LinearLayout.LayoutParams.WRAP_CONTENT) })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) mSecs.toFloat() / scale.toFloat() else 0f,
                        goalRatio = -1f,
                        trackColor = themeCoordinator.bgColor,
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = lightenColor(themeCoordinator.primaryColor, 0.3f),
                        isToday = false,
                        barHeight = dp(10)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(10), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(this).apply { text = "${mSecs / 3600}h ${(mSecs % 3600) / 60}m"; setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.MONOSPACE; alpha = 0.85f })
                    row.setOnClickListener { showMonthDialog(mb.label, mSecs, mb.breakSecs) }
                    chartContainer.addView(row)
                }
            }
        }

        val totalLifeFocus = snap.totalLifeFocus
        val totalLifeBreak = snap.totalLifeBreak
        val totalLife = snap.totalLife

        val lifetimeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val lifetimeTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lifetimeLeftCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val lifetimeFocusLabel = TextView(this).apply { text = "Lifetime focus"; setTextColor(themeCoordinator.primaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeFocusValue = TextView(this).apply { setTextColor(themeCoordinator.primaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, dp(10)) }
        val lifetimeBreakLabel = TextView(this).apply { text = "Lifetime breaks"; setTextColor(themeCoordinator.secondaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeBreakValue = TextView(this).apply { setTextColor(themeCoordinator.secondaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, 0) }
        lifetimeFocusValue.text = "${totalLifeFocus / 3600}h ${(totalLifeFocus % 3600) / 60}m"
        lifetimeBreakValue.text = "${totalLifeBreak / 3600}h ${(totalLifeBreak % 3600) / 60}m"
        lifetimeLeftCol.addView(lifetimeFocusLabel)
        lifetimeLeftCol.addView(lifetimeFocusValue)
        lifetimeLeftCol.addView(lifetimeBreakLabel)
        lifetimeLeftCol.addView(lifetimeBreakValue)
        lifetimeTopRow.addView(lifetimeLeftCol)

        val focusFrac = if (totalLife > 0L) totalLifeFocus.toFloat() / totalLife.toFloat() else 0f
        val breakFrac = if (totalLife > 0L) totalLifeBreak.toFloat() / totalLife.toFloat() else 0f
        val lifetimeDonutWrap = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }
        lifetimeDonutWrap.addView(SegmentRing(
            listOf(focusFrac to themeCoordinator.primaryColor, breakFrac to themeCoordinator.secondaryColor),
            themeCoordinator.bgColor,
            dp(10),
            null
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        lifetimeDonutWrap.addView(TextView(this).apply {
            text = "${(focusFrac * 100f).toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        lifetimeTopRow.addView(lifetimeDonutWrap)
        lifetimeCard.addView(lifetimeTopRow)

        val timelineContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())

        val longestStreak = snap.longestStreak
        val activeDays = snap.activeDays
        val bestWeekdaySecs = snap.bestWeekdaySecs
        val bestWeekdayName = snap.bestWeekdayName
        val bestWeekSecs = snap.bestWeekSecs
        val bestWeekLabel = snap.bestWeekLabel

        val thisWeek = snap.thisWeek
        val prevWeek = snap.prevWeek

        val goalHits = snap.goalHits

        val insightsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var insightCount = 0

        if (thisWeek > 0L || prevWeek > 0L) {
            val diff = if (prevWeek > 0L) ((thisWeek - prevWeek).toFloat() / prevWeek.toFloat() * 100f).toInt() else 100
            val statement = when {
                diff > 0 -> "$diff% more focus than last week"
                diff < 0 -> "${-diff}% less focus than last week"
                else -> "same pace as last week"
            }
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_trending, themeCoordinator.primaryColor,
                "TREND", statement, "This week \u00B7 ${thisWeek / 3600}h ${(thisWeek % 3600) / 60}m"
            ))
            insightCount++
        }

        if (longestStreak > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_flame, themeCoordinator.primaryColor,
                "LONGEST STREAK",
                "$longestStreak-day streak",
                "Consecutive daily goal+ focus days"
            ))
            insightCount++
        }

        if (activeDays > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_book, themeCoordinator.secondaryColor,
                "TOTAL SESSIONS",
                "$activeDays active day${if (activeDays == 1) "" else "s"}",
                "Days with logged focus time"
            ))
            insightCount++
        }

        if (bestWeekdaySecs > 0L) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_trending, themeCoordinator.primaryColor,
                "BEST WEEKDAY",
                bestWeekdayName,
                "${bestWeekdaySecs / 3600}h ${(bestWeekdaySecs % 3600) / 60}m avg on this day"
            ))
            insightCount++
        }

        if (bestWeekSecs > 0L) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_medal, themeCoordinator.secondaryColor,
                "BEST WEEK",
                "Week of $bestWeekLabel",
                "${bestWeekSecs / 3600}h ${(bestWeekSecs % 3600) / 60}m of focus"
            ))
            insightCount++
        }

        if (goalHits > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_target, themeCoordinator.secondaryColor,
                "CONSISTENCY",
                "Hit daily goal on $goalHits of the last 14 days",
                ""
            ))
            insightCount++
        }

        if (tab == AppStatsTab.OVERVIEW) {
            val hasAnySessions = snap.hasAnySessions
            if (!hasAnySessions) {
                content.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(28), dp(20), dp(28))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
                    addView(TextView(this@MainActivity).apply {
                        text = "\uD83C\uDFA7\uFE0F"
                        textSize = 36f
                        gravity = Gravity.CENTER
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = "No sessions yet"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 16f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(10), 0, 0)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = "Start your first focus block to see insights here."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                })
            } else {
            content.addView(heroCard)
            content.addView(chartCard)
            buildChart(selectedDaysFilter)

            val showHeatmap = snap.showHeatmap
            val showPattern = snap.showPattern

            if (showHeatmap) {
                val heatmapData = snap.heatmapData

                val heatmapCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val heatmapHeaderRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val heatmapHeaderCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                heatmapHeaderCol.addView(TextView(this).apply {
                    text = "FOCUS HEATMAP"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                heatmapHeaderCol.addView(TextView(this).apply {
                    text = "Last 6 months \u00B7 swipe to scroll \u00B7 tap a day to inspect"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    setPadding(0, dp(2), 0, 0)
                })
                heatmapHeaderRow.addView(heatmapHeaderCol)
                heatmapHeaderRow.addView(TextView(this).apply {
                    text = "\u26F6 Fullscreen"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18).toFloat()
                        setStroke(dp(1).toInt(), tintedColor(themeCoordinator.primaryColor, 180))
                        setColor(tintedColor(themeCoordinator.primaryColor, 25))
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener { navigateToPanel(AppPanel.HEATMAP) }
                })
                heatmapCard.addView(heatmapHeaderRow)
                val heatmapScroll = HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isFillViewport = true
                }
                heatmapScroll.addView(HeatmapView(this).apply {
                    setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
                    onDayTap = { dateStr ->
                        val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                        val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                        this@MainActivity.showDayDialog(dateStr, lbl)
                    }
                })
                heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
                heatmapCard.addView(heatmapScroll)

                val legendRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, 0)
                }
                legendRow.addView(TextView(this).apply { text = "Less"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 10f })
                fun addLegendLevel(alpha: Int) {
                    legendRow.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { setMargins(dp(4), 0, dp(4), 0) }
                        background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.argb(alpha, Color.red(themeCoordinator.primaryColor), Color.green(themeCoordinator.primaryColor), Color.blue(themeCoordinator.primaryColor))) }
                    })
                }
                addLegendLevel(30); addLegendLevel(60); addLegendLevel(110); addLegendLevel(170)
                legendRow.addView(TextView(this).apply { text = "More"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 10f })
                heatmapCard.addView(legendRow)

                content.addView(heatmapCard)
            }

            if (showPattern) {
            var using7 = true

            val patternCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
            }
            val patternHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            patternHeader.addView(TextView(this).apply {
                text = "FOCUS PATTERN"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val segWrap = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(tintedColor(themeCoordinator.textColor, 16)) }
                setPadding(dp(3), dp(2), dp(3), dp(2))
            }
            val seg7 = TextView(this).apply {
                text = "7d"
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            val seg30 = TextView(this).apply {
                text = "30d"
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            fun styleSeg(btn: TextView, selected: Boolean) {
                btn.setTextColor(if (selected) themeCoordinator.bgColor else themeCoordinator.textColor)
                btn.alpha = if (selected) 1f else 0.6f
                btn.background = if (selected) GradientDrawable().apply { cornerRadius = dp(11).toFloat(); setColor(themeCoordinator.primaryColor) } else null
            }
            segWrap.addView(seg7); segWrap.addView(seg30)
            patternHeader.addView(segWrap)
            patternCard.addView(patternHeader)

            val blockLabels = focusBlockLabels()
            val startLabels = focusBlockStartLabels()
            val cellsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val cellsScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            cellsScroll.addView(cellsRow)
            patternCard.addView(cellsScroll)

            val footerText = TextView(this).apply {
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            }
            patternCard.addView(footerText)

            fun showBlockDialog(b: Int, secs: Long, total: Long, winLabel: String) {
                val pct = if (total > 0L) (secs * 100.0 / total) else 0.0
                val dialog = android.app.Dialog(this)
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val dialogContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(28f)
                    setPadding(dp(20), dp(20), dp(20), dp(18))
                }
                dialogContent.addView(TextView(this).apply {
                    text = focusBlockRangeLabel(b)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                dialogContent.addView(TextView(this).apply {
                    text = "${formatDuration(secs)} of focus"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(8), 0, 0)
                })
                dialogContent.addView(TextView(this).apply {
                    text = "${String.format(java.util.Locale.getDefault(), "%.0f%%", pct)} of ${winLabel} total \u00B7 2-hour block"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 12f
                    setPadding(0, dp(4), 0, 0)
                })
                dialogContent.addView(TextView(this).apply {
                    text = "CLOSE"
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
                    setOnClickListener { dialog.dismiss() }
                })
                dialog.setContentView(dialogContent)
                dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.show()
            }

            fun rebuildCells() {
                val blockSecs = if (using7) snap.blockSecs7 else snap.blockSecs30
                val maxBlock = if (using7) snap.maxBlock7 else snap.maxBlock30
                val total = if (using7) snap.patternTotal7 else snap.patternTotal30
                val winLabel = if (using7) "7-day" else "30-day"
                cellsRow.removeAllViews()
                for (b in 0 until 12) {
                    val v = blockSecs[b]
                    val intensity = if (maxBlock > 0L) v.toFloat() / maxBlock.toFloat() else 0f
                    val col = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(8), 0, 0)
                    }
                    val tile = FrameLayout(this).apply {
                        background = if (v > 0L) GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setColor(tintedColor(themeCoordinator.primaryColor, 40 + (185 * intensity).toInt()))
                        } else GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setColor(tintedColor(themeCoordinator.textColor, 16))
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                        setOnClickListener {
                            showBlockDialog(b, v, total, winLabel)
                        }
                    }
                    col.addView(tile)
                    col.addView(TextView(this).apply {
                        text = startLabels[b]
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.45f
                        textSize = 8f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                        setPadding(0, dp(4), 0, 0)
                    })
                    col.addView(TextView(this).apply {
                        text = if (v > 0L && total > 0L) "${(v * 100.0 / total).toInt()}%" else ""
                        setTextColor(themeCoordinator.primaryColor)
                        alpha = if (v > 0L) 0.85f else 0.3f
                        textSize = 9f
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    col.setOnClickListener {
                        showBlockDialog(b, v, total, winLabel)
                    }
                    cellsRow.addView(col)
                }
                if (total > 0L) {
                    var bestBlock = 0
                    for (b in 1 until 12) if (blockSecs[b] > blockSecs[bestBlock]) bestBlock = b
                    val bestLabel = blockLabels[bestBlock].replace("-", " \u2013 ")
                    footerText.text = "Most focused ${bestLabel} \u00B7 ${formatDuration(total)} total"
                    footerText.setAlpha(0.6f)
                    cellsScroll.post {
                        val colW = dp(42)
                        val target = (bestBlock * colW - (cellsScroll.width - colW) / 2).coerceAtLeast(0)
                        cellsScroll.smoothScrollTo(target, 0)
                    }
                } else {
                    footerText.text = "No focus logged in the last ${if (using7) "7" else "30"} days"
                    footerText.setAlpha(0.5f)
                }
            }

            styleSeg(seg7, true); styleSeg(seg30, false)
            seg7.setOnClickListener {
                using7 = true
                styleSeg(seg7, true); styleSeg(seg30, false)
                rebuildCells()
            }
            seg30.setOnClickListener {
                using7 = false
                styleSeg(seg7, false); styleSeg(seg30, true)
                rebuildCells()
            }
            rebuildCells()
            content.addView(patternCard)
            }

            content.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)) })
            if (insightCount > 0) {
                content.addView(createSectionLabel("Highlights"))
                content.addView(insightsContainer)
            }
            content.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)) })
            content.addView(lifetimeCard)

            val exportCardBtn = TextView(this).apply {
                text = "\uD83D\uDCF1 Export Summary Card"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(12)) }
                setOnClickListener {
                    showSummaryCardPreview()
                }
            }
            content.addView(exportCardBtn)
            }
        } else {
            val fullDateSdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            val allHistKeys = snap.allHistKeys
            val entriesByDay = snap.entriesByDay
            val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
            var generatedCount = 0

            for (keyStr in allHistKeys) {
                if (generatedCount >= currentTimelineLimit) break
                val dateStr = keyStr.removeSuffix("_focus_total")
                val fSecs = sharedPrefs.getLong(keyStr, 0L)
                val bSecs = sharedPrefs.getLong("${dateStr}_break_total", 0L)
                if (fSecs <= 0L && bSecs <= 0L) continue
                generatedCount++

                val streakThreshold = if (streakGoalBased) resolveGoalFor(dateStr) else StreakCalculator.QUALIFYING_SECS
                val isQualified = fSecs >= streakThreshold

                var parsedDate = Date()
                try { parsedDate = sdf.parse(dateStr) ?: Date() } catch (_: Exception) {}

                val card = LinearLayout(this)
                card.orientation = LinearLayout.VERTICAL
                card.background = themeCoordinator.createCardBackground()
                card.setPadding(dp(14), dp(12), dp(14), dp(12))
                card.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) }
                card.tag = dateStr

                val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                val headerTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                headerTextCol.addView(TextView(this).apply {
                    text = fullDateSdf.format(parsedDate)
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                })
                headerTextCol.addView(TextView(this).apply {
                    text = dayNameSdf.format(parsedDate)
                    setTextColor(themeCoordinator.textColor)
                    textSize = 11f
                    letterSpacing = 0.16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(0, dp(2), 0, 0)
                })
                headerRow.addView(headerTextCol)
                val chevron = TextView(this).apply {
                    text = "\u25BE"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.4f
                    textSize = 14f
                    setPadding(dp(8), 0, 0, 0)
                }
                headerRow.addView(chevron)
                card.addView(headerRow)

                val detailsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, 0) }
                detailsRow.addView(TextView(this).apply { text = "Focus ${fSecs / 3600}h ${(fSecs % 3600) / 60}m"; setTextColor(themeCoordinator.primaryColor); textSize = 13f; typeface = Typeface.MONOSPACE })
                detailsRow.addView(TextView(this).apply { text = "  |  "; setTextColor(themeCoordinator.textColor); alpha = 0.3f; textSize = 13f })
                detailsRow.addView(TextView(this).apply { text = "Break ${bSecs / 3600}h ${(bSecs % 3600) / 60}m"; setTextColor(themeCoordinator.secondaryColor); textSize = 13f; typeface = Typeface.MONOSPACE })
                detailsRow.addView(TextView(this).apply { text = "  "; layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
                detailsRow.addView(TextView(this).apply {
                    text = if (isQualified) "🔥 Streak" else "< ${formatDuration(streakThreshold)}"
                    setTextColor(if (isQualified) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    alpha = if (isQualified) 1f else 0.4f
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                card.addView(detailsRow)

                val dayEntries = entriesByDay[dateStr]
                if (dayEntries != null && dayEntries.isNotEmpty()) {
                    val (allSessions, allBreaks) = dayBlocks(dateStr)
                    val sessions = allSessions.filter { it.secs >= 60L }
                    val breaksList = allBreaks.filter { it.secs >= 60L }
                    val longest = sessions.maxOfOrNull { it.secs } ?: 0L
                    card.addView(TextView(this).apply {
                        text = "${sessions.size} focus blocks \u00B7 ${breaksList.size} breaks \u00B7 longest ${longest / 3600}h ${(longest % 3600) / 60}m"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 11f
                        setPadding(0, dp(4), 0, 0)
                    })
                }

                val detailContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(6), 0, 0) }
                var expanded = false
                card.setOnClickListener {
                    expanded = !expanded
                    chevron.text = if (expanded) "\u25B4" else "\u25BE"
                    if (expanded) {
                        detailContainer.removeAllViews()
                        val (allSessions, allBreaks) = dayBlocks(dateStr)
                        val sessions = allSessions.filter { it.secs >= 60L }
                        val breaksList = allBreaks.filter { it.secs >= 60L }
                        if (sessions.isEmpty() && breaksList.isEmpty()) {
                            detailContainer.addView(TextView(this).apply {
                                text = "No session log for this day"
                                setTextColor(themeCoordinator.textColor)
                                alpha = 0.45f
                                textSize = 12f
                            })
                        } else {
                            fillBlockRows(
                                detailContainer, sessions, breaksList,
                                onDelete = { block, blockIsBreak ->
                                    val kind = if (blockIsBreak) "break" else "focus"
                                    showConfirmDialog(
                                        "Delete this ${kind} block?",
                                        "Removes ${formatBlockRow(block.startMs, block.endMs, block.secs)} from this day's log."
                                    ) {
                                        val timelineTs = TimelineLogger.load(this@MainActivity).map { it.timestamp }.toHashSet()
                                        val startTs = block.startMs
                                        val endBoundary = if (timelineTs.contains(block.endMs)) block.endMs else timelineTs.filter { it > startTs }.minOrNull()
                                        TimelineLogger.deleteEntry(this@MainActivity, startTs)
                                        if (endBoundary != null) TimelineLogger.deleteEntry(this@MainActivity, endBoundary)
                                        reconcileDayTotals(dateStr)
                                        Toast.makeText(this@MainActivity, "${kind} block deleted", Toast.LENGTH_SHORT).show()
                                        statsDirty = true
                                        recalculateStreak()
                                        navigateToPanel(AppPanel.STATS)
                                    }
                                }
                            )
                        }
                        card.addView(detailContainer)
                    } else {
                        card.removeView(detailContainer)
                    }
                }

                if (dateStr != todayStr) {
                    card.setOnLongClickListener {
                        showConfirmDialog(
                            "Delete ${fullDateSdf.format(parsedDate)}?",
                            "Removes the day's focus/break totals and its session timeline."
                        ) {
                            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
                                remove("${dateStr}_focus_total")
                                remove("${dateStr}_break_total")
                                remove("${dateStr}_focus_manual")
                                remove("${dateStr}_break_manual")
                            }.apply()
                            TimelineLogger.deleteDay(this@MainActivity, dateStr)
                            Toast.makeText(this@MainActivity, "Day deleted", Toast.LENGTH_SHORT).show()
                            recalculateStreak()
                            navigateToPanel(AppPanel.STATS)
                        }
                        true
                    }
                }

                timelineContainer.addView(card)
            }

            if (allHistKeys.isEmpty()) {
                timelineContainer.addView(LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(40), 0, dp(40))
                    addView(TextView(this@MainActivity).apply { text = "No sessions logged yet"; setTextColor(themeCoordinator.textColor); alpha = 0.4f; textSize = 15f })
                })
            }

            content.addView(timelineContainer)

            if (allHistKeys.size > currentTimelineLimit) {
                content.addView(TextView(this).apply {
                    text = "Load more"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f)
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(16)) }
                    setOnClickListener {
                        var count = 0
                        for (k in allHistKeys) {
                            val d = k.removeSuffix("_focus_total")
                            val f = sharedPrefs.getLong(k, 0L)
                            val b = sharedPrefs.getLong("${d}_break_total", 0L)
                            if (f <= 0L && b <= 0L) continue
                            count++
                            if (count == currentTimelineLimit) { pendingAnchorDate = d; break }
                        }
                        currentTimelineLimit += 15
                        navigateToPanel(AppPanel.STATS)
                    }
                })
            }
        }

        if (pendingAnchorDate != null) {
            val anchor = pendingAnchorDate
            pendingAnchorDate = null
            scroll.post {
                val target = timelineContainer.findViewWithTag<View>(anchor)
                if (target != null) {
                    scroll.scrollTo(0, target.top)
                }
            }
        }

        layout.addView(scroll)

        statsRoot.addView(layout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val backFab = TextView(this).apply {
            text = "\u2190"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(themeCoordinator.primaryColor) }
            elevation = dp(8).toFloat()
            setOnClickListener { navigateToPanel(AppPanel.FOCUS) }
            layoutParams = FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(20), dp(20)) }
        }
        statsRoot.addView(backFab)
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }
        content.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(18), 0, 0) }
        }
        buttonRow.addView(Button(this).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener { dialog.dismiss() }
        })
        buttonRow.addView(Button(this).apply {
            text = "Delete"
            setTextColor(0xFFFFF7ED.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(0xFFEF4444.toInt()) }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
            setOnClickListener { dialog.dismiss(); onConfirm() }
        })
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun createSectionLabel(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(16), 0, dp(8))
        }
    }

    private fun createSettingsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(4)) }
        }
    }

    private fun createInsightCard(iconRes: Int, color: Int, title: String, statement: String, subtext: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }

            val iconBox = FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(tintedColor(color, 30)) }
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            }
            iconBox.addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(color)
                contentDescription = title
                layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
            })
            addView(iconBox)

            val textCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, 0, 0) }
            }
            textCol.addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(color)
                textSize = 10f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = statement
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(3), 0, 0)
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = subtext
                setTextColor(themeCoordinator.textColor)
                textSize = 11f
                alpha = 0.55f
                setPadding(0, dp(2), 0, 0)
                visibility = if (subtext.isEmpty()) View.GONE else View.VISIBLE
            })
            addView(textCol)
        }
    }

    private inner class BarTrackView(
        private val ratio: Float,
        private val goalRatio: Float,
        private val trackColor: Int,
        private val fillStart: Int,
        private val fillEnd: Int,
        private val isToday: Boolean,
        private val barHeight: Int
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f
        private val maxFillRatio = ratio.coerceIn(0f, 1f)

        init {
            tickPaint.style = Paint.Style.FILL
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = 700
            anim.interpolator = android.view.animation.DecelerateInterpolator()
            anim.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            anim.start()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = h / 2f

            paint.style = Paint.Style.FILL
            paint.color = trackColor
            canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)

            val fillW = w * maxFillRatio * progress
            if (fillW > 0f) {
                fillPaint.shader = LinearGradient(0f, 0f, w, 0f, fillStart, fillEnd, Shader.TileMode.CLAMP)
                if (fillW >= radius * 2f) {
                    canvas.drawRoundRect(RectF(0f, 0f, fillW, h), radius, radius, fillPaint)
                } else {
                    canvas.save()
                    canvas.clipRect(0f, 0f, fillW, h)
                    canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, fillPaint)
                    canvas.restore()
                }
            }

            if (goalRatio > 0f) {
                val gx = w * goalRatio
                tickPaint.color = Color.argb(120, 255, 255, 255)
                canvas.drawRect(gx - dp(1).toFloat(), 0f, gx + dp(1).toFloat(), h, tickPaint)
            }

            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2) * 0.75f
                paint.color = Color.argb(180, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
                canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private inner class SegmentRing(
        private val segments: List<Pair<Float, Int>>,
        private val trackColor: Int,
        private val strokeWidth: Int,
        private val gradient: Pair<Int, Int>?
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.strokeCap = if (segments.size <= 1) Paint.Cap.ROUND else Paint.Cap.BUTT
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = 800
            anim.interpolator = android.view.animation.DecelerateInterpolator()
            anim.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            anim.start()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec)
            setMeasuredDimension(size, size)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val r = min(w, h) / 2f - strokeWidth / 2f

            paint.shader = null
            paint.color = trackColor
            canvas.drawCircle(cx, cy, r, paint)

            var startAngle = -90f
            for ((frac, color) in segments) {
                val f = frac.coerceIn(0f, 1f)
                if (f <= 0f) continue
                val sweep = f * 360f * progress
                if (gradient != null && segments.size == 1) {
                    paint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, gradient.first, gradient.second, Shader.TileMode.CLAMP)
                } else {
                    paint.shader = null
                    paint.color = color
                }
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), startAngle, sweep, false, paint)
                startAngle += sweep
                if (segments.size > 1) startAngle += 3f
            }
        }
    }

    private data class BlockInfo(val startMs: Long, val endMs: Long, val secs: Long, val running: Boolean = false, val manual: Boolean = false)

    private fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> {
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val entries = TimelineLogger.load(this)
            .filter { dateSdf.format(Date(it.timestamp)) == dateStr }
            .sortedBy { it.timestamp }
        val sessions = ArrayList<BlockInfo>()
        val breaks = ArrayList<BlockInfo>()
        var fs: Long? = null
        var fsManual = false
        var bs: Long? = null
        var bsManual = false
        for (e in entries) {
            when (e.state) {
                "STUDYING", "MANUAL_FOCUS" -> {
                    if (fs != null) {
                        val gapMs = e.timestamp - fs
                        if (gapMs <= 24L * 3600_000) {
                            sessions.add(BlockInfo(fs, e.timestamp, gapMs / 1000L, manual = fsManual))
                        }
                    }
                    if (bs != null) breaks.add(BlockInfo(bs, e.timestamp, (e.timestamp - bs) / 1000L, manual = bsManual))
                    bs = null
                    bsManual = false
                    fs = e.timestamp
                    fsManual = e.state == "MANUAL_FOCUS"
                }
                "BREAK", "MANUAL_BREAK" -> {
                    if (fs != null) sessions.add(BlockInfo(fs, e.timestamp, (e.timestamp - fs) / 1000L, manual = fsManual))
                    fs = null
                    fsManual = false
                    if (bs == null) {
                        bs = e.timestamp
                        bsManual = e.state == "MANUAL_BREAK"
                    }
                }
                "IDLE" -> {
                    if (fs != null) sessions.add(BlockInfo(fs, e.timestamp, (e.timestamp - fs) / 1000L, manual = fsManual))
                    fs = null
                    fsManual = false
                    if (bs != null) breaks.add(BlockInfo(bs, e.timestamp, (e.timestamp - bs) / 1000L, manual = bsManual))
                    bs = null
                    bsManual = false
                }
            }
        }
        if (fs != null || bs != null) {
            val isToday = dateStr == dateSdf.format(Date())
            val timerState = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                .getString("timerState", "IDLE") ?: "IDLE"
            val timerRunning = timerState == "STUDYING" || timerState == "BREAK"
            val endTs = if (isToday && timerRunning) {
                System.currentTimeMillis()
            } else {
                entries.lastOrNull()?.timestamp ?: 0L
            }
            if (endTs > 0L) {
                if (fs != null) {
                    val gapMs = endTs - fs
                    if (gapMs <= 24L * 3600_000) {
                        sessions.add(BlockInfo(fs, endTs, gapMs / 1000L, isToday && timerRunning, fsManual))
                    }
                }
                if (bs != null) breaks.add(BlockInfo(bs, endTs, (endTs - bs) / 1000L, manual = bsManual))
            }
        }
        val manualPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val focusManual = manualPrefs.getLong("${dateStr}_focus_manual", 0L)
        val breakManual = manualPrefs.getLong("${dateStr}_break_manual", 0L)
        if (focusManual != 0L) {
            if (sessions.isNotEmpty()) {
                val last = sessions.last()
                val adj = max(0L, last.secs + focusManual)
                sessions[sessions.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (focusManual > 0L) {
                val lastEnd = entries.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                sessions.add(BlockInfo(lastEnd, lastEnd + focusManual * 1000L, focusManual, manual = true))
            }
        }
        if (breakManual != 0L) {
            if (breaks.isNotEmpty()) {
                val last = breaks.last()
                val adj = max(0L, last.secs + breakManual)
                breaks[breaks.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (breakManual > 0L) {
                val lastEnd = entries.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                breaks.add(BlockInfo(lastEnd, lastEnd + breakManual * 1000L, breakManual, manual = true))
            }
        }
        return sessions to breaks
    }

    private fun reconcileDayTotals(dateStr: String) {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val timerActive = prefs.getString("timerState", "IDLE") != "IDLE"
        if (timerActive && dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) return
        val (sessions, breaks) = dayBlocks(dateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        val breakSum = breaks.filter { !it.running }.sumOf { it.secs }
        prefs.edit()
            .putLong("${dateStr}_focus_total", focusSum)
            .putLong("${dateStr}_break_total", breakSum)
            .apply()
    }

    private fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val base = try { sdf.parse(dateStr) } catch (_: Exception) { null } ?: Date()
        val c = Calendar.getInstance().apply { time = base }
        c.set(Calendar.HOUR_OF_DAY, h)
        c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) {
        val entries = TimelineLogger.load(this).sortedBy { it.timestamp }
        val startIdx = entries.indexOfFirst { it.timestamp == block.startMs }
        if (startIdx >= 0) {
            val endEntry = entries.getOrNull(startIdx + 1)
            TimelineLogger.moveEntry(this, block.startMs, newStartMs)
            if (endEntry != null) {
                TimelineLogger.moveEntry(this, endEntry.timestamp, newEndMs)
            } else {
                TimelineLogger.recordRaw(this, "IDLE", newEndMs)
            }
        } else {
            val startState = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
            TimelineLogger.recordRaw(this, startState, newStartMs)
            TimelineLogger.recordRaw(this, "IDLE", newEndMs)
        }
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putLong("${dateStr}_focus_manual", 0L)
            .putLong("${dateStr}_break_manual", 0L)
            .apply()
        reconcileDayTotals(dateStr)
        statsDirty = true
        recalculateStreak()
    }

    private fun showBlockEditDialog(dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) {
        val kind = if (isBreak) "break" else "focus"
        val is24 = TimeFormat.is24Hour(this)
        val startCal = Calendar.getInstance().apply { timeInMillis = block.startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = block.endMs }
        android.app.TimePickerDialog(
            this,
            { _, h, m ->
                val newStartMs = msForDateAndTime(dateStr, h, m)
                android.app.TimePickerDialog(
                    this,
                    { _, h2, m2 ->
                        val newEndMs = msForDateAndTime(dateStr, h2, m2)
                        if (newEndMs <= newStartMs) {
                            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                        } else {
                            applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
                            Toast.makeText(this, "$kind block updated", Toast.LENGTH_SHORT).show()
                            onApplied?.invoke() ?: navigateToPanel(AppPanel.STATS)
                        }
                    },
                    endCal.get(Calendar.HOUR_OF_DAY),
                    endCal.get(Calendar.MINUTE),
                    is24
                ).apply { setTitle("Block end") }.show()
            },
            startCal.get(Calendar.HOUR_OF_DAY),
            startCal.get(Calendar.MINUTE),
            is24
        ).apply { setTitle("Block start") }.show()
    }

    private fun showDevTimelineEditor() {
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val todayStr = dateSdf.format(Date())
        var selectedStr = todayStr

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        content.addView(TextView(this).apply {
            text = "TIMELINE EDITOR"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        var render: () -> Unit = {}
        var addBlock: (Boolean) -> Unit = {}

        fun parseSelected(): Calendar {
            val c = Calendar.getInstance()
            c.time = try { dateSdf.parse(selectedStr) ?: Date() } catch (_: Exception) { Date() }
            return c
        }

        val dateLabel = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val c = parseSelected()
                android.app.DatePickerDialog(
                    this@MainActivity,
                    { _, y, m, d ->
                        val nc = Calendar.getInstance()
                        nc.set(y, m, d, 0, 0, 0)
                        nc.set(Calendar.MILLISECOND, 0)
                        selectedStr = dateSdf.format(nc.time)
                        render()
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        fun makeDayNav(label: String, delta: Int): TextView = TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener {
                val c = parseSelected()
                c.add(Calendar.DAY_OF_YEAR, delta)
                selectedStr = dateSdf.format(c.time)
                render()
            }
        }
        val todayBtn = TextView(this).apply {
            text = "TODAY"
            textSize = 10f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                selectedStr = todayStr
                render()
            }
        }
        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(10), 0, 0) }
        }
        dateRow.addView(makeDayNav("\u25C0", -1))
        dateRow.addView(dateLabel)
        dateRow.addView(makeDayNav("\u25B6", 1))
        dateRow.addView(todayBtn)
        content.addView(dateRow)
        content.addView(TextView(this).apply {
            text = "Tap the date to open the calendar (any day/month). \u270E edits a block's start/end."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(0, dp(2), 0, 0)
        })

        val blockList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(320)).apply { setMargins(0, dp(8), 0, 0) }
            addView(blockList)
        }
        content.addView(scroll)

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
        }
        fun makeAddBtn(label: String, isBreak: Boolean): TextView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.primaryColor)
            background = GradientDrawable().apply { cornerRadius = 14f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(3, 0, 3, 0) }
            setOnClickListener { addBlock(isBreak) }
        }
        addRow.addView(makeAddBtn("＋  Add focus block", false))
        addRow.addView(makeAddBtn("＋  Add break block", true))
        content.addView(addRow)

        render = {
            val isToday = selectedStr == todayStr
            val parsed = try { dateSdf.parse(selectedStr) } catch (_: Exception) { null }
            val display = if (parsed != null) displaySdf.format(parsed) else selectedStr
            dateLabel.text = if (isToday) "Today \u00B7 $display" else display
            dateLabel.setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
            blockList.removeAllViews()
            val (sessions, breaks) = dayBlocks(selectedStr)
            val rows = ArrayList<Pair<BlockInfo, Boolean>>()
            for (s in sessions) rows.add(Pair(s, false))
            for (b in breaks) rows.add(Pair(b, true))
            rows.sortBy { it.first.startMs }
            if (rows.isEmpty()) {
                blockList.addView(TextView(this).apply {
                    text = "No blocks for this day"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 12f
                    setPadding(0, dp(6), 0, 0)
                })
            }
            for ((b, isBreak) in rows) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(3), 0, 0)
                }
                val label = when {
                    b.manual -> "Manual  "
                    isBreak -> "Break  "
                    else -> "Focus  "
                }
                row.addView(TextView(this).apply {
                    text = label + formatBlockRow(b.startMs, b.endMs, b.secs)
                    setTextColor(if (isBreak) themeCoordinator.secondaryColor else themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "\u270E"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    alpha = if (b.running) 0.2f else 0.55f
                    setPadding(dp(10), 0, 0, 0)
                    setOnClickListener {
                        if (b.running) {
                            Toast.makeText(this@MainActivity, "Open block \u2014 stop the timer first", Toast.LENGTH_SHORT).show()
                        } else {
                            showBlockEditDialog(selectedStr, b, isBreak) { render() }
                        }
                    }
                })
                blockList.addView(row)
            }
        }
        render()

        addBlock = { isBreak ->
            val kind = if (isBreak) "break" else "focus"
            val is24 = TimeFormat.is24Hour(this)
            val nowCal = Calendar.getInstance()
            android.app.TimePickerDialog(
                this,
                { _, h, m ->
                    val newStartMs = msForDateAndTime(selectedStr, h, m)
                    android.app.TimePickerDialog(
                        this,
                        { _, h2, m2 ->
                            val newEndMs = msForDateAndTime(selectedStr, h2, m2)
                            if (newEndMs <= newStartMs) {
                                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                            } else {
                                val state = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
                                TimelineLogger.recordRaw(this, state, newStartMs)
                                TimelineLogger.recordRaw(this, "IDLE", newEndMs)
                                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                                    .putLong("${selectedStr}_focus_manual", 0L)
                                    .putLong("${selectedStr}_break_manual", 0L)
                                    .apply()
                                reconcileDayTotals(selectedStr)
                                statsDirty = true
                                recalculateStreak()
                                render()
                                Toast.makeText(this, "$kind block added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        nowCal.get(Calendar.HOUR_OF_DAY),
                        nowCal.get(Calendar.MINUTE),
                        is24
                    ).apply { setTitle("Block end") }.show()
                },
                nowCal.get(Calendar.HOUR_OF_DAY),
                nowCal.get(Calendar.MINUTE),
                is24
            ).apply { setTitle("Block start") }.show()
        }

        content.addView(TextView(this).apply {
            text = "CLOSE"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun formatBlockRow(startMs: Long, endMs: Long, secs: Long): String {
        val dur = when {
            secs >= 3600 -> "${secs / 3600}h ${(secs % 3600) / 60}m"
            secs >= 60 -> "${secs / 60}m"
            else -> "${secs}s"
        }
        return "${TimeFormat.formatWallClock(this, startMs)} \u2013 ${TimeFormat.formatWallClock(this, endMs)} \u00B7 $dur"
    }

    private fun formatDuration(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            m > 0 -> "${m}m"
            else -> "0m"
        }
    }

    private fun focusBlockLabels(): Array<String> {
        if (TimeFormat.is24Hour(this)) {
            return Array(12) { i -> "${i * 2}-${i * 2 + 2}" }
        }
        fun short12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "a" else "p"}"
        }
        fun full12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "am" else "pm"}"
        }
        return Array(12) { i -> "${short12(i * 2)}-${full12(i * 2 + 2)}" }
    }

    private fun focusBlockStartLabels(): Array<String> {
        if (TimeFormat.is24Hour(this)) {
            return Array(12) { i -> "${i * 2}" }
        }
        fun short12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "a" else "p"}"
        }
        return Array(12) { i -> short12(i * 2) }
    }

    private fun focusBlockRangeLabel(b: Int): String {
        val s = b * 2
        val e = s + 2
        if (TimeFormat.is24Hour(this)) return "$s:00\u2013$e:00"
        fun h12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "am" else "pm"}"
        }
        return "${h12(s)} \u2013 ${h12(e)}"
    }

    private fun fillBlockRows(container: LinearLayout, sessions: List<BlockInfo>, breaks: List<BlockInfo>, onDelete: ((BlockInfo, Boolean) -> Unit)? = null) {
        val rows = ArrayList<Pair<BlockInfo, Boolean>>()
        for (s in sessions) rows.add(Pair(s, false))
        for (b in breaks) rows.add(Pair(b, true))
        rows.sortBy { it.first.startMs }
        var prevWasBreak = false
        for ((b, isBreak) in rows) {
            val blockLabel = when {
                b.manual -> "Manual  "
                isBreak -> "Break  "
                else -> "Focus  "
            }
            if (onDelete == null || b.running) {
                container.addView(TextView(this).apply {
                    text = blockLabel + formatBlockRow(b.startMs, b.endMs, b.secs)
                    setTextColor(if (isBreak) themeCoordinator.secondaryColor else themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    setPadding(0, if (prevWasBreak) dp(12) else dp(3), 0, 0)
                })
            } else {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, if (prevWasBreak) dp(12) else dp(3), 0, 0)
                }
                row.addView(TextView(this).apply {
                    text = blockLabel + formatBlockRow(b.startMs, b.endMs, b.secs)
                    setTextColor(if (isBreak) themeCoordinator.secondaryColor else themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    alpha = 0.55f
                    setPadding(dp(10), 0, 0, 0)
                    setOnClickListener { onDelete(b, isBreak) }
                })
                container.addView(row)
            }
            prevWasBreak = isBreak
        }
    }

    private fun showDayDialog(dateStr: String, label: String) {
        val shared = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val focusSecs = shared.getLong("${dateStr}_focus_total", 0L) + (if (dateStr == todayStr) shared.getLong("accumulatedStudy", 0L) else 0L)
        val breakSecs = shared.getLong("${dateStr}_break_total", 0L) + (if (dateStr == todayStr) currentBreakSeconds else 0L)
        val (allSessions, allBreaks) = dayBlocks(dateStr)
        val sessions = allSessions.filter { it.secs >= 60L }
        val breaks = allBreaks.filter { it.secs >= 60L }
        val longest = sessions.maxOfOrNull { it.secs } ?: 0L

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        content.addView(TextView(this).apply {
            text = label
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Focus ${focusSecs / 3600}h ${(focusSecs % 3600) / 60}m    \u00B7    Break ${breakSecs / 3600}h ${(breakSecs % 3600) / 60}m"
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = "${sessions.size} sessions \u00B7 longest ${longest / 3600}h ${(longest % 3600) / 60}m"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(2))
        })
        if (sessions.isEmpty() && breaks.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "No session log for this day"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            })
        } else {
            fillBlockRows(content, sessions, breaks)
        }
        content.addView(TextView(this).apply {
            text = "CLOSE"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        content.addView(TextView(this).apply {
            text = mName
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Focus ${focusSecs / 3600}h ${(focusSecs % 3600) / 60}m    \u00B7    Break ${breakSecs / 3600}h ${(breakSecs % 3600) / 60}m"
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        })

        val shared = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val daySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val todayStr = daySdf.format(Date())
        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val days = shared.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { key ->
                val dStr = key.removeSuffix("_focus_total")
                val parsed = try { daySdf.parse(dStr) } catch (_: Exception) { null }
                if (parsed != null && monthSdf.format(parsed) == mName) dStr to parsed else null
            }
            .sortedBy { it.second.time }

        val dayList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, 0) }
        if (days.isEmpty()) {
            dayList.addView(TextView(this).apply {
                text = "No days logged this month"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(8), 0, 0)
            })
        } else {
            for ((dStr, parsed) in days) {
                var f = shared.getLong("${dStr}_focus_total", 0L)
                var b = shared.getLong("${dStr}_break_total", 0L)
                if (dStr == todayStr) { f += accumulatedStudy; b += currentBreakSeconds }
                if (f <= 0L && b <= 0L) continue
                dayList.addView(TextView(this).apply {
                    text = "${dayLabelSdf.format(parsed)}  \u00B7  Focus ${f / 3600}h ${(f % 3600) / 60}m  \u00B7  Break ${b / 3600}h ${(b % 3600) / 60}m"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    setPadding(0, dp(4), 0, 0)
                })
            }
        }
        val dayScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
            isVerticalScrollBarEnabled = false
        }
        dayScroll.addView(dayList)
        content.addView(dayScroll)

        content.addView(TextView(this).apply {
            text = "CLOSE"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showSummaryCardPreview() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.attributes?.windowAnimations = R.style.DialogScaleAnimation

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val currentSessionSecs = sharedPrefs.getLong("accumulatedStudy", 0L)
        val currentBreakSecs = sharedPrefs.getLong("currentBreakSeconds", 0L)
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
        val dateRangeFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Fixed output aspect for the shareable card (9:16 portrait)
        val CARD_ASPECT = 0.5625f

        // Compute stats for a given week (0 = this week, -1 = last week, etc.)
        fun computeWeekStats(weekOffset: Int): WeekStats {
            val mo = WeekHelper.mondayOffset(Calendar.getInstance())
            var totalSecs = 0L
            var breakSecs = 0L
            var bestSecs = 0L
            var bestName = "N/A"
            var bestDate = ""
            var sessionCount = 0
            val daySecs = LongArray(7)
            val dayGoals = LongArray(7)
            val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + weekOffset * 7) }
                val dateStr = sdf.format(c.time)
                var dayFocus = sharedPrefs.getLong("${dateStr}_focus_total", 0L)
                var dayBreak = sharedPrefs.getLong("${dateStr}_break_total", 0L)
                if (weekOffset == 0 && dateStr == todayStr) {
                    dayFocus += currentSessionSecs
                    dayBreak += currentBreakSecs
                }
                totalSecs += dayFocus
                breakSecs += dayBreak
                if (dayFocus > 0L) sessionCount++
                daySecs[i] = dayFocus
                dayGoals[i] = resolveGoalFor(dateStr)
                if (dayFocus > bestSecs) {
                    bestSecs = dayFocus
                    bestName = dayNameSdf.format(c.time)
                    bestDate = dateRangeFormat.format(c.time)
                }
            }

            var prevSecs = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + (weekOffset - 1) * 7) }
                prevSecs += sharedPrefs.getLong("${sdf.format(c.time)}_focus_total", 0L)
            }

            val vsPrev = if (prevSecs == 0L) {
                "No previous data"
            } else {
                val diff = (((totalSecs - prevSecs).toFloat() / prevSecs.toFloat()) * 100).toInt()
                if (diff >= 0) "+$diff%" else "$diff%"
            }

            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + weekOffset * 7) }
            val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + 6 + weekOffset * 7) }
            val dateRange = "${dateRangeFormat.format(startCal.time)} – ${dateRangeFormat.format(endCal.time)}".uppercase()

            // Streak as of the week's end (capped at today), counting back goal-qualifying days
            var streakEnd = endCal.clone() as Calendar
            val todayCal = Calendar.getInstance()
            if (streakEnd.after(todayCal)) streakEnd = todayCal
            var streak = 0
            var streakCal = streakEnd.clone() as Calendar
            while (streak < 365) {
                val dStr = sdf.format(streakCal.time)
                val dSecs = sharedPrefs.getLong("${dStr}_focus_total", 0L)
                val streakThreshold = if (streakGoalBased) resolveGoalFor(dStr) else 2700L
                if (dSecs >= streakThreshold) streak++ else break
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            return WeekStats(totalSecs, bestSecs, bestName, bestDate, vsPrev, dateRange, breakSecs, streak, totalSecs > 0L || breakSecs > 0L, daySecs, dayGoals, dayLabels, sessionCount)
        }

        // Render card with given stats at the fixed 9:16 aspect
        var currentBitmap: android.graphics.Bitmap? = null
        fun renderCard(stats: WeekStats) {
            val days = (0..6).map { i -> WeeklyCardView.Day(stats.dayLabels[i], stats.daySecs[i], stats.dayGoals[i]) }
            val card = WeeklyCardView(this).apply {
                setData(
                    WeeklyCardView.CardData(
                        dateRange = stats.dateRange,
                        totalSecs = stats.totalSecs,
                        breakSecs = stats.breakSecs,
                        bestName = stats.bestName,
                        bestSecs = stats.bestSecs,
                        streak = stats.streak,
                        vsPrev = stats.vsPrev,
                        sessionCount = stats.sessionCount,
                        days = days,
                        hasData = stats.hasData
                    )
                )
            }
            currentBitmap = renderWeeklyCardBitmap(card, CARD_ASPECT)
        }

        // Compute min/max week bounds
        val allDates = sharedPrefs.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { try { sdf.parse(it.removeSuffix("_focus_total")) } catch (_: Exception) { null } }
        val minDate = allDates.minOrNull()
        val minWeekOffset = if (minDate != null) {
            val minCal = Calendar.getInstance().apply { time = minDate }
            val daysFromToday = ((System.currentTimeMillis() - minCal.timeInMillis) / 86400000L).toInt()
            -(daysFromToday / 7) - 1
        } else {
            -52
        }
        val maxWeekOffset = 0 // don't show future weeks

        // Default to last week
        var weekOffset = -1
        var currentStats = computeWeekStats(weekOffset)
        renderCard(currentStats)

        // Scaled preview — fits the screen without scrolling
        val dm = resources.displayMetrics
        val maxW = (dm.widthPixels - dp(64)).coerceAtMost(dp(330))
        val reservedH = dp(60 + 16 + 28 + 24 + 48 + 8 + 56)
        val maxH = (dm.heightPixels - reservedH).coerceAtLeast(dp(280))
        var previewWidth = maxW
        var previewHeight = (previewWidth / CARD_ASPECT).toInt()
        if (previewHeight > maxH) {
            previewHeight = maxH
            previewWidth = (previewHeight * CARD_ASPECT).toInt()
        }

        val dialogRoot = FrameLayout(this)

        val scrollView = android.widget.ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(60), dp(16), dp(16))
        }
        scrollView.addView(contentColumn)

        val previewImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(previewWidth, previewHeight).apply { setMargins(0, 0, 0, dp(28)) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Weekly summary card preview"
            setImageBitmap(currentBitmap)
        }

        contentColumn.addView(previewImageView)

        // Week navigation row
        val dateRangeText = TextView(this).apply {
            text = currentStats.dateRange
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateNavButtons(prev: Button, next: Button) {
            prev.alpha = if (weekOffset <= minWeekOffset) 0.3f else 1f
            next.alpha = if (weekOffset >= maxWeekOffset) 0.3f else 1f
        }

        val prevWeekBtn = Button(this).apply {
            text = "\u25C0"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val nextWeekBtn = Button(this).apply {
            text = "\u25B6"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        prevWeekBtn.setOnClickListener {
            if (weekOffset > minWeekOffset) {
                weekOffset--
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        nextWeekBtn.setOnClickListener {
            if (weekOffset < maxWeekOffset) {
                weekOffset++
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        // Set initial button states
        updateNavButtons(prevWeekBtn, nextWeekBtn)

        val weekNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(24))
            }
        }
        weekNavRow.addView(prevWeekBtn)
        weekNavRow.addView(dateRangeText)
        weekNavRow.addView(nextWeekBtn)
        contentColumn.addView(weekNavRow)

        // Action buttons
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Loading overlay indicator
        val loadingOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setBackgroundColor(tintedColor(themeCoordinator.bgColor, 220))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        loadingOverlay.addView(android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
        })
        loadingOverlay.addView(TextView(this).apply {
            text = "Saving\u2026"
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            setPadding(0, dp(12), 0, 0)
        })

        fun getSafeBitmap() = currentBitmap

        val closeBtn = Button(this).apply {
            text = "Close"
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val saveBtn = Button(this).apply {
            text = "\uD83D\uDCBE Save"
            setTextColor(themeCoordinator.bgColor)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    if (bmp != null) saveBitmapToMediaStore(bmp)
                    handler.post { loadingOverlay.visibility = View.GONE; dialog.dismiss() }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val shareBtn = Button(this).apply {
            text = "\uD83D\uDCE4 Share"
            setTextColor(themeCoordinator.bgColor)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    var shared = false
                    if (bmp != null) {
                        val uri = writeBitmapToCache(bmp)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(shareIntent, "Share Summary Card")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            handler.post {
                                loadingOverlay.visibility = View.GONE
                                startActivity(chooser)
                            }
                            shared = true
                        }
                    }
                    if (!shared) {
                        handler.post {
                            loadingOverlay.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Failed to share", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }

        actionRow.addView(closeBtn)
        actionRow.addView(saveBtn)
        actionRow.addView(shareBtn)
        contentColumn.addView(actionRow)

        dialogRoot.addView(scrollView)
        dialogRoot.addView(loadingOverlay)

        dialog.setContentView(dialogRoot)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    data class WeekStats(
        val totalSecs: Long,
        val bestSecs: Long,
        val bestName: String,
        val bestDate: String,
        val vsPrev: String,
        val dateRange: String,
        val breakSecs: Long,
        val streak: Int,
        val hasData: Boolean,
        val daySecs: LongArray,
        val dayGoals: LongArray,
        val dayLabels: Array<String>,
        val sessionCount: Int
    )

    private fun renderViewToBitmap(view: View): android.graphics.Bitmap {
        val w = dp(360)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val h = view.measuredHeight
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun saveBitmapToMediaStore(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val filename = "StudySummary_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/StudyTimer")
                }
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    private fun renderWeeklyCardBitmap(view: WeeklyCardView, aspect: Float): android.graphics.Bitmap {
        val w = 1080
        val h = (w / aspect).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun writeBitmapToCache(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val dir = java.io.File(cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "StudySummary_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            }
            androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun createSettingsRow(icon: String, title: String, subtitle: String, endWidget: View? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            isClickable = true
            isFocusable = true
        }
        val iconText = TextView(this).apply {
            text = icon
            textSize = 22f
            setPadding(0, 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row.addView(iconText)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textContainer.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor(themeCoordinator.textColor)
            alpha = 0.5f
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, 3, 0, 0)
        })
        row.addView(textContainer)
        if (endWidget != null) {
            row.addView(endWidget)
        }
        return row
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(28, 0, 28, 0) }
            setBackgroundColor(tintedColor(themeCoordinator.textColor, 22))
        }
    }

    private fun refreshSettingsPanelPreservingScroll() {
        pendingSettingsScrollY = settingsScrollViewRef?.scrollY ?: 0
        navigateToPanel(AppPanel.SETTINGS)
        settingsScrollViewRef?.post {
            settingsScrollViewRef?.scrollTo(0, pendingSettingsScrollY)
        }
    }

    private fun applyRandomBothHues() {
        val primaryHue = (0..359).random()
        val secondaryHue = (0..359).random()
        val primaryColor = Color.HSVToColor(floatArrayOf(primaryHue.toFloat(), 0.65f, 0.95f))
        val secondaryColor = Color.HSVToColor(floatArrayOf(secondaryHue.toFloat(), 0.65f, 0.95f))
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putBoolean("dynamic_color", false)
            .putInt("customHue", primaryHue)
            .putInt("customPrimary", primaryColor)
            .putInt("customSecondaryHue", secondaryHue)
            .putInt("customSecondary", secondaryColor)
            .apply()
        themeCoordinator.applyThemeCoordinates()
        refreshSettingsPanelPreservingScroll()
    }

    private fun buildSettingsPanel(target: android.view.ViewGroup = panelContainer, captureScrollRef: Boolean = true) {
        if (captureScrollRef) {
            tabPageCache.keys.removeIf { it.startsWith("ST:") }
        }
        val settingsRootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val headerText = TextView(this).apply {
            text = "Settings"
            setTextColor(themeCoordinator.textColor)
            textSize = 22f
            letterSpacing = 0.02f
            setPadding(dp(6), dp(16), dp(6), dp(4))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setOnLongClickListener {
                isDevModeUnlocked = true
                Toast.makeText(context, "Developer Config Enabled", Toast.LENGTH_SHORT).show()
                navigateToPanel(AppPanel.SETTINGS)
                true
            }
        }
        settingsRootLayout.addView(headerText)

        val subtitleText = TextView(this).apply {
            text = "Customize your experience"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 13f
            setPadding(dp(6), 0, dp(6), dp(16))
        }
        settingsRootLayout.addView(subtitleText)

        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), 0, dp(6), dp(12))
        }

        fun createSettingsTabButton(title: String, targetTab: AppSettingsTab): TextView {
            return TextView(this).apply {
                text = title
                textSize = 14f
                setPadding(dp(20), dp(12), dp(20), dp(12))
                typeface = Typeface.create("sans-serif-medium", if (currentSettingsTab == targetTab) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (currentSettingsTab == targetTab) themeCoordinator.bgColor else themeCoordinator.textColor)
                background = if (currentSettingsTab == targetTab)
                    themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                else
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 80), 50f)
                setOnClickListener {
                    currentSettingsTab = targetTab
                    navigateToPanel(AppPanel.SETTINGS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(8), 0) }
            }
        }
        tabContainer.addView(createSettingsTabButton("General", AppSettingsTab.SIMPLE))
        tabContainer.addView(createSettingsTabButton("Appearance", AppSettingsTab.THEME))
        settingsRootLayout.addView(tabContainer)

        val existingScrollView = if (captureScrollRef) settingsScrollViewRef else null
        val settingsScrollView: ScrollView
        if (existingScrollView != null) {
            (existingScrollView.parent as? android.view.ViewGroup)?.removeView(existingScrollView)
            settingsScrollView = existingScrollView
        } else {
            settingsScrollView = ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                isFillViewport = true
            }
        }
        if (captureScrollRef) settingsScrollViewRef = settingsScrollView
        settingsScrollView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        settingsScrollView.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(120))
        }
        settingsScrollView.addView(layout)

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (currentSettingsTab == AppSettingsTab.SIMPLE) {

            layout.addView(createSectionLabel("GOALS"))
            val goalCard = createSettingsCard()
            val goalValueText = TextView(this).apply {
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(themeCoordinator.primaryColor)
                setPadding(dp(6), 0, dp(6), 0)
            }
            fun makeGoalStepBtn(label: String, delta: Long): TextView {
                return TextView(this).apply {
                    text = label
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                    setOnClickListener {
                        val current = sharedPrefs.getLong("daily_goal_secs", 2700L)
                        var next = current + delta
                        next = next.coerceIn(900L, 16 * 3600L)
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        sharedPrefs.edit()
                            .putLong("daily_goal_secs", next)
                            .putLong("${todayStr}_goal_secs", next)
                            .apply()
                        goalValueText.text = formatGoalLabel(next)
                    }
                }
            }
            goalValueText.text = formatGoalLabel(sharedPrefs.getLong("daily_goal_secs", 2700L))
            val goalRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
            }
            goalRow.addView(TextView(this).apply { text = "\uD83C\uDFAF"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
            val goalTextCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            goalTextCol.addView(TextView(this).apply { text = "Daily Goal"; setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
            goalTextCol.addView(TextView(this).apply { text = "Daily focus target shown in Insights"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
            goalRow.addView(goalTextCol)
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            controls.addView(makeGoalStepBtn("\u2212", -900L))
            controls.addView(goalValueText)
            controls.addView(makeGoalStepBtn("+", 900L))
            goalRow.addView(controls)
            goalCard.addView(goalRow)
            goalCard.addView(createDivider())
            val isStreakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
            val streakGoalSwitch = SwitchMaterial(this).apply {
                isChecked = isStreakGoalBased
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("streak_uses_daily_goal", isChecked).apply()
                }
            }
            goalCard.addView(createSettingsRow("\uD83D\uDD25", "Streak uses daily goal", "Off = 45m threshold, On = your daily goal", streakGoalSwitch))
            goalCard.addView(createDivider())
            goalCard.addView(TextView(this).apply {
                text = "15 min steps"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 11f
                setPadding(dp(18), dp(8), dp(18), dp(14))
            })
            layout.addView(goalCard)

            layout.addView(createSectionLabel("TIMER MODE"))
            val timerModeCard = createSettingsCard()
            val isCountdown = timerMode == "COUNTDOWN"

            fun modeRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val stopwatchRow = createSettingsRow("\u23F1\uFE0F", "Stopwatch", "Timer counts up until you end the session", modeRadio(!isCountdown))
            stopwatchRow.setOnClickListener {
                sharedPrefs.edit().putString("timer_mode", "STOPWATCH").apply()
                timerMode = "STOPWATCH"
                navigateToPanel(AppPanel.SETTINGS)
            }
            timerModeCard.addView(stopwatchRow)
            timerModeCard.addView(createDivider())
            val countdownRow = createSettingsRow("\u23F2\uFE0F", "Pomodoro Countdown", "Focus counts down, auto-switches to break at 0", modeRadio(isCountdown))
            countdownRow.setOnClickListener {
                sharedPrefs.edit().putString("timer_mode", "COUNTDOWN").apply()
                timerMode = "COUNTDOWN"
                navigateToPanel(AppPanel.SETTINGS)
            }
            timerModeCard.addView(countdownRow)
            layout.addView(timerModeCard)

            if (isCountdown) {
                val durationCard = createSettingsCard()
                val durationValueText = TextView(this).apply {
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    setPadding(dp(6), 0, dp(6), 0)
                }
                fun makeDurationBtn(label: String, delta: Long): TextView {
                    return TextView(this).apply {
                        text = label
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setTextColor(themeCoordinator.primaryColor)
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
                        setPadding(dp(14), dp(6), dp(14), dp(6))
                        applyHoldToRepeat(this) {
                            val current = sharedPrefs.getLong("focus_countdown_secs", 1500L)
                            var next = current + delta
                            next = next.coerceIn(300L, 4 * 3600L)
                            sharedPrefs.edit().putLong("focus_countdown_secs", next).apply()
                            durationValueText.text = formatCountdown(next)
                        }
                    }
                }
                durationValueText.text = formatCountdown(sharedPrefs.getLong("focus_countdown_secs", 1500L))
                val durationRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                durationRow.addView(TextView(this).apply { text = "\u23F3"; textSize = 22f; setPadding(0, 0, dp(14), 0) })
                val durationTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                durationTextCol.addView(TextView(this).apply { text = "Focus Duration"; setTextColor(themeCoordinator.textColor); textSize = 15f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
                durationTextCol.addView(TextView(this).apply { text = "Counts down, then break starts automatically"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 12f; setPadding(0, 3, 0, 0) })
                durationRow.addView(durationTextCol)
                val durationControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                durationControls.addView(makeDurationBtn("\u2212", -300L))
                durationControls.addView(durationValueText)
                durationControls.addView(makeDurationBtn("+", 300L))
                durationRow.addView(durationControls)
                durationCard.addView(durationRow)
                durationCard.addView(createDivider())
                durationCard.addView(TextView(this).apply {
                    text = "5 min steps \u00B7 5 min to 4 hours"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    setPadding(dp(18), dp(8), dp(18), dp(14))
                })
                layout.addView(durationCard)
            }

            layout.addView(createSectionLabel("DAILY REMINDER"))
            val reminderCard = createSettingsCard()
            val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
            val reminderSwitch = SwitchMaterial(this).apply {
                isChecked = reminderEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("reminder_enabled", isChecked).apply()
                    if (isChecked) {
                        ensureExactAlarmPermissionIfNeeded()
                        GoalReminderScheduler.schedule(this@MainActivity)
                    } else {
                        GoalReminderScheduler.cancel(this@MainActivity)
                    }
                }
            }
            reminderCard.addView(createSettingsRow("\uD83D\uDD14", "Evening Goal Reminder", "Reminds you if the daily goal isn't reached", reminderSwitch))
            reminderCard.addView(createDivider())
            val reminderHour = sharedPrefs.safeInt("reminder_hour", 21)
            val reminderMinute = sharedPrefs.safeInt("reminder_minute", 0)
            val timeLabel = TimeFormat.formatHourMinute(this@MainActivity, reminderHour, reminderMinute)
            val reminderTimeRow = createSettingsRow("\u23F0", "Reminder Time", "Shown when daily goal not yet hit", null)
            reminderTimeRow.addView(TextView(this).apply {
                text = timeLabel
                setTextColor(themeCoordinator.primaryColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            reminderTimeRow.setOnClickListener {
                android.app.TimePickerDialog(this@MainActivity, { _, h, m ->
                    sharedPrefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply()
                    if (sharedPrefs.getBoolean("reminder_enabled", true)) {
                        ensureExactAlarmPermissionIfNeeded()
                        GoalReminderScheduler.schedule(this@MainActivity)
                    }
                    navigateToPanel(AppPanel.SETTINGS)
                }, reminderHour, reminderMinute, TimeFormat.is24Hour(this@MainActivity)).show()
            }
            reminderCard.addView(reminderTimeRow)
            layout.addView(reminderCard)

            layout.addView(createSectionLabel("DISPLAY"))
            val displayCard = createSettingsCard()
            val isZenDefault = sharedPrefs.getBoolean("true_fullscreen_landscape", false)
            val zenSwitch = SwitchMaterial(this).apply {
                isChecked = isZenDefault
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("true_fullscreen_landscape", isChecked).apply()
                    applyImmersiveModeForLandscape()
                }
            }
            displayCard.addView(createSettingsRow("\u2194\uFE0F", "Immersive Landscape", "Hide system bars for distraction-free study", zenSwitch))
            displayCard.addView(createDivider())
            val timeFormatLabel = TextView(this).apply {
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(4), dp(4))
            }
            fun refreshTimeFormatLabel() {
                timeFormatLabel.text = when (TimeFormat.currentMode(this@MainActivity)) {
                    TimeFormat.Mode.H24 -> "24H"
                    TimeFormat.Mode.H12 -> "12H"
                    else -> "System"
                }
                timeFormatLabel.setTextColor(themeCoordinator.primaryColor)
            }
            refreshTimeFormatLabel()
            val timeFormatRow = createSettingsRow("\uD83D\uDD5B", "Time Format", "12h or 24h for sessions, reminders & charts", timeFormatLabel)
            timeFormatRow.setOnClickListener {
                val options = arrayOf("Follow System", "24 Hour", "12 Hour")
                val modes = arrayOf(TimeFormat.Mode.SYSTEM, TimeFormat.Mode.H24, TimeFormat.Mode.H12)
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Time Format")
                    .setSingleChoiceItems(options, modes.indexOf(TimeFormat.currentMode(this@MainActivity))) { dlg, which ->
                        TimeFormat.setMode(this@MainActivity, modes[which])
                        refreshTimeFormatLabel()
                        navigateToPanel(AppPanel.SETTINGS)
                        dlg.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            displayCard.addView(timeFormatRow)
            displayCard.addView(createDivider())
            val isWhiteTimer = sharedPrefs.getBoolean("pureWhiteTimer", false)
            val whiteTimerSwitch = SwitchMaterial(this).apply {
                isChecked = isWhiteTimer
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("pureWhiteTimer", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("⬜", "Pure White Timer", "Timer text always pure white", whiteTimerSwitch))
            displayCard.addView(createDivider())
            val isHeatmapEnabled = sharedPrefs.getBoolean("show_focus_heatmap", true)
            val heatmapSwitch = SwitchMaterial(this).apply {
                isChecked = isHeatmapEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_focus_heatmap", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("\uD83D\uDD25", "Focus Heatmap", "Show heatmap card in Insights", heatmapSwitch))
            displayCard.addView(createDivider())
            val isPatternEnabled = sharedPrefs.getBoolean("show_focus_pattern", true)
            val patternSwitch = SwitchMaterial(this).apply {
                isChecked = isPatternEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_focus_pattern", isChecked).apply()
                }
            }
            displayCard.addView(createSettingsRow("\u23F1\uFE0F", "Focus Pattern", "Show time-of-day pattern in Insights", patternSwitch))
            displayCard.addView(createDivider())
            val isKeepScreenOn = sharedPrefs.getBoolean("keep_screen_on", true)
            val keepScreenOnSwitch = SwitchMaterial(this).apply {
                isChecked = isKeepScreenOn
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("keep_screen_on", isChecked).apply()
                    updateKeepScreenOn()
                }
            }
            displayCard.addView(createSettingsRow("\uD83D\uDD11", "Keep Screen On", "Stay awake while focusing (turns off on break)", keepScreenOnSwitch))
            displayCard.addView(createDivider())
            val isPauseButtonEnabled = sharedPrefs.getBoolean("show_pause_button", true)
            val pauseButtonSwitch = SwitchMaterial(this).apply {
                isChecked = isPauseButtonEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    sharedPrefs.edit().putBoolean("show_pause_button", isChecked).apply()
                    if (currentPanel == AppPanel.FOCUS) updateVisualStyles()
                }
            }
            displayCard.addView(createSettingsRow("\u23F8\uFE0F", "Pause Button", "Show pause button while focusing", pauseButtonSwitch))
            layout.addView(displayCard)

            layout.addView(createSectionLabel("DATA MANAGEMENT"))
            val dataCard = createSettingsCard()
            val exportRow = createSettingsRow("\uD83D\uDCE4", "Export Logs", "Save study data as JSON file")
            exportRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    val fileDateFormat = SimpleDateFormat("dd_MMM", Locale.getDefault())
                    putExtra(Intent.EXTRA_TITLE, "backup_${fileDateFormat.format(Date())}.json")
                }
                exportLauncher.launch(intent)
            }
            dataCard.addView(exportRow)
            dataCard.addView(createDivider())
            val importRow = createSettingsRow("\uD83D\uDCE5", "Import Data", "Restore from backup file")
            importRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/json"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                importLauncher.launch(Intent.createChooser(intent, "Select Backup JSON File"))
            }
            dataCard.addView(importRow)
            dataCard.addView(createDivider())
            val csvRow = createSettingsRow("\uD83D\uDCCA", "Export CSV", "Daily summary, sessions & totals for spreadsheets")
            csvRow.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    val fileDateFormat = SimpleDateFormat("dd_MMM", Locale.getDefault())
                    putExtra(Intent.EXTRA_TITLE, "study_log_${fileDateFormat.format(Date())}.csv")
                }
                csvLauncher.launch(intent)
            }
            dataCard.addView(csvRow)
            layout.addView(dataCard)

            layout.addView(createSectionLabel("ABOUT"))
            val aboutCard = createSettingsCard()
            val versionRow = createSettingsRow("\uD83D\uDCCB", "Version", "v${currentVersionName()} \u00B7 build ${currentVersionCodeLong()}")
            aboutCard.addView(versionRow)
            aboutCard.addView(createDivider())
            val updateRow = createSettingsRow("\uD83D\uDD04", "Check for Updates", "Looks for a newer version on our server")
            updateRow.setOnClickListener { checkForUpdates(manual = true) }
            aboutCard.addView(updateRow)
            layout.addView(aboutCard)

            layout.addView(createSectionLabel("EXPERIMENTAL"))
            val dangerCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { cornerRadius = 35f; setColor(0x0DFF4444.toInt()); setStroke(1, 0x1AFF4444.toInt()) }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(4)) }
            }
            dangerCard.addView(TextView(this).apply {
                text = "These actions are irreversible. Hold the button to confirm."
                setTextColor(0xFFEF4444.toInt()); alpha = 0.7f; textSize = 12f; setPadding(dp(18), dp(16), dp(18), dp(4))
            })
            val deleteTodayBtn = Button(this).apply {
                text = "Wipe Today's Data"
                setTextColor(0xFFEF4444.toInt()); textSize = 14f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = GradientDrawable().apply { cornerRadius = 20f; setColor(0x1AFF4444.toInt()) }
                setPadding(dp(24), dp(16), dp(24), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(16)) }
                setOnLongClickListener {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply { remove("${todayStr}_focus_total"); remove("${todayStr}_break_total"); remove("${todayStr}_focus_manual"); remove("${todayStr}_break_manual"); apply() }
                    TimelineLogger.deleteDay(context, todayStr)
                    handleStopSession(silent = true)
                    Toast.makeText(context, "Today's logs cleared.", Toast.LENGTH_SHORT).show()
                    true
                }
                setOnClickListener { Toast.makeText(context, "Hold to confirm wipe", Toast.LENGTH_SHORT).show() }
            }
            dangerCard.addView(deleteTodayBtn)
            layout.addView(dangerCard)

            if (isDevModeUnlocked) {
                layout.addView(createSectionLabel("DEVELOPER"))
                val devCard = createSettingsCard().apply { setPadding(20, 20, 20, 20) }
                devCard.addView(TextView(this).apply {
                    text = "Dev-only timeline tools. Normal users only see the log in Stats \u2192 Timeline."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.7f
                    textSize = 12f
                    setPadding(0, 0, 0, dp(10))
                })
                val toolRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, 0) }
                }
                val editorBtn = TextView(this).apply {
                    text = "\u270E  Edit timeline"
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.bgColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { showDevTimelineEditor() }
                }
                val modeToggleBtn = TextView(this).apply {
                    text = if (isAdjustingFocusMode) "\uD83C\uDFAF Focus Pool" else "\u2615 Break Pool"
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.bgColor)
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { isAdjustingFocusMode = !isAdjustingFocusMode; text = if (isAdjustingFocusMode) "\uD83C\uDFAF Focus Pool" else "\u2615 Break Pool" }
                }
                toolRow.addView(editorBtn)
                toolRow.addView(modeToggleBtn)
                devCard.addView(toolRow)
                devCard.addView(TextView(this).apply {
                    text = "Shift today's last block by:"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 11f
                    setPadding(0, dp(10), 0, 0)
                })

                fun applyShift(ds: Long) {
                    val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val isFocus = isAdjustingFocusMode
                    val key = if (isFocus) "${todayStr}_focus_total" else "${todayStr}_break_total"
                    val manualKey = if (isFocus) "${todayStr}_focus_manual" else "${todayStr}_break_manual"
                    var cv = prefs.getLong(key, 0L)
                    cv = max(0L, cv + ds)
                    prefs.edit().putLong(key, cv).putLong(manualKey, prefs.getLong(manualKey, 0L) + ds).apply()
                    statsDirty = true
                    recalculateStreak()
                    val what = if (isFocus) "focus" else "break"
                    Toast.makeText(this, "$what last block shifted on Today", Toast.LENGTH_SHORT).show()
                }
                fun makeIncBtn(l: String, ds: Long): Button { return Button(this).apply { text = l; setTextColor(themeCoordinator.textColor); textSize = 12f; typeface = Typeface.MONOSPACE; background = themeCoordinator.createButtonBackground(themeCoordinator.bgColor); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(6, 6, 6, 6) }; setOnClickListener { applyShift(ds) } } }
                val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6, 0, 0) } }
                row1.addView(makeIncBtn("- 1h", -3600L)); row1.addView(makeIncBtn("-15m", -900L))
                val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
                row2.addView(makeIncBtn("+15m", 900L)); row2.addView(makeIncBtn("+ 1h", 3600L))
                devCard.addView(row1); devCard.addView(row2)
                layout.addView(devCard)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                layout.addView(createSectionLabel("DYNAMIC COLOR"))
                val dynamicCard = createSettingsCard()
                val isDynamic = sharedPrefs.getBoolean("dynamic_color", false)
                val dynamicSwitch = SwitchMaterial(this).apply {
                    isChecked = isDynamic
                    setOnCheckedChangeListener { _, isChecked ->
                        sharedPrefs.edit().putBoolean("dynamic_color", isChecked).apply()
                        themeCoordinator.applyThemeCoordinates()
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                dynamicCard.addView(createSettingsRow(
                    "\uD83C\uDFA8", "Material You",
                    if (isDynamic) "Follows your wallpaper palette" else "Use your wallpaper's accent colors (Android 12+)",
                    dynamicSwitch
                ))
                layout.addView(dynamicCard)
            }

            layout.addView(createSectionLabel("DARK MODE"))

            val modeCard = createSettingsCard()
            val isEclipse = themeCoordinator.activeBgMode == "ECLIPSE"
            val isLight = themeCoordinator.activeBgMode == "LIGHT"

            fun modeRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val lightRow = createSettingsRow("\u2600\uFE0F", "Light", "Clean bright theme", modeRadio(isLight))
            lightRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "LIGHT").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(lightRow)
            modeCard.addView(createDivider())
            val eclipseRow = createSettingsRow("\uD83C\uDF19", "Slate", "Soft dark grey tones", modeRadio(isEclipse))
            eclipseRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "ECLIPSE").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(eclipseRow)
            modeCard.addView(createDivider())
            val oledRow = createSettingsRow("\u2B24", "AMOLED Black", "True black for OLED screens", modeRadio(!isEclipse && !isLight))
            oledRow.setOnClickListener {
                sharedPrefs.edit().putString("activeBgMode", "OLED").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            modeCard.addView(oledRow)
            layout.addView(modeCard)

            layout.addView(createSectionLabel("THEME STYLE"))

            val styleCard = createSettingsCard()
            val isGlass = themeCoordinator.isGlassStyle()

            fun styleRadio(selected: Boolean): View {
                return View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(26, 26)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (selected) themeCoordinator.primaryColor else Color.TRANSPARENT)
                        setStroke(3, if (selected) themeCoordinator.primaryColor else themeCoordinator.textColor)
                    }
                }
            }

            val glassRow = createSettingsRow("\u2728", "Glass", "Translucent cards, glow & gradients", styleRadio(isGlass))
            glassRow.setOnClickListener {
                sharedPrefs.edit().putString("ui_style", "GLASS").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            styleCard.addView(glassRow)
            styleCard.addView(createDivider())
            val classicRow = createSettingsRow("\u25A6", "Classic", "Solid flat panels, true black stays true", styleRadio(!isGlass))
            classicRow.setOnClickListener {
                sharedPrefs.edit().putString("ui_style", "CLASSIC").apply()
                themeCoordinator.applyThemeCoordinates()
                navigateToPanel(AppPanel.SETTINGS)
            }
            styleCard.addView(classicRow)
            layout.addView(styleCard)

            layout.addView(createSectionLabel("ACCENT COLORS"))

            val randomAccentCard = createSettingsCard()
            val randomAccentRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                isClickable = true
                isFocusable = true
                setOnClickListener { applyRandomBothHues() }
            }
            val randomAccentIcon = TextView(this).apply {
                text = "\uD83C\uDFB2"
                textSize = 24f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(tintedColor(themeCoordinator.primaryColor, 30)) }
            }
            randomAccentRow.addView(randomAccentIcon)
            val randomAccentText = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(16), 0, dp(8), 0)
            }
            randomAccentText.addView(TextView(this).apply {
                text = "Randomize accent colors"
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            randomAccentText.addView(TextView(this).apply {
                text = "Picks fresh hues for focus & break together"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setPadding(0, 2, 0, 0)
            })
            randomAccentRow.addView(randomAccentText)
            randomAccentRow.addView(TextView(this).apply {
                text = "\u21BA"
                setTextColor(themeCoordinator.textColor)
                textSize = 20f
            })
            randomAccentCard.addView(randomAccentRow)
            layout.addView(randomAccentCard)

            val spectrumColors = IntArray(9)
            for (i in 0..8) { spectrumColors[i] = Color.HSVToColor(floatArrayOf((i * 45).toFloat(), 0.85f, 0.85f)) }
            val spectrumTrack = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, spectrumColors).apply { cornerRadius = 15f }

            val focusCard = createSettingsCard()

            val focusHeaderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
            val focusHeaderColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            focusHeaderColumn.addView(TextView(this).apply { text = "FOCUS ACCENT"; setTextColor(themeCoordinator.primaryColor); textSize = 12f; letterSpacing = 0.15f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) })
            focusHeaderColumn.addView(TextView(this).apply { text = "Toggle to adjust hue"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) })
            focusHeaderRow.addView(focusHeaderColumn)
            val focusToggle = SwitchMaterial(this).apply {
                isChecked = showFocusHueBar
                setOnCheckedChangeListener { _, isChecked ->
                    showFocusHueBar = isChecked
                    navigateToPanel(AppPanel.SETTINGS)
                }
            }
            focusHeaderRow.addView(focusToggle)
            focusCard.addView(focusHeaderRow)

            val currentHue = sharedPrefs.safeInt("customHue", 200)
            if (showFocusHueBar) {
                val primaryHueValue = TextView(this).apply {
                    text = "${currentHue}\u00B0"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(18), 0, dp(18), 0)
                }
                focusCard.addView(primaryHueValue)

                val primaryHueBar = SeekBar(this).apply {
                    max = 360; progress = currentHue; background = spectrumTrack
                    setPadding(20, 14, 20, 14)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(8), 8, dp(8), 12) }
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            primaryHueValue.text = "${progress}\u00B0"
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            themeCoordinator.primaryColor = color
                            primaryHueValue.setTextColor(color)
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val progress = seekBar?.progress ?: currentHue
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customHue", progress).putInt("customPrimary", color).apply()
                            themeCoordinator.applyThemeCoordinates()
                            refreshSettingsPanelPreservingScroll()
                        }
                    })
                }
                focusCard.addView(primaryHueBar)
                focusCard.addView(createDivider())
            }

            val primarySwatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(16), dp(14), dp(16), dp(18))
            }
            fun addPrimarySwatch(colorHex: Int, nameStr: String) {
                val swatchContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                swatchContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(56, 56)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colorHex) }
                    setOnClickListener {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(colorHex, hsv)
                        sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customHue", hsv[0].toInt()).putInt("customPrimary", colorHex).apply()
                        themeCoordinator.applyThemeCoordinates()
                        refreshSettingsPanelPreservingScroll()
                    }
                })
                swatchContainer.addView(TextView(this).apply {
                    text = nameStr
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 0)
                    maxLines = 1
                })
                primarySwatchRow.addView(swatchContainer)
            }
            addPrimarySwatch(0xFFFFFFFF.toInt(), "White")
            addPrimarySwatch(0xFFF472B6.toInt(), "Rose")
            addPrimarySwatch(0xFFC4B5FD.toInt(), "Lavender")
            addPrimarySwatch(0xFF38BDF8.toInt(), "Sky")
            addPrimarySwatch(0xFFFB923C.toInt(), "Orange")
            addPrimarySwatch(0xFFA7F3D0.toInt(), "Mint")
            focusCard.addView(primarySwatchRow)
            layout.addView(focusCard)

            val breakCard = createSettingsCard()

            val breakHeaderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
            val breakHeaderColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            breakHeaderColumn.addView(TextView(this).apply { text = "BREAK ACCENT"; setTextColor(themeCoordinator.secondaryColor); textSize = 12f; letterSpacing = 0.15f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) })
            breakHeaderColumn.addView(TextView(this).apply { text = "Toggle to adjust hue"; setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) })
            breakHeaderRow.addView(breakHeaderColumn)
            val breakToggle = SwitchMaterial(this).apply {
                isChecked = showBreakHueBar
                setOnCheckedChangeListener { _, isChecked ->
                    showBreakHueBar = isChecked
                    navigateToPanel(AppPanel.SETTINGS)
                }
            }
            breakHeaderRow.addView(breakToggle)
            breakCard.addView(breakHeaderRow)

            val currentSecHue = sharedPrefs.safeInt("customSecondaryHue", 330)
            if (showBreakHueBar) {
                val secondaryHueValue = TextView(this).apply {
                    text = "${currentSecHue}\u00B0"
                    setTextColor(themeCoordinator.secondaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(18), 0, dp(18), 0)
                }
                breakCard.addView(secondaryHueValue)

                val secondaryHueBar = SeekBar(this).apply {
                    max = 360; progress = currentSecHue; background = spectrumTrack
                    setPadding(20, 14, 20, 14)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(8), 8, dp(8), 12) }
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            secondaryHueValue.text = "${progress}\u00B0"
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            themeCoordinator.secondaryColor = color
                            secondaryHueValue.setTextColor(color)
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val progress = seekBar?.progress ?: currentSecHue
                            val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), 0.65f, 0.95f))
                            sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customSecondaryHue", progress).putInt("customSecondary", color).apply()
                            themeCoordinator.applyThemeCoordinates()
                            refreshSettingsPanelPreservingScroll()
                        }
                    })
                }
                breakCard.addView(secondaryHueBar)
                breakCard.addView(createDivider())
            }

            val secondarySwatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(16), dp(14), dp(16), dp(18))
            }
            fun addSecondarySwatch(colorHex: Int, nameStr: String) {
                val swatchContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                swatchContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(56, 56)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colorHex) }
                    setOnClickListener {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(colorHex, hsv)
                        sharedPrefs.edit().putBoolean("dynamic_color", false).putInt("customSecondaryHue", hsv[0].toInt()).putInt("customSecondary", colorHex).apply()
                        themeCoordinator.applyThemeCoordinates()
                        refreshSettingsPanelPreservingScroll()
                    }
                })
                swatchContainer.addView(TextView(this).apply {
                    text = nameStr
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 0)
                    maxLines = 1
                })
                secondarySwatchRow.addView(swatchContainer)
            }
            addSecondarySwatch(0xFFF472B6.toInt(), "Rose")
            addSecondarySwatch(0xFFFBBF24.toInt(), "Gold")
            addSecondarySwatch(0xFFA78BFA.toInt(), "Violet")
            addSecondarySwatch(0xFF34D399.toInt(), "Emerald")
            addSecondarySwatch(0xFFF87171.toInt(), "Coral")
            breakCard.addView(secondarySwatchRow)
            layout.addView(breakCard)
        }

        val pushToBottomSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        layout.addView(pushToBottomSpacer)

        val creditsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(30), 0, dp(8)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        creditsContainer.addView(TextView(this).apply { text = "Developed by Pushkar Saini"; setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); gravity = Gravity.CENTER })
        creditsContainer.addView(TextView(this).apply { text = "Special thanks to Nikhil Tyagi"; setTextColor(themeCoordinator.textColor); alpha = 0.4f; textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.NORMAL); gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0) })
        layout.addView(creditsContainer)

        settingsRootLayout.addView(settingsScrollView)

        val settingsRoot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        settingsRoot.addView(settingsRootLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val settingsBackFab = TextView(this).apply {
            text = "\u2190 Back"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(themeCoordinator.primaryColor) }
            elevation = dp(8).toFloat()
            setOnClickListener { navigateToPanel(AppPanel.FOCUS) }
            layoutParams = FrameLayout.LayoutParams(dp(150), dp(54), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(20), dp(20)) }
        }
        settingsRoot.addView(settingsBackFab)

        target.addView(settingsRoot)
    }

    private fun formatTime(totalSeconds: Long): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun maybeFireForegroundGoalPing(todayStr: String) {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("goal_pinged_date", null) == todayStr) return
        val focus = prefs.getLong("${todayStr}_focus_total", 0L) + accumulatedStudy
        val goal = resolveGoalFor(todayStr)
        if (focus < goal) return
        prefs.edit().putString("goal_pinged_date", todayStr).apply()
        try { rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
        Toast.makeText(this, "\uD83C\uDF89 Daily goal reached!", Toast.LENGTH_LONG).show()
    }

    private fun setupTimerLoop() {
        updateRunnable = Runnable {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
            currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
            currentTimerState = TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE")
            timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
            focusCountdownSecs = sharedPrefs.getLong("focus_countdown_secs", 1500L)
            focusRemainingSecs = sharedPrefs.getLong("focus_remaining_secs", 0L)
            prePauseState = runCatching { TimerState.valueOf(sharedPrefs.getString("pre_pause_state", "STUDYING") ?: "STUDYING") }.getOrDefault(TimerState.STUDYING)

            updateKeepScreenOn()

            val dayBucket = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86400000L
            if (dayBucket != lastDayBucket) {
                lastDayBucket = dayBucket
                cachedTodayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                statsDirty = true
            }
            if (currentTimerState != lastTickTimerState) {
                lastTickTimerState = currentTimerState
                statsDirty = true
            }

            maybeFireForegroundGoalPing(cachedTodayStr)

            if (currentPanel == AppPanel.FOCUS) {
                val showCountdown = timerMode == "COUNTDOWN" &&
                    (currentTimerState == TimerState.STUDYING ||
                        (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING))
                studyTimerDisplay.text = if (showCountdown) formatCountdown(focusRemainingSecs) else formatTime(accumulatedStudy)
                breakTimerDisplay.text = "Break: " + formatTime(currentBreakSeconds)
                updateTimerRing(showCountdown)
                val styleKey = "$currentTimerState|${themeCoordinator.primaryColor}|${themeCoordinator.secondaryColor}|${themeCoordinator.bgColor}|$isZenModeActive|${sharedPrefs.getBoolean("show_pause_button", true)}"
                if (styleKey != lastStyleKey) {
                    lastStyleKey = styleKey
                    updateVisualStyles()
                }
                updatePauseBlink()
            }
            handler.postDelayed(updateRunnable, 500)
        }
        handler.post(updateRunnable)
    }

    private fun updatePauseBlink() {
        val animationsOff = try {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f
        } catch (_: Exception) { false }
        val shouldBlink = currentTimerState == TimerState.PAUSED && !animationsOff
        if (shouldBlink && pauseBlinkAnimator == null) {
            val target = if (prePauseState == TimerState.BREAK) breakTimerDisplay else studyTimerDisplay
            pauseBlinkAnimator = ValueAnimator.ofFloat(1f, 0.25f).apply {
                duration = 560
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { a ->
                    val alpha = a.animatedValue as Float
                    target.alpha = alpha
                    statusBadge.alpha = alpha
                }
                start()
            }
        } else if (!shouldBlink) {
            pauseBlinkAnimator?.cancel()
            pauseBlinkAnimator = null
            studyTimerDisplay.alpha = 1f
            breakTimerDisplay.alpha = 1f
            statusBadge.alpha = 1f
        }
    }

    private fun playToggleFeedback() {
        try {
            rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    private fun playStopFeedback() {
        try {
            rootLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
    }

    private fun handleStateToggle() {
        if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
            flipMainButton()
        }
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_TOGGLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    private fun handlePause() {
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    private fun updateTimerRing(showCountdown: Boolean) {
        if (!::timerRing.isInitialized || timerRing.visibility != View.VISIBLE) return
        if (timerMode != "COUNTDOWN") return
        val base = themeCoordinator.textColor
        val track = (base and 0x00FFFFFF) or 0x1F000000
        val studying = currentTimerState == TimerState.STUDYING ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
        val breaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
        when {
            studying && showCountdown && focusCountdownSecs > 0 -> {
                timerRing.setProgress(focusRemainingSecs.toFloat() / focusCountdownSecs, themeCoordinator.primaryColor, track)
            }
            breaking -> {
                timerRing.setProgress(1f, themeCoordinator.secondaryColor, track)
            }
            else -> {
                timerRing.setProgress(0f, themeCoordinator.primaryColor, track)
            }
        }
    }

    private fun formatCountdown(totalSeconds: Long): String {
        val s = max(0L, totalSeconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    private fun flipMainButton() {
        if (frontFlipAnim?.isRunning == true || backFlipAnim?.isRunning == true) return
        mainBtn.scaleY = 1f
        val goingToBreak = currentTimerState != TimerState.BREAK
        frontFlipAnim = ValueAnimator.ofFloat(1f, 0f)
        frontFlipAnim!!.duration = 280
        frontFlipAnim!!.interpolator = android.view.animation.AccelerateInterpolator()
        frontFlipAnim!!.addUpdateListener { mainBtn.scaleY = it.animatedValue as Float }
        frontFlipAnim!!.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (animation !== frontFlipAnim) return
                if (goingToBreak) {
                    mainBtn.text = "RESUME DEEP FOCUS"
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                } else {
                    mainBtn.text = "TAKE A BREAK"
                    mainBtn.background = rippleBackground(themeCoordinator.secondaryColor)
                }
                mainBtn.setTextColor(themeCoordinator.bgColor)
                backFlipAnim = ValueAnimator.ofFloat(0f, 1f)
                backFlipAnim!!.duration = 280
                backFlipAnim!!.interpolator = android.view.animation.DecelerateInterpolator()
                backFlipAnim!!.addUpdateListener { mainBtn.scaleY = it.animatedValue as Float }
                backFlipAnim!!.start()
            }
        })
        frontFlipAnim!!.start()
    }

    private fun handleStopSession(silent: Boolean = false) {
        val savedStudy = accumulatedStudy
        val intent = Intent(this, TimerService::class.java).apply {
            action = if (silent) TimerService.ACTION_STOP_SILENT else TimerService.ACTION_STOP
        }
        startService(intent)

        currentTimerState = TimerState.IDLE
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        focusRemainingSecs = 0L

        statsDirty = true
        Thread { backupManager.runSilentAutoBackup() }.start()
        recalculateStreak(todayExtra = if (silent) 0L else savedStudy)

        if (silent) {
            try { rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
        } else {
            playStopFeedback()
        }

        if (currentPanel == AppPanel.FOCUS) {
            updateVisualStyles()
            studyTimerDisplay.text = "00:00:00"
            breakTimerDisplay.text = "Break: 00:00:00"
        }
        StudyWidgetProvider.refresh(this)
    }

    private fun updateKeepScreenOn() {
        val enabled = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).getBoolean("keep_screen_on", true)
        val shouldKeep = enabled && currentTimerState == TimerState.STUDYING
        val newFlag = if (shouldKeep) 1 else 0
        if (newFlag == lastKeepScreenOn) return
        lastKeepScreenOn = newFlag
        if (shouldKeep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateVisualStyles() {
        val timerColor = if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor
        when (currentTimerState) {
            TimerState.IDLE -> {
                statusBadge.text = "READY TO TRACK"; statusBadge.setTextColor(themeCoordinator.primaryColor); studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                mainBtn.text = "START FOCUS"; mainBtn.setTextColor(themeCoordinator.bgColor)
                mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = View.GONE; stopBtn.visibility = View.GONE 
            }
            TimerState.STUDYING -> {
                statusBadge.text = "⚡ Learning time"; statusBadge.setTextColor(themeCoordinator.primaryColor); studyTimerDisplay.setTextColor(timerColor)
                breakTimerDisplay.setTextColor(themeCoordinator.textColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE; mainBtn.text = "TAKE A BREAK"; mainBtn.setTextColor(themeCoordinator.bgColor)
                mainBtn.background = rippleBackground(themeCoordinator.secondaryColor); pauseBtn.visibility = pauseButtonVisibility(); pauseBtn.setTextColor(themeCoordinator.textColor); pauseBtn.background = outlinedButtonBackground(); stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
            TimerState.BREAK -> {
                statusBadge.text = "☕ BREAK IN PROGRESS"; statusBadge.setTextColor(themeCoordinator.secondaryColor); studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor) 
                breakTimerDisplay.setTextColor(themeCoordinator.secondaryColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE; mainBtn.text = "RESUME DEEP FOCUS"; mainBtn.setTextColor(themeCoordinator.bgColor)
                mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = pauseButtonVisibility(); pauseBtn.setTextColor(themeCoordinator.textColor); pauseBtn.background = outlinedButtonBackground(); stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
            TimerState.PAUSED -> {
                statusBadge.text = "⏸ PAUSED"; statusBadge.setTextColor(themeCoordinator.textColor); studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                breakTimerDisplay.setTextColor(themeCoordinator.textColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE; mainBtn.text = "RESUME"; mainBtn.setTextColor(themeCoordinator.bgColor)
                mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = View.GONE; stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
        }
    }

    private fun maybeShowOnboarding() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_done", false)) return
        prefs.edit().putBoolean("onboarding_done", true).apply()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = "\uD83D\uDCC5 Welcome to StudyTimer"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        val steps = listOf(
            "\u25B6\ufe0f  Tap START FOCUS to begin tracking a study session",
            "\u2615\ufe0f  Take breaks anytime \u2014 they won't break your focus streak",
            "\uD83D\uDCCA  Visit Insights to see charts, heatmap & streak history",
            "\u23F0  Add a widget or enable the daily reminder from Settings"
        )
        for (s in steps) {
            content.addView(TextView(this).apply {
                text = s
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                setPadding(0, dp(12), 0, 0)
            })
        }
        content.addView(TextView(this).apply {
            text = "GET STARTED"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 24f; setColor(themeCoordinator.primaryColor) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(18), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Study Timer Control", NotificationManager.IMPORTANCE_LOW).apply { 
                description = "Persistent tray utilities for active sessions"
                setShowBadge(false) 
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun ensureExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                } catch (_: Exception) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveModeForLandscape()
        StudyWidgetProvider.refresh(this)
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed({ checkForUpdates(manual = false) }, 4000)
        handler.postDelayed({ maybePromptBatteryOptimization() }, 1200)
    }

    override fun onDestroy() {
        handler.removeCallbacks(holdToEndRunnable)
        pauseBlinkAnimator?.cancel()
        pauseBlinkAnimator = null
        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    private fun applyTrueFullscreenMode() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isZenModeEnabled = sharedPrefs.getBoolean("true_fullscreen_landscape", false)

        if (!isLandscape) {
            isZenModeActive = false
            panelContainer.setOnClickListener(null)
            return
        }

        if (isZenModeEnabled) {
            isZenModeActive = true
            navHeader.visibility = View.GONE
            statusBadgeContainer.visibility = View.GONE
            breakTimerDisplay.visibility = View.GONE
            controlActionContainer.visibility = View.GONE
            studyTimerDisplay.textSize = 110f

            panelContainer.setOnClickListener {
                if (controlActionContainer.visibility == View.GONE) {
                    navHeader.visibility = View.VISIBLE
                    statusBadgeContainer.visibility = View.VISIBLE
                    breakTimerDisplay.visibility = View.VISIBLE
                    controlActionContainer.visibility = View.VISIBLE
                    studyTimerDisplay.textSize = 64f
                } else {
                    navHeader.visibility = View.GONE
                    statusBadgeContainer.visibility = View.GONE
                    breakTimerDisplay.visibility = View.GONE
                    controlActionContainer.visibility = View.GONE
                    studyTimerDisplay.textSize = 110f
                }
            }
        } else {
            isZenModeActive = false
            navHeader.visibility = View.VISIBLE
            statusBadgeContainer.visibility = View.VISIBLE
            breakTimerDisplay.visibility = View.VISIBLE
            controlActionContainer.visibility = View.VISIBLE
            studyTimerDisplay.textSize = 64f
            panelContainer.setOnClickListener(null)
        }
    }

    private fun applyImmersiveModeForLandscape() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            showSystemUI()
            return
        }
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val immersiveEnabled = sharedPrefs.getBoolean("true_fullscreen_landscape", false)
        if (immersiveEnabled) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

}

private class HoldRingButton(context: Context) : androidx.appcompat.widget.AppCompatButton(context) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var ringColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val cornerRadius = 80f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0.05f) return
        paint.color = ringColor
        val inset = paint.strokeWidth / 2f
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val w = right - left
        val h = bottom - top
        val r = min(cornerRadius, min(w, h) / 2f)
        val arcLen = (PI * r / 2f).toFloat()
        val edgeW = w - 2f * r
        val edgeH = h - 2f * r
        val totalLen = 2f * (edgeW + edgeH) + 4f * arcLen
        var remaining = progress * totalLen
        val path = Path()

        fun lineSegment(fullLen: Float, startX: Float, startY: Float, endX: Float, endY: Float) {
            if (remaining <= 0f || fullLen <= 0f) return
            if (remaining >= fullLen) {
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
                remaining -= fullLen
            } else {
                val t = remaining / fullLen
                path.moveTo(startX, startY)
                path.lineTo(startX + (endX - startX) * t, startY + (endY - startY) * t)
                remaining = 0f
            }
        }

        fun cornerArc(arcBounds: RectF, startAngle: Float) {
            if (remaining <= 0f || arcLen <= 0f) return
            if (remaining >= arcLen) {
                path.addArc(arcBounds, startAngle, 90f)
                remaining -= arcLen
            } else {
                path.addArc(arcBounds, startAngle, (remaining / arcLen) * 90f)
                remaining = 0f
            }
        }

        lineSegment(edgeW, left + r, top, right - r, top)
        cornerArc(RectF(right - 2f * r, top, right, top + 2f * r), 270f)
        lineSegment(edgeH, right, top + r, right, bottom - r)
        cornerArc(RectF(right - 2f * r, bottom - 2f * r, right, bottom), 0f)
        lineSegment(edgeW, right - r, bottom, left + r, bottom)
        cornerArc(RectF(left, bottom - 2f * r, left + 2f * r, bottom), 90f)
        lineSegment(edgeH, left, bottom - r, left, top + r)
        cornerArc(RectF(left, top, left + 2f * r, top + 2f * r), 180f)

        canvas.drawPath(path, paint)
    }

}

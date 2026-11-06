package com.madeby.JAI

import android.app.Dialog
import android.view.Window
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
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.HapticFeedbackConstants
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min


import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    internal val timerViewModel: TimerViewModel by viewModels()

    internal var currentPanel = AppPanel.FOCUS
    internal var currentTimerState: TimerState
        get() = timerViewModel.uiState.value.state
        set(value) {
            timerViewModel.setTimerState(value)
        }
    internal var currentStatsTab = AppStatsTab.OVERVIEW
    internal var currentSettingsTab = AppSettingsTab.HUB
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
    private var tabDragCommitSettingsTab = AppSettingsTab.HUB
    internal class CachedTabPage(val view: View, val statsGen: Int, val themeSig: String)
    internal val tabPageCache = HashMap<String, CachedTabPage>()
    internal var selectedDaysFilter = 7

    internal var accumulatedStudy: Long
        get() = timerViewModel.uiState.value.accumulatedStudy
        set(value) {
            if (timerViewModel.uiState.value.accumulatedStudy != value) {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putLong("accumulatedStudy", value).apply()
            }
        }
    internal var currentBreakSeconds: Long
        get() = timerViewModel.uiState.value.currentBreakSeconds
        set(value) {
            if (timerViewModel.uiState.value.currentBreakSeconds != value) {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putLong("currentBreakSeconds", value).apply()
            }
        }

    internal fun resetRunningSessionAccumulators() {
        timerViewModel.resetRunningSessionAccumulators()
    }

    internal var timerMode: String
        get() = timerViewModel.uiState.value.mode
        set(value) {
            timerViewModel.setTimerMode(value)
        }
    internal var focusCountdownSecs: Long
        get() = timerViewModel.uiState.value.focusCountdownSecs
        set(value) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putLong("focusCountdownSecs", value).apply()
        }
    internal var focusRemainingSecs: Long
        get() = timerViewModel.uiState.value.focusRemainingSecs
        set(value) {
            timerViewModel.updateFocusRemaining(value)
        }
    internal var prePauseState: TimerState
        get() = timerViewModel.uiState.value.prePauseState
        set(value) {
            timerViewModel.setPrePauseState(value)
        }

    private var lastKeepScreenOn = -1

    private var pauseBlinkAnimator: ValueAnimator? = null

    internal var isDevModeUnlocked: Boolean
        get() = BuildConfig.DEBUG || getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getBoolean("dev_mode_unlocked", false)
        set(value) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("dev_mode_unlocked", value).apply()
        }
    internal var isAdjustingFocusMode = true
    internal var showFocusHueBar = false
    internal var showBreakHueBar = false
    internal var settingsScrollViewRef: ScrollView? = null
    private var pendingSettingsScrollY = 0

    internal fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    internal fun loadSessionGoalsFromJson(jsonStr: String): List<SessionGoal> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<SessionGoal>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val subId = if (obj.has("subjectId") && !obj.isNull("subjectId")) obj.optString("subjectId").takeIf { it.isNotEmpty() } else null
                val subTopic = if (obj.has("subTopic") && !obj.isNull("subTopic")) obj.optString("subTopic").takeIf { it.isNotEmpty() } else null
                val daysList = mutableListOf<Int>()
                val daysArr = obj.optJSONArray("scheduledDays")
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) {
                        daysList.add(daysArr.getInt(d))
                    }
                }
                list.add(SessionGoal(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    note = obj.optString("note", ""),
                    targetMinutes = obj.optInt("targetMinutes", 0),
                    completed = obj.optBoolean("completed", false),
                    checkedAt = obj.optLong("checkedAt", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    subjectId = subId,
                    reminderEnabled = obj.optBoolean("reminderEnabled", false),
                    reminderHour = obj.optInt("reminderHour", 20),
                    reminderMinute = obj.optInt("reminderMinute", 0),
                    subTopic = subTopic,
                    scheduledDays = daysList
                ))
            }
            list.distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveSessionGoalsToJson(goals: List<SessionGoal>) {
        val array = org.json.JSONArray()
        for (goal in goals) {
            val obj = org.json.JSONObject().apply {
                put("id", goal.id)
                put("title", goal.title)
                put("note", goal.note)
                put("targetMinutes", goal.targetMinutes)
                put("completed", goal.completed)
                put("checkedAt", goal.checkedAt)
                put("createdAt", goal.createdAt)
                if (goal.subjectId != null) {
                    put("subjectId", goal.subjectId)
                }
                put("reminderEnabled", goal.reminderEnabled)
                put("reminderHour", goal.reminderHour)
                put("reminderMinute", goal.reminderMinute)
                if (goal.subTopic != null) {
                    put("subTopic", goal.subTopic)
                }
                if (goal.scheduledDays.isNotEmpty()) {
                    val daysArr = org.json.JSONArray()
                    goal.scheduledDays.forEach { daysArr.put(it) }
                    put("scheduledDays", daysArr)
                }
            }
            array.put(obj)
        }
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putString("session_goals_json", array.toString()).apply()
        BackupManager(this).markDataModified()
        PlannerStateManager.notifyGoalsChanged()
    }

    internal var isLocalPlannerUpdateInProgress = false
    internal var pendingPlannerScrollY = 0

    internal fun findPlannerScrollView(): android.widget.ScrollView? {
        return panelContainer.findViewWithTag<android.widget.ScrollView>("planner_scroll_view")
    }

    private val plannerGoalListener = object : PlannerStateManager.GoalChangeListener {
        override fun onGoalsChanged() {
            runOnUiThread {
                TasksWidgetProvider.refresh(this@MainActivity)
                if (currentPanel == AppPanel.PLANNER) {
                    if (!isLocalPlannerUpdateInProgress) {
                        buildPlannerPanel()
                    } else {
                        updateStatsLiveViews()
                    }
                } else if (currentPanel == AppPanel.STATS && currentStatsTab == AppStatsTab.PLANNER) {
                    refreshStatsPanel()
                } else {
                    updateStatsLiveViews()
                }
            }
        }
    }

    internal fun resolvePlannerColors(): Pair<Int, Int> =
        PlannerThemeHelper.resolvePlannerColors(this, themeCoordinator)

    internal fun resolvePlannerAccentColor(): Int =
        PlannerThemeHelper.resolvePlannerAccentColor(this, themeCoordinator)

    internal fun showPlannerThemePickerDialog() {
        PlannerThemeHelper.showPlannerThemePickerDialog(this, themeCoordinator) {
            refreshStatsPanel()
        }
    }

    private val pickAvatarLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            AvatarCropDialogHelper.showCropDialog(
                activity = this,
                imageUri = uri,
                themeCoordinator = themeCoordinator
            ) { croppedBitmap ->
                val success = LocalAvatarManager.saveCroppedBitmap(this, croppedBitmap)
                if (success) {
                    val isLoggedIn = AuthManager.isLoggedIn(this) && !AuthManager.isGuest(this)
                    if (!isLoggedIn) {
                        // Guest user: save locally only, do not trigger approval workflow
                        LocalAvatarManager.markAvatarPendingUpload(this, false)
                        CoroutineScope(Dispatchers.IO).launch {
                            val current = ProfileManager.getProfile(this@MainActivity)
                            val targetFile = LocalAvatarManager.getAvatarFile(this@MainActivity)
                            ProfileManager.saveProfile(this@MainActivity, current.copy(
                                avatarUrl = targetFile.absolutePath,
                                updatedAt = System.currentTimeMillis()
                            ))
                            withContext(Dispatchers.Main) {
                                tabPageCache.clear()
                                buildCurrentPanel()
                                if (currentPanel == AppPanel.SETTINGS) {
                                    navigateToPanel(AppPanel.SETTINGS)
                                }
                                AppToast.show(this@MainActivity, "Profile picture updated locally. Sign in to submit for public verification!")
                            }
                        }
                        return@showCropDialog
                    }

                    AppToast.show(this, "Profile picture cropped. Submitting for approval...")
                    if (currentPanel == AppPanel.SETTINGS) {
                        navigateToPanel(AppPanel.SETTINGS)
                    }

                    // Strictly route via ProfileSyncService for moderation, storage upload, and Telegram admin dispatch
                    CoroutineScope(Dispatchers.IO).launch {
                        val current = ProfileManager.getProfile(this@MainActivity)
                        ProfileSyncService.submitProfile(
                            context = this@MainActivity,
                            displayName = current.displayName,
                            bio = current.bio,
                            targetExam = current.targetExam,
                            dailyGoalMinutes = current.dailyGoalMinutes,
                            avatarPresetId = current.avatarPresetId,
                            avatarUrl = current.avatarUrl
                        )

                        withContext(Dispatchers.Main) {
                            tabPageCache.clear()
                            buildCurrentPanel()
                            if (currentPanel == AppPanel.SETTINGS) {
                                navigateToPanel(AppPanel.SETTINGS)
                            }
                        }
                    }
                } else {
                    AppToast.show(this, "Could not save cropped image")
                }
            }
        }
    }

    internal var wallpaperImageView: ImageView? = null
    internal var liveWallpaperView: LiveWallpaperView? = null
    internal var wallpaperDimScrimView: View? = null

    private val pickWallpaperLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = WallpaperManager.saveCustomWallpaperFromUri(this, uri)
            if (success) {
                AppToast.show(this, "Wallpaper applied!")
                updateWallpaperLayer()
                updateVisualStyles()
                tabPageCache.clear()
                if (currentPanel == AppPanel.SETTINGS) {
                    navigateToPanel(AppPanel.SETTINGS)
                }
            } else {
                AppToast.show(this, "Failed to load wallpaper image")
            }
        }
    }

    private val pickLiveVideoLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            liveWallpaperView?.release()
            when (WallpaperManager.saveCustomLiveVideoFromUri(this, uri)) {
                WallpaperManager.VideoSaveResult.SUCCESS -> {
                    AppToast.show(this, "Live video wallpaper applied!")
                    updateWallpaperLayer()
                    updateVisualStyles()
                    tabPageCache.clear()
                    if (currentPanel == AppPanel.SETTINGS) {
                        navigateToPanel(AppPanel.SETTINGS)
                    }
                }
                WallpaperManager.VideoSaveResult.EXCEEDS_DURATION -> {
                    AppToast.show(this, "Video exceeds 20-second limit. Please pick a shorter clip.")
                    updateWallpaperLayer()
                }
                WallpaperManager.VideoSaveResult.ERROR -> {
                    AppToast.show(this, "Failed to load video file")
                    updateWallpaperLayer()
                }
            }
        }
    }

    internal fun pickCustomWallpaper() {
        try {
            pickWallpaperLauncher.launch("image/*")
        } catch (_: Exception) {
            AppToast.show(this, "No image picker available")
        }
    }

    internal fun pickLiveVideoWallpaper() {
        try {
            pickLiveVideoLauncher.launch("video/*")
        } catch (_: Exception) {
            AppToast.show(this, "No video picker available")
        }
    }

    internal fun setLiveWallpaper(type: String) {
        WallpaperManager.setWallpaperType(this, type)
        AppToast.show(this, "Live wallpaper applied!")
        updateWallpaperLayer()
        updateVisualStyles()
        tabPageCache.clear()
        if (currentPanel == AppPanel.SETTINGS) {
            navigateToPanel(AppPanel.SETTINGS)
        }
    }

    internal fun showWallpaperGalleryDialog() {
        WallpaperGalleryDialog.show(this, themeCoordinator) {
            updateWallpaperLayer()
            updateVisualStyles()
            tabPageCache.clear()
            if (currentPanel == AppPanel.SETTINGS) {
                navigateToPanel(AppPanel.SETTINGS)
            }
        }
    }

    internal fun updateWallpaperLayer() {
        val type = WallpaperManager.getWallpaperType(this)
        val hasWallpaper = type != WallpaperManager.TYPE_NONE

        if (WallpaperManager.isLiveWallpaper(type)) {
            wallpaperImageView?.visibility = View.GONE
            wallpaperImageView?.setImageDrawable(null)
            liveWallpaperView?.visibility = View.VISIBLE
            liveWallpaperView?.setLiveMode(type)
            liveWallpaperView?.resume()
        } else {
            liveWallpaperView?.pause()
            liveWallpaperView?.visibility = View.GONE

            when (type) {
                WallpaperManager.TYPE_PRESET -> {
                    val presetId = WallpaperManager.getActivePresetId(this)
                    val bm = WallpaperManager.loadPresetBitmap(this, presetId)
                    if (bm != null) {
                        wallpaperImageView?.setImageBitmap(bm)
                        wallpaperImageView?.visibility = View.VISIBLE
                    } else {
                        wallpaperImageView?.setImageDrawable(null)
                        wallpaperImageView?.visibility = View.GONE
                    }
                }
                WallpaperManager.TYPE_STATIC -> {
                    val file = WallpaperManager.getCustomWallpaperFile(this)
                    if (file.exists()) {
                        val bm = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bm != null) {
                            wallpaperImageView?.setImageBitmap(bm)
                            wallpaperImageView?.visibility = View.VISIBLE
                        } else {
                            wallpaperImageView?.setImageDrawable(null)
                            wallpaperImageView?.visibility = View.GONE
                        }
                    } else {
                        wallpaperImageView?.setImageDrawable(null)
                        wallpaperImageView?.visibility = View.GONE
                    }
                }
                else -> {
                    wallpaperImageView?.setImageDrawable(null)
                    wallpaperImageView?.visibility = View.GONE
                }
            }
        }

        // Dynamic Scrim Dimmer for legibility
        if (hasWallpaper) {
            val dimAlpha = WallpaperManager.getWallpaperDimAlpha(this)
            val scrimColor = Color.argb((dimAlpha * 255).toInt(), 0, 0, 0)
            wallpaperDimScrimView?.setBackgroundColor(scrimColor)
            wallpaperDimScrimView?.visibility = View.VISIBLE
        } else {
            wallpaperDimScrimView?.visibility = View.GONE
        }
        themeCoordinator.applyThemeCoordinates()
    }

    internal fun pickProfileAvatar() {
        try {
            pickAvatarLauncher.launch("image/*")
        } catch (_: Exception) {
            AppToast.show(this, "No image picker available")
        }
    }

    internal val accountDialogHelper by lazy { AccountDialogHelper(this) }
    internal val systemDialogHelper by lazy { SystemDialogHelper(this) }

    internal fun showSignOutConfirmDialog(onConfirmed: (() -> Unit)? = null) {
        accountDialogHelper.showSignOutConfirmDialog(onConfirmed)
    }

    internal var statsSnapshotCache: StatsSnapshot? = null
    internal var statsSnapshotGen = 0
    internal var statsDirty = true
    internal var statsInternalRefresh = false
    internal var hasPlayedStatsEntranceAnimation = false
    private var lastStyleKey = ""
    private var lastTickTimerState: TimerState? = null
    private var lastRenderedTimerState: TimerState? = null
    private var lastIsBreakingState: Boolean? = null
    private var lastZenModeState: Boolean? = null
    private var lastShowPauseState: Boolean? = null
    private var lastDayBucket: Long = -1L
    internal var cachedTodayStr = ""
    private var lastStatsMinuteTick: Long = -1L
    private var lastWallMinuteTick: Long = -1L

    internal lateinit var themeCoordinator: ThemeCoordinator
    private lateinit var backupManager: BackupManager
    internal val statsEngine by lazy { StatsEngine(this) }
    internal val dateKeyFmt by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    internal lateinit var rootLayout: FrameLayout
    internal lateinit var panelContainer: LinearLayout

    internal lateinit var statusBadge: TextView
    internal lateinit var studyTimerDisplay: TextView
    internal lateinit var breakTimerDisplay: TextView
    internal lateinit var timerRing: TimerRingView
    internal lateinit var mainBtn: Button
    internal lateinit var pauseBtn: Button
    internal lateinit var stopBtn: HoldRingButton
    internal lateinit var controlActionContainer: LinearLayout
    private lateinit var panelHost: FrameLayout
    internal lateinit var studyBottomNavBar: StudyBottomNavBar
    internal lateinit var navHeader: View
    internal lateinit var statusBadgeContainer: LinearLayout
    internal var landscapeHeaderView: View? = null
    internal var landscapeDockView: View? = null
    internal var landscapeSubjectPillView: View? = null
    internal var landscapeGoalProgressBar: ProgressBar? = null
    internal var landscapeTodayFocusText: TextView? = null
    internal var landscapeGoalPercentText: TextView? = null
    internal var extraControlsContainer: LinearLayout? = null
    internal var gestureHintView: View? = null
    internal var statsFloatingIcon: ImageView? = null
    internal var modeOptionsPill: View? = null
    internal var leaderboardLiveDot: View? = null
    internal var isPortraitFullscreenActive = false
    internal var settingsOriginPanel: AppPanel = AppPanel.FOCUS
    internal var settingsOriginWasSubTab: Boolean = false

    internal fun handleSettingsBack() {
        if (currentSettingsTab == AppSettingsTab.PROFILE) {
            ProfileManager.clearDraftForm(this)
            tabPageCache.remove(settingsTabKey(AppSettingsTab.PROFILE))
        }
        if (settingsOriginWasSubTab) {
            settingsOriginWasSubTab = false
            currentSettingsTab = AppSettingsTab.HUB
            navigateToPanel(settingsOriginPanel)
        } else if (currentSettingsTab != AppSettingsTab.HUB) {
            currentSettingsTab = AppSettingsTab.HUB
            navigateToPanel(AppPanel.SETTINGS)
        } else {
            navigateToPanel(settingsOriginPanel)
        }
    }
    private var lastConfigOrientation = Configuration.ORIENTATION_UNDEFINED
    private var lastStatusBarInset = 0
    private var lastNavBottomInset = 0
    private var lastNavRightInset = 0
    private var lastNavLeftInset = 0
    private var lastIsThreeButtonNav = false
    private var lastFullscreenToggleTime = 0L

    internal fun isLandscapeMode(): Boolean {
        if (lastConfigOrientation == Configuration.ORIENTATION_LANDSCAPE) return true
        if (lastConfigOrientation == Configuration.ORIENTATION_PORTRAIT) return false
        val conf = resources.configuration
        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) return true
        if (conf.orientation == Configuration.ORIENTATION_PORTRAIT) return false
        val dm = resources.displayMetrics
        return dm.widthPixels > dm.heightPixels
    }
    private var lastActionTriggerTime = 0L
    private var isZenModeActive = false
    private var isNavigatingBack = false
    internal val appPrefs by lazy { getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE) }

    internal fun canExecuteAction(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionTriggerTime < 250L) return false
        lastActionTriggerTime = now
        return true
    }
    internal var calendarYear = 0
    internal var calendarMonth = 0
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    internal val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> showRestoreConfirmDialog(uri) }
        }
    }

    internal fun showEditNameDialog() {
        accountDialogHelper.showEditNameDialog()
    }

    internal fun showExpandedAvatarDialog() {
        accountDialogHelper.showExpandedAvatarDialog()
    }

    internal fun showDeleteAccountDialog() {
        accountDialogHelper.showDeleteAccountDialog()
    }

    internal fun showRestoreConfirmDialog(uri: Uri) {
        accountDialogHelper.showRestoreConfirmDialog(uri)
    }

    internal fun showSyncConflictDialog(localTs: Long, cloudTs: Long, cloudRecord: org.json.JSONObject) {
        accountDialogHelper.showSyncConflictDialog(localTs, cloudTs, cloudRecord)
    }

    internal val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = backupManager.exportDataToJSON(uri)
                if (success) {
                    AppToast.show(this, R.string.toast_logs_exported)
                } else {
                    AppToast.show(this, R.string.toast_logs_export_failed)
                }
            }
        }
    }



    internal val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private val HOLD_TO_END_DURATION_MS = 650L
    internal var holdStartTime = 0L
    internal var isHoldingStop = false

    internal var lastHoldHapticMs = 0L

    internal fun performMicroHaptic(view: View? = null) {
        HapticFeedbackHelper.performMicroHaptic(this, view ?: window.decorView)
    }

    internal fun performMicroHapticTick(view: View? = null, progressFraction: Float = 0f) {
        HapticFeedbackHelper.performMicroHapticTick(this, view ?: window.decorView, progressFraction)
    }

    internal fun performHapticConfirm(view: View? = null) {
        HapticFeedbackHelper.performHapticConfirm(this, view ?: window.decorView)
    }

    internal fun resetHoldToEnd() {
        isHoldingStop = false
        if (::stopBtn.isInitialized) {
            stopBtn.isPressed = false
            stopBtn.animateProgressToZero()
        }
        handler.removeCallbacks(holdToEndRunnable)
    }

    internal val holdToEndRunnable = object : Runnable {
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

            val now = SystemClock.uptimeMillis()
            val hapticInterval = (140L - (progress * 105L)).toLong().coerceAtLeast(28L)
            if (now - lastHoldHapticMs >= hapticInterval) {
                lastHoldHapticMs = now
                performMicroHapticTick(stopBtn, progress)
            }

            if (progress >= 1f) {
                resetHoldToEnd()
                performHapticConfirm(stopBtn)
                handleStopSession()
                AppToast.show(this@MainActivity, R.string.toast_session_saved)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    CloudSyncManager.syncDataToCloud(this@MainActivity)
                }
            } else {
                handler.postDelayed(this, 16L)
            }
        }
    }


    internal fun showCustomDialog(
        badge: String? = null,
        title: String,
        message: String,
        positiveText: String,
        onPositive: () -> Unit,
        negativeText: String? = null,
        onNegative: (() -> Unit)? = null
    ) {
        systemDialogHelper.showCustomDialog(badge, title, message, positiveText, onPositive, negativeText, onNegative)
    }

    internal fun maybePromptBatteryOptimization() {
        systemDialogHelper.maybePromptBatteryOptimization()
    }

    internal fun showBatteryOptimizationDialog() {
        systemDialogHelper.showBatteryOptimizationDialog()
    }

    internal fun openBatterySettings() {
        systemDialogHelper.openBatterySettings()
    }

    internal fun openNotificationSettings() {
        systemDialogHelper.openNotificationSettings()
    }

    @Suppress("DEPRECATION")
    internal fun currentVersionCodeLong(): Long = systemDialogHelper.currentVersionCodeLong()

    @Suppress("DEPRECATION")
    internal fun currentVersionName(): String = systemDialogHelper.currentVersionName()

    internal fun openUpdateUrl(url: String) {
        systemDialogHelper.openUpdateUrl(url)
    }

    internal fun checkForUpdates(manual: Boolean) {
        systemDialogHelper.checkForUpdates(manual)
    }

    internal fun showUpdateAvailableDialog(info: UpdateInfo) {
        systemDialogHelper.showUpdateAvailableDialog(info)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
                    .remove("notification_perm_prompt_count")
                    .remove("notif_perm_do_not_ask_again")
                    .apply()
                pendingNotificationAction?.invoke()
            }
            pendingNotificationAction = null
        }
    }

    internal fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    internal fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
    internal fun dpF(value: Float): Float = value * resources.displayMetrics.density

    internal fun lightenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * amount).coerceIn(0f, 1f)
        hsv[1] = (hsv[1] * (1f - amount * 0.45f)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    internal fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    internal fun darkenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    internal fun rippleBackground(color: Int): android.graphics.drawable.Drawable {
        if (themeCoordinator.isBubbleStyle()) {
            return themeCoordinator.createButtonBackground(color)
        }
        val shape = themeCoordinator.createGlowGradient(color, 80f)
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(90, 255, 255, 255)),
            shape,
            shape
        )
    }

    internal fun outlinedButtonBackground(): android.graphics.drawable.Drawable {
        val density = resources.displayMetrics.density
        val isWallpaperActive = WallpaperManager.getWallpaperType(this) != "NONE"
        val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(this)
        val isBrightSurface = isPomodoroPureWhiteActive() || (isWallpaperActive && isBrightUnderTimer) || !themeCoordinator.isDarkMode()

        if (themeCoordinator.isBubbleStyle()) {
            val fill = if (isBrightSurface) 0xF2FFFFFF.toInt() else 0x1AFFFFFF.toInt()
            val stroke = if (isBrightSurface) 0xFFCBD5E1.toInt() else 0x33FFFFFF.toInt()
            return GradientDrawable().apply {
                this.cornerRadius = 80f * density
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
        }
        if (isBrightSurface) {
            return android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x1A0F172A.toInt()),
                GradientDrawable().apply {
                    cornerRadius = 80f * density
                    setColor(0xF5FFFFFF.toInt())
                    setStroke((1.5f * density).toInt(), 0xFFCBD5E1.toInt())
                },
                null
            )
        }
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
            GradientDrawable().apply {
                cornerRadius = 80f * density
                setColor(0x331E293B.toInt())
                setStroke((1.5f * density).toInt(), if (themeCoordinator.isGlassStyle()) tintedColor(themeCoordinator.primaryColor, 120) else 0x44FFFFFF.toInt())
            },
            null
        )
    }

    internal fun applyBubbleTouchAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    (v.background as? Soft3DBubbleDrawable)?.isPressed = true
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    performMicroHaptic(v)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v.background as? Soft3DBubbleDrawable)?.isPressed = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220).setInterpolator(android.view.animation.OvershootInterpolator(1.3f)).start()
                    false
                }
                else -> false
            }
        }
    }

    private fun pauseButtonVisibility(): Int {
        val showPause = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getBoolean("show_pause_button", true)
        return if (showPause) View.VISIBLE else View.GONE
    }

    internal fun formatGoalLabel(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return when {
            h > 0 && m > 0 -> getString(R.string.duration_h_m, h, m)
            h > 0 -> getString(R.string.duration_h, h)
            else -> getString(R.string.duration_m, m)
        }
    }

    internal fun dailyGoalSecs(): Long = statsEngine.dailyGoalSecs()

    internal fun resolveGoalFor(dateStr: String): Long = statsEngine.resolveGoalFor(dateStr)

    internal fun pureWhiteTimerEnabled(): Boolean {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pureWhiteTimer", true) && themeCoordinator.activeBgMode != "LIGHT"
    }

    internal fun isPomodoroPureWhiteActive(): Boolean {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("pomodoro_pure_white_theme", true)
        val hasWallpaper = WallpaperManager.getWallpaperType(this) != "NONE"
        return isEnabled && timerMode == "COUNTDOWN" && !hasWallpaper
    }

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale.coerceIn(0.85f, 1.05f)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            overrideConfiguration.fontScale = overrideConfiguration.fontScale.coerceIn(0.85f, 1.05f)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putString("ui_style", "GLASS")
                .putString("wallpaper_type", WallpaperManager.TYPE_LIVE_AURORA)
                .putInt("customHue", 255)
                .putInt("customPrimary", Color.parseColor("#A78BFA"))
                .putInt("customSecondaryHue", 199)
                .putInt("customSecondary", Color.parseColor("#38BDF8"))
                .putString("timer_mode", "SUBJECT")
                .putBoolean("enable_subject_tagging", true)
                .putBoolean("show_subject_pie_chart", true)
                .putLong("focus_countdown_secs", 3600L)
                .putInt("reminder_hour", 20)
                .putInt("reminder_minute", 0)
                .putBoolean("reminder_enabled", true)
                .putBoolean("festival_greetings_enabled", true)
                .putBoolean("is_landscape_mode_enabled", true)
                .putBoolean("true_fullscreen_landscape", true)
                .putBoolean("keep_screen_on", true)
                .putBoolean("show_pause_button", true)
                .putBoolean("show_focus_heatmap", true)
                .putBoolean("show_focus_pattern", true)
                .putBoolean("pureWhiteTimer", true)
                .apply()
        }

        if (!sharedPrefs.contains("daily_goal_secs") && !sharedPrefs.contains("daily_goal_minutes")) {
            sharedPrefs.edit()
                .putLong("daily_goal_secs", StatsEngine.DEFAULT_DAILY_GOAL_SECS)
                .putInt("daily_goal_minutes", (StatsEngine.DEFAULT_DAILY_GOAL_SECS / 60L).toInt())
                .apply()
        }

        // Auto-heal any preference keys that were stored as Long instead of Int (prevents ClassCastException)
        val intKeysToHeal = setOf(
            "daily_goal_minutes", "customBg", "customPrimary", "customHue", "customSecondary", "customSecondaryHue",
            "current_streak", "selected_days_filter", "reminder_hour", "reminder_minute", "update_launch_count",
            "update_dismissed_version", "battery_opt_opens", "post_notif_rationale_shown", "pomo_completed_count",
            "pomodoro_study_minutes", "pomodoro_break_minutes", "pomo_long_break_interval", "last_celebrated_streak"
        )
        val healEditor = sharedPrefs.edit()
        var needsHeal = false
        for (k in intKeysToHeal) {
            val v = sharedPrefs.all[k]
            if (v is Long) {
                healEditor.putInt(k, v.toInt())
                needsHeal = true
            }
        }
        if (needsHeal) healEditor.commit()

        PlannerStateManager.addListener(plannerGoalListener)

        themeCoordinator = ThemeCoordinator(this)
        backupManager = BackupManager(this)
        themeCoordinator.applyThemeCoordinates()
        statsEngine.sanitizeAndHealHistoricalTotals()
        GoalHistoryManager.reconcileAllHistoricalGoals(this)
        createNotificationChannel()


        Thread {
            try {
                AppAnalytics.init(this)
                backupManager.triggerAutoRestoreIfPresent()
                createNotificationChannel()
                GoalReminderScheduler.schedule(this)
                FestivalScheduler.schedule(this)
                FestivalManager.checkAndNotify(this)
                migrateHistoricalDailyGoals(this)
                checkAndResetGoalsForNewDay()
                triggerAutoSyncIfEligible(force = true)
            } catch (_: Exception) {}
        }.start()

        rootLayout = FrameLayout(this).apply {
            background = themeCoordinator.createBackgroundDrawable()
        }

        liveWallpaperView = LiveWallpaperView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        rootLayout.addView(liveWallpaperView)

        wallpaperImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        rootLayout.addView(wallpaperImageView)

        wallpaperDimScrimView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        rootLayout.addView(wallpaperDimScrimView)

        updateWallpaperLayer()

        panelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        panelHost = FrameLayout(this).apply {
            setPadding(dp(16), dp(4), dp(16), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        panelHost.addView(panelContainer)
        rootLayout.addView(panelHost)

        val isInitialLandscape = isLandscapeMode()
        studyBottomNavBar = StudyBottomNavBar(this).apply {
            applyTheme(themeCoordinator)
            visibility = if (isInitialLandscape) View.GONE else View.VISIBLE
            onTabSelectedListener = { targetPanel ->
                navigateToPanel(targetPanel)
            }
        }
        val navParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(58),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            setMargins(dp(20), 0, dp(20), dp(16))
        }
        rootLayout.addView(studyBottomNavBar, navParams)

        setContentView(rootLayout)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBarInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            val navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            val tappableInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.tappableElement())
            val navBottom = navInsets.bottom
            val tappableBottom = tappableInsets.bottom

            lastStatusBarInset = statusBarInset
            lastNavBottomInset = navBottom
            lastNavRightInset = navInsets.right
            lastNavLeftInset = navInsets.left
            lastIsThreeButtonNav = (tappableBottom > 0 && navBottom > dp(28)) || navBottom > dp(36)

            applyLayoutInsetsForOrientation(resources.configuration.orientation)
            insets
        }
        lastConfigOrientation = resources.configuration.orientation
        updateStatusBarIcons()

        tabDragSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        currentTimerState = runCatching { TimerState.valueOf(sharedPrefs.safeString("timerState", "IDLE") ?: "IDLE") }.getOrDefault(TimerState.IDLE)
        accumulatedStudy = sharedPrefs.safeLong("accumulatedStudy", 0L)
        currentBreakSeconds = sharedPrefs.safeLong("currentBreakSeconds", 0L)
        selectedDaysFilter = sharedPrefs.safeInt("selected_days_filter", 7)

        // Self-heal corrupted 0-second PAUSED session to clean IDLE
        if (currentTimerState == TimerState.PAUSED && accumulatedStudy == 0L && currentBreakSeconds == 0L) {
            currentTimerState = TimerState.IDLE
            sharedPrefs.edit()
                .putString("timerState", TimerState.IDLE.name)
                .putLong("lastTimestamp", 0L)
                .apply()
        }

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
        if (intent?.getBooleanExtra("EXTRA_OPEN_PLANNER", false) == true) {
            intent?.removeExtra("EXTRA_OPEN_PLANNER")
            navigateToPanel(AppPanel.PLANNER)
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var lastBackTime = 0L
            override fun handleOnBackPressed() {
                if (isPortraitFullscreenActive) {
                    exitPortraitFullscreenMode()
                    return
                }
                if (currentPanel == AppPanel.HEATMAP) {
                    // Pop from full-screen Heatmap back to Insights
                    navigateToPanel(AppPanel.STATS)
                } else if (currentPanel == AppPanel.STATS && currentStatsTab == AppStatsTab.TIMELINE) {
                    // Return from Detailed Calendar History back to Stats Overview
                    currentStatsTab = AppStatsTab.OVERVIEW
                    navigateToPanel(AppPanel.STATS)
                } else if (currentPanel == AppPanel.SETTINGS) {
                    handleSettingsBack()
                } else if (currentPanel != AppPanel.FOCUS) {
                    // Return to Root Start Destination (Timer/Focus)
                    navigateToPanel(AppPanel.FOCUS)
                } else {
                    // Primary Root Anchor (Timer / Focus Screen): Trigger exit confirmation / double-tap to exit
                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < 2000) {
                        finish()
                    } else {
                        lastBackTime = now
                        if (currentTimerState != TimerState.IDLE) {
                            AppToast.show(this@MainActivity, "Timer is active. Press back again to exit.")
                        } else {
                            AppToast.show(this@MainActivity, R.string.toast_press_back_again)
                        }
                    }
                }
            }
        })

        if (intent?.getBooleanExtra("NOTIFICATION_TOGGLE_TRIGGER", false) == true) {
            handleStateToggle()
        }
        if (intent?.getBooleanExtra(TimerService.EXTRA_SWITCH_TO_LECTURE, false) == true) {
            intent?.removeExtra(TimerService.EXTRA_SWITCH_TO_LECTURE)
            showSwitchToLectureDialog()
        }

        applyImmersiveModeForLandscape()

        // Set has_seen_app_guide true so it doesn't interrupt first launch
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_seen_app_guide", false)) {
            prefs.edit().putBoolean("has_seen_app_guide", true).apply()
        }

        // Trigger FTUX interactive 3-step setup on first launch
        if (!prefs.getBoolean("has_completed_onboarding_v2", false)) {
            window.decorView.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    OnboardingDialogHelper.showOnboardingIfNeeded(this)
                }
            }, 500L)
        }
    }



    internal fun applyLayoutInsetsForOrientation(orientation: Int = lastConfigOrientation) {
        if (!::panelHost.isInitialized) return
        val isLandscape = isLandscapeMode()
        if (isLandscape) {
            if (::studyBottomNavBar.isInitialized) {
                studyBottomNavBar.animate().cancel()
                studyBottomNavBar.visibility = View.GONE
            }
            // In landscape, remove top and bottom padding entirely.
            // Only protect against side navigation bars/cutouts if present.
            val sidePaddingLeft = if (lastNavLeftInset > 0) lastNavLeftInset else 0
            val sidePaddingRight = if (lastNavRightInset > 0) lastNavRightInset else 0
            panelHost.setPadding(sidePaddingLeft, 0, sidePaddingRight, 0)
        } else {
            val bottomNavMargin = if (lastIsThreeButtonNav) {
                lastNavBottomInset + dp(10)
            } else {
                dp(16)
            }
            if (::studyBottomNavBar.isInitialized) {
                (studyBottomNavBar.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                    if (lp.bottomMargin != bottomNavMargin) {
                        lp.bottomMargin = bottomNavMargin
                        studyBottomNavBar.layoutParams = lp
                    }
                }
                studyBottomNavBar.animate().cancel()
                studyBottomNavBar.translationY = 0f
                studyBottomNavBar.alpha = 1f
                val shouldHide = (currentPanel == AppPanel.SETTINGS || currentPanel == AppPanel.HEATMAP || isPortraitFullscreenActive)
                studyBottomNavBar.visibility = if (shouldHide) View.GONE else View.VISIBLE
            }
            val panelBottomPadding = if (lastIsThreeButtonNav) {
                lastNavBottomInset + dp(76)
            } else {
                dp(16)
            }
            panelHost.setPadding(dp(16), lastStatusBarInset + dp(4), dp(16), panelBottomPadding)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.fontScale = newConfig.fontScale.coerceIn(0.85f, 1.05f)
        super.onConfigurationChanged(newConfig)
        val orientationChanged = (newConfig.orientation != lastConfigOrientation)
        lastConfigOrientation = newConfig.orientation

        val isLandscape = isLandscapeMode()

        applyLayoutInsetsForOrientation(newConfig.orientation)

        if (orientationChanged) {
            isPortraitFullscreenActive = false
            if (isLandscape && currentPanel == AppPanel.FOCUS) {
                applyImmersiveModeForLandscape()
            } else {
                showSystemUI()
            }
            if (::panelContainer.isInitialized) {
                panelContainer.animate().cancel()
                panelContainer.alpha = 0f
            }
            buildCurrentPanel()
            updateVisualStyles()
            if (::panelContainer.isInitialized) {
                panelContainer.animate()
                    .alpha(1f)
                    .setDuration(220L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        } else {
            if (isLandscape && currentPanel == AppPanel.FOCUS) {
                applyImmersiveModeForLandscape()
            } else if (isPortraitFullscreenActive && currentPanel == AppPanel.FOCUS) {
                hideSystemUI()
            }
        }
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
        if (intent?.getBooleanExtra(TimerService.EXTRA_SWITCH_TO_LECTURE, false) == true) {
            intent?.removeExtra(TimerService.EXTRA_SWITCH_TO_LECTURE)
            showSwitchToLectureDialog()
        }
        if (intent?.getBooleanExtra("EXTRA_PROMPT_CHANGE_SUBJECT", false) == true) {
            checkPomoSubjectPrompt()
        }
        if (intent?.getBooleanExtra("EXTRA_OPEN_PLANNER", false) == true) {
            intent?.removeExtra("EXTRA_OPEN_PLANNER")
            navigateToPanel(AppPanel.PLANNER)
        }
    }

    private fun showSwitchToLectureDialog() {
        if (isFinishing || isDestroyed) return
        if (switchLectureDialog?.isShowing == true) return

        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("pending_switch_to_lecture").apply()

        val title = prefs.getString("pending_lecture_title", "") ?: ""
        val startTime = prefs.getString("pending_lecture_start", "") ?: ""
        val endTime = prefs.getString("pending_lecture_end", "") ?: ""
        val remainingSecs = prefs.getLong("pending_lecture_remaining_secs", 0L)
        val skipKey = prefs.getString("pending_lecture_skip_key", null)
        if (title.isEmpty() || remainingSecs <= 0L) return

        if (lectureEndDialog?.isShowing == true) {
            lectureEndDialog?.dismiss()
            lectureEndDialog = null
        }

        ongoingLectureDialogShowing = true
        val remainingStr = if (remainingSecs >= 3600) "${remainingSecs / 3600}h ${(remainingSecs % 3600) / 60}m" else "${(remainingSecs % 3600) / 60}m"

        val dialog = Dialog(this)
        switchLectureDialog = dialog
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(28f)
        }

        root.addView(TextView(this).apply {
            text = "🎓 Scheduled Class Starting"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Class \"$title\" ($startTime – $endTime) is now in progress.\nRemaining: $remainingStr\n\nSwitch timer to lecture mode? Your current session data will be saved."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13f
            setPadding(0, dp(10), 0, dp(18))
        })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(TextView(this).apply {
            text = "Keep Current"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener {
                // Mark skipped so service doesn't ask again for this lecture slot
                if (skipKey != null) prefs.edit().putBoolean(skipKey, true).apply()
                prefs.edit().remove("pending_switch_to_lecture").apply()
                ongoingLectureDialogShowing = false
                dialog.dismiss()
            }
        })
        btnRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
        btnRow.addView(TextView(this).apply {
            text = "Switch to Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f)
            setOnClickListener {
                ongoingLectureDialogShowing = false
                dialog.dismiss()
                // Mark skipKey so service knows we handled this lecture
                if (skipKey != null) prefs.edit().putBoolean(skipKey, true).apply()
                prefs.edit().remove("pending_switch_to_lecture").apply()
                val pendingSubId = prefs.getString("pending_lecture_subject_id", null)
                val nowSecs = System.currentTimeMillis() / 1000
                prefs.edit()
                    .putString("timer_mode", "LECTURE")
                    .putBoolean("lecture_mode_enabled", true)
                    .putString("timerState", "STUDYING")
                    .putLong("focus_remaining_secs", remainingSecs)
                    .putLong("focusRemainingSecs", remainingSecs)
                    .putLong("focus_countdown_secs", remainingSecs)
                    .putLong("focusCountdownSecs", remainingSecs)
                    .putLong("lastTimestamp", nowSecs)
                    .putLong("accumulatedStudy", 0L)
                    .apply()

                if (!pendingSubId.isNullOrEmpty()) {
                    prefs.edit().putString("active_lecture_subject_id", pendingSubId).apply()
                    SubjectTagManager.setSelectedSubject(this@MainActivity, pendingSubId)
                }

                currentTimerState = TimerState.STUDYING
                timerMode = "LECTURE"
                focusRemainingSecs = remainingSecs
                focusCountdownSecs = remainingSecs

                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    action = TimerService.ACTION_START_LECTURE
                    putExtra(TimerService.EXTRA_LECTURE_TITLE, title)
                    putExtra(TimerService.EXTRA_LECTURE_SUBJECT_ID, pendingSubId ?: "")
                    putExtra(TimerService.EXTRA_LECTURE_REMAINING_SECS, remainingSecs)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                if (::updateRunnable.isInitialized) updateRunnable.run()
            }
        })

        root.addView(btnRow)
        dialog.setOnDismissListener {
            ongoingLectureDialogShowing = false
            if (switchLectureDialog === dialog) {
                switchLectureDialog = null
            }
        }
        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }


    private fun handleTabDragTouch(ev: MotionEvent): Boolean {
        tabDragArmed = false
        tabDragActive = false
        return false
    }

    internal fun statsTabKey(t: AppStatsTab): String = "S:${t.ordinal}"
    internal fun settingsTabKey(t: AppSettingsTab): String = "ST:${t.ordinal}"

    private fun tabThemeSig(): String {
        val plannerPreset = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("planner_theme_preset", "DEFAULT") ?: "DEFAULT"
        return "${themeCoordinator.primaryColor}|${themeCoordinator.secondaryColor}|${themeCoordinator.bgColor}|${themeCoordinator.uiStyle}|${themeCoordinator.activeBgMode}|$plannerPreset"
    }

    private fun getOrBuildTabPage(key: String): View {
        val cached = tabPageCache[key]
        if (cached != null) {
            val valid = (cached.themeSig == tabThemeSig()) && (key.startsWith("ST:") || cached.statsGen == statsSnapshotGen)
            if (valid && cached.view.parent == null) return cached.view
        }
        val scratch = FrameLayout(this)
        val page = when {
            key.startsWith("ST:") -> {
                val prev = currentSettingsTab
                currentSettingsTab = when (key) {
                    settingsTabKey(AppSettingsTab.THEME) -> AppSettingsTab.THEME
                    settingsTabKey(AppSettingsTab.PROFILE) -> AppSettingsTab.PROFILE
                    settingsTabKey(AppSettingsTab.COMMUNITY) -> AppSettingsTab.COMMUNITY
                    settingsTabKey(AppSettingsTab.TIMER) -> AppSettingsTab.TIMER
                    settingsTabKey(AppSettingsTab.PLANNER) -> AppSettingsTab.PLANNER
                    settingsTabKey(AppSettingsTab.ANALYTICS) -> AppSettingsTab.ANALYTICS
                    settingsTabKey(AppSettingsTab.CLOUD) -> AppSettingsTab.CLOUD
                    settingsTabKey(AppSettingsTab.HUB) -> AppSettingsTab.HUB
                    else -> AppSettingsTab.HUB
                }
                buildSettingsPanel(scratch, captureScrollRef = false)
                currentSettingsTab = prev
                scratch.getChildAt(0)
            }
            else -> {
                val prev = currentStatsTab
                currentStatsTab = when (key) {
                    statsTabKey(AppStatsTab.TIMELINE) -> AppStatsTab.TIMELINE
                    statsTabKey(AppStatsTab.PLANNER) -> AppStatsTab.PLANNER
                    else -> AppStatsTab.OVERVIEW
                }
                buildStatsPanel(scratch)
                currentStatsTab = prev
                scratch.getChildAt(0)
            }
        }
        scratch.removeView(page)
        FontStyleManager.applyRecursively(page)
        tabPageCache[key] = CachedTabPage(page, if (key.startsWith("ST:")) 0 else statsSnapshotGen, tabThemeSig())
        return page
    }

    private val prewarmHandler = Handler(Looper.getMainLooper())
    private val prewarmRunnable = Runnable { prewarmTabPages() }

    private fun schedulePrewarmTabPages() {
        prewarmHandler.removeCallbacks(prewarmRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Looper.myQueue().addIdleHandler {
                if (!isFinishing && !isDestroyed) {
                    prewarmTabPages()
                }
                false
            }
        } else {
            prewarmHandler.postDelayed(prewarmRunnable, 400L)
        }
    }

    private fun prewarmTabPages() {
        if (panelHost.childCount > 1 || tabDragSettling) return
        when (currentPanel) {
            AppPanel.SETTINGS -> {
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.HUB))
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.THEME))
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.COMMUNITY))
            }
            else -> {}
        }
    }

    internal fun buildCurrentPanel() {
        panelContainer.alpha = 1f
        panelContainer.translationX = 0f
        panelContainer.removeAllViews()
        if (currentPanel == AppPanel.FOCUS && isPomodoroPureWhiteActive()) {
            rootLayout.setBackgroundColor(0xFFFFFFFF.toInt())
        } else {
            rootLayout.background = themeCoordinator.createBackgroundDrawable()
        }
        when (currentPanel) {
            AppPanel.FOCUS -> buildFocusPanel()
            AppPanel.PLANNER -> buildPlannerPanel()
            AppPanel.STATS -> buildStatsPanel()
            AppPanel.COMMUNITY -> buildCommunityPanel()
            AppPanel.SETTINGS -> buildSettingsPanel()
            AppPanel.HEATMAP -> buildHeatmapFullscreenPanel()
            AppPanel.LEADERBOARD -> buildCommunityPanel()
        }
        FontStyleManager.applyRecursively(panelContainer)
        if (::studyBottomNavBar.isInitialized) {
            studyBottomNavBar.applyTheme(themeCoordinator)
        }
        schedulePrewarmTabPages()
        updateStatusBarIcons()
    }

    internal fun buildPlannerPanel(target: android.view.ViewGroup = panelContainer) {
        val existingScroll = target.findViewWithTag<android.widget.ScrollView>("planner_scroll_view")
        if (existingScroll != null && existingScroll.scrollY > 0) {
            pendingPlannerScrollY = existingScroll.scrollY
        }
        PlannerPanelBuilder(this).build(target)
    }

    private fun buildCommunityPanel(target: android.view.ViewGroup = panelContainer) {
        CommunityPanelBuilder(this).build(target)
    }

    internal fun navigateToPanel(targetPanel: AppPanel) {
        isNavigatingBack = false

        if (::studyBottomNavBar.isInitialized) {
            val bottomNavPanel = when (targetPanel) {
                AppPanel.LEADERBOARD -> AppPanel.COMMUNITY
                else -> targetPanel
            }
            studyBottomNavBar.selectTab(bottomNavPanel, animated = true)

            val isLandscape = isLandscapeMode()
            val shouldHide = isLandscape ||
                targetPanel == AppPanel.SETTINGS ||
                targetPanel == AppPanel.HEATMAP ||
                isPortraitFullscreenActive

            studyBottomNavBar.visibility = if (shouldHide) View.GONE else View.VISIBLE
        }

        if (isPortraitFullscreenActive && targetPanel != AppPanel.FOCUS) {
            exitPortraitFullscreenMode()
        }

        if (targetPanel == AppPanel.STATS && currentPanel != AppPanel.STATS) {
            currentStatsTab = AppStatsTab.OVERVIEW
            hasPlayedStatsEntranceAnimation = false
        }
        statsInternalRefresh = (targetPanel == AppPanel.STATS && currentPanel == AppPanel.STATS)

        if (currentPanel == AppPanel.STATS && targetPanel != AppPanel.STATS && targetPanel != AppPanel.HEATMAP) {
            hasPlayedStatsEntranceAnimation = false
        }

        if (currentPanel == AppPanel.SETTINGS && targetPanel != AppPanel.SETTINGS) {
            CoroutineScope(Dispatchers.IO).launch {
                backupManager.runSilentAutoBackup()
            }
        }

        if (currentPanel == AppPanel.COMMUNITY && targetPanel != AppPanel.COMMUNITY) {
            CommunityPanelBuilder.cancelActivePolling()
        }

        if (targetPanel == AppPanel.HEATMAP && currentPanel != AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (targetPanel != AppPanel.HEATMAP && currentPanel == AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (currentPanel != AppPanel.SETTINGS && targetPanel == AppPanel.SETTINGS) {
            settingsOriginPanel = currentPanel
            settingsOriginWasSubTab = (currentSettingsTab != AppSettingsTab.HUB)
        }

        val isSamePanel = targetPanel == currentPanel
        val prevPanel = currentPanel
        currentPanel = targetPanel

        if (isSamePanel || panelContainer.childCount == 0) {
            buildCurrentPanel()
            return
        }

        fun getPanelNavIndex(panel: AppPanel): Int = when (panel) {
            AppPanel.FOCUS -> 0
            AppPanel.PLANNER -> 1
            AppPanel.STATS -> 2
            AppPanel.COMMUNITY -> 3
            AppPanel.SETTINGS -> 4
            AppPanel.HEATMAP -> 2
            AppPanel.LEADERBOARD -> 3
        }

        val movingRight = getPanelNavIndex(targetPanel) > getPanelNavIndex(prevPanel)
        val exitDir = if (movingRight) -1 else 1

        performSlidingTransition(exitDir) {
            buildCurrentPanel()
        }
    }

    private fun performSlidingTransition(exitDir: Int, rebuild: () -> Unit) {
        panelContainer.animate().cancel()
        val slideDist = dp(20).toFloat()
        panelContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        panelContainer.animate()
            .alpha(0f)
            .translationX(exitDir * slideDist)
            .setDuration(70L)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.4f))
            .withEndAction {
                rebuild()
                panelContainer.alpha = 0f
                panelContainer.translationX = -exitDir * slideDist
                panelContainer.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(160L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f))
                    .withEndAction {
                        panelContainer.alpha = 1f
                        panelContainer.translationX = 0f
                        panelContainer.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                    .start()
            }
            .start()
    }

    private fun buildFocusPanel() {
        panelContainer.removeAllViews()
        FocusPanelBuilder(this).build()
    }

    internal fun recalculateStreak(todayExtra: Long = 0L) {
        statsEngine.recalculateStreak(todayExtra)
    }

    internal fun computeStatsSnapshot(): StatsSnapshot {
        val cached = statsSnapshotCache
        if (!statsDirty && cached != null) {
            return cached
        }
        val snap = statsEngine.computeStatsSnapshot(currentBreakSeconds)
        statsSnapshotCache = snap
        statsDirty = false
        return snap
    }

    private fun buildStatsPanel(target: android.view.ViewGroup = panelContainer) {
        StatsPanelBuilder(this).build(target)
    }

    internal fun renderStatsContent(statsRoot: FrameLayout, snap: StatsSnapshot, tab: AppStatsTab = currentStatsTab) {
        StatsPanelBuilder(this).renderStatsContent(statsRoot, snap, tab)
    }

    internal fun buildHeatmapData(): Map<String, Long> = statsEngine.buildHeatmapData()

    internal fun resolveSubjectGoalFor(subjectId: String, dateStr: String): Long {
        return StatsPanelBuilder(this).resolveSubjectGoalFor(subjectId, dateStr)
    }

    internal fun buildHeatmapFullscreenPanel() {
        StatsPanelBuilder(this).buildHeatmapFullscreenPanel()
    }

    internal fun invalidateStatsCache() {
        StatsPanelBuilder(this).invalidateStatsCache()
    }

    internal fun refreshStatsPanel() {
        StatsPanelBuilder(this).refreshStatsPanel()
    }

    internal fun checkAndResetGoalsForNewDay() {
        StatsPanelBuilder(this).checkAndResetGoalsForNewDay()
    }

    internal fun renderPlannerTabContent(parent: LinearLayout, snap: StatsSnapshot) {
        PlannerPanelBuilder(this).renderPlannerTabContent(parent, snap)
    }

    internal fun showGoalHistoryDialog(goal: PlannerGoal, displayMonthOffset: Int = 0) {
        PlannerPanelBuilder(this).showGoalHistoryDialog(goal, displayMonthOffset)
    }

    internal fun showEditYesterdayGoalsDialog() {
        PlannerPanelBuilder(this).showEditYesterdayGoalsDialog()
    }

    internal fun migrateHistoricalDailyGoals(context: Context) {
        PlannerPanelBuilder(this).migrateHistoricalDailyGoals(context)
    }

    internal fun showPlannerMatrixDialog(startFullscreen: Boolean = false) {
        PlannerPanelBuilder(this).showPlannerMatrixDialog(startFullscreen)
    }

    internal fun showFeedbackReportDialog() {
        PlannerPanelBuilder(this).showFeedbackReportDialog()
    }

    internal fun showAppGuideDialog() {
        PlannerPanelBuilder(this).showAppGuideDialog()
    }

    internal fun showDeletePlannerGoalMatrixDialog(goalId: String, goalTitle: String, onDeleted: () -> Unit) {
        PlannerPanelBuilder(this).showDeletePlannerGoalMatrixDialog(goalId, goalTitle, onDeleted)
    }

    internal fun showAddSessionGoalDialog() {
        PlannerPanelBuilder(this).showAddSessionGoalDialog()
    }

    internal fun showEditSessionGoalDialog(goal: PlannerGoal) {
        PlannerPanelBuilder(this).showEditSessionGoalDialog(goal)
    }

    internal fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return PlannerPanelBuilder(this).loadLectureSchedulesFromJson(jsonStr)
    }

    internal fun saveLectureSchedulesToJson(items: List<LectureScheduleItem>) {
        PlannerPanelBuilder(this).saveLectureSchedulesToJson(items)
    }

    internal fun showLectureScheduleManagerDialog() {
        PlannerPanelBuilder(this).showLectureScheduleManagerDialog()
    }

    internal fun showAddLectureScheduleDialog(editItem: LectureScheduleItem? = null) {
        PlannerPanelBuilder(this).showAddLectureScheduleDialog(editItem)
    }

    internal fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {

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
            text = getString(R.string.btn_cancel)
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener { dialog.dismiss() }
        })
        buttonRow.addView(Button(this).apply {
            text = getString(R.string.btn_delete)
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

    internal fun createSectionLabel(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(16), 0, dp(8))
        }
    }

    internal fun createSettingsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(4)) }
        }
    }

    internal inner class BarTrackView(
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
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val trackRect = RectF()
        private val fillRect = RectF()
        private var progress = 0f
        private val maxFillRatio = ratio.coerceIn(0f, 1f)
        private val fillShadowColor = Color.argb(150, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
        private val todayStrokeColor = Color.argb(180, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
        private val tickColor = Color.argb(120, 255, 255, 255)
        private var gradientShader: LinearGradient? = null

        init {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (!hasPlayedStatsEntranceAnimation) {
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 700
                anim.interpolator = android.view.animation.DecelerateInterpolator()
                anim.addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                anim.start()
            } else {
                progress = 1f
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0) {
                gradientShader = LinearGradient(0f, 0f, w.toFloat(), 0f, fillStart, fillEnd, Shader.TileMode.CLAMP)
                fillPaint.shader = gradientShader
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = h / 2f

            trackRect.set(0f, 0f, w, h)
            paint.style = Paint.Style.FILL
            paint.color = trackColor
            canvas.drawRoundRect(trackRect, radius, radius, paint)

            val fillW = w * maxFillRatio * progress
            if (fillW > 0f) {
                fillPaint.setShadowLayer(dp(7).toFloat(), 0f, 0f, fillShadowColor)
                if (fillW >= radius * 2f) {
                    fillRect.set(0f, 0f, fillW, h)
                    canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
                } else {
                    canvas.save()
                    canvas.clipRect(0f, 0f, fillW, h)
                    fillRect.set(0f, 0f, w, h)
                    canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
                    canvas.restore()
                }
                fillPaint.setShadowLayer(0f, 0f, 0f, 0)
            }

            if (goalRatio > 0f) {
                val gx = w * goalRatio
                tickPaint.color = tickColor
                canvas.drawRect(gx - dp(1).toFloat(), 0f, gx + dp(1).toFloat(), h, tickPaint)
            }

            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2) * 0.75f
                paint.color = todayStrokeColor
                canvas.drawRoundRect(trackRect, radius, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    internal inner class SegmentRing(
        private val segments: List<Pair<Float, Int>>,
        private val trackColor: Int,
        private val strokeWidth: Int,
        private val gradient: Pair<Int, Int>?,
        private val animate: Boolean = false,
        private val progressSupplier: (() -> Float)? = null
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arcRect = RectF()
        private var progress = if (animate && progressSupplier == null) 0f else 1f
        private var gradientShader: LinearGradient? = null
        private var ringAnimator: ValueAnimator? = null

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.strokeCap = if (segments.size <= 1) Paint.Cap.ROUND else Paint.Cap.BUTT
            if (animate && progressSupplier == null) {
                ringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 600
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener {
                        progress = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            } else if (progressSupplier == null) {
                progress = 1f
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            ringAnimator?.cancel()
            ringAnimator = null
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0) {
                val cx = w / 2f
                val cy = h / 2f
                val r = min(w.toFloat(), h.toFloat()) / 2f - strokeWidth / 2f
                arcRect.set(cx - r, cy - r, cx + r, cy + r)
                if (gradient != null && segments.size == 1) {
                    gradientShader = LinearGradient(cx - r, cy - r, cx + r, cy + r, gradient.first, gradient.second, Shader.TileMode.CLAMP)
                }
            }
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
            if (r <= 0f) return

            paint.shader = null
            paint.color = trackColor
            canvas.drawCircle(cx, cy, r, paint)

            val curProgress = progressSupplier?.invoke() ?: progress
            var startAngle = -90f
            for ((frac, color) in segments) {
                val f = frac.coerceIn(0f, 1f)
                if (f <= 0f) continue
                val sweep = f * 360f * curProgress
                if (gradientShader != null) {
                    paint.shader = gradientShader
                } else {
                    paint.shader = null
                    paint.color = color
                }
                canvas.drawArc(arcRect, startAngle, sweep, false, paint)
                startAngle += sweep
                if (segments.size > 1) startAngle += 3f
            }
        }
    }


    internal fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> = DayTimelineDialogHelper(this).dayBlocks(dateStr)
    internal fun reconcileDayTotals(dateStr: String) = DayTimelineDialogHelper(this).reconcileDayTotals(dateStr)
    internal fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long = DayTimelineDialogHelper(this).msForDateAndTime(dateStr, h, m)
    internal fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) = DayTimelineDialogHelper(this).applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
    internal fun showBlockEditDialog(dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) = DayTimelineDialogHelper(this).showBlockEditDialog(dateStr, block, isBreak, onApplied)
    internal fun showDevTimelineEditor() = DayTimelineDialogHelper(this).showDevTimelineEditor()
    internal fun confirmDeleteBlock(dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) = DayTimelineDialogHelper(this).confirmDeleteBlock(dateStr, block, isBreak, onDone)
    internal fun confirmDeleteDay(dateStr: String, label: String) = DayTimelineDialogHelper(this).confirmDeleteDay(dateStr, label)
    internal fun showDayDialog(dateStr: String, label: String) = DayTimelineDialogHelper(this).showDayDialog(dateStr, label)
    internal fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) = DayTimelineDialogHelper(this).showMonthDialog(mName, focusSecs, breakSecs)



    internal fun showCustomizeHighlightsDialog() {
        WeeklySummaryShareHelper(this).showCustomizeHighlightsDialog()
    }

    internal fun showWeeklyTrendDetailDialog(thisWeek: Long, prevWeek: Long, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showWeeklyTrendDetailDialog(thisWeek, prevWeek, snap)
    }

    internal fun showActiveDaysDetailDialog(activeDays: Int, longestStreak: Int, currentStreak: Int, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showActiveDaysDetailDialog(activeDays, longestStreak, currentStreak, snap)
    }

    internal fun showBestDayDetailDialog(bestDayLabel: String, bestDaySecs: Long, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showBestDayDetailDialog(bestDayLabel, bestDaySecs, snap)
    }

    internal fun showRecordWeekDetailDialog(bestWeekLabel: String, bestWeekSecs: Long, longestStreak: Int, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showRecordWeekDetailDialog(bestWeekLabel, bestWeekSecs, longestStreak, snap)
    }

    internal fun showGoalSuccessDetailDialog(snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showGoalSuccessDetailDialog(snap)
    }

    internal fun showAvgSessionDetailDialog(snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showAvgSessionDetailDialog(snap)
    }

    internal fun showStudyScorecardExportDialog() {
        val snap = statsSnapshotCache ?: computeStatsSnapshot()
        StudyScorecardExportHelper(this).showExportDialog(snap)
    }

    internal fun startFocusForGoal(goal: SessionGoal) {
        val sp = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val goalSubId = goal.subjectId?.takeIf { it.isNotBlank() && it != "all" } ?: "general"
        val goalSubTopic = goal.subTopic?.takeIf { it.isNotBlank() }
        val currentSub = SubjectTagManager.getSelectedSubject(this)
        val currentSubTopic = SubjectTagManager.getSelectedSubTopic(this, currentSub.id)
        val activeGoalId = sp.getString("active_planner_goal_id", null)

        val isSessionActive = currentTimerState == TimerState.STUDYING ||
            currentTimerState == TimerState.BREAK ||
            currentTimerState == TimerState.PAUSED

        val isSameSubjectAndTopic = (activeGoalId == goal.id) ||
            (currentSub.id == goalSubId && (currentSubTopic ?: "") == (goalSubTopic ?: ""))

        if (isSessionActive) {
            if (isSameSubjectAndTopic) {
                // Same subject and topic/goal: continue seamlessly without overriding pre-existing time!
                sp.edit()
                    .putString("active_planner_goal_id", goal.id)
                    .putString("active_planner_goal_title", goal.title)
                    .putString("active_planner_goal_subject_id", if (goalSubId != "general") goalSubId else "")
                    .putString("active_planner_goal_subtopic", goalSubTopic ?: "")
                    .putInt("active_planner_goal_target_mins", goal.targetMinutes)
                    .apply()

                navigateToPanel(AppPanel.FOCUS)
                if (currentTimerState == TimerState.PAUSED) {
                    handleStateToggle()
                    AppToast.show(this, "Resuming goal: ${goal.title}")
                } else {
                    AppToast.show(this, "Continuing goal: ${goal.title}")
                }
                return
            } else {
                // Different subject/topic running: verify first before switching so no time is lost or overridden!
                DeveloperToolsHelper.showThemedConfirmDialog(
                    activity = this,
                    themeCoordinator = themeCoordinator,
                    title = "Switch Study Goal?",
                    message = "You have an active session for '${currentSub.name}'. Would you like to save and finish it to start '${goal.title}'?",
                    confirmText = "Switch & Save",
                    isDestructive = false,
                    onCancel = {}
                ) {
                    handleStopSession()
                    handler.postDelayed({
                        startFocusForGoal(goal)
                    }, 350)
                }
                return
            }
        }

        sp.edit()
            .putString("active_planner_goal_id", goal.id)
            .putString("active_planner_goal_title", goal.title)
            .putString("active_planner_goal_subject_id", if (goalSubId != "general") goalSubId else "")
            .putString("active_planner_goal_subtopic", goalSubTopic ?: "")
            .putInt("active_planner_goal_target_mins", goal.targetMinutes)
            .apply()

        if (goalSubId != "general") {
            SubjectTagManager.setSelectedSubject(this, goalSubId)
            SubjectTagManager.setSelectedSubTopic(this, goalSubId, goalSubTopic)
        } else {
            SubjectTagManager.setSelectedSubject(this, "general")
            SubjectTagManager.setSelectedSubTopic(this, "general", null)
        }

        // In countdown mode, adjust remaining time based on already-completed progress today
        if (timerMode == "COUNTDOWN" && goal.targetMinutes > 0) {
            val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }
            val dailySubjDurs = SubjectTagManager.getSubjectDurationsForDate(this, todayStr)
            val dailyGoalDurs = PlannerHistoryManager.getDailyGoalDurations(this, todayStr)
            val dailySubTopicDurs = if (goalSubId != "general") {
                mapOf(goalSubId to SubjectTagManager.getDailySubTopicDurations(this, goalSubId, todayStr))
            } else emptyMap()
            val progMap = PlannerHistoryManager.calculateGoalProgress(
                listOf(goal), dailySubjectDurations = dailySubjDurs,
                dailyGoalDurations = dailyGoalDurs, dailySubTopicDurations = dailySubTopicDurs
            )
            val actualMins = progMap[goal.id]?.actualMinutes ?: 0
            val remainingMins = (goal.targetMinutes - actualMins).coerceAtLeast(0)
            val remainingSecs = (if (remainingMins > 0) remainingMins * 60L else goal.targetMinutes * 60L).coerceAtLeast(60L)
            sp.edit()
                .putLong("focus_countdown_secs", remainingSecs)
                .putLong("focus_remaining_secs", remainingSecs)
                .apply()
        }

        navigateToPanel(AppPanel.FOCUS)
        AppToast.show(this, "Focusing on: ${goal.title}")

        if (currentTimerState == TimerState.IDLE) {
            handler.post {
                if (currentTimerState == TimerState.IDLE) {
                    handleStateToggle()
                }
            }
        }
    }

    internal fun clearActivePlannerGoal() {
        val isRunning = currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK
        if (isRunning) {
            AppToast.show(this, "Pause or stop timer to remove active task")
            return
        }
        val sp = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        sp.edit()
            .remove("active_planner_goal_id")
            .remove("active_planner_goal_title")
            .remove("active_planner_goal_subject_id")
            .remove("active_planner_goal_subtopic")
            .remove("active_planner_goal_target_mins")
            .apply()
        if (currentPanel == AppPanel.FOCUS) {
            buildCurrentPanel()
        }
    }

    internal fun focusBlockLabels(): Array<String> = DayTimelineDialogHelper(this).focusBlockLabels()
    internal fun focusBlockStartLabels(): Array<String> = DayTimelineDialogHelper(this).focusBlockStartLabels()
    internal fun focusBlockRangeLabel(b: Int): String = DayTimelineDialogHelper(this).focusBlockRangeLabel(b)

    internal fun createSettingsRow(icon: String, title: String, subtitle: String, endWidget: View? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            isClickable = true
            isFocusable = true
        }

        val iconView: View = if (title.equals("3D look", ignoreCase = true) || title.contains("3D look", ignoreCase = true)) {
            FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(14), 0) }
                val shadow = View(this@MainActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(6), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                        setMargins(0, 0, 0, dp(1))
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.argb(80, 0, 0, 0))
                    }
                }
                addView(shadow)
                val sphere = View(this@MainActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                    val pColor = themeCoordinator.primaryColor
                    val lightHighlight = tintedColor(pColor, 200)
                    val darkDepth = tintedColor(pColor, 50)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        orientation = GradientDrawable.Orientation.TL_BR
                        colors = intArrayOf(lightHighlight, pColor, darkDepth)
                    }
                }
                addView(sphere)
            }
        } else if (title.contains("AMOLED", ignoreCase = true) || icon == "\u2B24" || icon == "⚫") {
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { setMargins(0, 0, dp(14), 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.BLACK)
                    setStroke(dp(2), Color.parseColor("#555555"))
                }
            }
        } else {
            TextView(this).apply {
                text = icon
                textSize = 22f
                setPadding(0, 0, dp(14), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        row.addView(iconView)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textContainer.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 13.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, 3, 0, 0)
        })
        row.addView(textContainer)
        if (endWidget != null) {
            row.addView(endWidget)
        }
        return row
    }

    internal fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(28, 0, 28, 0) }
            setBackgroundColor(tintedColor(themeCoordinator.textColor, 22))
        }
    }

    internal fun refreshSettingsPanelPreservingScroll() {
        pendingSettingsScrollY = settingsScrollViewRef?.scrollY ?: 0
        navigateToPanel(AppPanel.SETTINGS)
        settingsScrollViewRef?.post {
            settingsScrollViewRef?.scrollTo(0, pendingSettingsScrollY)
        }
    }

    private fun buildSettingsPanel(target: android.view.ViewGroup = panelContainer, captureScrollRef: Boolean = true) {
        SettingsPanelBuilder(this).build(target, captureScrollRef)
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
        AppToast.show(this, R.string.toast_goal_reached, Toast.LENGTH_LONG)
    }

    private fun setupTimerLoop() {
        updateRunnable = Runnable {
            val sharedPrefs = appPrefs
            val prefStudy = sharedPrefs.safeLong("accumulatedStudy", 0L)
            val prefBreak = sharedPrefs.safeLong("currentBreakSeconds", 0L)
            if (timerViewModel.uiState.value.accumulatedStudy != prefStudy) {
                accumulatedStudy = prefStudy
            }
            if (timerViewModel.uiState.value.currentBreakSeconds != prefBreak) {
                currentBreakSeconds = prefBreak
            }
            currentTimerState = runCatching { TimerState.valueOf(sharedPrefs.safeString("timerState", "IDLE") ?: "IDLE") }.getOrDefault(TimerState.IDLE)

            // Self-heal corrupted 0-second PAUSED session to clean IDLE
            if (currentTimerState == TimerState.PAUSED && accumulatedStudy == 0L && currentBreakSeconds == 0L) {
                currentTimerState = TimerState.IDLE
                sharedPrefs.edit()
                    .putString("timerState", TimerState.IDLE.name)
                    .putLong("lastTimestamp", 0L)
                    .apply()
            }
            timerMode = sharedPrefs.safeString("timer_mode", sharedPrefs.safeString("timerMode", "STOPWATCH")) ?: "STOPWATCH"
            val pomodoroConfiguredSecs = sharedPrefs.safeLong("study_interval_minutes", 25L) * 60L
            focusCountdownSecs = if (timerMode == "LECTURE") {
                sharedPrefs.safeLong("focus_countdown_secs", pomodoroConfiguredSecs)
            } else {
                pomodoroConfiguredSecs
            }
            focusRemainingSecs = sharedPrefs.safeLong("focus_remaining_secs", 0L)
            prePauseState = runCatching { TimerState.valueOf(sharedPrefs.safeString("pre_pause_state", sharedPrefs.safeString("prePauseState", "STUDYING")) ?: "STUDYING") }.getOrDefault(TimerState.STUDYING)

            updateKeepScreenOn()

            val dayBucket = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86400000L
            if (dayBucket != lastDayBucket) {
                lastDayBucket = dayBucket
                val prevTodayStr = cachedTodayStr
                cachedTodayStr = dateKeyFmt.format(Date())
                statsDirty = true
                statsSnapshotCache = null
                tabPageCache.clear()
                // Lock yesterday's goal into history if not already sealed
                if (prevTodayStr.isNotBlank() && !sharedPrefs.contains("${prevTodayStr}_goal_secs")) {
                    sharedPrefs.edit().putLong("${prevTodayStr}_goal_secs", StatsEngine.getDailyGoalSecs(sharedPrefs)).apply()
                }
                if (!sharedPrefs.contains("${cachedTodayStr}_goal_secs")) {
                    sharedPrefs.edit().putLong("${cachedTodayStr}_goal_secs", StatsEngine.getDailyGoalSecs(sharedPrefs)).apply()
                }
                GoalHistoryManager.cleanFutureGoalCaches(this)
                checkAndResetGoalsForNewDay()
                recalculateStreak()
                if (currentPanel == AppPanel.STATS) {
                    refreshStatsPanel()
                }
                StudyWidgetProvider.refresh(this)
            }
            val timerStateChanged = currentTimerState != lastTickTimerState
            if (timerStateChanged) {
                lastTickTimerState = currentTimerState
                statsDirty = true
                statsSnapshotCache = null
                tabPageCache.clear()
                if (currentPanel == AppPanel.FOCUS && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) && !isPortraitFullscreenActive) {
                    scheduleAutoFullscreen(7000L)
                } else if (currentTimerState != TimerState.STUDYING && currentTimerState != TimerState.BREAK) {
                    cancelAutoFullscreen()
                }
            }

            if (appPrefs.getBoolean("pomo_prompt_subject_change", false)) {
                checkPomoSubjectPrompt()
            }

            val currentMinuteBucket = (accumulatedStudy + currentBreakSeconds) / 60L
            val currentWallMinute = System.currentTimeMillis() / 60000L
            val isMinuteTick = (currentMinuteBucket != lastStatsMinuteTick && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK))
                || (lastWallMinuteTick != -1L && currentWallMinute != lastWallMinuteTick)
            if (isMinuteTick || lastWallMinuteTick == -1L) {
                lastStatsMinuteTick = currentMinuteBucket
                lastWallMinuteTick = currentWallMinute
                if (isMinuteTick) {
                    statsDirty = true
                    statsSnapshotCache = null
                    tabPageCache.clear()
                    if (currentPanel == AppPanel.STATS || currentPanel == AppPanel.PLANNER) {
                        updateStatsLiveViews()
                    }
                }
            }

            maybeFireForegroundGoalPing(cachedTodayStr)

            // Check if TimerService stored a pending lecture-switch request
            if (!ongoingLectureDialogShowing
                && switchLectureDialog?.isShowing != true
                && sharedPrefs.getBoolean("pending_switch_to_lecture", false)
                && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK)) {
                sharedPrefs.edit().remove("pending_switch_to_lecture").apply()
                showSwitchToLectureDialog()
            }

            // Check if TimerService triggered an anti-cheat inactivity check-in prompt
            if (!inactivityCheckDialogShowing
                && sharedPrefs.getBoolean("pending_inactivity_check", false)
                && currentTimerState == TimerState.STUDYING) {
                showInactivityCheckDialog()
            }

            // Automatic Day Rollover Detection: Ensures goals, stats, and timer reset seamlessly at midnight
            val currentDayStr = dateKeyFmt.format(Date())
            if (cachedTodayStr.isNotEmpty() && cachedTodayStr != currentDayStr) {
                cachedTodayStr = currentDayStr
                checkAndResetGoalsForNewDay()
                if (currentPanel == AppPanel.STATS) {
                    updateStatsLiveViews()
                } else if (currentPanel == AppPanel.PLANNER) {
                    PlannerPanelBuilder(this@MainActivity).build()
                } else if (currentPanel == AppPanel.FOCUS) {
                    updateVisualStyles()
                }
            }

            if (currentPanel == AppPanel.FOCUS) {
                val isLectureModeActive = sharedPrefs.getBoolean("lecture_mode_enabled", false)
                val isStudying = currentTimerState == TimerState.STUDYING ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
                val isBreaking = currentTimerState == TimerState.BREAK ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
                val showCountdown = (timerMode == "COUNTDOWN" || isLectureModeActive) && isStudying

                val nextStudyText = when {
                    showCountdown -> {
                        if (focusRemainingSecs < 0L) {
                            "+${formatCountdown(-focusRemainingSecs)}"
                        } else {
                            formatCountdown(if (accumulatedStudy == 0L && focusRemainingSecs == 0L) focusCountdownSecs else focusRemainingSecs.coerceAtLeast(0L))
                        }
                    }
                    timerMode == "COUNTDOWN" && currentTimerState == TimerState.IDLE -> formatCountdown(pomodoroConfiguredSecs)
                    else -> formatTime(accumulatedStudy)
                }
                if (studyTimerDisplay.text != nextStudyText) {
                    studyTimerDisplay.text = nextStudyText
                }

                val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
                val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

                val nextBreakText = when {
                    isBreaking && timerMode == "COUNTDOWN" && breakCountdownSecs > 0L -> {
                        formatCountdown(breakRemainingSecs)
                    }
                    else -> formatTime(currentBreakSeconds)
                }
                if (breakTimerDisplay.text != nextBreakText) {
                    breakTimerDisplay.text = nextBreakText
                }

                val isLandscape = isLandscapeMode()
                if (isLandscape) {
                    if (lastIsBreakingState != isBreaking) {
                        lastIsBreakingState = isBreaking
                        val isCountdown = (timerMode == "COUNTDOWN")
                        val activeTextSize = if (isCountdown) 46f else 72f
                        val subTextSize = if (isCountdown) 15f else 17f
                        if (isBreaking) {
                            breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                            breakTimerDisplay.textSize = activeTextSize
                            breakTimerDisplay.setPadding(0, 0, 0, 5)

                            studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                            studyTimerDisplay.textSize = subTextSize
                            studyTimerDisplay.setPadding(0, 0, 0, 16)
                        } else {
                            studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                            studyTimerDisplay.textSize = activeTextSize
                            studyTimerDisplay.setPadding(0, 0, 0, 5)

                            breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                            breakTimerDisplay.textSize = subTextSize
                            breakTimerDisplay.setPadding(0, 0, 0, 16)
                        }
                    }
                    updateLandscapeDockStats()
                } else {
                    if (lastIsBreakingState != isBreaking || timerStateChanged) {
                        lastIsBreakingState = isBreaking
                        applyPortraitFullscreenLayout()
                    }
                }

                updateTimerRing(showCountdown)

                val showPause = sharedPrefs.getBoolean("show_pause_button", true)
                if (lastRenderedTimerState != currentTimerState || timerStateChanged || isZenModeActive != lastZenModeState || showPause != lastShowPauseState) {
                    lastRenderedTimerState = currentTimerState
                    lastZenModeState = isZenModeActive
                    lastShowPauseState = showPause
                    updateVisualStyles()
                }
                updatePauseBlink()
                if (currentTimerState == TimerState.STUDYING && accumulatedStudy > 0L) {
                    checkCelebration()
                }
            } else if (currentPanel == AppPanel.STATS || currentPanel == AppPanel.PLANNER) {
                updateStatsLiveViews()
            }
            handler.postDelayed(updateRunnable, 500)
        }
        handler.post(updateRunnable)
    }

    internal fun updateStatsLiveViews() {
        if (currentPanel != AppPanel.STATS && currentPanel != AppPanel.PLANNER) return
        val statsContainer = panelContainer
        val snap = computeStatsSnapshot()
        val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }

        if (currentPanel == AppPanel.PLANNER || currentStatsTab == AppStatsTab.PLANNER) {
                val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(this, todayStr)
                val dailyGoalDurations = PlannerHistoryManager.getDailyGoalDurations(this, todayStr)
                val dailySubTopicDurations = mutableMapOf<String, Map<String, Long>>()
                val goalsJson = appPrefs.getString("session_goals_json", "[]") ?: "[]"
                val goalsList = loadSessionGoalsFromJson(goalsJson)
                val todayCal = Calendar.getInstance()
                val scheduledTodayGoals = goalsList.filter { PlannerHistoryManager.isGoalScheduledOnDay(it.scheduledDays, todayCal) }

                for (g in scheduledTodayGoals) {
                    val subId = g.subjectId
                    if (!subId.isNullOrBlank() && subId != "all" && !dailySubTopicDurations.containsKey(subId)) {
                        dailySubTopicDurations[subId] = SubjectTagManager.getDailySubTopicDurations(this, subId, todayStr)
                    }
                }
                val progressMap = PlannerHistoryManager.calculateGoalProgress(
                    scheduledTodayGoals, snap.todayFocus, dailySubjectDurations, dailyGoalDurations, dailySubTopicDurations
                )

                val trulyAchievedIds = mutableSetOf<String>()
                for (goal in scheduledTodayGoals) {
                    val prog = progressMap[goal.id]
                    val targetMins = goal.targetMinutes
                    val actualMins = prog?.actualMinutes ?: 0
                    if (goal.completed || (targetMins > 0 && actualMins >= targetMins)) {
                        trulyAchievedIds.add(goal.id)
                    }
                }
                val completedCount = scheduledTodayGoals.count { trulyAchievedIds.contains(it.id) }
                val totalCount = scheduledTodayGoals.size
                val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

                val (plannerPrimary, _) = resolvePlannerColors()
                val isGoalReached = progressPct >= 100 && totalCount > 0
                val lineProgressColor = if (isGoalReached) 0xFF43D36E.toInt() else plannerPrimary

                (statsContainer.findViewWithTag<TextView>("planner_completed_text"))?.let {
                    it.text = "$completedCount of $totalCount completed"
                }
                (statsContainer.findViewWithTag<TextView>("planner_pct_text"))?.let {
                    it.text = "$progressPct%"
                    it.setTextColor(lineProgressColor)
                }
                (statsContainer.findViewWithTag<ProgressBar>("planner_progress_bar"))?.let {
                    it.progress = progressPct
                    it.progressTintList = android.content.res.ColorStateList.valueOf(lineProgressColor)
                }

                val activeCount = (totalCount - completedCount).coerceAtLeast(0)
                (statsContainer.findViewWithTag<TextView>("filter_tab_ALL"))?.text = "All ($totalCount)"
                (statsContainer.findViewWithTag<TextView>("filter_tab_ACTIVE"))?.text = "Remaining ($activeCount)"
                (statsContainer.findViewWithTag<TextView>("filter_tab_DONE"))?.text = "Done ($completedCount)"

                val greenColor = 0xFF22C55E.toInt()
                val redColor = 0xFFEF4444.toInt()

                for (goal in goalsList) {
                    val isScheduledToday = PlannerHistoryManager.isGoalScheduledOnDay(goal.scheduledDays, todayCal)
                    val targetMins = goal.targetMinutes
                    val prog = progressMap[goal.id]
                    val actualMins = prog?.actualMinutes ?: 0

                    if (!isScheduledToday) {
                        (statsContainer.findViewWithTag<TextView>("goal_chip_${goal.id}"))?.let { chip ->
                            val shouldShow = targetMins > 0
                            chip.visibility = if (shouldShow) View.VISIBLE else View.GONE
                            chip.text = if (targetMins > 0) "${targetMins}m" else ""
                            chip.setTextColor(tintedColor(themeCoordinator.textColor, 70))
                            chip.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 8f)
                        }
                        (statsContainer.findViewWithTag<TextView>("goal_check_${goal.id}"))?.let { checkBtn ->
                            checkBtn.text = "—"
                            checkBtn.textSize = 12f
                            checkBtn.setTextColor(tintedColor(themeCoordinator.textColor, 80))
                            checkBtn.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(Color.TRANSPARENT)
                                setStroke(dp(1.8f), tintedColor(themeCoordinator.textColor, 70))
                            }
                        }
                        (statsContainer.findViewWithTag<View>("goal_action_${goal.id}"))?.let { actionCta ->
                            actionCta.visibility = View.GONE
                        }
                        (statsContainer.findViewWithTag<TextView>("goal_title_${goal.id}"))?.let { titleTv ->
                            titleTv.setTextColor(themeCoordinator.textColor)
                            titleTv.paintFlags = titleTv.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                    } else if (targetMins > 0) {
                        val isDone = actualMins >= targetMins
                        val isChecked = isDone || goal.completed
                        val progressText = "${actualMins}/${targetMins}m"
                        val chipColor = if (isChecked) greenColor else plannerPrimary

                        (statsContainer.findViewWithTag<TextView>("goal_chip_${goal.id}"))?.let { chip ->
                            val shouldShow = if (isChecked) true else (targetMins > 0 || actualMins > 0)
                            chip.visibility = if (shouldShow) View.VISIBLE else View.GONE
                            chip.text = progressText
                            chip.setTextColor(chipColor)
                            chip.background = themeCoordinator.createGlassChip(tintedColor(chipColor, 50), 8f)
                        }

                        (statsContainer.findViewWithTag<TextView>("goal_check_${goal.id}"))?.let { checkBtn ->
                            val symbol = if (isChecked) "✓" else ""
                            checkBtn.text = symbol
                            checkBtn.textSize = 13f
                            val checkColor = if (isChecked) chipColor else Color.TRANSPARENT
                            val strokeColor = if (isChecked) chipColor else plannerPrimary
                            checkBtn.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(checkColor)
                                setStroke(dp(2), strokeColor)
                            }
                        }

                        (statsContainer.findViewWithTag<TextView>("goal_title_${goal.id}"))?.let { titleTv ->
                            titleTv.setTextColor(if (isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                            if (isChecked) {
                                titleTv.paintFlags = titleTv.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                            } else {
                                titleTv.paintFlags = titleTv.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                            }
                        }

                        (statsContainer.findViewWithTag<View>("goal_action_${goal.id}"))?.let { actionCta ->
                            actionCta.visibility = if (!isChecked) View.VISIBLE else View.GONE
                        }

                        (statsContainer.findViewWithTag<ProgressBar>("goal_minibar_${goal.id}"))?.let { bar ->
                            val pct = ((actualMins * 100) / targetMins).coerceIn(0, 100)
                            bar.progress = pct
                            bar.progressTintList = android.content.res.ColorStateList.valueOf(chipColor)
                        }
                    } else {
                        val isChecked = goal.completed
                        val progressText = if (actualMins > 0) "${actualMins}m" else (if (isChecked) "Done" else "Mark Done")
                        val chipColor = if (isChecked) greenColor else plannerPrimary

                        (statsContainer.findViewWithTag<TextView>("goal_chip_${goal.id}"))?.let { chip ->
                            chip.visibility = if (isChecked || actualMins > 0) View.VISIBLE else View.GONE
                            chip.text = progressText
                            chip.setTextColor(chipColor)
                            chip.background = themeCoordinator.createGlassChip(tintedColor(chipColor, 50), 8f)
                        }

                        (statsContainer.findViewWithTag<TextView>("goal_check_${goal.id}"))?.let { checkBtn ->
                            checkBtn.text = if (isChecked) "✓" else ""
                            checkBtn.textSize = 13f
                            val checkColor = if (isChecked) chipColor else Color.TRANSPARENT
                            val strokeColor = if (isChecked) chipColor else plannerPrimary
                            checkBtn.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(checkColor)
                                setStroke(dp(2), strokeColor)
                            }
                        }

                        (statsContainer.findViewWithTag<TextView>("goal_title_${goal.id}"))?.let { titleTv ->
                            titleTv.setTextColor(if (isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                            if (isChecked) {
                                titleTv.paintFlags = titleTv.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                            } else {
                                titleTv.paintFlags = titleTv.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                            }
                        }

                        (statsContainer.findViewWithTag<View>("goal_action_${goal.id}"))?.let { actionCta ->
                            actionCta.visibility = if (!isChecked) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        if (currentPanel == AppPanel.STATS && currentStatsTab == AppStatsTab.OVERVIEW) {
                val todayFocus = snap.todayFocus
                val todayH = todayFocus / 3600
                val todayM = (todayFocus % 3600) / 60
                val todayBH = snap.todayBreak / 3600
                val todayBM = (snap.todayBreak % 3600) / 60
                val heroGoalSecs = snap.heroGoalSecs
                val heroGoalPctRaw = if (heroGoalSecs > 0) todayFocus.toFloat() / heroGoalSecs.toFloat() * 100f else 0f
                val goalReached = todayFocus >= heroGoalSecs && heroGoalSecs > 0

                (statsContainer.findViewWithTag<TextView>("overview_today_focus_time"))?.let {
                    it.text = getString(R.string.duration_h_m, todayH, todayM)
                }
                (statsContainer.findViewWithTag<TextView>("overview_target_subtext"))?.let {
                    it.text = "Target: ${formatGoalLabel(heroGoalSecs)} · ${(heroGoalPctRaw).toInt()}% completed"
                }
                (statsContainer.findViewWithTag<TextView>("overview_ring_center_text"))?.let {
                    it.text = if (goalReached) "✓" else "${heroGoalPctRaw.toInt()}%"
                }
                val remainingSecs = (heroGoalSecs - todayFocus).coerceAtLeast(0L)
                val remainingLabel = if (goalReached) "Goal Reached!" else "${formatGoalLabel(remainingSecs)} left"
                (statsContainer.findViewWithTag<TextView>("overview_remaining_badge"))?.let {
                    it.text = remainingLabel
                }
                val breakLabel = if (todayBH > 0) "${todayBH}h ${todayBM}m break" else "${todayBM}m break"
                (statsContainer.findViewWithTag<TextView>("overview_break_badge"))?.let {
                    it.text = breakLabel
                }
                (statsContainer.findViewWithTag<SubjectPieChartView>("overview_pie_chart"))?.let { pieChart ->
                    if (StatsPanelBuilder.activePiePeriod == SubjectTagManager.SubjectStatsPeriod.TODAY) {
                        val (allDaySessions, _) = dayBlocks(todayStr)
                        val hasLive = allDaySessions.any { it.running }
                        val map = LinkedHashMap<String, Pair<SubjectTag, Long>>()
                        for (s in allDaySessions) {
                            val subId = s.subjectId ?: "general"
                            val subj = SubjectTagManager.resolveSubject(this, subId, s.subjectName, s.subjectColor)
                            val currentSecs = map[subj.id]?.second ?: 0L
                            map[subj.id] = subj to (currentSecs + s.secs)
                        }
                        if (map.isEmpty()) {
                            val legacy = SubjectTagManager.getSubjectDurationsForDate(this, todayStr)
                            for ((subId, secs) in legacy) {
                                val subj = SubjectTagManager.resolveSubject(this, subId)
                                val currentSecs = map[subj.id]?.second ?: 0L
                                map[subj.id] = subj to (currentSecs + secs)
                            }
                        }
                        val isRunning = (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.PAUSED)
                        val todayFocusSecs = FocusTimeProvider.getTodayFocusSeconds(this)
                        val sumRecorded = map.values.sumOf { it.second }
                        if (todayFocusSecs > sumRecorded) {
                            val diff = todayFocusSecs - sumRecorded
                            if (isRunning) {
                                val activeSub = SubjectTagManager.getSelectedSubject(this)
                                val currentSecs = map[activeSub.id]?.second ?: 0L
                                map[activeSub.id] = activeSub to (currentSecs + diff)
                            } else if (map.isNotEmpty()) {
                                val targetSub = map.values.maxByOrNull { it.second }?.first ?: SubjectTagManager.resolveSubject(this, "general", "General Study", "#6366F1")
                                val curr = map[targetSub.id]?.second ?: 0L
                                map[targetSub.id] = targetSub to (curr + diff)
                            } else {
                                val genSub = SubjectTagManager.resolveSubject(this, "general", "General Study", "#6366F1")
                                map[genSub.id] = genSub to diff
                            }
                        }
                        val sortedSubjects = map.values.filter { it.second > 0L }.sortedByDescending { it.second }
                        if (sortedSubjects.isNotEmpty()) {
                            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()
                            for ((subj, secs) in sortedSubjects) {
                                slicesList.add(SubjectPieChartView.PieSlice(subj.name, "", secs.toDouble(), subj.colorHex))
                            }
                            pieChart.setData(slicesList, preserveSelection = true)
                        }
                    }
                }
        }
        if (currentPanel == AppPanel.STATS && currentStatsTab == AppStatsTab.TIMELINE) {
            val exams = ExamCountdownManager.getExams(this)
            for (exam in exams) {
                val breakdown = ExamCountdownManager.getCountdownBreakdown(exam.targetTimestampMs)
                (statsContainer.findViewWithTag<TextView>("exam_main_countdown_${exam.id}"))?.let {
                    it.text = breakdown.mainHeadline
                }
                (statsContainer.findViewWithTag<TextView>("exam_sub_countdown_${exam.id}"))?.let {
                    it.text = if (breakdown.readableSubtitle.isNotBlank()) breakdown.readableSubtitle else "Scheduled event"
                }
            }
        }
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
        performMicroHaptic(rootLayout)
        if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
            scheduleAutoFullscreen()
        } else {
            cancelAutoFullscreen()
        }
    }

    private fun playStopFeedback() {
        cancelAutoFullscreen()
        performHapticConfirm(rootLayout)
    }

    private var pendingNotificationAction: (() -> Unit)? = null

    internal fun showNotificationRationaleDialog(onGranted: (() -> Unit)? = null) = BreakAndLectureDialogHelper(this).showNotificationRationaleDialog(onGranted)

    internal fun handleStateToggle() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (currentTimerState == TimerState.IDLE && Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val doNotAskAgain = prefs.getBoolean("notif_perm_do_not_ask_again", false)
                if (!doNotAskAgain) {
                    showNotificationRationaleDialog {
                        executeStateToggle()
                    }
                    return
                }
            }
        }
        executeStateToggle()
    }

    private fun executeStateToggle() {
        if (!canExecuteAction()) return
        val prefs = appPrefs
        val isFreedomMode = prefs.getBoolean("pomodoro_freedom_mode", false)
        if (currentTimerState == TimerState.STUDYING && timerMode == "COUNTDOWN" && !isFreedomMode) {
            // Save current remaining focus countdown before break so returning to focus resumes from this position
            prefs.edit()
                .putLong("focus_remaining_secs", focusRemainingSecs)
                .putLong("focusRemainingSecs", focusRemainingSecs)
                .apply()
            showBreakDurationDialog()
            return
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
        dismissGestureHint()
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    internal fun dismissGestureHint() {
        val hint = gestureHintView ?: return
        if (hint.visibility == View.VISIBLE) {
            hint.animate().alpha(0f).setDuration(220).withEndAction {
                hint.visibility = View.GONE
            }.start()
            getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).edit().putBoolean("gesture_hint_dismissed", true).apply()
        }
    }

    internal fun handlePause() {
        if (!canExecuteAction()) return
        dismissGestureHint()
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

    internal fun handleStartBreakOnly(breakSecs: Long = 300L) {
        if (!canExecuteAction()) return
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_BREAK
            putExtra("BREAK_SECS", breakSecs)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    internal fun showBreakDurationDialog() = BreakAndLectureDialogHelper(this).showBreakDurationDialog()

    private fun updateTimerRing(showCountdown: Boolean) {
        if (!::timerRing.isInitialized) return
        val shouldShowCountdownRing = (timerMode == "COUNTDOWN")
        timerRing.visibility = if (shouldShowCountdownRing) View.VISIBLE else View.GONE
        if (!shouldShowCountdownRing) return

        val isPomoWhite = isPomodoroPureWhiteActive()
        val isDark = themeCoordinator.isDarkMode()
        val track = when {
            isPomoWhite -> 0xFFE2E8F0.toInt()
            isDark -> 0x2EFFFFFF.toInt()
            else -> 0x1E000000.toInt()
        }
        val primaryRingColor = if (isPomoWhite) 0xFF000000.toInt() else themeCoordinator.primaryColor
        val breakRingColor = if (isPomoWhite) 0xFF475569.toInt() else themeCoordinator.secondaryColor

        val isActive = currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK
        timerRing.setTimerActive(isActive)

        if (currentTimerState == TimerState.IDLE) {
            timerRing.setProgress(1f, primaryRingColor, track)
            return
        }

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
        val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

        val studying = currentTimerState == TimerState.STUDYING ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
        val breaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)

        when {
            studying && focusCountdownSecs > 0 -> {
                if (focusRemainingSecs < 0L) {
                    val overtimeRingColor = if (isPomoWhite) 0xFF000000.toInt() else Color.parseColor("#F59E0B")
                    timerRing.setProgress(1f, overtimeRingColor, track)
                } else {
                    timerRing.setProgress((focusRemainingSecs.toFloat() / focusCountdownSecs).coerceIn(0f, 1f), primaryRingColor, track)
                }
            }
            breaking && breakCountdownSecs > 0L -> {
                timerRing.setProgress((breakRemainingSecs.toFloat() / breakCountdownSecs).coerceIn(0f, 1f), breakRingColor, track)
            }
            breaking -> {
                timerRing.setProgress(1f, breakRingColor, track)
            }
            else -> {
                timerRing.setProgress(1f, primaryRingColor, track)
            }
        }
    }

    internal fun formatCountdown(totalSeconds: Long): String {
        val s = max(0L, totalSeconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    internal fun handleStopSession(silent: Boolean = false) {
        lectureEndDialog?.dismiss()
        lectureEndDialog = null
        val savedStudy = accumulatedStudy
        val savedBreak = currentBreakSeconds

        // Safely update today's focus and break totals in SharedPreferences immediately,
        // ensuring Today's Focus in Insights never drops or starts from zero.
        val todayStr = dateKeyFmt.format(Date())
        val sp = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val curFocus = sp.getLong("${todayStr}_focus_total", 0L)
        val curBreak = sp.getLong("${todayStr}_break_total", 0L)
        if (savedStudy > 0L || savedBreak > 0L) {
            sp.edit()
                .putLong("${todayStr}_focus_total", curFocus + savedStudy)
                .putLong("${todayStr}_break_total", curBreak + savedBreak)
                .apply()
        }

        TimelineLogger.record(this, TimerState.IDLE)

        val intent = Intent(this, TimerService::class.java).apply {
            action = if (silent) TimerService.ACTION_STOP_SILENT else TimerService.ACTION_STOP
            putExtra("EXTRA_STUDY_SECS", savedStudy)
            putExtra("EXTRA_BREAK_SECS", savedBreak)
            putExtra("EXTRA_ALREADY_FINALIZED", true)
        }
        try {
            startService(intent)
        } catch (_: Exception) {}

        currentTimerState = TimerState.IDLE
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        focusRemainingSecs = 0L
        focusCountdownSecs = 0L

        sp.edit()
            .putString("timerState", TimerState.IDLE.name)
            .putLong("accumulatedStudy", 0L)
            .putLong("currentBreakSeconds", 0L)
            .putLong("focus_remaining_secs", 0L)
            .putLong("focus_countdown_secs", 0L)
            .putLong("break_countdown_secs", 0L)
            .putLong("break_remaining_secs", 0L)
            .putLong("lastTimestamp", 0L)
            .putBoolean("lecture_mode_enabled", false)
            .remove("active_lecture_subject_id")
            .putString("pre_pause_state", TimerState.STUDYING.name)
            .apply()

        statsDirty = true
        statsSnapshotCache = null
        tabPageCache.clear()
        invalidateStatsCache()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            backupManager.runSilentAutoBackup()
            if (AuthManager.isLoggedIn(this@MainActivity)) {
                CloudSyncManager.syncDataToCloud(this@MainActivity)
            }
        }
        recalculateStreak(todayExtra = if (silent) 0L else savedStudy)

        if (silent) {
            try { rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
        } else {
            playStopFeedback()
        }

        if (currentPanel == AppPanel.FOCUS) {
            lastIsBreakingState = false
            applyPortraitFullscreenLayout()
            updateVisualStyles()
            studyTimerDisplay.text = "00:00:00"
            breakTimerDisplay.text = getString(R.string.break_prefix, "00:00:00")
            breakTimerDisplay.visibility = View.GONE
        }
        if (::timerRing.isInitialized) {
            val isPomoWhite = isPomodoroPureWhiteActive()
            val isDark = themeCoordinator.isDarkMode()
            val track = when {
                isPomoWhite -> 0xFFE2E8F0.toInt()
                isDark -> 0x2EFFFFFF.toInt()
                else -> 0x1E000000.toInt()
            }
            val primaryRingColor = if (isPomoWhite) 0xFF000000.toInt() else themeCoordinator.primaryColor
            timerRing.setTimerActive(false)
            if (timerMode == "COUNTDOWN") {
                timerRing.setProgress(1f, primaryRingColor, track, animate = false)
            } else {
                timerRing.setProgress(0f, primaryRingColor, track, animate = false)
            }
        }
        StudyWidgetProvider.refresh(this)
        checkCelebration()
    }

    internal fun updateKeepScreenOn() {
        val shouldKeep = (currentTimerState == TimerState.STUDYING)
        val newFlag = if (shouldKeep) 1 else 0
        if (newFlag == lastKeepScreenOn) return
        lastKeepScreenOn = newFlag
        if (shouldKeep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    internal var lectureEndDialog: Dialog? = null

    internal fun checkAndShowLecturePrompt() {
        if (isFinishing || isDestroyed) return
        if (currentTimerState == TimerState.LECTURE_ENDED) {
            if (lectureEndDialog?.isShowing == true) return
            showLectureEndDialog()
        } else {
            if (lectureEndDialog?.isShowing == true) {
                lectureEndDialog?.dismiss()
                lectureEndDialog = null
            }
        }
    }

    internal fun showLectureEndDialog() = BreakAndLectureDialogHelper(this).showLectureEndDialog()

    internal fun handleStartBreak() {
        lectureEndDialog?.dismiss()
        lectureEndDialog = null
        statsDirty = true
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.BREAK
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putString("timerState", "BREAK")
            .putBoolean("lecture_mode_enabled", false)
            .putLong("focus_remaining_secs", 0L)
            .putLong("lastTimestamp", now - 1L)
            .putLong("lecture_prompt_timestamp", 0L)
            .apply()

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_BREAK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }


    internal fun handleExtendLecture(secs: Long = 300L) {
        lectureEndDialog?.dismiss()
        lectureEndDialog = null
        statsDirty = true
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.STUDYING
        timerMode = "LECTURE"
        focusCountdownSecs = secs
        focusRemainingSecs = secs
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putString("timer_mode", "LECTURE")
            .putBoolean("lecture_mode_enabled", true)
            .putString("timerState", "STUDYING")
            .putLong("focus_countdown_secs", secs)
            .putLong("focusCountdownSecs", secs)
            .putLong("focus_remaining_secs", secs)
            .putLong("focusRemainingSecs", secs)
            .putLong("lastTimestamp", now - 1L)
            .putLong("lecture_prompt_timestamp", 0L)
            .apply()

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_EXTEND_LECTURE
            putExtra("EXTEND_SECS", secs)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        if (::studyTimerDisplay.isInitialized) {
            studyTimerDisplay.text = formatCountdown(secs)
        }
        StudyWidgetProvider.refresh(this)
    }




    internal fun updateVisualStyles() {
        val isLandscapeOrientation = isLandscapeMode()
        val isPomoWhite = isPomodoroPureWhiteActive()
        if (currentPanel == AppPanel.FOCUS) {
            if (isPomoWhite) {
                rootLayout.setBackgroundColor(0xFFFFFFFF.toInt())
            } else {
                rootLayout.background = themeCoordinator.createBackgroundDrawable()
            }
        }

        if (::studyBottomNavBar.isInitialized) {
            if (isLandscapeOrientation) {
                studyBottomNavBar.animate().cancel()
                studyBottomNavBar.visibility = View.GONE
            } else {
                studyBottomNavBar.applyTheme(themeCoordinator)
                studyBottomNavBar.setStudyingDim(currentTimerState == TimerState.STUDYING)
                val shouldHide = (currentPanel == AppPanel.SETTINGS || currentPanel == AppPanel.HEATMAP || isPortraitFullscreenActive)
                studyBottomNavBar.visibility = if (shouldHide) View.GONE else View.VISIBLE
            }
        }

        // Mode switch pill is only visible when no timer is running
        panelContainer.findViewWithTag<View>("mode_pill")?.let { pill ->
            pill.visibility = if (currentTimerState == TimerState.IDLE) View.VISIBLE else View.GONE
        }

        val selectedTf = FontStyleManager.getTypeface(this)
        val selectedBoldTf = FontStyleManager.getBoldTypeface(this)
        if (::studyTimerDisplay.isInitialized) {
            studyTimerDisplay.typeface = selectedTf
        }
        if (::breakTimerDisplay.isInitialized) {
            breakTimerDisplay.typeface = selectedTf
        }
        if (::mainBtn.isInitialized) mainBtn.typeface = selectedBoldTf
        if (::pauseBtn.isInitialized) pauseBtn.typeface = selectedBoldTf
        if (::stopBtn.isInitialized) stopBtn.typeface = selectedBoldTf
        if (::statusBadge.isInitialized) statusBadge.typeface = selectedBoldTf
        FontStyleManager.applyRecursively(panelContainer)

        val isWallpaperActive = WallpaperManager.getWallpaperType(this) != "NONE"
        val isBrightUnderTimer = WallpaperManager.isBackgroundUnderTimerBright(this)
        val isBrightSurface = isPomoWhite || (isWallpaperActive && isBrightUnderTimer) || !themeCoordinator.isDarkMode()

        val timerColor = if (isBrightSurface) {
            0xFF0F172A.toInt() // High-contrast obsidian slate for bright backgrounds
        } else if (pureWhiteTimerEnabled() || (isWallpaperActive && !isBrightUnderTimer)) {
            0xFFFFFFFF.toInt() // Crisp pure white for dark backgrounds
        } else {
            themeCoordinator.primaryColor
        }

        val dynamicBtnTextColor = if (isBrightSurface) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()

        val mainBtnTextColor = if (isPomoWhite) {
            0xFFFFFFFF.toInt()
        } else {
            ThemeCoordinator.getContrastColor(themeCoordinator.primaryColor)
        }

        if (isPomoWhite) {
            studyTimerDisplay.setShadowLayer(0f, 0f, 0f, 0)
        }

        when (currentTimerState) {
            TimerState.IDLE -> {
                statusBadge.text = getString(R.string.ready_to_track)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                }
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                if (timerMode == "COUNTDOWN") {
                    pauseBtn.visibility = View.VISIBLE
                    pauseBtn.text = "Start"
                    pauseBtn.setTextColor(mainBtnTextColor)
                    if (isPomoWhite) {
                        pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(20).toFloat()
                            setColor(0xFF000000.toInt())
                        }
                    } else {
                        pauseBtn.background = rippleBackground(themeCoordinator.primaryColor)
                    }
                    pauseBtn.setOnClickListener { handleStateToggle() }

                    mainBtn.visibility = View.VISIBLE
                    mainBtn.text = "Break"
                    if (isPomoWhite) {
                        mainBtn.setTextColor(0xFF0F172A.toInt())
                        mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(20).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                        }
                    } else {
                        mainBtn.setTextColor(dynamicBtnTextColor)
                        mainBtn.background = outlinedButtonBackground()
                    }
                    mainBtn.setOnClickListener { showBreakDurationDialog() }
                    stopBtn.visibility = View.GONE
                } else {
                    pauseBtn.visibility = View.VISIBLE
                    pauseBtn.text = "Start"
                    pauseBtn.setTextColor(mainBtnTextColor)
                    pauseBtn.background = rippleBackground(themeCoordinator.primaryColor)
                    pauseBtn.setOnClickListener { handleStateToggle() }
                    mainBtn.visibility = View.GONE
                    stopBtn.visibility = View.GONE 
                }
            }
            TimerState.STUDYING -> {
                val isOvertime = timerMode == "COUNTDOWN" && focusRemainingSecs < 0L
                statusBadge.text = when {
                    timerMode == "LECTURE" -> "LECTURE IN PROGRESS"
                    isOvertime -> "OVERTIME"
                    else -> getString(R.string.learning_time)
                }
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else if (isOvertime) {
                    val amber = Color.parseColor("#F59E0B")
                    statusBadge.setTextColor(amber)
                    statusBadge.background = themeCoordinator.createGlassChip(amber, 30f)
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                }
                studyTimerDisplay.setTextColor(timerColor)
                if (isPomoWhite) {
                    breakTimerDisplay.setTextColor(0xFF64748B.toInt())
                } else {
                    breakTimerDisplay.setTextColor(themeCoordinator.textColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = if (isLandscapeOrientation || timerMode == "COUNTDOWN") View.GONE else View.VISIBLE

                // Slot 0 (Left): Primary CTA - PAUSE
                pauseBtn.visibility = pauseButtonVisibility()
                pauseBtn.text = "Pause"
                pauseBtn.setOnClickListener { handlePause() }
                if (isPomoWhite) {
                    pauseBtn.setTextColor(0xFFFFFFFF.toInt())
                    pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFF0F172A.toInt())
                    }
                } else {
                    pauseBtn.setTextColor(mainBtnTextColor)
                    pauseBtn.background = rippleBackground(themeCoordinator.primaryColor)
                }

                // Slot 1 (Center): Auxiliary - BREAK
                mainBtn.visibility = View.VISIBLE
                mainBtn.text = "Break"
                mainBtn.setOnClickListener { handleStateToggle() }
                if (isPomoWhite) {
                    mainBtn.setTextColor(0xFF0F172A.toInt())
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    mainBtn.setTextColor(dynamicBtnTextColor)
                    mainBtn.background = outlinedButtonBackground()
                }

                // Slot 2 (Right): Terminal Action - FINISH
                stopBtn.visibility = View.VISIBLE
                stopBtn.text = "Finish"
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = if (isBrightSurface) 0xFF0F172A.toInt() else themeCoordinator.primaryColor
                    stopBtn.setTextColor(dynamicBtnTextColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
            TimerState.LECTURE_ENDED -> {
                statusBadge.text = "LECTURE ENDED"
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                pauseBtn.visibility = View.VISIBLE
                pauseBtn.text = "Break"
                pauseBtn.setTextColor(mainBtnTextColor)
                pauseBtn.background = rippleBackground(themeCoordinator.secondaryColor)
                pauseBtn.setOnClickListener { handleStateToggle() }
                mainBtn.visibility = View.GONE
                stopBtn.visibility = View.VISIBLE
                stopBtn.text = "Finish"
                stopBtn.ringColor = themeCoordinator.primaryColor
                checkAndShowLecturePrompt()
            }
            TimerState.BREAK -> {
                statusBadge.text = getString(R.string.break_in_progress)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                    studyTimerDisplay.setTextColor(0xFF64748B.toInt())
                    breakTimerDisplay.setTextColor(0xFF000000.toInt())
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                    studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                    breakTimerDisplay.setTextColor(themeCoordinator.secondaryColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE

                // Slot 0 (Left): Primary CTA - RESUME STUDY
                pauseBtn.visibility = View.VISIBLE
                pauseBtn.text = "Resume"
                pauseBtn.setTextColor(mainBtnTextColor)
                if (isPomoWhite) {
                    pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFF000000.toInt())
                    }
                } else {
                    pauseBtn.background = rippleBackground(themeCoordinator.primaryColor)
                }
                pauseBtn.setOnClickListener { handleStateToggle() }

                // Slot 1 (Center): Auxiliary - PAUSE BREAK
                mainBtn.visibility = pauseButtonVisibility()
                mainBtn.text = "Pause"
                mainBtn.setOnClickListener { handlePause() }
                if (isPomoWhite) {
                    mainBtn.setTextColor(0xFF0F172A.toInt())
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    mainBtn.setTextColor(dynamicBtnTextColor)
                    mainBtn.background = outlinedButtonBackground()
                }

                // Slot 2 (Right): Terminal Action - FINISH
                stopBtn.visibility = View.VISIBLE
                stopBtn.text = "Finish"
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = if (isBrightSurface) 0xFF0F172A.toInt() else themeCoordinator.primaryColor
                    stopBtn.setTextColor(dynamicBtnTextColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
            TimerState.PAUSED -> {
                statusBadge.text = getString(R.string.paused)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                }
                studyTimerDisplay.setTextColor(timerColor)
                if (isPomoWhite) {
                    breakTimerDisplay.setTextColor(0xFF64748B.toInt())
                } else {
                    breakTimerDisplay.setTextColor(themeCoordinator.textColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = if (isLandscapeOrientation || (timerMode == "COUNTDOWN" && currentBreakSeconds == 0L)) View.GONE else View.VISIBLE

                // Slot 0 (Left): Primary CTA - RESUME
                pauseBtn.visibility = View.VISIBLE
                pauseBtn.text = "Resume"
                pauseBtn.setTextColor(mainBtnTextColor)
                if (isPomoWhite) {
                    pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFF000000.toInt())
                    }
                } else {
                    pauseBtn.background = rippleBackground(themeCoordinator.primaryColor)
                }
                pauseBtn.setOnClickListener { handleStateToggle() }

                // Slot 1 (Center): Auxiliary - BREAK
                mainBtn.visibility = View.VISIBLE
                mainBtn.text = "Break"
                if (isPomoWhite) {
                    mainBtn.setTextColor(0xFF0F172A.toInt())
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    mainBtn.setTextColor(dynamicBtnTextColor)
                    mainBtn.background = outlinedButtonBackground()
                }
                mainBtn.setOnClickListener { handleStateToggle() }

                // Slot 2 (Right): Terminal Action - FINISH
                stopBtn.visibility = View.VISIBLE
                stopBtn.text = "Finish"
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = if (isBrightSurface) 0xFF0F172A.toInt() else themeCoordinator.primaryColor
                    stopBtn.setTextColor(dynamicBtnTextColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
        }
        if (isLandscapeOrientation) {
            applyLandscapeActionControlLayout()
            if (::statusBadge.isInitialized) {
                statusBadge.background = null
                statusBadge.setPadding(0, 0, 0, 0)
            }
        } else if (::controlActionContainer.isInitialized) {
            applyPortraitActionControlLayout()
        }
        if (::studyBottomNavBar.isInitialized) {
            studyBottomNavBar.applyTheme(themeCoordinator)
        }
        if (isPortraitFullscreenActive) {
            applyPortraitFullscreenLayout()
        }
        updateTimerRing(false)
        updateStatusBarIcons()
    }

    private fun applyLandscapeActionControlLayout() {
        if (!::controlActionContainer.isInitialized) return
        controlActionContainer.orientation = LinearLayout.HORIZONTAL
        controlActionContainer.gravity = Gravity.CENTER

        val isIdle = (currentTimerState == TimerState.IDLE)
        val btnHeight = dp(44)

        if (isIdle) {
            if (::pauseBtn.isInitialized && pauseBtn.visibility == View.VISIBLE) {
                pauseBtn.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply {
                    setMargins(0, dp(4), 0, dp(4))
                }
            }
            if (::mainBtn.isInitialized) mainBtn.visibility = View.GONE
            if (::stopBtn.isInitialized) stopBtn.visibility = View.GONE
        } else {
            val showPause = pauseButtonVisibility() == View.VISIBLE
            val showBreak = ::mainBtn.isInitialized && mainBtn.visibility == View.VISIBLE
            val showStop = ::stopBtn.isInitialized && stopBtn.visibility == View.VISIBLE

            if (::pauseBtn.isInitialized && pauseBtn.visibility == View.VISIBLE) {
                pauseBtn.layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                    setMargins(0, 0, if (showBreak || showStop) dp(4) else 0, 0)
                }
            }
            if (::mainBtn.isInitialized && mainBtn.visibility == View.VISIBLE) {
                mainBtn.layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1f).apply {
                    setMargins(dp(2), 0, if (showStop) dp(2) else 0, 0)
                }
            }
            if (::stopBtn.isInitialized && stopBtn.visibility == View.VISIBLE) {
                stopBtn.layoutParams = LinearLayout.LayoutParams(0, btnHeight, 1.15f).apply {
                    setMargins(dp(4), 0, 0, 0)
                }
            }
        }
    }

    internal fun updateLandscapeDockStats() {
        val bar = landscapeGoalProgressBar ?: return
        val todayTxt = landscapeTodayFocusText ?: return
        val percentTxt = landscapeGoalPercentText ?: return

        val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }
        val storedTodayFocus = appPrefs.getLong("${todayStr}_focus_total", 0L)
        val runningSecs = if (currentTimerState != TimerState.IDLE) accumulatedStudy else 0L
        val totalTodaySecs = storedTodayFocus + runningSecs
        val goal = resolveGoalFor(todayStr)

        val h = totalTodaySecs / 3600
        val m = (totalTodaySecs % 3600) / 60
        todayTxt.text = if (h > 0) "${h}h ${m}m" else "${m}m"

        if (goal > 0L) {
            val pct = ((totalTodaySecs.toFloat() / goal.toFloat()) * 100f).toInt().coerceIn(0, 100)
            bar.progress = pct
            val gh = goal / 3600
            val gm = (goal % 3600) / 60
            val goalStr = if (gh > 0) "${gh}h ${gm}m" else "${gm}m"
            percentTxt.text = "$pct% of $goalStr"
        } else {
            bar.progress = 100
            percentTxt.text = "No goal target"
        }
    }

    private fun applyPortraitActionControlLayout() {
        if (!::controlActionContainer.isInitialized) return
        controlActionContainer.orientation = LinearLayout.HORIZONTAL
        controlActionContainer.gravity = Gravity.CENTER

        val visibleButtons = mutableListOf<View>()
        if (::pauseBtn.isInitialized && pauseBtn.visibility == View.VISIBLE) visibleButtons.add(pauseBtn)
        if (::mainBtn.isInitialized && mainBtn.visibility == View.VISIBLE) visibleButtons.add(mainBtn)
        if (::stopBtn.isInitialized && stopBtn.visibility == View.VISIBLE) visibleButtons.add(stopBtn)

        when (visibleButtons.size) {
            1 -> {
                visibleButtons[0].layoutParams = LinearLayout.LayoutParams(dp(210), dp(48)).apply {
                    gravity = Gravity.CENTER
                }
            }
            2 -> {
                visibleButtons[0].layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(12), 0, dp(6), 0)
                }
                visibleButtons[1].layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(6), 0, dp(12), 0)
                }
            }
            3 -> {
                visibleButtons[0].layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(8), 0, dp(4), 0)
                }
                visibleButtons[1].layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                visibleButtons[2].layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(4), 0, dp(8), 0)
                }
            }
        }
    }

    internal fun updateStatusBarIcons() {
        try {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            val hasWallpaper = WallpaperManager.getWallpaperType(this) != "NONE"
            val isPomoWhiteOnFocus = (currentPanel == AppPanel.FOCUS && isPomodoroPureWhiteActive())
            val isLight = if (hasWallpaper) {
                WallpaperManager.isBackgroundUnderTimerBright(this)
            } else {
                isPomoWhiteOnFocus || themeCoordinator.activeBgMode == "LIGHT"
            }

            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            val decor = window.peekDecorView() ?: window.decorView
            androidx.core.view.WindowCompat.getInsetsController(window, decor).let { controller ->
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        } catch (_: Exception) {}
    }


    private fun createNotificationChannel() {
        NotificationHelper.createAllNotificationChannels(this)
    }

    fun syncFlipToFocusState() {
        if (FlipToFocusManager.isEnabled(this)) {
            FlipToFocusManager.setContext(this)
            FlipToFocusManager.start(
                context = this,
                onFaceDown = {
                    if (currentTimerState == TimerState.IDLE || currentTimerState == TimerState.PAUSED || currentTimerState == TimerState.BREAK) {
                        handleStateToggle()
                    }
                },
                onFaceUp = {
                    if (FlipToFocusManager.isPauseOnFaceUp(this) && currentTimerState == TimerState.STUDYING) {
                        handlePause()
                    }
                }
            )
        } else {
            FlipToFocusManager.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        liveWallpaperView?.resume()
        if (::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
            handler.post(updateRunnable)
        }
        val currentOrientation = resources.configuration.orientation
        if (lastConfigOrientation != currentOrientation) {
            lastConfigOrientation = currentOrientation
            isPortraitFullscreenActive = false
            buildCurrentPanel()
            updateVisualStyles()
        }
        applyImmersiveModeForLandscape()
        StudyWidgetProvider.refresh(this)
        LectureScheduleScheduler.scheduleAll(this)
        checkOngoingScheduledLecturePrompt()
        checkPomoSubjectPrompt()
        checkCelebration()
        triggerAutoSyncIfEligible()
        syncFlipToFocusState()
        AppBlockerManager.onStudyTimerResumed(this)
        val oldProfile = ProfileManager.getProfile(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            LeaderboardManager.syncVerifiedOfflineStudy(this@MainActivity)
            val updated = ProfileSyncService.refreshProfileStatus(this@MainActivity)
            if (oldProfile.moderationStatus == ModerationStatus.PENDING_APPROVAL && updated.moderationStatus == ModerationStatus.APPROVED) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        AppToast.show(this@MainActivity, "🎉 Your profile changes have been approved by the moderator!", Toast.LENGTH_LONG)
                        if (currentPanel == AppPanel.SETTINGS || currentPanel == AppPanel.COMMUNITY || currentPanel == AppPanel.LEADERBOARD) {
                            buildCurrentPanel()
                        }
                    }
                }
            } else if (oldProfile.moderationStatus == ModerationStatus.PENDING_APPROVAL && updated.moderationStatus == ModerationStatus.REJECTED) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        AppToast.show(this@MainActivity, "⚠️ Your recent profile edit was not approved.", Toast.LENGTH_LONG)
                        if (currentPanel == AppPanel.SETTINGS || currentPanel == AppPanel.COMMUNITY || currentPanel == AppPanel.LEADERBOARD) {
                            buildCurrentPanel()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        liveWallpaperView?.pause()
        FlipToFocusManager.stop()
        if (::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
        }
        if (AuthManager.isLoggedIn(this)) {
            triggerAutoSyncIfEligible(force = false)
        }
    }



    internal fun checkCelebration() {
        if (isFinishing || isDestroyed) return
        val todayStr = dateKeyFmt.format(Date())
        val dailyGoal = resolveGoalFor(todayStr)
        if (dailyGoal <= 0L) return

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val storedTodayFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val runningSessionSecs = if (currentTimerState != TimerState.IDLE) accumulatedStudy else 0L
        val totalTodayFocus = storedTodayFocus + runningSessionSecs

        if (totalTodayFocus >= dailyGoal) {
            val streak = sharedPrefs.safeInt("current_streak", 1)
            CelebrationEngine.checkAndCelebrate(
                activity = this,
                todayTotalSecs = totalTodayFocus,
                dailyGoalSecs = dailyGoal,
                streakTargetSecs = dailyGoal,
                currentStreak = streak
            )
        }
    }

    internal var ongoingLectureDialogShowing = false
    internal var switchLectureDialog: Dialog? = null
    internal var scheduledLecturePromptDialog: Dialog? = null

    private fun checkOngoingScheduledLecturePrompt() {
        if (ongoingLectureDialogShowing || currentTimerState != TimerState.IDLE) return

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }
        if (items.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val currentSecsInMin = cal.get(Calendar.SECOND)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val endMins = parseTimeToMinutes(item.endTime) ?: continue

            if (currentMins in startMins until endMins) {
                val skipKey = "skipped_lecture_${todayStr}_${item.title}_${item.startTime}"
                if (sharedPrefs.getBoolean(skipKey, false)) {
                    continue
                }

                val remainingSecs = ((endMins - currentMins) * 60 - currentSecsInMin).toLong()
                if (remainingSecs <= 30L) continue

                showScheduledLecturePromptDialog(item, remainingSecs, skipKey)
                break
            }
        }
    }

    internal fun showScheduledLecturePromptDialog(item: LectureScheduleItem, remainingSecs: Long, skipKey: String) = BreakAndLectureDialogHelper(this).showScheduledLecturePromptDialog(item, remainingSecs, skipKey)

    internal var inactivityCheckDialogShowing = false
    internal var inactivityCheckDialog: Dialog? = null

    internal fun showInactivityCheckDialog() {
        inactivityCheckDialogShowing = true
        BreakAndLectureDialogHelper(this).showInactivityCheckDialog()
    }

    internal fun onInactivityDialogDismissed() {
        inactivityCheckDialogShowing = false
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed({ checkForUpdates(manual = false) }, 4000)
        handler.postDelayed({ maybePromptBatteryOptimization() }, 1200)
    }

    override fun onDestroy() {
        PlannerStateManager.removeListener(plannerGoalListener)
        liveWallpaperView?.release()
        systemDialogHelper.dismissDialogs()
        handler.removeCallbacks(holdToEndRunnable)
        pauseBlinkAnimator?.cancel()
        pauseBlinkAnimator = null
        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    internal fun toggleLandscapeControls() {
        if (!isLandscapeMode()) return
        val sharedPrefs = appPrefs
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        if (!isLandscapeEnabled) return
        if (!::controlActionContainer.isInitialized) return

        // In IDLE state, controls should remain visible so the Start button is always accessible
        if (currentTimerState == TimerState.IDLE) {
            controlActionContainer.visibility = View.VISIBLE
            controlActionContainer.alpha = 1f
            landscapeDockView?.visibility = View.VISIBLE
            landscapeDockView?.alpha = 1f
            landscapeHeaderView?.visibility = View.VISIBLE
            landscapeHeaderView?.alpha = 1f
            isZenModeActive = false
            return
        }

        val animDuration = 200L
        if (controlActionContainer.visibility == View.VISIBLE && controlActionContainer.alpha > 0.5f) {
            isZenModeActive = true
            controlActionContainer.animate()
                .alpha(0f)
                .setDuration(animDuration)
                .withEndAction {
                    controlActionContainer.visibility = View.GONE
                }
                .start()
            landscapeDockView?.animate()
                ?.alpha(0f)
                ?.setDuration(animDuration)
                ?.withEndAction {
                    landscapeDockView?.visibility = View.GONE
                }
                ?.start()
            landscapeHeaderView?.animate()
                ?.alpha(0f)
                ?.setDuration(animDuration)
                ?.withEndAction {
                    landscapeHeaderView?.visibility = View.GONE
                }
                ?.start()
            landscapeSubjectPillView?.animate()
                ?.alpha(0f)
                ?.setDuration(animDuration)
                ?.withEndAction {
                    landscapeSubjectPillView?.visibility = View.GONE
                }
                ?.start()
            if (::statusBadgeContainer.isInitialized) {
                statusBadgeContainer.animate().alpha(0f).setDuration(animDuration).start()
            }
        } else {
            isZenModeActive = false
            controlActionContainer.visibility = View.VISIBLE
            controlActionContainer.alpha = 0f
            controlActionContainer.animate()
                .alpha(1f)
                .setDuration(animDuration)
                .start()
            landscapeDockView?.visibility = View.VISIBLE
            landscapeDockView?.alpha = 0f
            landscapeDockView?.animate()
                ?.alpha(1f)
                ?.setDuration(animDuration)
                ?.start()
            landscapeHeaderView?.visibility = View.VISIBLE
            landscapeHeaderView?.alpha = 0f
            landscapeHeaderView?.animate()
                ?.alpha(1f)
                ?.setDuration(animDuration)
                ?.start()
            landscapeSubjectPillView?.visibility = View.VISIBLE
            landscapeSubjectPillView?.alpha = 0f
            landscapeSubjectPillView?.animate()
                ?.alpha(1f)
                ?.setDuration(animDuration)
                ?.start()
            if (::statusBadgeContainer.isInitialized) {
                statusBadgeContainer.animate().alpha(1f).setDuration(animDuration).start()
            }
        }
    }

    internal fun applyTrueFullscreenMode() {
        val sharedPrefs = appPrefs
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = isLandscapeMode()

        if (!isLandscape) {
            isZenModeActive = false
            if (!isPortraitFullscreenActive) {
                panelContainer.setOnClickListener(null)
            }
            return
        }

        if (isLandscapeEnabled) {
            if (::studyBottomNavBar.isInitialized) {
                studyBottomNavBar.animate().cancel()
                studyBottomNavBar.visibility = View.GONE
            }
            isZenModeActive = false
            if (::navHeader.isInitialized) navHeader.visibility = View.GONE
            if (::controlActionContainer.isInitialized) {
                controlActionContainer.visibility = View.VISIBLE
                controlActionContainer.alpha = 1f
            }
            landscapeDockView?.visibility = View.VISIBLE
            landscapeDockView?.alpha = 1f
            landscapeHeaderView?.visibility = View.VISIBLE
            landscapeHeaderView?.alpha = 1f
            landscapeSubjectPillView?.visibility = View.VISIBLE
            landscapeSubjectPillView?.alpha = 1f
            if (::statusBadgeContainer.isInitialized) {
                statusBadgeContainer.visibility = View.VISIBLE
                statusBadgeContainer.alpha = 1f
            }

            panelContainer.setOnClickListener {
                toggleLandscapeControls()
            }
            rootLayout.setOnClickListener {
                toggleLandscapeControls()
            }
        } else {
            isZenModeActive = false
            panelContainer.setOnClickListener(null)
            rootLayout.setOnClickListener(null)
        }
    }

    internal fun applyImmersiveModeForLandscape() {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = isLandscapeMode()
        if (::studyBottomNavBar.isInitialized) {
            studyBottomNavBar.visibility = if (isLandscape || currentPanel == AppPanel.SETTINGS || currentPanel == AppPanel.HEATMAP) View.GONE else View.VISIBLE
        }
        if (!isLandscape) {
            showSystemUI()
            return
        }
        if (isLandscapeEnabled) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (currentPanel == AppPanel.FOCUS && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) && !isPortraitFullscreenActive) {
            scheduleAutoFullscreen(7000L)
        }
    }

    private val autoFullscreenHandler = Handler(Looper.getMainLooper())
    private val autoFullscreenRunnable = Runnable {
        if (!isDestroyed && !isFinishing && currentPanel == AppPanel.FOCUS
            && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK)
            && !isPortraitFullscreenActive && !isLandscapeMode()) {
            enterPortraitFullscreenMode()
        }
    }

    internal fun scheduleAutoFullscreen(delayMs: Long = 6000L) {
        autoFullscreenHandler.removeCallbacks(autoFullscreenRunnable)
        if (currentPanel == AppPanel.FOCUS && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) && !isPortraitFullscreenActive) {
            autoFullscreenHandler.postDelayed(autoFullscreenRunnable, delayMs)
        }
    }

    internal fun cancelAutoFullscreen() {
        autoFullscreenHandler.removeCallbacks(autoFullscreenRunnable)
    }

    internal fun togglePortraitFullscreenMode() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFullscreenToggleTime < 350L) return
        lastFullscreenToggleTime = now

        if (isLandscapeMode()) return
        if (currentPanel != AppPanel.FOCUS) return
        if (isPortraitFullscreenActive) {
            exitPortraitFullscreenMode()
        } else {
            enterPortraitFullscreenMode()
        }
    }

    internal fun enterPortraitFullscreenMode() {
        if (isPortraitFullscreenActive) return
        isPortraitFullscreenActive = true
        cancelAutoFullscreen()
        hideSystemUI()

        val fadeViews = listOfNotNull(
            if (::navHeader.isInitialized) navHeader else null,
            modeOptionsPill,
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer else null,
            extraControlsContainer,
            gestureHintView,
            if (::controlActionContainer.isInitialized) controlActionContainer else null,
            statsFloatingIcon
        )

        for (v in fadeViews) {
            v.animate().alpha(0f).setDuration(400).withEndAction {
                if (isPortraitFullscreenActive) v.visibility = View.INVISIBLE
            }.start()
        }

        if (::studyBottomNavBar.isInitialized) {
            studyBottomNavBar.setBarVisibility(false, animated = true)
        }

        applyPortraitFullscreenLayout()
        panelContainer.setOnClickListener {
            if (isPortraitFullscreenActive) {
                exitPortraitFullscreenMode()
            }
        }
        rootLayout.setOnClickListener {
            if (isPortraitFullscreenActive) {
                exitPortraitFullscreenMode()
            }
        }
        performMicroHaptic(window.decorView)
    }

    internal fun exitPortraitFullscreenMode() {
        if (!isPortraitFullscreenActive) return
        isPortraitFullscreenActive = false
        showSystemUI()
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isLandscape = isLandscapeMode()
        if (!isLandscape) {
            val revealViews = mutableListOf<View>()
            if (::navHeader.isInitialized) { navHeader.visibility = View.VISIBLE; revealViews.add(navHeader) }
            modeOptionsPill?.let { it.visibility = View.VISIBLE; revealViews.add(it) }
            panelContainer.findViewWithTag<View>("mode_pill")?.let { pill ->
                pill.visibility = if (currentTimerState == TimerState.IDLE) View.VISIBLE else View.GONE
            }
            if (::statusBadgeContainer.isInitialized) { statusBadgeContainer.visibility = View.VISIBLE; revealViews.add(statusBadgeContainer) }
            extraControlsContainer?.let { it.visibility = View.VISIBLE; revealViews.add(it) }
            val isHintDismissed = sharedPrefs.getBoolean("gesture_hint_dismissed", false)
            if (!isHintDismissed && currentTimerState == TimerState.IDLE) {
                gestureHintView?.let { it.visibility = View.VISIBLE; revealViews.add(it) }
            }
            if (::controlActionContainer.isInitialized) { controlActionContainer.visibility = View.VISIBLE; revealViews.add(controlActionContainer) }
            statsFloatingIcon?.let { it.visibility = View.VISIBLE; revealViews.add(it) }

            for (v in revealViews) {
                v.animate().alpha(1f).setDuration(350).start()
            }

            if (::studyBottomNavBar.isInitialized && currentPanel != AppPanel.SETTINGS && currentPanel != AppPanel.HEATMAP) {
                studyBottomNavBar.setBarVisibility(true, animated = true)
            }

            applyPortraitFullscreenLayout()
            panelContainer.setOnClickListener(null)
            rootLayout.setOnClickListener(null)
            performMicroHaptic(window.decorView)

            // Re-arm auto-fullscreen if currently studying/breaking
            if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
                scheduleAutoFullscreen(7000L)
            }
        } else {
            if (::studyBottomNavBar.isInitialized) {
                studyBottomNavBar.animate().cancel()
                studyBottomNavBar.visibility = View.GONE
            }
        }
    }

    internal fun applyPortraitFullscreenLayout() {
        val isLandscape = isLandscapeMode()
        if (isLandscape) return
        if (!::studyTimerDisplay.isInitialized || !::breakTimerDisplay.isInitialized) return
        val isBreaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
        val hasRing = (timerMode == "COUNTDOWN")

        if (::timerRing.isInitialized) {
            timerRing.visibility = if (hasRing) View.VISIBLE else View.GONE
            timerRing.isFullscreen = (isPortraitFullscreenActive && hasRing)
        }

        studyTimerDisplay.translationY = 0f
        breakTimerDisplay.translationY = 0f
        if (::timerRing.isInitialized) timerRing.translationY = 0f

        if (!isLandscape && isPortraitFullscreenActive) {
            val activeText = if (isBreaking) breakTimerDisplay.text.toString() else studyTimerDisplay.text.toString()
            val fullscreenTextSize = when {
                hasRing && activeText.length <= 5 -> 68f
                hasRing -> 56f
                activeText.length <= 5 -> 86f
                else -> 78f
            }

            if (isBreaking) {
                breakTimerDisplay.visibility = View.VISIBLE
                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                breakTimerDisplay.textSize = fullscreenTextSize
                breakTimerDisplay.setPadding(0, 0, 0, 0)

                studyTimerDisplay.visibility = if (currentTimerState == TimerState.PAUSED) View.VISIBLE else View.INVISIBLE
                if (currentTimerState == TimerState.PAUSED) {
                    studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                    studyTimerDisplay.textSize = 22f
                    studyTimerDisplay.setPadding(0, 0, 0, 44)
                }
            } else {
                studyTimerDisplay.visibility = View.VISIBLE
                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                studyTimerDisplay.textSize = fullscreenTextSize
                studyTimerDisplay.setPadding(0, 0, 0, 0)

                breakTimerDisplay.visibility = if (currentTimerState == TimerState.PAUSED) View.VISIBLE else View.INVISIBLE
                if (currentTimerState == TimerState.PAUSED) {
                    breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                    breakTimerDisplay.textSize = 22f
                    breakTimerDisplay.setPadding(0, 0, 0, 44)
                }
            }
        } else if (!isLandscape) {
            studyTimerDisplay.visibility = View.VISIBLE
            val showBreak = isBreaking || (currentBreakSeconds > 0L && currentTimerState == TimerState.PAUSED && timerMode != "COUNTDOWN")
            breakTimerDisplay.visibility = if (showBreak) View.VISIBLE else View.GONE
            val regularTextSize = if (hasRing) 64f else 78f
            val smallSubTextSize = 22f

            if (isBreaking) {
                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                breakTimerDisplay.textSize = regularTextSize
                breakTimerDisplay.setPadding(0, 0, 0, 5)

                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                studyTimerDisplay.textSize = smallSubTextSize
                studyTimerDisplay.setPadding(0, 0, 0, 44)
            } else {
                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                studyTimerDisplay.textSize = regularTextSize
                studyTimerDisplay.setPadding(0, 0, 0, 5)

                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                breakTimerDisplay.textSize = smallSubTextSize
                breakTimerDisplay.setPadding(0, 0, 0, 44)
            }
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
        updateStatusBarIcons()
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
        updateStatusBarIcons()
    }

    fun showSubjectPickerDialog(allowActiveSwitch: Boolean = false) {
        SubjectDialogHelper(this).showSubjectPickerDialog(allowActiveSwitch)
    }

    internal fun checkPomoSubjectPrompt() {
        SubjectDialogHelper(this).checkPomoSubjectPrompt()
    }

    internal fun showDeleteSubjectConfirmDialog(subj: SubjectTag) {
        SubjectDialogHelper(this).showDeleteSubjectConfirmDialog(subj)
    }

    fun showAddCustomSubjectDialog(onSubjectCreated: ((SubjectTag) -> Unit)? = null) {
        SubjectDialogHelper(this).showAddCustomSubjectDialog(onSubjectCreated)
    }

    internal fun showPieChartDetailsModal(
        initialDateKey: String = SubjectTagManager.getTodayKey(),
        initialPeriod: SubjectTagManager.SubjectStatsPeriod = SubjectTagManager.SubjectStatsPeriod.TODAY
    ) {
        SubjectDialogHelper(this).showPieChartDetailsModal(initialDateKey, initialPeriod)
    }

    private var lastForegroundSyncCheckTime = 0L

    internal fun triggerAutoSyncIfEligible(force: Boolean = false) {
        if (!AuthManager.isLoggedIn(this)) return
        val now = SystemClock.elapsedRealtime()
        if (!force && (now - lastForegroundSyncCheckTime < 30_000L)) return
        lastForegroundSyncCheckTime = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (remoteMeta, rawRecord) = CloudSyncManager.fetchRemoteMetadata(this@MainActivity)
                if (remoteMeta != null && rawRecord != null) {
                    val localTs = BackupManager(this@MainActivity).getLastModifiedTimestamp()
                    val cloudTs = maxOf(remoteMeta.lastModifiedTimestamp, remoteMeta.updatedAt)
                    if (cloudTs > localTs + 1500L) {
                        android.util.Log.i("MainActivity", "Live auto-sync: Cloud data ($cloudTs) is newer than local ($localTs). Merging in background...")
                        val merged = CloudSyncManager.mergeCloudAndLocalData(this@MainActivity, rawRecord)
                        if (merged) {
                            withContext(Dispatchers.Main) {
                                if (!isDestroyed && !isFinishing) {
                                    statsDirty = true
                                    statsSnapshotCache = null
                                    tabPageCache.clear()
                                    themeCoordinator.applyThemeCoordinates()
                                    navigateToPanel(currentPanel)
                                    StudyWidgetProvider.refresh(this@MainActivity)
                                }
                            }
                        }
                    } else if (localTs > cloudTs + 1500L) {
                        android.util.Log.i("MainActivity", "Live auto-sync: Local data ($localTs) is newer than cloud ($cloudTs). Uploading to cloud in background...")
                        CloudSyncManager.syncDataToCloud(this@MainActivity, force = true)
                    }
                } else if (remoteMeta == null && rawRecord == null) {
                    android.util.Log.i("MainActivity", "Live auto-sync: Initializing fresh cloud account with local data...")
                    CloudSyncManager.syncDataToCloud(this@MainActivity, force = true)
                }
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Auto-sync check non-fatal exception", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        hasPlayedStatsEntranceAnimation = false
        CommunityPanelBuilder.cancelActivePolling()
        if (AuthManager.isLoggedIn(this)) {
            triggerAutoSyncIfEligible(force = false)
        }
    }

}





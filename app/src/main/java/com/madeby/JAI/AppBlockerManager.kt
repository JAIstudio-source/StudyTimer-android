package com.madeby.JAI

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages App Blocker configuration, permission checks, application filtering,
 * and foreground distraction detection.
 *
 * Strictly optional: No services or checks run unless explicitly enabled by the user.
 */
object AppBlockerManager {

    const val PREFS_NAME = "StudyTimerPrefs"
    const val KEY_BLOCKER_ENABLED = "app_blocker_enabled"
    const val KEY_BLOCKED_PACKAGES = "app_blocker_blocked_packages"
    const val KEY_INITIALIZED = "app_blocker_initialized"

    // Runtime-only emergency bypass. Does not persist to disk.
    // Reset immediately whenever the user reopens StudyTimer.
    private val emergencyBypassActive = AtomicBoolean(false)

    // Debounce to prevent multiple overlay launches within a short time window for the same app
    @Volatile
    private var lastOverlayLaunchTime: Long = 0L
    @Volatile
    private var currentBlockedPackage: String? = null
    private const val OVERLAY_DEBOUNCE_MS = 600L

    fun clearCurrentBlocked() {
        currentBlockedPackage = null
    }

    data class AppItem(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
        val isBlocked: Boolean
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCKER_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply()
    }

    fun isEmergencyBypassActive(): Boolean {
        return emergencyBypassActive.get()
    }

    fun activateEmergencyBypass() {
        emergencyBypassActive.set(true)
    }

    /**
     * Called whenever StudyTimer returns to the foreground (MainActivity.onResume).
     * Automatically arms the focus shield again.
     */
    fun onStudyTimerResumed(context: Context? = null) {
        emergencyBypassActive.set(false)
        currentBlockedPackage = null
        context?.let { BlockerOverlayWindowManager.hide(it) }
    }

    private var activeAudioManager: android.media.AudioManager? = null
    private var activeAudioFocusRequest: Any? = null

    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange != android.media.AudioManager.AUDIOFOCUS_GAIN && BlockerOverlayWindowManager.isShowing()) {
            activeAudioManager?.let { am ->
                am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
                am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
            }
        }
    }

    /**
     * Immediately seizes audio focus and sends media pause event to force-silence
     * any media/video playback from the restricted app.
     */
    fun muteBackgroundMedia(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
            activeAudioManager = audioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                activeAudioFocusRequest = focusRequest
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(audioFocusChangeListener, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerManager", "Failed to mute background media: ${e.message}")
        }
    }

    fun releaseAudioFocus() {
        try {
            val am = activeAudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = activeAudioFocusRequest as? android.media.AudioFocusRequest
                if (am != null && req != null) {
                    am.abandonAudioFocusRequest(req)
                }
            } else {
                @Suppress("DEPRECATION")
                am?.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
        activeAudioFocusRequest = null
    }

    @Volatile
    private var cachedBlockedPackages: Set<String>? = null

    fun getBlockedPackages(context: Context): Set<String> {
        val cached = cachedBlockedPackages
        if (cached != null) return cached
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(KEY_BLOCKED_PACKAGES, null) ?: emptySet()
        cachedBlockedPackages = set
        return set
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        cachedBlockedPackages = packages
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun setPackageBlocked(context: Context, packageName: String, blocked: Boolean) {
        val current = getBlockedPackages(context).toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        setBlockedPackages(context, current)
    }

    // --- Permissions ---

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasAllPermissions(context: Context): Boolean {
        return hasUsageStatsPermission(context) && hasOverlayPermission(context)
    }

    // --- System & Essential Package Filtering ---

    /**
     * Identifies core system packages that must never be blocked to prevent locking the device.
     */
    fun isSystemEssentialPackage(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.android.settings") return true
        if (packageName.startsWith("com.google.android.packageinstaller") || packageName.startsWith("com.android.packageinstaller")) return true
        if (packageName.startsWith("com.google.android.permissioncontroller") || packageName.startsWith("com.android.permissioncontroller")) return true
        if (packageName.startsWith("com.google.android.gms")) return true

        // 1. Phone / Dialer
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (defaultDialer != null && packageName == defaultDialer) return true
        if (packageName.contains("dialer") || packageName.contains("incallui")) return true

        // 2. Default SMS app
        val defaultSms = runCatching { android.provider.Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
        if (defaultSms != null && packageName == defaultSms) return true

        // 3. Home Launchers
        val homePackages = getHomeLauncherPackages(context)
        if (homePackages.contains(packageName)) return true

        return false
    }

    private fun getHomeLauncherPackages(context: Context): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveList.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    /**
     * Loads all launchable third-party/user apps, filtering out system essential tools.
     * Initializes default blocked set if not yet initialized.
     */
    fun loadInstalledApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val prefs = getPrefs(context)
        val isFirstInit = !prefs.getBoolean(KEY_INITIALIZED, false)
        val savedBlocked = prefs.getStringSet(KEY_BLOCKED_PACKAGES, null)

        val appItems = mutableListOf<AppItem>()
        val defaultBlockedSet = mutableSetOf<String>()

        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveList) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            // Skip self and essential system applications
            if (isSystemEssentialPackage(context, pkg)) continue

            val label = runCatching { resolveInfo.loadLabel(pm).toString() }.getOrDefault(pkg)
            val icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()

            val isBlocked = if (isFirstInit) {
                // By default, third-party apps start as blocked
                defaultBlockedSet.add(pkg)
                true
            } else {
                savedBlocked?.contains(pkg) == true
            }

            appItems.add(AppItem(packageName = pkg, label = label, icon = icon, isBlocked = isBlocked))
        }

        if (isFirstInit) {
            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putStringSet(KEY_BLOCKED_PACKAGES, defaultBlockedSet)
                .apply()
        }

        return appItems.sortedBy { it.label.lowercase() }
    }

    // --- Foreground Distraction Detection ---

    private class PkgLifecycle {
        var lastResumeTime: Long = 0L
        var lastPauseTime: Long = 0L
        fun reset() {
            lastResumeTime = 0L
            lastPauseTime = 0L
        }
    }

    private val reusableUsageEvent = android.app.usage.UsageEvents.Event()
    private val reusableTracker = HashMap<String, PkgLifecycle>(32)
    private val lifecyclePool = ArrayList<PkgLifecycle>(32)

    /**
     * Checks if the current foreground app is on the blocked list during an active study session.
     * Safe to invoke on background threads.
     * @return true if StudyTimer itself is the foreground application (enables adaptive polling power-saving).
     */
    fun checkForegroundApp(context: Context): Boolean {
        if (!isEnabled(context)) return false
        if (isEmergencyBypassActive()) return false
        val blockedSet = getBlockedPackages(context)
        if (blockedSet.isEmpty()) return false
        if (!hasAllPermissions(context)) return false

        val foregroundPkg = getForegroundPackage(context) ?: return false

        // If StudyTimer itself is in the foreground, dismiss overlay if showing
        if (foregroundPkg == context.packageName) {
            if (BlockerOverlayWindowManager.isShowing()) {
                BlockerOverlayWindowManager.hide(context)
            }
            return true
        }

        // If user is on Home launcher, phone dialer, or system UI:
        if (isSystemEssentialPackage(context, foregroundPkg)) {
            // If the blocker overlay is actively protecting the screen, keep it visible!
            // Do NOT dismiss it while the user is actively viewing the blocker shield.
            if (!BlockerOverlayWindowManager.isShowing()) {
                currentBlockedPackage = null
            }
            return false
        }

        if (blockedSet.contains(foregroundPkg)) {
            val now = System.currentTimeMillis()
            val isPackageChanged = (foregroundPkg != currentBlockedPackage)

            // If the user switched to a DIFFERENT blocked app or overlay isn't showing yet, show immediately!
            if (isPackageChanged || !BlockerOverlayWindowManager.isShowing() || (now - lastOverlayLaunchTime >= OVERLAY_DEBOUNCE_MS)) {
                lastOverlayLaunchTime = now
                currentBlockedPackage = foregroundPkg

                // 1. Immediately mute any background audio/video playback from the restricted app
                muteBackgroundMedia(context)

                // 2. Attach instant WindowManager system overlay (prevents visual flash)
                if (Settings.canDrawOverlays(context)) {
                    BlockerOverlayWindowManager.show(context, foregroundPkg)

                    // 3. Immediately kick the blocked app into background via Home intent to pause/stop it.
                    // This forces onPause() and onStop() within ~20ms with ZERO visual flicker,
                    // because the overlay sits on top on layer 2038 covering the screen!
                    try {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val options = android.app.ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
                        context.startActivity(homeIntent, options)
                    } catch (e: Exception) {
                        android.util.Log.e("AppBlockerManager", "Failed to send home intent: ${e.message}")
                    }
                } else {
                    // Fallback to activity if overlay permission is missing
                    launchBlockerOverlay(context, foregroundPkg)
                }
            }
        } else {
            // User opened an allowed third-party app
            currentBlockedPackage = null
            if (BlockerOverlayWindowManager.isShowing()) {
                BlockerOverlayWindowManager.hide(context)
            }
        }
        return false
    }

    private fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            ?: return null

        val now = System.currentTimeMillis()
        // 10-second lookback window ensures lifecycle events are captured across all OEMs and clock skews
        val startTime = now - 10000L
        val events = usageStatsManager.queryEvents(startTime, now) ?: return null

        reusableTracker.clear()

        while (events.hasNextEvent()) {
            events.getNextEvent(reusableUsageEvent)
            val pkg = reusableUsageEvent.packageName ?: continue
            val time = reusableUsageEvent.timeStamp
            val type = reusableUsageEvent.eventType

            val entry = reusableTracker.getOrPut(pkg) {
                if (lifecyclePool.isNotEmpty()) lifecyclePool.removeAt(lifecyclePool.size - 1).apply { reset() } else PkgLifecycle()
            }

            when (type) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (time >= entry.lastResumeTime) entry.lastResumeTime = time
                }
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (time >= entry.lastPauseTime) entry.lastPauseTime = time
                }
                @Suppress("DEPRECATION")
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (time >= entry.lastResumeTime) entry.lastResumeTime = time
                }
                @Suppress("DEPRECATION")
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (time >= entry.lastPauseTime) entry.lastPauseTime = time
                }
            }
        }

        var activeTopPkg: String? = null
        var activeMaxResume = 0L
        var fallbackTopPkg: String? = null
        var fallbackMaxResume = 0L

        for ((pkg, lifecycle) in reusableTracker) {
            if (lifecycle.lastResumeTime > 0L) {
                if (lifecycle.lastResumeTime >= lifecycle.lastPauseTime && lifecycle.lastResumeTime >= activeMaxResume) {
                    activeMaxResume = lifecycle.lastResumeTime
                    activeTopPkg = pkg
                }
                if (lifecycle.lastResumeTime >= fallbackMaxResume) {
                    fallbackMaxResume = lifecycle.lastResumeTime
                    fallbackTopPkg = pkg
                }
            }
            lifecyclePool.add(lifecycle)
        }

        return activeTopPkg ?: fallbackTopPkg
    }

    private fun launchBlockerOverlay(context: Context, blockedPackage: String) {
        try {
            val intent = Intent(context, BlockerOverlayActivity::class.java).apply {
                putExtra(BlockerOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val options = android.app.ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            context.startActivity(intent, options)
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerManager", "Failed to launch blocker overlay: ${e.message}", e)
        }
    }
}

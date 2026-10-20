package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Detects physical phone flip gesture (face-down on desk) to effortlessly initiate or pause focus sessions.
 * Combines Accelerometer gravity vector (Z < -7.0 m/s^2) with Proximity sensor proximity.
 */
object FlipToFocusManager : SensorEventListener {

    const val PREF_KEY_ENABLED = "flip_to_focus_enabled"
    const val PREF_KEY_PAUSE_ON_FACE_UP = "flip_to_focus_pause_face_up"

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null

    private var isRegistered = false
    private var isFaceDown = false
    private var lastProximityNear = false
    private var lastGravityZ = 9.8f

    private var onFaceDownAction: (() -> Unit)? = null
    private var onFaceUpAction: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var pendingFlipRunnable: Runnable? = null
    private const val STABILITY_DELAY_DOWN_MS = 850L // Stable resting window before starting
    private const val STABILITY_DELAY_UP_MS = 650L   // Stable pickup window before pausing
    private const val MIN_TRIGGER_INTERVAL_MS = 1500L // Prevent rapid jitter bounce
    private var lastTriggerEpoch: Long = 0L

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(PREF_KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(PREF_KEY_ENABLED, enabled).apply()
    }

    fun isPauseOnFaceUp(context: Context): Boolean {
        return getPrefs(context).getBoolean(PREF_KEY_PAUSE_ON_FACE_UP, true)
    }

    fun setPauseOnFaceUp(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(PREF_KEY_PAUSE_ON_FACE_UP, enabled).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
    }

    private var isVirtualOrMissingProximity = false
    private var lastGravityX = 0f
    private var lastGravityY = 0f
    private var lightSensor: Sensor? = null
    private var lastLux: Float = 100f
    private var hasLightSensor = false

    /**
     * Start observing sensor state if enabled in settings.
     */
    fun start(
        context: Context,
        onFaceDown: () -> Unit,
        onFaceUp: () -> Unit
    ) {
        if (!isEnabled(context)) {
            stop()
            return
        }

        onFaceDownAction = onFaceDown
        onFaceUpAction = onFaceUp

        if (isRegistered) return

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Check if optical proximity sensor is absent or virtual (common on Samsung F13, A13, etc.)
        val proxName = proximitySensor?.name?.lowercase() ?: ""
        val isHardwareFeaturePresent = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_PROXIMITY)
        isVirtualOrMissingProximity = (proximitySensor == null) ||
                proxName.contains("virtual") ||
                proxName.contains("ear") ||
                !isHardwareFeaturePresent

        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.let {
            hasLightSensor = true
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        isRegistered = true
    }

    /**
     * Unregister sensor listeners to preserve battery when activity is paused or timer stops.
     */
    fun stop() {
        pendingFlipRunnable?.let { handler.removeCallbacks(it) }
        pendingFlipRunnable = null
        if (isRegistered) {
            sensorManager?.unregisterListener(this)
            isRegistered = false
        }
        onFaceDownAction = null
        onFaceUpAction = null
        isFaceDown = false
        lastProximityNear = false
        hasLightSensor = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.getOrNull(0) ?: 10f
                val max = event.sensor.maximumRange
                // If distance is less than maximumRange (or close to 0), surface is covered
                lastProximityNear = distance < max && distance < 4.0f
                evaluateOrientation(event)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Gravity on 3 axes
                lastGravityX = event.values.getOrNull(0) ?: 0f
                lastGravityY = event.values.getOrNull(1) ?: 0f
                // Gravity on Z axis: ~9.8 when facing ceiling, ~ -9.8 when facing floor/desk
                lastGravityZ = event.values.getOrNull(2) ?: 9.8f
                evaluateOrientation(event)
            }
            Sensor.TYPE_LIGHT -> {
                lastLux = event.values.getOrNull(0) ?: 100f
                evaluateOrientation(event)
            }
        }
    }

    private fun evaluateOrientation(event: SensorEvent) {
        val currentlyFaceDown: Boolean
        val currentlyFaceUp: Boolean

        // Phone is lying relatively flat (not held vertically or heavily tilted):
        val isHorizontalFlat = Math.abs(lastGravityX) < 4.2f && Math.abs(lastGravityY) < 4.8f

        if (!isVirtualOrMissingProximity) {
            // Standard hardware proximity mode:
            // Z must be strongly pointing towards desk (Z < -6.8 m/s^2), phone reasonably flat, AND proximity sensor covered
            currentlyFaceDown = (lastGravityZ < -6.8f) && isHorizontalFlat && lastProximityNear
            // Lifted / Face up: Z is positive OR proximity uncovered with screen facing up
            currentlyFaceUp = (lastGravityZ > 2.5f && !lastProximityNear) || (lastGravityZ > 0f && (Math.abs(lastGravityX) > 5.5f || Math.abs(lastGravityY) > 5.5f))
        } else {
            // Virtual / missing proximity mode (e.g. devices without hardware IR proximity):
            val isRestingFlat = Math.abs(lastGravityX) < 2.5f && Math.abs(lastGravityY) < 2.5f
            val isZPointingDown = lastGravityZ < -7.8f
            val lightCorroboration = if (hasLightSensor) lastLux < 8.0f else true

            currentlyFaceDown = isZPointingDown && isRestingFlat && lightCorroboration
            currentlyFaceUp = (lastGravityZ > 1.8f) || (Math.abs(lastGravityX) > 4.5f || Math.abs(lastGravityY) > 4.5f)
        }

        if (currentlyFaceDown && !isFaceDown) {
            scheduleStateChange(targetFaceDown = true, STABILITY_DELAY_DOWN_MS)
        } else if (currentlyFaceUp && isFaceDown) {
            scheduleStateChange(targetFaceDown = false, STABILITY_DELAY_UP_MS)
        } else if (!currentlyFaceDown && targetPendingState == true) {
            // Cancel pending face-down transition if user lifted phone before stability window elapsed
            cancelPendingTransition()
        } else if (!currentlyFaceUp && targetPendingState == false) {
            // Cancel pending face-up transition if user put phone back down before stability window elapsed
            cancelPendingTransition()
        }
    }

    private var targetPendingState: Boolean? = null

    private fun scheduleStateChange(targetFaceDown: Boolean, delayMs: Long) {
        if (targetPendingState == targetFaceDown) return
        cancelPendingTransition()
        targetPendingState = targetFaceDown

        val runnable = Runnable {
            targetPendingState = null
            val now = System.currentTimeMillis()
            if (targetFaceDown != isFaceDown && (now - lastTriggerEpoch >= MIN_TRIGGER_INTERVAL_MS)) {
                lastTriggerEpoch = now
                isFaceDown = targetFaceDown
                triggerHapticFeedback()
                if (isFaceDown) {
                    onFaceDownAction?.invoke()
                } else {
                    onFaceUpAction?.invoke()
                }
            }
        }
        pendingFlipRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun cancelPendingTransition() {
        pendingFlipRunnable?.let { handler.removeCallbacks(it) }
        pendingFlipRunnable = null
        targetPendingState = null
    }

    private fun triggerHapticFeedback() {
        val app = StudyTimerApp_instance ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(75L)
            }
        } catch (_: Exception) {}
    }

    private var StudyTimerApp_instance: Context? = null
    fun setContext(context: Context) {
        StudyTimerApp_instance = context.applicationContext
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

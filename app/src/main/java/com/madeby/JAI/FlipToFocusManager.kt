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
    private const val STABILITY_DELAY_MS = 450L // Debounce stability window

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

        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
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
                // Gravity on Z axis: ~9.8 when facing ceiling, ~ -9.8 when facing floor/desk
                lastGravityZ = event.values.getOrNull(2) ?: 9.8f
                evaluateOrientation(event)
            }
        }
    }

    private fun evaluateOrientation(event: SensorEvent) {
        // Face down criteria: Z is strongly negative (screen pointing down) AND proximity sensor detects covering/desk
        val currentlyFaceDown = (lastGravityZ < -6.5f) && lastProximityNear
        // Face up criteria: Z is positive (screen pointing up) OR proximity clear
        val currentlyFaceUp = (lastGravityZ > 2.0f) && !lastProximityNear

        if (currentlyFaceDown && !isFaceDown) {
            scheduleStateChange(targetFaceDown = true, event)
        } else if (currentlyFaceUp && isFaceDown) {
            scheduleStateChange(targetFaceDown = false, event)
        } else if (!currentlyFaceDown && targetPendingState == true) {
            // Cancel pending face-down transition if user lifted phone before debounce
            cancelPendingTransition()
        }
    }

    private var targetPendingState: Boolean? = null

    private fun scheduleStateChange(targetFaceDown: Boolean, event: SensorEvent) {
        if (targetPendingState == targetFaceDown) return
        cancelPendingTransition()
        targetPendingState = targetFaceDown

        val runnable = Runnable {
            targetPendingState = null
            if (targetFaceDown != isFaceDown) {
                isFaceDown = targetFaceDown
                if (isFaceDown) {
                    triggerHapticFeedback()
                    onFaceDownAction?.invoke()
                } else {
                    triggerHapticFeedback()
                    onFaceUpAction?.invoke()
                }
            }
        }
        pendingFlipRunnable = runnable
        handler.postDelayed(runnable, STABILITY_DELAY_MS)
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

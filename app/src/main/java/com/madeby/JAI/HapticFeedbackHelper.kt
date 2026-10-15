package com.madeby.JAI

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized tactile haptics & vibration utility for StudyTimer.
 */
object HapticFeedbackHelper {

    fun isHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("haptics_enabled", true)
    }

    fun performMicroHaptic(context: Context, view: View? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    fun performMicroHapticTick(context: Context, view: View? = null, progressFraction: Float = 0f) {
        if (!isHapticsEnabled(context)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effect = if (progressFraction > 0.75f) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        } else {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        }
                        vibrator.vibrate(effect)
                        return
                    } else {
                        val amp = (35 + (progressFraction * 175)).toInt().coerceIn(1, 255)
                        vibrator.vibrate(VibrationEffect.createOneShot(10L, amp))
                        return
                    }
                }
            }
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    fun performHapticConfirm(context: Context, view: View? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (_: Exception) {}
    }
}

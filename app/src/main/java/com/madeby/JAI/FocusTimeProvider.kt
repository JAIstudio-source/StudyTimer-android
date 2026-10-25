package com.madeby.JAI

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unified, single source of truth for focus time calculations across the application.
 * Ensures Today's Focus Card, Insights Pie Chart, and Leaderboard display identical,
 * perfectly synchronized minute counts without drifting.
 */
object FocusTimeProvider {

    private const val PREFS_NAME = "StudyTimerPrefs"

    fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Centralized calculation for Today's total focus time in seconds.
     * Combines committed focus total for today and any active in-flight study session.
     */
    fun getTodayFocusSeconds(context: Context): Long {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = getTodayKey()
        val committedToday = sp.getLong("${todayStr}_focus_total", 0L)

        val timerStateStr = sp.getString("timerState", "IDLE") ?: "IDLE"
        val prePauseStr = sp.getString("pre_pause_state", sp.getString("prePauseState", "STUDYING")) ?: "STUDYING"
        val isTimerStudying = timerStateStr == "STUDYING" ||
            (timerStateStr == "PAUSED" && prePauseStr == "STUDYING")

        val runningStudy = if (isTimerStudying) {
            sp.getLong("accumulatedStudy", 0L).coerceAtLeast(0L)
        } else {
            0L
        }

        return (committedToday + runningStudy).coerceAtLeast(0L)
    }

    /**
     * Calculates total focus seconds for any given date.
     * If dateStr is today, includes active in-flight study seconds.
     */
    fun getFocusSecondsForDate(context: Context, dateStr: String): Long {
        val todayStr = getTodayKey()
        if (dateStr == todayStr) {
            return getTodayFocusSeconds(context)
        }
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getLong("${dateStr}_focus_total", 0L).coerceAtLeast(0L)
    }

    /**
     * Calculates the real focus seconds across a leaderboard period (DAILY, WEEKLY, MONTHLY).
     */
    fun getFocusSecondsForPeriod(context: Context, period: LeaderboardPeriod): Long {
        return when (period) {
            LeaderboardPeriod.DAILY -> getTodayFocusSeconds(context)
            LeaderboardPeriod.WEEKLY -> {
                val now = Calendar.getInstance()
                val mondayCal = WeekHelper.mondayOf(now)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = getTodayKey()

                var sum = 0L
                val scanCal = Calendar.getInstance().apply { timeInMillis = mondayCal.timeInMillis }
                while (!scanCal.after(now)) {
                    val dStr = sdf.format(scanCal.time)
                    sum += getFocusSecondsForDate(context, dStr)
                    scanCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                sum
            }
            LeaderboardPeriod.MONTHLY -> {
                val now = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val monthCal = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                var sum = 0L
                while (!monthCal.after(now)) {
                    val dStr = sdf.format(monthCal.time)
                    sum += getFocusSecondsForDate(context, dStr)
                    monthCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                sum
            }
        }
    }
}

package com.madeby.JAI

import java.util.Calendar
import java.util.Date

object StreakCalculator {

    const val QUALIFYING_SECS = 2700L

    fun longestStreak(
        dayFocus: List<Pair<Date, Long>>,
        goalFor: ((Date) -> Long)? = null
    ): Int {
        var longest = 0
        var run = 0
        var prevDayMs: Long? = null
        for ((date, f) in dayFocus.sortedBy { it.first.time }) {
            val consecutive = prevDayMs?.let { prev ->
                val cal = Calendar.getInstance().apply { timeInMillis = prev }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis == date.time
            } ?: false
            val goal = goalFor?.invoke(date) ?: QUALIFYING_SECS
            run = if (f >= goal) (if (consecutive) run + 1 else 1) else 0
            prevDayMs = date.time
            if (run > longest) longest = run
        }
        return longest
    }

    fun currentStreak(
        focusOn: (daysAgo: Int) -> Long,
        todayExtra: Long = 0L,
        goalFor: ((daysAgo: Int) -> Long)? = null
    ): Int {
        var streak = 0
        for (i in 0 until 365) {
            val focusSecs = focusOn(i) + (if (i == 0) todayExtra else 0L)
            val goal = goalFor?.invoke(i) ?: QUALIFYING_SECS
            if (i == 0 && focusSecs < goal) continue
            if (focusSecs >= goal) streak++
            else break
        }
        return streak
    }
}

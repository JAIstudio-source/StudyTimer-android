package com.madeby.JAI

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakCalculator {

    const val QUALIFYING_SECS = 2700L
    const val MAX_FREEZES_PER_MONTH = 2
    const val PREF_FROZEN_DATES = "streak_frozen_dates"

    fun getFrozenDates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getStringSet(PREF_FROZEN_DATES, emptySet()) ?: emptySet()
    }

    fun getUsedFreezesForMonth(context: Context, monthKey: String): Set<String> {
        // monthKey is "yyyy-MM"
        return getFrozenDates(context).filter { it.startsWith(monthKey) }.toSet()
    }

    fun getAvailableFreezesThisMonth(context: Context, date: Date = Date()): Int {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(date)
        val used = getUsedFreezesForMonth(context, monthKey).size
        return (MAX_FREEZES_PER_MONTH - used).coerceAtLeast(0)
    }

    fun applyStreakFreeze(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val monthKey = if (dateKey.length >= 7) dateKey.substring(0, 7) else return false
        val used = getUsedFreezesForMonth(context, monthKey)
        if (used.contains(dateKey)) return true // already frozen
        if (used.size >= MAX_FREEZES_PER_MONTH) return false // max reached

        val currentSet = prefs.getStringSet(PREF_FROZEN_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(dateKey)
        prefs.edit().putStringSet(PREF_FROZEN_DATES, currentSet).apply()
        return true
    }

    fun longestStreak(
        dayFocus: List<Pair<Date, Long>>,
        goalFor: ((Date) -> Long)? = null,
        isFrozen: ((Date) -> Boolean)? = null
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
            val qualifies = f >= goal || (isFrozen?.invoke(date) == true)
            run = if (qualifies) (if (consecutive) run + 1 else 1) else 0
            prevDayMs = date.time
            if (run > longest) longest = run
        }
        return longest
    }

    fun currentStreak(
        focusOn: (daysAgo: Int) -> Long,
        todayExtra: Long = 0L,
        goalFor: ((daysAgo: Int) -> Long)? = null,
        isFrozen: ((daysAgo: Int) -> Boolean)? = null
    ): Int {
        var streak = 0
        for (i in 0 until 365) {
            val focusSecs = focusOn(i) + (if (i == 0) todayExtra else 0L)
            val goal = goalFor?.invoke(i) ?: QUALIFYING_SECS
            val frozen = isFrozen?.invoke(i) == true
            if (i == 0 && focusSecs < goal && !frozen) continue
            if (focusSecs >= goal || frozen) streak++
            else break
        }
        return streak
    }
}

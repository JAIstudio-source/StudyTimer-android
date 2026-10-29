package com.madeby.JAI

import android.content.Context
import android.content.ContextWrapper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GoalHistoryEntry(
    val date: String,
    val goalSecs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

object GoalHistoryManager {

    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val HISTORY_KEY = "daily_goal_history_json"

    fun loadHistory(context: Context): List<GoalHistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        return parseHistory(raw)
    }

    fun parseHistory(raw: String): List<GoalHistoryEntry> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<GoalHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val d = obj.optString("date", "")
                val g = obj.optLong("goalSecs", 2700L)
                val ts = obj.optLong("timestamp", 0L)
                if (d.isNotBlank() && g > 0L) {
                    list.add(GoalHistoryEntry(d, g, ts))
                }
            }
            if (list.isEmpty() && raw.trim().startsWith("[")) {
                parseHistoryFallback(raw)
            } else {
                list.sortedBy { it.date }
            }
        } catch (_: Throwable) {
            parseHistoryFallback(raw)
        }
    }

    internal fun parseHistoryFallback(raw: String): List<GoalHistoryEntry> {
        if (!raw.trim().startsWith("[")) return emptyList()
        val list = mutableListOf<GoalHistoryEntry>()
        val objectRegex = Regex("""\{([^}]+)\}""")
        val dateRegex = Regex(""""date"\s*:\s*"([^"]+)"""")
        val goalRegex = Regex(""""goalSecs"\s*:\s*(-?\d+)""")
        val tsRegex = Regex(""""timestamp"\s*:\s*(\d+)""")

        for (match in objectRegex.findAll(raw)) {
            val body = match.groupValues[1]
            val date = dateRegex.find(body)?.groupValues?.get(1) ?: continue
            val goal = goalRegex.find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 2700L
            val ts = tsRegex.find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            if (date.isNotBlank() && goal > 0L) {
                list.add(GoalHistoryEntry(date, goal, ts))
            }
        }
        return list.sortedBy { it.date }
    }

    fun setDailyGoal(context: Context, goalSecs: Long, forDateStr: String? = null) {
        if (goalSecs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val targetDateStr = forDateStr?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val history = loadHistory(context).toMutableList()
        val existingIdx = history.indexOfFirst { it.date == targetDateStr }
        val newEntry = GoalHistoryEntry(targetDateStr, goalSecs, System.currentTimeMillis())
        if (existingIdx >= 0) {
            history[existingIdx] = newEntry
        } else {
            history.add(newEntry)
        }
        history.sortBy { it.date }
        val arr = JSONArray()
        for (e in history) {
            arr.put(JSONObject().apply {
                put("date", e.date)
                put("goalSecs", e.goalSecs)
                put("timestamp", e.timestamp)
            })
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val isTodayOrFuture = targetDateStr >= todayStr
        val priorConfiguredGoal = StatsEngine.getDailyGoalSecs(prefs)

        // Lock any past dates that had activity to the prior configured goal so future changes never bleed into the past
        if (isTodayOrFuture && priorConfiguredGoal > 0L && priorConfiguredGoal != goalSecs) {
            val allKeys = prefs.all.keys.toList()
            val focusDates = allKeys.filter { it.endsWith("_focus_total") }.map { it.removeSuffix("_focus_total") }
            val pastEditor = prefs.edit()
            for (pastDate in focusDates) {
                if (pastDate < targetDateStr && !prefs.contains("${pastDate}_goal_secs")) {
                    pastEditor.putLong("${pastDate}_goal_secs", priorConfiguredGoal)
                }
            }
            pastEditor.apply()
        }

        prefs.edit().apply {
            if (isTodayOrFuture) {
                putLong("daily_goal_secs", goalSecs)
                putInt("daily_goal_minutes", (goalSecs / 60L).toInt())
            }
            putLong("${targetDateStr}_goal_secs", goalSecs)
            putString(HISTORY_KEY, arr.toString())
            apply()
        }

        val act = (context as? MainActivity) ?: ((context as? ContextWrapper)?.baseContext as? MainActivity)
        act?.let { host ->
            host.invalidateStatsCache()
            host.recalculateStreak()
            StudyWidgetProvider.refresh(host)
        }
    }

    fun recordGoalChange(context: Context, dateStr: String, goalSecs: Long) {
        setDailyGoal(context, goalSecs, forDateStr = dateStr)
    }

    fun resolveGoalForDate(context: Context, dateStr: String, fallbackGlobalGoal: Long): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val hasKey = prefs.contains("${dateStr}_goal_secs")
        val explicit = if (hasKey) prefs.getLong("${dateStr}_goal_secs", 0L) else null
        if (explicit != null && explicit > 0L) {
            return explicit
        }

        val history = loadHistory(context)
        val matchingEntry = if (history.isNotEmpty()) history.filter { it.date <= dateStr }.maxByOrNull { it.date } else null
        val oldestEntry = if (history.isNotEmpty()) history.minByOrNull { it.date } else null

        val configuredGoal = StatsEngine.getDailyGoalSecs(prefs)
        val defaultGoal = if (configuredGoal > 0L) configuredGoal else (if (fallbackGlobalGoal > 0L) fallbackGlobalGoal else StatsEngine.DEFAULT_DAILY_GOAL_SECS)

        val result = when {
            matchingEntry != null && matchingEntry.goalSecs > 0L -> matchingEntry.goalSecs
            oldestEntry != null && oldestEntry.goalSecs > 0L -> oldestEntry.goalSecs
            else -> defaultGoal
        }

        // Permanently cache this day's goal so future changes to daily_goal_secs never alter past days!
        prefs.edit().putLong("${dateStr}_goal_secs", result).apply()

        return result
    }

    /**
     * Reconciles all historical dates without overwriting existing saved goals on specific days.
     */
    fun reconcileAllHistoricalGoals(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val globalGoal = StatsEngine.getDailyGoalSecs(prefs)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val history = loadHistory(context)
        if (history.isEmpty()) {
            setDailyGoal(context, globalGoal, forDateStr = todayStr)
        }

        val allKeys = prefs.all.keys.toList()
        val focusKeys = allKeys.filter { it.endsWith("_focus_total") }
        val snapshotKeys = allKeys.filter { it.endsWith("_planner_snapshot") }
        val allDates = (focusKeys.map { it.removeSuffix("_focus_total") } +
                        snapshotKeys.map { it.removeSuffix("_planner_snapshot") } +
                        setOf(todayStr)).toSet()

        for (dStr in allDates) {
            val goalKey = "${dStr}_goal_secs"
            // If the date already has a saved goal, DO NOT overwrite it!
            if (prefs.contains(goalKey) && prefs.getLong(goalKey, 0L) > 0L) {
                continue
            }
            val resolved = resolveGoalForDate(context, dStr, globalGoal)
            setDailyGoal(context, resolved, forDateStr = dStr)
        }
    }

    fun mergeCloudHistory(localJson: String, cloudJson: String): String {
        val localList = parseHistory(localJson)
        val cloudList = parseHistory(cloudJson)
        val mergedMap = mutableMapOf<String, GoalHistoryEntry>()

        for (e in localList + cloudList) {
            val existing = mergedMap[e.date]
            if (existing == null || e.timestamp >= existing.timestamp) {
                mergedMap[e.date] = e
            }
        }

        val sortedList = mergedMap.values.sortedBy { it.date }
        val arr = JSONArray()
        for (e in sortedList) {
            arr.put(JSONObject().apply {
                put("date", e.date)
                put("goalSecs", e.goalSecs)
                put("timestamp", e.timestamp)
            })
        }
        return arr.toString()
    }
}

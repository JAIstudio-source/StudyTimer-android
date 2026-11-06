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

    fun cleanFutureGoalCaches(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val allKeys = prefs.all.keys.toList()
        val futureGoalKeys = allKeys.filter { 
            it != "daily_goal_secs" && 
            it != "daily_goal_minutes" && 
            it.endsWith("_goal_secs") && 
            it.removeSuffix("_goal_secs").matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) && 
            it.removeSuffix("_goal_secs") > todayStr 
        }
        if (futureGoalKeys.isNotEmpty()) {
            val editor = prefs.edit()
            for (k in futureGoalKeys) {
                editor.remove(k)
            }
            editor.apply()
        }
    }

    fun setDailyGoal(context: Context, goalSecs: Long, forDateStr: String? = null) {
        if (goalSecs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val targetDateStr = forDateStr?.takeIf { it.isNotBlank() } ?: todayStr

        val isTodayOrFuture = targetDateStr >= todayStr
        val priorConfiguredGoal = StatsEngine.getDailyGoalSecs(prefs)
        val hasExplicitPriorGoal = prefs.contains("daily_goal_secs") || prefs.contains("daily_goal_minutes")

        // Lock any past dates that had activity to the prior configured goal so future changes never bleed into the past
        if (isTodayOrFuture && hasExplicitPriorGoal && priorConfiguredGoal > 0L && priorConfiguredGoal != goalSecs) {
            val allKeys = prefs.all.keys.toList()
            val focusDates = allKeys.filter { it.endsWith("_focus_total") }.map { it.removeSuffix("_focus_total") }
            val pastEditor = prefs.edit()
            for (pastDate in focusDates) {
                if (pastDate < todayStr && !prefs.contains("${pastDate}_goal_secs")) {
                    pastEditor.putLong("${pastDate}_goal_secs", priorConfiguredGoal)
                }
            }
            pastEditor.apply()
        }

        // Clean up any future-dated keys in prefs so future days always reflect the new goal
        if (isTodayOrFuture) {
            val allKeys = prefs.all.keys.toList()
            val futureGoalKeys = allKeys.filter { 
            it != "daily_goal_secs" && 
            it != "daily_goal_minutes" && 
            it.endsWith("_goal_secs") && 
            it.removeSuffix("_goal_secs").matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) && 
            it.removeSuffix("_goal_secs") > todayStr 
        }
            if (futureGoalKeys.isNotEmpty()) {
                val cleanEditor = prefs.edit()
                for (k in futureGoalKeys) {
                    cleanEditor.remove(k)
                }
                cleanEditor.apply()
            }
        }

        // In history, keep entries up to max(todayStr, targetDateStr), removing future dates that would override the current goal
        val maxAllowedDate = if (targetDateStr > todayStr) targetDateStr else todayStr
        val history = loadHistory(context).filter { it.date <= maxAllowedDate }.toMutableList()
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
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val configuredGoal = StatsEngine.getDailyGoalSecs(prefs)
        val currentActiveGoal = if (configuredGoal > 0L) configuredGoal else (if (fallbackGlobalGoal > 0L) fallbackGlobalGoal else StatsEngine.DEFAULT_DAILY_GOAL_SECS)

        // 1. For today and all next/future days, always return the current active configured goal!
        if (dateStr >= todayStr) {
            // Remove any stale cache that might have been accidentally saved for a future date
            if (dateStr > todayStr && prefs.contains("${dateStr}_goal_secs")) {
                prefs.edit().remove("${dateStr}_goal_secs").apply()
            }
            return currentActiveGoal
        }

        // 2. For past dates (dateStr < todayStr): check if explicitly saved for that day
        val hasKey = prefs.contains("${dateStr}_goal_secs")
        val explicit = if (hasKey) prefs.getLong("${dateStr}_goal_secs", 0L) else null
        if (explicit != null && explicit > 0L) {
            return explicit
        }

        // 3. For past dates without explicit key: find the latest historical entry on or before that date
        val history = loadHistory(context)
        val matchingEntry = if (history.isNotEmpty()) history.filter { it.date <= dateStr }.maxByOrNull { it.date } else null
        val oldestEntry = if (history.isNotEmpty()) history.minByOrNull { it.date } else null

        val result = when {
            matchingEntry != null && matchingEntry.goalSecs > 0L -> matchingEntry.goalSecs
            oldestEntry != null && oldestEntry.goalSecs > 0L -> oldestEntry.goalSecs
            else -> currentActiveGoal
        }

        // Permanently cache past day's goal so future changes to daily_goal_secs never alter past days!
        // ONLY cache for past days (dateStr < todayStr)!
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

        cleanFutureGoalCaches(context)

        // Self-heal corrupted historical entries that were reset to 2700s (45m) by prior update bug
        if (globalGoal > 0L && globalGoal != StatsEngine.DEFAULT_DAILY_GOAL_SECS) {
            repairCorruptedHistoricalGoals(context, globalGoal)
        }

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

        val editor = prefs.edit()
        var modified = false
        for (dStr in allDates) {
            if (dStr > todayStr) continue // Never lock future dates!
            val goalKey = "${dStr}_goal_secs"
            // If the date already has a saved goal, DO NOT overwrite it!
            if (prefs.contains(goalKey) && prefs.getLong(goalKey, 0L) > 0L) {
                continue
            }
            val resolved = resolveGoalForDate(context, dStr, globalGoal)
            editor.putLong(goalKey, resolved)
            modified = true
        }
        if (modified) editor.apply()
    }

    /**
     * Repairs past dates where daily goals were erroneously set to 2700s (45m)
     * by applying the user's active/chosen daily goal to those days.
     */
    fun repairCorruptedHistoricalGoals(context: Context, newGoalSecs: Long) {
        if (newGoalSecs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val allKeys = prefs.all.keys.toList()
        val editor = prefs.edit()
        var modified = false

        // 1. Repair date-specific keys that were set to default 2700s
        for (k in allKeys) {
            if (k != "daily_goal_secs" && k != "daily_goal_minutes" && k.endsWith("_goal_secs")) {
                val datePart = k.removeSuffix("_goal_secs")
                if (datePart.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
                    val currentVal = prefs.getLong(k, 0L)
                    if (currentVal == StatsEngine.DEFAULT_DAILY_GOAL_SECS) {
                        editor.putLong(k, newGoalSecs)
                        modified = true
                    }
                }
            }
        }

        // 2. Repair history entries that had 2700s
        val history = loadHistory(context)
        if (history.isNotEmpty()) {
            val updatedHistory = history.map { entry ->
                if (entry.goalSecs == StatsEngine.DEFAULT_DAILY_GOAL_SECS) {
                    entry.copy(goalSecs = newGoalSecs)
                } else {
                    entry
                }
            }
            val arr = JSONArray()
            for (e in updatedHistory) {
                arr.put(JSONObject().apply {
                    put("date", e.date)
                    put("goalSecs", e.goalSecs)
                    put("timestamp", e.timestamp)
                })
            }
            editor.putString(HISTORY_KEY, arr.toString())
            modified = true
        }

        if (modified) editor.apply()

        // 3. Ensure active daily goal is updated
        setDailyGoal(context, newGoalSecs, forDateStr = todayStr)
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

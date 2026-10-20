package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class GoalHistoryStatus { ACHIEVED, DEFICIT, NOT_COMPLETED, NOT_SCHEDULED }

data class GoalInsights(
    val totalDaysTracked: Int,
    val completedDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float, // 0.0f to 1.0f
    val bestWeekday: String
)

data class OverallPlannerInsights(
    val bestStreakGoalTitle: String,
    val bestStreakDays: Int,
    val mostConsistentGoalTitle: String,
    val mostConsistentPct: Int,
    val bestWeekdayName: String,
    val overallCompletionPct: Int,
    val hasEnoughData: Boolean
)

data class GoalProgress(
    val goalId: String,
    val actualMinutes: Int,
    val targetMinutes: Int,
    val isAchieved: Boolean
)

object PlannerHistoryManager {

    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val SNAPSHOT_KEY_SUFFIX = "_planner_snapshot"
    private const val KEY_DAILY_GOAL_DURATIONS = "daily_goal_durations_json"

    fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun isGoalScheduledOnDay(scheduledDays: List<Int>, cal: Calendar): Boolean {
        if (scheduledDays.isEmpty()) return true
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return scheduledDays.contains(dayOfWeek)
    }

    fun getDailyGoalDurations(context: Context, dateKey: String = getTodayKey()): Map<String, Long> {
        val map = HashMap<String, Long>()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dailyStr = prefs.getString(KEY_DAILY_GOAL_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey)
            if (dayObj != null) {
                val keys = dayObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = dayObj.optLong(k, 0L)
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun recordGoalStudyTime(context: Context, goalId: String, durationSecs: Long, dateKey: String = getTodayKey()) {
        if (durationSecs <= 0L || goalId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dailyStr = prefs.getString(KEY_DAILY_GOAL_DURATIONS, "{}") ?: "{}"
        try {
            val dailyJson = JSONObject(dailyStr)
            val dayObj = dailyJson.optJSONObject(dateKey) ?: JSONObject()
            val curSecs = dayObj.optLong(goalId, 0L)
            dayObj.put(goalId, curSecs + durationSecs)
            dailyJson.put(dateKey, dayObj)
            prefs.edit().putString(KEY_DAILY_GOAL_DURATIONS, dailyJson.toString()).apply()
        } catch (_: Exception) {}
    }

    fun calculateGoalProgress(
        goals: List<PlannerGoal>,
        totalFocusSecs: Long,
        dailySubjectDurations: Map<String, Long>,
        dailyGoalDurations: Map<String, Long> = emptyMap(),
        dailySubTopicDurations: Map<String, Map<String, Long>> = emptyMap()
    ): Map<String, GoalProgress> {
        val totalFocusMins = (totalFocusSecs / 60).toInt()
        val result = mutableMapOf<String, GoalProgress>()

        // 1. Process goals that have dedicated tracked study time (selected as active focus task)
        val goalsWithDedicatedTime = mutableSetOf<String>()
        for (g in goals) {
            val dedicatedSecs = dailyGoalDurations[g.id] ?: 0L
            if (dedicatedSecs > 0L) {
                goalsWithDedicatedTime.add(g.id)
                val dedicatedMins = (dedicatedSecs / 60).toInt()
                val isAchieved = if (g.targetMinutes > 0) dedicatedMins >= g.targetMinutes else g.completed
                result[g.id] = GoalProgress(g.id, actualMinutes = dedicatedMins, targetMinutes = g.targetMinutes, isAchieved = isAchieved)
            }
        }

        // 2. Separate sub-topic goals, subject-tagged goals, and untagged goals
        val subTopicGoals = goals.filter {
            !goalsWithDedicatedTime.contains(it.id) &&
            !it.subjectId.isNullOrBlank() &&
            it.subjectId != "all" &&
            !it.subTopic.isNullOrBlank()
        }
        val remainingSubjectGoals = goals.filter {
            !goalsWithDedicatedTime.contains(it.id) &&
            !it.subjectId.isNullOrBlank() &&
            it.subjectId != "all" &&
            it.subTopic.isNullOrBlank()
        }
        val remainingUntaggedGoals = goals.filter {
            !goalsWithDedicatedTime.contains(it.id) &&
            (it.subjectId.isNullOrBlank() || it.subjectId == "all")
        }

        // Process sub-topic goals isolated per sub-topic
        for (g in subTopicGoals) {
            val subId = g.subjectId!!
            val topic = g.subTopic!!
            val topicSecs = dailySubTopicDurations[subId]?.get(topic) ?: 0L
            val topicMins = (topicSecs / 60).toInt()
            val isAchieved = if (g.targetMinutes > 0) topicMins >= g.targetMinutes else g.completed
            result[g.id] = GoalProgress(g.id, actualMinutes = topicMins, targetMinutes = g.targetMinutes, isAchieved = isAchieved)
        }

        // Process remaining subject-tagged goals grouped by subjectId (isolated per subject)
        val groupedSubjectGoals = remainingSubjectGoals.groupBy { it.subjectId!! }
        for ((subId, subGoalsList) in groupedSubjectGoals) {
            val totalSubMins = ((dailySubjectDurations[subId] ?: 0L) / 60).toInt()
            val dedicatedSubMins = goals.filter { it.subjectId == subId && goalsWithDedicatedTime.contains(it.id) }
                .sumOf { ((dailyGoalDurations[it.id] ?: 0L) / 60).toInt() }
            val availableSubjectMins = (totalSubMins - dedicatedSubMins).coerceAtLeast(0)
            var remainingSubjectMins = availableSubjectMins

            val goalsWithTarget = subGoalsList.filter { it.targetMinutes > 0 }
            val lastTargetGoalId = goalsWithTarget.lastOrNull()?.id

            for (g in subGoalsList) {
                if (g.targetMinutes <= 0) {
                    val isAchieved = g.completed
                    result[g.id] = GoalProgress(g.id, actualMinutes = availableSubjectMins, targetMinutes = 0, isAchieved = isAchieved)
                } else {
                    val allocated: Int
                    if (g.id == lastTargetGoalId) {
                        allocated = remainingSubjectMins
                        remainingSubjectMins = (remainingSubjectMins - g.targetMinutes).coerceAtLeast(0)
                    } else {
                        allocated = minOf(remainingSubjectMins, g.targetMinutes)
                        remainingSubjectMins = (remainingSubjectMins - g.targetMinutes).coerceAtLeast(0)
                    }
                    val isAchieved = allocated >= g.targetMinutes
                    result[g.id] = GoalProgress(g.id, actualMinutes = allocated, targetMinutes = g.targetMinutes, isAchieved = isAchieved)
                }
            }
        }

        // 3. Compute untagged / all-subjects focus pool
        val dedicatedUntaggedMins = goals.filter { goalsWithDedicatedTime.contains(it.id) && (it.subjectId.isNullOrBlank() || it.subjectId == "all") }
            .sumOf { ((dailyGoalDurations[it.id] ?: 0L) / 60).toInt() }
        var untaggedPoolMins = (totalFocusMins - dedicatedUntaggedMins).coerceAtLeast(0)

        val untaggedGoalsWithTarget = remainingUntaggedGoals.filter { it.targetMinutes > 0 }
        val lastUntaggedTargetGoalId = untaggedGoalsWithTarget.lastOrNull()?.id

        for (g in remainingUntaggedGoals) {
            if (g.targetMinutes <= 0) {
                val isAchieved = g.completed
                result[g.id] = GoalProgress(g.id, actualMinutes = untaggedPoolMins, targetMinutes = 0, isAchieved = isAchieved)
            } else {
                val allocated: Int
                if (g.id == lastUntaggedTargetGoalId) {
                    allocated = untaggedPoolMins
                    untaggedPoolMins = (untaggedPoolMins - g.targetMinutes).coerceAtLeast(0)
                } else {
                    allocated = minOf(untaggedPoolMins, g.targetMinutes)
                    untaggedPoolMins = (untaggedPoolMins - g.targetMinutes).coerceAtLeast(0)
                }
                val isAchieved = allocated >= g.targetMinutes
                result[g.id] = GoalProgress(g.id, actualMinutes = allocated, targetMinutes = g.targetMinutes, isAchieved = isAchieved)
            }
        }

        return result
    }

    fun snapshotToday(context: Context, goals: List<PlannerGoal>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        snapshotForDate(context, todayStr, goals)
    }

    fun snapshotForDate(context: Context, dateStr: String, goals: List<PlannerGoal>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dayFocusSecs = prefs.getLong("${dateStr}_focus_total", 0L)
        val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(context, dateStr)
        val dailyGoalDurations = getDailyGoalDurations(context, dateStr)

        val dailySubTopicDurations = mutableMapOf<String, Map<String, Long>>()
        for (g in goals) {
            val subId = g.subjectId
            if (!subId.isNullOrBlank() && !dailySubTopicDurations.containsKey(subId)) {
                dailySubTopicDurations[subId] = SubjectTagManager.getDailySubTopicDurations(context, subId, dateStr)
            }
        }

        val progressMap = calculateGoalProgress(goals, dayFocusSecs, dailySubjectDurations, dailyGoalDurations, dailySubTopicDurations)

        val array = JSONArray()
        for (g in goals) {
            val prog = progressMap[g.id]
            val actualMins = prog?.actualMinutes ?: 0
            val isDurationAchieved = if (g.targetMinutes > 0) (actualMins >= g.targetMinutes) else g.completed
            val isCompleted = g.completed || isDurationAchieved
            // Strictly require duration achievement when targetMinutes > 0
            val isAchieved = if (g.targetMinutes > 0) isDurationAchieved else isCompleted

            val obj = JSONObject().apply {
                put("goalId", g.id)
                put("title", g.title)
                put("note", g.note)
                put("targetMinutes", g.targetMinutes)
                put("actualMinutes", actualMins)
                put("completed", isCompleted)
                put("checkedAt", if (isCompleted && g.checkedAt == 0L) System.currentTimeMillis() else g.checkedAt)
                put("isAchieved", isAchieved)
                if (g.subjectId != null) {
                    put("subjectId", g.subjectId)
                }
                if (g.subTopic != null) {
                    put("subTopic", g.subTopic)
                }
                if (g.scheduledDays.isNotEmpty()) {
                    val daysArr = JSONArray()
                    g.scheduledDays.forEach { daysArr.put(it) }
                    put("scheduledDays", daysArr)
                }
            }
            array.put(obj)
        }
        prefs.edit().putString("${dateStr}${SNAPSHOT_KEY_SUFFIX}", array.toString()).apply()
    }

    fun loadDaySnapshot(context: Context, dateStr: String): List<PlannerGoalSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("${dateStr}${SNAPSHOT_KEY_SUFFIX}", null) ?: return emptyList()
        return parseSnapshots(jsonStr)
    }

    private fun parseSnapshots(jsonStr: String): List<PlannerGoalSnapshot> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PlannerGoalSnapshot>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val completed = obj.optBoolean("completed", false)
                val isAchieved = obj.optBoolean("isAchieved", completed)
                val subId = if (obj.has("subjectId") && !obj.isNull("subjectId")) obj.optString("subjectId") else null
                val subTopic = if (obj.has("subTopic") && !obj.isNull("subTopic")) obj.optString("subTopic") else null
                val daysList = mutableListOf<Int>()
                val daysArr = obj.optJSONArray("scheduledDays")
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) daysList.add(daysArr.getInt(d))
                }
                list.add(
                    PlannerGoalSnapshot(
                        goalId = obj.optString("goalId", ""),
                        title = obj.optString("title", ""),
                        targetMinutes = obj.optInt("targetMinutes", 0),
                        completed = completed,
                        checkedAt = obj.optLong("checkedAt", 0L),
                        isAchieved = isAchieved,
                        subjectId = subId,
                        note = obj.optString("note", ""),
                        subTopic = subTopic,
                        scheduledDays = daysList,
                        actualMinutes = obj.optInt("actualMinutes", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadGoalSnapshotsMap(context: Context, goalId: String): Map<String, PlannerGoalSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snapshotsMap = mutableMapOf<String, PlannerGoalSnapshot>()
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.endsWith(SNAPSHOT_KEY_SUFFIX)) {
                val dateStr = key.removeSuffix(SNAPSHOT_KEY_SUFFIX)
                val rawJson = prefs.getString(key, null) ?: continue
                val snapshots = parseSnapshots(rawJson)
                val match = snapshots.firstOrNull { it.goalId == goalId }
                if (match != null) {
                    snapshotsMap[dateStr] = match
                }
            }
        }
        return snapshotsMap
    }

    /**
     * Loads detailed completion status per date ("yyyy-MM-dd" -> GoalHistoryStatus)
     * for a specific goal across all relevant calendar days.
     * Accurately distinguishes between:
     * - ACHIEVED: Goal completed & target verified (✓)
     * - DEFICIT: Goal completed early / deficit without meeting duration (✕)
     * - NOT_COMPLETED: Goal was scheduled but missed / not reached on reset (✕)
     * - NOT_SCHEDULED: Goal was not meant to be completed on this day (—)
     */
    fun loadGoalHistoryDetailed(
        context: Context,
        goalId: String,
        scheduledDays: List<Int> = emptyList(),
        createdAt: Long = 0L
    ): Map<String, GoalHistoryStatus> {
        val historyMap = mutableMapOf<String, GoalHistoryStatus>()
        val snapshotsMap = loadGoalSnapshotsMap(context, goalId)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = getTodayKey()
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val startCal = Calendar.getInstance().apply {
            if (createdAt > 0L) {
                timeInMillis = createdAt
            } else {
                add(Calendar.DAY_OF_YEAR, -30)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dateStr in snapshotsMap.keys) {
            val d = runCatching { sdf.parse(dateStr) }.getOrNull()
            if (d != null && d.before(startCal.time)) {
                startCal.time = d
            }
        }

        val currCal = startCal.clone() as Calendar
        while (!currCal.after(todayCal)) {
            val dateKey = sdf.format(currCal.time)
            val match = snapshotsMap[dateKey]
            val isScheduled = isGoalScheduledOnDay(scheduledDays, currCal)

            if (!isScheduled) {
                historyMap[dateKey] = GoalHistoryStatus.NOT_SCHEDULED
            } else {
                if (match != null) {
                    val isTargetMet = match.isAchieved || (match.targetMinutes > 0 && match.actualMinutes >= match.targetMinutes) || (match.targetMinutes == 0 && match.completed)
                    val isEarlyDeficit = match.completed && match.targetMinutes > 0 && match.actualMinutes < match.targetMinutes
                    val status = when {
                        isTargetMet -> GoalHistoryStatus.ACHIEVED
                        isEarlyDeficit -> GoalHistoryStatus.DEFICIT
                        dateKey == todayStr -> null // Today is active and in-progress: never mark as NOT_COMPLETED or missed
                        else -> GoalHistoryStatus.NOT_COMPLETED
                    }
                    if (status != null) {
                        historyMap[dateKey] = status
                    }
                } else {
                    if (dateKey == todayStr) {
                        // Today is in progress - leave unmapped so today ring/active indicator shows
                    } else {
                        // Past scheduled date where goal was NOT completed: explicitly mark as NOT_COMPLETED (✕)
                        historyMap[dateKey] = GoalHistoryStatus.NOT_COMPLETED
                    }
                }
            }
            currCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return historyMap
    }

    fun deleteGoalHistory(context: Context, goalId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.endsWith(SNAPSHOT_KEY_SUFFIX)) {
                val rawJson = prefs.getString(key, null) ?: continue
                val snapshots = parseSnapshots(rawJson)
                val filtered = snapshots.filterNot { it.goalId == goalId }
                if (filtered.isEmpty()) {
                    editor.remove(key)
                } else {
                    val array = JSONArray()
                    for (s in filtered) {
                        array.put(JSONObject().apply {
                            put("goalId", s.goalId)
                            put("title", s.title)
                            put("targetMinutes", s.targetMinutes)
                            put("completed", s.completed)
                            put("checkedAt", s.checkedAt)
                            put("isAchieved", s.isAchieved)
                        })
                    }
                    editor.putString(key, array.toString())
                }
            }
        }
        editor.apply()
    }

    fun loadGoalHistory(context: Context, goalId: String): Map<String, Boolean> {
        return loadGoalHistoryDetailed(context, goalId).mapValues { it.value == GoalHistoryStatus.ACHIEVED }
    }

    fun computeGoalInsights(
        context: Context,
        goalId: String,
        scheduledDays: List<Int> = emptyList(),
        createdAt: Long = 0L
    ): GoalInsights {
        val historyDetailed = loadGoalHistoryDetailed(context, goalId, scheduledDays, createdAt)
        val scheduledOnly = historyDetailed.filter { it.value != GoalHistoryStatus.NOT_SCHEDULED }
        if (scheduledOnly.isEmpty()) {
            return GoalInsights(0, 0, 0, 0, 0f, "N/A")
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedDates = scheduledOnly.keys.mapNotNull {
            runCatching { sdf.parse(it) }.getOrNull()
        }.sorted()

        val totalDays = sortedDates.size
        val completedDays = scheduledOnly.values.count { it == GoalHistoryStatus.ACHIEVED }
        val completionRate = if (totalDays > 0) completedDays.toFloat() / totalDays.toFloat() else 0f

        var maxStreak = 0
        var tempStreak = 0

        for (d in sortedDates) {
            val dStr = sdf.format(d)
            if (scheduledOnly[dStr] == GoalHistoryStatus.ACHIEVED) {
                tempStreak++
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        var currentStreak = 0
        var checkCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var checkStr = sdf.format(checkCal.time)
        if (scheduledOnly[checkStr] != GoalHistoryStatus.ACHIEVED) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(checkCal.time)
        }

        while (scheduledOnly[checkStr] == GoalHistoryStatus.ACHIEVED) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            checkStr = sdf.format(checkCal.time)
        }

        val weekdayCounts = IntArray(7)
        val weekdayCompletions = IntArray(7)
        for ((dStr, status) in scheduledOnly) {
            val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: continue
            val cal = Calendar.getInstance().apply { time = parsed }
            val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
            weekdayCounts[idx]++
            if (status == GoalHistoryStatus.ACHIEVED) weekdayCompletions[idx]++
        }

        val bestIdx = (0 until 7).maxByOrNull {
            if (weekdayCounts[it] > 0) weekdayCompletions[it].toFloat() / weekdayCounts[it].toFloat() else 0f
        } ?: 0

        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestWeekdayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestIdx + 1) }.time)

        return GoalInsights(
            totalDaysTracked = totalDays,
            completedDays = completedDays,
            currentStreak = currentStreak,
            longestStreak = maxStreak,
            completionRate = completionRate,
            bestWeekday = bestWeekdayName
        )
    }

    fun computeOverallPlannerInsights(context: Context, currentGoals: List<PlannerGoal>): OverallPlannerInsights {
        if (currentGoals.isEmpty()) {
            return OverallPlannerInsights("", 0, "", 0, "N/A", 0, false)
        }

        var maxStreakVal = 0
        var maxStreakGoalTitle = ""

        var maxRateVal = 0f
        var maxRateGoalTitle = ""

        var totalGoalDays = 0
        var totalCompletedDays = 0

        val weekdayTotals = IntArray(7)

        for (goal in currentGoals) {
            val insights = computeGoalInsights(context, goal.id)
            if (insights.totalDaysTracked > 0) {
                totalGoalDays += insights.totalDaysTracked
                totalCompletedDays += insights.completedDays

                if (insights.longestStreak > maxStreakVal) {
                    maxStreakVal = insights.longestStreak
                    maxStreakGoalTitle = goal.title
                }

                if (insights.completionRate > maxRateVal) {
                    maxRateVal = insights.completionRate
                    maxRateGoalTitle = goal.title
                }
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (key in prefs.all.keys) {
            if (key.endsWith(SNAPSHOT_KEY_SUFFIX)) {
                val dStr = key.removeSuffix(SNAPSHOT_KEY_SUFFIX)
                val parsed = runCatching { sdf.parse(dStr) }.getOrNull() ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
                val rawJson = prefs.getString(key, null) ?: continue
                val snapshots = parseSnapshots(rawJson)
                val completedCount = snapshots.count { it.completed }
                weekdayTotals[idx] += completedCount
            }
        }

        val bestDayIdx = (0 until 7).maxByOrNull { weekdayTotals[it] } ?: 0
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestDayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestDayIdx + 1) }.time)

        val overallPct = if (totalGoalDays > 0) (totalCompletedDays * 100) / totalGoalDays else 0
        val hasEnoughData = totalGoalDays >= 3 || currentGoals.any { it.completed }

        return OverallPlannerInsights(
            bestStreakGoalTitle = if (maxStreakGoalTitle.isNotBlank()) maxStreakGoalTitle else currentGoals.first().title,
            bestStreakDays = maxStreakVal,
            mostConsistentGoalTitle = if (maxRateGoalTitle.isNotBlank()) maxRateGoalTitle else currentGoals.first().title,
            mostConsistentPct = (maxRateVal * 100).toInt(),
            bestWeekdayName = bestDayName,
            overallCompletionPct = overallPct,
            hasEnoughData = hasEnoughData
        )
    }
}

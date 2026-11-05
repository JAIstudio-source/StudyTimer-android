package com.madeby.JAI

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class StatsEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_DAILY_GOAL_SECS = 2700L // 45 minutes default for brand-new users upon first install

        fun getDailyGoalSecs(prefs: android.content.SharedPreferences): Long {
            if (prefs.contains("daily_goal_secs")) {
                val s = prefs.safeLong("daily_goal_secs", 0L)
                if (s > 0L) return s
            }
            if (prefs.contains("daily_goal_minutes")) {
                val m = prefs.safeInt("daily_goal_minutes", 0)
                if (m > 0) {
                    return m * 60L
                }
            }
            return DEFAULT_DAILY_GOAL_SECS
        }
    }

    private val parsedDateCache = java.util.concurrent.ConcurrentHashMap<String, Date>()

    fun dailyGoalSecs(): Long {
        return getDailyGoalSecs(prefs)
    }

    fun resolveGoalFor(dateStr: String): Long {
        return GoalHistoryManager.resolveGoalForDate(context, dateStr, dailyGoalSecs())
    }

    fun recalculateStreak(todayExtra: Long = prefs.getLong("accumulatedStudy", 0L)) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val streakGoalBased = prefs.getBoolean("streak_uses_daily_goal", true)
        val frozenDates = StreakCalculator.getFrozenDates(context)

        val streak = StreakCalculator.currentStreak(
            todayExtra = todayExtra,
            focusOn = { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                prefs.getLong("${sdf.format(cal.time)}_focus_total", 0L)
            },
            goalFor = if (streakGoalBased) ({ daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                resolveGoalFor(sdf.format(cal.time))
            }) else null,
            isFrozen = { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val dKey = sdf.format(cal.time)
                frozenDates.contains(dKey)
            }
        )

        if (streak == prefs.safeInt("current_streak", 0) && prefs.getString("streak_last_calculated", null) == todayStr) return
        prefs.edit()
            .putInt("current_streak", streak)
            .putString("streak_last_calculated", todayStr)
            .apply()
    }

    fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> {
        val entries = TimelineLogger.load(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedStart = runCatching { sdf.parse(dateStr)?.time }.getOrNull() ?: 0L
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis
        startCal.add(Calendar.DAY_OF_YEAR, 1)
        val endMs = startCal.timeInMillis

        val parsed = parseDayBlocks(entries)
        val sessions = ArrayList<BlockInfo>()
        val breaks = ArrayList<BlockInfo>()

        for (s in parsed.sessions) {
            if (s.startMs in startMs until endMs) {
                sessions.add(s)
            }
        }
        for (b in parsed.breaks) {
            if (b.startMs in startMs until endMs) {
                breaks.add(b)
            }
        }

        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val isToday = dateStr == dateSdf.format(Date())
        val timerState = prefs.safeString("timerState", "IDLE") ?: "IDLE"
        val timerRunning = timerState == "STUDYING" || timerState == "BREAK"

        if (parsed.openFocusStart != null) {
            val fs = parsed.openFocusStart
            val endTs = if (isToday && timerRunning) System.currentTimeMillis() else (entries.lastOrNull()?.timestamp ?: fs)
            if (fs in startMs until endMs) {
                val segStart = fs
                val segEnd = minOf(endTs, endMs - 1000L)
                val gapMs = (segEnd - segStart).coerceAtLeast(0L)
                val effectiveGapMs = minOf(gapMs, 14400_000L) // Anti-cheat 4h cap
                if (effectiveGapMs > 0L) {
                    sessions.add(
                        BlockInfo(
                            segStart,
                            segStart + effectiveGapMs,
                            effectiveGapMs / 1000L,
                            isToday && timerRunning,
                            parsed.openFocusManual,
                            subjectId = parsed.openFocusSubId,
                            subjectName = parsed.openFocusSubName,
                            subjectColor = parsed.openFocusSubColor
                        )
                    )
                }
            }
        }

        if (parsed.openBreakStart != null) {
            val bs = parsed.openBreakStart
            val endTs = if (isToday && timerRunning) System.currentTimeMillis() else (entries.lastOrNull()?.timestamp ?: bs)
            if (bs in startMs until endMs) {
                val segStart = bs
                val segEnd = minOf(endTs, endMs - 1000L)
                val gapMs = (segEnd - segStart).coerceAtLeast(0L)
                val effectiveGapMs = minOf(gapMs, 7200_000L) // Anti-cheat 2h break cap
                if (effectiveGapMs > 0L) {
                    breaks.add(BlockInfo(segStart, segStart + effectiveGapMs, effectiveGapMs / 1000L, manual = parsed.openBreakManual))
                }
            }
        }

        val focusManual = prefs.getLong("${dateStr}_focus_manual", 0L)
        val breakManual = prefs.getLong("${dateStr}_break_manual", 0L)
        val hasRawManualFocus = entries.any { it.state == "MANUAL_FOCUS" && it.timestamp in startMs until endMs }
        val hasRawManualBreak = entries.any { it.state == "MANUAL_BREAK" && it.timestamp in startMs until endMs }

        if (focusManual != 0L && !hasRawManualFocus) {
            if (sessions.isNotEmpty()) {
                val last = sessions.last()
                val adj = max(0L, last.secs + focusManual)
                sessions[sessions.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (focusManual > 0L) {
                val lastEnd = entries.filter { it.timestamp in startMs until endMs }.lastOrNull()?.timestamp ?: startMs
                sessions.add(BlockInfo(lastEnd, lastEnd + focusManual * 1000L, focusManual, manual = true))
            }
        }
        if (breakManual != 0L && !hasRawManualBreak) {
            if (breaks.isNotEmpty()) {
                val last = breaks.last()
                val adj = max(0L, last.secs + breakManual)
                breaks[breaks.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (breakManual > 0L) {
                val lastEnd = entries.filter { it.timestamp in startMs until endMs }.lastOrNull()?.timestamp ?: startMs
                breaks.add(BlockInfo(lastEnd, lastEnd + breakManual * 1000L, breakManual, manual = true))
            }
        }
        return sessions to breaks
    }

    fun getSubjectBreakdownForDates(dates: List<String>): Map<String, Pair<SubjectTag, Long>> {
        val result = LinkedHashMap<String, Pair<SubjectTag, Long>>()
        if (dates.isEmpty()) return result

        val entries = TimelineLogger.load(context)
        val parsed = parseDayBlocks(entries)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateSet = dates.toSet()
        val todayStr = sdf.format(Date())
        val timerState = prefs.safeString("timerState", "IDLE") ?: "IDLE"
        val timerRunning = timerState == "STUDYING" || timerState == "BREAK"

        val ranges = dates.mapNotNull { dStr ->
            val t = runCatching { sdf.parse(dStr)?.time }.getOrNull() ?: return@mapNotNull null
            val c = Calendar.getInstance().apply {
                timeInMillis = t
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val s = c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 1)
            val e = c.timeInMillis
            dStr to (s until e)
        }

        val matchedDatesWithSessions = HashSet<String>()

        for (s in parsed.sessions) {
            for ((dStr, range) in ranges) {
                if (s.startMs in range) {
                    matchedDatesWithSessions.add(dStr)
                    val subId = s.subjectId ?: "general"
                    val subj = SubjectTagManager.resolveSubject(context, subId, s.subjectName, s.subjectColor)
                    val curr = result[subj.id]?.second ?: 0L
                    result[subj.id] = subj to (curr + s.secs)
                    break
                }
            }
        }

        if (todayStr in dateSet && parsed.openFocusStart != null) {
            val fs = parsed.openFocusStart
            val endTs = if (timerRunning) System.currentTimeMillis() else (entries.lastOrNull()?.timestamp ?: fs)
            val gapMs = (endTs - fs).coerceAtLeast(0L)
            val effectiveGapMs = minOf(gapMs, 14400_000L)
            if (effectiveGapMs > 0L) {
                val subId = parsed.openFocusSubId ?: "general"
                val subj = SubjectTagManager.resolveSubject(context, subId, parsed.openFocusSubName, parsed.openFocusSubColor)
                val curr = result[subj.id]?.second ?: 0L
                result[subj.id] = subj to (curr + (effectiveGapMs / 1000L))
            }
        }

        for ((dStr, _) in ranges) {
            if (!matchedDatesWithSessions.contains(dStr)) {
                val legacy = SubjectTagManager.getSubjectDurationsForDate(context, dStr)
                for ((subId, secs) in legacy) {
                    if (secs > 0L) {
                        val subj = SubjectTagManager.resolveSubject(context, subId)
                        val curr = result[subj.id]?.second ?: 0L
                        result[subj.id] = subj to (curr + secs)
                    }
                }
            }
        }

        return result
    }

    fun getAllTimeSubjectBreakdown(): Map<String, Pair<SubjectTag, Long>> {
        val result = LinkedHashMap<String, Pair<SubjectTag, Long>>()
        val entries = TimelineLogger.load(context)
        val parsed = parseDayBlocks(entries)
        for (s in parsed.sessions) {
            val subId = s.subjectId ?: "general"
            val subj = SubjectTagManager.resolveSubject(context, subId, s.subjectName, s.subjectColor)
            val curr = result[subj.id]?.second ?: 0L
            result[subj.id] = subj to (curr + s.secs)
        }

        val allTimeLegacy = SubjectTagManager.getSubjectDurations(context)
        for ((subId, secs) in allTimeLegacy) {
            if (secs > 0L) {
                val subj = SubjectTagManager.resolveSubject(context, subId)
                val curr = result[subj.id]?.second ?: 0L
                if (curr < secs) {
                    result[subj.id] = subj to secs
                }
            }
        }
        return result
    }

    fun dayBlocks(dateStr: String, entries: List<TimelineEntry>): Pair<List<BlockInfo>, List<BlockInfo>> {
        val parsed = parseDayBlocks(entries)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedStart = runCatching { sdf.parse(dateStr)?.time }.getOrNull() ?: 0L
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis
        val endMs = startMs + 24L * 3600_000L

        val sessions = ArrayList<BlockInfo>()
        val breaks = ArrayList<BlockInfo>()

        for (s in parsed.sessions) {
            if (s.startMs in startMs until endMs) {
                sessions.add(s)
            }
        }
        for (b in parsed.breaks) {
            if (b.startMs in startMs until endMs) {
                breaks.add(b)
            }
        }

        if (parsed.openFocusStart != null || parsed.openBreakStart != null) {
            val isToday = dateStr == sdf.format(Date())
            val timerState = prefs.safeString("timerState", "IDLE") ?: "IDLE"
            val timerRunning = timerState == "STUDYING" || timerState == "BREAK"
            val endTs = if (isToday && timerRunning) {
                System.currentTimeMillis()
            } else {
                entries.lastOrNull()?.timestamp ?: 0L
            }
            if (endTs > 0L) {
                val fs = parsed.openFocusStart
                if (fs != null && fs in startMs until endMs) {
                    val gapMs = (endTs - fs).coerceAtLeast(0L)
                    val effectiveGapMs = minOf(gapMs, 14400_000L)
                    if (effectiveGapMs > 0L) {
                        sessions.add(
                            BlockInfo(
                                fs,
                                minOf(fs + effectiveGapMs, endMs - 1000L),
                                effectiveGapMs / 1000L,
                                isToday && timerRunning,
                                parsed.openFocusManual,
                                subjectId = parsed.openFocusSubId,
                                subjectName = parsed.openFocusSubName,
                                subjectColor = parsed.openFocusSubColor
                            )
                        )
                    }
                }
                val bs = parsed.openBreakStart
                if (bs != null && bs in startMs until endMs) {
                    val gapMs = (endTs - bs).coerceAtLeast(0L)
                    val effectiveGapMs = minOf(gapMs, 7200_000L)
                    if (effectiveGapMs > 0L) {
                        breaks.add(BlockInfo(bs, minOf(bs + effectiveGapMs, endMs - 1000L), effectiveGapMs / 1000L, manual = parsed.openBreakManual))
                    }
                }
            }
        }
        val focusManual = prefs.safeLong("${dateStr}_focus_manual", 0L)
        val breakManual = prefs.safeLong("${dateStr}_break_manual", 0L)
        val hasRawManualFocus = entries.any { it.state == "MANUAL_FOCUS" && it.timestamp in startMs until endMs }
        val hasRawManualBreak = entries.any { it.state == "MANUAL_BREAK" && it.timestamp in startMs until endMs }

        if (focusManual != 0L && !hasRawManualFocus) {
            if (sessions.isNotEmpty()) {
                val last = sessions.last()
                val adj = max(0L, last.secs + focusManual)
                sessions[sessions.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (focusManual > 0L) {
                val lastEnd = entries.filter { it.timestamp in startMs until endMs }.lastOrNull()?.timestamp ?: startMs
                sessions.add(BlockInfo(lastEnd, lastEnd + focusManual * 1000L, focusManual, manual = true))
            }
        }
        if (breakManual != 0L && !hasRawManualBreak) {
            if (breaks.isNotEmpty()) {
                val last = breaks.last()
                val adj = max(0L, last.secs + breakManual)
                breaks[breaks.size - 1] = last.copy(secs = adj, endMs = last.startMs + adj * 1000L, manual = adj != last.secs)
            } else if (breakManual > 0L) {
                val lastEnd = entries.filter { it.timestamp in startMs until endMs }.lastOrNull()?.timestamp ?: startMs
                breaks.add(BlockInfo(lastEnd, lastEnd + breakManual * 1000L, breakManual, manual = true))
            }
        }
        return sessions to breaks
    }

    fun sanitizeAndHealHistoricalTotals() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val allKeys = prefs.all.keys.toList()
        val totalKeys = allKeys.filter { it.endsWith("_focus_total") || it.endsWith("_break_total") }
        val entries = TimelineLogger.load(context)

        val timelineDates = entries.map { sdf.format(Date(it.timestamp)) }.toSet()
        val allDates = (totalKeys.map { it.removeSuffix("_focus_total").removeSuffix("_break_total") } + timelineDates).toSet()

        val editor = prefs.edit()
        var modified = false

        for (dateStr in allDates) {
            val isTimerActive = (prefs.safeString("timerState", "IDLE") ?: "IDLE") != "IDLE"
            if (dateStr == todayStr && isTimerActive) continue

            val (sessions, breaks) = dayBlocks(dateStr, entries)
            val trueFocusSum = sessions.filter { !it.running }.sumOf { it.secs }
            val trueBreakSum = breaks.filter { !it.running }.sumOf { it.secs }
            val key = "${dateStr}_focus_total"
            val breakKey = "${dateStr}_break_total"
            val storedFocus = prefs.safeLong(key, 0L)
            val storedBreak = prefs.safeLong(breakKey, 0L)

            val finalFocusSum = if (storedFocus > 0L && trueFocusSum == 0L) {
                storedFocus
            } else {
                trueFocusSum
            }
            val finalBreakSum = if (storedBreak > 0L && trueBreakSum == 0L) {
                storedBreak
            } else {
                trueBreakSum
            }

            if (storedFocus != finalFocusSum || storedBreak != finalBreakSum) {
                runCatching {
                    android.util.Log.i("StatsEngine", "Self-healing day totals for $dateStr: storedFocus=$storedFocus -> finalFocusSum=$finalFocusSum, storedBreak=$storedBreak -> finalBreakSum=$finalBreakSum")
                }
                editor.putLong(key, finalFocusSum)
                editor.putLong(breakKey, finalBreakSum)
                modified = true
            }
            TimelineLogger.reconcileSubjectDurationsFromTimeline(context, dateStr)
        }
        if (modified) {
            editor.apply()
        }
    }

    fun reconcileDayTotals(dateStr: String) {
        val timerActive = (prefs.safeString("timerState", "IDLE") ?: "IDLE") != "IDLE"
        if (timerActive && dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) return
        val (sessions, breaks) = dayBlocks(dateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        val breakSum = breaks.filter { !it.running }.sumOf { it.secs }
        val focusKey = "${dateStr}_focus_total"
        val breakKey = "${dateStr}_break_total"
        val storedFocus = prefs.safeLong(focusKey, 0L)
        val storedBreak = prefs.safeLong(breakKey, 0L)
        val effectiveFocus = focusSum
        val effectiveBreak = breakSum
        if (storedFocus != effectiveFocus || storedBreak != effectiveBreak) {
            val editor = prefs.edit()
            if (storedFocus != effectiveFocus) {
                editor.putLong(focusKey, effectiveFocus)
            }
            if (storedBreak != effectiveBreak) {
                editor.putLong(breakKey, effectiveBreak)
            }
            editor.apply()
        }
        TimelineLogger.reconcileSubjectDurationsFromTimeline(context, dateStr)
    }

    fun forceReconcileDayTotals(dateStr: String) {
        val (sessions, breaks) = dayBlocks(dateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        val breakSum = breaks.filter { !it.running }.sumOf { it.secs }
        val focusKey = "${dateStr}_focus_total"
        val breakKey = "${dateStr}_break_total"

        prefs.edit()
            .putLong(focusKey, focusSum)
            .putLong(breakKey, breakSum)
            .apply()
        TimelineLogger.reconcileSubjectDurationsFromTimeline(context, dateStr)
    }

    fun computeStatsSnapshot(currentBreakSecs: Long = 0L): StatsSnapshot {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        reconcileDayTotals(todayStr)
        val currentSessionSecs = prefs.getLong("accumulatedStudy", 0L)

        recalculateStreak(todayExtra = currentSessionSecs)
        val streak = prefs.safeInt("current_streak", 0)

        val todayFocus = FocusTimeProvider.getTodayFocusSeconds(context)
        val todayBreak = prefs.getLong("${todayStr}_break_total", 0L) + currentBreakSecs

        val avg7 = run {
            var total7 = 0L
            var activeDays = 0
            for (i in 0 until 7) {
                val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i)
                val d = sdf.format(cal.time)
                var dayTotal = prefs.getLong("${d}_focus_total", 0L)
                if (d == todayStr) dayTotal += currentSessionSecs
                total7 += dayTotal
                if (dayTotal > 0L) {
                    activeDays++
                }
            }
            // Average per active day in the last 7 days, not calendar day.
            if (activeDays > 0) total7 / activeDays else 0L
        }

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdaySecs = prefs.getLong("${sdf.format(yesterdayCal.time)}_focus_total", 0L)

        val heroGoalSecs = resolveGoalFor(todayStr)

        val allKeys = prefs.all.keys
        val allFocusKeys = allKeys.filter { it.endsWith("_focus_total") }

        val dayFocus = HashMap<String, Long>()
        val dayBreak = HashMap<String, Long>()
        var totalLifeFocus = 0L
        var totalLifeBreak = 0L
        for (key in allFocusKeys) {
            val dStr = key.removeSuffix("_focus_total")
            val f = prefs.getLong(key, 0L)
            val b = prefs.getLong("${dStr}_break_total", 0L)
            dayFocus[dStr] = f
            dayBreak[dStr] = b
            totalLifeFocus += f
            totalLifeBreak += b
        }
        dayFocus[todayStr] = (dayFocus[todayStr] ?: 0L) + currentSessionSecs
        dayBreak[todayStr] = (dayBreak[todayStr] ?: 0L) + currentBreakSecs
        totalLifeFocus += currentSessionSecs
        totalLifeBreak += currentBreakSecs
        val totalLife = totalLifeFocus + totalLifeBreak

        val streakGoalBased = prefs.getBoolean("streak_uses_daily_goal", true)

        val activeDays = dayFocus.keys.count { d ->
            val f = dayFocus[d] ?: 0L
            val b = dayBreak[d] ?: 0L
            f > 0L || b > 0L
        }

        val sortedDateKeys = dayFocus.keys.sorted()
        val pairsForStreak = ArrayList<Pair<Date, Long>>(sortedDateKeys.size)
        val weekdayTotals = LongArray(7)
        val weekdayCounts = IntArray(7)
        val weekTotals = HashMap<String, Long>()
        var bestDaySecs = 0L
        var bestDayDateStr = ""
        var bestDayLabel = ""
        val tempCal = Calendar.getInstance()
        val displayDateSdf = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

        for (dStr in sortedDateKeys) {
            val f = dayFocus[dStr] ?: 0L
            val parsed = parsedDateCache.getOrPut(dStr) {
                try { sdf.parse(dStr) ?: Date() } catch (_: Exception) { Date() }
            }
            pairsForStreak.add(Pair(parsed, f))

            if (f > bestDaySecs) {
                bestDaySecs = f
                bestDayDateStr = dStr
                bestDayLabel = displayDateSdf.format(parsed)
            }

            if (f > 0L) {
                tempCal.time = parsed
                val weekKey = sdf.format(WeekHelper.mondayOf(tempCal).time)
                weekTotals[weekKey] = (weekTotals[weekKey] ?: 0L) + f

                if (dStr != todayStr) {
                    val idx = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                    weekdayTotals[idx] += f
                    weekdayCounts[idx]++
                }
            }
        }

        val longestStreak = StreakCalculator.longestStreak(
            pairsForStreak,
            goalFor = if (streakGoalBased) ({ date -> resolveGoalFor(sdf.format(date)) }) else null
        )

        val todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        if (todayFocus > 0L) {
            weekdayTotals[todayIdx] += todayFocus
            weekdayCounts[todayIdx]++
        }
        val bestWeekdayIdx = (0 until 7).maxByOrNull { idx ->
            if (weekdayCounts[idx] > 0) weekdayTotals[idx] / weekdayCounts[idx] else 0L
        } ?: 0
        val bestWeekdaySecs = if (weekdayCounts[bestWeekdayIdx] > 0) weekdayTotals[bestWeekdayIdx] / weekdayCounts[bestWeekdayIdx] else 0L
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val bestWeekdayName = dayNameSdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, bestWeekdayIdx + 1) }.time)

        var bestWeekSecs = 0L
        var bestWeekLabel = ""
        val shortDateSdf = SimpleDateFormat("d MMM", Locale.getDefault())
        weekTotals.maxByOrNull { it.value }?.let { best ->
            if (best.value > 0L) {
                val weekStart = parsedDateCache.getOrPut(best.key) {
                    try { sdf.parse(best.key) ?: Date() } catch (_: Exception) { Date() }
                }
                tempCal.time = weekStart
                tempCal.add(Calendar.DAY_OF_YEAR, 6)
                bestWeekSecs = best.value
                bestWeekLabel = "${shortDateSdf.format(weekStart)} \u2013 ${shortDateSdf.format(tempCal.time)}"
            }
        }

        fun mondayOffset(): Int = WeekHelper.mondayOffset(Calendar.getInstance())
        fun weekFocus(weekOffset: Int): Long {
            var sum = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mondayOffset() + i + weekOffset * 7) }
                sum += dayFocus[sdf.format(c.time)] ?: 0L
            }
            return sum
        }
        val thisWeek = weekFocus(0)
        val prevWeek = weekFocus(-1)

        var goalHits = 0
        for (i in 0 until 14) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(c.time)
            if ((dayFocus[dStr] ?: 0L) >= resolveGoalFor(dStr)) goalHits++
        }

        val heatmapData = HashMap<String, Long>()
        for (k in allFocusKeys) {
            val dStr = k.removeSuffix("_focus_total")
            heatmapData[dStr] = prefs.getLong(k, 0L)
        }
        if (currentSessionSecs > 0L) {
            heatmapData[todayStr] = (heatmapData[todayStr] ?: 0L) + currentSessionSecs
        }

        val showHeatmap = prefs.getBoolean("show_focus_heatmap", true)

        val showPieChart = prefs.getBoolean("show_subject_pie_chart", true)

        val timeline = TimelineLogger.load(context)

        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthAgg = LinkedHashMap<String, MonthBucket>()
        for ((dStr, f) in dayFocus) {
            val parsed = try { sdf.parse(dStr) } catch (_: Exception) { null } ?: continue
            val b = dayBreak[dStr] ?: 0L
            if (f <= 0L && b <= 0L) continue
            val mKey = monthSdf.format(parsed)
            val existing = monthAgg[mKey]
            monthAgg[mKey] = MonthBucket(mKey, (existing?.focus ?: 0L) + f, (existing?.breakSecs ?: 0L) + b)
        }
        val monthBuckets = monthAgg.values.toList().sortedBy { runCatching { monthSdf.parse(it.label)?.time }.getOrNull() ?: 0L }

        val allHistKeys = allFocusKeys.sortedDescending()
        val entriesByDay = timeline.groupBy { sdf.format(Date(it.timestamp)) }

        val hasAnySessions = totalLifeFocus > 0L || totalLifeBreak > 0L || currentSessionSecs > 0L || currentBreakSecs > 0L

        return StatsSnapshot(
            todayFocus, todayBreak, streak, avg7, yesterdaySecs, heroGoalSecs,
            totalLifeFocus, totalLifeBreak, totalLife, longestStreak, activeDays,
            bestWeekdayName, bestWeekdaySecs, bestDayLabel, bestDaySecs, bestDayDateStr, bestWeekLabel, bestWeekSecs,
            thisWeek, prevWeek, goalHits, hasAnySessions,
            showHeatmap, showPieChart, heatmapData,
            dayFocus, monthBuckets, allHistKeys, entriesByDay
        )
    }

    fun buildHeatmapData(): Map<String, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val data = HashMap<String, Long>()
        for (k in prefs.all.keys) {
            if (k.endsWith("_focus_total")) {
                data[k.removeSuffix("_focus_total")] = prefs.getLong(k, 0L)
            }
        }
        val currentSessionSecs = prefs.getLong("accumulatedStudy", 0L)
        if (currentSessionSecs > 0L) {
            data[todayStr] = (data[todayStr] ?: 0L) + currentSessionSecs
        }
        return data
    }

    fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val base = try { sdf.parse(dateStr) } catch (_: Exception) { null } ?: Date()
        val c = Calendar.getInstance().apply { time = base }
        c.set(Calendar.HOUR_OF_DAY, h)
        c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) {
        val newSecs = (newEndMs - newStartMs) / 1000L
        val startState = if (isBreak) {
            if (block.manual) "MANUAL_BREAK" else "BREAK"
        } else {
            // If the session was an authentic timer session and is being trimmed or adjusted within real bounds, preserve STUDYING
            if (!block.manual && newSecs <= block.secs) "STUDYING" else if (block.manual) "MANUAL_FOCUS" else "STUDYING"
        }
        TimelineLogger.replaceBlock(context, block.startMs, block.endMs, newStartMs, newEndMs, startState)

        if (!isBreak) {
            val oldSecs = block.secs
            val delta = newSecs - oldSecs
            val subId = block.subjectId ?: SubjectTagManager.getSelectedSubject(context).id
            if (delta != 0L) {
                SubjectTagManager.adjustSubjectStudyTime(context, subId, delta, dateStr)
            }
        }

        prefs.edit()
            .putLong("${dateStr}_focus_manual", 0L)
            .putLong("${dateStr}_break_manual", 0L)
            .apply()
        forceReconcileDayTotals(dateStr)
        recalculateStreak()
    }
}

internal data class ParsedDay(
    val sessions: List<BlockInfo>,
    val breaks: List<BlockInfo>,
    val openFocusStart: Long?,
    val openFocusManual: Boolean,
    val openFocusSubId: String?,
    val openFocusSubName: String?,
    val openFocusSubColor: String?,
    val openBreakStart: Long?,
    val openBreakManual: Boolean
)

internal fun parseDayBlocks(entries: List<TimelineEntry>): ParsedDay {
    val sessions = ArrayList<BlockInfo>()
    val breaks = ArrayList<BlockInfo>()
    var fs: Long? = null
    var fsManual = false
    var fsSubId: String? = null
    var fsSubName: String? = null
    var fsSubColor: String? = null
    var bs: Long? = null
    var bsManual = false

    fun addSplitBlock(
        list: ArrayList<BlockInfo>,
        startMs: Long,
        endMs: Long,
        manual: Boolean,
        subjectId: String? = null,
        subjectName: String? = null,
        subjectColor: String? = null
    ) {
        if (endMs <= startMs) return
        var curStart = startMs
        val cal = Calendar.getInstance()
        while (curStart < endMs) {
            cal.timeInMillis = curStart
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEndMs = cal.timeInMillis

            val curEnd = if (endMs <= dayEndMs + 1000L) endMs else dayEndMs
            val secs = ((curEnd - curStart) / 1000L).coerceAtLeast(1L)
            list.add(
                BlockInfo(
                    startMs = curStart,
                    endMs = curEnd,
                    secs = secs,
                    manual = manual,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    subjectColor = subjectColor
                )
            )
            curStart = dayEndMs + 1L
        }
    }

    for (e in entries) {
        when (e.state) {
            "STUDYING", "MANUAL_FOCUS" -> {
                if (fs != null) {
                    val gapMs = e.timestamp - fs
                    if (gapMs <= 24L * 3600_000L) {
                        val effectiveEndMs = if (gapMs > 14400_000L) fs + 14400_000L else e.timestamp
                        addSplitBlock(sessions, fs, effectiveEndMs, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor)
                    }
                }
                if (bs != null) {
                    val gapMs = e.timestamp - bs
                    if (gapMs <= 24L * 3600_000L) {
                        val effectiveEndMs = if (gapMs > 14400_000L) bs + 14400_000L else e.timestamp
                        addSplitBlock(breaks, bs, effectiveEndMs, manual = bsManual)
                    }
                }
                bs = null
                bsManual = false
                fs = e.timestamp
                fsManual = e.state == "MANUAL_FOCUS"
                fsSubId = e.subId
                fsSubName = e.subName
                fsSubColor = e.subColor
            }
            "BREAK", "MANUAL_BREAK" -> {
                if (fs != null) {
                    val gapMs = e.timestamp - fs
                    val effectiveEndMs = if (gapMs > 14400_000L) fs + 14400_000L else e.timestamp
                    addSplitBlock(sessions, fs, effectiveEndMs, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor)
                }
                fs = null
                fsManual = false
                fsSubId = null
                fsSubName = null
                fsSubColor = null
                if (bs == null) {
                    bs = e.timestamp
                    bsManual = e.state == "MANUAL_BREAK"
                }
            }
            "IDLE" -> {
                if (fs != null) {
                    val gapMs = e.timestamp - fs
                    val effectiveEndMs = if (gapMs > 14400_000L) fs + 14400_000L else e.timestamp
                    addSplitBlock(sessions, fs, effectiveEndMs, manual = fsManual, subjectId = fsSubId, subjectName = fsSubName, subjectColor = fsSubColor)
                }
                fs = null
                fsManual = false
                fsSubId = null
                fsSubName = null
                fsSubColor = null
                if (bs != null) {
                    val gapMs = e.timestamp - bs
                    val effectiveEndMs = if (gapMs > 14400_000L) bs + 14400_000L else e.timestamp
                    addSplitBlock(breaks, bs, effectiveEndMs, manual = bsManual)
                }
                bs = null
                bsManual = false
            }
        }
    }
    return ParsedDay(sessions, breaks, fs, fsManual, fsSubId, fsSubName, fsSubColor, bs, bsManual)
}

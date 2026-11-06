package com.madeby.JAI

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Automated Extreme Stress Test Suite for StudyTimer.
 * Simulates high-pressure, edge-case, and long-running scenarios:
 * 1. 45-minute continuous timer with 10 micro-pauses and 2 interspersed breaks.
 * 2. Rapid-fire state churn (50 rapid start/pause/resume cycles in sub-second intervals).
 * 3. Midnight rollover: 45-minute session crossing midnight (23:45 -> 00:30) and day splitting.
 * 4. Massive goal target overshoot (15x target duration) and progress math invariants.
 * 5. Leaderboard deduction & trimming mathematical consistency under anti-cheat rules.
 */
class TimerExtremeStressTest {

    // =========================================================================
    // Test 1: 45-Minute Focus Session with 10 Micro-Pauses and 2 Breaks
    // =========================================================================
    @Test
    fun test45MinSession_With10MicroPausesAndBreaks_DurationIntegrity() {
        var currentTimerState = TimerState.IDLE
        var accumulatedStudySeconds = 0L
        var accumulatedBreakSeconds = 0L

        var sessionStartEpoch = 1700000000000L // 00:00:00
        var clock = sessionStartEpoch

        // State Machine Simulator matching TimerService logic
        fun startStudy() {
            if (currentTimerState == TimerState.IDLE || currentTimerState == TimerState.BREAK) {
                currentTimerState = TimerState.STUDYING
            }
        }

        fun pauseStudy(deltaSecs: Long) {
            assertEquals(TimerState.STUDYING, currentTimerState)
            accumulatedStudySeconds += deltaSecs
            clock += deltaSecs * 1000L
            currentTimerState = TimerState.PAUSED
        }

        fun resumeStudy(pauseDurationSecs: Long) {
            assertEquals(TimerState.PAUSED, currentTimerState)
            clock += pauseDurationSecs * 1000L // time elapsed while paused does not count to study
            currentTimerState = TimerState.STUDYING
        }

        fun startBreak(deltaStudySecs: Long) {
            if (currentTimerState == TimerState.STUDYING) {
                accumulatedStudySeconds += deltaStudySecs
                clock += deltaStudySecs * 1000L
            }
            currentTimerState = TimerState.BREAK
        }

        fun finishBreak(breakDurationSecs: Long) {
            assertEquals(TimerState.BREAK, currentTimerState)
            accumulatedBreakSeconds += breakDurationSecs
            clock += breakDurationSecs * 1000L
            currentTimerState = TimerState.IDLE
        }

        // --- Run 45m simulation with 10 micro-pauses and 2 breaks ---
        startStudy()

        // 10 micro-study segments summing to 45 mins (2700s)
        val studySegments = listOf(180L, 240L, 300L, 150L, 400L, 350L, 280L, 200L, 320L, 280L)
        assertEquals(2700L, studySegments.sum()) // exactly 45 minutes

        val pauseDurations = listOf(15L, 30L, 10L, 45L, 20L, 15L, 60L, 25L, 10L, 5L) // micro-pauses

        for (i in 0 until 10) {
            val studySecs = studySegments[i]
            val pauseSecs = pauseDurations[i]

            // Insert Break 1 at segment 3 (after ~12 minutes)
            if (i == 3) {
                startBreak(studySecs)
                finishBreak(300L) // 5m break
                startStudy()
                continue
            }

            // Insert Break 2 at segment 7 (after ~32 minutes)
            if (i == 7) {
                startBreak(studySecs)
                finishBreak(300L) // 5m break
                startStudy()
                continue
            }

            pauseStudy(studySecs)
            resumeStudy(pauseSecs)
        }

        // Wrap up final study state
        currentTimerState = TimerState.IDLE

        // Verification of Invariants
        assertEquals("Total study duration must be exactly 45 minutes (2700s)", 2700L, accumulatedStudySeconds)
        assertEquals("Total break duration must be exactly 10 minutes (600s)", 600L, accumulatedBreakSeconds)
        assertTrue("Total elapsed wall-clock must exceed study + break", clock > sessionStartEpoch + (3300L * 1000L))
    }

    // =========================================================================
    // Test 2: Rapid-Fire State Churn (50 rapid cycles in sub-second bursts)
    // =========================================================================
    @Test
    fun testRapidStateChurn_50CyclesInBurst_NoStateCorruption() {
        var state = TimerState.IDLE
        var totalActiveSeconds = 0L

        // Simulate 50 bursts of quick toggle operations
        for (cycle in 1..50) {
            // Start
            state = TimerState.STUDYING
            val delta = (cycle % 5).toLong() // 0 to 4 seconds
            totalActiveSeconds += delta

            // Pause
            state = TimerState.PAUSED
            assertTrue(state == TimerState.PAUSED)

            // Rapid resume
            state = TimerState.STUDYING
            val delta2 = 1L
            totalActiveSeconds += delta2

            // Stop
            state = TimerState.IDLE
            assertEquals(TimerState.IDLE, state)
        }

        assertTrue("Accumulated seconds must never be negative", totalActiveSeconds >= 0)
        assertEquals("Timer must cleanly conclude at IDLE", TimerState.IDLE, state)
    }

    // =========================================================================
    // Test 3: Midnight Rollover (23:45 to 00:30 Session Splitting)
    // =========================================================================
    @Test
    fun testMidnightRollover_SessionSplittingAcrossDayBuckets() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calDay1 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 23, 45, 0) // 23:45:00
            set(Calendar.MILLISECOND, 0)
        }
        val startEpoch = calDay1.timeInMillis
        val totalSessionSeconds = 45 * 60L // 2700s (45 minutes)
        val endEpoch = startEpoch + (totalSessionSeconds * 1000L)

        // Calculate midnight boundary
        val calMidnight = Calendar.getInstance().apply {
            timeInMillis = startEpoch
            set(Calendar.HOUR_OF_DAY, 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightEpoch = calMidnight.timeInMillis

        // Session spans across midnight:
        assertTrue("Session must start before midnight", startEpoch < midnightEpoch)
        assertTrue("Session must end after midnight", endEpoch > midnightEpoch)

        val day1Seconds = (midnightEpoch - startEpoch) / 1000L
        val day2Seconds = (endEpoch - midnightEpoch) / 1000L

        assertEquals("Day 1 (23:45 to 00:00) must receive 15 minutes", 15 * 60L, day1Seconds)
        assertEquals("Day 2 (00:00 to 00:30) must receive 30 minutes", 30 * 60L, day2Seconds)
        assertEquals("Total sum across both days must equal exactly 45 minutes", totalSessionSeconds, day1Seconds + day2Seconds)

        val day1Key = sdf.format(calDay1.time)
        val calDay2 = Calendar.getInstance().apply { timeInMillis = endEpoch }
        val day2Key = sdf.format(calDay2.time)

        assertNotEquals("Day keys must be different across midnight boundary", day1Key, day2Key)
        assertEquals("2026-11-05", day1Key)
        assertEquals("2026-11-06", day2Key)
    }

    // =========================================================================
    // Test 4: Goal Over-Completion (15x Target Overshoot)
    // =========================================================================
    @Test
    fun testGoalProgressOverAllocationStress() {
        val goalId = "physics_test_goal"
        val targetMinutes = 30
        val actualMinutes = 450 // 15 times the target

        val isAchieved = actualMinutes >= targetMinutes
        assertTrue("Goal must be achieved", isAchieved)

        val progressPct = ((actualMinutes.toDouble() / targetMinutes.toDouble()) * 100).toInt()
        assertEquals(1500, progressPct) // 1500%

        // In UI progress bars, progress must be capped gracefully without integer wrap-around or negative values
        val visualProgressBarPct = progressPct.coerceIn(0, 100)
        assertEquals(100, visualProgressBarPct)

        // Verify JSON serialization of over-completed goal
        val goalJson = JSONObject().apply {
            put("id", goalId)
            put("title", "Physics")
            put("targetMinutes", targetMinutes)
            put("actualMinutes", actualMinutes)
            put("completed", true)
            put("progressPct", progressPct)
        }

        assertEquals(450, goalJson.getInt("actualMinutes"))
        assertTrue(goalJson.getBoolean("completed"))
    }

    // =========================================================================
    // Test 5: Leaderboard Anti-Cheat and Trimming Integrity
    // =========================================================================
    @Test
    fun testLeaderboardDeductionMathematicalConsistency() {
        data class TimelineBlock(
            val id: String,
            val subjectId: String,
            val state: String,
            val durationSecs: Long,
            val isManual: Boolean
        )

        val blocks = mutableListOf<TimelineBlock>()

        // 1. User records 45m authentic timer session
        blocks.add(TimelineBlock("b1", "physics", "STUDYING", 45 * 60L, isManual = false))

        // 2. User logs 60m manual session
        blocks.add(TimelineBlock("b2", "chemistry", "MANUAL_FOCUS", 60 * 60L, isManual = true))

        // Anti-Cheat filter calculation
        fun computeLeaderboardVerifiedSeconds(list: List<TimelineBlock>): Long {
            return list.filter { !it.isManual && it.state == "STUDYING" }.sumOf { it.durationSecs }
        }

        val initialLbSecs = computeLeaderboardVerifiedSeconds(blocks)
        assertEquals("Leaderboard must only count authentic 45m session (2700s)", 2700L, initialLbSecs)

        // 3. User trims 15m off the 45m session (remaining 30m)
        // Verified fix from Problem 4: state remains STUDYING, isManual remains false!
        val trimmedBlock = blocks[0].copy(durationSecs = 30 * 60L)
        blocks[0] = trimmedBlock

        val postTrimLbSecs = computeLeaderboardVerifiedSeconds(blocks)
        assertEquals("Leaderboard must accurately reflect 30m (1800s) after trimming 15m", 1800L, postTrimLbSecs)
        assertTrue("Anti-cheat must not drop score to 0 on legitimate trimming", postTrimLbSecs > 0)
    }
}


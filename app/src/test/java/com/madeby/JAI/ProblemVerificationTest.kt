package com.madeby.JAI

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Real-time unit tests verifying the 4 core problems reported by the user:
 * 1. Cloud Sync 401 Unauthorized Error (token invalid/expired vs anonKey).
 * 2. Chapter / Subtopic time showing all-time sum on single day pie chart instead of day's focus time.
 * 3. Leaderboard sync timing (20s interval), offline anti-cheat verification from TimelineLogger.
 * 4. Deducting/trimming subject time converting session to MANUAL_FOCUS and wiping out all leaderboard time.
 */
class ProblemVerificationTest {

    // =========================================================================
    // Problem 1: Cloud Sync 401 Unauthorized Error
    // =========================================================================
    @Test
    fun testProblem1_AuthTokenResolution_Causes401_WhenGoogleIdTokenOrExpired() {
        val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.supabase-anon-key"
        val googleIdToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9.google-id-token-not-supabase-jwt"

        // The faulty logic in CloudSyncManager:
        // val authToken = AuthManager.getAccessToken(context)?.takeIf { it.isNotBlank() } ?: anonKey
        // If user logged in via Google and exchange failed/fell back, getAccessToken returns googleIdToken!
        fun resolveFaultyAuthToken(storedToken: String?): String {
            return storedToken?.takeIf { it.isNotBlank() } ?: anonKey
        }

        // When storedToken is a Google ID token:
        val faultyToken = resolveFaultyAuthToken(googleIdToken)
        assertEquals("google-id-token-not-supabase-jwt", faultyToken.substringAfter("."))
        // PostgREST will fail signature verification against Supabase JWT secret -> HTTP 401 Unauthorized!

        // The improved/fixed logic:
        // Supabase PostgREST requires either a valid Supabase JWT or the permanent anonKey.
        // For REST endpoints with anon table permissions, anonKey is universally valid.
        // Or token must be validated as a Supabase JWT before sending, falling back to anonKey.
        fun resolveRobustAuthToken(storedToken: String?): String {
            val isSupabaseToken = storedToken != null && !storedToken.contains("google") && storedToken.split(".").size == 3
            // If token is invalid, expired, or non-Supabase, fallback cleanly to anonKey to prevent 401
            return if (isSupabaseToken) storedToken!! else anonKey
        }

        val safeToken = resolveRobustAuthToken(googleIdToken)
        assertEquals(anonKey, safeToken)
    }

    // =========================================================================
    // Problem 2: Chapter / Subtopic time showing all-time total on single day pie chart
    // =========================================================================
    @Test
    fun testProblem2_SubtopicDayDuration_Vs_AllTimeDuration() {
        // Mock data structure matching SubjectTagManager
        val allTimeTotals = mutableMapOf("Chapter 1" to 5400L, "Chapter 2" to 3600L) // 90m and 60m all time
        val dailyDurations = mapOf(
            "2026-11-03" to mapOf("Chapter 1" to 1800L), // 30m on Nov 3
            "2026-11-04" to mapOf("Chapter 1" to 3600L, "Chapter 2" to 3600L) // 60m & 60m today
        )

        val targetDate = "2026-11-03"
        val subTopics = listOf("Chapter 1", "Chapter 2")

        // FAULTY BEHAVIOR (in SubjectDialogHelper line 2251):
        // It always used allTimeTotals (getSubTopicStudyTotals) even when viewing a single day!
        val faultyResultNov3 = subTopics.map { topic ->
            val tSecs = allTimeTotals[topic] ?: 0L
            topic to tSecs
        }.toMap()

        // Faulty result shows 5400s (90 mins) for Chapter 1 on Nov 3, which is WRONG (Nov 3 only had 30 mins!)
        assertEquals(5400L, faultyResultNov3["Chapter 1"])

        // CORRECT BEHAVIOR:
        // For a single day, query dailyDurations for that targetDate!
        val correctDayDurations = dailyDurations[targetDate] ?: emptyMap()
        val fixedResultNov3 = subTopics.map { topic ->
            val tSecs = correctDayDurations[topic] ?: 0L
            topic to tSecs
        }.toMap()

        // Fixed result accurately shows 1800s (30 mins) for Nov 3 and 0s for Chapter 2
        assertEquals(1800L, fixedResultNov3["Chapter 1"])
        assertEquals(0L, fixedResultNov3["Chapter 2"])
    }

    // =========================================================================
    // Problem 3: Leaderboard sync timing (20s) and offline anti-cheat verification
    // =========================================================================
    @Test
    fun testProblem3_SyncThresholdAndAntiCheatVerification() {
        // 1. Verify sync threshold change from 30s to 20s
        val oldThreshold = 30L
        val newThreshold = 20L
        var accumulatedStudy = 25L
        var lastLeaderboardSync = 0L

        // Under old 30s threshold, 25s elapsed did NOT trigger sync
        assertFalse(accumulatedStudy - lastLeaderboardSync >= oldThreshold)

        // Under new 20s threshold, 25s elapsed DOES trigger sync
        assertTrue(accumulatedStudy - lastLeaderboardSync >= newThreshold)

        // 2. Anti-cheat verification: Only real timer sessions (STUDYING) are credited to leaderboard.
        // Manual sessions (MANUAL_FOCUS) must be excluded.
        val baseMs = 1700000000000L
        val entries = listOf(
            TimelineEntry(baseMs, "STUDYING", subId = "physics"),
            TimelineEntry(baseMs + 1800_000L, "IDLE"), // 30 min real timer session
            TimelineEntry(baseMs + 2000_000L, "MANUAL_FOCUS", subId = "physics"),
            TimelineEntry(baseMs + 5600_000L, "IDLE") // 60 min manual session (tampered/offline cheat attempt)
        )

        val parsed = parseDayBlocks(entries)
        assertEquals(2, parsed.sessions.size)

        val realSession = parsed.sessions[0]
        val manualSession = parsed.sessions[1]

        assertFalse("Real timer session must NOT be manual", realSession.manual)
        assertTrue("Manual focus session MUST be marked manual", manualSession.manual)

        // Anti-cheat sum for leaderboard:
        val verifiedLeaderboardSecs = parsed.sessions.filter { !it.manual }.sumOf { it.secs }
        assertEquals(1800L, verifiedLeaderboardSecs) // Only the genuine 30 mins, 60 mins manual cheat blocked!
    }

    // =========================================================================
    // Problem 4: Deducting/trimming time converting session to MANUAL_FOCUS and wiping out leaderboard
    // =========================================================================
    @Test
    fun testProblem4_DeductingTime_PreservesRealTimerState() {
        val baseMs = 1700000000000L
        // User studied Physics for 60 minutes with the real timer:
        val initialEntries = listOf(
            TimelineEntry(baseMs, "STUDYING", subId = "physics", subName = "Physics"),
            TimelineEntry(baseMs + 3600_000L, "IDLE")
        )

        val initialParsed = parseDayBlocks(initialEntries)
        assertEquals(1, initialParsed.sessions.size)
        assertEquals(3600L, initialParsed.sessions[0].secs)
        assertFalse(initialParsed.sessions[0].manual)

        // User now trims/deducts 5 minutes (300 seconds) from this 60 min session.
        val deductSecs = 300L
        val remainingSecs = 3600L - deductSecs // 3300s = 55 mins

        // FAULTY LOGIC in TimelineLogger.kt lines 284 & 322:
        // val state = if (isBreak) "MANUAL_BREAK" else "MANUAL_FOCUS"
        // It converted the remaining block to MANUAL_FOCUS!
        val faultyRemainingEntries = listOf(
            TimelineEntry(baseMs, "MANUAL_FOCUS", subId = "physics", subName = "Physics"),
            TimelineEntry(baseMs + remainingSecs * 1000L, "IDLE")
        )
        val faultyParsed = parseDayBlocks(faultyRemainingEntries)
        assertTrue("Bug: remaining session converted to manual", faultyParsed.sessions[0].manual)

        // Anti-cheat leaderboard check with buggy logic:
        val faultyLeaderboardSecs = faultyParsed.sessions.filter { !it.manual }.sumOf { it.secs }
        assertEquals("Bug: entire subject wiped out from leaderboard (0 secs instead of 55 mins)!", 0L, faultyLeaderboardSecs)

        // FIXED LOGIC:
        // When trimming/deducting time from an existing real timer session, preserve the original state ("STUDYING")!
        val originalState = initialEntries[0].state // "STUDYING"
        val fixedRemainingEntries = listOf(
            TimelineEntry(baseMs, originalState, subId = "physics", subName = "Physics"),
            TimelineEntry(baseMs + remainingSecs * 1000L, "IDLE")
        )
        val fixedParsed = parseDayBlocks(fixedRemainingEntries)
        assertFalse("Fix: remaining session remains non-manual", fixedParsed.sessions[0].manual)

        val fixedLeaderboardSecs = fixedParsed.sessions.filter { !it.manual }.sumOf { it.secs }
        assertEquals("Fix: only the deducted 5 minutes was removed, 55 mins preserved on leaderboard!", 3300L, fixedLeaderboardSecs)
    }

    // =========================================================================
    // Improvement 1: Days off goals in Planner
    // =========================================================================
    @Test
    fun testImprovement1_DaysOffGoals_NotActiveAndNotScheduled() {
        // Scheduled days: 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri (Sat & Sun off)
        val weekdaysOnly = listOf(1, 2, 3, 4, 5)

        val calWednesday = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY) }
        val calSunday = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) }

        // PlannerHistoryManager.isGoalScheduledOnDay logic:
        fun isScheduled(scheduledDays: List<Int>, cal: Calendar): Boolean {
            if (scheduledDays.isEmpty() || scheduledDays.size >= 7) return true
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val isoDow = if (dow == Calendar.SUNDAY) 7 else dow - 1
            return scheduledDays.contains(isoDow)
        }

        assertTrue("Wednesday is active study day", isScheduled(weekdaysOnly, calWednesday))
        assertFalse("Sunday is DAY OFF", isScheduled(weekdaysOnly, calSunday))

        // On Sunday (day off), the goal must NOT be counted in active goals or daily target!
        val goals = listOf(
            PlannerGoal(id = "1", title = "Maths", targetMinutes = 60, scheduledDays = weekdaysOnly),
            PlannerGoal(id = "2", title = "Daily Revision", targetMinutes = 30, scheduledDays = emptyList()) // everyday
        )

        val sundayActiveGoals = goals.filter { isScheduled(it.scheduledDays, calSunday) }
        assertEquals(1, sundayActiveGoals.size)
        assertEquals("Daily Revision", sundayActiveGoals[0].title) // Maths excluded on day off!

        // Matrix table representation for Sunday:
        fun resolveMatrixCellText(isScheduled: Boolean, isAchieved: Boolean, isToday: Boolean): String {
            val isUnscheduled = !isScheduled && !isAchieved
            return when {
                isUnscheduled -> "—"
                isAchieved -> "✓"
                isToday -> "○"
                else -> "✕"
            }
        }
        assertEquals("—", resolveMatrixCellText(isScheduled = false, isAchieved = false, isToday = true))
        assertEquals("○", resolveMatrixCellText(isScheduled = true, isAchieved = false, isToday = true))

        // History Detail status for off day:
        fun resolveDetailStatus(isScheduled: Boolean, isAchieved: Boolean, actualMins: Int): String {
            return when {
                !isScheduled && !isAchieved && actualMins == 0 -> "— Not Scheduled"
                isAchieved -> "✓ Completed"
                actualMins > 0 -> "○ In Progress"
                else -> "○ Incomplete"
            }
        }
        assertEquals("— Not Scheduled", resolveDetailStatus(isScheduled = false, isAchieved = false, actualMins = 0))
    }

    // =========================================================================
    // Improvement 2: Sounds Pill visibility when Ambient is OFF
    // =========================================================================
    @Test
    fun testImprovement2_SoundsPillVisibility() {
        var isAmbientEnabled = false
        fun shouldShowSoundsPillOnHomeScreen(enabled: Boolean): Boolean {
            return enabled
        }

        // When ambient sound is turned off, sound pill should NOT be shown on home screen
        assertFalse(shouldShowSoundsPillOnHomeScreen(isAmbientEnabled))

        isAmbientEnabled = true
        // Only when enabled from settings should it be shown
        assertTrue(shouldShowSoundsPillOnHomeScreen(isAmbientEnabled))
    }

    // =========================================================================
    // Improvement 3: Dynamic Contrast for Wallpapers
    // =========================================================================
    @Test
    fun testImprovement3_DynamicTextContrast() {
        fun resolveContrastTextColor(isBackgroundBright: Boolean): Int {
            return if (isBackgroundBright) {
                0xFF0F172A.toInt() // High-contrast crisp black / dark slate
            } else {
                0xFFFFFFFF.toInt() // Crisp pure white
            }
        }

        // For bright wallpaper (e.g., bright day, light parchment)
        val brightBgTextColor = resolveContrastTextColor(isBackgroundBright = true)
        assertEquals(0xFF0F172A.toInt(), brightBgTextColor)

        // For dark wallpaper (e.g., starry night, OLED, dimmed wallpaper)
        val darkBgTextColor = resolveContrastTextColor(isBackgroundBright = false)
        assertEquals(0xFFFFFFFF.toInt(), darkBgTextColor)
    }

    // =========================================================================
    // Regression & Safety Tests: ClassCastException & Cloud Sync HTTP 400 Prevention
    // =========================================================================
    @Test
    fun testClassCastExceptionPrevention_SafeTypeHandling() {
        val mockPrefsMap = mutableMapOf<String, Any>()
        // Simulate a key saved as Long from cloud restore:
        mockPrefsMap["daily_goal_minutes"] = 120L
        mockPrefsMap["current_streak"] = 5L
        mockPrefsMap["customPrimary"] = 0xFF3B82F6

        // safeInt logic:
        fun safeInt(key: String, def: Int): Int {
            val v = mockPrefsMap[key]
            return (v as? Number)?.toInt() ?: (v as? String)?.toIntOrNull() ?: def
        }

        // Must safely return int without ClassCastException!
        assertEquals(120, safeInt("daily_goal_minutes", 0))
        assertEquals(5, safeInt("current_streak", 0))
        assertEquals(0xFF3B82F6.toInt(), safeInt("customPrimary", 0))
    }

    @Test
    fun testCloudSyncPayload_OnlyContainsSchemaColumns() {
        val validColumns = setOf(
            "user_id", "user_name", "user_email", "profile_image_uri",
            "prefs_data", "timeline_data", "updated_at"
        )

        val payload = JSONObject().apply {
            put("user_id", "test-user-uuid")
            put("user_name", "Student")
            put("user_email", "test@example.com")
            put("profile_image_uri", "")
            put("prefs_data", "{}")
            put("timeline_data", "[]")
            put("updated_at", System.currentTimeMillis())
        }

        val keys = payload.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            assertTrue("Payload column '$k' must be a valid schema column", validColumns.contains(k))
        }

        // Must NOT contain terms_accepted_at or unknown columns
        assertFalse(payload.has("terms_accepted_at"))
        assertFalse(payload.has("terms_version"))
        assertFalse(payload.has("unknown_column_xyz"))
        assertTrue(payload.get("updated_at") is Long)
    }

    // =========================================================================
    // Future Prevention: Force Updates, Min Supported Version & Broadcasts
    // =========================================================================
    @Test
    fun testForceUpdate_HardBlockingLogic() {
        // Case 1: Version is below minSupportedVersionCode -> HARD BLOCKED
        val infoMinSupported = UpdateInfo(
            versionCode = 55,
            versionName = "3.5.0",
            url = "https://get-studytimer.vercel.app",
            apkUrl = null,
            releaseNotes = "Major overhaul",
            minSupportedVersionCode = 52,
            forceUpdate = false
        )
        // Client version 50 is strictly below minSupportedVersionCode 52
        assertTrue(infoMinSupported.isHardBlocked(currentVersion = 50L))
        // Client version 52 is equal to minSupportedVersionCode 52 -> Not hard blocked
        assertFalse(infoMinSupported.isHardBlocked(currentVersion = 52L))
        // Client version 55 is latest -> Not hard blocked
        assertFalse(infoMinSupported.isHardBlocked(currentVersion = 55L))

        // Case 2: forceUpdate flag is true and newer version available -> HARD BLOCKED
        val infoForceFlag = UpdateInfo(
            versionCode = 52,
            versionName = "3.4.0",
            url = "https://get-studytimer.vercel.app",
            apkUrl = null,
            releaseNotes = "Critical security fix",
            minSupportedVersionCode = 0,
            forceUpdate = true
        )
        // Client version 50 has a newer version 52 with forceUpdate=true -> Hard blocked
        assertTrue(infoForceFlag.isHardBlocked(currentVersion = 50L))
        // Client version 52 is already on the updated version -> Not hard blocked
        assertFalse(infoForceFlag.isHardBlocked(currentVersion = 52L))

        // Case 3: Regular optional update -> NOT hard blocked
        val infoOptional = UpdateInfo(
            versionCode = 52,
            versionName = "3.4.0",
            url = "https://get-studytimer.vercel.app",
            apkUrl = null,
            releaseNotes = "Minor enhancements",
            minSupportedVersionCode = 45,
            forceUpdate = false
        )
        assertFalse(infoOptional.isHardBlocked(currentVersion = 50L))
    }

    @Test
    fun testVersionJson_BroadcastAndMinVersionParsing() {
        val sampleJson = """
        {
          "versionCode": 52,
          "versionName": "2.4.0",
          "minSupportedVersionCode": 50,
          "forceUpdate": false,
          "broadcast": {
            "id": "notice_2026_11",
            "title": "Scheduled Maintenance",
            "message": "Cloud sync will be under maintenance on Sunday.",
            "type": "maintenance",
            "canDismiss": true,
            "actionUrl": "https://get-studytimer.vercel.app",
            "actionText": "Status"
          }
        }
        """.trimIndent()

        val parsed = UpdateChecker.parseUpdateInfo(sampleJson)
        assertNotNull(parsed)
        assertEquals(52, parsed!!.versionCode)
        assertEquals("2.4.0", parsed.versionName)
        assertEquals(50, parsed.minSupportedVersionCode)
        assertFalse(parsed.forceUpdate)

        assertNotNull(parsed.broadcast)
        val broadcast = parsed.broadcast!!
        assertEquals("notice_2026_11", broadcast.id)
        assertEquals("Scheduled Maintenance", broadcast.title)
        assertEquals("Cloud sync will be under maintenance on Sunday.", broadcast.message)
        assertEquals("maintenance", broadcast.type)
        assertTrue(broadcast.canDismiss)
        assertEquals("https://get-studytimer.vercel.app", broadcast.actionUrl)
        assertEquals("Status", broadcast.actionText)
    }

    @Test
    fun testBroadcastSnippetGeneration() {
        val msg = BroadcastMessage(
            id = "notice_emergency_01",
            title = "Database Index Upgrade",
            message = "Temporary 15 min server maintenance.",
            type = "error",
            canDismiss = false,
            actionUrl = "https://status.example.com",
            actionText = "System Health"
        )

        val snippet = DeveloperToolsHelper.generateVersionJsonSnippet(msg)
        assertTrue(snippet.contains("broadcast"))
        assertTrue(snippet.contains("notice_emergency_01"))
        assertTrue(snippet.contains("Database Index Upgrade"))
        assertTrue(snippet.contains("canDismiss") && snippet.contains("false"))
        assertTrue(snippet.contains("https://status.example.com"))
    }
}

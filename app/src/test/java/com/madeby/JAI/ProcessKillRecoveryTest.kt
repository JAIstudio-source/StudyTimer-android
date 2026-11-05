package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unit test verifying process kill recovery behavior.
 * Setup: prefs have timerState=STUDYING, lastTimestamp = T, timeline has a single STUDYING at T - 3600_000.
 * Trigger the startup reconciliation function.
 * Assert:
 *  (a) timeline now has an IDLE at T
 *  (b) focusSum for today = 3600
 *  (c) computeStatsSnapshot().todayFocus == 3600
 */
class ProcessKillRecoveryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val todayStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private class FakeSharedPreferences(val map: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Number)?.toInt() ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Number)?.toLong() ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Number)?.toFloat() ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listeners.remove(listener)
        }

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val temp = HashMap(map)
            private val modifiedKeys = mutableSetOf<String>()

            override fun putString(k: String?, v: String?) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun putStringSet(k: String?, v: MutableSet<String>?) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun putInt(k: String?, v: Int) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun putLong(k: String?, v: Long) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun putFloat(k: String?, v: Float) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun putBoolean(k: String?, v: Boolean) = apply {
                val key = k ?: ""
                temp[key] = v
                modifiedKeys.add(key)
            }
            override fun remove(k: String?) = apply {
                val key = k ?: ""
                temp.remove(key)
                modifiedKeys.add(key)
            }
            override fun clear() = apply {
                modifiedKeys.addAll(temp.keys)
                temp.clear()
            }
            override fun commit(): Boolean {
                val notifyKeys = modifiedKeys.toList()
                map.clear()
                map.putAll(temp)
                modifiedKeys.clear()
                for (l in ArrayList(listeners)) {
                    for (k in notifyKeys) {
                        l.onSharedPreferenceChanged(this@FakeSharedPreferences, k)
                    }
                }
                return true
            }
            override fun apply() { commit() }
        }
    }

    private lateinit var prefsMap: MutableMap<String, FakeSharedPreferences>

    private fun createMockContext(studyTimerPrefs: FakeSharedPreferences): Context {
        return object : android.content.ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                val key = name ?: "StudyTimerPrefs"
                if (key == "StudyTimerPrefs") return studyTimerPrefs
                return prefsMap.getOrPut(key) { FakeSharedPreferences() }
            }
            override fun getFilesDir(): File {
                return tempFolder.root
            }
            override fun getApplicationContext(): Context {
                return this
            }
        }
    }

    @Before
    fun setUp() {
        TimelineLogger.invalidate()
        prefsMap = mutableMapOf()
    }

    @Test
    fun testProcessKillRecovery_reconcilesUnclosedStudyingSession() {
        val nowSecs = System.currentTimeMillis() / 1000L
        val tSecs = nowSecs - 7200L // 2 hours ago (guaranteed > MAX_ACCEPTABLE_GAP_SECS)
        val tMs = tSecs * 1000L
        val studyStartMs = tMs - 3600_000L // T - 3600s
        val sessionDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(studyStartMs))

        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "timerState" to "STUDYING",
                "lastTimestamp" to tSecs,
                "active_session_date_str" to sessionDateStr,
                "${sessionDateStr}_focus_total" to 0L,
                "accumulatedStudy" to 0L
            )
        )
        val context = createMockContext(fakePrefs)

        // Timeline has a single STUDYING at T - 3600s
        TimelineLogger.recordRaw(context, "STUDYING", timestamp = studyStartMs)

        // Trigger the startup reconciliation function
        TimerService.reconcileStartupState(context)

        // Assert: (a) timeline now has an IDLE at T
        val timeline = TimelineLogger.load(context)
        val lastEntry = timeline.lastOrNull()
        assertNotNull("Timeline must not be empty", lastEntry)
        assertEquals("IDLE", lastEntry?.state)
        assertEquals(tMs, lastEntry?.timestamp)

        // Assert: (b) focusSum for session date = 3600
        val (sessions, _) = StatsEngine(context).dayBlocks(sessionDateStr)
        val focusSum = sessions.filter { !it.running }.sumOf { it.secs }
        assertEquals(3600L, focusSum)

        // Assert: (c) computeStatsSnapshot() focus on session date == 3600
        val snapshot = StatsEngine(context).computeStatsSnapshot()
        val sessionFocus = if (sessionDateStr == todayStr) snapshot.todayFocus else (snapshot.dayFocus[sessionDateStr] ?: 0L)
        assertEquals(3600L, sessionFocus)
    }

    @Test
    fun testCaseA_recentSession_notOrphaned_preserved() {
        val nowSecs = System.currentTimeMillis() / 1000L
        val lastTimestampSecs = nowSecs - 30L // 30 seconds ago
        val initialAccumulated = 1500L

        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "timerState" to "STUDYING",
                "timer_state" to "STUDYING",
                "lastTimestamp" to lastTimestampSecs,
                "active_session_date_str" to todayStr,
                "accumulatedStudy" to initialAccumulated
            )
        )
        val context = createMockContext(fakePrefs)

        // Timeline has an open STUDYING entry
        TimelineLogger.recordRaw(context, "STUDYING", timestamp = (nowSecs - 60L) * 1000L)

        // Trigger startup reconciliation
        val reconciled = TimerService.reconcileStartupState(context)

        // Assert:
        // A. timerState MUST still be STUDYING
        // accumulatedStudy MUST be unchanged
        // timeline MUST NOT have an IDLE entry
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        assertEquals("STUDYING", prefs.getString("timerState", null))
        assertEquals(initialAccumulated, prefs.getLong("accumulatedStudy", 0L))

        val timeline = TimelineLogger.load(context)
        assertFalse("Timeline MUST NOT have an IDLE entry", timeline.any { it.state == "IDLE" })
        assertEquals("STUDYING", timeline.lastOrNull()?.state)
        assertFalse("reconcileStartupState should return false for active session", reconciled)
    }

    @Test
    fun testCaseB_staleSession_orphaned_closedWithIdle() {
        val nowSecs = System.currentTimeMillis() / 1000L
        val lastTimestampSecs = nowSecs - 7200L // 2 hours ago
        val initialAccumulated = 1500L

        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "timerState" to "STUDYING",
                "timer_state" to "STUDYING",
                "lastTimestamp" to lastTimestampSecs,
                "active_session_date_str" to todayStr,
                "accumulatedStudy" to initialAccumulated
            )
        )
        val context = createMockContext(fakePrefs)

        // Timeline has a STUDYING entry from 3 hours ago
        TimelineLogger.recordRaw(context, "STUDYING", timestamp = (nowSecs - 10800L) * 1000L)

        // Trigger startup reconciliation
        val reconciled = TimerService.reconcileStartupState(context)

        // Assert:
        // B. timeline MUST have IDLE at lastTimestamp * 1000L, timerState == IDLE
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        assertEquals("IDLE", prefs.getString("timerState", null))

        val timeline = TimelineLogger.load(context)
        val lastEntry = timeline.lastOrNull()
        assertNotNull("Timeline must not be empty", lastEntry)
        assertEquals("IDLE", lastEntry?.state)
        assertEquals(lastTimestampSecs * 1000L, lastEntry?.timestamp)
        assertTrue("reconcileStartupState should return true for orphaned session", reconciled)
    }

    @Test
    fun testOldFormatMsBackupCorrectedOnRead() {
        // Setup: prefs has lastTimestamp stored in milliseconds (> 100_000_000_000L)
        val nowMs = System.currentTimeMillis()
        val staleMs = nowMs - 7200_000L // 2 hours ago in ms
        val expectedSecs = staleMs / 1000L

        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "timerState" to "STUDYING",
                "timer_state" to "STUDYING",
                "lastTimestamp" to staleMs,
                "active_session_date_str" to todayStr,
                "accumulatedStudy" to 500L
            )
        )
        val context = createMockContext(fakePrefs)
        TimelineLogger.recordRaw(context, "STUDYING", timestamp = staleMs - 3600_000L)

        // Trigger startup reconciliation
        TimerService.reconcileStartupState(context)

        // Assert: prefs "lastTimestamp" has been normalized from ms to seconds
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        assertEquals(expectedSecs, prefs.getLong("lastTimestamp", 0L))

        // And timeline was closed with IDLE at the millisecond equivalent (expectedSecs * 1000L)
        val timeline = TimelineLogger.load(context)
        val lastEntry = timeline.lastOrNull()
        assertNotNull("Timeline must have closed entry", lastEntry)
        assertEquals("IDLE", lastEntry?.state)
        assertEquals(expectedSecs * 1000L, lastEntry?.timestamp)
    }
}

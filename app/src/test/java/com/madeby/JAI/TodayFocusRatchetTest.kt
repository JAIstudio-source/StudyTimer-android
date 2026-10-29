package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Unit tests verifying todayFocus and storedFocus reconciliation behavior.
 * Tests production entry point (StatsEngine.computeStatsSnapshot -> reconcileDayTotals)
 * against timeline state changes.
 */
class TodayFocusRatchetTest {

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

    private fun add30MinBlock(context: Context) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedStart = sdf.parse(todayStr)!!.time
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val blockStartMs = startCal.timeInMillis
        val blockEndMs = blockStartMs + 1800_000L // 30 mins = 1800s
        TimelineLogger.addBlock(context, blockStartMs, blockEndMs, "STUDYING")
    }

    private fun add30MinBreakBlock(context: Context) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedStart = sdf.parse(todayStr)!!.time
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val blockStartMs = startCal.timeInMillis
        val blockEndMs = blockStartMs + 1800_000L // 30 mins = 1800s
        TimelineLogger.addBlock(context, blockStartMs, blockEndMs, "BREAK")
    }

    @Before
    fun setUp() {
        TimelineLogger.invalidate()
        File(tempFolder.root, "focus_timeline.json").delete()
        prefsMap = mutableMapOf()
    }

    /**
     * 1. storedFocus = 3600, timeline empty → after reconcile, assert storedFocus == 0 and todayFocus == 0.
     */
    @Test
    fun test1_storedFocus3600_timelineEmpty() {
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)
        val statsEngine = StatsEngine(context)

        val snapshot = statsEngine.computeStatsSnapshot()
        val storedFocus = fakePrefs.getLong("${todayStr}_focus_total", -1L)

        assertEquals(0L, storedFocus)
        assertEquals(0L, snapshot.todayFocus)
    }

    /**
     * 2. storedFocus = 3600, timeline has one 30-min block → after reconcile, assert storedFocus == 1800 and todayFocus == 1800.
     */
    @Test
    fun test2_storedFocus3600_timelineOne30MinBlock() {
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)
        add30MinBlock(context)
        val statsEngine = StatsEngine(context)

        val snapshot = statsEngine.computeStatsSnapshot()
        val storedFocus = fakePrefs.getLong("${todayStr}_focus_total", -1L)

        assertEquals(1800L, storedFocus)
        assertEquals(1800L, snapshot.todayFocus)
    }

    /**
     * 3. storedFocus = 0, timeline has one 30-min block → after reconcile, assert todayFocus == 1800.
     */
    @Test
    fun test3_storedFocus0_timelineOne30MinBlock() {
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)
        add30MinBlock(context)
        val statsEngine = StatsEngine(context)

        val snapshot = statsEngine.computeStatsSnapshot()

        assertEquals(1800L, snapshot.todayFocus)
    }

    /**
     * 4. storedFocus = 3600, timeline empty, second call to reconcile → assert storedFocus stays 0 (idempotent — no second write fires a listener).
     */
    @Test
    fun test4_storedFocus3600_timelineEmpty_idempotentSecondCall() {
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)
        val statsEngine = StatsEngine(context)

        // First call to reconcile
        val snap1 = statsEngine.computeStatsSnapshot()
        val storedFocus1 = fakePrefs.getLong("${todayStr}_focus_total", -1L)
        assertEquals(0L, storedFocus1)
        assertEquals(0L, snap1.todayFocus)

        // Register listener for changes to storedFocus
        var listenerFired = false
        fakePrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "${todayStr}_focus_total") {
                listenerFired = true
            }
        }

        // Second call to reconcile
        val snap2 = statsEngine.computeStatsSnapshot()
        val storedFocus2 = fakePrefs.getLong("${todayStr}_focus_total", -1L)

        assertEquals(0L, storedFocus2)
        assertEquals(0L, snap2.todayFocus)
        assertFalse("Second call to reconcile must not fire listener on ${todayStr}_focus_total", listenerFired)
    }

    /**
     * 5. test5_breakSymmetricToFocus: Symmetrically tests todayBreak with the four ratchet cases:
     * Case 1: storedBreak = 3600, timeline empty -> after reconcile, assert storedBreak == 0 and todayBreak == 0.
     * Case 2: storedBreak = 3600, timeline has one 30-min block -> after reconcile, assert storedBreak == 1800 and todayBreak == 1800.
     * Case 3: storedBreak = 0, timeline has one 30-min block -> after reconcile, assert todayBreak == 1800.
     * Case 4: storedBreak = 3600, timeline empty, second call to reconcile -> assert storedBreak stays 0 (idempotent — no second write fires a listener).
     */
    @Test
    fun test5_breakSymmetricToFocus() {
        // --- Case 1: storedBreak = 3600, timeline empty -> assert storedBreak == 0 and todayBreak == 0 ---
        TimelineLogger.invalidate()
        File(tempFolder.root, "focus_timeline.json").delete()
        val fakePrefs1 = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "${todayStr}_break_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context1 = createMockContext(fakePrefs1)
        val statsEngine1 = StatsEngine(context1)

        statsEngine1.sanitizeAndHealHistoricalTotals()
        val snapshot1 = statsEngine1.computeStatsSnapshot()
        val storedBreak1 = fakePrefs1.getLong("${todayStr}_break_total", -1L)

        assertEquals("Case 1: storedBreak must reconcile to 0", 0L, storedBreak1)
        assertEquals("Case 1: todayBreak must be 0", 0L, snapshot1.todayBreak)

        // --- Case 2: storedBreak = 3600, timeline has one 30-min block -> assert storedBreak == 1800 and todayBreak == 1800 ---
        TimelineLogger.invalidate()
        File(tempFolder.root, "focus_timeline.json").delete()
        val fakePrefs2 = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "${todayStr}_break_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context2 = createMockContext(fakePrefs2)
        add30MinBreakBlock(context2)
        val statsEngine2 = StatsEngine(context2)

        statsEngine2.sanitizeAndHealHistoricalTotals()
        val snapshot2 = statsEngine2.computeStatsSnapshot()
        val storedBreak2 = fakePrefs2.getLong("${todayStr}_break_total", -1L)

        assertEquals("Case 2: storedBreak must reconcile to 1800", 1800L, storedBreak2)
        assertEquals("Case 2: todayBreak must be 1800", 1800L, snapshot2.todayBreak)

        // --- Case 3: storedBreak = 0, timeline has one 30-min block -> assert todayBreak == 1800 ---
        TimelineLogger.invalidate()
        File(tempFolder.root, "focus_timeline.json").delete()
        val fakePrefs3 = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "${todayStr}_break_total" to 0L,
                "timerState" to "IDLE"
            )
        )
        val context3 = createMockContext(fakePrefs3)
        add30MinBreakBlock(context3)
        val statsEngine3 = StatsEngine(context3)

        statsEngine3.sanitizeAndHealHistoricalTotals()
        val snapshot3 = statsEngine3.computeStatsSnapshot()

        assertEquals("Case 3: todayBreak must be 1800", 1800L, snapshot3.todayBreak)

        // --- Case 4: storedBreak = 3600, timeline empty, second call to reconcile -> assert storedBreak stays 0 (idempotent) ---
        TimelineLogger.invalidate()
        File(tempFolder.root, "focus_timeline.json").delete()
        val fakePrefs4 = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "${todayStr}_break_total" to 3600L,
                "timerState" to "IDLE"
            )
        )
        val context4 = createMockContext(fakePrefs4)
        val statsEngine4 = StatsEngine(context4)

        // First call to reconcile
        statsEngine4.sanitizeAndHealHistoricalTotals()
        val snap4_1 = statsEngine4.computeStatsSnapshot()
        val storedBreak4_1 = fakePrefs4.getLong("${todayStr}_break_total", -1L)
        assertEquals("Case 4: first call storedBreak must be 0", 0L, storedBreak4_1)
        assertEquals("Case 4: first call todayBreak must be 0", 0L, snap4_1.todayBreak)

        // Register listener for changes to storedBreak
        var listenerFired = false
        fakePrefs4.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "${todayStr}_break_total") {
                listenerFired = true
            }
        }

        // Second call to reconcile
        statsEngine4.sanitizeAndHealHistoricalTotals()
        val snap4_2 = statsEngine4.computeStatsSnapshot()
        val storedBreak4_2 = fakePrefs4.getLong("${todayStr}_break_total", -1L)

        assertEquals("Case 4: second call storedBreak must stay 0", 0L, storedBreak4_2)
        assertEquals("Case 4: second call todayBreak must stay 0", 0L, snap4_2.todayBreak)
        assertFalse("Case 4: second call must not fire listener on ${todayStr}_break_total", listenerFired)
    }
}

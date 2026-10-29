package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalKeyConsistencyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val todayStr: String = sdf.format(Date())

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
            if (listener != null && !listeners.contains(listener)) listeners.add(listener)
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listeners.remove(listener)
        }

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        inner class EditorImpl : SharedPreferences.Editor {
            private val temp = HashMap(map)

            override fun putString(key: String?, value: String?) = apply { if (key != null) temp[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) temp[key] = values }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) temp[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) temp[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) temp[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) temp[key] = value }
            override fun remove(key: String?) = apply { if (key != null) temp.remove(key) }
            override fun clear() = apply { temp.clear() }
            override fun commit(): Boolean {
                map.clear()
                map.putAll(temp)
                return true
            }
            override fun apply() { commit() }
        }
    }

    private lateinit var studyPrefs: FakeSharedPreferences
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        studyPrefs = FakeSharedPreferences()
        mockContext = object : android.content.ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return studyPrefs
            }
            override fun getFilesDir(): File {
                return tempFolder.root
            }
            override fun getApplicationContext(): Context {
                return this
            }
        }
    }

    private fun assertKeysEqual(expectedSecs: Long, dateKey: String = todayStr) {
        val dailySecs = studyPrefs.getLong("daily_goal_secs", -1L)
        val dailyMins = studyPrefs.getInt("daily_goal_minutes", -1)
        val dateSecs = studyPrefs.getLong("${dateKey}_goal_secs", -1L)

        assertEquals("daily_goal_secs must match expected", expectedSecs, dailySecs)
        assertEquals("daily_goal_minutes must match expected", (expectedSecs / 60L).toInt(), dailyMins)
        assertEquals("${dateKey}_goal_secs must match expected", expectedSecs, dateSecs)
    }

    @Test
    fun testGoalConsistency_AcrossSetDailyGoal_And_Rollover() {
        // Step 1: Set goal to 14400 (4 hours)
        GoalHistoryManager.setDailyGoal(mockContext, 14400L)
        assertKeysEqual(14400L)

        // Step 2: Simulate next day rollover
        val nextDayStr = "2026-10-29"
        val configuredGoal = StatsEngine.getDailyGoalSecs(studyPrefs)
        assertEquals(14400L, configuredGoal)

        // Rollover simulation writes via setDailyGoal
        GoalHistoryManager.setDailyGoal(mockContext, configuredGoal, forDateStr = nextDayStr)

        // Assert all 3 keys are consistent and reflect 14400
        assertKeysEqual(14400L, dateKey = nextDayStr)

        // Step 3: Change goal to 3600 (60 mins)
        GoalHistoryManager.setDailyGoal(mockContext, 3600L)
        assertKeysEqual(3600L)

        // Step 4: Run each step twice to ensure values do not drift
        GoalHistoryManager.setDailyGoal(mockContext, 3600L)
        assertKeysEqual(3600L)

        GoalHistoryManager.setDailyGoal(mockContext, 3600L, forDateStr = nextDayStr)
        assertKeysEqual(3600L, dateKey = nextDayStr)
    }

    @Test
    fun testGoalConsistency_ReconcileAllHistoricalGoals() {
        // Preset history and totals
        studyPrefs.edit().putLong("2026-10-20_focus_total", 3600L).commit()

        GoalHistoryManager.setDailyGoal(mockContext, 7200L)
        assertKeysEqual(7200L)

        GoalHistoryManager.reconcileAllHistoricalGoals(mockContext)

        // Verify today's keys and historical keys
        assertKeysEqual(7200L)
        val histSecs = studyPrefs.getLong("2026-10-20_goal_secs", -1L)
        assertEquals(7200L, histSecs)

        // Second pass
        GoalHistoryManager.reconcileAllHistoricalGoals(mockContext)
        assertKeysEqual(7200L)
        assertEquals(7200L, studyPrefs.getLong("2026-10-20_goal_secs", -1L))
    }

    @Test
    fun testStatsEngine_GetDailyGoalSecs_IsPureRead() {
        // Only set daily_goal_minutes without daily_goal_secs
        studyPrefs.edit().putInt("daily_goal_minutes", 60).commit()

        val computed = StatsEngine.getDailyGoalSecs(studyPrefs)
        assertEquals(3600L, computed)

        // Must NOT write back into daily_goal_secs
        assertTrue("getDailyGoalSecs must not mutate SharedPreferences", !studyPrefs.contains("daily_goal_secs"))
    }
}

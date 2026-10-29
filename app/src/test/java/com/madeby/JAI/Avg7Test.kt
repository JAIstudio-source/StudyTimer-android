package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Avg7Test {

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
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listeners.remove(listener)
        }

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        inner class EditorImpl : SharedPreferences.Editor {
            private val temp = HashMap(map)
            private val modifiedKeys = mutableSetOf<String>()

            override fun putString(key: String?, value: String?) = apply {
                if (key != null) { temp[key] = value; modifiedKeys.add(key) }
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
                if (key != null) { temp[key] = values; modifiedKeys.add(key) }
            }
            override fun putInt(key: String?, value: Int) = apply {
                if (key != null) { temp[key] = value; modifiedKeys.add(key) }
            }
            override fun putLong(key: String?, value: Long) = apply {
                if (key != null) { temp[key] = value; modifiedKeys.add(key) }
            }
            override fun putFloat(key: String?, value: Float) = apply {
                if (key != null) { temp[key] = value; modifiedKeys.add(key) }
            }
            override fun putBoolean(key: String?, value: Boolean) = apply {
                if (key != null) { temp[key] = value; modifiedKeys.add(key) }
            }
            override fun remove(key: String?) = apply {
                if (key != null) { temp.remove(key); modifiedKeys.add(key) }
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
        File(tempFolder.root, "focus_timeline.json").delete()
        prefsMap = mutableMapOf()
    }

    private fun addBlockForDate(context: Context, dateStr: String, secs: Long) {
        if (secs <= 0L) return
        val parsedStart = sdf.parse(dateStr)!!.time
        val startCal = Calendar.getInstance().apply {
            timeInMillis = parsedStart
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val blockStartMs = startCal.timeInMillis
        val blockEndMs = blockStartMs + secs * 1000L
        TimelineLogger.addBlock(context, blockStartMs, blockEndMs, "STUDYING")
    }

    private fun getDateStringDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return sdf.format(cal.time)
    }

    @Test
    fun testCase1_onlyTodayHasFocus() {
        // Case 1: only today has 10800s (3h), other 6 days have 0. Assert avg7 == 10800 (3h, not 1h 25m).
        val prefs = FakeSharedPreferences()
        val context = createMockContext(prefs)
        addBlockForDate(context, todayStr, 10800L)
        prefs.edit()
            .putLong("${todayStr}_focus_total", 10800L)
            .putString("timerState", "IDLE")
            .commit()

        val statsEngine = StatsEngine(context)
        val snapshot = statsEngine.computeStatsSnapshot()

        assertEquals(10800L, snapshot.avg7)
    }

    @Test
    fun testCase2_todayAndYesterdayHaveFocus() {
        // Case 2: today 3600s, yesterday 7200s, other 5 days 0. Assert avg7 == 5400 (1h 30m = 10800/2).
        val yesterdayStr = getDateStringDaysAgo(1)
        val prefs = FakeSharedPreferences()
        val context = createMockContext(prefs)
        addBlockForDate(context, todayStr, 3600L)
        addBlockForDate(context, yesterdayStr, 7200L)
        prefs.edit()
            .putLong("${todayStr}_focus_total", 3600L)
            .putLong("${yesterdayStr}_focus_total", 7200L)
            .putString("timerState", "IDLE")
            .commit()

        val statsEngine = StatsEngine(context)
        val snapshot = statsEngine.computeStatsSnapshot()

        assertEquals(5400L, snapshot.avg7)
    }

    @Test
    fun testCase3_allSevenDaysZero() {
        // Case 3: all 7 days have 0. Assert avg7 == 0.
        val prefs = FakeSharedPreferences()
        val context = createMockContext(prefs)
        prefs.edit()
            .putString("timerState", "IDLE")
            .commit()

        val statsEngine = StatsEngine(context)
        val snapshot = statsEngine.computeStatsSnapshot()

        assertEquals(0L, snapshot.avg7)
    }

    @Test
    fun testCase4_allSevenDaysHave3600s() {
        // Case 4: all 7 days have 3600s. Assert avg7 == 3600 (1h, same as before — sanity check).
        val prefs = FakeSharedPreferences()
        val context = createMockContext(prefs)
        val editor = prefs.edit()
        for (i in 0 until 7) {
            val dStr = getDateStringDaysAgo(i)
            addBlockForDate(context, dStr, 3600L)
            editor.putLong("${dStr}_focus_total", 3600L)
        }
        editor.putString("timerState", "IDLE").commit()

        val statsEngine = StatsEngine(context)
        val snapshot = statsEngine.computeStatsSnapshot()

        assertEquals(3600L, snapshot.avg7)
    }
}

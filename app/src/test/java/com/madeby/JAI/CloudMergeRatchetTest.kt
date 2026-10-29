package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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

class CloudMergeRatchetTest {

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

    @Test
    fun testMerge_timelineWinsOverHigherCloudVal() = runBlocking {
        // Setup: local timeline has one 30-min block today, local ${today}_focus_total = 1800, cloud has 3600.
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 1800L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)
        add30MinBlock(context)

        val cloudPrefs = JSONObject().apply {
            put("${todayStr}_focus_total", 3600L)
        }
        val cloudRecord = JSONObject().apply {
            put("prefs_data", cloudPrefs.toString())
        }

        // Run merge
        CloudSyncManager.mergeCloudAndLocalData(context, cloudRecord)

        // Assert: after merge, ${today}_focus_total == 1800 (timeline wins)
        val afterMerge = fakePrefs.getLong("${todayStr}_focus_total", 0L)
        assertEquals(1800L, afterMerge)
    }

    @Test
    fun testMerge_prefsWinWhenTimelineIsMissing() = runBlocking {
        // Second test: local timeline empty, local ${today}_focus_total = 0, cloud has 3600.
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 0L,
                "timerState" to "IDLE"
            )
        )
        val context = createMockContext(fakePrefs)

        val cloudPrefs = JSONObject().apply {
            put("${todayStr}_focus_total", 3600L)
        }
        val cloudRecord = JSONObject().apply {
            put("prefs_data", cloudPrefs.toString())
        }

        // Run merge
        CloudSyncManager.mergeCloudAndLocalData(context, cloudRecord)

        // Assert after merge, ${today}_focus_total == 3600 (prefs wins when timeline is missing)
        val afterMerge = fakePrefs.getLong("${todayStr}_focus_total", 0L)
        assertEquals(3600L, afterMerge)
    }

    @Test
    fun testMerge_inFlightSession_preservesPrefs() = runBlocking {
        // Setup: local _focus_total = 3600, timeline has a single STUDYING at T - 30min with no IDLE (in-flight), cloud has _focus_total = 0.
        val now = System.currentTimeMillis()
        val fakePrefs = FakeSharedPreferences(
            mutableMapOf(
                "${todayStr}_focus_total" to 3600L,
                "timerState" to "STUDYING"
            )
        )
        val context = createMockContext(fakePrefs)
        // Single STUDYING at T - 30min with no IDLE
        TimelineLogger.recordRaw(context, "STUDYING", timestamp = now - 1800_000L)

        val cloudPrefs = JSONObject().apply {
            put("${todayStr}_focus_total", 0L)
        }
        val cloudRecord = JSONObject().apply {
            put("prefs_data", cloudPrefs.toString())
        }

        // Run merge
        CloudSyncManager.mergeCloudAndLocalData(context, cloudRecord)

        // Assert: after merge, _focus_total == 3600 (prefs preserved — timer is still running)
        val afterMerge = fakePrefs.getLong("${todayStr}_focus_total", 0L)
        assertEquals(3600L, afterMerge)
    }
}

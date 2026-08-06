package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class TimelineEntry(val timestamp: Long, val state: String, val id: String? = null)

object TimelineLogger {

    private const val KEY = "focus_timeline"

    fun record(context: Context, state: TimerState, id: String? = null) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val obj = JSONObject().put("t", System.currentTimeMillis()).put("s", state.name)
        if (id != null) obj.put("id", id)
        arr.put(obj)
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun recordRaw(context: Context, state: String, timestamp: Long = System.currentTimeMillis(), id: String? = null) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val obj = JSONObject().put("t", timestamp).put("s", state)
        if (id != null) obj.put("id", id)
        var inserted = false
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i)
            if (e != null && e.optLong("t", Long.MAX_VALUE) > timestamp) {
                arr.put(i, obj)
                inserted = true
                break
            }
        }
        if (!inserted) arr.put(obj)
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun load(context: Context): List<TimelineEntry> {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = try { JSONArray(raw) } catch (_: Exception) { return emptyList() }
        val result = ArrayList<TimelineEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            result.add(TimelineEntry(
                e.optLong("t", 0L),
                e.optString("s", ""),
                e.optString("id", "").takeIf { it.isNotEmpty() }
            ))
        }
        return result
    }

    fun deleteEntry(context: Context, startMs: Long) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        val arr = try { JSONArray(raw) } catch (_: Exception) { return }
        val filtered = JSONArray()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            if (e.optLong("t", 0L) != startMs) filtered.put(e)
        }
        prefs.edit().putString(KEY, filtered.toString()).apply()
    }

    fun moveEntry(context: Context, oldTs: Long, newTs: Long) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        val arr = try { JSONArray(raw) } catch (_: Exception) { return }
        val filtered = JSONArray()
        var moved: JSONObject? = null
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            if (moved == null && e.optLong("t", 0L) == oldTs) {
                val clone = JSONObject()
                val it = e.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    clone.put(k, e.get(k))
                }
                clone.put("t", newTs)
                moved = clone
            } else {
                filtered.put(e)
            }
        }
        if (moved != null) {
            var inserted = false
            for (i in 0 until filtered.length()) {
                val e = filtered.optJSONObject(i)
                if (e != null && e.optLong("t", Long.MAX_VALUE) > newTs) {
                    filtered.put(i, moved)
                    inserted = true
                    break
                }
            }
            if (!inserted) filtered.put(moved)
            prefs.edit().putString(KEY, filtered.toString()).apply()
        }
    }

    fun deleteDay(context: Context, dateStr: String) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        val arr = try { JSONArray(raw) } catch (_: Exception) { return }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val filtered = JSONArray()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val ts = e.optLong("t", 0L)
            if (ts > 0L && fmt.format(Date(ts)) != dateStr) filtered.put(e)
        }
        prefs.edit().putString(KEY, filtered.toString()).apply()
    }
}

package com.madeby.JAI

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

fun parseTimeToMinutes(timeStr: String): Int? {
    val parts = timeStr.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
    val list = ArrayList<LectureScheduleItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                LectureScheduleItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    startTime = obj.getString("startTime"),
                    endTime = obj.getString("endTime"),
                    enabled = obj.optBoolean("enabled", true),
                    subjectId = obj.optString("subjectId", "general")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

object LectureScheduleScheduler {

    private const val REQUEST_CODE = 4001

    /**
     * Schedules an AlarmManager alarm for the earliest upcoming enabled lecture.
     *
     * **Design note**: Uses a wake-up alarm chain. Rather than scheduling N separate alarms,
     * this registers a single alarm for the earliest trigger. When fired, [LectureScheduleReceiver]
     * processes matching items and re-calls [scheduleAll] to arm the next event in sequence,
     * while the active timer service maintains per-second real-time verification.
     */
    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (items.isEmpty()) {
            cancelAll(context)
            return
        }

        NotificationHelper.createAllNotificationChannels(context)

        val nowMs = System.currentTimeMillis()
        var earliestMs: Long? = null

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val h = startMins / 60
            val m = startMins % 60

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMs + 10_000L) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val triggerTime = cal.timeInMillis
            if (earliestMs == null || triggerTime < earliestMs) {
                earliestMs = triggerTime
            }
        }

        val targetMs = earliestMs ?: return

        val intent = Intent(context, LectureScheduleReceiver::class.java).apply {
            action = LectureScheduleReceiver.ACTION_LECTURE_START
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exact = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true
        }

        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMs, pending)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetMs, pending)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, LectureScheduleReceiver::class.java).apply {
            action = LectureScheduleReceiver.ACTION_LECTURE_START
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}

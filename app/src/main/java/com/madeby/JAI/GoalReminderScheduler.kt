package com.madeby.JAI

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

object GoalReminderScheduler {

    const val CHANNEL_ID = NotificationHelper.CHANNEL_GOAL_REMINDER
    private const val REQUEST_CODE = 2001
    private const val REMIND_HOUR = 20
    private const val REMIND_MINUTE = 0

    /**
     * Convenience delegation to [NotificationHelper.createAllNotificationChannels]. Kept for call-site
     * backward compatibility across alarm scheduling triggers.
     */
    fun ensureChannel(context: Context) {
        NotificationHelper.createAllNotificationChannels(context)
    }

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", true)) {
            cancel(context)
            return
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_DAILY_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hour = prefs.safeInt("reminder_hour", REMIND_HOUR)
        val minute = prefs.safeInt("reminder_minute", REMIND_MINUTE)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val exact = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true
        }
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_DAILY_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun scheduleGoalReminder(context: Context, goal: PlannerGoal) {
        if (!goal.reminderEnabled) {
            cancelGoalReminder(context, goal.id)
            return
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_GOAL_ITEM_REMIND
            putExtra(GoalReminderReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(GoalReminderReceiver.EXTRA_GOAL_TITLE, goal.title)
        }
        val requestCode = 30000 + (goal.id.hashCode() and 0x7FFFFFFF) % 500000
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, goal.reminderHour)
            set(Calendar.MINUTE, goal.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val exact = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true
        }
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    fun cancelGoalReminder(context: Context, goalId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = GoalReminderReceiver.ACTION_GOAL_ITEM_REMIND
        }
        val requestCode = 30000 + (goalId.hashCode() and 0x7FFFFFFF) % 500000
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun rescheduleAllGoalReminders(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("session_goals_json", "[]") ?: "[]"
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val enabled = obj.optBoolean("reminderEnabled", false)
                val completed = obj.optBoolean("completed", false)
                if (enabled && !completed) {
                    val goal = PlannerGoal(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        reminderEnabled = true,
                        reminderHour = obj.optInt("reminderHour", 20),
                        reminderMinute = obj.optInt("reminderMinute", 0)
                    )
                    scheduleGoalReminder(context, goal)
                }
            }
        } catch (_: Exception) {}
    }
}

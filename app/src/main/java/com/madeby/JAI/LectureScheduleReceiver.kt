package com.madeby.JAI

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LectureScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_LECTURE_START = "com.madeby.JAI.ACTION_LECTURE_START"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                maybeNotifyLecture(context)
                LectureScheduleScheduler.scheduleAll(context)
            }
            ACTION_LECTURE_START -> {
                maybeNotifyLecture(context)
                LectureScheduleScheduler.scheduleAll(context)
            }
        }
    }

    private fun maybeNotifyLecture(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }
        if (items.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val currentSecs = cal.get(Calendar.SECOND)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        // Check if TimerService is already running an active lecture
        val timerState = prefs.getString("timerState", TimerState.IDLE.name)
        val timerMode = prefs.getString("timer_mode", "SUBJECT")
        val lectureModeEnabled = prefs.getBoolean("lecture_mode_enabled", false)
        val isAlreadyRunningLecture = (timerState == TimerState.STUDYING.name || timerState == TimerState.PAUSED.name) &&
                timerMode == "LECTURE" && lectureModeEnabled

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val endMins = parseTimeToMinutes(item.endTime) ?: continue

            // Allow match if current time is within [startMins, endMins)
            if (currentMins in startMins until endMins) {
                val skipKey = "skipped_lecture_${todayStr}_${item.title}_${item.startTime}"
                if (prefs.getBoolean(skipKey, false)) continue

                val switchKey = "lecture_switch_asked_${todayStr}_${item.title}_${item.startTime}"
                if (prefs.getBoolean(switchKey, false)) continue

                val remainingSecs = ((endMins - currentMins) * 60 - currentSecs).toLong().coerceAtLeast(1L)
                if (remainingSecs <= 30L) continue

                val isIdleOrEnded = timerState == TimerState.IDLE.name || timerState == TimerState.LECTURE_ENDED.name || timerState == null
                if (isIdleOrEnded) {
                    // Auto-start scheduled lecture immediately if no active timer is running!
                    prefs.edit()
                        .putBoolean(switchKey, true)
                        .putBoolean("pending_switch_to_lecture", false)
                        .putString("pending_lecture_skip_key", skipKey)
                        .apply()
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_START_LECTURE
                        putExtra(TimerService.EXTRA_LECTURE_TITLE, item.title)
                        putExtra(TimerService.EXTRA_LECTURE_SUBJECT_ID, item.subjectId)
                        putExtra(TimerService.EXTRA_LECTURE_REMAINING_SECS, remainingSecs)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else if (!isAlreadyRunningLecture) {
                    // A different study session is actively running: prompt to switch without abruptly killing it
                    prefs.edit()
                        .putBoolean("pending_switch_to_lecture", true)
                        .putBoolean(switchKey, true)
                        .putString("pending_lecture_title", item.title)
                        .putString("pending_lecture_start", item.startTime)
                        .putString("pending_lecture_end", item.endTime)
                        .putLong("pending_lecture_remaining_secs", remainingSecs)
                        .putString("pending_lecture_skip_key", skipKey)
                        .putString("pending_lecture_subject_id", item.subjectId)
                        .apply()
                    postLectureNotification(context, item, remainingSecs)
                }
                break
            }
        }
    }

    private fun postLectureNotification(context: Context, item: LectureScheduleItem, remainingSecs: Long) {
        NotificationHelper.createAllNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(TimerService.EXTRA_SWITCH_TO_LECTURE, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            5001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMins = (remainingSecs / 60).coerceAtLeast(1)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_LECTURE)
            .setSmallIcon(R.drawable.ic_small_app_logo)
            .setContentTitle("🎓 Class Starting: ${item.title}")
            .setContentText("Class (${item.startTime} – ${item.endTime}) is now in session (~$remainingMins min left). Tap to begin.")
            .setAutoCancel(true)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NotificationHelper.NOTIFICATION_ID_LECTURE, notification)
    }
}

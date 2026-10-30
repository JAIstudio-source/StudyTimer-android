package com.madeby.JAI

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationHelper {

    const val CHANNEL_TIMER = "study_timer_channels"
    const val CHANNEL_COMPLETION = "study_timer_completion_v4"
    const val CHANNEL_GOAL_REMINDER = "goal_reminders_v4"
    const val CHANNEL_LECTURE = "lecture_schedule_v1"
    const val CHANNEL_FESTIVALS = "festivals_v1"
    const val CHANNEL_INACTIVITY = "study_inactivity_v1"
    const val CHANNEL_MILESTONES = "milestones_v1"

    // Legacy channel IDs that previously caused duplicate entries in system notification settings.
    // They are explicitly deleted on startup so user settings stay clean.
    private val LEGACY_CHANNELS_TO_DELETE = listOf(
        "study_timer_channel",
        "study_timer_completion",
        "study_timer_goal_reminder",
        "study_timer_completion_v3",
        "goal_reminders_v3"
    )

    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_COMPLETION = 1002
    const val NOTIFICATION_ID_INACTIVITY_CHECK = 1005
    const val NOTIFICATION_ID_AUTO_PAUSED = 1006
    const val NOTIFICATION_ID_GOAL_REMINDER = 1007
    const val NOTIFICATION_ID_LECTURE = 1008
    const val NOTIFICATION_ID_FESTIVAL = 1009
    const val NOTIFICATION_ID_MILESTONE = 1010

    /**
     * Initializes all app notification channels (Android O+) and cleans up legacy duplicates.
     */
    fun createAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Step 0: Remove legacy duplicate channels from system settings
        LEGACY_CHANNELS_TO_DELETE.forEach { legacyId ->
            try {
                nm.deleteNotificationChannel(legacyId)
            } catch (_: Exception) {}
        }

        // 1. Ongoing Timer Channel (Tray & Status Bar)
        if (nm.getNotificationChannel(CHANNEL_TIMER) == null) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.channel_control_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_control_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(timerChannel)
        }

        // 2. Completion Channel (Pomodoro / Session Finished)
        if (nm.getNotificationChannel(CHANNEL_COMPLETION) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION,
                context.getString(R.string.channel_completion_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_completion_desc)
                enableVibration(false)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(completionChannel)
        }

        // 3. Goal Reminder Channel
        if (nm.getNotificationChannel(CHANNEL_GOAL_REMINDER) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val goalChannel = NotificationChannel(
                CHANNEL_GOAL_REMINDER,
                context.getString(R.string.goal_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.goal_channel_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(goalChannel)
        }

        // 4. Lecture / Timetable Schedule Reminders Channel
        if (nm.getNotificationChannel(CHANNEL_LECTURE) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val lectureChannel = NotificationChannel(
                CHANNEL_LECTURE,
                context.getString(R.string.channel_lecture_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_lecture_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(lectureChannel)
        }

        // 5. Timer Inactivity & Safety Check Channel
        if (nm.getNotificationChannel(CHANNEL_INACTIVITY) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val inactivityChannel = NotificationChannel(
                CHANNEL_INACTIVITY,
                context.getString(R.string.channel_inactivity_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_inactivity_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(inactivityChannel)
        }

        // 6. Streaks & Milestones Channel
        if (nm.getNotificationChannel(CHANNEL_MILESTONES) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val milestoneChannel = NotificationChannel(
                CHANNEL_MILESTONES,
                context.getString(R.string.channel_milestones_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_milestones_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(milestoneChannel)
        }

        // 7. Festival Greetings Channel
        if (nm.getNotificationChannel(CHANNEL_FESTIVALS) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val festChannel = NotificationChannel(
                CHANNEL_FESTIVALS,
                context.getString(R.string.channel_festivals_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_festivals_desc)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            nm.createNotificationChannel(festChannel)
        }
    }
}

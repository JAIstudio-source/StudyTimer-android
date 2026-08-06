package com.madeby.JAI

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class GoalReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMIND = "com.madeby.JAI.ACTION_DAILY_REMIND"
        private const val NOTIFICATION_ID = 3001
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> GoalReminderScheduler.schedule(context)
            ACTION_DAILY_REMIND -> maybeShowReminder(context)
        }
    }

    private fun maybeShowReminder(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val focus = prefs.getLong("${todayStr}_focus_total", 0L) + prefs.getLong("accumulatedStudy", 0L)
        val goal = prefs.getLong("${todayStr}_goal_secs", prefs.getLong("daily_goal_secs", 2700L))
        if (focus >= goal) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val goalLabel = "${goal / 3600}h ${(goal % 3600) / 60}m"
        val notification = NotificationCompat.Builder(context, GoalReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flame)
            .setContentTitle("Daily goal not reached yet")
            .setContentText("You've focused ${focus / 3600}h ${(focus % 3600) / 60}m today. Goal is $goalLabel.")
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}

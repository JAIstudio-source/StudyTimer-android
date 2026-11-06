package com.madeby.JAI

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class StudyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.madeby.JAI.WIDGET_TOGGLE"
        const val ACTION_STOP = "com.madeby.JAI.WIDGET_STOP"
        private val handler = Handler(Looper.getMainLooper())
        private var refreshScheduled = false

        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, StudyWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                render(context, appWidgetManager, ids)
            }
            MatrixGoalWidgetProvider.refresh(context)
            TasksWidgetProvider.refresh(context)
        }

        private fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            val accumulated = prefs.safeLong("accumulatedStudy", 0L)
            val focusToday = prefs.safeLong("${todayStr}_focus_total", 0L) + accumulated
            val streak = prefs.safeInt("current_streak", 0)
            val state = runCatching { TimerState.valueOf(prefs.safeString("timerState", "IDLE") ?: "IDLE") }.getOrDefault(TimerState.IDLE)
            val subject = SubjectTagManager.getSelectedSubject(context)

            val light = prefs.safeString("activeBgMode", "LIGHT") == "LIGHT"
            val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.safeBoolean("dynamic_color", false)) {
                context.getColor(android.R.color.system_accent1_500)
            } else {
                prefs.safeInt("customPrimary", 0xFFA78BFA.toInt())
            }

            val views = RemoteViews(context.packageName, R.layout.study_widget_layout)

            // Background surface
            views.setInt(R.id.widRoot, "setBackgroundResource", if (light) R.drawable.bg_widget_light else R.drawable.bg_widget_dark)

            // Header Row
            views.setTextColor(R.id.widTitle, primary)
            val subjectLabel = if (subject != null) "• ${subject.name}" else ""
            views.setTextViewText(R.id.widSubject, subjectLabel)
            views.setTextViewText(R.id.widStreak, "🔥 ${streak}d streak")

            // Focus Hours & Progress
            val focusHours = focusToday / 3600
            val focusMinutes = (focusToday % 3600) / 60
            views.setTextViewText(R.id.widToday, "${focusHours}h ${focusMinutes}m")

            val goalSecs = prefs.safeLong("${todayStr}_goal_secs", prefs.safeLong("daily_goal_secs", 7200L))
            val percent = if (goalSecs > 0) ((focusToday.toDouble() / goalSecs.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
            views.setProgressBar(R.id.widProgress, 100, percent, false)

            val goalText = if (focusToday >= goalSecs) {
                "Goal complete! ($percent%)"
            } else {
                val leftSecs = goalSecs - focusToday
                val leftH = leftSecs / 3600
                val leftM = (leftSecs % 3600) / 60
                "$percent% • ${leftH}h ${leftM}m left"
            }
            views.setTextViewText(R.id.widGoalText, goalText)

            // State & Interactive Controls
            when (state) {
                TimerState.STUDYING -> {
                    views.setTextViewText(R.id.widToggle, "⏸ PAUSE")
                    views.setTextViewText(R.id.widStop, "⏹ END")
                    views.setViewVisibility(R.id.widStop, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widStatusText, "● Focusing")
                    views.setTextColor(R.id.widStatusText, 0xFF4ADE80.toInt()) // Green
                }
                TimerState.PAUSED -> {
                    views.setTextViewText(R.id.widToggle, "▶ RESUME")
                    views.setTextViewText(R.id.widStop, "⏹ END")
                    views.setViewVisibility(R.id.widStop, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widStatusText, "● Paused")
                    views.setTextColor(R.id.widStatusText, 0xFFFACC15.toInt()) // Yellow/Amber
                }
                TimerState.BREAK -> {
                    views.setTextViewText(R.id.widToggle, "▶ RESUME")
                    views.setTextViewText(R.id.widStop, "⏹ FINISH")
                    views.setViewVisibility(R.id.widStop, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widStatusText, "● On Break")
                    views.setTextColor(R.id.widStatusText, 0xFF38BDF8.toInt()) // Sky blue
                }
                else -> { // IDLE
                    views.setTextViewText(R.id.widToggle, "▶ START FOCUS")
                    views.setViewVisibility(R.id.widStop, android.view.View.GONE)
                    views.setTextViewText(R.id.widStatusText, "Ready to focus")
                    views.setTextColor(R.id.widStatusText, 0x88FFFFFF.toInt())
                }
            }

            // Click Handlers
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widRoot, openPi)
            views.setOnClickPendingIntent(R.id.widHeader, openPi)
            views.setOnClickPendingIntent(R.id.widStatsCol, openPi)

            val toggleIntent = Intent(context, StudyWidgetProvider::class.java).apply { action = ACTION_TOGGLE }
            val togglePi = PendingIntent.getBroadcast(
                context, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widToggle, togglePi)

            val stopIntent = Intent(context, StudyWidgetProvider::class.java).apply { action = ACTION_STOP }
            val stopPi = PendingIntent.getBroadcast(
                context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widStop, stopPi)

            for (id in appWidgetIds) {
                appWidgetManager.updateAppWidget(id, views)
            }

            schedulePeriodicRefresh(context)
        }

        /**
         * Throttled periodic loop for ambient widget updates.
         * Explicit state updates bypass this via [refresh], rendering immediately.
         */
        private fun schedulePeriodicRefresh(context: Context) {
            if (refreshScheduled) return
            refreshScheduled = true
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val state = runCatching { TimerState.valueOf(prefs.safeString("timerState", "IDLE") ?: "IDLE") }.getOrDefault(TimerState.IDLE)
            val delay = if (state != TimerState.IDLE) 30_000L else 5 * 60 * 1000L
            handler.postDelayed({
                refreshScheduled = false
                refresh(context.applicationContext)
            }, delay)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val receivedAction = intent?.action
        if (context == null) return
        when (receivedAction) {
            ACTION_TOGGLE -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_TOGGLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                handler.postDelayed({ refresh(context.applicationContext) }, 1000)
            }
            ACTION_STOP -> {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_STOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                handler.postDelayed({ refresh(context.applicationContext) }, 1000)
            }
        }
    }
}

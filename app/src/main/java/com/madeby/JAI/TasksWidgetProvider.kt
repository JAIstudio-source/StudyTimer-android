package com.madeby.JAI

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimalist To-Do List Home Screen Widget displaying today's tasks
 * from Planner with interactive check-off capabilities.
 */
class TasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TASK = "com.madeby.JAI.WIDGET_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "com.madeby.JAI.EXTRA_TASK_ID"

        data class WidgetTaskItem(
            val id: String,
            val title: String,
            val targetMinutes: Int,
            val completed: Boolean
        )

        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TasksWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                render(context, appWidgetManager, ids)
            }
        }

        private fun loadTasks(context: Context): List<WidgetTaskItem> {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("session_goals_json", "[]") ?: "[]"
            val list = mutableListOf<WidgetTaskItem>()

            try {
                val array = JSONArray(jsonStr)
                val cal = java.util.Calendar.getInstance()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val daysArr = obj.optJSONArray("scheduledDays")
                    val days = mutableListOf<Int>()
                    if (daysArr != null) {
                        for (d in 0 until daysArr.length()) days.add(daysArr.getInt(d))
                    }
                    if (!PlannerHistoryManager.isGoalScheduledOnDay(days, cal)) continue

                    list.add(
                        WidgetTaskItem(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", "Task"),
                            targetMinutes = obj.optInt("targetMinutes", 0),
                            completed = obj.optBoolean("completed", false)
                        )
                    )
                }
            } catch (_: Exception) {
            }

            // Show pending tasks first, followed by completed ones
            return list.sortedWith(compareBy({ it.completed }, { it.title }))
        }

        private fun render(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val light = prefs.safeString("activeBgMode", "OLED") == "LIGHT"
            val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.safeBoolean("dynamic_color", false)) {
                context.getColor(android.R.color.system_accent1_500)
            } else {
                prefs.safeInt("customPrimary", 0xFFA78BFA.toInt())
            }

            val tasks = loadTasks(context)
            val pendingCount = tasks.count { !it.completed }

            val rowIds = listOf(
                R.id.widTaskRow1,
                R.id.widTaskRow2,
                R.id.widTaskRow3,
                R.id.widTaskRow4
            )
            val checkIds = listOf(
                R.id.widTaskCheck1,
                R.id.widTaskCheck2,
                R.id.widTaskCheck3,
                R.id.widTaskCheck4
            )
            val titleIds = listOf(
                R.id.widTaskTitle1,
                R.id.widTaskTitle2,
                R.id.widTaskTitle3,
                R.id.widTaskTitle4
            )
            val tagIds = listOf(
                R.id.widTaskTag1,
                R.id.widTaskTag2,
                R.id.widTaskTag3,
                R.id.widTaskTag4
            )

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_tasks_layout)

                // Background
                views.setInt(
                    R.id.widTasksRoot,
                    "setBackgroundResource",
                    if (light) R.drawable.bg_widget_light else R.drawable.bg_widget_dark
                )

                // Header
                views.setTextColor(R.id.widTasksTitle, primary)
                views.setTextViewText(R.id.widTasksBadge, "$pendingCount pending")
                views.setTextColor(R.id.widTasksAction, primary)

                // Open Planner intent
                val openPlannerIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_OPEN_PLANNER", true)
                }
                val pendingOpenPlanner = PendingIntent.getActivity(
                    context,
                    201,
                    openPlannerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widTasksHeader, pendingOpenPlanner)
                views.setOnClickPendingIntent(R.id.widTasksFooter, pendingOpenPlanner)
                views.setOnClickPendingIntent(R.id.widTasksEmpty, pendingOpenPlanner)

                if (tasks.isEmpty()) {
                    views.setViewVisibility(R.id.widTasksEmpty, View.VISIBLE)
                    views.setViewVisibility(R.id.widTasksListContainer, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widTasksEmpty, View.GONE)
                    views.setViewVisibility(R.id.widTasksListContainer, View.VISIBLE)

                    for (i in 0 until 4) {
                        val rowId = rowIds[i]
                        val checkId = checkIds[i]
                        val titleId = titleIds[i]
                        val tagId = tagIds[i]

                        if (i < tasks.size) {
                            val task = tasks[i]
                            views.setViewVisibility(rowId, View.VISIBLE)

                            if (task.completed) {
                                views.setTextViewText(checkId, "✓")
                                views.setTextColor(checkId, 0xFF4ADE80.toInt()) // Soft green
                                views.setTextColor(titleId, if (light) 0x66000000 else 0x66FFFFFF)
                                views.setTextViewText(titleId, "${task.title}")
                            } else {
                                views.setTextViewText(checkId, "○")
                                views.setTextColor(checkId, primary)
                                views.setTextColor(titleId, if (light) 0xFF1F2937.toInt() else 0xFFFFFFFF.toInt())
                                views.setTextViewText(titleId, task.title)
                            }

                            if (task.targetMinutes > 0) {
                                views.setTextViewText(tagId, "${task.targetMinutes}m")
                                views.setViewVisibility(tagId, View.VISIBLE)
                            } else {
                                views.setViewVisibility(tagId, View.GONE)
                            }

                            // Interactive toggle broadcast intent
                            val toggleIntent = Intent(context, TasksWidgetProvider::class.java).apply {
                                action = ACTION_TOGGLE_TASK
                                putExtra(EXTRA_TASK_ID, task.id)
                            }
                            val pendingToggle = PendingIntent.getBroadcast(
                                context,
                                300 + i,
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(rowId, pendingToggle)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            if (!taskId.isNullOrEmpty()) {
                toggleTask(context, taskId)
            }
        }
        refresh(context)
    }

    private fun toggleTask(context: Context, taskId: String) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("session_goals_json", "[]") ?: "[]"

        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") == taskId) {
                    val wasCompleted = obj.optBoolean("completed", false)
                    val newCompleted = !wasCompleted
                    obj.put("completed", newCompleted)
                    if (newCompleted) {
                        obj.put("checkedAt", System.currentTimeMillis())
                    } else {
                        obj.put("checkedAt", 0L)
                    }
                    break
                }
            }
            prefs.edit().putString("session_goals_json", array.toString()).apply()
            PlannerStateManager.notifyGoalsChanged()
        } catch (_: Exception) {
        }
    }
}


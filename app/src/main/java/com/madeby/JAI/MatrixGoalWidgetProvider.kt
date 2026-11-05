package com.madeby.JAI

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimalist Home Screen Widget displaying the user's daily study progress
 * with a clean 6x4 (24-hour) micro-block matrix visualization.
 */
class MatrixGoalWidgetProvider : AppWidgetProvider() {

    companion object {
        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MatrixGoalWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                render(context, appWidgetManager, ids)
            }
        }

        private fun render(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())

            val accumulated = prefs.safeLong("accumulatedStudy", 0L)
            val focusToday = prefs.safeLong("${todayStr}_focus_total", 0L) + accumulated
            val streak = prefs.safeInt("current_streak", 0)
            val subject = SubjectTagManager.getSelectedSubject(context)

            val light = prefs.safeString("activeBgMode", "OLED") == "LIGHT"
            val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.safeBoolean("dynamic_color", false)) {
                context.getColor(android.R.color.system_accent1_500)
            } else {
                prefs.safeInt("customPrimary", 0xFFA78BFA.toInt())
            }

            val goalSecs = prefs.safeLong("${todayStr}_goal_secs", prefs.safeLong("daily_goal_secs", 7200L))
            val percent = if (goalSecs > 0) ((focusToday.toDouble() / goalSecs.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

            val focusHours = focusToday / 3600
            val focusMinutes = (focusToday % 3600) / 60
            val goalH = goalSecs / 3600
            val goalM = (goalSecs % 3600) / 60

            val matrixBmp = createMatrixBitmap(context, focusToday, goalSecs, primary, light)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_matrix_layout)

                // Background
                views.setInt(
                    R.id.widMatrixRoot,
                    "setBackgroundResource",
                    if (light) R.drawable.bg_widget_light else R.drawable.bg_widget_dark
                )

                // Header
                views.setTextColor(R.id.widMatrixTitle, primary)
                val subjectLabel = if (subject != null) "• ${subject.name}" else ""
                views.setTextViewText(R.id.widMatrixSubject, subjectLabel)
                views.setTextViewText(R.id.widMatrixStreak, "🔥 ${streak}d")

                // Time & Target
                views.setTextViewText(R.id.widMatrixFocusTime, "${focusHours}h ${String.format("%02d", focusMinutes)}m")
                views.setTextViewText(R.id.widMatrixTargetText, "Goal: ${goalH}h ${String.format("%02d", goalM)}m ($percent%)")
                views.setTextColor(R.id.widMatrixActionHint, primary)

                // Matrix visualization
                views.setImageViewBitmap(R.id.widMatrixCanvas, matrixBmp)

                // Click to open main app
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    101,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widMatrixRoot, pendingOpen)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun createMatrixBitmap(
            context: Context,
            focusSecs: Long,
            goalSecs: Long,
            primaryColor: Int,
            isLight: Boolean
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = (120 * density).toInt().coerceAtLeast(120)
            val height = (64 * density).toInt().coerceAtLeast(64)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cols = 6
            val rows = 4
            val totalBlocks = cols * rows // 24 micro-blocks

            val progressRatio = if (goalSecs > 0) (focusSecs.toDouble() / goalSecs.toDouble()).coerceIn(0.0, 1.0) else 0.0
            val activeBlocks = (progressRatio * totalBlocks).toInt()

            val padX = 4 * density
            val padY = 4 * density
            val blockW = (width - padX * 2) / cols
            val blockH = (height - padY * 2) / rows
            val corner = 3 * density

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            var count = 0
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val left = padX + c * blockW + 1.5f * density
                    val top = padY + r * blockH + 1.5f * density
                    val right = left + blockW - 3f * density
                    val bottom = top + blockH - 3f * density
                    val rect = RectF(left, top, right, bottom)

                    if (count < activeBlocks) {
                        paint.color = primaryColor
                        paint.style = Paint.Style.FILL
                        canvas.drawRoundRect(rect, corner, corner, paint)
                    } else if (count == activeBlocks && progressRatio > 0) {
                        paint.color = (primaryColor and 0x00FFFFFF) or 0x66000000
                        paint.style = Paint.Style.FILL
                        canvas.drawRoundRect(rect, corner, corner, paint)
                    } else {
                        val emptyColor = if (isLight) 0x1A000000 else 0x1FFFFFFF
                        paint.color = emptyColor
                        paint.style = Paint.Style.FILL
                        canvas.drawRoundRect(rect, corner, corner, paint)
                    }
                    count++
                }
            }

            return bitmap
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        refresh(context)
    }
}


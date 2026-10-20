package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import android.view.GestureDetector
import kotlin.math.max

class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDayTap: ((String) -> Unit)? = null
    var onDayLongClick: ((String) -> Unit)? = null
    var forcedCellSize: Float = 0f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val cells = HashMap<String, Long>()
    private val dayDates = ArrayList<String>()
    private var todayStr = ""
    private var numCols = 0
    private var cellSize = 12f
    private var gapPx = 0f
    private var labelW = 0f
    private var topPad = 0f
    private var monthRowH = 0f
    private var rowH = 0f
    private var startX = 0f
    private val minCellSize = 14f * resources.displayMetrics.density

    private var primary = Color.HSVToColor(floatArrayOf(190f, 0.65f, 0.95f))
    private var textColor = Color.WHITE
    private var emptyColor = Color.argb(20, 255, 255, 255)
    private var futureColor = Color.argb(8, 255, 255, 255)
    private var headerTextColor = Color.argb(220, 255, 255, 255)
    private var rowTextColor = Color.argb(200, 255, 255, 255)
    private var goalFor: ((String) -> Long)? = null

    private fun isTextDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luminance < 0.5
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            performClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            handleLongClick(e.x, e.y)
        }
    })

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokeCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 10f * resources.displayMetrics.density
    }
    private val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val weekdayLetters = arrayOf("M", "T", "W", "T", "F", "S", "S")

    fun setData(data: Map<String, Long>, primaryColor: Int, text: Int, goalFor: (String) -> Long) {
        primary = primaryColor
        textColor = text
        this.goalFor = goalFor

        val isLightMode = isTextDark(text)
        emptyColor = if (isLightMode) 0xFFF1F5F9.toInt() else Color.argb(25, 255, 255, 255)
        futureColor = if (isLightMode) 0xFFF8FAFC.toInt() else Color.argb(10, 255, 255, 255)
        headerTextColor = if (isLightMode) Color.argb(180, Color.red(text), Color.green(text), Color.blue(text)) else Color.argb(220, 255, 255, 255)
        rowTextColor = if (isLightMode) Color.argb(160, Color.red(text), Color.green(text), Color.blue(text)) else Color.argb(200, 255, 255, 255)

        cells.clear()
        cells.putAll(data)
        todayStr = dateSdf.format(Date())

        computeRollingDates(26)

        requestLayout()
        invalidate()
    }

    private val monthHeaders = ArrayList<Pair<Int, String>>()

    private fun computeRollingDates(targetCols: Int) {
        numCols = max(26, targetCols)
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // Find the date user started studying (earliest date with recorded study focus > 0)
        val earliestStudyDateStr = cells.filter { it.value > 0L }.keys.minOrNull()
        val startCal = if (!earliestStudyDateStr.isNullOrBlank()) {
            try {
                val parsed = dateSdf.parse(earliestStudyDateStr)
                Calendar.getInstance().apply {
                    time = parsed!!
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
            } catch (_: Exception) {
                (todayCal.clone() as Calendar)
            }
        } else {
            (todayCal.clone() as Calendar)
        }

        // Start strictly from that day's whole column (Monday of that start date's week)
        val userStartMonday = WeekHelper.mondayOf(startCal)
        val todayMonday = WeekHelper.mondayOf(todayCal)

        val weeksSinceUserStart = ((todayMonday.timeInMillis - userStartMonday.timeInMillis) / (7L * 86400000L)).toInt().coerceAtLeast(0)

        // Maintain at least 3 future columns runway on the right
        val minFutureRunway = 3
        val maxTodayCol = (numCols - 1 - minFutureRunway).coerceAtLeast(1)

        val startMonday = if (weeksSinceUserStart > maxTodayCol) {
            // When near to filling whole: move one column at a time without resetting whole map
            val overflowWeeks = weeksSinceUserStart - maxTodayCol
            Calendar.getInstance().apply {
                timeInMillis = userStartMonday.timeInMillis + (overflowWeeks * 7L) * 86400000L
            }
        } else {
            // Starts with the column of the start date
            userStartMonday
        }

        dayDates.clear()
        monthHeaders.clear()

        var lastMonth = -1
        var lastLabelCol = -99
        for (col in 0 until numCols) {
            val colStartCal = Calendar.getInstance().apply {
                timeInMillis = startMonday.timeInMillis + (col * 7L) * 86400000L
            }
            val m = colStartCal.get(Calendar.MONTH)
            if (col == 0 || (m != lastMonth && (col - lastLabelCol) >= 3)) {
                val mName = monthSdf.format(colStartCal.time)
                monthHeaders.add(Pair(col, mName))
                lastMonth = m
                lastLabelCol = col
            }

            for (row in 0..6) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = startMonday.timeInMillis + (col * 7L + row) * 86400000L
                }
                dayDates.add(dateSdf.format(c.time))
            }
        }
    }

    fun getTodayScrollX(viewportWidth: Int): Int {
        val todayIdx = dayDates.indexOf(todayStr)
        val todayCol = if (todayIdx >= 0) todayIdx / 7 else 0
        val todayX = startX + labelW + todayCol * (cellSize + gapPx) + cellSize / 2f
        val scrollX = (todayX - viewportWidth / 2f).toInt()
        return max(0, scrollX)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        gapPx = 3f * density
        labelW = 18f * density
        topPad = 6f * density
        monthRowH = 18f * density

        if (forcedCellSize > 0f) {
            cellSize = forcedCellSize
            if (numCols < 26) {
                computeRollingDates(26)
            }
            rowH = cellSize + gapPx
            val totalGridW = labelW + numCols * (cellSize + gapPx) - gapPx
            startX = 0f
            val height = (topPad + monthRowH + 7 * rowH + 6 * density).toInt()
            setMeasuredDimension((startX + totalGridW).toInt(), height)
        } else {
            val targetCols = 26
            if (targetCols != numCols || dayDates.isEmpty()) {
                computeRollingDates(targetCols)
            }
            cellSize = 18f * density
            rowH = cellSize + gapPx
            val totalGridW = labelW + numCols * (cellSize + gapPx) - gapPx
            startX = 0f
            val measuredW = max(width.toFloat(), totalGridW).toInt()
            val height = (topPad + monthRowH + 7 * rowH + 6 * density).toInt()
            setMeasuredDimension(measuredW, height)
        }
    }

    private fun levelFor(focus: Long, dateStr: String): Int {
        if (focus <= 0L) return 0
        val goal = goalFor?.invoke(dateStr) ?: 2700L
        if (goal <= 0L) return 4
        val f = focus.toFloat()
        val g = goal.toFloat()
        return when {
            f <= g * (1f / 6f) -> 1
            f <= g * (2.5f / 6f) -> 2
            f <= g * (4f / 6f) -> 3
            else -> 4
        }
    }

    private fun levelColor(level: Int): Int {
        if (level <= 0) return emptyColor
        val isLight = isTextDark(textColor)
        val alpha = if (isLight) {
            when (level) {
                1 -> 75
                2 -> 135
                3 -> 195
                else -> 255
            }
        } else {
            when (level) {
                1 -> 35
                2 -> 75
                3 -> 135
                else -> 200
            }
        }
        return Color.argb(alpha, Color.red(primary), Color.green(primary), Color.blue(primary))
    }

    private val cellRect = RectF()
    private val cellRadius by lazy { 4f * resources.displayMetrics.density }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val isLight = isTextDark(textColor)

        textPaint.color = headerTextColor
        val oldAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.LEFT
        for ((col, mName) in monthHeaders) {
            val colX = startX + labelW + col * (cellSize + gapPx)
            canvas.drawText(mName, colX, topPad + monthRowH - 4f * resources.displayMetrics.density, textPaint)
        }
        textPaint.textAlign = oldAlign

        for (row in 0..6) {
            textPaint.color = rowTextColor
            val ly = topPad + monthRowH + row * rowH + cellSize / 2f + textPaint.textSize / 3f
            canvas.drawText(weekdayLetters[row], startX + labelW / 2f, ly, textPaint)

            for (col in 0 until numCols) {
                val idx = col * 7 + row
                val dateStr = dayDates.getOrNull(idx) ?: continue
                val x = startX + labelW + col * (cellSize + gapPx)
                val y = topPad + monthRowH + row * rowH
                val future = dateStr > todayStr
                val lvl = levelFor(cells[dateStr] ?: 0L, dateStr)
                cellRect.set(x, y, x + cellSize, y + cellSize)

                if (future) {
                    paint.color = futureColor
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, paint)
                    strokeCellPaint.color = if (isLight) 0xFFE2E8F0.toInt() else Color.argb(18, 255, 255, 255)
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, strokeCellPaint)
                } else if (lvl <= 0) {
                    paint.color = emptyColor
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, paint)
                    strokeCellPaint.color = if (isLight) 0xFFCBD5E1.toInt() else Color.argb(20, 255, 255, 255)
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, strokeCellPaint)
                } else {
                    paint.color = levelColor(lvl)
                    canvas.drawRoundRect(cellRect, cellRadius, cellRadius, paint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        val col = floor((x - startX - labelW) / (cellSize + gapPx)).toInt()
        val row = floor((y - topPad - monthRowH) / rowH).toInt()
        if (col in 0 until numCols && row in 0..6) {
            val idx = col * 7 + row
            val dateStr = dayDates.getOrNull(idx) ?: return
            if (dateStr > todayStr) return
            onDayTap?.invoke(dateStr)
        }
    }

    private fun handleLongClick(x: Float, y: Float) {
        val col = floor((x - startX - labelW) / (cellSize + gapPx)).toInt()
        val row = floor((y - topPad - monthRowH) / rowH).toInt()
        if (col in 0 until numCols && row in 0..6) {
            val idx = col * 7 + row
            val dateStr = dayDates.getOrNull(idx) ?: return
            if (dateStr > todayStr) return
            onDayLongClick?.invoke(dateStr)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun parseSafe(dateStr: String): Date {
        return try { dateSdf.parse(dateStr) ?: Date() } catch (_: Exception) { Date() }
    }
}

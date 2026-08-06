package com.madeby.JAI.cardtest

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.madeby.JAI.WeeklyCardView
import java.io.File
import javax.imageio.ImageIO

private val WEEK_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

fun days(vararg d: Triple<String, Long, Long>): List<WeeklyCardView.Day> {
    val out = ArrayList<WeeklyCardView.Day>(7)
    for (i in 0..6) {
        if (i < d.size) {
            out.add(WeeklyCardView.Day(d[i].first, d[i].second, d[i].third))
        } else {
            out.add(WeeklyCardView.Day(WEEK_LABELS[i], 0L, 5400L))
        }
    }
    return out
}

fun main() {
    val ctx = Context()
    val outDir = File("out").apply { mkdirs() }
    val W = 1080
    val H = 1920

    fun render(name: String, data: WeeklyCardView.CardData) {
        val card = WeeklyCardView(ctx).apply { setData(data) }
        card.measure(
            View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY)
        )
        card.layout(0, 0, W, H)
        val img = java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        card.draw(Canvas(img))
        ImageIO.write(img, "png", File(outDir, name))
        println("wrote $name")
    }

    // 1. Rich week: goal ring ~67%, best-day marker, streak, delta
    render("01_rich_week.png", WeeklyCardView.CardData(
        dateRange = "JUL 27 – AUG 02",
        totalSecs = 67200L,   // 18h40m
        breakSecs = 9000L,    // 2h30m
        bestName = "Tuesday", bestSecs = 14400L,
        streak = 5, vsPrev = "+32%", sessionCount = 12,
        days = days(
            Triple("M", 10800L, 7200L),
            Triple("T", 14400L, 7200L),
            Triple("W", 9000L, 7200L),
            Triple("T", 12600L, 7200L),
            Triple("F", 7200L, 7200L),
            Triple("S", 3600L, 7200L),
            Triple("S", 9600L, 7200L)
        ),
        hasData = true
    ))

    // 2. Empty week: placeholder state
    render("02_empty_week.png", WeeklyCardView.CardData(
        dateRange = "AUG 03 – AUG 09",
        totalSecs = 0L, breakSecs = 0L,
        bestName = "", bestSecs = 0L,
        streak = 0, vsPrev = "No previous data", sessionCount = 0,
        days = days(),
        hasData = false
    ))

    // 3. Minimal: one low day, goal line too high (should be suppressed), tiny bars
    render("03_minimal_week.png", WeeklyCardView.CardData(
        dateRange = "JUL 13 – JUL 19",
        totalSecs = 3600L, breakSecs = 600L,
        bestName = "Sunday", bestSecs = 3600L,
        streak = 1, vsPrev = "No previous data", sessionCount = 1,
        days = days(
            Triple("M", 0L, 5400L),
            Triple("T", 0L, 5400L),
            Triple("W", 0L, 5400L),
            Triple("T", 0L, 5400L),
            Triple("F", 0L, 5400L),
            Triple("S", 0L, 5400L),
            Triple("S", 3600L, 5400L)
        ),
        hasData = true
    ))

    // 4. Huge values: big hero text, long streak, tall bars
    render("04_huge_values.png", WeeklyCardView.CardData(
        dateRange = "JAN 05 – JAN 11",
        totalSecs = 720000L,  // 200h
        breakSecs = 54000L,
        bestName = "Saturday", bestSecs = 144000L,
        streak = 30, vsPrev = "+1200%", sessionCount = 99,
        days = days(
            Triple("M", 72000L, 36000L),
            Triple("T", 54000L, 36000L),
            Triple("W", 108000L, 36000L),
            Triple("T", 36000L, 36000L),
            Triple("F", 90000L, 36000L),
            Triple("S", 144000L, 36000L),
            Triple("S", 72000L, 36000L)
        ),
        hasData = true
    ))

    // 5. Goal fully met: 100% ring, focus-heavy donut, negative delta
    render("05_goal_met.png", WeeklyCardView.CardData(
        dateRange = "FEB 02 – FEB 08",
        totalSecs = 120000L, breakSecs = 12000L,
        bestName = "Saturday", bestSecs = 24000L,
        streak = 7, vsPrev = "-15%", sessionCount = 21,
        days = days(
            Triple("M", 14400L, 14400L),
            Triple("T", 14400L, 14400L),
            Triple("W", 18000L, 14400L),
            Triple("T", 14400L, 14400L),
            Triple("F", 16000L, 14400L),
            Triple("S", 24000L, 14400L),
            Triple("S", 16800L, 14400L)
        ),
        hasData = true
    ))

    // 6. Even focus/break split: donut 50/50, no previous data
    render("06_even_split.png", WeeklyCardView.CardData(
        dateRange = "MAR 09 – MAR 15",
        totalSecs = 36000L, breakSecs = 36000L,
        bestName = "Monday", bestSecs = 7200L,
        streak = 0, vsPrev = "No previous data", sessionCount = 8,
        days = days(
            Triple("M", 7200L, 5400L),
            Triple("T", 5400L, 5400L),
            Triple("W", 7200L, 5400L),
            Triple("T", 0L, 5400L),
            Triple("F", 5400L, 5400L)
        ),
        hasData = true
    ))
}

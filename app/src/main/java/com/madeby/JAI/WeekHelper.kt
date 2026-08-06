package com.madeby.JAI

import java.util.Calendar

object WeekHelper {

    const val DAY_MS = 86400000L

    fun mondayOf(cal: Calendar): Calendar {
        val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
        var dow = c.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) dow = 8
        c.add(Calendar.DAY_OF_YEAR, -(dow - Calendar.MONDAY))
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    fun mondayOffset(cal: Calendar): Int {
        var dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) dow = 8
        return dow - Calendar.MONDAY
    }
}

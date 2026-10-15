package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExamCountdownManagerTest {

    @Test
    fun testExamIsToday() {
        val now = System.currentTimeMillis()
        val breakdown = ExamCountdownManager.getCountdownBreakdown(now)

        assertTrue(breakdown.isToday)
        assertFalse(breakdown.isPast)
        assertEquals("Exam is Today", breakdown.mainHeadline)
        assertEquals("Scheduled for today", breakdown.readableSubtitle)
        assertEquals(0L, breakdown.totalDays)
    }

    @Test
    fun testFutureExamDaysLeft() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 5)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        val breakdown = ExamCountdownManager.getCountdownBreakdown(cal.timeInMillis)

        assertFalse(breakdown.isToday)
        assertFalse(breakdown.isPast)
        assertTrue(breakdown.totalDays in 4L..6L)
        assertTrue(breakdown.mainHeadline.endsWith("Days Left"))
    }

    @Test
    fun testPastExamDaysAgo() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -3)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val breakdown = ExamCountdownManager.getCountdownBreakdown(cal.timeInMillis)

        assertFalse(breakdown.isToday)
        assertTrue(breakdown.isPast)
        assertEquals(3L, breakdown.daysAgo)
        assertEquals("3 days ago", breakdown.mainHeadline)
        assertEquals("Exam date has passed", breakdown.readableSubtitle)
    }

    @Test
    fun testPastExamOneDayAgo() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val breakdown = ExamCountdownManager.getCountdownBreakdown(cal.timeInMillis)

        assertFalse(breakdown.isToday)
        assertTrue(breakdown.isPast)
        assertEquals(1L, breakdown.daysAgo)
        assertEquals("1 day ago", breakdown.mainHeadline)
    }
}

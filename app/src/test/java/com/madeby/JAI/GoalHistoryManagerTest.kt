package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalHistoryManagerTest {

    @Test
    fun testParseHistoryEmpty() {
        val emptyResult = GoalHistoryManager.parseHistory("")
        assertTrue(emptyResult.isEmpty())

        val invalidJson = GoalHistoryManager.parseHistory("{not: an: array}")
        assertTrue(invalidJson.isEmpty())
    }

    @Test
    fun testParseHistoryValidSorted() {
        val json = """
            [
                {"date": "2026-10-14", "goalSecs": 3600, "timestamp": 1000},
                {"date": "2026-10-12", "goalSecs": 1800, "timestamp": 2000},
                {"date": "2026-10-15", "goalSecs": 5400, "timestamp": 3000}
            ]
        """.trimIndent()

        val parsed = GoalHistoryManager.parseHistory(json)
        assertEquals(3, parsed.size)
        // Should be sorted by date ascending
        assertEquals("2026-10-12", parsed[0].date)
        assertEquals(1800L, parsed[0].goalSecs)
        assertEquals("2026-10-14", parsed[1].date)
        assertEquals(3600L, parsed[1].goalSecs)
        assertEquals("2026-10-15", parsed[2].date)
        assertEquals(5400L, parsed[2].goalSecs)
    }

    @Test
    fun testParseHistoryFiltersInvalidEntries() {
        val json = """
            [
                {"date": "2026-10-10", "goalSecs": -500, "timestamp": 1000},
                {"date": "", "goalSecs": 3600, "timestamp": 2000},
                {"date": "2026-10-11", "goalSecs": 0, "timestamp": 3000},
                {"date": "2026-10-12", "goalSecs": 2700, "timestamp": 4000}
            ]
        """.trimIndent()

        val parsed = GoalHistoryManager.parseHistory(json)
        assertEquals(1, parsed.size)
        assertEquals("2026-10-12", parsed[0].date)
        assertEquals(2700L, parsed[0].goalSecs)
    }
}

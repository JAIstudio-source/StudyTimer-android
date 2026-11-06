package com.madeby.JAI

import org.junit.Assert.*
import org.junit.Test

class FestivalManagerTest {

    private fun containsEmoji(s: String): Boolean {
        // Checks for common Unicode emoji code point blocks
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            if (cp in 0x1F600..0x1F64F || // Emoticons
                cp in 0x1F300..0x1F5FF || // Misc Symbols and Pictographs
                cp in 0x1F680..0x1F6FF || // Transport and Map
                cp in 0x1F700..0x1F77F || // Alchemical Symbols
                cp in 0x1F780..0x1F7FF || // Geometric Shapes Extended
                cp in 0x1F800..0x1F8FF || // Supplemental Arrows-C
                cp in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs
                cp in 0x1FA00..0x1FA6F || // Chess Symbols
                cp in 0x1FA70..0x1FAFF || // Symbols and Pictographs Extended-A
                cp in 0x2600..0x26FF   || // Misc symbols
                cp in 0x2700..0x27BF   || // Dingbats
                cp in 0xFE00..0xFE0F   || // Variation Selectors
                cp in 0x1F1E6..0x1F1FF    // Flags
            ) {
                return true
            }
            i += Character.charCount(cp)
        }
        return false
    }

    @Test
    fun testNoEmojisInAnyFestivalContent() {
        for (festival in FestivalType.values()) {
            assertFalse("Display name must not contain emoji: ${festival.displayName}", containsEmoji(festival.displayName))
            assertFalse("Default title must not contain emoji: ${festival.defaultTitle}", containsEmoji(festival.defaultTitle))
            for (greeting in festival.greetings) {
                assertFalse("Greeting title template must not contain emoji: ${greeting.titleTemplate}", containsEmoji(greeting.titleTemplate))
                assertFalse("Greeting message template must not contain emoji: ${greeting.messageTemplate}", containsEmoji(greeting.messageTemplate))
            }
        }
    }

    @Test
    fun testNoGuiltMentionedInAnyFestivalContent() {
        for (festival in FestivalType.values()) {
            for (greeting in festival.greetings) {
                val fullText = (greeting.titleTemplate + " " + greeting.messageTemplate).lowercase()
                assertFalse("Must not mention guilt: $fullText", fullText.contains("guilt"))
            }
        }
    }

    @Test
    fun testPersonalizedNameFormatting() {
        val diwaliGreeting = FestivalType.DIWALI.greetings[0]

        // 1. With user name
        val (personalizedTitle, personalizedMsg) = diwaliGreeting.format("Aarav")
        assertEquals("Happy Diwali, Aarav!", personalizedTitle)
        assertFalse(personalizedTitle.contains("{name}"))

        // 2. Without user name (null or blank)
        val (genericTitle, _) = diwaliGreeting.format(null)
        assertEquals("Happy Diwali!", genericTitle)
        assertFalse(genericTitle.contains(","))
        assertFalse(genericTitle.contains("{name}"))

        val (blankTitle, _) = diwaliGreeting.format("   ")
        assertEquals("Happy Diwali!", blankTitle)
        assertFalse(blankTitle.contains(","))
    }

    @Test
    fun testAllFestivalsHaveDedicatedNonEmptyGreetings() {
        for (festival in FestivalType.values()) {
            assertTrue("Festival ${festival.name} must have greetings", festival.greetings.isNotEmpty())
            assertTrue("Festival ${festival.name} must have at least 2 variations", festival.greetings.size >= 2)
            for (greeting in festival.greetings) {
                assertTrue("Title template must not be blank for ${festival.name}", greeting.titleTemplate.isNotBlank())
                assertTrue("Message template must not be blank for ${festival.name}", greeting.messageTemplate.isNotBlank())
            }
        }
    }

    @Test
    fun testGreetingsNeverCrossContaminateBetweenFestivals() {
        // Diwali greetings must never mention Eid or Holi
        for (g in FestivalType.DIWALI.greetings) {
            val lower = (g.titleTemplate + " " + g.messageTemplate).lowercase()
            assertTrue("Diwali greeting should mention Diwali or Deepavali", lower.contains("diwali") || lower.contains("deepavali"))
            assertFalse(lower.contains("eid"))
            assertFalse(lower.contains("holi"))
            assertFalse(lower.contains("christmas"))
        }

        // Holi greetings must never mention Diwali or Eid
        for (g in FestivalType.HOLI.greetings) {
            val lower = (g.titleTemplate + " " + g.messageTemplate).lowercase()
            assertTrue("Holi greeting should mention Holi", lower.contains("holi"))
            assertFalse(lower.contains("diwali"))
            assertFalse(lower.contains("eid"))
            assertFalse(lower.contains("christmas"))
        }

        // Eid-ul-Fitr greetings must never mention Diwali or Holi
        for (g in FestivalType.EID_UL_FITR.greetings) {
            val lower = (g.titleTemplate + " " + g.messageTemplate).lowercase()
            assertTrue("Eid greeting should mention Eid", lower.contains("eid"))
            assertFalse(lower.contains("diwali"))
            assertFalse(lower.contains("holi"))
            assertFalse(lower.contains("christmas"))
        }

        // Christmas greetings must never mention Diwali or Holi
        for (g in FestivalType.CHRISTMAS.greetings) {
            val lower = (g.titleTemplate + " " + g.messageTemplate).lowercase()
            assertTrue("Christmas greeting should mention Christmas", lower.contains("christmas"))
            assertFalse(lower.contains("diwali"))
            assertFalse(lower.contains("eid"))
            assertFalse("\\bholi\\b".toRegex().containsMatchIn(lower))
        }
    }

    @Test
    fun testFixedDatesAcrossMultipleYears() {
        val testYears = listOf(2025, 2026, 2028, 2030, 2035)
        for (y in testYears) {
            assertEquals("Jan 1 should be New Year", FestivalType.NEW_YEAR, FestivalManager.getFestivalForDate("$y-01-01"))
            assertEquals("Jan 26 should be Republic Day", FestivalType.REPUBLIC_DAY, FestivalManager.getFestivalForDate("$y-01-26"))
            assertEquals("Aug 15 should be Independence Day", FestivalType.INDEPENDENCE_DAY, FestivalManager.getFestivalForDate("$y-08-15"))
            assertEquals("Dec 25 should be Christmas", FestivalType.CHRISTMAS, FestivalManager.getFestivalForDate("$y-12-25"))
        }
    }

    @Test
    fun testMovableDatesAccuratelyResolved() {
        // Diwali
        assertEquals(FestivalType.DIWALI, FestivalManager.getFestivalForDate("2025-10-20"))
        assertEquals(FestivalType.DIWALI, FestivalManager.getFestivalForDate("2026-11-08"))
        assertEquals(FestivalType.DIWALI, FestivalManager.getFestivalForDate("2027-10-29"))

        // Holi
        assertEquals(FestivalType.HOLI, FestivalManager.getFestivalForDate("2025-03-14"))
        assertEquals(FestivalType.HOLI, FestivalManager.getFestivalForDate("2026-03-04"))
        assertEquals(FestivalType.HOLI, FestivalManager.getFestivalForDate("2027-03-23"))

        // Eid-ul-Fitr
        assertEquals(FestivalType.EID_UL_FITR, FestivalManager.getFestivalForDate("2025-03-31"))
        assertEquals(FestivalType.EID_UL_FITR, FestivalManager.getFestivalForDate("2026-03-20"))

        // Eid-ul-Adha
        assertEquals(FestivalType.EID_UL_ADHA, FestivalManager.getFestivalForDate("2025-06-07"))
        assertEquals(FestivalType.EID_UL_ADHA, FestivalManager.getFestivalForDate("2026-05-27"))

        // Dussehra
        assertEquals(FestivalType.DUSSEHRA, FestivalManager.getFestivalForDate("2025-10-02"))
        assertEquals(FestivalType.DUSSEHRA, FestivalManager.getFestivalForDate("2026-10-20"))
    }

    @Test
    fun testNonFestivalDayReturnsNull() {
        assertNull(FestivalManager.getFestivalForDate("2025-07-15"))
        assertNull(FestivalManager.getFestivalForDate("2026-04-18"))
        assertNull(FestivalManager.getFestivalForDate("2027-09-02"))
    }

    @Test
    fun testVariationSelection() {
        val f = FestivalType.DIWALI
        val v0 = f.getGreeting(variationIndex = 0)
        val v1 = f.getGreeting(variationIndex = 1)
        val v2 = f.getGreeting(variationIndex = 2)

        assertNotEquals(v0.titleTemplate, v1.titleTemplate)
        assertEquals(f.greetings[0], v0)
        assertEquals(f.greetings[1], v1)
        assertEquals(f.greetings[2], v2)
    }
}

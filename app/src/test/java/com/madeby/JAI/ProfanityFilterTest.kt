package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfanityFilterTest {

    @Test
    fun testCleanNamesPass() {
        val result = ProfanityFilter.checkName("Aarav Sharma")
        assertTrue(result.isClean)
        assertEquals("Aarav Sharma", result.sanitizedText)
    }

    @Test
    fun testCleanSubjectNamesPass() {
        val result = ProfanityFilter.checkSubjectName("Organic Chemistry")
        assertTrue(result.isClean)
        assertEquals("Organic Chemistry", result.sanitizedText)
    }

    @Test
    fun testEmptyOrBlankNameFails() {
        assertFalse(ProfanityFilter.checkName("").isClean)
        assertFalse(ProfanityFilter.checkName("   ").isClean)
        assertFalse(ProfanityFilter.checkName(null).isClean)
    }

    @Test
    fun testShortNameFails() {
        assertFalse(ProfanityFilter.checkName("A").isClean)
    }

    @Test
    fun testTooLongNameFails() {
        val longName = "A".repeat(31)
        assertFalse(ProfanityFilter.checkName(longName).isClean)
    }

    @Test
    fun testEnglishProfanityDetected() {
        assertTrue(ProfanityFilter.hasProfanity("fuck"))
        assertTrue(ProfanityFilter.hasProfanity("bitch"))
        assertTrue(ProfanityFilter.hasProfanity("asshole"))
        assertFalse(ProfanityFilter.checkName("John asshole").isClean)
    }

    @Test
    fun testHindiProfanityDetected() {
        assertTrue(ProfanityFilter.hasProfanity("गांड"))
        assertTrue(ProfanityFilter.hasProfanity("चूतिया"))
        assertTrue(ProfanityFilter.hasProfanity("बहनचोद"))
    }

    @Test
    fun testHinglishProfanityDetected() {
        assertTrue(ProfanityFilter.hasProfanity("chutiya"))
        assertTrue(ProfanityFilter.hasProfanity("bhosdike"))
        assertTrue(ProfanityFilter.hasProfanity("madarchod"))
    }

    @Test
    fun testLeetspeakProfanityDetected() {
        assertTrue(ProfanityFilter.hasProfanity("b1tch"))
        assertTrue(ProfanityFilter.hasProfanity("f.u.c.k"))
    }
}

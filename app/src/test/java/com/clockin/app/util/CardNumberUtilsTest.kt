package com.clockin.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardNumberUtilsTest {
    @Test
    fun normalizesBadgePrefixWithoutLosingLeadingZeros() {
        assertEquals("00123", CardNumberUtils.normalize("  bAdGe  00123  "))
    }

    @Test
    fun acceptsNumericBadgesWithOrWithoutPrefix() {
        assertTrue(CardNumberUtils.isValidCardNumber("Badge 00123"))
        assertTrue(CardNumberUtils.isValidCardNumber("00123"))
    }

    @Test
    fun rejectsLettersPunctuationAndEmbeddedSpaces() {
        listOf("Badge AB123", "12-34", "12 34").forEach {
            assertFalse(it, CardNumberUtils.isValidCardNumber(it))
        }
    }

    @Test
    fun formatsPayloadSoItCanBeReadBack() {
        val payload = CardNumberUtils.nfcPayload("00123")
        assertEquals("Badge 00123", payload)
        assertEquals("00123", CardNumberUtils.normalize(payload))
    }

    @Test
    fun duplicateLookupIncludesLegacyAndNormalizedBadges() {
        assertEquals(listOf("00123", "Badge 00123"), CardNumberUtils.duplicateCandidates("00123"))
    }
}

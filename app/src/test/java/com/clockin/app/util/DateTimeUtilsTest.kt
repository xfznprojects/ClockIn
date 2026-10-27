package com.clockin.app.util

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {
    @Test
    fun acceptsLeapDayInBothSupportedFormats() {
        assertEquals("2024-02-29", DateTimeUtils.parseDateInputToStorage("29/02/2024"))
        assertEquals("2024-02-29", DateTimeUtils.parseDateInputToStorage("2024-02-29"))
    }

    @Test
    fun rejectsImpossibleDatesAndTrailingContent() {
        listOf("29/02/2025", "31/04/2026", "27/10/26", "2026-10-27junk", "").forEach {
            assertNull(it, DateTimeUtils.parseDateInputToStorage(it))
        }
    }

    @Test
    fun backfilledTimestampRepresentsEnteredTime() {
        val expected = LocalDateTime.of(2024, 2, 29, 7, 30, 15)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, DateTimeUtils.parseDateTimeToTimestamp("2024-02-29", "07:30:15"))
    }

    @Test
    fun backfilledRecordsSortBeforeLaterScans() {
        val earlier = requireNotNull(DateTimeUtils.parseDateTimeToTimestamp("2026-10-26", "23:59:59"))
        val later = requireNotNull(DateTimeUtils.parseDateTimeToTimestamp("2026-10-27", "00:00:00"))
        assertTrue(earlier < later)
    }

    @Test
    fun rejectsMalformedScanTimes() {
        listOf("25:00:00", "07:60:00", "07:30:15junk", "").forEach {
            assertNull(it, DateTimeUtils.parseDateTimeToTimestamp("2026-10-27", it))
        }
    }
}

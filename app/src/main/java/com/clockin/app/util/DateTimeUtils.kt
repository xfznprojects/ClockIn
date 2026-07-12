package com.clockin.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

object DateTimeUtils {
    // Storage formats are locale-independent ASCII: yyyy-MM-dd and HH:mm:ss.
    private val storageDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val storageTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
    // Strict input parser: requires exactly dd/MM/yyyy with a 4-digit year and
    // rejects trailing garbage.
    private val inputDateParser: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT)

    fun getCurrentDate(): String = LocalDate.now().format(storageDateFormatter)
    fun getCurrentTime(): String = LocalTime.now().format(storageTimeFormatter)
    fun getCurrentDisplayDate(): String = formatDisplayDate(getCurrentDate())

    fun formatDisplayDate(date: String): String {
        return try {
            LocalDate.parse(date, storageDateFormatter).format(displayDateFormatter)
        } catch (_: Exception) {
            date
        }
    }

    fun parseDateInputToStorage(date: String): String? {
        val trimmed = date.trim()
        return when {
            trimmed.isBlank() -> null
            trimmed.contains("/") -> parseWith(inputDateParser, trimmed)
            else -> parseWith(storageDateFormatter, trimmed)
        }
    }

    fun parseDateTimeToTimestamp(scanDate: String, scanTime: String): Long? {
        return try {
            val date = LocalDate.parse(scanDate, storageDateFormatter)
            val time = LocalTime.parse(scanTime.trim(), storageTimeFormatter)
            LocalDateTime.of(date, time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWith(formatter: DateTimeFormatter, date: String): String? {
        return try {
            LocalDate.parse(date, formatter).format(storageDateFormatter)
        } catch (_: Exception) {
            null
        }
    }
}

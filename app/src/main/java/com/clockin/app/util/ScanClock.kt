package com.clockin.app.util

import android.content.Context
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScanClock(context: Context) {

    private val prefs = context.getSharedPreferences("scan_clock_guard", Context.MODE_PRIVATE)
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun getScanTime(): ScanTimeResult {
        val systemWallTime = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()

        if (isValidScanTime(systemWallTime)) {
            rememberGoodClock(systemWallTime, elapsedRealtime)
            return ScanTimeResult.Valid(toScanTime(systemWallTime), usedEstimatedTime = false)
        }

        val manualDate = getValidManualDate(elapsedRealtime)
        if (manualDate != null) {
            return ScanTimeResult.Valid(toManualScanTime(manualDate, systemWallTime), usedEstimatedTime = true)
        }

        val estimatedWallTime = estimateWallTime(elapsedRealtime)
        if (estimatedWallTime != null && isValidScanTime(estimatedWallTime)) {
            rememberGoodClock(estimatedWallTime, elapsedRealtime)
            return ScanTimeResult.Valid(toScanTime(estimatedWallTime), usedEstimatedTime = true)
        }

        return ScanTimeResult.Invalid(
            "The phone's date/time looks wrong. Enable automatic date & time before scanning."
        )
    }

    fun needsManualDate(): Boolean {
        val systemWallTime = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()
        if (isValidScanTime(systemWallTime)) return false
        if (getValidManualDate(elapsedRealtime) != null) return false

        val estimatedWallTime = estimateWallTime(elapsedRealtime)
        return estimatedWallTime == null || !isValidScanTime(estimatedWallTime)
    }

    fun setManualDate(scanDate: String) {
        prefs.edit()
            .putString(KEY_MANUAL_SCAN_DATE, scanDate)
            .putLong(KEY_MANUAL_SET_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
            .apply()
    }

    fun getManualDisplayDate(): String? {
        val scanDate = getValidManualDate(SystemClock.elapsedRealtime()) ?: return null
        return DateTimeUtils.formatDisplayDate(scanDate)
    }

    private fun estimateWallTime(elapsedRealtime: Long): Long? {
        val lastWallTime = prefs.getLong(KEY_LAST_WALL_TIME, 0L)
        val lastElapsedRealtime = prefs.getLong(KEY_LAST_ELAPSED_REALTIME, 0L)
        if (lastWallTime <= 0L || lastElapsedRealtime <= 0L) return null

        if (elapsedRealtime < lastElapsedRealtime) {
            // elapsedRealtime restarted (device rebooted); the stored reference is
            // from another boot session, so deltas against it are meaningless.
            clearClockReference()
            return null
        }

        val elapsedDelta = elapsedRealtime - lastElapsedRealtime
        if (elapsedDelta > MAX_ESTIMATE_DELTA_MS) return null

        return lastWallTime + elapsedDelta
    }

    private fun clearClockReference() {
        prefs.edit()
            .remove(KEY_LAST_WALL_TIME)
            .remove(KEY_LAST_ELAPSED_REALTIME)
            .apply()
    }

    private fun rememberGoodClock(wallTime: Long, elapsedRealtime: Long) {
        prefs.edit()
            .putLong(KEY_LAST_WALL_TIME, wallTime)
            .putLong(KEY_LAST_ELAPSED_REALTIME, elapsedRealtime)
            .apply()
    }

    private fun isValidScanTime(wallTime: Long): Boolean {
        return wallTime >= MIN_VALID_SCAN_TIME_MILLIS
    }

    private fun toScanTime(wallTime: Long): ScanTime {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(wallTime), zoneId)
        return ScanTime(
            scanDate = dateTime.toLocalDate().format(DATE_FORMATTER),
            scanTime = dateTime.toLocalTime().format(TIME_FORMATTER),
            timestamp = wallTime
        )
    }

    private fun toManualScanTime(scanDate: String, systemWallTime: Long): ScanTime {
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(systemWallTime), zoneId)
            .toLocalTime()
            .withNano(0)
        val date = LocalDate.parse(scanDate, DATE_FORMATTER)
        val dateTime = LocalDateTime.of(date, time)
        return ScanTime(
            scanDate = scanDate,
            scanTime = time.format(TIME_FORMATTER),
            timestamp = dateTime.atZone(zoneId).toInstant().toEpochMilli()
        )
    }

    private fun getValidManualDate(elapsedRealtime: Long): String? {
        val scanDate = prefs.getString(KEY_MANUAL_SCAN_DATE, null) ?: return null
        val setElapsedRealtime = prefs.getLong(KEY_MANUAL_SET_ELAPSED_REALTIME, 0L)
        if (setElapsedRealtime <= 0L) return null
        val age = elapsedRealtime - setElapsedRealtime
        if (age < 0L || age > MANUAL_DATE_VALID_MS) return null

        return try {
            // Advance by the number of whole elapsed days so the date rolls over
            // at day boundaries instead of staying frozen for the whole window.
            LocalDate.parse(scanDate, DATE_FORMATTER)
                .plusDays(age / MILLIS_PER_DAY)
                .format(DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_LAST_WALL_TIME = "last_wall_time"
        private const val KEY_LAST_ELAPSED_REALTIME = "last_elapsed_realtime"
        private const val KEY_MANUAL_SCAN_DATE = "manual_scan_date"
        private const val KEY_MANUAL_SET_ELAPSED_REALTIME = "manual_set_elapsed_realtime"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        private const val MILLIS_PER_DAY: Long = 24L * 60L * 60L * 1000L
        private const val MANUAL_DATE_VALID_MS: Long = MILLIS_PER_DAY
        private const val MAX_ESTIMATE_DELTA_MS: Long = 7L * MILLIS_PER_DAY
        private val MIN_VALID_SCAN_TIME_MILLIS: Long = LocalDate.of(2026, 1, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

data class ScanTime(
    val scanDate: String,
    val scanTime: String,
    val timestamp: Long
)

sealed class ScanTimeResult {
    data class Valid(
        val scanTime: ScanTime,
        val usedEstimatedTime: Boolean
    ) : ScanTimeResult()

    data class Invalid(
        val message: String
    ) : ScanTimeResult()
}

package com.clockin.app.util

object CardNumberUtils {
    private val badgePrefix = Regex("^\\s*Badge\\s+", RegexOption.IGNORE_CASE)

    fun normalize(rawValue: String): String {
        return badgePrefix.replace(rawValue.trim(), "").trim()
    }

    fun display(rawValue: String): String {
        val normalized = normalize(rawValue)
        return if (normalized.isBlank()) "Badge ?" else "Badge $normalized"
    }

    fun nfcPayload(rawValue: String): String {
        return display(rawValue)
    }

    fun isValidCardNumber(rawValue: String): Boolean {
        return normalize(rawValue).all { it.isDigit() }
    }

    fun duplicateCandidates(rawValue: String): List<String> {
        val trimmed = rawValue.trim()
        val normalized = normalize(rawValue)
        return linkedSetOf(
            trimmed,
            normalized,
            nfcPayload(normalized)
        ).filter { it.isNotBlank() }
    }
}

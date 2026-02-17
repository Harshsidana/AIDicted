package com.ai.aidicted.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Converts an ISO 8601 timestamp to relative time string like "2h ago", "3 days ago".
 */
fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}

/**
 * Formats an ISO 8601 timestamp to a readable date string.
 */
fun formatDate(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        isoTimestamp
    }
}

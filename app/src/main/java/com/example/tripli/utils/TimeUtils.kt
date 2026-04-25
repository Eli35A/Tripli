package com.example.tripli.utils

object TimeUtils {
    fun timeAgo(timestampMillis: Long): String {
        if (timestampMillis == 0L) return "Just now"
        val diff = System.currentTimeMillis() - timestampMillis
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000}h ago"
            diff < 604_800_000L -> "${diff / 86_400_000}d ago"
            else -> "${diff / 604_800_000}w ago"
        }
    }
}

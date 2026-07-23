package com.feige.snippetstudio.util

import android.content.Context
import com.feige.snippetstudio.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    fun formatRelativeTime(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp

        if (diffMs < 60_000L) {
            return context.getString(R.string.time_just_now)
        }

        val diffMin = (diffMs / 60_000L).toInt()
        if (diffMin < 60) {
            return context.getString(R.string.time_min, diffMin)
        }

        val diffHours = (diffMs / (3600_000L)).toInt()
        if (diffHours < 24) {
            return context.getString(R.string.time_hour, diffHours)
        }

        if (diffHours < 48) {
            return context.getString(R.string.time_yesterday)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

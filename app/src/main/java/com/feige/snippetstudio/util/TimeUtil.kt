package com.feige.snippetstudio.util

import android.content.Context
import com.feige.snippetstudio.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [TimeUtil] 时间格式化工具。
 */
object TimeUtil {
    /**
     * 将时间戳格式化为相对可读时间（如“刚刚”、“X 分钟前”、“X 小时前”、“昨天”、“yyyy-MM-dd HH:mm”）。
     *
     * @param context 上下文（用于提取多语言 string 资源）
     * @param timestamp 目标时间戳 (毫秒)
     */
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

    /**
     * 将时间戳格式化为完整的标准年月日时分秒格式 ("yyyy-MM-dd HH:mm:ss")。
     */
    fun formatFullDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}


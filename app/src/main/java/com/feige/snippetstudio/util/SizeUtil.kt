package com.feige.snippetstudio.util

import java.util.Locale

/**
 * [SizeUtil] 文件与文本字节大小格式化工具。
 */
object SizeUtil {
    /**
     * 将字节数 (Bytes) 自动转换为易读的 B, KB, MB 单位字符串。
     *
     * @param bytes 字节数
     * @return 格式化后的字符串 (如 "512 B", "4.2 KB", "1.5 MB")
     */
    fun formatBytes(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}


package com.feige.snippetstudio.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * [SearchHighlighter] 搜索结果关键词高亮工具函数集。
 *
 * 架构职责：
 * 将用户搜索关键词在目标文本中匹配的部分以高亮样式（加粗 + 主题色）标注，
 * 提升搜索结果列表的视觉可辨识性与可用性。
 *
 * 使用示例：
 * ```kotlin
 * val highlighted = SearchHighlighter.highlight(
 *     text = "JavaScript 高级教程",
 *     query = "高级",
 *     highlightColor = tc.primary
 * )
 * Text(text = highlighted)
 * ```
 */
object SearchHighlighter {

    /**
     * 对文本中匹配搜索关键词的部分应用高亮样式。
     * 支持忽略大小写匹配，匹配所有出现位置。
     *
     * @param text 原始完整文本
     * @param query 搜索关键词（为空时直接返回原文）
     * @param highlightColor 高亮文字颜色（通常使用主题 primary 色）
     * @param baseColor 非高亮部分的文字颜色（可选，默认不设置即继承）
     * @return 包含高亮 Span 的 [AnnotatedString]
     */
    fun highlight(
        text: String,
        query: String,
        highlightColor: Color,
        baseColor: Color? = null
    ): AnnotatedString {
        // 关键词为空时直接返回纯文本
        if (query.isBlank()) return AnnotatedString(text)

        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.trim().lowercase()
            var cursor = 0

            while (cursor < text.length) {
                val matchIndex = lowerText.indexOf(lowerQuery, cursor)
                if (matchIndex == -1) {
                    // 无更多匹配，追加剩余文本
                    appendRemaining(text, cursor, baseColor)
                    break
                }

                // 追加匹配前的普通文本
                if (matchIndex > cursor) {
                    appendRemaining(text.substring(cursor, matchIndex), 0, baseColor)
                }

                // 追加高亮匹配段
                withStyle(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(matchIndex, matchIndex + lowerQuery.length))
                }

                cursor = matchIndex + lowerQuery.length
            }
        }
    }

    /**
     * 内部辅助：追加非高亮文本段（可选设置基础颜色）。
     */
    private fun AnnotatedString.Builder.appendRemaining(
        text: String,
        startIndex: Int,
        baseColor: Color?
    ) {
        val segment = if (startIndex == 0) text else text.substring(startIndex)
        if (baseColor != null) {
            withStyle(SpanStyle(color = baseColor)) {
                append(segment)
            }
        } else {
            append(segment)
        }
    }
}

package com.feige.snippetstudio.util

/**
 * [MarkdownRenderer] 是一个轻量级 Markdown 语法向 HTML 网页渲染转换器。
 *
 * 用于在 WebView 或简易 HTML 容器中渲染展示 Markdown 代码片段。
 * 支持内嵌 CSS 样式（兼容浅色与深色深色模式 prefers-color-scheme）、标题 (#, ##, ###)、代码块 (```)、列表 (- ) 及行内样式 (**粗体**, *斜体*, `代码`, [链接](url))。
 */
object MarkdownRenderer {
    /**
     * 将输入的原始 Markdown 文本解析转换为完整的带有样式表的 HTML 文档字符串。
     *
     * @param markdown 原始 Markdown 字符串
     * @return 可用 WebView 加载渲染的 HTML 网页文本
     */
    fun toHtml(markdown: String): String {
        val lines = markdown.lines()
        val htmlBuilder = StringBuilder()
        var inCodeBlock = false

        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 16px; color: #16181F; line-height: 1.6; background: #FFFFFF; }
                    @media (prefers-color-scheme: dark) {
                        body { background: #181B22; color: #EDEFF4; }
                        pre, code { background: #20242C !important; color: #8B7FF2 !important; }
                        blockquote { border-left-color: #5B4FE0 !important; color: #A7ADBA !important; }
                    }
                    h1 { font-size: 22px; border-bottom: 1px solid #E7E8F0; padding-bottom: 8px; margin-top: 16px; }
                    h2 { font-size: 18px; margin-top: 14px; }
                    h3 { font-size: 16px; margin-top: 12px; }
                    code { background: #EFEDFF; color: #5B4FE0; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.9em; }
                    pre { background: #F5F6FA; padding: 12px; border-radius: 8px; overflow-x: auto; font-family: monospace; }
                    pre code { background: transparent; padding: 0; color: inherit; }
                    ul { padding-left: 20px; }
                    a { color: #5B4FE0; text-decoration: none; }
                    p { margin: 8px 0; }
                </style>
            </head>
            <body>
        """.trimIndent())

        for (line in lines) {
            val trimmed = line.trim()

            // 匹配多行代码块分隔线 (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    htmlBuilder.append("</code></pre>\n")
                    inCodeBlock = false
                } else {
                    htmlBuilder.append("<pre><code>")
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                htmlBuilder.append(escapeHtml(line)).append("\n")
                continue
            }

            if (trimmed.isEmpty()) {
                htmlBuilder.append("<br/>")
                continue
            }

            val escapedLine = escapeHtml(line)
            val inlineParsed = parseInline(escapedLine)

            when {
                trimmed.startsWith("### ") -> htmlBuilder.append("<h3>").append(parseInline(escapeHtml(line.substring(4)))).append("</h3>")
                trimmed.startsWith("## ") -> htmlBuilder.append("<h2>").append(parseInline(escapeHtml(line.substring(3)))).append("</h2>")
                trimmed.startsWith("# ") -> htmlBuilder.append("<h1>").append(parseInline(escapeHtml(line.substring(2)))).append("</h1>")
                trimmed.startsWith("- ") -> htmlBuilder.append("<ul><li>").append(parseInline(escapeHtml(line.substring(2)))).append("</li></ul>")
                else -> htmlBuilder.append("<p>").append(inlineParsed).append("</p>")
            }
        }

        if (inCodeBlock) {
            htmlBuilder.append("</code></pre>\n")
        }

        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }

    /** 转义 HTML 特殊敏感字符，防御 XSS 风险 */
    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /** 使用正则解析 Markdown 行内富文本标记 (粗体/斜体/行内代码/超链接) */
    private fun parseInline(text: String): String {
        var result = text
        // **bold** 粗体
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        // *italic* 斜体
        result = result.replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        // `code` 行内代码
        result = result.replace(Regex("`(.*?)`"), "<code>$1</code>")
        // [text](url) 链接
        result = result.replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href=\"$2\">$1</a>")
        return result
    }
}


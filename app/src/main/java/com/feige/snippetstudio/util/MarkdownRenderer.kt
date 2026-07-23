package com.feige.snippetstudio.util

object MarkdownRenderer {
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

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun parseInline(text: String): String {
        var result = text
        // **bold**
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        // *italic*
        result = result.replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        // `code`
        result = result.replace(Regex("`(.*?)`"), "<code>$1</code>")
        // [text](url)
        result = result.replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href=\"$2\">$1</a>")
        return result
    }
}

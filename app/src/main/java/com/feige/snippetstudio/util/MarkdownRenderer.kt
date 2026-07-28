package com.feige.snippetstudio.util

/**
 * [MarkdownRenderer] 是一个轻量级 Markdown 语法向 HTML 网页渲染转换器。
 *
 * 用于在 WebView 或简易 HTML 容器中渲染展示 Markdown 代码片段。
 * 支持内嵌 CSS 样式（兼容浅色与深色模式 prefers-color-scheme）、标题 (#, ##, ###)、多语言代码块 (```java, ```js 等全语法色彩高亮)、列表 (- ) 及行内样式 (**粗体**, *斜体*, `代码`, [链接](url))。
 */
object MarkdownRenderer {

    private data class TokenSpan(val start: Int, val end: Int, val cssClass: String)

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
        var currentCodeLang = ""
        val codeBlockLines = mutableListOf<String>()

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
                        pre { background: #20242C !important; border-color: #2D323E !important; }
                        code { background: #20242C !important; color: #8B7FF2 !important; }
                        blockquote { border-left-color: #5B4FE0 !important; color: #A7ADBA !important; }
                    }
                    h1 { font-size: 22px; border-bottom: 1px solid #E7E8F0; padding-bottom: 8px; margin-top: 16px; }
                    h2 { font-size: 18px; margin-top: 14px; }
                    h3 { font-size: 16px; margin-top: 12px; }
                    code { background: #EFEDFF; color: #5B4FE0; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.9em; }
                    pre { background: #F5F6FA; padding: 12px; border-radius: 8px; overflow-x: auto; font-family: 'JetBrains Mono', Consolas, monospace; font-size: 13px; line-height: 1.5; position: relative; border: 1px solid #E7E8F0; }
                    .code-lang-badge { display: inline-block; font-size: 11px; font-weight: bold; text-transform: uppercase; color: #5B4FE0; background: rgba(91,79,224,0.1); padding: 2px 8px; border-radius: 4px; margin-bottom: 8px; }
                    pre code { background: transparent !important; padding: 0; color: inherit; display: block; white-space: pre; }
                    ul { padding-left: 20px; }
                    a { color: #5B4FE0; text-decoration: none; }
                    p { margin: 8px 0; }

                    /* 语法高亮分词配色 */
                    .hl-kw { color: #6A1B9A; font-weight: bold; }
                    .hl-str { color: #2E7D32; }
                    .hl-num { color: #D84315; }
                    .hl-cmt { color: #78909C; font-style: italic; }
                    .hl-fn { color: #1565C0; }
                    .hl-anno { color: #F57F17; }

                    @media (prefers-color-scheme: dark) {
                        .hl-kw { color: #C792EA !important; font-weight: bold; }
                        .hl-str { color: #C3E88D !important; }
                        .hl-num { color: #F78C6C !important; }
                        .hl-cmt { color: #607D8B !important; font-style: italic; }
                        .hl-fn { color: #82AAFF !important; }
                        .hl-anno { color: #FFCB6B !important; }
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        for (line in lines) {
            val trimmed = line.trim()

            // 匹配多行围栏代码块分隔线 (```lang)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val rawCode = codeBlockLines.joinToString("\n")
                    val highlightedHtml = highlightCodeToHtml(rawCode, currentCodeLang)
                    if (currentCodeLang.isNotEmpty()) {
                        htmlBuilder.append("<pre><span class=\"code-lang-badge\">")
                            .append(escapeHtml(currentCodeLang))
                            .append("</span><code>")
                            .append(highlightedHtml)
                            .append("</code></pre>\n")
                    } else {
                        htmlBuilder.append("<pre><code>")
                            .append(highlightedHtml)
                            .append("</code></pre>\n")
                    }
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    currentCodeLang = trimmed.substring(3).trim()
                    codeBlockLines.clear()
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
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
            val rawCode = codeBlockLines.joinToString("\n")
            val highlightedHtml = highlightCodeToHtml(rawCode, currentCodeLang)
            htmlBuilder.append("<pre><code>").append(highlightedHtml).append("</code></pre>\n")
        }

        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }

    /**
     * 将代码块根据语言 tag 生成包含语法高亮 HTML 标签的富字符串。
     *
     * @param code 原始代码块文本
     * @param lang 语言标识 (如 "java", "js", "python", "cpp" 等)
     * @return 包含 HTML 高亮 <span> 节点的格式化字符串
     */
    private fun highlightCodeToHtml(code: String, lang: String): String {
        if (code.isBlank()) return ""
        val cleanLang = lang.trim().lowercase()

        val spans = mutableListOf<TokenSpan>()

        // 依据语言类型进行正则表达式词法解析
        val isJava = cleanLang == "java"
        val isJs = cleanLang in listOf("js", "javascript", "ts", "typescript")
        val isPy = cleanLang in listOf("py", "python")
        val isCpp = cleanLang in listOf("c", "cpp", "c++", "h", "hpp")
        val isGo = cleanLang in listOf("go", "golang")
        val isRust = cleanLang in listOf("rs", "rust")
        val isShell = cleanLang in listOf("sh", "bash", "shell", "zsh")
        val isJson = cleanLang == "json"

        // 1. 注释正则 (高优先级)
        val cmtRegex = when {
            isPy || isShell -> Regex("#[^\n]*")
            else -> Regex("//[^\n]*|/\\*[\\s\\S]*?\\*/")
        }
        for (m in cmtRegex.findAll(code)) {
            spans.add(TokenSpan(m.range.first, m.range.last + 1, "hl-cmt"))
        }

        // 2. 字符串正则
        val strRegex = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'")
        for (m in strRegex.findAll(code)) {
            spans.add(TokenSpan(m.range.first, m.range.last + 1, "hl-str"))
        }

        // 3. 注解/装饰器正则 (@Override, @param, @return 等)
        if (isJava || isPy || isRust) {
            val annoRegex = Regex("@[a-zA-Z0-9_]+")
            for (m in annoRegex.findAll(code)) {
                spans.add(TokenSpan(m.range.first, m.range.last + 1, "hl-anno"))
            }
        }

        // 4. 关键字正则
        val kwPattern = when {
            isJava -> "\\b(package|import|public|protected|private|class|interface|enum|extends|implements|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|throws|new|this|super|instanceof|abstract|final|static|synchronized|volatile|transient|native|void|boolean|byte|char|short|int|long|float|double|var|record)\\b"
            isJs -> "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|new|this|async|await|import|export|from|of|in|typeof|instanceof|class|extends|super)\\b"
            isPy -> "\\b(def|class|return|if|elif|else|for|while|try|except|finally|raise|import|from|as|with|lambda|yield|pass|break|continue|global|nonlocal|assert|is|in|not|and|or)\\b"
            isCpp -> "\\b(include|define|using|namespace|class|struct|public|protected|private|template|typename|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|throw|new|delete|const|constexpr|auto|void|int|char|double|float|bool|long|short)\\b"
            isGo -> "\\b(package|import|func|type|struct|interface|return|if|else|for|range|switch|case|default|break|continue|select|go|defer|chan|var|const|map|nil|true|false)\\b"
            isRust -> "\\b(fn|let|mut|pub|use|mod|struct|enum|trait|impl|return|if|else|match|for|while|loop|break|continue|async|await|move|where|as|type|const|static|true|false)\\b"
            isShell -> "\\b(if|then|else|elif|fi|for|while|do|done|case|esac|function|return|exit|echo|export|local|set|unset|in)\\b"
            isJson -> "\\b(true|false|null)\\b"
            else -> "\\b(package|import|public|private|protected|class|def|fn|func|function|return|if|else|for|while|new|var|let|const|val|void|int|string|boolean|true|false|null)\\b"
        }
        val kwRegex = Regex(kwPattern)
        for (m in kwRegex.findAll(code)) {
            spans.add(TokenSpan(m.range.first, m.range.last + 1, "hl-kw"))
        }

        // 5. 数字正则
        val numRegex = Regex("\\b\\d+(\\.\\d+)?([fFLlDdBb])?\\b")
        for (m in numRegex.findAll(code)) {
            spans.add(TokenSpan(m.range.first, m.range.last + 1, "hl-num"))
        }

        // 6. 方法/函数调用正则
        val fnRegex = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()")
        for (m in fnRegex.findAll(code)) {
            val fnGroup = m.groups[1]
            if (fnGroup != null) {
                spans.add(TokenSpan(fnGroup.range.first, fnGroup.range.last + 1, "hl-fn"))
            }
        }

        // 升序排序并过滤重叠区间（无缝切分 Token）
        spans.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))
        val finalSpans = mutableListOf<TokenSpan>()
        var lastEnd = 0
        for (span in spans) {
            if (span.start >= lastEnd) {
                finalSpans.add(span)
                lastEnd = span.end
            }
        }

        // 拼接 HTML 输出
        val sb = StringBuilder()
        var curr = 0
        for (span in finalSpans) {
            if (span.start > curr) {
                sb.append(escapeHtml(code.substring(curr, span.start)))
            }
            sb.append("<span class=\"").append(span.cssClass).append("\">")
                .append(escapeHtml(code.substring(span.start, span.end)))
                .append("</span>")
            curr = span.end
        }
        if (curr < code.length) {
            sb.append(escapeHtml(code.substring(curr)))
        }

        return sb.toString()
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



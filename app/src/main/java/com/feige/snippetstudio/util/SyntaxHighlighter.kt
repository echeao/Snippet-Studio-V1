package com.feige.snippetstudio.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.feige.snippetstudio.model.SnippetType
import java.util.regex.Pattern

/**
 * [SyntaxHighlighter] 是为 Compose UI 打造的轻量级纯文本代码语法高亮解析器。
 *
 * 核心原理：使用正则表达式匹配各种代码元素（关键字、字符串、数字、注释、标签、变量等），
 * 并利用 Jetpack Compose 的 [AnnotatedString] 与 [SpanStyle] 给匹配到的字符区间叠加富文本色彩与字重，
 * 自动适配浅色 (Light) 和深色 (Dark) 主题模式。
 *
 * 支持语言：HTML, JS, CSS, JSON, Python, Markdown, Prompt, XML, YAML, Shell
 */
object SyntaxHighlighter {

    // ===== 词法单元色彩定义 (Token Styles) =====

    /** 获取关键字样式（如 JavaScript 的 const, let, function） */
    private fun getKeywordStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFC792EA) else Color(0xFF7B1FA2),
        fontWeight = FontWeight.Bold
    )

    /** 获取字符串文本样式 */
    private fun getStringStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFC3E88D) else Color(0xFF2E7D32)
    )

    /** 获取数字文本样式 */
    private fun getNumberStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFF78C6C) else Color(0xFFE65100)
    )

    /** 获取注释文本样式（斜体灰色） */
    private fun getCommentStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF78909C) else Color(0xFF757575),
        fontStyle = FontStyle.Italic
    )

    /** 获取 HTML 标签样式 */
    private fun getTagStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B),
        fontWeight = FontWeight.SemiBold
    )

    /** 获取 HTML 属性名称样式 */
    private fun getAttrStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFFCB6B) else Color(0xFFF57F17)
    )

    /** 获取 Markdown 标题样式 */
    private fun getHeaderStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF82AAFF) else Color(0xFF1565C0),
        fontWeight = FontWeight.Bold
    )

    /** 获取 Prompt 提示词变量样式（如 {var} 或 $var） */
    private fun getVariableStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFF5370) else Color(0xFFD81B60),
        fontWeight = FontWeight.Bold
    )

    /** 获取 CSS 选择器样式 */
    private fun getSelectorStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF82AAFF) else Color(0xFF1565C0),
        fontWeight = FontWeight.SemiBold
    )

    /** 获取装饰器/注解样式 (Python @decorator) */
    private fun getDecoratorStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFFCB6B) else Color(0xFFF57F17),
        fontStyle = FontStyle.Italic
    )

    /** 获取 YAML key 样式 */
    private fun getYamlKeyStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B),
        fontWeight = FontWeight.SemiBold
    )

    /** 获取 Shell 变量样式 ($var) */
    private fun getShellVarStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFFF5370) else Color(0xFFD81B60)
    )

    // ===== 语法匹配正则表达式模式 (Regex Patterns) =====

    private val JS_KEYWORD_PATTERN = Pattern.compile(
        "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|import|export|from|default|class|extends|async|await|try|catch|finally|throw|new|this|typeof|instanceof|void|in|of|null|undefined|true|false)\\b"
    )
    private val JS_STRING_PATTERN = Pattern.compile(
        "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`"
    )
    private val NUMBER_PATTERN = Pattern.compile(
        "\\b\\d+(\\.\\d+)?\\b"
    )
    private val JS_COMMENT_PATTERN = Pattern.compile(
        "//.*|/\\*[\\s\\S]*?\\*/"
    )

    private val HTML_TAG_PATTERN = Pattern.compile(
        "</?[a-zA-Z0-9\\-]+(?:\\s+[a-zA-Z0-9\\-]+(?:=(?:\"[^\"]*\"|'[^']*'|[^>\\s]+))?)*\\s*/?>"
    )
    private val HTML_COMMENT_PATTERN = Pattern.compile(
        "<!--[\\s\\S]*?-->"
    )
    private val HTML_ATTR_NAME_PATTERN = Pattern.compile(
        "\\b[a-zA-Z0-9\\-]+(?=\\=)"
    )

    private val MD_HEADER_PATTERN = Pattern.compile(
        "(?m)^#{1,6}\\s+.*$"
    )
    private val MD_BOLD_PATTERN = Pattern.compile(
        "\\*\\*.*?\\*\\*|__.*?__"
    )
    private val MD_CODE_PATTERN = Pattern.compile(
        "`[^`]+`"
    )

    private val PROMPT_VAR_PATTERN = Pattern.compile(
        "\\{\\{?[a-zA-Z0-9_\\u4e00-\\u9fa5]+\\}?\\}|\\$[a-zA-Z0-9_]+"
    )

    // ===== 新增语言正则模式 =====

    private val JSON_KEY_PATTERN = Pattern.compile(
        "\"[^\"\\\\]*\"(?=\\s*:)"
    )
    private val JSON_BOOL_PATTERN = Pattern.compile(
        "\\b(true|false|null)\\b"
    )

    private val PYTHON_KEYWORD_PATTERN = Pattern.compile(
        "\\b(def|class|import|from|return|if|elif|else|for|while|try|except|finally|with|as|yield|lambda|pass|break|continue|and|or|not|in|is|None|True|False|raise|global|nonlocal|assert|del|print)\\b"
    )
    private val PYTHON_COMMENT_PATTERN = Pattern.compile(
        "#.*"
    )
    private val PYTHON_STRING_PATTERN = Pattern.compile(
        "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"
    )
    private val PYTHON_DECORATOR_PATTERN = Pattern.compile(
        "@[a-zA-Z_][a-zA-Z0-9_.]*"
    )

    private val CSS_SELECTOR_PATTERN = Pattern.compile(
        "(?m)^[^{}@/][^{}]*(?=\\s*\\{)"
    )
    private val CSS_PROP_PATTERN = Pattern.compile(
        "[a-zA-Z-]+(?=\\s*:)"
    )
    private val CSS_COMMENT_PATTERN = Pattern.compile(
        "/\\*[\\s\\S]*?\\*/"
    )

    private val YAML_KEY_PATTERN = Pattern.compile(
        "(?m)^\\s*[a-zA-Z0-9_.-]+(?=\\s*:)"
    )
    private val YAML_COMMENT_PATTERN = Pattern.compile(
        "#.*"
    )

    private val SHELL_KEYWORD_PATTERN = Pattern.compile(
        "\\b(if|then|else|elif|fi|for|while|do|done|case|esac|function|echo|export|source|cd|ls|grep|awk|sed|cat|mkdir|rm|cp|mv|chmod|sudo|apt|npm|git|docker)\\b"
    )
    private val SHELL_VAR_PATTERN = Pattern.compile(
        "\\$\\{?[a-zA-Z_][a-zA-Z0-9_]*\\}?|\\$\\([^)]*\\)"
    )
    private val SHELL_COMMENT_PATTERN = Pattern.compile(
        "#.*"
    )

    private val HTML_SCRIPT_BLOCK = Pattern.compile(
        "(?s)<script[^>]*>(.*?)</script>"
    )
    private val HTML_STYLE_BLOCK = Pattern.compile(
        "(?s)<style[^>]*>(.*?)</style>"
    )

    /**
     * 对给定的文本按照指定片段类型进行语法高亮分析。
     *
     * @param text 纯代码文本
     * @param type 片段类型 (JS, HTML, Markdown, Prompt)
     * @param isDark 当前是否为深色主题
     * @return 注入样式后的 [AnnotatedString] 富文本对象
     */
    fun highlight(text: String, type: SnippetType, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            append(text)
            when (type) {
                SnippetType.JS -> highlightJs(text, isDark)
                SnippetType.HTML -> highlightHtml(text, isDark)
                SnippetType.MARKDOWN -> highlightMarkdown(text, isDark)
                SnippetType.PROMPT -> highlightPrompt(text, isDark)
            }
        }
    }

    /**
     * 根据 [SyntaxLanguage] 进行语法高亮（支持 11 种语言）。
     * 对超大文本进行截断保护，仅高亮前 8000 字符。
     */
    fun highlightByLanguage(text: String, language: SyntaxLanguage, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        // 性能保护：超大文本仅高亮前 8000 字符
        val effectiveText = if (text.length > 8000) text.substring(0, 8000) else text

        return buildAnnotatedString {
            append(text)
            when (language) {
                SyntaxLanguage.HTML -> highlightHtml(effectiveText, isDark)
                SyntaxLanguage.JS -> highlightJs(effectiveText, isDark)
                SyntaxLanguage.CSS -> highlightCss(effectiveText, isDark)
                SyntaxLanguage.JSON -> highlightJson(effectiveText, isDark)
                SyntaxLanguage.PYTHON -> highlightPython(effectiveText, isDark)
                SyntaxLanguage.MARKDOWN -> highlightMarkdown(effectiveText, isDark)
                SyntaxLanguage.PROMPT -> highlightPrompt(effectiveText, isDark)
                SyntaxLanguage.XML -> highlightXml(effectiveText, isDark)
                SyntaxLanguage.YAML -> highlightYaml(effectiveText, isDark)
                SyntaxLanguage.SHELL -> highlightShell(effectiveText, isDark)
                SyntaxLanguage.PLAIN -> { /* 无高亮 */ }
            }
        }
    }

    /** JavaScript / TypeScript 语法高亮分词算法 */
    private fun AnnotatedString.Builder.highlightJs(text: String, isDark: Boolean) {
        // 1. 正则检索 JS 关键字 (如 const, function, return)
        val kwMatcher = JS_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            // matcher.start() 为起点索引, matcher.end() 为终点索引 [start, end)
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 2. 正则检索数字面量 (整数/浮点数)
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 3. 正则检索单双引号与模板字符串 ("...", '...', `...`)
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 4. 正则检索单行 // 与多行 /* */ 注释 (最高优先级覆盖其他颜色)
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }


    /** HTML 标记语言高亮分词（支持内嵌 JS/CSS 混合高亮） */
    private fun AnnotatedString.Builder.highlightHtml(text: String, isDark: Boolean) {
        // 1. 先对内嵌 <script> 区块应用 JS 高亮
        val scriptMatcher = HTML_SCRIPT_BLOCK.matcher(text)
        while (scriptMatcher.find()) {
            val innerStart = scriptMatcher.start(1)
            val innerEnd = scriptMatcher.end(1)
            if (innerStart < innerEnd) {
                val jsContent = text.substring(innerStart, innerEnd)
                applyJsHighlightInRange(jsContent, innerStart, isDark)
            }
        }

        // 2. 对内嵌 <style> 区块应用 CSS 高亮
        val styleMatcher = HTML_STYLE_BLOCK.matcher(text)
        while (styleMatcher.find()) {
            val innerStart = styleMatcher.start(1)
            val innerEnd = styleMatcher.end(1)
            if (innerStart < innerEnd) {
                val cssContent = text.substring(innerStart, innerEnd)
                applyCssHighlightInRange(cssContent, innerStart, isDark)
            }
        }

        // 3. HTML 标签高亮
        val tagMatcher = HTML_TAG_PATTERN.matcher(text)
        val tagStyle = getTagStyle(isDark)
        while (tagMatcher.find()) {
            addStyle(tagStyle, tagMatcher.start(), tagMatcher.end())
        }

        // 4. 属性名高亮
        val attrMatcher = HTML_ATTR_NAME_PATTERN.matcher(text)
        val attrStyle = getAttrStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, attrMatcher.start(), attrMatcher.end())
        }

        // 5. 字符串高亮
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 6. HTML 注释高亮（最高优先级）
        val cmtMatcher = HTML_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /** 在指定偏移范围内应用 JS 高亮（用于 HTML 内嵌 script） */
    private fun AnnotatedString.Builder.applyJsHighlightInRange(jsText: String, offset: Int, isDark: Boolean) {
        val kwMatcher = JS_KEYWORD_PATTERN.matcher(jsText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }
        val strMatcher = JS_STRING_PATTERN.matcher(jsText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(jsText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /** 在指定偏移范围内应用 CSS 高亮（用于 HTML 内嵌 style） */
    private fun AnnotatedString.Builder.applyCssHighlightInRange(cssText: String, offset: Int, isDark: Boolean) {
        val propMatcher = CSS_PROP_PATTERN.matcher(cssText)
        val propStyle = getAttrStyle(isDark)
        while (propMatcher.find()) {
            addStyle(propStyle, offset + propMatcher.start(), offset + propMatcher.end())
        }
        val numMatcher = NUMBER_PATTERN.matcher(cssText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }
    }

    /** Markdown 标题与加粗代码语法高亮 */
    private fun AnnotatedString.Builder.highlightMarkdown(text: String, isDark: Boolean) {
        val headerMatcher = MD_HEADER_PATTERN.matcher(text)
        val headerStyle = getHeaderStyle(isDark)
        while (headerMatcher.find()) {
            addStyle(headerStyle, headerMatcher.start(), headerMatcher.end())
        }

        val boldMatcher = MD_BOLD_PATTERN.matcher(text)
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        while (boldMatcher.find()) {
            addStyle(boldStyle, boldMatcher.start(), boldMatcher.end())
        }

        val codeMatcher = MD_CODE_PATTERN.matcher(text)
        val codeStyle = getStringStyle(isDark)
        while (codeMatcher.find()) {
            addStyle(codeStyle, codeMatcher.start(), codeMatcher.end())
        }
    }

    /** Prompt AI 提示词变量（如 {input} 或 $var）语法高亮 */
    private fun AnnotatedString.Builder.highlightPrompt(text: String, isDark: Boolean) {
        val varMatcher = PROMPT_VAR_PATTERN.matcher(text)
        val varStyle = getVariableStyle(isDark)
        while (varMatcher.find()) {
            addStyle(varStyle, varMatcher.start(), varMatcher.end())
        }
    }

    /** JSON 语法高亮：key + string + number + boolean/null */
    private fun AnnotatedString.Builder.highlightJson(text: String, isDark: Boolean) {
        val keyMatcher = JSON_KEY_PATTERN.matcher(text)
        val keyStyle = getTagStyle(isDark)
        while (keyMatcher.find()) {
            addStyle(keyStyle, keyMatcher.start(), keyMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        val boolMatcher = JSON_BOOL_PATTERN.matcher(text)
        val boolStyle = getKeywordStyle(isDark)
        while (boolMatcher.find()) {
            addStyle(boolStyle, boolMatcher.start(), boolMatcher.end())
        }
    }

    /** Python 语法高亮 */
    private fun AnnotatedString.Builder.highlightPython(text: String, isDark: Boolean) {
        val kwMatcher = PYTHON_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        val strMatcher = PYTHON_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        val decMatcher = PYTHON_DECORATOR_PATTERN.matcher(text)
        val decStyle = getDecoratorStyle(isDark)
        while (decMatcher.find()) {
            addStyle(decStyle, decMatcher.start(), decMatcher.end())
        }

        val cmtMatcher = PYTHON_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /** CSS 语法高亮 */
    private fun AnnotatedString.Builder.highlightCss(text: String, isDark: Boolean) {
        val selMatcher = CSS_SELECTOR_PATTERN.matcher(text)
        val selStyle = getSelectorStyle(isDark)
        while (selMatcher.find()) {
            addStyle(selStyle, selMatcher.start(), selMatcher.end())
        }

        val propMatcher = CSS_PROP_PATTERN.matcher(text)
        val propStyle = getAttrStyle(isDark)
        while (propMatcher.find()) {
            addStyle(propStyle, propMatcher.start(), propMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        val cmtMatcher = CSS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /** XML 语法高亮（复用 HTML 逻辑） */
    private fun AnnotatedString.Builder.highlightXml(text: String, isDark: Boolean) {
        val tagMatcher = HTML_TAG_PATTERN.matcher(text)
        val tagStyle = getTagStyle(isDark)
        while (tagMatcher.find()) {
            addStyle(tagStyle, tagMatcher.start(), tagMatcher.end())
        }

        val attrMatcher = HTML_ATTR_NAME_PATTERN.matcher(text)
        val attrStyle = getAttrStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, attrMatcher.start(), attrMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        val cmtMatcher = HTML_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /** YAML 语法高亮 */
    private fun AnnotatedString.Builder.highlightYaml(text: String, isDark: Boolean) {
        val keyMatcher = YAML_KEY_PATTERN.matcher(text)
        val keyStyle = getYamlKeyStyle(isDark)
        while (keyMatcher.find()) {
            addStyle(keyStyle, keyMatcher.start(), keyMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        val cmtMatcher = YAML_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /** Shell/Bash 语法高亮 */
    private fun AnnotatedString.Builder.highlightShell(text: String, isDark: Boolean) {
        val kwMatcher = SHELL_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        val varMatcher = SHELL_VAR_PATTERN.matcher(text)
        val varStyle = getShellVarStyle(isDark)
        while (varMatcher.find()) {
            addStyle(varStyle, varMatcher.start(), varMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        val cmtMatcher = SHELL_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }
}


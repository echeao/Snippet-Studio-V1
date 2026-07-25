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
        "\\{\\{?[a-zA-Z0-9_]+\\}?\\}|\\$[a-zA-Z0-9_]+"
    )

    /**
     * 对给定的文本按照指定片段类型进行语法高亮分析。
     *
     * 教学解析：
     * 1. `buildAnnotatedString`: Compose 提供的文本构造器，允许在同一个 String 上追加不同颜色的 `SpanStyle`。
     * 2. 覆盖次序逻辑 (Precedence Ordering): 先匹配关键字/数字，再匹配字符串，最后匹配注释。
     *    后添加的 `addStyle` 会覆盖先添加的重叠区间样式，因此注释在最后遍历，可确保被注释掉的代码统一变为灰色斜体。
     *
     * @param text 纯代码文本
     * @param type 片段类型 (JS, HTML, Markdown, Prompt)
     * @param isDark 当前是否为深色主题
     * @return 注入样式后的 [AnnotatedString] 富文本对象
     */
    fun highlight(text: String, type: SnippetType, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            // 先将原始字符串无样式追加到构造器中
            append(text)

            // 根据传入的 SnippetType 语法分流分词
            when (type) {
                SnippetType.JS -> highlightJs(text, isDark)
                SnippetType.HTML -> highlightHtml(text, isDark)
                SnippetType.MARKDOWN -> highlightMarkdown(text, isDark)
                SnippetType.PROMPT -> highlightPrompt(text, isDark)
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


    /** HTML 标记语言高亮分词 */
    private fun AnnotatedString.Builder.highlightHtml(text: String, isDark: Boolean) {
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
}


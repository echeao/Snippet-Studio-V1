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
 * 核心原理：使用正则表达式匹配各种代码元素（关键字、字符串、数字、注释、标签、变量、CSS选择器/属性/单位、JS函数/内置对象等），
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

    /** 获取函数/方法调用样式（如 document.getElementById, console.log） */
    private fun getFunctionStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF82AAFF) else Color(0xFF1565C0),
        fontWeight = FontWeight.SemiBold
    )

    /** 获取 CSS 属性常用值/关键字样式 */
    private fun getCssValueStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFFC792EA) else Color(0xFF7B1FA2)
    )

    /** 获取 CSS 变量名与颜色值样式 (如 --bg, #101214) */
    private fun getCssVarStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B),
        fontWeight = FontWeight.Medium
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
        "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|import|export|from|default|class|extends|async|await|try|catch|finally|throw|new|this|typeof|instanceof|void|in|of|null|undefined|true|false|yield)\\b"
    )
    private val JS_BUILTIN_PATTERN = Pattern.compile(
        "\\b(document|window|console|Math|JSON|Array|Object|String|Number|Boolean|Promise|Date|RegExp|Set|Map|Event|Element|HTMLElement|JSZip|URL|Blob)\\b"
    )
    private val JS_FUNC_CALL_PATTERN = Pattern.compile(
        "\\b[a-zA-Z_$][a-zA-Z0-9_$]*(?=\\s*\\()"
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

    /**
     * 优化后的 CSS 选择器匹配正则：
     * 允许前导缩进空格，精确提取选择器内容（支持 .class, #id, tag, :root 等）。
     */
    private val CSS_SELECTOR_PATTERN = Pattern.compile(
        "(?m)^\\s*([^{}@/\\s][^{}]*?)(?=\\s*\\{)"
    )
    private val CSS_PROP_PATTERN = Pattern.compile(
        "[a-zA-Z0-9_-]+(?=\\s*:)"
    )
    private val CSS_VAR_PATTERN = Pattern.compile(
        "var\\(--[a-zA-Z0-9_-]+\\)|--[a-zA-Z0-9_-]+"
    )
    private val CSS_HEX_COLOR_PATTERN = Pattern.compile(
        "#[0-9a-fA-F]{3,8}\\b"
    )
    private val CSS_VALUE_KEYWORD_PATTERN = Pattern.compile(
        "\\b(none|block|flex|grid|inline|inline-block|relative|absolute|fixed|sticky|bold|normal|pointer|auto|solid|dashed|dotted|transparent|center|left|right|top|bottom|cover|contain|nowrap|wrap|column|row|space-between|space-around|blur|inherit|initial|unset)\\b"
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

    /** 不区分大小写且支持未闭合标签到末尾 ($) 的内嵌 script/style 匹配正则 */
    private val HTML_SCRIPT_BLOCK = Pattern.compile(
        "(?is)<script[^>]*>(.*?)(?:</script>|$)"
    )
    private val HTML_STYLE_BLOCK = Pattern.compile(
        "(?is)<style[^>]*>(.*?)(?:</style>|$)"
    )
    /** HTML 元素内联 style="..." 属性内容匹配正则 */
    private val HTML_INLINE_STYLE_ATTR = Pattern.compile(
        "\\bstyle\\s*=\\s*\"([^\"]*)\"|\\bstyle\\s*=\\s*'([^']*)'"
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
     * 对超大文本进行截断保护，支持最高 150000 字符（约 150KB），避免撕裂语法块。
     *
     * @param text 待解析的高亮代码全文
     * @param language 目标语法语言枚举
     * @param isDark 当前是否为深色主题
     * @return 富文本 [AnnotatedString] 对象
     */
    fun highlightByLanguage(text: String, language: SyntaxLanguage, isDark: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        // 性能保护上限提升至 150,000 字符 (150KB)，足以保障绝大多数复杂网页代码不被截断
        val effectiveText = if (text.length > 150000) text.substring(0, 150000) else text

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

    /** JavaScript / TypeScript 语法高亮分词算法（包含内置对象与函数调用识别） */
    private fun AnnotatedString.Builder.highlightJs(text: String, isDark: Boolean) {
        // 1. 函数/方法调用高亮 (如 console.log(), alert())
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(text)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, funcMatcher.start(), funcMatcher.end())
        }

        // 2. JS 语言关键字匹配 (如 const, let, function)
        val kwMatcher = JS_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 3. JS 内置全局对象匹配 (如 document, window, Math)
        val builtinMatcher = JS_BUILTIN_PATTERN.matcher(text)
        while (builtinMatcher.find()) {
            addStyle(kwStyle, builtinMatcher.start(), builtinMatcher.end())
        }

        // 4. 正则检索数字面量 (整数/浮点数)
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 5. 正则检索单双引号与模板字符串 ("...", '...', `...`)
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 6. 正则检索单行 // 与多行 /* */ 注释 (最高优先级覆盖其他颜色)
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /**
     * HTML 标记语言高亮分词（支持内嵌 <script>、<style> 及内联 style="..." 属性分词与防覆盖）。
     *
     * @param text 待高亮解析的 HTML 源码文本
     * @param isDark 当前是否为深色主题模式
     */
    private fun AnnotatedString.Builder.highlightHtml(text: String, isDark: Boolean) {
        // 存储内嵌代码块 (script/style) 内部内容的绝对索引区间，用于防止后续 HTML 标签/属性/字符串正则误覆盖内嵌代码
        val embeddedRanges = mutableListOf<IntRange>()

        // 1. 先对内嵌 <script> 区块应用 JS 语法高亮
        val scriptMatcher = HTML_SCRIPT_BLOCK.matcher(text)
        while (scriptMatcher.find()) {
            val innerStart = scriptMatcher.start(1)
            val innerEnd = scriptMatcher.end(1)
            if (innerStart < innerEnd) {
                // 记录脚本内容区间
                embeddedRanges.add(innerStart until innerEnd)
                val jsContent = text.substring(innerStart, innerEnd)
                applyJsHighlightInRange(jsContent, innerStart, isDark)
            }
        }

        // 2. 对内嵌 <style> 区块应用 CSS 语法高亮
        val styleMatcher = HTML_STYLE_BLOCK.matcher(text)
        while (styleMatcher.find()) {
            val innerStart = styleMatcher.start(1)
            val innerEnd = styleMatcher.end(1)
            if (innerStart < innerEnd) {
                // 记录样式表内容区间
                embeddedRanges.add(innerStart until innerEnd)
                val cssContent = text.substring(innerStart, innerEnd)
                applyCssHighlightInRange(cssContent, innerStart, isDark)
            }
        }

        // 3. 对元素内联 style="..." 属性内容应用 CSS 高亮
        val inlineStyleMatcher = HTML_INLINE_STYLE_ATTR.matcher(text)
        while (inlineStyleMatcher.find()) {
            val innerStart = if (inlineStyleMatcher.start(1) != -1) inlineStyleMatcher.start(1) else inlineStyleMatcher.start(2)
            val innerEnd = if (inlineStyleMatcher.end(1) != -1) inlineStyleMatcher.end(1) else inlineStyleMatcher.end(2)
            if (innerStart != -1 && innerEnd > innerStart) {
                val inlineCss = text.substring(innerStart, innerEnd)
                applyCssHighlightInRange(inlineCss, innerStart, isDark)
            }
        }

        // 辅助检查函数：判断给定的起点和终点是否完全落在内嵌 script/style 区域内部
        fun isInsideEmbeddedRange(start: Int, end: Int): Boolean {
            return embeddedRanges.any { range -> start >= range.first && end <= (range.last + 1) }
        }

        // 4. HTML 标签高亮（避开内嵌 script/style 内部代码）
        val tagMatcher = HTML_TAG_PATTERN.matcher(text)
        val tagStyle = getTagStyle(isDark)
        while (tagMatcher.find()) {
            val start = tagMatcher.start()
            val end = tagMatcher.end()
            if (!isInsideEmbeddedRange(start, end)) {
                addStyle(tagStyle, start, end)
            }
        }

        // 5. HTML 属性名高亮（避开内嵌 script/style 内部代码）
        val attrMatcher = HTML_ATTR_NAME_PATTERN.matcher(text)
        val attrStyle = getAttrStyle(isDark)
        while (attrMatcher.find()) {
            val start = attrMatcher.start()
            val end = attrMatcher.end()
            if (!isInsideEmbeddedRange(start, end)) {
                addStyle(attrStyle, start, end)
            }
        }

        // 6. HTML 字符串高亮（避开内嵌 script/style 内部代码，防止覆盖内嵌 JS/CSS 字符串与结构）
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            val start = strMatcher.start()
            val end = strMatcher.end()
            if (!isInsideEmbeddedRange(start, end)) {
                addStyle(strStyle, start, end)
            }
        }

        // 7. HTML 注释高亮（最高优先级覆盖，避开内嵌代码块）
        val cmtMatcher = HTML_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            val start = cmtMatcher.start()
            val end = cmtMatcher.end()
            if (!isInsideEmbeddedRange(start, end)) {
                addStyle(cmtStyle, start, end)
            }
        }
    }

    /**
     * 在指定偏移范围内应用 JS 语法高亮（用于 HTML 内嵌 <script> 标签解析）。
     *
     * @param jsText script 标签内部的 JavaScript 代码片段
     * @param offset 该代码片段在完整 HTML 中的起始字符偏移量
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.applyJsHighlightInRange(jsText: String, offset: Int, isDark: Boolean) {
        // 1. JS 函数/方法调用名称匹配 (如 addEventListener, downloadBlob)
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(jsText)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, offset + funcMatcher.start(), offset + funcMatcher.end())
        }

        // 2. JS 关键字匹配 (如 const, function, return)
        val kwMatcher = JS_KEYWORD_PATTERN.matcher(jsText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        // 3. JS 内置全局对象匹配 (如 document, window, console)
        val builtinMatcher = JS_BUILTIN_PATTERN.matcher(jsText)
        while (builtinMatcher.find()) {
            addStyle(kwStyle, offset + builtinMatcher.start(), offset + builtinMatcher.end())
        }

        // 4. JS 数字字面量匹配
        val numMatcher = NUMBER_PATTERN.matcher(jsText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        // 5. JS 字符串与模板文本匹配 ("...", '...', `...`)
        val strMatcher = JS_STRING_PATTERN.matcher(jsText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        // 6. JS 注释匹配
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(jsText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 CSS 语法高亮（用于 HTML 内嵌 <style> 标签解析）。
     *
     * @param cssText style 标签内部的 CSS 代码片段
     * @param offset 该代码片段在完整 HTML 中的起始字符偏移量
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.applyCssHighlightInRange(cssText: String, offset: Int, isDark: Boolean) {
        // 1. CSS 选择器匹配（剔除前导空白后高亮真正选择器名称）
        val selMatcher = CSS_SELECTOR_PATTERN.matcher(cssText)
        val selStyle = getSelectorStyle(isDark)
        while (selMatcher.find()) {
            val selStart = selMatcher.start(1)
            val selEnd = selMatcher.end(1)
            if (selStart < selEnd) {
                addStyle(selStyle, offset + selStart, offset + selEnd)
            }
        }

        // 2. CSS 属性名匹配 (如 margin, display, font-size)
        val propMatcher = CSS_PROP_PATTERN.matcher(cssText)
        val propStyle = getAttrStyle(isDark)
        while (propMatcher.find()) {
            addStyle(propStyle, offset + propMatcher.start(), offset + propMatcher.end())
        }

        // 3. CSS 变量名与 var(...) 函数匹配 (如 --bg, --text-dim)
        val varMatcher = CSS_VAR_PATTERN.matcher(cssText)
        val varStyle = getCssVarStyle(isDark)
        while (varMatcher.find()) {
            addStyle(varStyle, offset + varMatcher.start(), offset + varMatcher.end())
        }

        // 4. CSS 十六进制颜色匹配 (如 #101214, #fff)
        val hexMatcher = CSS_HEX_COLOR_PATTERN.matcher(cssText)
        val hexStyle = getCssVarStyle(isDark)
        while (hexMatcher.find()) {
            addStyle(hexStyle, offset + hexMatcher.start(), offset + hexMatcher.end())
        }

        // 5. CSS 属性关键字与常用值匹配 (如 flex, sticky, bold, pointer)
        val valueKwMatcher = CSS_VALUE_KEYWORD_PATTERN.matcher(cssText)
        val valueKwStyle = getCssValueStyle(isDark)
        while (valueKwMatcher.find()) {
            addStyle(valueKwStyle, offset + valueKwMatcher.start(), offset + valueKwMatcher.end())
        }

        // 6. CSS 数字与单位匹配
        val numMatcher = NUMBER_PATTERN.matcher(cssText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        // 7. CSS 注释匹配
        val cmtMatcher = CSS_COMMENT_PATTERN.matcher(cssText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
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
            val selStart = selMatcher.start(1)
            val selEnd = selMatcher.end(1)
            if (selStart < selEnd) {
                addStyle(selStyle, selStart, selEnd)
            }
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

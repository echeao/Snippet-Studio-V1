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
@Deprecated(
    message = "项目已升级为 Sora-Editor + TextMate 语法高亮引擎。旧版基于 BasicTextField + 正则匹配的 SyntaxHighlighter 仅保留作为历史参考或轻量预览兼容，不再用于主代码编辑器。",
    replaceWith = ReplaceWith("SoraCodeEditor", "com.feige.snippetstudio.ui.components.SoraCodeEditor")
)
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
        "`[^`\\r\\n]+`"
    )
    /** Markdown 多行围栏代码块正则（捕获组 1: 语言标识；捕获组 2: 代码正文；捕获组 3: 结尾围栏标记） */
    private val MD_CODE_BLOCK_PATTERN = Pattern.compile(
        "(?m)^```([a-zA-Z0-9_+#-]*)[ \\t]*\\r?\\n([\\s\\S]*?)(?:(^```[ \\t]*(?:\\r?\\n|\\z))|(?=\\z))"
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

    // ===== Java / C++ / Go / Rust 语言匹配正则表达式模式 =====

    private val JAVA_KEYWORD_PATTERN = Pattern.compile(
        "\\b(public|protected|private|class|interface|enum|extends|implements|import|package|static|final|abstract|void|int|long|double|float|boolean|char|byte|short|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|throws|new|this|super|instanceof|synchronized|volatile|transient|native|var|record|true|false|null)\\b"
    )
    private val JAVA_ANNOTATION_PATTERN = Pattern.compile(
        "@[a-zA-Z_][a-zA-Z0-9_]*"
    )

    private val CPP_PREPROCESSOR_PATTERN = Pattern.compile(
        "(?m)^\\s*#(include|define|undef|ifdef|ifndef|if|else|elif|endif|pragma)\\b.*"
    )
    private val CPP_KEYWORD_PATTERN = Pattern.compile(
        "\\b(int|long|short|char|float|double|bool|void|unsigned|signed|const|static|struct|class|union|enum|typedef|template|typename|namespace|using|public|protected|private|virtual|override|final|inline|constexpr|auto|return|if|else|for|while|do|switch|case|default|break|continue|goto|try|catch|throw|new|delete|sizeof|this|nullptr|true|false)\\b"
    )

    private val GO_KEYWORD_PATTERN = Pattern.compile(
        "\\b(func|package|import|type|struct|interface|var|const|return|if|else|for|range|switch|case|default|select|defer|go|break|continue|fallthrough|goto|chan|map|make|new|len|cap|append|copy|panic|recover|true|false|nil|string|int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|float32|float64|bool|byte|rune)\\b"
    )
    private val GO_RAW_STRING_PATTERN = Pattern.compile(
        "`[^`]*`"
    )

    private val RUST_KEYWORD_PATTERN = Pattern.compile(
        "\\b(fn|let|mut|pub|use|mod|struct|enum|trait|impl|type|where|for|in|if|else|loop|while|match|return|break|continue|async|await|dyn|static|const|unsafe|extern|as|move|ref|self|Self|true|false|Some|None|Ok|Err|i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|bool|char|str)\\b"
    )
    private val RUST_MACRO_PATTERN = Pattern.compile(
        "\\b[a-zA-Z_][a-zA-Z0-9_]*!"
    )
    private val RUST_ATTRIBUTE_PATTERN = Pattern.compile(
        "#!?\\[[^\\]]*\\]"
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
                SnippetType.JAVA -> highlightJava(text, isDark)
                SnippetType.GENERAL -> highlightPrompt(text, isDark)
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
                SyntaxLanguage.JAVA -> highlightJava(effectiveText, isDark)
                SyntaxLanguage.CPP -> highlightCpp(effectiveText, isDark)
                SyntaxLanguage.GO -> highlightGo(effectiveText, isDark)
                SyntaxLanguage.RUST -> highlightRust(effectiveText, isDark)
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

    /**
     * 根据围栏代码块中的语言标识串（如 "java", "py", "js"）或文本特征推断对应的 [SyntaxLanguage]。
     *
     * @param tag 语言标签串 (如 "java", "javascript", "python")
     * @param content 代码块内部正文
     * @return 识别出的 [SyntaxLanguage]
     */
    private fun resolveLanguageFromTag(tag: String, content: String): SyntaxLanguage {
        val cleanTag = tag.trim().lowercase()
        return when (cleanTag) {
            "java" -> SyntaxLanguage.JAVA
            "js", "javascript", "ts", "typescript", "mjs" -> SyntaxLanguage.JS
            "html", "htm" -> SyntaxLanguage.HTML
            "css", "scss", "less" -> SyntaxLanguage.CSS
            "json" -> SyntaxLanguage.JSON
            "py", "python" -> SyntaxLanguage.PYTHON
            "xml", "svg" -> SyntaxLanguage.XML
            "yaml", "yml" -> SyntaxLanguage.YAML
            "sh", "bash", "zsh", "shell" -> SyntaxLanguage.SHELL
            "c", "cpp", "c++", "h", "hpp" -> SyntaxLanguage.CPP
            "go", "golang" -> SyntaxLanguage.GO
            "rs", "rust" -> SyntaxLanguage.RUST
            else -> SyntaxLanguageDetector.fromContent(content)
        }
    }

    /**
     * 在文本指定区间段应用特定编程语言的高亮算法（用于 Markdown 嵌套代码块高亮）。
     *
     * @param codeText 代码块内部正文
     * @param language 目标语法语言 [SyntaxLanguage]
     * @param offset 代码块内部起点在全文中的绝对字符偏移量
     * @param isDark 是否为深色模式
     */
    private fun AnnotatedString.Builder.applyLanguageHighlightInRange(
        codeText: String,
        language: SyntaxLanguage,
        offset: Int,
        isDark: Boolean
    ) {
        when (language) {
            SyntaxLanguage.JAVA -> applyJavaHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.JS -> applyJsHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.CSS -> applyCssHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.PYTHON -> applyPythonHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.JSON -> applyJsonHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.SHELL -> applyShellHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.CPP -> applyCppHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.GO -> applyGoHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.RUST -> applyRustHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.YAML -> applyYamlHighlightInRange(codeText, offset, isDark)
            SyntaxLanguage.HTML, SyntaxLanguage.XML -> applyXmlHighlightInRange(codeText, offset, isDark)
            else -> { /* PLAIN / PROMPT 保持默认样式 */ }
        }
    }

    /**
     * 在指定偏移范围内应用 Python 语法高亮。
     */
    private fun AnnotatedString.Builder.applyPythonHighlightInRange(pyText: String, offset: Int, isDark: Boolean) {
        val kwMatcher = PYTHON_KEYWORD_PATTERN.matcher(pyText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(pyText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val strMatcher = PYTHON_STRING_PATTERN.matcher(pyText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val decMatcher = PYTHON_DECORATOR_PATTERN.matcher(pyText)
        val decStyle = getDecoratorStyle(isDark)
        while (decMatcher.find()) {
            addStyle(decStyle, offset + decMatcher.start(), offset + decMatcher.end())
        }

        val cmtMatcher = PYTHON_COMMENT_PATTERN.matcher(pyText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 JSON 语法高亮。
     */
    private fun AnnotatedString.Builder.applyJsonHighlightInRange(jsonText: String, offset: Int, isDark: Boolean) {
        val keyMatcher = JSON_KEY_PATTERN.matcher(jsonText)
        val keyStyle = getTagStyle(isDark)
        while (keyMatcher.find()) {
            addStyle(keyStyle, offset + keyMatcher.start(), offset + keyMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(jsonText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(jsonText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val boolMatcher = JSON_BOOL_PATTERN.matcher(jsonText)
        val boolStyle = getKeywordStyle(isDark)
        while (boolMatcher.find()) {
            addStyle(boolStyle, offset + boolMatcher.start(), offset + boolMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 Shell/Bash 语法高亮。
     */
    private fun AnnotatedString.Builder.applyShellHighlightInRange(shText: String, offset: Int, isDark: Boolean) {
        val kwMatcher = SHELL_KEYWORD_PATTERN.matcher(shText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val varMatcher = SHELL_VAR_PATTERN.matcher(shText)
        val varStyle = getShellVarStyle(isDark)
        while (varMatcher.find()) {
            addStyle(varStyle, offset + varMatcher.start(), offset + varMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(shText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val cmtMatcher = SHELL_COMMENT_PATTERN.matcher(shText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 XML/HTML 语法高亮。
     */
    private fun AnnotatedString.Builder.applyXmlHighlightInRange(xmlText: String, offset: Int, isDark: Boolean) {
        val tagMatcher = HTML_TAG_PATTERN.matcher(xmlText)
        val tagStyle = getTagStyle(isDark)
        while (tagMatcher.find()) {
            addStyle(tagStyle, offset + tagMatcher.start(), offset + tagMatcher.end())
        }

        val attrMatcher = HTML_ATTR_NAME_PATTERN.matcher(xmlText)
        val attrStyle = getAttrStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, offset + attrMatcher.start(), offset + attrMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(xmlText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val cmtMatcher = HTML_COMMENT_PATTERN.matcher(xmlText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 YAML 语法高亮。
     */
    private fun AnnotatedString.Builder.applyYamlHighlightInRange(yamlText: String, offset: Int, isDark: Boolean) {
        val keyMatcher = YAML_KEY_PATTERN.matcher(yamlText)
        val keyStyle = getYamlKeyStyle(isDark)
        while (keyMatcher.find()) {
            addStyle(keyStyle, offset + keyMatcher.start(), offset + keyMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(yamlText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(yamlText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val cmtMatcher = YAML_COMMENT_PATTERN.matcher(yamlText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 Java 语法高亮。
     */
    private fun AnnotatedString.Builder.applyJavaHighlightInRange(javaText: String, offset: Int, isDark: Boolean) {
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(javaText)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, offset + funcMatcher.start(), offset + funcMatcher.end())
        }

        val annoMatcher = JAVA_ANNOTATION_PATTERN.matcher(javaText)
        val annoStyle = getDecoratorStyle(isDark)
        while (annoMatcher.find()) {
            addStyle(annoStyle, offset + annoMatcher.start(), offset + annoMatcher.end())
        }

        val kwMatcher = JAVA_KEYWORD_PATTERN.matcher(javaText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(javaText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(javaText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val cmtMatcher = JS_COMMENT_PATTERN.matcher(javaText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 C/C++ 语法高亮。
     */
    private fun AnnotatedString.Builder.applyCppHighlightInRange(cppText: String, offset: Int, isDark: Boolean) {
        val prepMatcher = CPP_PREPROCESSOR_PATTERN.matcher(cppText)
        val prepStyle = getTagStyle(isDark)
        while (prepMatcher.find()) {
            addStyle(prepStyle, offset + prepMatcher.start(), offset + prepMatcher.end())
        }

        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(cppText)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, offset + funcMatcher.start(), offset + funcMatcher.end())
        }

        val kwMatcher = CPP_KEYWORD_PATTERN.matcher(cppText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(cppText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(cppText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val cmtMatcher = JS_COMMENT_PATTERN.matcher(cppText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 Go 语法高亮。
     */
    private fun AnnotatedString.Builder.applyGoHighlightInRange(goText: String, offset: Int, isDark: Boolean) {
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(goText)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, offset + funcMatcher.start(), offset + funcMatcher.end())
        }

        val kwMatcher = GO_KEYWORD_PATTERN.matcher(goText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(goText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(goText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val rawStrMatcher = GO_RAW_STRING_PATTERN.matcher(goText)
        while (rawStrMatcher.find()) {
            addStyle(strStyle, offset + rawStrMatcher.start(), offset + rawStrMatcher.end())
        }

        val cmtMatcher = JS_COMMENT_PATTERN.matcher(goText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /**
     * 在指定偏移范围内应用 Rust 语法高亮。
     */
    private fun AnnotatedString.Builder.applyRustHighlightInRange(rustText: String, offset: Int, isDark: Boolean) {
        val attrMatcher = RUST_ATTRIBUTE_PATTERN.matcher(rustText)
        val attrStyle = getDecoratorStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, offset + attrMatcher.start(), offset + attrMatcher.end())
        }

        val macroMatcher = RUST_MACRO_PATTERN.matcher(rustText)
        val macroStyle = getTagStyle(isDark)
        while (macroMatcher.find()) {
            addStyle(macroStyle, offset + macroMatcher.start(), offset + macroMatcher.end())
        }

        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(rustText)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, offset + funcMatcher.start(), offset + funcMatcher.end())
        }

        val kwMatcher = RUST_KEYWORD_PATTERN.matcher(rustText)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, offset + kwMatcher.start(), offset + kwMatcher.end())
        }

        val numMatcher = NUMBER_PATTERN.matcher(rustText)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, offset + numMatcher.start(), offset + numMatcher.end())
        }

        val strMatcher = JS_STRING_PATTERN.matcher(rustText)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, offset + strMatcher.start(), offset + strMatcher.end())
        }

        val cmtMatcher = JS_COMMENT_PATTERN.matcher(rustText)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, offset + cmtMatcher.start(), offset + cmtMatcher.end())
        }
    }

    /** Markdown 标题、加粗以及多语言内嵌围栏代码块语法高亮 */
    private fun AnnotatedString.Builder.highlightMarkdown(text: String, isDark: Boolean) {
        val codeBlockRanges = mutableListOf<IntRange>()

        // 1. 优先提取多行围栏代码块 (```java, ```js 等)，进行局部精细代码高亮
        val blockMatcher = MD_CODE_BLOCK_PATTERN.matcher(text)
        val codeBlockBorderStyle = SpanStyle(
            color = if (isDark) Color(0xFF82AAFF) else Color(0xFF1565C0),
            fontWeight = FontWeight.Bold
        )

        while (blockMatcher.find()) {
            val fullStart = blockMatcher.start()
            val fullEnd = blockMatcher.end()
            codeBlockRanges.add(fullStart until fullEnd)

            val langTag = blockMatcher.group(1)?.trim()?.lowercase() ?: ""
            val innerStart = blockMatcher.start(2)
            val innerEnd = blockMatcher.end(2)

            // 高亮 ``` 代码块界定符号
            addStyle(codeBlockBorderStyle, fullStart, (fullStart + 3 + langTag.length).coerceAtMost(fullEnd))
            if (text.substring(fullStart, fullEnd).endsWith("```")) {
                addStyle(codeBlockBorderStyle, (fullEnd - 3).coerceAtLeast(fullStart), fullEnd)
            }

            if (innerStart < innerEnd) {
                val codeContent = text.substring(innerStart, innerEnd)
                val lang = resolveLanguageFromTag(langTag, codeContent)
                applyLanguageHighlightInRange(codeContent, lang, innerStart, isDark)
            }
        }

        fun isInsideCodeBlock(start: Int, end: Int): Boolean {
            return codeBlockRanges.any { range -> start >= range.first && end <= (range.last + 1) }
        }

        // 2. 标粗 Markdown 标题 (避开代码块内部)
        val headerMatcher = MD_HEADER_PATTERN.matcher(text)
        val headerStyle = getHeaderStyle(isDark)
        while (headerMatcher.find()) {
            val start = headerMatcher.start()
            val end = headerMatcher.end()
            if (!isInsideCodeBlock(start, end)) {
                addStyle(headerStyle, start, end)
            }
        }

        // 3. 标粗 Markdown **粗体** 文本 (避开代码块内部)
        val boldMatcher = MD_BOLD_PATTERN.matcher(text)
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        while (boldMatcher.find()) {
            val start = boldMatcher.start()
            val end = boldMatcher.end()
            if (!isInsideCodeBlock(start, end)) {
                addStyle(boldStyle, start, end)
            }
        }

        // 4. 高亮 Markdown 行内 `代码` 标记 (避开代码块内部)
        val codeMatcher = MD_CODE_PATTERN.matcher(text)
        val codeStyle = getStringStyle(isDark)
        while (codeMatcher.find()) {
            val start = codeMatcher.start()
            val end = codeMatcher.end()
            if (!isInsideCodeBlock(start, end)) {
                addStyle(codeStyle, start, end)
            }
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

    /**
     * Java 语言语法高亮分词算法。
     *
     * 处理逻辑：
     * 1. 高亮类名/函数与注解 (如 `@Override`, `@Deprecated`)
     * 2. 高亮 Java 核心关键字 (如 `public`, `class`, `extends`, `void`, `new`)
     * 3. 高亮数字字面量
     * 4. 高亮双引号与单引号字符串
     * 5. 高亮单行与多行注释
     *
     * @param text 待解析的 Java 代码正文
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.highlightJava(text: String, isDark: Boolean) {
        // 1. 函数/方法调用名称匹配
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(text)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, funcMatcher.start(), funcMatcher.end())
        }

        // 2. Java 注解高亮 (@Override, @Entity 等)
        val annoMatcher = JAVA_ANNOTATION_PATTERN.matcher(text)
        val annoStyle = getDecoratorStyle(isDark)
        while (annoMatcher.find()) {
            addStyle(annoStyle, annoMatcher.start(), annoMatcher.end())
        }

        // 3. Java 关键字匹配
        val kwMatcher = JAVA_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 4. 数字字面量
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 5. 字符串字面量
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 6. 单行/多行注释 (最高优先级)
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /**
     * C/C++ 语言语法高亮分词算法。
     *
     * 处理逻辑：
     * 1. 高亮 `#include`, `#define` 等预处理指令
     * 2. 高亮 C/C++ 核心关键字及常用标准类型 (`int`, `char`, `class`, `template`, `namespace`)
     * 3. 高亮函数与方法调用名称
     * 4. 高亮数字、字符串与注释
     *
     * @param text 待解析的 C/C++ 代码正文
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.highlightCpp(text: String, isDark: Boolean) {
        // 1. 预处理指令匹配 (#include <iostream>, #define MAX 100)
        val prepMatcher = CPP_PREPROCESSOR_PATTERN.matcher(text)
        val prepStyle = getTagStyle(isDark)
        while (prepMatcher.find()) {
            addStyle(prepStyle, prepMatcher.start(), prepMatcher.end())
        }

        // 2. 函数调用高亮 (如 printf, std::cout)
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(text)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, funcMatcher.start(), funcMatcher.end())
        }

        // 3. C/C++ 关键字匹配
        val kwMatcher = CPP_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 4. 数字字面量
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 5. 字符串字面量
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 6. 单行/多行注释
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /**
     * Go (Golang) 语言语法高亮分词算法。
     *
     * 处理逻辑：
     * 1. 高亮 Go 核心关键字 (`func`, `package`, `import`, `struct`, `interface`, `go`, `chan`)
     * 2. 高亮函数与方法调用名称
     * 3. 高亮数字、单/双引号字符串以及反引号多行原生字符串 (`...`)
     * 4. 高亮注释
     *
     * @param text 待解析的 Go 代码正文
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.highlightGo(text: String, isDark: Boolean) {
        // 1. 函数/方法调用高亮 (如 fmt.Println, make)
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(text)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, funcMatcher.start(), funcMatcher.end())
        }

        // 2. Go 关键字匹配
        val kwMatcher = GO_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 3. 数字字面量
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 4. 普通字符串 ("...", '...')
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 5. 反引号原生多行文本 (`...`)
        val rawStrMatcher = GO_RAW_STRING_PATTERN.matcher(text)
        while (rawStrMatcher.find()) {
            addStyle(strStyle, rawStrMatcher.start(), rawStrMatcher.end())
        }

        // 6. 单行/多行注释
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }

    /**
     * Rust 语言语法高亮分词算法。
     *
     * 处理逻辑：
     * 1. 高亮 Rust 属性/ Derive 标记 (如 `#[derive(Debug, Clone)]`)
     * 2. 高亮 Rust 宏调用 (如 `println!`, `vec!`, `format!`)
     * 3. 高亮 Rust 核心关键字与内建基础数据类型 (`fn`, `let`, `mut`, `pub`, `impl`, `trait`, `i32`, `String`)
     * 4. 高亮函数与方法调用名称
     * 5. 高亮数字、字符串与注释
     *
     * @param text 待解析的 Rust 代码正文
     * @param isDark 当前是否为深色主题
     */
    private fun AnnotatedString.Builder.highlightRust(text: String, isDark: Boolean) {
        // 1. Rust 属性与衍生宏标签 (#[derive(...)])
        val attrMatcher = RUST_ATTRIBUTE_PATTERN.matcher(text)
        val attrStyle = getDecoratorStyle(isDark)
        while (attrMatcher.find()) {
            addStyle(attrStyle, attrMatcher.start(), attrMatcher.end())
        }

        // 2. Rust 宏调用高亮 (println!, vec!, panic!)
        val macroMatcher = RUST_MACRO_PATTERN.matcher(text)
        val macroStyle = getTagStyle(isDark)
        while (macroMatcher.find()) {
            addStyle(macroStyle, macroMatcher.start(), macroMatcher.end())
        }

        // 3. 函数/方法调用高亮
        val funcMatcher = JS_FUNC_CALL_PATTERN.matcher(text)
        val funcStyle = getFunctionStyle(isDark)
        while (funcMatcher.find()) {
            addStyle(funcStyle, funcMatcher.start(), funcMatcher.end())
        }

        // 4. Rust 关键字匹配
        val kwMatcher = RUST_KEYWORD_PATTERN.matcher(text)
        val kwStyle = getKeywordStyle(isDark)
        while (kwMatcher.find()) {
            addStyle(kwStyle, kwMatcher.start(), kwMatcher.end())
        }

        // 5. 数字字面量
        val numMatcher = NUMBER_PATTERN.matcher(text)
        val numStyle = getNumberStyle(isDark)
        while (numMatcher.find()) {
            addStyle(numStyle, numMatcher.start(), numMatcher.end())
        }

        // 6. 字符串字面量
        val strMatcher = JS_STRING_PATTERN.matcher(text)
        val strStyle = getStringStyle(isDark)
        while (strMatcher.find()) {
            addStyle(strStyle, strMatcher.start(), strMatcher.end())
        }

        // 7. 单行/多行注释
        val cmtMatcher = JS_COMMENT_PATTERN.matcher(text)
        val cmtStyle = getCommentStyle(isDark)
        while (cmtMatcher.find()) {
            addStyle(cmtStyle, cmtMatcher.start(), cmtMatcher.end())
        }
    }
}


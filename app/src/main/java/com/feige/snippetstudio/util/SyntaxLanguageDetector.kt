package com.feige.snippetstudio.util

import com.feige.snippetstudio.model.SnippetType

/**
 * [SyntaxLanguage] 枚举定义了语法高亮引擎支持的所有编程语言/标记语言。
 *
 * 与 [SnippetType] 的区别：SnippetType 是业务层的片段分类（4 种），
 * 而 SyntaxLanguage 是纯渲染层的语法着色分流（增加 Java, C/C++, Go, Rust 后共 15 种），粒度更细。
 */
enum class SyntaxLanguage {
    HTML, JS, CSS, JSON, PYTHON, MARKDOWN, PROMPT, XML, YAML, SHELL, JAVA, CPP, GO, RUST, PLAIN
}

/**
 * [SyntaxLanguageDetector] 根据文件扩展名或正文特征推断对应的语法高亮语言类型。
 */
object SyntaxLanguageDetector {

    /**
     * 根据文件名的扩展名推断语法高亮语言。
     *
     * @param fileName 带有后缀的文件名（如 "Main.java", "main.go"）
     * @return 推断出的 [SyntaxLanguage] 枚举值
     */
    fun fromFileName(fileName: String): SyntaxLanguage {
        val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.').lowercase()}" else ""
        return when (ext) {
            ".html", ".htm" -> SyntaxLanguage.HTML
            ".js", ".jsx", ".ts", ".tsx", ".mjs" -> SyntaxLanguage.JS
            ".css", ".scss", ".less" -> SyntaxLanguage.CSS
            ".json" -> SyntaxLanguage.JSON
            ".py", ".pyw" -> SyntaxLanguage.PYTHON
            ".md", ".markdown" -> SyntaxLanguage.MARKDOWN
            ".txt", ".prompt" -> SyntaxLanguage.PROMPT
            ".xml", ".svg", ".plist" -> SyntaxLanguage.XML
            ".yaml", ".yml" -> SyntaxLanguage.YAML
            ".sh", ".bash", ".zsh" -> SyntaxLanguage.SHELL
            ".java" -> SyntaxLanguage.JAVA
            ".c", ".h", ".cpp", ".hpp", ".cc", ".cxx", ".c++" -> SyntaxLanguage.CPP
            ".go" -> SyntaxLanguage.GO
            ".rs" -> SyntaxLanguage.RUST
            else -> SyntaxLanguage.PLAIN
        }
    }

    /**
     * 根据 SnippetType 和文件名综合推断语法语言。
     * 优先使用文件扩展名判断，无法识别时回退到 SnippetType 的默认映射。
     *
     * @param fileName 文件名
     * @param snippetType 业务层代码片段类型
     * @return 推断出的 [SyntaxLanguage] 枚举值
     */
    fun detect(fileName: String, snippetType: SnippetType): SyntaxLanguage {
        val byExtension = fromFileName(fileName)
        if (byExtension != SyntaxLanguage.PLAIN) return byExtension

        // 回退到 SnippetType 业务类型映射（当扩展名无法推断时，按选择的片段类型映射默认高亮）
        return when (snippetType) {
            SnippetType.HTML -> SyntaxLanguage.HTML
            SnippetType.JS -> SyntaxLanguage.JS
            SnippetType.MARKDOWN -> SyntaxLanguage.MARKDOWN
            SnippetType.PROMPT -> SyntaxLanguage.PROMPT
            SnippetType.JAVA -> SyntaxLanguage.JAVA
            SnippetType.GENERAL -> SyntaxLanguage.PLAIN
        }
    }

    /**
     * 根据文本正文特征推断代码语言（常用于无文件名或剪贴板识别场景）。
     *
     * @param text 代码正文文本
     * @return 推断出的 [SyntaxLanguage] 枚举值
     */
    fun fromContent(text: String): SyntaxLanguage {
        val trimmed = text.trimStart()
        return when {
            trimmed.startsWith("{") && trimmed.contains("\"") -> SyntaxLanguage.JSON
            trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html") -> SyntaxLanguage.HTML
            trimmed.startsWith("<?xml") -> SyntaxLanguage.XML
            trimmed.startsWith("#!") || trimmed.startsWith("#!/bin/") -> SyntaxLanguage.SHELL
            trimmed.contains(Regex("(?m)^#{1,6}\\s")) -> SyntaxLanguage.MARKDOWN
            trimmed.contains(Regex("\\b(package\\s+[a-zA-Z0-9_.]+|public\\s+class|import\\s+java)\\b")) -> SyntaxLanguage.JAVA
            trimmed.contains(Regex("#include\\s*[<\"]")) || trimmed.contains(Regex("\\b(std::|int\\s+main\\s*\\()")) -> SyntaxLanguage.CPP
            trimmed.contains(Regex("\\b(package\\s+main|func\\s+main|import\\s*\\()")) -> SyntaxLanguage.GO
            trimmed.contains(Regex("\\b(fn\\s+main|pub\\s+fn|let\\s+mut|use\\s+std)\\b")) -> SyntaxLanguage.RUST
            trimmed.contains(Regex("\\b(def|class|import|from)\\b")) && trimmed.contains(":") -> SyntaxLanguage.PYTHON
            trimmed.contains(Regex("\\b(const|let|var|function|=>)\\b")) -> SyntaxLanguage.JS
            else -> SyntaxLanguage.PLAIN
        }
    }
}


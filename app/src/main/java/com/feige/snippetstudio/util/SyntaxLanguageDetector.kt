package com.feige.snippetstudio.util

import com.feige.snippetstudio.model.SnippetType

/**
 * [SyntaxLanguage] 枚举定义了语法高亮引擎支持的所有编程语言/标记语言。
 *
 * 与 [SnippetType] 的区别：SnippetType 是业务层的片段分类（4 种），
 * 而 SyntaxLanguage 是纯渲染层的语法着色分流（11 种），粒度更细。
 */
enum class SyntaxLanguage {
    HTML, JS, CSS, JSON, PYTHON, MARKDOWN, PROMPT, XML, YAML, SHELL, PLAIN
}

/**
 * [SyntaxLanguageDetector] 根据文件扩展名或内容特征推断语法高亮语言。
 */
object SyntaxLanguageDetector {

    /**
     * 根据文件名的扩展名推断语法语言。
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
            else -> SyntaxLanguage.PLAIN
        }
    }

    /**
     * 根据 SnippetType 和文件名综合推断语法语言。
     * 优先使用文件扩展名，回退到 SnippetType 映射。
     */
    fun detect(fileName: String, snippetType: SnippetType): SyntaxLanguage {
        val byExtension = fromFileName(fileName)
        if (byExtension != SyntaxLanguage.PLAIN) return byExtension

        // 回退到 SnippetType 映射
        return when (snippetType) {
            SnippetType.HTML -> SyntaxLanguage.HTML
            SnippetType.JS -> SyntaxLanguage.JS
            SnippetType.MARKDOWN -> SyntaxLanguage.MARKDOWN
            SnippetType.PROMPT -> SyntaxLanguage.PROMPT
        }
    }

    /**
     * 根据文本内容特征推断语言（用于无文件名的场景）。
     */
    fun fromContent(text: String): SyntaxLanguage {
        val trimmed = text.trimStart()
        return when {
            trimmed.startsWith("{") && trimmed.contains("\"") -> SyntaxLanguage.JSON
            trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html") -> SyntaxLanguage.HTML
            trimmed.startsWith("<?xml") -> SyntaxLanguage.XML
            trimmed.startsWith("#!") || trimmed.startsWith("#!/bin/") -> SyntaxLanguage.SHELL
            trimmed.contains(Regex("(?m)^#{1,6}\\s")) -> SyntaxLanguage.MARKDOWN
            trimmed.contains(Regex("\\b(def|class|import|from)\\b")) && trimmed.contains(":") -> SyntaxLanguage.PYTHON
            trimmed.contains(Regex("\\b(const|let|var|function|=>)\\b")) -> SyntaxLanguage.JS
            else -> SyntaxLanguage.PLAIN
        }
    }
}

package com.feige.snippetstudio.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SnippetType(val code: String, val displayName: String, val extension: String) {
    HTML("html", "HTML", ".html"),
    JS("js", "JavaScript", ".js"),
    MARKDOWN("markdown", "Markdown", ".md"),
    PROMPT("prompt", "Prompt", ".txt");

    companion object {
        fun fromCode(code: String): SnippetType {
            return entries.firstOrNull { it.code.lowercase() == code.lowercase() } ?: HTML
        }
    }
}

data class Snippet(
    val id: String,
    val type: SnippetType,
    val title: String,
    val fileName: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val starred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Int = 0,
    val trashed: Boolean = false,
    val trashedAt: Long? = null
) {
    val displayTitle: String
        get() = title.ifBlank {
            if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        }

    val defaultFileName: String
        get() = if (title.isBlank()) "snippet${type.extension}" else "${title.replace("\\s+".toRegex(), "_")}${type.extension}"

    companion object {
        fun createDefaultContent(type: SnippetType): String {
            return when (type) {
                SnippetType.HTML -> """<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>Snippet Preview</title>
</head>
<body>
    <p style="color:red; font-family:sans-serif; font-size:18px;">能看到红色文字</p>
</body>
</html>"""
                SnippetType.JS -> """// Snippet Studio JavaScript
console.log("Hello, Snippet Studio!");
const items = [1, 2, 3, 4, 5];
const doubled = items.map(n => n * 2);
console.log("Doubled array:", doubled);"""
                SnippetType.MARKDOWN -> """# Hello Markdown

This is a **Markdown** snippet created in *Snippet Studio*.

- Clean layout
- Instant preview
- Local storage

```javascript
console.log("Code inside markdown");
```"""
                SnippetType.PROMPT -> """You are an expert software developer. Please review the following architecture and provide constructive feedback on code modularity, security, and performance."""
            }
        }

        fun generateDefaultTitle(type: SnippetType): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return "${type.displayName} · ${sdf.format(Date())}"
        }
    }
}

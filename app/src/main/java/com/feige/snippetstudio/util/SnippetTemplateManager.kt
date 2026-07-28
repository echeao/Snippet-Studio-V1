package com.feige.snippetstudio.util

import android.content.Context
import com.feige.snippetstudio.model.SnippetType

/**
 * [SnippetTemplateManager] 负责管理与读取存放在 `assets/templates/` 目录下的内置代码片段样板文件。
 *
 * 架构解析：
 * 遵循数据与领域模型分离原则（Separation of Concerns），将之前硬编码在 Snippet companion object 中的
 * HTML/JS/Markdown 样板解耦至 Android assets 静态资源目录中，并在此提供统一安全读取入口。
 */
object SnippetTemplateManager {

    /**
     * 根据 [SnippetType] 从应用 `assets/templates/` 目录中读取对应的模版代码内容。
     *
     * @param context 应用 Context
     * @param type 片段类型 [SnippetType]
     * @return 样板代码文本，若 Context 为 null 或读取文件失败，则返回空串 `""`。
     */
    fun getTemplate(context: Context?, type: SnippetType): String {
        if (context == null) return ""
        val assetPath = when (type) {
            SnippetType.HTML -> "templates/template_html.html"
            SnippetType.JS -> "templates/template_js.js"
            SnippetType.MARKDOWN -> "templates/template_markdown.md"
            SnippetType.PROMPT -> "templates/template_prompt.txt"
            SnippetType.JAVA -> "templates/template_java.java"
            SnippetType.GENERAL -> "templates/template_prompt.txt"
        }
        return runCatching {
            context.assets.open(assetPath).use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            }
        }.getOrDefault("")
    }
}

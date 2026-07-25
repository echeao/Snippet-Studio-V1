package com.feige.snippetstudio.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [SnippetType] 枚举定义了代码片段支持的所有文件类型及其关联元数据。
 *
 * @param code 类型的唯一下拉/标识字符串 (如 "html", "js")
 * @param displayName 在 UI 界面上展示给用户的类型名称 (如 "JavaScript")
 * @param extension 对应的默认文件扩展名 (如 ".html", ".js", ".md")
 */
enum class SnippetType(val code: String, val displayName: String, val extension: String) {
    HTML("html", "HTML", ".html"),
    JS("js", "JavaScript", ".js"),
    MARKDOWN("markdown", "Markdown", ".md"),
    PROMPT("prompt", "Prompt", ".txt");

    companion object {
        /**
         * 根据类型标识代码转为对应的 [SnippetType] 枚举实例。
         *
         * 教学解析：
         * Kotlin 1.9+ 推荐使用 `entries` 代替传统的 `values()`，`entries` 返回不变集合 (EnumEntries)，
         * 性能更好且避免了每次调用都分配新 Array 对象的开销。
         */
        fun fromCode(code: String): SnippetType {
            return entries.firstOrNull { it.code.lowercase() == code.lowercase() } ?: HTML
        }

        /**
         * 根据文件名后缀推断代码片段类型 [SnippetType]。
         * 使用 `substringAfterLast` 获取包含点之后的扩展名并转小写比对。
         */
        fun fromFileName(fileName: String): SnippetType {
            val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.').lowercase()}" else ""
            return when (ext) {
                ".html", ".htm" -> HTML
                ".js", ".jsx", ".ts", ".tsx" -> JS
                ".md", ".markdown" -> MARKDOWN
                else -> PROMPT
            }
        }
    }
}

/**
 * [Snippet] 是应用的核心业务数据模型 (Domain Model)，代表一个代码片段/文件。
 *
 * 教学解析：
 * 在 Clean Architecture 架构中，UI 界面与 ViewModel 直接依赖于此纯粹的领域对象，
 * 而不是 SQLite 数据库表实体类 SnippetEntity，从而解耦了持久化存储框架的实现细节。
 *
 * @param id 代码片段唯一标识符（使用 UUID 或包含相对路径的 MD5 哈希）
 * @param type 代码片段类型 [SnippetType]
 * @param title 标题
 * @param fileName 对应在文件系统中的文件名（如 "my_script.js"）
 * @param content 代码片段的具体正文内容
 * @param tags 关联的标签列表
 * @param starred 是否已加星标（收藏）
 * @param createdAt 创建时间戳 (毫秒)
 * @param updatedAt 最后修改时间戳 (毫秒)
 * @param sizeBytes 内容字节大小
 * @param folder 所属文件夹相对路径（如 "frontend/components"）
 * @param trashed 是否处于回收站中（软删除机制，非直接删除数据库记录）
 * @param trashedAt 移入回收站的时间戳
 */
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
    val folder: String = "",
    val trashed: Boolean = false,
    val trashedAt: Long? = null
) {
    /**
     * 【只读计算属性】界面展示用的显示标题。
     * 逻辑：若用户未指定标题，则截取文件名剥离扩展名后的前缀字符串展示。
     */
    val displayTitle: String
        get() = title.ifBlank {
            if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        }

    /**
     * 【只读计算属性】根据标题和类型自动推导默认合法文件名。
     * 使用正则表达式 `\s+` 将标题中的空白字符替换为下划线，避免产生包含非法空格的文件名。
     */
    val defaultFileName: String
        get() = if (title.isBlank()) "snippet${type.extension}" else "${title.replace("\\s+".toRegex(), "_")}${type.extension}"

    companion object {
        /**
         * 新建代码片段时自动生成包含时间的默认标题 (例如 "JavaScript · 07-25 10:30")。
         */
        fun generateDefaultTitle(type: SnippetType): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return "${type.displayName} · ${sdf.format(Date())}"
        }
    }
}



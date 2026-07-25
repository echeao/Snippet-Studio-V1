package com.feige.snippetstudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType

/**
 * [SnippetEntity] 是 Room 持久化数据库表 `snippets` 的映射实体数据类 (Database Entity)。
 *
 * 数据库与领域模型解耦：Room 中使用扁平化的 [SnippetEntity]（如 tags 以逗号分隔的字符串存储），
 * 并提供与业务领域模型 [Snippet] 互相转换的双向映射函数。
 *
 * @param id 主键 ID
 * @param type 类型代码字符串 (如 "html", "js")
 * @param title 代码片段标题
 * @param fileName 文件名
 * @param content 代码片段详细正文
 * @param tags 标签列表（以逗号分隔存储在数据库中，如 "Android,Kotlin"）
 * @param starred 是否收藏
 * @param createdAt 创建时间戳
 * @param updatedAt 修改时间戳
 * @param sizeBytes 字节数
 * @param folder 文件夹相对路径
 * @param trashed 是否处于回收站
 * @param trashedAt 移入回收站时间戳
 */
@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val fileName: String,
    val content: String,
    val tags: String = "",
    val starred: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val sizeBytes: Int,
    val folder: String = "",
    val trashed: Boolean = false,
    val trashedAt: Long? = null
) {
    /**
     * 将数据库持久化实体 [SnippetEntity] 转换为应用业务逻辑使用的领域模型 [Snippet]。
     *
     * 教学解析：
     * 字符串与 List 转换：SQLite 原生不支持直接存取 List 集合，此处将逗号分隔符连结的 `tags` 字符串（如 "UI, API"），
     * 通过 `split(",")` 拆分并进行 `trim()` 去空格，还原为 List<String> 对象。
     */
    fun toDomain(): Snippet {
        return Snippet(
            id = id,
            type = SnippetType.fromCode(type), // 将存储的平铺字符串代码转回安全的 SnippetType 枚举
            title = title,
            fileName = fileName,
            content = content,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
            starred = starred,
            createdAt = createdAt,
            updatedAt = updatedAt,
            sizeBytes = sizeBytes,
            folder = folder,
            trashed = trashed,
            trashedAt = trashedAt
        )
    }

    companion object {
        /**
         * 从业务领域模型 [Snippet] 映射生成适用于 Room 持久化保存的 [SnippetEntity]。
         *
         * 教学解析：
         * 使用 `joinToString(",")` 将 List 标签反向序列化拼接为单个 SQLite TEXT 文本字符串。
         */
        fun fromDomain(snippet: Snippet): SnippetEntity {
            return SnippetEntity(
                id = snippet.id,
                type = snippet.type.code,
                title = snippet.title,
                fileName = snippet.fileName,
                content = snippet.content,
                tags = snippet.tags.joinToString(","),
                starred = snippet.starred,
                createdAt = snippet.createdAt,
                updatedAt = snippet.updatedAt,
                sizeBytes = snippet.sizeBytes,
                folder = snippet.folder,
                trashed = snippet.trashed,
                trashedAt = snippet.trashedAt
            )
        }
    }
}



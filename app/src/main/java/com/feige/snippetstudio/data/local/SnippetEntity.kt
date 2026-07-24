package com.feige.snippetstudio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType

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
    fun toDomain(): Snippet {
        return Snippet(
            id = id,
            type = SnippetType.fromCode(type),
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

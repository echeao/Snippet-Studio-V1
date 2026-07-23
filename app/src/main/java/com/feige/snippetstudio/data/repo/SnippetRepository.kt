package com.feige.snippetstudio.data.repo

import android.content.Context
import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.data.local.SnippetEntity
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.util.LocalFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class SnippetRepository(
    private val snippetDao: SnippetDao,
    private val context: Context? = null
) {

    suspend fun syncWithLocalRepository(context: Context, repoTreeUriStr: String) = withContext(Dispatchers.IO) {
        LocalFileManager.syncRepositoryToDatabase(context, repoTreeUriStr, snippetDao)
    }

    fun observeActive(): Flow<List<Snippet>> = snippetDao.observeActive().map { list ->
        list.map { it.toDomain() }
    }

    fun observeStarred(): Flow<List<Snippet>> = snippetDao.observeStarred().map { list ->
        list.map { it.toDomain() }
    }

    fun observeTrashed(): Flow<List<Snippet>> = snippetDao.observeTrashed().map { list ->
        list.map { it.toDomain() }
    }

    fun observeByType(type: SnippetType): Flow<List<Snippet>> = snippetDao.observeByType(type.code).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getById(id: String): Snippet? {
        return snippetDao.byId(id)?.toDomain()
    }

    suspend fun create(
        type: SnippetType,
        initialContent: String? = null,
        initialTitle: String? = null,
        repoTreeUriStr: String = ""
    ): Snippet {
        val now = System.currentTimeMillis()
        val title = initialTitle ?: Snippet.generateDefaultTitle(type)
        val content = initialContent ?: Snippet.createDefaultContent(type)
        val fileName = if (title.isBlank()) "snippet${type.extension}" else "${title.take(20).replace("\\s+".toRegex(), "_")}${type.extension}"
        val sizeBytes = content.toByteArray(Charsets.UTF_8).size

        val snippet = Snippet(
            id = "s_${now}_${UUID.randomUUID().toString().take(4)}",
            type = type,
            title = title,
            fileName = fileName,
            content = content,
            createdAt = now,
            updatedAt = now,
            sizeBytes = sizeBytes
        )

        snippetDao.upsert(SnippetEntity.fromDomain(snippet))
        context?.let { ctx ->
            withContext(Dispatchers.IO) {
                LocalFileManager.writeSnippetToFile(ctx, snippet, repoTreeUriStr)
            }
        }
        return snippet
    }

    suspend fun saveOrUpdate(snippet: Snippet, repoTreeUriStr: String = "") {
        val now = System.currentTimeMillis()
        val sizeBytes = snippet.content.toByteArray(Charsets.UTF_8).size
        val updated = snippet.copy(
            updatedAt = now,
            sizeBytes = sizeBytes,
            fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
        )
        snippetDao.upsert(SnippetEntity.fromDomain(updated))
        context?.let { ctx ->
            withContext(Dispatchers.IO) {
                LocalFileManager.writeSnippetToFile(ctx, updated, repoTreeUriStr)
            }
        }
    }

    suspend fun toggleStar(id: String, currentStarred: Boolean) {
        snippetDao.setStar(id, !currentStarred)
    }

    suspend fun trash(id: String) {
        snippetDao.trash(id, System.currentTimeMillis())
    }

    suspend fun restore(id: String) {
        snippetDao.restore(id)
    }

    suspend fun purge(id: String, repoTreeUriStr: String = "") {
        val snippet = getById(id)
        if (snippet != null && context != null) {
            withContext(Dispatchers.IO) {
                LocalFileManager.deleteSnippetFile(context, snippet, repoTreeUriStr)
            }
        }
        snippetDao.purge(id)
    }

    suspend fun purgeExpired(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - (days * 24L * 3600L * 1000L)
        snippetDao.purgeExpired(cutoff)
    }

    suspend fun allForExport(): List<Snippet> {
        return snippetDao.allActiveSnapshot().map { it.toDomain() }
    }

    suspend fun activeCount(): Int {
        return snippetDao.activeCount()
    }
}

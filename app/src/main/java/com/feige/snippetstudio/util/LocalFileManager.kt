package com.feige.snippetstudio.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.data.local.SnippetEntity
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import java.io.File
import java.util.UUID

object LocalFileManager {
    private const val TAG = "LocalFileManager"

    /**
     * Get default local storage folder if SAF uri is empty
     */
    fun getDefaultRepoDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "snippets")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Scans physical files in either SAF document tree or default local directory,
     * and syncs them into Room database.
     */
    suspend fun syncRepositoryToDatabase(
        context: Context,
        repoTreeUriStr: String,
        snippetDao: SnippetDao
    ) {
        try {
            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    val files = docTree.listFiles()
                    for (doc in files) {
                        if (doc.isFile && doc.name != null && !doc.name!!.startsWith(".")) {
                            val name = doc.name!!
                            val type = SnippetType.fromFileName(name)
                            readAndSyncDocumentFile(context, doc, name, type, snippetDao)
                        }
                    }
                    return
                }
            }

            // Fallback to local app storage directory
            val localDir = getDefaultRepoDir(context)
            localDir.walkTopDown().forEach { file ->
                if (file.isFile && !file.name.startsWith(".")) {
                    val relativeParent = file.parentFile?.relativeToOrNull(localDir)?.path?.replace('\\', '/') ?: ""
                    val name = file.name
                    val type = SnippetType.fromFileName(name)
                    readAndSyncLocalFile(file, name, relativeParent, type, snippetDao)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing repository to database", e)
        }
    }

    private suspend fun readAndSyncDocumentFile(
        context: Context,
        doc: DocumentFile,
        fileName: String,
        type: SnippetType,
        snippetDao: SnippetDao
    ) {
        try {
            val content = context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""

            val title = fileName.substringBeforeLast(".")
            val lm = doc.lastModified()
            val now = if (lm > 0L) lm else System.currentTimeMillis()
            val existing = snippetDao.allActiveSnapshot().find { it.fileName == fileName || it.title == title }

            if (existing != null) {
                if (existing.content != content) {
                    val updated = existing.copy(
                        content = content,
                        sizeBytes = content.toByteArray(Charsets.UTF_8).size,
                        updatedAt = now
                    )
                    snippetDao.upsert(updated)
                }
            } else {
                val snippet = Snippet(
                    id = "s_${now}_${UUID.randomUUID().toString().take(4)}",
                    type = type,
                    title = title,
                    fileName = fileName,
                    content = content,
                    createdAt = now,
                    updatedAt = now,
                    sizeBytes = content.toByteArray(Charsets.UTF_8).size
                )
                snippetDao.upsert(SnippetEntity.fromDomain(snippet))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading SAF document $fileName", e)
        }
    }

    private suspend fun readAndSyncLocalFile(
        file: File,
        fileName: String,
        folder: String,
        type: SnippetType,
        snippetDao: SnippetDao
    ) {
        try {
            val content = file.readText(Charsets.UTF_8)
            val title = fileName.substringBeforeLast(".")
            val now = if (file.lastModified() > 0) file.lastModified() else System.currentTimeMillis()
            val existing = snippetDao.allActiveSnapshot().find { it.fileName == fileName || it.title == title }

            if (existing != null) {
                if (existing.content != content || existing.folder != folder) {
                    val updated = existing.copy(
                        content = content,
                        folder = folder,
                        sizeBytes = content.toByteArray(Charsets.UTF_8).size,
                        updatedAt = now
                    )
                    snippetDao.upsert(updated)
                }
            } else {
                val snippet = Snippet(
                    id = "s_${now}_${UUID.randomUUID().toString().take(4)}",
                    type = type,
                    title = title,
                    fileName = fileName,
                    folder = folder,
                    content = content,
                    createdAt = now,
                    updatedAt = now,
                    sizeBytes = content.toByteArray(Charsets.UTF_8).size
                )
                snippetDao.upsert(SnippetEntity.fromDomain(snippet))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading local file $fileName", e)
        }
    }

    /**
     * Saves or updates physical file in either SAF document tree or default local directory.
     */
    fun writeSnippetToFile(
        context: Context,
        snippet: Snippet,
        repoTreeUriStr: String
    ) {
        try {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName

            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    var targetDoc = docTree.findFile(fileName)
                    if (targetDoc == null) {
                        val mimeType = when (snippet.type) {
                            SnippetType.HTML -> "text/html"
                            SnippetType.JS -> "text/javascript"
                            SnippetType.MARKDOWN -> "text/markdown"
                            SnippetType.PROMPT -> "text/plain"
                        }
                        targetDoc = docTree.createFile(mimeType, fileName)
                    }
                    if (targetDoc != null) {
                        context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { stream ->
                            stream.write(snippet.content.toByteArray(Charsets.UTF_8))
                        }
                        return
                    }
                }
            }

            // Fallback to local app storage directory with folder support
            val localDir = getDefaultRepoDir(context)
            val targetDir = if (snippet.folder.isBlank()) localDir else File(localDir, snippet.folder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing snippet file to disk", e)
        }
    }

    /**
     * Deletes physical file when snippet is purged.
     */
    fun deleteSnippetFile(
        context: Context,
        snippet: Snippet,
        repoTreeUriStr: String
    ) {
        try {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName

            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    val targetDoc = docTree.findFile(fileName)
                    targetDoc?.delete()
                    return
                }
            }

            val localDir = getDefaultRepoDir(context)
            val targetDir = if (snippet.folder.isBlank()) localDir else File(localDir, snippet.folder)
            val file = File(targetDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting snippet file from disk", e)
        }
    }
}

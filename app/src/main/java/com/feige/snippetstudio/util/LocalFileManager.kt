package com.feige.snippetstudio.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.feige.snippetstudio.data.local.FolderDao
import com.feige.snippetstudio.data.local.FolderEntity
import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.data.local.SnippetEntity
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import java.io.File
import java.util.UUID

/**
 * [LocalFileManager] 负责 Android 本地存储与 SAF (Storage Access Framework) 授权目录的文件交互工具类。
 *
 * 关键特性：
 * 1. 优先使用用户通过 SAF 授权的外部文件夹目录 (DocumentFile Tree URI)。
 * 2. 当无 SAF 授权时自动降级 fallback 使用 App 私有扩展存储 `getExternalFilesDir/snippets` 文件夹。
 * 3. 实现物理文件系统与 Room 数据库之间的双向同步与增删改落盘。
 * 4. 支持文件夹（包含空文件夹）在 SQLite 数据库与物理磁盘之间的双向联动同步。
 */
object LocalFileManager {
    private const val TAG = "LocalFileManager"

    /**
     * 获取默认的内部应用私有存储目录 `snippets`。
     *
     * @param context 上下文
     * @return 准备就绪的 [File] 目录对象
     */
    fun getDefaultRepoDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "snippets")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 扫描物理文件系统（SAF DocumentTree 或默认应用私有存储），将其同步至 Room 数据库。
     *
     * 教学解析：
     * 不仅扫描带代码文件的目录，还递归扫描空文件夹，并将其作为 [FolderEntity] 存入 [folderDao]，
     * 实现物理空文件夹在应用界面树状视图中的 100% 双向同步显示。
     *
     * @param context 上下文
     * @param repoTreeUriStr SAF 授权目录 URI 字符串
     * @param snippetDao 代码片段数据库 DAO
     * @param folderDao 文件夹数据库 DAO（可选）
     */
    suspend fun syncRepositoryToDatabase(
        context: Context,
        repoTreeUriStr: String,
        snippetDao: SnippetDao,
        folderDao: FolderDao? = null
    ) {
        try {
            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    val files = docTree.listFiles()
                    for (doc in files) {
                        if (doc.isDirectory && doc.name != null && !doc.name!!.startsWith(".")) {
                            // SAF 模式下扫描到文件夹，存入 folderDao
                            val folderPath = doc.name!!
                            folderDao?.upsert(FolderEntity(path = folderPath))
                        } else if (doc.isFile && doc.name != null && !doc.name!!.startsWith(".")) {
                            val name = doc.name!!
                            val type = SnippetType.fromFileName(name)
                            readAndSyncDocumentFile(context, doc, name, type, snippetDao)
                        }
                    }
                    return
                }
            }

            // 降级使用应用本地文件私有目录，递归扫描文件与文件夹
            val localDir = getDefaultRepoDir(context)
            localDir.walkTopDown().forEach { file ->
                if (file.isDirectory && file != localDir && !file.name.startsWith(".")) {
                    val relativePath = file.relativeToOrNull(localDir)?.path?.replace('\\', '/') ?: ""
                    if (relativePath.isNotBlank()) {
                        val parent = file.parentFile?.relativeToOrNull(localDir)?.path?.replace('\\', '/') ?: ""
                        folderDao?.upsert(FolderEntity(path = relativePath, parentPath = parent))
                    }
                } else if (file.isFile && !file.name.startsWith(".")) {
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

    /**
     * 在物理磁盘或 SAF 授权目录下显式创建物理子文件夹。
     * 响应用户指示：保证应用内新建文件夹与物理文件管理器 100% 双向一致。
     *
     * @param context 上下文
     * @param folderPath 待创建的相对文件夹路径（如 "components/ui"）
     * @param repoTreeUriStr SAF 授权目录 URI 字符串
     */
    fun createPhysicalFolder(
        context: Context,
        folderPath: String,
        repoTreeUriStr: String
    ) {
        if (folderPath.isBlank()) return
        try {
            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    val parts = folderPath.split("/").filter { it.isNotBlank() }
                    var currentDoc: DocumentFile? = docTree
                    for (part in parts) {
                        val parent = currentDoc ?: break
                        val existing = parent.findFile(part)
                        currentDoc = if (existing != null && existing.isDirectory) {
                            existing
                        } else {
                            parent.createDirectory(part)
                        }
                    }
                    return
                }
            }

            // 本地文件系统模式：物理递归创建子目录
            val localDir = getDefaultRepoDir(context)
            val targetDir = File(localDir, folderPath)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating physical folder: $folderPath", e)
        }
    }

    /**
     * 读取单个 SAF DocumentFile 内容并写入/更新 Room 数据库。
     */
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

    /**
     * 读取单个应用私有物理文件内容并写入/更新 Room 数据库。
     */
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
     * 将代码片段 [Snippet] 实时保存/写入至 SAF 目录或应用私有物理文件。
     */
    /**
     * 将代码片段 [Snippet] 实时保存/写入至 SAF 授权目录或应用私有物理文件。
     *
     * 教学解析：
     * 1. SAF 模式 (Storage Access Framework): 使用 `DocumentFile.fromTreeUri` 构建授权目录。
     *    调用 `contentResolver.openOutputStream(uri, "wt")` ("wt" = write truncate)，清空现有内容并写入 UTF-8 字节。
     * 2. 降级本地 File 模式 (Fallback Internal File): 当用户未设置外部授权文件夹时，
     *    自动降级写入 `context.getExternalFilesDir(null)/snippets` 私有沙盒，无需申请危险的 READ/WRITE_EXTERNAL_STORAGE 权限。
     */
    fun writeSnippetToFile(
        context: Context,
        snippet: Snippet,
        repoTreeUriStr: String
    ) {
        try {
            // 计算合法文件名 (若为空则取 defaultFileName)
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName

            // ===== 路径分支 1: 存在有效 SAF 目录 URI =====
            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    var targetDoc = docTree.findFile(fileName)
                    // 若目标文件不存在，则在 SAF 树中依据 MIME 类型新建 DocumentFile
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
                        // 打开 ContentResolver 写入流并以 UTF-8 格式覆写文件
                        context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { stream ->
                            stream.write(snippet.content.toByteArray(Charsets.UTF_8))
                        }
                        return
                    }
                }
            }

            // ===== 路径分支 2: 降级使用内部 App 物理存储目录 =====
            val localDir = getDefaultRepoDir(context)
            // 支持子文件夹路径 (例如 folder = "components/ui"，若不存在则先 mkdirs)
            val targetDir = if (snippet.folder.isBlank()) localDir else File(localDir, snippet.folder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing snippet file to disk", e)
        }
    }


    /**
     * 当彻底物理删除代码片段时，从磁盘删除对应的物理文件。
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


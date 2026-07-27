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
    private const val TRASH_DIR = ".trash"

    /**
     * 在 SAF DocumentTree 根节点下递归查找相对路径指定的子目录。
     *
     * @param docTree SAF 根目录 DocumentFile
     * @param folderPath 相对文件夹路径 (如 "components/button")
     * @return 找到的子目录 [DocumentFile]，若不存在则返回 null
     */
    fun findSubFolder(docTree: DocumentFile, folderPath: String): DocumentFile? {
        val cleanPath = folderPath.trim().trim('/')
        if (cleanPath.isBlank()) return docTree
        val parts = cleanPath.split("/").filter { it.isNotBlank() }
        var currentDoc: DocumentFile? = docTree
        for (part in parts) {
            val parent = currentDoc ?: return null
            val existing = parent.findFile(part)
            if (existing != null && existing.isDirectory) {
                currentDoc = existing
            } else {
                return null
            }
        }
        return currentDoc
    }

    /**
     * 在 SAF DocumentTree 根节点下递归查找或创建相对路径指定的子目录。
     *
     * @param docTree SAF 根目录 DocumentFile
     * @param folderPath 相对文件夹路径 (如 "components/button")
     * @return 创建或找到的子目录 [DocumentFile]
     */
    fun findOrCreateSubFolder(docTree: DocumentFile, folderPath: String): DocumentFile? {
        val cleanPath = folderPath.trim().trim('/')
        if (cleanPath.isBlank()) return docTree
        val parts = cleanPath.split("/").filter { it.isNotBlank() }
        var currentDoc: DocumentFile? = docTree
        for (part in parts) {
            val parent = currentDoc ?: return null
            val existing = parent.findFile(part)
            currentDoc = if (existing != null && existing.isDirectory) {
                existing
            } else {
                parent.createDirectory(part)
            }
        }
        return currentDoc
    }

    /**
     * 软删除时将物理文件移入隐藏回收站目录 `.trash/`。
     * 支持 SAF 与本地磁盘两种模式，自动保留文件相对子目录。
     */
    fun moveSnippetToTrash(
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
                    val trashDir = docTree.findFile(TRASH_DIR)?.takeIf { it.isDirectory }
                        ?: docTree.createDirectory(TRASH_DIR)
                    val sourceDir = findSubFolder(docTree, snippet.folder) ?: docTree
                    val sourceDoc = sourceDir.findFile(fileName)
                    if (sourceDoc != null && trashDir != null) {
                        val targetName = resolveTrashName(trashDir, fileName)
                        // SAF 模式下使用流拷贝+删除，确保跨目录正确移动，避免在根目录留存并产生 (1) 副本
                        copyAndDeleteDoc(context, sourceDoc, trashDir, targetName)
                    }
                    return
                }
            }

            val localDir = getDefaultRepoDir(context)
            val trashDir = File(localDir, TRASH_DIR).apply { if (!exists()) mkdirs() }
            val sourceDir = if (snippet.folder.isBlank()) localDir else File(localDir, snippet.folder)
            val sourceFile = File(sourceDir, fileName)
            if (sourceFile.exists()) {
                val targetFile = File(trashDir, resolveTrashNameLocal(trashDir, fileName))
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error moving snippet to trash: ${snippet.fileName}", e)
        }
    }

    /**
     * 从隐藏回收站目录 `.trash/` 恢复物理文件到原路径（含原子目录）。
     */
    fun restoreSnippetFromTrash(
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
                    val trashDir = docTree.findFile(TRASH_DIR) ?: return
                    val trashedDoc = findTrashedDoc(trashDir, fileName)
                    if (trashedDoc != null) {
                        val targetDir = findOrCreateSubFolder(docTree, snippet.folder) ?: docTree
                        copyAndDeleteDoc(context, trashedDoc, targetDir, fileName)
                    }
                    return
                }
            }

            val localDir = getDefaultRepoDir(context)
            val trashDir = File(localDir, TRASH_DIR)
            if (!trashDir.exists()) return
            val trashedFile = findTrashedFile(trashDir, fileName)
            if (trashedFile != null) {
                val targetDir = if (snippet.folder.isBlank()) localDir
                    else File(localDir, snippet.folder).apply { if (!exists()) mkdirs() }
                trashedFile.copyTo(File(targetDir, fileName), overwrite = true)
                trashedFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring snippet from trash: ${snippet.fileName}", e)
        }
    }

    /**
     * 从 `.trash/` 目录中彻底物理删除文件。
     */
    fun purgeFromTrash(
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
                    val trashDir = docTree.findFile(TRASH_DIR) ?: return
                    findTrashedDoc(trashDir, fileName)?.delete()
                    return
                }
            }

            val localDir = getDefaultRepoDir(context)
            val trashDir = File(localDir, TRASH_DIR)
            if (!trashDir.exists()) return
            findTrashedFile(trashDir, fileName)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error purging from trash: ${snippet.fileName}", e)
        }
    }

    private fun resolveTrashName(trashDir: DocumentFile, fileName: String): String {
        if (trashDir.findFile(fileName) == null) return fileName
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        return "${base}_${System.currentTimeMillis()}.$ext"
    }

    private fun resolveTrashNameLocal(trashDir: File, fileName: String): String {
        if (!File(trashDir, fileName).exists()) return fileName
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        return "${base}_${System.currentTimeMillis()}.$ext"
    }

    private fun findTrashedDoc(trashDir: DocumentFile, originalName: String): DocumentFile? {
        trashDir.findFile(originalName)?.let { return it }
        val base = originalName.substringBeforeLast(".")
        return trashDir.listFiles().firstOrNull {
            it.isFile && it.name?.startsWith(base) == true
        }
    }

    private fun findTrashedFile(trashDir: File, originalName: String): File? {
        val exact = File(trashDir, originalName)
        if (exact.exists()) return exact
        val base = originalName.substringBeforeLast(".")
        return trashDir.listFiles()?.firstOrNull {
            it.isFile && it.name.startsWith(base)
        }
    }

    /**
     * 将源 DocumentFile 拷贝至目标 SAF 目录并删除源文件。
     *
     * 关键预清理逻辑（防御问题 D）：
     * 在调用 `targetDir.createFile` 之前，先显式检查并删除目标目录下已存在的同名文件，
     * 彻底解决 SAF Provider 在文件冲突时自动追加 `(1)` 重名副本的问题。
     */
    private fun copyAndDeleteDoc(
        context: Context,
        source: DocumentFile,
        targetDir: DocumentFile,
        targetName: String
    ) {
        // 显式预清理目标同名残余文件，防止产生 (1) 副本
        val existing = targetDir.findFile(targetName)
        existing?.delete()

        val mimeType = source.type ?: "application/octet-stream"
        val newDoc = targetDir.createFile(mimeType, targetName) ?: return
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            context.contentResolver.openOutputStream(newDoc.uri, "wt")?.use { output ->
                input.copyTo(output)
            }
        }
        source.delete()
    }

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
     * 支持 SAF 递归深层子目录扫描，保留子文件夹 [FolderEntity] 层级结构与 Snippet 的 [folder] 字段。
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
            // 预加载回收站快照，同步时跳过已被用户删除的文件，避免重启后“复活”
            val trashedSnapshots = snippetDao.allTrashedSnapshot()

            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    // SAF 模式递归遍历子目录
                    scanSafDirectoryRecursive(context, docTree, "", snippetDao, folderDao, trashedSnapshots)

                    // ===== 反向清理：将物理文件已被外部删除的数据库记录移入回收站 =====
                    cleanupMissingPhysicalFiles(docTree, snippetDao, folderDao)

                    // ===== 去重保护：清理因历史匹配缺陷产生的重复记录 =====
                    deduplicateDatabase(snippetDao)
                    return
                }
                return
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
                    val title = name.substringBeforeLast(".")
                    if (trashedSnapshots.any { it.fileName == name || it.title == title }) return@forEach
                    val type = SnippetType.fromFileName(name)
                    readAndSyncLocalFile(file, name, relativeParent, type, snippetDao)
                }
            }

            // ===== 反向清理：将物理文件已被外部删除的数据库记录移入回收站 =====
            cleanupMissingLocalFiles(localDir, snippetDao, folderDao)

            // ===== 去重保护：清理因历史匹配缺陷产生的重复记录 =====
            deduplicateDatabase(snippetDao)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing repository to database", e)
        }
    }

    /**
     * 递归扫描 SAF DocumentTree 授权目录，解析多层级子文件夹与物理文件。
     */
    private suspend fun scanSafDirectoryRecursive(
        context: Context,
        currentDir: DocumentFile,
        relativeFolder: String,
        snippetDao: SnippetDao,
        folderDao: FolderDao?,
        trashedSnapshots: List<SnippetEntity>
    ) {
        val files = currentDir.listFiles()
        for (doc in files) {
            val docName = doc.name ?: continue
            if (docName.startsWith(".")) continue

            if (doc.isDirectory) {
                val folderPath = if (relativeFolder.isBlank()) docName else "$relativeFolder/$docName"
                val parentPath = relativeFolder
                folderDao?.upsert(FolderEntity(path = folderPath, parentPath = parentPath))
                // 递归扫描深层 SAF 子目录
                scanSafDirectoryRecursive(context, doc, folderPath, snippetDao, folderDao, trashedSnapshots)
            } else if (doc.isFile) {
                val title = docName.substringBeforeLast(".")
                if (trashedSnapshots.any { (it.fileName == docName || it.title == title) && (it.folder == relativeFolder || relativeFolder.isBlank()) }) continue
                val type = SnippetType.fromFileName(docName)
                readAndSyncDocumentFile(context, doc, docName, relativeFolder, type, snippetDao)
            }
        }
    }

    /**
     * SAF 模式反向清理：检查数据库中的活动记录对应的物理文件是否存在于 SAF 目录树中。
     * 若物理文件已被外部删除，则将对应数据库记录移入回收站。
     */
    private suspend fun cleanupMissingPhysicalFiles(
        docTree: DocumentFile,
        snippetDao: SnippetDao,
        folderDao: FolderDao?
    ) {
        try {
            val activeSnippets = snippetDao.allActiveSnapshot()
            val now = System.currentTimeMillis()

            activeSnippets.forEach { entity ->
                val fileName = entity.fileName.ifBlank {
                    entity.toDomain().defaultFileName
                }
                val targetDir = findSubFolder(docTree, entity.folder)
                if (targetDir == null || targetDir.findFile(fileName) == null) {
                    snippetDao.trash(entity.id, now)
                    Log.d(TAG, "Cleanup: trashed missing SAF file '${entity.folder}/$fileName'")
                }
            }

            // 清理物理目录已不存在的文件夹记录
            folderDao?.let { dao ->
                val allFolders = dao.allSnapshot()
                allFolders.forEach { folder ->
                    if (findSubFolder(docTree, folder.path) == null) {
                        dao.deleteByPath(folder.path)
                        Log.d(TAG, "Cleanup: removed missing SAF folder '${folder.path}'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during SAF cleanup", e)
        }
    }

    /**
     * 本地文件模式反向清理：检查数据库中的活动记录对应的物理文件是否存在于本地目录中。
     * 若物理文件已被外部删除，则将对应数据库记录移入回收站。
     */
    private suspend fun cleanupMissingLocalFiles(
        localDir: File,
        snippetDao: SnippetDao,
        folderDao: FolderDao?
    ) {
        try {
            val activeSnippets = snippetDao.allActiveSnapshot()
            val now = System.currentTimeMillis()

            activeSnippets.forEach { entity ->
                val fileName = entity.fileName.ifBlank {
                    entity.toDomain().defaultFileName
                }
                val targetDir = if (entity.folder.isBlank()) localDir else File(localDir, entity.folder)
                val file = File(targetDir, fileName)
                if (!file.exists()) {
                    snippetDao.trash(entity.id, now)
                    Log.d(TAG, "Cleanup: trashed missing local file '${entity.folder}/$fileName'")
                }
            }

            // 清理物理目录已不存在的文件夹记录
            folderDao?.let { dao ->
                val allFolders = dao.allSnapshot()
                allFolders.forEach { folder ->
                    val dir = File(localDir, folder.path)
                    if (!dir.exists() || !dir.isDirectory) {
                        dao.deleteByPath(folder.path)
                        Log.d(TAG, "Cleanup: removed missing local folder '${folder.path}'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during local cleanup", e)
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
                    findOrCreateSubFolder(docTree, folderPath)
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
     * 读取单个 SAF DocumentFile 内容并写入/更新 Room 数据库，透传 folder 目录归属。
     */
    private suspend fun readAndSyncDocumentFile(
        context: Context,
        doc: DocumentFile,
        fileName: String,
        folder: String,
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
            val existing = findExistingSnippet(snippetDao, fileName, title, folder)

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
            val existing = findExistingSnippet(snippetDao, fileName, title, folder)

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
     * 多策略匹配已有数据库记录，防止重启同步时产生重复条目。
     */
    private suspend fun findExistingSnippet(
        snippetDao: SnippetDao,
        fileName: String,
        derivedTitle: String,
        folder: String? = null
    ): SnippetEntity? {
        val allActive = snippetDao.allActiveSnapshot()

        // 策略 1: fileName 精确匹配
        val byFileName = allActive.find { entity ->
            entity.fileName == fileName && (folder == null || entity.folder == folder || entity.folder.isBlank())
        }
        if (byFileName != null) return byFileName

        // 策略 2: 通过 DB 记录的 title 推导 defaultFileName 进行匹配
        val byDefaultFileName = allActive.find { entity ->
            val ext = SnippetType.fromCode(entity.type).extension
            val expectedFileName = if (entity.title.isBlank()) {
                "snippet${ext}"
            } else {
                "${entity.title.replace("\\s+".toRegex(), "_")}${ext}"
            }
            val expectedFileNameTruncated = if (entity.title.isBlank()) {
                "snippet${ext}"
            } else {
                "${entity.title.take(20).replace("\\s+".toRegex(), "_")}${ext}"
            }
            (expectedFileName == fileName || expectedFileNameTruncated == fileName) &&
                (folder == null || entity.folder == folder || entity.folder.isBlank())
        }
        if (byDefaultFileName != null) return byDefaultFileName

        // 策略 3: title 规范化匹配（将空格和下划线统一为同一字符后比较）
        val normalizedDerived = derivedTitle.replace("_", " ").replace("\\s+".toRegex(), " ").trim().lowercase()
        val byNormalizedTitle = allActive.find { entity ->
            val normalizedDbTitle = entity.title.replace("_", " ").replace("\\s+".toRegex(), " ").trim().lowercase()
            (normalizedDbTitle == normalizedDerived ||
             normalizedDbTitle.take(20).trim() == normalizedDerived) &&
                (folder == null || entity.folder == folder || entity.folder.isBlank())
        }
        if (byNormalizedTitle != null) return byNormalizedTitle

        return null
    }

    /**
     * 同步完成后执行去重清理：检测数据库中 fileName + folder 完全相同的重复记录，
     * 仅保留 updatedAt 最新的一条，将其余重复项移入回收站。
     */
    private suspend fun deduplicateDatabase(snippetDao: SnippetDao) {
        try {
            val allActive = snippetDao.allActiveSnapshot()
            val grouped = allActive.groupBy { "${it.folder}/${it.fileName}" }
            val now = System.currentTimeMillis()

            for ((_, entities) in grouped) {
                if (entities.size > 1) {
                    val sorted = entities.sortedByDescending { it.updatedAt }
                    sorted.drop(1).forEach { duplicate ->
                        snippetDao.trash(duplicate.id, now)
                        Log.d(TAG, "Dedup: trashed duplicate '${duplicate.fileName}' (id=${duplicate.id})")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during deduplication", e)
        }
    }

    /**
     * 将代码片段 [Snippet] 实时保存/写入至 SAF 授权目录或应用私有物理文件。
     */
    fun writeSnippetToFile(
        context: Context,
        snippet: Snippet,
        repoTreeUriStr: String
    ) {
        try {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName

            // ===== 路径分支 1: 存在有效 SAF 目录 URI =====
            if (repoTreeUriStr.isNotBlank()) {
                val treeUri = Uri.parse(repoTreeUriStr)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.exists() && docTree.isDirectory) {
                    val targetDir = findOrCreateSubFolder(docTree, snippet.folder) ?: docTree
                    var targetDoc = targetDir.findFile(fileName)
                    // 若目标文件不存在，则在 SAF 树中依据 MIME 类型新建 DocumentFile
                    if (targetDoc == null) {
                        val mimeType = when (snippet.type) {
                            SnippetType.HTML -> "text/html"
                            SnippetType.JS -> "text/javascript"
                            SnippetType.MARKDOWN -> "text/markdown"
                            SnippetType.PROMPT -> "text/plain"
                            SnippetType.GENERAL -> "text/plain"
                        }
                        targetDoc = targetDir.createFile(mimeType, fileName)
                    }
                    if (targetDoc != null) {
                        context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { stream ->
                            stream.write(snippet.content.toByteArray(Charsets.UTF_8))
                        }
                        return
                    }
                }
            }

            // ===== 路径分支 2: 降级使用内部 App 物理存储目录 =====
            val localDir = getDefaultRepoDir(context)
            val targetDir = if (snippet.folder.isBlank()) localDir else File(localDir, snippet.folder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing snippet file to disk", e)
        }
    }

    /**
     * 在 SAF 目录中容错查找指定物理名称的文件。
     *
     * 容错匹配防线：
     * 1. 优先以精准 [fileName] 匹配。
     * 2. 其次以去除非法字符后的规范名称匹配。
     * 3. 最后剥离扩展名匹配 display name（解决某些 SAF Provider 自动追加 .txt 的歧义）。
     */
    fun findDocFileByName(targetDir: DocumentFile, fileName: String): DocumentFile? {
        targetDir.findFile(fileName)?.let { return it }
        val baseName = fileName.substringBeforeLast('.')
        val sanitizedBase = baseName.replace("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]".toRegex(), "_")
        return targetDir.listFiles().firstOrNull { doc ->
            if (!doc.isFile) return@firstOrNull false
            val docName = doc.name ?: return@firstOrNull false
            docName == fileName || docName.startsWith(baseName) || docName.startsWith(sanitizedBase)
        }
    }

    /**
     * 当彻底物理删除代码片段或重命名改改旧文件时，从磁盘删除对应的物理文件。
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
                    val targetDir = findSubFolder(docTree, snippet.folder) ?: docTree
                    val targetDoc = findDocFileByName(targetDir, fileName)
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


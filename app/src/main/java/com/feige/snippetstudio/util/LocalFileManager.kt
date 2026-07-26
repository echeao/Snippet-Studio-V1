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
     * 软删除时将物理文件移入隐藏回收站目录 `.trash/`。
     * 若目标已存在同名文件则追加时间戳后缀避免覆盖。
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
                    val sourceDoc = docTree.findFile(fileName)
                    if (sourceDoc != null && trashDir != null) {
                        val targetName = resolveTrashName(trashDir, fileName)
                        val moved = sourceDoc.renameTo(targetName)
                        if (!moved) {
                            copyAndDeleteDoc(context, sourceDoc, trashDir, targetName)
                        }
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
     * 从隐藏回收站目录 `.trash/` 恢复物理文件到原路径。
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
                        val targetDir = if (snippet.folder.isBlank()) docTree
                            else docTree.findFile(snippet.folder) ?: docTree
                        val restored = trashedDoc.renameTo(fileName)
                        if (!restored) {
                            copyAndDeleteDoc(context, trashedDoc, targetDir, fileName)
                        }
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

    private fun copyAndDeleteDoc(
        context: Context,
        source: DocumentFile,
        targetDir: DocumentFile,
        targetName: String
    ) {
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
            // 预加载回收站快照，同步时跳过已被用户删除的文件，避免重启后“复活”
            val trashedSnapshots = snippetDao.allTrashedSnapshot()

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
                            val title = name.substringBeforeLast(".")
                            // 跳过回收站中已存在的文件（用户已删除，不应重新导入）
                            if (trashedSnapshots.any { it.fileName == name || it.title == title }) continue
                            val type = SnippetType.fromFileName(name)
                            readAndSyncDocumentFile(context, doc, name, type, snippetDao)
                        }
                    }

                    // ===== 反向清理：将物理文件已被外部删除的数据库记录移入回收站 =====
                    cleanupMissingPhysicalFiles(docTree, snippetDao, folderDao)

                    // ===== 去重保护：清理因历史匹配缺陷产生的重复记录 =====
                    deduplicateDatabase(snippetDao)
                    return
                }
                // SAF 目录不可用时不执行清理（可能是临时不可用，不能误删）
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
                    // 跳过回收站中已存在的文件（用户已删除，不应重新导入）
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
     * SAF 模式反向清理：检查数据库中的活动记录对应的物理文件是否存在于 SAF 目录中。
     * 若物理文件已被外部删除，则将对应数据库记录移入回收站。
     *
     * 注意：SAF 模式下文件以扁平方式存储在根目录，不检查子文件夹路径。
     * 仅当 SAF 目录确认可访问时才执行清理，避免目录临时不可用时误删记录。
     */
    private suspend fun cleanupMissingPhysicalFiles(
        docTree: DocumentFile,
        snippetDao: SnippetDao,
        folderDao: FolderDao?
    ) {
        try {
            val activeSnippets = snippetDao.allActiveSnapshot()
            val now = System.currentTimeMillis()

            // SAF 模式下文件存储在根目录，按文件名检查是否存在
            activeSnippets.forEach { entity ->
                val fileName = entity.fileName.ifBlank {
                    entity.toDomain().defaultFileName
                }
                if (docTree.findFile(fileName) == null) {
                    snippetDao.trash(entity.id, now)
                    Log.d(TAG, "Cleanup: trashed missing SAF file '${fileName}'")
                }
            }

            // 清理物理目录已不存在的文件夹记录
            folderDao?.let { dao ->
                val allFolders = dao.allSnapshot()
                allFolders.forEach { folder ->
                    // SAF 模式只支持一级目录
                    val topDir = folder.path.split("/").firstOrNull() ?: folder.path
                    if (docTree.findFile(topDir) == null) {
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
     *
     * 匹配策略（防止重启后产生重复记录）：
     * 1. 优先按 fileName 精确匹配
     * 2. 其次按 defaultFileName 推导匹配（兼容 title 含空格/截断的情况）
     * 3. 最后按 title 的规范化形式（去空格、下划线统一）模糊匹配
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
            val existing = findExistingSnippet(snippetDao, fileName, title)

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
     *
     * 匹配策略（防止重启后产生重复记录）：
     * 1. 优先按 fileName + folder 精确匹配
     * 2. 其次按 defaultFileName 推导匹配
     * 3. 最后按 title 规范化形式模糊匹配
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
     *
     * 匹配优先级：
     * 1. fileName 精确匹配（+ folder 匹配，若指定）
     * 2. 通过 DB 记录的 title 推导 defaultFileName 与物理 fileName 比对
     * 3. 将 title 规范化后（空格/下划线统一、忽略大小写）进行模糊匹配
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
        // 解决 create() 中 title.take(20) 截断 + 空格替换下划线后 fileName 与原始 title 不一致的问题
        val byDefaultFileName = allActive.find { entity ->
            val ext = SnippetType.fromCode(entity.type).extension // ".md", ".js" 等
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
            // 匹配完整 title 或 title 的前 20 字符（兼容 take(20) 截断）
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
                    // 保留 updatedAt 最大的（最新的），其余移入回收站
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


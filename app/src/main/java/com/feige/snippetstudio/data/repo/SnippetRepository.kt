package com.feige.snippetstudio.data.repo

import android.content.Context
import com.feige.snippetstudio.data.local.FolderDao
import com.feige.snippetstudio.data.local.FolderEntity
import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.data.local.SnippetEntity
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.util.LocalFileManager
import com.feige.snippetstudio.util.SnippetTemplateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

import com.feige.snippetstudio.data.git.GitManager

/**
 * [SnippetRepository] 是代码片段的核心业务数据仓库类。
 *
 * 架构中心作用：
 * 采用 Repository 模式统筹三方数据源联动：
 * 1. **Room 本地 SQLite 数据库**：持久化元数据与本地代码片段，支持 Flow 响应式实时观察。
 * 2. **Android SAF 本地目录**：调用 [LocalFileManager] 将文件与文件夹同步保存到用户指定的外部存储。
 * 3. **Git 沙盒仓与远程仓库**：调用 [GitManager] 将变动落盘到 Git 本地沙盒，为 Commit / Push / Pull 做准备。
 *
 * @param snippetDao Room 数据库代码片段 DAO
 * @param folderDao Room 数据库文件夹持久化 DAO（可选）
 * @param context 应用 Context（可选，用于 SAF 文件写入与 Assets 模板读取）
 * @param gitManager Git 版本控制管理器（可选，用于 Git 仓增删改）
 */
class SnippetRepository(
    private val snippetDao: SnippetDao,
    private val folderDao: FolderDao? = null,
    private val context: Context? = null,
    private val gitManager: GitManager? = null
) {

    /** 暴露 SnippetDao 供 SyncEngine 等内部组件使用 */
    fun getSnippetDao(): SnippetDao = snippetDao

    /**
     * 将外部授权的 SAF 目录或本地私有存储中的物理文件与文件夹全量扫描并同步更新至 Room 数据库。
     */
    suspend fun syncWithLocalRepository(context: Context, repoTreeUriStr: String) = withContext(Dispatchers.IO) {
        LocalFileManager.syncRepositoryToDatabase(context, repoTreeUriStr, snippetDao, folderDao)
    }

    /**
     * 响应式观察全量已持久化的文件夹列表。
     */
    fun observeFolders(): Flow<List<FolderEntity>> = folderDao?.observeAll() ?: flowOf(emptyList())

    /**
     * 显式创建新文件夹。
     * 依据双向一致性原则：同步在 Room SQLite 的 `folders` 表中新增记录，并在本地物理磁盘/SAF 构建物理目录。
     *
     * @param folderPath 相对文件夹路径 (如 "components/button")
     * @param repoTreeUriStr SAF 授权目录 URI 字符串
     */
    suspend fun createFolder(folderPath: String, repoTreeUriStr: String = "") = withContext(Dispatchers.IO) {
        val cleanPath = folderPath.trim().trim('/')
        if (cleanPath.isBlank()) return@withContext

        val parentPath = if (cleanPath.contains("/")) cleanPath.substringBeforeLast("/") else ""
        val entity = FolderEntity(path = cleanPath, parentPath = parentPath)
        folderDao?.upsert(entity)

        context?.let { ctx ->
            LocalFileManager.createPhysicalFolder(ctx, cleanPath, repoTreeUriStr)
        }
    }

    /**
     * 响应式观察未入回收站的活动代码片段列表 (数据库实体反解析为领域模型 [Snippet])。
     */
    fun observeActive(): Flow<List<Snippet>> = snippetDao.observeActive().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * 响应式观察星标 (收藏) 的代码片段列表。
     */
    fun observeStarred(): Flow<List<Snippet>> = snippetDao.observeStarred().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * 响应式观察回收站中的代码片段列表。
     */
    fun observeTrashed(): Flow<List<Snippet>> = snippetDao.observeTrashed().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * 根据 [SnippetType] 类型（HTML, JS, Markdown, Prompt）过滤观察代码片段。
     */
    fun observeByType(type: SnippetType): Flow<List<Snippet>> = snippetDao.observeByType(type.code).map { list ->
        list.map { it.toDomain() }
    }

    /**
     * 根据 ID 异步获取单个代码片段模型。
     */
    suspend fun getById(id: String): Snippet? {
        return snippetDao.byId(id)?.toDomain()
    }

    /**
     * 创建全新的代码片段对象。
     *
     * 教学解析：
     * 三端原子协同写入步骤 (Three-way Sync Protocol):
     * 1. 内存中构建 [Snippet] 数据模型对象，生成唯一 ID (`s_${timestamp}_${uuid}`) 并计算字节长度。
     * 2. 持久化到 SQLite 数据库 (`snippetDao.upsert`)。
     * 3. 异步写入外部 SAF 本地物理磁盘 (`LocalFileManager.writeSnippetToFile`)。
     * 4. 异步落盘至 JGit 本地工作树 (`gitManager.writeSnippetFile`)，以便随后的 Commit/Push。
     *
     * @param type 语言分类类型 [SnippetType]
     * @param initialContent 初始文本内容（若为 null 且开启样板代码，则自动注入对应类型的内置样板代码）
     * @param initialTitle 初始标题（若为 null 则根据系统时间自动推导）
     * @param repoTreeUriStr SAF 目录 URI 字符串
     * @param useBoilerplate 当 initialContent 为 null 时，是否自动注入 assets 中的默认样板代码
     * @return 构建并完成持久化的 [Snippet] 实例
     */
    suspend fun create(
        type: SnippetType,
        initialContent: String? = null,
        initialTitle: String? = null,
        repoTreeUriStr: String = "",
        useBoilerplate: Boolean = true
    ): Snippet {
        val now = System.currentTimeMillis()
        val title = initialTitle ?: Snippet.generateDefaultTitle(type)
        val content = initialContent ?: if (useBoilerplate) {
            SnippetTemplateManager.getTemplate(context, type)
        } else {
            ""
        }
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

        // 步骤 1: 将纯领域模型映射为实体并写入 Room 数据库
        snippetDao.upsert(SnippetEntity.fromDomain(snippet))

        // 步骤 2: 若 Context 与 SAF Tree URI 存在，在 IO 线程池中写入本地物理文件
        context?.let { ctx ->
            withContext(Dispatchers.IO) {
                LocalFileManager.writeSnippetToFile(ctx, snippet, repoTreeUriStr)
            }
        }

        // 步骤 3: 写入 JGit 沙盒物理工作树文件
        gitManager?.writeSnippetFile(snippet)
        return snippet
    }

    /**
     * 保存或更新已存在的代码片段。
     *
     * 自动刷新 `updatedAt` 修改时间戳与 `sizeBytes` 字节长度，并同步更新 Room、SAF 文件与 Git 仓。
     */
    suspend fun saveOrUpdate(snippet: Snippet, repoTreeUriStr: String = "") {
        val now = System.currentTimeMillis()
        val sizeBytes = snippet.content.toByteArray(Charsets.UTF_8).size
        val updated = snippet.copy(
            updatedAt = now,
            sizeBytes = sizeBytes,
            fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
        )
        // 1. 刷盘到 Room SQLite
        snippetDao.upsert(SnippetEntity.fromDomain(updated))
        // 2. 刷盘到 SAF 本地物理磁盘
        context?.let { ctx ->
            withContext(Dispatchers.IO) {
                LocalFileManager.writeSnippetToFile(ctx, updated, repoTreeUriStr)
            }
        }
        // 3. 刷盘到 Git 沙盒工作树
        gitManager?.writeSnippetFile(updated)
    }


    /**
     * 切换代码片段的星标收藏状态。
     */
    suspend fun toggleStar(id: String, currentStarred: Boolean) {
        snippetDao.setStar(id, !currentStarred)
    }

    /**
     * 将代码片段移入回收站 (软删除)。
     * 同步将物理文件移入隐藏 `.trash/` 目录，避免用户在文件管理器中看到"已删除"文件。
     */
    suspend fun trash(id: String, repoTreeUriStr: String = "") {
        val snippet = getById(id)
        snippetDao.trash(id, System.currentTimeMillis())
        snippet?.let { s ->
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.moveSnippetToTrash(ctx, s, repoTreeUriStr)
                }
            }
        }
    }

    /**
     * 从回收站还原代码片段。
     * 同步将物理文件从 `.trash/` 恢复到原目录。
     */
    suspend fun restore(id: String, repoTreeUriStr: String = "") {
        val snippet = getById(id)
        snippetDao.restore(id)
        snippet?.let { s ->
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.restoreSnippetFromTrash(ctx, s, repoTreeUriStr)
                }
            }
        }
    }

    /**
     * 彻底物理删除代码片段（同步清理数据库、`.trash/` 中的物理文件与 Git 沙盒文件）。
     */
    suspend fun purge(id: String, repoTreeUriStr: String = "") {
        val snippet = getById(id)
        if (snippet != null) {
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.purgeFromTrash(ctx, snippet, repoTreeUriStr)
                }
            }
            gitManager?.removeSnippetFile(snippet)
        }
        snippetDao.purge(id)
    }

    /**
     * 清理回收站中停放天数超过 [days] 天的过期废弃代码片段。
     * 同步清理 `.trash/` 目录中对应的物理文件。
     */
    suspend fun purgeExpired(days: Int = 30, repoTreeUriStr: String = "") {
        val cutoff = System.currentTimeMillis() - (days * 24L * 3600L * 1000L)
        val expired = snippetDao.allTrashedSnapshot().filter {
            it.trashedAt != null && it.trashedAt < cutoff
        }
        expired.forEach { entity ->
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.purgeFromTrash(ctx, entity.toDomain(), repoTreeUriStr)
                }
            }
        }
        snippetDao.purgeExpired(cutoff)
    }

    /**
     * 获取全量活动代码片段用于导出备份。
     */
    suspend fun allForExport(): List<Snippet> {
        return snippetDao.allActiveSnapshot().map { it.toDomain() }
    }

    /**
     * 获取活动代码片段总数。
     */
    suspend fun activeCount(): Int {
        return snippetDao.activeCount()
    }

    /**
     * 将 Git 沙盒仓中的物理文件方向解析导入到数据库中。
     */
    suspend fun syncGitFilesToDb() {
        gitManager?.importGitDirToDatabase(snippetDao)
    }

    /**
     * 重命名代码片段的标题及文件名。
     * 若文件名发生变更，同步清理旧文件名在 Git 沙盒与 SAF 工作区中的残留物理文件。
     */
    suspend fun updateRename(id: String, newTitle: String, newFileName: String, repoTreeUriStr: String = "") {
        val snippet = getById(id) ?: return
        val updated = snippet.copy(
            title = newTitle,
            fileName = newFileName,
            updatedAt = System.currentTimeMillis()
        )

        // 若文件名变更，清理旧文件名的物理残留（避免推送预览出现幽灵文件）
        if (snippet.fileName != newFileName && snippet.fileName.isNotBlank()) {
            gitManager?.removeSnippetFile(snippet)
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.deleteSnippetFile(ctx, snippet, repoTreeUriStr)
                }
            }
        }

        saveOrUpdate(updated, repoTreeUriStr)
    }

    /**
     * 修改代码片段所属的文件夹分类路径。
     * 若文件夹发生变更，同步清理旧路径下的物理文件残留。
     */
    suspend fun updateFolder(id: String, newFolder: String, repoTreeUriStr: String = "") {
        val snippet = getById(id) ?: return
        val cleanFolder = newFolder.trim().trim('/')
        if (cleanFolder.isNotBlank()) {
            val parentPath = if (cleanFolder.contains("/")) cleanFolder.substringBeforeLast("/") else ""
            folderDao?.upsert(FolderEntity(path = cleanFolder, parentPath = parentPath))
        }

        // 若文件夹路径变更，清理旧路径下的物理文件残留
        if (snippet.folder != cleanFolder) {
            gitManager?.removeSnippetFile(snippet)
            context?.let { ctx ->
                withContext(Dispatchers.IO) {
                    LocalFileManager.deleteSnippetFile(ctx, snippet, repoTreeUriStr)
                }
            }
        }

        val updated = snippet.copy(
            folder = cleanFolder,
            updatedAt = System.currentTimeMillis()
        )
        saveOrUpdate(updated, repoTreeUriStr)
    }

    /**
     * 将 Room 中的全量代码片段导出到 Git 沙盒工作树中。
     */
    suspend fun exportAllToGit() {
        val snippets = allForExport()
        gitManager?.exportAllSnippetsToDir(snippets)
    }

    /**
     * 将数据库中的活动代码片段与文件夹结构同步回写到用户物理工作区（SAF 或内部存储）。
     * 用于 Git Pull 完成后，确保拉取到的新内容在用户的物理文件夹中可见。
     *
     * @param repoTreeUriStr SAF 授权目录 URI 字符串
     */
    suspend fun syncAllToPhysicalStorage(repoTreeUriStr: String) = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext

        // 1. 先创建文件夹结构（包含空文件夹）
        gitManager?.getFolderStructure()?.forEach { folderPath ->
            LocalFileManager.createPhysicalFolder(ctx, folderPath, repoTreeUriStr)
        }

        // 2. 将所有活动片段写入物理存储
        val snippets = snippetDao.allActiveSnapshot().map { it.toDomain() }
        snippets.forEach { snippet ->
            LocalFileManager.writeSnippetToFile(ctx, snippet, repoTreeUriStr)
        }
    }
}


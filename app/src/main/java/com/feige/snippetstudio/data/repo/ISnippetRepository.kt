package com.feige.snippetstudio.data.repo

import android.content.Context
import com.feige.snippetstudio.data.local.FolderEntity
import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import kotlinx.coroutines.flow.Flow

/**
 * [ISnippetRepository] 是代码片段数据仓库的抽象契约接口。
 *
 * 架构设计职责：
 * 1. 遵循依赖倒置原则 (DIP)，使表现层 (ViewModel) 依赖于抽象而非具体实现。
 * 2. 支持在单元测试中注入 Fake 或 Mock 实现，摆脱对真实 Android Context / Room / Git 文件的运行期依赖。
 * 3. 统一规范代码片段与文件夹的 CRUD、SAF 外部工作区同步及 JGit 版本控制落盘行为。
 */
interface ISnippetRepository {

    /** 获取底层 SnippetDao 实例（供 SyncEngine 等组件使用） */
    fun getSnippetDao(): SnippetDao

    /**
     * 将外部授权的 SAF 目录或本地私有存储中的物理文件与文件夹全量扫描并同步更新至 Room 数据库。
     */
    suspend fun syncWithLocalRepository(context: Context, repoTreeUriStr: String)

    /**
     * 将外部授权的 SAF 目录或本地私有存储中的物理文件与文件夹全量扫描并同步更新至 Room 数据库（使用内部 Context）。
     */
    suspend fun syncWithLocalRepository(repoTreeUriStr: String)

    /** 响应式观察全量已持久化的文件夹列表 */
    fun observeFolders(): Flow<List<FolderEntity>>

    /** 显式创建新文件夹 */
    suspend fun createFolder(folderPath: String, repoTreeUriStr: String = "")

    /** 响应式观察未入回收站的活动代码片段列表 */
    fun observeActive(): Flow<List<Snippet>>

    /** 响应式观察星标 (收藏) 的代码片段列表 */
    fun observeStarred(): Flow<List<Snippet>>

    /** 响应式观察回收站中的代码片段列表 */
    fun observeTrashed(): Flow<List<Snippet>>

    /** 根据 [SnippetType] 类型（HTML, JS, Markdown, Prompt）过滤观察代码片段 */
    fun observeByType(type: SnippetType): Flow<List<Snippet>>

    /** 根据 ID 异步获取单个代码片段模型 */
    suspend fun getById(id: String): Snippet?

    /** 创建全新的代码片段对象 */
    suspend fun create(
        type: SnippetType,
        initialContent: String? = null,
        initialTitle: String? = null,
        repoTreeUriStr: String = "",
        useBoilerplate: Boolean = true
    ): Snippet

    /** 保存或更新已存在的代码片段 */
    suspend fun saveOrUpdate(snippet: Snippet, repoTreeUriStr: String = "")

    /** 切换代码片段的星标收藏状态 */
    suspend fun toggleStar(id: String, currentStarred: Boolean)

    /** 将代码片段移入回收站 (软删除) */
    suspend fun trash(id: String, repoTreeUriStr: String = "")

    /** 从回收站还原代码片段 */
    suspend fun restore(id: String, repoTreeUriStr: String = "")

    /** 彻底物理删除代码片段 */
    suspend fun purge(id: String, repoTreeUriStr: String = "")

    /** 清理回收站中停放天数超过 [days] 天的过期废弃代码片段 */
    suspend fun purgeExpired(days: Int = 30, repoTreeUriStr: String = "")

    /** 获取全量活动代码片段用于导出备份 */
    suspend fun allForExport(): List<Snippet>

    /** 获取活动代码片段总数 */
    suspend fun activeCount(): Int

    /** 将 Git 沙盒仓中的物理文件方向解析导入到数据库中 */
    suspend fun syncGitFilesToDb()

    /** 重命名代码片段的标题及文件名 */
    suspend fun updateRename(id: String, newTitle: String, newFileName: String, repoTreeUriStr: String = "")

    /** 修改代码片段所属的文件夹分类路径 */
    suspend fun updateFolder(id: String, newFolder: String, repoTreeUriStr: String = "")

    /** 将 Room 中的全量代码片段导出到 Git 沙盒工作树中 */
    suspend fun exportAllToGit()

    /** 将数据库中的活动代码片段与文件夹结构同步回写到用户物理工作区 */
    suspend fun syncAllToPhysicalStorage(
        repoTreeUriStr: String,
        remoteDeletedPaths: Set<String> = emptySet()
    )
}

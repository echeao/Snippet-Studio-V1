package com.feige.snippetstudio.data.git

import com.feige.snippetstudio.data.local.SnippetDao
import com.feige.snippetstudio.model.ConflictResolution
import com.feige.snippetstudio.model.SyncChangeItem
import com.feige.snippetstudio.model.SyncChangeType
import com.feige.snippetstudio.model.SyncConflict
import com.feige.snippetstudio.model.SyncDirection
import com.feige.snippetstudio.model.SyncPreview
import com.feige.snippetstudio.data.repo.SnippetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [SyncEngine] 是 Git 同步的细粒度操作控制引擎。
 *
 * 核心职责：
 * 1. 生成同步预览（Dry Run）：在不修改任何数据的前提下，对比本地/远端差异并生成变更清单。
 * 2. 冲突检测：识别本地与远端同时修改了同一文件的情况。
 * 3. 执行同步：在用户确认预览并解决冲突后，安全地执行 Pull/Push 操作。
 *
 * @param gitManager Git 版本控制管理器
 * @param snippetRepository 代码片段数据仓库
 * @param snippetDao 代码片段数据库 DAO
 */
class SyncEngine(
    private val gitManager: GitManager,
    private val snippetRepository: SnippetRepository,
    private val snippetDao: SnippetDao
) {

    /**
     * 生成 Pull 预览：fetch 远端最新，对比本地状态，产出变更清单与冲突列表。
     *
     * 对比逻辑：
     * - 远端有、沙盒无 → ADDED (INCOMING)：远端新增的文件
     * - 远端有、沙盒有但内容不同 → 检查本地 DB 是否也修改了
     *   - 本地未改 (DB == 沙盒) → UPDATED (INCOMING)：安全拉取
     *   - 本地已改 (DB != 沙盒) → CONFLICT：需要用户决定
     * - 远端无、沙盒有 → DELETED (INCOMING)：远端删除了（仅提示，不自动删本地）
     *
     * @param branch 目标分支
     * @param pat 鉴权 Token
     * @return 预览结果
     */
    suspend fun previewPull(branch: String, pat: String): Result<SyncPreview> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Fetch 远端最新（不修改工作树）
            gitManager.fetch(branch, pat).getOrThrow()

            // 2. 获取三方数据
            val remoteFiles = gitManager.getRemoteFileContents(branch).getOrThrow()
            val sandboxFiles = gitManager.getSandboxFileContents()
            val dbSnippets = snippetDao.allActiveSnapshot()
            val dbMap = dbSnippets.associate { entity ->
                val path = if (entity.folder.isBlank()) entity.fileName
                else "${entity.folder}/${entity.fileName}"
                path to entity.content
            }

            val changes = mutableListOf<SyncChangeItem>()
            val conflicts = mutableListOf<SyncConflict>()

            // 3. 遍历远端文件，检测新增/更新/冲突
            for ((path, remoteContent) in remoteFiles) {
                val fileName = path.substringAfterLast('/')
                val folder = if (path.contains('/')) path.substringBeforeLast('/') else ""
                val sandboxContent = sandboxFiles[path]
                val localContent = dbMap[path]

                if (sandboxContent == null) {
                    // 远端有、沙盒无 → 新增
                    changes.add(
                        SyncChangeItem(
                            fileName = fileName,
                            folder = folder,
                            changeType = SyncChangeType.ADDED,
                            direction = SyncDirection.INCOMING,
                            remoteContent = remoteContent
                        )
                    )
                } else if (sandboxContent != remoteContent) {
                    // 远端有、沙盒有但内容不同 → 检查本地是否也修改了
                    if (localContent != null && localContent != sandboxContent) {
                        // 本地也修改了 → 冲突
                        conflicts.add(
                            SyncConflict(
                                fileName = fileName,
                                folder = folder,
                                localContent = localContent,
                                remoteContent = remoteContent
                            )
                        )
                    } else {
                        // 本地未修改 → 安全更新
                        changes.add(
                            SyncChangeItem(
                                fileName = fileName,
                                folder = folder,
                                changeType = SyncChangeType.UPDATED,
                                direction = SyncDirection.INCOMING,
                                localContent = localContent,
                                remoteContent = remoteContent
                            )
                        )
                    }
                }
                // 远端有、沙盒有且内容相同 → 无变化，跳过
            }

            // 4. 检测远端已删除的文件（沙盒有但远端无）
            for ((path, _) in sandboxFiles) {
                if (path !in remoteFiles) {
                    val fileName = path.substringAfterLast('/')
                    val folder = if (path.contains('/')) path.substringBeforeLast('/') else ""
                    changes.add(
                        SyncChangeItem(
                            fileName = fileName,
                            folder = folder,
                            changeType = SyncChangeType.DELETED,
                            direction = SyncDirection.INCOMING
                        )
                    )
                }
            }

            SyncPreview(
                changes = changes,
                conflicts = conflicts,
                direction = SyncDirection.INCOMING
            )
        }
    }

    /**
     * 生成 Push 预览：将 DB 内容导出到沙盒后，通过 git status 检测未提交的变更。
     *
     * 对比逻辑（基于 git status）：
     * - 未追踪/新增文件 → ADDED (OUTGOING)
     * - 已修改文件 → UPDATED (OUTGOING)
     * - 已删除文件 → DELETED (OUTGOING)
     *
     * 这样即使用户新建片段时已同步写入沙盒，只要尚未 commit，
     * git status 仍能正确检测到变更，避免"没有修改内容"的误报。
     *
     * @return 预览结果
     */
    /**
     * 生成 Push 预览：在导出 DB 到沙盒后，通过 git status 检测未提交的变更。
     *
     * 优化逻辑（解决问题 B, C）：
     * 1. 允许穿透传入 [repoTreeUri]，在预览前静默触发一次物理磁盘至 DB 的扫描。
     * 2. 导出时触发沙盒镜像清理 (`cleanDeletedFiles`)，确保 deleted 状态能被 JGit 精准识别。
     *
     * @param repoTreeUri 用户授权的 SAF 目录 URI 字符串
     * @return 预览结果
     */
    suspend fun previewPush(repoTreeUri: String = ""): Result<SyncPreview> = withContext(Dispatchers.IO) {
        runCatching {
            // 0. 前置步骤：先触发物理磁盘至数据库的增量同步（若指定了 repoTreeUri）
            if (repoTreeUri.isNotBlank()) {
                snippetRepository.syncWithLocalRepository(repoTreeUri)
            }

            // 1. 先将 DB 全量导出到沙盒（内部自动执行 cleanDeletedFiles 沙盒镜像对齐）
            snippetRepository.exportAllToGit()

            // 2. 通过 git status 检测未提交的变更（含未追踪的新文件与已删除文件）
            val uncommittedChanges = gitManager.getUncommittedChanges()

            // 3. 一次性读取沙盒文件内容（用于展示变更详情）
            val sandboxFiles = if (uncommittedChanges.isNotEmpty()) {
                gitManager.getSandboxFileContents()
            } else {
                emptyMap()
            }

            val changes = mutableListOf<SyncChangeItem>()

            for ((path, changeType) in uncommittedChanges) {
                // 跳过隐藏文件
                val fileName = path.substringAfterLast('/')
                if (fileName.startsWith(".")) continue

                val folder = if (path.contains('/')) path.substringBeforeLast('/') else ""

                when (changeType) {
                    "ADDED" -> {
                        changes.add(
                            SyncChangeItem(
                                fileName = fileName,
                                folder = folder,
                                changeType = SyncChangeType.ADDED,
                                direction = SyncDirection.OUTGOING,
                                localContent = sandboxFiles[path]
                            )
                        )
                    }
                    "MODIFIED" -> {
                        changes.add(
                            SyncChangeItem(
                                fileName = fileName,
                                folder = folder,
                                changeType = SyncChangeType.UPDATED,
                                direction = SyncDirection.OUTGOING,
                                localContent = sandboxFiles[path]
                            )
                        )
                    }
                    "DELETED" -> {
                        changes.add(
                            SyncChangeItem(
                                fileName = fileName,
                                folder = folder,
                                changeType = SyncChangeType.DELETED,
                                direction = SyncDirection.OUTGOING
                            )
                        )
                    }
                }
            }

            SyncPreview(
                changes = changes,
                conflicts = emptyList(),
                direction = SyncDirection.OUTGOING
            )
        }
    }

    /**
     * 执行 Pull 操作（含冲突解决与远端删除透传）。
     *
     * 流程：
     * 1. 按用户选择解决冲突（写入沙盒）
     * 2. 捕获拉取前沙盒文件列表，执行 git pull
     * 3. 计算远端删除的文件相对路径集合 `remoteDeletedPaths`
     * 4. 将沙盒文件导入 DB
     * 5. 回写物理工作区，精准擦除远端删除的文件及对应的 DB 记录（解决问题 A/E）
     *
     * @param conflicts 已解决的冲突列表
     * @param branch 目标分支
     * @param pat 鉴权 Token
     * @param repoTreeUri 用户工作区 SAF URI
     * @param onProgress 进度回调
     */
    suspend fun executePull(
        conflicts: List<SyncConflict>,
        branch: String,
        pat: String,
        repoTreeUri: String,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 解决冲突：按用户选择写入沙盒
            if (conflicts.isNotEmpty()) {
                onProgress("正在解决 ${conflicts.size} 个冲突...")
                for (conflict in conflicts) {
                    val relativePath = if (conflict.folder.isBlank()) conflict.fileName
                    else "${conflict.folder}/${conflict.fileName}"

                    when (conflict.resolution) {
                        ConflictResolution.KEEP_LOCAL -> {
                            gitManager.writeSandboxFile(relativePath, conflict.localContent)
                        }
                        ConflictResolution.KEEP_REMOTE -> {
                            gitManager.writeSandboxFile(relativePath, conflict.remoteContent)
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            gitManager.writeSandboxFile(relativePath, conflict.localContent)
                            val base = conflict.fileName.substringBeforeLast('.')
                            val ext = conflict.fileName.substringAfterLast('.', "")
                            val remoteName = if (ext.isNotBlank()) "${base}_remote.$ext" else "${base}_remote"
                            val remotePath = if (conflict.folder.isBlank()) remoteName
                            else "${conflict.folder}/$remoteName"
                            gitManager.writeSandboxFile(remotePath, conflict.remoteContent)
                        }
                        ConflictResolution.PENDING -> {
                            gitManager.writeSandboxFile(relativePath, conflict.localContent)
                        }
                    }
                }
            }

            // 2. 捕获拉取前沙盒文件列表，用于计算远端删除项目
            val beforeSandboxFiles = gitManager.getSandboxFileContents().keys

            onProgress("正在从远端拉取...")
            gitManager.pull("", branch, pat).getOrThrow()

            // 计算从沙盒中消失的文件相对路径（即远端删除了的文件）
            val afterSandboxFiles = gitManager.getSandboxFileContents().keys
            val remoteDeletedPaths = beforeSandboxFiles - afterSandboxFiles

            // 3. 导入数据库
            onProgress("正在写入数据库...")
            snippetRepository.syncGitFilesToDb()

            // 4. 回写物理工作区，精准清理物理文件与 DB 中的废弃记录
            onProgress("正在同步到文件夹...")
            snippetRepository.syncAllToPhysicalStorage(repoTreeUri, remoteDeletedPaths)

            onProgress("拉取完成")
            Unit
        }
    }

    /**
     * 执行 Push 操作。
     *
     * @param url 远端仓库地址
     * @param branch 目标分支
     * @param pat 鉴权 Token
     * @param repoTreeUri 用户工作区 SAF URI
     * @param onProgress 进度回调
     */
    suspend fun executePush(
        url: String,
        branch: String,
        pat: String,
        repoTreeUri: String = "",
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("正在准备本地文件...")
            if (repoTreeUri.isNotBlank()) {
                snippetRepository.syncWithLocalRepository(repoTreeUri)
            }
            snippetRepository.exportAllToGit()

            onProgress("正在推送到远端...")
            gitManager.commitAndPush(
                commitMessage = "sync: Snippet Studio push at ${System.currentTimeMillis()}",
                url = url,
                branch = branch,
                pat = pat
            ).getOrThrow()

            onProgress("推送完成")
            Unit
        }
    }
}

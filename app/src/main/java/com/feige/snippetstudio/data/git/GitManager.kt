package com.feige.snippetstudio.data.git

import android.content.Context
import com.feige.snippetstudio.model.DiffLine
import com.feige.snippetstudio.model.DiffType
import com.feige.snippetstudio.model.GitCommitInfo
import com.feige.snippetstudio.model.Snippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * [GitManager] 是负责与 Git 版本控制系统通信与本地文件同步的核心服务对象。
 *
 * 底层技术实现：使用 Eclipse JGit 纯 Java 类库（不需要系统安装命令行 git），在 Android 应用内部沙盒目录
 * `files/git_snippets_repo` 中维持一个独立的 Git 工作树，支持：
 * 1. 远程 Git 仓库连接测试与 Personal Access Token (PAT) 鉴权。
 * 2. 自动克隆 (Clone)、初始化 (Init)、拉取 (Pull) 及推送 (Push)。
 * 3. 将数据库中的代码片段单向/双向落盘同步至 Git 本地工作树。
 * 4. Git 冲突解决与备份策略：若遇到 Merge 冲突，自动将本地文件备份为 `conflict_{timestamp}_filename`，并以远端为准重置。
 *
 * @param context 应用上下文对象
 */
class GitManager(private val context: Context) {

    companion object {
        /**
         * 支持导入的文件扩展名白名单。
         * 只有匹配这些扩展名的文本文件才会被导入到数据库中，
         * 避免二进制文件（如 .zip, .rar, .png）被错误读取导致应用卡死。
         */
        private val SUPPORTED_EXTENSIONS = setOf(
            "html", "htm",
            "js", "jsx", "ts", "tsx",
            "md", "markdown",
            "txt", "text",
            "json", "xml", "css", "csv"
        )

        /** 单文件最大导入大小：2MB。超过此大小的文件将被跳过。 */
        private const val MAX_IMPORT_FILE_SIZE = 2L * 1024 * 1024

        /**
         * 判断文件是否为受支持的文本文件类型。
         */
        private fun isSupportedFile(file: File): Boolean {
            val ext = file.extension.lowercase()
            return ext in SUPPORTED_EXTENSIONS && file.length() <= MAX_IMPORT_FILE_SIZE
        }
    }

    /**
     * Git 本地工作树在 Android 应用内部私有存储目录中的根路径。
     */
    private val gitRepoDir: File by lazy {
        File(context.filesDir, "git_snippets_repo").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * 测试远程 Git 仓库的连接状态以及 Personal Access Token (PAT) 凭据的有效性。
     *
     * @param url 远程 Git 仓库地址 (如 https://github.com/user/repo.git)
     * @param branch 目标分支名称 (如 main 或 master)
     * @param pat 访问令牌 PAT (AccessToken)
     * @return 包含连接结果 (true / false) 的 [Result] 包装对象
     */
    suspend fun testConnection(url: String, branch: String, pat: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (url.isBlank()) return@runCatching false
            // 使用 UsernamePasswordCredentialsProvider，Token 作为密码传入鉴权
            val provider = if (pat.isNotBlank()) UsernamePasswordCredentialsProvider("token", pat) else null
            val lsCommand = Git.lsRemoteRepository()
                .setRemote(url)

            if (provider != null) {
                lsCommand.setCredentialsProvider(provider)
            }

            val refs = lsCommand.call()
            refs.isNotEmpty()
        }
    }

    /**
     * 初始化本地 Git 仓库或者从远端 URL 克隆仓库至本地沙盒目录。
     *
     * @param url 远程仓库地址（若为空则在本地初始化纯新仓库）
     * @param branch 克隆分支名
     * @param pat 鉴权 Token
     */
    suspend fun initOrClone(url: String, branch: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(gitRepoDir, ".git")
            val provider = if (pat.isNotBlank()) UsernamePasswordCredentialsProvider("token", pat) else null

            if (!gitDir.exists()) {
                // 如果本地不存在 .git 目录且提供了远端 URL，尝试执行 clone
                if (url.isNotBlank()) {
                    // JGit cloneRepository 要求目标目录不存在或为空
                    // 而 gitRepoDir 可能已被 lazy init 创建为空目录，需先清理
                    if (gitRepoDir.exists()) {
                        gitRepoDir.deleteRecursively()
                    }

                    val cloneCmd = Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(gitRepoDir)
                        .setBranch(branch.ifBlank { "main" })

                    if (provider != null) {
                        cloneCmd.setCredentialsProvider(provider)
                    }
                    cloneCmd.call().close()
                } else {
                    // 本地没有远端 URL 时直接执行 git init
                    val git = Git.init().setDirectory(gitRepoDir).call()
                    git.close()
                }
            } else {
                // 若本地已被初始化为 Git 仓，则更新其 origin 远程地址
                Git.open(gitRepoDir).use { git ->
                    val config: StoredConfig = git.repository.config
                    config.setString("remote", "origin", "url", url)
                    config.save()
                }
            }
        }
    }

    /**
     * 将单一 [Snippet] 代码片段内容写入本地 Git 工作树中的物理文件。
     */
    suspend fun writeSnippetFile(snippet: Snippet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            val targetDir = if (snippet.folder.isBlank()) gitRepoDir else File(gitRepoDir, snippet.folder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        }
    }

    /**
     * 从本地 Git 工作树目录中彻底物理删除指定 [Snippet] 的文件。
     */
    suspend fun removeSnippetFile(snippet: Snippet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            val targetDir = if (snippet.folder.isBlank()) gitRepoDir else File(gitRepoDir, snippet.folder)
            File(targetDir, fileName).delete()
            Unit
        }
    }

    /**
     * 批量将 Room 数据库中的全量代码片段导出并落盘至 Git 沙盒工作树中。
     */
    suspend fun exportAllSnippetsToDir(snippets: List<Snippet>) = withContext(Dispatchers.IO) {
        snippets.forEach { snippet ->
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            val targetDir = if (snippet.folder.isBlank()) gitRepoDir else File(gitRepoDir, snippet.folder).apply { if (!exists()) mkdirs() }
            val file = File(targetDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        }
    }

    /**
     * 遍历 Git 沙盒工作树文件，反向同步导入或更新至 Room 数据库。
     *
     * @param snippetDao 数据访问对象
     */
    suspend fun importGitDirToDatabase(snippetDao: com.feige.snippetstudio.data.local.SnippetDao) = withContext(Dispatchers.IO) {
        val files = gitRepoDir.walkTopDown().filter {
            it.isFile && !it.name.startsWith(".") && !it.name.startsWith("README") && !it.path.contains(".git")
                    && isSupportedFile(it)
        }.toList()

        val now = System.currentTimeMillis()
        val currentEntities = snippetDao.allActiveSnapshot().associateBy { it.fileName }

        files.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            val fileName = file.name
            val folder = file.parentFile?.relativeToOrNull(gitRepoDir)?.path?.replace('\\', '/') ?: ""
            val existing = currentEntities[fileName]

            if (existing != null) {
                // 如果内容或文件夹路径发生变动则更新 Room 记录
                if (existing.content != content || existing.folder != folder) {
                    val updated = existing.copy(
                        content = content,
                        folder = folder,
                        updatedAt = now,
                        sizeBytes = content.toByteArray(Charsets.UTF_8).size
                    )
                    snippetDao.upsert(updated)
                }
            } else {
                // 解析扩展名并自动生成新的代码片段记录
                val type = com.feige.snippetstudio.model.SnippetType.fromFileName(fileName)
                val title = file.nameWithoutExtension
                val snippet = Snippet(
                    id = "git_${now}_${java.util.UUID.randomUUID().toString().take(4)}",
                    type = type,
                    title = title,
                    fileName = fileName,
                    folder = folder,
                    content = content,
                    createdAt = now,
                    updatedAt = now,
                    sizeBytes = content.toByteArray(Charsets.UTF_8).size
                )
                snippetDao.upsert(com.feige.snippetstudio.data.local.SnippetEntity.fromDomain(snippet))
            }
        }
    }

    /**
     * 执行 Git 提交 (`git add .` -> `git commit -m`) 并推送 (`git push`) 到远端 Git 仓库。
     *
     * @param commitMessage 提交说明消息
     * @param url 远端仓库地址
     * @param branch 推送的目标分支名称
     * @param pat 个人访问令牌 PAT
     */
    suspend fun commitAndPush(
        commitMessage: String,
        url: String,
        branch: String,
        pat: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!File(gitRepoDir, ".git").exists()) {
                initOrClone(url, branch, pat).getOrThrow()
            }

            Git.open(gitRepoDir).use { git ->
                // 将所有未暂存与新增文件添加至暂存区
                git.add().addFilepattern(".").call()

                // 如果存在变动文件或未追踪文件，则进行提交
                val status = git.status().call()
                if (status.hasUncommittedChanges() || status.untracked.isNotEmpty()) {
                    git.commit()
                        .setMessage(commitMessage)
                        .setAuthor("Snippet Studio", "app@snippetstudio.local")
                        .call()
                }

                // 推送变动到远端仓库
                if (url.isNotBlank()) {
                    val pushCmd = git.push().setRemote("origin")
                    val targetBranch = branch.ifBlank { "main" }
                    pushCmd.setRefSpecs(org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/$targetBranch"))

                    if (pat.isNotBlank()) {
                        pushCmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("token", pat))
                    }
                    pushCmd.call()
                }
            }
            Unit
        }
    }

    /**
     * 从远程 Git 仓库拉取最新提交 (`git pull`)，包含冲突判定与本地副本保护策略。
     *
     * 教学解析：
     * 1. `withContext(Dispatchers.IO)`: 强制将包含网络 I/O 与文件读写的异步任务切换至 Coroutine IO 线程池，防止主线程 UI 假死。
     * 2. 冲突备份算法 (Conflict Backup Algorithm): 当拉取发生合并冲突 (`MergeStatus.CONFLICTING`) 时，
     *    系统不直接抛出 Fatal Exception，而是将冲突的本地文件拷贝并命名为 `conflict_{timestamp}_{name}` 保存下来，
     *    随后调用 `ResetType.HARD` 将分支重置回干净的远端主流版本。
     */
    suspend fun pull(url: String, branch: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 检查本地沙盒目录中是否存在 .git 管理元目录，若没有则自动发起首次克隆/初始化
            if (!File(gitRepoDir, ".git").exists()) {
                initOrClone(url, branch, pat).getOrThrow()
                return@runCatching
            }

            // 打开本地 Git 工作树句柄，use 关键字可自动在代码块结束时关闭句柄释放资源
            Git.open(gitRepoDir).use { git ->
                // 构建 PullCommand 组装拉取参数
                val pullCmd = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branch.ifBlank { "main" })

                // 设置 Token 凭据进行 HTTPS 鉴权
                if (pat.isNotBlank()) {
                    pullCmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("token", pat))
                }

                // 执行与远程服务器的握手与代码 Pull 操作
                val pullResult = pullCmd.call()
                
                // 校验合并状态 (MergeResult)
                val mergeResult = pullResult.mergeResult
                if (mergeResult != null && mergeResult.mergeStatus == org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING) {
                    val conflicts = mergeResult.conflicts
                    // 遍历所有冲突文件的相对路径集合
                    conflicts?.keys?.forEach { conflictPath ->
                        val originalFile = File(gitRepoDir, conflictPath)
                        if (originalFile.exists()) {
                            // 备份本地有冲突的版本，避免用户的改动无痕丢失
                            val conflictBackupFile = File(
                                gitRepoDir, 
                                "conflict_${System.currentTimeMillis()}_${originalFile.name}"
                            )
                            originalFile.copyTo(conflictBackupFile, overwrite = true)
                        }
                    }

                    // 执行类似 `git reset --hard HEAD` 强制清除冲突锁止状态
                    git.reset()
                        .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                        .call()
                }
            }
            Unit
        }
    }


    /**
     * 获取 Git 本地沙盒仓中的全部非隐藏有效代码片段文件。
     */
    suspend fun getAllSnippetFiles(): List<File> = withContext(Dispatchers.IO) {
        gitRepoDir.listFiles()?.filter { 
            it.isFile && !it.name.startsWith(".") && !it.name.startsWith("README")
        } ?: emptyList()
    }

    /**
     * 获取 Git 沙盒仓中的所有有效文件夹相对路径（排除 .git 和隐藏目录）。
     * 用于同步后在用户物理工作区中重建目录结构。
     */
    suspend fun getFolderStructure(): List<String> = withContext(Dispatchers.IO) {
        gitRepoDir.walkTopDown()
            .filter { it.isDirectory && it != gitRepoDir && !it.name.startsWith(".") && !it.path.contains(".git") }
            .mapNotNull { it.relativeToOrNull(gitRepoDir)?.path?.replace('\\', '/') }
            .filter { it.isNotBlank() }
            .toList()
    }

    /**
     * 检测 Git 沙盒工作树中尚未提交的变更（含未追踪的新文件）。
     *
     * 执行 `git add .` 后读取 status，返回所有已暂存的变更文件路径集合。
     * 用于 Push 预览阶段判断是否有内容需要推送。
     *
     * @return 有变更的文件相对路径 → 变更类型 ("ADDED" / "MODIFIED" / "DELETED")
     */
    suspend fun getUncommittedChanges(): Map<String, String> = withContext(Dispatchers.IO) {
        val gitDir = File(gitRepoDir, ".git")
        if (!gitDir.exists()) return@withContext emptyMap()

        Git.open(gitRepoDir).use { git ->
            // 将所有未暂存与新增文件添加至暂存区
            git.add().addFilepattern(".").call()

            val status = git.status().call()
            val changes = mutableMapOf<String, String>()

            // 新增文件（之前未追踪，现已暂存）
            status.added.forEach { changes[it] = "ADDED" }
            // 已修改文件
            status.changed.forEach { changes[it] = "MODIFIED" }
            // 已删除文件
            status.removed.forEach { changes[it] = "DELETED" }
            // 仍未追踪的文件（理论上 add . 后不应有，但保险起见）
            status.untracked.forEach { changes[it] = "ADDED" }

            changes
        }
    }

    // ===== 同步预览与细粒度控制 API =====

    /**
     * 执行 `git fetch`：仅下载远端最新对象，不修改本地工作树。
     * 用于预览阶段安全地获取远端最新状态。
     *
     * @param branch 目标分支名
     * @param pat 鉴权 Token
     */
    suspend fun fetch(branch: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!File(gitRepoDir, ".git").exists()) {
                throw IllegalStateException("本地仓库未初始化，请先执行连接/克隆")
            }
            Git.open(gitRepoDir).use { git ->
                val fetchCmd = git.fetch()
                    .setRemote("origin")
                    .setRefSpecs(org.eclipse.jgit.transport.RefSpec("+refs/heads/${branch.ifBlank { "main" }}:refs/remotes/origin/${branch.ifBlank { "main" }}"))
                if (pat.isNotBlank()) {
                    fetchCmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("token", pat))
                }
                fetchCmd.setTimeout(30)
                fetchCmd.call()
            }
            Unit
        }
    }

    /**
     * 获取远端分支的文件内容映射（需在 fetch 之后调用）。
     * 通过读取 `origin/<branch>` 引用的 tree 对象，遍历所有 blob 并返回内容。
     *
     * @param branch 目标分支名
     * @return relativePath → content 映射
     */
    suspend fun getRemoteFileContents(branch: String): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!File(gitRepoDir, ".git").exists()) {
                throw IllegalStateException("本地仓库未初始化")
            }
            val result = mutableMapOf<String, String>()
            Git.open(gitRepoDir).use { git ->
                val repository = git.repository
                val branchName = branch.ifBlank { "main" }
                val remoteRef = repository.resolve("origin/$branchName")
                    ?: throw IllegalStateException("远端分支 origin/$branchName 不存在，请先 fetch")

                val commit = repository.parseCommit(remoteRef)
                val treeWalk = org.eclipse.jgit.treewalk.TreeWalk(repository)
                treeWalk.addTree(commit.tree)
                treeWalk.isRecursive = true

                while (treeWalk.next()) {
                    val path = treeWalk.pathString
                    val fileName = path.substringAfterLast('/')
                    // 跳过隐藏文件、README 及不支持的文件类型
                    if (fileName.startsWith(".") || fileName.startsWith("README")) continue
                    val file = File(gitRepoDir, path)
                    if (!isSupportedFile(file)) continue

                    val objectId = treeWalk.getObjectId(0)
                    val loader = repository.open(objectId)
                    val content = String(loader.bytes, Charsets.UTF_8)
                    result[path] = content
                }
                treeWalk.close()
            }
            result
        }
    }

    /**
     * 获取 Git 沙盒工作树中所有受支持文件的内容映射。
     *
     * @return relativePath → content 映射
     */
    suspend fun getSandboxFileContents(): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        gitRepoDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".") && !it.name.startsWith("README") && !it.path.contains(".git") && isSupportedFile(it) }
            .forEach { file ->
                val relativePath = file.relativeToOrNull(gitRepoDir)?.path?.replace('\\', '/') ?: return@forEach
                result[relativePath] = file.readText(Charsets.UTF_8)
            }
        result
    }

    /**
     * 将指定内容写入 Git 沙盒工作树中的文件（用于冲突解决后覆写）。
     *
     * @param relativePath 相对路径
     * @param content 文件内容
     */
    suspend fun writeSandboxFile(relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val file = File(gitRepoDir, relativePath)
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        file.writeText(content, Charsets.UTF_8)
    }

    // ===== Git 历史履历查询 API =====

    /**
     * 获取整个 Git 沙盒仓库的全量提交历史（不限定文件）。
     *
     * @param maxCount 最大返回条数（默认 50）
     * @return 提交历史列表（时间降序）
     */
    suspend fun getRepoLog(maxCount: Int = 50): Result<List<GitCommitInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(gitRepoDir, ".git")
            if (!gitDir.exists()) return@runCatching emptyList()

            Git.open(gitRepoDir).use { git ->
                val logCommand = git.log().setMaxCount(maxCount)
                val commits = mutableListOf<GitCommitInfo>()

                for (revCommit in logCommand.call()) {
                    commits.add(
                        GitCommitInfo(
                            commitId = revCommit.name,
                            shortId = revCommit.name.take(7),
                            message = revCommit.shortMessage,
                            author = revCommit.authorIdent.name,
                            timestamp = revCommit.commitTime * 1000L
                        )
                    )
                }
                commits
            }
        }
    }

    /**
     * 获取指定文件的 Git 提交历史列表。
     *
     * @param fileName 文件名
     * @param folder 文件夹相对路径
     * @return 提交历史列表（时间降序）
     */
    suspend fun getFileHistory(fileName: String, folder: String): Result<List<GitCommitInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(gitRepoDir, ".git")
            if (!gitDir.exists()) return@runCatching emptyList()

            val relativePath = if (folder.isBlank()) fileName else "$folder/$fileName"

            Git.open(gitRepoDir).use { git ->
                val logCommand = git.log().addPath(relativePath)
                val commits = mutableListOf<GitCommitInfo>()

                for (revCommit in logCommand.call()) {
                    commits.add(
                        GitCommitInfo(
                            commitId = revCommit.name,
                            shortId = revCommit.name.take(7),
                            message = revCommit.shortMessage,
                            author = revCommit.authorIdent.name,
                            timestamp = revCommit.commitTime * 1000L
                        )
                    )
                }
                commits
            }
        }
    }

    /**
     * 获取某次提交中指定文件的完整内容。
     *
     * @param commitId 提交 SHA-1
     * @param relativePath 文件相对路径
     * @return 文件内容字符串
     */
    suspend fun getFileContentAtCommit(commitId: String, relativePath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(gitRepoDir).use { git ->
                val repository = git.repository
                val commit = repository.parseCommit(org.eclipse.jgit.lib.ObjectId.fromString(commitId))
                val treeWalk = org.eclipse.jgit.treewalk.TreeWalk.forPath(repository, relativePath, commit.tree)
                    ?: throw IllegalStateException("文件不存在于此提交: $relativePath")
                val objectId = treeWalk.getObjectId(0)
                val loader = repository.open(objectId)
                String(loader.bytes, Charsets.UTF_8)
            }
        }
    }

    /**
     * 获取两个版本之间指定文件的简单行级 Diff。
     *
     * @param commitIdOld 旧版本 commit ID
     * @param commitIdNew 新版本 commit ID
     * @param relativePath 文件相对路径
     * @return Diff 行列表
     */
    suspend fun getFileDiff(commitIdOld: String, commitIdNew: String, relativePath: String): Result<List<DiffLine>> = withContext(Dispatchers.IO) {
        runCatching {
            val oldContent = getFileContentAtCommit(commitIdOld, relativePath).getOrDefault("")
            val newContent = getFileContentAtCommit(commitIdNew, relativePath).getOrDefault("")

            val oldLines = oldContent.lines()
            val newLines = newContent.lines()
            val diffLines = mutableListOf<DiffLine>()

            // 简单 LCS-based diff
            val maxLen = maxOf(oldLines.size, newLines.size)
            var oldIdx = 0
            var newIdx = 0

            while (oldIdx < oldLines.size || newIdx < newLines.size) {
                when {
                    oldIdx >= oldLines.size -> {
                        diffLines.add(DiffLine(DiffType.ADD, newLines[newIdx], newLineNum = newIdx + 1))
                        newIdx++
                    }
                    newIdx >= newLines.size -> {
                        diffLines.add(DiffLine(DiffType.DELETE, oldLines[oldIdx], oldLineNum = oldIdx + 1))
                        oldIdx++
                    }
                    oldLines[oldIdx] == newLines[newIdx] -> {
                        diffLines.add(DiffLine(DiffType.CONTEXT, oldLines[oldIdx], oldLineNum = oldIdx + 1, newLineNum = newIdx + 1))
                        oldIdx++
                        newIdx++
                    }
                    else -> {
                        diffLines.add(DiffLine(DiffType.DELETE, oldLines[oldIdx], oldLineNum = oldIdx + 1))
                        diffLines.add(DiffLine(DiffType.ADD, newLines[newIdx], newLineNum = newIdx + 1))
                        oldIdx++
                        newIdx++
                    }
                }
            }
            diffLines
        }
    }
}


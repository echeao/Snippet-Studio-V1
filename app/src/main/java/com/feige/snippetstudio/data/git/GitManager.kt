package com.feige.snippetstudio.data.git

import android.content.Context
import com.feige.snippetstudio.model.Snippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager(private val context: Context) {

    private val gitRepoDir: File by lazy {
        File(context.filesDir, "git_snippets_repo").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * 测试远程仓库连接及 PAT 鉴权
     */
    suspend fun testConnection(url: String, branch: String, pat: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (url.isBlank()) return@runCatching false
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
     * 初始化本地仓库或克隆远程仓库
     */
    suspend fun initOrClone(url: String, branch: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(gitRepoDir, ".git")
            val provider = if (pat.isNotBlank()) UsernamePasswordCredentialsProvider("token", pat) else null

            if (!gitDir.exists()) {
                // 如果仓库不存在且提供了 URL，尝试 Clone，否则本地 Init
                if (url.isNotBlank()) {
                    val cloneCmd = Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(gitRepoDir)
                        .setBranch(branch.ifBlank { "main" })

                    if (provider != null) {
                        cloneCmd.setCredentialsProvider(provider)
                    }
                    cloneCmd.call().close()
                } else {
                    val git = Git.init().setDirectory(gitRepoDir).call()
                    git.close()
                }
            } else {
                // 更新远程仓库配置
                Git.open(gitRepoDir).use { git ->
                    val config: StoredConfig = git.repository.config
                    config.setString("remote", "origin", "url", url)
                    config.save()
                }
            }
        }
    }

    /**
     * 将 Snippet 代码写入本地 Git 沙盒仓文件
     */
    suspend fun writeSnippetFile(snippet: Snippet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            val file = File(gitRepoDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        }
    }

    /**
     * 从本地 Git 沙盒仓删除对应的文件
     */
    suspend fun removeSnippetFile(snippet: Snippet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            File(gitRepoDir, fileName).delete()
            Unit
        }
    }

    /**
     * 将数据库中的全量 Snippet 导出落盘至 Git 沙盒仓
     */
    suspend fun exportAllSnippetsToDir(snippets: List<Snippet>) = withContext(Dispatchers.IO) {
        snippets.forEach { snippet ->
            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            val file = File(gitRepoDir, fileName)
            file.writeText(snippet.content, Charsets.UTF_8)
        }
    }

    /**
     * 从 Git 沙盒仓文件反向同步导入/更新到 Room 数据库
     */
    suspend fun importGitDirToDatabase(snippetDao: com.feige.snippetstudio.data.local.SnippetDao) = withContext(Dispatchers.IO) {
        val files = gitRepoDir.listFiles()?.filter {
            it.isFile && !it.name.startsWith(".") && !it.name.startsWith("README")
        } ?: return@withContext

        val now = System.currentTimeMillis()
        val currentEntities = snippetDao.allActiveSnapshot().associateBy { it.fileName }

        files.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            val fileName = file.name
            val existing = currentEntities[fileName]

            if (existing != null) {
                // 如果内容变动则更新
                if (existing.content != content) {
                    val updated = existing.copy(
                        content = content,
                        updatedAt = now,
                        sizeBytes = content.toByteArray(Charsets.UTF_8).size
                    )
                    snippetDao.upsert(updated)
                }
            } else {
                // 解析扩展名并生成新 Snippet
                val type = com.feige.snippetstudio.model.SnippetType.fromFileName(fileName)
                val title = file.nameWithoutExtension
                val snippet = Snippet(
                    id = "git_${now}_${java.util.UUID.randomUUID().toString().take(4)}",
                    type = type,
                    title = title,
                    fileName = fileName,
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
     * 提交本地修改并推送至远程仓库
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
                // Add all files
                git.add().addFilepattern(".").call()

                // Commit
                val status = git.status().call()
                if (status.hasUncommittedChanges() || status.untracked.isNotEmpty()) {
                    git.commit()
                        .setMessage(commitMessage)
                        .setAuthor("Snippet Studio", "app@snippetstudio.local")
                        .call()
                }

                // Push
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
     * 从远程仓库拉取更新（包含冲突自动处理与副本安全备份策略）
     */
    suspend fun pull(url: String, branch: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!File(gitRepoDir, ".git").exists()) {
                initOrClone(url, branch, pat).getOrThrow()
                return@runCatching
            }

            Git.open(gitRepoDir).use { git ->
                val pullCmd = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branch.ifBlank { "main" })

                if (pat.isNotBlank()) {
                    pullCmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("token", pat))
                }

                val pullResult = pullCmd.call()
                
                // 检查是否存在 Merge 冲突
                val mergeResult = pullResult.mergeResult
                if (mergeResult != null && mergeResult.mergeStatus == org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING) {
                    val conflicts = mergeResult.conflicts
                    conflicts?.keys?.forEach { conflictPath ->
                        val originalFile = File(gitRepoDir, conflictPath)
                        if (originalFile.exists()) {
                            // 备份本地冲突版本，避免丢失用户修改
                            val conflictBackupFile = File(
                                gitRepoDir, 
                                "conflict_${System.currentTimeMillis()}_${originalFile.name}"
                            )
                            originalFile.copyTo(conflictBackupFile, overwrite = true)
                        }
                    }

                    // 自动重置并采用远端版本作为主文件解决 Merge 冲突状态
                    git.reset()
                        .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                        .call()
                }
            }
            Unit
        }
    }

    /**
     * 获取沙盒仓内所有的文件用于同步给 Room
     */
    suspend fun getAllSnippetFiles(): List<File> = withContext(Dispatchers.IO) {
        gitRepoDir.listFiles()?.filter { 
            it.isFile && !it.name.startsWith(".") && !it.name.startsWith("README")
        } ?: emptyList()
    }
}
